package com.atguigu.netty.protocoltcp;


import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

/**
 * @author lixing
 * @date 2022-05-11 17:52
 * @description
 */
public class MyServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new MyMessageDecoder()); // 解码器：处理接收的数据
        pipeline.addLast(new MyMessageEncoder()); // 编码器：处理发送的数据
        pipeline.addLast(new MyServerHandler());
    }
}
