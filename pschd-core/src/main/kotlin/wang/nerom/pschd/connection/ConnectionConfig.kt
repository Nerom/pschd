package wang.nerom.pschd.connection

import wang.nerom.pschd.util.HostUtil

object ConnectionConfig {
    val ipv4: String = HostUtil.getIpv4()
    var localPort = 9111
        private set

    init {
        // TODO parse port
    }
}