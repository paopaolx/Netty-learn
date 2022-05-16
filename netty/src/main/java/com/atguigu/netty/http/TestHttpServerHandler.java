package com.atguigu.netty.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.net.URI;

/**
 * @author lixing
 * @date 2022-05-05 16:26
 * @description
 */
public class TestHttpServerHandler extends SimpleChannelInboundHandler<HttpObject> {
    // 读取客户端数据
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        // 判断msg是不是httpRequest请求
        if(msg instanceof HttpRequest){
            HttpRequest httpRequest = (HttpRequest) msg;
            // 获取uri
            URI uri = new URI(httpRequest.uri());
            if("/favicon.ico".equals(uri.getPath())){
                System.out.println("请求了 favicon.ico，不做响应");
                return;
            }
            System.out.println("msg类型="+msg.getClass());
            System.out.println("客户端地址="+ctx.channel().remoteAddress());
            // 回复信息给浏览器 [http协议]
            ByteBuf content = Unpooled.copiedBuffer("hello，我是服务器", CharsetUtil.UTF_8);
            // 构造一个http响应，即httpResponse
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain;charset=utf-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
            // 将构建好的response返回
            ctx.writeAndFlush(response);
        }
    }
}
