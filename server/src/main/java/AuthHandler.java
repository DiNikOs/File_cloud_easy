import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class AuthHandler extends ChannelInboundHandlerAdapter {
    private boolean authOk = false;
    public static String username = null;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String input = (String) msg;
        // /auth user1
        if (authOk) {
            ctx.fireChannelRead(input);
            return;
        }
        if (input.split(" ")[0].equals("/auth")) {
            username = input.split(" ")[1];
            authOk = true;
            System.out.println("AuthOk");
            ctx.pipeline().addLast(new MainHandler());
        }
        if (input!=null) {
            System.out.println(msg);
        }
    }

    public static String getUsername() {
        if (username != null) {
            return username;
        }
        return "Username is not registered";
    }

}
