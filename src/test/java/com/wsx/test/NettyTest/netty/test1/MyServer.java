package com.wsx.test.NettyTest.netty.test1;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.nio.ByteBuffer;

public class MyServer {

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
                            socketChannel.pipeline().addLast(new ChannelHandlerAdapter(){
                                @Override
                                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                    ByteBuf  inbb = (ByteBuf) msg;
                                    byte[] in = new byte[inbb.readableBytes()];
                                    inbb.readBytes(in);
                                    String inStr  = new String(in,"utf-8");
                                    String outStr = null;
                                    if("QUERY_TIME".equalsIgnoreCase(inStr.trim())){
                                        outStr=""+System.currentTimeMillis();
                                    }else{
                                        outStr = "WRONG_ORDER";
                                    }
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
