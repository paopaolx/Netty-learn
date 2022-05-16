package com.atguigu.netty.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @author lixing
 * @date 2022-05-11 18:06
 * @description
 */
public class MyServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private int count;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        byte[] buffer = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(buffer);
        // 将buffer转成字符串
        String msg = new String(buffer, StandardCharsets.UTF_8);
        System.out.println("服务端接收数据：" + msg);
        System.out.println("服务器接收到的消息量：" + (++this.count));

        // 服务器回送数据给客户端，回送一个随机id
        ByteBuf sendMsg = Unpooled.copiedBuffer(UUID.randomUUID().toString()+" ", StandardCharsets.UTF_8);
        ctx.writeAndFlush(sendMsg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
