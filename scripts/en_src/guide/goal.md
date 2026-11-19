# Session Goals

"Goal" lets the AI advance **autonomously in a loop** toward a result you define: it plans, executes, and verifies on its own, and at the end of each round judges whether the goal is met, stopping only when it is done, blocked, or the budget is exhausted. It suits tasks that "have a clear endpoint and can verify their own progress", such as "make the checkout tests all green" or "bring an endpoint's p95 latency below 120 ms".

::: tip Version note
The Goal feature was introduced in 1.0.0.
:::

## Setting a Goal

Type in the input field:

```
/goal Make all unit tests on the current branch pass without changing the public API
```

The AI starts the autonomous loop immediately. Setting a goal counts as an authorization: **while the goal is running, tool calls are allowed automatically** (safety guards against catastrophic operations such as deleting the system/workspace root are still kept), and **PLAN mode is paused** — because plan mode requires you to approve step by step, which conflicts with "finish autonomously".

A good goal should be self-verifying:

- Say clearly "what counts as done" and "how to verify it", such as "run `./gradlew test` all green" or "the page builds locally".
- Point the AI to the files, logs, or references it should read.
- A weak goal: "write the docs for this feature". A strong goal: "produce the Goals doc page, explain its lifecycle and commands, and make sure the local build passes".

## Viewing and Managing

| Command | Effect |
| --- | --- |
| `/goal <goal>` | Set or override the goal and start immediately |
| `/goal` | View the current goal and its status |
| `/goal pause` | Pause the autonomous loop |
| `/goal resume` | Resume the autonomous loop |
| `/goal clear` | Clear the goal |

A persistent goal bar below the input field shows the status (in progress / paused / completed / blocked / budget exhausted) and provides pause, resume, and clear buttons, so you don't have to remember the commands.

## What Happens While Running

- At the end of each round, the AI checks the goal against the evidence to judge whether it is met; if not met and the budget is not exhausted, it automatically moves to the next round without stopping to ask "continue?".
- To step in midway: just send a message. Your message takes priority over resuming execution, and the goal continues after it is handled.
- On completion the AI marks the goal "completed"; when it hits a blocker that needs you (missing credentials, a decision required), it marks "blocked" and stops.
- Tapping "Stop" interrupts and automatically pauses the goal (to avoid it continuing on its own after the next message); use `/goal resume` to resume.
- Safety valve: if the automatic loop makes no progress for 3 consecutive rounds (no tool calls and no visible output), it is automatically set to "blocked" and stops, avoiding idle token burn.
- Reaching the budget cap stops the loop automatically and marks it "budget exhausted"; you can then `/goal resume` to continue or `/goal clear` to clear.

## Budget Cap

The autonomous loop keeps consuming tokens, so there are two safeguards you can adjust under **Settings → Preferences → Goals (goal)**:

- **Maximum continuation rounds**: 50 by default.
- **Token budget**: 500,000 by default, accumulated from input + output tokens during the loop.

Hitting either one stops the loop and shows a prompt.

## Notes

- PLAN mode cannot be used while a goal is running; to plan, first `/goal pause` or `/goal clear`.
- Goals are saved per session; switching sessions does not affect each other.
