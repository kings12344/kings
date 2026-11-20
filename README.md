# Tunnel Lite

A lightweight Android OpenVPN profile launcher.

## What it does
- Imports a provider-issued `.ovpn` profile using Android's document picker.
- Stores the selected profile in the app's private cache.
- Hands it to an installed compatible OpenVPN client using Android's secure content-sharing API.

## Important limitation
This app does **not** create mobile data, bypass carrier billing, provide VPN servers, or contain VPN credentials. A working VPN requires an existing Wi-Fi/mobile-data connection, a valid profile from a VPN provider, and a compatible OpenVPN client installed on the phone.

## Build
GitHub Actions builds a debug APK on pushes to `main` and manual runs. Download the `tunnel-lite-debug-apk` artifact from the Actions run.
