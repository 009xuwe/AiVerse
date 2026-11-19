# Logs and Troubleshooting

When the app misbehaves, logs are the most direct clue. "Settings → Logs" lets you view them right inside the app.

## Viewing Logs in the App

It opens on today's logs by default, and you can do the following on the page:

- **Log level**: the settings row at the top shows the current level; tap it to choose from a bottom panel (Verbose / Debug / Info / Warn / Error / None). When chasing a bug, set it to `Debug`; keep it at `Info` day to day. Release builds default to `Info`, filtering out debug noise.
- **Switch files**: tap the "Log file" row to bring up a list of dates from the bottom, with today selected by default.
- **Search and copy**: filter by keyword in real time, and copy the current filtered results with one tap in the top-right corner.
- **Color-coded levels**: Verbose gray, Debug blue, Info green, Warn orange, Error red.
- **Copy a single line**: tap any line to copy that full log entry (with time, level, and source); long-press to bring up a menu where you can choose the copy scope — this entry, this entry plus its stack trace, or just the message content.
- **Live scrolling**: on by default; refreshes every 3 seconds and follows the latest lines. Turn it off to stop refreshing and keep the position you scrolled to manually.

To avoid stuttering on huge logs, the app only shows the last portion of the current file under the current filters; view the full file in the file system.

## Where the Log Files Are

```
/storage/emulated/0/Android/data/com.aiverse/files/logs/
```

No root is required; you can open it with the phone's built-in file manager.

Workspace project files and AI configuration are stored in the app's private directory, which ordinary file managers cannot see; for how to access them see [File Browsing and Code Editing](https://aivapp.cn/md/guide/files.md#工作区文件在手机上的位置).

## Filter by MCP Name

When troubleshooting an MCP connection problem, go to "Settings → MCP servers", edit a server, then tap the log button in the top-right corner. It jumps to the Logs page and filters by that server name automatically (a filter badge appears at the top of the page and can be cleared in one tap). The log button in the top-right of the list page shows all logs.

## How Crashes Are Handled

When the app hits an uncaught exception it **does not crash straight to the home screen**; instead it shows a full-screen crash error page:

- The page shows the version, device, time, and full stack trace at the moment of the crash.
- Tapping "Copy error info" copies the complete crash report (with the stack trace and log file path) so you can paste it straight to a developer, without digging through logs yourself.
- You can choose "Restart app" to return to the main screen, or "Exit app" to quit completely.

Crash logs are likewise written to the `logs/` directory above.

Besides the uncaught exceptions above, exits that never reach the error page — native crashes, ANRs (not responding), being killed by the system for low memory, or being killed by a signal — are also recorded automatically to the `logs/` directory the next time you open the app, making it easier to investigate "it crashed but I never saw an error page".

## When Long Tasks Are Interrupted in the Background

If the AI is running a long task (such as compiling or a deep code review) or the terminal is running a time-consuming command and it keeps getting killed when moved to the background, go to "Settings → App permissions" and enable **Keep alive in background**, **Ignore battery optimization**, and **Auto-start management**. See [App Permissions](https://aivapp.cn/md/guide/app-permissions.md) for details.
