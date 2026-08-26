package sn.uam.uamconnect.client.network;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Lit la configuration réseau du client (adresse du serveur), pour permettre
 * de basculer facilement entre test local (localhost) et déploiement réel
 * sur le réseau local de l'université (LAN) sans recompiler le code.
 */
public class ConfigClient {

    private static String hoteServeur;

    public static String getHoteServeur() {
        if (hoteServeur == null) {
            Properties proprietes = new Properties();

            // 1. Cherche d'abord un fichier externe, à côté de l'exécutable (modifiable sans recompiler)
            java.io.File fichierExterne = new java.io.File("client.properties");

            try {
                if (fichierExterne.exists()) {
                    try (java.io.FileInputStream input = new java.io.FileInputStream(fichierExterne)) {
                        proprietes.load(input);
                        hoteServeur = proprietes.getProperty("serveur.hote", "localhost");
                    }
                } else {
                    // 2. Sinon, utilise celui embarqué dans le JAR (valeur par défaut)
                    try (InputStream input = ConfigClient.class.getClassLoader()
                            .getResourceAsStream("client.properties")) {
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
     * Enregistre une nouvelle adresse de serveur dans le fichier externe,
     * pour qu'elle soit mémorisée au prochain lancement de l'application.
     */
    public static void sauvegarderHoteServeur(String nouvelleAdresse) {
        hoteServeur = nouvelleAdresse; // mise à jour immédiate en mémoire

        java.io.File fichierExterne = new java.io.File("client.properties");

        try (java.io.FileOutputStream sortie = new java.io.FileOutputStream(fichierExterne)) {
            Properties proprietes = new Properties();
            proprietes.setProperty("serveur.hote", nouvelleAdresse);
            proprietes.store(sortie, "Configuration UAM Connect - généré automatiquement");
        } catch (IOException e) {
            System.err.println("Impossible d'enregistrer l'adresse du serveur : " + e.getMessage());
        }
    }
}