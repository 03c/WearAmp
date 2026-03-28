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

## Contributing

Pull requests are welcome. Please open an issue first to discuss proposed changes.

