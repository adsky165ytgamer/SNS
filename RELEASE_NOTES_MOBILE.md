# NoticeFlow Sender v1.1.0 Alpha — Mobile-first overhaul

This release changes only the Sender Android application. The Receiver app, backend routes, authentication contract, and Android TV/panel layouts are not changed in this pass.

## Included

The Sender now uses bottom navigation for Home, Notices, Receivers, and Settings. Home has a prominent Create Notice action and live status summary. Create Notice is a focused mobile flow with notice type selection, title, description, recipient selection, preview, and send confirmation. Receivers uses searchable, selectable cards instead of a cramped dropdown. Notices contains chronological delivery history and detail views. Settings contains account controls, connection state, notification information, diagnostics, About, and proprietary license information.

The build is package `app.sender`, version `1.1.0-alpha`, version code `3`. The APK is intended for phone testing and is not an Android TV/panel build. It uses the original `school-notics` Firebase base and the existing authenticated backend contract.

## License

NoticeFlow is proprietary software created by **ad_vibe_dev**. It is not open source. Redistribution, reverse engineering, modification, publication, or reuse of the source or APK requires written authorization.
