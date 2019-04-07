package com.wsx.test.NettyTest.java.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class MyServer {



    public static void main(String[] args){
        int port =8080;
        Selector selector =null;
        ServerSocketChannel serverSocketChannel = null;
        try {
            selector = Selector.open();

            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().bind(new InetSocketAddress(port),1024);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while(true){
                selector.select(1000);//一秒唤醒一次
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                SelectionKey key = null;
                while(it.hasNext()){
                    key = it.next();
                    it.remove();
                    try {
                        handleInput(selector,key);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(selector !=null){
                try {
                    selector.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void handleInput(Selector selector,SelectionKey key)throws Exception{
        if(key.isValid()){
            if (key.isAcceptable()){
                ServerSocketChannel ssc = (ServerSocketChannel)key.channel();
                SocketChannel sc = ssc.accept();
                sc.configureBlocking(false);
                sc.register(selector,SelectionKey.OP_READ);
            }
            if(key.isReadable()){
                SocketChannel sc = (SocketChannel)key.channel();
                ByteBuffer inbb = ByteBuffer.allocate(1024);
                int len = sc.read(inbb);
                if(len>0){
                    inbb.flip();
                    byte[] bytes = new byte[inbb.remaining()];
                    inbb.get(bytes);
                    String instr = new String(bytes,"utf-8");
                    String outStr = null;
                    System.out.println(instr);
                    if(" ".equalsIgnoreCase(instr.trim())){
                        outStr=""+System.currentTimeMillis();
                    }else{
                        outStr="WRONG_ORDER";
                    }
                    bytes = outStr.getBytes("utf-8");
                    ByteBuffer outbb = ByteBuffer.allocate(bytes.length);
                    outbb.put(bytes);
                    outbb.flip();
                    sc.write(outbb);
                }else if(len <0){
                    key.channel();
                    sc.close();
                }else{

                }
            }
        }
    }
}
