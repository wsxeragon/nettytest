package com.wsx.test.NettyTest.java.aio;

public class MyServer {

    public static void main(String[] args){

        new Thread(new ServerHandler()).start();


    }
}
