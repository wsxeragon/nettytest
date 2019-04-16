package com.wsx.test.NettyTest.netty.filesys;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Pattern;

public class MyServer1 {


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
                            socketChannel.pipeline().addLast("http-decoder", new HttpRequestDecoder());
                            socketChannel.pipeline().addLast("http-aggregator", new HttpObjectAggregator(65536));
                            socketChannel.pipeline().addLast("http-encoder", new HttpResponseEncoder());
                            socketChannel.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
                            socketChannel.pipeline().addLast(new SimpleChannelInboundHandler<FullHttpRequest>() {
                                @Override
                                protected void messageReceived(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
                                    if(!msg.decoderResult().isSuccess()){
                                        sendError(ctx, HttpResponseStatus.BAD_REQUEST);
                                        return;
                                    }
                                    if(msg.method() != HttpMethod.GET){
                                        sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
                                        return;
                                    }

                                    String uri = msg.uri();
                                    if (!uri.contains("hello")){
                                        sendError(ctx, HttpResponseStatus.BAD_REQUEST);
                                        return;
                                    }
                                    FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=UTF-8");
                                    StringBuffer sb = new StringBuffer();
                                    sb.append("<!DOCTYPE html>\r\n");
                                    sb.append("<html><head><title>");
                                    sb.append("hello world");
                                    sb.append("</title></head><body>\r\n");
                                    sb.append("哈哈哈哈哈哈哈啊哈");
                                    sb.append("</body></html>\r\n");
                                    response.content().writeBytes(Unpooled.copiedBuffer(sb,CharsetUtil.UTF_8));
                                    if(HttpHeaderUtil.isKeepAlive(msg)){
                                        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                                    }

                                    ChannelFuture future = ctx.writeAndFlush(response);
                                    future.addListener(new ChannelProgressiveFutureListener() {

                                        @Override
                                        public void operationComplete(ChannelProgressiveFuture future)
                                                throws Exception {
                                            System.out.println("Transfer complete.");

                                        }

                                        @Override
                                        public void operationProgressed(ChannelProgressiveFuture future,
                                                                        long progress, long total) throws Exception {
                                            if(total < 0)
                                                System.err.println("Transfer progress: " + progress);
                                            else
                                                System.err.println("Transfer progress: " + progress + "/" + total);
                                        }
                                    });

                                    ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
                                    if(!HttpHeaderUtil.isKeepAlive(msg))
                                        lastContentFuture.addListener(ChannelFutureListener.CLOSE);
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                                        throws Exception {
                                    cause.printStackTrace();
                                    if(ctx.channel().isActive())
                                        sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
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



    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status){
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
                Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }










}
