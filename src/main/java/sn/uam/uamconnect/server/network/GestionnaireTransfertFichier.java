package sn.uam.uamconnect.server.network;

import sn.uam.uamconnect.common.Fichier;
import sn.uam.uamconnect.common.RequeteReseau;
import sn.uam.uamconnect.server.dao.FichierDAO;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Gère UNE connexion de transfert de fichier (upload ou download), sur le port dédié.
 * Protocole simple basé sur DataInputStream/DataOutputStream (pas d'objets sérialisés ici,
 * pour manipuler des flux d'octets bruts efficacement, sans charger tout le fichier en mémoire).
 */
public class GestionnaireTransfertFichier extends Thread {

    private static final String DOSSIER_STOCKAGE = "fichiers_serveur";
    private static final int TAILLE_BUFFER = 8192; // 8 Ko par bloc de lecture/écriture

    private final Socket socket;
    private final ServeurUAM serveur;
    private final FichierDAO fichierDAO;

    public GestionnaireTransfertFichier(Socket socket, ServeurUAM serveur) {
        this.socket = socket;
        this.serveur = serveur;
        this.fichierDAO = new FichierDAO();
    }

    @Override
    public void run() {
        try (DataInputStream entree = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
             DataOutputStream sortie = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {

            String action = entree.readUTF(); // "UPLOAD" ou "DOWNLOAD"

            if ("UPLOAD".equals(action)) {
                traiterUpload(entree);
            } else if ("DOWNLOAD".equals(action)) {
                traiterDownload(entree, sortie);
            }

        } catch (IOException e) {
            System.err.println("[TRANSFERT] Erreur : " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Réceptionne un fichier envoyé par un client, le sauvegarde sur disque,
     * l'enregistre en base, puis notifie tous les clients (temps réel).
     */
    private void traiterUpload(DataInputStream entree) throws IOException {
        int expediteurId = entree.readInt();
        String nomFichier = entree.readUTF();
        int destinataireId = entree.readInt(); // -1 si aucun (envoi de salon)
        int salonId = entree.readInt();        // -1 si aucun (envoi privé)
        long tailleOctets = entree.readLong();

        // Crée le dossier de stockage s'il n'existe pas encore
        Files.createDirectories(Path.of(DOSSIER_STOCKAGE));

        // Préfixe le nom avec un timestamp pour éviter les collisions (2 fichiers "cours.pdf" par ex.)
        String nomStocke = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_")
                .format(java.time.LocalDateTime.now()) + nomFichier;
        Path cheminDisque = Path.of(DOSSIER_STOCKAGE, nomStocke);

        // Écriture par blocs : jamais tout le fichier en mémoire d'un coup
        try (OutputStream fichierSortie = new BufferedOutputStream(Files.newOutputStream(cheminDisque))) {
            byte[] buffer = new byte[TAILLE_BUFFER];
            long octetsRestants = tailleOctets;

            while (octetsRestants > 0) {
                int aLire = (int) Math.min(buffer.length, octetsRestants);
                int lus = entree.read(buffer, 0, aLire);
                if (lus == -1) break;
                fichierSortie.write(buffer, 0, lus);
                octetsRestants -= lus;
            }
        }

        // Enregistrement en base de données (section 4.7 : "les fichiers reçus sont enregistrés localement")
        Fichier fichier = new Fichier(
                nomFichier,
                cheminDisque.toString(),
                expediteurId,
                destinataireId == -1 ? null : destinataireId,
                salonId == -1 ? null : salonId,
                tailleOctets
        );

        int idGenere = fichierDAO.creer(fichier);
        fichier.setId(idGenere);

        Optional<sn.uam.uamconnect.common.User> expediteur =
                new sn.uam.uamconnect.server.dao.UserDAO().trouverParId(expediteurId);
        expediteur.ifPresent(u -> fichier.setExpediteurUsername(u.getUsername()));

        System.out.println("[TRANSFERT] Fichier reçu : " + nomFichier + " (" + tailleOctets + " octets)");

        // Notifie tous les clients connectés via le canal principal (chat), en temps réel
        RequeteReseau notification = new RequeteReseau(RequeteReseau.Type.FICHIER_RECEPTION, fichier);
        for (GestionnaireClient client : serveur.getClientsConnectes()) {
            client.envoyer(notification);
        }
        serveur.diffuserNotification(
                (expediteur.map(sn.uam.uamconnect.common.User::getUsername).orElse("Quelqu'un"))
                        + " a envoyé un fichier."
        );
    }

    /**
     * Envoie un fichier stocké sur le serveur vers un client qui le demande en téléchargement.
     */
    private void traiterDownload(DataInputStream entree, DataOutputStream sortie) throws IOException {
        int fichierId = entree.readInt();

        Optional<Fichier> resultat = fichierDAO.trouverParId(fichierId);

        if (resultat.isEmpty()) {
            sortie.writeLong(-1); // signale une erreur au client (fichier introuvable)
            return;
        }

        Fichier fichier = resultat.get();
        Path cheminDisque = Path.of(fichier.getChemin());

        if (!Files.exists(cheminDisque)) {
            sortie.writeLong(-1);
            return;
        }

        long taille = Files.size(cheminDisque);
        sortie.writeLong(taille);

        try (InputStream fichierEntree = new BufferedInputStream(Files.newInputStream(cheminDisque))) {
            byte[] buffer = new byte[TAILLE_BUFFER];
            int lus;
            while ((lus = fichierEntree.read(buffer)) != -1) {
                sortie.write(buffer, 0, lus);
            }
        }

        sortie.flush();
        System.out.println("[TRANSFERT] Fichier envoyé : " + fichier.getNomFichier());
    }
}