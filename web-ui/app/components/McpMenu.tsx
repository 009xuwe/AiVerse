import { useQuery } from "@tanstack/react-query";
import { Plug } from "lucide-react";
import { Dropdown } from "./Dropdown";
import { listMcp } from "~/services/api";
import type { McpServerDto } from "~/types/core";

const STATE_STYLE: Record<McpServerDto["state"], string> = {
  CONNECTED: "bg-emerald-500",
  CONNECTING: "bg-amber-500",
  NEEDS_AUTH: "bg-amber-500",
  FAILED: "bg-danger",
  DISABLED: "bg-faint",
};

const STATE_TEXT: Record<McpServerDto["state"], string> = {
  CONNECTED: "已连接",
  CONNECTING: "连接中",
  NEEDS_AUTH: "待授权",
  FAILED: "失败",
  DISABLED: "已停用",
};

export function McpMenu() {
  const { data } = useQuery({ queryKey: ["mcp"], queryFn: listMcp });
  const servers = data ?? [];
  const connected = servers.filter((s) => s.state === "CONNECTED").length;

  return (
    <Dropdown
      icon={<Plug className="h-[18px] w-[18px]" />}
      label="MCP 服务"
      value={servers.length ? `${connected}/${servers.length}` : null}
    >
      {() =>
        servers.length === 0 ? (
          <p className="px-2.5 py-3 text-center text-xs text-faint">暂无 MCP 服务</p>
        ) : (
          servers.map((s) => (
            <div
              key={s.name}
              className="flex items-center gap-2 rounded-lg px-2.5 py-2"
              title={s.error || undefined}
            >
              <span className={`h-2 w-2 shrink-0 rounded-full ${STATE_STYLE[s.state]}`} />
              <span className="min-w-0 flex-1 truncate text-sm text-fg/90">{s.name}</span>
              <span className="shrink-0 text-[11px] text-faint">
                {STATE_TEXT[s.state]}
                {s.state === "CONNECTED" && ` · ${s.toolCount} 工具`}
              </span>
            </div>
          ))
        )
      }
    </Dropdown>
  );
}
