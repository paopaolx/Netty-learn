package com.atguigu.netty.sample;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

/**
 * @author lixing
 * @date 2022-04-29 17:24
 * @description
 */
public class NettyClientHandler extends ChannelInboundHandlerAdapter {
    // 当通道就绪时，会触发
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client ctx="+ctx);
        ctx.writeAndFlush(Unpooled.copiedBuffer("hello，Server 😵 喵", CharsetUtil.UTF_8));
    }

    // 当通道有读取事件时，会触发
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        System.out.println("收到服务器 "+ctx.channel().remoteAddress()+" 回复的消息；"+buf.toString(CharsetUtil.UTF_8));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
