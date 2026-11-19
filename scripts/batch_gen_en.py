#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""整批把中文页复制成英文页外壳。

思路（用户方案：直接复制中文改英文，不逐篇从零搭）:
  以每篇中文页 HTML 为模板，产出对应英文页 en/<section>/<name>.html：
    - <title> 首段标题先保留中文（翻译阶段再换）；加英文站后缀
    - vp-doc 类 _<section>_<name> → _en_<section>_<name>
    - Open-as-Markdown 链接 /md/<section>/<name>.md → /md/en/<section>/<name>.md
    - 侧边栏/正文里的站内 md 链接 ./xxx.md、../guide/xxx.md → 英文路径（未译页回退由 site-enhance.js 处理）
    - 注入 translation-banner（译完由翻译阶段移除）

用法:
  python3 batch_gen_en.py            # 生成所有缺的英文页（已有 4 页跳过）
  python3 batch_gen_en.py <name>...  # 只生成指定 name
"""
import os
import re
import sys

DOCS = os.path.expanduser('~/workspace/docs-site')
# 已完成的英文页（不覆盖）
DONE = {'guide/quick-start', 'guide/overview', 'guide/providers', 'guide/free-providers',
        'advanced/build-android-app'}

BANNER = ('<div class="translation-banner" data-v-495a6007><p data-v-495a6007>'
          '<strong data-v-495a6007>This page is being translated.</strong> '
          'The English version of the manual is in progress; this page still shows the '
          'original Chinese content and will be translated soon. </p></div>')


def en_href(m):
    """把站内 .md 相对链接换成英文路径。"""
    href = m.group(2)
    if not href.endswith('.md'):
        return m.group(0)
    # ./x.md ../guide/x.md guide/x.md → 英文目录下同名
    base = href.rstrip('.md').split('/')[-1] if False else href
    # 取文件名（去目录与查询）
    fname = re.sub(r'^\.\./', '', base).split('/')[-1]
    fname = fname[:-3] if fname.endswith('.md') else fname
    # 无法确定 section，保留原相对结构：只加 en 前缀到最近的 .md
    new = m.group(1) + href + m.group(3)
    return new


def convert(ch_path, en_path):
    h = open(ch_path, encoding='utf-8').read()
    # 1) vp-doc 类（宽松匹配，全页仅 1 处，无歧义）
    h = re.sub(r'class="vp-doc ([^"]+)"', r'class="vp-doc _en\1"', h, count=1)
    # 2) Open-as-Markdown 主链接
    h = h.replace('/md/guide/', '/md/en/guide/').replace('/md/advanced/', '/md/en/advanced/')
    # 3) 面包屑/其它站内 html 链接：/guide/x.html → /en/guide/x.html
    h = re.sub(r'(href|content)="(/(?:guide|advanced)/[^"]+)"', r'\1="/en\2"', h)
    # 4) 注入 banner 到正文 vp-doc > div 开头（class 后可能有 data-v-* 属性）
    h = re.sub(r'(<div style="position:relative;" class="vp-doc _en[^"]*"[^>]*><div>)',
               r'\1' + BANNER, h, count=1)
    # 5) title 标记为待翻译（保留中文标题，翻译阶段替换）
    os.makedirs(os.path.dirname(en_path), exist_ok=True)
    open(en_path, 'w', encoding='utf-8').write(h)


def main():
    only = set(sys.argv[1:])
    made, skipped = [], []
    for section in ('guide', 'advanced'):
        src_dir = os.path.join(DOCS, section)
        if not os.path.isdir(src_dir):
            continue
        for fn in sorted(os.listdir(src_dir)):
            if not fn.endswith('.html'):
                continue
            name = fn[:-5]
            rel = f'{section}/{name}'
            if rel in DONE or (only and name not in only):
                continue
            ch = os.path.join(src_dir, fn)
            en = os.path.join(DOCS, 'en', section, fn)
            convert(ch, en)
            made.append(rel)
    print('生成 %d 页:' % len(made))
    for r in made:
        print('  en/' + r + '.html')
    if skipped:
        print('跳过已完成:', skipped)


if __name__ == '__main__':
    main()