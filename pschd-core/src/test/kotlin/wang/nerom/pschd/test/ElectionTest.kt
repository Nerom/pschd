package wang.nerom.pschd.test

import org.junit.Test
import wang.nerom.pschd.config.PschdConfig
import wang.nerom.pschd.leader.PschdNode
import wang.nerom.pschd.util.HostUtil

class ElectionTest {
    @Test
    fun electionTest() {
        val localIpv4 = HostUtil.getIpv4()
        println("local ipv4: [$localIpv4]")
        val candidates = "$localIpv4:9101,$localIpv4:9102,$localIpv4:9103"

        val config1 = PschdConfig()
        config1.localPort = 9101
        config1.candidates = candidates
        val node1 = PschdNode(config1)
        node1.doInit()

        val config2 = PschdConfig()
        config2.localPort = 9102
        config2.candidates = candidates
        val node2 = PschdNode(config2)
        node2.doInit()

        val config3 = PschdConfig()
        config3.localPort = 9103
        config3.candidates = candidates
        val node3 = PschdNode(config3)
        node3.doInit()

        while (true) {
            Thread.sleep(10000)
        }
    }

    @Test
    fun electionNode1Test() {
        val localIpv4 = HostUtil.getIpv4()
        println("local ipv4: [$localIpv4]")
        val candidates = "$localIpv4:9101,$localIpv4:9102,$localIpv4:9103"

        val config1 = PschdConfig()
        config1.localPort = 9101
        config1.candidates = candidates
        val node1 = PschdNode(config1)
        node1.doInit()

        while (true) {
            Thread.sleep(10000)
        }
    }

    @Test
    fun electionNode2Test() {
        val localIpv4 = HostUtil.getIpv4()
        println("local ipv4: [$localIpv4]")
        val candidates = "$localIpv4:9101,$localIpv4:9102,$localIpv4:9103"

        val config2 = PschdConfig()
        config2.localPort = 9102
        config2.candidates = candidates
        val node2 = PschdNode(config2)
        node2.doInit()

        while (true) {
            Thread.sleep(10000)
        }
    }

    @Test
    fun electionNode3Test() {
        val localIpv4 = HostUtil.getIpv4()
        println("local ipv4: [$localIpv4]")
        val candidates = "$localIpv4:9101,$localIpv4:9102,$localIpv4:9103"

        val config3 = PschdConfig()
        config3.localPort = 9103
        config3.candidates = candidates
        val node3 = PschdNode(config3)
        node3.doInit()

        while (true) {
            Thread.sleep(10000)
        }
    }
}