

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Controller {

    @FXML
    VBox leftPanel, rightPanel;

    public void btnExitAction(ActionEvent actionEvent) {
        Platform.exit();
    }

    public void copyBtnAction(ActionEvent actionEvent) {
        // Получаем ссылки на контроллеры панелей
        PanelController leftPC = (PanelController) leftPanel.getProperties().get("ctrl");
        PanelController rightPC = (PanelController) rightPanel.getProperties().get("ctrl");

        // Если файл не выбран
        if (leftPC.getSelectedFilename() == null && rightPC.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        // Понимаем что куда копировать
        PanelController srcPC = null, dstPC = null;
        if(leftPC.getSelectedFilename() != null) {
            srcPC = leftPC;
            dstPC = rightPC;
        }
        if(rightPC.getSelectedFilename() != null) {
            srcPC = rightPC;
            dstPC = leftPC;
        }

        // Какой файл копировать
        Path srcPath = Paths.get(srcPC.getCurrentPath(), srcPC.getSelectedFilename());
        Path dstPath = Paths.get(dstPC.getCurrentPath()).resolve(srcPath.getFileName().toString());

        try{
            // Копирование файла из источника в точку назначения
            Files.copy(srcPath, dstPath);
            // Обновляем панель в которую скопировали, чтоб отображать новый файл
            dstPC.updateList(Paths.get(dstPC.getCurrentPath()));
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось скопировать указанный файл", ButtonType.OK);
            alert.showAndWait();
        }

        // Прогресс бар при копировании
//        ProgressBar pb = new ProgressBar();
//        for (int i = 0; i < 100; i++) {
//            pb.setProgress(1.0 / 100 * i);
//        }
    }

    public void moveBtnAction(ActionEvent actionEvent) {
        // Получаем ссылки на контроллеры панелей
        PanelController leftPC = (PanelController) leftPanel.getProperties().get("ctrl");
        PanelController rightPC = (PanelController) rightPanel.getProperties().get("ctrl");

        // Если файл не выбран
        if (leftPC.getSelectedFilename() == null && rightPC.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        // Понимаем что куда копировать
        PanelController srcPC = null, dstPC = null;
        if(leftPC.getSelectedFilename() != null) {
            srcPC = leftPC;
            dstPC = rightPC;
        }
        if(rightPC.getSelectedFilename() != null) {
            srcPC = rightPC;
            dstPC = leftPC;
        }

        // Какой файл копировать
        Path srcPath = Paths.get(srcPC.getCurrentPath(), srcPC.getSelectedFilename());
        Path dstPath = Paths.get(dstPC.getCurrentPath()).resolve(srcPath.getFileName().toString());

        try{
            // Копирование файла из источника в точку назначения
            Files.move(srcPath, dstPath);
            // Обновляем панель в которую скопировали, чтоб отображать новый файл
            dstPC.updateList(Paths.get(dstPC.getCurrentPath()));
            srcPC.updateList(Paths.get(srcPC.getCurrentPath()));
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось переместить указанный файл", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void deletedBtnAction(ActionEvent actionEvent) {
        // Получаем ссылки на контроллеры панелей
        PanelController leftPC = (PanelController) leftPanel.getProperties().get("ctrl");
        PanelController rightPC = (PanelController) rightPanel.getProperties().get("ctrl");

        // Если файл не выбран
        if (leftPC.getSelectedFilename() == null && rightPC.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        // Понимаем что куда копировать
        PanelController srcPC = null;
        if(leftPC.getSelectedFilename() != null) {
            srcPC = leftPC;
        }
        if(rightPC.getSelectedFilename() != null) {
            srcPC = rightPC;
        }

        // Какой файл копировать
        Path srcPath = Paths.get(srcPC.getCurrentPath(), srcPC.getSelectedFilename());


        try{
            // Копирование файла из источника в точку назначения
            Files.delete(srcPath);
            // Обновляем панель в которую скопировали, чтоб отображать новый файл
            srcPC.updateList(Paths.get(srcPC.getCurrentPath()));
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось удалить указанный файл", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void btnConnect(ActionEvent actionEvent) {
        // Получаем ссылки на контроллеры панелей
        PanelController leftPC = (PanelController) leftPanel.getProperties().get("ctrl");
        PanelController rightPC = (PanelController) rightPanel.getProperties().get("ctrl");

        // Если панель не выбрана
        if (!leftPC.getSelectedPanel() && !rightPC.getSelectedPanel()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Выберите окно для подключения к серверу", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        if(leftPC.getSelectedPanel()) {
            leftPC.serverConnect();
        }
        if(rightPC.getSelectedPanel()) {
            rightPC.serverConnect();
        }

    }
}
