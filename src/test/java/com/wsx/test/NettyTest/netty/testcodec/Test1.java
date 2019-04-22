package com.wsx.test.NettyTest.netty.testcodec;

import com.wsx.test.NettyTest.netty.private_protocol.Header;
import com.wsx.test.NettyTest.netty.private_protocol.MyClient;
import com.wsx.test.NettyTest.netty.private_protocol.NettyMessage;
import com.wsx.test.NettyTest.netty.test_proto.PersonOuterClass;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.msgpack.MessagePack;
import org.msgpack.template.Templates;

import javax.print.DocFlavor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Test1 {

    @Test
    public void test1() throws  Exception{
        PersonOuterClass.Person.Builder personBuilder = PersonOuterClass.Person.newBuilder();
        personBuilder.setEmail("test@gmail.com");
        personBuilder.setId(1000);
        personBuilder.setName("张三");
        List<String> names= new ArrayList<>();
        names.add("tim");
        names.add("tom");
        names.add("tommy");
        personBuilder.addAllNickname(names);
        PersonOuterClass.Person person = personBuilder.build();

        //第一种方式
        //序列化
        byte[] data = person.toByteArray();//获取字节数组，适用于SOCKET或者保存在磁盘。
        //反序列化
        PersonOuterClass.Person result = PersonOuterClass.Person.parseFrom(data);
        System.out.println(result);
    }

    @Test
    public void test12() throws  Exception{
       List<String> list0 = new ArrayList<>();

       list0.add("1");
       list0.add("2");
       list0.add("3");

        MessagePack messagePack = new MessagePack();
       byte[] bs = messagePack.write(list0);

       List<String> list1 = messagePack.read(bs, Templates.tList(Templates.TString));
       System.out.println(list1);

    }

    @Test
    public void test1w2() throws  Exception{
        ByteBuf buf = Unpooled.buffer();

        buf.writeByte(1);
        buf.writeInt(22);
        buf.writeLong(3333L);
        byte[] bs1 = "haha".getBytes("UTF-8");
        buf.writeInt(bs1.length);
        buf.writeBytes(bs1);


        System.out.println(buf.readByte());
        System.out.println(buf.readInt());
        System.out.println(buf.readLong());
        byte[] bs = new byte[buf.readInt()];
        buf.readBytes(bs);
        System.out.println(new String(bs,"UTF-8"));

    }


    @Test
    public void test123(){
          ScheduledExecutorService executor1 = Executors
                .newScheduledThreadPool(1);
        executor1.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                    System.out.println(System.currentTimeMillis()/1000);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, 1000, 1000,TimeUnit.MILLISECONDS);

        try {
            Thread.sleep(100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
