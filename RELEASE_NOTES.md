# NoticeFlow Sender v0.1.2

This release packages the latest Sender application source and the matching debug APK.

## Included

- Firebase Email/Password sign-in, account creation, password reset, session restoration, and sign-out.
- Live receiver discovery from the authenticated backend.
- Explicit target selection using real Receiver names and IDs returned by the backend.
- Guided Account → Delivery Target → Notice workflow with animated transitions.
- Authenticated high-priority notice dispatch through Fastify and Firebase Cloud Messaging.
- Clear success, failure, and backend diagnostic states.
- Edge-to-edge layout handling for status bars, navigation bars, and display cutouts.

## APK

`NoticeFlow-Sender-debug.apk` targets package `app.sender`. Install it on a test Android device after configuring the original `school-notics` Firebase project and a reachable HTTPS backend.
