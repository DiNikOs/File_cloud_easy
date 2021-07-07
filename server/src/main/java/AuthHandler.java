import com.dinikos.file_cloud_easy.common.Command;
import com.dinikos.file_cloud_easy.common.FileMessage;
import com.dinikos.file_cloud_easy.common.FileRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.file.Files;
import java.nio.file.Paths;

public class AuthHandler extends ChannelInboundHandlerAdapter {
    private boolean authOk = true; // false
    public String username = null;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        String input = ""; //
        input = (String)(input + msg);
        ctx.pipeline().addLast(new MainHandler());//username
        // /auth user1
        if (authOk) {
            ctx.fireChannelRead(input);
            return;
        }
        if (input.split(" ")[0].equals("/auth")) {
            username = input.split(" ")[1];
            authOk = true;
            System.out.println("AuthOk");
            ctx.pipeline().addLast(new MainHandler()); //username
        }
//        if (input!=null) {
//            System.out.println(msg);
//        }
    }

    public String getUsername() {
        if (username != null) {
            return username;
        }
        return "Username is not registered";
    }

}
