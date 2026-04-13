package musicapp.model;

public final class Visiteur extends Utilisateur {
    private static final long serialVersionUID = 1L;

    public Visiteur(String id, String nomUtilisateur, String motDePasse) {
        super(id, nomUtilisateur, motDePasse);
    }

    @Override
    public Role getRole() {
        return Role.VISITEUR;
    }
}
