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

    private static Path currentDir = Paths.get("client-sep-2021", "root");
    public ListView<String> fileClientView;
    public ListView<String> fileServerView;
    public TextField input;
    public TextField currentDirectoryOnClient;
    public TextField currentDirectoryOnServer;
    private Net net;


    public void sendFile(ActionEvent actionEvent) throws IOException {
        String fileName = input.getText();
        input.clear();
        Path file = Paths.get(String.valueOf(currentDir.resolve(fileName)));
        net.sendCommand(new FileMessage(file));
    }

    public void receiveArrayFiles(ActionEvent actionEvent) {

        net.sendCommand(new ListRequest());
    }

    public void updateArrayFiles(ActionEvent actionEvent) throws IOException {
        refreshClientView();
    }

    public void receiveFile(ActionEvent actionEvent) {
        String fileName = input.getText();
        input.clear();
        Path file = Paths.get(fileName);
        net.sendCommand(new FileRequest(file));
    }

    public void clientPathUp(ActionEvent actionEvent) throws IOException {
        currentDir = currentDir.getParent();
        currentDirectoryOnClient.setText(currentDir.toString());
        refreshClientView();
    }

    public void serverPathUp(ActionEvent actionEvent) {
        net.sendCommand(new PathUpRequest());
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        //показываем список файлов на клиенте
        try {
            currentDirectoryOnClient.setText(currentDir.toString());
            refreshClientView();
            addNavigationListener();
        } catch (IOException e) {
            e.printStackTrace();
        }

        net = Net.getInstance(cmd -> {
                    switch (cmd.getType()) {
                        case LIST_RESPONSE:
                            ListResponse listResponse = (ListResponse) cmd;
                            refreshServerView(listResponse.getList());
                            break;
                        case FILE_MESSAGE:
                            FileMessage fileMessage = (FileMessage) cmd;
                            Files.write(
                                    currentDir.resolve(fileMessage.getName()),
                                    fileMessage.getBytes()
                            );
                            refreshClientView();
                            break;
                        case PATH_RESPONSE:
                            PathResponse pathResponse = (PathResponse) cmd;
                            currentDirectoryOnServer.setText(pathResponse.getPath());
                            break;
                        default:
                            log.error("Incorrect server response: " + cmd.getType());
                    }
                }
        );
    }

    private void refreshServerView(List<String> names) {
        fileServerView.getItems().clear();
        fileServerView.getItems().addAll(names);
    }


    private void refreshClientView() throws IOException {
        fileClientView.getItems().clear();
        List<String> names = Files.list(currentDir)
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());
        fileClientView.getItems().addAll(names);

        fileClientView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String item = fileClientView.getSelectionModel().getSelectedItem();
                input.setText(item);
            }
        });

        fileServerView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String item = fileServerView.getSelectionModel().getSelectedItem();
                input.setText(item);
            }
        });


    }

    public void addNavigationListener() {
        fileClientView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String item = fileClientView.getSelectionModel().getSelectedItem();
                Path newPath = currentDir.resolve(item);
                if (Files.isDirectory(newPath)) {
                    currentDir = newPath;
                    try {
                        refreshClientView();
                        currentDirectoryOnClient.setText(currentDir.toString());
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }
        });
        fileServerView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String item = fileServerView.getSelectionModel().getSelectedItem();
                net.sendCommand(new PathInRequest(item));

            }
        });
    }
}
