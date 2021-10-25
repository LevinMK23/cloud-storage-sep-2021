import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;


import com.geekbrains.*;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;

import static javafx.stage.Modality.WINDOW_MODAL;


@Slf4j
public class Controller implements Initializable {

    private static Path ROOT_DIR = Paths.get("client-sep-2021","root");
    private final long filesPartsSize = 100000;


    private Path currentDir =ROOT_DIR;// меняем
    private static Path userDir = ROOT_DIR;// корневая папка юзера




    public Label defaultMesages;
    private Net net;
    @FXML
    TreeTableView<FileItem> treeTableView;
    @FXML
    TreeTableColumn<FileItem,Boolean> clientStatusCol;
    @FXML
    TreeTableColumn<FileItem,Boolean> serverStatusCol;
    @FXML
    TreeTableColumn<FileItem,String> filesCol;
    @FXML
    TreeTableColumn<FileItem,Boolean> deleteColumn;

    @FXML
    CheckMenuItem serverOnlyDeleteItem;
    @FXML
    CheckMenuItem clientOnlyDeleteItem;
    @FXML
    CheckMenuItem enableDeleteMode;
    @FXML
    Button sendButton;
    @FXML
    Button uploadButton;
    @FXML
    CheckMenuItem sincAll;
    @FXML
    CheckMenuItem sincSelected;
    @FXML
    Button delAllButton;
    @FXML
    Button clientDeleteButton;
    @FXML
    Button serverDeleteButton;
    @FXML
    Button sincSelectedButton;
    @FXML
    Label delLabel;
    @FXML
    Label sincLabel;




    private TreeItem<FileItem> root;



    private static TreeItem<FileItem> currentNode;

    private Path pathToSend = null;



    private final Image folderIcon =
            new Image(getClass().getResourceAsStream("folder(17x15).png"));
    private final Image fileIcon =
            new Image(getClass().getResourceAsStream("file(17x15).png"));

    boolean isDelModeEnabled = false;
    private LinkedList<FileItem> foldersToDelete = new LinkedList<>();
    private LinkedList<FileItem> filesToDelete = new LinkedList<>();








    @FXML
    private void moveOrSend(ActionEvent actionEvent) throws IOException {


        String fileName = defaultMesages.getText();

        Path fileToSend = this.pathToSend;
        Path fullPathToSend = ROOT_DIR.resolve(fileToSend.normalize());
        log.debug("Prepare to send filename, {}",fileName,fileToSend);

         if(!fullPathToSend.startsWith(userDir)){
            //тут не пускаем пользователя в другие папки
            log.warn("message canceled : no access");

            defaultMesages.setText("message canceled - no access");
        }else if (!Files.exists(fullPathToSend)){
            //не даём отправить несуществующие файлы
            log.warn("message canceled : the file or folder is missing");

            defaultMesages.setText("message canceled - the file or folder is missing");
        }else if(Files.isDirectory(fullPathToSend)){
                //отправляем запрос на переход или созданиеновой директории на сервере
                net.sendFile(new ListRequest(getStringPathForSend()));
                defaultMesages.setText("Обновил папку");
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
    @FXML
    private void sincSelected(ActionEvent actionEvent){
        // TODO: 25.10.2021 сделать синхронизацию
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


        log.debug("________client started___________");



        userDir = ClientFileMessageHandler.getCurPath();
        currentDir = ROOT_DIR;


        net = Net.getInstance(s -> Platform.runLater(()->switchCommands(s)) );


        // просим текущую директорию
        net.sendFile(new FirstRequest());// тут запрос корня

        //слушатель для мода удаления

        enableDeleteMode.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {

                log.debug("in enableDelMode setting params");

                clientOnlyDeleteItem.setVisible(oldValue);
                serverOnlyDeleteItem.setVisible(oldValue);
                delAllButton.setVisible(newValue);

                deleteMethodSetup(newValue);
            }
        });
        clientOnlyDeleteItem.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {

                log.debug("in clientOnlyDelMode setting params");

                enableDeleteMode.setVisible(oldValue);
                serverOnlyDeleteItem.setVisible(oldValue);
                clientDeleteButton.setVisible(newValue);

                deleteMethodSetup(newValue);
            }
        });
        serverOnlyDeleteItem.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {

                log.debug("in serverOnlyDelMode setting params");

                enableDeleteMode.setVisible(oldValue);
                clientOnlyDeleteItem.setVisible(oldValue);
                serverDeleteButton.setVisible(newValue);

                deleteMethodSetup(newValue);
            }
        });




    }

    private void deleteMethodSetup(boolean newValue){
        // при включяении мода удаления необходимо закрыть доступ
        // к элементам управления синхронизацией и посылок

        deleteColumn.setVisible(newValue);
        sendButton.setVisible(!newValue);
        uploadButton.setVisible(!newValue);
        isDelModeEnabled = newValue;
        sincAll.setVisible(!newValue);
        sincSelected.setVisible(!newValue);
        defaultMesages.setVisible(!newValue);
        delLabel.setVisible(newValue);
        delLabel.setText("выберите файлы для удаления");



        if(!newValue){
            // тут чистим расстрельные списки если в них что-то осталось а из делейт мода мы вышли
            for(FileItem fi : foldersToDelete){
                fi.setToDelete(false);
            }
            for(FileItem fi : filesToDelete){
                fi.setToDelete(false);
            }
            foldersToDelete.clear();
            filesToDelete.clear();
        }

    }
    @FXML
    private void delAll(ActionEvent actionEvent){
       List<FileItem> validList = validateDelete(foldersToDelete,filesToDelete);
        List<String> stringsPaths = new LinkedList<>();
        int size = validList.size();
        for (int i = 0; i < size; i++){
            FileItem fi = validList.get(i);
            if(fi.isServerStatus()) {
                stringsPaths.add(fi.getPath().toString());
            }
            if(fi.isClientStatus()){
                if(fi.isDir()){
                    try {
                        Files.walkFileTree(ROOT_DIR.resolve(fi.getPath()),new MyVisitorForDelete());
                    } catch (IOException e) {
                        log.debug("проблема при удалении папки ",e);
                        delLabel.setText("что-то пошло не так");
                    }
                }else {
                    try {
                        Files.delete(ROOT_DIR.resolve(fi.getPath()));
                    } catch (IOException e) {
                        log.debug("проблема при удалении файла ",e);
                        delLabel.setText("что-то пошло не так");
                    }
                }
            }
        }
        log.debug("send to server DELETE ALL : "+ stringsPaths.toString());
        net.sendFile(new DeleteRequest(stringsPaths));
        validList.clear();
        refreshTreeView();


    }

    private void refreshTreeView() {

        root.getChildren().clear();
        log.debug("refresh in delete mode _____ "+ userDir);
        net.sendFile(new ListRequest(userDir.subpath(2,3).toString()));
    }

    @FXML
    private void clientDelete(ActionEvent actionEvent){
        List<FileItem> validList = validateDelete(foldersToDelete,filesToDelete);
        log.debug(validList.toString());
        int size = validList.size();
        boolean succsess = true;
        for (int i = 0; i < size; i++){
            FileItem fi = validList.get(0);
            if(fi.isClientStatus()){
                if(fi.isDir()){
                    try {
                        Files.walkFileTree(ROOT_DIR.resolve(fi.getPath()),new MyVisitorForDelete());
                        log.debug("успешно удалено " + fi.getPath());
                    } catch (IOException e) {
                        log.debug("проблема при удалении папки ",e);
                        succsess = false;
                    }
                }else {
                    try {
                        Files.delete(ROOT_DIR.resolve(fi.getPath()));
                        log.debug("успешно удалено " + fi.getPath());
                    } catch (IOException e) {
                        log.debug("проблема при удалении файла ",e);
                        succsess = false;
                    }
                }
            }
        }
        if (succsess) delLabel.setText("всё успешно удалено");
        validList.clear();
        refreshTreeView();


    }
    @FXML
    private void serverDelete(ActionEvent actionEvent){
        List<FileItem> validList = validateDelete(foldersToDelete,filesToDelete);
        List<String> stringsPaths = new LinkedList<>();
        int size = validList.size();
        for (int i =0; i<size;i++){
            FileItem fi = validList.get(i);
            if(fi.isServerStatus()) stringsPaths.add(fi.getPath().toString());// тут отправляем строки на удаление
        }
        log.debug("DELETE prepare for send : " + stringsPaths.toString());
        net.sendFile(new DeleteRequest(stringsPaths));

        refreshTreeView();
    }

    // тут написать алгоритм отсеивания из списка файлов не входящих в серверные списки или в клиентские списки
    private List<FileItem> validateDelete(LinkedList<FileItem> folders, LinkedList<FileItem> files){
        // тут написать алгоритм проверки списков на удаление
        // предназначен для того чтобы сократить количество запросов на сервер и к жёсткому диску компа
        LinkedList<FileItem> result = new LinkedList<>();
        filterFolders(folders, result);
        filterFiles(folders, files, result);// на выходе результ заполнен папками и файлами

        // ещё 100 милионов строк говнокода и я у цели
        log.debug("________________________filter_____________________________");
        for (FileItem f : result){
            log.debug("result contains {}",f.getPath().toString());
        }

        // TODO: 24.10.2021 добавить алгоритм составления запросов на обновление папок, чтобы изменения происходили интерактивно

        folders.clear();
        files.clear();
        return result;


    }

    private void filterFolders(LinkedList<FileItem> folders, LinkedList<FileItem> result) {
        for (int i = 0; i < folders.size(); i++){//фильтр
            if(result.size()>1){
                FileItem fi = folders.get(i);
                boolean isParent = false;
                boolean isCild = false;
                LinkedList<FileItem> toRemoveFromList = new LinkedList<>();
                int resultSize = result.size();
                for (int j = 0; j < resultSize ; j++) {
                    Path resultsPath= result.get(j).getPath();
                    Path folder = fi.getPath();
                    int lenthItem = resultsPath.getNameCount();
                    int lenthFi = folder.getNameCount();
                    Path isShort = null;
                    if(lenthItem==lenthFi){
                        if(folder.equals(resultsPath)) {// ОТСЕИВАЕМ ОДИНАКОВЫЕ
                            isParent = true;
                            break;
                        }
                    }else if(lenthItem>lenthFi){// путь к папке короче
                        isShort = cutPaths(resultsPath,lenthItem-lenthFi);
                        if(isShort.equals(folder)) {// он парент надо убрать возможных детей из листа
                            toRemoveFromList.add(result.get(j));
                            if(!isParent) result.addFirst(fi);// тут смотри если косяк
                            isParent = true;

                        }
                    }else{// короче тот который уже в результатах
                        isShort = cutPaths(folder,lenthFi-lenthItem);
                        if(isShort.equals(folder)) {// он чилдрен
                            isCild = true;
                            break;
                        }
                    }

                }
                if(!isParent) result.add(fi);//добавили его если он нигде не был чилдреном или парентом
                else if(!isCild){
                    //чистим резалт от детей и добавляем парента
                    for (FileItem f : toRemoveFromList){
                        result.remove(f);
                    }
                    toRemoveFromList.clear();
                    result.add(fi);
                }


            }else result.add(folders.get(i));

        }
        folders.clear();
        folders.addAll(result);// результат вернули в папки
        result.clear();
    }

    private void filterFiles(LinkedList<FileItem> folders, LinkedList<FileItem> files, LinkedList<FileItem> result) {
        int count = files.size();
        for (int i = 0; i < count;i++){
            FileItem fi = files.get(i);

            if(folders.size()>0){
                boolean isCilden = false;
                for (FileItem f : folders){
                    Path parent = fi.getPath().getParent();
                    Path folder = f.getPath();
                    int parentSize = parent.getNameCount();
                    int folderSize = folder.getNameCount();
                    if(parentSize>=folderSize){
                        Path test = cutPaths(parent,parentSize-folderSize);
                        if(test.equals(folder)){
                            isCilden=true;
                            break;
                        }
                    }

                }
                if(!isCilden&&!result.contains(fi)) result.add(fi);


            }else {
                result.addAll(files);
                break;
            }

        }
        result.addAll(folders);
    }

    private Path cutPaths(Path path, int forCut){

        Path result =path.subpath(0,path.getNameCount()-forCut);
        log.debug(path.toString() + " : " +forCut+" : " + result);

       return result;

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
                        defaultMesages.setText(fileResponse.getMessage());
                        String toSend = getStringPathForSend(true);
                        net.sendFile(new ListRequest(toSend));
                    }else defaultMesages.setText(fileResponse.getMessage());
                    break;

                case DELETE_RESPONSE:
                    DeleteResponse drs = (DeleteResponse) s;
                    delLabel.setText(drs.getMessage());

                    break;
                }




    }
    public static TreeItem<FileItem> getCurrentNode() {
        return currentNode;
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
                defaultMesages.setText(pathToSend.getFileName()+" : request send");

    }
    private void createTree(List<FileItem> items){
        root = new TreeItem<>(new FileItem(userDir.getFileName(), true,true,true),getCurrentImage(true));
        root.setExpanded(true);
        treeTableView.setRoot(root);



        treeEventHandler();

        serverStatusEventHandler();
        clientStatusEventHandler();
        deleteEventHandler();


//https://betacode.net/11149/javafx-treetableview

        setOnMousClickedOneRow();


        displayChildrenTree(root,items);

    }

    private void deleteEventHandler() {
        deleteColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<FileItem, Boolean>, ObservableValue<Boolean>>() {
            @Override
            public ObservableValue<Boolean> call(TreeTableColumn.CellDataFeatures<FileItem, Boolean> param) {
                log.debug("in callback for checkbox ");
                TreeItem<FileItem> ti = param.getValue();
                FileItem fi = ti.getValue();
                SimpleBooleanProperty sbp = new SimpleBooleanProperty(fi.isToDelete());
                sbp.addListener(new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {

                        fi.setToDelete(newValue);
                        log.debug("listener for checkbox : "+ fi.isToDelete());

                    }
                });

                return sbp;// проверить коректность работы
            }
        });
        deleteColumn.setCellFactory(p -> {
            CheckBox checkBox = new CheckBox();
            TreeTableCell<FileItem, Boolean> cell = new TreeTableCell<FileItem, Boolean>() {
                @Override
                public void updateItem(Boolean item, boolean empty) {

                    if (empty) {
                        setGraphic(null);

                    } else {

                        checkBox.setSelected(item);
                        setGraphic(checkBox);

                    }
                }
            };
            checkBox.selectedProperty().addListener((obs, wasSelected, isSelected) ->{

                  FileItem fi =  (FileItem)cell.getTreeTableRow().getItem();
                  if(ROOT_DIR.resolve(fi.getPath()).equals(userDir)){
                      log.debug("cant setToDelete user dir");
                  }else {
                      fi.setToDelete(isSelected);
                      if(fi.isDir()) {
                          if(isSelected ){
                              foldersToDelete.add(fi);
                          }else foldersToDelete.remove(fi);
                      } else {
                          if(isSelected){
                              filesToDelete.add(fi);
                          }else {
                              filesToDelete.remove(fi);
                          }
                      }
                      log.debug(fi.getFileName() + " : cb : changed to : " + isSelected );
                  }


            });
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            cell.setAlignment(Pos.CENTER);
            return cell ;
        });
    }

    private void clientStatusEventHandler() {
        serverStatusCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<FileItem,Boolean> p)
                        -> new ReadOnlyBooleanWrapper(p.getValue().getValue().isServerStatus()));
    }

    private void serverStatusEventHandler() {
        clientStatusCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<FileItem,Boolean> p)
                        -> new ReadOnlyBooleanWrapper(p.getValue().getValue().isClientStatus()));
    }

    private void treeEventHandler() {
        filesCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<FileItem,String> p)
                        -> new ReadOnlyStringWrapper(p.getValue().getValue().getFileName()));
    }

    private void setOnMousClickedOneRow() {
        treeTableView.setOnMouseClicked(event -> {
            if(event.getClickCount()==2){
                String item = treeTableView.getSelectionModel().getSelectedItem().getValue().getFileName();
                currentNode = treeTableView.getSelectionModel().getSelectedItem();

                if (isDelModeEnabled) currentNode.getValue().setToDelete(true);

                pathToSend = currentNode.getValue().getPath();
                log.debug("pathTosend :" + pathToSend.toString());
                if(Files.isDirectory(ROOT_DIR.resolve(pathToSend))) net.sendFile(new ListRequest(pathToSend.toString()));
                else defaultMesages.setText("Отправить на сервер этот файл ? :" + item);
            }
        });
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
        log.debug(fileFolder.toString());

        List<FileItem> result = new ArrayList<>();
        for (int i = 0; i < listIncome.size(); i++) {
            Path path =Paths.get(listIncome.get(i));
            Path fullPath =ROOT_DIR.resolve(path);
            if (fileFolder.get(i)&&!Files.exists(fullPath)) {
                try {
                    Files.createDirectory(fullPath);
                } catch (IOException e) {
                    log.debug("не смог создать папку" + fullPath);
                }
            }
            result.add(new FileItem(path,fileFolder.get(i),!isServersList,isServersList));
        }

        log.debug( result.toString());
        return result;
    }
    @FXML
    private void fileOrFolderEdit(TreeTableColumn.CellEditEvent<FileItem, String> fileItemStringCellEditEvent) {
        // TODO: 24.10.2021 добавить возморжность переименования файлов и папок

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
    @FXML
    public void closeApp(){
        log.debug("on close");
        net.sendFile(new DisconnectRequest());
        Scene scene = defaultMesages.getScene();
        Stage stage = (Stage) scene.getWindow();
        stage.hide();



    }
    @FXML
    private void createNewFolder(){
       FileItem fi = treeTableView.getSelectionModel().getSelectedItem().getValue();
       Path p = ROOT_DIR.resolve(fi.getPath());
       openCreateFolderWindow();



    }

    private void openCreateFolderWindow() {


        TextInputDialog dialog = new TextInputDialog("Create");

        dialog.setTitle("New Folder");
        dialog.setHeaderText("Enter folders name:");
        dialog.setContentText("Name:");

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(name -> {
            createFolder(name);

        });

    }

    private void createFolder(String newFileName) {
        FileItem fi = treeTableView.getSelectionModel().getSelectedItem().getValue();
        Path p = ROOT_DIR.resolve(fi.getPath());

        try {
            if(Files.isDirectory(p)){
                p = p.resolve(newFileName);
                Files.createDirectory(p);
                Path toFileitem = fi.getPath().resolve(newFileName);
                treeTableView.getSelectionModel()
                        .getSelectedItem().getChildren()
                        .add(new TreeItem<>(new FileItem(toFileitem,true,true,false),new ImageView(folderIcon)));
            }else {
                p= p.getParent().resolve(newFileName);
                Files.createDirectory(p);
                Path toFileitem = fi.getPath().getParent().resolve(newFileName);
                treeTableView.getSelectionModel()
                        .getSelectedItem().getParent()
                        .getChildren().add(new TreeItem<>(new FileItem(toFileitem,true,true,false),new ImageView(folderIcon)));
            }

        }catch (IOException e){
            log.debug("cant create dir");
        }
    }
    @FXML
    private void goToUploadScene(){

        Scene scene = treeTableView.getScene();

        Stage stage = (Stage) scene.getWindow();
        stage.setOnHidden(e->log.debug(" сменили метод зарытия "));
        stage.hide();

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("upload_form.fxml"));
            Parent parent = fxmlLoader.load();
            UfController ufController = fxmlLoader.getController();
            stage.setOnHidden(e-> ufController.backToMainWindow());

            stage.setTitle("Выберите файлы для загрузки в облако");


            stage.setScene(new Scene(parent));

            stage.show();

        } catch (IOException e) {
            log.error("cant open new window",e);
        }
    }

    public static Path getUserDir() {
        return userDir;
    }
}
