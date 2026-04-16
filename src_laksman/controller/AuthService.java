package controller;

import model.*;
import persistence.UtilisateurRepository;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class AuthService {
    private final UtilisateurRepository repository;
    private final Session session;

    public AuthService(UtilisateurRepository repository, Session session) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.session = Objects.requireNonNull(session, "session");
    }

    public void chargerDonnees() throws IOException, ClassNotFoundException {
        repository.charger();
        creerAdminParDefautSiNecessaire();
    }

    public void sauvegarderDonnees() throws IOException {
        repository.sauvegarder();
    }

    public Abonne inscrireAbonne(String nomUtilisateur, String motDePasse) throws AuthException {
        verifierNomUtilisateur(nomUtilisateur);
        verifierMotDePasse(motDePasse);
        verifierDisponible(nomUtilisateur);

        Abonne abonne;
        try {
            abonne = new Abonne(UUID.randomUUID().toString(), nomUtilisateur.trim(), motDePasse);
        } catch (IllegalArgumentException e) {
            throw new AuthException(e.getMessage());
        }

        repository.ajouter(abonne);
        return abonne;
    }

    public Visiteur creerVisiteurTemporaire() {
        return new Visiteur("VISITEUR-SESSION", "visiteur", "guest");
    }

    public Utilisateur connecter(String nomUtilisateur, String motDePasse) throws AuthException {
        verifierNomUtilisateur(nomUtilisateur);
        verifierMotDePasse(motDePasse);

        Utilisateur utilisateur = repository.trouverParNomUtilisateur(nomUtilisateur.trim());
        if (utilisateur == null) {
            throw new AuthException("Utilisateur introuvable.");
        }
        if (!utilisateur.isActif()) {
            throw new AuthException("Compte suspendu.");
        }
        if (!utilisateur.verifierMotDePasse(motDePasse)) {
            throw new AuthException("Mot de passe incorrect.");
        }

        session.ouvrir(utilisateur);
        return utilisateur;
    }

    public Utilisateur connecter(String nomUtilisateur, String motDePasse, Role roleAttendu) throws AuthException {
        Utilisateur utilisateur = connecter(nomUtilisateur, motDePasse);
        if (roleAttendu != null && utilisateur.getRole() != roleAttendu) {
            session.fermer();
            throw new AuthException("Ce compte n'a pas le bon rôle pour cet accès.");
        }
        return utilisateur;
    }



    public Utilisateur connecterOuCreerDepuisOAuth(String nomUtilisateurPropose) throws AuthException {
        String baseNom = nettoyerNomOAuth(nomUtilisateurPropose);
        String nomFinal = baseNom;
        int suffixe = 1;
        while (repository.existe(nomFinal)) {
            Utilisateur existant = repository.trouverParNomUtilisateur(nomFinal);
            if (existant != null && existant.isActif()) {
                session.ouvrir(existant);
                return existant;
            }
            nomFinal = baseNom + suffixe;
            suffixe++;
        }

        Abonne abonne = new Abonne(UUID.randomUUID().toString(), nomFinal, "oauth-temp");
        repository.ajouter(abonne);
        session.ouvrir(abonne);
        return abonne;
    }

    public void continuerCommeVisiteur() {
        session.ouvrir(creerVisiteurTemporaire());
    }

    public void deconnecter() {
        session.fermer();
    }

    public void suspendreCompte(String nomUtilisateur) throws AuthException {
        Utilisateur utilisateur = repository.trouverParNomUtilisateur(normaliserNomUtilisateur(nomUtilisateur));
        if (utilisateur == null) {
            throw new AuthException("Impossible de suspendre : utilisateur introuvable.");
        }
        if (utilisateur.getRole() == Role.ADMIN) {
            throw new AuthException("Le compte administrateur par défaut ne peut pas être suspendu.");
        }
        utilisateur.suspendre();
    }

    public void reactiverCompte(String nomUtilisateur) throws AuthException {
        Utilisateur utilisateur = repository.trouverParNomUtilisateur(normaliserNomUtilisateur(nomUtilisateur));
        if (utilisateur == null) {
            throw new AuthException("Impossible de réactiver : utilisateur introuvable.");
        }
        utilisateur.reactiver();
    }

    public void supprimerCompte(String nomUtilisateur) throws AuthException {
        Utilisateur utilisateur = repository.trouverParNomUtilisateur(normaliserNomUtilisateur(nomUtilisateur));
        if (utilisateur == null) {
            throw new AuthException("Impossible de supprimer : utilisateur introuvable.");
        }
        if (utilisateur.getRole() == Role.ADMIN) {
            throw new AuthException("Le compte administrateur par défaut ne peut pas être supprimé.");
        }

        repository.supprimer(nomUtilisateur);
        if (session.estConnecte()
                && session.getUtilisateurCourant().getNomUtilisateur().equalsIgnoreCase(nomUtilisateur.trim())) {
            session.fermer();
        }
    }

    public Collection<Utilisateur> listerUtilisateurs() {
        return repository.trouverTous().stream()
                .sorted(Comparator.comparing(Utilisateur::getRole)
                        .thenComparing(Utilisateur::getNomUtilisateur, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public Session getSession() {
        return session;
    }

    private void verifierDisponible(String nomUtilisateur) throws AuthException {
        if (repository.existe(nomUtilisateur.trim())) {
            throw new AuthException("Ce nom d'utilisateur existe déjà.");
        }
    }

    private void verifierNomUtilisateur(String nomUtilisateur) throws AuthException {
        if (nomUtilisateur == null || nomUtilisateur.isBlank()) {
            throw new AuthException("Le nom d'utilisateur est obligatoire.");
        }
    }

    private void verifierMotDePasse(String motDePasse) throws AuthException {
        if (motDePasse == null || motDePasse.isBlank()) {
            throw new AuthException("Le mot de passe est obligatoire.");
        }
        if (motDePasse.length() < 4) {
            throw new AuthException("Le mot de passe doit contenir au moins 4 caractères.");
        }
    }

    private String normaliserNomUtilisateur(String nomUtilisateur) throws AuthException {
        if (nomUtilisateur == null || nomUtilisateur.isBlank()) {
            throw new AuthException("Le nom d'utilisateur est obligatoire.");
        }
        return nomUtilisateur.trim();
    }



    private String nettoyerNomOAuth(String nomUtilisateurPropose) throws AuthException {
        if (nomUtilisateurPropose == null || nomUtilisateurPropose.isBlank()) {
            throw new AuthException("Impossible de créer un compte OAuth sans identifiant.");
        }
        String nettoye = nomUtilisateurPropose.trim().toLowerCase()
                .replaceAll("[^a-z0-9._-]", "_")
                .replaceAll("_+", "_");
        if (nettoye.isBlank()) {
            throw new AuthException("Impossible de dériver un identifiant local valide depuis OAuth.");
        }
        if (nettoye.length() < 3) {
            nettoye = nettoye + "_user";
        }
        return nettoye;
    }

    private void creerAdminParDefautSiNecessaire() {
        if (!repository.existe("admin")) {
            repository.ajouter(new Admin(UUID.randomUUID().toString(), "admin", "admin1234"));
        }
    }
}
