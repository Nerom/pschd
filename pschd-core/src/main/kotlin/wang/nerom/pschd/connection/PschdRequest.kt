package wang.nerom.pschd.connection

import java.io.Serializable

class PschdRequest : Serializable {
    var action: Action = Action.ELECTION
    var endpoint = PschdEndpoint.EMPTY
    var term = 0L

    enum class Action {
        HEARTBEAT, ELECTION, BALLOT, FOLLOW
    }
}