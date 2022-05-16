package com.atguigu.zerocopy;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * @author lixing
 * @date 2022-04-27 15:40
 * @description 传统IO拷贝，客户端
 */
public class OldIOClient {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 7001);
        String fileName = "src/main/报销.zip";
        InputStream inputStream = new FileInputStream(fileName);
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        byte[] buffer = new byte[4096];
        long readCount;
        long total = 0;
        long startTime = System.currentTimeMillis();
        while((readCount = inputStream.read(buffer)) >= 0){
            total += readCount;
            dataOutputStream.write(buffer);
        }
        System.out.println("发送总字节数："+total+" ，耗时："+(System.currentTimeMillis()-startTime)); // 发送总字节数：3575330 ，耗时：20
        dataOutputStream.close();
        inputStream.close();
        socket.close();
    }
}
