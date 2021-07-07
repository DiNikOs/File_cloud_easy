package com.dinikos.file_cloud_easy.client;

//import com.flamexander.netty.example.common.AbstractMessage;
import com.dinikos.file_cloud_easy.common.AbstractMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;
import java.net.Socket;

public class Network {
    private static Socket socket;
    private static ObjectEncoderOutputStream outMsg;
    private static ObjectDecoderInputStream inMsg;


    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected...");
    }

    public static void start() {
        try { //(Socket socket = new Socket("localhost", 8189))
            socket = new Socket("localhost", 8189);
            outMsg = new ObjectEncoderOutputStream(socket.getOutputStream());
           // outMsg.flush();
            inMsg = new ObjectDecoderInputStream(socket.getInputStream(), 100 * 1024 * 1024);

        } catch (IOException e) {
            e.printStackTrace();
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
        }
    }

    public static void stop() {
        try {
            outMsg.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            inMsg.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean sendMsg(AbstractMessage msg) {
        try {
            outMsg.writeObject(msg);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static AbstractMessage readObject() throws ClassNotFoundException, IOException {
        Object obj = inMsg.readObject();
        return (AbstractMessage) obj;
    }
}
