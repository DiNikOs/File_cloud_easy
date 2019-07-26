import com.dinikos.file_cloud_easy.common.Command;
import com.dinikos.file_cloud_easy.common.FileList;
import com.dinikos.file_cloud_easy.common.FileMessage;
import com.dinikos.file_cloud_easy.common.FileRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;

public class AuthHandler extends ChannelInboundHandlerAdapter {
    private boolean authOk = false; // false
    public String username = null;
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
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {


        if (authOk) {
            ctx.fireChannelRead(msg);
            return;
        }

        if (msg instanceof Command) {
            String cmd = ((Command) msg).getCommand();
            System.out.println("received CMD");
            System.out.println("cmd= " + cmd);
          //  String cmdDel = ((Command) msg).getFilename();
            String[] tokens = cmd.split(" ");
            if (tokens.length>1) {
                key = tokens[0];
                login = tokens[1];
                pass = tokens[2];
                if (key.equals("/auth")) {
                    if (getNickByLoginAndPass(login, pass)) {
                        //server.subscribe(this);
                        authOk = true;
                        ctx.pipeline().addLast(new MainHandler(login));
                        System.out.println("Auth Ok! ");
                        ctx.writeAndFlush(new Command("/authOk",""));
                    } else {

                    }
                }
                if (key.equals("/sign")) {
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
                if (key.equals("/end")) {
                    if (getNickByLoginAndPass(login, pass)) {
                        //server.subscribe(this);
                        authOk = false;
                        //ctx.pipeline().remove(login);
                        //  remove(new MainHandler(login));
                        System.out.println("disconect! ");
                    } else {

                    }

                }
            }
          //  sendFileList(ctx, msg);
        }
    }


//    public void sendFileList (ChannelHandlerContext ctx, Object msg){
//        try {
//            FileList fl = new FileList(filesList);
//            ctx.writeAndFlush(fl);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        System.out.println("tranfer List OK ");
//    }

    public String getUsername() {
        if (username != null) {
            return username;
        }
        return "Username is not registered";
    }

    public boolean getNickByLoginAndPass(String login, String pass) {
        ResultSet dat = null;
        System.out.println("l/p= " + login + "/" + pass);
        try {
          //  ResultSet rs = stmt.executeQuery(String.format("SELECT nick FROM "  + NAME_TABLE + "where" + " login = '%s' and passwd = '%s'", login, pass));
            String sql = String.format("SELECT nick FROM user where " +
                    "login = '%s' and passwd = '%s'", login, pass);
            ResultSet rs = stmt.executeQuery(sql);
            if(rs.next()) {
                System.out.println("nick= " + rs.getString("nick"));
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            System.out.println("rs= " + dat);
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


}
