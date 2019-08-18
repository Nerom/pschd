package wang.nerom.pschd.connection

import com.alipay.remoting.Url
import com.alipay.remoting.config.Configs
import com.alipay.remoting.rpc.RpcClient
import com.alipay.remoting.rpc.RpcConfigs
import com.alipay.remoting.rpc.RpcServer
import com.alipay.remoting.rpc.protocol.RpcProtocol
import com.alipay.remoting.rpc.protocol.RpcProtocolV2
import com.alipay.remoting.rpc.protocol.UserProcessor
import com.alipay.remoting.util.StringUtils
import org.slf4j.LoggerFactory
import wang.nerom.pschd.util.HostUtil

class PschdConnection {
    private val log = LoggerFactory.getLogger(javaClass)
    private var manageConnection = true
    private var localEndpoint: PschdEndpoint
    private var server: RpcServer
    private var client: RpcClient

    constructor(localPort: Int) {
        localEndpoint = PschdEndpoint(HostUtil.getIpv4(), localPort)
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
            val url = buildUrl(endpoint)
            client.invokeSync(url, message, 1000)
            PschdResponse(true)
        } catch (e: Throwable) {
            log.error("connection to $endpoint error, errMsg:${e.message}", e)
            PschdResponse(PschdResponse.ErrorCode.SYS_ERR, "connection error")
        }
    }

    private fun buildUrl(endpoint: PschdEndpoint): Url {
        val url = Url(endpoint.ipv4, endpoint.port)
        val connTimeoutStr = url.getProperty(RpcConfigs.CONNECT_TIMEOUT_KEY)
        var connTimeout = Configs.DEFAULT_CONNECT_TIMEOUT
        if (StringUtils.isNotBlank(connTimeoutStr)) {
            if (StringUtils.isNumeric(connTimeoutStr)) {
                connTimeout = Integer.parseInt(connTimeoutStr)
            } else {
                throw IllegalArgumentException(
                    "Url args illegal value of key [" + RpcConfigs.CONNECT_TIMEOUT_KEY
                            + "] must be positive integer! The origin url is ["
                            + url.originUrl + "]"
                )
            }
        }
        url.connectTimeout = connTimeout

        val protocolStr = url.getProperty(RpcConfigs.URL_PROTOCOL)
        var protocol = RpcProtocol.PROTOCOL_CODE
        if (StringUtils.isNotBlank(protocolStr)) {
            if (StringUtils.isNumeric(protocolStr)) {
                protocol = java.lang.Byte.parseByte(protocolStr)
            } else {
                throw IllegalArgumentException(
                    "Url args illegal value of key [" + RpcConfigs.URL_PROTOCOL
                            + "] must be positive integer! The origin url is ["
                            + url.originUrl + "]"
                )
            }
        }
        url.protocol = protocol

        val versionStr = url.getProperty(RpcConfigs.URL_VERSION)
        var version = RpcProtocolV2.PROTOCOL_VERSION_1
        if (StringUtils.isNotBlank(versionStr)) {
            if (StringUtils.isNumeric(versionStr)) {
                version = java.lang.Byte.parseByte(versionStr)
            } else {
                throw IllegalArgumentException(
                    "Url args illegal value of key [" + RpcConfigs.URL_VERSION
                            + "] must be positive integer! The origin url is ["
                            + url.originUrl + "]"
                )
            }
        }
        url.version = version

        val connNumStr = url.getProperty(RpcConfigs.CONNECTION_NUM_KEY)
        var connNum = Configs.DEFAULT_CONN_NUM_PER_URL
        if (StringUtils.isNotBlank(connNumStr)) {
            if (StringUtils.isNumeric(connNumStr)) {
                connNum = Integer.parseInt(connNumStr)
            } else {
                throw IllegalArgumentException(
                    "Url args illegal value of key [" + RpcConfigs.CONNECTION_NUM_KEY
                            + "] must be positive integer! The origin url is ["
                            + url.originUrl + "]"
                )
            }
        }
        url.connNum = connNum

        val connWarmupStr = url.getProperty(RpcConfigs.CONNECTION_WARMUP_KEY)
        var connWarmup = false
        if (StringUtils.isNotBlank(connWarmupStr)) {
            connWarmup = java.lang.Boolean.parseBoolean(connWarmupStr)
        }
        url.isConnWarmup = connWarmup

        return url
    }
}