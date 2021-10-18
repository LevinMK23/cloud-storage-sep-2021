import com.geekbrains.CommandType;
import com.geekbrains.LoginResponse;
import com.geekbrains.RegistrationRequest;
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
public class RegistrController implements Initializable {
    @FXML
    TextField userNameReg;
    @FXML
    TextField passFieldReg;
    @FXML
    TextField confirmPassReg;
    private boolean isLogin = false;
    private Net net;


    @FXML
    private void backToLogin(ActionEvent actionEvent){
        Button button = (Button) actionEvent.getSource();

        Stage stage = (Stage)button.getScene().getWindow();
        stage.hide();

        Parent parent = null;
        try {
            parent = FXMLLoader.load(getClass().getResource("login_form.fxml"));
        } catch (IOException e) {
            log.error("cant open new window",e);
        }

        stage.setScene(new Scene(parent));

        stage.show();
    }

    @FXML
    private void sendRegistrationInfo(ActionEvent actionEvent){
        String login = userNameReg.getText();
        String pass = passFieldReg.getText();
        String confirm = confirmPassReg.getText();
        if(login.length()>0&&pass.length()>0&&pass.equals(confirm)){
            log.debug("sended ----"+login+" : "+pass);
            net.sendFile(new RegistrationRequest(login,pass));
        }else {
            passFieldReg.clear();
            confirmPassReg.clear();

        }

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.debug("reg controller started");
        net = Net.getInstance(s->Platform.runLater(()->{

            System.out.println("in registration controller");
            if(s.getType().equals(CommandType.LOGIN_RESPONSE)){
                changeScene() ;
            }}));




    }
    public void changeScene(){

        Scene scene = passFieldReg.getScene();
        Stage stage = (Stage) scene.getWindow();
        stage.hide();
        Parent parent = null;
        try {
            parent = FXMLLoader.load(getClass().getResource("app_form.fxml"));
        } catch (IOException e) {
            log.error("cant open new window",e);
        }

        stage.setScene(new Scene(parent));
        stage.show();


    }

}
