# NoticeFlow Sender v1.1.0 Alpha — Reference UI integration

This release integrates the supplied `SenderActivity.kt` reference into the real Sender app rather than shipping a static mock.

The interface is wired to Firebase Email/Password authentication, the live `/api/v1/receivers` endpoint, tappable Receiver selection, the `/api/v1/test-notice` notice dispatch endpoint, and persistent local delivery history. Authentication and backend failures are shown in the real UI. The interface includes Home, Create Notice, Receiver selection, history, settings, safe phone insets, and restrained page-entry animations.

The notice type chips, account dialog, receiver search, refresh action, target selection, send flow, success screen, and history are functional. The Sender remains proprietary and is not open source. Creator: **ad_vibe_dev**.

Verification: package `app.sender`; version `1.1.0-alpha`; version code `3`; Android build successful; backend test suite 5/5 passed.
