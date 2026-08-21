package sn.uam.uamconnect.server.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Classe utilitaire responsable de la connexion à la base de données MySQL.
 * Lit les identifiants depuis le fichier db.properties (configuration externalisée).
 *
 * Utilise le pattern "Singleton léger" : une seule connexion partagée,
 * recréée automatiquement si elle est fermée ou invalide.
 */
public class ConnexionMySQL {

    private static Connection connection;
    private static Properties proprietes;

    // Empêche l'instanciation : classe utilitaire uniquement statique
    private ConnexionMySQL() {
    }

    /**
     * Charge le fichier db.properties une seule fois (lazy loading).
     */
    private static void chargerProprietes() {
        if (proprietes == null) {
            proprietes = new Properties();
            try (InputStream input = ConnexionMySQL.class
                    .getClassLoader()
                    .getResourceAsStream("db.properties")) {

                if (input == null) {
                    throw new RuntimeException(
                            "Fichier db.properties introuvable dans src/main/resources"
                    );
                }
                proprietes.load(input);

            } catch (IOException e) {
                throw new RuntimeException("Erreur de lecture de db.properties", e);
            }
        }
    }

    /**
     * Retourne une connexion active à la base de données.
     * Recrée la connexion si elle est fermée ou inexistante.
     *
     * @return Connection JDBC valide
     */
    public static Connection getConnection() {
        try {
            chargerProprietes();

            if (connection == null || connection.isClosed()) {
                String url = proprietes.getProperty("db.url");
                String user = proprietes.getProperty("db.user");
                String password = proprietes.getProperty("db.password");

                connection = DriverManager.getConnection(url, user, password);
                System.out.println("[BDD] Connexion MySQL établie avec succès.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Échec de connexion à la base de données", e);
        }

        return connection;
    }

    /**
     * Ferme proprement la connexion à la base de données.
     * À appeler lors de l'arrêt du serveur.
     */
    public static void fermerConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[BDD] Connexion MySQL fermée.");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la fermeture de la connexion : " + e.getMessage());
        }
    }
}