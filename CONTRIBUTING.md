# Feedback and testing

Bug reports, compatibility results and feature suggestions are welcome through GitHub Issues. Search existing issues first.

Include your phone model, Android / HyperOS version, app version, default dialer, permission status and reproduction steps. Specify incoming or outgoing calls, locked or unlocked phone, and whether another app was open.

Test with a contact who has a photo. Check swipe dismissal, call-end cleanup and a contact without a photo. Never use an emergency number for testing. Remove private caller information from screenshots and logs.

For code changes, discuss substantial changes in an issue first. Keep changes focused and run `./gradlew :app:assembleDebug :app:lintDebug`. Describe device testing and any behaviour that remains unverified.
