package com.atguigu.netty.sample;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * @author lixing
 * @date 2022-04-29 15:03
 * @description Netty服务端
 */
public class NettyServer {
    public static void main(String[] args) throws InterruptedException {
        // 创建BossGroup和WorkerGroup
        // bossGroup只处理连接请求，真正与客户端进行的业务处理，交给workerGroup完成。这两个都是无限循环
        // bossGroup和workerGroup默认含有的子线程（NioEventLoop）个数为 cpu核数*2
        // 可以自定义设置bossGroup和workerGroup的NioEventLoop线程数，如果workerGroup设置子线程数为8，则如果有超过8个客户端连接，会按照1，2，3...，8，1，2...的方式循环分配
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            // 创建服务器端启动对象，配置参数
            ServerBootstrap serverBootstrap = new ServerBootstrap();

            // 创建业务线程池
            EventExecutorGroup group = new DefaultEventExecutorGroup(2);

            // 使用链式编程来进行设置
            serverBootstrap.group(bossGroup, workerGroup) // 设置两个线程组
                    .channel(NioServerSocketChannel.class) // 使用NioServerSocketChannel作为服务器端的通道实现类（反射）
                    .option(ChannelOption.SO_BACKLOG, 128) // 设置线程队列，得到连接个数
                    .childOption(ChannelOption.SO_KEEPALIVE, true) // 设置保持活动连接状态
                    .childHandler(new ChannelInitializer<SocketChannel>() { // 创建一个通道初始化对象（匿名对象）
                        // 给pipeline设置处理器
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            // 在此处可以获取客户端的channel，将其保存到一个集合中管理，可以在推送消息时，将业务加入到到不同channel对应的NIOEventLoop的taskQueue或scheduleTaskQueue中执行
                            System.out.println("客户socketChannel的hashcode="+socketChannel.hashCode());
                            // 说明：如果我们在使用addLast()向pipeline中添加handler时，前面有指定EventExecutorGroup，那么该handler会优先加入到线程池中
                            socketChannel.pipeline().addLast(group, new NettyServerHandler());
                        }
                    }); // 给workerGroup的EventLoop对应的管道设置处理器

            System.out.println("服务器 is ok...");
            ChannelFuture cf = serverBootstrap.bind(6668).sync(); // 绑定端口，并且同步，生成了一个ChannelFuture对象

            // 给cf注册监听器，监听我们关心的事件
            cf.addListener((ChannelFutureListener) channelFuture -> {
                if(cf.isSuccess()){
                    System.out.println("监听端口 6668 成功");
                }else{
                    System.out.println("监听端口 6668 失败");
                }
            });

            cf.channel().closeFuture().sync(); // 对关闭通道进行见监听
        }finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
