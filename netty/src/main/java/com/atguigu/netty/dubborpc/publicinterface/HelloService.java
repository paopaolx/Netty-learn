package com.atguigu.netty.dubborpc.publicinterface;

/**
 * @author lixing
 * @date 2022-05-16 10:42
 * @description 接口，服务提供方和服务消费方公用的部分
 */
public interface HelloService {
    String hello(String msg);
}
