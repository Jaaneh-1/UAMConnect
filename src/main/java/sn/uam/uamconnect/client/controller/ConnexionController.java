package sn.uam.uamconnect.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import sn.uam.uamconnect.client.network.ClientReseau;
import sn.uam.uamconnect.common.RequeteReseau;
import sn.uam.uamconnect.common.User;

import java.io.IOException;

/**
 * Contrôleur de l'écran de connexion.
 * Relie l'interface FXML (connexion-view.fxml) à la logique réseau (ClientReseau).
 */
public class ConnexionController {

    @FXML
    private TextField champUsername;

    @FXML
    private PasswordField champMotDePasse;

    @FXML
    private Label labelErreur;

    @FXML
    private Button boutonConnexion;

    private ClientReseau clientReseau;

    /**
     * Appelée automatiquement par JavaFX juste après le chargement du FXML.
     * On y initialise la connexion réseau.
     */
    @FXML
    public void initialize() {
        clientReseau = new ClientReseau();

        try {
            clientReseau.connecter();
        } catch (IOException e) {
            afficherErreur("Impossible de joindre le serveur. Vérifiez qu'il est démarré.");
            boutonConnexion.setDisable(true);
        }
    }

    /**
     * Appelée quand l'utilisateur clique sur "Se connecter" (liée via onAction="#seConnecter" dans le FXML).
     */
    @FXML
    private void seConnecter() {
        String username = champUsername.getText().trim();
        String motDePasse = champMotDePasse.getText();

        if (username.isEmpty() || motDePasse.isEmpty()) {
            afficherErreur("Veuillez remplir tous les champs.");
            return;
        }

        boutonConnexion.setDisable(true);
        labelErreur.setVisible(false);

        // On écoute la réponse du serveur AVANT d'envoyer la demande
        clientReseau.setEcouteurMessages(this::traiterReponseServeur);

        RequeteReseau requete = new RequeteReseau(
                RequeteReseau.Type.CONNEXION_DEMANDE,
                new String[]{username, motDePasse}
        );
        clientReseau.envoyer(requete);
    }

    /**
     * Traite les réponses du serveur. Attention : cette méthode s'exécute dans le thread
     * réseau (ClientReseau), PAS dans le thread JavaFX. Toute manipulation de l'interface
     * doit passer par Platform.runLater(...) pour éviter une exception.
     */
    private void traiterReponseServeur(RequeteReseau requete) {
        switch (requete.getType()) {

            case CONNEXION_REPONSE_OK -> {
                User utilisateurConnecte = (User) requete.getPayload();
                Platform.runLater(() -> ouvrirEcranPrincipal(utilisateurConnecte));
            }

            case CONNEXION_REPONSE_ECHEC -> {
                String messageErreur = (String) requete.getPayload();
                Platform.runLater(() -> {
                    afficherErreur(messageErreur);
                    boutonConnexion.setDisable(false);
                });
            }

            default -> {
                // Autres types de requêtes possibles reçus avant même l'ouverture de l'écran principal :
                // on les ignore ici, ils seront traités par le contrôleur principal une fois connecté
            }
        }
    }

    /**
     * Affiche un message d'erreur sous le formulaire.
     */
    private void afficherErreur(String message) {
        labelErreur.setText(message);
        labelErreur.setVisible(true);
    }


    private void ouvrirEcranPrincipal(User utilisateurConnecte) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/sn/uam/uamconnect/client/view/chat-view.fxml")
            );
            javafx.scene.Parent racine = loader.load();

            sn.uam.uamconnect.client.controller.ChatController chatController = loader.getController();
            chatController.initialiserDonnees(clientReseau, utilisateurConnecte);

            javafx.scene.Scene scene = new javafx.scene.Scene(racine, 1100, 700);
            scene.getStylesheets().add(
                    getClass().getResource("/sn/uam/uamconnect/client/view/style.css").toExternalForm()
            );

            javafx.stage.Stage stage = (javafx.stage.Stage) boutonConnexion.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("UAM Connect — " + utilisateurConnecte.getUsername());

        } catch (java.io.IOException e) {
            afficherErreur("Erreur lors du chargement de l'interface principale.");
            e.printStackTrace();
        }
    }
}