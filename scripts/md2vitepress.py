#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Markdown → VitePress HTML 正文转换器。

把 VitePress 风格的 Markdown（标题、表格、``` 代码块、::: tip/warning/info/danger
容器、链接、行内代码、加粗、列表）渲染成与 VitePress 预渲染输出一致的 HTML 片段，
用于给 AiVerse 文档站批量生成英文正文页（无 VitePress 源码，直接改构建产物）。

用法:
  from md2vitepress import md_to_body_html, slugify
  html = md_to_body_html(md_text)

支持语法:
  # H1 / ## H2 / ### H3 / #### H4
  | 表格 | (含对齐行)
  ```lang ... ```  围栏代码块
  ::: tip 标题 / ::: warning / ::: info / ::: danger / ::: note ... :::
  - / * 无序列表, 1. 有序列表
  [text](link), `code`, **bold**, *italic*
"""
import re
import html as _html

_SLUG_RE = re.compile(r'[^\w\- ]', flags=re.UNICODE)


def slugify(text):
    """VitePress 风格的标题锚点 slug：小写、去标点、空格转连字符。"""
    s = text.strip().lower()
    s = _SLUG_RE.sub('', s)
    s = s.replace(' ', '-')
    return s


def _inline(text):
    """行内渲染：转义 → 代码 → 链接 → 粗体 → 斜体。"""
    # 1) 先抽出 `code`，避免其内容被其它规则改写
    codes = []

    def _stash_code(m):
        codes.append('<code v-pre>' + _html.escape(m.group(1)) + '</code>')
        return '\x00CODE%d\x00' % (len(codes) - 1)

    text = re.sub(r'`([^`]+)`', _stash_code, text)
    # 2) 转义其余 HTML
    text = _html.escape(text, quote=False)
    # 3) 链接 [text](href)
    text = re.sub(r'\[([^\]]+)\]\(([^)]+)\)',
                  lambda m: '<a href="%s">%s</a>' % (m.group(2), m.group(1)),
                  text)
    # 4) 粗体 **text**
    text = re.sub(r'\*\*([^*]+)\*\*', r'<strong>\1</strong>', text)
    # 5) 斜体 *text*（简单：不成对 ** 已处理）
    text = re.sub(r'(?<!\*)\*([^*\n]+)\*(?!\*)', r'<em>\1</em>', text)
    # 6) 还原代码占位
    for i, c in enumerate(codes):
        text = text.replace('\x00CODE%d\x00' % i, c)
    return text


def _render_code_block(lang, code_lines):
    """VitePress 围栏代码块结构。

    英文页无 Shiki 高亮 token，用每行 <span class="line"> 包裹的纯文本，
    保留 language-* / copy / lang / shiki vp-code 外壳，可读且结构正确。
    """
    lang = lang or 'txt'
    lines_html = '\n'.join('<span class="line">' + cl + '</span>' for cl in code_lines)
    return (
        '<div class="language-' + lang + ' vp-adaptive-theme"><button '
        'title="Copy Code" class="copy"></button><span class="lang">' + lang +
        '</span><pre v-pre class="shiki vp-code" tabindex="0"><code>' + lines_html +
        '</code></pre></div>'
    )


def _render_table(header_cells, align, rows):
    """VitePress 表格结构，含 tabindex=0 与 colgroup 对齐。"""
    n = len(header_cells)
    colgroup = ''
    for i in range(n):
        a = align[i] if i < len(align) else None
        style = ''
        if a == 'left':
            style = 'text-align:left;'
        elif a == 'right':
            style = 'text-align:right;'
        elif a == 'center':
            style = 'text-align:center;'
        colgroup += '<col' + ((' style="%s"' % style) if style else '') + '>'
    thead_ths = ''.join('<th>%s</th>' % _inline(c) for c in header_cells)
    body_rows = ''
    for r in rows:
        tds = ''.join('<td>%s</td>' % _inline(c) for c in r)
        body_rows += '<tr>%s</tr>' % tds
    return (
        '<table tabindex="0"><thead><tr>%s</tr></thead>'
        '<tbody>%s</tbody></table>' % (thead_ths, body_rows)
    ), '<colgroup>%s</colgroup>' % colgroup  # colgroup 由调用方决定是否插入


def _render_container(kind, title, inner_lines):
    """VitePress ::: 容器结构。"""
    inner_html = _parse_blocks(inner_lines)
    title_html = ('<p class="custom-block-title">%s</p>' % _inline(title)) if title else ''
    return '<div class="%s custom-block"><p class="custom-block-title">%s</p>%s</div>' % (
        kind, _inline(title) if title else '', inner_html) if False else (
        '<div class="%s custom-block">%s%s</div>' % (kind, title_html, inner_html)
    )


def _parse_blocks(lines):
    """块级解析主循环，返回拼接的 HTML。"""
    out = []
    i = 0
    n = len(lines)
    while i < n:
        line = lines[i]
        stripped = line.strip()

        # 空行
        if not stripped:
            i += 1
            continue

        # 围栏代码块
        if stripped.startswith('```'):
            lang = stripped[3:].strip()
            i += 1
            code_lines = []
            while i < n and not lines[i].strip().startswith('```'):
                code_lines.append(_html.escape(lines[i]))
                i += 1
            i += 1  # 跳过结束 ```
            out.append(_render_code_block(lang, code_lines))
            continue

        # ::: 容器
        if stripped.startswith(':::'):
            first = stripped[3:].strip()
            # kind = 第一个词，title = 其余
            parts = first.split(' ', 1)
            kind = parts[0] if parts and parts[0] else 'info'
            title = parts[1].strip() if len(parts) > 1 else ''
            i += 1
            inner = []
            while i < n and not lines[i].strip().startswith(':::'):
                inner.append(lines[i])
                i += 1
            i += 1  # 跳过结束 :::
            out.append(_render_container(kind, title, inner))
            continue

        # 标题
        m = re.match(r'^(#{1,6})\s+(.*)$', stripped)
        if m:
            level = len(m.group(1))
            raw = m.group(2).strip()
            slug = slugify(raw)
            anchor = ('<a class="header-anchor" href="#%s" aria-label="Permalink to &quot;%s&quot;">\u200b</a>'
                      % (slug, _html.escape(raw, quote=True)))
            out.append('<h%d id="%s" tabindex="-1">%s %s</h%d>'
                       % (level, slug, _inline(raw), anchor, level))
            i += 1
            continue

        # 表格：当前行含 | 且下一行是分隔行
        if '|' in line and i + 1 < n and re.match(r'^\s*\|?[\s:|-]+\|[\s:|-]*$', lines[i + 1]):
            def split_row(row):
                row = row.strip()
                if row.startswith('|'):
                    row = row[1:]
                if row.endswith('|'):
                    row = row[:-1]
                return [c.strip() for c in row.split('|')]
            header = split_row(lines[i])
            align_line = lines[i + 1]
            aligns = []
            for seg in split_row(align_line):
                if seg.startswith(':') and seg.endswith(':'):
                    aligns.append('center')
                elif seg.endswith(':'):
                    aligns.append('right')
                elif seg.startswith(':'):
                    aligns.append('left')
                else:
                    aligns.append(None)
            i += 2
            rows = []
            while i < n and '|' in lines[i] and lines[i].strip():
                rows.append(split_row(lines[i]))
                i += 1
            tbl_html, _cg = _render_table(header, aligns, rows)
            out.append(tbl_html)
            continue

        # 无序列表
        if re.match(r'^[-*]\s+', stripped):
            items = []
            while i < n and re.match(r'^[-*]\s+', lines[i].strip()):
                items.append(re.sub(r'^[-*]\s+', '', lines[i].strip()))
                i += 1
            out.append('<ul>' + ''.join('<li>%s</li>' % _inline(it) for it in items) + '</ul>')
            continue

        # 有序列表
        if re.match(r'^\d+\.\s+', stripped):
            items = []
            while i < n and re.match(r'^\d+\.\s+', lines[i].strip()):
                items.append(re.sub(r'^\d+\.\s+', '', lines[i].strip()))
                i += 1
            out.append('<ol>' + ''.join('<li>%s</li>' % _inline(it) for it in items) + '</ol>')
            continue

        # 段落（连续非空、非以上任何块）
        para = [stripped]
        i += 1
        while i < n:
            nxt = lines[i].strip()
            if (not nxt or nxt.startswith('```') or nxt.startswith(':::')
                    or re.match(r'^#{1,6}\s+', nxt) or re.match(r'^[-*]\s+', nxt)
                    or re.match(r'^\d+\.\s+', nxt) or ('|' in nxt and i + 1 < n
                    and re.match(r'^\s*\|?[\s:|-]+\|[\s:|-]*$', lines[i + 1] if i + 1 < n else ''))):
                break
            para.append(nxt)
            i += 1
        out.append('<p>%s</p>' % _inline(' '.join(para)))

    return '\n'.join(out)


def md_to_body_html(md_text):
    """把完整 Markdown 文本渲染为 VitePress 正文 HTML 片段（剥离 front matter）。"""
    # 剥离 front matter
    if md_text.startswith('---'):
        end = md_text.find('\n---', 3)
        if end != -1:
            md_text = md_text[end + 4:]
    lines = md_text.replace('\r\n', '\n').split('\n')
    return _parse_blocks(lines)


if __name__ == '__main__':
    import sys
    src = sys.argv[1] if len(sys.argv) > 1 else '/dev/stdin'
    with open(src, 'r', encoding='utf-8') as f:
        md = f.read()
    print(md_to_body_html(md))