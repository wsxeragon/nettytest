package com.wsx.test.NettyTest.netty.ordersys;

import io.netty.handler.codec.http.FullHttpRequest;

public class MyRequest {

    private FullHttpRequest request;

    private Object body;

    public MyRequest(FullHttpRequest request, Object body) {
        this.request = request;
        this.body = body;
    }

    public MyRequest() {
    }

    public FullHttpRequest getRequest() {
        return request;
    }

    public void setRequest(FullHttpRequest request) {
        this.request = request;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }

}
