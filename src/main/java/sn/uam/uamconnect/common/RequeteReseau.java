package sn.uam.uamconnect.common;

import java.io.Serializable;

/**
 * Enveloppe générique pour toute communication entre client et serveur via Socket.
 * Chaque échange réseau est une RequeteReseau, identifiée par un type,
 * et transportant une donnée (payload) dont la nature dépend du type.
 */
public class RequeteReseau implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        // --- Authentification ---
        CONNEXION_DEMANDE,      // payload: String[] {username, motDePasse}
        CONNEXION_REPONSE_OK,   // payload: User
        CONNEXION_REPONSE_ECHEC,// payload: String (message d'erreur)
        DECONNEXION,            // payload: null

        // --- Messagerie ---
        MESSAGE_ENVOI,          // payload: Message
        MESSAGE_RECEPTION,      // payload: Message

        // --- Utilisateurs connectés ---
        LISTE_UTILISATEURS,     // payload: List<User>
        LISTE_UTILISATEURS_DEMANDE, // payload: null — demande explicite du client

        // --- Salons ---
        LISTE_SALONS,
        LISTE_SALONS_DEMANDE,    // payload: null — demande explicite du client// payload: List<Salon>
        HISTORIQUE_SALON_DEMANDE,// payload: Integer (id du salon)
        HISTORIQUE_SALON_REPONSE,// payload: List<Message>

        // --- Annonces ---
        ANNONCE_PUBLICATION,    // payload: Annonce (envoyé par le client qui publie)
        ANNONCE_NOUVELLE,       // payload: Annonce (diffusée aux autres clients)
        LISTE_ANNONCES_DEMANDE, // payload: null
        LISTE_ANNONCES_REPONSE, // payload: List<Annonce>

        // --- Fichiers ---
        FICHIER_ENVOI,          // payload: Fichier (métadonnées) — les octets suivent séparément
        FICHIER_RECEPTION,      // payload: Fichier

        // --- Notifications temps réel ---
        NOTIFICATION            // payload: String (ex: "Aminata vient de se connecter.")
    }

    private Type type;
    private Object payload;

    public RequeteReseau() {
    }

    public RequeteReseau(Type type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "RequeteReseau{type=" + type + ", payload=" + payload + '}';
    }
}