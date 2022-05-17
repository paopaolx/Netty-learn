package com.atguigu.netty.dubborpc.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.Callable;

/**
 * @author lixing
 * @date 2022-05-16 11:07
 * @description
 */
public class NettyClientHandler extends ChannelInboundHandlerAdapter implements Callable {

    private ChannelHandlerContext context; // 上下文
    private String result; // 返回的结果
    private String params; // 客户端调用方法时，传入的参数

    // 与服务器的连接创建成功后，就会被调用
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("调用次序1");
        context = ctx;
    }

    // 收到数据后，调用的方法
    @Override
    public synchronized void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		System.out.println("调用次序4");
        result = msg.toString();
        // 唤醒等待的线程
        notify();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

    // 被代理对象调用，发送数据给服务器 -> wait -> 等待被唤醒（被channelRead唤醒） -> 返回结果
    @Override
    public synchronized Object call() throws Exception {
		System.out.println("调用次序3");
        // 客户端发送给服务端的消息
        context.writeAndFlush(params);
        // 进入等待，等待channelRead方法获取到服务器返回结果后，唤醒
        // 客户端发送调用远程服务的参数，等待调用到远程服务，并获取到返回结果，才进行后续操作，所以需要等待
        wait();
		System.out.println("调用次序5");
        return result;
    }

    void setParams(String params){
		System.out.println("调用次序2");
        this.params = params;
    }
}
