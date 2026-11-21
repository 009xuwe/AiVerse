# Tablets and Large Screens

Version note

The large-screen layout described on this page is available from 1.0.0 onward.

On a large screen, AiVerse automatically switches to a two-column layout: the sidebar stays persistent, and you can keep code or a terminal open side by side next to the chat. The interface and controls on a phone remain exactly the same.

## When the Layout Switches

The decision is based on the **width of the current window**, not the screen size:

Window width

Interface form

Narrow (most phones in portrait)

Single column; the sidebar slides out via the hamburger button, same as before

Medium (tablet in portrait, phone in landscape)

Single column, with a slightly wider sidebar

Wide (tablet in landscape, unfolded foldable, desktop window)

Persistent sidebar; the chat page can open a right column

Because the decision is based on window width, dragging AiVerse into a half-screen or third-screen on a tablet automatically reverts it to a single column rather than squeezing everything together.

## Persistent Sidebar

On a wide screen, entering the chat page pins the session list and file tree to the left, with no need to slide them out each time. The hamburger button in the title bar disappears (the sidebar is already persistent).

Tapping a file in the file tree, or selecting another session, does not collapse the sidebar, so you can keep tapping through and viewing things.

## Side-by-Side Workbench

On a wide screen, the right side of the chat page can show a panel side by side:

-   Tap the **terminal** icon in the title bar → the right column opens a terminal; tap again to collapse it
-   Tap the **Git** icon in the title bar → the right column opens the Git panel; tap again to collapse it
-   Tap a file in the file tree, or tap a file-path link in an AI reply → the right column opens the code editor (in the file tree, images open as a fullscreen preview and APKs open in the system installer)

Whichever panel is currently open in the right column, the corresponding icon in the title bar is highlighted.

You can watch the AI change code while checking file contents or running commands on the right, without switching pages back and forth.

**Adjusting the width**: drag the vertical divider in the middle to adjust the split anywhere between 30/70 and 70/30; the ratio is remembered.

**Closing the right column**: tap the ✕ in the top-left of the right column, or press the back key once. If the editor has unsaved changes, it still shows the save prompt first; tapping another file in the file tree, or switching the right column to the terminal / Git, likewise asks whether to save first, so changes are never silently discarded.

## Two-Column Settings Page

On a wide screen, opening Settings shows the settings list on the left (always visible) and the content of the selected item on the right. Switching settings items changes only the right half, with no full-page navigation. The back arrow in the top-left is used to leave Settings and return to the chat.

## Rotation and Split-Screen No Longer Interrupt

When switching between portrait and landscape, or dragging the split-screen size, the interface re-lays itself out but does not reload:

-   Commands running in the terminal keep executing and their output isn't lost
-   Unsaved changes in the editor are still there
-   The chat list stays where it was

## Reading Width

On a wide screen, chat content doesn't stretch across the entire screen; it is limited to a comfortable reading width and centered — overly long single lines add to reading strain. When the right column is open, the left half narrows and the content automatically narrows with it.

## Mouse and Trackpad

When a mouse is connected, moving the pointer over the column divider turns it into a drag cursor; hold and drag left or right to adjust the width of the two columns.
