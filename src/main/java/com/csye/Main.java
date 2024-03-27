package com.csye;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Main implements HttpFunction {
    private static final String API_KEY = "ad928d31583fce4488fa735dd5503710-f68a26c9-e217c4e7";
    private static final String DOMAIN = "sandbox3ed133689fbe48f586ebcb9175847375.mailgun.org";


    @Override
    public void service(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        try {
            String url = "jdbc:postgresql://localhost:5432/NEU";
            String user = "postgres";
            String password = "Joseph5#";
            Class.forName("org.postgresql.Driver");
            Connection conn = DriverManager.getConnection(url, user, password);

            String sql = "select id from person where username ='janedoe1@example.com'";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);

            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                String id = rs.getString("id");
                System.out.println("ID: " + id);
            }
            conn.close();

            System.out.println("Connection closed");
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}