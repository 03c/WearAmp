# WearAmp
WearAmp is a Wear OS companion app for Plex that lets you listen to your music library directly from your wrist.

## Features

- **Play music from Plex** – Stream your Plex music library directly on your Wear OS device
- **Easy login to Plex** – PIN-based authentication via plex.tv/link; no typing passwords on a tiny screen
- **Play music without a phone** – Supports streaming over mobile data when your phone is unavailable
- **Star / like a track** – Rate tracks directly from your watch
- **Browse library** – Navigate artists, albums and tracks from your wrist

## Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose for Wear OS |
| Navigation | Wear Compose Navigation (swipe-dismiss) |
| Media Playback | Media3 / ExoPlayer + Horologist |
| Networking | Retrofit + OkHttp |
| Authentication | Plex PIN-based OAuth (plex.tv) |
| Dependency Injection | Hilt |
| Local Storage | DataStore Preferences |
| Image Loading | Coil |
| Language | Kotlin |
| Min SDK | 30 (Wear OS 3.0) |

## Getting Started

### Prerequisites

- Android Studio Ladybug (2024.2) or newer
- A Plex Media Server with a music library
- A Wear OS device or emulator (API 30+)

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/03c/WearAmp.git
   cd WearAmp
   ```

2. Open the project in Android Studio.

3. Build and run on a Wear OS device or emulator:
   ```bash
   ./gradlew :app:assembleDebug
   ```

### First-Time Configuration

1. On first launch the app shows a **Sign In** screen.
2. Tap **Sign In** – a PIN code will appear on-screen.
3. On any browser, go to **https://plex.tv/link** and enter the PIN.
4. After successful authentication, the app navigates to the **Library** screen.
5. In **Settings**, enter your Plex server URL (e.g. `http://192.168.1.x:32400`).

## Project Structure

```
app/src/main/java/com/wearamp/
├── WearAmpApplication.kt          Application class (Hilt entry point)
├── MainActivity.kt                Wear OS main activity
├── di/
│   └── NetworkModule.kt           Hilt network dependency providers
├── data/
│   ├── api/
│   │   ├── PlexAuthApi.kt         Plex authentication REST API
│   │   ├── PlexMediaApi.kt        Plex media/library REST API
│   │   └── model/                 Gson data models
│   ├── local/
│   │   └── UserPreferences.kt     DataStore for auth token & settings
│   └── repository/
│       ├── AuthRepository.kt      Authentication logic & PIN polling
│       └── MediaRepository.kt     Library browsing & track rating
├── domain/
│   └── model/                     Domain models (Track, Album, Artist…)
├── service/
│   └── WearAmpMediaService.kt     Media3 background playback service
└── presentation/
    ├── navigation/                 Wear swipe-dismiss nav graph
    ├── theme/                      Compose theme (Plex orange palette)
    └── screens/
        ├── login/                  PIN login flow
        ├── library/                Music library sections
        ├── browse/                 Artists / albums / tracks
        ├── player/                 Now Playing controls
        └── settings/               Server URL & sign-out
```

## Plex Authentication Flow

```
App                        plex.tv
 |--- POST /api/v2/pins ------>|
 |<-- { id, code } ------------|
 |                             |
 |  Show PIN code to user      |
 |  User visits plex.tv/link   |
 |  User enters PIN code       |
 |                             |
 |--- GET /api/v2/pins/{id} -->|  (poll every 3 s)
 |<-- { auth_token } ----------|
 |                             |
 | Save token → navigate to library
```

## Deployment

Every merge to `main` triggers the **Deploy to Google Play** GitHub Actions workflow (`.github/workflows/deploy.yml`).
The workflow:

1. Decodes the release keystore from a base64 secret
2. Builds a signed release AAB with `versionCode = GITHUB_RUN_NUMBER` (auto-increments on every run)
3. Reads `versionName` from `version.properties` (bump manually for each meaningful release)
4. Uploads the AAB to the **Internal Testing** track on Google Play

### One-time setup steps

#### 1 – Generate a release keystore (do this once, keep it safe)

```bash
keytool -genkey -v \
  -keystore wearamp-release.jks \
  -alias wearamp \
  -keyalg RSA -keysize 2048 -validity 10000
```

Base64-encode it for the secret:

```bash
# macOS
base64 -i wearamp-release.jks | tr -d '\n'

# Linux
base64 -w 0 wearamp-release.jks
```

#### 2 – Create a Google Play service account

1. Open [Google Cloud Console](https://console.cloud.google.com/) and select (or create) a project.
2. Enable the **Google Play Android Developer API**.
3. Go to **IAM & Admin → Service Accounts** and create a new service account (any name, e.g. `github-deploy`).
4. Create a JSON key for that service account and download it.
5. Open [Google Play Console](https://play.google.com/console) → **Setup → API access**.
6. Link the Google Cloud project you used above.
7. Find the service account in the list and click **Grant access**.
8. Assign the **Release manager** permission (or at minimum **Release to Internal Testing**).

#### 3 – Add GitHub Secrets

In your repository go to **Settings → Secrets and variables → Actions** and add:

| Secret name | Value |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded content of `wearamp-release.jks` (step 1) |
| `KEY_STORE_PASSWORD` | The keystore password you chose in step 1 |
| `KEY_ALIAS` | `wearamp` (or whatever alias you used) |
| `KEY_PASSWORD` | The key password you chose in step 1 |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | Full JSON content of the service account key downloaded in step 2 |

#### 4 – First upload (manual)

Google Play requires at least one manually uploaded AAB before the API can push subsequent updates.
Build a release AAB locally (with your keystore env vars set) or via Android Studio, then upload it
to the **Internal Testing** track in the Play Console for package `com.wearamp` under account
`8704724568888000444`.  After that, every merge to `main` is handled automatically.

#### Bumping the version name

Edit `version.properties` and update `VERSION_NAME` before merging:

```
VERSION_NAME=1.1.0
```

The `versionCode` is always the GitHub Actions run number and never needs manual changes.

---

## Contributing

Pull requests are welcome. Please open an issue first to discuss proposed changes.

