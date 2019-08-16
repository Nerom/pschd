package wang.nerom.pschd.connection

class PschdRequest {
    var action: Action = Action.ELECTION
    var endpoint = PschdEndpoint.EMPTY
    var term = 0L

    enum class Action {
        HEARTBEAT, ELECTION, BALLOT, FOLLOW
    }
}