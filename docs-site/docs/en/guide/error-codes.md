# Common Error Messages

When an AI request fails or the network fluctuates, the chat page first shows a bubble reading "Retrying N/6", with the reason for this retry noted on the bubble's first line; when the retries are exhausted and it still fails, the error is shown as a red bubble. This article explains what these messages mean by category and how to troubleshoot them.

## Retry Message Categories

When the connection drops mid-request or the server has a temporary problem, AiVerse retries automatically (up to 6 times, with exponential backoff). Before each retry, the bubble first shows the reason for the failure:

Bubble message

Trigger condition

Possible causes and troubleshooting

Rate limit (429)

The server returns 429

Requests are too frequent, or your plan's quota is nearly used up. Just wait for the automatic retry; if it happens often, check your usage in the provider's backend

Server overloaded (503)

The server returns 503

The model service or relay station is under heavy load — a temporary fault. Waiting for the automatic retry is enough

Server error (5xx)

The server returns 500 / 502 / 504, etc.

A server-side fault or gateway problem, unrelated to your configuration. Retry later

Connection timed out

Timed out establishing the connection or waiting for a response

A slow network, an unresponsive service, or a slow relay. Check the network and, if necessary, switch networks or nodes

Connection refused

No service is listening on the target port

The provider's Base URL is wrong (wrong domain / IP or port), or the service isn't running — verify it under "Settings → AI Providers"

DNS resolution failed

Domain name resolution failed

The network isn't connected, or the domain in your custom Base URL is misspelled

Connection interrupted

The connection was established but then dropped, or the stream was cut off

Network jitter, an unstable proxy, or the server closing the connection actively. The automatic retry handles it; if it happens repeatedly, check your network and proxy settings

SSL handshake failed

TLS certificate verification failed

With a custom Base URL, confirm the protocol (https) and certificate are correct, or the server's certificate may be faulty

Network connection lost

Other network-layer faults

Mobile network switching, a weak-network environment. Check the network and retry manually

Network error

Errors that cannot be classified

Look at the specific error text; if you still can't resolve it, report it with logs attached

## Common HTTP Error Codes

When a request fails, the retry bubble and the final error bubble include the HTTP status code. The meaning of common status codes is as follows:

Status code

Meaning

Possible cause

401

Authentication failed

The API Key is wrong, expired, or revoked — check the key under "Settings → AI Providers"

403

Insufficient permissions

The key lacks permission for that model or endpoint, a regional restriction, or a zero balance

404

Endpoint not found

The custom Base URL path is wrong (e.g. missing `/v1`) — verify it in the provider settings

429

Rate limit

Requests are too frequent or the quota is used up — retry later or upgrade your plan

500

Internal server error

A temporary server fault, unrelated to your configuration

502

Bad gateway

The relay station's upstream is unavailable

503

Service overloaded

The server is under heavy load — retry later

504

Gateway timeout

The upstream response timed out

## Error Codes Returned by Tools

When the AI fails to call a tool, the tool card in the chat shows the reason for the failure, some of which carry fixed error codes. The ones related to file editing are as follows:

Error code

Meaning

What to do

`STALE_FILE`

The file to be changed has already been modified by **another session or the user**; the content the AI holds is out of date, and this change was **not written**

No need to worry — the AI re-reads the file and redoes it. If it happens repeatedly, another session really is editing the same file at the same time, so stop one of them first

`FILE_NOT_FOUND`

The target file does not exist

Confirm the path; the AI will usually create it with `writeFile` instead

`NO_MATCH`

The original text that `editFile` is meant to replace cannot be found in the file

The file content doesn't match what the AI expected; the AI re-reads and retries

`MULTIPLE_MATCHES`

The original text to be replaced appears more than once in the file, so it can't tell which occurrence to change

The AI adds more context and retries

`FILE_EXISTS`

On writing, the target already exists and overwriting is not allowed

Normal protective behavior — the AI switches to `editFile` for a partial modification

## Multiple Sessions Editing the Same File

AiVerse supports multiple sessions (including subagents) working in parallel. When two sessions modify the same file at the same time, if left unchecked, whichever writes later will **silently overwrite** the other's changes, and both will believe they succeeded.

To prevent this, AiVerse has three layers of protection:

-   **Same instant**: writes to the same file are serialized, so two sessions never interleave writes to the same file.
-   **Across instants**: after the AI writes a file it remembers the version; before changing the same file again it compares first, and if the file was changed in the meantime it refuses to write and prompts to re-read (this is the `STALE_FILE` above).
-   **Atomic write**: when writing a file it writes a temporary file first and then swaps it in as a whole, so a concurrent read never sees "half-written" content, and a failure mid-write leaves no half file behind.

You usually don't need to intervene. What to watch for: **after you edit a file yourself in the editor, the AI's write may also be judged stale** — this is expected behavior; it re-reads and then edits, and won't overwrite your changes. Conversely, if the AI has just changed a file and you save it in the editor, the editor will prompt about a conflict, letting you choose to overwrite or abandon your own changes.

### Behavior When Undoing Code

"Undo to a checkpoint" tries to restore the file content as it was then, but **will not overwrite changes it doesn't know about**:

-   If the file was changed by **another session** after the checkpoint → that file is skipped, listed in the prompt, and its changes are kept.
-   If the file was changed by the **current session** itself → it is restored as usual (which is exactly what undo is for).

Known limitation: if a file was **newly created** during this conversation, it is always deleted on undo, even if another session wrote content into it in the meantime. Before deleting, it cannot be reliably determined, so pay attention to the prompt when undoing such files.

Want Fewer Conflicts

Have parallel sessions each take responsibility for different files. If two sessions really must edit the same batch of files over a long period, run them staggered rather than at the same time.

## Relation to Multi-Key Switching

After you enable multi-Key under "Settings → AI Providers", only errors that are **inherent to the key itself** (authentication failure, insufficient permissions, rate limiting, exhausted balance) consume the failure count and trigger Key switching; errors such as server 5xx, request timeout, and unreachable network cannot be fixed by switching Keys, so no switch is triggered by mistake. With multi-Key off, there is no alternate key for rate limiting (429), so it backs off and retries like any other transient fault. For details, see [AI Providers and Models → Multi-Key](https://aivapp.cn/md/guide/providers.md).
