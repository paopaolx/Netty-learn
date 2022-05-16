package com.atguigu.netty.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * @author lixing
 * @date 2022-05-05 16:27
 * @description
 */
public class TestServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        // 向管道中加入处理器
        // 得到管道
        ChannelPipeline pipeline = socketChannel.pipeline();
        // 加入一个netty提供的httpServerCodec codec => [coder - decoder]  http编解码器
        // 1. httpServerCodec 是netty提供的处理http的编解码器
        pipeline.addLast("MyHttpServerCodec", new HttpServerCodec());
        // 2. 增加一个自定义的handler
        pipeline.addLast("MyTestHttpServerHandler", new TestHttpServerHandler());
    }
}
