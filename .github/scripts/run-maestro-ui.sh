#!/usr/bin/env bash
set -uo pipefail

mkdir -p test-results

wait_for_android() {
  adb start-server >/dev/null 2>&1 || true
  adb wait-for-device

  for _ in $(seq 1 90); do
    boot_completed="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
    bootanim="$(adb shell getprop init.svc.bootanim 2>/dev/null | tr -d '\r' || true)"
    if [[ "$boot_completed" == "1" && "$bootanim" == "stopped" ]]; then
      adb shell wm dismiss-keyguard >/dev/null 2>&1 || true
      adb shell input keyevent 82 >/dev/null 2>&1 || true
      return 0
    fi
    sleep 2
  done

  echo "Android emulator did not become fully ready in time." >&2
  return 1
}

collect_diagnostics() {
  adb logcat -d -v threadtime > test-results/logcat.txt 2>&1 || true
  adb shell dumpsys activity activities > test-results/activities.txt 2>&1 || true
  adb shell getprop > test-results/getprop.txt 2>/dev/null || true
  adb devices -l > test-results/adb-devices.txt 2>&1 || true
  adb shell screencap -p /sdcard/keptee-ui.png >/dev/null 2>&1 || true
  adb pull /sdcard/keptee-ui.png test-results/final-screen.png >/dev/null 2>&1 || true
  find "$HOME/.maestro" -type f \( -name "*.png" -o -name "*.jpg" \) -exec cp -n {} test-results/ \; 2>/dev/null || true
}

run_maestro() {
  local log_file="$1"
  local junit_file="$2"

  set +e
  maestro test --format junit --output "$junit_file" .maestro/flows/core-ui.yaml 2>&1 | tee "$log_file"
  local status=${PIPESTATUS[0]}
  set -e
  return "$status"
}

set -e
trap collect_diagnostics EXIT

wait_for_android
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm grant com.rezoxnemesis.kebtee android.permission.POST_NOTIFICATIONS || true
adb logcat -c

export MAESTRO_CLI_NO_ANALYTICS=1
export MAESTRO_DRIVER_STARTUP_TIMEOUT=120000
maestro --version

set +e
run_maestro test-results/maestro.log test-results/maestro-junit.xml
test_status=$?
set -e

if [[ "$test_status" -ne 0 ]] && grep -Eq   "Maestro Android driver did not start up in time|MaestroDriverStartupException|AndroidDriverTimeoutException"   test-results/maestro.log; then
  echo "Detected a transient Maestro Android-driver startup failure. Reinitialising ADB and retrying once."

  cp test-results/maestro.log test-results/maestro-first-attempt.log || true
  cp test-results/maestro-junit.xml test-results/maestro-first-attempt-junit.xml 2>/dev/null || true

  adb kill-server >/dev/null 2>&1 || true
  sleep 3
  wait_for_android
  sleep 5

  set +e
  run_maestro test-results/maestro.log test-results/maestro-junit.xml
  test_status=$?
  set -e
fi

exit "$test_status"
