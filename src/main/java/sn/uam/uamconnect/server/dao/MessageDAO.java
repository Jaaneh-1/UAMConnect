package sn.uam.uamconnect.server.dao;

import sn.uam.uamconnect.common.Message;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DAO responsable des opérations SQL sur la table "messages".
 * Gère à la fois les messages privés et les messages de salon.
 */
public class MessageDAO {

    /**
     * Enregistre un nouveau message (privé ou de salon) en base.
     * C'est ce qui assure "l'historique des conversations" exigé section 4.8.
     */
    public int creer(Message message) {
        String sql = "INSERT INTO messages (expediteur_id, destinataire_id, salon_id, contenu) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, message.getExpediteurId());

            if (message.getDestinataireId() != null) {
                ps.setInt(2, message.getDestinataireId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }

            if (message.getSalonId() != null) {
                ps.setInt(3, message.getSalonId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }

            ps.setString(4, message.getContenu());

            int lignesAffectees = ps.executeUpdate();

            if (lignesAffectees > 0) {
                try (ResultSet cles = ps.getGeneratedKeys()) {
                    if (cles.next()) {
                        return cles.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'enregistrement du message", e);
        }

        return -1;
    }

    /**
     * Récupère l'historique des messages d'un salon (les N derniers, du plus ancien au plus récent).
     */
    public List<Message> trouverParSalon(int salonId, int limite) {
        String sql = """
                SELECT m.*, u.username AS expediteur_username
                FROM messages m
                JOIN users u ON m.expediteur_id = u.id
                WHERE m.salon_id = ?
                ORDER BY m.date_message DESC
                LIMIT ?
                """;

        List<Message> messages = new ArrayList<>();

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, salonId);
            ps.setInt(2, limite);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapperResultSetVersMessage(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des messages du salon id=" + salonId, e);
        }

        // On les remet dans l'ordre chronologique (du plus ancien au plus récent) pour l'affichage
        java.util.Collections.reverse(messages);
        return messages;
    }

    /**
     * Récupère l'historique des messages privés entre deux utilisateurs.
     */
    public List<Message> trouverConversationPrivee(int userId1, int userId2, int limite) {
        String sql = """
                SELECT m.*, u.username AS expediteur_username
                FROM messages m
                JOIN users u ON m.expediteur_id = u.id
                WHERE (m.expediteur_id = ? AND m.destinataire_id = ?)
                   OR (m.expediteur_id = ? AND m.destinataire_id = ?)
                ORDER BY m.date_message DESC
                LIMIT ?
                """;

        List<Message> messages = new ArrayList<>();

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId1);
            ps.setInt(2, userId2);
            ps.setInt(3, userId2);
            ps.setInt(4, userId1);
            ps.setInt(5, limite);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapperResultSetVersMessage(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération de la conversation privée", e);
        }

        java.util.Collections.reverse(messages);
        return messages;
    }

    private Message mapperResultSetVersMessage(ResultSet rs) throws SQLException {
        Message message = new Message();
        message.setId(rs.getInt("id"));
        message.setExpediteurId(rs.getInt("expediteur_id"));
        message.setExpediteurUsername(rs.getString("expediteur_username"));

        int destinataireId = rs.getInt("destinataire_id");
        message.setDestinataireId(rs.wasNull() ? null : destinataireId);

        int salonId = rs.getInt("salon_id");
        message.setSalonId(rs.wasNull() ? null : salonId);

        message.setContenu(rs.getString("contenu"));

        Timestamp ts = rs.getTimestamp("date_message");
        if (ts != null) {
            message.setDateMessage(ts.toLocalDateTime());
        }

        return message;
    }

    public Optional<Message> trouverParId(int id) {
        String sql = """
            SELECT m.*, u.username AS expediteur_username
            FROM messages m
            JOIN users u ON m.expediteur_id = u.id
            WHERE m.id = ?
            """;

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapperResultSetVersMessage(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche du message id=" + id, e);
        }

        return Optional.empty();
    }

    public boolean supprimer(int id) {
        String sql = "DELETE FROM messages WHERE id = ?";

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du message id=" + id, e);
        }
    }

    public Map<String, Integer> compterParSalon() {
        String sql = """
            SELECT s.nom, COUNT(m.id) AS total
            FROM salons s
            LEFT JOIN messages m ON m.salon_id = s.id
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
            throw new RuntimeException("Erreur lors du comptage des messages par salon", e);
        }

        return resultat;
    }
}