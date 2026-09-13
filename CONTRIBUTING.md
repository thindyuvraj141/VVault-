# Contributing to V Vault

Thanks for considering a contribution! This is a small, dependency-free project — the whole app lives in `index.html`, which keeps the barrier to contributing low.

## Before you start

- Check open issues to avoid duplicate work.
- For anything larger than a small fix (new screens, storage changes), open an issue first to discuss the approach.

## Development setup

No build step, no installs:

```bash
git clone https://github.com/<your-username>/vvault.git
cd vvault
python3 -m http.server 8080
```

Open `http://localhost:8080` and start editing `index.html`. Refresh to see changes.

## Guidelines

- **Keep it dependency-free** where reasonably possible — part of this project's value is that it's a single file anyone can read end-to-end.
- **Don't add analytics, telemetry, or any network calls.** This app's entire premise is that nothing leaves the device.
- **Match the existing visual style** — the color tokens and type scale are defined as CSS variables at the top of the `<style>` block.
- **Test the full flow** before submitting: setup → lock → forgot password → vault → albums/search/captions → backup/restore. A regression in the password/reset flow can lock someone out of their own photos.

## Reporting security issues

If you find something that could expose someone's password or photos, please avoid filing a fully public issue with exploit details. Open an issue describing the *category* of the problem and ask for a private channel to share specifics, or contact the maintainer directly if the repo lists one.

## Submitting changes

1. Fork the repo and create a branch from `main`.
2. Make your change.
3. Open a pull request describing what changed and why.
