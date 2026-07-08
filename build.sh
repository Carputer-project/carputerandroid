#!/bin/bash
set -e

cd "$(dirname "$0")"

echo "=== CarputerAndroid Build Script ==="
echo ""

# Check prerequisites
check_cmd() {
    if ! command -v "$1" &>/dev/null; then
        echo "ERROR: $1 not found. Please install it."
        case "$1" in
            java)    echo "  Install: sudo apt install openjdk-17-jdk" ;;
            gradle)  echo "  Use ./gradlew instead of 'gradle'" ;;
        esac
        return 1
    fi
}

echo "Checking prerequisites..."
check_cmd java || exit 1

if [ ! -f gradlew ]; then
    echo "ERROR: gradlew not found. Generate it with: gradle wrapper"
    exit 1
fi

# Check ANDROID_HOME
if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
    echo "ERROR: ANDROID_HOME or ANDROID_SDK_ROOT must be set."
    echo "  Example: export ANDROID_HOME=\$HOME/Android/Sdk"
    exit 1
fi
echo "ANDROID_HOME: ${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
echo ""

# Make gradlew executable
chmod +x gradlew

# Build
echo "Building APK..."
./gradlew assembleDebug

echo ""
echo "=== Build Complete ==="
APK="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK" ]; then
    echo "APK: $APK"
    ls -lh "$APK"
else
    echo "APK not found at $APK"
    echo "Check app/build/outputs/apk/ for the built APK."
fi
