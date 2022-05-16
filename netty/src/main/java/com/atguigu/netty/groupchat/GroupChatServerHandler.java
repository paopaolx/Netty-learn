package com.atguigu.netty.groupchat;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author lixing
 * @date 2022-05-06 17:41
 * @description
 */
public class GroupChatServerHandler extends SimpleChannelInboundHandler<String> {

    // 定义一个channel组，管理所有的channel
    // GlobalEventExecutor.INSTANCE 是一个全局的事件执行器，是一个单例
    private static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    // 表示连接建立，一旦连接，第一个被执行
    // 将当前channel加入到channelGroup中
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        // 将该客户加入聊天的消息推送给其他在线客户端
        // 该方法会将channelGroup中所有的channel遍历，并发送消息
        channelGroup.writeAndFlush(sdf.format(new Date())+" [客户端]"+channel.remoteAddress()+"加入聊天\n");
        channelGroup.add(channel);
    }

    // 表示channel处于一个活动的状态
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(sdf.format(new Date())+" "+ctx.channel().remoteAddress()+"上线了~");
    }

    //  表示channel中有读事件
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        // 获取到当前channel
        Channel channel = ctx.channel();
        // 遍历一下channelGroup，给自己发送的消息和给其他客户端发送的消息不一样
        channelGroup.forEach(ch ->{
            if(ch != channel){ // 不是当前channel，直接转发
                ch.writeAndFlush(sdf.format(new Date())+" [客户]"+channel.remoteAddress()+"发送消息："+ msg + "\n");
            }else{
                ch.writeAndFlush(sdf.format(new Date())+" [自己]发送了消息："+msg+"\n");
            }
        });
    }

    // 表示channel处于一个非活动状态
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(sdf.format(new Date())+" "+ctx.channel().remoteAddress()+"离线了~");
    }

    // 断开连接，将xx客户离开的信息推送给当前在线的客户
    // 触发此方法后，会自动将断开连接的channel从channelGroup中移除
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        channelGroup.writeAndFlush(sdf.format(new Date())+" [客户端]"+channel.remoteAddress()+"离开了\n");
        System.out.println("channelGroup size = "+channelGroup.size());
    }


    // 发生异常，关闭通道
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
