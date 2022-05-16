package com.atguigu.nio;

import java.nio.ByteBuffer;

/**
 * @author lixing
 * @date 2022-04-26 10:39
 * @description
 */
public class NIOByteBufferPutGet {
    public static void main(String[] args) {
        // 创建一个Buffer
        ByteBuffer buffer = ByteBuffer.allocate(64);
        // 类型化方式放入数据
        buffer.putInt(100);
        buffer.putLong(9);
        buffer.putChar('尚');
        buffer.putShort((short) 4);
        buffer.flip(); // 读写切换
        // 取出
        System.out.println(buffer.getInt());
        System.out.println(buffer.getLong());
        System.out.println(buffer.getChar());
        System.out.println(buffer.getShort());
    }
}
