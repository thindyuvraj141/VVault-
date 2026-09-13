# Fixing the failed build & getting a real APK

## Why the build failed

The old workflow ran `./gradlew assembleDebug`, which only works on a **native Android/Gradle project** (one with `settings.gradle`, `build.gradle`, etc.). This repo is a **static web app** (HTML/CSS/JS + PWA) — there's no Gradle project here, and there was never meant to be one. That's exactly what the log says:

> `Directory '/home/runner/work/V-Vault/V-Vault' does not contain a Gradle build.`

Trying to "fix" that workflow won't work — it's solving the wrong problem. The real fix is: **stop trying to Gradle-build this repo**, and instead turn the working PWA into an APK the way PWAs are actually turned into APKs — by wrapping the live, hosted site.

## Step 1 — Remove the broken workflow

In your repo, find the workflow file that runs `assembleDebug` (likely `.github/workflows/build.yml` or similar — whatever file has the `Build APK` job shown in your screenshot) and delete it, or disable it. It can't succeed as written.

## Step 2 — Give the app a live URL (GitHub Pages)

Add the file from this package at `.github/workflows/pages.yml` to your repo (same path), then:

1. Repo → **Settings → Pages**
2. Under **Build and deployment → Source**, choose **GitHub Actions**
3. Push to `main` — the workflow deploys automatically
4. Your app will be live at:
   `https://<your-username>.github.io/<your-repo-name>/`

   (Based on your screenshot, that's probably `https://thindyuv.github.io/V-Vault/` — confirm the exact URL under **Settings → Pages** once it deploys.)

Open that URL on your phone first and confirm the app loads and installs correctly (Add to Home Screen) — this also confirms the manifest and service worker are being served correctly, which the next step depends on.

## Step 3 — Generate the actual APK (PWABuilder)

This is the standard, reliable way to turn a PWA into a real Android `.apk`/`.aab` — no custom CI needed, and it's free:

1. Go to **[pwabuilder.com](https://www.pwabuilder.com)**
2. Paste your live GitHub Pages URL and click **Start**
3. PWABuilder scans your manifest + service worker and shows a score/checklist
4. Click **Package for stores → Android**
5. Download the generated `.apk` (for sideloading/testing) or `.aab` (for Play Store)

This produces a **Trusted Web Activity (TWA)** — a real, installable Android app that opens your PWA full-screen with no browser UI, uses your app icon, and works offline via the same service worker already in this repo. No Flutter/native rewrite needed for this.

## If you want it fully signed for Play Store later

PWABuilder can generate a signing key for you, or let you upload your own. Keep that keystore file somewhere safe — you'll need the same one for every future update of the app on Play Store.

## Note on the native app guide in `docs/`

The `docs/native-app-guide.md` in this repo describes a **separate, bigger path** — rewriting the app from scratch in Flutter/React Native for deeper native features (biometric unlock, encrypted photo storage, etc.). That's a real ground-up rebuild, not something the current CI can produce. The TWA/PWABuilder approach above is the fast path to an installable APK from what you already have; the native rewrite is the path to a more capable, store-polished app later.
