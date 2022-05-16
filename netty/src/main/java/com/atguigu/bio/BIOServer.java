package com.atguigu.bio;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author lixing
 * @date 2022-04-25 13:12
 * @description
 */
public class BIOServer {
    public static void main(String[] args) {
        ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
        try {
            ServerSocket serverSocket = new ServerSocket(6666); // 服务端创建socket连接，监听6666端口
            System.out.println("服务端启动...");
            while(true){
                System.out.println("等待连接...");
                final Socket socket = serverSocket.accept(); // 监听，等待客户端连接（会阻塞）
                cachedThreadPool.execute(()->{
                    handler(socket); // 创建线程处理客户端IO请求
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void handler(Socket socket){
        try {
            System.out.println("收到"+Thread.currentThread().getName()+"客户端连接...");
            byte[] bytes = new byte[1024];
            InputStream inputStream = socket.getInputStream(); // 通过socket获取输入流
            while (true){
                System.out.println("read...");
                int read = inputStream.read(bytes); // 如果通道中没有数据（会阻塞）
                if(read != -1){
                    System.out.println(new String(bytes, 0, read)); // 输出客户端发送的数据
                }else{
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                System.out.println("关闭"+Thread.currentThread().getName()+"客户端连接...");
                socket.close(); // 关闭socket
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
