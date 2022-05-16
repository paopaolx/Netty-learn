package com.atguigu.netty.protocoltcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

/**
 * @author lixing
 * @date 2022-05-12 10:06
 * @description ReplayingDecoder 是 byte-to-message 解码的一种特殊的抽象基类，读取缓冲区的数据之前需要检查缓冲区是否有足够的字节。
 * 使用ReplayingDecoder就无需自己检查，若ByteBuf中有足够的字节，则会正常读取；若没有足够的字节则会停止解码。
 */
public class MyMessageDecoder extends ReplayingDecoder<Void> {
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        System.out.println("MyMessageDecoder decode方法被调用");
        // 需要将得到的二进制字节码 => MessageProtocol数据包（对象）
        int len = byteBuf.readInt();
        byte[] content = new byte[len];
        byteBuf.readBytes(content);
        // 封装成MessageProtocol数据包，放入list中，传递给下一个handler进行业务处理
        MessageProtocol messageProtocol = new MessageProtocol();
        messageProtocol.setLen(len);
        messageProtocol.setContent(content);
        list.add(messageProtocol);
    }
}
