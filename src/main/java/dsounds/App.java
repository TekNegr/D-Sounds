package dsounds;

import dsounds.controllers.LocalAuthController;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;
    private static LocalAuthController authController;
    private static String pendingSongSelectionId;

    @Override
    public void start(Stage stage) throws IOException {
        authController = LocalAuthController.createDefault();
        try {
            authController.loadUsers();
        } catch (ClassNotFoundException ex) {
            throw new IOException("Could not load user storage.", ex);
        }

        scene = new Scene(loadFXML("auth"), 760, 560);
        stage.setScene(scene);
        stage.setTitle("dSounds");
        stage.show();
    }

    @Override
    public void stop() throws IOException {
        if (authController != null) {
            authController.saveUsers();
        }
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    public static LocalAuthController getAuthController() {
        return authController;
    }

    public static void setPendingSongSelectionId(String songId) {
        pendingSongSelectionId = songId;
    }

    public static String consumePendingSongSelectionId() {
        String songId = pendingSongSelectionId;
        pendingSongSelectionId = null;
        return songId;
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }

}