# NoticeFlow Sender — Mobile-first Android app

> **v1.1.0 Alpha · app.sender · Created by ad_vibe_dev · Proprietary / not open source**

NoticeFlow Sender is a mobile control room for composing and delivering school notices to real named Receiver devices. This release focuses on the phone experience only. Android TV, classroom-panel, and large-screen presentation work remain intentionally separate.

## Mobile experience

The app is organized around four bottom-navigation destinations: **Home**, **Notices**, **Receivers**, and **Settings**. Home provides a greeting, status summary, prominent **Create Notice** action, recent activity, receiver availability, and connection state. Notices presents a chronological local delivery history and opens individual notice details with type, recipient, sent time, message ID, and delivery state. Receivers replaces a cramped dropdown with searchable, tappable target cards. Settings contains the sender account, connection status, notifications, diagnostics, About, and license information.

Create Notice is a focused step-by-step mobile flow. The sender chooses a notice type, writes a title and description, chooses a real Receiver card, previews the complete message, and sends only after the target is explicit. The screen uses phone-sized spacing, large touch targets, edge-to-edge insets, navigation-bar protection, and short transition animations instead of stretching a desktop-like layout.

## Live behavior

Firebase Email/Password authentication remains the primary session. The Sender obtains a Firebase ID token and attaches it to protected API requests. Receiver cards are loaded from the live backend; no classroom names, online states, IDs, or delivery results are fabricated. A locally stored delivery history records only notices accepted by the backend.

## Build

Copy `gradle.properties.example` to `gradle.properties`, fill in the permanent HTTPS backend URL and public Firebase client metadata for `school-notics`, then build only the Sender module:

```bash
cp gradle.properties.example gradle.properties
cd android
../gradle-8.13/bin/gradle :sender-app:assembleDebug
```

The package is `app.sender`, version `1.1.0-alpha`, version code `3`. The matching APK is available from the [v1.1.0 Alpha release](https://github.com/adsky165ytgamer/SNS/releases/tag/v1.1.0-alpha).

## Proprietary license

This application and repository are proprietary and are not open source. No permission is granted to copy, redistribute, reverse engineer, modify, publish, or use the source or APK without written authorization from **ad_vibe_dev**. Never commit `google-services.json`, local Gradle properties, keystores, Firebase Admin credentials, FCM server credentials, or private keys.
