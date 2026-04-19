package models;

import java.io.Serializable;

/**
 * Référence vers une piste audio dans un pattern.
 */
public class TrackRef implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String nom;
    private String sampleId;

    public TrackRef(String id, String nom, String sampleId) {
        this.id = id;
        this.nom = nom;
        this.sampleId = sampleId;
    }

    public String getId() { return id; }
    public String getNom() { return nom; }
    public String getSampleId() { return sampleId; }
}
