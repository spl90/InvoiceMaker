#!/bin/bash
# deploy.sh — commit, push, wait for CI build, install APK on phone

set -e

# ── paths ──────────────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
DOWNLOAD_DIR="$SCRIPT_DIR/.ci_download"

# ── java (needed if you ever run gradle locally in same shell) ─────────────────
export JAVA_HOME="$(/usr/libexec/java_home 2>/dev/null || echo /opt/homebrew/opt/openjdk@17)"
export PATH="$JAVA_HOME/bin:$PATH"

cd "$SCRIPT_DIR"

# ── commit message ─────────────────────────────────────────────────────────────
if [ -n "$1" ]; then
  MSG="$1"
else
  printf "Commit message (or press Enter for 'Update app'): "
  read -r MSG
  MSG="${MSG:-Update app}"
fi

# ── check for changes ──────────────────────────────────────────────────────────
if git diff --quiet && git diff --cached --quiet && [ -z "$(git ls-files --others --exclude-standard)" ]; then
  echo "No changes to commit — pushing existing commits and rebuilding."
else
  git add .
  git commit -m "$MSG"
fi

# ── push ───────────────────────────────────────────────────────────────────────
echo "Pushing to GitHub..."
git push

# ── wait for CI ────────────────────────────────────────────────────────────────
echo "Waiting for GitHub Actions build to finish..."
RUN_ID=$(gh run list --branch main --limit 1 --json databaseId -q '.[0].databaseId')
gh run watch "$RUN_ID" --exit-status

# ── download APK ───────────────────────────────────────────────────────────────
echo "Downloading APK..."
rm -rf "$DOWNLOAD_DIR"
mkdir -p "$DOWNLOAD_DIR"
gh run download "$RUN_ID" --name app-debug --dir "$DOWNLOAD_DIR"

APK=$(find "$DOWNLOAD_DIR" -name "*.apk" | head -1)
if [ -z "$APK" ]; then
  echo "ERROR: APK not found in download."
  exit 1
fi

# ── install on phone ───────────────────────────────────────────────────────────
echo "Looking for connected device..."
DEVICE=$("$ADB" devices | awk '/\tdevice$/{print $1; exit}')

if [ -z "$DEVICE" ]; then
  echo ""
  echo "No device found via USB. Options:"
  echo "  1) Plug in phone with USB debugging on, then re-run ./deploy.sh"
  echo "  2) Or manually install: $APK"
  exit 0
fi

echo "Installing on $DEVICE..."
"$ADB" -s "$DEVICE" install -r "$APK"
"$ADB" -s "$DEVICE" shell am start -n com.example.invoicegen/.MainActivity

echo ""
echo "Done! App updated and launched on your phone."
