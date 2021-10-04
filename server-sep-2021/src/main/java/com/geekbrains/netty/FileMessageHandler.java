package com.geekbrains.netty;

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

    private static Path currentPath;

    public FileMessageHandler() throws IOException {
        currentPath= Paths.get("server-sep-2021", "root");//todo добавить к пути username
        if (!Files.exists(currentPath)){
            Files.createDirectory(currentPath);
        }
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("Client connected!");
        log.debug("Send list of files and current directory to the client");
        //Отправляем список файлов в текущей папке на сервере
        ctx.writeAndFlush(new ListResponse(currentPath));
        //Отправляем текущую дирректорию
        ctx.writeAndFlush(new PathResponse(currentPath.toString()));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command cmd) throws Exception {
        log.debug("Received command from client: {}", cmd.getType());
        switch (cmd.getType()) {
            case FILE_MESSAGE:
                FileMessage fileMessage = (FileMessage) cmd;
                Files.write(
                        currentPath.resolve(fileMessage.getName()),
                        fileMessage.getBytes()
                );
                ctx.writeAndFlush(new ListResponse(currentPath));
                log.debug("Received a file {} from the client",fileMessage.getName());
                break;

            case FILE_REQUEST:
                FileRequest fileRequest = (FileRequest) cmd;
                String fileName = fileRequest.getName();
                Path file = Paths.get(String.valueOf(currentPath), fileName);
                ctx.writeAndFlush(new FileMessage(file));
                log.debug("Send file {} to the client",fileName);
                break;

            case LIST_REQUEST:
                ctx.writeAndFlush(new ListResponse(currentPath));
                log.debug("Send list of files to the client");
                break;

            case PATH_UP_REQUEST:
                if (currentPath.getParent()!=null){ //todo подумать над логикой, если будет авторизация пользователя
                    //todo завести переменную, за которую нельзы выходить при регистрации и сравнивать при запросе
                    // этой команды
                    currentPath = currentPath.getParent();
                }
                log.debug("Send list of files and current directory to the client");
                ctx.writeAndFlush(new ListResponse(currentPath));
                ctx.writeAndFlush(new PathResponse(currentPath.toString()));
                break;

            case PATH_IN_REQUEST:
                PathInRequest request = (PathInRequest) cmd;
                Path newPAth = currentPath.resolve(request.getDir());
                if(Files.isDirectory(newPAth)){
                    currentPath = newPAth;
                    log.debug("Send list of files and current directory to the client");
                    ctx.writeAndFlush(new ListResponse(currentPath));
                    ctx.writeAndFlush(new PathResponse(currentPath.toString()));
                }

            default:
                log.debug("Invalid command {}",cmd.getType());
                break;
        }
    }
}
