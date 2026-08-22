package sn.uam.uamconnect;

import sn.uam.uamconnect.common.User;
import sn.uam.uamconnect.server.dao.ConnexionMySQL;
import sn.uam.uamconnect.server.service.AuthService;

/**
 * Classe temporaire : crée rapidement des utilisateurs de test.
 * À utiliser autant de fois que besoin, puis à supprimer avant le rendu final.
 */
public class CreerUtilisateurTest {
    public static void main(String[] args) {
        AuthService authService = new AuthService();

        int id = authService.inscrire(
                "etudiant1",           // username
                "etudiant123",         // mot de passe en clair (sera haché automatiquement)
                User.Role.ETUDIANT,    // rôle
                "Fatou Diop",           // nom complet
                "fatou.diop@uam.sn"    // email
        );

        if (id != -1) {
            System.out.println("✅ Utilisateur créé avec succès ! id=" + id);
            System.out.println("Username : etudiant1");
            System.out.println("Mot de passe : etudiant123");
        } else {
            System.out.println("❌ Échec (username peut-être déjà pris).");
        }

        ConnexionMySQL.fermerConnection();
    }
}