
import com.dinikos.file_cloud_easy.common.*;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import javafx.application.Platform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

public class MainHandler extends ChannelInboundHandlerAdapter {

    private String nick;
    List<String> filesList;
    File dir1;

    public MainHandler(String username) { // String username
        this.nick =  username;
        dir1 = null;
        System.out.println("MainHandler start" + this.nick);
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected...");
        refreshServerFilesList();
        sendFileList(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("nick= " + nick);
        System.out.println("=============server/" + getNick());
        dir1 = new File("server/" + getNick());
        if(!dir1.exists()) {
            if(dir1.mkdir()) {
                System.out.println("Kaтaлoг " + dir1.getAbsolutePath()
                        + " ycпeшнo coздaн.");
            } else {
                System.out.println("Kaтaлoг " + dir1.getAbsolutePath()
                        + " coздвть нe yдaлocь.");
            }
        } else {
            System.out.println("Kaтaлoг " + dir1.getAbsolutePath()
                    + " yжe cyщecтвyeт.");
        }

        try {
            if (nick.equals("exit")) {
                ctx.close();
                ctx.pipeline().remove(this);
                return;
            }

            if (msg instanceof FileRequest) {
                FileRequest fr = (FileRequest) msg;
                if (Files.exists(Paths.get("server/" + getNick() + "/" + fr.getFilename()))) {
                    FileMessage fm = new FileMessage(Paths.get("server/" + getNick() + "/" + fr.getFilename()));
                    ctx.writeAndFlush(fm);
                    System.out.println("tranfer Obj OK " + fm);
                }
            }
            if (msg instanceof FileMessage) {//
                FileMessage fm = (FileMessage) msg;
                Files.write(Paths.get("server/" + getNick() + "/" + fm.getFilename()),
                        fm.getData(), StandardOpenOption.CREATE);
                refreshServerFilesList();
                sendFileList(ctx);
                System.out.println("Obj OK " + fm);
                }
            if (msg instanceof Command) {
                String cmd = ((Command) msg).getCommand();
                String cmdDel = ((Command) msg).getFilename();
                System.out.println("SendCMD" + cmd);
                String[] tokens = cmd.split(" ");
                String key = tokens[0];
                if (key.equals("/auth")){
                   // System.out.println("listLen= " + filesList.size());
                    System.out.println("get ServerList OK ");
                }
                if (key.equals("/end_login")||key.equals("/exit")) {
                    final ChannelFuture f = ctx.writeAndFlush(new Command("/exit",""));
                    ctx.fireChannelRead(msg);
                    f.addListener(ChannelFutureListener.CLOSE);
                    System.out.println("=========Client disconectMH!=========== ");
                    ctx.close();
                    ctx.pipeline().remove(this);
                    return;
                }
                if (key.equals("/delFile")){
                    System.out.println("SendCmdDel= " + cmd);
                    if (cmdDel!=null) {
                        System.out.println("del tryth= " + cmdDel);
                        File file = new File("server/" + getNick() + "/" + cmdDel);
                        if( file.delete()){
                            System.out.println("server/" + getNick() + "/" + cmdDel + " файл удален");
                        } else {
                            System.out.println("Файла" +  cmdDel + " не обнаружен");
                        }
                    }
                    System.out.println("Del List OK ");
                }
                refreshServerFilesList();
                sendFileList(ctx);
            }

            if (msg == null) {
                System.out.println("msg_Null");
                return;
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        System.out.println("ctx_close_Exception");
        ctx.close();
    }

    public void refreshServerFilesList() {
        File dir = new File("server/" + getNick() + "/"); //path указывает на директорию
        System.out.println("server_user= " + getNick());
        String[] arrFiles = dir.list();
        filesList = Arrays.asList(arrFiles);
        //System.out.println("listLen= " + filesList.size());
//        for (String file : filesList) {
//            System.out.println(file);
//        }
    }

    public void sendFileList (ChannelHandlerContext ctx){
        try {
            FileList fl = new FileList(filesList);
            ctx.writeAndFlush(fl);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("tranfer List OK ");
    }

    public static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    public String getNick() {
        return this.nick;
    }

    public void sendMsg(ChannelHandlerContext ctx, Object msg) {
        ctx.writeAndFlush(msg);
    }
}
