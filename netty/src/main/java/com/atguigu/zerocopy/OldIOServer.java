package com.atguigu.zerocopy;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author lixing
 * @date 2022-04-27 15:39
 * @description 传统IO拷贝，服务端
 */
public class OldIOServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(7001);
        while (true){
            Socket socket = serverSocket.accept();
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            try{
                byte[] byteArray = new byte[4096];
                while (true){
                    int readCount = dataInputStream.read(byteArray, 0, byteArray.length);
                    if(-1 == readCount){
                        break;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
