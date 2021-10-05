package com.geekbrains.netty;

import java.nio.file.*;

import com.geekbrains.Command;
import com.geekbrains.FileMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import static com.geekbrains.CommandType.FILE_MESSAGE;

public class FileMessageHandler extends SimpleChannelInboundHandler<Command> {

    private static final Path ROOT = Paths.get("server-sep-2021", "root");
    private static final byte[] bytes = new byte[64000000];


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command cmd) throws Exception {
        // TODO: 23.09.2021 Разработка системы команд
//        Files.write(
//                ROOT.resolve(fileMessage.getName()),
//                fileMessage.getBytes()
//        );
//
//        ctx.writeAndFlush("OK");
        // тут пишу в контекст
        switch (cmd.getType()) {
            case FILE_MESSAGE:
                FileMessage inMsg = (FileMessage) cmd;
                if(inMsg.isFirstPart()){
                    System.out.println(cmd.toString());
                    System.out.println(inMsg.getName());
                    Files.write(ROOT.resolve(inMsg.getName()),inMsg.getBytes(),StandardOpenOption.CREATE);//тут может быть ошибка
                    System.out.println(new String(inMsg.getBytes()));
                }else {

                    System.out.println(inMsg.getName());
                    Files.write(ROOT.resolve(inMsg.getName()),inMsg.getBytes(), StandardOpenOption.APPEND );
                    System.out.println(Files.size(ROOT.resolve(inMsg.getName())));
                    System.out.println(new String(inMsg.getBytes()));
                }
                break;
            case LIST_REQUEST:
                break;
            case PATH_REQUEST:
                break;
            case MOVE_REQUEST:
                break;
        }

    }
}
