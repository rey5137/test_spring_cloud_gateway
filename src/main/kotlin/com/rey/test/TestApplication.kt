package com.rey.test

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelOption
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.util.CharsetUtil
import io.netty.util.NettyRuntime
import io.netty.util.internal.SystemPropertyUtil
import reactor.core.publisher.Mono
import reactor.netty.http.server.HttpServer
import reactor.netty.resources.LoopResources


class TestApplication

fun main(args: Array<String>) {
    val nThreads = Math.max(1, SystemPropertyUtil.getInt(
        "io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2))
    val loop = LoopResources.create("test", nThreads, nThreads, true)
    try {
        val disposableServer = HttpServer.create()
            .port(8081)
            .tcpConfiguration {
                it.selectorOption(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    // Specifies: bossGroup, workerGroup, NIO transport
                    .runOn(loop, false)
            }
            .handle { req, res ->
                req.receive()
                    // Aggregates the incoming data
                    .aggregate()
                    .then(res.header(HttpHeaderNames.CONTENT_TYPE, "text/html")
                        .header(HttpHeaderNames.CONTENT_LENGTH, "12")
                        // Sending Mono will send FullHttpResponse
                        .send(Mono.just(Unpooled.copiedBuffer("Hello World!", CharsetUtil.UTF_8)))
                        .then())
            }
            .bindNow()
        disposableServer.onDispose().block()
    } finally {
        loop.disposeLater().block()
    }
}