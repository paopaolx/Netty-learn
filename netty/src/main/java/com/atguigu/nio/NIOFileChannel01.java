package com.atguigu.nio;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author lixing
 * @date 2022-04-25 16:25
 * @description 将一个字符串写入到一个txt文件中。 str => 写入到buffer => 写入到channel => 写入到fileOutputStream
 */
public class NIOFileChannel01 {
    public static void main(String[] args) throws IOException {
        String str = "hello,尚硅谷";
        // 创建一个输出流 --> channel
        FileOutputStream fileOutputStream = new FileOutputStream("E:\\learn\\java-learn\\netty-learn\\netty\\src\\main\\java\\com\\atguigu\\nio\\file01.txt");
        // 通过fileOutputStream获取对应的FileChannel
        FileChannel fileChannel = fileOutputStream.getChannel();
        // 创建一个缓存区ByteBuffer
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        // 将str放入到bytebuffer中（读操作）
        byteBuffer.put(str.getBytes());
        // 将bytebuffer中的数据写入到channel（写操作）
        byteBuffer.flip(); // 读写切换
        fileChannel.write(byteBuffer);
        // 关闭流
        fileOutputStream.close();
    }
}
