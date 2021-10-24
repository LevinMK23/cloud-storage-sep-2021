package com.geekbrains.netty;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.*;
import java.sql.ResultSet;
import java.util.List;

import com.geekbrains.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileMessageHandler extends SimpleChannelInboundHandler<Command> {

    private static final Path ROOT = Paths.get("server-sep-2021", "root");
    private static Path currentPath = null;
    private static String userDir;
    private boolean isLogin = false;
    private Path userPath;
    private static final int filesPartsSize = 100000;






    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command cmd) throws Exception {
        log.debug("Message received {}" , cmd);
        // TODO: 23.09.2021 Разработка системы команд
        if(!Files.exists(ROOT)) {
            try {
                Files.createDirectory(ROOT);
            } catch (IOException e) {
                log.error("Cant create root dir!",e);
            }
        }

        if (!isLogin){

            if(cmd.getType().equals(CommandType.LOGIN_REQUEST)){
                LoginRequest loginCommand = (LoginRequest) cmd;

                ResultSet resultSet = SQLHandler.getUserFromDb(loginCommand.getLogin(),loginCommand.getPass());
                if(!resultSet.isBeforeFirst()) {
                    ctx.writeAndFlush(new LoginResponse(false,""));


                }else {
                    resultSet.next();
                    log.debug("Db response contains");

                    while (!resultSet.isAfterLast()){
                        log.debug(" str : " + resultSet.getString("id")+ " : " + resultSet.getString("user_name"));
                        resultSet.next();
                    }
                    userDir = loginCommand.getLogin();
                    currentPath = ROOT.resolve(userDir);
                    userPath = ROOT.resolve(userDir);

                    ctx.writeAndFlush(new LoginResponse(true,userDir));

                    isLogin = true;

                }

            }else if(cmd.getType().equals(CommandType.REGISTRATION_REQUEST)){
                RegistrationRequest reg = (RegistrationRequest) cmd;

                if(SQLHandler.createNewUser(reg.getUserName(),reg.getPass())){
                    userDir =reg.getUserName();

                    currentPath = ROOT.resolve(userDir);
                    userPath = ROOT.resolve(userDir);

                    System.out.println("NEW USER REGISTRED " + userDir );
                    isLogin = true;

                    ctx.writeAndFlush(new LoginResponse(true,userDir));
                }else ctx.writeAndFlush(new LoginResponse(false,""));




            }else{
                ctx.writeAndFlush(new LoginResponse(false,""));

            }

        }else {

            switch (cmd.getType()) {
                case FILE_MESSAGE:// посмотреть возможность замены моей логики на chunked file на клиентскую и серверную сторону
                    FileMessage inMsg = (FileMessage) cmd;
                    if(inMsg.isFirstPart()){

                        Files.write(ROOT.resolve(inMsg.getName()),inMsg.getBytes(),StandardOpenOption.CREATE);

                    }else {


                        Files.write(ROOT.resolve(inMsg.getName()),inMsg.getBytes(), StandardOpenOption.APPEND );

                    }
                    break;
                case LIST_REQUEST:
                    ListRequest lrq = (ListRequest) cmd;

                    //тут обрабатываются переходы по папкам
                    log.debug("обработка запроса папки :" + ROOT.resolve(lrq.getDir()).normalize());
                        if (ROOT.resolve(lrq.getDir()).normalize().startsWith(userPath)){//на всякий случай не разрешаем уйти за папку пользователя
                            currentPath=ROOT.resolve(lrq.getDir());

                            if (!Files.exists(currentPath)) Files.createDirectory(currentPath);

                            ctx.writeAndFlush(new ListResponse(currentPath,false, Paths.get(lrq.getDir())));
                            currentPath.normalize();
                            log.debug("Server send : {}",currentPath);
                        }


                    break;
                case FIRST_REQUEST: //обрабатываем первичный запрос на то что есть в папке пользователя на сервере
                    FirstRequest fq = (FirstRequest) cmd;
                    log.debug("First Handle : {}", userPath);
                    ListResponse listResponse = new ListResponse(userPath,true,userPath.getFileName());

                    ctx.writeAndFlush(listResponse);

                    break;

                case FILE_REQUEST:

                    FileRequest frq = (FileRequest) cmd;
                    String fileName = frq.getFileName();
                    currentPath = ROOT.resolve(fileName);
                    if(Files.exists(currentPath)){

                        sendFileToClient(currentPath,fileName,ctx);
                        ctx.writeAndFlush(new FileResponse(true,"success..."));
                    }else ctx.writeAndFlush(new FileResponse(false,"unsuccessful..cant find file"));

                break;
                case DELETE_REQUEST:
                    DeleteRequest drq = (DeleteRequest) cmd;
                    List<String> list = drq.getPaths();
                    log.debug("обработка удаления"+ list.toString());
                    int size = list.size();
                    boolean isDone = true;
                    for (int i =0; i< size; i++){
                        Path path = ROOT.resolve(list.get(i));
                        try {
                            if(Files.isDirectory(path)){
                                Files.walkFileTree(path, new MyVisitorForDelete());
                            }else Files.delete(path);

                        }catch (IOException e){
                            log.debug("problem when delete file ",e);

                            isDone = false;
                        }
                    }
                    if(isDone) ctx.writeAndFlush(new DeleteResponse("delete success"));
                    else ctx.writeAndFlush(new DeleteResponse("Cant delete file"));
                    list.clear();


                    break;

            }
        }


    }
    private void sendFileToClient(Path fileToSend, String fileName, ChannelHandlerContext ctx) throws IOException {

        long fileSize = Files.size(fileToSend);
        if(fileSize<= filesPartsSize){//маленький
            ctx.writeAndFlush(new FileMessage(fileToSend));

        }else {
            //большой
            sendBigFile(fileName, fileToSend, fileSize,ctx);


        }
    }
    private void sendBigFile(String fileName, Path fileToSend, long fileSize,ChannelHandlerContext ctx) {
        try(RandomAccessFile raf = new RandomAccessFile(fileToSend.toFile(),"r")){
            long skip = 0;
            boolean isFirstPart = true;
            while (skip< fileSize){
                if(skip + filesPartsSize > fileSize){
                    byte[] bytes= new byte[(int)(fileSize -skip)];
                    raf.read(bytes);
                    ctx.writeAndFlush(new FileMessage(fileName,bytes, fileSize,isFirstPart));
                    skip+= fileSize -skip;
                }else {
                    byte[] bytes = new byte[(int)filesPartsSize];
                    raf.read(bytes);
                    ctx.writeAndFlush(new FileMessage(fileName,bytes, fileSize,isFirstPart));
                    skip += filesPartsSize;
                }
                isFirstPart=false;
            }
        }catch (Exception e){
            log.error("Ошибка чтения файла", e);
        }
    }



}
