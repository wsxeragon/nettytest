package com.wsx.test.NettyTest.netty.ordersys;

import com.alibaba.fastjson.JSON;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

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
                            //自带解析httpResponse
                            socketChannel.pipeline().addLast(new HttpResponseDecoder());
                            //将分散得http请求组合成完整的一个http请求
                            socketChannel.pipeline().addLast(new HttpObjectAggregator(65536));
                            //自定义解析xml为pojo
                             socketChannel.pipeline().addLast(new MyClient.HttpXmlResponseDecoder());

                             //自带编码httpRequest
                            socketChannel.pipeline().addLast(new HttpRequestEncoder());
                            //自定义将pojo转换 为xml
                            socketChannel.pipeline().addLast(new MyClient.HttpXmlRequestEncoder());
                            socketChannel.pipeline().addLast(new SimpleChannelInboundHandler<MyResponse>(){

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    Order order = new Order();
                                    order.setOrderNumber(123);
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

                                    MyRequest request = new MyRequest(null, order);
                                    ctx.writeAndFlush(request);
                                }

                                @Override
                                protected void messageReceived(ChannelHandlerContext ctx, MyResponse msg) throws Exception {
                                    System.out.println(JSON.toJSON(msg));

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


    private static class HttpXmlRequestEncoder extends MessageToMessageEncoder<MyRequest>{
        @Override
        protected void encode(ChannelHandlerContext ctx, MyRequest msg, List<Object> out) throws Exception {
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
            FullHttpRequest request = msg.getRequest();
            // 如request为空，则新建一个FullHttpRequest对象，并将设置消息头
            if (request == null) {
                // 在构造方法中，将body设置为请求消息体
                request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/do", body);
                HttpHeaders headers = request.headers();
                // 表示请求的服务器网址
                headers.set(HttpHeaderNames.HOST, InetAddress.getLocalHost().getHostAddress());
                // Connection表示客户端与服务连接类型；Keep-Alive表示长连接；CLOSE表示短连接
                // header中包含了值为close的connection，都表明当前正在使用的tcp链接在请求处理完毕后会被断掉。
                // 以后client再进行新的请求时就必须创建新的tcp链接了。
                headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
                // 浏览器支持的压缩编码是 gzip 和 deflate
                headers.set(HttpHeaderNames.ACCEPT_ENCODING,
                        HttpHeaderValues.GZIP.toString() + ',' + HttpHeaderValues.DEFLATE.toString());
                // 浏览器支持的解码集
                headers.set(HttpHeaderNames.ACCEPT_CHARSET, "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
                // 浏览器支持的语言
                headers.set(HttpHeaderNames.ACCEPT_LANGUAGE, "zh");
                // 使用的用户代理是 Netty xml Http Client side
                headers.set(HttpHeaderNames.USER_AGENT, "Netty xml Http Client side");
                // 浏览器支持的 MIME类型,优先顺序为从左到右
                headers.set(HttpHeaderNames.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            }
            // 由于此处没有使用chunk方式，所以要设置消息头中设置消息体的CONTENT_LENGTH
            request.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, body.readableBytes());
            // 将请求消息添加进out中，待后面的编码器对消息进行编码
            out.add(request);

        }
    }

    private static class HttpXmlResponseDecoder extends MessageToMessageDecoder<FullHttpResponse> {
        @Override
        protected void decode(ChannelHandlerContext ctx, FullHttpResponse msg, List<Object> out) throws Exception {


            XStream xStream = new XStream(new DomDriver());
            xStream.setMode(XStream.NO_REFERENCES);
            xStream.processAnnotations(Order.class);
            xStream.processAnnotations(Customer.class);
            xStream.processAnnotations(Address.class);

            XStream.setupDefaultSecurity(xStream);
            xStream.allowTypes(new Class[]{Order.class,Customer.class,Address.class,Order.Shipping.class});
            xStream.setClassLoader(Order.class.getClassLoader());
            Order body =(Order)xStream.fromXML(msg.content().toString(CharsetUtil.UTF_8));
            MyResponse myResponse = new MyResponse(msg,body);
            out.add(myResponse);
        }
    }

}
