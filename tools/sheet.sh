#!/usr/bin/env bash
# Build, install, render the art sheet on the phone, pull it and cut it into pieces.
# Usage: tools/sheet.sh [character-id]
set -e
cd "$(dirname "$0")/.."
export MSYS_NO_PATHCONV=1 TEMP=D:/tmp TMP=D:/tmp JAVA_TOOL_OPTIONS="-Djdk.net.unixdomain.tmpdir=D:/tmp -Djava.io.tmpdir=D:/tmp"
id=${1:-naruto}
./gradlew -q :app:assembleDebug --console=plain 2>&1 | grep -E "^e:|error:" || true
adb install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null
adb shell am start --activity-clear-task -n com.zainkhalid.animebattery/.MainActivity --es cmd sheet >/dev/null 2>&1
sleep 3
mkdir -p spike/out
adb pull /sdcard/Android/data/com.zainkhalid.animebattery/files/sheets/$id.png spike/out/sheet_$id.png >/dev/null
python tools/cut_sheet.py spike/out/sheet_$id.png
