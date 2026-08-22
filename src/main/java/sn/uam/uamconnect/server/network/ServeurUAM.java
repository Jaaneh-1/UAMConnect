package sn.uam.uamconnect.server.network;

import sn.uam.uamconnect.common.Message;
import sn.uam.uamconnect.common.RequeteReseau;
import sn.uam.uamconnect.common.User;
import sn.uam.uamconnect.server.dao.UserDAO;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Serveur principal : écoute les connexions entrantes et gère la liste
 * des clients actifs (exigence section 4.3 du sujet).
 */
public class ServeurUAM {

    private static final int PORT = 5000;

    private ServerSocket serverSocket;
    private final UserDAO userDAO;

    // CopyOnWriteArrayList : liste thread-safe, adaptée quand il y a beaucoup de lectures
    // (parcours pour diffuser) et peu d'écritures (ajout/retrait de clients) - contexte multi-thread
    private final List<GestionnaireClient> clientsConnectes = new CopyOnWriteArrayList<>();

    public ServeurUAM() {
        this.userDAO = new UserDAO();
    }

    /**
     * Démarre le serveur : ouvre le port et accepte les connexions en boucle infinie.
     */
    public void demarrer() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("========================================");
            System.out.println(" Serveur UAM Connect démarré sur le port " + PORT);
            System.out.println("========================================");

            while (true) {
                // accept() est bloquant : le serveur attend ici qu'un client se connecte
                Socket socketClient = serverSocket.accept();
                System.out.println("[SERVEUR] Nouvelle connexion depuis : " + socketClient.getInetAddress());

                // Chaque client reçoit son propre thread pour être géré indépendamment
                GestionnaireClient gestionnaire = new GestionnaireClient(socketClient, this);
                gestionnaire.start();
            }

        } catch (IOException e) {
            System.err.println("[SERVEUR] Erreur critique : " + e.getMessage());
        }
    }

    // ================================================
    // Gestion de la liste des clients connectés
    // ================================================

    public void ajouterClient(GestionnaireClient client) {
        clientsConnectes.add(client);
    }

    public void retirerClient(GestionnaireClient client) {
        clientsConnectes.remove(client);
    }

    // ================================================
    // Diffusion (broadcast) vers les clients
    // ================================================

    /**
     * Envoie une notification texte à TOUS les clients connectés.
     * Exemples exigés section 4.9 : "Aminata vient de se connecter.", "Moussa a envoyé un fichier."
     */
    public void diffuserNotification(String texte) {
        RequeteReseau notif = new RequeteReseau(RequeteReseau.Type.NOTIFICATION, texte);
        for (GestionnaireClient client : clientsConnectes) {
            client.envoyer(notif);
        }
    }

    /**
     * Envoie à tous les clients la liste à jour des utilisateurs actuellement connectés
     * (exigence section 4.4 : "voir la liste des utilisateurs connectés").
     */
    public void diffuserListeUtilisateurs() {
        List<User> utilisateursConnectes = clientsConnectes.stream()
                .map(GestionnaireClient::getUtilisateurConnecte)
                .filter(u -> u != null)
                .collect(Collectors.toList());

        RequeteReseau requete = new RequeteReseau(RequeteReseau.Type.LISTE_UTILISATEURS, utilisateursConnectes);

        for (GestionnaireClient client : clientsConnectes) {
            client.envoyer(requete);
        }
    }

    /**
     * Envoie la liste des utilisateurs connectés à UN client précis (celui qui la demande),
     * plutôt qu'à tout le monde (évite d'attendre le prochain événement connexion/déconnexion).
     */
    public void envoyerListeUtilisateursA(GestionnaireClient demandeur) {
        List<User> utilisateursConnectes = clientsConnectes.stream()
                .map(GestionnaireClient::getUtilisateurConnecte)
                .filter(u -> u != null)
                .collect(Collectors.toList());

        demandeur.envoyer(new RequeteReseau(RequeteReseau.Type.LISTE_UTILISATEURS, utilisateursConnectes));
    }

    /**
     * Diffuse un message de salon à tous les clients connectés (chacun filtrera
     * côté client s'il est dans ce salon, ou on peut filtrer ici selon les salons "actifs" par client).
     * Pour rester simple et fiable : on diffuse à tous, le client affiche selon le salon actuellement ouvert.
     */
    public void diffuserMessageSalon(Message message) {
        RequeteReseau requete = new RequeteReseau(RequeteReseau.Type.MESSAGE_RECEPTION, message);
        for (GestionnaireClient client : clientsConnectes) {
            client.envoyer(requete);
        }
    }

    /**
     * Envoie un message privé uniquement à son destinataire (et en copie à l'expéditeur,
     * pour qu'il voie son propre message apparaître dans sa fenêtre de chat).
     */
    public void envoyerMessagePrive(Message message) {
        RequeteReseau requete = new RequeteReseau(RequeteReseau.Type.MESSAGE_RECEPTION, message);

        for (GestionnaireClient client : clientsConnectes) {
            User u = client.getUtilisateurConnecte();
            if (u != null && (u.getId() == message.getDestinataireId() || u.getId() == message.getExpediteurId())) {
                client.envoyer(requete);
            }
        }
    }

    public static void main(String[] args) {
        ServeurUAM serveur = new ServeurUAM();

        // Le canal de fichiers tourne dans un thread séparé, en parallèle du chat
        ServeurFichiers serveurFichiers = new ServeurFichiers(serveur);
        new Thread(serveurFichiers::demarrer).start();

        // Le canal principal (chat) démarre sur le thread principal
        serveur.demarrer();
    }

    public List<GestionnaireClient> getClientsConnectes() {
        return clientsConnectes;
    }

    /**
     * Diffuse n'importe quelle requête à tous les clients connectés (méthode générique).
     */
    public void diffuserATous(RequeteReseau requete) {
        for (GestionnaireClient client : clientsConnectes) {
            client.envoyer(requete);
        }
    }
}