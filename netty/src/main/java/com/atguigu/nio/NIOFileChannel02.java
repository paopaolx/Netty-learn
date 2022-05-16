package com.atguigu.nio;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author lixing
 * @date 2022-04-25 16:42
 * @description
 */
public class NIOFileChannel02 {
    public static void main(String[] args) throws IOException {
        // 创建一个文件输入流
        File file = new File("E:\\learn\\java-learn\\netty-learn\\netty\\src\\main\\java\\com\\atguigu\\nio\\file01.txt");
        FileInputStream fileInputStream = new FileInputStream(file);
        // 通过fileInputStream获取对应的fileChannel
        FileChannel channel = fileInputStream.getChannel();
        // 创建缓冲区
        ByteBuffer byteBuffer = ByteBuffer.allocate((int) file.length());
        // 将通道的数据读入到缓冲区中
        channel.read(byteBuffer);
        // 将bytebuffer中的字节数据转成String
        System.out.println(new String(byteBuffer.array()));
        // 关闭流
        fileInputStream.close();
    }
}
