# Building a Flutter release APK in the Container

"Building Flutter Apps in the Container" covered the debug build pipeline. This article completes the **release build** inside the same Debian 12 (bookworm) aarch64 PRoot container: by using qemu user-mode translation to run Flutter's AOT compiler, which is only provided for x64, you can produce a release APK locally — verified working inside the container.

Scope

This article builds the release package for the **android-arm64** target, using Flutter 3.27.4 (Dart 3.6.2) + Android SDK 35. android-arm / android-x64 are handled the same way (swap in the linux-x64 gen\_snapshot for each target), and for a profile build you'd use `android-arm64-profile/linux-x64/gen_snapshot` — the mechanism is identical.

Mirrors and Network Notes

qemu and the x86\_64 runtime libraries are downloaded from domestic mirrors. The mirror output is fully equivalent to the official one: even if the network can reach official sources directly, there is no need to switch back to the official configuration.

## Environment Baseline

Item

Value

System

Debian GNU/Linux 12 (bookworm) aarch64 (PRoot)

Flutter

3.27.4 stable (Dart 3.6.2)

SDK

platforms android-35, build-tools 35.0.0

qemu

qemu-user-static 7.2 (Debian 1:7.2+dfsg-7+deb12u18+b3)

x86\_64 runtime

libc6 2.36 (amd64)

Prerequisites

First follow sections 1–5 of "Building Flutter Apps in the Container" to install the JDK, the Android SDK (including the ARM64 native binary replacement), the Flutter SDK and the mirror configuration, and confirm that a debug APK builds normally, before continuing with this article.

## Why the release Build Fails by Default

When Flutter builds a release package it calls `gen_snapshot` to AOT-compile the Dart code into machine code. This tool is distributed per host platform, and the official builds only provide a `linux-x64` version for Android targets, so the engine artifact directory contains only this one subdirectory:

txt

```
bin/cache/artifacts/engine/android-arm64-release/
└── linux-x64/gen_snapshot
```

On a Linux ARM64 host, Flutter looks for `android-arm64-release/linux-arm64/gen_snapshot`, the directory doesn't exist, and the build fails outright:

txt

```
Target android_aot_release_android-arm64 failed:
  Failed to find ".../android-arm64-release/linux-arm64/gen_snapshot" in the search path.
```

This isn't a configuration problem — upstream still doesn't support Linux ARM64 hosts. The AOT snapshot gen\_snapshot produces embeds a version hash of the Dart SDK, which is verified at runtime by `libflutter.so`, so **you cannot substitute a gen\_snapshot compiled from a different Flutter version** (it fails with `Invalid SDK hash` or a snapshot version mismatch), and third-party community builds are likewise bound to their own Flutter versions.

There is only one workable approach: get the x64 gen\_snapshot running on this machine, i.e. qemu user-mode translation.

## 1\. Install the qemu User-Mode Emulator

Running `apt install qemu-user-static` directly would also update systemd and other components, which is unnecessary inside a container. Instead, just download the deb and unpack it into a separate directory:

bash

```
cd /tmp
apt-get download qemu-user-static            # About 56 MB, using the mirror already configured in the container
mkdir -p /opt/qemu-user-static
dpkg-deb -x qemu-user-static_*.deb /opt/qemu-user-static
/opt/qemu-user-static/usr/bin/qemu-x86_64-static --version
# qemu-x86_64 version 7.2.22 (Debian 1:7.2+dfsg-7+deb12u18+b3)
```

Optional: create a symlink for convenience (the container's PATH doesn't include `/usr/local/bin`, but an absolute path works too):

bash

```
ln -sf /opt/qemu-user-static/usr/bin/qemu-x86_64-static /usr/local/bin/qemu-x86_64-static
```

## 2\. Complete the x86\_64 Runtime Libraries

gen\_snapshot is a dynamically linked x86\_64 program that depends only on the loader and libc, but the container has no x86\_64 libraries at all:

bash

```
qemu-x86_64-static  --version
# qemu-x86_64-static: Could not open '/lib64/ld-linux-x86-64.so.2': No such file or directory
```

Note

qemu's `-L ` and `QEMU_LD_PREFIX` were verified not to work for this binary (it still looks for the loader by absolute path), so you must put the libraries on the system path.

Fetch the amd64 libc6 from a Debian mirror:

bash

```
cd /tmp
curl -fsSL -o libc6-amd64.deb \
  http://mirrors.huaweicloud.com/debian/pool/main/g/glibc/libc6_2.36-9+deb12u14_amd64.deb
mkdir -p /opt/x86_64-root
dpkg-deb -x libc6-amd64.deb /opt/x86_64-root

# Put them on the system library path (the container's /lib is a symlink to usr/lib)
mkdir -p /usr/lib/x86_64-linux-gnu /lib64
cp -a /opt/x86_64-root/lib/x86_64-linux-gnu/. /usr/lib/x86_64-linux-gnu/
ln -sfn ../lib/x86_64-linux-gnu/ld-linux-x86-64.so.2 /lib64/ld-linux-x86-64.so.2
```

Verify:

bash

```
qemu-x86_64-static ~/.local/flutter-3.27.4/bin/cache/artifacts/engine/android-arm64-release/linux-x64/gen_snapshot --version
# Dart SDK version: 3.6.2 (stable) ... on "linux_simarm64"
```

The `linux_simarm64` in the output means an x64 host with an arm64 target — exactly the build the android-arm64 release needs.

## 3\. Replace gen\_snapshot with a qemu Wrapper Script

Keep the original binary under a new name and forward to it with a shell script:

bash

```
R=~/.local/flutter-3.27.4/bin/cache/artifacts/engine/android-arm64-release
Q=/opt/qemu-user-static/usr/bin/qemu-x86_64-static

mv $R/linux-x64/gen_snapshot $R/linux-x64/gen_snapshot.x64-real
cat > $R/linux-x64/gen_snapshot &lt;
flutter build apk --release --target-platform android-arm64
```

The environment variables above are the recommended defaults (same as section 5 of the debug tutorial); you can keep them even when the network can reach official sources — no need to revert them.

The output is:

txt

```
build/app/outputs/flutter-apk/app-release.apk
```

The first build takes about 15 minutes, the vast majority of it spent downloading release-only Gradle dependencies such as lint, kotlin-compiler and intellij-core; the duration depends on the network. The AOT step is translated by qemu on a single thread and takes about 2–3 minutes, producing `build/app/intermediates/flutter/release/arm64-v8a/app.so`; packaging and signing take tens of seconds. qemu translation is slower than native x64 but isn't the main time sink, and incremental builds are much faster.

## 5\. Verify the Output

bash

```
A=~/workspace//build/app/outputs/flutter-apk/app-release.apk

~/android/sdk/build-tools/35.0.0/aapt2 dump badging $A | head -3
unzip -l $A | grep 'lib/.*\.so'
~/android/sdk/build-tools/35.0.0/apksigner verify --print-certs $A
```

When everything is normal, `aapt2` outputs `package` and `minSdkVersion` / `targetSdkVersion`, `unzip -l` should list the following files (indicating an AOT build), and `apksigner` exits with code 0:

txt

```
lib/arm64-v8a/libapp.so
lib/arm64-v8a/libflutter.so

Signer #1 certificate DN: ...
```

-   `lib/arm64-v8a/libapp.so` exists, confirming it really is an AOT build (a debug build doesn't have this file).
-   Only the single `arm64-v8a` ABI is included, matching `--target-platform android-arm64`.
-   If no release signing is configured, Gradle automatically signs with the debug keystore, which is fine for install verification; a production release needs `android/key.properties` and a keystore configured per the official Flutter documentation.

## Troubleshooting

Symptom

Cause

Fix

`Failed to find .../android-arm64-release/linux-arm64/gen_snapshot`

Section 3 wasn't done, or the engine artifacts were overwritten by a reinstall

Redo the wrapper script per section 3

`qemu-x86_64-static: Could not open '/lib64/ld-linux-x86-64.so.2'`

Missing x86\_64 glibc, or you tried to work around it with `-L`

Put the libraries into `/usr/lib/x86_64-linux-gnu` and `/lib64` per section 2

No output for a long time during the AOT stage, high CPU usage

qemu is translating — this is normal

Wait, don't interrupt

`Invalid SDK hash` / snapshot version mismatch

Used a gen\_snapshot from another Flutter version

You must use the x64 binary bundled with the current SDK

Release build reports missing signing

The project has no release signing configured

Configure `android/key.properties` and a keystore, or first produce a debug-signed build to verify the pipeline

## Limitations and Tips

-   **This article only covers android-arm64**: the directory mapping for other ABIs and the profile build is the same, but you should verify it yourself before using it for production builds.
-   **Section 3 must be redone after the engine artifacts are reinstalled**: `flutter upgrade` and `flutter precache` overwrite the qemu wrapper script.
