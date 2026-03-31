# WearAmp 🎵

> A Plex music player for your wrist — stream your Plex library directly from a Wear OS watch, no phone required.

[![CI](https://github.com/03c/WearAmp/actions/workflows/ci.yml/badge.svg)](https://github.com/03c/WearAmp/actions/workflows/ci.yml)

---

## What is WearAmp?

WearAmp is an open-source Wear OS app that connects to your self-hosted [Plex](https://www.plex.tv/) media server and lets you browse and play your music library straight from your watch. Sign in with your Plex account, point the app at your server, and you're ready to listen.

## Features

- 🔐 **Easy Plex sign-in** — PIN-based authentication via [plex.tv/link](https://plex.tv/link); no typing passwords on a tiny screen
- 🎵 **Browse your library** — navigate music sections, artists, albums, and tracks from your wrist
- ▶️ **Playback controls** — play/pause and skip tracks from the Now Playing screen
- 📶 **Phone-free streaming** — works over Wi-Fi or mobile data without needing your phone nearby
- ⚙️ **Simple settings** — configure your Plex server URL and manage your account from the watch

## Requirements

- A Wear OS watch running **Wear OS 3.0 (API 30)** or later
- A **Plex Media Server** with a music library that is reachable from your watch (local network or remote access)
- A free [Plex account](https://www.plex.tv/sign-up/)

## Getting the app

> The app is currently in **Internal Testing** on Google Play. A public release will follow once the core features are complete.

In the meantime you can build and sideload it yourself — see [Building from source](#building-from-source) below.

## First-time setup

1. Open WearAmp on your watch. You'll be greeted by the **Sign In** screen.
2. Tap **Sign In** — a short PIN code will appear.
3. On any device with a browser, go to **[plex.tv/link](https://plex.tv/link)** and enter the PIN.
4. Once your Plex account is linked, the app navigates to your **Library**.
5. Scroll to the bottom of the **Library** screen and tap **Settings**, then tap **Server** to enter your Plex server URL, for example `http://192.168.1.100:32400`.

That's it — your music library is ready to browse.

---

## Building from source

### Prerequisites

- Android Studio Ladybug (2024.2) or newer
- JDK 17
- A Wear OS device or emulator (API 30+)

### Build & run

```bash
git clone https://github.com/03c/WearAmp.git
cd WearAmp
./gradlew :app:assembleDebug
```

Open the project in Android Studio and deploy to a Wear OS device or emulator.

---

## How Plex authentication works

WearAmp uses Plex's PIN-based OAuth flow — your password is never sent to or stored by the app.

```
App                        plex.tv
 |--- POST /api/v2/pins ------>|
 |<-- { id, code } ------------|
 |                             |
 |  Shows PIN to the user      |
 |  User visits plex.tv/link   |
 |  User enters the PIN        |
 |                             |
 |--- GET /api/v2/pins/{id} -->|  (polls every 3 s, up to 2 min)
 |<-- { auth_token } ----------|
 |                             |
 | Saves token → opens Library
```

---

## Contributing

Contributions are very welcome! Please read **[docs/contributing.md](docs/contributing.md)** for guidelines on setting up a dev environment, the project structure, and how to submit a pull request.

## Deployment & CI

Details on the GitHub Actions release pipeline and one-time Google Play setup are in **[docs/deployment.md](docs/deployment.md)**.

---

## License

WearAmp is open-source software. See the `LICENSE` file for details.

