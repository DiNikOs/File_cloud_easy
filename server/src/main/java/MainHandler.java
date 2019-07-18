
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
              //  refreshLocalFilesList();
                refreshServerFilesList();
                System.out.println("Obj OK " + fm);
                }
            if (msg instanceof Command) {
                String cmd = ((Command) msg).getCommand();
                refreshServerFilesList();
                System.out.println("listLen= " + filesList.size());
                if (cmd.equals("getServerList")){
                    FileList fl = new FileList(filesList);

                    ctx.writeAndFlush(fl);
                    System.out.println("tranfer List OK " + fl);
                }
                if (cmd.equals("Del")){
                    FileList fl = new FileList(filesList);

                    ctx.writeAndFlush(fl);
                    System.out.println("Del List OK " + fl);
                }


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

//    public void refreshLocalFilesList() {
//        updateUI(() -> {
//            try {
//                filesList.getItems().clear();
//                Files.list(Paths.get("server/server_storage/")).
//                        map(p -> p.getFileName().toString()).
//                        forEach(o -> filesList.getItems().add(o));
//                System.out.println("listLen= " + filesList.getItems().size());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });
//    }

    public void refreshServerFilesList() {

        File dir = new File("server/server_storage/"); //path указывает на директорию
        String[] arrFiles = dir.list();
        filesList = Arrays.asList(arrFiles);
        System.out.println("listLen= " + filesList.size());
        for (String file : filesList) {
            System.out.println(file);
        }
    }

    public static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }
}
