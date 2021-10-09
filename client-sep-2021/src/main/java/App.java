import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    public static void setCurrent(Stage current) {
        App.current = current;
    }

    public static Stage getCurrent() {
        return current;
    }

    private static Stage current = null;
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent parent = FXMLLoader.load(getClass().getResource("login_form.fxml"));
        primaryStage.setScene(new Scene(parent));
        current = primaryStage;
        primaryStage.show();
    }
}
