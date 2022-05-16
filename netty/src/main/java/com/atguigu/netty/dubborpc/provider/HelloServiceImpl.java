package com.atguigu.netty.dubborpc.provider;

import com.atguigu.netty.dubborpc.publicinterface.HelloService;

/**
 * @author lixing
 * @date 2022-05-16 10:43
 * @description
 */
public class HelloServiceImpl implements HelloService {
    // 当有消费方调用该方法时，就返回一个结果
    @Override
    public String hello(String msg){
        System.out.println("收到客户端消息="+msg);
        if(msg != null){
            return "你好客户端，我已经收到你的消息["+msg+"]";
        }else{
            return "你好客户端，我已经收到你的消息";
        }
    }
}
