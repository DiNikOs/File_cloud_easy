import com.dinikos.file_cloud_easy.common.Command;
import com.dinikos.file_cloud_easy.common.FileList;
import com.dinikos.file_cloud_easy.common.FileMessage;
import com.dinikos.file_cloud_easy.common.FileRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Comparator;

public class AuthHandler extends ChannelInboundHandlerAdapter {
    private boolean authOk = false; // false
    protected String username;
    private Server server;
    public String key, login, pass;
    final String NAME_TABLE = "user";

    private static Connection connection;
    private static Statement stmt;

    public static void connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:cloud_base.db");
            stmt = connection.createStatement();
            System.out.println("Base_connect!");

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Auth connected...");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg instanceof Command) {
            String cmd = ((Command) msg).getCommand();
            System.out.println("isAuth CMD" + cmd);
            String[] tokens = cmd.split(" ");
            if (tokens.length > 0) {
                key = tokens[0];
                if (key.equals("/init")) {
                    System.out.println("Client connect! ");
                }
                if (key.equals("/end_login")||key.equals("/sign")) {
                    authOk = false;
                    ctx.writeAndFlush(new Command("/exit",""));
                    ctx.fireChannelRead(msg);
                    System.out.println("disconectAH! ");
                }
                if (key.equals("/exit")) {
                    authOk = false;
                    System.out.println("Client close! ");
                    ctx.pipeline().addLast(new MainHandler(key));
                    ctx.fireChannelRead(msg);
                }
                if (key.equals("/remove")) {
                    if (!authOk) { // && getNickByLoginAndPass(tokens[1], tokens[2])
                        try {
                            int del = stmt.executeUpdate(
                                    String.format("DELETE from " + NAME_TABLE +
                                            " WHERE login = '%s' and passwd = '%s'", login, pass));
                            System.out.println("delUser= " + del);
                            if (del > 0) {
                                File file = new File("server/" + tokens[1]);
                                deleteFile(file);
                                System.out.println("remove Ok! ");
                            } else System.out.println("not Remove!");
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {

                        }
                    } else  ctx.writeAndFlush(new Command("/needDiscon",""));
                }
            }
        }

        if (authOk) {
            ctx.writeAndFlush(new Command("/AuthHandlerWork_return",""));
            ctx.fireChannelRead(msg);
            return;
        } else {
            ctx.writeAndFlush(new Command("/AuthHandlerWork",""));
        }

        if (msg instanceof Command) {
            String cmd = ((Command) msg).getCommand();
            System.out.println("received cmd= " + cmd);
            String[] tokens = cmd.split(" ");
            if (tokens.length>1) {
                key = login = pass = null;
                key = tokens[0];
                login = tokens[1];
                pass = tokens[2];
                if (key.equals("/auth")) {
                    if (getNickByLoginAndPass(login, pass)) {
                        System.out.println("log/pass=" + login + "/" + pass);
                        authOk = true;
                        ctx.writeAndFlush(new Command("/authOk",""));
                        ctx.pipeline().addLast(new MainHandler(login));
                        ctx.fireChannelRead(msg);
                        System.out.println("Client Auth Ok! ");
                    } else {
                        ctx.writeAndFlush(new Command("/authNOK",""));
                        System.out.println("send NOK");
                    }
                } else if (key.equals("/sign")) {
                    try {
                        stmt.executeUpdate("INSERT INTO " + NAME_TABLE +
                                " (login, passwd) " +
                                "VALUES ('" + login + "', '" + pass + "');");
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        System.out.println("Sign Ok! ");
                    }
                }
            }
            System.out.println("AuthHand= " + authOk);
        }
    }

    public String getUsername() {
        if (username != null) {
            return username;
        }
        return "Username is not registered";
    }

    public boolean getNickByLoginAndPass(String login, String pass) {
        System.out.println("l/p= " + login + "/" + pass);
        try {
            String sql = String.format("SELECT nick FROM user where " +
                    "login = '%s' and passwd = '%s'", login, pass);
            ResultSet rs = stmt.executeQuery(sql);
            if(rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {

        }
        return false;
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteFile(File element) {
        if (element.isDirectory()) {
            for (File sub : element.listFiles()) {
                deleteFile(sub);
            }
        }
        element.delete();
    }

}
