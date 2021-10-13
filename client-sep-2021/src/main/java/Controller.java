
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.geekbrains.*;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class Controller implements Initializable {

    private static Path currentDir = Paths.get("client-sep-2021", "root");
    public ListView<String> fileClientView;
    public ListView<String> fileServerView;
    public TextField input;
    public TextField currentDirectoryOnClient;
    public TextField currentDirectoryOnServer;
    public AnchorPane mainScene;

    public TextField loginField;
    public TextField passwordField;
    public Button Authorization;

    private Net net;

    public void sendLoginAndPassword(ActionEvent actionEvent) {
        String login = loginField.getText();
        String password = passwordField.getText();
        loginField.clear();
        passwordField.clear();
        net.sendCommand(new AuthRequest(login, password));
    }

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
                        case AUTH_RESPONSE:
                            AuthResponse authResponse = (AuthResponse) cmd;
                            log.debug("AuthResponse {}", authResponse.getAuthStatus());
                            if (authResponse.getAuthStatus()) {
                                mainScene.setVisible(true);
                                loginField.setVisible(false);
                                passwordField.setVisible(false);
                                Authorization.setVisible(false);
                                net.sendCommand(new ListRequest());
                            } else {
                                //todo Warning
                            }

                            break;
                        default:
                            log.debug("Invalid command {}", cmd.getType());
                    }
                }
        );
    }
    private String resolveFileType(Path path) {
        if (Files.isDirectory(path)) {
            return "[Dir]" + " " + path.getFileName().toString();
        } else {
            return "[File]" + " " + path.getFileName().toString();
        }
    }

    public String returnName2(String str) {
        String[] words = str.split(" ");
        return words[1];
    }

    public String returnName1(String str) {
        String[] words = str.split(" ");
        return words[0];
    }

    private void refreshServerView(List<String> names) {
        fileServerView.getItems().clear();
        fileServerView.getItems().addAll(names);
    }


    private void refreshClientView() throws IOException {
        fileClientView.getItems().clear();
        List<String> names = Files.list(currentDir)
                .map(this::resolveFileType)
                .collect(Collectors.toList());
        fileClientView.getItems().addAll(names);
    }

    public void addNavigationListener() {
        fileClientView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String item = returnName2(fileClientView.getSelectionModel().getSelectedItem());
                Path newPath = currentDir.resolve(item);
                if (Files.isDirectory(newPath)) {
                    currentDir = newPath;

                    try {
                        refreshClientView();
                        currentDirectoryOnClient.setText(currentDir.toString());
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                } else {
                    input.setText(item);
                }
            }
        });
        fileServerView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String item = returnName2(fileServerView.getSelectionModel().getSelectedItem());
                if (returnName1(fileServerView.getSelectionModel().getSelectedItem()).equals("[Dir]")) {
                    net.sendCommand(new PathInRequest(item));
                } else {
                    input.setText(item);
                }
            }
        });
    }
}
