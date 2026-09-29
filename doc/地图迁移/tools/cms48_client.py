# -*- coding: utf-8 -*-
"""把 cms48 的任务 8516-8527 / NPC 9330015 / 地图 life 写进 v53 客户端 Data"""
import io, json, os, re, shutil, sys
import xml.etree.ElementTree as ET
sys.path.insert(0, r'E:\game\ms\gms053\client-plugins\CMSLauncherLite\tools\quest_zh')
from wzmcp import Mcp, KEY, flatten

CLI  = r'E:\game\ms\gms053\v53-client\Data'
CSRC = r'E:\game\ms\gms053\汉化\cms048\cms-img\Data'
QDX  = r'E:\game\ms\gms053\server\gms53-Server\gms-server\gms-handler\wz-zh-CN\Quest.wz'
FILES = ['Check', 'Act', 'QuestInfo', 'Say']
IDS = [str(i) for i in range(8516, 8528)]
LIFE = [
    (r'Map\Map\Map1\104000000.img', '9300011'),
    (r'Map\Map\Map1\101010101.img', '9330015'),
    (r'Map\Map\Map2\222010201.img', '9300013'),
    (r'Map\Map\Map2\220070201.img', '9300014'),
]

def session(path):
    m = Mcp()
    try:
        m.call('unload_all', {})
    except Exception:
        pass
    m.call('load_files', {'paths': [path], 'key': KEY})
    return m
def read(p):
    with io.open(p, 'r', encoding='utf-8-sig', newline='') as f:
        return f.read()

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

def quest_xml(kind, qid):
    lines = read(os.path.join(QDX, kind + '.img.xml')).split('\n')
    a, b = block(lines, qid)
    return '\n'.join(bare(x) for x in lines[a:b + 1])

def ops_of(root, parent, elem, ops, skip=()):
    for ch in elem:
        nm = ch.get('name')
        if nm in skip:
            continue
        path = (parent + '/' + nm).strip('/')
        if ch.tag == 'imgdir':
            ops.append({'op': 'create_child', 'rootPath': root, 'nodePath': parent,
                        'type': 'LIST', 'name': nm})
            ops_of(root, path, ch, ops, skip)
        else:
            t = 'INT' if ch.tag == 'int' else 'STRING'
            ops.append({'op': 'create_child', 'rootPath': root, 'nodePath': parent,
                        'type': t, 'name': nm, 'value': ch.get('value')})
def apply_quests(dry=True):
    for kind in FILES:
        img = os.path.join(CLI, 'Quest', kind + '.img')
        ops = []
        for qid in IDS:
            ops_of(img, '', ET.fromstring(quest_xml(kind, qid)), ops)
        print('%s: %d quests, %d ops' % (kind, len(IDS), len(ops)))
        if dry:
            continue
        m = session(img)
        res = []
        for i in range(0, len(ops), 200):
            r = m.call('mutate_nodes', {'operations': ops[i:i + 200]})
            res += (r.get('results') or [])
        bad = [x for x in res if x.get('error')]
        print('   results=%d errors=%d %s' % (len(res), len(bad), bad[:2]))
        print('   save:', m.call('save_node', {'rootPath': img}))
def server_life(rel, npc):
    import cms48_tool as T
    sub = rel.replace('Map\\Map\\', '', 1).replace('.img', '.img.xml')
    lines = T.read(os.path.join(T.MDST, sub)).split('\n')
    return T.fields(lines, *T.life_block(lines, npc))

def apply_life(dry=True):
    for rel, npc in LIFE:
        img = os.path.join(CLI, rel)
        f = server_life(rel, npc)
        m = session(img)
        tree = m.call('get_node_tree_json', {'rootPath': img, 'nodePath': 'life', 'maxDepth': 1})['tree']
        kids = tree.get('children') or []
        idx = max([int(k['name']) for k in kids] + [-1]) + 1
        print('%s npc=%s life index=%d 现有%d条' % (rel, npc, idx, len(kids)))
        if dry:
            continue
        ops = [{'op': 'create_child', 'rootPath': img, 'nodePath': 'life',
                'type': 'LIST', 'name': str(idx)}]
        for k in ('type', 'id', 'x', 'y', 'fh', 'cy', 'rx0', 'rx1'):
            t = 'STRING' if k in ('type', 'id') else 'INT'
            v = 'n' if k == 'type' else (npc if k == 'id' else f[k])
            ops.append({'op': 'create_child', 'rootPath': img, 'nodePath': 'life/%d' % idx,
                        'type': t, 'name': k, 'value': v})
        r = m.call('mutate_nodes', {'operations': ops})
        print('   %s' % [(x.get('name'), x.get('error')) for x in (r.get('results') or []) if x.get('error')][:3])
        print('   save:', m.call('save_node', {'rootPath': img}))
def apply_npc(dry=True):
    s = os.path.join(CSRC, 'Npc', '9330015.img')
    d = os.path.join(CLI, 'Npc', '9330015.img')
    print('npc img %s -> %s  (%s)' % (s, d, os.path.getsize(s)))
    if not dry:
        shutil.copy2(s, d)

def check_strings():
    m = session(os.path.join(CLI, 'String', 'Npc.img'))
    t = m.call('get_node_tree_json', {'rootPath': os.path.join(CLI, 'String', 'Npc.img'),
                                      'nodePath': '9330015', 'maxDepth': 4})['tree']
    print('client String/Npc.img 9330015:', json.dumps(t, ensure_ascii=False)[:600])

if __name__ == '__main__':
    cmd = sys.argv[1] if len(sys.argv) > 1 else 'dry'
    dry = '--apply' not in sys.argv
    if cmd == 'quests':
        apply_quests(dry)
    elif cmd == 'life':
        apply_life(dry)
    elif cmd == 'npc':
        apply_npc(dry)
    elif cmd == 'strings':
        check_strings()
def flat_xml(elem, parent=''):
    out = {}
    for ch in elem:
        nm, path = ch.get('name'), (parent + '/' + ch.get('name')).strip('/')
        if ch.tag == 'imgdir':
            out.update(flat_xml(ch, path))
        else:
            out[path] = ch.get('value')
    return out

def verify():
    bad_all = 0
    for kind in FILES:
        img = os.path.join(CLI, 'Quest', kind + '.img')
        m = session(img)
        for qid in IDS:
            exp = flat_xml(ET.fromstring(quest_xml(kind, qid)))
            tree = m.call('get_node_tree_json', {'rootPath': img, 'nodePath': qid, 'maxDepth': 8})['tree']
            got = flatten(tree, {}, qid)
            bad = [k for k, v in exp.items() if str(got.get(k)) != str(v)]
            bad_all += len(bad)
            if bad:
                print('  BAD %s/%s %s' % (kind, qid, bad[:5]))
        print('%s: 12 quests 校验完' % kind)
    for rel, npc in LIFE:
        img = os.path.join(CLI, rel)
        m = session(img)
        tree = m.call('get_node_tree_json', {'rootPath': img, 'nodePath': 'life', 'maxDepth': 2})['tree']
        hit = [k for k in (tree.get('children') or []) if any(c.get('value') == npc for c in (k.get('children') or []))]
        print('%s: %s -> %s' % (rel, npc, [k['name'] for k in hit]))
    print('BAD total =', bad_all)

if __name__ == '__main__':
    pass
def life_ops(m, img, rel, npc):
    f = server_life(rel, npc)
    tree = m.call('get_node_tree_json', {'rootPath': img, 'nodePath': 'life', 'maxDepth': 1})['tree']
    kids = tree.get('children') or []
    idx = max([int(k['name']) for k in kids] + [-1]) + 1
    ops = [{'op': 'create_child', 'rootPath': img, 'nodePath': 'life',
            'type': 'LIST', 'name': str(idx)}]
    for k in ('type', 'id', 'x', 'y', 'fh', 'cy', 'rx0', 'rx1'):
        t = 'STRING' if k in ('type', 'id') else 'INT'
        v = 'n' if k == 'type' else (npc if k == 'id' else f[k])
        ops.append({'op': 'create_child', 'rootPath': img, 'nodePath': 'life/%d' % idx,
                    'type': t, 'name': k, 'value': v})
    return ops, idx

def redo(dry=True):
    jobs = [('Check', os.path.join(CLI, 'Quest', 'Check.img'), None, None)]
    for rel, npc in LIFE:
        jobs.append((None, os.path.join(CLI, rel), rel, npc))
    for kind, img, rel, npc in jobs:
        m = session(img)
        if kind:
            ops = []
            for qid in IDS:
                ops_of(img, '', ET.fromstring(quest_xml(kind, qid)), ops)
            tag = kind
        else:
            ops, tag = life_ops(m, img, rel, npc)
            tag = 'life %s idx=%d' % (rel, tag)
        r = m.call('mutate_nodes', {'operations': ops})
        errs = [x for x in (r.get('results') or []) if x.get('error')]
        print('%s: %d ops errors=%d %s' % (os.path.basename(img), len(ops), len(errs), tag))
        if dry:
            continue
        if os.path.exists(img + '.new.img'):
            os.remove(img + '.new.img')
        print('   save_as:', m.call('save_as', {'rootPath': img, 'filePath': img + '.new.img'}))
def verify_files():
    bad = 0
    img = os.path.join(CLI, 'Quest', 'Check.img.new.img')
    m = session(img)
    for qid in IDS:
        exp = flat_xml(ET.fromstring(quest_xml('Check', qid)))
        tree = m.call('get_node_tree_json', {'rootPath': img, 'nodePath': qid, 'maxDepth': 8})['tree']
        got = flatten(tree, {}, qid)
        b = [k for k, v in exp.items() if str(got.get(k)) != str(v)]
        bad += len(b)
        if b:
            print('  BAD Check/%s %s' % (qid, b[:4]))
    print('Check.img.new.img: %d quests checked' % len(IDS))
    for rel, npc in LIFE:
        p2 = os.path.join(CLI, rel + '.new.img')
        m2 = session(p2)
        tree = m2.call('get_node_tree_json', {'rootPath': p2, 'nodePath': 'life', 'maxDepth': 2})['tree']
        hit = [k for k in (tree.get('children') or [])
               if any(str(c.get('value')) == npc for c in (k.get('children') or []))]
        print('%s: npc %s -> life %s' % (rel, npc, [k['name'] for k in hit]))
        bad += 0 if hit else 1
    print('BAD total =', bad)
def putfile(src, dst):
    data = open(src, 'rb').read()
    with open(dst, 'r+b') as f:
        f.truncate(0)
        f.seek(0)
        f.write(data)
    return len(data)

def restore():
    bk = r'E:\game\ms\gms053\地图迁移\备份\20260929-123501-8516-8527与NPC9330015\客户端'
    for f in FILES:
        n = putfile(os.path.join(bk, 'Quest', f + '.img'), os.path.join(CLI, 'Quest', f + '.img'))
        print('restored Quest/%s.img  %d' % (f, n))
    for rel, _ in LIFE:
        base = rel.replace('\\', '_')
        n = putfile(os.path.join(bk, 'Map', base), os.path.join(CLI, rel))
        print('restored %s  %d' % (rel, n))
def apply_all(dry=True):
    for kind in FILES:
        img = os.path.join(CLI, 'Quest', kind + '.img')
        m = session(img)
        t = m.call('get_node_tree_json', {'rootPath': img, 'maxDepth': 1})['tree']
        names = [c['name'] for c in (t.get('children') or [])]
        dels = [n for n in ('0', '1') if n in names]
        ops = []
        for qid in IDS:
            ops.append({'op': 'create_child', 'rootPath': img, 'nodePath': '',
                        'type': 'LIST', 'name': qid})
            ops_of(img, qid, ET.fromstring(quest_xml(kind, qid)), ops)
        print('%s: 清理%s ops=%d' % (kind, dels, len(ops)))
        if dry:
            continue
        if dels:
            m.call('mutate_nodes', {'operations': [
                {'op': 'delete', 'rootPath': img, 'nodePath': n} for n in dels]})
        r = m.call('mutate_nodes', {'operations': ops})
        errs = [x for x in (r.get('results') or []) if x.get('error')]
        print('   errors=%d save=%s' % (len(errs), m.call('save_node', {'rootPath': img})))
    for rel, npc in LIFE:
        img = os.path.join(CLI, rel)
        m = session(img)
        ops, idx = life_ops(m, img, rel, npc)
        print('%s: life idx=%d ops=%d' % (rel, idx, len(ops)))
        if dry:
            continue
        r = m.call('mutate_nodes', {'operations': ops})
        errs = [x for x in (r.get('results') or []) if x.get('error')]
        print('   errors=%d save=%s' % (len(errs), m.call('save_node', {'rootPath': img})))
SCRATCH = os.path.join(os.path.dirname(os.path.abspath(__file__)), '_scratch')

def verify_baks(tag='v'):
    bad = 0
    for kind in FILES:
        src = os.path.join(CLI, 'Quest', kind + '.img.bak')
        dst = os.path.join(SCRATCH, tag + '_' + kind + '.img')
        open(dst, 'wb').write(open(src, 'rb').read())
        m = session(dst)
        t = m.call('get_node_tree_json', {'rootPath': dst, 'maxDepth': 1})['tree']
        names = [c['name'] for c in (t.get('children') or [])]
        stray = [n for n in ('0', '1') if n in names]
        print('%s: children=%d 8516=%s 8527=%s 误建=%s' % (kind, len(names), '8516' in names, '8527' in names, stray))
        bad += len(stray)
        for qid in IDS:
            exp = flat_xml(ET.fromstring(quest_xml(kind, qid)))
            tr = m.call('get_node_tree_json', {'rootPath': dst, 'nodePath': qid, 'maxDepth': 8})['tree']
            got = flatten(tr, {}, qid)
            b = [k for k, v in exp.items() if str(got.get(k)) != str(v)]
            bad += len(b)
            if b:
                print('   BAD %s/%s %s' % (kind, qid, b[:4]))
    for rel, npc in LIFE:
        src = os.path.join(CLI, rel + '.bak')
        dst = os.path.join(SCRATCH, tag + '_' + rel.replace('\\', '_'))
        open(dst, 'wb').write(open(src, 'rb').read())
        m = session(dst)
        t = m.call('get_node_tree_json', {'rootPath': dst, 'nodePath': 'life', 'maxDepth': 2})['tree']
        hit = [(k['name'], dict((c['name'], c.get('value')) for c in k['children']))
               for k in (t.get('children') or [])
               if any(str(c.get('value')) == npc for c in (k.get('children') or []))]
        print('%s: %s -> %s' % (rel, npc, hit))
        bad += 0 if hit else 1
    print('BAD total =', bad)
def tree_vals(node, out, base=''):
    p = node.get('nodePath', '')
    rel = p[len(base):].strip('/') if base and p.startswith(base) else p
    t = node.get('type')
    if t in ('INT_PROPERTY', 'STRING_PROPERTY', 'FLOAT_PROPERTY'):
        out[rel] = str(node.get('value'))
    for c in (node.get('children') or []):
        tree_vals(c, out, base)
    return out

def verify_baks2(tag='w'):
    bad = 0
    for kind in FILES:
        src = os.path.join(CLI, 'Quest', kind + '.img.bak')
        dst = os.path.join(SCRATCH, tag + '_' + kind + '.img')
        open(dst, 'wb').write(open(src, 'rb').read())
        m = session(dst)
        for qid in IDS:
            exp = flat_xml(ET.fromstring(quest_xml(kind, qid)))
            tr = m.call('get_node_tree_json', {'rootPath': dst, 'nodePath': qid, 'maxDepth': 9})['tree']
            got = tree_vals(tr, {}, qid)
            b = [k for k, v in exp.items() if got.get(k) != v]
            extra = [k for k in got if k not in exp]
            bad += len(b) + len(extra)
            if b or extra:
                print('  BAD %s/%s missing=%s extra=%s' % (kind, qid, b[:5], extra[:5]))
        print('%s: 12 quests 对比完' % kind)
    print('BAD total =', bad)