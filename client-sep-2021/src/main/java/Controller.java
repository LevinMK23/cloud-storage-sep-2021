import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.geekbrains.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import lombok.extern.slf4j.Slf4j;



@Slf4j
public class Controller implements Initializable {

    private static Path ROOT_DIR = Paths.get("client-sep-2021/root");
    private final long filesPartsSize = 100000;
    private static Controller controller;
    public static Controller getController() {
        return controller;
    }
    private Path currentDir =ROOT_DIR;// меняем
    private Path userDir = ROOT_DIR;// корневая папка юзера


    public ListView<String> listView;
    public TextField input;
    private Net net;





    @FXML
    private void moveOrSend(ActionEvent actionEvent) throws IOException {


        String fileName = input.getText();

        Path fileToSend = currentDir.resolve( fileName);
        log.debug("Prepare to send {}",fileName,currentDir);
        if(!fileToSend.normalize().startsWith(userDir)){//тут не пускаем пользователя в другие папки
            log.warn("message canceled : no access");
            input.clear();
            input.setText("message canceled - no access");
        }else if (!Files.exists(fileToSend.normalize())){
            log.warn("message canceled : the file or folder is missing");
            input.clear();
            input.setText("message canceled - the file or folder is missing");
        } else {
            if(Files.isDirectory(fileToSend)){

                net.sendFile(new ListRequest(fileName));
            }else {
                long fileSize = Files.size(fileToSend);
                if(fileSize<= filesPartsSize){
                    net.sendFile(new FileMessage(fileToSend));
                }else {
                    try(RandomAccessFile raf = new RandomAccessFile(fileToSend.toFile(),"r")){
                        long skip = 0;
                        boolean isFirstPart = true;
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
                        log.error("Ошибка чтения файла", e);
                    }


                }
            }
        }







    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        controller = this;
        log.debug("________client started___________");



        userDir = ClientFileMessageHandler.getCurPath();
        currentDir = ROOT_DIR;

        // просим текущую директорию
        net = Net.getInstance(s -> Platform.runLater(()->switchCommands(s)) );



        net.sendFile(new ListRequest("*"));

    }




    private List<String> splitLists(List<String> serverList) throws IOException {
        log.debug("cd: "+currentDir );
        log.debug("ud: "+userDir);
        //тут объединяем листы чтобы показать их на стол
        List<String> clientListFiles = Files.list(currentDir)
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());


        for (int i = 0; i < clientListFiles.size(); i++) {
            if(Files.isDirectory(currentDir.resolve(clientListFiles.get(i)))){
                clientListFiles.set(i, "[D]:" + clientListFiles.get(i) + ":[c][X]");
            }else  clientListFiles.set(i, "<f>:" + clientListFiles.get(i) + ":[c][X]");

        }
        for (int i = 0; i < serverList.size(); i++) {// проверить логику подготовки серверного листа

            String item = serverList.get(i) + ":[c][X]";
            if (clientListFiles.contains(item)) {
                int index = clientListFiles.indexOf(item);
                clientListFiles.set(index, serverList.get(i) + ":[c][s]");
            } else clientListFiles.add(serverList.get(i) + ":[X][s]");
        }
        if (!currentDir.equals(userDir)){
            serverList.clear();

            serverList.add("..");
            serverList.addAll(clientListFiles);
            return serverList;
        }

        return clientListFiles;

    }
    private void refreshTableView(List<String> list){
        // тут обновляю стол
        listView.getItems().clear();
        listView.getItems().addAll(list);
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String item = listView.getSelectionModel().getSelectedItem();
                if(!item.equals(".."))input.setText(item.split(":")[1]);
                else input.setText(item);
            }
        });
    }
    private void switchCommands(Command s) {
        // тут обрабатываю команды


            switch (s.getType()) {
                case LIST_RESPONSE:
                    ListResponse lr = (ListResponse) s;
                    log.debug("List response contains: {}",lr.getCurrentDir());
                    currentDir = currentDir.resolve(lr.getCurrentDir());
                    currentDir = currentDir.normalize();
                    log.debug("Move to dir : {}",currentDir);

                    try {
                        refreshTableView(splitLists(lr.getFilesList()));
                    } catch (IOException e) {
                        log.error("ошибка ВВОДА-ВЫВОДА при попытке обновить стол", e);
                    }
                    break;
                case FILE_MESSAGE:
                    FileMessage fm = (FileMessage) s;
                    try {
                        inFileTransfer(fm);
                    } catch (IOException e) {
                        log.error("Ошибка записи файла", e);
                    }
                    break;


                }



        }

    private void inFileTransfer(FileMessage command) throws IOException {
        FileMessage inMsg = command;
        if(inMsg.isFirstPart()){

            Files.write(currentDir.resolve(inMsg.getName()),inMsg.getBytes(), StandardOpenOption.CREATE);

        }else {


            Files.write(currentDir.resolve(inMsg.getName()),inMsg.getBytes(), StandardOpenOption.APPEND );

        }
    }



}
