# NoticeFlow Sender v1.1.7 Beta — Signed Authentication

This is a new non-destructive official-signed Beta release for `app.sender`.

## Authentication experience

The Sender account screen now clearly offers three real Firebase-backed paths: **sign in with email**, **create account**, and **continue with Google**. Password reset and sign-out remain available. Google uses the native Android Credential Manager flow and Firebase Authentication; it is not a browser mock or a hardcoded identity.

Sender preserves the live notice workflow: sign in, select a real Receiver, enter notice details, review, and send through the authenticated backend.

## Signing and verification

Package `app.sender`, version code `10`, version `1.1.7-beta-authentication`, Android 8.0+. The release APK verifies with APK Signature Scheme v2 and the official Sender signing certificate SHA-1 `D2:E7:74:1B:AB:01:19:63:69:DE:50:4B:D1:06:9A:88:1C:AC:D0:50`.

No keystore, password, Firebase client file, Web OAuth client value, OAuth secret, service-account credential, or FCM server key is included in this repository or release.
