package wang.nerom.pschd.test

import org.junit.Test
import wang.nerom.pschd.connection.ConnectionConfig
import wang.nerom.pschd.connection.Endpoint
import wang.nerom.pschd.connection.PaxosConnection

class ConnectionTest {
    @Test
    fun ipv4Test() {
        println("localIpv4: [${ConnectionConfig.ipv4}]")
        assert(ConnectionConfig.ipv4.isNotEmpty())
    }

    @Test
    fun connectionTest() {
        val local = Endpoint(ConnectionConfig.ipv4, ConnectionConfig.localPort)
        val server = PaxosConnection(listOf(local))
        val s = Thread { server.serve() }
        s.start()
        val c = Thread { server.connect() }
        c.start()
        s.join()
        c.join()
    }
}