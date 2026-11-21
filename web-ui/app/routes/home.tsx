import { Asterisk, Terminal, GitBranch, Smartphone } from "lucide-react";
import { Shell } from "~/components/Shell";
import { SessionList } from "~/components/SessionList";

const FEATURES = [
  { icon: Terminal, title: "终端与 Agent", desc: "在手机的 Linux 容器里读写文件、执行命令" },
  { icon: GitBranch, title: "会话同步", desc: "网页端与 App 共享同一份会话与消息" },
  { icon: Smartphone, title: "随处访问", desc: "同一局域网内，用电脑浏览器接着聊" },
];

export default function Home() {
  return (
    <Shell title="AiVerse" subtitle="选择一个会话继续对话">
      {/* 手机：直接展示会话列表 */}
      <div className="flex min-h-0 flex-1 flex-col pt-2 md:hidden">
        <SessionList />
      </div>

      {/* 桌面：欢迎页（会话在左侧栏） */}
      <main className="hidden flex-1 items-center justify-center overflow-y-auto p-8 md:flex">
        <div className="anim-in w-full max-w-2xl text-center">
          <div className="mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-2xl bg-accent text-accent-fg shadow-card">
            <Asterisk className="h-9 w-9" strokeWidth={2.2} />
          </div>
          <h2 className="font-serif text-3xl font-normal tracking-tight">欢迎使用 AiVerse</h2>
          <p className="mt-2 text-sm text-muted">从左侧选择一个会话，继续与 AI Agent 协作。</p>
          <div className="mt-10 grid grid-cols-3 gap-3 text-left">
            {FEATURES.map(({ icon: Icon, title, desc }) => (
              <div
                key={title}
                className="rounded-2xl border border-line bg-surface p-4 transition-colors hover:border-accent/40"
              >
                <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-accent-soft text-accent">
                  <Icon className="h-[18px] w-[18px]" />
                </span>
                <p className="mt-3 text-sm font-medium">{title}</p>
                <p className="mt-1 text-xs leading-relaxed text-muted">{desc}</p>
              </div>
            ))}
          </div>
        </div>
      </main>
    </Shell>
  );
}
