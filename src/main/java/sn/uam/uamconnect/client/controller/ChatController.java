package sn.uam.uamconnect.client.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.TextArea;
import sn.uam.uamconnect.common.Annonce;
import sn.uam.uamconnect.client.network.ClientReseau;
import sn.uam.uamconnect.common.Message;
import sn.uam.uamconnect.common.RequeteReseau;
import sn.uam.uamconnect.common.Salon;
import sn.uam.uamconnect.common.User;

import java.util.List;

/**
 * Contrôleur de l'écran principal : salons, utilisateurs connectés, messagerie temps réel.
 */
public class ChatController {

    @FXML private ListView<Salon> listeSalons;
    @FXML private ListView<User> listeUtilisateurs;
    @FXML private ListView<Message> listeMessages;
    @FXML private TextField champMessage;
    @FXML private Button boutonEnvoyer;
    @FXML private Label labelSalonActif;
    @FXML private Label labelUtilisateurConnecte;
    @FXML private VBox formulaireAnnonce;
    @FXML private TextField champTitreAnnonce;
    @FXML private TextArea champContenuAnnonce;
    @FXML private Button boutonPublierAnnonce;
    @FXML private ListView<Annonce> listeAnnonces;

    private ClientReseau clientReseau;
    private User utilisateurConnecte;
    private Salon salonActif;

    private final ObservableList<Salon> donneesSalons = FXCollections.observableArrayList();
    private final ObservableList<User> donneesUtilisateurs = FXCollections.observableArrayList();
    private final ObservableList<Message> donneesMessages = FXCollections.observableArrayList();
    private final ObservableList<Annonce> donneesAnnonces = FXCollections.observableArrayList();

    /**
     * Appelée manuellement depuis ConnexionController juste après le chargement du FXML,
     * car clientReseau et utilisateurConnecte ne sont pas disponibles au moment de initialize().
     */
    public void initialiserDonnees(ClientReseau clientReseau, User utilisateurConnecte) {
        this.clientReseau = clientReseau;
        this.utilisateurConnecte = utilisateurConnecte;

        labelUtilisateurConnecte.setText(
                utilisateurConnecte.getNomComplet() + " (" + utilisateurConnecte.getRole() + ")"
        );

        configurerListes();

        // À partir de maintenant, c'est ce contrôleur qui traite tous les messages entrants
        clientReseau.setEcouteurMessages(this::traiterMessageServeur);
        clientReseau.envoyer(new RequeteReseau(RequeteReseau.Type.LISTE_SALONS_DEMANDE,
                null));
        clientReseau.envoyer(new RequeteReseau(RequeteReseau.Type.LISTE_UTILISATEURS_DEMANDE,
                null));
        // Le formulaire de publication n'est visible que pour Enseignant et Administrateur
        boolean peutPublier = utilisateurConnecte.getRole() != User.Role.ETUDIANT;
        formulaireAnnonce.setVisible(peutPublier);
        formulaireAnnonce.setManaged(peutPublier);

        configurerListeAnnonces();
        clientReseau.envoyer(new RequeteReseau(RequeteReseau.Type.LISTE_ANNONCES_DEMANDE,
                null));
    }

    @FXML
    public void initialize() {
        listeSalons.setItems(donneesSalons);
        listeUtilisateurs.setItems(donneesUtilisateurs);
        listeMessages.setItems(donneesMessages);
        listeAnnonces.setItems(donneesAnnonces);
    }

    /**
     * Configure l'affichage personnalisé des listes (cell factories).
     */
    private void configurerListes() {
        // Affichage des salons : juste le nom (Salon.toString() le fait déjà)
        listeSalons.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Salon salon, boolean vide) {
                super.updateItem(salon, vide);
                setText(vide || salon == null ? null : "# " + salon.getNom());
            }
        });

        listeSalons.getSelectionModel().selectedItemProperty().addListener((obs, ancien, nouveau) -> {
            if (nouveau != null) {
                ouvrirSalon(nouveau);
            }
        });

        // Affichage des utilisateurs connectés avec un petit point vert
        listeUtilisateurs.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean vide) {
                super.updateItem(user, vide);
                setText(vide || user == null ? null : "🟢 " + user.getUsername());
            }
        });

        // Affichage des messages en "bulles" façon messagerie
        listeMessages.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Message message, boolean vide) {
                super.updateItem(message, vide);

                if (vide || message == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                boolean estMoi = message.getExpediteurId() == utilisateurConnecte.getId();

                Label texteAuteur = new Label(message.getExpediteurUsername());
                texteAuteur.getStyleClass().add("auteur-message");

                Label texteMessage = new Label(message.getContenu());
                texteMessage.setWrapText(true);
                texteMessage.getStyleClass().add(estMoi ? "texte-message-moi" : "texte-message-autre");

                VBox bulle = new VBox(2, texteAuteur, texteMessage);
                bulle.getStyleClass().add(estMoi ? "bulle-message-moi" : "bulle-message-autre");

                if (estMoi) {
                    bulle.setAlignment(Pos.CENTER_RIGHT);
                    setAlignment(Pos.CENTER_RIGHT);
                } else {
                    setAlignment(Pos.CENTER_LEFT);
                }

                setGraphic(bulle);
            }
        });
    }


    /**
     * Configure l'affichage des annonces sous forme de "cartes".
     */
    private void configurerListeAnnonces() {
        listeAnnonces.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Annonce annonce, boolean vide) {
                super.updateItem(annonce, vide);

                if (vide || annonce == null) {
                    setGraphic(null);
                    return;
                }

                Label titre = new Label(annonce.getTitre());
                titre.getStyleClass().add("titre-annonce");

                Label meta = new Label("Par " + annonce.getAuteurUsername());
                meta.getStyleClass().add("meta-annonce");

                Label contenu = new Label(annonce.getContenu());
                contenu.setWrapText(true);
                contenu.getStyleClass().add("contenu-annonce");

                VBox carte = new VBox(6, titre, meta, contenu);
                carte.getStyleClass().add("carte-annonce");
                VBox.setMargin(carte, new javafx.geometry.Insets(6, 12, 6, 12));

                setGraphic(carte);
            }
        });
    }

    /**
     * Change de salon actif : met à jour l'affichage et demande l'historique au serveur.
     */
    private void ouvrirSalon(Salon salon) {
        this.salonActif = salon;
        labelSalonActif.setText("# " + salon.getNom());
        donneesMessages.clear();

        clientReseau.envoyer(new RequeteReseau(RequeteReseau.Type.HISTORIQUE_SALON_DEMANDE, salon.getId()));
    }

    /**
     * Envoie le message saisi dans le salon actuellement ouvert.
     */
    @FXML
    private void envoyerMessage() {
        String contenu = champMessage.getText().trim();

        if (contenu.isEmpty() || salonActif == null) {
            return;
        }

        Message message = Message.creerMessageSalon(utilisateurConnecte.getId(), salonActif.getId(), contenu);
        clientReseau.envoyer(new RequeteReseau(RequeteReseau.Type.MESSAGE_ENVOI, message));

        champMessage.clear();
    }

    /**
     * Traite tous les messages entrants du serveur (thread réseau -> UI via Platform.runLater).
     */
    private void traiterMessageServeur(RequeteReseau requete) {
        switch (requete.getType()) {

            case LISTE_SALONS -> {
                @SuppressWarnings("unchecked")
                List<Salon> salons = (List<Salon>) requete.getPayload();
                Platform.runLater(() -> donneesSalons.setAll(salons));
            }

            case LISTE_UTILISATEURS -> {
                @SuppressWarnings("unchecked")
                List<User> utilisateurs = (List<User>) requete.getPayload();
                Platform.runLater(() -> donneesUtilisateurs.setAll(utilisateurs));
            }

            case HISTORIQUE_SALON_REPONSE -> {
                @SuppressWarnings("unchecked")
                List<Message> historique = (List<Message>) requete.getPayload();
                Platform.runLater(() -> {
                    donneesMessages.setAll(historique);
                    listeMessages.scrollTo(donneesMessages.size() - 1);
                });
            }

            case MESSAGE_RECEPTION -> {
                Message message = (Message) requete.getPayload();
                Platform.runLater(() -> {
                    // N'affiche que si le message concerne le salon actuellement ouvert
                    if (salonActif != null && message.getSalonId() != null
                            && message.getSalonId().equals(salonActif.getId())) {
                        donneesMessages.add(message);
                        listeMessages.scrollTo(donneesMessages.size() - 1);
                    }
                });
            }

            case NOTIFICATION -> {
                String texte = (String) requete.getPayload();
                System.out.println("[NOTIFICATION] " + texte);
                // TODO étape notifications avancées : afficher une petite popup/toast
            }

            case LISTE_ANNONCES_REPONSE -> {
                @SuppressWarnings("unchecked")
                List<Annonce> annonces = (List<Annonce>) requete.getPayload();
                Platform.runLater(() -> donneesAnnonces.setAll(annonces));
            }

            case ANNONCE_NOUVELLE -> {
                Annonce annonce = (Annonce) requete.getPayload();
                Platform.runLater(() -> donneesAnnonces.add(0, annonce)); // ajoutée en haut de la liste
            }

            default -> { /* ignoré ici */ }
        }
    }

    /**
     * Déconnexion propre : ferme le socket et revient à l'écran de connexion.
     */
    @FXML
    private void seDeconnecter() {
        clientReseau.deconnecter();
        Stage stage = (Stage) champMessage.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void publierAnnonce() {
        String titre = champTitreAnnonce.getText().trim();
        String contenu = champContenuAnnonce.getText().trim();

        if (titre.isEmpty() || contenu.isEmpty()) {
            return;
        }

        Annonce annonce = new Annonce(titre, contenu, utilisateurConnecte.getId(), null);
        clientReseau.envoyer(new RequeteReseau(RequeteReseau.Type.ANNONCE_PUBLICATION, annonce));

        champTitreAnnonce.clear();
        champContenuAnnonce.clear();
    }
}