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
including compiling native C/C++ libraries via Docker, building the EPUB reader webui, and signing the final APK.

### Prerequisites

- **Docker Desktop** installed and running
- **JDK 17+** installed
- **Git** (with submodules initialized)

### Step 0: Initialize Submodules (first time only)

```powershell
git submodule update --init --recursive
```

### Custom Modifications in This Fork

This fork includes the following enhancements:

| Feature | Details |
|---------|---------|
| **Prefetch & Cache Settings** | Configurable via **Settings > Image Reader** (sliders for Prefetch Spread Count and Image Cache Size) |
| **Offline Reading Priority** | When a book is downloaded for offline, the reader loads from the local copy instead of streaming |
| **Offline Badge** | Shows "📱 Offline" badge in top-right of reader when reading from local copy |
| **Image Card Size** | Minimum card size lowered from 150dp to 80dp (Settings > Appearance) |

All settings are persisted in the database and survive app restarts. No code editing needed.

### Step 1: Build Native Libraries via Docker

```powershell
# Build the Docker image (one-time, ~10 min)
docker build -t komelia-build-android . -f ./cmake/android.Dockerfile

# Compile native libraries for arm64 (S23 Ultra, Pixel, etc.) (~30 min first time)
docker run -u root -v ${PWD}:/build komelia-build-android aarch64
```

> **Note:** The `-u root` flag is required on Windows to avoid permission issues with
> volume mounts.

### Step 2: Copy Native Libraries into the Project

```powershell
# Run inside Docker since the Android SDK is there
docker run --entrypoint bash -u root -v ${PWD}:/build komelia-build-android -c './gradlew android-aarch64_copyJniLibs'
```

### Step 3: Build EPUB Reader WebUI + Release APK + Sign (All-in-One)

This single command handles everything: builds the EPUB reader webuis (komga + ttu),
copies them to compose resources, runs the Gradle release build, zipaligns, and signs.

```powershell
docker run --rm --entrypoint bash -u root -v ${PWD}:/build komelia-build-android -c 'cd /build/komelia-epub-reader/komga-webui && npm ci && npm run build && cd /build/komelia-epub-reader/ttu-ebook-reader && npm ci && npm run build && mkdir -p /build/komelia-ui/src/commonMain/composeResources/files && cp /build/komelia-epub-reader/komga-webui/dist/* /build/komelia-ui/src/commonMain/composeResources/files/ && cp /build/komelia-epub-reader/ttu-ebook-reader/dist/* /build/komelia-ui/src/commonMain/composeResources/files/ && cd /build && rm -rf .gradle && ./gradlew :komelia-app:assembleRelease && /android-sdk/build-tools/35.0.0/zipalign -f -v -p 4 /build/komelia-app/build/outputs/apk/release/komelia-app-release-unsigned.apk /build/komelia-app/build/outputs/apk/release/komelia-app-release-aligned.apk && /android-sdk/build-tools/35.0.0/apksigner sign --ks /build/komelia-debug.keystore --ks-key-alias komelia --ks-pass pass:android --key-pass pass:android --out /build/komelia-app/build/outputs/apk/release/komelia-app-release.apk /build/komelia-app/build/outputs/apk/release/komelia-app-release-aligned.apk'
```

> **Note:** The `rm -rf .gradle` step is critical to prevent `java.io.IOException` errors caused by Gradle caching issues across Docker volume mounts. We also added `--rm` so the container removes itself after exiting.

### Step 3a: Generate Keystore (first time only)

If you don't already have `komelia-debug.keystore` in the project root:

```powershell
docker run --entrypoint bash -u root -v ${PWD}:/build komelia-build-android -c 'keytool -genkeypair -v -keystore /build/komelia-debug.keystore -alias komelia -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=Debug,OU=Debug,O=Debug,L=Debug,ST=Debug,C=US"'
```

### Step 4: Install

The signed APK is at:

```
komelia-app/build/outputs/apk/release/komelia-app-release.apk
```

Transfer this file to your Android device and install it. If updating from a different signing key,
you must uninstall the old version first.

### Build Timings (approximate)

| Phase | Time |
|-------|------|
| komga-webui `npm ci` | ~4 min |
| komga-webui `npm run build` | ~25 sec |
| ttu-reader `npm ci` | ~2 min |
| ttu-reader `npm run build` | ~40 sec |
| Gradle configure | ~3 min |
| Kotlin compile + R8 minify | ~15 min |
| zipalign + apksigner | ~30 sec |
| **Total (full rebuild)** | **~25 min** |

> Incremental builds (code-only changes, no native lib changes) skip Step 1 and are ~20 min.

### Important Notes

- **Keep the keystore:** Future updates must be signed with the **same** `komelia-debug.keystore`.
  If you lose it, you'll need to uninstall the app before installing a new build.
- **Rebuilding after settings/code changes:** If you only changed Kotlin source code,
  you can skip Step 1 (native libs don't change). Just repeat Step 3–4.
- **EPUB reader:** The EPUB webui must be built at least once (included in Step 3).
  On subsequent builds it's cached unless you clean the compose resources.
- **Output files in the release directory:**
  - `komelia-app-release-unsigned.apk` — raw from Gradle (don't install)
  - `komelia-app-release-aligned.apk` — intermediate zipaligned (don't install)
  - **`komelia-app-release.apk`** — ✅ final signed APK (install this one)