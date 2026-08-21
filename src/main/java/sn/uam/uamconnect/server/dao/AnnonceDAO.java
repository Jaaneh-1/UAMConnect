package sn.uam.uamconnect.server.dao;

import sn.uam.uamconnect.common.Annonce;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO responsable des opérations SQL sur la table "annonces".
 * Permet aux enseignants de publier (section 4.6) et aux étudiants de consulter.
 */
public class AnnonceDAO {

    public int creer(Annonce annonce) {
        String sql = "INSERT INTO annonces (titre, contenu, auteur_id, salon_id) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, annonce.getTitre());
            ps.setString(2, annonce.getContenu());
            ps.setInt(3, annonce.getAuteurId());

            if (annonce.getSalonId() != null) {
                ps.setInt(4, annonce.getSalonId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }

            int lignesAffectees = ps.executeUpdate();

            if (lignesAffectees > 0) {
                try (ResultSet cles = ps.getGeneratedKeys()) {
                    if (cles.next()) {
                        return cles.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la création de l'annonce", e);
        }

        return -1;
    }

    /**
     * Récupère toutes les annonces (les plus récentes en premier).
     */
    public List<Annonce> trouverToutes() {
        String sql = """
                SELECT a.*, u.username AS auteur_username
                FROM annonces a
                JOIN users u ON a.auteur_id = u.id
                ORDER BY a.date_publication DESC
                """;

        List<Annonce> annonces = new ArrayList<>();

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                annonces.add(mapperResultSetVersAnnonce(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des annonces", e);
        }

        return annonces;
    }

    /**
     * Récupère les annonces ciblant un salon précis (ex: annonces de "L3 Informatique").
     */
    public List<Annonce> trouverParSalon(int salonId) {
        String sql = """
                SELECT a.*, u.username AS auteur_username
                FROM annonces a
                JOIN users u ON a.auteur_id = u.id
                WHERE a.salon_id = ?
                ORDER BY a.date_publication DESC
                """;

        List<Annonce> annonces = new ArrayList<>();

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, salonId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    annonces.add(mapperResultSetVersAnnonce(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des annonces du salon id=" + salonId, e);
        }

        return annonces;
    }

    public boolean supprimer(int id) {
        String sql = "DELETE FROM annonces WHERE id = ?";

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression de l'annonce id=" + id, e);
        }
    }

    private Annonce mapperResultSetVersAnnonce(ResultSet rs) throws SQLException {
        Annonce annonce = new Annonce();
        annonce.setId(rs.getInt("id"));
        annonce.setTitre(rs.getString("titre"));
        annonce.setContenu(rs.getString("contenu"));
        annonce.setAuteurId(rs.getInt("auteur_id"));
        annonce.setAuteurUsername(rs.getString("auteur_username"));

        int salonId = rs.getInt("salon_id");
        annonce.setSalonId(rs.wasNull() ? null : salonId);

        Timestamp ts = rs.getTimestamp("date_publication");
        if (ts != null) {
            annonce.setDatePublication(ts.toLocalDateTime());
        }

        return annonce;
    }
}