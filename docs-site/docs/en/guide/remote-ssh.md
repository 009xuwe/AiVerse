# Remote SSH Mode

Once you select a "Remote SSH" container, AiVerse enters remote workspace mode: the AI's **file read/write, command execution, and terminal all happen on the remote server**, `~/workspace` is mapped to a specified directory on the server, and the phone serves only as the operating interface.

This suits scenarios where the server already has a complete toolchain, or where you need the AI to modify a remote project directly.

When you switch back to the local container, the remote terminal tabs are closed, the remote execution and file access connections are disconnected, and automatic reconnection stops; selecting the remote container again reconnects.

## Prerequisite: Set Up an SFTP Channel First

Remote SSH reuses the connection details (host, port, username, password) of an **SFTP** channel on the "Connection & Sync" page, so you need to set up an SFTP connection on that page first — see [Workspace Sync](https://aivapp.cn/md/guide/sync.md).

The FTP channel does not support command execution and will not appear in the selectable list.

## Create a Remote SSH Container

Under "Settings → Containers & Images", tap the + in the top-right corner and choose "Remote SSH":

-   **Name**: The alias for the configuration.
-   **SSH Connection Channel**: Pick a configured SFTP channel from the dropdown. When no channel is available, the interface prompts you to configure one on the "Connection & Sync" page first, and the save button is disabled.
-   **Remote Workspace Path**: The directory on the server that the AI's `~/workspace` maps to. When you select a channel it is filled in automatically with the default `~/.aiverse/workspaces` (resolved against the real home directory of that user on the server, e.g. `/root/.aiverse/workspaces` for the root user); you can modify it.

After saving, selecting it in the list makes it take effect.

Authentication follows the configuration of the selected SFTP channel and supports both **password authentication** and **SSH private key authentication** (including passphrases).

## Connection Behavior

-   **Automatic Connection**: If the App is in remote mode at startup, it connects automatically in the background; a failed connection does not block the interface and is retried when you first run a command.
-   **Keep-Alive and Automatic Reconnect**: After connecting, a heartbeat is sent every 30 seconds to prevent idle disconnection; on disconnect the background reconnects automatically (with a backoff interval starting at 5 seconds and capped at 30 seconds), and it also retries once immediately when the App returns to the foreground.
-   **Status Display**: The top of the chat page shows "SSH: Connected / Connecting / Connection Failed / Not Connected". When not connected, the message area shows a waiting prompt.
-   **Two Independent Connections**: File read/write goes over SFTP while the terminal and commands go over exec, each using its own SSH connection without affecting the other; if the file channel fails it is rebuilt automatically without interrupting commands that are running.
-   **Path Bootstrap**: If the remote workspace path does not exist it is created automatically; if that directory contains no subdirectories, a workspace named `default` is created automatically, ensuring the AI always has a usable `~/workspace`.

## Local Data vs. Data That Follows the Execution Environment

In remote mode, commands, files, the terminal, and the workspace all live on the server. Most configuration is still stored locally on the phone; a few "execution environment resources" follow the environment and defer to the remote side:

Item

Description

Chat history, model and provider configuration, API keys, app settings

All local

Memory

Local. Project-level memory is isolated by "project name + server identifier", so identically named workspaces on different servers are not mixed up

Global skills, subagents, and panel scripts

**Follow the execution environment**: in remote mode, read/write `~/.aiverse/skills/`, `~/.aiverse/agents/`, `~/.aiverse/scripts/` on the **server**, independent of the copies in local mode

Project-level skills and subagents

Read/write `/.aiverse/skills/`, `agents/` inside the **server workspace** directly, and they can be committed to git along with the codebase

Project-level configuration (tool authorization rules / MCP / disable lists for skills and subagents)

Local on the phone. In remote mode it is isolated by "project name + server identifier" under the App's private directory and is not written to the server

MCP servers

HTTP-type servers are connected to directly by the App; stdio-type servers run on the "Default Container" (see below), not on the remote server

Logs and tool output

Local

After a successful connection, AiVerse syncs the built-in documentation to the server's `~/.aiverse/docs/`; in remote mode the AI can also consult these docs.

## Default Container

In remote mode, some features still need to run locally, for example stdio-type MCP servers. They run on the **Default Container**, which defaults to the built-in Alpine and can be changed to any local container in the "Default Container" group below the container list.

After switching containers (including switching to Remote SSH), all stdio MCP servers disconnect automatically and reconnect with the new container; HTTP MCP servers do not depend on the container and are unaffected.

## Where Git Signatures and Credentials Go

The **signature** saved in the Git page is written to the current execution environment — in remote mode, that means it is written to the git configuration on the server.

**Credentials** use the phone's local credential file as the single source of truth. In remote mode you can enable "Auto-inject Credentials" on the credentials page (remembered per server, asked on first entry) to sync credentials to the server. Once enabled, initializing or switching workspaces syncs and updates the effective scope: it only applies to repositories under the current workspace directory, does not affect repositories in other directories on the server, and does not modify the server user's global git configuration. Credentials are uploaded by the assistant over SFTP and the original file is replaced only after a complete write; if the upload fails, the original file is kept. When the remote assistant cannot find matching credentials it returns immediately rather than waiting for a popup on the phone; if credentials need to be added, please add them on the App's credentials page and then sync.
