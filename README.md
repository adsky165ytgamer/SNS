# NoticeFlow Sender

> **Testing build · `app.sender` · Proprietary software**

NoticeFlow Sender is the mobile control room for composing and delivering school notices to named Receiver devices.

## Current Home screen

The Home screen is the Sender’s operational dashboard. It shows the signed-in account state, Firebase connection status, live Receiver availability, recent notice activity, and the primary **Create Notice** action. The workflow remains phone-first:

1. Select a live Receiver.
2. Enter the notice type, title, and body.
3. Review the complete notice.
4. Write the notice directly to Firebase for that Receiver.

Receivers, Notices, and Settings remain available from the bottom navigation. The source for the Home screen and workflow is `android/sender-app/src/main/kotlin/app/sender/SenderActivity.kt`.

## Direct Firebase architecture

The Sender connects directly to the `school-notics` Firebase project:

- Firebase Authentication handles Email/Password and Google Sign-In.
- Firestore `receivers` documents provide live Receiver discovery.
- Firestore `receivers/{receiverId}/notices` stores outgoing notices.
- No Fastify server, HTTP `BackendClient`, Cloud Run service, Firebase Admin SDK, or FCM sender path is used.
- Firestore rules are in `firebase/firestore.rules`.

The direct data client is `android/sender-app/src/main/kotlin/app/sender/DirectFirebaseStore.kt`.

## Firebase configuration

`google-services.json` is intentionally ignored by Git. For a local build, place the Firebase Android configuration containing the `app.sender` client at:

```text
android/sender-app/google-services.json
```

The Google web OAuth client is configured in the build as:

```text
763216367314-7ikindb4e0cabej1ut4rhj7n0ejeke6q.apps.googleusercontent.com
```

Enable **Google** under Firebase Authentication and register the SHA-1 certificate used to sign the APK in Firebase Project Settings. For the current sandbox debug APK, the SHA-1 is:

```text
4E:5E:53:54:94:3D:8C:5A:31:9F:A4:40:2B:27:D9:17:58:09:23:72
```

## Build

```bash
cd android
../gradle-8.12/bin/gradle :sender-app:assembleDebug
```

The current testing APK is published in the GitHub testing release. The package is `app.sender`; the build targets SDK 36.

## Testing release

See [`RELEASE_NOTES_TESTING.md`](RELEASE_NOTES_TESTING.md) for the complete Sender change list, removed components, Firebase rules, Google Auth requirements, and APK checksum.

## License

This repository and application are proprietary. Do not commit `google-services.json`, service-account credentials, keystores, private keys, or local Gradle properties.
