package com.geekbrains.netty;

import java.nio.file.*;
import java.sql.ResultSet;

import com.geekbrains.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import javax.sound.midi.Soundbank;

import static com.geekbrains.CommandType.FILE_MESSAGE;

public class FileMessageHandler extends SimpleChannelInboundHandler<Command> {

    private static final Path ROOT = Paths.get("server-sep-2021", "root");
    private Path currentPath = null;
    private boolean isLogin = false;






    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command cmd) throws Exception {

        // TODO: 23.09.2021 Разработка системы команд
        System.out.println("перед вайл");
        if (!isLogin){

            if(cmd.getType().equals(CommandType.LOGIN_REQUEST)){
                LoginRequest loginCommand = (LoginRequest) cmd;

                ResultSet resultSet = SQLHandler.getUserFromDb(loginCommand.getLogin(),loginCommand.getPass());
                if(resultSet.wasNull()) {
                    ctx.writeAndFlush(new LoginResponse(false));

                }else {
                    ctx.writeAndFlush(new LoginResponse(true));
                    isLogin = true;

                }

            }else if(cmd.getType().equals(CommandType.REGISTRATION_REQUEST)){
                RegistrationRequest reg = (RegistrationRequest) cmd;
                SQLHandler.createNewUser(reg.getUserName(),reg.getPass());
                ctx.write(new LoginResponse(true));
                isLogin = true;



            }else{
                ctx.write(new LoginResponse(false));

            }

        }else {
            System.out.println("перед свич кейс");
            switch (cmd.getType()) {
                case FILE_MESSAGE:
                    FileMessage inMsg = (FileMessage) cmd;
                    if(inMsg.isFirstPart()){

                        Files.write(ROOT.resolve(inMsg.getName()),inMsg.getBytes(),StandardOpenOption.CREATE);//тут может быть ошибка

                    }else {


                        Files.write(ROOT.resolve(inMsg.getName()),inMsg.getBytes(), StandardOpenOption.APPEND );

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


}
