package com.wsx.test.NettyTest.netty.private_protocol;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.internal.ConcurrentSet;
import org.springframework.util.StringUtils;

import javax.print.DocFlavor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MyServer {

    public static void main(String[] args){
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        try {
            bootstrap.group(bossGroup,workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG,1024)
                    //handler()是在初始化的时候执行，childHandler()是在客户端成功connect后才执行。
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new NettyMessageDecoder());
                            socketChannel.pipeline().addLast(new NettyMessageEncoder());
                            socketChannel.pipeline().addLast(new ReadTimeoutHandler(50000));
                            socketChannel.pipeline().addLast(new LoginAuthRspHandler());
                            socketChannel.pipeline().addLast(new HeartBeatRespHandler());
                            socketChannel.pipeline().addLast( new MyRspHandler());

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


    private static class HeartBeatRespHandler extends  ChannelHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            System.out.println(" server channel active... ");
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            NettyMessage message = (NettyMessage) msg;
            // 如果是握手请求消息，处理，其它消息透传
            if (message.getHeader() != null
                    && message.getHeader().getType() == MessageType.HEARTBEAT_REQ) {
                System.out.println(" 收到心跳... ");
                NettyMessage rsp = new NettyMessage();
                Header header = new Header();
                header.setType(MessageType.HEARTBEAT_RESP);
                rsp.setHeader(header);
                ctx.writeAndFlush(rsp);
            } else {
                ctx.fireChannelRead(msg);
            }
        }


        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                throws Exception {
            cause.printStackTrace();
            ctx.close();
            ctx.fireExceptionCaught(cause);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {

        }


    }


    private static class MyRspHandler extends ChannelHandlerAdapter{

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            NettyMessage message = (NettyMessage) msg;
            if(message.getHeader() != null && message.getHeader().getType() == MessageType.BUSINESS_0){
                System.out.println("收到业务请求："+message.getBody());
                NettyMessage resp = new NettyMessage();
                Header header = new Header();
                header.setType(MessageType.BUSINESS_1);
                Map<String,Object> attach = new HashMap<>();
                header.setAttachment(attach);
                resp.setHeader(header);
                resp.setBody("hehehehhee");
                System.out.println("发送业务返回...");
                ctx.writeAndFlush(resp);
            }else{
                ctx.fireChannelRead(msg);
            }
        }
    }

    private static class LoginAuthRspHandler extends ChannelHandlerAdapter{

        private static Map<String,String> loginMap = new ConcurrentHashMap<>();
        private static Set<String> tokens = new ConcurrentSet<>();

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

            NettyMessage res=(NettyMessage)msg;
            //建立连接后，发送认证消息
            NettyMessage message=new NettyMessage();
            Map<String,Object> result = new HashMap<>();
            Header header=new Header();
            header.setType((byte) MessageType.LOGIN_RESP);
            message.setHeader(header);
            byte errCode = 0;
            String token = "";
            String errMsg =null;
            //若是握手应答消息，判断是否认证成功
            if (res.getHeader() != null && res.getHeader().getType() == MessageType.LOGIN_REQ) {
               String username = (String)res.getHeader().getAttachment().get("username");
                if (loginMap.containsKey(username)) {
                    //重复登陆，拒绝
                    errCode = 1;
                    errMsg="重复登陆";
                    //刷新token
                    token = ""+System.currentTimeMillis();
                    loginMap.put(username,token);
                    tokens.add(token);
                }else{
                    if (StringUtils.isEmpty(username) || !"wsx".equals(username)){
                        errCode=-1;
                        errMsg="用户名不存在";
                    }
                    String password = (String)res.getHeader().getAttachment().get("password");
                    if (!"123456".equals(password)){
                        errCode=0;
                        errMsg="密码错误";
                    }
                    errCode = 1;
                    token = ""+System.currentTimeMillis();
                    loginMap.put(username,token);
                    tokens.add(token);
                }
                result.put("errCode",errCode);
                result.put("errMsg",errMsg);
                Map<String,Object> attach = new HashMap<>();
                attach.put("token",token);
                header.setAttachment(attach);
                message.setBody(result);
                System.out.println("client 发送 认证返回：message="+message);
                ctx.writeAndFlush(message);
            }else{
                String token0 = (String)res.getHeader().getAttachment().get("token");
                if(!tokens.contains(token0)){
                    errCode = -3;
                    errMsg="未登陆";
                    result.put("errCode",errCode);
                    result.put("errMsg",errMsg);
                    message.setBody(result);
                    System.out.println("client 发送未登录返回：message="+message);
                    ctx.writeAndFlush(message);
                }else{
                    ctx.fireChannelRead(msg);
                }
            }

        }
    }
}
