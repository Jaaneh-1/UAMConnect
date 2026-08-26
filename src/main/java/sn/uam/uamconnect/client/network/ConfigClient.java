package sn.uam.uamconnect.client.network;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Lit et sauvegarde la configuration réseau du client (adresse du serveur).
 * Le fichier est stocké dans le dossier de données utilisateur (AppData),
 * où l'application a toujours le droit d'écrire, contrairement au dossier
 * d'installation (Program Files) qui est protégé par Windows.
 */
public class ConfigClient {

    private static String hoteServeur;

    /**
     * Retourne le fichier de config dans le dossier de données utilisateur,
     * en le créant si besoin (ex: C:\Users\<utilisateur>\AppData\Roaming\UAMConnect\).
     */
    private static File fichierConfigUtilisateur() {
        String dossierAppData = System.getenv("APPDATA"); // dossier standard Windows pour les configs utilisateur
        File dossier = new File(dossierAppData, "UAMConnect");
        if (!dossier.exists()) {
            dossier.mkdirs();
        }
        return new File(dossier, "client.properties");
    }

    public static String getHoteServeur() {
        if (hoteServeur == null) {
            Properties proprietes = new Properties();
            File fichierUtilisateur = fichierConfigUtilisateur();

            try {
                if (fichierUtilisateur.exists()) {
                    try (FileInputStream input = new FileInputStream(fichierUtilisateur)) {
                        proprietes.load(input);
                        hoteServeur = proprietes.getProperty("serveur.hote", "localhost");
                    }
                } else {
                    // Première utilisation : pas encore de config utilisateur, on prend la valeur par défaut embarquée
                    try (var input = ConfigClient.class.getClassLoader().getResourceAsStream("client.properties")) {
                        if (input != null) {
                            proprietes.load(input);
                            hoteServeur = proprietes.getProperty("serveur.hote", "localhost");
                        } else {
                            hoteServeur = "localhost";
                        }
                    }
                }
            } catch (IOException e) {
                hoteServeur = "localhost";
            }
        }
        return hoteServeur;
    }

    /**
     * Enregistre une nouvelle adresse de serveur dans le dossier utilisateur (AppData),
     * pour qu'elle soit mémorisée au prochain lancement, sans nécessiter de droits administrateur.
     */
    public static void sauvegarderHoteServeur(String nouvelleAdresse) {
        hoteServeur = nouvelleAdresse;

        File fichierUtilisateur = fichierConfigUtilisateur();

        try (FileOutputStream sortie = new FileOutputStream(fichierUtilisateur)) {
            Properties proprietes = new Properties();
            proprietes.setProperty("serveur.hote", nouvelleAdresse);
            proprietes.store(sortie, "Configuration UAM Connect - généré automatiquement");
        } catch (IOException e) {
            System.err.println("Impossible d'enregistrer l'adresse du serveur : " + e.getMessage());
        }
    }
}