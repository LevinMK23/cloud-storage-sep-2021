package com.geekbrains.netty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.geekbrains.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileMessageHandler extends SimpleChannelInboundHandler<Command> {

    private static final Path ROOT = Paths.get("server-sep-2021", "root");

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("Client connected!");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command cmd) throws Exception {
        log.debug("Received command from client: {}", cmd.getType());
        switch (cmd.getType()) {
            case FILE_MESSAGE:
                FileMessage fileMessage = (FileMessage) cmd;
                Files.write(
                        ROOT.resolve(fileMessage.getName()),
                        fileMessage.getBytes()
                );
                ctx.writeAndFlush(createFileList(ROOT.toString()));
                log.debug("Received a file {} from the client",fileMessage.getName());
                break;

            case LIST_REQUEST:
                ctx.writeAndFlush(createFileList(ROOT.toString()));
                log.debug("Send list of files to the client");
                break;

            case FILE_REQUEST:
                FileRequest fileRequest = (FileRequest) cmd;
                String fileName = fileRequest.getName();
                Path file = Paths.get(String.valueOf(ROOT), fileName);
                ctx.writeAndFlush(new FileMessage(file));
                log.debug("Send file {} to the client",fileName);
                break;

            default:
                log.debug("Invalid command {}",cmd.getType());
                break;
        }


    }
    //Получить список файлов в папке на сервере
    private static ListResponse createFileList (String str){
        File dir = new File(String.valueOf(str));
        File[] arrFiles = dir.listFiles();
        List<File> list = Arrays.asList(arrFiles);
        ListResponse listResponse = new ListResponse(list);
        return listResponse;
    }
}
