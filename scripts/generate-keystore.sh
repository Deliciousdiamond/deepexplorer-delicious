#!/usr/bin/env bash
# Generates a local release keystore for DeepExplorer.
# Never commit the generated .jks file.
set -euo pipefail
cd "$(dirname "$0")/.."

KEYSTORE="${1:-keystore/release.jks}"
if [ -f "$KEYSTORE" ]; then
  echo "error: $KEYSTORE already exists; refusing to overwrite" >&2
  exit 1
fi
for tool in keytool; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "error: '$tool' is not on PATH; install a JDK" >&2
    exit 1
  fi
done

PASS="${KEYSTORE_PASS:-}"
if [ -z "$PASS" ]; then
  read -r -s -p "New keystore password: " PASS
  echo
fi
if [ -z "$PASS" ]; then
  echo "error: password must not be empty" >&2
  exit 1
fi

mkdir -p "$(dirname "$KEYSTORE")"
keytool -genkeypair -v \
  -keystore "$KEYSTORE" -storetype PKCS12 \
  -alias release -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$PASS" -keypass "$PASS" \
  -dname "CN=DeepExplorer, OU=Open Source, O=DeepExplorer, L=Unknown, ST=Unknown, C=CN"
chmod 600 "$KEYSTORE" || true
echo "Created: $KEYSTORE"
echo "Now export KEYSTORE and KEYSTORE_PASS before running ./build.sh."
