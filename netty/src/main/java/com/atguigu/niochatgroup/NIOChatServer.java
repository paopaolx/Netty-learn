package com.atguigu.niochatgroup;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

/**
 * @author lixing
 * @date 2022-04-27 11:27
 * @description 群聊系统服务端
 */
public class NIOChatServer {
    // 定义属性
    private Selector selector;
    private ServerSocketChannel listenChannel;
    private static final int PORT = 6667;

    // 构造器，初始化工作
    public NIOChatServer(){
        try {
            // 获取选择器
            selector = Selector.open();
            // 获取服务端channel
            listenChannel = ServerSocketChannel.open();
            // 设置服务端网络通道监听端口
            listenChannel.socket().bind(new InetSocketAddress(PORT));
            // 设置服务端channel为非阻塞
            listenChannel.configureBlocking(false);
            // 将服务端channel注册到selector上，监听客户端连接事件
            listenChannel.register(selector, SelectionKey.OP_ACCEPT);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    // 监听方法
    public void listenHandler(){
        try {
            // 循环监听客户端事件
            while (true){
                int count = selector.select(); // 阻塞监听客户端通道是否有事件发生
                if(count > 0){ // 说明有事件发生
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while(iterator.hasNext()){
                        SelectionKey key = iterator.next();
                        if(key.isAcceptable()){ // 监听到连接事件，将连接事件的通道注册到selector上
                            SocketChannel socketChannel = listenChannel.accept();
                            socketChannel.configureBlocking(false);
                            socketChannel.register(selector, SelectionKey.OP_READ);
                            System.out.println("客户端 "+socketChannel.getRemoteAddress() + " 上线了...");
                        }
                        if(key.isReadable()){ // 监听到读事件
                            readClientData(key);
                        }
                        iterator.remove(); // 移除当前SelectionKey，防止重复操作
                    }
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    // 读取客户端消息
    public void readClientData(SelectionKey key) throws IOException {
        SocketChannel channel = null;
        try{
            // 获取到发生读事件的channel
            channel = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            // 将通道中数据读出到buffer
            int read = channel.read(buffer);
            if(read > 0){
                String msg = new String(buffer.array());
                System.out.println("接收到客户端 "+channel.getRemoteAddress()+" 消息："+msg);
                // 将消息转发给其他客户端
                sendMsgToOtherClients(msg, channel);
            }
        }catch (IOException e){
            System.out.println("客户端 "+channel.getRemoteAddress()+" 离线...");
            // 取消注册，关闭通道
            key.cancel();
            channel.close();
        }
    }

    // 转发消息给其他客户端
    public void sendMsgToOtherClients(String msg, SocketChannel self) throws IOException {
        // 转发消息的时候要排除自己
        System.out.println("服务器转发消息...");
        // 遍历所有注册到selector上的socketChannel，并排除自己
        for(SelectionKey key: selector.keys()){
            Channel channel = key.channel();
            // 因为注册到selector上的channel还有服务端的ServerSocketChannel
            if(channel instanceof SocketChannel && channel != self){
                ((SocketChannel) channel).write(ByteBuffer.wrap(msg.getBytes()));
            }
        }
    }

    public static void main(String[] args) {
        // 启动服务端
        NIOChatServer nioChatServer = new NIOChatServer();
        nioChatServer.listenHandler();
    }
}
