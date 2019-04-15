package com.wsx.test.NettyTest.netty.testcodec;

import com.wsx.test.NettyTest.netty.test_proto.PersonOuterClass;
import org.junit.Test;
import org.msgpack.MessagePack;
import org.msgpack.template.Templates;

import java.util.ArrayList;
import java.util.List;

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
}
