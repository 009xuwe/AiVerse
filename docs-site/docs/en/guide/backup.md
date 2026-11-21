# Backup and Restore

"Settings → Backup and Restore" lets you export the app configuration, chat history, credentials, and workspace files into an encrypted archive, and also restore from a backup.

## Export

At the top of the page, check the scope to export: AI providers, connections & sync (including Git credentials), chat history, MCP servers, authorization rules, app settings, memory, message favorites, skills, script plugins, and workspace files (**last**). The checked state is remembered and reused the next time you come in.

Among these, **workspace files are off by default** and sit at the **very last position** of the export scope list, needing to be turned on manually when needed. It packs all internal workspace files of local mode, including the `.git` directory, so the repository history and unpushed commits can be fully restored. External workspaces added via "Add external workspace" are original directories on your device and won't be packed. The exclusion rules are each workspace's own `.gitignore` plus the sync feature's default ignore list (`node_modules`, `build`, `dist`, etc.), and symbolic links aren't backed up.

Prefer Git for code backups

A Git repository already fully preserves your code and history, so the export here is better suited as an extra fallback rather than the primary way to back up code.

Tapping "Export backup" pops up a dialog to set a passphrase:

-   **Enter a passphrase**: uses AES-GCM encryption.
-   **Leave empty**: outputs a plaintext tar.gz.

The exported package contains credentials such as API keys, and is saved through the system file picker to the location you specify.

Be sure to remember the encryption passphrase

An encrypted backup cannot be decrypted and recovered if the passphrase is lost; there is no backdoor or recovery path.

## Import

Restore from a backup file using a **merge** approach, which won't clear existing data. You need to enter the corresponding passphrase; for unencrypted backups just leave it empty.

Only backups that had "App settings" checked at export time restore the theme, color scheme, and preferences. Chat history exported on its own, as well as backups without "App settings" checked, keep the current app settings after import.

An encrypted backup is fully decrypted and verified once before preview, and the verification result is reused when you confirm the restore, without decrypting again. Larger backups need enough app-internal storage space to hold the temporary archive; temporary files are cleaned up after completion, cancellation, or failure.

If the backup contains workspace files, a "Restore workspaces" selection dialog pops up first, listing each workspace in the backup (name and file count), and you can check which ones to restore.

Restoring workspaces overwrites directly

The checked workspaces overwrite the files in the local workspace of the same name, and this cannot be undone. When there's no local workspace of the same name, one is created automatically and then written to, so you won't lose data after switching devices or reinstalling. Remote-mode workspaces (where files live on a server) don't participate in the restore.

After import completes, a summary list of the restored data is shown: how many providers, Git credentials, remote connections, remote mounts, chat sessions, chat messages, todos, MCP servers, global authorization rules, memory, message favorites, skills, script plugins, and workspace files were restored.

Chat sessions are placed back according to the workspace they belonged to at backup time: if a local workspace of the same name exists, they're restored into it directly; if not, an empty workspace is created automatically and the sessions are attached to it (consistent with the "Restore workspaces" auto-create logic), so after switching devices or reinstalling, each workspace's sessions won't get lumped into one place.

## Network Backup

Besides exporting as a local file, you can upload backups directly to the cloud or pull them back from the cloud to restore. The bottom of the backup page has two tabs, "Local backup / Network backup": the local page manages the export scope and export / import, while the network page manages the backup targets. After switching to "Network backup", use the "＋" at the top right to add a **backup target**; two types are supported:

-   **WebDAV**: fill in the server address (including the path, e.g. `https://dav.jianguoyun.com/dav`), remote directory, username, and password.
-   **Object storage (S3)**: fill in the service endpoint, region, bucket, and Access Key / Secret Key; you can add a Session Token and an object prefix, and path-style addressing can be toggled. Compatible with AWS S3, Cloudflare R2, Alibaba Cloud OSS, Tencent Cloud COS, MinIO, and other S3-compatible services.

Each target can "Test connection" to verify the configuration. Once configured:

-   **Upload to cloud**: exports the currently checked backup and uploads it to the selected target; you can set a passphrase to encrypt it, or leave it empty to upload in plaintext.
-   **Restore from cloud**: lists the backups on the selected target (in reverse chronological order); pick one to download and restore, with the same flow as importing from a local file (including passphrase verification and workspace selection).

Every target card has "Upload" and "Restore" buttons at the bottom; tap a card to edit, and swipe left to delete.

The remote filename looks like `aiverse-backup-.tar.gz`. The passwords and keys in a backup target are encrypted and stored by the system keystore (Android Keystore), independent of the backup's own encryption passphrase.

Cloud backups also contain credentials

An unencrypted backup contains plaintext credentials such as API keys; when placing it on public storage, be sure to set a passphrase to encrypt it.

## Additional Notes

The backup passphrase only applies to the exported backup file itself. Sensitive configuration stored inside the app, such as API keys, connection passwords, and SSH private keys, is encrypted and stored separately by the system keystore (Android Keystore), independent of the backup passphrase; when switching devices, please migrate via backup import, since directly copying the app data directory can't be decrypted due to mismatched keys.
