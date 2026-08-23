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
        HISTORIQUE_PRIVE_DEMANDE, // payload: Integer (id de l'autre utilisateur)
        HISTORIQUE_PRIVE_REPONSE, // payload: List<Message>

        // --- Annonces ---
        ANNONCE_PUBLICATION,    // payload: Annonce (envoyé par le client qui publie)
        ANNONCE_NOUVELLE,       // payload: Annonce (diffusée aux autres clients)
        LISTE_ANNONCES_DEMANDE, // payload: null
        LISTE_ANNONCES_REPONSE, // payload: List<Annonce>

        // --- Fichiers ---
        FICHIER_ENVOI,           // (non utilisé sur ce canal - transfert via port dédié)
        FICHIER_RECEPTION,       // payload: Fichier (notification qu'un nouveau fichier est disponible)
        FICHIER_LISTE_DEMANDE,   // payload: Integer (id du salon)
        FICHIER_LISTE_REPONSE,   // payload: List<Fichier>

        // --- Notifications temps réel ---
        NOTIFICATION,            // payload: String (ex: "Aminata vient de se connecter.")

        // --- Gestion des utilisateurs (CRUD admin - section 4.2) ---
        UTILISATEURS_TOUS_DEMANDE,   // payload: null
        UTILISATEURS_TOUS_REPONSE,   // payload: List<User>
        UTILISATEUR_CREER,           // payload: User (avec mot de passe en clair dans le champ password)
        UTILISATEUR_MODIFIER,        // payload: User
        UTILISATEUR_SUPPRIMER,       // payload: Integer (id)
        UTILISATEUR_OPERATION_REPONSE, // payload: String (message de résultat, succès ou erreur)

        // --- Suppression de contenu (bonus) ---
        FICHIER_SUPPRIMER,  // payload: Integer (id du fichier) - demande du client
        FICHIER_SUPPRIME,   // payload: Integer (id du fichier) - diffusion pour retrait de l'affichage
        MESSAGE_SUPPRIMER,  // payload: Integer (id du message)
        MESSAGE_SUPPRIME,   // payload: Integer (id du message)

        PHOTO_PROFIL_MISE_A_JOUR, // payload: User (avec le nouveau champ photoProfil) - diffusé à tous
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