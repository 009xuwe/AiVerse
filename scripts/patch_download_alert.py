#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
为 download.html / en/download.html 的轻量 Markdown 渲染器补上 blockquote 解析，
使其适配更新公告里的 GitHub 告警语法（> [!NOTE] / [!TIP] / [!IMPORTANT] / [!WARNING] / [!CAUTION]）。

改动两处：
  1) renderMD() 里在「段落」分支之前插入 blockquote 分支：
     - 连续以 > 开头的行聚成一个引用块
     - 首行若匹配 [!TYPE] 则渲染为告警卡片 <div class="md-alert md-alert--type">
     - 普通引用渲染为 <blockquote>
  2) 在 .release-note hr 规则后追加告警卡片 / blockquote 的 CSS。
"""
import sys, io

# 告警类型 -> (显示标题, 颜色)
ALERT_TYPES = ["NOTE", "TIP", "IMPORTANT", "WARNING", "CAUTION"]

OLD_RENDER_ANCHOR = """      if(/^\\s*$/.test(line)){ i++; continue; }
      var para=[];
      while(i<lines.length && !/^\\s*$/.test(lines[i]) && !/^\\s*(?:[-*+]|\\d+\\.)\\s+/.test(lines[i]) && !/^#{1,6}\\s/.test(lines[i]) && lines[i].indexOf('|')===-1){
        para.push(lines[i]); i++;
      }
      if(para.length){ out.push('<p>'+para.map(inline).join('<br>')+'</p>'); }
      else { i++; }"""

NEW_RENDER_ANCHOR = """      if(/^\\s*$/.test(line)){ i++; continue; }
      // 引用块：连续以 > 开头的行（含 GitHub 告警卡片 > [!NOTE] 等）
      if(/^\\s*>/.test(line)){
        var q=[];
        while(i<lines.length && /^\\s*>/.test(lines[i])){
          q.push(lines[i].replace(/^\\s*>\\s?/,'')); i++;
        }
        var alertM=(q[0]||'').match(/^\\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\\]\\s*(.*)$/i);
        if(alertM){
          var type=alertM[1].toUpperCase();
          var rest=alertM[2]||'';
          if(/^\\s*$/.test(rest)) q.shift();
          else q[0]=rest;
          var body=q.filter(function(x){return !/^\\s*$/.test(x);}).map(inline).join('<br>');
          out.push('<div class="md-alert md-alert--'+type.toLowerCase()+'"><div class="md-alert-title">'+type+'</div><div class="md-alert-body">'+body+'</div></div>');
        } else {
          var qbody=q.filter(function(x){return !/^\\s*$/.test(x);}).map(inline).join('<br>');
          out.push('<blockquote>'+qbody+'</blockquote>');
        }
        continue;
      }
      var para=[];
      while(i<lines.length && !/^\\s*$/.test(lines[i]) && !/^\\s*(?:[-*+]|\\d+\\.)\\s+/.test(lines[i]) && !/^#{1,6}\\s/.test(lines[i]) && !/^\\s*>/.test(lines[i]) && lines[i].indexOf('|')===-1){
        para.push(lines[i]); i++;
      }
      if(para.length){ out.push('<p>'+para.map(inline).join('<br>')+'</p>'); }
      else { i++; }"""

OLD_CSS_ANCHOR = ".release-note hr{border:0;border-top:1px solid var(--border);margin:var(--s-3) 0}"

NEW_CSS_ANCHOR = """.release-note hr{border:0;border-top:1px solid var(--border);margin:var(--s-3) 0}
/* GitHub 风格告警卡片（> [!NOTE] / [!TIP] / [!IMPORTANT] / [!WARNING] / [!CAUTION]） */
.release-note .md-alert{--ac:var(--text-2);--ac-bg:var(--surface-2);border:1px solid var(--border);border-left:3px solid var(--ac);background:var(--ac-bg);border-radius:var(--r-1);padding:var(--s-2) var(--s-3);margin-bottom:var(--s-3)}
.release-note .md-alert-title{font-size:var(--fs-xs);font-weight:600;letter-spacing:.04em;color:var(--ac);margin-bottom:2px}
.release-note .md-alert-body{font-size:var(--fs-xs);color:var(--text-2);line-height:1.6}
.release-note .md-alert--note{--ac:var(--accent);--ac-bg:rgba(52,81,178,.07)}
.release-note .md-alert--tip{--ac:#2c6a4d;--ac-bg:#e3eee7}
.release-note .md-alert--important{--ac:#6a3fa0;--ac-bg:#ece4f6}
.release-note .md-alert--warning{--ac:#86571d;--ac-bg:#f2e7d3}
.release-note .md-alert--caution{--ac:#a23b2e;--ac-bg:#f6e6e1}
html.dark .release-note .md-alert{border-color:var(--border)}
html.dark .release-note .md-alert--note{--ac:#8ea3ec;--ac-bg:rgba(142,163,236,.12)}
html.dark .release-note .md-alert--tip{--ac:#6fcf9f;--ac-bg:rgba(111,207,159,.13)}
html.dark .release-note .md-alert--important{--ac:#b79ae8;--ac-bg:rgba(183,154,232,.13)}
html.dark .release-note .md-alert--warning{--ac:#e2a763;--ac-bg:rgba(226,167,99,.13)}
html.dark .release-note .md-alert--caution{--ac:#ef8f80;--ac-bg:rgba(239,143,128,.12)}
.release-note blockquote{margin:0 0 var(--s-3);padding:var(--s-1) var(--s-3);border-left:3px solid var(--border-strong);color:var(--text-2);font-size:var(--fs-xs)}"""


def patch(path):
    with io.open(path, "r", encoding="utf-8") as f:
        s = f.read()
    changed = False
    if NEW_RENDER_ANCHOR.split("// 引用块")[0] in s and "md-alert" in s and "function renderMD" in s:
        # 已打过补丁
        if "/^\\\\s*>/" in s or "md-alert--" in s:
            print(f"[skip] {path}: 已包含 blockquote/告警支持")
            return
    if OLD_RENDER_ANCHOR in s:
        s = s.replace(OLD_RENDER_ANCHOR, NEW_RENDER_ANCHOR, 1)
        changed = True
        print(f"[ok] renderMD 已插入 blockquote 分支: {path}")
    elif "md-alert--" in s:
        print(f"[skip] renderMD 已打过补丁: {path}")
    else:
        print(f"[ERR] 未找到 renderMD 段落锚点: {path}")
        sys.exit(1)
    if OLD_CSS_ANCHOR in s and ".md-alert--note" not in s:
        s = s.replace(OLD_CSS_ANCHOR, NEW_CSS_ANCHOR, 1)
        changed = True
        print(f"[ok] 已追加告警卡片 CSS: {path}")
    elif ".md-alert--note" in s:
        print(f"[skip] CSS 已打过补丁: {path}")
    else:
        print(f"[ERR] 未找到 CSS 锚点: {path}")
        sys.exit(1)
    if changed:
        with io.open(path, "w", encoding="utf-8") as f:
            f.write(s)
        print(f"[done] 已写入 {path}")


if __name__ == "__main__":
    for p in sys.argv[1:]:
        patch(p)