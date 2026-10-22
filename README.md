# Sanjaya MDM Sample

A minimal **Device Owner** MDM app, built after SanjayaCast, to show what being Device Owner unlocks. It blocks
the **camera** and the **microphone**, and can be provisioned by **QR code** (or `afw#`) at the setup wizard.

SanjayaCast is a platform-signed system app but not the panel's Device Owner, so on some boards (the SKG 75 Pro)
Android ignores its camera block and it needs vendor workarounds. A Device Owner sets the block through
`DevicePolicyManager.addUserRestriction`, which Android enforces itself on every board. Settings also shows the
blocked controls greyed out as "Blocked by your admin" instead of hiding them.

| Control | Device Owner call | Read-back |
|---|---|---|
| Camera | `addUserRestriction(DISALLOW_CAMERA)` + `setCameraDisabled(true)` | `getCameraDisabled(null)` |
| Microphone | `addUserRestriction(DISALLOW_UNMUTE_MICROPHONE)` | `AudioManager.isMicrophoneMute` |

## Build

```bash
export JAVA_HOME=~/Library/Java/JavaVirtualMachines/jbr-21.0.11/Contents/Home
./gradlew :app:assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest
```

### Release build (for provisioning)

Provisioning needs a signed release APK. Create the keystore **once** and keep it forever — a Device Owner app
can only ever be updated with the same key:

```bash
keytool -genkeypair -v -keystore release.keystore -alias mdmsample \
  -keyalg RSA -keysize 4096 -validity 10950
cp keystore.properties.sample keystore.properties   # fill in the passwords (gitignored)
./gradlew :app:assembleRelease      # app/build/outputs/apk/release/app-release.apk
```

## Provision by QR code (no adb)

Works on GMS/EDLA panels with a camera, at the setup wizard only — a fresh panel or after a factory reset.

1. Host `app-release.apk` at a stable HTTPS URL the panels can reach.
2. Generate the QR:

   ```bash
   keytool -exportcert -rfc -alias mdmsample -keystore release.keystore -file mdmsample.pem
   ./tools/make_qr.py \
     --apk app/build/outputs/apk/release/app-release.apk \
     --cert mdmsample.pem \
     --url https://your-host/app-release.apk \
     --ssid factory-wifi --wifi-password secret \
     --out qr.png
   ```

   The payload carries the APK URL, checksums (so Android verifies the download), optional Wi-Fi, and an admin
   extras bundle. Extras default to blocking camera and mic; `--allow-camera` / `--allow-mic` leave one open.
3. On the panel's welcome screen, **tap the same spot 6 times** → the QR scanner opens → scan.
4. Android downloads the APK, verifies it, installs the app as Device Owner, and calls
   `ProvisioningActivity`, which applies the blocks before setup finishes.

No camera on the panel? At the setup wizard's Google sign-in screen type `afw#com.teachmint.mdmsample`
instead — same provisioning path, but the app must be published on Google Play (a private listing is enough)
and, since no extras can be passed, it blocks everything by default.

## Provision from adb (factory line / test panels)

Android allows this only on a panel with **no accounts and a single user**, so in practice a freshly
factory-reset panel that skipped Google sign-in.

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell dpm set-device-owner com.teachmint.mdmsample/.MdmAdminReceiver
```

If it says "already has accounts" or "several users", remove them (Settings > Accounts, Settings > System > Users)
or reset the panel.

## Use it

On the panel, open **Sanjaya MDM Sample** and switch **Block camera** / **Block microphone**. Or from adb:

```bash
R="am broadcast -a com.teachmint.mdmsample.CONTROL -n com.teachmint.mdmsample/.AdbControlReceiver"
adb shell $R --es sensor camera --ez blocked true
adb shell $R --es sensor mic --ez blocked true
adb shell $R --es action status
adb shell $R --es sensor camera --ez blocked false
```

To check the camera is really blocked, open any camera app: it should report the camera disabled by policy.

## Remove it

```bash
adb shell $R --es action release          # clears the blocks, then gives up Device Owner
adb uninstall com.teachmint.mdmsample
```

On a production (non-`testOnly`) build, `dpm remove-active-admin` no longer works — removal is the in-app/adb
release above, or a factory reset (after which QR/`afw#` provisioning restores management anyway).

## Not covered yet

- A fleet backend: policy changes after provisioning still need the UI or adb.
- Other owner-only controls: tethering greyed out, kiosk (lock task), silent installs, blocking uninstall.
