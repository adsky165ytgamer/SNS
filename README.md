# School Notice Sender

**NoticeFlow Sender** is the Android notice-composition application for the V0.1 school-notice broadcast prototype. It loads only real enabled Receiver records from the backend, requires a user to select one target explicitly, then unlocks notice composition and sends the selected device’s notice through the trusted backend.

> This is a prototype package. The included debug APK was built against a temporary test backend URL and may no longer connect after that endpoint expires. Configure a permanent HTTPS backend URL before relying on it.

## What this repository contains

| Path | Purpose |
|---|---|
| `android/sender-app/` | Sender Android source, package ID `app.sender` |
| `backend/` | Shared Fastify + Firebase Admin backend source needed by the complete system |
| `prototype-apk/NoticeFlowSender-v0.1.1-debug.apk` | Verified prototype Sender debug APK |
| `docs/` | Device-test guide and complete V0.1 technical handoff |

## Sender flow

```text
Load registered Receiver records → select exact target → write notice →
POST /api/v1/test-notice → trusted backend → Firebase Cloud Messaging → Receiver
```

The sender does not contain privileged Firebase credentials and never sends directly to FCM. The backend controls Firestore access and FCM delivery.

## Local build prerequisites

1. Deploy or run the trusted backend with Firebase Admin credentials configured only in the server environment.
2. Copy `android/gradle.properties.example` to `android/gradle.properties` and set `BACKEND_BASE_URL` to the permanent HTTPS backend URL.
3. From `android/`, run `./gradlew :sender-app:assembleDebug` or an equivalent Gradle command.

The Sender does not require `google-services.json`; it only uses the HTTPS REST backend.

## Backend setup

The `backend/` directory is the trusted Fastify service. Configure Firebase Admin credentials only through runtime environment configuration, such as `FIREBASE_SERVICE_ACCOUNT_JSON`, or a cloud runtime identity. Never commit credentials. See `backend/README.md` and `docs/NOTICEFLOW_V01_DETAILED_HANDOFF_PROMPT.md`.

## Prototype status

The target-first Sender interaction was verified with a real registered Receiver record. The Sender loaded the live Receiver, required explicit selection, submitted its precise receiver ID to the backend, and a real notice was delivered to the Receiver.
