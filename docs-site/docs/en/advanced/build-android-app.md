# Building Android Apps in the Container

Set up the JDK and Android SDK inside AiVerse's Linux container and you can build Android apps straight from source. This guide uses Debian 12 (bookworm) aarch64 as the example; Ubuntu, Arch and other distributions work the same way.

Use a custom Debian / Ubuntu image

The built-in Alpine container uses musl libc, which needs extra handling to compile Android apps under PRoot (see the note at the end). See "Custom container images" for how to import a custom image.

## Environment Baseline

Item

Value

System

Debian GNU/Linux 12 (bookworm) aarch64

JDK

OpenJDK 17

Gradle

Bundled with the project wrapper, no system install needed

SDK

Android 36 / build-tools 35.0.0

aapt2

ARM64 static build 35.0.2 (community build)

## 1\. Install Base Dependencies

bash

```
apt update
apt install -y openjdk-17-jdk-headless curl wget unzip ripgrep
```

Verify the JDK:

bash

```
/usr/lib/jvm/java-17-openjdk-arm64/bin/java -version
# Should print openjdk version "17..."
```

## 2\. Install the Android SDK

By default the SDK is installed from a domestic mirror (faster and more reliable downloads). Apart from the sdkmanager binary itself (cmdline-tools), which you download and unzip manually, every other component is installed by overriding sdkmanager's default repository root URL with the `SDK_TEST_BASE_URL` environment variable (it must end with `/`, and the mirror must provide `repository2-3.xml`). One command installs everything (`--list` and `--install` are both verified to work):

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

yes | sdkmanager --install "platforms;android-36" "build-tools;35.0.0" "platform-tools"
```

Version notes

Pick component versions based on the target project's `compileSdk` and `buildToolsVersion`. This guide uses Android 36 / build-tools 35.0.0.

Mirror vs. official source

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

yes | sdkmanager --install "platforms;android-36" "build-tools;35.0.0" "platform-tools"
```

### Alternative: Assemble the SDK Manually from a Mirror

If the `SDK_TEST_BASE_URL` method is unavailable, download the component zips from the Tencent Cloud mirror and assemble them by hand. You end up with three directories — `build-tools/`, `platforms/` and `platform-tools/` — matching what sdkmanager produces:

Component

Mirror URL

commandlinetools

`https://mirrors.cloud.tencent.com/AndroidSDK/commandlinetools-linux-13114758_latest.zip`

platform-36

`https://mirrors.cloud.tencent.com/AndroidSDK/platform-36_r01.zip`

build-tools 35.0.0

`https://mirrors.cloud.tencent.com/AndroidSDK/build-tools_r35_linux.zip`

platform-tools

`https://mirrors.cloud.tencent.com/AndroidSDK/platform-tools_r35.0.2-linux.zip`

bash

```
cd /workspace
curl -L -O https://mirrors.cloud.tencent.com/AndroidSDK/platform-36_r01.zip
curl -L -O https://mirrors.cloud.tencent.com/AndroidSDK/build-tools_r35_linux.zip
curl -L -O https://mirrors.cloud.tencent.com/AndroidSDK/platform-tools_r35.0.2-linux.zip

# 1) build-tools: the zip extracts to android-15/, move its contents into build-tools/35.0.0/
unzip build-tools_r35_linux.zip -d ~/android/sdk
mkdir -p ~/android/sdk/build-tools/35.0.0
mv ~/android/sdk/android-15/* ~/android/sdk/build-tools/35.0.0/

# 2) platform: extracts to android-36/
unzip platform-36_r01.zip -d ~/android/sdk
mkdir -p ~/android/sdk/platforms
mv ~/android/sdk/android-36 ~/android/sdk/platforms/android-36

# 3) platform-tools: extracts straight to platform-tools/, no moving needed
unzip platform-tools_r35.0.2-linux.zip -d ~/android/sdk

ls ~/android/sdk   # You should see build-tools/ platforms/ platform-tools/
```

## 3\. Replace the ARM64 Native Binaries

Google's official `aapt2`, `adb` and similar tools are compiled for x86\_64 and cannot run on aarch64, so you need the community-maintained ARM64 static builds.

Download from `https://github.com/lzhiyong/android-sdk-tools/releases/download/35.0.2/android-sdk-tools-static-aarch64.zip`. If the download is slow or fails, prefix the original link with a GitHub proxy (verified working, 2026-08):

Accelerator

Usage

gh-proxy.com

`https://gh-proxy.com/https://github.com/...`

ghproxy.vip

`https://ghproxy.vip/https://github.com/...`

bash

```
# Example: download via gh-proxy.com
curl -L -O https://gh-proxy.com/https://github.com/lzhiyong/android-sdk-tools/releases/download/35.0.2/android-sdk-tools-static-aarch64.zip
# When the container can reach GitHub directly, download straight from the source
# curl -L -O https://github.com/lzhiyong/android-sdk-tools/releases/download/35.0.2/android-sdk-tools-static-aarch64.zip
unzip /workspace/android-sdk-tools-static-aarch64.zip -d ~/armtools35
```

Replace the binaries inside the SDK:

bash

```
cp -p ~/armtools35/build-tools/* ~/android/sdk/build-tools/35.0.0/
cp -p ~/armtools35/platform-tools/* ~/android/sdk/platform-tools/
```

Verify that `aapt2` runs:

bash

```
~/android/sdk/build-tools/35.0.0/aapt2 version
# Should print a version number without errors
```

## 4\. Configuration

### local.properties (project root)

bash

```
echo "sdk.dir=/root/android/sdk" > /workspace/local.properties
```

This file is normally ignored by `.gitignore` and won't pollute the repository.

### Global Gradle Configuration

Write the aapt2 override into the global file, not into the project's `gradle.properties`:

bash

```
mkdir -p ~/.gradle
echo "android.aapt2FromMavenOverride=/root/android/sdk/build-tools/35.0.0/aapt2" > ~/.gradle/gradle.properties
```

### apt Sources (optional)

The container may not reach the official sources by default; switching to a domestic mirror is recommended:

txt

```
URIs: http://mirrors.ustc.edu.cn/debian
URIs: http://mirrors.ustc.edu.cn/debian-security
```

### Downloading a Gradle Distribution (optional)

When a project uses the Gradle wrapper you don't need a system Gradle, but bootstrapping a brand-new project requires a distribution first. If `https://services.gradle.org/distributions/gradle-8.11.1-bin.zip` is slow, use a mirror:

-   Huawei Cloud: `https://mirrors.huaweicloud.com/gradle/gradle-8.11.1-bin.zip`
-   Tencent Cloud: `https://mirrors.cloud.tencent.com/gradle/gradle-8.11.1-bin.zip`

bash

```
curl -L -o /workspace/gradle.zip https://mirrors.huaweicloud.com/gradle/gradle-8.11.1-bin.zip
unzip gradle.zip -d /workspace/tools
/workspace/tools/gradle-8.11.1/bin/gradle --version   # Use it to run builds or generate the wrapper
```

## 5\. Build

bash

```
cd /workspace
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64 \
ANDROID_HOME=/root/android/sdk \
./gradlew assembleDebug
```

When dependencies download slowly, you can switch to the Aliyun mirror in `dependencyResolutionManagement.repositories` inside `settings.gradle.kts` (AGP pulls dependencies from `google()` / `mavenCentral()` by default):

kotlin

```
maven { url = uri("https://maven.aliyun.com/repository/google") }
maven { url = uri("https://maven.aliyun.com/repository/central") }
maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
```

The first build takes roughly 10 to 15 minutes (dependency downloads plus Kotlin compilation); incremental builds after that take 1 to 2 minutes. The Gradle daemon works fine under PRoot and speeds up incremental builds; if the daemon exits abnormally and causes lock-file conflicts, add `--no-daemon` for a single run.

Once the build succeeds, the APK output path depends on the project configuration and is usually:

txt

```
app/build/outputs/apk//debug/app--debug.apk
```

## Troubleshooting

Symptom

Cause

Fix

`JAVA_HOME is not set`

Environment variable not set

Add `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64` on the command line

`SDK location not found`

Missing local.properties

Create `local.properties` with `sdk.dir=/root/android/sdk`

`aapt2 syntax error`

Using the x86 aapt2

Make sure `aapt2FromMavenOverride` in `~/.gradle/gradle.properties` points to the ARM build

`Failed to install build-tools`

AGP online verification failed

Confirm the build-tools version the project needs is installed locally

apt download 403

Mirror temporarily unavailable

Switch mirrors (Tsinghua → USTC)

Gradle lock-file conflict

Previous build didn't exit cleanly

Delete `~/.gradle/caches/*.lock`, or add `--no-daemon`

## Appendix: Fix for the Built-in Alpine Container

Building Android apps inside the built-in Alpine container may hit `Failed to delete /tmp/tempdir_*`. The cause is that AGP's apkzlib `TemporaryFile` uses `File.delete()` to remove temp directories, which can fail under PRoot. Custom Debian images are not affected.

Version-specific

The code below targets apkzlib in Gradle 8.11.1 / AGP 8.x (`com.android.tools.build.apkzlib.bytestorage.TemporaryFile`). Package paths and internals may differ across versions — adjust based on the jar you actually use.

Replace `File.delete()` with `Files.delete()` (goes through unlinkat to bypass the failing low-level delete):

java

```
package com.android.tools.build.apkzlib.bytestorage;

import com.google.common.base.Preconditions;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;

public class TemporaryFile implements Closeable {
    private boolean deleted;
    private final File file;

    public TemporaryFile(File file) {
        this.deleted = false;
        this.file = file;
    }

    public File getFile() {
        Preconditions.checkState(!this.deleted, "File already deleted");
        return this.file;
    }

    @Override
    public void close() throws IOException {
        if (this.deleted) {
            return;
        }
        this.deleted = true;
        deleteFile(this.file);
    }

    private void deleteFile(File f) throws IOException {
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) {
                for (File kid : kids) {
                    deleteFile(kid);
                }
            }
        }
        if (f.exists()) {
            try {
                Files.delete(f.toPath());
            } catch (NoSuchFileException e) {
                // Already deleted, ignore
            } catch (IOException e) {
                if (!f.delete()) {
                    throw new IOException("Failed to delete '" + f.getAbsolutePath() + "'", e);
                }
            }
        }
    }
}
```

Compile this source and replace the `.class` files back into the `instrumented-apkzlib-*.jar` in the Gradle cache.
