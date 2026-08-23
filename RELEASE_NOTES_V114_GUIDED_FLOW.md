# NoticeFlow Sender v1.1.4 Beta — Guided Notice Flow

This is a new non-destructive Beta release for `app.sender`. No prior release was replaced or removed.

## Corrected notice sequence

The Sender now enforces the correct live workflow instead of allowing disconnected navigation:

1. **Choose Receiver** from the authenticated live backend list.
2. **Enter notice details**, including type, title, and description, for that selected Receiver.
3. **Review and send**, confirming the exact live destination and notice content before the authenticated dispatch button becomes available.

The app returns to the chronological dispatch history only after the backend accepts the notice. Sign-in is required before creating a live notice, and the app gives clear loading, empty, selection, review, success, and error state feedback without creating mock device or delivery data.

## History refinement

Notice history is now a lightweight chronological feed with a delivery marker, recipient, timestamp, title, and preview rather than a stack of visually heavy tiles. The stored history still contains every distinct backend-accepted delivery.

## Verification

The package is `app.sender`, version code `7`, version `1.1.4-beta-sender-flow`, Android 8.0+, and signed with the official Sender certificate SHA-1 `D2:E7:74:1B:AB:01:19:63:69:DE:50:4B:D1:06:9A:88:1C:AC:D0:50`. The APK verifies with APK Signature Scheme v2. The Android release build, backend contract suite (5/5), and TypeScript check passed.
