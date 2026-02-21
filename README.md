# Komelia - Komga media client

### Downloads:

- Latest prebuilt release is available at https://github.com/Snd-R/Komelia/releases
- Google Play Store https://play.google.com/store/apps/details?id=io.github.snd_r.komelia
- F-Droid https://f-droid.org/packages/io.github.snd_r.komelia/
- AUR package https://aur.archlinux.org/packages/komelia

## Screenshots

<details>
  <summary>Mobile</summary>
   <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" alt="Komelia" width="270">  
   <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" alt="Komelia" width="270">  
   <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" alt="Komelia" width="270">  
   <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" alt="Komelia" width="270">  
   <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" alt="Komelia" width="270">  
   <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/6.png" alt="Komelia" width="270">  
</details>

<details>
  <summary>Tablet</summary>
   <img src="/fastlane/metadata/android/en-US/images/tenInchScreenshots/1.jpg" alt="Komelia" width="400" height="640">  
   <img src="/fastlane/metadata/android/en-US/images/tenInchScreenshots/2.jpg" alt="Komelia" width="400" height="640">  
   <img src="/fastlane/metadata/android/en-US/images/tenInchScreenshots/3.jpg" alt="Komelia" width="400" height="640">  
   <img src="/fastlane/metadata/android/en-US/images/tenInchScreenshots/4.jpg" alt="Komelia" width="400" height="640">  
   <img src="/fastlane/metadata/android/en-US/images/tenInchScreenshots/5.jpg" alt="Komelia" width="400" height="640">  
   <img src="/fastlane/metadata/android/en-US/images/tenInchScreenshots/6.jpg" alt="Komelia" width="400" height="640">  
</details>

<details>
  <summary>Desktop</summary>
   <img src="/screenshots/1.jpg" alt="Komelia" width="1280">  
   <img src="/screenshots/2.jpg" alt="Komelia" width="1280">  
   <img src="/screenshots/3.jpg" alt="Komelia" width="1280">  
   <img src="/screenshots/4.jpg" alt="Komelia" width="1280">  
   <img src="/screenshots/5.jpg" alt="Komelia" width="1280">  
</details>

[//]: # (![screenshots]&#40;./screenshots/screenshot.jpg&#41;)

## Native libraries build instructions

Android and JVM targets require C and C++ compiler for native libraries as well nodeJs for epub reader build

The recommended way to build native libraries is by using docker images that contain all required build dependencies\
If you want to build with system toolchain and dependencies try running:\
`./gradlew komeliaBuildNonJvmDependencies` (Linux Only)

## Desktop App Build

Requires jdk 17 or higher

To build with docker container, replace <*platform*> placeholder with your target platform\
Available platforms include: `linux-x86_64`, `windows-x86_64`

- `docker build -t komelia-build-<platfrom> . -f ./cmake/<paltform>.Dockerfile `
- `docker run -v .:/build komelia-build-<paltform>`
- `./gradlew <platform>_copyJniLibs` - copy built shared libraries to resource directory that will be bundled with the
  app
- `./gradlew buildWebui` - build and copy epub reader webui (npm is required for build)

Then choose your packaging option:
- `./gradlew :komelia-app:run` to launch desktop app
- `./gradlew :komelia-app:packageReleaseUberJarForCurrentOS` package jar file (output in `komelia-app/build/compose/jars`)
- `./gradlew :komelia-app:packageReleaseDeb` package Linux deb file (output in `komelia-app/build/compose/binaries`)
- `./gradlew :komelia-app:packageReleaseMsi` package Windows msi installer (output in `komelia-app/build/compose/binaries`)

## Android App Build

To build with docker container, replace <*arch*> placeholder with your target architecture\
Available architectures include:  `aarch64`, `armv7a`, `x86_64`, `x86`

- `docker build -t komelia-build-android . -f ./cmake/android.Dockerfile `
- `docker run -v .:/build komelia-build-android <arch>`
- `./gradlew <arch>_copyJniLibs` - copy built shared libraries to resource directory that will be bundled with the app
- `./gradlew buildWebui` - build and copy epub reader webui (npm is required for build)

Then choose app build option:

- `./gradlew :komelia-app:assembleDebug` debug apk build (output in `komelia-app/build/outputs/apk/debug`)
- `./gradlew :komelia-app:assembleRelease` unsigned release apk build (output in
  `komelia-app/build/outputs/apk/release`)

## Komf Extension Build

run`./gradlew :komelia-komf-extension:app:packageExtension` \
output archive will be in `./komelia-komf-extension/app/build/distributions`

---

## Custom Android APK Build (Windows — Full Walkthrough)

This section documents the complete process for building a custom Komelia Android APK on Windows,
including compiling native C/C++ libraries via Docker and signing the final APK.

### Prerequisites

- **Docker Desktop** installed and running
- **JDK 17+** installed
- **Git** (with submodules initialized)

### Step 0: Initialize Submodules (first time only)

```powershell
git submodule update --init --recursive
```

### Configurable Settings

Before building, you can tweak the page prefetch and cache settings in:

```
komelia-ui/src/commonMain/kotlin/snd/komelia/ui/reader/image/paged/PagedReaderState.kt
```

Look for the `companion object` at the top of the `PagedReaderState` class:

```kotlin
companion object {
    const val PREFETCH_SPREAD_COUNT = 5   // spreads to prefetch ahead and behind
    const val IMAGE_CACHE_SIZE = 30L      // max decoded pages kept in memory
}
```

- **`PREFETCH_SPREAD_COUNT`** — Number of spreads loaded ahead/behind current page. Higher = faster
  page turns but more memory. Default upstream is `1`, custom default is `5`.
- **`IMAGE_CACHE_SIZE`** — Max decoded page images in memory. Should be ≥ `PREFETCH_SPREAD_COUNT * 2 + 1`.
  Default upstream is `10`, custom default is `30`.

### Step 1: Build Native Libraries via Docker

```powershell
# Build the Docker image (one-time, ~10 min)
docker build -t komelia-build-android . -f ./cmake/android.Dockerfile

# Compile native libraries for arm64 (S23 Ultra, Pixel, etc.) (~30 min)
docker run -u root -v ${PWD}:/build komelia-build-android aarch64
```

> **Note:** The `-u root` flag is required on Windows to avoid permission issues with
> volume mounts.

### Step 2: Copy Native Libraries into the Project

```powershell
# Run inside Docker since the Android SDK is there
docker run --entrypoint bash -u root -v ${PWD}:/build komelia-build-android -c './gradlew android-aarch64_copyJniLibs'
```

### Step 3: Build the Release APK

```powershell
# Create local.properties pointing to the Docker SDK path
echo "sdk.dir=/android-sdk" > local.properties

# Build the APK inside Docker
docker run --entrypoint bash -u root -v ${PWD}:/build komelia-build-android -c './gradlew :komelia-app:assembleRelease'
```

### Step 4: Sign the APK

Android requires APK Signature Scheme v2/v3 (using `apksigner`, NOT `jarsigner`).

```powershell
# Generate a debug keystore (one-time only)
docker run --entrypoint bash -u root -v ${PWD}:/build komelia-build-android -c 'keytool -genkeypair -v -keystore /build/komelia-debug.keystore -alias komelia -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=Debug,OU=Debug,O=Debug,L=Debug,ST=Debug,C=US"'

# Install build-tools, zipalign, and sign — all in one command
docker run --entrypoint bash -u root -v ${PWD}:/build komelia-build-android -c '/android-sdk/cmdline-tools/latest/bin/sdkmanager "build-tools\;35.0.0" && /android-sdk/build-tools/35.0.0/zipalign -f -v -p 4 /build/komelia-app/build/outputs/apk/release/komelia-app-release-unsigned.apk /build/komelia-app/build/outputs/apk/release/komelia-app-release-aligned.apk && /android-sdk/build-tools/35.0.0/apksigner sign --ks /build/komelia-debug.keystore --ks-key-alias komelia --ks-pass pass:android --key-pass pass:android --out /build/komelia-app/build/outputs/apk/release/komelia-app-release.apk /build/komelia-app/build/outputs/apk/release/komelia-app-release-aligned.apk'
```

### Step 5: Install

The signed APK is at:

```
komelia-app/build/outputs/apk/release/komelia-app-release.apk
```

Transfer this file to your Android device and install it. If updating from a different signing key,
you must uninstall the old version first.

### Important Notes

- **Keep the keystore:** Future updates must be signed with the **same** `komelia-debug.keystore`.
  If you lose it, you'll need to uninstall the app before installing a new build.
- **Rebuilding after config changes:** If you only changed `PREFETCH_SPREAD_COUNT` or
  `IMAGE_CACHE_SIZE`, you can skip Step 1 (native libs don't change). Just repeat Steps 3–5.
- **Output files in the release directory:**
  - `komelia-app-release-unsigned.apk` — raw from Gradle (don't install)
  - `komelia-app-release-aligned.apk` — intermediate zipaligned (don't install)
  - **`komelia-app-release.apk`** — ✅ final signed APK (install this one)