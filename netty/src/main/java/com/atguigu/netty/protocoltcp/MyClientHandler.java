package com.atguigu.netty.protocoltcp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.nio.charset.StandardCharsets;

/**
 * @author lixing
 * @date 2022-05-11 17:58
 * @description
 */
public class MyClientHandler extends SimpleChannelInboundHandler<MessageProtocol> {
    private int count;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 使用客户端发送5条数据
        for (int i = 0; i < 5; i++) {
            String msgTo = "今天天气冷，吃火锅";
            byte[] content = msgTo.getBytes(StandardCharsets.UTF_8);
            int length = msgTo.getBytes(StandardCharsets.UTF_8).length; // 获取待发送数据的长度
            // 创建协议包对象
            MessageProtocol messageProtocol = new MessageProtocol();
            messageProtocol.setContent(content);
            messageProtocol.setLen(length);
            ctx.writeAndFlush(messageProtocol);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageProtocol msg) throws Exception {
        int len = msg.getLen();
        byte[] content = msg.getContent();

        System.out.println("客户端接收到信息如下：");
        System.out.println("长度="+len);
        System.out.println("内容="+new String(content, StandardCharsets.UTF_8));
        System.out.println("客户端接收到消息包数量="+(++this.count));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("异常消息：" + cause.getMessage());
        ctx.close();
    }
}
