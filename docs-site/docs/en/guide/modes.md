# Four Modes: Build / Plan / Auto / Expert

The mode button on the left of the input field opens a menu where you can switch between Auto, Build, Plan, and Expert. What distinguishes the four modes is the AI's **scope of permissions**.

## BUILD (Build Mode)

The default mode, with full functionality. The AI can write files, change code, and run build commands, but every operation that needs authorization pops up a prompt.

This is the recommended mode for everyday development. For high-risk commands blocked by the built-in safety guard (such as `rm` on the root directory), the AI can retry with `elevate: true`, handing a one-time authorization to you (see [Tool Authorization](https://aivapp.cn/md/guide/permissions.md)).

## PLAN (Plan Mode)

Once in this mode, the tool permission policy blocks implementation actions such as editing files and running commands. The AI can use the allowed read-only tools and their actions to read code, look up material, answer questions, and work out a plan.

Good fits for this mode:

-   You are not yet sure how to change something and want the AI to explore the codebase and propose an approach first.
-   The change involves important architectural trade-offs, has an unclear blast radius, or carries higher risk, and you want to see a plan before deciding.
-   You only want to ask questions and understand the existing implementation, without having it touch anything.

The AI judges whether to plan first based on risk and uncertainty; when the goal is clear, risk is manageable, or the approach is already approved, it can proceed directly. Pure Q&A and approach discussion can stay in PLAN, and it can request to exit and wait for approval when it is time to start implementing.

## AUTO (Auto Mode)

Ordinary tool calls (writing files, running commands, terminal operations) are approved automatically, letting the AI execute continuously within the scope of the current task. Operations that are hard to undo, affect shared state, or exceed the current task scope still require explicit, per-operation authorization — for example force-pushing, modifying a remote, sending messages, or opening/merging a PR.

This mode has very broad permissions

Catastrophic deletions (such as `rm` on the root or a system directory) and suspected destructive commands that cannot be judged statically are still blocked; when truly necessary, the AI can request a one-time authorization and retry. `Shizuku` host Android operations are still confirmed one by one, while the remaining ordinary tool calls are approved automatically. You can turn on "Disable safety interception" under "Settings → Tool authorization" to remove these fallback confirmations. It suits scenarios where you clearly understand the risk and want the AI to run batch tasks fully automatically. It is not recommended for unfamiliar projects or when you are unsure what the AI will do.

## EXPERT (Expert Mode)

Expert mode is coordinated by the Lead (the main brain): it first understands the goal, breaks down the task, and produces a plan, then spins up sub-agent experts to execute in parallel as needed, and finally consolidates and decides. It suits complex multi-file, cross-module, or collaboration-requiring tasks.

-   Complex tasks produce a plan for your approval first, and experts are spun up to execute only after you confirm; small tasks that cannot be split (a one-liner, a single query, a small single-file change) need not be split.
-   Built-in expert roles (research, implementation, review, testing) are available, and you can also define custom roles on the fly; experts discuss with each other directly as equals by id, each reporting to the Lead, and the Lead consolidates.
-   Permissions are the same level as BUILD: modification operations still pop up for authorization, making it stricter than AUTO.
-   Expert mode has the exclusive `reviewPr` tool, which fetches the changes of a remote GitHub PR (commits, file list, diff) for review.
-   At most 5 sub-agents run at the same time globally; when the limit is reached, wait for some to finish before dispatching new tasks.

## The AI Can Also Request a Switch

Besides you tapping the button manually, the AI can also request to enter or exit PLAN mode on its own. When it is ready to implement, the AI first presents the plan in its reply, then raises the exit request in the same reply; only after you approve in the plan review panel does it restore the mode it was in before entering PLAN and begin implementing. The plan body is for reviewing the scope of changes and the verification arrangement; the request reason is only a brief summary.

There is one directional restriction: **AUTO mode can only be entered by you manually** — the AI cannot switch into AUTO through a tool. The reverse, however, is possible: while in AUTO mode the AI can switch to PLAN on its own — this is the only path for the AI to exit AUTO. Expert mode likewise can only be entered by you manually; the AI cannot switch into it by itself.

## Exiting PLAN Returns to the Mode Before Entering

PLAN is a temporary state: the mode at the time of entry is remembered, and exiting restores it automatically. Entering PLAN from AUTO returns to AUTO after the plan is done and you exit; entering PLAN from BUILD returns to BUILD. It no longer always falls back to BUILD.

Note that exiting PLAN still requires your approval in the plan review panel (tap "Approve and implement"); only after approval is the mode actually restored. After a successful entry or exit, the tool result shows the current mode and its description directly.

## You Can Switch Mid-Run Too

The run-mode reminder the AI receives is attached automatically by the app to explain the current permissions; it is not part of your message content and does not mean you added a new task or authorization. The reminder carries clear start and end markers so it can be distinguished from the original request.

The mode button also works while the AI is executing a task. If you switch modes while the AI is in the middle of a task, it **takes effect immediately in that round**: the AI receives a mode-switch notification, and its current task is handled under the new mode from that moment on (for example, if you switch to AUTO midway, subsequent operations stop prompting one by one; if you switch to PLAN, subsequent write operations are blocked) — there is no need to wait for the round to end.
