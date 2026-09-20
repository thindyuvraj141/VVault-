#!/usr/bin/env bash
# Adds fingerprint / face unlock support to the Capacitor app.
# Safe to run many times. Run it in your build BEFORE `npx cap sync` / gradle build.
set -e

# 1. install the native biometric plugin
npm install @aparajita/capacitor-biometric-auth --save

# 2. Android permission
MANIFEST="android/app/src/main/AndroidManifest.xml"
if [ -f "$MANIFEST" ] && ! grep -q "USE_BIOMETRIC" "$MANIFEST"; then
  sed -i 's#<application#<uses-permission android:name="android.permission.USE_BIOMETRIC" />\n    <application#' "$MANIFEST"
  echo "Added USE_BIOMETRIC to AndroidManifest.xml"
fi

# 3. iOS Face ID text (only if an ios project exists)
PLIST="ios/App/App/Info.plist"
if [ -f "$PLIST" ] && ! grep -q "NSFaceIDUsageDescription" "$PLIST"; then
  python3 - "$PLIST" <<'PY'
import sys
p = sys.argv[1]
s = open(p, encoding="utf-8").read()
i = s.rindex("</dict>")
s = s[:i] + "\t<key>NSFaceIDUsageDescription</key>\n\t<string>Unlock V Vault with Face ID</string>\n" + s[i:]
open(p, "w", encoding="utf-8").write(s)
PY
  echo "Added NSFaceIDUsageDescription to Info.plist"
fi

# 4. copy the plugin into the native projects
npx cap sync

