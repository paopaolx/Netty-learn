package com.atguigu.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

/**
 * @author lixing
 * @date 2022-04-26 18:01
 * @description NIO客户端
 */
public class NIOServer {
    public static void main(String[] args) throws IOException {
        // 服务端channel
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        // 服务端channel设置成非阻塞
        serverSocketChannel.configureBlocking(false);
        // 服务端channel绑定网络监听端口
        serverSocketChannel.socket().bind(new InetSocketAddress(6666));
        // 通过Selector的open方法获取到一个Selector实例
        Selector selector = Selector.open();
        // 将服务端channel注册到selector上，关注客户端连接事件
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        // 循环监听客户端连接
        while(true){
            // 通过selector的select(long timeout)方法，设置超时1s去检测是否有客户端通道事件发生
            if(selector.select(1000) == 0){ // 如果等待1s后还是没有任何事件发生，则打印（非阻塞，程序可以做其他事情）
                System.out.println("服务器等待1秒，暂无客户端连接...");
                continue;
            }
            // 如果select方法返回值大于0，表示有事件发生。先获取到所有有事件的selectionKeys集合
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> selectionKeyIterator = selectionKeys.iterator();
            // 迭代遍历key，通过key的操作类型进行不同的处理（accept，read）
            while(selectionKeyIterator.hasNext()){
                SelectionKey key = selectionKeyIterator.next();
                if(key.isAcceptable()){ // 如果channel上发生的是客户端连接成功的事件
                    // 获取到对应的客户端channel
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    // 设置客户端channel为非阻塞
                    socketChannel.configureBlocking(false);
                    System.out.println("客户端连接成功，客户端channel："+socketChannel.hashCode());
                    // 将客户端channel注册到selector上，关注客户端channel读的事件
                    socketChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));
                }
                if(key.isReadable()){ // 如果channel上发生的是数据读的事件（也就是客户端向服务端发送数据了）
                    // 获取到发生事件的客户端channel
                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    ByteBuffer buffer = (ByteBuffer) key.attachment();
                    // 将客户端channel中的数据读出到buffer
                    socketChannel.read(buffer);
                    System.out.println("收到客户端"+socketChannel.hashCode()+"发送的数据："+new String(buffer.array()));
                }
                // 删除当前key，防止重复操作
                selectionKeyIterator.remove();
            }
        }
    }
}
