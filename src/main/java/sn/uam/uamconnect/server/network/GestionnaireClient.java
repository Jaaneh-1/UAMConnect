package sn.uam.uamconnect.server.network;

import sn.uam.uamconnect.common.*;
import sn.uam.uamconnect.server.dao.MessageDAO;
import sn.uam.uamconnect.server.service.AuthService;
import sn.uam.uamconnect.server.dao.SalonDAO;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

    public GestionnaireClient(Socket socket, ServeurUAM serveur) {
        this.socket = socket;
        this.serveur = serveur;
        this.authService = new AuthService();
        this.messageDAO = new MessageDAO();
        this.salonDAO = new SalonDAO();
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
}