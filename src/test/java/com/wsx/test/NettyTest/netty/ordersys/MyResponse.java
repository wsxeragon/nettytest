package com.wsx.test.NettyTest.netty.ordersys;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

public class MyResponse {

    private FullHttpResponse response;

    private Object body;

    public MyResponse(FullHttpResponse response, Object body) {
        this.response = response;
        this.body = body;
    }

    public MyResponse() {
    }

    public FullHttpResponse getResponse() {
        return response;
    }

    public void setResponse(FullHttpResponse response) {
        this.response = response;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }

}
