package wang.nerom.pschd.connection

import com.alipay.remoting.Url
import com.alipay.remoting.rpc.RpcServer
import com.alipay.remoting.rpc.protocol.UserProcessor
import wang.nerom.pschd.util.HostUtil

class PschdConnection {
    private var manageConnection = true
    private var localEndpoint = PschdEndpoint(9101, HostUtil.getIpv4())
    private var server = RpcServer(localEndpoint.port, manageConnection)

    constructor() {
        parseConfig()
        init()
    }

    private fun parseConfig() {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun registerProcessor(processor: UserProcessor<PschdRequest>){
        // TODO
    }

    private fun init() {
        this.server.start()
    }

    fun invokeSync(endpoint: PschdEndpoint, message: PschdRequest): PschdResponse {
        val url = Url(endpoint.ipv4, endpoint.port)
        return server.invokeSync(url, message, 1000) as PschdResponse
    }
}