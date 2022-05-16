package com.atguigu.nio;

import java.nio.IntBuffer;
import java.nio.channels.Channel;

/**
 * @author lixing
 * @date 2022-04-25 14:34
 * @description Buffer的使用
 */
public class BasicBuffer {
    public static void main(String[] args) {
        // 创建一个buffer，大小为5
        IntBuffer intBuffer = IntBuffer.allocate(5);
        // 向buffer中存放数据
        for(int i=0; i<intBuffer.capacity(); i++){
            intBuffer.put(i*2);
        }
        // 从buffer中取出数据
        intBuffer.flip(); // 将buffer切换，读写切换
        while(intBuffer.hasRemaining()){
            System.out.println(intBuffer.get());
        }
    }
}
