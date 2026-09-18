# Google Drive Backup — Setup Guide

Google Drive se connect karne ke liye ek **Client ID** chahiye hota hai — ye Google khud deta hai, muft hai, aur sirf ek baar banana hai.

## Step 1 — Google Cloud project banao

1. **console.cloud.google.com** kholo (apne Google account se login)
2. Upar "Select a project" → **"New Project"**
3. Naam do (jaise "VVault"), **Create**

## Step 2 — Drive API on karo

1. Left menu → **"APIs & Services" → "Library"**
2. Search karo: **"Google Drive API"**
3. Usay tap karo → **"Enable"**

## Step 3 — OAuth consent screen banao

1. **"APIs & Services" → "OAuth consent screen"**
2. User type: **"External"** → Create
3. App name: "V Vault", apna email daal do jahan poocha jaye
4. Save/Continue karte jao (scopes wala step khali chhod sakte ho, agle step mein bata dunga agar zaroorat pari)
5. **"Test users"** section mein apna hi Google email add karo (zaroori hai — warna sign-in nahi hoga)

## Step 4 — Client ID banao

1. **"APIs & Services" → "Credentials"**
2. **"+ Create Credentials" → "OAuth client ID"**
3. Application type: **"Web application"**
4. Naam do (jo bhi)
5. **"Authorized JavaScript origins"** mein apni exact live URL daalo (bina trailing slash):
   `https://thindyuvraj141.github.io`
6. **Create**
7. Ek popup mein **Client ID** milega — kuch aisa dikhega:
   `123456789-abc123xyz.apps.googleusercontent.com`
8. **Ise copy kar lo**

## Step 5 — App mein daalo

1. V Vault kholo → **Settings → "Google Drive backup"**
2. Wahi Client ID paste karo → **Save**
3. **"Connect Google Drive"** tap karo
4. Google ka sign-in popup khulega — apna account choose karo
5. Ek warning aa sakti hai: **"Google hasn't verified this app"** — ye normal hai kyun ke ye app sirf aapke liye (test user) hai, kisi ne submit/verify nahi karaya
   - **"Advanced"** → **"Go to V Vault (unsafe)"** tap karo — ye "unsafe" sirf isliye keh raha hai kyun ke Google ne review nahi kiya, warna sab kuch normal hai
6. Permission screen pe **Allow** karo — ye sirf itni permission mangega ke "sirf V Vault ke banaye hue files dekh sake" (aap ki baaki Drive files nahi dikhengi is app ko)

Ho gaya — ab "Backup now to Drive" aur "Restore from Drive" kaam karenge.

## Zaroori limitations

- **Ye sirf normal browser (Chrome) mein kaam karega** — agar aapne Capacitor se native `.apk` bhi banaya hai, us wrapped app ke andar Google ka sign-in kaam nahi karega (Google security wajhon se "embedded browser" ke andar se sign-in block kar deta hai). Native app mein backup ke liye abhi ke liye local file backup (`.vault` file) hi use karo.
- Har kuch mahine baad dobara "Connect" karna par sakta hai (Google ka access token kuch der mein expire ho jata hai — bas dobara "Connect Google Drive" tap kar dena).
- "Test user" wali list mein sirf wahi Google accounts kaam karenge jo aap ne Step 3 mein add kiye — agar kisi aur account se try karo to error aayega. Zyada logon ke liye Google ki app-verification process se guzarna padega (alag, lamba process).
