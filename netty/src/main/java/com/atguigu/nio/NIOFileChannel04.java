package com.atguigu.nio;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * @author lixing
 * @date 2022-04-26 10:13
 * @description 拷贝文件
 */
public class NIOFileChannel04 {
    public static void main(String[] args) throws IOException {
        // 创建相关流
        FileInputStream fileInputStream = new FileInputStream("src/main/a.jpg");
        FileOutputStream fileOutputStream = new FileOutputStream("src/main/b.jpg");
        // 获取各个流对应的fileChannel
        FileChannel sourceChannel = fileInputStream.getChannel();
        FileChannel destChannel = fileOutputStream.getChannel();
        // 使用transferForm完成拷贝
        destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        // 关闭相关流
        sourceChannel.close();
        destChannel.close();
        fileInputStream.close();
        fileOutputStream.close();
    }
}
