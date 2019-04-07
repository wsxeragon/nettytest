package com.wsx.test.NettyTest.java.bio;

import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MyServerHandler implements Runnable{

    private Socket socket = null;

    public MyServerHandler(Socket socket){
        this.socket = socket;
    }

    @Override
    public void run() {
        System.out.println("hahahahhahaah");
        BufferedReader in = null;
        PrintWriter out = null;

        try {
            in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            out = new PrintWriter(this.socket.getOutputStream(),true);

            String inStr = null;
            String outStr = null;
            while(true){
                inStr = in.readLine();
                if(StringUtils.isEmpty(inStr)){
                    break;
                }
                System.out.println(inStr);
                if("QUERY_TIME".equalsIgnoreCase(inStr)){
                    outStr = ""+System.currentTimeMillis();
                }else{
                    outStr="BAD_ORDER";
                }
                out.println(outStr);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(in != null){
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(out != null){
                out.close();
            }
            if(socket != null){
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
