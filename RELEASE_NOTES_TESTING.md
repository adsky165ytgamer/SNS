# NoticeFlow Sender — Testing Release

## Summary

This testing release moves the Sender from the former Fastify/HTTP delivery path to direct Firebase access.

## Included changes

- Updated the main Home screen dashboard in `SenderActivity.kt` with current account, Firebase connection, Receiver availability, activity, and Create Notice state.
- Added direct Firestore Receiver discovery from `receivers`.
- Added direct Firestore notice creation at `receivers/{receiverId}/notices`.
- Configured Firebase Authentication for Email/Password and Google Sign-In.
- Configured the production Google web OAuth client ID from the supplied Firebase project.
- Added AndroidX/Jetifier build flags and SDK 36 build configuration.
- Added `firebase/firestore.rules` for authenticated Sender/Receiver access.

## Removed

- Fastify backend runtime.
- HTTP `SenderBackendClient`.
- Cloud Run/API route dependency.
- Firebase Admin delivery dependency.
- FCM-based Sender delivery path.
- Stale backend URL Gradle property and backend-only module link.

## Firebase setup required

Publish `firebase/firestore.rules` to the `school-notics` Firebase project. Enable Google under Firebase Authentication and register the APK signing SHA-1 in Firebase Project Settings. Keep `google-services.json` local; it is intentionally ignored by Git.

## Artifact

- Package: `app.sender`
- Target SDK: 36
- APK: `NoticeFlow-Sender-google-auth-debug.apk`
- SHA-256: `71112fa8b55d28a1e4fe7bbd6328196d3bd341b894bc3698921d3762b58b7bc3`
- APK v2 signature: verified
