#!/bin/bash
# Records a walkthrough of the Kotlin/Wasm web app running in a Chrome --app
# window: the walkthrough is DRIVEN over CDP, but CAPTURED with screencapture -v.
#
# Why screencapture and not CDP Page.startScreencast: screencast encodes a PNG
# per frame inside the renderer process, which starves the wasm compositor during
# a scroll and drops it to ~20fps (visibly jagged). screencapture grabs the
# already-composited window at the OS level (~55fps) with no renderer overhead,
# so a fling scrolls fluidly. Its one requirement is that the window be visible
# and frontmost (a minimized/occluded GPU window captures black), so this raises
# and focuses it first — KEEP IT FRONTMOST for the ~25s take.
#
# Synthetic OS mouse events don't reach the wasm <canvas>; CDP does. The detail
# scrolls with fling-shaped mouseWheel bursts (a trackpad, not a stepped drag);
# Escape closes the detail; Add Sailing closes on its own X. Beat-driven off
# beats.sh so it lines up with the other captures.
#
# Prereqs: web server on :8099, node (built-in WebSocket), Chrome launched with
#   --remote-debugging-port=9222 --remote-allow-origins=* --app=http://localhost:8099
#   --window-size=451,1020 --user-data-dir=<tmp>
#
# Usage: capture-scripts/capture-web.sh [chrome-window-id] <out.mp4>
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
source "$HERE/beats.sh"
if [ $# -ge 2 ]; then OUT=$2; else OUT=${1:-web.mp4}; fi
TMP=$(mktemp -d)

cat > "$TMP/drive.mjs" << 'EOF'
const list = await (await fetch('http://localhost:9222/json/list')).json();
const page = list.find(t => t.type==='page' && t.url.includes('8099'));
const ws = new WebSocket(page.webSocketDebuggerUrl);
let id=0; const pend={};
const send=(m,p={})=>new Promise(r=>{const i=++id;pend[i]=r;ws.send(JSON.stringify({id:i,method:m,params:p}));});
ws.onmessage = e => { const m=JSON.parse(e.data); if(m.id&&pend[m.id]){pend[m.id](m);delete pend[m.id];} };
const wait=ms=>new Promise(r=>setTimeout(r,ms));
await new Promise(r=>ws.onopen=r);
const click=async(x,y)=>{await send('Input.dispatchMouseEvent',{type:'mouseMoved',x,y});await send('Input.dispatchMouseEvent',{type:'mousePressed',x,y,button:'left',clickCount:1});await wait(45);await send('Input.dispatchMouseEvent',{type:'mouseReleased',x,y,button:'left',clickCount:1});};
// Fling: a burst of high-frequency wheel deltas whose velocity eases out
// (momentum decay), like a trackpad fling — one fluid glide, not a constant
// crawl. dt≈8ms ~= a trackpad's momentum-phase event rate. On a collapsed sheet
// the scaffold spends the early deltas expanding it, then scrolls the content.
const wheel=async(x,y,total,ms=850)=>{const dt=8,n=Math.max(1,Math.round(ms/dt));let w=[],s=0;for(let i=0;i<n;i++){const v=Math.pow(1-i/n,2);w.push(v);s+=v;}for(let i=0;i<n;i++){await send('Input.dispatchMouseEvent',{type:'mouseWheel',x,y,deltaX:0,deltaY:total*w[i]/s});await wait(dt);}};
// A drag is still needed for the map-reveal (collapses the whole sheet).
const swipe=async(x,y1,y2)=>{await send('Input.dispatchMouseEvent',{type:'mousePressed',x,y:y1,button:'left',clickCount:1});const N=36;for(let i=1;i<=N;i++){await send('Input.dispatchMouseEvent',{type:'mouseMoved',x,y:y1+(y2-y1)*i/N,button:'left',buttons:1});await wait(6);}await send('Input.dispatchMouseEvent',{type:'mouseReleased',x,y:y2,button:'left',clickCount:1});};
const esc=async()=>{await send('Input.dispatchKeyEvent',{type:'keyDown',key:'Escape',code:'Escape',windowsVirtualKeyCode:27,nativeVirtualKeyCode:27});await send('Input.dispatchKeyEvent',{type:'keyUp',key:'Escape',code:'Escape',windowsVirtualKeyCode:27,nativeVirtualKeyCode:27});};

const t0=Date.now();
const at=async t=>{const d=t0+t*1000-Date.now(); if(d>0)await wait(d);};

await at(2.6);  await click(225, 430);        // open the live sailing (top row center)
// Flings end slightly early: the wasm renderer DEFERS a popup until scrolling
// has fully settled (the menu was rendering ~2s late tied to the fling tail,
// not the click time), so give the scroll room to settle before the menu beat.
await at(4.35); await wheel(225, 600, 1300, 420);
await at(5.15); await wheel(225, 600, 1300, 420);
await at(6.3);  await click(120, 952);        // sailing menu (... in the action bar)
await at(8.4);  await click(413, 330);        // dismiss the menu (tap the sheet)
// One strong fling (plus a short top-up) back to the TOP of the detail — wheel
// over the content at y=600 (lower sits over the action bar and scrolls
// nothing). Compressed so the collapse can start early: the close must land on
// the standard beat, and the wasm spring needs time to settle first.
await at(8.9);  await wheel(225, 600, -2600, 500);
await at(9.55); await wheel(225, 600, -1200, 300);
// Content-drag down: content is at its top, so the whole drag collapses the
// sheet (a DRAG is a captured gesture — unlike a wheel the pointer doesn't fall
// off the shrinking sheet — so it drives it all the way to the peek). Fired
// early so the spring settles at the peek BEFORE the close beat (esc during the
// settle animation is swallowed).
await at(10.0); await swipe(225, 460, 955);
await at(11.3); await esc();                  // close: fired 0.3 early — the wasm close transition runs ~0.9s
await at(13.2); await click(204, 952);        // Friends
await at(14.9); await click(270, 952);        // Logbook
await at(16.6); await click(330, 952);        // search -> Add Sailing
await at(18.9); await click(415, 80);         // close Add Sailing on its X
// Add Sailing's auto-focused text field tears down the browser's focus when it
// closes, and the NEXT canvas click is swallowed restoring focus. Spend that
// click on an inert spot (logbook-card body) so the avatar click that follows
// actually opens the profile menu.
await at(20.0); await click(220, 560);        // focus-restore primer (inert)
await at(20.4); await click(413, 374);        // profile menu (avatar)
await at(22.3); await esc();                  // dismiss
await at(23.6);
ws.close(); console.log('web walkthrough done');
EOF

# reset to My Sailings, warm
node -e "
const run=async()=>{const l=await(await fetch('http://localhost:9222/json/list')).json();const p=l.find(t=>t.type==='page'&&t.url.includes('8099'));const ws=new WebSocket(p.webSocketDebuggerUrl);await new Promise(r=>ws.onopen=r);ws.send(JSON.stringify({id:1,method:'Page.reload',params:{ignoreCache:true}}));await new Promise(r=>setTimeout(r,200));ws.close();};run();
"
sleep 14  # wasm reload + settle

# PRE-WARM the sailing menu before recording: wasm composes a popup the first
# time it opens (~0.8-1.5s, variable), which made the recorded menu open a beat
# late no matter the compensation. Open detail -> open menu -> dismiss both;
# the composition cache survives, so the recorded open costs what the native
# panels' do. Ends back on the clean My Sailings list.
node -e '
const run=async()=>{
  const l=await(await fetch("http://localhost:9222/json/list")).json();
  const p=l.find(t=>t.type==="page"&&t.url.includes("8099"));
  const ws=new WebSocket(p.webSocketDebuggerUrl);let id=0;const pend={};
  const send=(m,pr={})=>new Promise(r=>{const i=++id;pend[i]=r;ws.send(JSON.stringify({id:i,method:m,params:pr}));});
  ws.onmessage=e=>{const m=JSON.parse(e.data);if(m.id&&pend[m.id]){pend[m.id](m);delete pend[m.id];}};
  const wait=ms=>new Promise(r=>setTimeout(r,ms));
  const click=async(x,y)=>{await send("Input.dispatchMouseEvent",{type:"mouseMoved",x,y});await send("Input.dispatchMouseEvent",{type:"mousePressed",x,y,button:"left",clickCount:1});await wait(45);await send("Input.dispatchMouseEvent",{type:"mouseReleased",x,y,button:"left",clickCount:1});};
  const esc=async()=>{await send("Input.dispatchKeyEvent",{type:"keyDown",key:"Escape",code:"Escape",windowsVirtualKeyCode:27,nativeVirtualKeyCode:27});await send("Input.dispatchKeyEvent",{type:"keyUp",key:"Escape",code:"Escape",windowsVirtualKeyCode:27,nativeVirtualKeyCode:27});};
  await new Promise(r=>ws.onopen=r);
  await click(225,430); await wait(2000);   // open the detail
  await click(120,952); await wait(1800);   // open the sailing menu (composes+caches the popup)
  await esc(); await wait(600);             // dismiss the menu
  await esc(); await wait(1800);            // close the detail -> My Sailings
  ws.close();
};run();
'
sleep 1

# raise + un-minimize + focus so the GPU window is captured (not black), and find it
osascript >/dev/null 2>&1 <<'OSA' || true
tell application "Google Chrome"
  activate
  repeat with w in windows
    try
      set miniaturized of w to false
      set index of w to 1
    end try
  end repeat
end tell
OSA
sleep 1
WIN=$(python3 -c "
import Quartz
for w in Quartz.CGWindowListCopyWindowInfo(Quartz.kCGWindowListOptionOnScreenOnly, Quartz.kCGNullWindowID):
    b=w.get('kCGWindowBounds',{})
    if w.get('kCGWindowOwnerName') in ('Google Chrome','Chromium') and b.get('Height',0)>400:
        print(w['kCGWindowNumber']); break")
echo "chrome window $WIN"

# Park the REAL cursor away from the window: physical mouse hover/moves over the
# canvas interleave with the synthetic CDP pointer and cancel its clicks (a real
# mousemove between a synthetic press and release turns the click into a drag).
python3 -c "import Quartz; Quartz.CGWarpMouseCursorPosition((5, 5))"

# sckrecord (ScreenCaptureKit): vsync-aligned tear-free 60fps window capture
# (screencapture -v samples mid-repaint and shows tear lines).
[ -x "$HERE/sckrecord" ] || swiftc -O "$HERE/sckrecord.swift" -o "$HERE/sckrecord"
"$HERE/sckrecord" "$WIN" "$TMP/raw.mov" 60 &
REC=$!
sleep 1.2
node "$TMP/drive.mjs"
kill -INT $REC 2>/dev/null || true
wait $REC 2>/dev/null || true
sleep 1

# Detect the app content region in the recorded window (Chrome --app adds a
# window shadow all round; there's essentially no title bar). Locate the light
# sheet band for the horizontal extent, the content rows for the vertical, then
# crop to a 0.46 device aspect anchored at the bottom so the nav bar is kept.
ffmpeg -y -v error -i "$TMP/raw.mov" -frames:v 1 "$TMP/frame0.png"
CROP=$(python3 - "$TMP/frame0.png" << 'PY'
import sys
from PIL import Image
im = Image.open(sys.argv[1]).convert('RGB'); W, H = im.size; px = im.load()
def nb(x, y): return sum(px[x, y]) > 40
midy = int(H * 0.66)                                   # inside the light sheet
xs = [x for x in range(W) if px[x, midy][0] > 150 and px[x, midy][1] > 150 and px[x, midy][2] > 150]
xL, xR = min(xs), max(xs); cw = xR - xL + 1
rows = [y for y in range(H) if sum(1 for x in range(xL, xR, 4) if nb(x, y)) > (cw / 4) * 0.5]
yB = max(rows)
sh = round(cw / 0.4603)
top = max(0, yB - sh)
print(f"{cw}:{sh}:{xL}:{top}")
PY
)
echo "content crop $CROP"
if [ -z "$CROP" ]; then echo "crop detection failed (window black? keep Chrome frontmost)"; exit 1; fi
ffmpeg -y -v error -i "$TMP/raw.mov" -vf "crop=$CROP,fps=60" -fps_mode cfr -r 60 \
  -c:v libx264 -preset slow -crf 18 -pix_fmt yuv420p "$OUT"
echo "wrote $OUT"
