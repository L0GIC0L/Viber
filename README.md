# Artifacts

An Android app that lists your GitHub repositories, shows each one's Actions
artifacts, and downloads and installs the APKs inside them. Material You
(dynamic colour), edge-to-edge, and a side-by-side list/detail layout when a
foldable such as the Pixel Fold is open.

## Get the app

Every push builds the app in GitHub Actions. Open the latest **Build APK** run
under the Actions tab, download the `artifacts-app` artifact, unzip it and
install the APK. After that the app can update itself, since it lists this repo too.

## Set up

1. Create a token at <https://github.com/settings/personal-access-tokens/new>:
   - Fine-grained: *Repository access → All repositories*, permissions
     *Actions: Read* and *Metadata: Read*. (A fine-grained token covers one
     owner; pick the organisation as owner to see its repos.)
   - Or a classic token with the `repo` scope, which covers every repo you can reach.
2. Paste it into the app.

## Installing

Settings (gear icon) → *Install with*:

- **App Manager** (default): hands the APK to
  [App Manager](https://github.com/MuntashirAkon/AppManager). Split bundles
  (`.apks`, `.apkm`, `.xapk`) always go to App Manager.
- **System installer**: Android's own installer. Allow *Install unknown apps*
  for Artifacts when Android asks.
- **Ask every time**: Android's app picker.

## Stable signing (so updates install over the old app)

Without these secrets each CI build is signed with a throwaway key, and Android
refuses to install a new build over an old one. One-time setup on a computer
with a JDK:

```sh
keytool -genkeypair -keystore release.jks -alias artifacts \
  -keyalg RSA -keysize 2048 -validity 20000 -dname "CN=Artifacts"
base64 -w0 release.jks > release.jks.b64     # macOS: base64 -i release.jks
```

Then in the repo: *Settings → Secrets and variables → Actions → New repository secret*:

| Secret | Value |
| --- | --- |
| `SIGNING_KEYSTORE_BASE64` | contents of `release.jks.b64` |
| `SIGNING_STORE_PASSWORD` | the keystore password you chose |
| `SIGNING_KEY_ALIAS` | `artifacts` |
| `SIGNING_KEY_PASSWORD` | the key password (same as above unless you set another) |

Keep `release.jks` somewhere safe and out of git.

## Notes

- The token is kept in app-private storage; Android backup is turned off.
- `versionCode` is the Actions run number, so newer builds always upgrade.

## Build locally

Needs JDK 17 and the Android SDK (API 36): `./gradlew assembleRelease`.
