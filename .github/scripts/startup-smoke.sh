#!/usr/bin/env bash
set +e

mkdir -p app/build/diagnostics
adb wait-for-device

boot_state=""
for attempt in $(seq 1 60); do
  boot_state="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
  [ "$boot_state" = "1" ] && break
  sleep 2
done

smoke_exit=0

if [ "$boot_state" != "1" ]; then
  echo "Emulator did not finish booting within 120 seconds." | tee app/build/diagnostics/startup-smoke.txt
  smoke_exit=1
else
  adb logcat -c
  adb install -r app/build/outputs/apk/debug/app-debug.apk > app/build/diagnostics/install.txt 2>&1
  if [ $? -ne 0 ]; then
    cat app/build/diagnostics/install.txt
    smoke_exit=1
  else
    for component in MainActivity HomeLauncherActivity; do
      package="com.rezoxnemesis.kebtee"
      echo "Launching $component" | tee -a app/build/diagnostics/startup-smoke.txt
      adb shell am force-stop "$package"
      adb shell am start -W -n "$package/.$component" > "app/build/diagnostics/launch-$component.txt" 2>&1
      launch_status=$?
      cat "app/build/diagnostics/launch-$component.txt"
      # Android 16 emulator startup can make am start -W time out even when the
      # activity eventually launches. Check the foreground activity and app-specific
      # crash output rather than treating a live PID alone as a successful launch.
      sleep 8
      adb shell pidof "$package" > "app/build/diagnostics/pid-$component.txt" 2>&1
      adb shell dumpsys activity activities > "app/build/diagnostics/activities-$component.txt" 2>&1
      adb logcat -d -v threadtime > "app/build/diagnostics/logcat-$component.txt" 2>&1
      resumed="$(grep -E 'topResumedActivity|mResumedActivity|Resumed:' "app/build/diagnostics/activities-$component.txt" | grep -F "$package/.$component" || true)"
      crash="$(grep -E 'FATAL EXCEPTION|AndroidRuntime.*(Exception|Error)|Process: com[.]rezoxnemesis[.]kebtee|Unable to start activity' "app/build/diagnostics/logcat-$component.txt" || true)"
      if [ ! -s "app/build/diagnostics/pid-$component.txt" ] || [ -z "$resumed" ] || [ -n "$crash" ]; then
        echo "FAIL: $component did not stay foreground without an app crash." | tee -a app/build/diagnostics/startup-smoke.txt
        printf '%s\n' "$resumed" "$crash" >> app/build/diagnostics/startup-smoke.txt
        smoke_exit=1
      else
        echo "PASS: $component stayed foreground with no detected app crash." | tee -a app/build/diagnostics/startup-smoke.txt
      fi
    done
  fi
fi

adb logcat -d -v threadtime > app/build/diagnostics/emulator-logcat.txt 2>&1
echo "$smoke_exit" > app/build/diagnostics/startup-smoke-exit-code.txt
grep -E "FATAL EXCEPTION|AndroidRuntime|Process: com\.rezoxnemesis\.kebtee|Fatal signal|Unable to start activity|Caused by:" app/build/diagnostics/emulator-logcat.txt | tail -n 100 || true

exit "$smoke_exit"
