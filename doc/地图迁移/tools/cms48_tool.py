# -*- coding: utf-8 -*-
"""CMS v048 -> GMS v053 迁移工具：任务 8516-8527 + NPC 9300011~9300014 / 9330015"""
import io, os, re, sys

ROOT = r'E:\game\ms\gms053\server\gms53-Server'
SRC  = os.path.join(ROOT, 'cms48')
SRV  = os.path.join(ROOT, 'gms-server', 'gms-handler')
QSRC = os.path.join(SRC, 'Quest.wz')
QDST = os.path.join(SRV, 'wz-zh-CN', 'Quest.wz')
MSRC = os.path.join(SRC, 'Map.wz', 'Map')
MDST = os.path.join(SRV, 'wz', 'Map.wz', 'Map')
FILES = ['Check', 'Act', 'QuestInfo', 'Say']
IDS = [str(i) for i in range(8516, 8528)]
MAPS = [
    ('9300011', r'Map1\104000000'),
    ('9330015', r'Map1\101010101'),
    ('9300013', r'Map2\222010201'),
    ('9300014', r'Map2\220070201'),
]
def read(p):
    with io.open(p, 'r', encoding='utf-8-sig', newline='') as f:
        return f.read()

def write(p, s):
    with io.open(p, 'w', encoding='utf-8-sig', newline='') as f:
        f.write(s)

def bare(s):
    return s[:-1] if s.endswith('\r') else s

def block(lines, name):
    for i, ln in enumerate(lines):
        m = re.match(r'^(\s*)<imgdir name="' + re.escape(name) + r'">$', bare(ln))
        if m:
            ind = m.group(1)
            for j in range(i + 1, len(lines)):
                if bare(lines[j]) == ind + '</imgdir>':
                    return i, j
    return None
def life_block(lines, npc):
    """返回 life 下 id==npc 那一条的 (起,止) 行号"""
    for i, ln in enumerate(lines):
        if re.match(r'^\s*<string name="id" value="' + npc + r'"/>$', bare(ln)):
            s = i
            while s >= 0 and not re.match(r'^\s*<imgdir name="\d+">$', bare(lines[s])):
                s -= 1
            e = i
            while e < len(lines) and not re.match(r'^\s*</imgdir>$', bare(lines[e])):
                e += 1
            return s, e
    return None

def fields(lines, s, e):
    d = {}
    for ln in lines[s:e + 1]:
        m = re.match(r'^\s*<(?:int|string) name="([^"]+)" value="([^"]*)"/>$', bare(ln))
        if m:
            d[m.group(1)] = m.group(2)
    return d
def footholds(path):
    """{fh_id: {'x1':..,'y1':..,'x2':..,'y2':..}}"""
    out, stack, cur = {}, [], None
    for ln in read(path).split('\n'):
        t = bare(ln).strip()
        m = re.match(r'^<imgdir name="([^"]+)"[^>]*>$', t)
        if m:
            stack.append(m.group(1))
            if len(stack) >= 5 and stack[-4] == 'foothold':
                cur = m.group(1)
                out[cur] = {}
            continue
        if t == '</imgdir>':
            if cur is not None and stack and stack[-1] == cur:
                cur = None
            if stack:
                stack.pop()
            continue
        m = re.match(r'^<int name="(x1|y1|x2|y2)" value="(-?\d+)"/>$', t)
        if m and cur is not None:
            out[cur][m.group(1)] = int(m.group(2))
    return out
def diag():
    for npc, rel in MAPS:
        sf = os.path.join(MSRC, rel + '.img.xml')
        tf = os.path.join(MDST, rel + '.img.xml')
        sl = read(sf).split('\n')
        r = life_block(sl, npc)
        f = fields(sl, r[0], r[1])
        fs, ft = footholds(sf), footholds(tf)
        g = fs.get(f['fh'], {})
        key = (g.get('x1'), g.get('y1'), g.get('x2'), g.get('y2'))
        same = [k for k, v in ft.items()
                if (v.get('x1'), v.get('y1'), v.get('x2'), v.get('y2')) == key]
        x = int(f['x'])
        cover = [k for k, v in ft.items() if v.get('y1') == g.get('y1')
                 and min(v.get('x1'), v.get('x2')) <= x <= max(v.get('x1'), v.get('x2'))]
        print('%s npc=%s x=%s y=%s cy=%s rx0=%s rx1=%s fh=%s' %
              (rel, npc, f.get('x'), f.get('y'), f.get('cy'), f.get('rx0'), f.get('rx1'), f.get('fh')))
        print('   src_fh_geom=%s  target_same_geom=%s  target_same_y_cover_x=%s' % (key, same, cover))
LIFE = [
    ('9300011', r'Map1\104000000', '235'),
    ('9330015', r'Map1\101010101', '198'),
    ('9300013', r'Map2\222010201', '42'),
    ('9300014', r'Map2\220070201', '3'),
]

def strip_root_attr(t):
    return re.sub(r'^(<imgdir name="[^"]+\.img")[^>]*>', r'\1>', t, count=1, flags=re.M)

def apply_npc(dry=True):
    src = os.path.join(SRC, 'Npc.wz', '9330015.img.xml')
    dst = os.path.join(SRV, 'wz', 'Npc.wz', '9330015.img.xml')
    s = strip_root_attr(read(src))
    print('npc 9330015 ->', dst, len(s))
    if not dry:
        write(dst, s)
def apply_quests(dry=True):
    for f in FILES:
        sl = read(os.path.join(QSRC, f + '.img.xml')).split('\n')
        dl = read(os.path.join(QDST, f + '.img.xml')).split('\n')
        add = []
        for qid in IDS:
            r = block(sl, qid)
            if not r:
                print('  !! source missing', f, qid)
                continue
            b = [ln for ln in sl[r[0]:r[1] + 1]
                 if not re.match(r'^\s*<string name="(start|end)" ', bare(ln))]
            add.append(b)
        r = block(dl, '8515')
        at = r[1] + 1
        new = dl[:at] + [x for b in add for x in b] + dl[at:]
        print('%s: +%d quests, insert at line %d, %d -> %d lines' %
              (f, len(add), at, len(dl), len(new)))
        if not dry:
            write(os.path.join(QDST, f + '.img.xml'), '\n'.join(new))
def apply_life(dry=True):
    for npc, rel, tfh in LIFE:
        sf = os.path.join(MSRC, rel + '.img.xml')
        tf = os.path.join(MDST, rel + '.img.xml')
        sl = read(sf).split('\n')
        r0 = life_block(sl, npc)
        f = fields(sl, r0[0], r0[1])
        lines = read(tf).split('\n')
        a, b = block(lines, 'life')
        ei, idx, cnt = None, 0, 0
        for k in range(a + 1, b):
            m = re.match(r'^(\s*)<imgdir name="(\d+)">$', bare(lines[k]))
            if m:
                cnt += 1
                idx = max(idx, int(m.group(2)) + 1)
                if ei is None:
                    ei = m.group(1)
        if ei is None:
            ei = re.match(r'^(\s*)', bare(lines[a])).group(1) + '    '
        blk = ['%s<imgdir name="%d">' % (ei, idx)]
        for k, v in (('type', 'n'), ('id', npc), ('x', f['x']), ('y', f['y']),
                     ('fh', tfh), ('cy', f['cy']), ('rx0', f['rx0']), ('rx1', f['rx1'])):
            tag = 'string' if k in ('type', 'id') else 'int'
            blk.append('%s    <%s name="%s" value="%s"/>' % (ei, tag, k, v))
        blk.append(ei + '</imgdir>')
        print('%s npc=%s life index=%d fh=%s(源%s) 现有%d条' % (rel, npc, idx, tfh, f['fh'], cnt))
        if not dry:
            write(tf, '\n'.join(lines[:b] + blk + lines[b:]))

if __name__ == '__main__':
    cmd = sys.argv[1] if len(sys.argv) > 1 else 'diag'
    dry = '--apply' not in sys.argv
    if cmd == 'diag':
        diag()
    elif cmd == 'quests':
        apply_quests(dry)
    elif cmd == 'life':
        apply_life(dry)
    elif cmd == 'npc':
        apply_npc(dry)