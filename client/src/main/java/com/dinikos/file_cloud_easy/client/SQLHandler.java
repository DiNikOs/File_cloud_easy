package com.dinikos.file_cloud_easy.client;

import java.sql.*;

public class SQLHandler {
    private static Connection connection;
    private static Statement stmt;
    private static PreparedStatement psInsert;

    public static boolean conection () {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:cloud_base.db");
            stmt = connection.createStatement();
            return true;
            } catch (ClassNotFoundException | SQLException  e) {
                e.printStackTrace();
            }
        return false;
    }

    public static String getNameByLogin (String login, String password) {
        String nick = null;
        try {
            ResultSet rs = stmt.executeQuery("SELECT nickname FROM user WHERE login = '" + login + "' AND password = '" + password + "';");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nick;

    }

    public static void disconect () {
        try {
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
