package model;

public final class CataloguePermissions {
    private CataloguePermissions() {
    }

    public static boolean peutCreerPlaylist(Session session) {
        return session != null && session.estConnecte() && session.aRole(Role.ABONNE);
    }

    public static boolean peutGererCatalogue(Session session) {
        return session != null && session.estConnecte() && session.aRole(Role.ADMIN);
    }

    public static boolean peutConsulterCatalogue(Session session) {
        return true;
    }
}
