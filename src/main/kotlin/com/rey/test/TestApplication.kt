package com.rey.test

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil


class TestApplication {

    @Throws(Exception::class)
    fun run() {
        val bossGroup: EventLoopGroup = NioEventLoopGroup()
        val workerGroup: EventLoopGroup = NioEventLoopGroup()
        try {
            val httpBootstrap = ServerBootstrap()
            httpBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(ServerInitializer())
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
            val httpChannel: ChannelFuture = httpBootstrap.bind(8081).sync()
            httpChannel.channel().closeFuture().sync()
        } finally {
            workerGroup.shutdownGracefully()
            bossGroup.shutdownGracefully()
        }
    }
}

class ServerInitializer : ChannelInitializer<Channel>() {
    override fun initChannel(ch: Channel) {
        val pipeline: ChannelPipeline = ch.pipeline()
        pipeline.addLast(HttpServerCodec())
        pipeline.addLast(HttpObjectAggregator(Int.MAX_VALUE))
        pipeline.addLast(ServerHandler())
    }
}

class ServerHandler : SimpleChannelInboundHandler<FullHttpRequest?>() {
    override fun channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest?) {
        val content = Unpooled.copiedBuffer("Hello World!", CharsetUtil.UTF_8)
        val response: FullHttpResponse = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content)
        response.headers()[HttpHeaderNames.CONTENT_TYPE] = "text/html"
        response.headers()[HttpHeaderNames.CONTENT_LENGTH] = content.readableBytes()
        ctx.write(response)
        ctx.flush()
    }
}

fun main(args: Array<String>) {
    TestApplication().run()
}