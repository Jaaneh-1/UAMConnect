package sn.uam.uamconnect.server.service;

import org.mindrot.jbcrypt.BCrypt;
import sn.uam.uamconnect.common.User;
import sn.uam.uamconnect.server.dao.UserDAO;

import java.util.Optional;

/**
 * Service responsable de l'authentification et de la création sécurisée des comptes.
 * Couche "service" : contient la logique métier, au-dessus du DAO (accès pur aux données).
 */
public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    /**
     * Tente de connecter un utilisateur avec son username et mot de passe en clair.
     *
     * @return l'utilisateur si les identifiants sont corrects, Optional.empty() sinon
     */
    public Optional<User> seConnecter(String username, String motDePasseClair) {
        Optional<User> utilisateurTrouve = userDAO.trouverParUsername(username);

        if (utilisateurTrouve.isEmpty()) {
            return Optional.empty(); // username inconnu
        }

        User user = utilisateurTrouve.get();

        // Comparaison sécurisée : BCrypt.checkpw compare le mot de passe en clair
        // avec le hash stocké, sans jamais déchiffrer ce dernier.
        boolean motDePasseCorrect = BCrypt.checkpw(motDePasseClair, user.getPassword());

        if (!motDePasseCorrect) {
            return Optional.empty(); // mot de passe incorrect
        }

        // Connexion réussie : on met à jour le statut et on marque l'utilisateur comme connecté
        userDAO.mettreAJourStatutConnexion(user.getId(), true);
        user.setEstConnecte(true);

        return Optional.of(user);
    }

    /**
     * Déconnecte un utilisateur (met à jour son statut en base).
     */
    public void seDeconnecter(int userId) {
        userDAO.mettreAJourStatutConnexion(userId, false);
    }

    /**
     * Inscrit un nouvel utilisateur : hache son mot de passe avant de le stocker.
     * Utilisé par le CRUD utilisateurs (section 4.2 du sujet) et par l'admin.
     *
     * @return l'id du nouvel utilisateur créé, ou -1 en cas d'échec (ex: username déjà pris)
     */
    public int inscrire(String username, String motDePasseClair, User.Role role,
                        String nomComplet, String email) {

        if (userDAO.usernameExiste(username)) {
            return -1; // username déjà utilisé
        }

        // Hachage du mot de passe : BCrypt.gensalt() génère un sel aléatoire à chaque appel
        String motDePasseHache = BCrypt.hashpw(motDePasseClair, BCrypt.gensalt());

        User nouvelUtilisateur = new User(username, motDePasseHache, role, nomComplet, email);

        return userDAO.creer(nouvelUtilisateur);
    }
}