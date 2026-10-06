#!/usr/bin/env python3
"""Offline reference/asset checks; these do not compile or execute Android code."""
from pathlib import Path
import json, math, subprocess, xml.etree.ElementTree as ET
import render_atelier as art
root=Path(__file__).resolve().parents[1]
assert len(art.CELLS)==33
assert [sum(r==row for r,c in art.CELLS) for row in range(7)]==[3,3,7,7,7,3,3]
board=art.initial();assert len(board)==32 and art.CENTER not in board
assert len(art.legal(board))==4
moves=[tuple(art.CELLS[int(v)] for v in line.split(',')) for line in (art.OUT/'solution.csv').read_text().splitlines()]
for move in moves:
    before=len(board);board=art.apply(board,move);assert len(board)==before-1
assert len(board)==1 and not art.legal(board)
# Full square cell targets are disjoint, and every gem stays within the playing disc.
side=328;step=side*.81/7;pad=side*.095
for r,c in art.CELLS:
    x=pad+step*(c+.5);y=pad+step*(r+.5)
    assert math.hypot(x-side/2,y-side/2)+step*.405 < side*.48*.925
for p in root.glob('app/src/main/res/**/*.xml'):ET.parse(p)
for p in art.OUT.glob('**/*.svg'):ET.parse(p)
for p in (art.OUT/'previews').glob('*.svg'):
    node=ET.parse(p).getroot();assert node.attrib['width']=='1080' and node.attrib['height']=='2400'
# Main text on the lightest background; primary action text on gold.
def lum(col):
    s=[int(col[i:i+2],16)/255 for i in (1,3,5)];s=[x/12.92 if x<=.04045 else ((x+.055)/1.055)**2.4 for x in s]
    return .2126*s[0]+.7152*s[1]+.0722*s[2]
def contrast(a,b):
    x,y=sorted([lum(a),lum(b)]);return (y+.05)/(x+.05)
ratios={'body_on_surface':contrast('#F6F0E5','#123B3A'),'secondary_on_surface':contrast('#B8CEC7','#123B3A'),'action_on_gold':contrast('#061C20','#EACB88')}
assert min(ratios.values())>=4.5
subprocess.run(['git','diff','--check'],cwd=root,check=True)
baseline='source-snapshot' if subprocess.run(['git','rev-parse','--verify','source-snapshot'],cwd=root,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode==0 else '7ad366e64e6b77621e62101c71d9be6ea52563d9'
subprocess.run(['git','diff','--exit-code',baseline,'--','app/src/main/java/com/spore/gemleap/model/GameState.kt','app/src/main/java/com/spore/gemleap/ui/PebbleController.kt','app/src/main/java/com/spore/gemleap/monetization/BillingManager.kt'],cwd=root,check=True)
report={'result':'PASS','scope':'Offline procedural reference and source-preservation checks only','positions':33,'opening_stones':32,'legal_solution_moves':len(moves),'final_stones':len(board),'contrast_ratios':{k:round(v,2) for k,v in ratios.items()},'android_build':'BLOCKED: Gradle distribution unavailable; no Android SDK/emulator','device_layout_performance_talkback_billing':'NOT RUN'}
(art.OUT/'verification.json').write_text(json.dumps(report,indent=2))
print(json.dumps(report,indent=2))
