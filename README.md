# D-Sounds : Création musicale avec Strudel propulsée par JavaFX

D-Sounds est une application de bureau Java et JavaFX dédiée à la création, la mise en file d’attente et l’écoute de motifs Strudel.
Le projet prend en charge la création en solo ainsi que les salles de jam collaboratives en temps réel.

## Vision

D-Sounds est une application sociale de création musicale où :
- les utilisateurs écrivent et exécutent des motifs Strudel
- les utilisateurs peuvent écouter instantanément un motif unique
- les utilisateurs peuvent mettre en file d’attente plusieurs motifs pour composer un morceau complet
- les utilisateurs peuvent créer des morceaux en solo sans mode party
- les utilisateurs peuvent, s’ils le souhaitent, rejoindre une salle de jam pour créer de la musique ensemble en temps réel
- les sessions de jam room peuvent être sauvegardées comme pistes réutilisables

Principe central :
- l’application stocke les blocs de composition musicale sous forme de motifs (patterns) en priorité, avec export optionnel de morceaux créés à partir de séquences de motifs mises en file d’attente

## Auteurs

- Henintsoa RAMAKAVELO
- Laksmann CHANDIRAKUMAR
- Emilien CORNILLION
- Simon ELYNN

## Orientation technique

### Frontend et Desktop

- Java 25
- JavaFX 25.0.2
- Architecture MVC
- JavaFX WebView pour l’exécution de Strudel dans un environnement web embarqué

### Moteur Strudel

- Strudel s’exécute dans le WebView grâce à JavaScript et WebAudio
- Java envoie le code des motifs à WebView pour exécution
- L’application prend en charge les opérations de démarrage, d’arrêt, de mise à jour et de mise en file d’attente des motifs

### Collaboration en temps réel (Mode Party)

Le mode party est uniquement collaboratif et n’est pas obligatoire pour la création musicale.
Les utilisateurs peuvent toujours créer des pistes en solo.

Choisissez un seul mode de synchronisation temps réel :
- Serveur Java WebSocket (recommandé pour le contrôle)
- Écouteurs Firebase Realtime Database ou Firestore (plus rapide pour un MVP)

Les deux options permettent :
- la présence des utilisateurs en temps réel dans la salle
- la mise à jour partagée de la file d’attente de motifs
- les flux d’événements d’activité

### Persistance des données

Stratégie hybride :
- cache local et données principales de l’application en SQLite
- état partagé dans le cloud pour les sessions multijoueurs via un backend temps réel

Modèle de stockage bibliothèque :
- motifs individuels
- morceaux complets comme séquences ordonnées de motifs

Les morceaux peuvent être sauvegardés à partir des sessions jam room ou du mode composition solo.

## Périmètre fonctionnel

### 1. Création et lecture de motifs

- Créer et éditer des motifs Strudel
- Exécuter un motif directement dans le lecteur embarqué
- Écouter un motif comme entité indépendante

### 2. Composition de pistes

- Mettre en file d’attente des motifs pour construire la chronologie d’un morceau
- Réorganiser, supprimer et gérer les versions des motifs mis en file d’attente
- Sauvegarder une file d’attente comme morceau complet dans la bibliothèque

### 3. Mode Party (Salle de Jam)

- Créer ou rejoindre une salle via un code
- Rédaction collaborative de motifs et édition partagée de la file d’attente
- Synchronisation de la lecture en cours pour tous les membres
- Événements en direct (connexion, départ, ajout/édition de motif, réorganisation de la file, votes)

Comportement important :
- le mode party est exclusivement pour la collaboration
- il n’est pas nécessaire d’utiliser le mode party pour créer des morceaux

### 4. Bibliothèque et réutilisation

- Sauvegarder et parcourir la bibliothèque de motifs
- Sauvegarder et parcourir la bibliothèque des morceaux complets (séquences de motifs)
- Importer les résultats d’une session jam dans sa bibliothèque personnelle
- Possibilité d’ajouter des notes et commentaires sur les motifs et les morceaux

### 5. Échantillons sonores (optionnel)

- Support des fichiers d’échantillons personnalisés utilisés dans les motifs Strudel
- En l’absence de fournisseur cloud gratuit, privilégier d’abord le stockage local
- Dès qu’un fournisseur gratuit viable est identifié, ajouter la synchronisation distante des échantillons

## Contraintes fonctionnelles

- Les données musicales sont centrées en priorité sur les motifs
- Les fonctions temps réel doivent se dégrader élégamment hors ligne
- Le stockage des fichiers d’échantillons reste optionnel et économique

## Modèle de domaine suggéré (adapté MVC)

- Utilisateur
- Motif (Pattern)
- VersionMotif (PatternVersion)
- ÉlémentFileAttenteMotif (PatternQueueItem)
- Morceau (Song)
- VersionMorceau (SongVersion)
- SalleJam (JamRoom)
- MembreSalle (RoomMember)
- ÉvénementSalle (RoomEvent)
- Note (Rating)
- Commentaire
- JournalActivité (ActivityLog)
- RéférenceÉchantillon (SampleRef)

## Structure de projet suggérée (direction actuelle)

```text
src/
  app/
    Main.java
  controllers/
    PatternController.java
    PlayerController.java
    LibraryController.java
    RoomController.java
  models/
    User.java
    Pattern.java
    Song.java
    PatternQueueItem.java
    JamRoom.java
    RoomEvent.java
    SampleRef.java
  services/
    strudel/
      StrudelBridgeService.java
      PatternExecutionService.java
    realtime/
      RoomRealtimeService.java
      WebSocketRoomClient.java
    persistence/
      UserRepository.java
      PatternRepository.java
      SongRepository.java
      ActivityRepository.java
      SampleRepository.java
  views/
    *.fxml
  web/
    strudel-host.html
```

## Feuille de route MVP

### Phase 1 : Flux local de création de motifs

- Stabiliser l’application MVC JavaFX
- Ajouter la persistance locale SQLite pour les motifs et morceaux
- Intégrer WebView et exécuter le code motif sauvegardé
- Mettre en place le flux de sauvegarde de la file vers un morceau

### Phase 2 : Bibliothèque et expérience de lecture

- Écrans pour la bibliothèque de motifs et de morceaux
- Lecture autonome de motif et de morceau
- Historique et versions des motifs

### Phase 3 : Mode Party

- Création et rejoindre salle
- Synchronisation des événements partagés sur la file d’attente
- Flux d’activité de salle et édition collaborative

### Phase 4 : Échantillons et partage

- Gestion des références d’échantillons
- Support des packs d’échantillons locaux en priorité
- Ajout optionnel du stockage distant des échantillons si un fournisseur gratuit est trouvé

## Checklist sécurité et fiabilité

- Ne jamais coder de secrets en dur dans le code source
- Valider les événements de salle temps réel côté serveur/hôte autoritaire
- Mettre en place le retry/backoff pour les opérations temps réel et de persistance
- Journaliser les échecs avec des messages conviviaux pour l’utilisateur
- Nettoyer/saniter les motifs avant exécution via le bridge

## Mise en place (actuelle)

### Prérequis

- Java 25
- JavaFX SDK 25.0.2
- IDE : VS Code, IntelliJ, ou Eclipse

### VS Code

1. Ouvrez le dossier du projet.
2. Vérifiez le chemin JavaFX dans .vscode/launch.json.
3. Lancez avec F5 ou Ctrl+F5.

## Prochaines étapes immédiates pour ce dépôt

1. Ajouter le dossier services/strudel avec un bridge de Java vers l’environnement JavaScript de WebView.
2. Ajouter le schéma SQLite pour motif, morceau, séquence_morceau_motif, note, journal_activité, référence_échantillon.
3. Implémenter les squelettes de PatternController et LibraryController.
4. Ajouter un strudel-host HTML minimal et le relier à la WebView JavaFX.
5. Implémenter le modèle d’événements de file d’attente en mode party pour la composition collaborative.

## Notes

Ce README reflète la direction "Strudel-first".
L’implémentation doit rester incrémentale afin que l’application reste utilisable à chaque étape.