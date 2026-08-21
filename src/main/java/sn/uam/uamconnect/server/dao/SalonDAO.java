package sn.uam.uamconnect.server.dao;

import sn.uam.uamconnect.common.Salon;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO responsable des opérations SQL sur la table "salons".
 */
public class SalonDAO {

    public List<Salon> trouverTous() {
        String sql = "SELECT * FROM salons ORDER BY nom";
        List<Salon> salons = new ArrayList<>();

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                salons.add(mapperResultSetVersSalon(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des salons", e);
        }

        return salons;
    }

    public Optional<Salon> trouverParId(int id) {
        String sql = "SELECT * FROM salons WHERE id = ?";

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapperResultSetVersSalon(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche du salon id=" + id, e);
        }

        return Optional.empty();
    }

    public int creer(Salon salon) {
        String sql = "INSERT INTO salons (nom, description) VALUES (?, ?)";

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, salon.getNom());
            ps.setString(2, salon.getDescription());

            int lignesAffectees = ps.executeUpdate();

            if (lignesAffectees > 0) {
                try (ResultSet cles = ps.getGeneratedKeys()) {
                    if (cles.next()) {
                        return cles.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la création du salon : " + salon.getNom(), e);
        }

        return -1;
    }

    public boolean supprimer(int id) {
        String sql = "DELETE FROM salons WHERE id = ?";

        try (Connection conn = ConnexionMySQL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du salon id=" + id, e);
        }
    }

    private Salon mapperResultSetVersSalon(ResultSet rs) throws SQLException {
        Salon salon = new Salon();
        salon.setId(rs.getInt("id"));
        salon.setNom(rs.getString("nom"));
        salon.setDescription(rs.getString("description"));

        Timestamp ts = rs.getTimestamp("date_creation");
        if (ts != null) {
            salon.setDateCreation(ts.toLocalDateTime());
        }

        return salon;
    }
}