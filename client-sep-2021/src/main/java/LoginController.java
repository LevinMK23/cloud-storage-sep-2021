import com.geekbrains.CommandType;
import com.geekbrains.DisconnectRequest;
import com.geekbrains.LoginRequest;
import com.geekbrains.LoginResponse;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
public class LoginController implements Initializable {
    @FXML
    TextField loginField;
    @FXML
    TextField passField;
    private boolean  isLogin = false;
    private Net net;


    @FXML
    private void changeToRegistrForm(ActionEvent actionEvent){
        Button button = (Button) actionEvent.getSource();

        Stage stage = (Stage)button.getScene().getWindow();
        stage.setOnHidden(e->log.debug(" сменили метод зарытия "));
        stage.hide();


        try {

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("registr_form.fxml"));
            Parent parent = fxmlLoader.load();
            RegistrController rc = fxmlLoader.getController();
            stage.setScene(new Scene(parent));
            stage.setOnHidden(e-> rc.onClose());
            stage.setTitle("Регистрация нового пользователя");

        } catch (IOException e) {
            log.error("cant open new window",e);
        }



        stage.show();

    }
    @FXML
    private void login(ActionEvent actionEvent){

        String login = loginField.getText();
        String pass = passField.getText();
        net.sendFile(new LoginRequest(login,pass));

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.debug("login controller started");

        net = Net.getInstance(s->Platform.runLater(()->{

            System.out.println("in login controller");
            if(s.getType().equals(CommandType.LOGIN_RESPONSE)){
                changeScene() ;
            }}));

    }
    public void changeScene(){

                   Scene scene = passField.getScene();

                   Stage stage = (Stage) scene.getWindow();
                   stage.setOnHidden(e->log.debug(" сменили метод зарытия "));
                   stage.hide();

                    try {
                        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("app_form.fxml"));
                        Parent parent = fxmlLoader.load();
                        Controller controller = fxmlLoader.getController();
                        stage.setOnHidden(e-> controller.closeApp());
                        stage.setTitle("Облако");

                        stage.setScene(new Scene(parent));
                        stage.show();

                    } catch (IOException e) {
                        log.error("cant open new window",e);
                    }




    }
    public void onClose(){
        log.debug("on close");
        net.sendFile(new DisconnectRequest());
    }
}


