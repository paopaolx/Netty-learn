package com.atguigu.nio;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author lixing
 * @date 2022-04-25 17:04
 * @description
 */
public class NIOFileChannel03 {
    public static void main(String[] args) throws IOException {
        FileInputStream fileInputStream = new FileInputStream("src/main/1.txt");
        FileChannel fileChannel01 = fileInputStream.getChannel();

        FileOutputStream fileOutputStream = new FileOutputStream("src/main/1.txt");
        FileChannel fileChannel02 = fileOutputStream.getChannel();

        ByteBuffer byteBuffer = ByteBuffer.allocate(512);
        while (true) {
            // 这里有一个重要的操作，将buffer的标识位重置（position=0）
            byteBuffer.clear(); // 清空buffer。如果不复位的话，因为第一次循环读完数据后 position=limit，第二次读的时候 read=0，之后会一直循环read=0
            int read = fileChannel01.read(byteBuffer);
            if(read != -1){
                byteBuffer.flip();
                fileChannel02.write(byteBuffer);
            }else{
                break;
            }
        }

        fileInputStream.close();
        fileOutputStream.close();
    }
}
