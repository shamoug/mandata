# Mandata

Mandata finds the tasks in your Outlook email threads with an AI that runs on your own computer, follows each one
to the email that closes it, and keeps a profile for everyone you work with. For Windows.

**Website:** https://shamoug.github.io/mandata/ · **Download:** https://shamoug.github.io/mandata/download.html ·
every file is also attached to each [release](https://github.com/shamoug/mandata/releases).

## Install

### Windows

1. Download [Mandata-Installer.zip](https://shamoug.github.io/mandata/downloads/Mandata-Installer.zip). Right-click it, **Properties**, tick **Unblock**, OK.
2. Extract it and open the folder it makes.
3. Double-click `install.bat`.

It installs for the current user in `%LOCALAPPDATA%\Programs\Mandata`, with no admin rights and no setup program.
It downloads the app runtime (Electron, checked against its SHA-256) and, after asking, the Ollama runtime and the
AI model that fits the laptop. Run it again to update; your data in `%USERPROFILE%\.mandata` is kept.
Needs Windows 10 or 11 (64-bit), classic Outlook signed in (or Microsoft 365, chosen in Settings > Mail), and
PowerShell scripts allowed.

### iPhone and Android

The phone reaches Mandata on your computer over the Wi-Fi once phone access is on in Mandata's **Settings > Phone**;
scan the QR code shown there (pick iPhone or Android above it). Your mail and the AI stay on the computer.

- **Android:** the code opens `open.html` on this site, which Android hands to the Mandata app when it is installed (an
  App Link, verified by `assetlinks.json` in the [shamoug.github.io](https://github.com/shamoug/shamoug.github.io)
  repository). Without the app, the page sends the phone to Mandata's phone version in Chrome, paired, where **Install
  the app** downloads [Mandata-Android.apk](https://shamoug.github.io/mandata/downloads/Mandata-Android.apk).
- **iPhone:** the code opens Mandata in Safari, paired; then Share > **Add to Home Screen**.
- [phone.html](https://shamoug.github.io/mandata/phone.html) explains it and has the Android download.

## This repository

The website (GitHub Pages, served from the `main` branch root) and the finished downloads: the Windows installer zip
and the Android app in `downloads/`, also attached to each [release](https://github.com/shamoug/mandata/releases). The
source code is not published here.
