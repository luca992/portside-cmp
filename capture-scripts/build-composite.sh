#!/bin/bash
# Assembles demo-captures/composite.mp4 from the six platform captures, laid out
# as two rows of three on a light canvas:
#
#   Row 1 (iOS):   Native SwiftUI | Liquid Glass + CMP | CMP
#   Row 2 (other): Android · CMP  | Desktop · JVM       | Web · Wasm
#
# Every panel is cropped to a TRUE ~0.46 device aspect (the phones are already
# frameless; desktop and web are cropped free of their window shadow) so no
# panel is stretched.
#
# Sync model — every panel is beat-driven off the shared beat table (beats.sh),
# so the only per-panel variable is each recording's launch trim. The -ss values
# below anchor the detail sheet's pale-pink "Docking in 2h 31min" banner (unique
# to the detail view) at ~t=1.6s in every panel; from there the identical beat
# intervals keep the rest of the walkthrough in step. RE-MEASURE the banner's
# first frame per capture (see detect-banner note) whenever anything is
# re-recorded, and verify by extracting composite frames around t=1.7 (all
# showing the detail) and t=12 (all back on their tabs).
#
# Usage: capture-scripts/build-composite.sh   (run from repo root)
set -euo pipefail
cd "$(dirname "$0")/../demo-captures"

PW=640; PH=1390       # phone panel (0.4603)
DW=662                # desktop panel width at PH (0.4765)
CW=2160; CH=3060      # canvas
DUR=22.5

# ---- rounded-corner masks + full-canvas header/label overlay ----------------
python3 << EOF
from PIL import Image, ImageDraw, ImageFont
PW,PH,DW,CW,CH=$PW,$PH,$DW,$CW,$CH
for name,w,h,r in [('/tmp/mask_phone.png',PW,PH,48),('/tmp/mask_desk.png',DW,PH,23)]:
    m=Image.new('L',(w,h),0); ImageDraw.Draw(m).rounded_rectangle([0,0,w-1,h-1],radius=r,fill=255); m.save(name)
img=Image.new('RGBA',(CW,CH),(0,0,0,0)); d=ImageDraw.Draw(img)
title_f=ImageFont.truetype('/System/Library/Fonts/HelveticaNeue.ttc',54,index=1)   # Bold
label_f=ImageFont.truetype('/System/Library/Fonts/HelveticaNeue.ttc',31,index=10)  # Medium
title='Portside: Kotlin Compose Multiplatform'
d.text(((CW-d.textlength(title,font=title_f))/2,42),title,font=title_f,fill=(17,20,28,255))
# panel centre x, label baseline y  (rows at y=175 and y=1613)
labels=[('iOS · Native (SwiftUI)',396,135),('iOS · Liquid Glass + CMP',1080,135),('iOS · CMP',1764,135),
        ('Android · Native (Compose)',385,1573),('Desktop · JVM · CMP',1080,1573),('Web · Wasm · CMP',1775,1573)]
for text,cx,y in labels:
    d.text((cx-d.textlength(text,font=label_f)/2,y),text,font=label_f,fill=(88,94,106,255))
img.save('/tmp/header_overlay.png')
EOF

# ---- retimed intermediates: -ss anchors each panel's scroll-down at ~t=3.4 ---
# Anchoring the detail scroll-down (the prominent motion) rather than the
# detail-open keeps the wasm/android panels — which render the scroll a beat
# slower than the native ones — from lagging visibly once the walkthrough scrolls
# the detail page. RE-MEASURE per capture (green "Arrival Forecast" bars first
# frame) whenever anything is re-recorded.
enc() { ffmpeg -y -v error -ss "$1" -i "$2" -vf "$3" -t "$DUR" \
  -c:v libx264 -preset fast -crf 15 -pix_fmt yuv420p "$4"; }
enc 0.35 ios-swiftui.mp4 "fps=60"                        /tmp/swiftui_rt.mp4
enc 0.55 ios-glass.mp4   "fps=60"                        /tmp/glass_rt.mp4
enc 0.65 ios-compose.mp4 "fps=60"                        /tmp/compose_rt.mp4
enc 0.00 android.mp4     "fps=60"                        /tmp/android_rt.mp4
# desktop: sckrecord captures the bare window (860x1800); crop the 56px title
# bar and center to the panel aspect -> 831x1744 (0.4765)
enc 2.50 desktop.mp4     "crop=831:1744:14:56,fps=60"    /tmp/desktop_rt.mp4
# web: screencapture window, already cropped to clean 902x1960 content by
# capture-web.sh. Its wheel expand+scroll lands the detail's forecast a beat
# later than the flick platforms (plus its capture lead-in), hence the larger -ss.
enc 2.10 web.mp4         "fps=60"                        /tmp/web_rt.mp4

# ---- assembly ----
cat > /tmp/composite.filter << FILTER
color=c=0xF2F2F5:s=${CW}x${CH}:r=60:d=${DUR}[bg0];
[8:v]format=rgba[hdr];
[bg0][hdr]overlay=0:0[bg];
[6:v]format=gray,scale=${PW}:${PH}[mp];
[7:v]format=gray,scale=${DW}:${PH}[md];
[0:v]scale=${PW}:${PH},setsar=1,tpad=stop_mode=clone:stop_duration=8[g0];
[1:v]scale=${PW}:${PH},setsar=1,tpad=stop_mode=clone:stop_duration=8[g1];
[2:v]scale=${PW}:${PH},setsar=1,tpad=stop_mode=clone:stop_duration=8[g2];
[3:v]scale=${PW}:${PH},setsar=1,tpad=stop_mode=clone:stop_duration=8[g3];
[4:v]scale=${DW}:${PH},setsar=1,tpad=stop_mode=clone:stop_duration=8[g4];
[5:v]scale=${PW}:${PH},setsar=1,tpad=stop_mode=clone:stop_duration=8[g5];
[mp]split=5[mp0][mp1][mp2][mp3][mp5];
[g0][mp0]alphamerge[p0];
[g1][mp1]alphamerge[p1];
[g2][mp2]alphamerge[p2];
[g3][mp3]alphamerge[p3];
[g4][md]alphamerge[p4];
[g5][mp5]alphamerge[p5];
[bg][p0]overlay=76:175[a];
[a][p1]overlay=760:175[b];
[b][p2]overlay=1444:175[c];
[c][p3]overlay=65:1613[d];
[d][p4]overlay=749:1613[e];
[e][p5]overlay=1455:1613[out]
FILTER
ffmpeg -y -v error \
  -i /tmp/swiftui_rt.mp4 -i /tmp/glass_rt.mp4 -i /tmp/compose_rt.mp4 \
  -i /tmp/android_rt.mp4 -i /tmp/desktop_rt.mp4 -i /tmp/web_rt.mp4 \
  -loop 1 -i /tmp/mask_phone.png -loop 1 -i /tmp/mask_desk.png -loop 1 -i /tmp/header_overlay.png \
  -filter_complex_script /tmp/composite.filter \
  -map "[out]" -t "$DUR" -c:v libx264 -preset slow -crf 18 -pix_fmt yuv420p composite.mp4

# poster + README-embeddable GIF (GitHub won't render committed mp4s inline)
ffmpeg -y -v error -ss 7 -i composite.mp4 -frames:v 1 poster.png
# Keep the GIF under GitHub's ~10MB inline-render cap (the mp4 is the crisp copy).
ffmpeg -y -v error -i composite.mp4 \
  -vf "fps=10,scale=720:-1:flags=lanczos,split[s0][s1];[s0]palettegen=max_colors=96[p];[s1][p]paletteuse=dither=bayer:bayer_scale=5" \
  composite.gif
echo "wrote composite.mp4, poster.png, composite.gif"
