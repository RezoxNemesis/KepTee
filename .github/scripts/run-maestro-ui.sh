#!/usr/bin/env bash
set -uo pipefail

mkdir -p test-results
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm grant com.rezoxnemesis.kebtee android.permission.POST_NOTIFICATIONS || true

curl -Ls "https://get.maestro.mobile.dev" | bash
export PATH="$HOME/.maestro/bin:$PATH"
maestro --version

set +e
maestro test --format junit --output test-results/maestro-junit.xml .maestro/flows/core-ui.yaml 2>&1 | tee test-results/maestro.log
test_status=${PIPESTATUS[0]}

adb logcat -d -v threadtime > test-results/logcat.txt
adb shell dumpsys activity activities > test-results/activities.txt
find "$HOME/.maestro" -type f \( -name "*.png" -o -name "*.jpg" \) -exec cp -n {} test-results/ \; 2>/dev/null || true

exit "$test_status"
