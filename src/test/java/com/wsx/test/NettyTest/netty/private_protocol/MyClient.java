package com.wsx.test.NettyTest.netty.private_protocol;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import jdk.nashorn.internal.parser.Token;
import org.springframework.util.StringUtils;

import javax.validation.constraints.Null;
import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MyClient {

    private static ScheduledExecutorService executor = Executors
            .newScheduledThreadPool(1);

    private static ScheduledExecutorService executor1 = Executors
            .newScheduledThreadPool(1);

    private static volatile int count = 0;

    private static String TOKEN =null;

    private static Bootstrap bootstrap=null;

    private static long sleepTime=1000;

    public static void main(String[] args){
        connect("127.0.0.1",8080);

    }

    private static void connect(String ip,int port){
        EventLoopGroup group = new NioEventLoopGroup();
        ScheduledFuture taskFuture = null;
        try {
            bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY,true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new NettyMessageDecoder());
                            socketChannel.pipeline().addLast(new NettyMessageEncoder());
                            socketChannel.pipeline().addLast(new ReadTimeoutHandler(5000));
                            socketChannel.pipeline().addLast(new LoginAuthReqHandler());
                            socketChannel.pipeline().addLast(new HeartBeatReqHandler());
                            socketChannel.pipeline().addLast(new MyReqHandler());
                        }
                    });
            ChannelFuture future = bootstrap.connect(ip,port).sync();
            sleepTime = 1000;
            taskFuture =  executor1.scheduleAtFixedRate(new Business(future.channel()),2000,5000,TimeUnit.MILLISECONDS);
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            // 首先监听网络断连事件，如果Channel关闭，则执行后续的重连任务，通过Bootstrap重新发起连接
            // 客户端挂在cIoseFuture上监听链路关闭信号，一旦关闭，则创建重连定时器，5s之后重新发起连接，直到重连成功。
            //服务端感知到断连事件之后，需要清空缓存的登录认证注册信息，以保证后续客户端能够正常重连。
            if(taskFuture != null){
                taskFuture.cancel(true);
                taskFuture=null;
            }
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    while(sleepTime<60*60*1000){
                        try {
                            sleepTime = (long)(sleepTime*1.5);
                            System.out.println("睡眠："+sleepTime);
                            Thread.sleep(sleepTime);
                            System.out.println("重连");
                            connect(ip, port);
                        } catch (InterruptedException e) {
                             e.printStackTrace();
                        }
                    }
                }
            });




        }


    }


    private static class MyReqHandler extends   ChannelHandlerAdapter{
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            NettyMessage message = (NettyMessage) msg;
           if(message.getHeader() != null && message.getHeader().getType() == MessageType.BUSINESS_1){
               System.out.println("收到业务返回："+message.getBody());
           }else{
               ctx.fireChannelRead(msg);
           }
        }
    }

    private static class HeartBeatReqHandler extends   ChannelHandlerAdapter{

        private ScheduledFuture heartbeat;
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            NettyMessage message = (NettyMessage) msg;
            //认证完后定时发送心跳
            if (message.getHeader() != null && message.getHeader().getType() == MessageType.LOGIN_RESP) {
                heartbeat=ctx.executor().scheduleAtFixedRate(new HeartBeat(ctx,TOKEN), 0, 5000, TimeUnit.MILLISECONDS);
                count = 0;
            } else if (message.getHeader() != null && message.getHeader().getType() == MessageType.HEARTBEAT_RESP) {
                System.out.println("收到心跳回复");
                count = 0;
            }else {
                ctx.fireChannelRead(msg);
            }
        }



        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            System.out.println("异常-------------" + cause);
            cause.printStackTrace();
            if(heartbeat != null){
                heartbeat.cancel(true);
                heartbeat=null;
            }
            ctx.fireExceptionCaught(cause);
        }


        private class HeartBeat implements Runnable {
            private final ChannelHandlerContext ctx;
            private String token;

            public HeartBeat(final ChannelHandlerContext ctx,String token) {
                this.ctx = ctx;
                this.token = token;
            }

            @Override
            public void run() {
                NettyMessage message = new NettyMessage();
                Header header = new Header();
                header.setType(MessageType.HEARTBEAT_REQ);
                Map<String,Object> attach = new HashMap<>();
                attach.put("token",token);
                header.setAttachment(attach);
                message.setHeader(header);
                System.out.println("客户端发送心跳...");
                ctx.writeAndFlush(message);
                count++;
            }

        }

    }

    private static class LoginAuthReqHandler extends ChannelHandlerAdapter{
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            //建立连接后，发送认证消息
            NettyMessage message=new NettyMessage();
            Header header=new Header();
            header.setType((byte) MessageType.LOGIN_REQ);
            Map<String,Object> auth = new HashMap<>();
            auth.put("username","wsx");
            auth.put("password","123456");
            header.setAttachment(auth);
            message.setHeader(header);
            System.out.println("client 发送 认证消息：message="+message);
            ctx.writeAndFlush(message);
        }


        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            //System.out.println("write:"+msg);
            super.write(ctx, msg, promise);
        }


        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            NettyMessage message=(NettyMessage)msg;
            //若是握手应答消息，判断是否认证成功
            if (message.getHeader() != null && message.getHeader().getType() == MessageType.LOGIN_RESP) {
                Map<String,Object> loginResult= (Map)message.getBody();
                if (loginResult.get("errCode") ==null || (byte)1!=(byte) loginResult.get("errCode")) {
                    ctx.close();
                }else {
                    TOKEN = (String) ((NettyMessage) msg).getHeader().getAttachment().get("token");
                    ctx.fireChannelRead(msg);
                }
            }else{//如果不是认证信息，直接过滤
                ctx.fireChannelRead(msg);
            }
        }


    }

    private static class Business implements Runnable {
        private Channel channel;

        public Business(Channel channel) {
            this.channel = channel;
        }

        @Override
        public void run() {
            if(!StringUtils.isEmpty(TOKEN)){
                NettyMessage message = new NettyMessage();
                Header header = new Header();
                header.setType(MessageType.BUSINESS_0);
                Map<String,Object> attach = new HashMap<>();
                attach.put("token",TOKEN);
                header.setAttachment(attach);
                message.setHeader(header);
                message.setBody("hahahhahaha");
                System.out.println("发送业务请求..."+message.getBody());
                channel.writeAndFlush(message);
            }
        }

    }
}
