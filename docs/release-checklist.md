# GemLeap — Release checklist

Ordered from "do now" to "day of launch." Items marked ⛔ are hard blockers.

## 0. Package id — DONE ✅
The `applicationId` and `namespace` are set to **`com.spore.gemleap`** (the Kotlin
source package was renamed to match). This is permanent after your first upload.

## 1. Generate your upload keystore (one time)
Run once, in the repo root (needs the JDK's keytool — it's on PATH with Android
Studio's JBR, or use a full path). Choose strong passwords and **store them in a
password manager** — losing this key means you can never update the app again.

```
keytool -genkey -v -keystore gemleap-release.jks -keyalg RSA -keysize 2048 \
        -validity 10000 -alias gemleap
```

Then:
1. Copy `keystore.properties.template` → `keystore.properties`.
2. Fill in the passwords / alias you just chose.
3. Both `gemleap-release.jks` and `keystore.properties` are git-ignored — keep
   backups somewhere safe and private (NOT in the repo).

> Also enable **Play App Signing** when you create the app (default, recommended):
> Google manages the final signing key; your keystore above is the *upload* key.

## 2. Build the signed release bundle (AAB)
Play wants an **.aab**, not an .apk. From Android Studio:
- **Build → Generate Signed Bundle / APK → Android App Bundle**, pick your
  keystore, choose `release`. Output: `app/build/outputs/bundle/release/app-release.aab`.

Or command line (once `keystore.properties` exists):
```
./gradlew bundleRelease
```

## 3. Graphic assets (required by Play)
| Asset | Spec | Notes |
|---|---|---|
| App icon | 512×512 PNG, 32-bit | We have an in-app adaptive icon; export a 512 version. |
| Feature graphic | 1024×500 PNG/JPG | Shown at top of listing. Board + "GemLeap". |
| Phone screenshots | 2–8, PNG/JPG, 16:9 or 9:16, min 320px | Capture on device. |
| Tablet screenshots | optional but recommended | 7" and 10". |

**Screenshot plan** (capture these 5 in-app):
1. Fresh board, full of gemstones (the hero shot).
2. Mid-game with a stone selected + amber destination rings.
3. The hint pulse ring active.
4. Daily challenge with the date label + streak.
5. The "Perfect!" / win banner.

I can also produce a polished feature graphic from the board SVG — just ask.

## 4. Store listing
All copy is in `docs/store-listing.md` — paste title, short & full description,
category, tags. Add support email + privacy policy URL.

## 5. Monetization setup in Play Console
- **In-app product:** id **`remove_ads`** (must match `AdIds.PRODUCT_REMOVE_ADS`),
  set price, **activate**.
- Swap **real AdMob ids** in `AndroidManifest.xml` + `monetization/AdIds.kt`, set
  `AdIds.USE_TEST = false`. (Keep test ids until you're actually publishing.)

## 6. Testing tracks
- **Internal testing** (up to 100 testers, no waiting period): use this to verify
  the real Remove Ads purchase and live ads fast.
- **Closed testing (⛔ personal accounts):** 20 testers opted in for **14
  continuous days** before production is unlocked. Start recruiting testers now —
  this is usually the longest part of the timeline.

## 7. Pre-launch
- Complete **content rating** questionnaire (Everyone/PEGI 3 expected).
- Complete **Data safety** form (see `docs/store-listing.md` — declare ads +
  advertising id + the IAP).
- Set pricing/countries (free with IAP).
- Roll out to production once closed testing requirement is satisfied.

## Account status (as of your last check)
- ⛔ Identity verification: in progress (Google reviewing — a few days).
- ⛔ Phone verification: complete when available.
- These gate app creation/publishing but not development or asset prep.
