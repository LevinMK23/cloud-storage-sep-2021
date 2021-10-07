import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.geekbrains.Command;
import com.geekbrains.FileMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import lombok.extern.slf4j.Slf4j;



@Slf4j
public class Controller implements Initializable {

    private static String ROOT_DIR = "client-sep-2021/root";
    private static byte[] buffer = new byte[1024];
    private final long filesPartsSize = 1000000;

    public ListView<String> listView;
    public TextField input;
    private Net net;



    public void send(ActionEvent actionEvent) throws Exception {
        String fileName = input.getText();
//        input.clear();
//        sendFile(fileName);
        net.sendMessage(fileName);

    }
    @FXML
    private void readAndPush(ActionEvent actionEvent) throws IOException {
        Button button = (Button) actionEvent.getSource();

        String fileName = input.getText();
        Path file = Paths.get(ROOT_DIR, fileName);// тут сделать текущую директорию
        long fileSize = Files.size(file);



        if(fileSize<= filesPartsSize){
            byte[] fileData =  Files.readAllBytes(file);
            net.sendFile(new FileMessage(file));


        }else {

            try(RandomAccessFile raf = new RandomAccessFile(file.toFile(),"r")){
                long skip = 0;
                boolean isFirstPart = true;


                System.out.println(fileSize);

                    while (skip<fileSize){



                        if(skip + filesPartsSize > fileSize){
                            byte[] bytes= new byte[(int)(fileSize-skip)];

                            raf.read(bytes);

                            net.sendFile(new FileMessage(fileName,bytes,fileSize,isFirstPart));
                            skip+= fileSize-skip;
                        }else {

                            byte[] bytes = new byte[(int)filesPartsSize];
                            raf.read(bytes);


                            net.sendFile(new FileMessage(fileName,bytes,fileSize,isFirstPart));


                            skip += filesPartsSize;
                        }
                        isFirstPart=false;

                    }





            }catch (Exception e){
                log.error("raf error ", e);
            }


        }



    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        net = Net.getInstance(s -> Platform.runLater(() -> listView.getItems().add(s.toString())));
        //тут написать то что прилетает от сервака при старте клиента ( лист респонс)

        try {
            fillFilesInCurrentDir();// тут обновляем лист клиента
        } catch (IOException e) {
            log.error("files are not awaliable",e);
        }

//        try {
//            fillFilesInCurrentDir();
//            Socket socket = new Socket("localhost", 8189);
//            os = new ObjectEncoderOutputStream(socket.getOutputStream());
//            is = new ObjectDecoderInputStream(socket.getInputStream());
//            Thread daemon = new Thread(() -> {
//                try {
//                    while (true) {
//                        Command msg = (Command) is.readObject();
//                        // TODO: 23.09.2021 Разработка системы команд
//                        switch (msg.getType()) {
//
//                        }
//                    }
//                } catch (Exception e) {
//                    log.error("exception while read from input stream");
//                }
//            });
//            daemon.setDaemon(true);
//            daemon.start();
//        } catch (IOException ioException) {
//            log.error("e=", ioException);
//        }
    }

    private void fillFilesInCurrentDir() throws IOException {
        listView.getItems().clear();
        listView.getItems().addAll(
                Files.list(Paths.get(ROOT_DIR))
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList())
        );
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String item = listView.getSelectionModel().getSelectedItem();
                input.setText(item);
            }
        });
    }
}
