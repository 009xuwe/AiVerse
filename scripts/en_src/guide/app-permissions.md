# App Permissions

"Settings → App Permissions" centrally manages the system-level permissions the app needs. Permission status refreshes automatically when the page resumes (including when you return from the system settings page); items that are already granted do nothing when tapped.

The switches for background keep-alive, screen always-on, agent completion notifications, realtime notifications, and authorization approval notifications are on the [Background Running](https://aivapp.cn/md/guide/background-run.md) page.

## Install Unknown Apps

Shows whether it's currently allowed. When not allowed, tapping this row jumps to the "Install unknown apps" switch page for this app in system settings; the switch must be on before you can install APKs.

## Access Storage

Shows whether it's currently granted. When not granted, tapping pops the system permission request dialog, used for reading and writing workspace files and container images.

If the permission has been permanently denied (the system no longer shows the dialog), a dialog prompts you and you can tap "Go to settings" to enable it manually.

## Ignore Battery Optimizations

Shows whether it's currently exempted. When not allowed, tapping jumps to the system's battery optimization request page to exempt this app from power-saving policies, reducing the chance that the background process gets killed.

## Auto-Start Management

Tapping jumps to your device manufacturer's auto-start management settings page (Xiaomi, Huawei, OPPO, vivo, Meizu, etc.), allowing the app to be launched automatically at boot or after the system terminates it. If the manufacturer's settings page can't be opened or no manufacturer is matched, it falls back to this app's detail page.

Used together with "Background keep-alive" and "Ignore battery optimizations", this further reduces the chance of being terminated in the background — domestic ROMs are fairly aggressive with background restrictions, so if long tasks are often interrupted, it's recommended to enable all three.

## Shizuku Authorization

Shizuku lets this app execute system commands and is a prerequisite for the AI's `Shizuku` tool. This row shows the current status:

- **Not installed / service not running**: tap to open the Shizuku app and follow its guidance to install or start the service.
- **Not authorized**: tap to request authorization, then choose "Allow" in the authorization dialog Shizuku shows.
- **Ready**: authorization is complete and the AI can call the `Shizuku` tool.

The status refreshes automatically when you return to this page from the Shizuku app. For usage and security notes, see [Shizuku Tool](https://aivapp.cn/md/guide/shizuku.md).

## Floating Ball

When enabled, an app icon floating ball stays on top of the screen: **tap** to quickly switch back to the current app, and **long-press** to pop up a control panel beside it where you can adjust the **size** and the **hide transparency**. Enabling requires the "Display over other apps" overlay permission; when not granted, tapping jumps to system settings to enable it.

**Dragging** (**since 1.0.3**): press and hold to drag it to any position on the screen; on release it smoothly snaps to the nearest left / right edge (about a 250ms animation, leaving an 8dp gap from the edge after snapping so it stays fully visible and easy to drag again), and it won't stop in the middle of the screen; the gesture snapping also happens if the gesture is interrupted.

## Phone Control Accessibility

"Phone Control" lets the AI operate your phone for you (reading the screen, tapping, typing, etc.) and requires enabling its accessibility service (able to see the screen and tap for you). This row on the page shows the current status; when not enabled, tapping jumps to the system accessibility settings. For operation rules, the app allowlist, and the floating bar, see the "Phone Control" section of [Shizuku Tool](https://aivapp.cn/md/guide/shizuku.md).
