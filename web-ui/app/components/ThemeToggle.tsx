import { useEffect, useState } from "react";
import { Moon, Sun } from "lucide-react";

type Theme = "light" | "dark";

export function ThemeToggle() {
  const [theme, setTheme] = useState<Theme>("light");

  useEffect(() => {
    setTheme(document.documentElement.dataset.theme === "dark" ? "dark" : "light");
  }, []);

  function toggle() {
    const next: Theme = theme === "dark" ? "light" : "dark";
    setTheme(next);
    document.documentElement.dataset.theme = next;
    document
      .querySelector('meta[name="theme-color"]')
      ?.setAttribute("content", next === "dark" ? "#262624" : "#faf9f5");
    try {
      localStorage.setItem("aiverse-theme", next);
    } catch {
      /* 隐私模式下可能不可用 */
    }
  }

  return (
    <button
      type="button"
      onClick={toggle}
      aria-label="切换主题"
      title="切换深色 / 浅色"
      className="flex h-9 w-9 items-center justify-center rounded-lg text-muted transition-colors hover:bg-surface2 hover:text-fg active:scale-95"
    >
      {theme === "dark" ? <Sun className="h-[18px] w-[18px]" /> : <Moon className="h-[18px] w-[18px]" />}
    </button>
  );
}
