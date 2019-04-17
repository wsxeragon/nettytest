package com.wsx.test.NettyTest.netty.ordersys;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.junit.Test;

import java.util.Arrays;

public class Test12 {
    @Test
    public void test1(){
        Order order = new Order();
        order.setOrderNumber(123);
        Customer customer = new Customer();
        customer.setFirstName("ali");
        customer.setMiddleNames(Arrays.asList("baba"));
        customer.setLastName("taobao");
        order.setCustomer(customer);
        Address address = new Address();
        address.setCity("南京市");
        address.setCountry("中国");
        address.setPostCode("123321");
        address.setState("江苏省");
        address.setStreet1("龙眠大道");
        address.setStreet2("INTERNATIONAL_MAIL");
        order.setBillTo(address);
        order.setShipTo(address);
        order.setShipping(Order.Shipping.INTERNATIONAL_MAIL);
        order.setTotal(33f);

        XStream xStream = new XStream(new DomDriver());
        xStream.setMode(XStream.NO_REFERENCES);
        xStream.processAnnotations(Order.class);
        xStream.processAnnotations(Customer.class);
        xStream.processAnnotations(Address.class);
        String s = xStream.toXML(order);
        System.out.println(s);



        XStream.setupDefaultSecurity(xStream);
        xStream.allowTypes(new Class[]{Order.class,Customer.class,Address.class,Order.Shipping.class});
        xStream.setClassLoader(Order.class.getClassLoader());
        Order o =(Order)xStream.fromXML(s);
        System.out.println(o);


    }


}
