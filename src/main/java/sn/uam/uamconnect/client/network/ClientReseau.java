package sn.uam.uamconnect.client.network;

import sn.uam.uamconnect.common.RequeteReseau;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Gère la connexion Socket du client vers le serveur.
 * Écoute en continu les messages entrants dans un thread dédié,
 * pour ne jamais bloquer l'interface JavaFX (thread UI séparé).
 */
public class ClientReseau {

    private static final String HOTE = ConfigClient.getHoteServeur();
    private static final int PORT = 5000;

    private Socket socket;
    private ObjectOutputStream sortie;
    private ObjectInputStream entree;

    private Thread threadEcoute;
    private Consumer<RequeteReseau> ecouteurMessages; // callback appelé à chaque message reçu

    private boolean connecte = false;

    /**
     * Établit la connexion au serveur.
     */
    public void connecter() throws IOException {
        socket = new Socket(HOTE, PORT);

        // Même ordre que côté serveur : sortie avant entrée (évite un blocage)
        sortie = new ObjectOutputStream(socket.getOutputStream());
        entree = new ObjectInputStream(socket.getInputStream());

        connecte = true;

        demarrerEcoute();
    }

    /**
     * Lance un thread dédié qui écoute en permanence les messages du serveur.
     */
    private void demarrerEcoute() {
        threadEcoute = new Thread(() -> {
            try {
                while (connecte) {
                    Object objetRecu = entree.readObject();

                    if (objetRecu instanceof RequeteReseau requete && ecouteurMessages != null) {
                        ecouteurMessages.accept(requete);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                if (connecte) {
                    System.err.println("[CLIENT] Connexion au serveur perdue.");
                    connecte = false;
                }
            }
        });

        threadEcoute.setDaemon(true); // le thread s'arrête automatiquement si l'appli se ferme
        threadEcoute.start();
    }

    /**
     * Envoie une requête au serveur.
     */
    public synchronized void envoyer(RequeteReseau requete) {
        try {
            sortie.writeObject(requete);
            sortie.flush();
            sortie.reset();
        } catch (IOException e) {
            System.err.println("[CLIENT] Erreur d'envoi : " + e.getMessage());
        }
    }

    /**
     * Définit la fonction à appeler à chaque fois qu'un message arrive du serveur.
     * Le contrôleur JavaFX branchera ici sa logique de mise à jour de l'interface.
     */
    public void setEcouteurMessages(Consumer<RequeteReseau> ecouteur) {
        this.ecouteurMessages = ecouteur;
    }

    public void deconnecter() {
        connecte = false;
        try {
            if (socket != null && !socket.isClosed()) {
                envoyer(new RequeteReseau(RequeteReseau.Type.DECONNEXION, null));
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("[CLIENT] Erreur lors de la déconnexion : " + e.getMessage());
        }
    }

    public boolean isConnecte() {
        return connecte;
    }
}