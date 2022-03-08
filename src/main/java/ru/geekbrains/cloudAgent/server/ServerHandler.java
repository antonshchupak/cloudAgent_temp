package ru.geekbrains.cloudAgent.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.geekbrains.cloudAgent.common.FileMessage;
import ru.geekbrains.cloudAgent.common.FileRequest;

import java.nio.file.Files;
import java.nio.file.Paths;


public class ServerHandler extends ChannelInboundHandlerAdapter {


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FileRequest) {
            FileRequest fr = (FileRequest) msg;
            if (Files.exists(Paths.get("server-repo/" + fr.getFilename()))) {
                FileMessage fm = new FileMessage(Paths.get("server-repo/" + fr.getFilename()));
                ctx.writeAndFlush(fm);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

}
