package com.atguigu.netty.sample;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * @author lixing
 * @date 2022-04-29 15:38
 * @description 自定义一个处理器，需要继承netty规定好的某个HandlerAdapter
 */
public class NettyServerHandler extends ChannelInboundHandlerAdapter {

    // group就充当了业务线程池，可以将任务提交到该线程池中
    static final EventExecutorGroup group = new DefaultEventExecutorGroup(16);

    // 读事件（读取客户端发送的数据）
    // ctx 上下文对象，含有 管道pipeline，一个管道里会有很多个业务处理的handler，通道channel，地址
    // msg 客户端发送的数据，是Object格式的
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//        System.out.println("server ctx="+ctx);
//        System.out.println("看看channel与pipeline的关系"); // channel与pipeline是相互包含的关系，ctx中包含channel和pipeline，以及其他的信息（大部分的信息都囊括在ctx中）
//        Channel channel = ctx.channel();
//        ChannelPipeline pipeline = ctx.pipeline(); // 本质是一个双向链表
//        // 将msg转成一个ByteBuf
//        ByteBuf buf = (ByteBuf) msg;
//        System.out.println("接收到客户端 "+ctx.channel().remoteAddress()+" 发送的消息："+buf.toString(CharsetUtil.UTF_8));


        System.out.println("NettyServerHandler 线程是 = "+Thread.currentThread().getName());

        // 执行耗时任务
        group.submit(()->{
            try {
                Thread.sleep(5*1000);
                System.out.println("NettyServerHandler execute1 线程是 = "+Thread.currentThread().getName());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        group.submit(()->{
            try {
                Thread.sleep(5*1000);
                System.out.println("NettyServerHandler execute2 线程是 = "+Thread.currentThread().getName());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        System.out.println("go on");
    }

    // 读事件完毕，发送数据回复给客户端
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        // write + flush 将数据写入到缓存，并刷新
        ctx.writeAndFlush(Unpooled.copiedBuffer("hello，客户端 😵 喵", CharsetUtil.UTF_8));
    }

    // 发生异常，则关闭通道
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
