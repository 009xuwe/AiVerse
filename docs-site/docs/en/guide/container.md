# Containers and Images

In AiVerse, the environment where the AI executes commands is called a "container"; the entry point is "Settings → Containers and Images". The list is single-select, and the selected container is the environment where AI command execution, file read/write, and the terminal live.

There are two kinds of backend, and you must choose one of them: a **local container** (running a Linux system on the phone) and **remote SSH** (running on a remote server; see [Remote SSH Mode](https://aivapp.cn/md/guide/remote-ssh.md)).

## Container List

Each row is a container configuration, with a source tag after the name: built-in, custom, or SSH. The currently active container has its name shown in the theme color.

The following actions apply to all kinds of containers:

-   **Reorder**: long-press a row's name area and drag up or down to change the order (kept after a restart); newly added containers are placed at the top of the list.
-   **Edit**: the pencil icon at the end of the row.
-   **Reset**: the refresh icon at the top right of the edit dialog. This is a destructive action — it deletes the tools and configuration already installed inside the container and requires a second confirmation; once confirmed, the running AI session and all terminal tabs are closed. Deleting a large container with a lot of content may take tens of seconds, during which "cleaning container data" is displayed along with the number of items cleaned; it closes automatically when finished and does not block the interface.
-   **Delete**: swipe left on the row to reveal the red delete button. Deleting a local image also cleans up its rootfs to free space (showing cleaning progress the same way); deleting a remote SSH only removes the configuration and does not affect the server's data.

The built-in Alpine can also be deleted. After everything is deleted the list shows an empty state, from which you can restore the built-in Alpine with one tap, so there is no need to worry about accidental deletion.

After a container is running, the App automatically detects its system type and shows the corresponding icon on the left of the list (Alpine, Ubuntu, Debian, and CentOS are supported); if it cannot be identified, a default icon is shown.

## Built-in Alpine Container

The App ships with an Alpine image, which is added to the list and selected automatically on first launch; the shell path is configured automatically and needs no extra setup.

First Use Requires Entering the Terminal to Complete Initialization

Preparing the local container's environment (extracting the rootfs and installing base tools) is triggered the first time you enter the **terminal page**. On first use, please enter the terminal to complete initialization; only then do AI command execution, Git management, and local MCP work properly.

The Built-in Container's Version Is Not Replaced When the App Upgrades

A newer version of the App may bundle an updated Alpine image, but **upgrading the App does not touch the container you are already using** — the tools you installed inside it and the software source configuration are all kept as they are.

To move to the new baseline, tap **Reset** for that container in "Containers and Images": resetting deletes its rootfs directory (including the tools and configuration installed inside the container), and the next time you enter the terminal it is re-extracted and re-initialized. **Workspace files and AI configuration (skills, MCP) are not affected** — they are stored outside the container. A newly installed App, on its first initialization, uses the latest bundled image directly.

## The Initialization Menu on First Entering the Terminal

Whether it is the built-in Alpine or an image you imported yourself, the first time you enter the terminal after the rootfs is extracted, an initialization menu appears with four options:

**1\. Install Dependencies Automatically (Recommended)**

It lists the items to be installed (base tools such as bash, curl, ripgrep, and git, plus Node.js and Python 3). Enter `y` to confirm, then choose a software source and the installation begins.

**2\. Environment Installation**

Opens the "Environment Installation" menu, where you choose a development environment by scenario (see the next section). A scenario is a bundle of runtimes; once selected, confirm the checklist to install. The menu also offers "Custom Install", where you can tick items one by one and specify the major version.

**3\. Manual Install (Don't Ask Again)**

Skips installation; the menu will not appear again when you enter the terminal, and you install tools yourself. To bring the menu back, reset the container.

**4\. Exit**

Nothing is installed this time; you will be prompted again the next time you enter the terminal.

A failed installation does not prevent entering the shell; re-entering the terminal lets you try again.

### Choosing a Software Source

When switching sources you can choose:

-   `1) Auto-detect (Recommended)`: measures the reachability of each mirror under the current package manager and automatically skips sources that are dead or blocked.
-   `2~6)` Specify a mirror: Huawei Cloud, Tsinghua tuna, USTC, Tencent Cloud, Aliyun.
-   `0)` Keep the default, no source switch.

The original configuration is backed up before switching, and restored automatically if it fails, without affecting subsequent installation. The built-in Alpine's rootfs keeps the official sources; switch in this menu when you need a domestic source.

## Installing Development Dependencies at Any Time (aiverse)

The initialization menu only appears once, the first time you enter the terminal. Afterwards, to install other runtimes (such as Java or Go), run the command `aiverse` in the terminal to open the "Environment Installation" menu.

`aiverse` installs by scenario — select a scenario and confirm the checklist:

Scenario

Installed content

General development

Base tools + Node.js + Python 3

Python development

Base tools + Python 3 + pip

Node.js / frontend development

Base tools + Node.js + npm

Kotlin / Android development

Base tools + JDK 17 + Android SDK

Flutter development

Base tools + JDK + Android SDK + Flutter SDK

Java development

Base tools + JDK

Go development

Base tools + Go

Rust development

Base tools + rustc + cargo

PHP development

Base tools + PHP + Composer

Base tools only

bash, curl, ripgrep, git

The base tools (bash, curl, ripgrep, git) are installed in every scenario. A scenario installs the latest major version from the software repository by default; to specify a version, use "Custom Install" in the menu and tick items one by one. The menu also supports switching the software source on its own and viewing the runtimes already installed.

"Kotlin / Android development" and "Flutter development" are heavy scenarios: they download the JDK, the Android SDK (including build-tools and platform-tools), and the Flutter SDK from domestic mirrors, which takes longer and uses more storage. Installing them in a Debian / Ubuntu image is recommended (the built-in Alpine needs extra handling because it uses musl libc); on ARM64 devices, native binaries such as aapt2 are replaced with executable builds automatically. An ARM64 container can build a Flutter debug APK directly; a release build needs additional translation tools installed — see "Building a Flutter release APK in the container". Once installation finishes, a newly opened terminal automatically has environment variables such as `JAVA_HOME` and `ANDROID_HOME` set; for the specific build steps see "Building Android apps in the container" and "Building Flutter apps in the container" in the "Advanced Tutorials".

The settings icon at the top right of the terminal opens the terminal settings panel, where you can adjust the color theme, font size, font, and cursor style.

## Custom Images

The built-in Alpine covers most scenarios. If you need Debian / Ubuntu, or an environment with a specific toolchain preinstalled, you can add images yourself in two ways.

### Method 1: Built-in Image Download (Recommended)

1.  Open "Settings → Containers and Images" and tap the download icon at the top right.
2.  The page lists the downloadable images (Alpine, Ubuntu, Debian, etc.); tap a row to view details and download.
3.  At the top right you can switch the download source (official / Huawei Cloud / Aliyun / Tencent Cloud); on a domestic network, choosing a domestic source is recommended.
4.  Once the download completes, tap "Import" and the image joins the container list, ready to use once selected.

An already-downloaded image can be imported repeatedly; swipe left to delete a downloaded image file.

The "Multi-select" button at the top right of the download page (a box with a check icon) lets you clean up several downloaded files at once: after entering multi-select, a checkbox appears on the left of each row, and a top action bar offers "Select all / Deselect all" and a delete button; once confirmed, the selected image files are deleted together (containers already imported from those files can no longer be extracted from them, but already-extracted containers are unaffected). Only entries that are **downloaded and whose file exists** can be checked; the checkboxes for undownloaded entries are grayed out.

**Version information updates automatically**: the version numbers in the list are refreshed from upstream release information, so you can see a new patch as soon as upstream releases it without waiting for an App update; when offline, the bundled version list is used. Therefore:

-   **Tapping into details shows the exact version**: for distributions like Debian that "only state the major version", the details show the actual patch number (such as `12.15`), matching what is actually downloaded.
-   **Status badges come in three tiers**: `End of support soon` means some support period ends within 30 days (e.g. Alpine 3.21 reaches end of maintenance on 2026-11-01); `Entered ESM / LTS extended support` means regular support has ended but upstream's extended-support program still provides security updates (Ubuntu 20.04 uses ESM, Debian 12 uses LTS); `End of support` means even the extended-support period has ended. **Tapping into details shows the exact dates of both support periods**, so you can judge how much longer it is usable. All three tiers can be downloaded as usual; it is only recommended to prefer branches still within their regular support period.
-   **An already-downloaded image shows "a new version is available"**: this means upstream has released a new patch (commonly a security fix). Just download it again and import; the old downloaded file can be deleted by swiping left. Images downloaded before upgrading to this version are also recognized — the version number is taken from the downloaded file name, so you don't need to re-download once just to learn there is a new version.
-   **Downloads verify integrity automatically**: the Alpine and Ubuntu images carry official SHA-256 checksums, which are verified after download; on a mismatch it reports verification failed and deletes the partial file. Debian's upstream only provides build artifacts with no checksum, so no verification is done.
-   **The Tencent Cloud source only provides Alpine**: its Ubuntu image has not been synced with patch versions for a long time, so under that source Ubuntu shows "no such image on the current source"; switch back to the official source or to Huawei Cloud / Aliyun.

### Method 2: Import an Image File Manually

When you need a specific version or distribution, you can prepare a rootfs image yourself and import it.

**Image requirements**:

-   Compression formats: `.tar.gz`, `.tgz`, `.tar.xz`, `.txz`
-   The content must be a complete Linux rootfs (the file system root, containing `/bin/sh` or another shell)
-   The architecture must match the device: use an aarch64 image on ARM64 devices and an x86\_64 image on x86 devices

**Image sources**:

-   Alpine (small and fast to boot): `https://alpine.linuxhub.cn/alpine/edge/releases/`, pick `alpine-minirootfs-*-aarch64.tar.gz` under the aarch64 directory
-   Ubuntu: `https://cdimage.ubuntu.com/ubuntu-base/releases/`, pick `ubuntu-base-*-base-arm64.tar.gz`
-   Debian: you can export it from an official Docker Hub image (such as `arm64v8/debian`)

If you have Docker available, you can export a rootfs from any image:

bash

```
docker pull debian:bookworm
docker create --name temp-rootfs debian:bookworm
docker export temp-rootfs | gzip > debian-rootfs.tar.gz
docker rm temp-rootfs
```

You can also make one yourself in an existing Linux environment with `debootstrap` (Debian / Ubuntu) or `alpine-make-rootfs` (Alpine).

**Import steps**:

1.  Open "Settings → Containers and Images", tap + at the top right, and choose "Local image".
2.  Fill in the configuration:

-   **Name**: an alias for the image configuration.
-   **Shell path**: the shell inside the container, such as `/bin/sh` or `/bin/bash`.
-   **Image file**: choose the prepared rootfs file. On saving, a copy is made into the App's private directory, and from then on resetting the container always re-extracts from that copy, so it does not depend on the original file; deleting the image also cleans up the copy.
-   **Mount** (optional): mount a phone directory into the container — see "Mounting a phone directory for the AI to use" below.
-   **proot arguments** (optional): added item by item and appended verbatim to the PRoot launch arguments.
-   **Environment variables** (optional): key-value pairs injected into processes inside the container.

1.  After saving, select that configuration in the list to switch to the custom container.

The first time you use a custom image, the App extracts the rootfs into its private directory, which takes time depending on the image size. Custom images have no domestic software source configured by default; you can switch in the initialization menu the first time you enter the terminal.

### Common Troubleshooting

**Errors when installing software**: caused by hard links being unavailable inside the container. Edit the image → proot arguments → add `--link2symlink` → save, then re-enter the terminal. Images imported through the built-in download feature already include this argument by default.

**Arrow keys show as `^[[A`**: the current shell is `sh`, which does not support line editing. Edit the container and change the Shell to `/bin/bash` to get arrow keys, history, and completion.

## Mounting a Phone Directory for the AI to Use

The AI runs inside the container and by default can only see the workspace, with no access to the phone's public storage. If you need it to have access, you must mount a phone directory into the container:

1.  Confirm storage permission has been granted. A permission request appears on first use; if you previously denied it, go to system settings → Apps → AiVerse → Permissions → Storage and enable it.
2.  Open "Settings → Containers and Images" and edit the container currently in use.
3.  Under "Mount", add an entry: **Local directory** is the path on the phone (such as `/sdcard/Download`), and **Container directory** is the path inside the container (such as `/mnt/Download`).
4.  After saving, restart the terminal session and the AI can access it through the in-container path.

Mount Only What You Need

Mount only the subdirectories you actually need — do not mount the entire `/sdcard` directly — as that would expose all files on the phone to the AI and create a privacy leak risk.

Example: to let the AI view the App logs, mount the local directory `/storage/emulated/0/Android/data/com.aiverse/` to `/mnt/aiverse` in the container, and it can then read the logs under `/mnt/aiverse/files/logs/`. Note that under `Android/data/` only AiVerse's own directory is accessible; other apps' directories cannot be reached due to scoped storage restrictions.

If what you need to handle is only a single file, a simpler approach is to copy it into the workspace, which the AI can access by default.

## Behavior After Everything Is Deleted

Even if you delete all containers (including the built-in Alpine), the App still works normally — when executing commands, the AI falls back to the built-in Alpine as the default execution environment. Reopening the "Containers and Images" page, you can restore the built-in Alpine with one tap on the empty-state page or add a new image.
