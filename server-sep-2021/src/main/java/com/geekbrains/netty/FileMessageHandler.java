package com.geekbrains.netty;

import java.nio.file.*;
import java.sql.ResultSet;
import java.util.List;
import java.util.stream.Collectors;

import com.geekbrains.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class FileMessageHandler extends SimpleChannelInboundHandler<Command> {

    private static final Path ROOT = Paths.get("server-sep-2021", "root");
    private Path currentPath = null;
    private String user;
    private boolean isLogin = false;






    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command cmd) throws Exception {

        // TODO: 23.09.2021 Разработка системы команд
        System.out.println("перед блоком аутентификации");
        if (!isLogin){

            if(cmd.getType().equals(CommandType.LOGIN_REQUEST)){
                LoginRequest loginCommand = (LoginRequest) cmd;

                ResultSet resultSet = SQLHandler.getUserFromDb(loginCommand.getLogin(),loginCommand.getPass());
                if(resultSet==null) {
                    ctx.writeAndFlush(new LoginResponse(false,""));


                }else {
                    ctx.writeAndFlush(new LoginResponse(true,loginCommand.getLogin()));
                    currentPath = ROOT.resolve(loginCommand.getLogin());
                    isLogin = true;

                }

            }else if(cmd.getType().equals(CommandType.REGISTRATION_REQUEST)){
                RegistrationRequest reg = (RegistrationRequest) cmd;
                if(SQLHandler.createNewUser(reg.getUserName(),reg.getPass())){
                    ctx.write(new LoginResponse(true,reg.getUserName()));
                    currentPath = ROOT.resolve(reg.getUserName());
                    user=reg.getUserName();
                    isLogin = true;
                }else ctx.write(new LoginResponse(false,""));




            }else{
                ctx.write(new LoginResponse(false,""));

            }

        }else {
            System.out.println("перед свич кейс");
            switch (cmd.getType()) {
                case FILE_MESSAGE:// посмотреть возможность замены моей логики на chunked file на клиентскую и серверную сторону
                    FileMessage inMsg = (FileMessage) cmd;
                    if(inMsg.isFirstPart()){

                        Files.write(currentPath.resolve(inMsg.getName()),inMsg.getBytes(),StandardOpenOption.CREATE);//тут может быть ошибка

                    }else {


                        Files.write(currentPath.resolve(inMsg.getName()),inMsg.getBytes(), StandardOpenOption.APPEND );

                    }
                    break;
                case LIST_REQUEST:


                    ctx.writeAndFlush(new ListResponse(currentPath,user));
                    break;

                case CD_REQUEST:
                    CdRequest cdRequest = (CdRequest)cmd;
                    Path newPath = currentPath.resolve(cdRequest.getDir());
                    if(!Files.exists(newPath)) Files.createDirectory(newPath);
                    newPath = currentPath;
                    ctx.writeAndFlush(new CdResponse(newPath));
                    break;
            }
        }


    }


}
