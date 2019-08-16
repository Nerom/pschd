package wang.nerom.pschd.connection

import com.alipay.remoting.Url
import com.alipay.remoting.rpc.RpcClient
import com.alipay.remoting.rpc.RpcServer
import com.alipay.remoting.rpc.protocol.UserProcessor
import org.slf4j.LoggerFactory
import wang.nerom.pschd.util.HostUtil

class PschdConnection {
    private val log = LoggerFactory.getLogger(javaClass)
    private var manageConnection = true
    private var localEndpoint: PschdEndpoint
    private var server: RpcServer
    private var client: RpcClient

    constructor(localPort: Int) {
        localEndpoint = PschdEndpoint(localPort, HostUtil.getIpv4())
        server = RpcServer(localEndpoint.port, manageConnection)
        client = RpcClient()
    }

    fun registerProcessor(processor: UserProcessor<PschdRequest>) {
        server.registerUserProcessor(processor)
    }

    fun start() {
        this.server.start()
        this.client.init()
    }

    fun invokeSync(endpoint: PschdEndpoint, message: PschdRequest): PschdResponse {
        return try {
            val url = Url(endpoint.ipv4, endpoint.port)
            url.connNum = 1
            client.invokeSync(url, message, 1000)
            PschdResponse(true)
        } catch (e: Throwable) {
            log.error("connection to $endpoint error, errMsg:${e.message}", e)
            PschdResponse(PschdResponse.ErrorCode.SYS_ERR, "connection error")
        }
    }
}