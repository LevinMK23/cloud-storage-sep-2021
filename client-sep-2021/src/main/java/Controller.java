import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class Controller implements Initializable {
    public ListView<String> listViewInClient;
    public TextField input;
    public ListView<String> listViewInServer;
    OutputStream out;

    private static final String FILE_DIR = "/GB cloud storage/Lesson_1/cloud-storage-sep-2021/client-sep-2021/src" +
            "/main/files";


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        //показать все файлы в директории
        File dir = new File(FILE_DIR);
        File[] arrFiles = dir.listFiles();
        List<File> lst = Arrays.asList(arrFiles);
        for (File str : lst) {
            Platform.runLater(() -> listViewInClient.getItems().add(str.toString()));
        }

        try {
            Socket socket = new Socket("localhost", 8189);
            out = socket.getOutputStream();
        } catch (IOException exc) {
            exc.printStackTrace();
        }
    }

    public void send(ActionEvent actionEvent) throws IOException {

        String fileName = input.getText();
        input.clear();

        // Преобразовываем строку, содержащую имя файла,
        // в массив байт

        byte[] name = fileName.getBytes("utf-8");

        // Отправляем длину этого массива
        out.write(name.length);

        // Отправляем байты имени
        out.write(name);

        File file = new File(FILE_DIR + "/" + fileName);

        // Получаем размер файла
        long fileSize = file.length();

        // Конвертируем его в массив байт
        ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
        buf.putLong(fileSize);

        // И отправляем
        out.write(buf.array());

        try (FileInputStream in = new FileInputStream(file)) {

            // Читаем файл блоками по килобайту и отправляем в сокет
            byte[] data = new byte[1024];
            int read;
            while ((read = in.read(data)) != -1) {
                out.write(data);
            }
        } catch (IOException exc) {
            exc.printStackTrace();
        }
    }
}
