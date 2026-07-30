# The walkthrough's beat table: one absolute timeline every platform follows.
#
# Each value is seconds since the app was launched. Capture scripts wait until
# the beat's wall-clock moment before acting rather than sleeping between
# actions, so a slower app (or a slower `idb`/`adb` round trip) cannot let one
# panel drift out of step with the others. That is what makes the finished
# composite line up without any per-panel retiming.
#
# Timings are paced like the reference: a beat to read each screen, quick
# flicks with inertia rather than long drags, and no dead air.

BEAT_OPEN_DETAIL=2.6      # tap the live sailing
BEAT_FLICK_DOWN_1=4.4     # flick toward the bottom of the detail page
BEAT_FLICK_DOWN_2=5.3
BEAT_MENU_OPEN=6.6        # the ... sailing menu
BEAT_MENU_DISMISS=8.4
BEAT_FLICK_UP_1=8.9       # back to the top
BEAT_FLICK_UP_2=9.7
BEAT_REVEAL_MAP=10.6      # pull the sheet down so the map shows
BEAT_CLOSE_DETAIL=11.6
BEAT_FRIENDS=13.2
BEAT_LOGBOOK=14.9
BEAT_ADD_SAILING=16.6
BEAT_ADD_DISMISS=18.9
BEAT_PROFILE_MENU=20.3
BEAT_PROFILE_DISMISS=22.3
BEAT_END=23.6             # total walkthrough length

# Marks "now" as t=0 for the beats below.
beats_start() { BEATS_T0=$(python3 -c 'import time; print(time.time())'); }

# Blocks until the given beat time, then returns so the action fires on time.
at() {
  python3 - "$1" "$BEATS_T0" <<'PY'
import sys, time
target = float(sys.argv[1]) + float(sys.argv[2])
delay = target - time.time()
if delay > 0:
    time.sleep(delay)
PY
}

# simctl's recorder takes a variable moment to start, so a fixed trim leaves
# each video with a different lead-in and the panels drift even though the
# actions were on time. Instead, find the first frame where the app itself is
# on screen (its dark space backdrop replacing the light springboard) and trim
# there — that puts t=0 at the same instant in every recording.
app_start_offset() {
  python3 - "$1" <<'PY'
import glob, subprocess, sys, tempfile
from PIL import Image
src = sys.argv[1]
d = tempfile.mkdtemp()
# Sample only the TOP third: in-app that's the dark globe/space backdrop
# (< 70), while the springboard wallpaper is bright there. A whole-frame test
# broke once the clean crop dropped the black phone frame — My Sailings' large
# light sheet then pushed the frame average above 70 and the trim fell back to
# 2.4s, leaving the springboard on screen.
subprocess.run(['ffmpeg', '-y', '-v', 'error', '-t', '10', '-i', src,
                '-vf', 'crop=in_w:in_h/3:0:0,fps=20,scale=64:-1', f'{d}/f%04d.png'], check=True)
frames = sorted(glob.glob(f'{d}/f*.png'))
for i, f in enumerate(frames):
    px = list(Image.open(f).convert('L').getdata())
    if sum(px) / len(px) < 70:          # the app's dark backdrop
        print(round(i / 20.0, 3))
        break
else:
    print(2.4)
PY
}

# `simctl io recordVideo` returns immediately but takes a moment to actually
# begin writing; launching before then silently loses the opening beats and
# leaves that panel shorter than the others. A generous fixed lead-in plus the
# app-start trim above keeps every recording covering the whole walkthrough.
RECORDER_LEAD_IN=3.5
