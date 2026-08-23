# NoticeFlow Sender v1.1.5 Beta — Firebase Registration

This is a new non-destructive Beta release for `app.sender`. Earlier Alpha and Beta releases remain available unchanged.

## Firebase configuration

The official Firebase client configuration for project `school-notics` is now installed locally for the Sender build. It contains the registered Sender Android identity and is processed through the Google Services Gradle plugin during the build. The `google-services.json` file is excluded from source control, release assets, and repository staging.

Sender retains only Firebase Authentication for its real Email/Password account session. Its live Receiver discovery and notice dispatch remain authenticated backend operations. No service-account credential, FCM server key, or private Firebase material is included in this app or release.

## Verification

Package `app.sender`, version code `8`, version `1.1.5-beta-firebase`, Android 8.0+, APK Signature Scheme v2 verified, official Sender signing certificate SHA-1 `D2:E7:74:1B:AB:01:19:63:69:DE:50:4B:D1:06:9A:88:1C:AC:D0:50`.
