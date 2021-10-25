import com.geekbrains.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@Slf4j
public class ClientFileMessageHandler extends SimpleChannelInboundHandler<Command> {

    private static final Path ROOT = Paths.get("client-sep-2021", "root");
    private static Path CLIENT_DIR = Paths.get("client-sep-2021");
    public static Path getCurPath() {
        return curPath;
    }
    private static Path curPath = null;

    public boolean isLogin() {
        return isLogin;
    }

    private boolean isLogin;

    public void setCallback(Callback callback,boolean isLogin) {
        this.callback = callback;
        this.isLogin = isLogin;
    }

    private Callback callback;


    public ClientFileMessageHandler(Callback callback,boolean isLogin) {


        this.callback = callback;
        this.isLogin = isLogin;

    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Command command) throws Exception {
        log.debug("received : {}",command.toString());

        if(command.getType().equals(CommandType.DISCONNECT_RESPONCE)){
            log.debug("disconnect....");

            channelHandlerContext.channel().closeFuture();
            channelHandlerContext.close();
        }else {
            if(!isLogin){
                login(command);

            }else {
                callback.call(command);
            }
        }



    }


    private void login(Command command) throws IOException {
        if(command.getType().equals(CommandType.LOGIN_RESPONSE)){
            log.debug("received : {}", command.toString());
            LoginResponse lr = (LoginResponse) command;

            if(lr.isValid()){
                isLogin=true;
                curPath = ROOT.resolve(lr.getUserName());
                if(!Files.exists(curPath)) {
                    if(!Files.exists(CLIENT_DIR))Files.createDirectory(CLIENT_DIR);
                    if(!Files.exists(ROOT))Files.createDirectory(ROOT);
                    Files.createDirectory(curPath);
                }
                callback.call(lr);// вызывает закрытие сцены авторизации или регистрации


                //установили папку пользователя (больше не меняется)
            }
        }
    }
}
