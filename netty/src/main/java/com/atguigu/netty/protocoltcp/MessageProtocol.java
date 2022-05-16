package com.atguigu.netty.protocoltcp;

/**
 * @author lixing
 * @date 2022-05-12 09:48
 * @description 自定义协议包（包含属性：数据字节内容，数据的字节长度）
 */
public class MessageProtocol {
    private int len;
    private byte[] content;

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
