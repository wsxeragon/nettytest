package com.wsx.test.NettyTest.java.aio;

public class MyClient {


    public static void main(String[] args){

        new Thread(new ClientHandler()).start();


    }
}
