package wang.nerom.pschd.leader

import com.alipay.remoting.BizContext
import com.alipay.remoting.NamedThreadFactory
import com.alipay.remoting.rpc.protocol.SyncUserProcessor
import com.google.common.collect.ImmutableList
import org.slf4j.LoggerFactory
import wang.nerom.pschd.config.PschdConfig
import wang.nerom.pschd.connection.PschdConnection
import wang.nerom.pschd.connection.PschdEndpoint
import wang.nerom.pschd.connection.PschdRequest
import wang.nerom.pschd.connection.PschdResponse
import wang.nerom.pschd.event.DisruptorEventBus
import wang.nerom.pschd.event.Event
import wang.nerom.pschd.event.EventHandler
import wang.nerom.pschd.util.HostUtil
import wang.nerom.pschd.util.RandomUtil
import java.util.ArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock

class PschdNode {
    private val log = LoggerFactory.getLogger(javaClass)
    private val connection: PschdConnection
    private val eventBus = DisruptorEventBus<PschdEvent>()
    private val readWriteLock = ReentrantReadWriteLock()
    private val timer = Executors.newScheduledThreadPool(5, NamedThreadFactory("election-timer"))
    private var state = PschdNodeState.FOLLOWER
    private var term = 0L
    private var leader = PschdEndpoint.EMPTY
    private var localEndpoint: PschdEndpoint

    private var candidateList: List<PschdEndpoint> = arrayListOf()
    /**
     * in millisecond
     */
    private var leadTimestamp = 0L
    private var timeout = 1000L
    private var maxDelay = 500L

    private var ballotCandidate = PschdEndpoint.EMPTY
    private var ballotTimestamp = 0L
    private var ballotCandidates: ArrayList<PschdEndpoint> = arrayListOf()

    constructor(config: PschdConfig) {
        this.localEndpoint = PschdEndpoint(config.localPort, HostUtil.getIpv4())
        this.connection = PschdConnection(config.localPort)
        this.candidateList = parseCandidates(config.candidates)
    }

    private fun parseCandidates(candidates: String): List<PschdEndpoint> {
        if (candidates.isBlank()) {
            return listOf(localEndpoint)
        }
        val candidateList = candidates.trim().split(",").map { ca ->
            val candidate = ca.trim().split(":")
            PschdEndpoint(candidate[1].toInt(), candidate[0].trim())
        }
        val resultList = ArrayList<PschdEndpoint>()
        if (!candidateList.contains(localEndpoint)) {
            resultList.add(localEndpoint)
        }
        resultList.addAll(candidateList)
        return ImmutableList.copyOf(resultList)
    }

    fun doInit() {
        connection.registerProcessor(object : SyncUserProcessor<PschdRequest>() {
            override fun interest(): String {
                return PschdRequest::class.java.name
            }

            override fun handleRequest(bizCtx: BizContext?, request: PschdRequest?): PschdResponse {
                var eventType = when (request?.action) {
                    PschdRequest.Action.HEARTBEAT -> EventType.LEADER_HEARTBEAT
                    PschdRequest.Action.ELECTION -> EventType.ASK_ELECTION
                    PschdRequest.Action.FOLLOW -> EventType.HEARTBEAT_REC
                    PschdRequest.Action.BALLOT -> EventType.ELECTION_REC
                    else -> return PschdResponse(true)
                }
                return try {
                    val event = PschdEvent(eventType)
                    event.term = request.term
                    event.endpoint = request.endpoint
                    eventBus.publish(event)
                    PschdResponse(true)
                } catch (e: Throwable) {
                    log.error(
                        "on leader heartbeat processor error, errMsg:${e.message}, endpoint:${request.endpoint}, term:${request.term}",
                        e
                    )
                    PschdResponse(PschdResponse.ErrorCode.SYS_ERR, e.message ?: "")
                }
            }
        })
        connection.start()

        eventBus.addHandler(PschdEventHandler())

        setFollowerTimer()
    }

    private fun setCandidateTimer() {
        timer.schedule(
            { eventBus.publish(PschdEvent(EventType.BALLOT_TIMEOUT)) },
            timeout + RandomUtil.random(maxDelay),
            TimeUnit.MILLISECONDS
        )
    }

    private fun setFollowerTimer() {
        timer.schedule(
            { eventBus.publish(PschdEvent(EventType.ELECTION_TIMEOUT)) },
            timeout + RandomUtil.random(maxDelay),
            TimeUnit.MILLISECONDS
        )
    }

    private fun setLeaderTimer() {
        timer.schedule(
            { eventBus.publish(PschdEvent(EventType.LEADER_TIMEOUT)) },
            timeout shr 1,
            TimeUnit.MILLISECONDS
        )
    }

    fun doElectionTimeout() {
        readWriteLock.writeLock().lock()
        try {
            if (this.state != PschdNodeState.FOLLOWER) {
                return
            }
            if (System.currentTimeMillis() - leadTimestamp < timeout) {
                setFollowerTimer()
                return
            }

            this.state = PschdNodeState.CANDIDATE

            startBallot()
        } finally {
            readWriteLock.writeLock().unlock()
        }
    }

    /**
     * call in write lock
     */
    private fun startBallot() {
        if (System.currentTimeMillis() - ballotTimestamp < timeout) {
            return
        }
        term++
        electSelf()
        setCandidateTimer()
    }

    /**
     * call in write lock
     */
    private fun electSelf() {
        ballotCandidate = localEndpoint
        ballotTimestamp = System.currentTimeMillis()
        ballotCandidates = arrayListOf(localEndpoint)
        for (candidate in this.candidateList) {
            if (candidate == localEndpoint) {
                continue
            }
            val request = PschdRequest()
            request.action = PschdRequest.Action.ELECTION
            request.term = this.term
            request.endpoint = this.localEndpoint
            val response = connection.invokeSync(candidate, request)
            if (!response.success) {
                log.warn("request to candidate [$candidate] election failed, errorCode:${response.errorCode}, errMsg:${response.errorMsg}")
            }
        }
    }

    fun doBallotTimeout() {
        readWriteLock.writeLock().lock()
        try {
            if (this.state != PschdNodeState.CANDIDATE) {
                return
            }
            if (System.currentTimeMillis() - ballotTimestamp < timeout) {
                setCandidateTimer()
                return
            }

            startBallot()
        } finally {
            readWriteLock.writeLock().unlock()
        }
    }

    fun sendHeartbeat() {
        readWriteLock.writeLock().lock()
        try {
            for (candidate in this.candidateList) {
                if (candidate == localEndpoint) {
                    continue
                }
                val request = PschdRequest()
                request.action = PschdRequest.Action.HEARTBEAT
                request.term = this.term
                request.endpoint = this.localEndpoint
                val response = connection.invokeSync(candidate, request)
                if (!response.success) {
                    log.warn("request to candidate [$candidate] election failed, errorCode:${response.errorCode}, errMsg:${response.errorMsg}")
                }
            }
            leadTimestamp = System.currentTimeMillis()
            setLeaderTimer()
        } finally {
            readWriteLock.writeLock().unlock()
        }
    }

    private fun onLeaderHeartbeat(e: PschdEvent) {
        readWriteLock.writeLock().lock()
        try {
            if (e.term < term) {
                doEchoHeartbeat(e.endpoint)
                return
            }
            if (e.term == term) {
                if (leader == e.endpoint) {
                    leadTimestamp = System.currentTimeMillis()
                } else {
                    this.state = PschdNodeState.CANDIDATE
                    startBallot()
                }
                doEchoHeartbeat(e.endpoint)
            }
            if (e.term > term) {
                leader = e.endpoint
                term = e.term
                leadTimestamp = System.currentTimeMillis()
                val preState = this.state
                this.state = PschdNodeState.FOLLOWER
                doEchoHeartbeat(e.endpoint)
                setFollowerTimer()
                if (preState == PschdNodeState.LEADER) {
                    doStepDown()
                }
            }
        } finally {
            readWriteLock.writeLock().unlock()
        }
    }

    private fun doEchoHeartbeat(endpoint: PschdEndpoint) {
        log.info("received endpoint $endpoint leader heartbeat")
    }

    /**
     * call in write lock
     */
    private fun doStepDown() {
        log.warn("leader $localEndpoint step down cause new term $term raised or electing new leader $leader")
    }

    private fun onAskElection(e: PschdEvent) {
        readWriteLock.writeLock().lock()
        try {
            if (e.term <= term) {
                return
            }
            term = e.term
            if (state == PschdNodeState.LEADER) {
                doStepDown()
            }
            this.leader = PschdEndpoint.EMPTY
            this.leadTimestamp = System.currentTimeMillis()
            this.ballotTimestamp = System.currentTimeMillis()
            this.ballotCandidate = e.endpoint
            this.state = PschdNodeState.CANDIDATE
            setCandidateTimer()
        } finally {
            readWriteLock.writeLock().unlock()
        }
    }

    private fun onHeartbeatEcho(e: PschdEvent) {
        log.info("endpoint ${e.endpoint} echoed term ${e.term}")
    }

    /**
     *
     */
    private fun receiveVote(e: PschdEvent) {
        readWriteLock.writeLock().lock()
        try {
            if (state != PschdNodeState.CANDIDATE || e.term != term || System.currentTimeMillis() - ballotTimestamp > timeout) {
                return
            }
            ballotCandidates.add(e.endpoint)
            if (ballotCandidates.size > (candidateList.size / 2 + 1)) {
                doStepUp()
            }
        } finally {
            readWriteLock.writeLock().unlock()
        }
    }

    private fun doStepUp() {
        this.state = PschdNodeState.LEADER
        this.leadTimestamp = System.currentTimeMillis()
        sendHeartbeat()
    }


    /**
     * event
     */
    inner class PschdEvent() : Event() {
        var eventType: EventType = EventType.ELECTION_TIMEOUT
        var term = 0L
        var endpoint = PschdEndpoint.EMPTY

        constructor(eventType: EventType) : this() {
            this.eventType = eventType
        }
    }

    enum class EventType {
        /**
         * when follower fail to receive leader's heartbeat
         */
        ELECTION_TIMEOUT,
        /**
         * when candidate fail to receive the most of votes
         */
        BALLOT_TIMEOUT,
        /**
         * when leader need to send heartbeat to followers
         */
        LEADER_TIMEOUT,

        /**
         * when received leader heartbeat
         */
        LEADER_HEARTBEAT,
        /**
         * when received election request
         */
        ASK_ELECTION,
        /**
         * when leader received heartbeat response
         */
        HEARTBEAT_REC,
        /**
         * when candidate receive election response
         */
        ELECTION_REC
    }

    inner class PschdEventHandler : EventHandler<PschdEvent> {
        override fun handle(e: PschdEvent) {
            when (e.eventType) {
                EventType.ELECTION_TIMEOUT -> doElectionTimeout()
                EventType.BALLOT_TIMEOUT -> doBallotTimeout()
                EventType.LEADER_TIMEOUT -> sendHeartbeat()
                EventType.HEARTBEAT_REC -> onHeartbeatEcho(e)
                EventType.ASK_ELECTION -> onAskElection(e)
                EventType.LEADER_HEARTBEAT -> onLeaderHeartbeat(e)
                EventType.ELECTION_REC -> receiveVote(e)
            }
        }

        override fun interest(e: PschdEvent): Boolean {
            return true
        }
    }
}