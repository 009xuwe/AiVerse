# Preferences

"Settings → General Settings → Preferences" holds the global preference switches that are not tied to a specific provider or a particular purpose. Changes take effect immediately; there is nothing to save.

## Models

**Automatically remove invalid models after fetching** (enabled by default)

When enabled: on the [provider edit page](https://aivapp.cn/md/guide/providers.md), after you tap "Fetch models", as long as the server successfully returns a model list, the models in the local list that the server no longer returns will be removed automatically, and a toast tells you how many were removed.

-   When the request fails or the server returns an empty list, the local list is left untouched, so a network glitch won't wipe out the models you have already added.
-   Manually added models that happen not to be in the server's response are removed as well. If that bothers you, you can turn the switch off.
-   When the switch is off, fetching only lists the new models for you to select and no longer changes the existing list.

**Auto-compression threshold** (85% by default, accepts an integer from 1 to 100)

When the next request is expected to use this proportion of the model's available input budget, context compression is triggered automatically (earlier conversation is summarized into a handoff summary). The budget takes the model's input limit into account and reserves space for the reply, so it is not necessarily equal to the model's advertised total window. A larger value compresses later; a smaller value compresses earlier but summarizes more often. Even set to 100%, it cannot bypass the hard input limit.

## Sessions

**Session on startup** (default "New session")

-   **New session**: every time you open the App you start from a new session. If the current workspace still has an empty session with no messages sent, it is reused directly instead of creating another one.
-   **Open recent session**: you go straight into the most recently updated session in the current workspace and continue where you left off. If the workspace has no session yet, a new one is created. Pinned sessions are not given priority — even if there is a pinned session, you still enter the most recently updated one.

Switching workspaces follows the same preference.

**Delete chat history of external workspaces** (disabled by default)

Controls how chat history is handled when you remove an external local workspace added via "Add external workspace". When enabled, removing an external local workspace also deletes its sessions and messages; when disabled, only the association is removed and the chat history is kept, so re-adding the same directory later still shows the history.

Deleting the app's built-in workspaces and remote workspaces is not affected by this switch: when you delete them, both the directory and the chat history are removed together.

## Input

**Enter key sends message** (disabled by default)

When enabled, the Enter key on the soft keyboard becomes a send key — one tap sends the message; when disabled, Enter inserts a newline and the send button sends the message. For multi-line input, keeping it disabled is recommended.

## Network

Both timeouts are in seconds — just type a number in the field, and **entering 0 means no limit**. After saving they take effect immediately for subsequent requests, with no restart needed.

**First-token timeout** (300 seconds by default)

The maximum time to wait for the first valid content (body, thinking, or tool call) after a request attempt starts, including the time waiting for the server's response headers. If exceeded, the request is cancelled and retried according to the retry-count setting; each retry restarts the timer. You can increase this for models that start slowly or think for a long time. Once the first valid content is received, this timeout no longer limits the duration of the rest of the reply.

**Chunk interval timeout** (300 seconds by default)

During a streaming reply, the longest allowed wait between two adjacent response chunks. If exceeded, the connection is closed and retried according to the retry-count setting, preventing a round from staying stuck at "running" when the server stops pushing; you can increase it when the model produces chunks slowly.

**Retry count** (6 by default)

The App periodically probes the HTTP/2 connection and disconnects when it finds the connection has stopped responding, handling it according to the retry settings; HTTP/1.1 connections do not support this probing. When the UI shows "retrying", it may be waiting for backoff, or it may already have sent the next request and be waiting for content.

The number of exponential-backoff retries after a network request fails (not counting the first request; the wait starts at 0.5 seconds, doubles each time, and is capped at 10 seconds per attempt). Within the same request, even if the connection drops again after some content has already been output, the network retry count accumulates rather than restarting. Entering 0 means failures report an error directly without retrying. This is also affected by multi-Key automatic switching — failures classified as an invalid Key will first try switching to another Key and resending.

## Tools

**sendFile per-file limit** (100MB by default, accepts an integer greater than 0)

The maximum size allowed for a single file when the AI sends a file to the chat area with `sendFile`. Files over the limit are not sent, and the AI receives the reason for the failure. Increasing the limit allows larger files, but a single file is first copied into the app's private directory before being shown, so very large files may consume considerable storage.
