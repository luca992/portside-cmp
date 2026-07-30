#!/bin/bash
# Records the Android walkthrough on a physical phone via adb.
#
# Prereqs: release build installed (`./kotlin run -m android-app
# --variant=release --device-id=$SERIAL` or `adb install -r` the APK from
# build/tasks/_android-app_buildAndroidRelease/), device awake and unlocked.
#
# Coordinates assume a 1080x2404 display (Pixel-class). The input script runs
# ON-DEVICE in a single `adb shell` so timing can't drift with host latency.
#
# Usage: capture-scripts/capture-android.sh <adb-serial> <out.mp4>
set -euo pipefail
S=${1:?adb serial}
OUT=${2:-android.mp4}
TMP=$(mktemp -d)

adb -s "$S" shell svc power stayon true
adb -s "$S" shell input keyevent KEYCODE_WAKEUP; sleep 1
adb -s "$S" shell cmd notification set_dnd on            # no popups over the take
adb -s "$S" shell settings put global heads_up_notifications_enabled 0
adb -s "$S" shell am force-stop com.portside.app
adb -s "$S" shell cmd statusbar collapse

# SysUI demo mode gives a clean, deterministic status bar for the take: a fixed
# 9:41 clock (matching the iOS panels), full signal/wifi/battery, and — the
# reason it's here — every notification icon hidden, so no stray app icon lands
# in the bar mid-recording. Exited in the cleanup section below.
adb -s "$S" shell settings put global sysui_demo_allowed 1
demo() { adb -s "$S" shell am broadcast -a com.android.systemui.demo "$@" >/dev/null; }
demo -e command enter
demo -e command notifications -e visible false
demo -e command clock -e hhmm 0941
demo -e command network -e wifi show -e level 4
demo -e command network -e mobile show -e datatype none -e level 4
demo -e command battery -e level 100 -e plugged false
sleep 1.2

adb -s "$S" shell "screenrecord --time-limit 30 --bit-rate 12000000 /sdcard/portside_walk.mp4" &
REC=$!
sleep 1.5

# The whole choreography as one on-device script (sleeps are device-side).
adb -s "$S" shell "am start -n com.portside.app/.MainActivity; sleep 2.6; \
input tap 540 1080; sleep 1.7; \
input swipe 540 2050 540 650 480; sleep 0.5; \
input swipe 540 2050 540 650 480; sleep 0.7; \
input tap 287 2255; sleep 1.0; \
input keyevent 4; sleep 0.15; \
input swipe 540 650 540 2050 480; sleep 0.45; \
input swipe 540 650 540 2050 480; sleep 0.45; \
input swipe 540 1200 540 2200 250; \
input keyevent 4; sleep 1.55; \
input tap 488 2258; sleep 1.5; \
input tap 647 2258; sleep 1.3; \
input tap 790 2231; sleep 2.2; \
input tap 990 195; sleep 1.5; \
input tap 986 916; sleep 1.9; \
input keyevent 4; sleep 1.6"

wait $REC 2>/dev/null || true
sleep 1
adb -s "$S" shell cmd notification set_dnd off
adb -s "$S" shell settings put global heads_up_notifications_enabled 1
adb -s "$S" shell am broadcast -a com.android.systemui.demo -e command exit >/dev/null
adb -s "$S" pull /sdcard/portside_walk.mp4 "$TMP/raw.mp4"
adb -s "$S" shell rm /sdcard/portside_walk.mp4

# Trim the pre-launch homescreen and re-encode 60fps CFR — without cfr the
# transcode silently drops ~1/3 of the VFR frames and the motion reads rough.
# Clear personal notifications (Clear all) before the take so the status bar is
# clean; the persistent "USB debugging connected" icon stays (it can't clear
# while the recording's USB-adb is attached) and that's fine.
ffmpeg -y -v error -ss 2.0 -i "$TMP/raw.mp4" -fps_mode cfr -r 60 \
  -c:v libx264 -preset slow -crf 18 -pix_fmt yuv420p "$OUT"
echo "wrote $OUT"
