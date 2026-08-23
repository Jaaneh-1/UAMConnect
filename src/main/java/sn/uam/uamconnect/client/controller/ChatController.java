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
import javafx.scene.control.ComboBox;
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
import javafx.scene.layout.StackPane;
import javafx.scene.image.Image;
import sn.uam.uamconnect.client.view.AvatarUtil;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

import javafx.scene.chart.PieChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import sn.uam.uamconnect.common.StatistiquesDTO;
import sn.uam.uamconnect.client.service.PdfExportService;
import javafx.stage.FileChooser;

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
    @FXML private Tab ongletUtilisateurs;
    @FXML private TextField champNouveauUsername;
    @FXML private PasswordField champNouveauMotDePasse;
    @FXML private TextField champNouveauNomComplet;
    @FXML private TextField champNouveauEmail;
    @FXML private ComboBox<User.Role> comboNouveauRole;
    @FXML private Button boutonCreerUtilisateur;
    @FXML private Label labelMessageUtilisateur;
    @FXML private ListView<User> listeGestionUtilisateurs;

    @FXML private Tab ongletDashboard;
    @FXML private VBox conteneurDashboard;

    @FXML private StackPane conteneurAvatarPersonnel;

    @FXML private Button boutonExporterSalon;
    @FXML private Button boutonExporterAnnonces;

    private final PdfExportService pdfExportService = new PdfExportService();
    private StatistiquesDTO dernieresStatistiques; // gardées en mémoire pour l'export

    private final Path dossierCachePhotos = Path.of(System.getProperty("java.io.tmpdir"), "uamconnect_photos");
    private final Map<Integer, Image> cacheImagesMemoire = new HashMap<>();

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
    private final ObservableList<User> donneesGestionUtilisateurs = FXCollections.observableArrayList();

    /**
     * Appelée manuellement depuis ConnexionController juste après le chargement du FXML,
     * car clientReseau et utilisateurConnecte ne sont pas disponibles au moment de initialize().
     */
    @FXML private TabPane ongletsPrincipaux;
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
        // L'onglet Utilisateurs n'est visible que pour l'Administrateur
        if (utilisateurConnecte.getRole() != User.Role.ADMINISTRATEUR) {
            ongletsPrincipaux.getTabs().remove(ongletUtilisateurs);
        } else {
            configurerListeGestionUtilisateurs();
            clientReseau.envoyer(new RequeteReseau(RequeteReseau.Type.UTILISATEURS_TOUS_DEMANDE, null));
        }

        afficherAvatar(conteneurAvatarPersonnel, utilisateurConnecte, 36);

        // Le dashboard n'est visible que pour Administrateur et Enseignant (pas Étudiant)
        if (utilisateurConnecte.getRole() == User.Role.ETUDIANT) {
            ongletsPrincipaux.getTabs().remove(ongletDashboard);
        } else {
            clientReseau.envoyer(new RequeteReseau(RequeteReseau.Type.STATISTIQUES_DEMANDE, null));
        }
    }

    @FXML
    public void initialize() {
        listeSalons.setItems(donneesSalons);
        listeUtilisateurs.setItems(donneesUtilisateurs);
        listeMessages.setItems(donneesMessages);
        listeAnnonces.setItems(donneesAnnonces);
        listeFichiers.setItems(donneesFichiers);
        listeGestionUtilisateurs.setItems(donneesGestionUtilisateurs);
        comboNouveauRole.getItems().setAll(User.Role.values());
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


        listeUtilisateurs.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean vide) {
                super.updateItem(user, vide);

                if (vide || user == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                StackPane avatar = new StackPane();
                afficherAvatar(avatar, user, 24);

                Label nom = new Label(user.getUsername());

                HBox ligne = new HBox(8, avatar, nom);
                ligne.setAlignment(Pos.CENTER_LEFT);

                setGraphic(ligne);
                setText(null);
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
                boolean peutSupprimer = estMoi || utilisateurConnecte.getRole() == User.Role.ADMINISTRATEUR;

                Label texteAuteur = new Label(message.getExpediteurUsername());
                texteAuteur.getStyleClass().add("auteur-message");

                Label texteMessage = new Label(message.getContenu());
                texteMessage.setWrapText(true);
                texteMessage.getStyleClass().add(estMoi ? "texte-message-moi" : "texte-message-autre");

                VBox contenuBulle = new VBox(2, texteAuteur, texteMessage);
                HBox bulleEtBouton = new HBox(6, contenuBulle);
                bulleEtBouton.setAlignment(Pos.CENTER_LEFT);

                if (peutSupprimer) {
                    Button boutonSupprimerMsg = new Button("✕");
                    boutonSupprimerMsg.getStyleClass().add("bouton-supprimer-mini");
                    boutonSupprimerMsg.setOnAction(e ->
                            clientReseau.envoyer(new RequeteReseau(RequeteReseau.Type.MESSAGE_SUPPRIMER, message.getId())));
                    bulleEtBouton.getChildren().add(boutonSupprimerMsg);
                }

                VBox bulle = new VBox(bulleEtBouton);
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

        boutonExporterSalon.setVisible(true);
        boutonExporterSalon.setManaged(true);

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

            case UTILISATEURS_TOUS_REPONSE -> {
                @SuppressWarnings("unchecked")
                List<User> tousLesUtilisateurs = (List<User>) requete.getPayload();
                Platform.runLater(() -> donneesGestionUtilisateurs.setAll(tousLesUtilisateurs));
            }

            case UTILISATEUR_OPERATION_REPONSE -> {
                String message = (String) requete.getPayload();
                Platform.runLater(() -> {
                    afficherMessageUtilisateur(message);
                    champNouveauUsername.clear();
                    champNouveauMotDePasse.clear();
                    champNouveauNomComplet.clear();
                    champNouveauEmail.clear();
                    comboNouveauRole.setValue(null);
                    annulerEdition();
                });
            }

            case FICHIER_SUPPRIME -> {
                int idSupprime = (Integer) requete.getPayload();
                Platform.runLater(() -> donneesFichiers.removeIf(f -> f.getId() == idSupprime));
            }

            case MESSAGE_SUPPRIME -> {
                int idSupprime = (Integer) requete.getPayload();
                Platform.runLater(() -> donneesMessages.removeIf(m -> m.getId() == idSupprime));
            }

            case PHOTO_PROFIL_MISE_A_JOUR -> {
                User userMisAJour = (User) requete.getPayload();
                cacheImagesMemoire.remove(userMisAJour.getId()); // invalide le cache pour forcer le rechargement

                Platform.runLater(() -> {
                    // Met à jour l'objet User correspondant dans la liste des utilisateurs connectés
                    // (sinon la liste garde l'ancienne version sans photo, même après refresh())
                    for (int i = 0; i < donneesUtilisateurs.size(); i++) {
                        if (donneesUtilisateurs.get(i).getId() == userMisAJour.getId()) {
                            donneesUtilisateurs.set(i, userMisAJour);
                            break;
                        }
                    }

                    if (userMisAJour.getId() == utilisateurConnecte.getId()) {
                        afficherAvatar(conteneurAvatarPersonnel, userMisAJour, 36);
                    }

                    listeUtilisateurs.refresh();
                });
            }

            case STATISTIQUES_REPONSE -> {
                StatistiquesDTO stats = (StatistiquesDTO) requete.getPayload();
                Platform.runLater(() -> afficherDashboard(stats));
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

                boolean peutSupprimer = fichier.getExpediteurId() == utilisateurConnecte.getId()
                        || utilisateurConnecte.getRole() == User.Role.ADMINISTRATEUR;

                if (peutSupprimer) {
                    Button boutonSupprimerFichier = new Button("Supprimer");
                    boutonSupprimerFichier.getStyleClass().add("bouton-supprimer");
                    boutonSupprimerFichier.setOnAction(e ->
                            clientReseau.envoyer(new RequeteReseau(RequeteReseau.Type.FICHIER_SUPPRIMER, fichier.getId())));
                    ligne.getChildren().add(boutonSupprimerFichier);
                }

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

    private void configurerListeGestionUtilisateurs() {
        listeGestionUtilisateurs.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean vide) {
                super.updateItem(user, vide);

                if (vide || user == null) {
                    setGraphic(null);
                    return;
                }

                Label nom = new Label(user.getNomComplet() + " (" + user.getUsername() + ")");
                nom.getStyleClass().add("nom-utilisateur");

                Label role = new Label(user.getRole().toString() + " • " + user.getEmail());
                role.getStyleClass().add("role-utilisateur");

                VBox infosTexte = new VBox(4, nom, role);

                Button boutonModifier = new Button("Modifier");
                boutonModifier.getStyleClass().add("bouton-secondaire-clair");
                boutonModifier.setOnAction(e -> ouvrirModificationUtilisateur(user));

                Button boutonSupprimer = new Button("Supprimer");
                boutonSupprimer.getStyleClass().add("bouton-supprimer");
                boutonSupprimer.setOnAction(e -> supprimerUtilisateur(user));

                HBox boutonsActions = new HBox(8, boutonModifier, boutonSupprimer);
                HBox ligne = new HBox(16, infosTexte, boutonsActions);

                ligne.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(infosTexte, javafx.scene.layout.Priority.ALWAYS);

                VBox carte = new VBox(ligne);
                carte.getStyleClass().add("carte-utilisateur");
                VBox.setMargin(carte, new javafx.geometry.Insets(6, 12, 6, 12));

                setGraphic(carte);
            }
        });
    }

    @FXML
    private void creerUtilisateur() {
        String username = champNouveauUsername.getText().trim();
        String nomComplet = champNouveauNomComplet.getText().trim();
        String email = champNouveauEmail.getText().trim();
        User.Role role = comboNouveauRole.getValue();

        if (username.isEmpty() || nomComplet.isEmpty() || role == null) {
            afficherMessageUtilisateur("Veuillez remplir tous les champs obligatoires.");
            return;
        }

        if (utilisateurEnCoursDedition != null) {
            // Mode modification
            utilisateurEnCoursDedition.setNomComplet(nomComplet);
            utilisateurEnCoursDedition.setEmail(email);
            utilisateurEnCoursDedition.setRole(role);

            clientReseau.envoyer(new RequeteReseau(RequeteReseau.Type.UTILISATEUR_MODIFIER, utilisateurEnCoursDedition));
            annulerEdition();

        } else {
            // Mode création
            String motDePasse = champNouveauMotDePasse.getText();
            if (motDePasse.isEmpty()) {
                afficherMessageUtilisateur("Le mot de passe est obligatoire pour créer un compte.");
                return;
            }

            User nouvelUtilisateur = new User(username, motDePasse, role, nomComplet, email);
            clientReseau.envoyer(new RequeteReseau(RequeteReseau.Type.UTILISATEUR_CREER, nouvelUtilisateur));
        }
    }

    private void supprimerUtilisateur(User user) {
        clientReseau.envoyer(new RequeteReseau(RequeteReseau.Type.UTILISATEUR_SUPPRIMER, user.getId()));
    }

    private void afficherMessageUtilisateur(String message) {
        labelMessageUtilisateur.setText(message);
        labelMessageUtilisateur.setVisible(true);
    }

    private User utilisateurEnCoursDedition = null; // null = mode création, sinon mode modification

    private void ouvrirModificationUtilisateur(User user) {
        utilisateurEnCoursDedition = user;

        champNouveauUsername.setText(user.getUsername());
        champNouveauUsername.setDisable(true); // le username ne se change pas ici, plus simple et plus sûr
        champNouveauMotDePasse.setPromptText("(laisser vide pour ne pas changer)");
        champNouveauNomComplet.setText(user.getNomComplet());
        champNouveauEmail.setText(user.getEmail());
        comboNouveauRole.setValue(user.getRole());

        boutonCreerUtilisateur.setText("Enregistrer les modifications");
    }

    private void annulerEdition() {
        utilisateurEnCoursDedition = null;
        champNouveauUsername.setDisable(false);
        champNouveauMotDePasse.setPromptText("Mot de passe");
        boutonCreerUtilisateur.setText("Créer l'utilisateur");
    }

    /**
     * Affiche l'avatar d'un utilisateur dans le conteneur donné : sa photo si elle est
     * disponible en cache, sinon un avatar généré à partir de son initiale, avec
     * téléchargement en arrière-plan si une photo existe côté serveur mais pas encore en cache.
     */
    private void afficherAvatar(StackPane conteneur, User user, double taille) {
        conteneur.getChildren().clear();

        if (cacheImagesMemoire.containsKey(user.getId())) {
            conteneur.getChildren().add(AvatarUtil.creerAvatarImage(cacheImagesMemoire.get(user.getId()), taille));
            return;
        }

        // Par défaut : avatar avec initiale, en attendant un éventuel téléchargement
        conteneur.getChildren().add(AvatarUtil.creerAvatarInitiale(user.getNomComplet(), taille));

        if (user.getPhotoProfil() != null) {
            new Thread(() -> {
                try {
                    Path chemin = clientTransfertFichier.telechargerPhotoProfil(user.getId(), dossierCachePhotos);
                    if (chemin != null) {
                        Image image = new Image(chemin.toUri().toString());
                        cacheImagesMemoire.put(user.getId(), image);
                        Platform.runLater(() -> {
                            conteneur.getChildren().clear();
                            conteneur.getChildren().add(AvatarUtil.creerAvatarImage(image, taille));
                        });
                    }
                } catch (IOException e) {
                    System.err.println("Erreur téléchargement photo profil : " + e.getMessage());
                }
            }).start();
        }
    }

    /**
     * Ouvre un sélecteur d'image pour changer sa propre photo de profil.
     */
    @FXML
    private void changerPhotoProfil() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        java.io.File fichierChoisi = fileChooser.showOpenDialog(champMessage.getScene().getWindow());
        if (fichierChoisi == null) return;

        new Thread(() -> {
            try {
                clientTransfertFichier.envoyerPhotoProfil(fichierChoisi.toPath(), utilisateurConnecte.getId());
                cacheImagesMemoire.remove(utilisateurConnecte.getId()); // force le rechargement
            } catch (IOException e) {
                System.err.println("Erreur envoi photo profil : " + e.getMessage());
            }
        }).start();
    }

    /**
     * Construit et affiche les 3 graphiques du dashboard à partir des statistiques reçues.
     */
    private void afficherDashboard(StatistiquesDTO stats) {
        this.dernieresStatistiques = stats;

        conteneurDashboard.getChildren().clear();

        // ===== Graphique 1 : utilisateurs par rôle (camembert) =====
        Label titre1 = new Label("Répartition des utilisateurs par rôle");
        titre1.getStyleClass().add("titre-graphique");

        PieChart camembert = new PieChart();
        stats.getUtilisateursParRole().forEach((role, total) ->
                camembert.getData().add(new PieChart.Data(role + " (" + total + ")", total))
        );
        camembert.setLegendVisible(true);
        camembert.setPrefHeight(300);

        VBox carte1 = new VBox(12, titre1, camembert);
        carte1.getStyleClass().add("carte-graphique");

        // ===== Graphique 2 : messages par salon (barres) =====
        Label titre2 = new Label("Messages envoyés par salon");
        titre2.getStyleClass().add("titre-graphique");

        CategoryAxis axeXMessages = new CategoryAxis();
        NumberAxis axeYMessages = new NumberAxis();
        BarChart<String, Number> graphiqueMessages = new BarChart<>(axeXMessages, axeYMessages);
        graphiqueMessages.setLegendVisible(false);
        graphiqueMessages.setPrefHeight(300);

        XYChart.Series<String, Number> serieMessages = new XYChart.Series<>();
        stats.getMessagesParSalon().forEach((salon, total) ->
                serieMessages.getData().add(new XYChart.Data<>(salon, total))
        );
        graphiqueMessages.getData().add(serieMessages);

        VBox carte2 = new VBox(12, titre2, graphiqueMessages);
        carte2.getStyleClass().add("carte-graphique");

        // ===== Graphique 3 : fichiers par salon (barres) =====
        Label titre3 = new Label("Fichiers partagés par salon");
        titre3.getStyleClass().add("titre-graphique");

        CategoryAxis axeXFichiers = new CategoryAxis();
        NumberAxis axeYFichiers = new NumberAxis();
        BarChart<String, Number> graphiqueFichiers = new BarChart<>(axeXFichiers, axeYFichiers);
        graphiqueFichiers.setLegendVisible(false);
        graphiqueFichiers.setPrefHeight(300);

        XYChart.Series<String, Number> serieFichiers = new XYChart.Series<>();
        stats.getFichiersParSalon().forEach((salon, total) ->
                serieFichiers.getData().add(new XYChart.Data<>(salon, total))
        );
        graphiqueFichiers.getData().add(serieFichiers);

        VBox carte3 = new VBox(12, titre3, graphiqueFichiers);
        carte3.getStyleClass().add("carte-graphique");

        Button boutonExportStats = new Button("📄 Exporter les statistiques en PDF");
        boutonExportStats.getStyleClass().add("bouton-principal");
        boutonExportStats.setOnAction(e -> exporterStatistiquesPdf());

        conteneurDashboard.getChildren().addAll(boutonExportStats, carte1, carte2, carte3);
    }

    @FXML
    private void exporterSalonPdf() {
        if (salonActif == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter l'historique en PDF");
        fileChooser.setInitialFileName("historique_" + salonActif.getNom().replace(" ", "_") + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));

        java.io.File destination = fileChooser.showSaveDialog(champMessage.getScene().getWindow());
        if (destination == null) return;

        try {
            pdfExportService.exporterHistoriqueSalon(salonActif.getNom(), donneesMessages, destination);
            NotificationToast.afficher(champMessage.getScene().getWindow(), "Export PDF réussi !");
        } catch (java.io.IOException e) {
            System.err.println("Erreur export PDF : " + e.getMessage());
        }
    }

    @FXML
    private void exporterAnnoncesPdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter les annonces en PDF");
        fileChooser.setInitialFileName("annonces_uamconnect.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));

        java.io.File destination = fileChooser.showSaveDialog(champMessage.getScene().getWindow());
        if (destination == null) return;

        try {
            pdfExportService.exporterAnnonces(donneesAnnonces, destination);
            NotificationToast.afficher(champMessage.getScene().getWindow(), "Export PDF réussi !");
        } catch (java.io.IOException e) {
            System.err.println("Erreur export PDF : " + e.getMessage());
        }
    }

    private void exporterStatistiquesPdf() {
        if (dernieresStatistiques == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter les statistiques en PDF");
        fileChooser.setInitialFileName("statistiques_uamconnect.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));

        java.io.File destination = fileChooser.showSaveDialog(champMessage.getScene().getWindow());
        if (destination == null) return;

        try {
            List<javafx.scene.Node> graphiques = new java.util.ArrayList<>(conteneurDashboard.getChildren());
            pdfExportService.exporterStatistiques(dernieresStatistiques, graphiques, destination);
            NotificationToast.afficher(champMessage.getScene().getWindow(), "Export PDF réussi !");
        } catch (java.io.IOException e) {
            System.err.println("Erreur export PDF : " + e.getMessage());
        }
    }
}