package com.atguigu.netty.dubborpc.provider;

import com.atguigu.netty.dubborpc.netty.NettyServer;

/**
 * @author lixing
 * @date 2022-05-16 10:47
 * @description ServerBootStrap会启动一个服务提供者，NettyServer
 */
public class ServerBootStrap {
    public static void main(String[] args) {
        NettyServer.startServer("127.0.0.1", 7000);
    }
}
