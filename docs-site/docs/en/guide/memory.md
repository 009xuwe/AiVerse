# Memory and Project Rules

This chapter covers two capabilities — automatic memory and project rules — used to keep the AI's context continuous across sessions and consistent with the project.

## Automatic Memory

Automatic memory is a long-term knowledge base maintained by the AI itself. Project conventions, user preferences, and architecture decisions formed during conversations are recorded automatically and reused in later sessions.

### Scopes

Scope

Description

Storage location

Global

Personal preferences that apply across projects

`~/.aiverse/memory/`

Project

Conventions and know-how specific to the current project

Local mode: `/.aiverse/memory/`; remote SSH mode: `~/.aiverse/memory/projects/-/`

Project memory is stored on the phone locally in remote mode and isolated by "project name + server identifier", so it is not affected by container resets.

### Memory Files

Each memory is a Markdown file consisting of YAML frontmatter (`name`, `description`) and a body:

markdown

```
---
name: conventions
description: Project code standards and naming conventions
---
(body)
```

### How It Takes Effect

At the start of a session, the system injects a summary of all memories (names and descriptions) into the prompt; when details are needed, the AI reads the full content through the `memory` tool.

Platform permissions and safety boundaries always apply. Within those boundaries, an explicit request in the current turn takes priority over long-term preferences and default conventions; when nothing is specified, conflicts are resolved by "user memory > project rules > built-in defaults".

### The memory Tool

Parameter

Description

`action`

`read` / `save` / `edit` / `delete` / `list`

`name`

Memory name (the file name)

`description`

Summary; required for `save`

`content`

Body (Markdown); required for `save`

`edits`

List of local edits, with the same semantics as editing a file

`scope`

`project` (default) or `global`

For memories with the same name, the project level takes priority over the global level. When updating an existing memory, prefer the `edit` local edit; for content involving hard-won debugging lessons, record it only after verifying the root cause.

### Managing Memory in the App

Besides letting the AI maintain memory automatically, you can manage it manually under "Settings → Memory":

-   **Create a memory and expand it** (**since 1.0.3**): tap the add button to create a blank memory; it expands automatically after creation, and you can fill in the name, summary, and body directly.
-   **Batch delete** (**since 1.0.3**): check multiple entries in multi-select mode and delete them at once; a second confirmation is required ("Delete the N selected memories? This action cannot be undone").

## Project Rules

Project rules come from two places: the global directory `~/.aiverse` and the current workspace root directory. Each place reads `AGENTS.md` first and falls back to `CLAUDE.md` when it does not exist. Global rules are for conventions that apply across projects; workspace rules are for project-specific rules, architecture constraints, and build guides. When the two conflict, the more specific workspace rules win, and the permission boundaries and conflict-resolution order above are followed.

### How It Takes Effect

Rules from both places are loaded automatically at the start of a session and injected together into the prompt, with no manual configuration needed; any single file whose content exceeds 32,000 characters is truncated.

### Customizing the Loading Convention

The loading convention for project rules corresponds to the prompt fragment `15-project-rules.md`, which can be overridden via `~/.aiverse/prompts.custom/15-project-rules.md`; see [Custom Prompts](https://aivapp.cn/md/guide/custom-prompts.md).
