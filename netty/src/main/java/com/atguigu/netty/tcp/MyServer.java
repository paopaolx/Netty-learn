package com.atguigu.netty.tcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @author lixing
 * @date 2022-05-11 17:45
 * @description 案例：演示tcp粘包拆包问题
 * 粘包拆包问题一般只发生在服务端，指的是（一个或多个）客户端发送了D1,D2数据包，由于服务端一次读取的字节数不是固定的，所以会出现接收到的，可能不是单个完整的数据包。一般有四种情况：
 * (1) 服务端分两次读取到了两个独立的数据包，分别是D1和D2，没有粘包和拆包；
 * (2) 服务端一次接收到了两个数据包，D1和D2粘合在一起，被称为TCP粘包；
 * (3) 服务端分两次读取到了两个数据包，第一次读取到了完整的D1包和D2包的部分内容，第二次读取到了D2包的剩余内容，这被称为TCP拆包；
 * (4) 服务端分两次读取到了两个数据包，第一次读取到了D1包的部分内容D1_1，第二次读取到了D1包的剩余内容D1_2和D2包的整包。
 */
public class MyServer {
    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new MyServerInitializer());
            ChannelFuture channelFuture = serverBootstrap.bind(7777).sync();
            channelFuture.channel().closeFuture().sync();
        }finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
