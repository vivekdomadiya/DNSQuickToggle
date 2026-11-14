#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

echo "=== DNS Quick Toggle Installer ==="

# Check for APK
APK_PATH=$(find "$DIR/app/build/outputs/apk/debug" -name "*.apk" 2>/dev/null | head -n 1)
if [ -z "$APK_PATH" ] || [ ! -f "$APK_PATH" ]; then
    echo "[*] Building APK with Gradle..."
    ./gradlew assembleDebug
    APK_PATH=$(find "$DIR/app/build/outputs/apk/debug" -name "*.apk" 2>/dev/null | head -n 1)
fi

if [ -n "$APK_PATH" ] && [ -f "$APK_PATH" ]; then
    echo "[+] APK built successfully: $APK_PATH"
else
    echo "[-] Error: APK build failed."
    exit 1
fi

PACKAGE_NAME="com.antigravity.dnsquicktoggle"

# Check if /sdcard/Download exists (Android local storage)
if [ -d "/sdcard/Download" ] || [ -d "/storage/emulated/0/Download" ]; then
    DEST_DIR="/sdcard/Download"
    [ ! -d "$DEST_DIR" ] && DEST_DIR="/storage/emulated/0/Download"
    APK_BASENAME=$(basename "$APK_PATH")
    cp "$APK_PATH" "$DEST_DIR/$APK_BASENAME"
    cp "$APK_PATH" "$DEST_DIR/DNSQuickToggle.apk"
    echo "[+] Copied APK to $DEST_DIR/$APK_BASENAME and $DEST_DIR/DNSQuickToggle.apk"
    echo "    You can open your Downloads folder and tap $APK_BASENAME to install!"
fi

# Try ADB if available
if command -v adb >/dev/null 2>&1; then
    DEVICES=$(adb devices 2>/dev/null | grep -v "List" | grep "device$" || true)
    if [ -n "$DEVICES" ]; then
        echo "[*] Device detected via ADB. Installing..."
        adb install -r "$APK_PATH"
        echo "[*] Granting WRITE_SECURE_SETTINGS permission via ADB..."
        adb shell pm grant "$PACKAGE_NAME" android.permission.WRITE_SECURE_SETTINGS
        echo "[+] Permission granted! You are ready to toggle Private DNS."
        exit 0
    fi
fi

# Try root if available
if command -v su >/dev/null 2>&1; then
    echo "[*] Trying to install and grant permission using root (su)..."
    if su -c "pm install -r $APK_PATH" 2>/dev/null; then
        su -c "pm grant $PACKAGE_NAME android.permission.WRITE_SECURE_SETTINGS" 2>/dev/null
        echo "[+] Successfully installed and granted permissions with root!"
        exit 0
    fi
fi

echo ""
echo "=== Manual Setup Instructions ==="
echo "1. Install the APK located at:"
echo "   $APK_PATH"
echo "   (or /sdcard/Download/DNSQuickToggle.apk)"
echo ""
echo "2. Grant WRITE_SECURE_SETTINGS permission using ADB on your computer:"
echo "   adb shell pm grant $PACKAGE_NAME android.permission.WRITE_SECURE_SETTINGS"
echo "   (Or tap 'Grant with Root' directly in the app if your device is rooted)"
echo ""
echo "3. Pull down your notification shade twice, tap edit, and add the 'Private DNS' tile!"
