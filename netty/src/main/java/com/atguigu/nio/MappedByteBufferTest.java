package com.atguigu.nio;

import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author lixing
 * @date 2022-04-26 11:13
 * @description MappedByteBuffer 可以让文件直接在内存（堆外内存）修改，操作系统不需要拷贝一次
 */
public class MappedByteBufferTest {
    public static void main(String[] args) throws Exception {
        RandomAccessFile randomAccessFile = new RandomAccessFile("src/main/1.txt", "rw");
        FileChannel channel = randomAccessFile.getChannel();
        /**
         * 参数1：FileChannel.MapMode.READ_WRITE 使用读写模式
         * 参数2：0 可以直接修改的起始位置
         * 参数3：5 映射到内存的大小，即将1.txt的多少个字节映射到内存中（可以直接修改的范围是0-5）。最多修改5个字节
         */
        MappedByteBuffer mappedByteBuffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, 5);
        mappedByteBuffer.put(0 , (byte) 'H');
        mappedByteBuffer.put(3, (byte) '9');
        randomAccessFile.close();
        System.out.println("修改成功~~");
    }
}
