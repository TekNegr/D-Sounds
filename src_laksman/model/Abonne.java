package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Abonne extends Utilisateur {
    private static final long serialVersionUID = 1L;

    private final List<String> historiqueEcoute;

    public Abonne(String id, String nomUtilisateur, String motDePasse) {
        super(id, nomUtilisateur, motDePasse);
        this.historiqueEcoute = new ArrayList<>();
    }

    public void ajouterHistorique(String morceau) {
        if (morceau != null && !morceau.isBlank()) {
            historiqueEcoute.add(morceau.trim());
        }
    }

    public List<String> getHistoriqueEcoute() {
        return Collections.unmodifiableList(historiqueEcoute);
    }

    @Override
    public Role getRole() {
        return Role.ABONNE;
    }
}
