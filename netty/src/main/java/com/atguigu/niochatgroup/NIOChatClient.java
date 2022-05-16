package com.atguigu.niochatgroup;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;

/**
 * @author lixing
 * @date 2022-04-27 11:28
 * @description 群聊系统客户端
 */
public class NIOChatClient {
    // 定义属性
    private final String HOST = "127.0.0.1";
    private final int PORT = 6667;
    private Selector selector;
    private SocketChannel socketChannel;
    private String username;

    // 构造器，进行初始化操作
    public NIOChatClient() throws IOException {
        // 获取selector对象
        selector = Selector.open();
        // 与服务端建立连接
        socketChannel = SocketChannel.open(new InetSocketAddress(HOST, PORT));
        // 设置客户端通道为非阻塞
        socketChannel.configureBlocking(false);
        // 通道注册到selector上，关注读事件
        socketChannel.register(selector, SelectionKey.OP_READ);
        // 获取当前客户端名称
        username = socketChannel.getLocalAddress().toString().substring(1);
        System.out.println(username+" is ok... ");
    }

    // 向服务器发送消息
    public void sendMsgToServer(String msg){
        msg = username + "说：" + msg;
        try {
            socketChannel.write(ByteBuffer.wrap(msg.getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 读取从服务端发送过来的消息
    public void readMsgFromServer(){
        try {
            int count = selector.select();
            if(count > 0){ // selector上有发生事件的通道
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while(iterator.hasNext()){
                    SelectionKey key = iterator.next();
                    if(key.isReadable()){ // 有读操作
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        // 将通道中的数据读出到buffer
                        socketChannel.read(buffer);
                        String msg = new String(buffer.array());
                        System.out.println(msg.trim());
                    }
                    iterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        // 启动服务端
        NIOChatClient nioChatClient = new NIOChatClient();
        new Thread(()->{
            // 间隔2秒读取服务端发送过来的消息
            while (true){
                nioChatClient.readMsgFromServer();
                try {
                    Thread.currentThread().sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        // 发送消息给服务端
        Scanner scanner = new Scanner(System.in);
        while(scanner.hasNextLine()){
            String str = scanner.nextLine();
            nioChatClient.sendMsgToServer(str);
        }
    }
}
