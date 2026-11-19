#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把已翻译的英文 md 正文注入到 batch_gen_en.py 生成的英文外壳页。

外壳页 = 复制中文页改的英文页（含中文正文 + translation-banner）。
本脚本：
  1. 读英文 md → md_to_body_html 渲染正文
  2. 在外壳页 <div class="vp-doc _en_xxx" ...><div> ... </div></div></main> 内整体换正文
  3. <title> 换成英文首标题
  4. 移除 translation-banner（div + scoped CSS）

用法: python3 fill_en_page.py <en_md> <en_shell_html> <rel_path>
  <en_md>        scripts/en_src/<section>/<name>.md
  <en_shell_html> docs-site/en/<section>/<name>.html（会被覆盖）
  <rel_path>     /en/<section>/<name>
"""
import re
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from md2vitepress import md_to_body_html

DOCS = os.path.expanduser('~/workspace/docs-site')


def first_heading(md_text):
    for line in md_text.splitlines():
        m = re.match(r'^#\s+(.*)$', line.strip())
        if m:
            return m.group(1).strip()
    return ''


def fill(en_md_path, shell_html_path, rel_path):
    md = open(en_md_path, encoding='utf-8').read()
    h = open(shell_html_path, encoding='utf-8').read()

    title = first_heading(md)
    body = md_to_body_html(md)

    # 1) 替换正文：vp-doc _en_... 的 div 内容整体替换到 </div></div></main>
    pat = re.compile(
        r'(<div style="position:relative;" class="vp-doc _en[^"]*"[^>]*><div>).*?(</div></div></main>)',
        re.S)
    h, n = pat.subn(lambda m: m.group(1) + body + m.group(2), h, count=1)
    if n != 1:
        raise SystemExit('正文替换失败: %s (匹配 %d)' % (shell_html_path, n))

    # 2) <title>
    safe_title = title.replace('&', '&amp;')
    h = re.sub(r'<title>.*?</title>',
               '<title>%s | AiVerse - AI Coding Tool &amp; Terminal on Android</title>' % safe_title,
               h, count=1)

    # 3) 移除 banner（div + CSS）
    h = re.sub(r'<div class="translation-banner"[^>]*>.*?</div>', '', h, count=1)
    h = re.sub(r'\.translation-banner[^{]*\{[^}]*\}', '', h)

    open(shell_html_path, 'w', encoding='utf-8').write(h)
    print('OK %s (title=%r)' % (os.path.basename(shell_html_path), title))


if __name__ == '__main__':
    if len(sys.argv) != 4:
        print(__doc__)
        raise SystemExit(1)
    fill(sys.argv[1], sys.argv[2], sys.argv[3])