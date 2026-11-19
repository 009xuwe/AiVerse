# Custom Prompts

AiVerse's system prompts can be modified yourself. The default prompts ship with the App and are updated automatically on upgrade; you only need to put the snippets you want to change or add into the custom directory to override or extend them, without touching the App itself.

::: tip From v1.0.0 Onward
This article's snippet naming rule (`<two-digit number>-<name>.md`), adding snippets by number, the `.no-builtin` full-disable switch, and the <span v-pre>`{{AIVERSE_*}}`</span> variables are supported **from v1.0.0** onward. Earlier versions can only override by placing a file with **exactly the same name** as a default snippet, and do not support adding by number, `.no-builtin`, or variables.
:::

## Managing Inside the App

You can also manage prompts without touching the file system, directly in **Settings → Prompts**:

- The page lists every snippet that is **actually in effect**, by number, with a badge on the right showing which layer the content comes from:
  - **Project**: the current workspace's `.aiverse/prompts.custom/`
  - **Global**: `~/.aiverse/prompts.custom/`
  - **Local**: `~/.aiverse/prompts/` (a copy released from the built-in set at startup)
  - **Built-in**: the App's own `assets/prompts/`
- Only the highest-priority copy takes effect for a given number, with priority **Project > Global > Local > Built-in**; the rest are shadowed.
- Tap a row to edit its content. Saving writes by default to the layer currently in effect for that number; "Local/Built-in" layers are not writable, so edits there fall to the Global layer by default, and on the edit page you can switch the scope to the Project layer (when a workspace is present).
- All snippets can be swiped left; Project/Global snippets can delete their current override, after which it automatically falls back to the next layer; the delete button for Local/Built-in snippets is grayed out and cannot be deleted.
- The "+" at the top right adds a new snippet using an unused number. When editing, the summary and body are filled in separately; the summary is shown in the list and on the preview page and is not injected into the AI. When writing files by hand, a leading `<!-- Summary -->` is likewise recognized as the summary, and the body preview hides that comment.
- **Enable / disable individually** (**from 1.0.3**): each snippet row has a switch on the right, so you can disable a single snippet on its own — once disabled it is no longer injected into the system prompt, but it stays in the list with its number unchanged, and its title and summary are shown in a muted color; you can re-enable it at any time. This is a separate layer from the top-level "fully disable built-in prompts": the master switch turns off all built-in prompts at once, while a snippet's switch only affects that one snippet.
- **Import from file** (**from 1.0.3**): when editing a snippet, the top right of the body area has "Import from file", which opens the system file picker to choose a `.md` / `.txt` text file, and the App fills the body box with its content: the title defaults to the file name (without extension), and a leading `<!-- Summary -->` comment is extracted as the summary. Importing only fills the editor — you still have to tap "Save" at the top right for it to take effect; if the file can't be read it shows "Import failed, unable to read file".
- On first entry, a usage announcement pops up automatically, and the Info button at the top right lets you review it at any time.

### Bulk Delete

The "Multi-select" button at the top right of the list (a box with a check icon) lets you delete several custom snippets at once:

1. Tap "Multi-select" to enter multi-select mode; a checkbox appears on the left of each row, and tapping a row selects it. Only snippets from **Project / Global** sources can be checked; the checkboxes for Local and Built-in sources are grayed out.
2. The top action bar offers "Select all / Deselect all" and a delete button; the "×" at the far right or the system back key exits multi-select.
3. Deleting shows a confirmation dialog; once confirmed, the custom overrides for those numbers are removed together, each number automatically falls back to the next source layer, and the list refreshes immediately.

The edit page avoids the soft keyboard; when the summary or body is long you can scroll up and down to keep editing the content at the end.

Changes made in the App are fully equivalent to placing files by hand, and the two approaches can be mixed.

## Directory Structure

Prompts live under the AI configuration directory `~/.aiverse/` (inside the container in local mode that is `/root/.aiverse/`; in remote mode it is `.aiverse/` under the server user's home, with reads and writes going to the remote side):

```
~/.aiverse/
├── prompts/          Default prompts (released in full at App startup, overwritten automatically on upgrade)
│   ├── 00-identity.md
│   ├── 10-communication.md
│   ├── ...           The top level holds the "static baseline" snippets, sorted by two-digit numeric prefix (the last built-in one is 99-runtime-context.md)
│   └── agent/        Snippets injected on demand (not part of the static baseline, read only when used)
│       ├── plan-mode.md           PLAN mode reminder
│       ├── auto-mode.md           AUTO mode reminder
│       ├── subagent-base.md       Subagent base operating rules
│       ├── subagent-context.md    Subagent runtime context (identity/workspace/time)
│       ├── compact-summary.md     Long-conversation context compression
│       ├── title-generator.md     Session title generation
│       └── init.md                Instruction body of the /init command
├── prompts.custom/   Your custom snippets (overrides or additions; upgrades don't touch this)
│   ├── 50-my-safety-rules.md
│   ├── 25-extra-requirements.md
│   └── agent/
│       └── plan-mode.md           Override an on-demand snippet
├── skills/
└── docs/
```

Snippets fall into two kinds, with different naming rules:

- **Static baseline** (top level of `prompts/`): `<two-digit number>-<name>.md`, where the number is its identity and determines its position in the system prompt.
- **On-demand snippets** (`prompts/agent/`): no number, read only when used (such as switching modes, compressing context, dispatching subagents, or generating titles).

## Naming Rules

Custom snippets go in `prompts.custom/`:

- **Top-level files** must be named `<two-digit number>-<name>.md` (such as `05-opening.md`, `50-my-safety-rules.md`). The number is the identity:
  - If the number matches a built-in static baseline snippet (the built-in numbers are `00`, `10`, `15`, `20`, `30`, `40`, `50`, `60`, `70`, `99`) → it **overrides** that snippet, and the trailing name can be anything;
  - If the number is not in that set → it becomes a **new snippet**, inserted at the corresponding position in the baseline by number (e.g. `05-opening.md` sorts between `00` and `10`).
- Files that don't match this format (such as `notes.md`, `2024-memo.md`) are ignored.
- When several files share a number, only the one that sorts first by file name is used and the rest are ignored — keep numbers unique.
- Snippets in the **`agent/` subdirectory** have no numeric identity and override by **exact same name** (such as `prompts.custom/agent/plan-mode.md`).

## Loading Priority

Top-level numbered snippets (`<two-digit number>-<name>.md`) are looked up across four source layers; for a given number only the highest-priority copy takes effect, and the search stops as soon as one is found:

1. **Project**: `<workspace>/.aiverse/prompts.custom/` (in remote mode, the corresponding project directory inside the App's private directory)
2. **Global**: `~/.aiverse/prompts.custom/`
3. **Local**: `~/.aiverse/prompts/<same-named file>` — the copy released at startup
4. **Built-in**: the App's own `assets/prompts/`

The on-demand snippets under `agent/` (unnumbered) have no Project layer and are looked up across three layers: "Global custom (`prompts.custom/agent/xxx.md`) → Local → Built-in".

So you only need to put in the few snippets you want to change or add; everything else automatically uses the default version.

## How to Modify

1. Under `~/.aiverse/prompts.custom/` (create it manually if it doesn't exist), put in snippets: name overrides of built-in snippets with the same number, name new snippets with a number not yet taken, and place subdirectory snippets (such as `agent/plan-mode.md`) under `prompts.custom/agent/` accordingly.
2. Edit the content.
3. **Changes take effect immediately after saving** (no need to restart the App).

::: tip Prompt Snippets Take Effect Immediately
Custom prompt snippets are re-read on every request, so edits take effect right away without restarting the App. Note: the skill list, memory, and subagent list are only refreshed when you **start a new session**.
:::

There are three ways to edit: modify directly in the terminal with `vi` / `nano`; have the AI write it for you (its file tools can read and write this directory directly); or [access the App's private directory with a file manager](https://aivapp.cn/md/guide/files.md#工作区文件在手机上的位置).

## Fully Disabling Built-in Prompts

Create a file named `.no-builtin` under `prompts.custom/` (the content doesn't matter — its existence is what takes effect), and the **main agent's** system prompt will then consist only of the numbered snippets you have written at the top level of `prompts.custom/`:

- No built-in snippet is injected anymore, and the skill list, subagent list, memory, project rules, workspace, and current time are no longer appended automatically;
- For whatever dynamic content you need, use the variables below in your own snippets to pull it back in;
- It only affects the main agent — subagents still have built-in snippets injected according to their own definitions;
- If there are no recognizable numbered snippets in `prompts.custom/`, the system prompt will be empty, so make sure you have at least one `NN-xxx.md`.

::: danger This Is "Full Takeover"
Once enabled, the AI's rules of conduct are decided entirely by the snippets you write. Be sure to provide identity, tool descriptions, safety boundaries, and similar content yourself, otherwise the AI will lack necessary context.
:::

## Available Variables

Write these placeholders in a snippet's body and they are replaced with real content at runtime; when a list is empty, they are replaced with an empty string:

::: v-pre
| Variable | Replaced with |
| --- | --- |
| `{{AIVERSE_SKILLS}}` | The list of available skills (one per line) |
| `{{AIVERSE_SUBAGENTS}}` | The list of available subagents (one per line) |
| `{{AIVERSE_MEMORY_GLOBAL}}` | The global memory list (one per line) |
| `{{AIVERSE_MEMORY_PROJECT}}` | The project memory list (one per line) |
| `{{AIVERSE_PROJECT_RULES_GLOBAL}}` | The global project rules body (`AGENTS.md` / `CLAUDE.md` under `~/.aiverse`) |
| `{{AIVERSE_PROJECT_RULES_PROJECT}}` | The workspace project rules body (`AGENTS.md` / `CLAUDE.md` at the project root) |
| `{{AIVERSE_PROJECT_RULES}}` | Equivalent to `{{AIVERSE_PROJECT_RULES_PROJECT}}`, kept for backward compatibility with older snippets |
| `{{AIVERSE_WORKSPACE}}` | The project root (`~/workspace`; a hint text when no workspace is selected) |
| `{{AIVERSE_ENVIRONMENT}}` | The current running environment (local container (PRoot) / remote SSH server) |
| `{{AIVERSE_DATE}}` | The current date (`yyyy-MM-dd`) |
| `{{AIVERSE_SUBAGENT_NAME}}` | The current subagent's name (only set when rendering a subagent's prompt) |
:::

::: tip Variables Are Expanded In Place in Any Snippet
Skills, subagents, memory, project rules, workspace, and the current time in the system prompt are carried by these variables in the built-in snippet `99-runtime-context.md` by default; you can also write these variables in your own snippets, and they are likewise replaced with real content at that spot. To change where or in what order these contents appear, just edit or override `99-runtime-context.md`.
Variables are replaced only with **plain data** (list rows, file bodies, paths, dates); headings and lead-ins such as "Available skills:" are written in the snippet file itself and edited along with the snippet.
:::

When `prompts.custom/` has no override of `99-runtime-context.md`, the runtime context is still injected from the built-in version; if you override it but write no variables, the corresponding content won't appear.

Other placeholders (such as <span v-pre>`{{INSTRUCTION}}`</span> used by the compression prompt) are unaffected.

## Behavior on Upgrade

- The `prompts/` directory is overwritten in full by the built-in version on every startup, so the default prompts are updated automatically as the App upgrades.
- The `prompts.custom/` directory is never written to or deleted automatically by the App, so your customizations are fully preserved after an upgrade.

::: danger Do Not Modify the prompts/ Directory Directly
The content in the `prompts/` directory is reset in full on every startup, so anything you modify by hand is overwritten after an App upgrade or restart. For customization that persists, be sure to save it in the `prompts.custom/` directory.
:::

## Snippet Reference

| File name | Content |
| --- | --- |
| `00-identity.md` | AI identity and role definition |
| `10-communication.md` | Communication and reply style |
| `15-project-rules.md` | Project rules loading convention (AGENTS.md / CLAUDE.md); see [Memory and Project Rules](https://aivapp.cn/md/guide/memory.md) for details |
| `20-coding-discipline.md` | Coding discipline |
| `30-comments.md` | Code comment conventions |
| `40-approach.md` | Way of working and process |
| `50-safety.md` | Safety and trust boundaries |
| `60-tools-and-paths.md` | Tool descriptions and path conventions |
| `70-skills-and-mcp.md` | Skills and MCP descriptions |
| `99-runtime-context.md` | Runtime context: skills, subagents, memory, project rules, workspace, and current time (carried by variables) |
| `agent/plan-mode.md` | PLAN mode reminder (injected on demand) |
| `agent/auto-mode.md` | AUTO mode reminder (injected on demand) |
| `agent/subagent-base.md` | Subagent base operating rules (injected on demand) |
| `agent/subagent-context.md` | Subagent runtime context: identity, workspace, and time (injected on demand) |
| `agent/compact-summary.md` | Prompt for long-conversation context compression |
| `agent/title-generator.md` | Prompt for session title generation |
| `agent/commit-generator.md` | Prompt for generating Conventional Commits messages from the Git diff |
| `agent/goal.md` | Prompt for the goal-driven autonomous loop: keep making progress, run completion audits, and judge when blocked |
| `agent/init.md` | Instruction body of the `/init` command (analyze the code base and generate/improve `AGENTS.md`), triggered with `/init` in the chat input box |

## Restoring Defaults

Delete the corresponding file under `~/.aiverse/prompts.custom/` and that snippet reverts to the default version; delete the whole `prompts.custom/` directory to restore everything (deleting `.no-builtin` as well re-enables the built-in prompts).

## Notes

- Older versions placed `80-plan-mode.md`, `81-auto-mode.md`, and `90-subagent-base.md` at the top level of `prompts/`; **from v1.0.0** they have moved to `prompts/agent/` and had their numbers removed. Please switch the corresponding custom overrides to paths like `prompts.custom/agent/plan-mode.md`.
- Snippets such as `60-tools-and-paths.md` are updated as the tools change. If you override one, an App upgrade won't automatically bring you the new tool descriptions, and the tool definitions the AI sees may not match reality. Update them manually when needed, or delete your custom version to let the default version take effect again.
