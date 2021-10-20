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

    private Path pathToSend = null;



    private final Image folderIcon =
            new Image(getClass().getResourceAsStream("folder(17x15).png"));
    private final Image fileIcon =
            new Image(getClass().getResourceAsStream("file(17x15).png"));







    @FXML
    private void moveOrSend(ActionEvent actionEvent) throws IOException {


        String fileName = input.getText();

        Path fileToSend = this.pathToSend;
        Path fullPathToSend = ROOT_DIR.resolve(fileToSend.normalize());
        log.debug("Prepare to send filename, {}",fileName,fileToSend);

         if(!fullPathToSend.startsWith(userDir)){
            //тут не пускаем пользователя в другие папки
            log.warn("message canceled : no access");
            input.clear();
            input.setText("message canceled - no access");
        }else if (!Files.exists(fullPathToSend)){
            //не даём отправить несуществующие файлы
            log.warn("message canceled : the file or folder is missing");
            input.clear();
            input.setText("message canceled - the file or folder is missing");
        }else if(Files.isDirectory(fullPathToSend)){
                //отправляем запрос на переход или созданиеновой директории на сервере
                net.sendFile(new ListRequest(getStringPathForSend()));
                input.setText("Обновил папку");
         }else {
                // передаём файл
                long fileSize = Files.size(fullPathToSend);
                if(fileSize<= filesPartsSize){//маленький
                    net.sendFile(new FileMessage(fullPathToSend));

                    net.sendFile(new ListRequest(getStringPathForSend(true)));

                }else {
                    //большой

                    sendBigFile(fullPathToSend, fileSize);
                    String toSend = getStringPathForSend(true);

                    net.sendFile(new ListRequest(toSend));

                }

                input.clear();
            }
        }


    private void sendBigFile( Path fileToSend, long fileSize) {
        log.debug("fileName : "+fileToSend.getFileName()+" pathFileToSend : "+fileToSend);
        try(RandomAccessFile raf = new RandomAccessFile(fileToSend.toFile(),"r")){
            long skip = 0;
            boolean isFirstPart = true;
            while (skip< fileSize){
                if(skip + filesPartsSize > fileSize){
                    byte[] bytes= new byte[(int)(fileSize -skip)];
                    raf.read(bytes);
                    net.sendFile(new FileMessage(getStringSubPath(fileToSend),bytes, fileSize,isFirstPart));
                    skip+= fileSize -skip;
                }else {
                    byte[] bytes = new byte[(int)filesPartsSize];
                    raf.read(bytes);
                    net.sendFile(new FileMessage(getStringSubPath(fileToSend),bytes, fileSize,isFirstPart));
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

                            createTree(createMergedFilesList(lr.getFilesList(),lr.getFileFolder(), userDir));

                        }catch (Exception e){
                            log.error("cant create tree",e);
                        }

                    } else {
                        log.debug(" Income message " + lr.getCurrentDir() +" : " + currentNode.getValue().getFileName());
                        try {

                            displayChildrenTree(currentNode,createMergedFilesList(lr.getFilesList(),lr.getFileFolder(),ROOT_DIR.resolve(lr.getCurrentDir())));
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
                        String toSend = getStringPathForSend(true);
                        net.sendFile(new ListRequest(toSend));
                    }else input.setText(fileResponse.getMessage());
                    break;


                }



    }

    private void inFileTransfer(FileMessage command) throws IOException {
        FileMessage inMsg = command;
        if(inMsg.isFirstPart()){

            Files.write(ROOT_DIR.resolve(inMsg.getName()),inMsg.getBytes(), StandardOpenOption.CREATE);

        }else {


            Files.write(ROOT_DIR.resolve(inMsg.getName()),inMsg.getBytes(), StandardOpenOption.APPEND );

        }
    }



    @FXML
    private void getInStorage(ActionEvent event) {
        //тут просим у сервера файл
                log.debug("request for file : "+ pathToSend.toString());
                net.sendFile(new FileRequest(pathToSend.toString()));
                input.setText(pathToSend.getFileName()+" : request send");

    }
    private void createTree(List<FileItem> items){
        root = new TreeItem<>(new FileItem(userDir.getFileName(), true,true,true),getCurrentImage(true));
        root.setExpanded(true);
        treeTableView.setRoot(root);


        filesCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<FileItem,String> p)
                        -> new ReadOnlyStringWrapper(p.getValue().getValue().getFileName()));

        clientStatusCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<FileItem,Boolean> p)
                        -> new ReadOnlyBooleanWrapper(p.getValue().getValue().isClientStatus()));
        serverStatusCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<FileItem,Boolean> p)
                        -> new ReadOnlyBooleanWrapper(p.getValue().getValue().isServerStatus()));


//https://betacode.net/11149/javafx-treetableview

        treeTableView.setOnMouseClicked(event -> {
            if(event.getClickCount()==2){
                String item = treeTableView.getSelectionModel().getSelectedItem().getValue().getFileName();
                currentNode = treeTableView.getSelectionModel().getSelectedItem();


                pathToSend = currentNode.getValue().getPath();
                log.debug("pathTosend :" + pathToSend.toString());
                if(Files.isDirectory(ROOT_DIR.resolve(pathToSend))) net.sendFile(new ListRequest(pathToSend.toString()));
                else input.setText("Отправить на сервер этот файл ? :" + item);
            }
        });


        displayChildrenTree(root,items);

    }
    private void displayChildrenTree(TreeItem<FileItem> currentParent,List<FileItem> children){
        currentParent.getChildren().clear();
        for (int i = 0; i < children.size(); i++) {
            TreeItem<FileItem> ti = new TreeItem<>((children.get(i)),getCurrentImage(children.get(i).isDir()));

            currentParent.getChildren().add(ti);
        }

    }
    private ImageView getCurrentImage(boolean isDir){
        if(isDir) return new ImageView(folderIcon);
        else return new ImageView(fileIcon);

    }
    private List<FileItem> createMergedFilesList(List<String> serverListincome,List<Boolean> filefolderServers,Path path) throws IOException {
        //тут лепим из строк файл айтемы для сервера
        List<FileItem> serverList = toFileItemsList(serverListincome,filefolderServers,true);
        log.debug("using Path : "+path);

        List<String> clientStringList= Files.list(path).map(p->p.subpath(2,p.getNameCount()).toString()).collect(Collectors.toList());
        List<Boolean> clientFilesFolders = new ArrayList<>();
        for (int i = 0; i < clientStringList.size(); i++) {
            clientFilesFolders.add(Files.isDirectory(path.resolve(Paths.get(clientStringList.get(i)).getFileName())));
        }
        //тут лепим из строк файл айтемы для клиента
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

    private List<FileItem> toFileItemsList(List<String> listIncome,List<Boolean> fileFolder,boolean isServersList) {


        log.debug(listIncome.toString());
        List<FileItem> result = new ArrayList<>();
        for (int i = 0; i < listIncome.size(); i++) {

            result.add(new FileItem(Paths.get(listIncome.get(i)),fileFolder.get(i),!isServersList,isServersList));
        }

        log.debug( result.toString());
        return result;
    }
    @FXML
    private void fileOrFolderEdit(TreeTableColumn.CellEditEvent<FileItem, String> fileItemStringCellEditEvent) {
    }

    private String getStringPathForSend(){// использовать для вынимания подготовленого пути из айтема

        String result = treeTableView.getSelectionModel().getSelectedItem().getValue().getPath().toString();
        log.debug(result);
        return result;

    }
    private String getStringPathForSend(boolean isFiles){// использовать для вынимания подготовленого пути из айтема

        String result = treeTableView.getSelectionModel().getSelectedItem().getValue().getPath().getParent().toString();
        currentNode = currentNode.getParent();
        log.debug(result);
        return result;

    }
    private String getStringSubPath(Path path){
        return path.subpath(2, path.getNameCount()).toString();
    }
}
