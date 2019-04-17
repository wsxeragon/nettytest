package com.wsx.test.NettyTest.netty.filesys;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.string.StringDecoder;
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

public class MyServer {

    private static final String path = "/src/";

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

                                    final String uri = msg.uri();
                                    //将uri转换为文件路径
                                    final String path0 = sanitizeUri(uri);
                                    if(path0 == null)
                                    {
                                        sendError(ctx, HttpResponseStatus.FORBIDDEN);
                                        return;
                                    }
                                    File file = new File(path0);
                                    if(file.isHidden() || !file.exists())
                                    {
                                        sendError(ctx, HttpResponseStatus.NOT_FOUND);
                                        return;
                                    }

                                    if(file.isDirectory())
                                    {
                                        if(uri.endsWith("/"))
                                        {
                                            sendListing(ctx, file);
                                        }else{
                                            sendRedirect(ctx, uri + "/");
                                        }
                                        return;
                                    }
                                    if(!file.isFile())
                                    {
                                        sendError(ctx, HttpResponseStatus.FORBIDDEN);
                                        return;
                                    }

                                    RandomAccessFile randomAccessFile = null;
                                    try{
                                        randomAccessFile = new RandomAccessFile(file, "r");
                                    }catch(FileNotFoundException fnfd){
                                        sendError(ctx, HttpResponseStatus.NOT_FOUND);
                                        return;
                                    }

                                    long fileLength = randomAccessFile.length();
                                    HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                                    HttpHeaderUtil.setContentLength(response, fileLength);
                                    MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();
                                    System.out.println(mimetypesFileTypeMap.getContentType(file.getPath()));
                                    //"application/octet-stream"
                                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, mimetypesFileTypeMap.getContentType(file.getPath()));



                                    if(HttpHeaderUtil.isKeepAlive(msg)){
                                        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                                    }

                                    ctx.write(response);
                                    ChannelFuture sendFileFuture = ctx.write(new ChunkedFile(randomAccessFile, 0, fileLength, 8192), ctx.newProgressivePromise());
                                    sendFileFuture.addListener(new ChannelProgressiveFutureListener() {

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


    private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");
    private static String sanitizeUri(String uri){
        try{
            uri = URLDecoder.decode(uri, "UTF-8");
        }catch(UnsupportedEncodingException e){
            try{
                uri = URLDecoder.decode(uri, "ISO-8859-1");
            }catch(UnsupportedEncodingException e1){
                throw new Error();
            }
        }

        if(!uri.startsWith(path))
            return null;
        if(!uri.startsWith("/"))
            return null;

        uri = uri.replace('/', File.separatorChar);
        if(uri.contains(File.separator + '.') || uri.contains('.' + File.separator) || uri.startsWith(".") || uri.endsWith(".")
                || INSECURE_URI.matcher(uri).matches()){
            return null;
        }
        System.out.println(System.getProperty("user.dir"));
        return System.getProperty("user.dir") + File.separator + uri;
    }

    private static final Pattern ALLOWED_FILE_NAME = Pattern.compile("[A-Za-z0-9][-_A-Za-z0-9\\.]*");
    private static void sendListing(ChannelHandlerContext ctx, File dir){
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
//        response.headers().set("CONNECT_TYPE", "text/html;charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=UTF-8");

        String dirPath = dir.getPath();
        StringBuilder buf = new StringBuilder();

        buf.append("<!DOCTYPE html>\r\n");
        buf.append("<html><head><title>");
        buf.append(dirPath);
        buf.append("目录:");
        buf.append("</title></head><body>\r\n");

        buf.append("<h3>");
        buf.append(dirPath).append(" 目录：");
        buf.append("</h3>\r\n");
        buf.append("<ul>");
        buf.append("<li>链接：<a href=\" ../\">..</a></li>\r\n");
        for (File f : dir.listFiles()) {
            if(f.isHidden() || !f.canRead()) {
                continue;
            }
            String name = f.getName();
            if (!ALLOWED_FILE_NAME.matcher(name).matches()) {
                continue;
            }

            buf.append("<li>链接：<a href=\"");
            buf.append(name);
            buf.append("\">");
            buf.append(name);
            buf.append("</a></li>\r\n");
        }

        buf.append("</ul></body></html>\r\n");
        System.out.println(buf.toString());
        ByteBuf buffer = Unpooled.copiedBuffer(buf,CharsetUtil.UTF_8);
        response.content().writeBytes(buffer);
        buffer.release();
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }


    private static void sendRedirect(ChannelHandlerContext ctx, String newUri){
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
//        response.headers().set("LOCATIN", newUri);
        response.headers().set(HttpHeaderNames.LOCATION, newUri);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }



}
