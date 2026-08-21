package sn.uam.uamconnect.common;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Représente un salon de discussion (ex: "L3 Informatique", "Master IA").
 */
public class Salon implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String nom;
    private String description;
    private LocalDateTime dateCreation;

    public Salon() {
    }

    public Salon(String nom, String description) {
        this.nom = nom;
        this.description = description;
    }

    public Salon(int id, String nom, String description, LocalDateTime dateCreation) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.dateCreation = dateCreation;
    }

    // ===== Getters & Setters =====

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    @Override
    public String toString() {
        return nom; // utile pour l'affichage direct dans une ListView JavaFX
    }
}