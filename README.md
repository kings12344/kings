# Tunnel Lite

Tunnel Lite is a lightweight Android profile launcher for OpenVPN-compatible clients. It does not provide VPN servers, mobile data, credentials, or carrier-billing bypasses.

## What it does

- Opens a provider-issued `.ovpn` profile with Android's document picker.
- Copies the profile into the app's private internal storage; it is not placed in shared storage.
- Limits imported profiles to 5 MB and rejects empty files.
- Remembers the imported profile across app restarts.
- Shares the profile only through Android's temporary, read-only `FileProvider` grant.
- Lets the user remove the saved profile at any time.
- Opens the profile in an installed OpenVPN-compatible client.

A working VPN still requires an existing Wi-Fi/mobile-data connection, a valid profile from a provider you trust, and a compatible OpenVPN client.

## Build locally

Install Android Studio with Android SDK 35, then run:

```bash
./gradlew --no-daemon assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Continuous integration

GitHub Actions runs on pushes and pull requests to `main`, plus manual dispatch. It builds the debug APK and uploads it as the `tunnel-lite-debug-apk` artifact. A debug APK is suitable for testing, not public distribution; a release build should use a protected signing key and a separate release workflow.
