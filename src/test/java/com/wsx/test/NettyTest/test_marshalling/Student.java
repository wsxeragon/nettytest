package com.wsx.test.NettyTest.test_marshalling;

import java.io.Serializable;

public class Student implements Serializable {

    private String name;

    private int no;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNo() {
        return no;
    }

    public void setNo(int no) {
        this.no = no;
    }

    @Override
    public String toString() {
        return "Student{" +
                "name='" + name + '\'' +
                ", no=" + no +
                '}';
    }
}
