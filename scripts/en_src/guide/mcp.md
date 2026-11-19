# MCP Servers

MCP (Model Context Protocol) lets the AI connect to external tools — querying databases, searching the web, controlling a browser, and the like. Once connected, these tools appear automatically in the AI's tool list and are invoked just like built-in tools.

The entry point is "Settings → MCP servers".

## Two-Level Scopes

MCP configuration has two levels, global and project:

- **Global**: shared by all projects, stored in `~/.aiverse/mcp.json`.
- **Project-level**: only applies to the current workspace, stored in `<workspace>/.aiverse/mcp.json`, and can be committed to git along with the project.

What actually takes effect is the merge of the two, with **project-level taking priority** — for servers with the same name, the project configuration overrides the global one.

## Server List

Each row shows the server name, source (global / project), type (built-in / HTTP), and connection status. When connected it also shows the number of tools; when a local stdio server is not connected it shows the startup command; when a server requires OAuth authorization and you are not signed in yet, it shows "Sign-in required".

- Tap a row to edit it.
- Swipe left to reveal a delete button; deletion applies to the scope marked on that row.
- "Reconnect" in the top-right corner reconnects all servers in one tap, and "+" adds a new server.
- When there are many servers, you can filter quickly by name, address, or command in the search bar at the top.

### Batch Operations

The "Multi-select" button in the top-right of the list (a checkbox with a checkmark icon) lets you handle several servers at once:

1. Tap "Multi-select" to enter multi-select mode; a checkbox appears on the left of each row, and tapping the whole row selects it.
2. The top action bar provides enable/disable, reconnect (a circular arrow icon), and delete (a trash icon); the "×" on the far right or the system back key exits multi-select.
3. "Select all" only checks servers **matching the current search filter**; if you change the search term, the filtered-out entries are unchecked automatically, so you won't accidentally delete entries you can't see.
4. Deleting pops up a confirmation dialog; after confirming, those servers are removed from their respective scopes and disconnected. Enable/disable reconnects and takes effect immediately, so you can keep adjusting while watching the status.

In multi-select mode you cannot tap into the edit page, nor swipe left to delete a single entry.

If you edited `mcp.json` directly inside the container or with an external editor, the app detects and refreshes it automatically within a few seconds: new or enabled servers connect automatically, and deleted or disabled ones disconnect automatically, with no restart needed.

## Adding a Server

At the top of the edit page, first choose the **scope** (defaults to "Project" when adding; editing an existing entry keeps its original scope); the "Enable" toggle below controls whether it participates in connections. The remaining fields come in two kinds, depending on the transport type.

**Name** is only an alias, but it is composed into the tool name given to the model (`mcp__name__toolName`), so it can only contain letters, numbers, underscores, and hyphens; entering Chinese or similar characters shows an error and cannot be saved.

### Remote HTTP

- **Name**: an alias, such as `fetch` or `github`.
- **Server address**: the Streamable HTTP URL of the remote service.
- **Custom request headers**: key-value pairs, used for fixed headers such as an auth token.

### OAuth Sign-In

Some remote services require OAuth authorization (returning 401 on connect and showing "Sign-in required" in the list). On the edit page, turn on the "OAuth authentication" switch and save, then tap "Sign in / re-authorize":

1. The app opens the system browser and jumps to that service's sign-in page.
2. Complete the authorization with your account; the browser shows "Authorization complete, you can return to AiVerse".
3. Back in the app, the server reconnects automatically with the new token and its status becomes "Connected".

- Most services support automatic client registration, so nothing extra is needed. If a service does not support it (such as GitHub), you can enter a pre-registered client ID in "client_id" under "Advanced authentication options".
- "Authorization server" is usually left blank; the app discovers it automatically. If discovery fails, you can fill it in manually.
- While authorization is in progress, if you need to abort, tap "Cancel authorization" directly.
- After the token expires, the app refreshes it automatically; to switch accounts, tap "Sign out" and authorize again.
- Authorization tokens are stored in the app's internal storage (`aiverse/mcp-oauth.json`). They are obfuscated but not encrypted storage, so avoid authorizing with sensitive accounts on a shared device.

### Local stdio

- **Name**: an alias.
- **Startup command**: something like `npx`, `node`, or `python`.
- **Command arguments**: added one by one, such as `-y` or `@modelcontextprotocol/server-xxx`.
- **Environment variables**: key-value pairs passed to the process, such as API_KEY.

stdio servers need to run inside a container: in local mode they run on the current container, and in remote SSH mode they run on the "default container". After switching containers, all stdio servers disconnect automatically and reconnect with the new container; HTTP servers are unaffected.

## Tool Management

After connecting successfully, the "Tools" tab on the edit page lists all the tools this server provides, one card per tool, showing its description and parameter names.

- The search bar at the top supports real-time filtering by tool name or description keyword.
- You can "Enable all" or "Disable all" in one tap for quick batch management.
- The switch in the top-right corner can **disable an individual tool**; once disabled it is no longer offered to the model, saving some context.
- MCP tools and built-in tools share the same permission mechanism: by default every call needs your confirmation, and you can choose "Always allow" in the authorization dialog to remember it.
- When no tools are detected, tap refresh in the top-right corner to fetch them again.

The log button in the top-right corner of the edit page jumps to the "Logs" page and filters by this server name automatically, which is useful for troubleshooting connection failures.

## On-Demand Tool Loading (Experimental)

"Settings → Preferences → Tools → **On-demand MCP tool loading (experimental)**" is off by default. When off, behavior is exactly the same as before: every MCP tool appears directly in the AI's tool list.

With many servers and many tools, dozens or even hundreds of tool definitions eat a lot of context. After turning this switch on, MCP tools are no longer listed individually; only two built-in entry points are kept (always available, not counted toward the tool number):

- **`mcp_search`**: search the tools provided by connected servers by keyword. The `query` parameter is optional; when left empty, all tools are listed. It returns each tool's `name` (the full name, such as `mcp__github__create_issue`), `description`, and `inputSchema`.
- **`mcp_call`**: call a tool by name. The `name` parameter takes the full name returned by `mcp_search`, and `arguments` is an object passed according to that tool's `inputSchema` (pass `{}` when the tool has no parameters).

Typical usage: first find the target tool with `mcp_search`, then call it with `mcp_call`. Names must come from `mcp_search` results; do not invent tool names starting with `mcp__`. The switch only affects MCP tools — **built-in tools are unaffected**, and their order stays the same. Changes take effect on reconnect immediately, with no app restart needed.

## Let the AI Configure It for You

Complex MCP configuration can be handed directly to the AI, and its management tools can be installed to the global or project scope. After adding or removing, the configuration **takes effect in the next session** (a new session automatically connects servers that are not yet connected).
