#!/usr/bin/env bash
# DeepExplorer APK build (no Gradle).
# Requires: JDK, Android build-tools (aapt, d8, zipalign, apksigner), zip,
#           an android.jar for API 29, and a local signing keystore.
#
# Required environment:
#   KEYSTORE_PASS   keystore password
# Optional environment:
#   ANDROID_JAR     path to android.jar (default: $ANDROID_HOME/platforms/android-29/android.jar)
#   KEYSTORE        path to keystore (default: keystore/release.jks)
#   KEY_ALIAS       key alias (default: release)
#   KEY_PASS        key password (default: same as KEYSTORE_PASS)
#   OUT             output APK path (default: build/DeepExplorer.apk)
#   COPY_TO         copy the APK to this path after signing (optional)
set -euo pipefail
cd "$(dirname "$0")"

ANDROID_JAR="${ANDROID_JAR:-${ANDROID_HOME:-$HOME/Android/Sdk}/platforms/android-29/android.jar}"
KEYSTORE="${KEYSTORE:-keystore/release.jks}"
KEY_ALIAS="${KEY_ALIAS:-release}"
OUT="${OUT:-build/DeepExplorer.apk}"
KEY_PASS="${KEY_PASS:-${KEYSTORE_PASS:-}}"

if [ -z "${KEYSTORE_PASS:-}" ]; then
  echo "error: set KEYSTORE_PASS to the keystore password" >&2
  echo "       see BUILDING.md for keystore setup" >&2
  exit 1
fi
if [ ! -f "$ANDROID_JAR" ]; then
  echo "error: android.jar not found: $ANDROID_JAR" >&2
  echo "       set ANDROID_JAR to <sdk>/platforms/android-29/android.jar" >&2
  exit 1
fi
if [ ! -f "$KEYSTORE" ]; then
  echo "error: keystore not found: $KEYSTORE" >&2
  echo "       run scripts/generate-keystore.sh and set KEYSTORE/KEYSTORE_PASS" >&2
  exit 1
fi
for tool in aapt javac d8 zipalign apksigner zip; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "error: '$tool' is not on PATH; install Android build-tools/JDK" >&2
    exit 1
  fi
done

echo "cleaning build/"
rm -rf build
mkdir -p build/gen build/classes build/dex "$(dirname "$OUT")"

echo "[1/6] resources"
aapt package -f -m -J build/gen \
  -M app/AndroidManifest.xml \
  -S app/res -A app/assets -I "$ANDROID_JAR" >/dev/null

echo "[2/6] javac"
javac -source 8 -target 8 -encoding UTF-8 \
  -bootclasspath "$ANDROID_JAR" -classpath "$ANDROID_JAR" \
  -d build/classes $(find app/src build/gen -name '*.java')

echo "[3/6] dex"
d8 --lib "$ANDROID_JAR" --min-api 23 --output build/dex \
  $(find build/classes -name '*.class') >/dev/null

echo "[4/6] package"
aapt package -f -M app/AndroidManifest.xml \
  -S app/res -A app/assets -I "$ANDROID_JAR" -F build/app.unsigned.apk >/dev/null
cp build/dex/classes.dex build/classes.dex
(cd build && zip -q -u app.unsigned.apk classes.dex)

echo "[5/6] align + sign"
zipalign -f -p 4 build/app.unsigned.apk build/app.aligned.apk
apksigner sign --ks "$KEYSTORE" --ks-key-alias "$KEY_ALIAS" \
  --ks-pass "pass:$KEYSTORE_PASS" --key-pass "pass:$KEY_PASS" \
  --out "$OUT" build/app.aligned.apk
apksigner verify --verbose "$OUT" >/dev/null

echo "[6/6] done"
if [ -n "${COPY_TO:-}" ]; then
  mkdir -p "$(dirname "$COPY_TO")"
  cp "$OUT" "$COPY_TO"
  ls -lh "$OUT" "$COPY_TO"
else
  ls -lh "$OUT"
fi
echo "OK: $OUT"
