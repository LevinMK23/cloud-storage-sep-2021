import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.geekbrains.Command;
import com.geekbrains.FileMessage;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
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
    private final long filesPartsSize = 10000000;

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

        byte[] fileData =  Files.readAllBytes(file);
        ByteBuffer buffer = ByteBuffer.wrap(fileData);
        if(fileSize<= filesPartsSize){
            System.out.println(file.toString());
            net.sendFile(new FileMessage(file));

        }else {
            int count = 0;
            while (buffer.hasRemaining()){
                net.sendFile(new FileMessage(fileName+"_"+count,buffer.get(new byte[(int)filesPartsSize]).array(),fileSize));
                count++;
            }
        }



    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //net = Net.getInstance(s -> Platform.runLater(() -> listView.getItems().add(s)));
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
