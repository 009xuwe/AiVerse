# Checkpoints and Undo

Before the AI performs a file modification, AiVerse automatically saves a snapshot of the file before the change and creates a checkpoint. If the generated result doesn't meet your expectations or introduces unexpected problems, you can roll back to a specific point at any time with one tap. This mechanism runs fully automatically and needs no manual intervention.

## How to Roll Back

Find the **message you sent yourself** in the chat history that you want to return to. There is an undo icon on its left; tapping it opens the action menu for that checkpoint.

The menu offers three choices:

Action

Code

Conversation

Restore code and conversation

Returns to the state before this message

Deletes the messages after this one, and the original prompt is filled back into the input box

Restore conversation only

Keeps the current state unchanged

Deletes the messages after this one

Restore code only

Returns to the state before this message

Keeps the conversation complete and unchanged

The first one is the most commonly used: it is equivalent to "redoing this round" — both the code and the conversation go back to the starting point, and you can revise the prompt before sending it again.

Restoring the conversation deletes the selected message and all messages after it, undoes the existing compression summary, and restores the original conversation preserved before the rollback point, preventing the summary from carrying later facts into the restored context. "Restore code only" does not change the conversation. When the restored history is too long, it will be re-compressed before the next send. Usage already consumed by calls that have happened will not be reset. Content that was already truncated or missing in the old version cannot be recovered by rolling back.

If you only want the AI to forget a few discussions that went off track but keep the code it has already written, use "Restore conversation only". Conversely, if you want to discard the code but keep the discussion for review, use "Restore code only".

## Scope and Limitations

Checkpoints only record changes the AI made **through file tools**. If a file was changed or deleted by a command in the terminal (`rm`, `mv`, `sed` and the like, whether typed by you or executed by the AI), the checkpoint cannot restore it precisely.

### Another Session Modifying the Same File at the Same Time

When rolling back, AiVerse first checks for each file "whether it has been touched by changes from outside the current session":

-   If the file was changed **by another session or by you** after the checkpoint (changes the current session never saw) → that file is **skipped**, left unchanged, and its name is listed in the prompt. This prevents code you or another session just wrote from being quietly wiped out.
-   If the file was changed by the **current session** itself → it is restored as usual, which is exactly what undo is for.

If the prompt lists skipped files, it means those changes were intentionally kept. To roll them back as well, first verify the content manually or use Git.

Known limitation: files **newly created** during this conversation are always deleted on rollback, even if another session wrote content into them in the meantime — before deletion, it cannot be reliably determined whether such a file was modified.

In this situation, restoring via Git is recommended: as long as the file was ever tracked by Git (a single commit is enough), go to the status tab on the Git page, long-press the file, and choose "Restore file" or "Revert changes" to get it back, regardless of how it was deleted. Conversely, new files that were never committed are not covered by this protection.

For important projects, use Git alongside checkpoints: checkpoints are well suited to undoing single-round changes, while Git is the complete version-control safety net.
