package sn.uam.uamconnect.common;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Représente un utilisateur de la plateforme (Administrateur, Enseignant ou Étudiant).
 * Implémente Serializable pour pouvoir être envoyé via Socket (ObjectOutputStream).
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Role {
        ADMINISTRATEUR, ENSEIGNANT, ETUDIANT
    }

    private int id;
    private String username;
    private String password; // haché (BCrypt), jamais en clair
    private Role role;
    private String nomComplet;
    private String email;
    private LocalDateTime dateCreation;
    private boolean estConnecte;

    // Constructeur vide (utile pour certains frameworks / DAO)
    public User() {
    }

    // Constructeur pour la création d'un nouvel utilisateur (sans id, pas encore en BDD)
    public User(String username, String password, Role role, String nomComplet, String email) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.nomComplet = nomComplet;
        this.email = email;
    }

    // Constructeur complet (utilisé quand on récupère un utilisateur depuis la BDD)
    public User(int id, String username, String password, Role role,
                String nomComplet, String email, LocalDateTime dateCreation, boolean estConnecte) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.nomComplet = nomComplet;
        this.email = email;
        this.dateCreation = dateCreation;
        this.estConnecte = estConnecte;
    }

    // ===== Getters & Setters =====

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getNomComplet() {
        return nomComplet;
    }

    public void setNomComplet(String nomComplet) {
        this.nomComplet = nomComplet;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public boolean isEstConnecte() {
        return estConnecte;
    }

    public void setEstConnecte(boolean estConnecte) {
        this.estConnecte = estConnecte;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", role=" + role +
                ", nomComplet='" + nomComplet + '\'' +
                '}';
    }
}