# Workspace Sync

Sync the workspace directory on your phone to a remote server. The entry point is "Settings → Runtime Environment → Connection & Sync", and the page has four tabs at the bottom.

::: tip Division of Labor Between This Page and "Containers & Images"
This page only handles the **connection channel and file sync**. If you want the AI to use the remote server directly as the execution environment (commands, files, and terminal all run on the server), that is a separate feature — see [Remote SSH Mode](https://aivapp.cn/md/guide/remote-ssh.md) — which reuses the SFTP channel set up here.
:::

## Connection Configuration

Tap + in the top-right to create a connection channel:

- **Protocol type**: `SFTP` (default port 22) or `FTP` (default port 21).
- **Connection name**: an alias.
- **Host address**: IP or domain.
- **Port / username / authentication**: required. For authentication you can choose **password**, or choose **key** (select from private keys already added on the "SSH Keys" page; the passphrase for an encrypted private key is entered on the key edit page and is not asked for again here).
- **Test connectivity**: you can test before saving.

**Sorting**: long-press the name area of a channel row and drag up or down to reorder (the order persists after restart). This order has a real effect — the "Associated connection channel" dropdown when creating a mount and the "SSH channel" dropdown when editing a container both default to the first item in the list.

### SSH Keys and Host Fingerprints

Tap the key icon in the top bar to enter; there are two tabs at the bottom:

**Keys**: manage the private keys used for login. Tap + in the top-right and there are two ways to add one: **Import from file** (pick a private key file from the file picker) or **Paste key** (opens the same form as the edit page; paste the private key content and fill in the name and passphrase — leave the name blank to auto-name it). The private key is copied encrypted into the private directory and its public key fingerprint is parsed out; an encrypted private key is labeled "Passphrase required". Tap a key entry to open the edit page: you can change the name and passphrase (for an encrypted private key), while the private key content is read-only (hidden by default; tap the eye on the right to view it). The passphrase is set on the key and is applied automatically when a connection uses that key. Swipe left to delete.

The key list loads asynchronously after you enter the page and refreshes automatically after importing, modifying, or deleting. When saving a key with a passphrase you don't need to wait on the page; return to the list to see the updated result.

**Fingerprints**: manage the trusted server host fingerprints. The first time you connect to a server, a dialog shows the key type and the `SHA256:` fingerprint; after you confirm "Trust and connect" it is saved and later connections are allowed straight through. If the server's fingerprint changes, you are asked to confirm again — this is the mechanism against man-in-the-middle attacks, so be cautious if a fingerprint suddenly changes. Swipe left to delete, and the next connection asks for confirmation again.

## Mount Directories

A connection channel only solves "can I connect"; you also need to set up the mapping of "which local workspace corresponds to which remote directory". Tap + in the top-right:

- **Associated connection channel**: pick an existing connection from the dropdown.
- **Remote target directory**: the folder icon on the right opens a remote directory browser so you can click through level by level.
- **Map to local workspace**: pick a local workspace from the dropdown (you need to create one in the sidebar first).
- **Connect automatically on app launch**: once enabled, it follows the current workspace — when the App starts or you switch workspaces, it automatically connects the mount mapped to the current workspace and watches for local changes, while disconnecting the mounts of other workspaces. If the connection fails it retries at backoff intervals; switching to another workspace or disconnecting manually stops the retries for that mount, and an unfinished old connection will not enable sync either.

If the same "channel + remote directory + workspace" combination already exists when you save, you are told the mount already exists and the save is blocked.

Once connected, the card offers several actions: disconnect, upload changes, download all, plus a "More" menu on the right (force full upload, clear sync records). Swipe left on a list row to delete it.

::: danger Sync Is One-Way
Automatic sync only **pushes local changes made after connecting to the remote** (it watches local files for additions, deletions, and modifications); changes in the remote directory are **not** pulled down automatically. Connecting does not automatically upload existing files — when needed, tap "Upload changes" manually.

To pull remote files to local, tap "Download all" manually, but be aware this is a **full overwrite-style** pull: it overwrites local files with the same name (it does not delete extra local files), and unsaved local modifications may be overwritten. Please make sure before using it.
:::

### Which Files Get Uploaded

Both "Upload changes" and automatic sync **transfer only changed files**: each file is compared against the last successful sync record using "size + modification time", and is skipped if they match; if the size is the same but the modification time differs, a content hash (SHA-256) is used as a fallback check, and the file is skipped if the content really hasn't changed. These sync records are stored in the App's private directory and remain valid after a restart.

The judgment is conservative: when in doubt it uploads, so nothing is missed. In the "More" menu on the right:

- **Force full upload**: ignores the sync records and re-uploads every file. Use this when the remote directory has been emptied or changed externally and you need to force an overwrite.
- **Clear sync records**: deletes the sync records for that mount so the next upload treats everything as new (equivalent to a full upload).

Also, for files that genuinely changed, SFTP/FTP can only retransmit the whole file — it cannot transfer just the differences.

The connection timeout is 15 seconds, so an unreachable host errors out quickly instead of waiting forever. A connected mount is probed every 15 seconds; when it disconnects, the card shows "Reconnecting" and automatically reconnects at 5, 10, 20, 40, and 60 second intervals, resuming sync once it succeeds.

## Built-in FTP Server

Start an FTP service on the phone so you can manage the projects on your phone from a computer.

The phone and the computer must be on the same WiFi. Once enabled, the UI shows the FTP address (e.g. `ftp://192.168.1.100:2121`; tap the address to copy it), and you can access it from the file explorer on your computer or FileZilla, managing all projects under `projects/`.

The configurable parameters are the listening port, login username, login password, whether to allow anonymous access, and whether to start on boot. After changing them, tap "Save configuration" at the bottom. Saving while the service is running restarts it and disconnects existing connections, after which only the currently configured account can log in; turning off anonymous access also invalidates anonymous login. `anonymous` is a reserved name for anonymous login, so use a different username for normal accounts.

::: warning Stay Safe
Allowing anonymous access means any device on the same network can read and write your project files. Do not enable anonymous access on public WiFi, and it is recommended to turn the service off when you're done.
:::

## Web Server

Start an embedded web service on the phone (default port 7676) that provides a web interface (Web UI), letting you access and manage AiVerse's sessions, files, models, and favorites from a computer browser.

Configurable options:

- **Port**: the listening port (1024-65535, default 7676).
- **Local access only**: listens only on localhost (127.0.0.1), disabling LAN access.
- **Authentication**: requires a password to access the web interface.
- **Access password**: the password entered when authentication is enabled.

While running, the page shows copyable access addresses:

- **LAN address**: uses the device's WiFi IP; other devices on the same network can access it.
- **Local address**: uses localhost; only this device can access it.

The "Start / Stop" button in the bottom-right controls the service, and a failed start shows the reason.

## Sync Settings

- **Follow .gitignore rules**: when enabled, the `.gitignore` in the project root is automatically read to filter which files to sync.
- **Max sync queue per run**: prevents server request overload or network congestion when a large number of files change at once; operations beyond the limit are queued and executed in batches.

In addition, the sync engine always ignores common build and cache directories (`node_modules`, `.git`, `build`, `dist`, etc.) with no manual configuration needed.

After changing settings, tap "Save settings" at the bottom to apply.
