package com.wsx.test.NettyTest.netty.ordersys;

import com.alibaba.fastjson.JSON;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

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
                            socketChannel.pipeline().addLast(new HttpRequestDecoder());
                            socketChannel.pipeline().addLast(new HttpObjectAggregator(65536));
                            socketChannel.pipeline().addLast(new MyServer.HttpXmlRequestDecoder());

                            socketChannel.pipeline().addLast(new HttpResponseEncoder());
                            socketChannel.pipeline().addLast(new MyServer.HttpXmlResponseEncoder());
                            socketChannel.pipeline().addLast(new SimpleChannelInboundHandler<MyRequest>() {

                                @Override
                                protected void messageReceived(ChannelHandlerContext ctx, MyRequest msg) throws Exception {
                                    System.out.println(JSON.toJSON(msg));

                                    Order order = new Order();
                                    order.setOrderNumber(321);
                                    Customer customer = new Customer();
                                    customer.setFirstName("ali");
                                    customer.setMiddleNames(Arrays.asList("baba"));
                                    customer.setLastName("taobao");
                                    order.setCustomer(customer);
                                    Address address = new Address();
                                    address.setCity("南京市");
                                    address.setCountry("中国");
                                    address.setPostCode("123321");
                                    address.setState("江苏省");
                                    address.setStreet1("龙眠大道");
                                    address.setStreet2("INTERNATIONAL_MAIL");
                                    order.setBillTo(address);
                                    order.setShipTo(address);
                                    order.setShipping(Order.Shipping.INTERNATIONAL_MAIL);
                                    order.setTotal(33f);

                                    MyResponse response = new MyResponse(null, order);
                                    ctx.writeAndFlush(response);
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

                                    if(ctx.channel().isActive()){
                                        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,HttpResponseStatus.BAD_REQUEST,Unpooled.copiedBuffer("Failure\r\n",CharsetUtil.UTF_8));
                                        response.headers().set(HttpHeaderNames.CONTENT_TYPE,"text/plain;charset=UTF-8");
                                        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

                                    }
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


    private static class HttpXmlResponseEncoder extends MessageToMessageEncoder<MyResponse> {
        @Override
        protected void encode(ChannelHandlerContext ctx, MyResponse msg, List<Object> out) throws Exception {
            //将pojo转换为xml
            XStream xStream = new XStream(new DomDriver());
            xStream.setMode(XStream.NO_REFERENCES);
            xStream.processAnnotations(Order.class);
            xStream.processAnnotations(Customer.class);
            xStream.processAnnotations(Address.class);
            String s = xStream.toXML(msg.getBody());
            System.out.println(s);
            ByteBuf body = Unpooled.copiedBuffer(s, CharsetUtil.UTF_8);

            //构造请求
            FullHttpResponse response = msg.getResponse();
            // 如request为空，则新建一个FullHttpRequest对象，并将设置消息头
            if (response == null) {
                response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, body);
            } else {
                response = new DefaultFullHttpResponse(msg.getResponse().protocolVersion(),
                        msg.getResponse().status(), body);
            }
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/xml");
            // 由于此处没有使用chunk方式，所以要设置消息头中设置消息体的CONTENT_LENGTH
            response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, body.readableBytes());
            // 将请求消息添加进out中，待后面的编码器对消息进行编码
            out.add(response);

        }
    }

    private static class HttpXmlRequestDecoder extends MessageToMessageDecoder<FullHttpRequest> {
        @Override
        protected void decode(ChannelHandlerContext ctx, FullHttpRequest msg, List<Object> out) throws Exception {
            if(!msg.decoderResult().isSuccess()){
                FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,HttpResponseStatus.BAD_REQUEST,Unpooled.copiedBuffer("Failure\r\n",CharsetUtil.UTF_8));
                response.headers().set(HttpHeaderNames.CONTENT_TYPE,"text/plain;charset=UTF-8");
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }
            XStream xStream = new XStream(new DomDriver());
            xStream.setMode(XStream.NO_REFERENCES);
            xStream.processAnnotations(Order.class);
            xStream.processAnnotations(Customer.class);
            xStream.processAnnotations(Address.class);

            XStream.setupDefaultSecurity(xStream);
            xStream.allowTypes(new Class[]{Order.class,Customer.class,Address.class,Order.Shipping.class});
            xStream.setClassLoader(Order.class.getClassLoader());
            System.out.println(msg.content().toString(CharsetUtil.UTF_8));
            Order body =(Order)xStream.fromXML(msg.content().toString(CharsetUtil.UTF_8));
            MyRequest myRequest = new MyRequest(msg,body);
            out.add(myRequest);
        }
    }
}
