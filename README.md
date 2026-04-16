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
4. (For Laksman) Make better role restrictions.
5. (For Simon) Improve transverse functionalities (reviews and collaborative playlists).
6. (For me) Merge everything (it is only downhill from there o_o).
7. (Not for me) Make the PPT and report.

### Suggested Extra Tasks
- (For me - core logic) Add delete flows with confirmation for songs, albums, and playlists, then sync repositories after deletion.
- (For me - core logic) Add quick regression checks after each merge: upload, browse, search, playlist play, and review submit/update.
- (For Laksman - auth) Enforce role checks both in UI and controller methods for all edit/delete actions.
- (For Laksman - auth) Add ownership checks for private playlists and collaborative actions.
- (For Simon - transverse) Define clear collaboration rules: owner, editor, viewer roles for collaborative playlists.
- (For Simon - transverse) Add conflict-safe behavior when two users edit the same playlist close in time.
- (For Emilien - GUI) Standardize spacing, typography, and button hierarchy across dashboard, list, playlist, and browser screens.
- (For Emilien - GUI) Add lightweight motion only where it improves UX (view transitions, now-playing feedback).
- (Team) Align JavaFX runtime and FXML API versions to remove compatibility/runtime warnings during demos.

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
