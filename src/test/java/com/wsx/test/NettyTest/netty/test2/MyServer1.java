package com.wsx.test.NettyTest.netty.test2;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

public class MyServer1 {
    private static final String  delimiter = "_$$_";

    public static void main(String[] args){
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        try {
            bootstrap.group(bossGroup,workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG,1024)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new DelimiterBasedFrameDecoder(1024,Unpooled.copiedBuffer(delimiter.getBytes())));
                            socketChannel.pipeline().addLast(new StringDecoder());
                            socketChannel.pipeline().addLast(new ChannelHandlerAdapter(){
                                private int count = 1;
                                @Override
                                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                    //没有StringDecoder时需要手动转成String
//                                    ByteBuf  inbb = (ByteBuf) msg;
//                                    byte[] in = new byte[inbb.readableBytes()];
//                                    inbb.readBytes(in);
//                                    int len = in.length;
//                                    String inStr  = new String(in,"utf-8");
                                    String inStr = (String)msg;
                                    System.out.println(inStr+"---"+count);
                                    count++;
                                    String outStr = null;
                                    if("QUERY_TIME".equalsIgnoreCase(inStr.trim())){
                                        outStr=""+System.currentTimeMillis();
                                    }else{
                                        outStr = "WRONG_ORDER";
                                    }
                                    outStr += delimiter;
                                    ByteBuf outbb = Unpooled.copiedBuffer(outStr.getBytes("utf-8"));
                                    ctx.write(outbb);
                                }

                                @Override
                                public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                                    ctx.flush();
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                    ctx.close();
                                }
                            });
                        }
                    });

            ChannelFuture future = bootstrap.bind(8080).sync();

            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
