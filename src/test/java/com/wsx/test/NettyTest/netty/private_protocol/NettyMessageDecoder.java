package com.wsx.test.NettyTest.netty.private_protocol;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.util.HashMap;
import java.util.Map;

/**
 * Netty消息解码类
 * @author chenwei
 * @create 2018-07-02 16:31
 *
 * 继承LengthFieldBasedFrameDecoder是为了更好了使用它对tcp的粘包和半包处理，
 * 只需要给我表示消息长度的字段偏移量和消息长度自身所占的字节数，该解码器就能
 * 自动实现对半包的处理，调用父类LengthFieldBasedFrameDecoder的decode方法后，
 * 返回的就是整包消息或者为null,
 **/
public class NettyMessageDecoder extends LengthFieldBasedFrameDecoder {

    private MarshallingCodecFactory.MyMarshallingDecoder myMarshallingDecoder= null;

    public NettyMessageDecoder() {
        super(1024*1024, 4, 4);
        myMarshallingDecoder = MarshallingCodecFactory.buildMarshallingDecoder();
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame =  (ByteBuf)super.decode(ctx, in);
        if(frame ==null){
            return null;
        }
        NettyMessage message =new NettyMessage();
        Header header = new Header();
        header.setCrcCode(frame.readInt());
        header.setLength(frame.readInt());
        header.setSessionID(frame.readLong());
        header.setType(frame.readByte());
        header.setPriority(frame.readByte());
        int size= frame.readInt();
        if (size>0){
            int len = 0;
            byte[] bytes =  null;
            Map<String, Object> attch = new HashMap<String, Object>();

            for(int i=0;i<size;i++){
                len = frame.readInt();
                bytes = new byte[len];
                frame.readBytes(bytes);
                attch.put(new String(bytes,"UTF-8"),myMarshallingDecoder.decode(ctx,frame));

            }
            header.setAttachment(attch);
        }
        //readableBytes即为判断剩余可读取的字节数（ this.writerIndex - this.readerIndex）
        //大于4说明有消息体（无消息体时readableBytes=4），故进行解码
        if (frame.readableBytes() > 4) {
            message.setBody(myMarshallingDecoder.decode(ctx, frame));
        }
        message.setHeader(header);
        return message;
    }
}
