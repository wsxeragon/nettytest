package com.wsx.test.NettyTest.netty.test_messagepack;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

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
                            //这里设置通过增加包头表示报文长度来避免粘包
                            socketChannel.pipeline().addLast("frameDecoder",new LengthFieldBasedFrameDecoder(1024, 0, 2,0,2));
                            //增加解码器
                            socketChannel.pipeline().addLast("msgpack decoder",new MyMPDecoder());
                            //这里设置读取报文的包头长度来避免粘包
                            socketChannel.pipeline().addLast("frameEncoder",new LengthFieldPrepender(2));
                            //增加编码器
                            socketChannel.pipeline().addLast("msgpack encoder",new MyMPEncoder());
                            socketChannel.pipeline().addLast(new ChannelHandlerAdapter(){
                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                    ctx.close();
                                }

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    for(int i =1;i<=300;i++){
                                       MyInfo info = new MyInfo();
                                       info.setInt1(i);
                                       info.setStr1("NO-"+i);
                                        ctx.write(info);
                                    }
                                    ctx.flush();
                                }

                                @Override
                                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                    System.out.println(msg);
                                }

                                @Override
                                public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                                    ctx.flush();
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
