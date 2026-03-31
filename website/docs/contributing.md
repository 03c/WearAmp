---
title: Contributing to WearAmp
layout: page
permalink: /docs/contributing/
description: "How to contribute to WearAmp — dev setup, project structure, and pull request guidelines."
---

Thanks for your interest in contributing! WearAmp is an open-source Wear OS music player for Plex. All kinds of contributions are welcome — bug fixes, new features, tests, and documentation improvements.

## Before you start

Please **open an issue** before starting work on a significant change. This lets us discuss the approach and make sure effort isn't duplicated.

For small fixes (typos, minor bugs) feel free to open a pull request directly.

---

## Getting started

### Prerequisites

- Android Studio Ladybug (2024.2) or newer
- JDK 17
- A Wear OS device or emulator (API 30 / Wear OS 3.0+)
- A Plex Media Server with a music library (for manual testing)

### Clone and build

```bash
git clone https://github.com/03c/WearAmp.git
cd WearAmp
./gradlew :app:assembleDebug
```

Open the project in Android Studio and run it on a Wear OS device or emulator.

### Running the tests

```bash
# Unit tests
./gradlew :app:testDebugUnitTest

# Lint
./gradlew :app:lintDebug
```

---

## Project structure

```
app/src/main/java/com/wearamp/
├── WearAmpApplication.kt          Application class (Hilt entry point)
├── MainActivity.kt                Wear OS main activity
├── di/
│   ├── NetworkModule.kt           Hilt network dependency providers
│   └── PlexServerUrlInterceptor.kt  OkHttp interceptor that rewrites
│                                    media API requests with the user's
│                                    configured server URL
├── data/
│   ├── api/
│   │   ├── PlexAuthApi.kt         Plex authentication REST API
│   │   ├── PlexMediaApi.kt        Plex media/library REST API
│   │   └── model/                 Gson data models (PlexPin, PlexMedia…)
│   ├── local/
│   │   └── UserPreferences.kt     DataStore — auth token, server URL,
│   │                              client ID, username
│   └── repository/
│       ├── AuthRepository.kt      PIN creation, polling & logout
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
        ├── library/                Music library section list
        ├── browse/                 Artists → Albums → Tracks browser
        ├── player/                 Now Playing controls
        └── settings/               Server URL & sign-out
```

### Key architectural decisions

| Concern | Approach |
|---|---|
| UI | Jetpack Compose for Wear OS |
| Navigation | `SwipeDismissableNavHost` (Wear gesture support) |
| DI | Hilt (ViewModels via `@HiltViewModel`) |
| State | `StateFlow` + `collectAsState()` |
| Media playback | Media3 / ExoPlayer + Horologist |
| Networking | Retrofit + OkHttp; server URL injected via interceptor |
| Auth | Plex PIN-based OAuth (plex.tv) |
| Persistence | DataStore Preferences |

---

## Pull request guidelines

1. **Branch from `main`** and name your branch descriptively (e.g. `feat/track-playback` or `fix/auth-timeout`).
2. **Keep pull requests focused** — one feature or fix per PR.
3. **Update documentation** if your change affects user-facing behaviour or the project structure.
4. **Run the tests and lint** before submitting:
   ```bash
   ./gradlew :app:testDebugUnitTest :app:lintDebug
   ```
5. **Fill in the PR description** — explain what changed and why.

The CI workflow will automatically build and lint your PR. A reviewer will take a look as soon as possible.

---

## Code style

- Kotlin — follow the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Compose — prefer stateless composables; pass state and callbacks down from ViewModels.
- No wildcard imports.

---

## License

By contributing you agree that your code will be released under the same license as the rest of the project.
