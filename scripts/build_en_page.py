#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成英文正文页：用英文模板外壳 + 翻译后的 md 正文。

用法:
  python3 build_en_page.py <en_md> <out_html> <rel_path>
    <en_md>     翻译好的英文 Markdown 源
    <out_html>  输出 HTML 路径（相对 docs-site，如 en/advanced/build-android-app.html）
    <rel_path>  页面路由（如 /en/advanced/build-android-app）

逻辑:
  1. 读英文模板 en/guide/overview.html 作为外壳（head/脚本/侧边栏 DOM 全保留）。
  2. 把 md 正文经 md2vitepress 渲染成 HTML，注入 <div class="vp-doc ..."> 内。
  3. 改 <title>、vp-doc 类名、Open-as-Markdown 链接指向 /md<rel>.md。
  4. 移除 translation-banner（正文已补齐）。
  5. 改面包屑路径 /en/guide/overview → <rel_path>（若有）。
"""
import re
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from md2vitepress import md_to_body_html, slugify  # noqa: E402

DOCS = os.path.expanduser('~/workspace/docs-site')
TEMPLATE = os.path.join(DOCS, 'en/guide/overview.html')


def first_heading(md_text):
    for line in md_text.splitlines():
        m = re.match(r'^#\s+(.*)$', line.strip())
        if m:
            return m.group(1).strip()
    return ''


def build(en_md_path, out_html_path, rel_path):
    with open(en_md_path, encoding='utf-8') as f:
        md = f.read()
    tpl = open(TEMPLATE, encoding='utf-8').read()

    title = first_heading(md)
    body_html = md_to_body_html(md)

    # 1) vp-doc 内的正文整体替换（从 <div class="vp-doc ..."> 的第一个 <div> 到其闭合前的正文）
    #    定位 <main class="main" ...><div style="position:relative;" class="vp-doc XXX" ...><div> ... </div></div></main>
    main_re = re.compile(
        r'(<main class="main"[^>]*><div style="position:relative;" class="vp-doc )'
        r'[^"]*("[^>]*><div>).*?(</div></div></main>)',
        re.S)
    # 英文 vp-doc 类名与 VitePress 约定一致：_en_advanced_build-android-app / _en_guide_xxx
    vp_cls = '_en_' + rel_path.replace('/en/', '', 1).replace('/', '_')
    tpl, n = main_re.subn(lambda m: m.group(1) + vp_cls + m.group(2) + body_html + m.group(3), tpl, count=1)
    if n != 1:
        raise SystemExit('vp-doc 正文注入失败: main 替换次数=%d' % n)

    # 2) <title>
    safe_title = title.replace('&', '&amp;')
    tpl = re.sub(r'<title>.*?</title>',
                 '<title>%s | AiVerse - AI Coding Tool &amp; Terminal on Android</title>' % safe_title,
                 tpl, count=1)

    # 3) Open-as-Markdown 链接
    md_link = '/md' + rel_path + '.md'
    tpl = tpl.replace('href="/md/en/guide/overview.md"', 'href="%s"' % md_link)

    # 4) 移除 translation-banner（div 本体 + 其 scoped CSS）
    tpl = re.sub(r'<div class="translation-banner"[^>]*>.*?</div>', '', tpl, count=1)
    tpl = re.sub(r'\.translation-banner[^{]*\{[^}]*\}', '', tpl)

    # 5) 面包屑路径（英文 overview 模板通常无面包屑，此步幂等）
    tpl = tpl.replace('/en/guide/overview', rel_path)

    os.makedirs(os.path.dirname(os.path.join(DOCS, out_html_path)), exist_ok=True)
    out = os.path.join(DOCS, out_html_path)
    with open(out, 'w', encoding='utf-8') as f:
        f.write(tpl)
    print('OK  %s  (title=%r)' % (out_html_path, title))


if __name__ == '__main__':
    if len(sys.argv) != 4:
        print(__doc__)
        raise SystemExit(1)
    build(sys.argv[1], sys.argv[2], sys.argv[3])