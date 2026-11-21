# Building Flutter Apps in the Container

After you set up the JDK, Android SDK and Flutter SDK inside AiVerse's Linux container (PRoot), you can build the Android APK of a Flutter app straight from source. This guide uses Debian 12 (bookworm) aarch64 as the example, and the debug APK build has been verified inside a PRoot container.

Scope

This article builds a **debug APK** (JIT mode, with native ARM64 support). A **release build** needs an extra AOT compiler supplied via qemu translation — see "Building a Flutter release APK in the container".

Mirrors and Network Notes

This article uses domestic mirrors for installation and building by default (faster and more reliable downloads). The mirror output is fully equivalent to the official one: even if the network can reach official sources such as `dl.google.com` directly, there is no need to switch back to the official configuration.

## Environment Baseline

Item

Value

System

Debian GNU/Linux 12 (bookworm) aarch64 (PRoot)

JDK

OpenJDK 17 (headless)

Flutter

3.27.4 stable (git clone at a pinned tag)

Dart

3.6.2 (linux-arm64)

Gradle

Bundled with the project wrapper (8.3), no system install needed

SDK

platforms android-35, build-tools 33.0.1 + 35.0.0, platform-tools

aapt2

ARM64 static build (lzhiyong/android-sdk-tools)

## 1\. Install Base Dependencies

bash

```
apt update
apt install -y openjdk-17-jdk-headless curl unzip zip git
```

Verify the JDK:

bash

```
java -version
# openjdk version "17..."
```

## 2\. Install the Android SDK

By default it is installed from a domestic mirror (faster and more reliable downloads). Apart from the sdkmanager binary itself (cmdline-tools), which you download and unzip manually, every other component is installed by overriding sdkmanager's default repository root URL with the `SDK_TEST_BASE_URL` environment variable (it must end with `/`, and the mirror must provide `repository2-3.xml`). One command installs everything (`--list` and `--install` are both verified to work):

bash

```
# The only manual step: fetch the sdkmanager binary (download from the mirror)
mkdir -p ~/android/sdk && cd ~/android/sdk
curl -fL -C - -o clt.zip https://mirrors.cloud.tencent.com/AndroidSDK/commandlinetools-linux-13114758_latest.zip
unzip -q clt.zip
mkdir -p cmdline-tools/latest && mv cmdline-tools/* cmdline-tools/latest/

# All components installed through the mirror (handles directory layout and license acceptance automatically)
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
export ANDROID_HOME=$HOME/android/sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
export SDK_TEST_BASE_URL=https://mirrors.cloud.tencent.com/AndroidSDK/

yes | sdkmanager --install "platforms;android-35" "build-tools;33.0.1" "build-tools;35.0.0" "platform-tools"
```

Version Notes

The Flutter 3.27 template has `compileSdk` 35 (needs `platforms;android-35`), and its `buildToolsVersion` defaults to **33.0.1** (if it's missing you get `Failed to find Build Tools revision 33.0.1`). build-tools 35.0.0 is used for the aapt2 override in section 5; it's optional but recommended to install.

Mirror vs. Official Source

Although `SDK_TEST_BASE_URL` is an environment variable intended for testing (not an officially documented setting), it is widely used by mirror-based setups. An SDK installed from a mirror is fully equivalent to the official one: even if the network can reach `dl.google.com` directly, there is no need to switch back to the official flow. If this method stops working in a particular case, fall back to the official source (below) or assemble the SDK manually.

### Optional: Install from the Official Source

If the network can reach `dl.google.com` directly, you can use the official standard flow:

bash

```
mkdir -p ~/android/sdk && cd ~/android/sdk
curl -O https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip
unzip -q commandlinetools-linux-*.zip
mkdir -p cmdline-tools/latest && mv cmdline-tools/* cmdline-tools/latest/

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
export ANDROID_HOME=$HOME/android/sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH

yes | sdkmanager --install "platforms;android-35" "build-tools;33.0.1" "build-tools;35.0.0" "platform-tools"
```

### Alternative: Assemble the SDK Manually from a Mirror

If the `SDK_TEST_BASE_URL` method is unavailable, download the component zips from the Tencent Cloud mirror and assemble them by hand. You end up with four directories — `build-tools/`, `platforms/`, `platform-tools/` and `cmdline-tools/` — matching what sdkmanager produces:

Component

Mirror URL

commandlinetools

`https://mirrors.cloud.tencent.com/AndroidSDK/commandlinetools-linux-13114758_latest.zip`

platform-35

`https://mirrors.cloud.tencent.com/AndroidSDK/platform-35_r02.zip`

build-tools 33.0.1

`https://mirrors.cloud.tencent.com/AndroidSDK/build-tools_r33.0.1-linux.zip`

build-tools 35.0.0

`https://mirrors.cloud.tencent.com/AndroidSDK/build-tools_r35_linux.zip`

platform-tools

`https://mirrors.cloud.tencent.com/AndroidSDK/platform-tools_r35.0.2-linux.zip`

Large downloads from a mirror are prone to connection drops, so it's best to use `curl -fL -C -` for resumable downloads combined with a retry loop. Directory assembly:

bash

```
cd ~/android/sdk
# cmdline-tools: extracts to cmdline-tools/, rename it to latest
unzip clt.zip && mv cmdline-tools cmdline-tools/latest   # Actual extracted layout depends on the zip
# platform-35: extracts to android-35/, move it into platforms/
mkdir -p platforms && mv android-35 platforms/android-35
# build-tools: the zip extracts to android-13/ (33.0.1) or android-15/ (35.0.0), move into the matching version directory
mkdir -p build-tools/33.0.1 build-tools/35.0.0
mv android-13/* build-tools/33.0.1/ && mv android-15/* build-tools/35.0.0/
# platform-tools: extracts straight to platform-tools/, no moving needed
```

## 3\. Replace the ARM64 Native Binaries

Google's official `aapt2`, `aidl`, `zipalign`, `adb` and similar tools are compiled for x86\_64 and cannot run on aarch64 (an architecture limitation, unrelated to the network), so you must replace them with the community-provided ARM64 static builds. Download from `https://github.com/lzhiyong/android-sdk-tools/releases/download/35.0.2/android-sdk-tools-static-aarch64.zip`; if the download is slow or fails, prefix the original link with a GitHub proxy: `https://gh-proxy.com/https://github.com/...`

bash

```
curl -L -O https://github.com/lzhiyong/android-sdk-tools/releases/download/35.0.2/android-sdk-tools-static-aarch64.zip
unzip android-sdk-tools-static-aarch64.zip -d ~/armtools35
# Overwrite build-tools (both the 33.0.1 and 35.0.0 directories) and platform-tools
cp -p ~/armtools35/build-tools/* ~/android/sdk/build-tools/35.0.0/
cp -p ~/armtools35/build-tools/* ~/android/sdk/build-tools/33.0.1/
cp -p ~/armtools35/platform-tools/* ~/android/sdk/platform-tools/
chmod +x ~/android/sdk/build-tools/*/* ~/android/sdk/platform-tools/*
```

Verify:

bash

```
~/android/sdk/build-tools/35.0.0/aapt2 version   # Normal if it prints a version number
~/android/sdk/build-tools/35.0.0/zipalign        # Normal if it prints usage
```

## 4\. Install the Flutter SDK

Use git to clone a pinned tag:

bash

```
git clone --depth 1 --branch 3.27.4 https://github.com/flutter/flutter.git ~/.local/flutter-3.27.4
export PATH=$PATH:~/.local/flutter-3.27.4/bin

flutter --version   # The first run downloads the dart-sdk and engine artifacts automatically
```

Handling a Slow dart-sdk Install Under PRoot

The first run of `flutter --version` downloads the dart-sdk (about 210MB) and unzips it. Under PRoot this step may hang for a long time (slow download, slow unzip, lock waiting). If it gets stuck, you can complete the install manually to bypass the bootstrap:

bash

```
cd ~/.local/flutter-3.27.4/bin/cache
EV=$(cat ../internal/engine.version)     # e.g. 82bd5b7209295a5b7ff8cae0df96e7870171e3a5
curl -fL -o dart-sdk-linux-arm64.zip \
  "https://storage.flutter-io.cn/flutter_infra_release/flutter/$EV/dart-sdk-linux-arm64.zip"
# When the network can reach it, you can also use the official source: https://storage.googleapis.com/flutter_infra_release/flutter/$EV/dart-sdk-linux-arm64.zip
unzip -q dart-sdk-linux-arm64.zip -d .   # Extracts to dart-sdk/
rm -f dart-sdk-linux-arm64.zip
echo "$EV" > engine-dart-sdk.stamp       # Write the stamp; flutter will then skip the automatic download
```

Verify:

bash

```
~/.local/flutter-3.27.4/bin/cache/dart-sdk/bin/dart --version
# Dart SDK version: 3.6.2 ... on "linux_arm64"
```

## 5\. Configuration

### local.properties

`flutter create` generates it automatically (including `sdk.dir` and `flutter.sdk`), so you don't need to write it by hand — just confirm the contents are correct:

bash

```
cat ~/workspace//android/local.properties
# sdk.dir=/root/android/sdk
# flutter.sdk=/root/.local/flutter-3.27.4
```

### Global Gradle Configuration

Write the aapt2 override into the global file, not into the project's `gradle.properties`:

bash

```
mkdir -p ~/.gradle
echo "android.aapt2FromMavenOverride=/root/android/sdk/build-tools/35.0.0/aapt2" > ~/.gradle/gradle.properties
```

The container has roughly 4G of memory available, so the project `android/gradle.properties` default of `-Xmx4G` can trigger OOM; it's recommended to lower it:

properties

```
org.gradle.jvmargs=-Xmx2G -XX:MaxMetaspaceSize=1G -XX:+HeapDumpOnOutOfMemoryError
```

### Mirror Configuration (Default)

When Gradle resolves dependencies, if the repository list contains `google()` (pointing at dl.google.com), an unreachable network causes it to hang for a long time on connection timeouts. It's recommended to just use the mirror repositories by default (faster builds), and even when the network can reach official sources there's no need to revert the following configuration:

1.  **Gradle wrapper mirror**: the `distributionUrl` in the project's `android/gradle/wrapper/gradle-wrapper.properties` defaults to `services.gradle.org`; change it to a domestic mirror:

txt

```
distributionUrl=https\://mirrors.huaweicloud.com/gradle/gradle-8.3-all.zip
```

1.  **Dependency repository mirrors**: remove `google()` / `mavenCentral()` / `gradlePluginPortal()` from all repositories and keep only the Aliyun mirrors:

groovy

```
maven { url 'https://maven.aliyun.com/repository/google' }
maven { url 'https://maven.aliyun.com/repository/central' }
maven { url 'https://maven.aliyun.com/repository/gradle-plugin' }   // Add the plugin repository when needed
```

There are 5 places to modify in total:

File

Description

Project `android/settings.gradle`

pluginManagement.repositories

Project `android/build.gradle`

allprojects.repositories

`flutter SDK/packages/flutter_tools/gradle/settings.gradle.kts`

:gradle subproject repositories (if not modified, AGP 7.3.0 will connect to dl.google.com)

`flutter SDK/packages/flutter_tools/gradle/resolve_dependencies.gradle`

Dependency pre-resolution

`flutter SDK/packages/flutter_tools/gradle/src/main/kotlin/dependency_version_checker.gradle.kts`

Version-check buildscript

Note

The Flutter engine artifact repository (`download.flutter.io`) is injected automatically by the Flutter plugin, and its address is assembled from `FLUTTER_STORAGE_BASE_URL` — do not delete it. Of the 5 places above, you only need to handle the google/mavenCentral entries. Files inside the SDK are overwritten after a Flutter upgrade, so you'll have to make these changes again.

1.  **Engine artifacts and dependency download source**: set environment variables at build time so that flutter's dart-sdk, engine artifacts and pub packages use a Chinese mirror or the official source:

bash

```
export FLUTTER_STORAGE_BASE_URL=https://storage.flutter-io.cn   # Engine artifacts etc. use the Chinese mirror
unset PUB_HOSTED_URL    # Use the official pub.dev (usually reachable directly); pub.flutter-io.cn has had content-hash verification failures
```

## 6\. Build

bash

```
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
export PATH=$PATH:~/.local/flutter-3.27.4/bin
export FLUTTER_STORAGE_BASE_URL=https://storage.flutter-io.cn   # Engine artifacts etc. use the Chinese mirror (recommended default)
unset PUB_HOSTED_URL    # Use the official pub.dev (recommended default, usually reachable directly)

cd ~/workspace/
flutter build apk --debug --target-platform android-arm64
```

The environment variables above are the recommended defaults (explained in section 5); you can keep them even when the network can reach official sources — no need to revert them.

The first build takes roughly 3 to 10 minutes (downloading the Gradle distribution, dependencies and compiling, depending on the network); incremental builds after that take 1 to 2 minutes. The Gradle daemon works fine under PRoot; if the daemon exits abnormally and causes lock-file conflicts, add `--no-daemon` or delete `~/.gradle/caches/*.lock`.

Once the build succeeds, the APK output is:

txt

```
build/app/outputs/flutter-apk/app-debug.apk
```

You can verify it with `aapt2 dump badging`:

bash

```
~/android/sdk/build-tools/35.0.0/aapt2 dump badging build/app/outputs/flutter-apk/app-debug.apk | head -3
```

If you need a release build, follow "Building a Flutter release APK in the container" on the same environment to add qemu, then build.

## Troubleshooting

Symptom

Cause

Fix

`Connect to dl.google.com timed out` (hangs for a long time)

Repositories include `google()`/`mavenCentral()` and dl.google.com is unreachable

Use the mirror repositories per section 5 (default configuration)

`Could not resolve io.flutter:flutter_embedding_debug`

Same as above — flutter engine dependency resolution reaches dl.google.com

Same as above

`Failed to find Build Tools revision 33.0.1`

The Flutter template defaults to buildTools 33.0.1 and the SDK is missing that version

Install `build-tools;33.0.1` (and replace the ARM64 binaries)

`aapt2 ... cannot execute binary / syntax error`

Using the x86 aapt2

Make sure `aapt2FromMavenOverride` in `~/.gradle/gradle.properties` points to the ARM64 build

flutter bootstrap downloading the dart-sdk hangs / keeps waiting on locks

Automatic download and unzip are unreliable under PRoot

Install the dart-sdk manually per section 4 and write the stamp

`Downloaded archive for xxx had wrong content-hash` (pub retries over and over)

The pub mirror returned a corrupt package

`unset PUB_HOSTED_URL` to use the official pub.dev, and clear `~/.pub-cache`

`flutter doctor` Android toolchain check times out

doctor verifies dl.google.com online via sdkmanager internally

This is normal when dl.google.com is unreachable and does not affect the build

Gradle lock-file conflict

The previous build didn't exit cleanly

Delete `~/.gradle/caches/*.lock`, or add `--no-daemon`

No progress visible after piping the build command

\`\\\\

tail\` buffers the output

Run the build command directly without piping to tail; failure messages show up during the first 1–3 minutes of dependency resolution

## Limitations and Tips

-   **A release build needs extra setup**: Android AOT's gen\_snapshot only ships as a linux-x64 build, so you need to run it through qemu translation per "Building a Flutter release APK in the container"; or switch to remote builds such as GitHub Actions (running `flutter build apk --release` on x64).
-   The template `flutter create` uses `compileSdk=35` and `buildToolsVersion=33.0.1`; before changing the template, make sure the matching SDK components are installed.
-   Extracting large numbers of small files under PRoot (such as the dart-sdk and dependency caches) being slow is normal — don't mistake it for a hang.
-   If dependency resolution keeps failing, first use `curl -sI` on its own to verify reachability of the repository URL, then check the repository order.
