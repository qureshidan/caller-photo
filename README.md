# Caller Photo

Full-screen contact photos over the existing Xiaomi call interface. Swipe up to hide the photo and return to the normal call controls. The photo contains only a faded upward arrow; swiping does not answer or end a call.

**Experimental Android app, tested on Redmi Note 13 5G with Android 15 / HyperOS 3.** It keeps the Xiaomi dialer as the default Phone app. Other manufacturers and dialers are not supported by the current implementation.

## Features

- Contact photos for detected cellular calls, including incoming calls while another app is open.
- Existing contact photos or a separate portrait chosen inside Caller Photo.
- Normal call interface for contacts without a photo.
- Photo disappears after a swipe or when the call ends.
- Local processing without an Internet permission.

## Install and set up

Build the APK using the instructions below, then install `app/build/outputs/apk/debug/app-debug.apk`.

1. Open Caller Photo and allow Contacts and Phone access.
2. Enable Caller Photo under Accessibility → Downloaded apps. Android may require App info → Allow restricted settings first for a sideloaded app.
3. Enable Detection over other apps (Notification access). HyperOS hides its floating call banner's caller details from Accessibility.
4. Enable Background autostart for Caller Photo and set its battery mode to No restrictions.
5. Use an existing contact photo, or choose Choose a contact → set a photo. This does not change the system contact photo.
6. Preview the photo, swipe up, and test an incoming call while another app is open.

The Keep working in the background button provides shortcuts and guidance. If Xiaomi's cleaner stops it, toggle Caller Photo's Accessibility service off and back on. If necessary, lock the app in recent apps to exclude it from cleanup. An explicit force stop will still stop it.

## Permissions and privacy

| Access | Purpose |
| --- | --- |
| Contacts | Match a caller to their name and saved photo. |
| Phone state | Detect a cellular call and remove the overlay when it ends. |
| Accessibility | Read known caller fields in Xiaomi's call UI and display the overlay. |
| Notification access | Read the caller from Xiaomi call notifications while a floating banner appears. |

Android grants Notification access system-wide. The code filters for call-category notifications from `com.android.incallui` and `com.android.contacts` before reading their contents. Active call notifications stay in memory; notification history is not stored on disk.

Selected photos stay in private app storage. There is no Internet, microphone, call-log, call-placement or default-dialer permission. The app never sends answer/reject notification actions; after dismissal it may open the existing notification's content intent to show the call screen. Uninstalling deletes photos stored by the app.

## Compatibility and limits

- Confirmed on the test device: incoming photo display over another app, arrow-only preview, swipe dismissal and restored incoming-call operation after background settings were corrected.
- Outgoing calls, locked-screen behaviour and opening the stock call screen after swiping need broader device testing.
- The overlay appears once per continuous call session. Call waiting and conference calls do not receive a second photo.
- Ambiguous duplicate contact names are skipped. No matching photo means the stock call interface stays visible.
- Xiaomi cleanup and future updates can interrupt services. Version 0.5 adds notification-service reconnection and recovery guidance.
- This is an overlay, not a replacement dialer or a photo inserted behind Xiaomi's buttons.

## Build

Requirements: Java 21, Android SDK platform 36.1 and Android build tools. The wrapper uses Gradle 9.4.1 and Android Gradle Plugin 9.2.1.

Set `sdk.dir` in an untracked `local.properties`, or configure your Android SDK environment. Run:

```sh
./gradlew :app:assembleDebug :app:lintDebug
```

Windows:

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug
```

The APK appears in `app/build/outputs/apk/debug/`. Debug builds are for testing. Another computer may use a different signing key and may not update an existing installation directly. Signing keys, local SDK paths, APKs, contact data and diagnostic captures are excluded from this repository.

## Feedback

Open an [issue](https://github.com/qureshidan/caller-photo/issues/new/choose) for a bug, compatibility result or suggestion. Include your phone model, Android / HyperOS version, app version and reproduction steps. Remove names, phone numbers and private photos from screenshots and logs.

See [CONTRIBUTING.md](CONTRIBUTING.md) for testing guidance. The current baseline is v0.5. Build and Android lint checks passed, and incoming-call operation was confirmed on the test device.
