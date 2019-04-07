package com.wsx.test.NettyTest.java.aio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

public class ClientHandler implements Runnable , CompletionHandler<Void,ClientHandler> {

    private AsynchronousSocketChannel channel;
    private CountDownLatch latch = new CountDownLatch(1);


    public ClientHandler(){
        try {
            channel = AsynchronousSocketChannel.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        channel.connect(new InetSocketAddress("127.0.0.1",8080),this,this);
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void completed(Void result, ClientHandler attachment) {
        byte[] outByte = new byte[0];
        try {
            outByte = "QUERY_TIME".getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        ByteBuffer outbb = ByteBuffer.allocate(outByte.length);
        outbb.put(outByte);
        outbb.flip();
        channel.write(outbb, outbb, new  CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result0, ByteBuffer attachment0) {
                if(attachment0.hasRemaining()){
                    channel.write(attachment0);
                }else{
                    ByteBuffer inbb = ByteBuffer.allocate(1024);
                    channel.read(inbb, inbb, new CompletionHandler<Integer, ByteBuffer>() {
                        @Override
                        public void completed(Integer result1, ByteBuffer attachment1) {
                            attachment1.flip();
                            byte[] inByte = new byte[attachment1.remaining()];
                            attachment1.get(inByte);
                            String inStr = null;
                            try {
                                inStr = new String(inByte,"utf-8");
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            System.out.println(inStr);
                        }

                        @Override
                        public void failed(Throwable exc, ByteBuffer attachment) {
                            try {
                                channel.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            latch.countDown();
                        }
                    });
                }
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                try {
                    channel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                latch.countDown();
            }
        });

    }

    @Override
    public void failed(Throwable exc, ClientHandler attachment) {
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        latch.countDown();
    }
}
