package wang.nerom.pschd.event

interface EventHandler<T : Event> {
    fun handle(e: T)

    /**
     * can this event be processed by this handler
     * if true then handle, otherwise skip
     * any exception will be take as false
     */
    fun interest(e: T): Boolean
}