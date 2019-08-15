package wang.nerom.pschd.util

import java.net.Inet4Address
import java.net.InetAddress

object HostUtil {
    fun getIpv4(): String {
        return Inet4Address.getLocalHost().hostAddress
    }

    /**
     * Converts IPv4 binary address into a string suitable for presentation.
     *
     * @param src a byte array representing an IPv4 numeric address
     * @return a String representing the IPv4 address in
     * textual representation format
     */
    fun getIpv4(ia: InetAddress): String {
        val src = ia.address
        return "" + (src[0].toInt() and 0xff) + "." + (src[1].toInt() and 0xff) + "." + (src[2].toInt() and 0xff) + "." + (src[3].toInt() and 0xff)
    }
}