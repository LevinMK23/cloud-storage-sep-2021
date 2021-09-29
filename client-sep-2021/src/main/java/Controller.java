import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.geekbrains.*;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class Controller implements Initializable {

    private static final Path ROOT_DIR = Paths.get("client-sep-2021","root");
    public ListView<String> listView;
    public TextField input;
    public ListView<String> listView1;
    private Net net;


    public void sendFile(ActionEvent actionEvent) throws IOException {
        String fileName = input.getText();
        input.clear();
        Path file = Paths.get(String.valueOf(ROOT_DIR), fileName);
        net.sendCommand(new FileMessage(file));
    }
    public void receiveArrayFiles(ActionEvent actionEvent) {

        net.sendCommand(new ListRequest());
    }
    public void updateArrayFiles(ActionEvent actionEvent) throws IOException {
        fillFilesInCurrentDir();
    }

    public void receiveFile(ActionEvent actionEvent){
        String fileName = input.getText();
        input.clear();
        Path file = Paths.get(fileName);
        net.sendCommand(new FileRequest(file));
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        //показываем список файлов на клиенте
        try {
            fillFilesInCurrentDir();
        } catch (IOException e) {
            e.printStackTrace();
        }

        net = Net.getInstance(cmd -> {
                    switch (cmd.getType()) {
                        case LIST_RESPONSE:
                            listView1.getItems().clear();
                            ListResponse listResponse = (ListResponse) cmd;
                            List<File> list = listResponse.getList();
                            for (File file : list) {
                                listView1.getItems().add(file.getName());
                                System.out.println(file.getName());
                            }
                            break;
                        case FILE_MESSAGE:
                            FileMessage fileMessage = (FileMessage) cmd;
                            Files.write(
                                    ROOT_DIR.resolve(fileMessage.getName()),
                                    fileMessage.getBytes()
                            );
                            fillFilesInCurrentDir();
                            break;
                    }
                }
        );

        //отправляем запрос списка файлов на сервере и отображаем его
        //  net.sendCommand(new ListRequest());  //todo не работает, приложение падает
    }

    private void fillFilesInCurrentDir() throws IOException {
        listView.getItems().clear();
        listView.getItems().addAll(
                Files.list(Paths.get(String.valueOf(ROOT_DIR)))
                        .map(p -> p.getFileName().toString())
                        .collect(Collectors.toList())
        );

        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String item = listView.getSelectionModel().getSelectedItem();
                input.setText(item);
            }
        });

        listView1.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String item = listView1.getSelectionModel().getSelectedItem();
                input.setText(item);
            }
        });

    }














}
