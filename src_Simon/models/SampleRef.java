package models;

import java.io.Serializable;

/**
 * Référence vers un échantillon sonore (sample).
 */
public class SampleRef implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String nom;
    private String cheminFichier;

    public SampleRef(String id, String nom, String cheminFichier) {
        this.id = id;
        this.nom = nom;
        this.cheminFichier = cheminFichier;
    }

    public String getId() { return id; }
    public String getNom() { return nom; }
    public String getCheminFichier() { return cheminFichier; }
}
