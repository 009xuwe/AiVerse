import { useState, type ReactNode } from "react";
import { Check, Copy } from "lucide-react";

/** 兼容 http 局域网访问（此时 navigator.clipboard 不可用） */
function copyText(text: string): Promise<void> {
  if (navigator.clipboard && window.isSecureContext) {
    return navigator.clipboard.writeText(text);
  }
  return new Promise((resolve, reject) => {
    const ta = document.createElement("textarea");
    ta.value = text;
    ta.style.cssText = "position:fixed;opacity:0;top:0;left:0";
    document.body.appendChild(ta);
    ta.select();
    try {
      document.execCommand("copy") ? resolve() : reject(new Error("copy failed"));
    } catch (e) {
      reject(e);
    } finally {
      document.body.removeChild(ta);
    }
  });
}

export function CopyButton({
  text,
  className = "",
}: {
  text: string;
  className?: string;
}) {
  const [done, setDone] = useState(false);
  return (
    <button
      type="button"
      aria-label="复制"
      onClick={() => {
        copyText(text)
          .then(() => {
            setDone(true);
            setTimeout(() => setDone(false), 1500);
          })
          .catch(() => {});
      }}
      className={`flex items-center gap-1 rounded-md px-1.5 py-1 text-[11px] text-muted transition-colors hover:bg-surface2 hover:text-fg ${className}`}
    >
      {done ? <Check className="h-3.5 w-3.5 text-accent" /> : <Copy className="h-3.5 w-3.5" />}
      {done ? "已复制" : "复制"}
    </button>
  );
}

function CodeBlock({ code, lang }: { code: string; lang: string }) {
  return (
    <div className="overflow-hidden rounded-xl border border-line bg-code">
      <div className="flex items-center justify-between border-b border-line px-3 py-1">
        <span className="font-mono text-[11px] uppercase tracking-wide text-faint">
          {lang || "text"}
        </span>
        <CopyButton text={code} />
      </div>
      <pre className="overflow-x-auto px-3.5 py-3 font-mono text-[13px] leading-relaxed">
        <code>{code}</code>
      </pre>
    </div>
  );
}

/* ───────── 行内 ───────── */
const INLINE =
  /(`[^`\n]+`)|(\*\*[^*\n]+\*\*|__[^_\n]+__)|(~~[^~\n]+~~)|(\*[^*\s][^*\n]*\*)|(\[[^\]\n]+\]\((?:https?:\/\/|\/)[^)\s]+\))/g;

function inline(text: string, key: string): ReactNode[] {
  const out: ReactNode[] = [];
  let last = 0;
  let i = 0;
  for (const m of text.matchAll(INLINE)) {
    const idx = m.index ?? 0;
    if (idx > last) out.push(text.slice(last, idx));
    const tok = m[0];
    const k = `${key}-${i++}`;
    if (m[1]) {
      out.push(<code key={k}>{tok.slice(1, -1)}</code>);
    } else if (m[2]) {
      out.push(<strong key={k}>{inline(tok.slice(2, -2), k)}</strong>);
    } else if (m[3]) {
      out.push(<del key={k}>{inline(tok.slice(2, -2), k)}</del>);
    } else if (m[4]) {
      out.push(<em key={k}>{inline(tok.slice(1, -1), k)}</em>);
    } else if (m[5]) {
      const mm = /^\[([^\]]+)\]\(([^)]+)\)$/.exec(tok);
      if (mm) {
        out.push(
          <a key={k} href={mm[2]} target="_blank" rel="noreferrer noopener">
            {mm[1]}
          </a>,
        );
      }
    }
    last = idx + tok.length;
  }
  if (last < text.length) out.push(text.slice(last));
  return out;
}

/* ───────── 块级 ───────── */
const RE_FENCE = /^\s*```\s*([\w+#.-]*)/;
const RE_HEAD = /^(#{1,6})\s+(.*)$/;
const RE_HR = /^\s*([-*_])(\s*\1){2,}\s*$/;
const RE_UL = /^(\s*)[-*+]\s+(.*)$/;
const RE_OL = /^(\s*)\d+[.)]\s+(.*)$/;
const RE_QUOTE = /^\s*>\s?(.*)$/;
const RE_TABLE_SEP = /^\s*\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)*\|?\s*$/;

function splitRow(line: string): string[] {
  return line
    .trim()
    .replace(/^\|/, "")
    .replace(/\|$/, "")
    .split("|")
    .map((c) => c.trim());
}

function isBlockStart(line: string, next?: string): boolean {
  return (
    RE_FENCE.test(line) ||
    RE_HEAD.test(line) ||
    RE_HR.test(line) ||
    RE_UL.test(line) ||
    RE_OL.test(line) ||
    RE_QUOTE.test(line) ||
    (line.includes("|") && next !== undefined && RE_TABLE_SEP.test(next))
  );
}

function renderBlocks(src: string): ReactNode[] {
  const lines = src.replace(/\r\n?/g, "\n").split("\n");
  const out: ReactNode[] = [];
  let i = 0;
  let n = 0;

  while (i < lines.length) {
    const line = lines[i];
    if (!line.trim()) {
      i++;
      continue;
    }

    // 代码块（流式输出时未闭合也能正常显示）
    const fence = RE_FENCE.exec(line);
    if (fence) {
      const buf: string[] = [];
      i++;
      while (i < lines.length && !/^\s*```\s*$/.test(lines[i])) buf.push(lines[i++]);
      i++;
      out.push(<CodeBlock key={n++} code={buf.join("\n")} lang={fence[1]} />);
      continue;
    }

    const head = RE_HEAD.exec(line);
    if (head) {
      const Tag = `h${head[1].length}` as "h1";
      out.push(<Tag key={n++}>{inline(head[2], `h${n}`)}</Tag>);
      i++;
      continue;
    }

    if (RE_HR.test(line)) {
      out.push(<hr key={n++} />);
      i++;
      continue;
    }

    if (RE_QUOTE.test(line)) {
      const buf: string[] = [];
      while (i < lines.length && RE_QUOTE.test(lines[i])) {
        buf.push(RE_QUOTE.exec(lines[i])![1]);
        i++;
      }
      out.push(<blockquote key={n++}>{renderBlocks(buf.join("\n"))}</blockquote>);
      continue;
    }

    if (line.includes("|") && i + 1 < lines.length && RE_TABLE_SEP.test(lines[i + 1])) {
      const header = splitRow(line);
      i += 2;
      const rows: string[][] = [];
      while (i < lines.length && lines[i].includes("|") && lines[i].trim()) {
        rows.push(splitRow(lines[i++]));
      }
      const k = `t${n}`;
      out.push(
        <div key={n++} className="table-wrap">
          <table>
            <thead>
              <tr>
                {header.map((h, c) => (
                  <th key={c}>{inline(h, `${k}h${c}`)}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.map((r, ri) => (
                <tr key={ri}>
                  {r.map((c, ci) => (
                    <td key={ci}>{inline(c, `${k}r${ri}c${ci}`)}</td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>,
      );
      continue;
    }

    const ul = RE_UL.exec(line);
    const ol = RE_OL.exec(line);
    if (ul || ol) {
      const ordered = !!ol;
      const re = ordered ? RE_OL : RE_UL;
      const items: { depth: number; text: string }[] = [];
      while (i < lines.length) {
        const m = re.exec(lines[i]);
        if (!m) break;
        items.push({ depth: Math.min(3, Math.floor(m[1].replace(/\t/g, "  ").length / 2)), text: m[2] });
        i++;
      }
      const List = ordered ? "ol" : "ul";
      const k = `l${n}`;
      out.push(
        <List key={n++}>
          {items.map((it, ii) => (
            <li key={ii} style={it.depth ? { marginLeft: it.depth * 18 } : undefined}>
              {inline(it.text, `${k}i${ii}`)}
            </li>
          ))}
        </List>,
      );
      continue;
    }

    // 段落
    const buf: string[] = [line];
    i++;
    while (i < lines.length && lines[i].trim() && !isBlockStart(lines[i], lines[i + 1])) {
      buf.push(lines[i++]);
    }
    const k = `p${n}`;
    out.push(
      <p key={n++}>
        {buf.map((l, li) => (
          <span key={li}>
            {li > 0 && <br />}
            {inline(l, `${k}-${li}`)}
          </span>
        ))}
      </p>,
    );
  }
  return out;
}

export function Markdown({ children }: { children: string }) {
  return <div className="md">{renderBlocks(children)}</div>;
}
