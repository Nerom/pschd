package wang.nerom.pschd.leader

import com.alipay.remoting.NamedThreadFactory
import wang.nerom.pschd.event.DisruptorEventBus
import wang.nerom.pschd.event.Event
import wang.nerom.pschd.util.RandomUtil
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock

class PschdLeaderElection {
    val eventBus = DisruptorEventBus<Event>()
    val readWriteLock = ReentrantReadWriteLock()
    val timer = Executors.newScheduledThreadPool(5, NamedThreadFactory("election-timer"))
    var state = PschdNodeState.FOLLOWER
    var term = 0L
    var leader = ""
    var leadTimestamp = 0L
    var candidateList = arrayListOf<String>()
    /**
     * in millisecond
     */
    var timeout = 1000L
    var maxDelay = 500L

    fun doInit() {
        timer.schedule({ doElectionTimeout() }, timeout + RandomUtil.random(maxDelay), TimeUnit.MILLISECONDS)
    }

    fun doElectionTimeout() {
        readWriteLock.writeLock().lock()
        try {
            this.state = PschdNodeState.CANDIDATE
        } finally {
            readWriteLock.writeLock().unlock()
        }
    }
}