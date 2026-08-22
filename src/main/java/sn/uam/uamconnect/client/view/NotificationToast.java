package sn.uam.uamconnect.client.view;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.util.Duration;
import javafx.stage.Window;

/**
 * Affiche une notification "toast" discrète en haut à droite de l'écran,
 * qui apparaît en fondu puis disparaît automatiquement (section 4.9 du sujet).
 */
public class NotificationToast {

    /**
     * Affiche une notification au-dessus de la fenêtre donnée.
     *
     * @param fenetrePropriétaire la fenêtre JavaFX sur laquelle ancrer la popup
     * @param message             le texte à afficher (ex: "Aminata vient de se connecter.")
     */
    public static void afficher(Window fenetrePropriétaire, String message) {
        Label label = new Label("🔔 " + message);
        label.setStyle(
                "-fx-background-color: #2c5282;" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 12px 20px;" +
                        "-fx-background-radius: 10px;" +
                        "-fx-font-size: 13px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0, 0, 4);"
        );
        label.setWrapText(true);
        label.setMaxWidth(320);

        StackPane conteneur = new StackPane(label);
        conteneur.setAlignment(Pos.TOP_RIGHT);
        conteneur.setMouseTransparent(true);

        Popup popup = new Popup();
        popup.getContent().add(conteneur);
        popup.setAutoFix(true);

        double x = fenetrePropriétaire.getX() + fenetrePropriétaire.getWidth() - 340;
        double y = fenetrePropriétaire.getY() + 70;

        popup.show(fenetrePropriétaire, x, y);

        label.setOpacity(0);
        FadeTransition apparition = new FadeTransition(Duration.millis(300), label);
        apparition.setFromValue(0);
        apparition.setToValue(1);

        PauseTransition pause = new PauseTransition(Duration.seconds(3));

        FadeTransition disparition = new FadeTransition(Duration.millis(400), label);
        disparition.setFromValue(1);
        disparition.setToValue(0);
        disparition.setOnFinished(e -> popup.hide());

        SequentialTransition sequence = new SequentialTransition(apparition, pause, disparition);
        sequence.play();
    }
}