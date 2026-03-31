---
title: "\U0001F510 Privacy Policy"
layout: page
description: "WearAmp privacy policy — what data we collect, how it's used, and how to contact us."
permalink: /privacy/
---

WearAmp ("we", "us", "our") is an open-source Wear OS application that connects to your
self-hosted [Plex Media Server](https://www.plex.tv/) to stream your personal music library.
This Privacy Policy explains how the app handles information related to your use of WearAmp.

## 1. Information We Collect

WearAmp collects only the minimum information required to function:

- **Plex Authentication Token** — After you sign in via [plex.tv/link](https://plex.tv/link),
  Plex returns a user-scoped authentication token (which may include both a user token and a
  server-scoped token). WearAmp stores these tokens locally on your watch using Android's
  DataStore (encrypted storage). They are used exclusively to authenticate requests from the
  app to your Plex Media Server.

- **Plex Server URL** — You manually enter the URL of your Plex Media Server (e.g.
  `http://192.168.1.100:32400`). This URL is stored locally on your watch using DataStore and
  is used only to direct API requests to your server.

- **Plex Client Identifier** — WearAmp stores a client ID assigned to your watch locally using
  DataStore. This identifier is required by Plex to recognize the WearAmp app instance when
  making API requests. It is not used for tracking outside your own Plex ecosystem.

- **Plex Username** — WearAmp may cache your Plex account display name locally on your watch
  using DataStore. This is used only to show your account name in the app interface (for
  example, to confirm which account is currently linked).

- **Plex User Thumbnail / Avatar** — WearAmp may cache a small avatar or profile thumbnail
  associated with your Plex account locally using DataStore. This is used only to visually
  represent your account inside the app UI.

All of the information listed above is stored only on your device (your watch) and is kept for
as long as you keep WearAmp installed and linked to your Plex account. You can remove this data
at any time by unlinking your Plex account, clearing WearAmp's app data, or uninstalling the
app. WearAmp does **not** collect, transmit, or share any of this information with the WearAmp
developers or any third-party analytics or advertising services.

## 2. How Information Is Used

- **Authentication Token** — Sent in request headers to your Plex Media Server and to the Plex
  API (`plex.tv`) solely to browse and stream your music library.

- **Server URL** — Used to construct API requests to your Plex server. It never leaves your
  device except as the target address of those requests.

## 3. Third-Party Services

WearAmp communicates with the following external services as part of its normal operation:

- **Plex (plex.tv)** — Used for PIN-based sign-in authentication. When you sign in, the app
  calls `plex.tv/api/v2/pins` to obtain a short-lived PIN, which you then enter at
  [plex.tv/link](https://plex.tv/link). Plex's own
  [Privacy Policy](https://www.plex.tv/about/privacy-legal/privacy-preferences/) governs data
  handled by Plex.

- **Your Plex Media Server** — WearAmp communicates directly with the Plex server address you
  provide. This server is operated by you and is not controlled by WearAmp.

WearAmp does **not** include any advertising SDKs, crash-reporting SDKs, or analytics SDKs.

## 4. Data Storage and Retention

- All data (authentication token and server URL) is stored exclusively on your Wear OS device
  using Android's DataStore.
- Data is retained until you sign out of WearAmp or uninstall the application, at which point
  it is deleted from your device.
- No data is stored on external servers operated by WearAmp.

## 5. Data Deletion

To remove all data stored by WearAmp:

- **Sign out** — Use the Settings screen inside WearAmp to sign out. This clears the stored
  authentication token and server URL.
- **Uninstall** — Uninstalling WearAmp from your watch removes all locally stored data.

## 6. Children's Privacy

WearAmp is not directed at children under 13 years of age and does not knowingly collect
personal information from children. If you believe a child has provided personal information
through the app, please contact us so we can address it.

## 7. Changes to This Policy

We may update this Privacy Policy from time to time. When we do, we will update this page to
reflect the changes. Continued use of WearAmp after changes are posted constitutes your
acceptance of the updated policy.

## 8. Contact

If you have questions or concerns about this Privacy Policy, you can reach us by opening an
issue on our [GitHub repository](https://github.com/03c/WearAmp/issues).
