---
title: Deployment
layout: page
permalink: /guides/deployment/
description: "WearAmp deployment — GitHub Actions release pipeline and Google Play setup."
---

WearAmp is deployed to Google Play via GitHub Actions. Every merge to `main` automatically builds a signed release AAB and uploads it to the **Internal Testing** track.

## How it works

The workflow lives at `.github/workflows/deploy.yml` and runs on every push to `main`. It:

1. Decodes the release keystore from a base64-encoded GitHub secret.
2. Uses the GitHub Actions `run_number` as both the `versionCode` and the patch component of the version name — so run 42 produces version `1.0.42`.
3. Reads `VERSION_MAJOR_MINOR` from `version.properties` and combines it with the run number to form the full `versionName` (e.g. `1.0.42`).
4. Builds a signed release AAB with `./gradlew :app:bundleRelease` (includes native debug symbols).
5. Creates a GitHub Release and attaches the AAB.
6. Uploads the AAB to the **Wear OS Internal Testing** track (`wear:internal`) on Google Play for package `com.wearamp` with **draft** status.

> **Note:** Since August 2023, Wear OS apps must be published on a dedicated Wear OS release track (`wear:internal`, `wear:production`, etc.). The workflow uses `status: draft` because Google Play requires draft status for apps that haven't been fully published yet. Once the app has its first production release, you can change the status to `completed` in the workflow to auto-publish.

---

## One-time setup

### 1 — Generate a release keystore

Do this once and keep the file somewhere safe (not in the repo).

```bash
keytool -genkey -v \
  -keystore wearamp-release.jks \
  -alias wearamp \
  -keyalg RSA -keysize 2048 -validity 10000
```

Base64-encode it so it can be stored as a GitHub secret:

```bash
# macOS
base64 -i wearamp-release.jks | tr -d '\n'

# Linux
base64 -w 0 wearamp-release.jks
```

### 2 — Create a Google Play service account

1. Open [Google Cloud Console](https://console.cloud.google.com/) and select (or create) a project.
2. Enable the **Google Play Android Developer API**.
3. Go to **IAM & Admin → Service Accounts** and create a new service account (e.g. `github-deploy`).
4. Create a JSON key for that service account and download it.
5. Open [Google Play Console](https://play.google.com/console) → **Setup → API access**.
6. Link the Google Cloud project you used above.
7. Find the service account in the list and click **Grant access**.
8. Assign the **Release manager** permission (or at minimum **Release to Internal Testing**).

### 3 — Add GitHub Secrets

In your repository go to **Settings → Secrets and variables → Actions** and add:

| Secret name | Value |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded content of `wearamp-release.jks` (step 1) |
| `KEY_STORE_PASSWORD` | The keystore password you set in step 1 |
| `KEY_ALIAS` | `wearamp` (or whatever alias you used) |
| `KEY_PASSWORD` | The key password you set in step 1 |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | Full JSON content of the service account key from step 2 |

### 4 — First upload (manual)

Google Play requires at least one manually uploaded AAB before the API can push subsequent updates. Build a release AAB locally (with your keystore environment variables set) or via Android Studio, then upload it to the **Wear OS Internal Testing** track in Play Console for package `com.wearamp`. After that, every merge to `main` is handled automatically.

> **Important:** When uploading via Play Console, use the **Wear OS** release track — not the standard mobile internal testing track. Since August 2023, Wear OS updates must be published on a dedicated Wear OS track.

### 5 — Configure testers

After creating the internal testing track, you must add testers or the release won't be available to anyone:

1. In [Google Play Console](https://play.google.com/console), go to **Testing → Internal testing** (under the Wear OS section).
2. Under **Testers**, create an email list or use a Google Group.
3. Add tester email addresses and save.
4. Share the opt-in link with your testers so they can access the build.

---

## Bumping the version

Edit `version.properties` and update `VERSION_MAJOR_MINOR` before merging:

```properties
VERSION_MAJOR_MINOR=1.1
```

The patch number is always the GitHub Actions run number (e.g. `1.1.58`), and the `versionCode` is set to the same run number. You never need to manage either manually.

---

## CI (pull requests)

The `ci.yml` workflow runs on every pull request targeting `main` and:

- Builds a debug APK
- Runs unit tests
- Runs Android Lint
