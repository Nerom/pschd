package wang.nerom.pschd.connection

class PschdEndpoint(
    val port: Int,
    val ipv4: String
) {
    val isEmpty = this == EMPTY

    companion object {
        val EMPTY = PschdEndpoint(0, "")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PschdEndpoint

        if (port != other.port) return false
        if (ipv4 != other.ipv4) return false

        return true
    }

    override fun hashCode(): Int {
        var result = port
        result = 31 * result + ipv4.hashCode()
        return result
    }
}