import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.util.Vector;

public class Server {
    private Vector<MainHandler> clients;
    final int PORT = 8189;
    Server server = null;

    public void run() throws Exception {
        clients = new Vector<>();
        EventLoopGroup mainGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            AuthHandler.connect();
            ServerBootstrap b = new ServerBootstrap();
            b.group(mainGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(
                                    new ObjectDecoder(50 * 1024 * 1024, ClassResolvers.cacheDisabled(null)),
                                    new ObjectEncoder(),
                                    new AuthHandler()
                            );
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture future = b.bind(PORT).sync();
            future.channel().closeFuture().sync();
        } finally {
            mainGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            AuthHandler.disconnect();
        }
    }

    public static void main(String[] args) throws Exception {
        new Server().run();
    }

    public boolean isNickBusy(String nick) {
        for (MainHandler o: clients){
            if(o.getNick().equals(nick)) {
                return true;
            }
        }
        return false;
    }

    public void subscribe(MainHandler client) {
        clients.add(client);
        broadcastClientList();
    }

    public void broadcastClientList() {
        StringBuilder sb = new StringBuilder();
        sb.append("/clientList ");
        for (MainHandler o: clients) {
            sb.append(o.getNick() + " ");
        }
        String out = sb.toString();
    }

    public void unsubscribe(MainHandler client) {
        clients.remove(client);
        broadcastClientList();
    }
}
