package sn.uam.uamconnect.server.dao;

import sn.uam.uamconnect.common.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO responsable de toutes les opérations SQL sur la table "users".
 * Utilise exclusivement des PreparedStatement (exigence du sujet, section 7).
 */
public class UserDAO {

    /**
     * Recherche un utilisateur par son username (utilisé pour la connexion).
     */
    public Optional<User> trouverParUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapperResultSetVersUser(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche de l'utilisateur : " + username, e);
        }

        return Optional.empty();
    }

    /**
     * Recherche un utilisateur par son id.
     */
    public Optional<User> trouverParId(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapperResultSetVersUser(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche de l'utilisateur id=" + id, e);
        }

        return Optional.empty();
    }

    /**
     * Retourne tous les utilisateurs (pour le CRUD admin - section 4.2 du sujet).
     */
    public List<User> trouverTous() {
        String sql = "SELECT * FROM users ORDER BY username";
        List<User> utilisateurs = new ArrayList<>();

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                utilisateurs.add(mapperResultSetVersUser(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des utilisateurs", e);
        }

        return utilisateurs;
    }

    /**
     * Crée un nouvel utilisateur (le mot de passe doit déjà être haché avant l'appel).
     * Retourne l'id généré, ou -1 en cas d'échec.
     */
    public int creer(User user) {
        String sql = "INSERT INTO users (username, password, role, nom_complet, email) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole().name());
            ps.setString(4, user.getNomComplet());
            ps.setString(5, user.getEmail());

            int lignesAffectees = ps.executeUpdate();

            if (lignesAffectees > 0) {
                try (ResultSet cles = ps.getGeneratedKeys()) {
                    if (cles.next()) {
                        return cles.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la création de l'utilisateur : " + user.getUsername(), e);
        }

        return -1;
    }

    /**
     * Met à jour les informations d'un utilisateur (hors mot de passe).
     */
    public boolean modifier(User user) {
        String sql = "UPDATE users SET nom_complet = ?, email = ?, role = ? WHERE id = ?";

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getNomComplet());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getRole().name());
            ps.setInt(4, user.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la modification de l'utilisateur id=" + user.getId(), e);
        }
    }

    /**
     * Supprime un utilisateur (CASCADE supprime aussi ses messages/fichiers en BDD).
     */
    public boolean supprimer(int id) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression de l'utilisateur id=" + id, e);
        }
    }

    /**
     * Met à jour le statut de connexion d'un utilisateur (utile pour "utilisateurs connectés").
     */
    public boolean mettreAJourStatutConnexion(int id, boolean estConnecte) {
        String sql = "UPDATE users SET est_connecte = ? WHERE id = ?";

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, estConnecte);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour du statut de connexion id=" + id, e);
        }
    }

    /**
     * Vérifie si un username existe déjà (utile avant création, pour éviter les doublons).
     */
    public boolean usernameExiste(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la vérification du username : " + username, e);
        }

        return false;
    }

    /**
     * Convertit une ligne de résultat SQL en objet User.
     * Méthode privée réutilisée par toutes les méthodes de lecture ci-dessus.
     */
    private User mapperResultSetVersUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setRole(User.Role.valueOf(rs.getString("role")));
        user.setNomComplet(rs.getString("nom_complet"));
        user.setEmail(rs.getString("email"));

        Timestamp ts = rs.getTimestamp("date_creation");
        if (ts != null) {
            user.setDateCreation(ts.toLocalDateTime());
        }

        user.setEstConnecte(rs.getBoolean("est_connecte"));

        user.setPhotoProfil(rs.getString("photo_profil"));

        return user;
    }

    public boolean mettreAJourPhotoProfil(int userId, String cheminPhoto) {
        String sql = "UPDATE users SET photo_profil = ? WHERE id = ?";

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cheminPhoto);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour de la photo de profil id=" + userId, e);
        }
    }
}