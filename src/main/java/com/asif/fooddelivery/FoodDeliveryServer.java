package com.asif.fooddelivery;

import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.stream.Collectors;

public class FoodDeliveryServer {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/api/menu")) {
                send(exchange, 200, getMenuJson(), "application/json");
            } else if (path.equals("/api/orders") && exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                        .lines().collect(Collectors.joining("\n"));
                saveOrder(body);
                send(exchange, 201, "{\"message\":\"Order received\"}", "application/json");
            } else {
                sendFile(exchange, "index.html");
            }
        });
        server.start();
        System.out.println("Food Delivery app running at http://localhost:8080");
    }

    private static String getMenuJson() {
        String url=System.getenv().getOrDefault("DB_URL","jdbc:mysql://localhost:3306/food_delivery");
        String user=System.getenv().getOrDefault("DB_USER","root");
        String pass=System.getenv().getOrDefault("DB_PASSWORD","");
        StringBuilder json=new StringBuilder("[");
        try(Connection c=DriverManager.getConnection(url,user,pass);
            PreparedStatement p=c.prepareStatement("SELECT id,name,price,category FROM menu ORDER BY id");
            ResultSet r=p.executeQuery()){
            while(r.next()){
                if(json.length()>1)json.append(",");
                json.append(String.format("{\"id\":%d,\"name\":\"%s\",\"price\":%.2f,\"category\":\"%s\"}",
                    r.getInt("id"),esc(r.getString("name")),r.getDouble("price"),esc(r.getString("category"))));
            }
        }catch(SQLException e){return "[{\"error\":\"Database connection failed. Check DB settings.\"}]";}
        return json.append("]").toString();
    }

    private static void saveOrder(String body)throws SQLException{
        String url=System.getenv().getOrDefault("DB_URL","jdbc:mysql://localhost:3306/food_delivery");
        String user=System.getenv().getOrDefault("DB_USER","root");
        String pass=System.getenv().getOrDefault("DB_PASSWORD","");
        try(Connection c=DriverManager.getConnection(url,user,pass);
            PreparedStatement p=c.prepareStatement("INSERT INTO orders(customer_name,items,total_amount) VALUES(?,?,?)")){
            String name=body.replaceAll(".*\"customerName\"\\s*:\\s*\"([^\"]*)\".*","$1");
            String items=body.replaceAll(".*\"items\"\\s*:\\s*\"([^\"]*)\".*","$1");
            String total=body.replaceAll(".*\"total\"\\s*:\\s*([0-9.]+).*","$1");
            p.setString(1,name);p.setString(2,items);p.setDouble(3,Double.parseDouble(total));p.executeUpdate();
        }
    }
    private static void sendFile(com.sun.net.httpserver.HttpExchange e,String name)throws IOException{
        try(InputStream in=FoodDeliveryServer.class.getResourceAsStream("/public/"+name)){
            if(in==null){send(e,404,"Not found","text/plain");return;}
            byte[] data=in.readAllBytes();e.getResponseHeaders().set("Content-Type","text/html; charset=UTF-8");
            e.sendResponseHeaders(200,data.length);e.getResponseBody().write(data);e.close();
        }
    }
    private static void send(com.sun.net.httpserver.HttpExchange e,int status,String body,String type)throws IOException{
        byte[] data=body.getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type",type+"; charset=UTF-8");
        e.sendResponseHeaders(status,data.length);e.getResponseBody().write(data);e.close();
    }
    private static String esc(String s){return s.replace("\\","\\\\").replace("\"","\\\"");}
}