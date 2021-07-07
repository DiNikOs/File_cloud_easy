
import com.dinikos.file_cloud_easy.common.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import io.netty.util.ReferenceCountUtil;
import javafx.application.Platform;
import javafx.scene.control.ListView;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MainHandler extends ChannelInboundHandlerAdapter {

    private String username;
    //ListView<String> filesList;
    List<String> filesList;

    public MainHandler() { // String username
        this.username =  username;
        System.out.println("MainHandler start");
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected...");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        try {
            if (msg instanceof FileRequest) {
                FileRequest fr = (FileRequest) msg;
                if (Files.exists(Paths.get("server/server_storage/" + fr.getFilename()))) {
                    FileMessage fm = new FileMessage(Paths.get("server/server_storage/" + fr.getFilename()));
                    ctx.writeAndFlush(fm);
                    System.out.println("tranfer Obj OK " + fm);
                }

            }
            if (msg instanceof FileMessage) {//
                FileMessage fm = (FileMessage) msg;
                Files.write(Paths.get("server/server_storage/" + fm.getFilename()),
                        fm.getData(), StandardOpenOption.CREATE);
                refreshServerFilesList();
                sendFileList(ctx);
                System.out.println("Obj OK " + fm);
                }
            if (msg instanceof Command) {
                String cmd = ((Command) msg).getCommand();
                String cmdDel = ((Command) msg).getFilename();

                if (cmd.equals("getServerList")){
                    refreshServerFilesList();
                    System.out.println("listLen= " + filesList.size());
                    System.out.println("get ServerList OK ");
                }
                if (cmd.equals("del")){
                    if (cmdDel!=null) {
                        System.out.println("del tryth= " + cmdDel);
                        File file = new File("server/server_storage/" + cmdDel);
                        if( file.delete()){
                            System.out.println("server/server_storage/" + cmdDel + " файл удален");
                        } else {
                            System.out.println("Файла" +  cmdDel + " не обнаружен");
                        }
                        refreshServerFilesList();
                    }
                    System.out.println("Del List OK ");
                }
                sendFileList(ctx);
            }

            if (msg == null) {
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
        ctx.close();
    }

    public void refreshServerFilesList() {

        File dir = new File("server/server_storage/"); //path указывает на директорию
        String[] arrFiles = dir.list();
        filesList = Arrays.asList(arrFiles);
        System.out.println("listLen= " + filesList.size());
        for (String file : filesList) {
            System.out.println(file);
        }
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
}
