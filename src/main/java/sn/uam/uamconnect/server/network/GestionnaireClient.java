package sn.uam.uamconnect.server.network;

import sn.uam.uamconnect.common.*;
import sn.uam.uamconnect.server.dao.MessageDAO;
import sn.uam.uamconnect.server.service.AuthService;
import sn.uam.uamconnect.server.dao.SalonDAO;
import sn.uam.uamconnect.server.dao.AnnonceDAO;
import sn.uam.uamconnect.common.Annonce;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.Socket;
import java.util.Optional;

/**
 * Gère la communication avec UN client connecté, dans son propre Thread.
 * Permet au serveur de gérer plusieurs clients simultanément (exigence section 4.3).
 */
public class GestionnaireClient extends Thread {

    private final Socket socket;
    private final ServeurUAM serveur;

    private ObjectOutputStream sortie;
    private ObjectInputStream entree;

    private User utilisateurConnecte; // null tant que le client ne s'est pas authentifié

    private final AuthService authService;
    private final MessageDAO messageDAO;
    private final SalonDAO salonDAO;
    private final AnnonceDAO annonceDAO;

    public GestionnaireClient(Socket socket, ServeurUAM serveur) {
        this.socket = socket;
        this.serveur = serveur;
        this.authService = new AuthService();
        this.messageDAO = new MessageDAO();
        this.salonDAO = new SalonDAO();
        this.annonceDAO = new AnnonceDAO();
    }

    @Override
    public void run() {
        try {
            // IMPORTANT : le flux de sortie doit être créé AVANT le flux d'entrée
            // (sinon risque de blocage mutuel entre client et serveur au démarrage)
            sortie = new ObjectOutputStream(socket.getOutputStream());
            entree = new ObjectInputStream(socket.getInputStream());

            // Boucle d'écoute : tant que le client est connecté, on traite ses requêtes
            while (true) {
                Object objetRecu = entree.readObject();

                if (objetRecu instanceof RequeteReseau requete) {
                    traiterRequete(requete);
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[SERVEUR] Client déconnecté : " +
                    (utilisateurConnecte != null ? utilisateurConnecte.getUsername() : "inconnu"));
        } finally {
            deconnecterProprement();
        }
    }

    /**
     * Aiguille chaque requête reçue vers le traitement approprié selon son type.
     */
    private void traiterRequete(RequeteReseau requete) throws IOException {
        switch (requete.getType()) {

            case CONNEXION_DEMANDE -> traiterConnexion(requete);
            case DECONNEXION -> deconnecterProprement();
            case MESSAGE_ENVOI -> traiterEnvoiMessage(requete);
            case HISTORIQUE_SALON_DEMANDE -> traiterDemandeHistorique(requete);
            case LISTE_SALONS_DEMANDE -> envoyer(new RequeteReseau(RequeteReseau.Type.LISTE_SALONS,
                    salonDAO.trouverTous()));
            case LISTE_UTILISATEURS_DEMANDE -> serveur.envoyerListeUtilisateursA(this);
            case ANNONCE_PUBLICATION -> traiterPublicationAnnonce(requete);
            case LISTE_ANNONCES_DEMANDE -> envoyer(new RequeteReseau(RequeteReseau.Type.LISTE_ANNONCES_REPONSE,
                    annonceDAO.trouverToutes()));
            case FICHIER_LISTE_DEMANDE -> {
                Integer salonId = (Integer) requete.getPayload();
                envoyer(new RequeteReseau(RequeteReseau.Type.FICHIER_LISTE_REPONSE,
                        new sn.uam.uamconnect.server.dao.FichierDAO().trouverParSalon(salonId)));
            }

            case HISTORIQUE_PRIVE_DEMANDE -> traiterDemandeHistoriquePrive(requete);
            case UTILISATEURS_TOUS_DEMANDE -> traiterListeUtilisateursTous();
            case UTILISATEUR_CREER -> traiterCreationUtilisateur(requete);
            case UTILISATEUR_MODIFIER -> traiterModificationUtilisateur(requete);
            case UTILISATEUR_SUPPRIMER -> traiterSuppressionUtilisateur(requete);
            case FICHIER_SUPPRIMER -> traiterSuppressionFichier(requete);
            case MESSAGE_SUPPRIMER -> traiterSuppressionMessage(requete);

            default -> System.out.println("[SERVEUR] Type de requête non géré : " + requete.getType());
        }
    }

    /**
     * Traite une demande de connexion (login/mot de passe).
     */
    private void traiterConnexion(RequeteReseau requete) throws IOException {
        String[] identifiants = (String[]) requete.getPayload();
        String username = identifiants[0];
        String motDePasse = identifiants[1];

        Optional<User> resultat = authService.seConnecter(username, motDePasse);

        if (resultat.isPresent()) {
            this.utilisateurConnecte = resultat.get();

            // Répond au client : connexion réussie, avec les infos de l'utilisateur
            envoyer(new RequeteReseau(RequeteReseau.Type.CONNEXION_REPONSE_OK, utilisateurConnecte));

            // Enregistre ce client comme actif auprès du serveur
            serveur.ajouterClient(this);

            // Notifie tous les autres clients connectés
            serveur.diffuserNotification(utilisateurConnecte.getUsername() + " vient de se connecter.");

            // Envoie la liste à jour des utilisateurs connectés à tout le monde
            serveur.diffuserListeUtilisateurs();

            System.out.println("[SERVEUR] Connexion réussie : " + username);

        } else {
            envoyer(new RequeteReseau(RequeteReseau.Type.CONNEXION_REPONSE_ECHEC,
                    "Identifiant ou mot de passe incorrect."));
            System.out.println("[SERVEUR] Échec de connexion : " + username);
        }
    }

    /**
     * Traite l'envoi d'un message (privé ou de salon) : sauvegarde en BDD puis diffusion.
     */
    private void traiterEnvoiMessage(RequeteReseau requete) {
        Message message = (Message) requete.getPayload();

        // Sécurité : on force l'expéditeur réel (jamais celui envoyé par le client, pour éviter l'usurpation)
        message.setExpediteurId(utilisateurConnecte.getId());
        message.setExpediteurUsername(utilisateurConnecte.getUsername());

        // Sauvegarde en base (historique des conversations - section 4.8)
        int idGenere = messageDAO.creer(message);
        message.setId(idGenere);

        // Diffusion en temps réel
        if (message.estMessageSalon()) {
            serveur.diffuserMessageSalon(message);
        } else if (message.estMessagePrive()) {
            serveur.envoyerMessagePrive(message);
        }
    }

    /**
     * Envoie un objet RequeteReseau à ce client précis via son flux de sortie.
     * Synchronized : évite que plusieurs threads écrivent en même temps sur le même flux.
     */
    public synchronized void envoyer(RequeteReseau requete) {
        try {
            sortie.writeObject(requete);
            sortie.flush();
            sortie.reset(); // évite les problèmes de cache d'objets déjà envoyés (important avec ObjectOutputStream)
        } catch (IOException e) {
            System.err.println("[SERVEUR] Erreur d'envoi vers " +
                    (utilisateurConnecte != null ? utilisateurConnecte.getUsername() : "client inconnu"));
        }
    }

    /**
     * Ferme proprement la connexion et notifie les autres clients du départ.
     */
    private void deconnecterProprement() {
        try {
            if (utilisateurConnecte != null) {
                authService.seDeconnecter(utilisateurConnecte.getId());
                serveur.retirerClient(this);
                serveur.diffuserNotification(utilisateurConnecte.getUsername() + " s'est déconnecté.");
                serveur.diffuserListeUtilisateurs();
            }

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("[SERVEUR] Erreur lors de la déconnexion : " + e.getMessage());
        }
    }

    public User getUtilisateurConnecte() {
        return utilisateurConnecte;
    }

    /**
     * Envoie l'historique des messages d'un salon au client qui le demande
     * (section 4.8 : historique des conversations).
     */
    private void traiterDemandeHistorique(RequeteReseau requete) {
        Integer salonId = (Integer) requete.getPayload();
        var messages = messageDAO.trouverParSalon(salonId, 100); // 100 derniers messages
        envoyer(new RequeteReseau(RequeteReseau.Type.HISTORIQUE_SALON_REPONSE, messages));
    }

    /**
     * Traite la publication d'une annonce. Vérifie le rôle (sécurité serveur, jamais côté client seul)
     * puis diffuse la nouvelle annonce à tous les clients connectés.
     */
    private void traiterPublicationAnnonce(RequeteReseau requete) {
        // Sécurité : seuls Enseignant et Administrateur peuvent publier (exigence section 4.6)
        if (utilisateurConnecte.getRole() == User.Role.ETUDIANT) {
            System.out.println("[SERVEUR] Tentative de publication refusée (étudiant) : " + utilisateurConnecte.getUsername());
            return;
        }

        Annonce annonce = (Annonce) requete.getPayload();
        annonce.setAuteurId(utilisateurConnecte.getId());

        int idGenere = annonceDAO.creer(annonce);
        annonce.setId(idGenere);
        annonce.setAuteurUsername(utilisateurConnecte.getUsername());

        // Diffusion à tous les clients connectés
        RequeteReseau diffusion = new RequeteReseau(RequeteReseau.Type.ANNONCE_NOUVELLE, annonce);
        for (GestionnaireClient client : serveur.getClientsConnectes()) {
            client.envoyer(diffusion);
        }

        serveur.diffuserNotification("Nouvelle annonce disponible : " + annonce.getTitre());
    }

    /**
     * Envoie l'historique de la conversation privée entre l'utilisateur connecté
     * et un autre utilisateur donné.
     */
    private void traiterDemandeHistoriquePrive(RequeteReseau requete) {
        int autreUtilisateurId = (Integer) requete.getPayload();
        var messages = messageDAO.trouverConversationPrivee(
                utilisateurConnecte.getId(), autreUtilisateurId, 100
        );
        envoyer(new RequeteReseau(RequeteReseau.Type.HISTORIQUE_PRIVE_REPONSE, messages));
    }

    /**
     * Vérifie que l'utilisateur connecté est bien Administrateur, sinon refuse.
     * Sécurité appliquée côté serveur, jamais seulement côté client.
     */
    private boolean estAdministrateur() {
        return utilisateurConnecte != null && utilisateurConnecte.getRole() == User.Role.ADMINISTRATEUR;
    }

    private void traiterListeUtilisateursTous() {
        if (!estAdministrateur()) return;
        envoyer(new RequeteReseau(RequeteReseau.Type.UTILISATEURS_TOUS_REPONSE, userDaoPourAdmin().trouverTous()));
    }

    private void traiterCreationUtilisateur(RequeteReseau requete) {
        if (!estAdministrateur()) {
            envoyer(new RequeteReseau(RequeteReseau.Type.UTILISATEUR_OPERATION_REPONSE, "Accès refusé."));
            return;
        }

        User nouvelUtilisateur = (User) requete.getPayload();
        var authService = new sn.uam.uamconnect.server.service.AuthService();

        int id = authService.inscrire(
                nouvelUtilisateur.getUsername(),
                nouvelUtilisateur.getPassword(), // mot de passe en clair, sera haché par AuthService
                nouvelUtilisateur.getRole(),
                nouvelUtilisateur.getNomComplet(),
                nouvelUtilisateur.getEmail()
        );

        String message = id != -1 ? "Utilisateur créé avec succès." : "Échec : nom d'utilisateur déjà pris.";
        envoyer(new RequeteReseau(RequeteReseau.Type.UTILISATEUR_OPERATION_REPONSE, message));

        if (id != -1) {
            envoyer(new RequeteReseau(RequeteReseau.Type.UTILISATEURS_TOUS_REPONSE, userDaoPourAdmin().trouverTous()));
        }
    }

    private void traiterModificationUtilisateur(RequeteReseau requete) {
        if (!estAdministrateur()) {
            envoyer(new RequeteReseau(RequeteReseau.Type.UTILISATEUR_OPERATION_REPONSE, "Accès refusé."));
            return;
        }

        User utilisateurModifie = (User) requete.getPayload();
        boolean succes = userDaoPourAdmin().modifier(utilisateurModifie);

        envoyer(new RequeteReseau(RequeteReseau.Type.UTILISATEUR_OPERATION_REPONSE,
                succes ? "Utilisateur modifié avec succès." : "Échec de la modification."));

        if (succes) {
            envoyer(new RequeteReseau(RequeteReseau.Type.UTILISATEURS_TOUS_REPONSE, userDaoPourAdmin().trouverTous()));
        }
    }

    private void traiterSuppressionUtilisateur(RequeteReseau requete) {
        if (!estAdministrateur()) {
            envoyer(new RequeteReseau(RequeteReseau.Type.UTILISATEUR_OPERATION_REPONSE, "Accès refusé."));
            return;
        }

        int idASupprimer = (Integer) requete.getPayload();

        if (idASupprimer == utilisateurConnecte.getId()) {
            envoyer(new RequeteReseau(RequeteReseau.Type.UTILISATEUR_OPERATION_REPONSE,
                    "Impossible de supprimer votre propre compte."));
            return;
        }

        boolean succes = userDaoPourAdmin().supprimer(idASupprimer);

        envoyer(new RequeteReseau(RequeteReseau.Type.UTILISATEUR_OPERATION_REPONSE,
                succes ? "Utilisateur supprimé avec succès." : "Échec de la suppression."));

        if (succes) {
            envoyer(new RequeteReseau(RequeteReseau.Type.UTILISATEURS_TOUS_REPONSE, userDaoPourAdmin().trouverTous()));
        }
    }

    /**
     * Petit raccourci pour accéder à un UserDAO (réutilise si possible, sinon en crée un).
     */
    private sn.uam.uamconnect.server.dao.UserDAO userDaoPourAdmin() {
        return new sn.uam.uamconnect.server.dao.UserDAO();
    }

    /**
     * Supprime un fichier : vérifie que l'utilisateur est l'expéditeur ou un Administrateur,
     * supprime la ligne en BDD ET le fichier physique sur le disque, puis diffuse la suppression.
     */
    private void traiterSuppressionFichier(RequeteReseau requete) {
        int fichierId = (Integer) requete.getPayload();

        var resultat = new sn.uam.uamconnect.server.dao.FichierDAO().trouverParId(fichierId);
        if (resultat.isEmpty()) return;

        var fichier = resultat.get();
        boolean autorise = fichier.getExpediteurId() == utilisateurConnecte.getId()
                || utilisateurConnecte.getRole() == User.Role.ADMINISTRATEUR;

        if (!autorise) {
            System.out.println("[SERVEUR] Suppression de fichier refusée pour " + utilisateurConnecte.getUsername());
            return;
        }

        boolean succesBdd = new sn.uam.uamconnect.server.dao.FichierDAO().supprimer(fichierId);

        if (succesBdd) {
            try {
                Files.deleteIfExists(Path.of(fichier.getChemin()));
            } catch (java.io.IOException e) {
                System.err.println("[SERVEUR] Impossible de supprimer le fichier physique : " + e.getMessage());
            }

            serveur.diffuserATous(new RequeteReseau(RequeteReseau.Type.FICHIER_SUPPRIME, fichierId));
        }
    }

    /**
     * Supprime un message : vérifie que l'utilisateur est l'auteur ou un Administrateur,
     * supprime en BDD puis diffuse la suppression à tous.
     */
    private void traiterSuppressionMessage(RequeteReseau requete) {
        int messageId = (Integer) requete.getPayload();

        var resultat = messageDAO.trouverParId(messageId);
        if (resultat.isEmpty()) return;

        var message = resultat.get();
        boolean autorise = message.getExpediteurId() == utilisateurConnecte.getId()
                || utilisateurConnecte.getRole() == User.Role.ADMINISTRATEUR;

        if (!autorise) {
            System.out.println("[SERVEUR] Suppression de message refusée pour " + utilisateurConnecte.getUsername());
            return;
        }

        boolean succes = messageDAO.supprimer(messageId);

        if (succes) {
            serveur.diffuserATous(new RequeteReseau(RequeteReseau.Type.MESSAGE_SUPPRIME, messageId));
        }
    }
}