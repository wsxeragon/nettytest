package com.wsx.test.NettyTest.netty.test_proto;

import com.wsx.test.NettyTest.netty.test_messagepack.MyInfo;
import com.wsx.test.NettyTest.netty.test_messagepack.MyMPDecoder;
import com.wsx.test.NettyTest.netty.test_messagepack.MyMPEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import java.util.ArrayList;
import java.util.List;

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
                            socketChannel.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                            socketChannel.pipeline().addLast(new ProtobufDecoder(PersonOuterClass.Person.getDefaultInstance()));
                            socketChannel.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                            socketChannel.pipeline().addLast(new ProtobufEncoder());
                            socketChannel.pipeline().addLast(new ChannelHandlerAdapter(){
                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                    ctx.close();
                                }

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    for(int i =1;i<=10;i++){
                                        PersonOuterClass.Person.Builder builder =  PersonOuterClass.Person.newBuilder();
                                        builder.setName("Res");
                                        builder.setId(i);
                                        builder.setEmail("test@qq.com");
                                        List<String> names= new ArrayList<>();
                                        names.add("tim");
                                        names.add("tom");
                                        names.add("tommy");
                                        builder.addAllNickname(names);

                                        PersonOuterClass.Person person = builder.build();

                                        ctx.write(person);
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
