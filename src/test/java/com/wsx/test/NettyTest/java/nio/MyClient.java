package com.wsx.test.NettyTest.java.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class MyClient {

    public static void main(String[] args){

        Selector selector = null;
        SocketChannel sc = null;
        try {
            selector = Selector.open();
            sc = SocketChannel.open();
            sc.configureBlocking(false);
            if(sc.connect(new InetSocketAddress("127.0.0.1",8080))){
                sc.register(selector, SelectionKey.OP_READ);
                byte[] outBytes = "QUERY_TIME".getBytes("utf-8");
                ByteBuffer outbb = ByteBuffer.allocate(outBytes.length);
                outbb.put(outBytes);
                outbb.flip();
                sc.write(outbb);
            }else{
                sc.register(selector, SelectionKey.OP_CONNECT);
            }



            while(true){
                selector.select(1000);
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
        } finally {
            if(selector !=null){
                try {
                    selector.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public static void handleInput(Selector selector,SelectionKey key) throws Exception{
        if(key.isValid()){
            SocketChannel sc = (SocketChannel)key.channel();
            if(key.isConnectable()){
                if(sc.finishConnect()){
                 sc.register(selector,SelectionKey.OP_READ );
                 byte[] outBytes = "QUERY_TIME".getBytes("utf-8");
                 ByteBuffer outbb = ByteBuffer.allocate(outBytes.length);
                 outbb.put(outBytes);
                 outbb.flip();
                 sc.write(outbb);
                }
            }
            if(key.isReadable()){
                ByteBuffer inbb = ByteBuffer.allocate(1024);
                int len = sc.read(inbb);
                if(len>0){
                    inbb.flip();
                    byte[] inBytes = new byte[inbb.remaining()];
                    inbb.get(inBytes);
                    String inStr = new String(inBytes,"utf-8");
                    System.out.println(inStr);

                }else if(len<0){
                    key.cancel();
                    sc.close();
                }else{

                }
            }
        }
    }
}
