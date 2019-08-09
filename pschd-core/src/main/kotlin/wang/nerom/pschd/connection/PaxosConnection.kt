package wang.nerom.pschd.connection

import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import java.io.UnsupportedEncodingException


/**
 * connection manager
 */
class PaxosConnection {
    val localType: EndpointType
    val localEndpoint: Endpoint
    val candidateEndpoints: List<Endpoint>
    val candidateCount get() = candidateEndpoints.size

    constructor(candidateEndpoints: List<Endpoint>) {
        this.candidateEndpoints = candidateEndpoints
        this.localEndpoint = Endpoint(ConnectionConfig.ipv4, ConnectionConfig.localPort)
        this.localType = decideLocalType(localEndpoint, candidateEndpoints)
    }

    private fun decideLocalType(localEndpoint: Endpoint, candidateEndpoints: List<Endpoint>): EndpointType {
        return if (candidateEndpoints.any(localEndpoint::equals)) {
            EndpointType.CANDIDATE
        } else {
            EndpointType.WORKER
        }
    }

    fun serve() {
        val bossGroup = NioEventLoopGroup() // (1)
        val workerGroup = NioEventLoopGroup()
        try {
            val b = ServerBootstrap() // (2)
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java) // (3)
                .childHandler(object : ChannelInitializer<SocketChannel>() { // (4)
                    @Throws(Exception::class)
                    public override fun initChannel(ch: SocketChannel) {
                        ch.pipeline().addLast(ConnectionHandler())
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                .childOption(ChannelOption.SO_KEEPALIVE, true) // (6)

            // Bind and start to accept incoming connections.
            val f = b.bind(localEndpoint.port).sync() // (7)

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync()
        } finally {
            workerGroup.shutdownGracefully()
            bossGroup.shutdownGracefully()
        }
    }

    fun connect() {
        val no = ConnectionConfig.ipv4.hashCode() % candidateCount
        val candidateEndpoint = candidateEndpoints[no]
        val eventLoopGroup = NioEventLoopGroup()

        try {

            val bootstrap = Bootstrap()
            bootstrap.channel(NioSocketChannel::class.java)
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true)
            bootstrap.group(eventLoopGroup)
            bootstrap.remoteAddress(candidateEndpoint.ipv4, candidateEndpoint.port)
            bootstrap.handler(object : ChannelInitializer<SocketChannel>() {
                @Throws(Exception::class)
                override fun initChannel(socketChannel: SocketChannel) {
                    socketChannel.pipeline().addLast(NettyClientHandler())
                }
            })
            val channelFuture = bootstrap.connect(candidateEndpoint.ipv4, candidateEndpoint.port).sync()
            if (channelFuture.isSuccess) {
                System.err.println("连接服务器成功")
            }
            channelFuture.channel().closeFuture().sync()
        } finally {
            eventLoopGroup.shutdownGracefully()
        }
    }

    class NettyClientHandler : ChannelInboundHandlerAdapter() {
        @ExperimentalStdlibApi
        override fun channelActive(ctx: ChannelHandlerContext) {
            val data = "你好，服务器".encodeToByteArray()
            var firstMessage = Unpooled.buffer()
            firstMessage.writeBytes(data)
            ctx.writeAndFlush(firstMessage);
            System.err.println("客户端发送消息:你好，服务器");
        }

         override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            val buf = msg as ByteBuf
            val rev = getMessage(buf)
            System.err.println("客户端收到服务器消息:" + rev);
        }

        fun getMessage(buf: ByteBuf): String {
            val con = ByteArray(buf.readableBytes())
            buf.readBytes(con);
            return try {
                String(con, charset("UTF8"));
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace();
                ""
            }
        }
    }

}

/**
 * connection handler
 */
class ConnectionHandler : ChannelInboundHandlerAdapter() {
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {

        val buf = msg as ByteBuf

        val recieved = getMessage(buf)
        System.err.println("服务器接收到客户端消息：" + recieved!!)

        try {
            ctx.writeAndFlush(getSendByteBuf("你好，客户端"))
            System.err.println("服务器回复消息：你好，客户端")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

    }


    private fun getMessage(buf: ByteBuf): String? {

        val con = ByteArray(buf.readableBytes())
        buf.readBytes(con)
        return try {
            String(con, charset("UTF-8"))
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            null
        }

    }

    private fun getSendByteBuf(message: String): ByteBuf {
        val req = message.toByteArray(charset("UTF-8"))
        val pingMessage = Unpooled.buffer()
        pingMessage.writeBytes(req)

        return pingMessage
    }
}

/**
 * connection endpoint info
 */
data class Endpoint(val ipv4: String, val port: Int)

/**
 * endpoint type
 */
enum class EndpointType {
    CANDIDATE, FOLLOWER, WORKER
}