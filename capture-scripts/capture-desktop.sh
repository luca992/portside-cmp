#!/bin/bash
# Records the desktop (JVM) walkthrough by driving the app window with
# synthetic mouse events (Quartz, via deskdrive.py).
#
# Prereqs:
#   - `pip install pyobjc pillow`
#   - the Portside window on the ACTIVE macOS Space, hands off mouse/keyboard
#     for the ~30s take (events go through the real cursor)
#   - terminal has Accessibility + Screen Recording permissions
#
# The script RESTARTS the app first: a fresh launch is the only reliable
# reset (blind clicks against an assumed UI state are how takes die), and it
# guarantees clean mock data. Targets that vary with sheet state (detail
# close X, avatar) are located from live pixels mid-take — each find costs
# ~0.5s, which is why the choreography uses them only where a blind
# coordinate has actually been seen to miss.
#
# Usage: capture-scripts/capture-desktop.sh <out.mp4>
set -euo pipefail
OUT=${1:-desktop.mp4}
HERE=$(cd "$(dirname "$0")" && pwd)
REPO=$(cd "$HERE/.." && pwd)
TMP=$(mktemp -d)

pkill -f MainKt 2>/dev/null || true
sleep 2
# NOT --variant=release: the 0.12-dev toolchain's release executable-jar is
# missing the Compose runtime (NoClassDefFoundError: androidx.compose.runtime
# .Composer at launch). Unlike iOS/Android, JVM debug vs release is the same
# bytecode on the same JIT, so the dev run performs identically.
(cd "$REPO" && ./kotlin run -m jvm-app > "$TMP/app.log" 2>&1 &)
sleep 24

python3 - "$HERE" << 'EOF'
import sys, time
sys.path.insert(0, sys.argv[1])
import Quartz
from AppKit import NSRunningApplication, NSApplicationActivateIgnoringOtherApps
from deskdrive import App
app = App()
# All windows, not just on-screen: the window may have opened on another Space
# (activation below switches to it).
wins = Quartz.CGWindowListCopyWindowInfo(Quartz.kCGWindowListOptionAll, Quartz.kCGNullWindowID)
pid = next(w['kCGWindowOwnerPID'] for w in wins if w.get('kCGWindowOwnerName') == 'MainKt')
NSRunningApplication.runningApplicationWithProcessIdentifier_(pid).activateWithOptions_(NSApplicationActivateIgnoringOtherApps)
time.sleep(1.0)
EOF

WINID=$(python3 -c "
import sys; sys.path.insert(0, '$HERE')
from deskdrive import bounds
print(bounds()[0])")

# sckrecord (ScreenCaptureKit): vsync-aligned tear-free 60fps window capture
# (screencapture -v samples mid-repaint and shows tear lines).
[ -x "$HERE/sckrecord" ] || swiftc -O "$HERE/sckrecord.swift" -o "$HERE/sckrecord"
"$HERE/sckrecord" "$WINID" "$TMP/raw.mov" 60 &
REC=$!
sleep 1.2

python3 - "$HERE" << 'EOF'
import sys, time
sys.path.insert(0, sys.argv[1])
from deskdrive import App
app = App()

def find_avatar():
    im = app.img().convert('RGB')
    for cy in range(80, 480, 3):
        for cx in range(355, 412, 3):
            r, g, b = app.probe(im, cx, cy)
            if 90 < r < 150 and g < 120 and b > 200:
                return cx - 8, cy
    return 385, 330

# Beat-driven off the SAME timeline as the iOS/Android captures (beats.sh), so
# every panel performs each action at the same moment and the composite lines
# up without per-panel speed retiming. beat(t) waits until t seconds past t0;
# an action that overruns just makes the next beat fire immediately.
t0 = time.time()
def beat(t):
    d = t0 + t - time.time()
    if d > 0:
        time.sleep(d)

# 0.2 EARLY: desktop's click->first-rendered-frame latency runs ~0.2s longer
# than the phones', so firing on the shared beat showed its detail opening late
# in the composite. The compensation makes the VISIBLE open land on the beat.
beat(2.4);  app.click(215, 395)                        # open detail
beat(4.4);  app.glide(215, 500, -1250, 0.85)           # scroll toward the bottom
beat(5.3);  app.glide(215, 500, -1250, 0.85)
beat(6.6);  app.click(116, 838)                        # sailing menu from the ... pill
beat(8.4);  app.click(392, 300)                        # dismiss menu
beat(8.9);  app.glide(215, 500, 1250, 0.7)             # back to the top
beat(9.7);  app.glide(215, 500, 1250, 0.7)
beat(10.6); app.drag(215, 120, 215, 470, 0.5)          # pull the sheet down to the small state (map fills)
cx = app.find_close_x()                                # locate the X NOW (~0.5s) so the click fires ON the beat
beat(11.6); app.click(410, cx or 300)                  # X -> home
beat(13.2); app.click(192, 816)                        # Friends
beat(14.9); app.click(259, 816)                        # Logbook
beat(16.6); app.click(318, 816)                        # search -> Add Sailing
beat(18.9); app.click(390, 76)                         # its X (fullscreen sheet: stable)
beat(19.6); ax, ay = find_avatar()                     # locate the avatar during the idle gap
beat(20.3); app.click(ax, ay)                          # profile menu
beat(22.3); app.click(140, 500)                        # dismiss
beat(23.6)
EOF

kill -INT $REC 2>/dev/null || true
wait $REC 2>/dev/null || true
sleep 1
ffmpeg -y -v error -i "$TMP/raw.mov" -fps_mode cfr -r 60 \
  -c:v libx264 -preset slow -crf 18 -pix_fmt yuv420p "$OUT"
echo "wrote $OUT"
