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
}