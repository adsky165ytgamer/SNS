# NoticeFlow Sender v1.1.3 Beta — Material 3 v2

This is a new non-destructive Beta release for `app.sender`. Earlier Alpha and Beta releases remain available unchanged.

## Shared Material 3 v2 system

The Sender now uses the same dark Material 3 v2 visual system as Receiver: consistent top status treatment, safe-edge layout, bottom navigation, responsive cards, shared account presentation, and state-focused feedback. The UI contains Home, Notices, Receivers, Settings, and a focused Create Notice flow.

## Live functionality preserved

This is not a static redesign. Firebase Email/Password sign-in, account creation, password reset, sign-out, authenticated live Receiver loading, real Receiver selection, notice composition, backend dispatch, success/error status, and local delivery history all use the working Sender contracts. Repeated accepted notices are retained by backend message ID rather than collapsed because their text matches.

## Verification

The package is `app.sender`, version code `6`, version `1.1.3-beta-sender-v2`, Android 8.0+, and signed with the official Sender certificate SHA-1 `D2:E7:74:1B:AB:01:19:63:69:DE:50:4B:D1:06:9A:88:1C:AC:D0:50`. The APK verifies with APK Signature Scheme v2. The backend contract suite passed 5/5 and the TypeScript check completed.

Install the matching Receiver v1.1.3 Beta, finish its sign-in/device-name/connection flow, then sign in to Sender and refresh the live Receiver list before sending a notice. Real physical-device delivery still needs verification with the running backend and FCM service.
