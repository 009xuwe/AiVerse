# Model Thinking Mode Reference: Thinking Effort & "Turn Off Thinking"

The same "thinking effort" button behaves very differently across models: some can go all the way from "no thinking at all" to "maximum effort", some only have two or three levels, some can't be turned off at all, and some stop recognizing the parameter entirely once you switch to a relay station.

This document lays out the forms actually supported, organized by model family, and gives you a way to "tell whether the setting really took effect", so you can quickly pinpoint the cause when you hit "I selected it but nothing happened".

About the data sources in this document

The levels and "can it be turned off" entries in the tables come from each vendor's official documentation and were verified by sending requests one by one against real channels (judging by the actual reasoning token count returned and whether thinking content still appears). Vendors may change things after updating their models, so when there's a discrepancy, trust what you actually measure.

## 1\. The Three Control Forms

Form

Behavior

Typical models

**Level-based**

Controls thinking depth with discrete levels: some subset of Off / Minimal / Low / Medium / High / Extra High / Max

GPT-5 family, Claude 4.6+, Gemini 3, Grok

**Budget-based**

Controlled by a "thinking token cap" with no level concept; some models treat a cap of 0 as off

Gemini 2.5 family, Claude 4.5 and earlier

**Switch-based**

Only on / off, no intermediate levels; some models offer both a switch and levels

Some DeepSeek, Kimi, GLM, Qwen models

The higher levels get rarer as you go up: `Extra High` and `Max` are only supported by some newer models, not every "supports thinking" model has them.

## 2\. Actual Support by Family

### Claude (Anthropic)

Model generation

Form

Levels

Can be turned off

Claude 4.6

Level-based

Low / Medium / High / Max

Yes

Claude 4.7, 4.8, 5, 5.5 family (incl. Opus, Sonnet)

Level-based

Low / Medium / High / Extra High / Max

Mostly yes; **Opus 5.5 and Fable 5.1 have thinking permanently on** — off requests are ignored

Claude 4.5 and earlier

Budget-based

No levels

Yes (no thinking budget consumed once off)

Key points:

-   **"Extra High" and "Max" aren't present in every generation**; 4.6 goes up to `max` only, with no `xhigh`.
-   Sonnet 5.5's off semantics are special: off only works at the low/medium/high levels, and is rejected or ignored at Extra High/Max.
-   The Opus 5 generation limits levels to "High" and below when thinking is turned off.

### GPT (OpenAI)

Model

Levels

Off

GPT-5 (incl. mini / nano)

Minimal / Low / Medium / High

Not supported

GPT-5.1

Low / Medium / High (+ Off)

Supported

GPT-5.2, 5.3, 5.4, 5.5

Off / Low / Medium / High / Extra High

Supported

GPT-5.6 family

Off / Low / Medium / High / Extra High / Max

Supported

GPT-5 Pro, 5.2 Pro, 5.4 Pro, 5.5 Pro

Only Medium / High / Extra High (some only High)

Not supported

GPT-6 Astra, 6.1 Sol

Low / Medium / High / Extra High / Max

**Not supported** (passing "Off" is rejected)

Key points:

-   **Whether "Off" takes effect depends on the API form**: over a `chat/completions`\-style endpoint, some services treat "Off" as "Low"; only over a `responses`\-style endpoint does it truly go to zero. If you selected off but the model still thinks, first switch to the new unified endpoint (Responses) in the provider settings and try again.
-   The official Pro family has few levels — only "Medium / High / Extra High" — and picking a lower level won't save anything.

### Gemini (Google)

Model

Form

Description

Gemini 3 Pro / 3.1 Pro

Level-based

Supports Low / Medium / High; **cannot be turned off**

Gemini 3 Flash family

Level-based

Supports Minimal / Low / Medium / High; Minimal is the closest to "not thinking" but still isn't a full off

Gemini 3.7 / 3.8 Flash

Level-based

Low / Medium / High (**no Minimal**)

Gemini image models (e.g. 3.1 Flash Image)

Level-based

Only Minimal / High

Gemini 2.5 Pro

Budget-based

152 ~ 32768 tokens, **cannot be turned off**

Gemini 2.5 Flash / Flash-Lite

Budget-based

Can be set to 0 to turn off; Flash 0~24576, Flash-Lite 0 or 512~24576

Key point: the Gemini 3 family **does not allow** using levels and budget settings at the same time; mixing them raises an error immediately.

### Domestic & Third-Party (over OpenAI-compatible endpoints)

Family

Form

How to turn off

Notes

DeepSeek V4 family

Switch + levels

**Only the dedicated off switch works**; the "Off" level still thinks

Levels Low / High / Max

Kimi K3

Level-based

None (K2.5 / K2.6 are switch-based and can be turned off)

Levels Low / High / Max

GLM 5 family

Switch + levels

The dedicated off switch works

5.2 only has High / Max; 5.3 has Low / High / Max

Qwen 3.8 family

Switch + levels

**Needs a different switch parameter from DeepSeek**; the "Off" level is ineffective

Levels Low / Medium / Extra High

Grok 4.5 / 4.6 / 4.7

Level-based

Depends on the channel; some channels ignore it

Levels Low / Medium / High / Extra High

MiniMax M3

Switch + levels

Depends on the version

M3.1 Flash levels Low ~ Max

Key points:

-   **Different vendors' "off" parameters aren't interchangeable**: applying one vendor's off method to another commonly results in the API still returning success while the model keeps thinking. These "no error but no effect" cases can only be identified with the method below.
-   Relay stations (aggregator channels) may rewrite or use their own parameters, so level and switch behavior should be judged by what you actually measure on that channel.

## 3\. How to Tell Whether the Setting Really Took Effect

Look at the token breakdown in "Token stats → Call records", or check whether thinking content is still shown in the conversation:

Observation

Meaning

After selecting "Off", this call's reasoning tokens drop to zero and no thinking content appears in the conversation

Off succeeded

After selecting "Off", reasoning tokens are still clearly above 0, or you can still see thinking content

**Off didn't take effect**: the model's thinking is permanently on, or the channel doesn't honor that off parameter

After selecting "Max", reasoning tokens are close to "High"

The model's top level only reaches "High", or the channel downgraded it

Reasoning tokens look about the same no matter how you adjust the level

The channel isn't passing the level parameter through (common with relay stations)

Reasoning tokens are usually listed separately (some services only give a total in usage, in which case "is thinking content still shown" is your only way to judge).

\> The app automatically learns a level whitelist from a service's error messages (when the error contains `valid levels: …` / `Supported values are: …`). > Once learned, this model on that channel only lists the levels actually supported and won't keep hitting the same error; switching channels re-evaluates it.

## 4\. FAQ

**Q: Why does the same model behave differently at different levels after I switch to another relay station?** Whether levels are passed through and whether it can be turned off depends on the API the channel actually uses and the upstream account's permissions. The same model name can be completely different across channels — see the method in section 3.

**Q: The relay station renamed the model and the levels can't be detected — what do I do?** In that case all levels are listed for you to pick manually. First use section 2 to find which tier your model belongs to, then choose a level it actually supports — picking a nonexistent level usually doesn't error, but gets silently downgraded.

**Q: I selected "Minimal" but it still thinks for a long time?** "Minimal" isn't "Off". The Gemini 3 family and some domestic models still produce some thinking at their lowest level; the only way to truly zero it out is "Off" (provided the model supports turning it off).

**Q: Some model clearly supports "Max", but selecting it feels like "High"?** Either that model generation has no higher level to begin with (e.g. a model with only Low/Medium/High), or the channel isn't passing it through. Use the method in section 3 to check.

**Q: After turning off thinking, will the thinking content from earlier rounds cause errors?** No. Thinking content from previous rounds is returned as usual, and the server ignores it or handles it compatibly.

## 5\. How to Choose a Level

-   **Everyday Q&A, copy editing, running simple commands**: Low or Medium — faster responses, lower cost.
-   **Editing multiple files, debugging complex bugs, writing tests**: High or Extra High.
-   **You only care about the result and don't mind the wait or the cost**: Max (only some models support it).
-   **Purely mechanical tasks that need no reasoning** (formatting, translating, extracting fields): turn it off if you can — the biggest token savings.
