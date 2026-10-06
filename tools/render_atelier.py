#!/usr/bin/env python3
"""Free, deterministic SVG reference art. Not Android screenshots.
Run with Python 3; PNG export additionally uses installed Inkscape.
Geometry, palette and texture formulae mirror MineralArt.kt / BoardGeometry.
"""
from pathlib import Path
import math, json, subprocess, argparse, html
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/jewel-atelier'
ASSETS=OUT/'assets'; PREV=OUT/'previews'
ASSETS.mkdir(parents=True,exist_ok=True); PREV.mkdir(parents=True,exist_ok=True)
GOLD='#EACB88'; TEXT='#F6F0E5'; MUTED='#B8CEC7'
COLORS=['#20B984','#387CE0','#42C6C5','#E1A244','#9673D6','#E4E2CB']
NAMES=['malachite','lapis','turquoise','amber','amethyst','pearl']
CELLS=[(r,c) for r in range(7) for c in range(7) if 2<=r<=4 or 2<=c<=4]
CENTER=(3,3)

def shade(h,f):
    v=[int(h[i:i+2],16) for i in (1,3,5)]
    return '#'+''.join(f'{round(x+(255-x)*f if f>=0 else x*(1+f)):02x}' for x in v)
def circle(x,y,r,fill,extra=''):return f'<circle cx="{x}" cy="{y}" r="{r}" fill="{fill}" {extra}/>'
def rect(x,y,w,h,fill,rx=0,extra=''):return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" fill="{fill}" {extra}/>'
def text(x,y,t,size=14,fill=TEXT,anchor='start',weight=400,extra=''):
    return f'<text x="{x}" y="{y}" font-family="DejaVu Sans,sans-serif" font-size="{size}" fill="{fill}" text-anchor="{anchor}" font-weight="{weight}" {extra}>{html.escape(str(t))}</text>'
def path(d,stroke,width=1,extra=''):return f'<path d="{d}" fill="none" stroke="{stroke}" stroke-width="{width}" stroke-linecap="round" stroke-linejoin="round" {extra}/>'
def band(i):
    return ' '.join(('M' if j==0 else 'L')+f'{-1.2+j*.06:.3f},{-1.1+i*.27+math.sin((-1.2+j*.06)*4+i*.7)*.13:.3f}' for j in range(41))
def vein(i):return f'M-1,{-0.75+i*.48}C-.5,{-0.9+i*.5} -.1,{.3-i*.2} 1,{-.4+i*.44}'
def defs():
    s='''<defs><linearGradient id="bg" x2="0" y2="1"><stop stop-color="#123B3A"/><stop offset="1" stop-color="#061C20"/></linearGradient>
<linearGradient id="rim" x2="1" y2="1"><stop stop-color="#AD8052"/><stop offset=".5" stop-color="#523527"/><stop offset="1" stop-color="#201811"/></linearGradient>
<radialGradient id="wood" cx="32%" cy="29%" r="85%"><stop stop-color="#76503B"/><stop offset="1" stop-color="#38271E"/></radialGradient>
<radialGradient id="hole" cx="50%" cy="35%" r="65%"><stop stop-color="#100F0D"/><stop offset="1" stop-color="#241D16"/></radialGradient>
<clipPath id="unit"><circle r="1"/></clipPath>'''
    for i,col in enumerate(COLORS):
        s+=f'<radialGradient id="gem{i}" cx="32%" cy="29%" r="83%"><stop stop-color="{shade(col,.38)}"/><stop offset=".5" stop-color="{col}"/><stop offset="1" stop-color="{shade(col,-.64)}"/></radialGradient>'
    return s+'</defs>'

def gem(x,y,r,i,selected=False):
    s=circle(x+r*.12,y+r*.23,r*.97,'#000', 'opacity=".36"')
    if selected:s+=circle(x,y,r*1.22,'none',f'stroke="{GOLD}" stroke-width="{r*.065}"')
    s+=f'<g transform="translate({x},{y-r*.09 if selected else y}) scale({r})"><g clip-path="url(#unit)">'+circle(0,0,1,f'url(#gem{i})')
    if i==0:
        for j in range(9):s+=path(band(j),'#063F32' if j%2==0 else '#9DF2C4',.07,'opacity=".30"')
    elif i==1:
        for j in range(17):s+=circle((j*37%91)/50-.9,(j*61%87)/50-.86,.028 if j%3==0 else .015,GOLD,'opacity=".65"')
    elif i==2:
        for j in range(4):s+=path(vein(j),'#165D60',.023,'opacity=".38"')
    elif i==3:
        s+=circle(-.17,-.05,.6,'#FFE6A1','opacity=".24"')
        for j in range(2):s+=path(vein(j),'#7A380E',.05,'opacity=".19"')
    elif i==4:
        for j in range(0,9,2):s+=path(band(j),'#E6CEFF',.13,'opacity=".18"')
    else:s+=circle(-.15,-.25,.65,'#fff','opacity=".12"')
    s+='<ellipse cx="-.29" cy="-.56" rx=".34" ry=".16" fill="white" opacity=".33"/><ellipse cx="-.355" cy="-.575" rx=".135" ry=".075" fill="white" opacity=".78"/>'
    s+=path('M.65,.46Q.45,.80 .03,.80',shade(COLORS[i],.4),.055,'opacity=".3"')
    return s+'</g>'+circle(0,0,1,'none',f'stroke="{shade(COLORS[i],-.52)}" stroke-width=".035"')+'</g>'

def socket(x,y,r,target=False):
    s=circle(x,y+r*.05,r*1.08,'#997453','opacity=".7"')+circle(x,y,r,'url(#hole)')
    if target:s+=circle(x,y,r*1.12,'none',f'stroke="#8BF0C4" stroke-width="{r*.11}"')+circle(x,y,r*.22,'#8BF0C4')
    return s

def board_base(cx,cy,r):
    s=circle(cx,cy+r*.028,r*1.018,'#000','opacity=".18"')+circle(cx,cy,r,'url(#rim)')
    s+=circle(cx,cy,r*.969,'none',f'stroke="{GOLD}" stroke-width="{r*.006}" opacity=".58"')
    s+=circle(cx,cy,r*.925,'url(#wood)')
    s+=f'<g transform="translate({cx},{cy}) scale({r*.925})" clip-path="url(#unit)">'
    for i in range(24):
        d=' '.join(('M' if j==0 else 'L')+f'{-1+j/24:.3f},{-1+i/12+math.sin((-1+j/24)*5+i*.51)*.024:.3f}' for j in range(49))
        s+=path(d,'#fff' if i%3==0 else '#000',.003,f'opacity="{.035 if i%3==0 else .09}"')
    s+='</g>'+circle(cx,cy,r*.925,'none',f'stroke="#000" stroke-width="{r*.012}" opacity=".32"')
    return s

def initial(empty=CENTER,seed=0):
    return {p: (((p[0]*73856093)^(p[1]*19349663)^(p[0]*p[1]*83492791)^((seed*-0x61c88647)&0xffffffff))&0x7fffffff)%14%6 for p in CELLS if p!=empty}
def legal(state):
    out=[]
    for r,c in CELLS:
        if (r,c) not in state:continue
        for dr,dc in [(-1,0),(1,0),(0,-1),(0,1)]:
            over=(r+dr,c+dc);to=(r+2*dr,c+2*dc)
            if over in state and to in CELLS and to not in state:out.append(((r,c),over,to))
    return out
def apply(state,m):
    assert m in legal(state),m
    s=dict(state);g=s.pop(m[0]);s.pop(m[1]);s[m[2]]=g;return s

def board(state,x=16,y=251,side=328,selected=None,target=None,hint=False):
    cx=x+side/2;cy=y+side/2;step=side*.81/7;pad=side*.095;r=step*.405
    s=board_base(cx,cy,side*.48)
    for rr,cc in CELLS:
        px=x+pad+step*(cc+.5);py=y+pad+step*(rr+.5)
        s+=socket(px,py,step*.385,(rr,cc)==target)
    for (rr,cc),i in state.items():
        px=x+pad+step*(cc+.5);py=y+pad+step*(rr+.5)
        s+=gem(px,py,r,i,(rr,cc)==selected)
    if hint and selected:
        for rr,cc in [selected,target]:s+=circle(x+pad+step*(cc+.5),y+pad+step*(rr+.5),r*1.23,'none','stroke="#8BF0C4" stroke-width="1"')
    if len(state)==1:
        rr,cc=next(iter(state));s+=circle(x+pad+step*(cc+.5),y+pad+step*(rr+.5),r*1.4,'none',f'stroke="{GOLD}" stroke-width=".7"')
    return s

ICONS={
 'settings':'M0,-7V-10M0,7V10M7,0H10M-7,0H-10M5,-5L7,-7M-5,5L-7,7M-5,-5L-7,-7M5,5L7,7 M6,0A6,6 0 1,0 -6,0A6,6 0 1,0 6,0 M2,0A2,2 0 1,0 -2,0A2,2 0 1,0 2,0',
 'sound':'M-8,-3H-4L1,-7V7L-4,3H-8Z M5,-4Q9,0 5,4M8,-7Q15,0 8,7',
 'mute':'M-8,-3H-4L1,-7V7L-4,3H-8Z M5,-3L11,3M11,-3L5,3',
 'stats':'M-7,6V-1M0,6V-7M7,6V-4',
 'hint':'M-4,3C-12,-7 12,-7 4,3L3,6H-3Z M-3,9H3',
 'undo':'M-8,-3H2Q10,-3 10,5 M-3,-8L-8,-3L-3,2',
 'restart':'M6,-6A9,9 0 1,0 9,3 M6,-11V-5H12',
 'check':'M-7,0L-2,5L8,-6'}
def icon(name,x,y,col=GOLD,scale=1):return f'<g transform="translate({x},{y}) scale({scale})">'+path(ICONS[name],col,1.6)+'</g>'
def button(x,y,w,h,label,primary=False,enabled=True,ico=None):
    fill=GOLD if primary else '#123B3A'; fg='#061C20' if primary else TEXT
    if not enabled:fill='#173236';fg='#829893'
    s=rect(x,y,w,h,fill,18,extra='' if primary else 'stroke="#3B5A54" stroke-width=".7"')
    if ico:s+=icon(ico,x+w/2,y+22,fg,.9)+text(x+w/2,y+h-13,label,13,fg,'middle',500)
    else:s+=text(x+w/2,y+h/2+5,label,14,fg,'middle',600)
    return s

def screen(state,kind='free',selected=None,target=None,hint=False,price=None):
    daily=kind in ['daily','daily_solved'];win=len(state)==1;stuck=not legal(state) and not win
    s=rect(0,0,360,800,'url(#bg)')
    s+=icon('settings',37,39)+icon('sound',322,39)
    s+=text(180,42,'GemLeap',27,TEXT,'middle',600)+text(180,59,'ONE STONE. A LITTLE WONDER.',8,MUTED,'middle',500,'letter-spacing="1"')
    s+=button(16,85,136,48,'Free',not daily)+button(160,85,136,48,'Daily',daily)+icon('stats',325,109)
    s+=text(28,166,'STONES LEFT',10,MUTED,extra='letter-spacing="1"')+text(28,201,len(state),30,GOLD)
    s+=text(332,166,'DAILY STREAK' if daily else 'WIN STREAK',10,MUTED,'end',extra='letter-spacing="1"')+text(332,201,'1' if kind=='daily_solved' else '0',30,GOLD,'end')
    sub='Tuesday, Oct 6' + (' · Solved ✓' if kind=='daily_solved' else '') if daily else 'A quiet moment. A clever move.'
    s+=text(180,232,sub,12,MUTED,'middle')+board(state,selected=selected,target=target,hint=hint)
    if win or stuck:
        s+=rect(16,590,328,77,'#123B3A',20,'stroke="#46645C" stroke-width=".7"')
        s+=text(180,620,'Beautifully solved' if win else 'No moves remaining',21,GOLD,'middle',600)
        s+=text(180,643,'One stone remains. A perfect finish.' if win else f'{len(state)} stones remain. Undo or try again.',12,MUTED,'middle')
    else:s+=text(180,607,'Follow the marked stone to its destination' if hint else 'Tap a marked socket to leap' if selected else 'Jump a stone. Leave just one.',12,MUTED,'middle')
    s+=button(16,688,104,68,'Hint',enabled=not(win or stuck),ico='hint')
    s+=button(128,688,104,68,'Undo',enabled=len(state)<32,ico='undo')
    s+=button(240,688,104,68,'Retry' if daily or stuck else 'New',primary=True,ico='restart')
    s+=text(180,784,'Remove ads · '+price if price else 'Remove ads · Price unavailable',11,MUTED if price else '#829893','middle')
    return s

def dialog(title,body,height=350):
    y=(800-height)/2
    s=rect(0,0,360,800,'#001112',extra='opacity=".77"')+rect(24,y,312,height,'#123B3A',24,'stroke="#46645C" stroke-width=".7"')
    return s+text(48,y+42,title,22,TEXT,weight=600)+body(y)

def theme_thumbnail(index,x,y,side):
    b=board(initial(),x,y,side)
    if index == 0:return b
    palettes=[[],['#F1EEE7','#1F7A4D','#3FC0C4','#27509B','#8E5AB8','#9A6B2F'],['#E6B84C','#2E8B8B','#3E5C86','#6E8B6A','#6E5A86','#D8D2C4'],['#1C1A18','#E8DEC8','#B08D4C','#7E2B2B','#2F6B4A','#32507E']]
    d=defs()
    for old,new in zip(COLORS,palettes[index]):
        d=d.replace(old,new)
        for factor in [0,.38,-.64,-.52,.4]:d=d.replace(shade(old,factor),shade(new,factor))
    if index==2:d=d.replace('#AD8052','#3A3F48').replace('#523527','#2A2D34').replace('#76503B','#2E323A').replace('#38271E','#16181E')
    if index==3:d=d.replace('#AD8052','#6E5A3E').replace('#523527','#4E3F2A').replace('#76503B','#5A4A32').replace('#38271E','#342916')
    for name in ['rim','wood','hole','unit']+[f'gem{i}' for i in range(6)]:
        d=d.replace(f'id="{name}"',f'id="t{index}-{name}"').replace(f'url(#{name})',f'url(#t{index}-{name})')
        b=b.replace(f'url(#{name})',f'url(#t{index}-{name})')
    return d+b

def settings(y):
    s=text(48,y+70,'Choose your board',12,MUTED)
    for j,(label,col) in enumerate([('Jewel Atelier',GOLD),('Classic Wood','#BB8954'),('Lamplight','#657078'),('Victorian','#837356')]):
        yy=y+86+j*78
        s+=rect(40,yy,280,70,'#102E2E',16,f'stroke="{GOLD if j==0 else "#2E4D45"}" stroke-width="1"')
        s+=theme_thumbnail(j,44,yy+3,64)+text(122,yy+39,label,13,TEXT,weight=500)
        if j==0:s+=icon('check',302,yy+35)
    s+=text(48,y+430,'Sound',14)+rect(264,y+412,48,27,GOLD,14)+circle(298,y+425.5,10,'#123B3A')
    return s+text(294,y+479,'Done',14,GOLD,'end',600)
def statistics(y,empty=False):
    s=''
    if empty:
        s+=text(48,y+76,'Your first finish starts the story.',12,MUTED)+text(48,y+95,'Play a board to see your progress.',12,MUTED)
    else:s+=text(48,y+72,'Sample statistics',11,MUTED)
    s+=text(48,y+127,'WIN RATE',10,MUTED,extra='letter-spacing="1"')+text(48,y+163,'—' if empty else '60%',30,GOLD)
    labels=['Games won','Games played','Best result','Current streak','Best streak','Daily streak','Best daily streak','Ads']
    values=['0','0','—','0','0','0','0','On'] if empty else ['12','20','1 left','2','4','3','5','On']
    for j,(label,val) in enumerate(zip(labels,values)):
        s+=text(48,y+199+j*29,label,12,MUTED)+text(310,y+199+j*29,val,13,TEXT,'end',600)
    s+=text(48,y+467,'Restore purchases',12,GOLD)+text(310,y+467,'Close',13,GOLD,'end',600)
    return s

def hint_body(y,kind):
    lines={'reward':['Watch a short ad to reveal','one legal move.'], 'loading':['An ad is still loading. You can','get a free hint now.'], 'unavailable':['No ad is available.','This hint is on us.']}[kind]
    s=icon('hint',180,y+78,GOLD,1.3)
    for j,l in enumerate(lines):s+=text(180,y+115+j*21,l,13,MUTED,'middle')
    s+=button(48,y+165,264,48,'Watch ad for hint' if kind=='reward' else 'Get free hint',True)
    return s+text(180,y+244,'Keep thinking',13,GOLD,'middle')

def svg(body,w=360,h=800,pxw=None,pxh=None):
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{pxw or w}" height="{pxh or h}" viewBox="0 0 {w} {h}">'+defs()+body+'</svg>'
def save(path,body,w=360,h=800,pxw=None,pxh=None):path.write_text(svg(body,w,h,pxw,pxh))

def run(png=False):
    start=initial();selected=(1,3);target=CENTER
    assert ((1,3),(2,3),CENTER) in legal(start)
    win=start
    for line in (OUT/'solution.csv').read_text().splitlines():win=apply(win,tuple(CELLS[int(v)] for v in line.split(',')))
    assert len(win)==1
    stuck=start
    while legal(stuck):stuck=apply(stuck,legal(stuck)[0])
    assert len(stuck)>1 and not legal(stuck)
    # Match LocalDate epoch and GameState's existing deterministic hash.
    import datetime
    day=(datetime.date(2026,10,6)-datetime.date(1970,1,1)).days
    daily=initial(CELLS[day%33],day)
    scenes={
        '01-free':screen(start),
        '02-selected':screen(start,selected=selected,target=target),
        '03-hint':screen(start,selected=selected,target=target,hint=True),
        '04-daily':screen(daily,'daily'),
        '05-success':screen(win),
        '06-no-moves':screen(stuck),
        '07-settings':screen(start)+dialog('Make it yours',settings,510),
        '08-statistics':screen(start)+dialog('Your quiet progress',statistics,500),
        '09-statistics-empty':screen(start)+dialog('Your quiet progress',lambda y:statistics(y,True),500),
        '10-hint-ad-ready':screen(start)+dialog('A little nudge',lambda y:hint_body(y,'reward'),280),
        '11-hint-loading':screen(start)+dialog('A little nudge',lambda y:hint_body(y,'loading'),280),
        '12-hint-unavailable':screen(start)+dialog('A little nudge',lambda y:hint_body(y,'unavailable'),280),
        '13-daily-solved':screen(win,'daily_solved'),
        '14-price-unavailable':screen(start),
    }
    for name,body in scenes.items():save(PREV/(name+'.svg'),body,pxw=1080,pxh=2400)
    for i,name in enumerate(NAMES):save(ASSETS/('gem-'+name+'.svg'),gem(64,62,48,i),128,128,512,512)
    save(ASSETS/'board-base.svg',board_base(256,256,240),512,512,1024,1024)
    save(ASSETS/'socket.svg',socket(64,64,48),128,128,256,256)
    save(ASSETS/'marker-destination.svg',socket(64,64,48,True),128,128,256,256)
    save(ASSETS/'marker-selection.svg',circle(64,64,52,'none',f'stroke="{GOLD}" stroke-width="3"'),128,128,256,256)
    save(ASSETS/'marker-hint.svg',circle(64,64,52,'none','stroke="#8BF0C4" stroke-width="3"'),128,128,256,256)
    for j,name in enumerate(['atelier','classic','lamplight','victorian']):save(ASSETS/('theme-'+name+'.svg'),theme_thumbnail(j,0,0,128),128,128,256,256)
    for name in ICONS:save(ASSETS/('icon-'+name+'.svg'),icon(name,16,16),32,32,96,96)
    for name,enabled,primary in [('normal',True,False),('pressed',True,True),('disabled',False,False)]:
        save(ASSETS/('button-'+name+'.svg'),rect(0,0,104,68,GOLD if primary else '#123B3A' if enabled else '#173236',18),104,68,312,204)
    save(ASSETS/'button-loading.svg',rect(0,0,104,68,'#123B3A',18)+path('M62,34A10,10 0 1,0 52,44',GOLD,2),104,68,312,204)
    # Icon uses the same 108-unit adaptive foreground layout as the Android vector.
    leap=path('M33,54C33,21 71,22 75,50',GOLD,2.2)+path('M70,46L75,51L78,45',GOLD,2)
    leap+='<ellipse cx="71" cy="62" rx="9" ry="6" fill="#071B1C" stroke="#A88750" stroke-width="1.5"/>'
    leap+=gem(55,63,9,1)+gem(40,44,13,0)
    iconbg=rect(0,0,108,108,'#092B2B')+'<path d="M0,0H108V48C70,20 30,70 0,46Z" fill="#123B3A"/>'
    save(ASSETS/'launcher-foreground.svg',leap,108,108,432,432)
    save(ASSETS/'launcher-background.svg',iconbg,108,108,432,432)
    save(ASSETS/'launcher-icon.svg',iconbg+leap,108,108,512,512)
    for shape,clip in [('circle','<circle cx="54" cy="54" r="45"/>'),('rounded','<rect x="9" y="9" width="90" height="90" rx="22"/>')]:
        save(ASSETS/('launcher-mask-'+shape+'.svg'),f'<defs><clipPath id="mask">{clip}</clipPath></defs><g clip-path="url(#mask)">'+iconbg+leap+'</g>',108,108,216,216)
    import xml.etree.ElementTree as ET
    ns='{http://schemas.android.com/apk/res/android}'
    mono=ET.parse(ROOT/'app/src/main/res/drawable/ic_launcher_monochrome.xml').getroot()
    mono_body=''
    for p in mono:
        attrs={'d':p.get(ns+'pathData'), 'fill':p.get(ns+'fillColor','#00000000')}
        if attrs['fill']=='#00000000':attrs['fill']='none'
        for k,v in [('strokeColor','stroke'),('strokeWidth','stroke-width'),('strokeLineCap','stroke-linecap')]:
            if p.get(ns+k):attrs[v]=p.get(ns+k)
        mono_body+='<path '+' '.join(f'{k}="{v}"' for k,v in attrs.items())+'/>'
    save(ASSETS/'launcher-monochrome.svg',mono_body,108,108,432,432)
    manifest={'kind' :'Procedural design references, not Android screenshots','screen_size':[1080,2400], 'cells':CELLS,'opening_count':len(start),'winning_count':len(win),'stuck_count':len(stuck),'selected_move':[[1,3],[2,3],[3,3]],'palette':dict(zip(NAMES,COLORS)), 'screens':list(scenes)}
    (OUT/'manifest.json').write_text(json.dumps(manifest,indent=2))
    if png:
        for f in sorted(PREV.glob('*.svg'))+sorted(ASSETS.glob('*.svg')):
            subprocess.run(['inkscape',str(f),'--export-type=png','--export-filename='+str(f.with_suffix('.png'))],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    print(json.dumps({'screens':len(scenes),'assets':len(list(ASSETS.glob('*.svg'))),'board_positions':len(CELLS),'winning_stones':len(win),'stuck_stones':len(stuck)}))
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--png',action='store_true');run(parser.parse_args().png)
