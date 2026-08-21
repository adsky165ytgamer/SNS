# NoticeFlow Sender v1.1.0 Alpha — mobile loading fix

This Sender-only patch fixes the live Receiver workflow and removes the blank or ambiguous states that made the previous mobile build feel broken.

## Fixed

The Sender now refreshes the Firebase session before requesting Receiver records, reports the exact live-list state, surfaces the configured backend endpoint label, shows authentication and backend failures inside the Receivers screen, and offers a retry path instead of leaving an empty card area. After a successful Email/Password sign-in or account creation, it automatically starts loading live Receivers. Home also exposes a direct **Load live Receivers** action.

The phone layout remains intentionally restrained: Home, Notices, Receivers, and Settings are separated by bottom navigation; the recipient list is searchable; cards show the actual backend-provided receiver name and last-seen value; and no online/offline label is fabricated in the UI.

## Verification

The APK is package `app.sender`, version `1.1.0-alpha`, version code `3`. The live backend health and Receiver endpoint responded successfully with two enabled Receiver records during validation. The backend test suite passed all five tests.

This release changes only Sender. Receiver and backend source are unchanged. The app remains proprietary and is not open source; creator: **ad_vibe_dev**.
