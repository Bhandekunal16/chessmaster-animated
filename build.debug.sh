#!/usr/bin/env sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

if [ ! -f "$PROJECT_DIR/debug.keystore" ]; then
  command -v keytool >/dev/null 2>&1 || {
    echo "keytool is required to create the local debug keystore." >&2
    exit 1
  }
  keytool -genkeypair -noprompt \
    -keystore "$PROJECT_DIR/debug.keystore" \
    -storepass android \
    -alias androiddebugkey \
    -keypass android \
    -dname "CN=Android Debug,O=Android,C=US" \
    -validity 10000 \
    -keyalg RSA \
    -keysize 2048
fi

exec sh "$PROJECT_DIR/gradlew" :app:assembleDebug "$@"