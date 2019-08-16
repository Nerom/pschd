package wang.nerom.pschd.event

import com.alipay.remoting.NamedThreadFactory
import com.lmax.disruptor.EventFactory
import com.lmax.disruptor.dsl.Disruptor
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

class DisruptorEventBus<T : Event> : EventBus<T> {
    private val log = LoggerFactory.getLogger(javaClass)
    private val handlerList = CopyOnWriteArrayList<EventHandler<T>>()

    override fun publish(e: T) {
        val seq = disruptor.ringBuffer.next()
        try {
            val pe = disruptor.ringBuffer[seq]
            pe.event = e
        } finally {
            disruptor.ringBuffer.publish(seq)
        }
    }

    @Synchronized
    override fun addHandler(eh: EventHandler<T>) {
        if (handlerList.contains(eh)) {
            return
        }
        handlerList.add(eh)
    }

    private val disruptor: Disruptor<PackedEvent<T?>>

    init {
        val eventFactory = EventFactory<PackedEvent<T?>> { PackedEvent(null) }
        this.disruptor = Disruptor(
            eventFactory, 16, Executors.newFixedThreadPool(4, NamedThreadFactory("event-bus"))
        )

        disruptor.handleEventsWith(object : com.lmax.disruptor.EventHandler<PackedEvent<T?>> {
            override fun onEvent(event: PackedEvent<T?>, sequence: Long, endOfBatch: Boolean) {
                if (event.event == null) {
                    return
                }
                val realEvent = event.event

                for (handler in handlerList) {
                    if (match(realEvent!!, handler)) {
                        return try {
                            handler.handle(realEvent)
                        } catch (e: Throwable) {
                            log.error(
                                "matched handler [$handler] process event [$realEvent] error, errorMsg:${e.message}", e
                            )
                        }
                    }
                }
                log.warn("none matched handler for $event")
            }
        })

        disruptor.start()
    }

    private fun match(realEvent: T, handler: EventHandler<T>): Boolean {
        return try {
            handler.interest(realEvent)
        } catch (e: Throwable) {
            log.error("try match handler [$handler] for event [$realEvent] error, errorMsg:${e.message}", e)
            false
        }
    }

    private class PackedEvent<T : Event?>(var event: T)
}