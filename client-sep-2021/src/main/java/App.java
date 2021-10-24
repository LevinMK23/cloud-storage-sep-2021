import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("login_form.fxml"));
        Parent parent = fxmlLoader.load();
        LoginController loginController = fxmlLoader.getController();
        primaryStage.setOnHidden(e->loginController.onClose());
        primaryStage.setTitle("Авторизация");

        primaryStage.setScene(new Scene(parent));


        primaryStage.show();
    }
}
