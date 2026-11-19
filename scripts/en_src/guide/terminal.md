# Terminal

AiVerse has a full terminal built in. In local mode it connects to the Linux container on your phone; in remote mode it connects to your SSH server — in both cases it is a real shell where you can install packages, compile, and run services.

Enter it from the "Terminal" item in the title bar at the top of the chat page. Tap the back button in the top-left to return to the chat page; repeated taps during the return won't keep leaving the chat page. Returning does not close the terminal tab.

## How the AI Chooses a Command Tool

The AI chooses how to execute based on whether the task is time-consuming and whether the session needs to be kept alive:

- **Bash**: for short, one-off commands, such as querying status or doing a quick check; it returns the result once execution finishes.
- **Terminal**: for time-consuming tasks like builds and installing dependencies, for long-running processes like dev servers, and for interactive operations that need continuous input. You can view output and keep interacting in the terminal tab.

For time-consuming tasks that finish on their own, the AI can enable a completion notification and continue processing the result once the task ends; for long-running services and interactive programs, it views output, sends input, or stops the process as needed.

## Tab Management

The terminal supports multiple tabs at once; the tab bar at the top scrolls horizontally, and the current tab automatically scrolls into view when you switch.

- The dot before a tab name indicates status: green is running, gray is finished.
- Tap the × on the right of a tab to close it, and tap the + at the end of the tab bar to create one. After closing the current tab, it automatically switches to an adjacent one.
- A terminal opened by the AI while running a command also appears here, and you can switch over at any time to check how it's running.
- After the AI enables a completion notification for a background task, both local and remote SSH modes send the exit code and the tail of the output back to the session that started the task when it finishes; if the AI is busy, it receives the notification during processing or after the current turn ends. If you stop the current AI reply, the pending completion notification is kept and delivered after this turn wraps up. Completion notifications arrive through a separate notification channel and do not enter the user message queue.

## Input and Copy

Tap the terminal area to focus it and you can type directly. Long-press to select and copy output content.

## Auxiliary Key Bar

Phone keyboards lack Esc, Ctrl, and arrow keys, so the terminal provides a horizontally scrollable auxiliary key bar at the bottom:

- **Control keys**: `ESC`, `TAB`, `CTRL`, `ALT`. After Ctrl or Alt is pressed it highlights, and pressing a letter key then forms the key combination.
- **Arrow keys**: up, down, left, right; long-press sends continuously for quickly moving the cursor.
- **Common combinations**: `C-c` interrupts the current command, `C-d` sends EOF or exits, `C-z` suspends the process, `C-l` clears the screen.
- **Common symbols**: `|`, `~`, `/`, `-`, `_`, `>`, `:`, `$`, `"`, `'` and more, saving you from switching to the symbol keyboard.

## Appearance Settings

Tap the gear icon in the top-right of the terminal page (or go to "Settings → General Settings → Terminal Settings") to adjust:

- **Color theme**: the default is "Follow app" (it automatically matches the current app theme's light/dark mode and background tone), and there are also five classic dark themes built in — Termius Dark, Dracula, One Dark, Monokai, and GitHub Dark — plus the GitHub Light bright theme. Selecting one takes effect immediately and changes all open tabs together.
- **Font size**: stepless adjustment between 5 and 22, with a live preview at the top of the panel; you can also pinch to zoom directly in the terminal area, which is the same value as here.
- **Font** (**from 1.0.0**): the default is JetBrains Mono (no ligatures), where `0` and `O`, `1` and `l` are clearly distinguishable. You can also switch to the system monospace font, or tap "Import" to pick a `ttf` / `otf` / `ttc` font file from your phone; imported fonts appear in the list, and tapping the × on a font label deletes it — deleting the one in use automatically falls back to the system monospace. Chinese characters are always filled in by the system font. The font only affects the terminal and does not change the AI chat area.
- **Cursor style**: three options — block, underline, and bar.
