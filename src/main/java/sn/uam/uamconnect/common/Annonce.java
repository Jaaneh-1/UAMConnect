package sn.uam.uamconnect.common;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Représente une annonce pédagogique publiée par un enseignant ou l'administration
 * (devoir, emploi du temps, annonce générale).
 */
public class Annonce implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String titre;
    private String contenu;
    private int auteurId;
    private String auteurUsername; // pratique pour l'affichage côté client
    private Integer salonId;       // salon ciblé (ex: "L3 Informatique"), peut être null (annonce générale)
    private LocalDateTime datePublication;

    public Annonce() {
    }

    public Annonce(String titre, String contenu, int auteurId, Integer salonId) {
        this.titre = titre;
        this.contenu = contenu;
        this.auteurId = auteurId;
        this.salonId = salonId;
    }

    public Annonce(int id, String titre, String contenu, int auteurId, String auteurUsername,
                   Integer salonId, LocalDateTime datePublication) {
        this.id = id;
        this.titre = titre;
        this.contenu = contenu;
        this.auteurId = auteurId;
        this.auteurUsername = auteurUsername;
        this.salonId = salonId;
        this.datePublication = datePublication;
    }

    // ===== Getters & Setters =====

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public int getAuteurId() {
        return auteurId;
    }

    public void setAuteurId(int auteurId) {
        this.auteurId = auteurId;
    }

    public String getAuteurUsername() {
        return auteurUsername;
    }

    public void setAuteurUsername(String auteurUsername) {
        this.auteurUsername = auteurUsername;
    }

    public Integer getSalonId() {
        return salonId;
    }

    public void setSalonId(Integer salonId) {
        this.salonId = salonId;
    }

    public LocalDateTime getDatePublication() {
        return datePublication;
    }

    public void setDatePublication(LocalDateTime datePublication) {
        this.datePublication = datePublication;
    }

    @Override
    public String toString() {
        return titre;
    }
}