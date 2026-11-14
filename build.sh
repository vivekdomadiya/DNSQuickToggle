#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

echo "=== Building DNS Quick Toggle ==="
./gradlew assembleDebug

APK=$(find "$DIR/app/build/outputs/apk/debug" -name "*.apk" | head -n 1)
if [ -f "$APK" ]; then
    echo "=================================================="
    echo " BUILD SUCCESSFUL!"
    echo " APK generated at:"
    echo "   $APK"
    echo " Size: $(du -h "$APK" | cut -f1)"
    echo "=================================================="
fi
