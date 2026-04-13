package musicapp.model;

public final class Admin extends Utilisateur {
    private static final long serialVersionUID = 1L;

    public Admin(String id, String nomUtilisateur, String motDePasse) {
        super(id, nomUtilisateur, motDePasse);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }
}
