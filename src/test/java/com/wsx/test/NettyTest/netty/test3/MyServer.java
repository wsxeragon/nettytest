package com.wsx.test.NettyTest.netty.test3;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.CharsetUtil;

public class MyServer {


    public static void main(String[] args){

        EventLoopGroup workGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();

        try {
            bootstrap.group(workGroup)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST,Boolean.TRUE)
                    //相比于TCP而言，UDP不存在客户端和服务端的实际链接，因此不需要为连接(ChannelPipeline)设置handler
                    .handler(new SimpleChannelInboundHandler<DatagramPacket>() {
                        @Override
                        protected void messageReceived(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
                            String str = msg.content().toString(CharsetUtil.UTF_8);
                            System.out.println(str);
                            byte[] bytes1 = "呵呵呵呵呵呵呵呵".getBytes("utf-8");
                           ByteBuf byteBuf = Unpooled.copiedBuffer(bytes1);
                            ctx.writeAndFlush(new DatagramPacket(byteBuf, msg.sender()));

                        }
                    });

            Channel channel =  bootstrap.bind(2225).sync().channel();

            channel.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
