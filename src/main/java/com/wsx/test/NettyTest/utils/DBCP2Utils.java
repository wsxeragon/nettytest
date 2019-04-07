 /**
 * Alipay.com Inc. Copyright (c) 2004-2019 All Rights Reserved.
 */
package com.wsx.test.NettyTest.utils;

import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author wb-wsx452256
 * @version $Id: DBCP2Utils.java, v 0.1 2019年03月22日 15:25 wb-wsx452256 Exp $
 */
public class DBCP2Utils {

    private static BasicDataSource dataSource =new BasicDataSource();
    static{
        try{
            dataSource.setUrl("");
            dataSource.setUsername("");
            dataSource.setPassword("");
            dataSource.setDriverClassName("");
            dataSource.setMaxTotal(30);
            dataSource.setMaxIdle(10);
            dataSource.setMinIdle(5);
            dataSource.setMaxWaitMillis(10000);
            dataSource.setRemoveAbandonedTimeout(100);
            dataSource.setRemoveAbandonedOnBorrow(true);
            dataSource.setRemoveAbandonedOnMaintenance(true);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public static Connection getConnection(){
        Connection connection = null;
        try{
            connection = dataSource.getConnection();
        }catch(SQLException e){
            e.printStackTrace();
        }
        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public static void release(Connection conn, PreparedStatement ps, ResultSet rs){
        if(rs!=null){
            try{
                rs.close();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(ps!=null){
            try{
                ps.close();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(conn!=null){
            try{
                conn.close();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}