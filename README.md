# مدرستنا - المعلم (Teacher App)

Android app for teachers to view and edit student grades. Connects directly to
a Turso (libSQL) database via the HTTP API — no backend server needed.

## Setup

### 1. Get your Turso database token

The org-level API token you were given **does not work directly** for database
access. You need a **database-specific token**. Create one via the Turso API:

```bash
curl -X POST "https://api.turso.tech/v1/organizations/amer321/databases/amer/auth/tokens" \
  -H "Authorization: Bearer YOUR_ORG_TOKEN"
```

This returns a `jwt` — that's your database token.

### 2. Configure secrets

Create `local.properties` in the project root (gitignored):

```properties
TURSO_URL=https://amer-amer321.aws-eu-west-1.turso.io
TURSO_TOKEN=eyJhbGciOiJFZERTQSIsInR5cCI6IkpXVCJ9...(full database token)
```

⚠️ **Important**: The token must be the **full** database JWT (336 chars). If
truncated, you'll get HTTP 401 errors.

### 3. Build & Run

Open the project in Android Studio and press Run. Or:

```bash
./gradlew assembleDebug
```

## Demo Login

- **Username:** `ahmed`
- **Password:** `teacher123`

## Features

- Teacher login (SHA-256 password verification)
- View assigned classes
- Edit student grades per subject (grades_json single-column model)
- Save grades with instant feedback (Snackbar)

## Architecture

- **Kotlin + Jetpack Compose** (Material 3)
- **OkHttp** for Turso HTTP API calls
- **Single-activity** architecture with state-based navigation
- Direct database access via Turso v2/pipeline endpoint

## Security Note

The Turso token is embedded in the APK via `BuildConfig`. Anyone who decompiles
the APK could extract it. For production use, consider:
- Using a backend proxy (like the HF Space backend) instead of direct DB access
- Or using Turso's per-database auth with limited scope
