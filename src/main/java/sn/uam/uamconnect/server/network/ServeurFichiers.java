package sn.uam.uamconnect.server.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Serveur dédié au transfert de fichiers, sur un port séparé du chat (architecture
 * inspirée du protocole FTP : canal de contrôle + canal de données).
 */
public class ServeurFichiers {

    public static final int PORT_FICHIERS = 5001;

    private final ServeurUAM serveurPrincipal;

    public ServeurFichiers(ServeurUAM serveurPrincipal) {
        this.serveurPrincipal = serveurPrincipal;
    }

    public void demarrer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT_FICHIERS)) {
            System.out.println("[SERVEUR] Canal de transfert de fichiers actif sur le port " + PORT_FICHIERS);

            while (true) {
                Socket socketClient = serverSocket.accept();
                new GestionnaireTransfertFichier(socketClient, serveurPrincipal).start();
            }

        } catch (IOException e) {
            System.err.println("[SERVEUR] Erreur sur le canal de fichiers : " + e.getMessage());
        }
    }
}