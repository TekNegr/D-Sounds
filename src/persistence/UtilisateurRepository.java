package persistence;

import model.Utilisateur;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class UtilisateurRepository {
    private final Path cheminSauvegarde;
    private final Map<String, Utilisateur> utilisateursParNom;

    public UtilisateurRepository(Path cheminSauvegarde) {
        this.cheminSauvegarde = Objects.requireNonNull(cheminSauvegarde, "cheminSauvegarde");
        this.utilisateursParNom = new HashMap<>();
    }

    public void ajouter(Utilisateur utilisateur) {
        utilisateursParNom.put(utilisateur.getNomUtilisateur().toLowerCase(Locale.ROOT), utilisateur);
    }

    public boolean existe(String nomUtilisateur) {
        return nomUtilisateur != null
                && utilisateursParNom.containsKey(nomUtilisateur.trim().toLowerCase(Locale.ROOT));
    }

    public Utilisateur trouverParNomUtilisateur(String nomUtilisateur) {
        if (nomUtilisateur == null) {
            return null;
        }
        return utilisateursParNom.get(nomUtilisateur.trim().toLowerCase(Locale.ROOT));
    }

    public Collection<Utilisateur> trouverTous() {
        return Collections.unmodifiableCollection(utilisateursParNom.values());
    }

    public void supprimer(String nomUtilisateur) {
        if (nomUtilisateur != null) {
            utilisateursParNom.remove(nomUtilisateur.trim().toLowerCase(Locale.ROOT));
        }
    }

    public void sauvegarder() throws IOException {
        if (cheminSauvegarde.getParent() != null) {
            Files.createDirectories(cheminSauvegarde.getParent());
        }
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(cheminSauvegarde))) {
            out.writeObject(utilisateursParNom);
        }
    }

    @SuppressWarnings("unchecked")
    public void charger() throws IOException, ClassNotFoundException {
        if (!Files.exists(cheminSauvegarde)) {
            return;
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(cheminSauvegarde))) {
            Object objet = in.readObject();
            utilisateursParNom.clear();
            utilisateursParNom.putAll((Map<String, Utilisateur>) objet);
        }
    }
}
