package wang.nerom.pschd.connection

import com.alipay.remoting.rpc.RpcServer

class PschdConnection {
    private var manageConnection = true
    private var localPort = 9101
    private var server = RpcServer(localPort, manageConnection)

    constructor() {
        parseConfig()
        init()
    }

    private fun parseConfig() {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun init() {
        this.server.start()
    }

}