# NoticeFlow Sender v1.1.0 Alpha

This Alpha release rebuilds the Sender around a deliberate workspace instead of a single long dispatch form.

## What changed

The Sender now begins with a first-run introduction explaining the secure account, target selection, and delivery-history flow. After introduction, the app is divided into **Home**, **Send**, **History**, and **About** sections. The Send section keeps account sign-in, live target selection, and composition separate, while History records notices that the backend accepted for dispatch. About identifies **v1.1.0 Alpha**, creator **ad_vibe_dev**, and the proprietary no-open-source status.

The Sender still uses Firebase Email/Password authentication, a verified Firebase ID token, live Receiver discovery, exact named target selection, and authenticated notice dispatch through the Fastify backend and FCM. The package is `app.sender` and the release asset is `NoticeFlow-Sender-v1.1.0-Alpha.apk`.

## Important

This is an Alpha build. Use it with the original `school-notics` Firebase configuration and a reachable permanent HTTPS backend. The application is proprietary and not open source. Do not redistribute, reverse engineer, or publish the application or source without written authorization from ad_vibe_dev.

