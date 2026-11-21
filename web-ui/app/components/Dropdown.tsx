import { useEffect, useRef, useState, type ReactNode } from "react";
import { Check, ChevronDown } from "lucide-react";

export function Dropdown({
  icon,
  label,
  value,
  children,
  align = "right",
}: {
  icon: ReactNode;
  label: string;
  value?: string | null;
  children: (close: () => void) => ReactNode;
  align?: "left" | "right";
}) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onDown = (e: MouseEvent) => {
      if (!ref.current?.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && setOpen(false);
    document.addEventListener("mousedown", onDown);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDown);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-label={label}
        title={label}
        aria-expanded={open}
        className={`flex h-9 items-center gap-1 rounded-lg px-2 text-muted transition-colors hover:bg-surface2 hover:text-fg active:scale-95 ${
          open ? "bg-surface2 text-fg" : ""
        }`}
      >
        {icon}
        {value && (
          <span className="max-w-[9rem] truncate text-xs font-medium">{value}</span>
        )}
        <ChevronDown
          className={`h-3.5 w-3.5 transition-transform ${open ? "rotate-180" : ""}`}
        />
      </button>

      {open && (
        <div
          className={`anim-in absolute top-full z-30 mt-1.5 max-h-72 w-56 overflow-y-auto rounded-xl border border-line bg-surface p-1 shadow-card ${
            align === "right" ? "right-0" : "left-0"
          }`}
        >
          {children(() => setOpen(false))}
        </div>
      )}
    </div>
  );
}

export function DropdownItem({
  active,
  onClick,
  children,
}: {
  active?: boolean;
  onClick: () => void;
  children: ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex w-full items-center gap-2 rounded-lg px-2.5 py-2 text-left text-sm transition-colors ${
        active ? "bg-accent-soft text-accent" : "text-fg/90 hover:bg-surface2"
      }`}
    >
      <span className="min-w-0 flex-1 truncate">{children}</span>
      {active && <Check className="h-4 w-4 shrink-0" />}
    </button>
  );
}
