package com.wsx.test.NettyTest.java.bio;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

public class MyServer {

    public static void main(String[] args){
        int port = 8080;

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            ExecutorService executorService = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
                    1000,129l, TimeUnit.SECONDS,new ArrayBlockingQueue<>(50));
            while(true){
                Socket socket = serverSocket.accept();
                executorService.execute(new MyServerHandler(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(serverSocket != null){
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }

}
