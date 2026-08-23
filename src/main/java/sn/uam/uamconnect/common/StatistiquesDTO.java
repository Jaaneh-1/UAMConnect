package sn.uam.uamconnect.common;

import java.io.Serializable;
import java.util.Map;

/**
 * Regroupe toutes les statistiques envoyées au dashboard en un seul objet,
 * pour éviter plusieurs allers-retours réseau.
 */
public class StatistiquesDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<String, Integer> utilisateursParRole;
    private Map<String, Integer> messagesParSalon;
    private Map<String, Integer> fichiersParSalon;

    public StatistiquesDTO() {
    }

    public StatistiquesDTO(Map<String, Integer> utilisateursParRole,
                           Map<String, Integer> messagesParSalon,
                           Map<String, Integer> fichiersParSalon) {
        this.utilisateursParRole = utilisateursParRole;
        this.messagesParSalon = messagesParSalon;
        this.fichiersParSalon = fichiersParSalon;
    }

    public Map<String, Integer> getUtilisateursParRole() {
        return utilisateursParRole;
    }

    public Map<String, Integer> getMessagesParSalon() {
        return messagesParSalon;
    }

    public Map<String, Integer> getFichiersParSalon() {
        return fichiersParSalon;
    }
}