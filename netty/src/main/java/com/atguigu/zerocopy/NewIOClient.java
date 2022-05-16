package com.atguigu.zerocopy;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

/**
 * @author lixing
 * @date 2022-04-27 17:07
 * @description NIO零拷贝，传输文件，客户端
 */
public class NewIOClient {
    public static void main(String[] args) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress("localhost", 7001));
        String fileName = "src/main/报销.zip";
        FileChannel fileChannel = new FileInputStream(fileName).getChannel();
        long startTime = System.currentTimeMillis();
        // 在linux下，一个transferTo方法就可以传输完成
        // 在windows下一次调用transferTo只能发8MB，如果传输文件过大，需要分段传输，记录每次传输的位置
        // transferTo底层使用到零拷贝
        long transferCount = fileChannel.transferTo(0, fileChannel.size(), socketChannel);
        System.out.println("发送的总的字节数="+transferCount+" 耗时："+(System.currentTimeMillis()-startTime)); // 发送的总的字节数=3575330 耗时：6
        fileChannel.close();
        socketChannel.close();
    }
}
