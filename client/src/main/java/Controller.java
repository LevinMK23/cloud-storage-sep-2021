import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    public ListView<String> listView;
    public TextField inputText;
    private DataOutputStream os;
    private DataInputStream is;
    private static final String FILES_FOLDER = "client/src/main/resources/files/";



    public void send(ActionEvent event) throws IOException {
        String fileName =inputText.getText();
        String msg = "{F_N}"+ fileName;
        inputText.clear();
        File file = new File(FILES_FOLDER+fileName);
        if(!file.exists()){
            listView.getItems().add("не верный ввод");
        }else {
            os.writeUTF(msg);
            os.flush();

            try(FileInputStream fis = new FileInputStream(file)){
                int readed = 0;
                int avaliable = fis.available();
                byte[] buffer = new byte[avaliable];
                os.writeInt(avaliable);
                os.write(buffer);
                os.writeUTF("{END}");
                System.out.println("передал файл");
            }catch (Exception e ){
                e.printStackTrace();
            }
        }






    }
    public void getAllFiles(File folder, String offset){
        File[] files = folder.listFiles();
        for (File entry: files) {
            if(entry.isDirectory()){
                listView.getItems().add(offset + "[]"+ entry.getName());
                getAllFiles(entry,offset+" ");

            }else {
                listView.getItems().add(offset+ ">>"+ entry.getName());
            }


        }
    }
    public void getAllFiles(File folder){
        getAllFiles(folder,"");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try{
            Socket socket = new Socket("localhost",8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            File files = new File(FILES_FOLDER);
            if(!files.exists()){
                files.mkdir();
                File file = new File(FILES_FOLDER + "empty.txt");
                file.createNewFile();


            }
            getAllFiles(files);
            Thread deamon = new Thread(()->{
                try{
                    while (true){
                        String msg = is.readUTF();
                        System.out.println("Recived new msg : "+ msg );
                        Platform.runLater(()->{
                            listView.getItems().add(msg);
                        });

                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            });
            deamon.setDaemon(true);
            deamon.start();


        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
