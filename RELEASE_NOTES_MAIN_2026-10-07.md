# NoticeFlow Sender — UI/UX rebuild

## Included in this main-branch release

- Rebuilt the Sender experience around a clean drawer/sidebar information architecture.
- Added Overview, Receivers, Notice history, and Settings sections.
- Reworked notice creation into a deliberate three-step workflow:
  1. Choose a Receiver
  2. Write the notice
  3. Review and send
- Added clearer loading, empty, error, connection, and account states.
- Improved hierarchy, spacing, descriptions, button emphasis, and responsive Compose layout.
- Preserved the existing email authentication, Google authentication, direct Firestore Receiver list, direct Firestore notice creation, and local delivery history.

## Not included

- No new backend server was added.
- No changes were made to the direct Firebase data model or authentication flow.
- FCM/foreground-service/overlay work belongs to the Receiver release, not the Sender.

## Verification

- Debug APK built successfully with Gradle 8.12 and Android SDK 36.
- Package: `app.sender`
- Target SDK: 36
