# NoticeFlow Sender v1.1.1 Alpha Material 3

This is a new, non-destructive official signed Sender release for package `app.sender`; it does not replace any previous SNS release.

## Included changes

The Material 3 interface now has clearer active bottom-navigation treatment, real live-Receiver loading feedback, receiver-count state sourced only from the authenticated backend response, safe display-cutout and navigation-bar spacing, consistent touch targets, and a password-reset path alongside sign-in and account creation. The existing live Receiver selection, send operation, response/error handling, and persistent local delivery history are retained.

## Verified status

The release APK and AAB compile successfully. The APK is verified with Android APK Signature Scheme v2 and uses the Sender official certificate SHA-1 `D2:E7:74:1B:AB:01:19:63:69:DE:50:4B:D1:06:9A:88:1C:AC:D0:50`. Backend contract tests passed 5/5 and the TypeScript build completed successfully.

Physical-device end-to-end delivery remains pending an installed Receiver and Sender because no Android device is attached to this environment. This release does not fabricate devices, delivery acknowledgements, or notification proof.
