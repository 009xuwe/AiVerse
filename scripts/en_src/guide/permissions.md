# Tool Authorization

The AI needs your authorization before it runs commands or writes files. This page manages the authorization rules that have already been saved; the entry point is "Settings → Tool authorization".

Automatic tool approval and task authorization take effect separately: even in AUTO mode, operations that are hard to undo or affect shared state (such as force-pushing, modifying a remote, sending messages, or opening/merging a PR) still require your explicit confirmation for that specific operation. Current requirements, long-term memory, and project rules must all respect platform permissions and safety boundaries.

## Where Rules Come From

When the AI requests to call a tool in a conversation, an authorization dialog pops up and you have two choices:

- **Always allow**: remember an allow rule for the **current project**, and future calls matching the rule are allowed automatically. Rules are exact matches (such as `git pull`, `rm temp.log`). If the command contains a pipe, redirection, or command substitution, it cannot be remembered and can only be allowed once — the dialog states which construct (such as "command substitution `$(...)`") makes it impossible to remember.
- **Deny**: deny only this once, producing no rule.

When denied by a permission rule, the prompt the AI receives includes the **matched rule** and the **specific fragment** that was blocked (such as "Matched rule: Bash(git push); triggering fragment: git push origin main"), so it can try a different, allowed formulation instead of retrying the whole command over and over.

In addition, the system has a built-in whitelist: a small set of completely harmless, read-only commands that spawn no child processes (`ls`, `pwd`, `cat`, `grep`, `ps`, `top`, `git status`, etc.) are allowed directly without a prompt.

Exceptions: the `Shizuku` tool (operating the host Android system directly) is high-risk — every command pops up for confirmation, "Always allow" is not supported, and the built-in whitelist does not apply; `ShizukuControl` (controlling the phone directly, such as opening apps, taking screenshots, simulating taps) is even higher-risk, forcing confirmation every time, not exempt even by "Disable safety interception", and it requires the overlay permission. See [Shizuku Tools](https://aivapp.cn/md/guide/shizuku.md) for details on both.

## Authorization with Parallel Sessions

When several sessions (including sub-agents) work at the same time, each session can hold its own pending authorization request without blocking the others:

- The top of the authorization dialog shows the **title of the session that raised the request** (such as "Session 'Refactor login module'"), making it clear which session the dialog belongs to.
- The dialog is shown in any session's interface, and once handled it moves automatically to the next pending request.
- In the sidebar session list, the indicator light on the left of a session **waiting for authorization** is **steady orange** (it is a green pulse while running). This lets you spot background sessions suspended waiting for authorization and tap in to handle them.

## Page Structure

Rules are split into "Current project" and "Global" groups, and each title can be tapped to collapse.

Each rule shows a decision icon (a checkmark for allow, a cross for deny), the tool name, the match scope, and the decision, such as `git pull · Allow` or `Entire tool · Allow`.

- On the right of a project rule there is a plus icon that **promotes the rule to global**, making it apply to all projects; after promotion it disappears from this project's list.
- Swipe left on a rule row to reveal a red delete button.

## Disable Safety Interception

There is a "Disable safety interception" switch at the top of the page, off by default.

- Off (default): AUTO mode still blocks catastrophic deletions — `rm` on the root directory, critical system directories (`/etc`, `/usr`, etc.), the workspace root directory, or the user home directory are rejected outright. Commands that cannot be judged statically and look destructive (such as `rm -rf $(...)`, `cat > /etc/xxx`) — those containing command substitution, sub-shells, or absolute-path redirections — are also blocked (the deletion target cannot be confirmed). At the same time, under AUTO the `Shizuku` and `ShizukuControl` tools still pop up for confirmation one by one (they are not exempted by "allow everything"). This is the bottom line against accidental deletion.
- On: in AUTO mode even this layer of interception is removed, all commands are allowed, and the `Shizuku` tool no longer prompts. Note that "Shizuku cannot be remembered" is not affected by this switch: under BUILD mode it is still confirmed one by one and cannot be remembered. `ShizukuControl` is stricter still: even "Disable safety interception" cannot exempt it, and it always forces confirmation every time. Safety interception under BUILD / PLAN modes is unaffected.

Before the switch is turned on, a confirmation dialog pops up, and its confirm button carries a **5-second countdown** that cannot be tapped until the countdown ends, to avoid accidental taps.

::: danger Only enable this when truly necessary
Once enabled, destructive commands like `rm` under AUTO mode have no fallback at all. Only turn it on when you fully understand the risk and genuinely need it.
:::

## Elevating High-Risk Commands

Outside PLAN mode, catastrophic operations such as `rm` on the root directory, critical system directories (`/etc`, `/usr`, etc.), the workspace root directory, or the user home directory are rejected outright by the built-in safety guard. If the AI judges that execution is truly necessary, it retries with `elevate: true` — instead of being rejected outright, a dialog asks you to **authorize once** (the dialog title is "High-risk operation elevation confirmation").

- Available in both BUILD and AUTO modes; the cases where a dialog appears under AUTO are: catastrophic-deletion elevation confirmation, and `Shizuku` / `ShizukuControl` tool calls.
- Elevation applies only to that single call and does not create a remembered rule ("Always allow" is unavailable).
- PLAN mode remains a hard block and is not affected by elevation.

## About "Deny" Rules

There is no "Deny" option in the authorization dialog (deny only affects a single call), so normal use never produces a deny rule.

Deny rules can currently only be created by editing the configuration file by hand: project rules go in `<project root>/.aiverse/permissions.json`, and global rules go in `~/.aiverse/permissions.json`. After you save a manual edit, the app detects it and refreshes the page within a few seconds, and the rules take effect immediately.

Configuration file format:

```json
{
  "permissions": {
    "allow": ["Bash(git pull)", "writeFile"],
    "deny": ["Bash(rm -rf /)"]
  }
}
```

::: tip Global deny takes priority over project allow
A rule in `deny` overrides a same-named rule in `allow`. For example, if `Bash(rm -rf /)` is denied globally, no project can allow that command. For extra safety, you can add rules to the global `deny`.
:::
