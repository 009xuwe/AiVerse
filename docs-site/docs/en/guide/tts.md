# Text-to-Speech

Text-to-speech (TTS) reads the AI's replies aloud, which is handy for listening while you do something else. The entry point is "Settings → Read Aloud", and you can also tap the read-aloud button directly on a message bubble.

## Two Engines

AiVerse supports two read-aloud engines, which you can switch between in settings:

-   **System TTS (default, offline and free)**: it calls the device's built-in speech engine (Android `TextToSpeech`), with no network and no cost; the voice and language are determined by the system engine and system language. The device needs a usable speech engine installed (most systems include one; if missing, you can install one in the system settings).
-   **Online TTS (optional)**: it sends the text to an online API and plays back the returned audio. The voices are more natural and you can specify a speaker, but you need to provide your own API endpoint and API Key.

When "Enable online voice" is on and the API is fully configured, the online API is used; otherwise it falls back to system TTS.

## How It Works

-   **System TTS**: it extracts the reply's Markdown into plain text (automatically skipping or announcing code blocks, removing formatting symbols, keeping link text, and simplifying images and tables), splits it into segments up to the engine's length limit (about 3500 characters) and enqueues them, then has the system engine read each segment in turn. Speech rate and pitch are adjustable.
-   **Online TTS**: following the OpenAI `/audio/speech` compatible protocol, it POSTs the text to the API, gets back an mp3, and plays the segments in order with a player. A single segment over about 4000 characters is split automatically, requesting and playing one segment before requesting the next. The API type can be set to "Auto-detect / OpenAI compatible / Fish Audio", and the App assembles the request URL and body accordingly.

## Settings

-   **Speech rate**: a slider from 0.5x to 2.0x.
-   **Pitch**: a slider from 0.5 to 2.0.
-   **Auto read new replies**: automatically reads a reply once it finishes generating.
-   **Announce code blocks**: skip code blocks when reading, or read a line saying "Code block, please check".
-   **Change system TTS engine**: jumps to the system voice settings to pick the speech engine on the device (the change takes effect automatically the next time it reads); if the device has no speech engine, you can install one here.
-   **Online TTS API (optional)**: switch, API type, API address, API Key, model, and voice.

## Recommended Platforms for the Online API

System TTS is completely free and works offline, which is enough for daily use. When you want a more natural Chinese voice, you can choose one of the following **online** APIs (all integrate via the OpenAI `/audio/speech` compatible or Fish Audio protocol):

-   **Fish Audio**: API address `https://api.fish.audio`, set the API type to "Fish Audio", the model can be `s2.1-pro-free`, and the API Key is obtained from the API Keys page on its website. There is a free quota.
-   **SiliconFlow**: API address `https://api.siliconflow.cn/v1`, API type "OpenAI compatible", models such as `FunAudioLLM/CosyVoice2-0.5B`, voices such as `FunAudioLLM/CosyVoice2-0.5B:alex`. New users get a free quota, and some voices require real-name verification.
-   **Any OpenAI-compatible relay / self-hosted**: fill the API address with `.../v1`, models such as `tts-1` or `gpt-4o-mini-tts`, voices such as `alloy`. Anything compatible with `/audio/speech` works.

\> The API address can be a full address (including `/audio/speech` or `/v1/tts`) or just the domain — the App completes the path based on the API type. Free quotas and whether real-name verification is required follow each platform's current policy.

## How to Use

-   Tap the read-aloud button on any AI reply bubble to start reading, and tap it again to stop; the message being read is highlighted, and **the text already read turns the theme color word by word along with the reading progress**, returning to normal once the whole message is finished.
-   After enabling "Auto read new replies", every new reply is read automatically once it finishes generating.
-   You can tap stop at any time during reading; switching messages stops the current reading before starting a new one.
