package com.wsx.test.NettyTest.netty.private_protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.marshalling.MarshallingEncoder;
import io.netty.util.CharsetUtil;

import java.util.List;
import java.util.Map;

public class NettyMessageEncoder extends MessageToMessageEncoder<NettyMessage> {

    private MarshallingCodecFactory.MyMarshallingEncoder myMarshallingEncoder;

    public NettyMessageEncoder(){
        myMarshallingEncoder= MarshallingCodecFactory.buildMarshallingEncoder();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, NettyMessage msg, List<Object> out) throws Exception {
        if(msg ==null || msg.getHeader() == null){
            throw new Exception("The encode message is null");
        }

        ByteBuf bf = Unpooled.buffer();
        //按顺利编码后，根据定义的字段数据类型写入ByteBuf,解码时也要按顺序挨个取出
        bf.writeInt(msg.getHeader().getCrcCode());
        bf.writeInt(msg.getHeader().getLength());
        bf.writeLong(msg.getHeader().getSessionID());
        bf.writeByte(msg.getHeader().getType());
        bf.writeByte(msg.getHeader().getPriority());
        bf.writeInt(msg.getHeader().getAttachment().size());
        String key=null;
        Object value=null;
        for(Map.Entry<String,Object> entry:msg.getHeader().getAttachment().entrySet()){
            key = entry.getKey();
            value = entry.getValue();

            bf.writeInt(key.length());
            bf.writeBytes(key.getBytes(CharsetUtil.UTF_8));
            myMarshallingEncoder.encode(ctx,value,bf);
        }

        if (msg.getBody() != null) {
            //使用MarshallingEncoder编码消息体
            myMarshallingEncoder.encode(ctx,msg.getBody(),bf);
        }else {
            //没有消息体的话，就赋予0值
            bf.writeInt(0);
        }

        //更新消息长度字段的值，至于为什么-8，是因为8是长度字段后的偏移量，LengthFieldBasedFrameDecoder的源码中
        //对长度字段和长度的偏移量之和做了判断，如果不-8，会导致LengthFieldBasedFrameDecoder解码返回null
        //这是 《Netty权威指南》中的写错的地方
        bf.setInt(4, bf.readableBytes()-8);
        //书中此处没有add，也即没有将ByteBuf加入到List中，也就没有消息进行编码了，所以导致运行了没有效果……
        out.add(bf);

    }


}
