# Installing Playwright Browser Automation

After installing Chromium inside the container and registering the Playwright MCP server, the AI can open web pages, click elements, fill forms, and capture page content and screenshots.

Requires a Debian / Ubuntu container

Chrome for Testing binaries depend on glibc; the built-in Alpine image uses musl libc and cannot run it. First import a custom Debian or Ubuntu image as described in [Containers & Images](https://aivapp.cn/md/guide/container.md).

## Environment Baseline

Item

Value

System

Debian / Ubuntu, root by default inside the container

Architecture

aarch64 (arm64) or x86\_64

Browser

Chrome for Testing 153.0.8010.12, installed to `/opt/chrome`

MCP server

`@playwright/mcp`, pulled on demand by `npx`

## 1\. Install Dependencies

bash

```
apt-get update -qq && apt-get install -y -qq unzip fonts-noto-cjk
```

Without Chinese fonts installed, Chinese text in screenshots will render as boxes.

## 2\. Download and Extract Chromium

The example below uses arm64; for x86\_64, replace `linux-arm64` with `linux64` in the URL and the extraction directory name with `chrome-linux64`.

bash

```
mkdir -p /opt/chrome
curl -L -o /tmp/chrome.zip \
  https://cdn.npmmirror.com/binaries/chrome-for-testing/153.0.8010.12/linux-arm64/chrome-linux-arm64.zip
unzip -q /tmp/chrome.zip -d /opt/chrome
rm /tmp/chrome.zip

/opt/chrome/chrome-linux-arm64/chrome --version
# Prints Google Chrome for Testing 153.0.8010.12
```

The command above uses the npmmirror mirror; you can also switch to the official source. If that version is pulled, just replace the version number in the URL:

txt

```
https://storage.googleapis.com/chrome-for-testing-public/153.0.8010.12/linux-arm64/chrome-linux-arm64.zip
```

## 3\. Register the MCP Server

Go to "Settings → MCP servers → +", choose local stdio as the transport type, fill in the fields per the table below and save; the app connects automatically.

Field

Value

Name

`playwright`

Start command

`npx`

Command arguments

`-y`, `@playwright/mcp@latest`, `--headless`, `--no-sandbox`, `--executable-path`, `/opt/chrome/chrome-linux-arm64/chrome` (add one per item)

Scope

Global (shared by all projects) or project (current workspace only)

You can also edit the config file directly: global is `~/.aiverse/mcp.json`, project-level is `/.aiverse/mcp.json`. Changes take effect automatically within a few seconds after saving:

json

```
{
    "mcpServers": {
        "playwright": {
            "command": "npx",
            "args": [
                "-y",
                "@playwright/mcp@latest",
                "--headless",
                "--no-sandbox",
                "--executable-path",
                "/opt/chrome/chrome-linux-arm64/chrome"
            ],
            "enabled": true
        }
    }
}
```

Here `--no-sandbox` is mandatory inside the container, otherwise the browser crashes on startup; `--executable-path` points at the Chromium you already installed, so Playwright won't download another copy.

The default headless User-Agent carries a `HeadlessChrome` marker, and some sites block based on it; you can add `--user-agent` to override it (add it in the UI as two argument items in sequence):

txt

```
--user-agent
Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36
```

Output files such as screenshots are written by default into `.playwright-mcp/` under the working directory — i.e. the current workspace root — which leaves untracked files in the repo. Adding `--output-dir` moves them out of the workspace:

txt

```
--output-dir
/root/playwright-output
```

The exception is when you specify a file name for a screenshot: relative paths are resolved against the workspace and bypass `--output-dir`, so use an absolute path.

## 4\. Verify

Go to "Settings → MCP servers"; the `playwright` row should show as connected with a tool count. If the connection fails, click the log button in the top-right corner to investigate.

Then start a new session (the tool list is fixed at session start) and ask the AI to open a web page and take a screenshot; getting an image back means the link works. The first connection has to wait for `npx` to download `@playwright/mcp`, which took about 3 to 4 minutes in testing.

## Troubleshooting

Symptom

Cause

Fix

Crashes on startup with `Target closed`

Missing `--no-sandbox`

Add the argument and reconnect

Running chrome directly in the terminal reports a `/dev/shm` FATAL

The container has no `/dev/shm` and `/dev` isn't writable

Add `--disable-dev-shm-usage`; launches started by Playwright are unaffected

`chrome: not found` or the binary won't execute

The container is Alpine (musl), or the package doesn't match the architecture

Switch to a Debian / Ubuntu image and double-check arm64 vs x64 packages

Chinese shows as boxes in screenshots

Missing Chinese fonts

Run `apt-get install -y fonts-noto-cjk`

Screenshot is blank or fully transparent

Rendering occasionally fails without a GPU

Retry once; if still blank, switch to the JPEG format

First call takes several minutes

`npx` is downloading `@playwright/mcp`

Wait for it to finish; afterwards it uses the local cache. You can also point the npm registry at `https://registry.npmmirror.com`

`playwright` doesn't appear in the MCP list

Config didn't take effect, or the JSON has a syntax error

Confirm `mcp.json` parses correctly, then click "Reconnect" in the top-right corner

## Appendix: One-Shot Install Script

The script below completes steps 1 through 3, and additionally auto-switches download sources, verifies the archive, skips the download if already installed, and backs up `mcp.json` before writing. Save it as `/tmp/install-playwright.sh` and run `bash /tmp/install-playwright.sh`:

bash

```
#!/usr/bin/env bash
set -euo pipefail

VERSION="${CHROME_VERSION:-153.0.8010.12}"
DIR=/opt/chrome
ZIP=/tmp/chrome-$VERSION.zip

case "$(uname -m)" in
  aarch64|arm64) SUB=linux-arm64; NAME=chrome-linux-arm64 ;;
  x86_64)        SUB=linux64;     NAME=chrome-linux64 ;;
  *) echo "Unsupported architecture: $(uname -m)"; exit 1 ;;
esac
BIN="$DIR/$NAME/chrome"

[ "$(id -u)" = 0 ] || { echo "Please run as root"; exit 1; }
command -v python3 >/dev/null || { echo "python3 is missing"; exit 1; }
command -v npx >/dev/null || echo "[Warning] npx not found; the MCP server won't start"

need=()
command -v unzip >/dev/null || need+=(unzip)
fc-list :lang=zh 2>/dev/null | grep -q . || need+=(fonts-noto-cjk)
if [ ${#need[@]} -gt 0 ]; then
  echo "Installing dependencies: ${need[*]}"
  apt-get update -qq && apt-get install -y -qq "${need[@]}"
fi

if [ -x "$BIN" ]; then
  echo "Already installed, skipping download: $BIN"
else
  for url in \
    "https://cdn.npmmirror.com/binaries/chrome-for-testing/$VERSION/$SUB/$NAME.zip" \
    "https://storage.googleapis.com/chrome-for-testing-public/$VERSION/$SUB/$NAME.zip"
  do
    echo "Downloading: $url"
    curl -fL --retry 3 --retry-delay 2 -o "$ZIP" "$url" && break
    echo "[Warning] That source is unavailable, trying the next one"
  done
  [ -s "$ZIP" ] || { echo "All download sources failed"; exit 1; }

  mkdir -p "$DIR"
  unzip -tq "$ZIP" && unzip -q "$ZIP" -d "$DIR"
  rm -f "$ZIP"
fi

"$BIN" --version

BIN="$BIN" python3 - &lt;&lt;'PY'
import json, os, shutil
p = os.path.expanduser("~/.aiverse/mcp.json")
os.makedirs(os.path.dirname(p), exist_ok=True)
data = {}
if os.path.exists(p):
    shutil.copy(p, p + ".bak")
    with open(p, encoding="utf-8") as f:
        data = json.load(f)   # Exits on syntax error instead of overwriting
data.setdefault("mcpServers", {})["playwright"] = {
    "command": "npx",
    "args": ["-y", "@playwright/mcp@latest", "--headless", "--no-sandbox",
             "--executable-path", os.environ["BIN"]],
    "enabled": True,
}
with open(p, "w", encoding="utf-8") as f:
    json.dump(data, f, indent=4, ensure_ascii=False)
print("playwright config written")
PY

echo "Installation complete: $BIN"
```

`CHROME_VERSION` can be set to another version. Once installation finishes, go back to step 4 to verify.
