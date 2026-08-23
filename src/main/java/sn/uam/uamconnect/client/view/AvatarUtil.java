package sn.uam.uamconnect.client.view;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * Génère des avatars circulaires : soit une photo personnalisée, soit un
 * cercle coloré avec l'initiale du nom (à la Slack/Discord) si aucune photo.
 */
public class AvatarUtil {

    private static final String[] COULEURS = {
            "#2c5282", "#38a169", "#d69e2e", "#c53030", "#805ad5", "#dd6b20", "#319795"
    };

    /**
     * Crée un avatar circulaire à partir des initiales d'un nom (fallback par défaut).
     */
    public static StackPane creerAvatarInitiale(String nom, double taille) {
        Circle cercle = new Circle(taille / 2);
        cercle.setFill(Color.web(couleurPour(nom)));

        Label initiale = new Label(nom != null && !nom.isEmpty() ? nom.substring(0, 1).toUpperCase() : "?");
        initiale.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: " + (taille * 0.4) + "px;");

        StackPane avatar = new StackPane(cercle, initiale);
        avatar.setPrefSize(taille, taille);
        avatar.setMaxSize(taille, taille);

        return avatar;
    }

    /**
     * Crée un avatar circulaire à partir d'une image (photo de profil personnalisée).
     */
    public static StackPane creerAvatarImage(javafx.scene.image.Image image, double taille) {
        javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);
        imageView.setFitWidth(taille);
        imageView.setFitHeight(taille);
        imageView.setPreserveRatio(false);

        Circle clip = new Circle(taille / 2, taille / 2, taille / 2);
        imageView.setClip(clip);

        StackPane avatar = new StackPane(imageView);
        avatar.setPrefSize(taille, taille);
        avatar.setMaxSize(taille, taille);

        return avatar;
    }

    /**
     * Choisit une couleur stable (toujours la même) pour un nom donné,
     * basé sur le hashcode du texte.
     */
    private static String couleurPour(String texte) {
        if (texte == null || texte.isEmpty()) return COULEURS[0];
        int index = Math.abs(texte.hashCode()) % COULEURS.length;
        return COULEURS[index];
    }
}