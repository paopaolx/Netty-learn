package com.atguigu.netty.dubborpc.consumer;

import com.atguigu.netty.dubborpc.netty.NettyClient;
import com.atguigu.netty.dubborpc.publicinterface.HelloService;

/**
 * @author lixing
 * @date 2022-05-16 13:23
 * @description
 */
public class ClientBootStrap {
    // 定义协议头
    public static final String providerName = "HelloService#hello#";

    public static void main(String[] args) {
        // 创建一个消费者
        NettyClient consumer = new NettyClient();
        // 创建代理对象
        HelloService service = (HelloService) consumer.getBean(HelloService.class, providerName);
        // 通过代理对象调用远程服务
        String res = service.hello("你好 dubbo~~");
        System.out.println("客户端调用远程服务的结果="+res);
    }
}
