package sn.uam.uamconnect.common;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Représente un fichier partagé (PDF, image, archive ZIP)
 * entre utilisateurs (privé) ou dans un salon (groupe).
 */
public class Fichier implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String nomFichier;
    private String chemin;         // chemin de stockage local sur le serveur
    private int expediteurId;
    private String expediteurUsername;
    private Integer destinataireId; // null si envoyé dans un salon
    private Integer salonId;        // null si envoi privé
    private long tailleOctets;
    private LocalDateTime dateEnvoi;

    public Fichier() {
    }

    public Fichier(String nomFichier, String chemin, int expediteurId,
                   Integer destinataireId, Integer salonId, long tailleOctets) {
        this.nomFichier = nomFichier;
        this.chemin = chemin;
        this.expediteurId = expediteurId;
        this.destinataireId = destinataireId;
        this.salonId = salonId;
        this.tailleOctets = tailleOctets;
    }

    public Fichier(int id, String nomFichier, String chemin, int expediteurId, String expediteurUsername,
                   Integer destinataireId, Integer salonId, long tailleOctets, LocalDateTime dateEnvoi) {
        this.id = id;
        this.nomFichier = nomFichier;
        this.chemin = chemin;
        this.expediteurId = expediteurId;
        this.expediteurUsername = expediteurUsername;
        this.destinataireId = destinataireId;
        this.salonId = salonId;
        this.tailleOctets = tailleOctets;
        this.dateEnvoi = dateEnvoi;
    }

    // ===== Getters & Setters =====

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNomFichier() {
        return nomFichier;
    }

    public void setNomFichier(String nomFichier) {
        this.nomFichier = nomFichier;
    }

    public String getChemin() {
        return chemin;
    }

    public void setChemin(String chemin) {
        this.chemin = chemin;
    }

    public int getExpediteurId() {
        return expediteurId;
    }

    public void setExpediteurId(int expediteurId) {
        this.expediteurId = expediteurId;
    }

    public String getExpediteurUsername() {
        return expediteurUsername;
    }

    public void setExpediteurUsername(String expediteurUsername) {
        this.expediteurUsername = expediteurUsername;
    }

    public Integer getDestinataireId() {
        return destinataireId;
    }

    public void setDestinataireId(Integer destinataireId) {
        this.destinataireId = destinataireId;
    }

    public Integer getSalonId() {
        return salonId;
    }

    public void setSalonId(Integer salonId) {
        this.salonId = salonId;
    }

    public long getTailleOctets() {
        return tailleOctets;
    }

    public void setTailleOctets(long tailleOctets) {
        this.tailleOctets = tailleOctets;
    }

    public LocalDateTime getDateEnvoi() {
        return dateEnvoi;
    }

    public void setDateEnvoi(LocalDateTime dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }

    @Override
    public String toString() {
        return nomFichier;
    }
}