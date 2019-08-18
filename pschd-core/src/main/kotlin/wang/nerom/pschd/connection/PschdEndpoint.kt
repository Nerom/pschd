package wang.nerom.pschd.connection

import java.io.Serializable

class PschdEndpoint(
    val ipv4: String,
    val port: Int
) : Serializable {
    val isEmpty = this == EMPTY

    companion object {
        val EMPTY = PschdEndpoint("", 0)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PschdEndpoint

        if (ipv4 != other.ipv4) return false
        if (port != other.port) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ipv4.hashCode()
        result = 31 * result + port
        return result
    }

    override fun toString(): String {
        return "PschdEndpoint(ipv4='$ipv4', port=$port)"
    }
}