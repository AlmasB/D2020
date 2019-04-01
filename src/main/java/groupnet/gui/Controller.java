package groupnet.gui;

import groupnet.GroupNetApp;
import groupnet.euler.Description;
import groupnet.gn.GNDescription;
import groupnet.ui.EulerDiagramVisTask;
import groupnet.ui.GNDiagramVisTask;
import groupnet.ui.Renderer;
import groupnet.ui.VisTask;
import groupnet.util.Examples;
import groupnet.util.Log;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

/**
 * @author Almas Baimagambetov (AlmasB) (almaslvl@gmail.com)
 */
public class Controller {

    private SettingsController settings;

    @FXML
    private Renderer renderer;

    @FXML
    private Menu menuDiagrams;

    @FXML
    private Menu menuGNDiagrams;

    @FXML
    private TextField fieldInput;

    @FXML
    private TextField fieldEdges;

    @FXML
    private TextField fieldSNAP;

    private Alert progressDialog = new Alert(Alert.AlertType.INFORMATION);

    private String SNAP_ID = "";

    private VisualizationService visService = new VisualizationService();

    public void initialize() {
        initSettingsDialog();

        initInputFields();

        initMenuDiagrams();

        progressDialog.setTitle("Working...");
        progressDialog.setHeaderText("Generating...");

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressDialog.getDialogPane().setContent(progressIndicator);
    }

    private void initSettingsDialog() {
        dialogSettings = new Dialog<>();
        dialogSettings.setTitle("Settings");
        dialogSettings.getDialogPane().getButtonTypes().add(ButtonType.OK);

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ui_settings.fxml"));

            Parent root = fxmlLoader.load();
            settings = fxmlLoader.getController();

            // set global settings hack
            GroupNetApp.getInstance().setSettings(settings);

            dialogSettings.getDialogPane().setContent(root);
        } catch (Exception e) {
            showError(e);
        }
    }

    private void initInputFields() {
        fieldInput.setOnAction(e -> {
            // TO DRAW EULER DIAGRAM ONLY
            Description ad = Description.from(fieldInput.getText());

            visualize(ad);
        });

        fieldEdges.setOnAction(e -> {
            // TO READ MANUALLY
            String informalGND = fieldInput.getText();

            String edgeDescription = fieldEdges.getText();

            visualize(GNDescription.from(informalGND, edgeDescription));
        });

        fieldSNAP.setOnAction(e -> {

        });
    }

    private void initMenuDiagrams() {
        Examples.INSTANCE.getList().forEach(pair -> {
            MenuItem item = new MenuItem(pair.getFirst());
            item.setOnAction(e -> {
                visualize(pair.getSecond());
            });

            menuDiagrams.getItems().addAll(item);
        });

        Examples.INSTANCE.getGndList().forEach(pair -> {
            MenuItem item = new MenuItem(pair.getFirst());
            item.setOnAction(e -> {
                visualize(pair.getSecond());
            });

            menuGNDiagrams.getItems().addAll(item);
        });
    }

    private Dialog<ButtonType> dialogSettings;

    @FXML
    private void settings() {
        dialogSettings.showAndWait();
    }

    private void showError(Throwable e) {
        System.out.println("Caught error:\n");
        e.printStackTrace();

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ooops");
        alert.setContentText(e.getMessage());
        alert.show();
    }

    private void visualize(Description description) {
        progressDialog.show();

        fieldInput.setText(description.getInformalDescription());

        visService.setInput(new EulerDiagramVisTask(description, renderer));
        visService.restart();
    }

    private void visualize(GNDescription description) {
        progressDialog.show();

        //fieldInput.setText(description.getInformalDescription());

        visService.setInput(new GNDiagramVisTask(description, renderer));
        visService.restart();
    }

    class VisualizationService extends Service<Void> {

        private VisTask<?> task;

        void setInput(VisTask<?> task) {
            this.task = task;
        }

        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() throws Exception {

                    long startTime = System.nanoTime();

                    task.run();

                    long timeTook = System.nanoTime() - startTime;

                    Log.INSTANCE.i(String.format("Drawing took: %.3f sec", timeTook / 1000000000.0));

                    return null;
                }

                @Override
                protected void succeeded() {
                    renderer.clear();
                    task.render();
                    //renderer.drawDebug();

                    progressDialog.hide();
                }

                @Override
                protected void failed() {
                    showError(getException());
                    progressDialog.hide();
                }
            };
        }
    }
}
