# Storage

"Settings → Storage" shows, by category, how much space AiVerse occupies on this device and provides an entry for clearing temporary data.

## Overview

The top of the page shows the app's total usage, a composition bar, and the device's remaining space. The full width of the composition bar represents the app's total usage, and its colors correspond one-to-one with the color dots of the categories below. The bottom of the page always shows the "Clear Selected Items" button along with the size of the selected data, and it stays usable while you scroll the list.

Computing the statistics requires traversing the files of each directory, and the container and workspace are usually the slowest. To keep the page from sitting empty, each category is displayed as soon as it is computed; categories not yet computed show "—", and a "Computing…" prompt appears at the top. To recompute, tap the refresh icon in the top-right corner.

## Usage Breakdown

| Category | What It Contains |
| --- | --- |
| Chat History | Database files: conversations, messages, tool results, invocation records, checkpoint index |
| Containers | The root filesystem of each container. Expanding shows each container, plus the largest few directories in the container that uses the most space |
| Container Image Packages | The image archives you downloaded or imported. Once a container is installed it is no longer a necessity, but after deleting it that container can no longer be extracted again |
| Workspaces | Project files (including `.git`) inside the app's built-in workspace. Expanding shows each project, with the subtitle marking the chat attachments inside it |
| AI Configuration & Artifacts | Skills, memory, subagents, MCP configuration, prompts, and docs, plus tool output archives and the vision session cache |
| Checkpoints | Snapshots the AI leaves before modifying files, used for undoing |
| Logs | App runtime logs and AI request logs |
| Cache | The system cache directory; can be cleared at any time |
| Other Data | Scattered data such as settings, background images, terminal fonts, and SSH private keys |
| App Installer | The APK itself; cannot be cleared |

Categories with an arrow next to their name can be tapped to expand the details. "AI Configuration & Artifacts" only shows clearable tool output archives and the vision session cache when expanded; non-clearable content such as skills, memory, and prompts is no longer listed item by item, but is still counted toward that category's total usage. Because of this, the sum of the expandable clearable sub-items may be smaller than the category's total size.

::: tip What Is Not Counted
External workspaces added via "Add External Workspace" live in your device's shared storage, do not belong to the app's usage, and are therefore not counted. The same applies to files on the server in remote SSH mode.
:::

The expanded details for chat history list the conversations that use the most space. It is a byte estimate based on message bodies, which is smaller than the database file itself — the database also includes indexes and page-alignment overhead. To reduce this item, go to the conversation list and delete conversations you no longer need; if you only want to clear the invocation records, reset them from the top-right corner of the "Token Stats" page.

Containers and workspaces cannot be deleted here; please go to the "Containers & Images" page to reset or delete containers, and delete workspaces in the workspace switcher panel, to avoid accidentally deleting an environment that is in use. Deleting a workspace also deletes that workspace's chat history; whether an external local workspace's chat history is deleted is decided by a switch in "Preferences".

## Clearable Space

The clearing entry sits together with the usage breakdown rather than in a separate group. The four clearable items are as follows; none of them include chat history, code, or configuration, but some clearing affects looking back at or following up on past content:

- **Cache Files**: Check the "Cache" category. Temporary files are rebuilt automatically afterwards.
- **Runtime Logs**: Check the "Logs" category. This includes app logs and AI request logs; after clearing, new logs are recorded as usual, and tapping the category still expands to show the usage of both kinds of logs.
- **Tool Output Archives**: Expand "AI Configuration & Artifacts" and check the corresponding details. When AI tool output is too long, a complete copy is saved to disk. After clearing, the output paths mentioned in past messages can no longer be read.
- **Vision Session Cache**: Expand "AI Configuration & Artifacts" and check the corresponding details. This is the history used for vision follow-up questions; after clearing, you can no longer continue asking about old images. These two items are shown only when they have usage.

Logs and cache are selected by default, while tool output and the vision cache are not selected by default. Once you check items, the button at the bottom updates the total size of the selection; collapsing a category does not cancel the selection. Items with no usage cannot be checked, and the button is disabled when there is no clearable data selected.

After tapping "Clear Selected Items", a confirmation dialog lists the clearing targets, the total size, and the impact of each item. After confirming, only those selected items are cleared; once finished, the page recomputes automatically and shows how much space was freed this time.

The clearing scope is the data in this device's app cache, logs, and global directories; tool output and the vision cache may be produced by multiple workspaces and are not limited to the current workspace. It does not clear each workspace's own files, attachments, build caches, or remote server data.

::: warning Clearing Is Irreversible
Deleted files cannot be recovered. Although these items do not contain chat history, code, or configuration, old tool output copies and vision session history cannot be recovered by reusing them.
:::

## What to Do When Usage Is Large

- **The container takes several GB**: The package manager caches inside the container are usually the biggest part (such as the cache directories of Gradle, npm, and pip). Expand the container category to see which directory is largest, then clean it up in the terminal, for example `rm -rf ~/.gradle/caches`. Containers you no longer use can be deleted on the "Containers & Images" page.
- **The workspace is large**: Build artifacts such as `node_modules`, `build`, and `.gradle` make up most of it; deleting them and rebuilding restores the space.
- **Chat history is large**: Tool output within long conversations takes the most space; delete early conversations you no longer need.
- **Image packages are still around**: Once the container is installed, you can delete the corresponding downloaded files on the "Containers & Images → Download Images" page.
