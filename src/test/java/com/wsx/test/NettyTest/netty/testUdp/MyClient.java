package com.wsx.test.NettyTest.netty.testUdp;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;


public class MyClient {

    public static void main(String[] args) {

        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        try {
            b.group(group)
                    .channel(NioDatagramChannel.class)
                    .handler(new SimpleChannelInboundHandler<DatagramPacket>() {
                        @Override
                        protected void messageReceived(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
                            String body = msg.content().toString(CharsetUtil.UTF_8);
                            System.out.println(body);
                        }
                    });

            Channel ch = b.bind(0).sync().channel();
            ch.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer("哈哈哈哈哈哈哈哈哈", CharsetUtil.UTF_8),
                    new InetSocketAddress("127.0.0.1",2555)));
            ch.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
