# Mandata

Mandata finds the tasks in your Outlook email threads with an AI that runs on your own computer, follows each one
to the email that closes it, and keeps a profile for everyone you work with. For Windows, macOS and Linux.

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

### macOS and Linux

In a terminal:

```bash
curl -fsSL https://shamoug.github.io/mandata/install.sh | bash
```

It installs for you alone, with no admin rights (macOS: `~/Applications/Mandata.app`; Linux: `~/.local/share/mandata`
with a menu entry and the `mandata` command), checks the download against the release's `SHA256SUMS.txt`, and offers
to install Ollama and the AI model. Run it again to update; `--uninstall` removes it. Options: `--no-ai`,
`--model NAME`, `--no-launch`, `--version vX.Y.Z` (after `bash -s --`).

Or download a package from the [latest release](https://github.com/shamoug/mandata/releases/latest):

| System | Files |
| --- | --- |
| macOS, Apple silicon | `Mandata-mac-arm64.dmg` |
| macOS, Intel | `Mandata-mac-x64.dmg` |
| Linux x64 / ARM64 | `Mandata-linux-<arch>.deb`, `.AppImage`, `.tar.gz` |

The Mac app is signed ad hoc, not notarized: opened from the .dmg, macOS asks you to allow it once in System Settings >
Privacy & Security > Open Anyway. The one-line install needs no such step.

On macOS and Linux Mandata reads mail from **Microsoft 365**: click **Sign in** in the app and enter the code it shows
at Microsoft's sign-in page. Needs macOS 12 or later, or a 64-bit Linux desktop.

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

The website (GitHub Pages, served from the `main` branch root), `install.sh` (the macOS and Linux installer, served
from the site) and the finished downloads: the Windows installer zip and the Android app in `downloads/`, and the
macOS and Linux packages on the [releases](https://github.com/shamoug/mandata/releases). The source code is not
published here.
