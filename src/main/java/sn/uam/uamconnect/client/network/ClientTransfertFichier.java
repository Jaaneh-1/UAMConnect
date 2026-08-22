package sn.uam.uamconnect.client.network;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Gère le transfert de fichiers (upload/download) côté client, vers le canal
 * dédié du serveur (port 5001). Chaque transfert ouvre sa propre connexion,
 * indépendante du Socket principal de chat.
 */
public class ClientTransfertFichier {

    private static final String HOTE = "localhost";
    private static final int PORT_FICHIERS = 5001;
    private static final int TAILLE_BUFFER = 8192;

    /**
     * Envoie un fichier vers le serveur.
     *
     * @param cheminFichier   chemin du fichier local à envoyer
     * @param expediteurId    id de l'utilisateur qui envoie
     * @param destinataireId  id du destinataire (-1 si envoi dans un salon)
     * @param salonId         id du salon (-1 si envoi privé)
     */
    public void envoyerFichier(Path cheminFichier, int expediteurId, int destinataireId, int salonId) throws IOException {
        long tailleOctets = Files.size(cheminFichier);
        String nomFichier = cheminFichier.getFileName().toString();

        try (Socket socket = new Socket(HOTE, PORT_FICHIERS);
             DataOutputStream sortie = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
             InputStream fichierEntree = new BufferedInputStream(Files.newInputStream(cheminFichier))) {

            sortie.writeUTF("UPLOAD");
            sortie.writeInt(expediteurId);
            sortie.writeUTF(nomFichier);
            sortie.writeInt(destinataireId);
            sortie.writeInt(salonId);
            sortie.writeLong(tailleOctets);

            byte[] buffer = new byte[TAILLE_BUFFER];
            int lus;
            while ((lus = fichierEntree.read(buffer)) != -1) {
                sortie.write(buffer, 0, lus);
            }

            sortie.flush();
        }
    }

    /**
     * Télécharge un fichier depuis le serveur et le sauvegarde localement.
     *
     * @param fichierId          id du fichier à télécharger (en base)
     * @param destinationLocale  chemin où sauvegarder le fichier téléchargé
     */
    public void telechargerFichier(int fichierId, Path destinationLocale) throws IOException {
        try (Socket socket = new Socket(HOTE, PORT_FICHIERS);
             DataOutputStream sortie = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
             DataInputStream entree = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {

            sortie.writeUTF("DOWNLOAD");
            sortie.writeInt(fichierId);
            sortie.flush();

            long taille = entree.readLong();

            if (taille == -1) {
                throw new IOException("Fichier introuvable sur le serveur.");
            }

            Files.createDirectories(destinationLocale.getParent());

            try (OutputStream fichierSortie = new BufferedOutputStream(Files.newOutputStream(destinationLocale))) {
                byte[] buffer = new byte[TAILLE_BUFFER];
                long octetsRestants = taille;

                while (octetsRestants > 0) {
                    int aLire = (int) Math.min(buffer.length, octetsRestants);
                    int lus = entree.read(buffer, 0, aLire);
                    if (lus == -1) break;
                    fichierSortie.write(buffer, 0, lus);
                    octetsRestants -= lus;
                }
            }
        }
    }
}