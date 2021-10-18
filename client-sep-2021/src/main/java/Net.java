import com.geekbrains.Command;
import com.geekbrains.FileItem;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Net {

    private static Net INSTANCE;
    private SocketChannel channel;


    private Callback callback;

    public void setCallback(Callback callback) {

            log.debug("Set callback : {}",callback);
            this.callback = callback;
            ClientFileMessageHandler thisHandler = channel.pipeline().get(ClientFileMessageHandler.class);
            boolean isLogin = thisHandler.isLogin();
            log.debug(thisHandler.toString());
            channel.pipeline().remove(ClientFileMessageHandler.class);

            channel.pipeline().addLast(new ClientFileMessageHandler(callback,isLogin));

            log.debug(thisHandler.toString());



    }




    public static Net getInstance(Callback callback) {
        log.debug("new instance : {}",callback);
        if (INSTANCE == null) {
            INSTANCE = new Net(callback);
        }else INSTANCE.setCallback(callback);

        return INSTANCE;
    }

    private Net(Callback callback) {
        this.callback = callback;

        Thread thread = new Thread(() -> {
            EventLoopGroup group = new NioEventLoopGroup();

            try {
                Bootstrap bootstrap = new Bootstrap();

                bootstrap.group(group)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel c) throws Exception {
                                channel = c;
                                channel.pipeline().addLast(
                                        new ObjectEncoder(),
                                        new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                                        new ClientFileMessageHandler(callback,false)

                                );
                            }
                        });

                ChannelFuture future = bootstrap.connect("localhost", 8189).sync();
                log.debug("Client connected");
                future.channel().closeFuture().sync(); // block
            } catch (Exception e) {
                log.error("", e);
            } finally {
                group.shutdownGracefully();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }



    public void sendFile(Command command) {
        channel.writeAndFlush(command);


    }


    // send command here "channel.write...

}
