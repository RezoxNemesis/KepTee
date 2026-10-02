#!/usr/bin/env bash
set -euo pipefail

mkdir -p test-results
collect_diagnostics() {
  adb logcat -d -v threadtime > test-results/logcat.txt 2>&1 || true
  adb shell dumpsys activity activities > test-results/activities.txt 2>&1 || true
  adb shell screencap -p /sdcard/keptee-ui.png >/dev/null 2>&1 || true
  adb pull /sdcard/keptee-ui.png test-results/final-screen.png >/dev/null 2>&1 || true
}
trap collect_diagnostics EXIT

adb wait-for-device
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
export MAESTRO_CLI_NO_ANALYTICS=1
export MAESTRO_DRIVER_STARTUP_TIMEOUT=120000
maestro --version
maestro test --format junit --output test-results/maestro-junit.xml .maestro/flows/core-ui.yaml 2>&1 | tee test-results/maestro.log
