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

## This repository

The website (GitHub Pages, served from the `main` branch root), `install.sh` (the macOS and Linux installer, served
from the site), the Windows installer zip in `downloads/`, and the workflow that builds the macOS and Linux packages.

### Releasing

1. In the app's folder, run `installer\pack.bat`. It makes `Mandata-Installer-<version>.zip` (Windows) and
   `Mandata-app.tar.gz` (the app files with the macOS and Linux build scripts).
2. Copy the zip to `downloads/Mandata-Installer.zip` and `installer/unix/install.sh` to `install.sh`; update the
   version on the pages; commit and push.
3. Make a release with both files: `gh release create vX.Y.Z Mandata-Installer.zip Mandata-app.tar.gz`.
4. [.github/workflows/packages.yml](.github/workflows/packages.yml) then builds the macOS and Linux packages on GitHub's
   Macs and Linux machines, starts each one to check it runs, attaches them and `SHA256SUMS.txt` to the release, and
   tests `install.sh` against it. Run it again from the Actions tab with the tag if needed.
