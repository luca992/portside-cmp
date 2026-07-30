#!/bin/bash
# Records an iOS-simulator walkthrough of any of the three iOS apps, driven by
# the shared beat table so every platform performs each action at the same
# moment (see beats.sh).
#
# Prereqs: RELEASE builds installed on the sim (debug K/N binaries are far too
# slow to demo): `./kotlin run -m <module> --platform=iosSimulatorArm64
# --variant=release --device-id=$UDID`, plus idb (`brew install idb-companion`).
#
# Coordinates are POINTS on an iPhone 17 Pro Max (440x956). idb swipes need
# --delta 3: the default emits sparse touch points and scrolls look robotic.
#
# Flick distances are tuned per app so that the two down-flicks land exactly on
# the bottom of the detail page and the two up-flicks land exactly on the top —
# reaching the end without flicking against it (measured, not guessed).
#
# Usage: capture-scripts/capture-ios.sh <sim-udid> compose|glass|swiftui <out.mp4>
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
source "$HERE/beats.sh"

UD=${1:?sim udid}
VARIANT=${2:?compose|glass|swiftui}
REVEAL_SKIP=""      # swiftui overrides: skip the map-reveal drag (just tap the X)
ADD_DISMISS_TAP=""  # swiftui overrides: its Add Sailing overlay closes on the X, not a swipe
REVEAL_DUR="0.3"    # swiftui overrides: a quick flick so its spring animates the collapse
OUT=${3:-ios-$VARIANT.mp4}
TMP=$(mktemp -d)

# Each app's chrome sits slightly differently; the choreography is identical.
case "$VARIANT" in
  glass)
    BUNDLE=com.portside-cmp.glass
    ROW="220 431"; DETAIL_X="409 467"; ELLIPSIS="118 884"; REVEAL="220 477 220 875"
    ADD_DISMISS_TAP="404 82"   # Add Sailing closes on its top-right X, like every panel
    DOWN="220 760 220 380"; UP="220 380 220 760"
    T_FRIENDS="184 901"; T_LOGBOOK="288 901"; T_SEARCH="386 901"; AVATAR="401 365"
    ;;
  swiftui)
    BUNDLE=com.portside-cmp.swiftui
    # Reveal is a quick downward flick on the grabber: the sheet's own spring then
    # animates the collapse to the peek/small state (map fills the top) smoothly,
    # instead of the finger dragging it the whole way (which reads as steppy). The
    # grabber no longer dismisses, so this reliably collapses. Close is then a tap
    # on the detail's X at the peek position (417,469). Add Sailing is a custom
    # overlay that closes on its own X, not a swipe.
    ROW="150 432"; DETAIL_X="417 469"; ELLIPSIS="118 897"; REVEAL="220 70 220 240"
    REVEAL_DUR="0.12"
    ADD_DISMISS_TAP="400 88"
    # Gentler flicks than the Compose apps use: this sheet expands as it
    # scrolls, so the viewport is taller and a full-length flick would hit the
    # bottom in one go and then bounce against it.
    DOWN="220 790 220 640"; UP="220 640 220 790"
    T_FRIENDS="184 903"; T_LOGBOOK="289 903"; T_SEARCH="387 903"; AVATAR="403 369"
    ;;
  *)
    BUNDLE=com.portside-cmp.app
    ROW="220 431"; DETAIL_X="409 467"; ELLIPSIS="118 884"; REVEAL="220 477 220 875"
    ADD_DISMISS_TAP="404 82"   # Add Sailing closes on its top-right X, like every panel
    DOWN="220 760 220 380"; UP="220 380 220 760"
    T_FRIENDS="199 880"; T_LOGBOOK="267 880"; T_SEARCH="327 875"; AVATAR="401 365"
    ;;
esac

xcrun simctl status_bar "$UD" override --time 9:41 --batteryLevel 100 \
  --cellularBars 4 --dataNetwork wifi --wifiBars 3
# Warm launch first: a cold first run after install stutters.
xcrun simctl terminate "$UD" "$BUNDLE" 2>/dev/null || true
xcrun simctl launch "$UD" "$BUNDLE" >/dev/null; sleep 5
xcrun simctl terminate "$UD" "$BUNDLE" 2>/dev/null || true
sleep 2

# simctl recordVideo caps at 30fps (visibly stutters when scrolling); the app
# renders at the display's full rate, so capture the sim WINDOW instead — with
# sckrecord (ScreenCaptureKit): vsync-aligned complete frames at 60fps, unlike
# screencapture -v which samples mid-repaint and shows tear lines.
SIMWIN=$(python3 << 'PY'
import Quartz
for w in Quartz.CGWindowListCopyWindowInfo(Quartz.kCGWindowListOptionOnScreenOnly, Quartz.kCGNullWindowID):
    if w.get('kCGWindowOwnerName') == 'Simulator' and w.get('kCGWindowBounds', {}).get('Height', 0) > 400:
        print(w['kCGWindowNumber']); break
PY
)
echo "sim window $SIMWIN"
[ -x "$HERE/sckrecord" ] || swiftc -O "$HERE/sckrecord.swift" -o "$HERE/sckrecord"
"$HERE/sckrecord" "$SIMWIN" "$TMP/raw.mov" 60 &
REC=$!
sleep "$RECORDER_LEAD_IN"
xcrun simctl launch "$UD" "$BUNDLE"
beats_start

flick() { idb ui swipe --udid "$UD" $1 --duration 0.14 --delta 3; }

at $BEAT_OPEN_DETAIL;     idb ui tap --udid "$UD" $ROW
at $BEAT_FLICK_DOWN_1;    flick "$DOWN"
at $BEAT_FLICK_DOWN_2;    flick "$DOWN"
at $BEAT_MENU_OPEN;       idb ui tap --udid "$UD" $ELLIPSIS
at $BEAT_MENU_DISMISS;    idb ui tap --udid "$UD" 350 150
at $BEAT_FLICK_UP_1;      flick "$UP"
at $BEAT_FLICK_UP_2;      flick "$UP"
if [ -z "$REVEAL_SKIP" ]; then
  at $BEAT_REVEAL_MAP;    idb ui swipe --udid "$UD" $REVEAL --duration "$REVEAL_DUR" --delta 3
fi
at $BEAT_CLOSE_DETAIL;    idb ui tap --udid "$UD" $DETAIL_X
at $BEAT_FRIENDS;         idb ui tap --udid "$UD" $T_FRIENDS
at $BEAT_LOGBOOK;        idb ui tap --udid "$UD" $T_LOGBOOK
at $BEAT_ADD_SAILING;      idb ui tap --udid "$UD" $T_SEARCH
if [ -n "$ADD_DISMISS_TAP" ]; then
  at $BEAT_ADD_DISMISS;   idb ui tap --udid "$UD" $ADD_DISMISS_TAP
else
  at $BEAT_ADD_DISMISS;   idb ui swipe --udid "$UD" 220 200 220 900 --duration 0.4 --delta 3
fi
at $BEAT_PROFILE_MENU;    idb ui tap --udid "$UD" $AVATAR
at $BEAT_PROFILE_DISMISS; idb ui tap --udid "$UD" 150 600
at $BEAT_END

kill -INT $REC
wait $REC 2>/dev/null || true
sleep 1

# Detect the screen crop from the RECORDED video (screencapture -v pads the
# window with a shadow margin, so its dimensions differ from a -o screenshot —
# the crop must be measured against the video itself). The macOS toolbar is a
# uniform gray band (~40,40,40) spanning the window's content width; find its
# left/right edges and its bottom, and the screen is the aspect-correct region
# directly below it (Show Device Bezels OFF → no phone frame to strip).
ffmpeg -y -v error -i "$TMP/raw.mov" -frames:v 1 "$TMP/frame0.png"
SCREEN_CROP=$(python3 - "$TMP/frame0.png" << 'PY'
import sys
from PIL import Image
im = Image.open(sys.argv[1]).convert('RGB'); W, H = im.size; px = im.load()
def isgray(p):
    r, g, b = p
    return 30 < r < 60 and 30 < g < 60 and 30 < b < 60 and abs(r-g) < 8 and abs(g-b) < 8
# toolbar row = the upper row with the longest contiguous gray run
best = (0, 0)
for y in range(0, 220):
    run = mx = 0
    for x in range(W):
        if isgray(px[x, y]): run += 1; mx = max(mx, run)
        else: run = 0
    if mx > best[0]: best = (mx, y)
ty = best[1]
xs = [x for x in range(W) if isgray(px[x, ty])]
xL, xR = min(xs), max(xs)
cx = (xL + xR) // 2
yb = ty
for y in range(ty, ty + 200):          # walk down to where the toolbar ends
    if not isgray(px[cx, y]): yb = y; break
cw = xR - xL + 1
sh = round(cw / 0.4603)
print(f"{cw}:{sh}:{xL}:{yb}")
PY
)
echo "screen crop $SCREEN_CROP"
# Crop the toolbar + shadow off, then trim to the app's first frame so every
# recording starts at the same instant. 60fps CFR keeps every captured frame.
ffmpeg -y -v error -i "$TMP/raw.mov" -vf "crop=$SCREEN_CROP" \
  -c:v libx264 -preset ultrafast -crf 12 -pix_fmt yuv420p "$TMP/screen.mp4"
START=$(app_start_offset "$TMP/screen.mp4")
ffmpeg -y -v error -ss "$START" -i "$TMP/screen.mp4" -fps_mode cfr -r 60 \
  -c:v libx264 -preset veryfast -crf 18 -pix_fmt yuv420p "$OUT"
echo "wrote $OUT"
# NOTE: if recordVideo says "Host recording is already in progress", a prior
# recording is orphaned inside SimRender — `xcrun simctl shutdown/boot` the sim
# (do NOT kill SimRender; that shuts the whole simulator down).
