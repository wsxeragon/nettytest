package com.wsx.test.NettyTest.netty.test_messagepack;

import org.msgpack.annotation.Message;

import java.io.Serializable;

@Message
public class MyInfo implements Serializable {

    private String str1;

    private int int1;

    public String getStr1() {
        return str1;
    }

    public void setStr1(String str1) {
        this.str1 = str1;
    }

    public int getInt1() {
        return int1;
    }

    public void setInt1(int int1) {
        this.int1 = int1;
    }

    @Override
    public String toString() {
        return "MyInfo{" +
                "str1='" + str1 + '\'' +
                ", int1=" + int1 +
                '}';
    }
}
