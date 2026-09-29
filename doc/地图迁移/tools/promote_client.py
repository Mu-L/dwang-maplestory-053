# -*- coding: utf-8 -*-
"""把 OrzRepacker 写出来的 .bak 落盘到客户端（需先关掉 OrzRepacker 释放占用）。
   用 md5 校验落盘结果，并清掉 .new.img / 临时文件。"""
import hashlib, os, sys

CLI = r'E:\game\ms\gms053\v53-client\Data'
TARGETS = [os.path.join(CLI, 'Quest', f + '.img') for f in ('Check', 'Act', 'QuestInfo', 'Say')] + [
    os.path.join(CLI, r'Map\Map\Map1\104000000.img'),
    os.path.join(CLI, r'Map\Map\Map1\101010101.img'),
    os.path.join(CLI, r'Map\Map\Map2\222010201.img'),
    os.path.join(CLI, r'Map\Map\Map2\220070201.img'),
]

def md5(p):
    return hashlib.md5(open(p, 'rb').read()).hexdigest()

def main():
    ok = True
    for t in TARGETS:
        bak = t + '.bak'
        if not os.path.exists(bak):
            print('没有 .bak：%s' % t)
            ok = False
            continue
        want = md5(bak)
        data = open(bak, 'rb').read()
        try:
            with open(t, 'r+b') as f:
                f.truncate(0)
                f.seek(0)
                f.write(data)
        except Exception as ex:
            try:
                os.remove(t)
                open(t, 'wb').write(data)
            except Exception as ex2:
                print('落盘失败 %s : %s / %s' % (t, ex, ex2))
                ok = False
                continue
        got = md5(t)
        print('%s  %d 字节  md5 %s  %s' % (os.path.basename(t), len(data), got[:12], 'OK' if got == want else '!!!不一致'))
        ok = ok and got == want
        os.remove(bak)
    # 清理我的临时文件
    for t in TARGETS:
        for junk in (t + '.new.img',):
            if os.path.exists(junk):
                os.remove(junk)
                print('删掉临时文件 %s' % junk)
    print('全部成功' if ok else '有失败项，见上面')
    return 0 if ok else 1

if __name__ == '__main__':
    sys.exit(main())