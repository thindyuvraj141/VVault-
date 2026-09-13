# V Vault

A private, offline-first photo vault for the browser. No account, no cloud, no sign-up — your photos and password never leave your device.

![platform](https://img.shields.io/badge/platform-web%20%7C%20PWA-C6A15B) ![license](https://img.shields.io/badge/license-MIT-blue) ![status](https://img.shields.io/badge/status-active-brightgreen)

## Features

- **No login, just a password** — set a password and a security question on first launch; no email, no account.
- **Password reset without email** — answer your security question to reset a forgotten password, entirely offline.
- **Albums** — organize photos into custom albums.
- **Search** — find photos by caption or album name.
- **Captions** — add a note to any photo.
- **Encrypted backup & restore** — export your vault to a single `.vault` file, encrypted with your password (AES‑256‑GCM). Restore it on any device.
- **Privacy blur** — vault content blurs instantly when the tab/app loses focus, so nothing sensitive is visible in the app switcher or to someone glancing over your shoulder. *(This is a browser-level deterrent, not an OS-level screenshot block — see [Security notes](#security-notes) below.)*
- **Installable PWA** — add it to your phone's home screen and it opens and works fully offline, like a native app.
- **No dependencies, no build step** — plain HTML/CSS/JS. Clone it and open it.

## Getting started

### Run it locally

The app is fully static, but the service worker (offline support) and IndexedDB storage need the page served over `http://` or `https://` — opening `index.html` directly via `file://` will show the UI but skip offline caching.

```bash
# any static file server works, for example:
python3 -m http.server 8080
# or
npx serve .
```

Then open `http://localhost:8080` in your browser.

### Install it as an app

1. Open the hosted URL (e.g. via GitHub Pages) on your phone.
2. **iOS Safari:** Share → *Add to Home Screen*.
3. **Android Chrome:** menu (⋮) → *Add to Home screen* / *Install app*.

It'll launch full-screen with its own icon, and works offline after the first load.

### Deploy to GitHub Pages

1. Push this repo to GitHub.
2. Repo **Settings → Pages** → set source to the `main` branch, root folder.
3. Your app will be live at `https://<username>.github.io/<repo>/`.

## How it works

- **Storage:** everything (photos, albums, captions, and your hashed password) is stored in this browser's [IndexedDB](https://developer.mozilla.org/en-US/docs/Web/API/IndexedDB_API), scoped to the site's origin. Nothing is transmitted anywhere — there is no backend.
- **Password:** never stored in plain text. It's hashed with PBKDF2 (SHA-256, 150,000 iterations) and a random per-vault salt; only the hash and salt are saved.
- **Backup files:** a backup bundles your photos/albums/captions as JSON, then encrypts the whole thing with AES-256-GCM using a key derived (PBKDF2) from your vault password. The password is never written into the file itself.

## Security notes

This project is offline-first and has no server, which removes a lot of common risk — but be aware of its current limits:

- **Photos are not encrypted at rest** in IndexedDB. IndexedDB is sandboxed to this site's origin (other sites/tabs can't read it), but someone with direct access to the browser profile's local storage files could potentially recover images. Backups *are* encrypted; the live vault currently is not.
- **No OS-level screenshot blocking.** Browsers don't expose an API to block screenshots or screen recording — the "privacy blur" feature only hides content when the tab/window loses focus (e.g. switching apps). A determined user can still screenshot the unlocked vault.
- Both of the above are solvable in a **native app** (iOS/Android), where OS APIs exist for encrypted file storage and screen-capture protection. See [`docs/native-app-guide.md`](docs/native-app-guide.md) for the full plan to port this to a native app with those protections.

If you find a security issue, please open an issue (or, for anything sensitive, avoid posting exploit details publicly — see [Contributing](#contributing)).

## Project structure

```
.
├── index.html              # entire app (markup, styles, and logic)
├── manifest.json            # PWA manifest
├── sw.js                    # service worker (offline caching)
├── icon-192.png / icon-512.png / icon-180.png
└── docs/
    └── native-app-guide.md  # roadmap for a real iOS/Android version
```

## Tech stack

Vanilla HTML, CSS, and JavaScript. No frameworks, no bundler, no npm install. Uses native browser APIs only:

- `IndexedDB` for local storage
- `Web Crypto API` (`SubtleCrypto`) for password hashing and backup encryption
- `Service Worker` + Web App Manifest for offline/installable support

## Contributing

Contributions are welcome — bug fixes, new features (e.g. real at-rest photo encryption, drag-and-drop reordering, tagging), or documentation improvements. See [`CONTRIBUTING.md`](CONTRIBUTING.md).

## Roadmap ideas

- [ ] Encrypt photos at rest in IndexedDB (not just in backups)
- [ ] Drag-and-drop photo reordering
- [ ] Multi-select for bulk delete/move
- [ ] Native iOS/Android app (see [`docs/native-app-guide.md`](docs/native-app-guide.md))

## License

MIT — see [LICENSE](LICENSE).
