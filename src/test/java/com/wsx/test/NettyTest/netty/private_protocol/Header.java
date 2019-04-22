package com.wsx.test.NettyTest.netty.private_protocol;

import java.util.HashMap;
import java.util.Map;

public final class Header {

    //Netty消息校验码（三部分）
    //1、0xABEF:固定值，表明消息是Netty协议消息，2字节
    //2、主版本号：1~255,1字节
    //3、次版本号：1~255,1字节
    //crcCode=0xABEF+主版本号+次版本号
    private int crcCode = 0xabef0101;

    private int length;// 消息长度

    private long sessionID;// 会话ID

    //0:业务请求消息
    //1:业务响应消息
    //2:业务ONE-WAY消息（既是请求又是响应）
    //3:握手请求消息
    //4:握手应答消息
    //5:心跳请求消息
    //6:心跳应答消息
    private byte type;// 消息类型

    private byte priority;// 消息优先级

    private Map<String, Object> attachment = new HashMap<String, Object>(); // 附件


    public final int getCrcCode() {
        return crcCode;
    }

    public final void setCrcCode(int crcCode) {
        this.crcCode = crcCode;
    }

    public final int getLength() {
        return length;
    }

    public final void setLength(int length) {
        this.length = length;
    }

    public final long getSessionID() {
        return sessionID;
    }


    public final void setSessionID(long sessionID) {
        this.sessionID = sessionID;
    }

    public final byte getType() {
        return type;
    }


    public final void setType(byte type) {
        this.type = type;
    }


    public final byte getPriority() {
        return priority;
    }


    public final void setPriority(byte priority) {
        this.priority = priority;
    }

    public final Map<String, Object> getAttachment() {
        return attachment;
    }


    public final void setAttachment(Map<String, Object> attachment) {
        this.attachment = attachment;
    }

    @Override
    public String toString() {
        return "Header{" +
                "crcCode=" + crcCode +
                ", length=" + length +
                ", sessionID=" + sessionID +
                ", type=" + type +
                ", priority=" + priority +
                ", attachment=" + attachment +
                '}';
    }
}
