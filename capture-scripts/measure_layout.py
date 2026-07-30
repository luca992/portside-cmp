#!/usr/bin/env python3
"""Measures a screen's layout in device points from a simulator screenshot.

Use it to check two apps against each other: run it on the same screen from
each and compare sheet position, card margins, and the vertical rhythm. It
reports points on a 440x956 phone, so numbers are directly comparable.

NOT usable against the reference video. That footage is a compressed recording
of a phone *mockup*, where the card fill (#FFFFFF) and the sheet fill
(#F2F2F5) end up only a few grey levels apart and the phone's bezel blends
into the dark map above the sheet. Automated screen-detection on it produced
plainly wrong results (identical sheet positions across different screens,
1pt card margins). Reference numbers have to be taken by hand, per screen,
against a high-contrast landmark — that is how the 11pt sheet margin in
PortsideMetrics was obtained.

Usage: measure_layout.py <screenshot.png>
"""

import sys

from PIL import Image

PT_W, PT_H = 440.0, 956.0


def analyse(im, screen):
    x0, y0, x1, y1 = screen
    px = im.convert("RGB").load()
    sx = (x1 - x0) / PT_W          # pixels per point, horizontally
    sy = (y1 - y0) / PT_H

    def light(c):
        return c[0] > 224 and c[1] > 224 and c[2] > 226

    def white(c):
        return c[0] > 250 and c[1] > 250 and c[2] > 250

    cx = int((x0 + x1) / 2)
    sheet_top = None
    for y in range(int(y0 + 40 * sy), int(y1)):
        if light(px[cx, y]):
            sheet_top = y
            break
    if sheet_top is None:
        raise SystemExit("no sheet found")

    # Card edges, sampled well inside the first card to avoid its rounded
    # corners, then confirmed on a few rows.
    margins = []
    card_tops, gaps = [], []
    in_card = False
    last_card_bottom = None
    for y in range(sheet_top, int(y1) - 2, max(1, int(sy))):
        xs = [x for x in range(int(x0) + 2, int(x1) - 2) if white(px[x, y])]
        wide = len(xs) > (x1 - x0) * 0.5
        if wide and not in_card:
            in_card = True
            card_tops.append(round((y - y0) / sy, 1))
            if last_card_bottom is not None:
                gaps.append(round((y - last_card_bottom) / sy, 1))
        elif not wide and in_card:
            in_card = False
            last_card_bottom = y
        if wide:
            margins.append((round((xs[0] - x0) / sx, 1), round((x1 - xs[-1]) / sx, 1)))

    mids = margins[len(margins) // 4: len(margins) * 3 // 4] or margins
    ml = round(sum(m[0] for m in mids) / len(mids), 1)
    mr = round(sum(m[1] for m in mids) / len(mids), 1)

    return {
        "sheet_top": round((sheet_top - y0) / sy, 1),
        "margin_l": ml,
        "margin_r": mr,
        "card_tops": card_tops[:6],
        "card_gaps": [g for g in gaps if g > 1][:5],
    }


def main():
    im = Image.open(sys.argv[1]).convert("RGB")
    for k, v in analyse(im, (0, 0, im.size[0], im.size[1])).items():
        print(f"{k:<11} {v}")


if __name__ == "__main__":
    main()
