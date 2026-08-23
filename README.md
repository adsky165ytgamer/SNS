# NoticeFlow Sender — Mobile-first Android app

> **v1.1.1 Alpha Material 3 · app.sender · Created by ad_vibe_dev · Proprietary / not open source**

NoticeFlow Sender is a mobile control room for composing and delivering school notices to real named Receiver devices. This release focuses on the phone experience only. Android TV, classroom-panel, and large-screen presentation work remain intentionally separate.

## Mobile experience

This official signed update preserves Firebase Email/Password authentication, live Receiver discovery, recipient selection, notice delivery, and persistent history while refining Material 3 active-navigation feedback, loading states, safe-area handling, and account recovery. It adds a password-reset path to the Sender sign-in dialog and shows a specific in-progress state while real Receiver data is loading.

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

The package is `app.sender`, version `1.1.1-alpha-material3`, version code `4`. The matching APK and AAB are available from the [v1.1.1 Alpha Material 3 release](https://github.com/adsky165ytgamer/SNS/releases/tag/v1.1.1-alpha-material3).

The official release key SHA-1 for `app.sender` is `D2:E7:74:1B:AB:01:19:63:69:DE:50:4B:D1:06:9A:88:1C:AC:D0:50`. Register it in the Google/Firebase Android OAuth configuration before attempting native Google sign-in. Email/Password is the tested primary sign-in route until a production Web OAuth client ID is configured.

## Proprietary license

This application and repository are proprietary and are not open source. No permission is granted to copy, redistribute, reverse engineer, modify, publish, or use the source or APK without written authorization from **ad_vibe_dev**. Never commit `google-services.json`, local Gradle properties, keystores, Firebase Admin credentials, FCM server credentials, or private keys.
