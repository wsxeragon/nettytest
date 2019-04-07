package com.wsx.test.NettyTest.netty.test2;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

import java.io.UnsupportedEncodingException;

public class MyClient {

    public static void main(String[] args){

        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY,true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new LineBasedFrameDecoder(1024));
                            socketChannel.pipeline().addLast(new StringDecoder());
                            socketChannel.pipeline().addLast(new ChannelHandlerAdapter(){
                                byte[] bytes = ("QUERY_TIME" + System.getProperty("line.separator")).getBytes("utf-8");
                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                    ctx.close();
                                }

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    for(int i =0;i<300;i++){
                                        ByteBuf bb = Unpooled.buffer(bytes.length);
                                        bb.writeBytes(bytes);
                                        ctx.writeAndFlush(bb);
                                    }
                                }

                                @Override
                                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//                                    ByteBuf inbb = (ByteBuf)msg;
//                                    byte[] inbytes = new byte[inbb.readableBytes()];
//                                    inbb.readBytes(inbytes);
//                  vcx                  String str = new String(inbytes,"utf-8");
                                    String str = (String)msg;
                                    System.out.println(str);
                                }
                            });
                        }
                    });
            ChannelFuture future = bootstrap.connect("127.0.0.1",8080).sync();
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            group.shutdownGracefully();
        }
    }
}
