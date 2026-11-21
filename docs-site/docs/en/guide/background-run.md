# Background Running

"Settings → Background Running" centrally manages the app's background running and notification behavior.

## Background Keep-Alive

When enabled, it shows a foreground notification to prevent the app's process from being terminated by the system when it moves to the background. Even without any terminal sessions and with the AI idle, the process stays alive; after the system reclaims and rebuilds the process, this persistent state is restored automatically. Tapping the notification returns to the app's home page.

It's recommended to enable this before running long tasks like compilation.

The AI automatically keeps itself alive while executing

Even if this switch is off, when the AI starts executing a task it automatically lights up the same persistent notification and holds a CPU wakelock (to avoid replies being interrupted or tools getting stuck after the screen turns off), and it retracts automatically once all sessions' tasks finish. The point of turning the switch on manually is to "stay persistent even when idle": if you want background commands you run yourself in the terminal not to be interrupted, keep it on. When the switch is on, the AI finishing its task won't turn it off.

## Screen Always-On

When enabled, as long as the app stays in the foreground the screen won't turn off automatically, which suits continuously watching AI tasks or terminal output. It stops applying automatically when you switch to another app or return to the home screen; it doesn't affect the system's auto-lock settings and needs no extra authorization.

Leaving it on for a long time uses more power, so it's better used while plugged into a charger.

## Agent Completion Notifications

When enabled, if the app is in the background and the AI completes a round of conversation, a system notification pops up (title "Task completed", with the content being the message you sent; if it was an automatic round triggered by a background task, the corresponding task title is shown). Tap the notification to return to the app and see the result.

It doesn't pop up while the app is in the foreground, so it won't disturb the conversation you're reading.

## Realtime Notifications

When enabled, while the AI works in the background it shows a realtime progress notification with the content being what it's currently doing ("Thinking", "Searching the web", "Executing command", etc.). When multiple sessions run at the same time they're merged into one notification, listing each session's status line by line. It retracts automatically when you return to the foreground or when all tasks finish.

It only shows while the app is in the background, so the conversation you're reading isn't disturbed.

Whether the system can promote the notification to the status bar / lock screen (on some devices it appears as the top "island") depends on the system version: **Android 16 and above** (for example ColorOS 16) displays it as a realtime notification; lower versions show it only as an ordinary notification.

## Authorization Approval Notifications

When enabled, if the app is in the background and the AI has a blocking confirmation waiting for you to handle, a system notification pops up, covering three cases: requesting tool authorization (such as executing a command or rewriting a file), asking you a question, and exiting plan mode to wait for plan approval. When multiple sessions wait at the same time they're merged into one notification, listing each one's status line by line ("Authorization needed", "Answer needed", "Waiting for plan approval"). Tap the notification to return to the app and handle it; it retracts automatically once handled or once you return to the foreground.

When there's only **one** pending item, the notification carries buttons directly: tool authorization shows "Deny" and "Allow" ("Allow" only permits this one time and doesn't remember a rule), and plan approval shows "Approve" and "Deny"; one tap completes the approval without returning to the app. If multiple sessions are pending at once, or the pending item is a question to you, it shows only the content and you tap the notification to return to the app to handle it. The buttons apply only to the approval request the notification corresponds to; when a request has been cancelled or already handled, the old buttons won't approve a later request.

It doesn't pop up while the app is in the foreground, so it won't disturb the conversation you're reading.

Android 13 and above

The first time you enable a switch that needs the notification permission (background keep-alive, agent completion notifications, realtime notifications, authorization approval notifications), it first requests the "Notifications" permission. If the system can't pop the authorization dialog (the permission was turned off or denied before), a dialog prompts you; tap "Go to settings" to jump to the system settings page and enable it manually. The switch has no effect until the permission is granted.

For system permissions (install unknown apps, storage, battery optimization, auto-start, Shizuku), see [App Permissions](https://aivapp.cn/md/guide/app-permissions.md).

## What to Do When the Network Drops After Moving to the Background

After the app has been in the background for a while, the AI conversation may be interrupted with a "Network connection lost" message. This is Android's normal restriction on background apps to save power: when the app isn't in the foreground, the system **freezes** its process, and while frozen it can neither receive nor send data, and already-established network connections get cut. This has nothing to do with whether the screen is off.

**Note**: changing the system's power-saving policy to "Unrestricted" usually doesn't solve this — it only lifts the "Doze / app standby" restriction and can't control process freezing; besides, when the screen isn't off the system never enters Doze at all, so changing it shows no visible effect.

Following the steps below improves things noticeably in most cases:

1.  **Turn on "Background keep-alive" above**, and keep the persistent notification in the notification bar (don't dismiss it).
2.  **Lock the app in the recent tasks screen** (see below for how on each device); don't swipe it away directly — swiping it away often equals a force stop.
3.  **In system settings set the app to "Unrestricted / Don't optimize" and allow auto-start**.

### Settings Paths for Common Brands

The background and power-saving entries are in different places per brand; if you can't find them, search for keywords like "auto-start", "background", or "power saving" at the top of system settings.

Brand

Key path

Huawei / Honor

App launch management → turn off automatic management → under "Manual management" enable "Allow auto-launch", "Allow associated launch", and "Allow background activity"; pull down the recent tasks card to lock

Xiaomi / Redmi

Power & battery → settings at top right → App intelligent power saving → select this app → Unrestricted; App settings → Authorization management → Auto-start management; long-press the recent tasks card to lock

OPPO / OnePlus / realme

Apps → App management → this app → Battery usage → Allow background activity; Apps → Auto-launch; Battery → More settings → Optimize battery usage → Don't optimize; lock the recent tasks card

vivo / iQOO

Apps & permissions → Permission management → All permissions → Auto-start; Battery → Background power management → Allow high background power usage; lock the recent tasks card

Samsung

Battery and device care → Battery → Background usage limits → add this app to "Never sleeping"; Apps → this app → Battery → Unrestricted

Stock Android etc.

Battery → Battery optimization → this app → Don't optimize; if it still drops with the screen off, WLAN → Advanced → Keep WLAN connection during sleep → Always

Brand paths change with system versions, so go by your phone's actual interface.

### A Few Other Possible Causes

-   Some systems, beyond power-saving policies, **separately restrict background apps' network access**; confirm this app isn't forbidden from background networking.
-   After two or three days of uptime, background management gets stricter; rebooting the phone at that point often improves things noticeably.
-   **Long AI answers being interrupted in the background is an expected situation**: when the model produces no output for a long time, gateways in the middle of the network may drop the idle connection, which the app can't fully avoid; it retries automatically once you return to the foreground.
