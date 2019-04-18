package com.wsx.test.NettyTest.netty.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;

import javax.sound.midi.VoiceStatus;

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
                            //将请求和应答解码为http消息
                            socketChannel.pipeline().addLast(new HttpServerCodec());
                            socketChannel.pipeline().addLast(new HttpObjectAggregator(65536));
                            socketChannel.pipeline().addLast(new ChunkedWriteHandler());
                            socketChannel.pipeline().addLast(new SimpleChannelInboundHandler<Object>() {
                                @Override
                                protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
                                    WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory("ws:127.0.0.1:8080/websocket",null,false);
                                    WebSocketServerHandshaker handshaker = null;
                                    //第一次为http
                                    if(msg instanceof FullHttpRequest){
                                        FullHttpRequest req = (FullHttpRequest)msg;
                                        //解码失败
                                        if(!req.decoderResult().isSuccess() || !"websocket".equals(req.headers().get("Upgrade"))){
                                            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.BAD_REQUEST);
                                            //首先它判断服务器要输出的是不是HTTP200状态（准备就绪），如果不是，表明出错了，将HTTP状态码输出给客户端：
                                            if(response.status().code() != 200){
                                                ByteBuf buf = Unpooled.copiedBuffer(response.status().toString(), CharsetUtil.UTF_8);
                                                response.content().writeBytes(buf);
                                                buf.release();
                                                HttpHeaderUtil.setContentLength(response,buf.readableBytes());
                                            }

                                            ChannelFuture future = ctx.channel().writeAndFlush(response);
                                            //关闭连接。当然要判断一下keep alive 和 HTTP 200标志
                                            if(!HttpHeaderUtil.isKeepAlive(req) || response.status().code() != 200){
                                                future.addListener(ChannelFutureListener.CLOSE);
                                            }
                                            return;
                                        }

                                        handshaker = factory.newHandshaker(req);
                                        //如果handshaker创建失败，发送错误消息，否则开始进行握手动作
                                        if(handshaker == null){
                                            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
                                        }else{
                                            handshaker.handshake(ctx.channel(),req);
                                        }

                                    }

                                    //websocket
                                    if(msg instanceof WebSocketFrame){
                                        WebSocketFrame frame = (WebSocketFrame) msg;
                                        //是否是关闭命令
                                        if(frame instanceof CloseWebSocketFrame){
                                            handshaker.close(ctx.channel(),((CloseWebSocketFrame) frame).retain());
                                            return;
                                        }

                                        //是否是ping
                                        if(frame instanceof PingWebSocketFrame){
                                            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
                                            return;
                                        }

                                        //不支持二进制
                                        if(!(frame instanceof  TextWebSocketFrame)){
                                            throw  new UnsupportedOperationException(String.format("%s not suppported",frame.getClass().getName()));
                                        }

                                        //返回应答
                                        String text = ((TextWebSocketFrame) frame).text();
                                        System.out.println(text);
                                        ctx.channel().write(new TextWebSocketFrame("hhahahaah"));
                                    }
                                }

                                @Override
                                public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                                    ctx.flush();
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
