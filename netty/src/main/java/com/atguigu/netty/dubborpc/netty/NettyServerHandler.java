package com.atguigu.netty.dubborpc.netty;

import com.atguigu.netty.dubborpc.provider.HelloServiceImpl;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author lixing
 * @date 2022-05-16 10:55
 * @description server端的自定义业务处理器
 */
public class NettyServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 获取客户端发送的消息，并调用服务
        System.out.println("msg="+msg);
        // 客户端在调用服务器的服务时，我们需要定义一个协议。满足协议才能调用服务
        // 比如规定协议：每次发送消息都必须以某个字符串开头 “HelloService#hello”，即消息头部必须带“HelloService#”才能调用服务
        if(msg.toString().startsWith("HelloService#")){
            // 调用服务
            String res = new HelloServiceImpl().hello(msg.toString().substring(msg.toString().lastIndexOf("#") + 1));
            ctx.writeAndFlush(res);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
