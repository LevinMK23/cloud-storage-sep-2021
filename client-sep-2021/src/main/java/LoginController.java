import com.geekbrains.CommandType;
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
        stage.hide();

        Parent parent = null;
        try {
            parent = FXMLLoader.load(getClass().getResource("registr_form.fxml"));
        } catch (IOException e) {
            log.error("cant open new window",e);
        }

        stage.setScene(new Scene(parent));

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
                   stage.hide();
                    Parent parent = null;
                    try {
                        parent = FXMLLoader.load(getClass().getResource("chat.fxml"));
                    } catch (IOException e) {
                        log.error("cant open new window",e);
                    }

                    stage.setScene(new Scene(parent));
                    stage.show();


    }
}


