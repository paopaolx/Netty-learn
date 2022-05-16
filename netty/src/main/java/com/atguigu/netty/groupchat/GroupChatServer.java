package com.atguigu.netty.groupchat;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * @author lixing
 * @date 2022-05-06 17:30
 * @description Netty群聊系统 服务端
 */
public class GroupChatServer {
    private int port; // 监听端口

    public GroupChatServer(int port){
        this.port = port;
    }

    // 编写run方法，处理客户端请求
    public void run() throws InterruptedException {
        // 创建两个线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(); // 默认线程数=cpu核数*2
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline(); // 获取到pipeline
                            pipeline.addLast("decoder", new StringDecoder()); // 向pipeline中加入解码器
                            pipeline.addLast("encoder", new StringEncoder()); // 向pipeline中加入编码器
                            pipeline.addLast(new GroupChatServerHandler()); // 向pipeline中加入自定义的业务处理handler
                        }
                    });
            System.out.println("netty服务端启动");
            ChannelFuture channelFuture = bootstrap.bind(port).sync(); // 绑定端口，并启动
            channelFuture.channel().closeFuture().sync(); // 监听关闭
        }finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new GroupChatServer(7000).run(); // 启动
    }
}
