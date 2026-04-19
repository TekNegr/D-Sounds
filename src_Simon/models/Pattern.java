package models;

import java.io.Serializable;

/**
 * Représente un pattern musical Strudel.
 * À compléter par le responsable du module Strudel.
 */
public class Pattern implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String nom;
    private String contenu; // Le code Strudel du pattern
    private String createurId;

    public Pattern(String id, String nom, String contenu, String createurId) {
        this.id = id;
        this.nom = nom;
        this.contenu = contenu;
        this.createurId = createurId;
    }

    public String getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public String getCreateurId() { return createurId; }

    @Override
    public String toString() { return nom; }
}
