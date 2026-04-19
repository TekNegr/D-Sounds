# dsounds - Action Plan

## Goal
Build a Java music app proof of concept where users can stream songs from cloud storage.

Phase 1 focuses only on storing music and playing it.
Phase 2 will add business logic (reviews, playlists, social features, etc.).

## Current Local MVP (Implemented)
The app now includes an Artist upload page for local development mode.

What is already working:
- You can open the Artist page from the home screen.
- You can choose an MP3 file.
- You can enter song details (title, artist, genre, description, lyrics).
- On save, the MP3 is copied to `local_storage/songs/`.
- Song metadata is saved to `local_storage/metadata/<id>.properties`.

The `local_storage/` folder is gitignored, so uploaded files are not committed.

How to test quickly:
1. Run the JavaFX app.
2. Open `Open Artist Upload Page`.
3. Select an MP3, fill title and artist (required), then click `Save Song Locally`.
4. Check the generated files under `local_storage/`.

## Team TODO (Current Sprint)

### Requested Tasks
1. (For me) Add removal.
2. (For me) Fix the Music Browser:
   - Publisher None => no display
   - Search and filters do not work
3. (For Emilien) Make the GUI better and more lookable.
   - Animations are optional, but desired if time allows.
4. ✅ (For Laksman) Make better role restrictions. — DONE
5. (For Simon) Improve transverse functionalities (reviews and collaborative playlists).
6. (For me) Merge everything (it is only downhill from there o_o).
7. (Not for me) Make the PPT and report.

### Suggested Extra Tasks
- (For me - core logic) Add delete flows with confirmation for songs, albums, and playlists, then sync repositories after deletion.
- (For me - core logic) Add quick regression checks after each merge: upload, browse, search, playlist play, and review submit/update.
- ✅ (For Laksman - auth) Enforce role checks both in UI and controller methods for all edit/delete actions. — DONE
- ✅ (For Laksman - auth) Add ownership checks for private playlists and collaborative actions. — DONE
- (For Simon - transverse) Define clear collaboration rules: owner, editor, viewer roles for collaborative playlists.
- (For Simon - transverse) Add conflict-safe behavior when two users edit the same playlist close in time.
- (For Emilien - GUI) Standardize spacing, typography, and button hierarchy across dashboard, list, playlist, and browser screens.
- (For Emilien - GUI) Add lightweight motion only where it improves UX (view transitions, now-playing feedback).
- (Team) Align JavaFX runtime and FXML API versions to remove compatibility/runtime warnings during demos.


---

## Laksman — Gestion des rôles et restrictions d'accès

> **Auteur : Laksman**
> Package concerné : `dsounds.security` + modifications dans `dsounds.controllers`

### Nouveaux fichiers

#### `dsounds/security/RoleGuard.java`
Point d'entrée unique pour toutes les vérifications de rôle dans le projet.

| Méthode | Utilisation |
|---|---|
| `requireAdmin(session)` | Lève `AuthException` si pas ADMIN — à appeler dans tout contrôleur admin |
| `requireSubscriber(session)` | Lève `AuthException` si pas SUBSCRIBER |
| `requireNotVisitor(session)` | Autorise SUBSCRIBER et ADMIN, refuse VISITOR |
| `requireAdminOrOwner(session, ownerUsername, resourceName)` | Pour les actions sur une ressource dont on doit être propriétaire ou admin |
| `requireAdminOnAccount(session, targetUsername)` | Empêche de modifier le compte admin par défaut |
| `isAdmin / isSubscriber / isVisitor` | Versions booléennes pour l'UI (désactivation des boutons) |
| `canModify / canCreatePlaylist / canManageCatalog / canManageUsers` | Conditions UI centralisées |

**Principe** : chaque action sensible est vérifiée *deux fois* — côté UI (bouton désactivé)
et côté contrôleur (exception levée), même si l'UI est contournée.

#### `dsounds/security/OwnershipChecker.java`
Gestion de l'appartenance et de l'accès collaboratif aux playlists.

- `canRead(session, playlist)` — playlist publique : tous ; privée : propriétaire, admin, ou collaborateur
- `canEdit(session, playlist)` — propriétaire, admin, ou collaborateur EDITOR
- `canDelete(session, playlist)` — propriétaire ou admin uniquement
- `addCollaborator(session, playlist, username, CollabRole)` — EDITOR ou VIEWER
- `removeCollaborator(session, playlist, username)` — idem
- `clearCollaborators(playlistId)` — nettoyage lors de la suppression d'une playlist

**CollabRole** : `EDITOR` (peut modifier les morceaux) / `VIEWER` (lecture seule d'une playlist privée).

Simon peut brancher `OwnershipChecker` directement dans `PlaylistController`
pour les playlists collaboratives via `addCollaborator()`.

### Fichiers modifiés

#### `dsounds/controllers/LocalAuthController.java`
- `suspendUser()` → appelle `RoleGuard.requireAdmin(session)` avant d'agir (défense en profondeur).
- `reactivateUser()` → idem.
- `deleteUser()` → idem.
- Javadoc mise à jour.

#### `dsounds/controllers/PlaylistController.java`
- `canEdit(playlist)` → délègue maintenant à `OwnershipChecker.canEdit()`, qui gère
  l'ownership ET les collaborateurs EDITOR (plus extensible).
- `createPlaylist()` → utilise `RoleGuard.canCreatePlaylist()` au lieu d'un test inline.
- `updatePermissions()` → idem pour `canCreate`.
- `deletePlaylist()` → appelle `ownershipChecker.clearCollaborators()` pour nettoyer le registre.

#### `dsounds/controllers/DashboardController.java`
Entièrement réécrit :
- `initialize()` appelle `applyRoleRestrictions()` à chaque ouverture du dashboard.
- **Bouton "Open Artist Upload Page"** : désactivé (+ opacité 0.45) pour non-admins.
- **Bouton "Open Playlists"** : désactivé (+ opacité 0.45) pour les visiteurs.
- **Label de bienvenue** : affiche le nom + rôle de l'utilisateur connecté.
- **Label d'info rôle** : message contextuel selon le rôle (visiteur limité, abonné, admin).

#### `dsounds/resources/dsounds/dashboard.fxml`
- `fx:id` ajoutés sur `artistUploadButton` et `playlistButton` pour le contrôle dynamique.
- `fx:id="welcomeLabel"` et `fx:id="roleInfoLabel"` ajoutés.

#### `dsounds/controllers/AuthController.java`
- Message de statut post-login enrichi avec le rôle et les droits.
- `continueAsVisitor()` informe l'utilisateur des restrictions visiteur.

#### `src/main/java/module-info.java`
- `exports dsounds.security;` ajouté pour rendre le package accessible.

### Comment l'équipe peut utiliser RoleGuard et OwnershipChecker

**Dans n'importe quel contrôleur FXML (côté UI) :**
```java
// Désactiver un bouton si pas admin
monBouton.setDisable(!RoleGuard.canManageCatalog(App.getAuthController().getSession()));

// Désactiver si pas abonné
monBouton.setDisable(RoleGuard.isVisitor(App.getAuthController().getSession()));
```

**Dans n'importe quelle méthode de contrôleur (côté logique) :**
```java
// Bloquer si pas admin (lève AuthException avec message clair)
RoleGuard.requireAdmin(App.getAuthController().getSession());

// Bloquer si pas propriétaire ni admin
RoleGuard.requireAdminOrOwner(session, playlist.getOwnerUsername(), "this playlist");
```

**Pour les playlists collaboratives (Simon) :**
```java
// Ajouter un éditeur collaborateur
ownershipChecker.addCollaborator(session, playlist, "simon_username", CollabRole.EDITOR);

// Vérifier si l'utilisateur courant peut modifier les morceaux
if (ownershipChecker.canEdit(session, playlist)) { ... }
```

---
## Target Architecture (Phase 1)
- Java backend API (Spring Boot recommended)
- Azure Blob Storage (private container) for MP3 files
- PostgreSQL for song metadata and user accounts
- JavaFX client (or web client) that requests playable URLs from backend

Important rule: do not store MP3 bytes directly in PostgreSQL for this project; store only metadata + blob path.

## Phase 1 Deliverables
1. Upload songs to Azure Blob Storage.
2. Save song metadata in PostgreSQL.
3. Generate short-lived read URLs (SAS) from backend.
4. Play songs in app using returned URL.
5. Basic authentication for API access.

## Step-by-Step Plan

### Step 1 - Prepare Azure Resources
Create:
- Resource Group
- Storage Account
- Private Blob Container (e.g. songs)
- Azure Database for PostgreSQL (Flexible Server)
- App Service (for backend deployment) - optional in early local testing

Checklist:
- Keep Blob container private.
- Enable HTTPS only.
- Store secrets in environment variables (or Key Vault later).

### Step 2 - Define Minimal Data Model
Create initial table:

- songs
  - id (UUID, PK)
  - title (text)
  - artist (text)
  - album (text, nullable)
  - duration_seconds (int)
  - blob_name (text)  # e.g. songs/<uuid>.mp3
  - mime_type (text)
  - created_at (timestamp)

Optional now, mandatory later:
- users table (for auth and ownership)

### Step 3 - Build Backend Endpoints (MVP)
Implement endpoints:
1. POST /songs/upload
   - Accept multipart MP3 file + metadata
   - Upload file to Blob Storage
   - Insert metadata into PostgreSQL
2. GET /songs
   - Return list of available songs (metadata only)
3. GET /songs/{id}/stream-url
   - Validate access
   - Return short-lived SAS URL (1 to 5 minutes)

### Step 4 - Integrate Player in Client
Client flow:
1. Call GET /songs
2. User selects a song
3. Call GET /songs/{id}/stream-url
4. Stream audio from URL in Java player

### Step 5 - Security Baseline
- Never expose storage account key in client app.
- Only backend generates SAS URLs.
- Keep SAS expiration short.
- Use parameterized SQL queries.
- Add simple request logging and error handling.

### Step 6 - Test and Demo Criteria
A successful Phase 1 demo means:
- You can upload at least 3 MP3 files.
- Songs appear in list with metadata.
- Two users can play the same song from separate clients.
- Expired SAS URL is rejected (expected behavior).

## Suggested Timeline (School-Friendly)
- Day 1: Azure resource setup + DB table
- Day 2: Upload endpoint + DB insert
- Day 3: Song listing + stream URL endpoint
- Day 4: Client playback integration
- Day 5: Cleanup, demo script, and bug fixes

## Phase 2 (Later)
After Phase 1 is stable, add:
- reviews (rating + comment)
- playlists and playlist songs
- likes/favorites
- listening rooms / synchronized playback
- moderation/admin tools

## Practical Notes
- Use only audio files you are allowed to use (copyright-safe for school demo).
- Start small: 5 to 20 songs is enough for proof of concept.
- Keep architecture simple first, then extend.

## Definition of Done for Current Scope
Project is considered ready for next phase when:
1. Cloud storage upload works.
2. Metadata persistence works.
3. Playback from signed cloud URL works.
4. End-to-end flow is reproducible from a clean setup.
