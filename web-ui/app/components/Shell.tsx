import { useEffect, useState, type ReactNode } from "react";
import { Link, useLocation } from "react-router";
import { Asterisk, Menu, X } from "lucide-react";
import { SessionList } from "./SessionList";
import { ThemeToggle } from "./ThemeToggle";

function Brand() {
  return (
    <Link to="/" className="flex items-center gap-2.5">
      <span className="flex h-8 w-8 items-center justify-center rounded-xl bg-accent text-sm font-bold text-accent-fg">
        <Asterisk className="h-[18px] w-[18px]" strokeWidth={2.4} />
      </span>
      <span className="font-serif text-[17px] font-medium tracking-tight">AiVerse</span>
    </Link>
  );
}

function SidebarBody({ activeId, onClose }: { activeId?: string; onClose?: () => void }) {
  return (
    <div className="safe-t flex h-full flex-col">
      <div className="flex items-center justify-between px-4 py-3.5">
        <Brand />
        {onClose && (
          <button
            onClick={onClose}
            aria-label="关闭"
            className="flex h-9 w-9 items-center justify-center rounded-lg text-muted hover:bg-surface2"
          >
            <X className="h-5 w-5" />
          </button>
        )}
      </div>
      <SessionList activeId={activeId} />
      <div className="safe-b border-t border-line px-4 py-3 text-xs text-faint">
        网页端 · 与 App 实时同步
      </div>
    </div>
  );
}

/**
 * 通用外壳：
 * - ≥ md：左侧常驻会话栏 + 右侧主区
 * - < md：主区全屏，会话栏变为抽屉
 */
export function Shell({
  activeId,
  title,
  subtitle,
  actions,
  children,
}: {
  activeId?: string;
  title: ReactNode;
  subtitle?: ReactNode;
  actions?: ReactNode;
  children: ReactNode;
}) {
  const [open, setOpen] = useState(false);
  const { pathname } = useLocation();

  useEffect(() => setOpen(false), [pathname]);
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && setOpen(false);
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open]);

  return (
    <div className="flex h-dvh overflow-hidden bg-bg text-fg">
      {/* 桌面侧栏 */}
      <aside className="hidden w-72 shrink-0 border-r border-line bg-sidebar md:block lg:w-80">
        <SidebarBody activeId={activeId} />
      </aside>

      {/* 移动抽屉 */}
      <div
        className={`fixed inset-0 z-40 bg-black/40 backdrop-blur-[2px] transition-opacity md:hidden ${
          open ? "opacity-100" : "pointer-events-none opacity-0"
        }`}
        onClick={() => setOpen(false)}
      />
      <aside
        className={`fixed inset-y-0 left-0 z-50 w-[85%] max-w-xs border-r border-line bg-sidebar shadow-2xl transition-transform duration-200 md:hidden ${
          open ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <SidebarBody activeId={activeId} onClose={() => setOpen(false)} />
      </aside>

      {/* 主区 */}
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="safe-t z-10 shrink-0 border-b border-line bg-bg/85 backdrop-blur-md">
          <div className="flex h-14 items-center gap-1 px-2 md:px-4">
            <button
              onClick={() => setOpen(true)}
              aria-label="会话列表"
              className="flex h-10 w-10 items-center justify-center rounded-lg text-muted hover:bg-surface2 hover:text-fg active:scale-95 md:hidden"
            >
              <Menu className="h-5 w-5" />
            </button>
            <div className="min-w-0 flex-1 px-1">
              <h1 className="truncate font-serif text-[16px] font-medium leading-tight">{title}</h1>
              {subtitle && (
                <p className="truncate text-xs leading-tight text-faint">{subtitle}</p>
              )}
            </div>
            {actions}
            <ThemeToggle />
          </div>
        </header>
        {children}
      </div>
    </div>
  );
}
