package com.wsx.test.NettyTest.netty.test_messagepack;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;

public class MyServer {

    public static void main(String[] args){
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        try {
            bootstrap.group(bossGroup,workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG,100)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            //这里设置通过增加包头表示报文长度来避免粘包
                            socketChannel.pipeline().addLast("frameDecoder",new LengthFieldBasedFrameDecoder(1024, 0, 2,0,2));
                            //增加解码器
                            socketChannel.pipeline().addLast("msgpack decoder",new MyMPDecoder());
                            //这里设置读取报文的包头长度来避免粘包
                            socketChannel.pipeline().addLast("frameEncoder",new LengthFieldPrepender(2));
                            //增加编码器
                            socketChannel.pipeline().addLast("msgpack encoder",new MyMPEncoder());
                            socketChannel.pipeline().addLast(new ChannelHandlerAdapter(){
                                private int count = 0;
                                @Override
                                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                    System.out.println(msg+"---"+count);
                                    count++;
                                    MyInfo myInfo = new MyInfo();
                                    myInfo.setStr1("RES-"+count);
                                    myInfo.setInt1(count);
                                    ctx.writeAndFlush(myInfo);
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
