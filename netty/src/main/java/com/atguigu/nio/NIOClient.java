package com.atguigu.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author lixing
 * @date 2022-04-26 18:25
 * @description NIO客户端
 */
public class NIOClient {
    public static void main(String[] args) throws IOException {
        // 客户端channel
        SocketChannel socketChannel = SocketChannel.open();
        // 设置客户端channel为非阻塞
        socketChannel.configureBlocking(false);
        // 获取服务端的连接ip和port
        InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 6666);
        // 与服务端建立连接
        if(!socketChannel.connect(inetSocketAddress)){ // 连接需要时间，客户端不会阻塞
            if(!socketChannel.finishConnect()){ // 如果连接失败，可以做其他的操作
                System.out.println("因为连接需要时间，客户端不会阻塞，可以做其他工作...");
            }
        }
        // 如果连接成功，则通过客户端channel向服务端发送数据
        String str = "hello，尚硅谷~";
        // 将buffer中数据写入到客户端channel中
        socketChannel.write(ByteBuffer.wrap(str.getBytes()));
        System.in.read();
    }
}
