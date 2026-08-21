package sn.uam.uamconnect.common;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Représente un message échangé entre utilisateurs (privé) ou dans un salon (groupe).
 * Si destinataireId != 0 => message privé.
 * Si salonId != 0 => message de salon.
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int expediteurId;
    private String expediteurUsername; // pratique pour l'affichage côté client, sans requête supplémentaire
    private Integer destinataireId;    // null si message de salon
    private Integer salonId;           // null si message privé
    private String contenu;
    private LocalDateTime dateMessage;

    public Message() {
    }

    // Constructeur pour un nouveau message privé
    public static Message creerMessagePrive(int expediteurId, int destinataireId, String contenu) {
        Message m = new Message();
        m.expediteurId = expediteurId;
        m.destinataireId = destinataireId;
        m.contenu = contenu;
        return m;
    }

    // Constructeur pour un nouveau message de salon
    public static Message creerMessageSalon(int expediteurId, int salonId, String contenu) {
        Message m = new Message();
        m.expediteurId = expediteurId;
        m.salonId = salonId;
        m.contenu = contenu;
        return m;
    }

    public Message(int id, int expediteurId, String expediteurUsername, Integer destinataireId,
                   Integer salonId, String contenu, LocalDateTime dateMessage) {
        this.id = id;
        this.expediteurId = expediteurId;
        this.expediteurUsername = expediteurUsername;
        this.destinataireId = destinataireId;
        this.salonId = salonId;
        this.contenu = contenu;
        this.dateMessage = dateMessage;
    }

    // ===== Getters & Setters =====

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDateTime getDateMessage() {
        return dateMessage;
    }

    public void setDateMessage(LocalDateTime dateMessage) {
        this.dateMessage = dateMessage;
    }

    public boolean estMessagePrive() {
        return destinataireId != null;
    }

    public boolean estMessageSalon() {
        return salonId != null;
    }

    @Override
    public String toString() {
        return "[" + expediteurUsername + "] " + contenu;
    }
}