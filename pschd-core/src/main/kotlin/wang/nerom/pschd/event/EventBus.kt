package wang.nerom.pschd.event

interface EventBus<T : Event> {
    fun publish(e: T)
    fun addHandler(eh: EventHandler<T>)
}