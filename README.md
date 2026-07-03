# مدرستنا - المعلم (Teacher App)

Android app for teachers. Talks **only** to the deployed backend
(`https://amer585-intlaqa-backend.hf.space/api`) over HTTPS — **no Turso/libSQL database
token is embedded in the APK**. The backend owns the databases, caching and
security and authenticates every request with a short-lived JWT.

Two entry points:

1. **Staff / grade entry** — username login → pick a class/subject → edit grades
   (routed through `/login`, `/hierarchy/*`, `/grades/update`).
2. **Teacher account** — email self-registration → admin approval → JWT login →
   a dashboard of linked students with read-only portal access
   (`/teacher/register|login|profile|students`, `/student/portal`).

## Setup

The backend URL is a **public** value and is shipped in `BuildConfig`. To
override it locally, create `local.properties` in the project root (gitignored):

```properties
BACKEND_URL=https://amer585-intlaqa-backend.hf.space/api
```

## Build & Run

Open in Android Studio and press Run, or:

```bash
./gradlew assembleDebug
```

## Architecture

- **Kotlin + Jetpack Compose** (Material 3), single-activity state navigation.
- **`ApiClient`** — OkHttp + JSON client; one JWT session in SharedPreferences.
- **`TeacherRepository`** — the single data layer; staff + teacher-account flows.
- **`TeacherAccountScreen`** — teacher login / register / dashboard.
- All network calls run on background threads and post results back to the UI.

## Security

There is **no** database credential in the APK. The app holds only a JWT issued
by the backend. Direct Turso access was removed so a decompiled APK leaks no
secrets — the backend is the only component that ever touches the databases.
