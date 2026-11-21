import { useState } from "react";
import { useNavigate } from "react-router";
import { Asterisk, LockKeyhole, LogIn } from "lucide-react";
import { login } from "~/services/api";
import { ThemeToggle } from "~/components/ThemeToggle";

export default function Login() {
  const navigate = useNavigate();
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!password.trim() || busy) return;
    setBusy(true);
    setError(null);
    try {
      await login(password.trim());
      navigate("/", { replace: true });
    } catch {
      setError("密码错误，请重试");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="flex h-dvh flex-col bg-bg text-fg">
      <div className="safe-t flex h-14 shrink-0 items-center justify-end px-3">
        <ThemeToggle />
      </div>

      <main className="flex flex-1 items-center justify-center px-6 pb-24">
        <form onSubmit={onSubmit} className="anim-in w-full max-w-sm">
          <div className="mb-8 text-center">
            <span className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-accent text-accent-fg shadow-card">
              <Asterisk className="h-9 w-9" strokeWidth={2.2} />
            </span>
            <h1 className="font-serif text-2xl font-medium tracking-tight">登录 AiVerse</h1>
            <p className="mt-1.5 text-sm text-muted">输入访问密码，与你的 Agent 继续协作</p>
          </div>

          <label className="flex items-center gap-2 rounded-xl border border-line bg-surface px-3.5 py-2.5 transition-colors focus-within:border-accent/60">
            <LockKeyhole className="h-4 w-4 shrink-0 text-faint" />
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="访问密码"
              autoComplete="current-password"
              autoFocus
              className="w-full min-w-0 bg-transparent text-[15px] text-fg outline-none placeholder:text-faint"
            />
          </label>

          {error && <p className="mt-2 text-sm text-danger">{error}</p>}

          <button
            type="submit"
            disabled={busy || !password.trim()}
            className="mt-5 flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-accent text-[15px] font-medium text-accent-fg transition-all hover:brightness-110 active:scale-[0.98] disabled:bg-surface2 disabled:text-faint"
          >
            <LogIn className="h-4 w-4" />
            {busy ? "登录中…" : "登录"}
          </button>
        </form>
      </main>
    </div>
  );
}
