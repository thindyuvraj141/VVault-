# V Vault — Native Android App (via Capacitor)

## Ye kya karta hai

Ye workflow aapke `index.html`/`manifest.json`/`sw.js`/icons ko ek asli Android app package (`.apk`) mein daal deta hai. Farak:

| PWA (abhi tak) | Capacitor app (ab) |
|---|---|
| Browser mein khulti hai, "Add to Home Screen" | Play Store jaisi real `.apk` file, seedha install |
| URL bar chhup jata hai lekin technically web hai | Code app ke andar bunda hua hai, koi URL nahi |
| Chrome/browser engine use karta hai | Same engine, lekin app ke apne container mein — user ko pata nahi chalta |
| Update GitHub Pages se automatically aata hai | Update ke liye naya APK build + reinstall karna hoga |

**Important honesty:** Capacitor bhi andar se WebView (chhota browser engine) use karta hai — lekin user ko koi fark nazar nahi aata. Play Store pe aisi lakhon apps hain jo isi tarah bani hain (Instagram ke kuch hisse, kayi banking apps ke kuch screens waghera). Ye "asli app" hi hai jahan tak user experience, installation, aur Play Store listing ka sawal hai.

## Setup steps

1. Repo mein `.github/workflows/build-android.yml` daalo (isi zip mein hai)
2. **Actions** tab pe jao, workflow ko manually trigger karo (ya `main` branch pe push karo)
3. 5-8 minute lagenge (Android SDK download + Gradle build)
4. Build complete hone pe, workflow run ke page pe neeche **"Artifacts"** section mein **"vvault-android-app"** milega — usay download karo
5. Andar `app-debug.apk` hai — phone pe transfer karo, "Unknown apps" permission do, install karo

## Zaroori limitations (honestly)

Kuch browser features WebView ke andar thoda alag behave karte hain:

- **Fingerprint/Face unlock (WebAuthn):** Ho sakta hai Capacitor ke WebView mein biometric prompt na aaye, ya alag tarah se behave kare. Agar ye na chale, password/PIN unlock hamesha kaam karega (fallback hai).
- **Share button:** `navigator.share()` kuch Android WebView versions mein kaam nahi karta bina extra native plugin ke.
- **Service worker (`sw.js`):** App ke andar sab kuch already bunda hua hai, is liye offline-caching wali service worker ki zarurat nahi rahegi — harmless hai, chalti rahegi bina asar ke.

Agar in features (biometric/share) ko poori tarah native banana ho, Capacitor plugins add karne padenge (`@capacitor/biometric`, `@capacitor/share`) — abhi ke liye app chalegi, bas ye do features shayad thoda alag react karein.

## Play Store pe daalne ke liye (baad mein)

Abhi ye **debug APK** hai (testing ke liye, sideload karne ke liye theek hai). Play Store pe daalne ke liye:

1. `assembleDebug` ko `bundleRelease` se badalna hoga workflow mein
2. Signing key banani hogi (`keytool` se) aur GitHub Secrets mein save karni hogi
3. `.aab` file milegi jo Play Console pe upload hoti hai

Ye agla step hai jab app poori tarah test ho jaye.

## Agar build fail ho

Capacitor + Android SDK + Gradle ka combination pehli baar theek se chalna guarantee nahi hai — agar error aaye, **Actions tab ka poora error log screenshot bhej dena**, exact wajah dekh ke fix kar denge (jaise humne Pages workflow ke saath kiya tha).
