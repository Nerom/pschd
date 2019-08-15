package wang.nerom.pschd.event

class PschdEvent : Event() {
    var eventType: EventType = EventType.ELECTION_TIMEOUT

    enum class EventType {
        ELECTION_TIMEOUT, BALLOT_TIMEOUT, LEADER_TIMEOUT
    }
}