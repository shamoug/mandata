#!/usr/bin/env bash
# Mandata installer for macOS and Linux. In a terminal:
#
#   curl -fsSL https://shamoug.github.io/mandata/install.sh | bash
#
# Installs (or updates) Mandata for you alone, with no administrator rights: on a Mac in ~/Applications, on Linux in
# ~/.local/share/mandata with a menu entry. The app comes from the latest GitHub release and is checked against its
# published SHA-256. Then it offers to install the local AI (Ollama and a model of about 2.5 GB). Your data in
# ~/.mandata is never touched by an update.
#
# Options, after "bash -s --":  --no-ai | --ai (no questions) | --model NAME | --no-launch | --version vX.Y.Z | --uninstall
#   curl -fsSL https://shamoug.github.io/mandata/install.sh | bash -s -- --no-ai
#
# Everything is inside main(), run on the last line: a download cut short runs nothing.

main() {
  set -eo pipefail
  local REPO="shamoug/mandata" AI="ask" MODEL="" LAUNCH=1 TAG="" UNINSTALL=0
  while [ $# -gt 0 ]; do
    case "$1" in
      --no-ai) AI=no ;; --ai) AI=yes ;; --model) MODEL="$2"; AI=yes; shift ;; --no-launch) LAUNCH=0 ;;
      --version) TAG="$2"; shift ;; --uninstall) UNINSTALL=1 ;;
      -h|--help) sed -n '2,15p' "$0" 2>/dev/null || true; return 0 ;;
      *) fail "Unknown option $1" ;;
    esac
    shift
  done

  local OS ARCH
  case "$(uname -s)" in Darwin) OS=mac ;; Linux) OS=linux ;; *) fail "Mandata installs on macOS and Linux with this script. On Windows, download the installer zip from https://shamoug.github.io/mandata/download.html" ;; esac
  case "$(uname -m)" in arm64|aarch64) ARCH=arm64 ;; x86_64|amd64) ARCH=x64 ;; *) fail "This computer's processor ($(uname -m)) is not supported." ;; esac
  # A Terminal running under Rosetta on Apple silicon says x86_64: the Mac itself is arm64.
  [ "$OS" = mac ] && [ "$(sysctl -n sysctl.proc_translated 2>/dev/null)" = 1 ] && ARCH=arm64

  # Where things go.
  local DATA="$HOME/.mandata" APP LAUNCHER DESKTOP ICON AUTOSTART SUPPORT
  if [ "$OS" = mac ]; then
    APP="$HOME/Applications/Mandata.app"
    AUTOSTART="$HOME/Library/LaunchAgents/io.github.shamoug.mandata.plist"
    SUPPORT="$HOME/Library/Application Support/Mandata"
  else
    APP="${XDG_DATA_HOME:-$HOME/.local/share}/mandata"
    LAUNCHER="$HOME/.local/bin/mandata"
    DESKTOP="${XDG_DATA_HOME:-$HOME/.local/share}/applications/mandata.desktop"
    ICON="${XDG_DATA_HOME:-$HOME/.local/share}/icons/hicolor/512x512/apps/mandata.png"
    AUTOSTART="${XDG_CONFIG_HOME:-$HOME/.config}/autostart/mandata.desktop"
    SUPPORT="${XDG_CONFIG_HOME:-$HOME/.config}/Mandata"
  fi

  if [ "$UNINSTALL" = 1 ]; then uninstall; return; fi

  heading "Mandata setup ($([ "$OS" = mac ] && echo macOS || echo Linux), $ARCH)"
  need curl; need tar
  if [ "$OS" = mac ]; then need ditto; local v; v="$(sw_vers -productVersion)"; [ "${v%%.*}" -ge 12 ] || fail "Mandata needs macOS 12 (Monterey) or later; this Mac has $v."; fi

  # ---------- the app ----------
  local BASE="https://github.com/$REPO/releases/${TAG:+download/$TAG}"; [ -n "$TAG" ] || BASE="https://github.com/$REPO/releases/latest/download"
  local ASSET; [ "$OS" = mac ] && ASSET="Mandata-mac-$ARCH.zip" || ASSET="Mandata-linux-$ARCH.tar.gz"
  TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
  step "Downloading $ASSET"
  curl -fL --retry 3 --progress-bar -o "$TMP/$ASSET" "$BASE/$ASSET" || fail "Could not download $BASE/$ASSET. Check the internet connection and run this again."
  curl -fsSL --retry 3 -o "$TMP/SHA256SUMS.txt" "$BASE/SHA256SUMS.txt" || fail "Could not download the release's SHA256SUMS.txt."
  local want got
  want="$(grep " $ASSET\$" "$TMP/SHA256SUMS.txt" | cut -d' ' -f1)"
  got="$(sha256 "$TMP/$ASSET")"
  [ -n "$want" ] && [ "$want" = "$got" ] || fail "The download does not match its published SHA-256: nothing was installed. Run this again."
  ok "Checked against its published SHA-256"

  quit_app
  if [ "$OS" = mac ]; then
    mkdir -p "$HOME/Applications"
    rm -rf "$TMP/x"; mkdir "$TMP/x"; ditto -x -k "$TMP/$ASSET" "$TMP/x"
    rm -rf "$APP"; mv "$TMP/x/Mandata.app" "$APP"
    # Downloaded by curl it carries no quarantine mark; cleared anyway, in case a copy came another way.
    xattr -dr com.apple.quarantine "$APP" 2>/dev/null || true
    ok "Installed $APP"
  else
    mkdir -p "$APP" "$(dirname "$LAUNCHER")" "$(dirname "$DESKTOP")" "$(dirname "$ICON")"
    rm -rf "$APP/Mandata.new"; mkdir "$APP/Mandata.new"
    tar -xzf "$TMP/$ASSET" -C "$APP/Mandata.new" --strip-components=1
    rm -rf "$APP/Mandata"; mv "$APP/Mandata.new" "$APP/Mandata"
    ln -sf "$APP/Mandata/mandata" "$LAUNCHER"
    install -m 644 "$APP/Mandata/mandata.png" "$ICON"
    printf '[Desktop Entry]\nType=Application\nName=Mandata\nGenericName=Email task tracker\nComment=Tasks found in your email, followed to the end\nExec="%s"\nIcon=mandata\nTerminal=false\nCategories=Office;Email;ProjectManagement;\nStartupWMClass=Mandata\n' "$APP/Mandata/mandata" > "$DESKTOP"
    command -v update-desktop-database >/dev/null && update-desktop-database -q "$(dirname "$DESKTOP")" 2>/dev/null || true
    ok "Installed $APP/Mandata, with a menu entry and the command mandata"
    case ":$PATH:" in *":$HOME/.local/bin:"*) ;; *) info "Add ~/.local/bin to your PATH to start it by typing mandata." ;; esac
    if ! ldconfig -p 2>/dev/null | grep -q 'libnss3\.so'; then
      info "If Mandata does not open, install the libraries it uses:"
      info "  Debian/Ubuntu: sudo apt install libgtk-3-0 libnss3 libasound2 libgbm1 libxss1 libnotify4"
      info "  Fedora:        sudo dnf install gtk3 nss alsa-lib mesa-libgbm libXScrnSaver libnotify"
    fi
  fi

  # ---------- the local AI ----------
  setup_ai || warn "The AI was not set up. Mandata opens without it; run this again to finish, or see https://ollama.com."

  heading "Done"
  info "Mandata reads your Outlook mail through Microsoft 365: click \"Sign in\" at the top of the app the first time."
  info "Run this command again at any time to update Mandata. Your data in ~/.mandata stays."
  if [ "$LAUNCH" = 1 ]; then
    if [ "$OS" = mac ]; then open "$APP"; else nohup "$APP/Mandata/mandata" >/dev/null 2>&1 & disown; fi
    ok "Mandata is starting"
  fi
}

# ---------- the local AI: Ollama and the model that suits this computer ----------
setup_ai() {
  [ "$AI" = no ] && { info "AI skipped (--no-ai)."; return 0; }
  local exe; exe="$(ollama_exe)"
  [ -z "$MODEL" ] && MODEL="$(best_model)"
  if [ -n "$exe" ] && "$exe" list 2>/dev/null | awk 'NR>1 {print $1}' | grep -qx -e "$MODEL" -e "$MODEL:latest"; then
    ok "The local AI is installed, with $MODEL"; remember_model; return 0
  fi
  heading "Local AI"
  info "Mandata reads mail with an AI that runs on this computer: Ollama, plus the model $MODEL (about 2.5 GB)."
  [ -z "$exe" ] && [ "$OS" = linux ] && info "Ollama's own installer asks for your password (sudo) to install it for this computer."
  if [ "$AI" = ask ] && ! yes_no "Install it now?" y; then info "Skipped. Mandata asks for the AI when it opens; run this again to install it."; return 0; fi

  if [ -z "$exe" ]; then
    step "Installing Ollama"
    if [ "$OS" = mac ]; then
      local dest=/Applications; [ -w "$dest" ] || dest="$HOME/Applications"
      curl -fL --retry 3 --progress-bar -o "$TMP/Ollama.zip" https://ollama.com/download/Ollama-darwin.zip || return 1
      rm -rf "$dest/Ollama.app"; ditto -x -k "$TMP/Ollama.zip" "$dest"
      xattr -dr com.apple.quarantine "$dest/Ollama.app" 2>/dev/null || true
    else
      curl -fsSL https://ollama.com/install.sh | sh || return 1
    fi
    exe="$(ollama_exe)"; [ -n "$exe" ] || return 1
    ok "Ollama installed"
  fi
  # The server runs while the model downloads. On Linux, Ollama's installer starts it as a service.
  if ! curl -fsS -m 3 http://127.0.0.1:11434/api/version >/dev/null 2>&1; then
    nohup "$exe" serve >/dev/null 2>&1 &
    for _ in $(seq 1 30); do curl -fsS -m 2 http://127.0.0.1:11434/api/version >/dev/null 2>&1 && break; sleep 1; done
  fi
  step "Downloading the model $MODEL (several minutes)"
  "$exe" pull "$MODEL" || return 1
  remember_model
  ok "The local AI is ready: $MODEL"
}
ollama_exe() {
  local p
  for p in "$(command -v ollama 2>/dev/null)" /Applications/Ollama.app/Contents/Resources/ollama "$HOME/Applications/Ollama.app/Contents/Resources/ollama" \
    /opt/homebrew/bin/ollama /usr/local/bin/ollama /usr/bin/ollama; do
    [ -n "$p" ] && [ -x "$p" ] && { echo "$p"; return; }
  done
}
# The same rule as setup on Windows (lib/models.json): the model on the graphics card when it fits (4 GB), else the
# one that runs best on the processor. Apple silicon's graphics share the Mac's memory.
best_model() {
  local vram=0
  if [ "$OS" = mac ]; then
    [ "$ARCH" = arm64 ] && vram=$(( $(sysctl -n hw.memsize) / 1073741824 * 2 / 3 ))
  elif command -v nvidia-smi >/dev/null; then
    local mb; mb="$(nvidia-smi --query-gpu=memory.total --format=csv,noheader,nounits 2>/dev/null | sort -n | tail -n1)"; mb="${mb//[^0-9]/}"
    vram=$(( ${mb:-0} / 1024 ))
  fi
  [ "$vram" -ge 4 ] && echo "qwen3:4b-instruct" || echo "phi4-mini"
}
# The model setup chose: Mandata starts with it until another is picked in Settings > AI.
remember_model() { mkdir -p "$DATA"; printf '{"model":"%s"}\n' "$MODEL" > "$DATA/install-defaults.json"; }

# ---------- uninstall ----------
uninstall() {
  heading "Uninstalling Mandata"
  quit_app
  rm -rf "$APP" "$AUTOSTART" "$SUPPORT"
  [ "$OS" = linux ] && rm -f "$LAUNCHER" "$DESKTOP" "$ICON"
  [ "$OS" = mac ] && launchctl remove io.github.shamoug.mandata 2>/dev/null || true
  ok "Mandata removed, with its menu entry and start at login"
  if [ -d "$DATA" ] && yes_no "Also delete your Mandata data in $DATA (tasks, people, backups)?" n; then rm -rf "$DATA"; ok "Data deleted"; else info "Your data stays in $DATA."; fi
  info "Ollama and its models stay: remove them as https://ollama.com explains, if nothing else uses them."
}

# ---------- helpers ----------
quit_app() {
  if [ "$OS" = mac ]; then
    pgrep -f "Mandata.app/Contents/MacOS/Mandata" >/dev/null 2>&1 || return 0
    osascript -e 'quit app "Mandata"' >/dev/null 2>&1 || true
    for _ in $(seq 1 20); do pgrep -f "Mandata.app/Contents/MacOS/Mandata" >/dev/null 2>&1 || return 0; sleep 0.5; done
    pkill -f "Mandata.app/Contents/MacOS/Mandata" 2>/dev/null || true
  else
    pgrep -f "mandata/Mandata/mandata-electron" >/dev/null 2>&1 || return 0
    pkill -f "mandata/Mandata/mandata-electron" 2>/dev/null || true
    for _ in $(seq 1 20); do pgrep -f "mandata/Mandata/mandata-electron" >/dev/null 2>&1 || return 0; sleep 0.5; done
  fi
  sleep 1
}
sha256() { if command -v sha256sum >/dev/null; then sha256sum "$1" | cut -d' ' -f1; else shasum -a 256 "$1" | cut -d' ' -f1; fi; }
need() { command -v "$1" >/dev/null || fail "This needs $1. Install it and run this again."; }
# Asked at the terminal even when this script comes through a pipe; with no terminal, the default answer.
yes_no() {
  local a d="$2"
  if [ -r /dev/tty ] && { true </dev/tty; } 2>/dev/null; then
    printf '  %s [%s] ' "$1" "$([ "$d" = y ] && echo Y/n || echo y/N)" >/dev/tty; read -r a </dev/tty || a=""
  fi
  a="${a:-$d}"; [ "$a" = y ] || [ "$a" = Y ] || [ "$a" = yes ]
}
if [ -t 1 ]; then B=$'\033[1m'; G=$'\033[32m'; Y=$'\033[33m'; R=$'\033[31m'; N=$'\033[0m'; else B= G= Y= R= N=; fi
heading() { printf '\n%s%s%s\n' "$B" "$*" "$N"; }
step() { printf '  %s...\n' "$*"; }
ok()   { printf '  %s✓%s %s\n' "$G" "$N" "$*"; }
info() { printf '    %s\n' "$*"; }
warn() { printf '  %s!%s %s\n' "$Y" "$N" "$*"; }
fail() { printf '\n  %sSETUP FAILED:%s %s\n' "$R" "$N" "$*" >&2; exit 1; }

main "$@"
