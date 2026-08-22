package sn.uam.uamconnect.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Point d'entrée de l'application cliente JavaFX.
 */
public class Launcher extends Application {

    @Override
    public void start(Stage stagePrincipal) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/sn/uam/uamconnect/client/view/connexion-view.fxml")
        );
        Parent racine = loader.load();

        Scene scene = new Scene(racine, 900, 650);
        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/sn/uam/uamconnect/client/view/style.css")
                ).toExternalForm()
        );

        stagePrincipal.setTitle("UAM Connect");
        stagePrincipal.setScene(scene);
        stagePrincipal.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}