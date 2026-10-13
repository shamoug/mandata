# Mandata

Mandata finds the tasks in your Outlook email threads with an AI that runs on your own laptop, follows each one
to the email that closes it, and keeps a profile for everyone you work with. Email never leaves the computer.

**Website:** https://shamoug.github.io/mandata/
**Download:** [Mandata-Installer.zip](https://shamoug.github.io/mandata/downloads/Mandata-Installer.zip) (also attached to each [release](https://github.com/shamoug/mandata/releases))

## Install

1. Download the zip. Right-click it, **Properties**, tick **Unblock**, OK.
2. Extract it and open the folder it makes.
3. Double-click `install.bat`.

It installs for the current user in `%LOCALAPPDATA%\Programs\Mandata`, with no admin rights and no setup program.
It downloads the app runtime (Electron, checked against its SHA-256) and, after asking, the Ollama runtime and the
AI model that fits the laptop. Run it again to update; your data in `%USERPROFILE%\.mandata` is kept.

Needs Windows 10 or 11 (64-bit), classic Outlook signed in, and PowerShell scripts allowed.

## This repository

This repository holds the website (GitHub Pages, served from the `main` branch root) and the installer zip in
`downloads/`.
