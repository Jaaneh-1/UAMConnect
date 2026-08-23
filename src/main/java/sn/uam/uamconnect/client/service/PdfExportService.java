package sn.uam.uamconnect.client.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import sn.uam.uamconnect.common.Annonce;
import sn.uam.uamconnect.common.Message;
import sn.uam.uamconnect.common.StatistiquesDTO;
import javafx.scene.Node;
import javafx.scene.image.WritableImage;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;

/**
 * Service de génération de documents PDF pour les exports de l'application
 * (historique de salon, annonces, statistiques) - bonus section 8 du sujet.
 * Utilise Apache PDFBox pour construire les documents mot par mot / bloc par bloc,
 * avec gestion automatique du passage à la page suivante.
 */
public class PdfExportService {

    private static final float MARGE = 50;
    private static final float LARGEUR_PAGE = PDRectangle.A4.getWidth();
    private static final float HAUTEUR_PAGE = PDRectangle.A4.getHeight();
    private static final PDFont POLICE_TITRE = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont POLICE_TEXTE = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Contexte d'écriture partagé entre les méthodes : garde la page et la position Y actuelles,
     * pour permettre d'écrire du contenu long sur plusieurs pages sans dupliquer la logique.
     */
    private static class ContexteEcriture {
        PDDocument document;
        PDPage pageActuelle;
        PDPageContentStream flux;
        float positionY;

        ContexteEcriture(PDDocument document) throws IOException {
            this.document = document;
            nouvellePage();
        }

        void nouvellePage() throws IOException {
            if (flux != null) flux.close();
            pageActuelle = new PDPage(PDRectangle.A4);
            document.addPage(pageActuelle);
            flux = new PDPageContentStream(document, pageActuelle);
            positionY = HAUTEUR_PAGE - MARGE;
        }

        void assurerEspace(float hauteurNecessaire) throws IOException {
            if (positionY - hauteurNecessaire < MARGE) {
                nouvellePage();
            }
        }
    }

    /**
     * Écrit une ligne de titre (grand, gras) en haut du document.
     */
    protected void ecrireTitre(ContexteEcriture ctx, String texte) throws IOException {
        texte = nettoyerTexte(texte);
        ctx.assurerEspace(40);
        ctx.flux.beginText();
        ctx.flux.setFont(POLICE_TITRE, 18);
        ctx.flux.newLineAtOffset(MARGE, ctx.positionY);
        ctx.flux.showText(texte);
        ctx.flux.endText();
        ctx.positionY -= 30;
    }

    /**
     * Écrit une ligne de sous-titre (métadonnées : date de génération, etc.).
     */
    protected void ecrireSousTitre(ContexteEcriture ctx, String texte) throws IOException {
        texte = nettoyerTexte(texte);
        ctx.assurerEspace(20);
        ctx.flux.beginText();
        ctx.flux.setFont(POLICE_TEXTE, 10);
        ctx.flux.newLineAtOffset(MARGE, ctx.positionY);
        ctx.flux.showText(texte);
        ctx.flux.endText();
        ctx.positionY -= 25;
    }

    /**
     * Écrit un paragraphe de texte normal, avec retour à la ligne automatique
     * si le texte dépasse la largeur de la page.
     */
    protected void ecrireParagraphe(ContexteEcriture ctx, String texte) throws IOException {
        texte = nettoyerTexte(texte);
        List<String> lignes = decouperEnLignes(texte, POLICE_TEXTE, 11, LARGEUR_PAGE - 2 * MARGE);

        for (String ligne : lignes) {
            ctx.assurerEspace(16);
            ctx.flux.beginText();
            ctx.flux.setFont(POLICE_TEXTE, 11);
            ctx.flux.newLineAtOffset(MARGE, ctx.positionY);
            ctx.flux.showText(ligne);
            ctx.flux.endText();
            ctx.positionY -= 16;
        }
        ctx.positionY -= 6;
    }

    /**
     * Découpe un texte en lignes qui tiennent dans la largeur donnée (retour à la ligne automatique).
     */
    private List<String> decouperEnLignes(String texte, PDFont police, float taille, float largeurMax) throws IOException {
        List<String> lignes = new java.util.ArrayList<>();
        String[] mots = texte.split(" ");
        StringBuilder ligneActuelle = new StringBuilder();

        for (String mot : mots) {
            String essai = ligneActuelle.isEmpty() ? mot : ligneActuelle + " " + mot;
            float largeur = police.getStringWidth(essai) / 1000 * taille;

            if (largeur > largeurMax && !ligneActuelle.isEmpty()) {
                lignes.add(ligneActuelle.toString());
                ligneActuelle = new StringBuilder(mot);
            } else {
                ligneActuelle = new StringBuilder(essai);
            }
        }
        if (!ligneActuelle.isEmpty()) {
            lignes.add(ligneActuelle.toString());
        }

        return lignes;
    }

    protected String formaterDate(LocalDateTime date) {
        return date != null ? date.format(FORMAT_DATE) : "";
    }

    protected ContexteEcriture creerContexte(PDDocument document) throws IOException {
        return new ContexteEcriture(document);
    }

    protected void fermerContexte(ContexteEcriture ctx) throws IOException {
        ctx.flux.close();
    }

    /**
     * Exporte l'historique d'un salon en PDF.
     */
    public void exporterHistoriqueSalon(String nomSalon, List<Message> messages, File destination) throws IOException {
        try (PDDocument document = new PDDocument()) {
            ContexteEcriture ctx = creerContexte(document);

            ecrireTitre(ctx, "Historique du salon : " + nomSalon);
            ecrireSousTitre(ctx, "Document généré le " + formaterDate(LocalDateTime.now()) + " — UAM Connect");

            if (messages.isEmpty()) {
                ecrireParagraphe(ctx, "Aucun message dans ce salon.");
            }

            for (Message m : messages) {
                ecrireParagraphe(ctx, "[" + formaterDate(m.getDateMessage()) + "] " + m.getExpediteurUsername() + " : " + m.getContenu());
            }

            fermerContexte(ctx);
            document.save(destination);
        }
    }

    /**
     * Exporte la liste des annonces en PDF.
     */
    public void exporterAnnonces(List<Annonce> annonces, File destination) throws IOException {
        try (PDDocument document = new PDDocument()) {
            ContexteEcriture ctx = creerContexte(document);

            ecrireTitre(ctx, "Annonces — UAM Connect");
            ecrireSousTitre(ctx, "Document généré le " + formaterDate(LocalDateTime.now()));

            if (annonces.isEmpty()) {
                ecrireParagraphe(ctx, "Aucune annonce publiée.");
            }

            for (Annonce a : annonces) {
                ecrireSousTitre(ctx, a.getTitre() + " — par " + a.getAuteurUsername() + " (" + formaterDate(a.getDatePublication()) + ")");
                ecrireParagraphe(ctx, a.getContenu());
            }

            fermerContexte(ctx);
            document.save(destination);
        }
    }

    /**
     * Exporte les statistiques du dashboard en PDF, avec les graphiques capturés en image
     * (snapshot JavaFX) et un résumé chiffré en texte.
     */
    public void exporterStatistiques(StatistiquesDTO stats, List<Node> graphiquesAExporter, File destination) throws IOException {
        try (PDDocument document = new PDDocument()) {
            ContexteEcriture ctx = creerContexte(document);

            ecrireTitre(ctx, "Statistiques — UAM Connect");
            ecrireSousTitre(ctx, "Document généré le " + formaterDate(LocalDateTime.now()));

            ecrireSousTitre(ctx, "Utilisateurs par rôle :");
            for (Map.Entry<String, Integer> entree : stats.getUtilisateursParRole().entrySet()) {
                ecrireParagraphe(ctx, "- " + entree.getKey() + " : " + entree.getValue());
            }

            ecrireSousTitre(ctx, "Messages par salon :");
            for (Map.Entry<String, Integer> entree : stats.getMessagesParSalon().entrySet()) {
                ecrireParagraphe(ctx, "- " + entree.getKey() + " : " + entree.getValue());
            }

            ecrireSousTitre(ctx, "Fichiers par salon :");
            for (Map.Entry<String, Integer> entree : stats.getFichiersParSalon().entrySet()) {
                ecrireParagraphe(ctx, "- " + entree.getKey() + " : " + entree.getValue());
            }

            // Ajoute une capture visuelle de chaque graphique JavaFX
            for (Node graphique : graphiquesAExporter) {
                ctx.assurerEspace(220);
                PDImageXObject image = capturerNoeudEnImage(document, graphique);
                float largeurAffichee = 400;
                float hauteurAffichee = largeurAffichee * image.getHeight() / image.getWidth();
                ctx.flux.drawImage(image, MARGE, ctx.positionY - hauteurAffichee, largeurAffichee, hauteurAffichee);
                ctx.positionY -= (hauteurAffichee + 20);
            }

            fermerContexte(ctx);
            document.save(destination);
        }
    }

    /**
     * Prend une capture d'écran (snapshot) d'un nœud JavaFX (ex: un graphique) et la convertit
     * en image PDFBox insérable dans le document.
     */
    private PDImageXObject capturerNoeudEnImage(PDDocument document, Node noeud) throws IOException {
        WritableImage snapshot = noeud.snapshot(null, null);
        BufferedImage bufferedImage = javafx.embed.swing.SwingFXUtils.fromFXImage(snapshot, null);
        return LosslessFactory.createFromImage(document, bufferedImage);
    }

    /**
     * Retire les caractères non supportés par la police PDF standard (WinAnsiEncoding),
     * notamment les emojis. Évite un crash silencieux de PDFBox lors de l'export.
     */
    private String nettoyerTexte(String texte) {
        if (texte == null) return "";
        StringBuilder resultat = new StringBuilder();
        for (char c : texte.toCharArray()) {
            // WinAnsiEncoding ne couvre que les caractères latins de base (jusqu'à ~0x00FF, avec quelques exceptions)
            if (c <= 0x00FF || c == '\n') {
                resultat.append(c);
            } else {
                resultat.append('?'); // remplace tout caractère non supporté (emoji, etc.)
            }
        }
        return resultat.toString();
    }
}