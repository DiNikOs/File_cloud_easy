
import com.dinikos.file_cloud_easy.common.AbstractMessage;
import com.dinikos.file_cloud_easy.common.FileMessage;
import com.dinikos.file_cloud_easy.common.FileRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import io.netty.util.ReferenceCountUtil;
import javafx.application.Platform;
import javafx.scene.control.ListView;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MainHandler extends ChannelInboundHandlerAdapter {

    private String username = null;
    ListView<String> filesList;

    public MainHandler() {
        this.username =  AuthHandler.getUsername();
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
                // Что делать если прилетел файл ??
                ByteBuf in = (ByteBuf) msg;
                try {
                    while (in.isReadable()) {
                        System.out.print((char) in.readByte());
                    }
                } finally {
                    ReferenceCountUtil.release(msg);
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

//    public static AbstractMessage readObject() throws ClassNotFoundException, IOException {
//        Object obj = inMsg.readObject();
//        return (AbstractMessage) obj;
//    }

    public void refreshLocalFilesList() {
        updateUI(() -> {
            try {
                filesList.getItems().clear();
                Files.list(Paths.get("client_storage")).
                        map(p -> p.getFileName().toString()).
                        forEach(o -> filesList.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }
}
