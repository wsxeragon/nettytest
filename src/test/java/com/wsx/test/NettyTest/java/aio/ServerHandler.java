package com.wsx.test.NettyTest.java.aio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

public class ServerHandler implements  Runnable {
    private AsynchronousServerSocketChannel assc= null;
    private CountDownLatch latch =new CountDownLatch(1);

    public ServerHandler() {
        try {
            assc = AsynchronousServerSocketChannel.open();
            assc.bind(new InetSocketAddress(8080));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        try {
            assc.accept(this, new CompletionHandler<AsynchronousSocketChannel, ServerHandler>() {
                @Override
                public void completed(AsynchronousSocketChannel socketChannel, ServerHandler serverHandler) {
                    serverHandler.getChannel().accept(serverHandler,this);
                    ByteBuffer inbb = ByteBuffer.allocate(1024);
                    socketChannel.read(inbb, inbb, new CompletionHandler<Integer, ByteBuffer>() {
                        @Override
                        public void completed(Integer result0, ByteBuffer attachment0) {
                            attachment0.flip();
                            byte[] inBytes = new byte[attachment0.remaining()];
                            attachment0.get(inBytes);
                            String inStr = null;
                            try {
                                inStr = new String(inBytes,"utf-8");
                                System.out.println(inStr);
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            String outStr = null;
                            if("QUERY_TIME".equalsIgnoreCase(inStr.trim())){
                                outStr=""+System.currentTimeMillis();
                            }else{
                                outStr="WRONG_ORDER";
                            }
                            byte[] outBytes = new byte[0];
                            try {
                                outBytes = outStr.getBytes("utf-8");
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            ByteBuffer outbb = ByteBuffer.allocate(outBytes.length);
                            outbb.put(outBytes);
                            outbb.flip();

                            socketChannel.write(outbb, outbb, new CompletionHandler<Integer, ByteBuffer>() {
                                @Override
                                public void completed(Integer result1, ByteBuffer attachment1) {
                                    if(attachment1.hasRemaining()){
                                        socketChannel.write(attachment1,attachment1,this);
                                    }
                                }

                                @Override
                                public void failed(Throwable exc, ByteBuffer attachment) {
                                    try {
                                        socketChannel.close();
                                     } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }

                        @Override
                        public void failed(Throwable exc, ByteBuffer attachment) {
                            try {
                                socketChannel.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }

                @Override
                public void failed(Throwable exc, ServerHandler attachment) {
                    latch.countDown();
                }
            });
            latch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public AsynchronousServerSocketChannel getChannel(){
        return this.assc;
    }
}
