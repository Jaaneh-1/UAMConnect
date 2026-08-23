package sn.uam.uamconnect.server.dao;

import sn.uam.uamconnect.common.Fichier;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DAO responsable des opérations SQL sur la table "fichiers".
 * Trace tous les fichiers partagés (section 4.7 du sujet).
 */
public class FichierDAO {

    public int creer(Fichier fichier) {
        String sql = "INSERT INTO fichiers (nom_fichier, chemin, expediteur_id, destinataire_id, salon_id, taille_octets) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, fichier.getNomFichier());
            ps.setString(2, fichier.getChemin());
            ps.setInt(3, fichier.getExpediteurId());

            if (fichier.getDestinataireId() != null) {
                ps.setInt(4, fichier.getDestinataireId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }

            if (fichier.getSalonId() != null) {
                ps.setInt(5, fichier.getSalonId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            ps.setLong(6, fichier.getTailleOctets());

            int lignesAffectees = ps.executeUpdate();

            if (lignesAffectees > 0) {
                try (ResultSet cles = ps.getGeneratedKeys()) {
                    if (cles.next()) {
                        return cles.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'enregistrement du fichier", e);
        }

        return -1;
    }

    /**
     * Récupère les fichiers partagés dans un salon.
     */
    public List<Fichier> trouverParSalon(int salonId) {
        String sql = """
                SELECT f.*, u.username AS expediteur_username
                FROM fichiers f
                JOIN users u ON f.expediteur_id = u.id
                WHERE f.salon_id = ?
                ORDER BY f.date_envoi DESC
                """;

        List<Fichier> fichiers = new ArrayList<>();

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, salonId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    fichiers.add(mapperResultSetVersFichier(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des fichiers du salon id=" + salonId, e);
        }

        return fichiers;
    }

    /**
     * Récupère les fichiers échangés en privé entre deux utilisateurs.
     */
    public List<Fichier> trouverEchangesPrives(int userId1, int userId2) {
        String sql = """
                SELECT f.*, u.username AS expediteur_username
                FROM fichiers f
                JOIN users u ON f.expediteur_id = u.id
                WHERE (f.expediteur_id = ? AND f.destinataire_id = ?)
                   OR (f.expediteur_id = ? AND f.destinataire_id = ?)
                ORDER BY f.date_envoi DESC
                """;

        List<Fichier> fichiers = new ArrayList<>();

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId1);
            ps.setInt(2, userId2);
            ps.setInt(3, userId2);
            ps.setInt(4, userId1);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    fichiers.add(mapperResultSetVersFichier(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des fichiers privés", e);
        }

        return fichiers;
    }

    private Fichier mapperResultSetVersFichier(ResultSet rs) throws SQLException {
        Fichier fichier = new Fichier();
        fichier.setId(rs.getInt("id"));
        fichier.setNomFichier(rs.getString("nom_fichier"));
        fichier.setChemin(rs.getString("chemin"));
        fichier.setExpediteurId(rs.getInt("expediteur_id"));
        fichier.setExpediteurUsername(rs.getString("expediteur_username"));

        int destinataireId = rs.getInt("destinataire_id");
        fichier.setDestinataireId(rs.wasNull() ? null : destinataireId);

        int salonId = rs.getInt("salon_id");
        fichier.setSalonId(rs.wasNull() ? null : salonId);

        fichier.setTailleOctets(rs.getLong("taille_octets"));

        Timestamp ts = rs.getTimestamp("date_envoi");
        if (ts != null) {
            fichier.setDateEnvoi(ts.toLocalDateTime());
        }

        return fichier;
    }

    public Optional<Fichier> trouverParId(int id) {
        String sql = """
            SELECT f.*, u.username AS expediteur_username
            FROM fichiers f
            JOIN users u ON f.expediteur_id = u.id
            WHERE f.id = ?
            """;

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapperResultSetVersFichier(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche du fichier id=" + id, e);
        }

        return Optional.empty();
    }

    public boolean supprimer(int id) {
        String sql = "DELETE FROM fichiers WHERE id = ?";

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du fichier id=" + id, e);
        }
    }

    public Map<String, Integer> compterParSalon() {
        String sql = """
            SELECT s.nom, COUNT(f.id) AS total
            FROM salons s
            LEFT JOIN fichiers f ON f.salon_id = s.id
            GROUP BY s.id, s.nom
            ORDER BY s.nom
            """;

        Map<String, Integer> resultat = new LinkedHashMap<>();

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                resultat.put(rs.getString("nom"), rs.getInt("total"));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du comptage des fichiers par salon", e);
        }

        return resultat;
    }
}