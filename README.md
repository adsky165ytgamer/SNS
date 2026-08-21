# NoticeFlow Sender

> **v1.1.0 Alpha** · Created by **ad_vibe_dev** · **Proprietary software — not open source**

NoticeFlow Sender is the Android control surface for selecting live named Receiver devices and dispatching school notices. Its application package is `app.sender`.

## v1.1.0 Alpha experience

The Sender has been rebuilt as a multi-section workspace rather than a single long form. A first-run introduction explains the secure account, live target, notice composition, and delivery-history path. After onboarding, the application provides **Home**, **Send**, **History**, and **About** sections. Home identifies the next useful action, Send separates authentication, targeting, and writing, History records notices accepted by the backend, and About records the version, creator, and license status.

The Sender uses Firebase Email/Password authentication and a Firebase ID token for every protected API call. It loads only real enabled Receivers returned by the backend, requires the operator to select one explicit named device before the composer opens, and records successful backend acceptances in a private local delivery history.

## Install and configure

Download the current APK from [Releases](https://github.com/adsky165ytgamer/SNS/releases). Install it on the sender device, create or use an Email/Password account enabled in the original `school-notics` Firebase project, then load live Receivers from the permanent HTTPS backend.

To build locally, copy `gradle.properties.example` to `gradle.properties` and add the permanent backend URL plus public Firebase client metadata. Never commit `google-services.json`, local Gradle properties, keystores, Firebase Admin credentials, FCM server credentials, or private keys.

## License

This repository and application are proprietary. No permission is granted to copy, redistribute, reverse engineer, modify, publish, or use the source or binaries without written authorization from **ad_vibe_dev**.

