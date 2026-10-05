# Play Store kit – Expenses Manager

| File | What it's for |
|---|---|
| `store-listing.md` | App name, short and full description, category, contact. Copy into Play Console. |
| `app-content-answers.md` | Answers for every "App content" form (data safety, content rating, ads, audience…). |
| `privacy-policy.html` | The privacy policy. Must be hosted at a public URL (see below). |
| `graphics/icon-512.png` | 512 × 512 store icon. |
| `graphics/feature-graphic.png` | 1024 × 500 feature graphic. |
| `graphics/screenshots/` | 7 phone screenshots, 1080 × 2160. |
| `graphics/GenerateGraphics.java` | Redraws the icon and feature graphic: `java GenerateGraphics.java` in that folder. |

The upload file is `app/build/outputs/bundle/release/app-release.aab`. Rebuild it with
`./gradlew :app:bundleRelease` (needs `keystore.properties` and the `.jks` key on this computer).

## Hosting the privacy policy (pick one)
**Google Sites (easiest):** sites.google.com → Blank site → name it "Expenses Manager Privacy Policy" →
paste the text from `privacy-policy.html` (open it in a browser and copy) → Publish → use that URL.

**GitHub Pages:** create a *public* repository (e.g. `expenses-manager-privacy`), upload
`privacy-policy.html` renamed to `index.html`, then Settings → Pages → Deploy from branch `main`.
The URL is `https://<your-username>.github.io/expenses-manager-privacy/`.

## Before every update
1. Increase `versionCode` (and usually `versionName`) in `app/build.gradle.kts`.
2. Test the release build: `./gradlew :app:assembleStaging` and install `app-staging.apk`.
3. Build `bundleRelease` and upload. If the update changes what data the app handles, update the
   privacy policy and the data safety form first.
