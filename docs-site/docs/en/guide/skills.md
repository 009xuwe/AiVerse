# Skills

A skill is a "specialized capability extension pack" for the AI — it distills a class of task's standard procedures, best practices, and dedicated knowledge into a guideline document. When the AI encounters a related task it loads and executes the skill on demand, so you do not have to repeat the background rules in every conversation.

The entry is under "Settings → Skills". Enabled skills appear in the `/` menu of the conversation input box, where you can trigger them with `/skillname` and optionally pass arguments (such as `/pdf-report a4`). See 〈Slash Commands〉 in [Chat & Messages](./chat.md) for details.

## Where Skills Come From

A skill is simply a directory; if it contains a `SKILL.md` (or a compatible `CLAUDE.md`), it is recognized automatically. There are two levels:

-   **Global Skills**: Placed in `~/.aiverse/skills/`, shared across projects, and not lost on app upgrades. They follow the execution environment: in local mode that is `/root/.aiverse/skills/` inside the container (mapped to the phone App's private directory); in remote mode it means `~/.aiverse/skills/` on the **server**, with reads and writes done over SSH.
-   **Project-Level Skills**: Placed in `/.aiverse/skills/`, effective only in the current workspace, and they can be committed to git along with the project. In remote mode this means the workspace on the **server**, with reads and writes done over SSH.

For skills with the same name, the project-level one takes precedence.

You can create a skill directly in the App (see below), create the skill directory by hand in the container, or have the AI install it from GitHub. In local mode the App watches the skills directory; after you add, delete, or rename manually, the list refreshes automatically within a few seconds and you do not need to re-enter the page. In remote mode, changes to files on the server are not watched, so after editing by hand you need to leave and re-enter the settings page to refresh the list.

The list of skills available to the AI is generated at the start of a conversation. After you install or enable a new skill, start a new conversation for the AI to use the updated list.

## List Page

Skills are shown in two collapsible groups, "Current Project / Global"; tap a group title to expand or collapse it.

Each row is one skill: icon, name, and description summary, with a colored badge to the right of the name showing whether it is enabled or disabled. Tap a row to open its details; swipe left to reveal the delete button, which pops up a confirmation dialog — it removes the skill directory directly and is **irreversible**.

The "＋" in the top-right corner adds a skill (see 〈Adding Skills〉 below).

### Batch Operations

The "Multi-Select" button in the top-right corner of the list (an icon of a box with a check) lets you handle multiple skills at once:

1.  Tap "Multi-Select" to enter multi-select mode; a checkbox appears on the left of each row, and tapping the whole row toggles it. Both groups share a single set of selections.
2.  The top action bar offers "Enable / Disable / Delete" and "Select All / Deselect All"; the "×" on the far right or the system back key exits multi-select.
3.  "Select All" includes the collapsed group, so nothing is missed just because a group is collapsed.
4.  Deleting pops up a confirmation dialog; after confirming, items are deleted one by one while "Deleting…" is shown, and once done the list refreshes automatically and multi-select exits. In remote mode each skill has to be deleted recursively over SSH, which is slower when there are many items — please wait for the prompt to disappear before leaving the page.

In multi-select mode you cannot tap through to the detail page, nor can you swipe left to delete a single item.

### Sharing and Export (from 1.0.3)

You can send skills to others, one at a time or in bulk, through two entries: the "⋯" menu on the detail page (single) and the "⋯" on the right side of the bottom action bar in multi-select mode (batch).

-   **Share**: Packs the skill's entire directory into a zip and opens the system share sheet (titled "Share Skill"); what the recipient gets is an archive.
-   **Export**: Also packs it into a zip, but lets you choose the save location with the system file picker; on success it prompts "Skill exported".

What gets packed is the **entire skill directory** (including scripts and resource files), avoiding "sending only a markdown and losing the scripts and resources". To import a zip someone shared with you, just use "Import from Archive" above — the flow is symmetric in both directions. If the skill files cannot be read, it prompts that sharing/exporting failed.

## Adding Skills

Tap the "＋" in the top-right corner to open a bottom sheet; first choose the **scope** (Global / Current Project, defaulting to Global), then choose the add method:

-   **Create Manually**: Enters the edit page, where you fill in the name, description, and body to create a skill from scratch.
-   **Import from File**: Choose a `.md` file (`.markdown` / `.txt` also work). The App reads the file's content, builds the skill from its frontmatter's `name`/`description` (falling back to the file name if `name` is missing), and writes it to `/skills//SKILL.md`.
-   **Import from Archive**: Choose a `.zip`. After extracting, the App recognizes every directory inside that contains a `SKILL.md` (or `CLAUDE.md`) and imports them one by one — a single package can contain multiple skills, which is common with packaged downloads from GitHub repositories.

Importing is equivalent to creating manually: files are always normalized into a `SKILL.md` on disk, and afterwards you can edit them the same way with the editor or container commands.

**Situations that get skipped** (a dialog lists the reason for each one and the remaining skills are unaffected):

-   The skill name is invalid (it contains characters that cannot be used in a directory name such as `/` `:`, or it is empty or too long);
-   A skill with the same name already exists in the target scope (it will not be overwritten);
-   The skill body is empty;
-   The archive contains no directory with a `SKILL.md` at all (fails as a whole).

## Detail Page

Three cards: "Enabled" (name, source, and the enable/disable switch), "Summary" (description), and "Body" (the full content of `SKILL.md` in a monospace font, selectable and copyable).

The pencil button in the top-right corner enters the edit page; the **"⋯" menu offers "Share" and "Export"** (**from 1.0.3**, see below).

## Creating and Editing

The edit page has two groups of content:

-   **Basic Info**: Name, description, and scope. The name is the identifier the AI uses when loading the skill, so an English lowercase name with hyphens is recommended (such as `pdf-report`); the description states "when it should be used", and the AI decides whether to load it based on that sentence; the scope chooses "Current Project" or "Global", determining which level the skill directory lands in.
-   **Instruction Body**: The skill's full instructions, supporting Markdown. Write out this class of task's standard procedure, constraints, and caveats clearly.

When the soft keyboard pops up, the editing area avoids it; if the body is long you can scroll up and down to keep editing the end.

The "Save" button in the top-right corner only becomes tappable once both the name and the body are filled in. Saving writes it to `/SKILL.md`, fully equivalent to a hand-written skill, and afterwards you can edit it the same way with a text editor or container commands.

A few things to watch out for:

-   **The scope can only be chosen when creating.** The scope of an existing skill is grayed out — changing it means moving the directory, which you must do yourself in the container.
-   **Renaming only changes the `name` inside `SKILL.md`, not the directory name.** A skill's body often references scripts in the same directory by a path like `~/.aiverse/skills//run.py`; renaming the directory would break those references.
-   When editing a skill installed from GitHub, if it uses `CLAUDE.md` instead of `SKILL.md`, saving writes back to that original file and does not create an extra copy.
-   If a skill with the same name already exists in the same scope, the name contains characters that cannot be used in a directory name such as `/` `:`, or the body is empty, saving fails and a red prompt appears at the top of the page.

## Enabling and Disabling

The switch on the detail page controls whether the skill participates in AI conversations. Once disabled, it no longer appears in the list of available skills in the system prompt and the AI cannot load it, reducing context usage — this switch is very useful when you have many skills.

The disable list is stored in `skills.json`: the global list lives in the current execution environment's `~/.aiverse/skills.json`, saved in the phone App's private directory in local mode and on the server in remote mode; the two are independent and are not migrated or synced automatically. The project-level list lives at `/.aiverse/skills.json` in local mode, while in remote mode it is still saved in the phone App's private directory, isolated by server and workspace. The format is:

json

```
{ "disabled": ["skillname"] }
```

The two levels are unioned; disabling in either scope takes effect. You can also edit the list by hand and it takes effect on the next read; in local mode the list refreshes automatically within a few seconds, while in remote mode you need to leave and re-enter the settings page after editing the list on the server to refresh it.

## AI Managing Skills (manageSkill)

Besides managing manually in the App, the AI can also manage skills directly in a conversation using the `manageSkill` tool: `list` to list, `create` to create, `edit` to edit, `delete` to delete, and `disable` to disable/enable. The parameters match creating/editing in the App (`name`, `new_name`, `description`, `body`, `scope`), and renaming only changes the `name` in `SKILL.md` without changing the directory name. Changes take effect in the next conversation (the available skills list is cached per conversation), consistent with "Enabling and Disabling" above.
