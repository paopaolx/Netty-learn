package com.atguigu.netty.protocoltcp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @author lixing
 * @date 2022-05-11 18:06
 * @description
 */
public class MyServerHandler extends SimpleChannelInboundHandler<MessageProtocol> {

    private int count;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageProtocol msg) throws Exception {
        // 接收到数据，并处理
        int len = msg.getLen();
        byte[] content = msg.getContent();
        System.out.println("服务端接收到信息如下：");
        System.out.println("长度="+len);
        System.out.println("内容="+new String(content, StandardCharsets.UTF_8));
        System.out.println("服务器接收到消息包数量="+(++this.count));

        // 服务端回复消息给客户端（为什么客户端会收到10个回复包，因为服务器这边每收到1个数据包，就会回复一个。对于一个完整数据包的判定，就是依赖自定义的MessageProtocol）
        String responseContent = UUID.randomUUID().toString();
        int responseLen = responseContent.getBytes(StandardCharsets.UTF_8).length;
        MessageProtocol messageProtocol = new MessageProtocol();
        messageProtocol.setLen(responseLen);
        messageProtocol.setContent(responseContent.getBytes(StandardCharsets.UTF_8));
        ctx.writeAndFlush(messageProtocol);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(cause.getMessage());
        ctx.close();
    }
}
