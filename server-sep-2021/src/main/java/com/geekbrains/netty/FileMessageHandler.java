package com.geekbrains.netty;

import java.nio.file.*;
import java.sql.ResultSet;

import com.geekbrains.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class FileMessageHandler extends SimpleChannelInboundHandler<Command> {

    private static final Path ROOT = Paths.get("server-sep-2021", "root");
    private static Path currentPath = null;
    private static String userDir;
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
                    userDir = loginCommand.getLogin();
                    currentPath = ROOT.resolve(userDir);

                    ctx.writeAndFlush(new LoginResponse(true,userDir));

                    isLogin = true;

                }

            }else if(cmd.getType().equals(CommandType.REGISTRATION_REQUEST)){
                RegistrationRequest reg = (RegistrationRequest) cmd;

                if(SQLHandler.createNewUser(reg.getUserName(),reg.getPass())){
                    userDir =reg.getUserName();

                    currentPath = ROOT.resolve(userDir);

                    System.out.println("NEW USER REGISTRED " + userDir );
                    isLogin = true;

                    ctx.writeAndFlush(new LoginResponse(true,userDir));
                }else ctx.writeAndFlush(new LoginResponse(false,""));




            }else{
                ctx.writeAndFlush(new LoginResponse(false,""));

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
                    ListRequest lrq = (ListRequest) cmd;
                    System.out.println("cp :"+ currentPath);
                    System.out.println("ud :"+ userDir);
                    if(lrq.getDir().equals("*")){
                        ctx.writeAndFlush(new ListResponse(currentPath, userDir));
                    } else {
                        currentPath=currentPath.resolve(lrq.getDir());
                        if (!Files.exists(currentPath)) Files.createDirectory(currentPath);

                        ctx.writeAndFlush(new ListResponse(currentPath, currentPath.getFileName().toString()));
                    }


                    break;


            }
        }


    }


}
