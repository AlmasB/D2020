package groupnet;

import groupnet.gui.SettingsController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GroupNetApp extends Application {

    private static GroupNetApp instance;

    public static GroupNetApp getInstance() {
        return instance;
    }

    private SettingsController settings;

    public SettingsController getSettings() {
        return settings;
    }

    // we'll settle for this hack for now
    public void setSettings(SettingsController settings) {
        this.settings = settings;
    }

    private Stage stage;

    public Stage getStage() {
        return stage;
    }

    @Override
    public void start(Stage stage) throws Exception {
        instance = this;
        this.stage = stage;

        Parent root = FXMLLoader.load(getClass().getResource("ui_main.fxml"));
        root.getStylesheets().add(getClass().getResource("ui_main.css").toExternalForm());

        stage.setOnCloseRequest(e -> {
            e.consume();
            System.exit(0);
        });

        stage.setScene(new Scene(root));
        stage.setTitle("GroupNet");
        stage.show();
    }

    public static void main(String args[]) {
        launch(args);
    }
}
