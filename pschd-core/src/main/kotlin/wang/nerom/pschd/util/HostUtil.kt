package wang.nerom.pschd.util

import java.net.Inet4Address

class HostUtil {
    companion object {
        fun getIpv4(): String {
            return Inet4Address.getLocalHost().hostAddress
        }
    }
}