package wang.nerom.pschd.connection

import wang.nerom.pschd.util.HostUtil

class PschdEndpoint {
    val port: Int
    val hostName: String?
    val ipv4: String?
    val ipv6: String?

    constructor(port: Int) {
        this.port = port
        ipv4 = HostUtil.getIpv4()
        hostName = null
        ipv6 = null
    }
}