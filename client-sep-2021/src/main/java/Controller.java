import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;


import com.geekbrains.*;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    TreeTableView<FileItem> treeTableView;
    @FXML
    TreeTableColumn<FileItem,Boolean> clientStatusCol;
    @FXML
    TreeTableColumn<FileItem,Boolean> serverStatusCol;
    @FXML
    TreeTableColumn<FileItem,String> filesCol;

    TreeItem<FileItem> root;
    TreeItem<FileItem> currentNode;



    private final ImageView folderIcon = new ImageView (
            new Image(getClass().getResourceAsStream("folder(17x15).png"))
    );
    private final ImageView fileIcon = new ImageView (
            new Image(getClass().getResourceAsStream("file(17x15).png"))
    );







    @FXML
    private void moveOrSend(ActionEvent actionEvent) throws IOException {


        String fileName = input.getText();

        Path fileToSend = currentDir.resolve( fileName);
        log.debug("Prepare to send {}",fileName,currentDir);
        if(!fileToSend.normalize().startsWith(userDir)){
            //тут не пускаем пользователя в другие папки
            log.warn("message canceled : no access");
            input.clear();
            input.setText("message canceled - no access");
        }else if (!Files.exists(fileToSend.normalize())){
            //не даём отправить несуществующие файлы
            log.warn("message canceled : the file or folder is missing");
            input.clear();
            input.setText("message canceled - the file or folder is missing");
        } else if (fileName.isEmpty()){
            // обновляем окно если отправлен пустой запрос
            net.sendFile(new RefreshRequest(treeTableView.getSelectionModel().getSelectedItem().getValue().getPath().getFileName()));
        } else {
            if(Files.isDirectory(fileToSend)){
                //отправляем запрос на переход или созданиеновой директории на сервере

                net.sendFile(new ListRequest(treeTableView.getSelectionModel().getSelectedItem().getValue().getPath().getFileName()));
            }else {
                // передаём файл
                long fileSize = Files.size(fileToSend);
                if(fileSize<= filesPartsSize){//маленький
                    net.sendFile(new FileMessage(fileToSend));
                    net.sendFile(new RefreshRequest(treeTableView.getSelectionModel().getSelectedItem().getValue().getPath().getFileName()));
                }else {
                    //большой
                    sendBigFile(fileName, fileToSend, fileSize);
                    net.sendFile(new RefreshRequest(treeTableView.getSelectionModel().getSelectedItem().getValue().getPath().getFileName()));

                }
                input.clear();
            }
        }
    }

    private void sendBigFile(String fileName, Path fileToSend, long fileSize) {
        try(RandomAccessFile raf = new RandomAccessFile(fileToSend.toFile(),"r")){
            long skip = 0;
            boolean isFirstPart = true;
            while (skip< fileSize){
                if(skip + filesPartsSize > fileSize){
                    byte[] bytes= new byte[(int)(fileSize -skip)];
                    raf.read(bytes);
                    net.sendFile(new FileMessage(fileName,bytes, fileSize,isFirstPart));
                    skip+= fileSize -skip;
                }else {
                    byte[] bytes = new byte[(int)filesPartsSize];
                    raf.read(bytes);
                    net.sendFile(new FileMessage(fileName,bytes, fileSize,isFirstPart));
                    skip += filesPartsSize;
                }
                isFirstPart=false;
            }
        }catch (Exception e){
            log.error("Ошибка чтения файла", e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if(!Files.exists(ROOT_DIR)) {
            try {
                Files.createDirectory(ROOT_DIR);
            } catch (IOException e) {
                log.error("Cant create root dir!",e);
            }
        }
        controller = this;
        log.debug("________client started___________");



        userDir = ClientFileMessageHandler.getCurPath();
        currentDir = ROOT_DIR;


        net = Net.getInstance(s -> Platform.runLater(()->switchCommands(s)) );


        // просим текущую директорию
        net.sendFile(new FirstRequest());// тут запрос корня




    }





    private void switchCommands(Command s) {
        // тут обрабатываю команды
        log.debug("Income command : {}",s);

            switch (s.getType()) {
                case LIST_RESPONSE:

                    ListResponse lr = (ListResponse) s;

                    if(lr.isFirstHandle()) {
                        log.debug("First Handle");
                        try {
                            userDir = ROOT_DIR.resolve(Paths.get(lr.getCurrentDir()).getFileName());
                            currentDir = ROOT_DIR.resolve(Paths.get(lr.getCurrentDir()).getFileName());
                            if(!Files.exists(userDir)) Files.createDirectory(userDir);// тут создаёи директорию пользователя

                            createTree(createMergedFilesList(lr.getFilesList(),lr.getFileFolder(), ROOT_DIR.resolve(Paths.get(lr.getCurrentDir()).getFileName())));

                        }catch (Exception e){
                            log.error("cant create tree",e);
                        }

                    } else {
                        log.debug(" Income message " + lr.getCurrentDir() +" : " + currentNode.getValue().getFileName());
                        try {

                            displayChildrenTree(currentNode,createMergedFilesList(lr.getFilesList(),lr.getFileFolder(), currentDir.resolve(lr.getCurrentDir())));
                        } catch (IOException e) {
                            log.error("cant create tree",e);
                        }
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
                case FILE_RESPONSE:
                    FileResponse fileResponse = (FileResponse) s;
                    if(fileResponse.isDone()){
                        input.setText(fileResponse.getMessage());
                        net.sendFile(new RefreshRequest(treeTableView.getSelectionModel().getSelectedItem().getValue().getPath()));
                    }else input.setText(fileResponse.getMessage());
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



    @FXML
    private void getInStorage(ActionEvent event) {
        //тут просим у сервера файл

    }
    private void createTree(List<FileItem> items){
        root = new TreeItem<>(new FileItem(userDir,true,true,true),folderIcon);
        root.setExpanded(true);
        treeTableView.setRoot(root);
        filesCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<FileItem,String> p)
                        ->  new ReadOnlyStringWrapper(p.getValue().getValue().getFileName()) );




        clientStatusCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<FileItem,Boolean> p)
                        -> new ReadOnlyBooleanWrapper(p.getValue().getValue().isClientStatus()));
        serverStatusCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<FileItem,Boolean> p)
                        -> new ReadOnlyBooleanWrapper(p.getValue().getValue().isServerStatus()));
        treeTableView.setOnMouseClicked(event -> {
            if(event.getClickCount()==2){
                String item = treeTableView.getSelectionModel().getSelectedItem().getValue().getFileName();
                currentNode = treeTableView.getSelectionModel().getSelectedItem();
                input.setText(item);
            }
        });

        displayChildrenTree(root,items);

    }
    private void displayChildrenTree(TreeItem<FileItem> currentParent,List<FileItem> children){
        for (int i = 0; i < children.size(); i++) {
            TreeItem<FileItem> ti = new TreeItem<>((children.get(i)),getCurrentImage(children.get(i).isDir()));

            currentParent.getChildren().add(ti);
        }

    }
    private ImageView getCurrentImage(boolean isDir){
        if(isDir) return folderIcon;
        else return fileIcon;

    }
    private List<FileItem> createMergedFilesList(List<String> serverListincome,List<Boolean> filefolderServers,Path path) throws IOException {
        List<FileItem> serverList = toFileItemsList(serverListincome,filefolderServers,true);
        List<String> clientStringList= Files.list(path).map(p->p.getFileName().toString()).collect(Collectors.toList());
        List<Boolean> clientFilesFolders = new ArrayList<>();
        for (int i = 0; i < clientStringList.size(); i++) {
            clientFilesFolders.add(Files.isDirectory(path.resolve(clientStringList.get(i))));
        }
        List<FileItem> clientList = toFileItemsList(clientStringList,clientFilesFolders,false);
        log.debug(path +" : " + clientList );

        for (int i =0;i<clientList.size();i++){
            for (int j = 0; j < serverList.size() ; j++) {
                boolean coincidence = clientList.get(i).getFileName().equals(serverList.get(j).getFileName());
                if(coincidence){
                    clientList.get(i).setServerStatus(true);
                    serverList.remove(j);
                    break;
                }
            }
        }

        for (FileItem item: serverList) {
            clientList.add(item);

        }
        serverList.clear();

        return clientList;

    }

    private List<FileItem> toFileItemsList(List<String> serverListincome,List<Boolean> fileFolder,boolean isServersList) {
        boolean status = false;
        if(isServersList) status = true;

        log.debug(serverListincome.toString());
        List<FileItem> result = new ArrayList<>();
        for (int i = 0; i < serverListincome.size(); i++) {

            result.add(new FileItem(Paths.get(serverListincome.get(i)),fileFolder.get(i),!status,status));
        }

        log.debug( result.toString());
        return result;
    }

    public void fileOrFolderEdit(TreeTableColumn.CellEditEvent<FileItem, String> fileItemStringCellEditEvent) {
    }
}
