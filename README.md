# NoticeFlow Sender

The NoticeFlow Sender is the Android control surface for composing and delivering school notices to real Receiver installations. It authenticates a sender account, loads enabled Receiver records from the live backend, lets the operator choose one exact named device, and sends a title and message through Firebase Cloud Messaging.

The application package is `app.sender`. The current debug APK is available from the [latest Sender release](https://github.com/adsky165ytgamer/SNS/releases/latest).

## What the Sender does

The Sender follows a deliberate three-stage workflow: authenticate the operator, choose a live delivery target, and write and deliver the notice. Receiver cards are populated from the backend response; the application does not fabricate device names, IDs, availability, or delivery status. A selected card is highlighted before the composer unlocks, reducing the chance of sending a notice to the wrong installation.

When the operator sends a notice, the Sender posts the selected receiver ID, title, and message to the authenticated Fastify API. The backend retrieves the private FCM token, sends a high-priority data message, and records the dispatch in Firestore without returning or logging the token to the client.

## Authentication

The primary sign-in system is Firebase Email/Password authentication in the original `school-notics` Firebase project. The Sender supports account creation, sign-in, password reset, session restoration, and sign-out. Firebase issues an ID token after successful authentication, and receiver discovery and notice delivery send that token as `Authorization: Bearer <token>`.

The backend verifies the token with Firebase Admin SDK. A Sender without a valid authenticated session cannot load receivers or dispatch notices. Google Sign-In remains optional code, but it is not required for the primary Sender workflow.

## Sender setup

Install the APK from the release page and create or use an Email/Password account enabled in Firebase Authentication. Select **Load registered receivers**, tap the intended named device, write a concise title and message, and select **Send to selected receiver**. The status area reports whether the backend accepted the dispatch.

The application requires a reachable permanent HTTPS backend URL and the public Firebase client metadata for the original `school-notics` project. These values belong in local build configuration and must not be committed.

## Building from source

Copy `gradle.properties.example` to `gradle.properties`, set the permanent backend URL and the public Firebase client metadata, then build the Sender module.

```bash
cp gradle.properties.example gradle.properties
cd android
../gradle-8.13/bin/gradle :sender-app:assembleDebug
```

The resulting debug APK is written to `android/sender-app/build/outputs/apk/debug/sender-app-debug.apk`. The repository also includes the release artifact under `artifacts/NoticeFlow-Sender-debug.apk`.

## Backend contract

The Sender uses the following live endpoints:

| Endpoint | Purpose |
|---|---|
| `GET /health` | Checks backend reachability. |
| `GET /api/v1/receivers` | Returns enabled Receiver metadata for the authenticated Sender. |
| `POST /api/v1/test-notice` | Sends a high-priority notice to the selected Receiver and logs the dispatch. |

The backend remains the source of truth for receiver availability and notice delivery. The Sender only displays data returned by the live API.

## Security notes

Never place Firebase Admin service-account JSON, FCM server credentials, private keys, or production backend secrets in this repository or inside the APK. The Sender uses only Firebase client-side authentication and short-lived Firebase ID tokens. The service-account key previously exposed during development must be revoked and replaced before production use.

## Release contents

The GitHub release contains the matching debug APK for package `app.sender`, while this repository contains the sanitized Android source, backend source, documentation, build examples, and authentication handoff. A physical device test is still required to validate the final live FCM delivery path.
