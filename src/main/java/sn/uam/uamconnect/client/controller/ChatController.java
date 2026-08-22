package sn.uam.uamconnect.client.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import sn.uam.uamconnect.client.network.ClientTransfertFichier;
import sn.uam.uamconnect.common.Fichier;
import sn.uam.uamconnect.common.Annonce;
import sn.uam.uamconnect.client.network.ClientReseau;
import sn.uam.uamconnect.common.Message;
import sn.uam.uamconnect.common.RequeteReseau;
import sn.uam.uamconnect.common.Salon;
import sn.uam.uamconnect.common.User;
import sn.uam.uamconnect.client.view.NotificationToast;

import java.util.List;
import java.nio.file.Path;

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
    @FXML private Button boutonJoindreFichier;
    @FXML private ListView<Fichier> listeFichiers;

    private ClientReseau clientReseau;
    private User utilisateurConnecte;
    private Salon salonActif;
    private User destinataireActif; // null si on est dans un salon, sinon conversation privée avec cet utilisateur

    private final ObservableList<Salon> donneesSalons = FXCollections.observableArrayList();
    private final ObservableList<User> donneesUtilisateurs = FXCollections.observableArrayList();
    private final ObservableList<Message> donneesMessages = FXCollections.observableArrayList();
    private final ObservableList<Annonce> donneesAnnonces = FXCollections.observableArrayList();
    private final ObservableList<Fichier> donneesFichiers = FXCollections.observableArrayList();
    private final ClientTransfertFichier clientTransfertFichier = new ClientTransfertFichier();

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
        configurerListeFichiers();

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
        listeFichiers.setItems(donneesFichiers);
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

        listeUtilisateurs.getSelectionModel().selectedItemProperty().addListener((obs, ancien, nouveau) -> {
            if (nouveau != null) {
                ouvrirConversationPrivee(nouveau);
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


    private void ouvrirSalon(Salon salon) {
        this.salonActif = salon;
        this.destinataireActif = null; // on quitte le mode privé si on était dedans
        listeUtilisateurs.getSelectionModel().clearSelection();

        labelSalonActif.setText("# " + salon.getNom());
        donneesMessages.clear();

        clientReseau.envoyer(new RequeteReseau(RequeteReseau.Type.HISTORIQUE_SALON_DEMANDE, salon.getId()));
        donneesFichiers.clear();
        clientReseau.envoyer(new RequeteReseau(RequeteReseau.Type.FICHIER_LISTE_DEMANDE, salon.getId()));
    }

    /**
     * Ouvre une conversation privée avec un utilisateur (clic dans la liste des connectés).
     */
    private void ouvrirConversationPrivee(User autreUtilisateur) {
        if (autreUtilisateur.getId() == utilisateurConnecte.getId()) {
            return; // on ne peut pas se parler à soi-même
        }

        this.destinataireActif = autreUtilisateur;
        this.salonActif = null; // on quitte le mode salon
        listeSalons.getSelectionModel().clearSelection();

        labelSalonActif.setText("💬 " + autreUtilisateur.getUsername() + " (privé)");
        donneesMessages.clear();

        clientReseau.envoyer(new RequeteReseau(RequeteReseau.Type.HISTORIQUE_PRIVE_DEMANDE, autreUtilisateur.getId()));
    }

    /**
     * Envoie le message saisi dans le salon actuellement ouvert.
     */
    @FXML
    private void envoyerMessage() {
        String contenu = champMessage.getText().trim();

        if (contenu.isEmpty()) {
            return;
        }

        Message message;

        if (salonActif != null) {
            message = Message.creerMessageSalon(utilisateurConnecte.getId(), salonActif.getId(), contenu);
        } else if (destinataireActif != null) {
            message = Message.creerMessagePrive(utilisateurConnecte.getId(), destinataireActif.getId(), contenu);
        } else {
            return; // aucune conversation active
        }

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
                    boolean concerneSalonActif = salonActif != null && message.getSalonId() != null
                            && message.getSalonId().equals(salonActif.getId());

                    boolean concerneConversationPrivee = destinataireActif != null && message.estMessagePrive()
                            && (message.getExpediteurId() == destinataireActif.getId()
                            || message.getDestinataireId() == destinataireActif.getId());

                    if (concerneSalonActif || concerneConversationPrivee) {
                        donneesMessages.add(message);
                        listeMessages.scrollTo(donneesMessages.size() - 1);
                    } else if (message.getExpediteurId() != utilisateurConnecte.getId()) {
                        // Le message concerne une autre conversation que celle actuellement affichée :
                        // on notifie l'utilisateur au lieu de l'ajouter silencieusement à la liste
                        String texteNotif = message.estMessagePrive()
                                ? "Nouveau message privé de " + message.getExpediteurUsername()
                                : message.getExpediteurUsername() + " a écrit dans un autre salon.";
                        NotificationToast.afficher(champMessage.getScene().getWindow(), texteNotif);
                    }
                });
            }

            case NOTIFICATION -> {
                String texte = (String) requete.getPayload();
                Platform.runLater(() -> {
                    NotificationToast.afficher(champMessage.getScene().getWindow(), texte);
                });
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

            case FICHIER_LISTE_REPONSE -> {
                @SuppressWarnings("unchecked")
                List<Fichier> fichiers = (List<Fichier>) requete.getPayload();
                Platform.runLater(() -> donneesFichiers.setAll(fichiers));
            }

            case FICHIER_RECEPTION -> {
                Fichier fichier = (Fichier) requete.getPayload();
                Platform.runLater(() -> {
                    if (salonActif != null && fichier.getSalonId() != null
                            && fichier.getSalonId().equals(salonActif.getId())) {
                        donneesFichiers.add(0, fichier);
                    }
                });
            }

            case HISTORIQUE_PRIVE_REPONSE -> {
                @SuppressWarnings("unchecked")
                List<Message> historique = (List<Message>) requete.getPayload();
                Platform.runLater(() -> {
                    donneesMessages.setAll(historique);
                    listeMessages.scrollTo(donneesMessages.size() - 1);
                });
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

    private void configurerListeFichiers() {
        listeFichiers.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Fichier fichier, boolean vide) {
                super.updateItem(fichier, vide);

                if (vide || fichier == null) {
                    setGraphic(null);
                    return;
                }

                Label nom = new Label("📄 " + fichier.getNomFichier());
                nom.getStyleClass().add("nom-fichier");

                String tailleLisible = formatTaille(fichier.getTailleOctets());
                Label meta = new Label("Envoyé par " + fichier.getExpediteurUsername() + " • " + tailleLisible);
                meta.getStyleClass().add("meta-fichier");

                Button boutonTelecharger = new Button("Télécharger");
                boutonTelecharger.getStyleClass().add("bouton-telecharger");
                boutonTelecharger.setOnAction(e -> telechargerFichier(fichier));

                VBox infosTexte = new VBox(4, nom, meta);
                HBox ligne = new HBox(16, infosTexte, boutonTelecharger);
                ligne.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(infosTexte, javafx.scene.layout.Priority.ALWAYS);

                VBox carte = new VBox(ligne);
                carte.getStyleClass().add("carte-fichier");
                VBox.setMargin(carte, new javafx.geometry.Insets(6, 12, 6, 12));

                setGraphic(carte);
            }
        });
    }

    private String formatTaille(long octets) {
        if (octets < 1024) return octets + " o";
        if (octets < 1024 * 1024) return String.format("%.1f Ko", octets / 1024.0);
        return String.format("%.1f Mo", octets / (1024.0 * 1024));
    }

    @FXML
    private void choisirFichier() {
        if (salonActif == null) {
            return; // pas de salon sélectionné, on ne peut pas envoyer
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un fichier à partager");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Fichiers supportés", "*.pdf", "*.png", "*.jpg", "*.jpeg", "*.zip"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        java.io.File fichierChoisi = fileChooser.showOpenDialog(champMessage.getScene().getWindow());

        if (fichierChoisi == null) {
            return; // utilisateur a annulé
        }

        // L'envoi réseau se fait dans un thread séparé pour ne jamais bloquer l'interface
        new Thread(() -> {
            try {
                clientTransfertFichier.envoyerFichier(
                        fichierChoisi.toPath(),
                        utilisateurConnecte.getId(),
                        -1, // pas de destinataire privé
                        salonActif.getId()
                );
            } catch (java.io.IOException e) {
                System.err.println("Erreur lors de l'envoi du fichier : " + e.getMessage());
            }
        }).start();
    }

    private void telechargerFichier(Fichier fichier) {
        javafx.stage.DirectoryChooser dossierChooser = new javafx.stage.DirectoryChooser();
        dossierChooser.setTitle("Choisir où enregistrer le fichier");

        java.io.File dossierChoisi = dossierChooser.showDialog(champMessage.getScene().getWindow());

        if (dossierChoisi == null) {
            return;
        }

        Path destination = Path.of(dossierChoisi.getAbsolutePath(), fichier.getNomFichier());

        new Thread(() -> {
            try {
                clientTransfertFichier.telechargerFichier(fichier.getId(), destination);
                System.out.println("✅ Fichier téléchargé : " + destination);
            } catch (java.io.IOException e) {
                System.err.println("Erreur lors du téléchargement : " + e.getMessage());
            }
        }).start();
    }
}