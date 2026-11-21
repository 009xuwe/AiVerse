import { useMemo, useState } from "react";
import { Link, useNavigate } from "react-router";
import { useQuery } from "@tanstack/react-query";
import { MessageSquare, Pin, Plus, Search } from "lucide-react";
import dayjs from "dayjs";
import { createSession, listSessions } from "~/services/api";
import type { SessionDto } from "~/types/core";

function formatTime(ts: number) {
  const d = dayjs(ts);
  if (d.isSame(dayjs(), "day")) return d.format("HH:mm");
  if (d.isSame(dayjs(), "year")) return d.format("MM-DD");
  return d.format("YY-MM-DD");
}

function Row({ s, active }: { s: SessionDto; active: boolean }) {
  return (
    <Link
      to={`/c/${s.id}`}
      className={`group flex items-center gap-3 rounded-xl px-3 py-2.5 transition-colors active:scale-[0.99] ${
        active
          ? "bg-accent-soft text-fg"
          : "text-fg/90 hover:bg-surface2"
      }`}
    >
      <span
        className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-lg ${
          active ? "bg-accent text-accent-fg" : "bg-surface2 text-muted group-hover:bg-line"
        }`}
      >
        <MessageSquare className="h-4 w-4" />
      </span>
      <span className="min-w-0 flex-1">
        <span className="flex items-center gap-1.5">
          <span className="truncate text-sm font-medium">{s.title || "未命名会话"}</span>
          {s.isPinned && <Pin className="h-3 w-3 shrink-0 text-faint" />}
        </span>
        <span className="mt-0.5 block truncate text-xs text-faint">
          {[s.model, s.mode].filter(Boolean).join(" · ") || "—"}
        </span>
      </span>
      <span className="shrink-0 self-start pt-0.5 text-[11px] tabular-nums text-faint">
        {formatTime(s.updatedAt)}
      </span>
    </Link>
  );
}

export function SessionList({ activeId }: { activeId?: string }) {
  const navigate = useNavigate();
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ["sessions"],
    queryFn: listSessions,
  });
  const [q, setQ] = useState("");
  const [creating, setCreating] = useState(false);

  async function onNew() {
    if (creating) return;
    setCreating(true);
    try {
      const s = await createSession();
      refetch();
      navigate(`/c/${s.id}`);
    } catch (e) {
      console.error(e);
    } finally {
      setCreating(false);
    }
  }

  const list = useMemo(() => {
    const kw = q.trim().toLowerCase();
    return (data ?? [])
      .filter((s) => !kw || (s.title || "未命名会话").toLowerCase().includes(kw))
      .sort(
        (a, b) =>
          Number(!!b.isPinned) - Number(!!a.isPinned) || b.updatedAt - a.updatedAt,
      );
  }, [data, q]);

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div className="flex items-center gap-2 px-3 pb-2 pt-1">
        <label className="flex min-w-0 flex-1 items-center gap-2 rounded-xl border border-line bg-surface px-3 py-2 text-muted focus-within:border-accent/60">
          <Search className="h-4 w-4 shrink-0" />
          <input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="搜索会话"
            className="w-full min-w-0 bg-transparent text-sm text-fg outline-none placeholder:text-faint"
          />
        </label>
        <button
          type="button"
          onClick={onNew}
          disabled={creating}
          aria-label="新建对话"
          title="新建对话"
          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-accent text-accent-fg transition-all hover:brightness-110 active:scale-95 disabled:bg-surface2 disabled:text-faint"
        >
          <Plus className="h-5 w-5" strokeWidth={2.2} />
        </button>
      </div>

      <nav className="min-h-0 flex-1 space-y-0.5 overflow-y-auto px-2 pb-3">
        {isLoading &&
          Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="flex items-center gap-3 px-3 py-2.5">
              <div className="skeleton h-8 w-8 rounded-lg" />
              <div className="flex-1 space-y-2">
                <div className="skeleton h-3 w-3/4 rounded" />
                <div className="skeleton h-2.5 w-1/2 rounded" />
              </div>
            </div>
          ))}

        {error && (
          <div className="m-2 rounded-xl border border-danger/30 bg-danger/10 p-3 text-sm text-danger">
            加载失败，请确认 App 中的网页服务已开启。
            <button onClick={() => refetch()} className="ml-2 underline underline-offset-2">
              重试
            </button>
          </div>
        )}

        {!isLoading && !error && list.length === 0 && (
          <p className="px-3 py-10 text-center text-sm text-faint">
            {q ? "没有匹配的会话" : "暂无会话，请先在 App 里发起对话"}
          </p>
        )}

        {list.map((s) => (
          <Row key={s.id} s={s} active={s.id === activeId} />
        ))}
      </nav>
    </div>
  );
}
