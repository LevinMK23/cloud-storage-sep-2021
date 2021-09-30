import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class OldController implements Initializable {

    private static final String ROOT_DIR = "client-sep-2021/root";
    private static byte[] buffer = new byte[1024];
    public ListView<String> listView;
    public TextField input;
    private DataInputStream is;
    private DataOutputStream os;

    public void send(ActionEvent actionEvent) throws Exception {
        String fileName = input.getText();
        input.clear();
        sendFile(fileName); // Отправляем файл на сервер
    }

    private void sendFile(String fileName) throws IOException {
        Path file = Paths.get(ROOT_DIR, fileName);
        long size = Files.size(file); // Вычисляем размер файла. Throw от этого метода
        os.writeUTF(fileName);
        os.writeLong(size);
        InputStream fileStream = Files.newInputStream(file);
        // InputStream fileStream = new FileInputStream(ROOT_DIR + "/" + fileName);
        int read;
        while ((read = fileStream.read(buffer)) != -1) {
            os.write(buffer, 0, read);
        }
        os.flush();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            fillFilesInCurrentDir();
            Socket socket = new Socket("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            Thread daemon = new Thread(() -> {
                try {
                    while (true) {
                        String msg = is.readUTF();
                        log.debug("received: {}", msg);
                        Platform.runLater(() -> listView.getItems().add(msg));
                    }
                } catch (Exception e) {
                    log.error("exception while read from input stream");
                }
            });
            daemon.setDaemon(true);
            daemon.start();
        } catch (IOException ioException) {
            log.error("e=", ioException);
        }
    }

    private void fillFilesInCurrentDir() throws IOException {
        // Чистим listView
        listView.getItems().clear();
        // Заполняем listView
        listView.getItems().addAll(
                Files.list(Paths.get(ROOT_DIR))
                        .map(p -> p.getFileName().toString())
                        .collect(Collectors.toList())
        );
        // Заполняем input выбранной позицией из listView
        listView.setOnMouseClicked(e -> {
            if(e.getClickCount() == 2) {
                String item = listView.getSelectionModel().getSelectedItem();
                input.setText(item);
            }
        });
    }
}