# CarInfoAR -- Complete Documentation

> **Version:** 1.2.6 (versionCode 20)
> **Platform:** Android
> **Last Updated:** 2026-04-21
> **Package:** `com.carinfo.ar`

---

## Table of Contents

1. [Overview](#1-overview)
2. [Architecture](#2-architecture)
3. [Build & Setup](#3-build--setup)
4. [Permissions](#4-permissions)
5. [Navigation Flow](#5-navigation-flow)
6. [Vehicle APIs](#6-vehicle-apis)
7. [OCR & Plate Detection](#7-ocr--plate-detection)
8. [Motion Tracking (Optical Flow) — DEPRECATED](#8-motion-tracking-optical-flow--deprecated)
9. [Camera Configuration](#9-camera-configuration)
10. [UI Screens](#10-ui-screens)
11. [Vehicle Info Display System](#11-vehicle-info-display-system)
12. [Country Detection](#12-country-detection)
13. [Language System](#13-language-system)
14. [Sound System](#14-sound-system)
15. [Data Storage](#15-data-storage)
16. [Ad System](#16-ad-system)
16b. [Privacy Policy](#16b-privacy-policy)
16c. [Firebase (Crashlytics & Analytics)](#16c-firebase-crashlytics--analytics)
16d. [In-App Purchase (Remove Ads)](#16d-in-app-purchase-remove-ads)
17. [Theme & Design](#17-theme--design)
18. [Debugging](#18-debugging)
19. [Testing Checklist](#19-testing-checklist)
20. [Known Issues & Limitations](#20-known-issues--limitations)
21. [Future Roadmap](#21-future-roadmap)
22. [Play Store Listing](#22-play-store-listing)
23. [Google Play Console](#23-google-play-console)

---

## 1. Overview

### What CarInfoAR Does

CarInfoAR is an Android augmented reality application that uses the device camera to scan vehicle license plates in real time. Once a plate is detected via on-device OCR, the app queries government vehicle-registration APIs to retrieve information about the vehicle (manufacturer, model, year, color, fuel type, and more). The results are displayed as info cards in a scrollable list at the bottom of the camera screen.

### Target Audience

- Car enthusiasts and buyers who want to quickly look up vehicle details.
- Mechanics and inspectors performing pre-purchase checks.
- Parking enforcement and fleet management personnel.
- General users curious about vehicles they encounter.

### Supported Countries

| Country       | API Source            | Plate Format Examples | Cost |
|---------------|-----------------------|-----------------------|------|
| Israel        | data.gov.il           | 1234567, 12345678     | Free |
| Netherlands   | RDW Open Data         | 3XKH01                | Free |
| United Kingdom| DVLA VES              | AB12CDE               | Free |

The app auto-detects the user's country from the SIM card or network operator and loads the corresponding API and OCR rules. Additional countries can be added by implementing a new API client and plate-regex pair.

---

## 2. Architecture

### Package Structure

```
com.carinfo.ar/
|-- MainActivity.kt              # Single-activity entry point, locale handling
|-- ui/
|   |-- navigation/
|   |   |-- AppNavigation.kt     # NavHost, route definitions, transitions
|   |   |-- Screen.kt            # Sealed class of route constants
|   |-- CameraScreen.kt          # Live preview, overlays, manual input, save animation
|   |-- FloatingCarInfo.kt       # Glass-morphism info card with Save/Info buttons
|   |-- LoadingPlateIndicator.kt # Pulsing plate-number animation
|   |-- InfoRow.kt               # Reusable label-value row component
|   |-- screens/
|   |   |-- SplashScreen.kt      # Animated splash with car icon & scan line
|   |   |-- OnboardingScreen.kt  # 3-page pager (Welcome, Country, Ready)
|   |   |-- SettingsScreen.kt    # Language, sound, history, premium, about, region
|   |   |-- HistoryScreen.kt     # Saved scans list, swipe-to-delete, expandable details
|   |-- components/
|   |   |-- ScanEffect.kt        # Camera scan line animation overlay
|   |   |-- ViewfinderOverlay.kt  # Camera viewfinder frame
|   |-- theme/
|       |-- Theme.kt             # Material 3 dark theme definition
|       |-- Color.kt             # Brand and country-accent colors
|       |-- Type.kt              # Typography scale
|-- data/
|   |-- api/
|   |   |-- IsraelApiService.kt  # Retrofit interface for data.gov.il
|   |   |-- NetherlandsApiService.kt  # Retrofit interface for RDW
|   |   |-- UkApiService.kt      # Retrofit interface for DVLA VES
|   |   |-- RetrofitClient.kt    # Shared Retrofit instances + trust-all client for data.gov.il
|   |   |-- DvlaRequest.kt       # UK DVLA request body model
|   |-- model/
|   |   |-- VehicleInfo.kt       # Unified vehicle data class
|   |-- ScanHistory.kt           # JSON file read/write + ScanRecord data class
|   |-- ScanRecord (in ScanHistory.kt) # History item data class
|   |-- VehicleCache.kt          # In-memory HashMap with synchronized + Mutex guard
|   |-- SupportedCountry.kt      # Enum: ISRAEL, NETHERLANDS, UK with plate regex
|   |-- UserPreferences.kt       # DataStore preferences helper
|-- camera/
|   |-- PlateAnalyzer.kt         # ImageAnalysis.Analyzer, ML Kit OCR + per-country fixes
|-- analytics/
|   |-- AnalyticsManager.kt      # Centralized Firebase Analytics event tracking
|-- ads/
|   |-- AdManager.kt             # Banner + interstitial ads, detection counter
|   |-- BillingManager.kt        # Google Play Billing for remove-ads IAP
|-- util/
|   |-- PriceEstimator.kt        # Offline multi-factor market-value estimate (age curve per country, ownership chain, mileage, brand tier, IL Korean bonus, etc.)
|   |-- SoundManager.kt          # MediaPlayer + ToneGenerator fallback + Vibrator
```

### Design Patterns

| Pattern         | Where Used                         | Purpose                                                  |
|-----------------|------------------------------------|----------------------------------------------------------|
| MVVM-lite       | CameraScreen state hoisting        | UI state is hoisted into composable scope; no ViewModel class but state is separated from UI rendering. |
| Repository      | VehicleRepository, HistoryRepository | Abstracts data sources (network, file) behind a clean interface. |
| Singleton       | VehicleCache, SoundManager, AdManager, BillingManager, AnalyticsManager | Single shared instance across the app lifetime. |
| Observer/Flow   | DataStore preferences, BillingManager.adsRemoved | Reactive preference reads via Kotlin Flow / StateFlow. |
| Strategy        | PlateAnalyzer per-country OCR fixes | Different OCR correction logic selected at runtime based on the active country. |

---

## 3. Build & Setup

### Prerequisites

| Requirement       | Version / Details                              |
|-------------------|------------------------------------------------|
| Android Studio    | Ladybug or newer (2024.2+)                     |
| JDK               | 11 (bundled JBR recommended)                   |
| Android SDK       | Compile SDK 35, Min SDK 24, Target SDK 35      |
| Gradle            | Wrapper included in project                    |
| Physical device   | Recommended (camera required for core feature) |

### Clone & Build

```bash
# Clone the repository
git clone <repo-url> CarInfoAR
cd CarInfoAR

# Set JAVA_HOME to the bundled JBR (Windows / Git Bash)
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"

# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

### Install on Device

```bash
# Install debug APK via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Dependencies (from `libs.versions.toml`)

| Dependency                | Version     | Purpose                          |
|---------------------------|-------------|----------------------------------|
| Compose BOM               | 2025.02.00  | Jetpack Compose UI toolkit       |
| CameraX                   | 1.4.1       | Camera preview & image analysis  |
| ML Kit Text Recognition   | 16.0.1      | On-device OCR (Latin script)     |
| Retrofit                  | 2.11.0      | HTTP client for vehicle APIs     |
| Navigation Compose        | 2.8.5       | In-app navigation                |
| DataStore Preferences     | 1.1.2       | Persistent key-value storage     |
| Play Services Ads         | 23.6.0      | AdMob integration (active, production IDs) |
| Play Billing              | 7.1.1       | In-app purchase (remove ads)     |
| Desugar JDK Libs          | 2.1.4       | java.time backport for API 24-25 |

### Custom App Icon

- Custom launcher icon provided at all standard densities: **mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi**.
- Play Store icon: **512x512** (`ic_launcher_playstore.png`).
- Adaptive icon XML has been removed; the app uses **PNG directly** for the launcher icon.

### Camera Screen Top Toolbar (v1.2.5+)

The top HUD has **3 visible controls**: a primary "Scan Options" expandable pill, a History pill, and a circular Settings icon button. Image and Manual input moved into the Scan Options dropdown menu.

Layout:
- **Column** with `Row` (pills) + `Box` (expandable menu anchored below).
- **Row:** `fillMaxWidth()` + `Arrangement.SpaceBetween` — Scan Options pill on start, History in middle, Settings gear on end.
- **Outside-tap dismissal:** an invisible full-screen `Box` with `pointerInput { detectTapGestures { showScanOptions = false } }` is declared **before** the toolbar so menu and pill taps capture first and empty-space taps fall through to the scrim.

Controls:
- **ScanOptionsPillButton** — labeled pill with `Icons.AutoAwesome` + chevron that rotates 180° when expanded. Localized via `toolbar_scan_options` ("More scan options") in all 14 languages.
- **ToolbarPillButton** (History) — same glass-morphism pill styling as before. Still pulses 1.0→1.4→1.0 when a "Saved!" flying-badge lands on it.
- **ToolbarIconButton** (Settings) — circular icon-only glass button (no label), frees horizontal space for the primary Scan Options pill.

Pill styling: `GlassOverlay` background, `RoundedCornerShape(100.dp)`, 0.5dp white-14%-alpha border, 9dp padding, 16dp icon, 4dp gap, 13sp SemiBold white text, `maxLines=1` + Ellipsis.

**ScanOptionsMenu** (expandable dropdown below the pills):
- Two items: **Pick Image** (`Icons.PhotoLibrary`) and **Manual Input** (`Icons.Edit`).
- Enter animation: `fadeIn + expandVertically(spring)`; exit: `fadeOut + shrinkVertically(spring)`.
- Aligned to `Alignment.Start` — layout-direction-aware so LTR menu appears on the left, RTL on the right, under the Scan Options pill.

**Scan hint:** the "Scan a license plate" hint renders each word on its **own line** (32sp ExtraBold, 38sp lineHeight) for a more vertical, prominent look.

**Dynamic save-target position:** the flying-save animation captures the History pill's center via `onGloballyPositioned` instead of hardcoded coordinates — correct for any translation length and RTL/LTR.

### Per-Card Dismiss & Exit Animation (v1.2.4+)

The global "Reset" button was removed. Each overlay (info card, loading indicator, not-found indicator) has its own small X dismiss button in its header.

Tap flow:
1. User taps X → plate added to `exitingPlates: SnapshotStateList<String>`
2. `LaunchedEffect(shouldExit)` flips the card's `MutableTransitionState.targetState` to `false`
3. `AnimatedVisibility` plays exit: `fadeOut(200ms) + scaleOut(target=0.6, origin=center, 260ms) + shrinkVertically(top, 280ms) + slideOutHorizontally(±width/4, 280ms)`
4. When `visibleState.currentState == false && targetState == false`, the state is removed from `overlayStates` and `plateExactCounts`
5. LazyColumn's `Modifier.animateItem(placementSpec=tween(320))` smoothly reflows remaining cards upward

Enter animation for new cards: `fadeIn(220ms) + scaleIn(initialScale=0.92f, 260ms) + slideInVertically(fromBottom=1/6, 280ms)`.

### Release Signing

| Property       | Value                                              |
|----------------|----------------------------------------------------|
| Keystore       | `~/.keystores/carinfo-release.keystore`            |
| Key Alias      | `carinfo`                                          |
| NOT in repo    | `*.keystore` in `.gitignore`                       |
| Backup         | Google Drive + local `~/.keystores/`               |

**CRITICAL:** If the keystore is lost, the app can never be updated on Play Store. Always back up to multiple locations. Enable Google Play App Signing as a safety net.

### ProGuard / R8 Rules (`app/proguard-rules.pro`)

Release builds use R8 minification (`isMinifyEnabled = true`). Key rules:

| Rule                                                   | Purpose                                              |
|--------------------------------------------------------|------------------------------------------------------|
| `-keep class com.carinfo.ar.data.model.** { *; }`     | Keep API response model field names for Gson          |
| `-keepclassmembers class com.carinfo.ar.data.ScanRecord { *; }` | Keep ScanRecord fields for JSON serialization |
| `-keep class com.carinfo.ar.data.api.** { *; }`       | Keep Retrofit API interface and request/response classes |
| `-keep class * extends com.google.gson.reflect.TypeToken { *; }` | Preserve Gson TypeToken subclasses (general safety) |
| `-keepattributes SourceFile,LineNumberTable`           | Readable Firebase Crashlytics stack traces            |
| `-keep class com.android.billingclient.** { *; }`     | Play Billing classes                                  |
| `-keep class com.google.firebase.crashlytics.** { *; }` | Firebase Crashlytics                                |

**Important:** `ScanHistory` uses `Array<ScanRecord>::class.java` (not `TypeToken`) for Gson deserialization specifically because R8 strips generic type info from `TypeToken` at minification time, causing silent deserialization failures in release builds.

### SDK Configuration

```
minSdk = 24        // Android 7.0 Nougat
targetSdk = 35     // Android 15
compileSdk = 35
```

---

## 4. Permissions

### Declared Permissions

| Permission           | Type      | Purpose                                        |
|----------------------|-----------|-------------------------------------------------|
| `CAMERA`             | Dangerous | Live camera preview and plate scanning          |
| `INTERNET`           | Normal    | Vehicle API calls                               |
| `VIBRATE`            | Normal    | Haptic feedback on plate detection              |
| `ACCESS_NETWORK_STATE` | Normal  | Network connectivity check for AdMob            |

### Network Security Configuration

The app uses a custom `network_security_config.xml` (referenced in `AndroidManifest.xml`):

- **Cleartext traffic disabled** (`cleartextTrafficPermitted="false"`) — all network communication must use HTTPS.
- **System certificates only** — the network security config trusts system certificates.

### data.gov.il SSL Handling

The Israeli government API (`data.gov.il`) uses an SSL certificate from SSL.com/Entrust that is **not trusted** by Android 8.x and older devices. Instead of bundling a certificate file (which would expire and need app updates), the app uses a **trust-all OkHttpClient scoped exclusively to `data.gov.il`**:

- **Hostname verification** ensures the trust-all policy only applies to `data.gov.il` — all other domains use standard certificate verification.
- **No expiration risk** — nothing to renew or update.
- **Acceptable security tradeoff** — `data.gov.il` serves public vehicle registration data (not sensitive/financial).
- **Other APIs** (RDW Netherlands, DVLA UK) use the standard secure `OkHttpClient` with full certificate verification.

### Permissions NOT Required

- **ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION:** The app does **not** use GPS. Country detection relies on `TelephonyManager` (network country ISO and SIM country ISO), which does not require location permission.
- **READ_EXTERNAL_STORAGE / WRITE_EXTERNAL_STORAGE:** History is stored in the app's internal `filesDir`.

### Runtime Permission Flow

1. On first launch, the camera permission is requested when the user navigates to the Camera screen.
2. If denied, a rationale dialog explains why the camera is needed.
3. If permanently denied, the user is directed to app settings.

---

## 5. Navigation Flow

### Route Definitions

```
SPLASH  -->  ONBOARDING  -->  CAMERA  -->  SETTINGS
                                       -->  HISTORY
```

| Route        | Composable         | Entry Condition                                  |
|--------------|--------------------|--------------------------------------------------|
| `splash`     | SplashScreen       | App launch; always shown first                   |
| `onboarding` | OnboardingScreen   | Shown if `onboarding_complete` is false AND country auto-detection fails |
| `camera`     | CameraScreen       | Main screen; entered after splash or onboarding  |
| `settings`   | SettingsScreen     | Accessible from camera toolbar                   |
| `history`    | HistoryScreen      | Accessible from camera toolbar                   |

### Auto-Detection Shortcut

If the app successfully detects the user's country via `TelephonyManager` on first launch, it marks onboarding as complete and navigates directly from Splash to Camera, skipping the onboarding flow entirely.

### Screen Transitions

All screen transitions use a combined **slide + fade** animation with a duration of **300 milliseconds**:

- **Enter:** Slide in from the right + fade in.
- **Exit:** Slide out to the left + fade out.
- **Pop enter:** Slide in from the left + fade in.
- **Pop exit:** Slide out to the right + fade out.

---

## 6. Vehicle APIs

### 6.1 Israel (data.gov.il) — 8 Resources

| Property       | Value                                                              |
|----------------|--------------------------------------------------------------------|
| Base URL       | `https://data.gov.il/`                                             |
| Endpoint       | `GET /api/3/action/datastore_search`                               |
| Authentication | None                                                               |
| Cost           | Free, unlimited                                                    |

The app queries **8 data.gov.il resources** for each Israeli plate. The main resource is fetched first (to get join keys), then 6 secondary resources are fetched **in parallel**.

**Resource IDs:**

| # | Resource | ID | Join Key | Data |
|---|----------|----|----------|------|
| 1 | Main registration | `053cea08-09bc-40ec-8f7a-156f0677aff3` | mispar_rechev | Basic vehicle info |
| 2 | WLTP Specs | `142afde2-6228-49f9-8a29-9b6c3a0cbe40` | tozeret_cd+degem_cd+shnat_yitzur | HP, displacement, safety systems, ADAS, body type, doors, seats, weight |
| 3 | Vehicle History | `56063a99-8a3e-4ff4-912e-5966c0279bad` | mispar_rechev | Engine number, odometer (km), color/tire/structure changes |
| 4 | Ownership History | `bb2355dc-9ec7-4f06-9c3f-3344672171da` | mispar_rechev | Ownership dates and types |
| 5 | Disabled Tag | `c8b9f9c8-4612-4068-934f-d4acd2e3c06e` | MISPAR RECHEV | Disabled parking permit |
| 6 | Summary (tow hook) | `0866573c-40cd-4ca8-91d2-9dd2d7a492e5` | mispar_rechev | Tow hook, tire codes |
| 7 | Importer & Price | `39f455bf-6db0-4926-859d-017f34eacbcb` | tozeret_cd+degem_cd+shnat_yitzur | Importer name, price |
| 8 | Statistics | `5e87a7a1-2f6f-41c1-8aec-7216d52a6cf6` | tozeret_cd+degem_cd+shnat_yitzur | Active vehicle count |

**Fetch Flow:**
1. Fetch main record by `mispar_rechev` → get `tozeret_cd`, `degem_cd`, `shnat_yitzur`
2. Fetch 6 secondary resources **in parallel** (each with independent error handling)
3. Merge all data into `VehicleInfo` via `.copy()`

**WLTP/Importer/Stats cache:** Cached by model key (`"P_735_789_2024"`) since all vehicles of same model share specs.

**All Israel Fields (40+):**
- Basic: manufacturer, model, year, color, fuel, trim, ownership, test dates, engine model, chassis, tires, on-road date, emission group
- WLTP: horsepower, engine displacement, drive type, drive technology, standard type, transmission, body type, doors, seats, weight, country of origin, green index, licensing group, safety score
- Equipment: sunroof, alloy wheels, electric windows, tire pressure sensors, reverse camera
- Safety: airbag count, ABS, stability control, lane departure, distance monitoring, adaptive cruise, pedestrian detection, blind spot detection
- History: engine number, odometer km, LPG added, color changed, tires changed, originality
- Tow hook, towing capacity (with/without brakes)
- Importer name, price at registration
- Ownership history (date + type for each owner)
- Disabled parking tag (yes/no)
- Active vehicles count (same model)
- Model code, manufacturer code, registration directive

---

### 6.2 Netherlands (RDW Open Data) — 3 Resources

| Property       | Value                                          |
|----------------|-------------------------------------------------|
| Base URL       | `https://opendata.rdw.nl/`                      |
| Authentication | None                                            |
| Cost           | Free, unlimited                                 |

The app queries **3 RDW resources**. Main is fetched first, then fuel + recalls in parallel.

**Endpoints:**

| # | Resource | Endpoint | Data |
|---|----------|----------|------|
| 1 | Main registration | `GET /resource/m9d7-ebf2.json?kenteken={PLATE}` | Basic + dimensions + odometer + recall indicator |
| 2 | Fuel/Emissions | `GET /resource/8ys7-d773.json?kenteken={PLATE}` | Power (kW), CO2, fuel consumption, Euro class |
| 3 | Recall Status | `GET /resource/t49b-isb7.json?kenteken={PLATE}` | Open recall details |

**All Netherlands Fields (35+):**
- Basic: brand, model, color, secondary color, first registration, fuel type, engine capacity, cylinders, doors, seats, APK expiry, catalog price, weight, empty mass, body type, insurance, wheelbase
- Extended: dimensions (L x W x H), BPM tax, owner registration date, open recall indicator, odometer judgment, odometer year, fuel efficiency class (A-G), export indicator, taxi indicator, towing capacity (braked/unbraked), EU category
- Fuel: CO2 emissions, engine power (kW), fuel consumption (city/highway/combined), Euro emission class
- Recalls: recall reference code, status

**OCR Fallback:** O/0 and I/1 character swap retry on empty result.

---

### 6.3 United Kingdom (DVLA VES)

| Property       | Value                                                               |
|----------------|----------------------------------------------------------------------|
| Base URL       | `https://driver-vehicle-licensing.api.gov.uk/`                       |
| Endpoint       | `POST /vehicle-enquiry/v1/vehicles`                                  |
| Authentication | API key via `x-api-key` header                                       |
| Rate Limit     | Standard DVLA limits                                                 |
| Cost           | Free                                                                 |
| Registration   | https://developer-portal.driver-vehicle-licensing.api.gov.uk/        |

**Request Headers:**

| Header          | Value                     |
|-----------------|---------------------------|
| `x-api-key`     | Your DVLA API key         |
| `Content-Type`  | `application/json`        |

**Request Body:**

```json
{
  "registrationNumber": "PLATE_NUMBER"
}
```

**Response Fields:**

| API Field         | Description              | Maps To         |
|-------------------|--------------------------|-----------------|
| `make`            | Manufacturer             | `manufacturer`  |
| `colour`          | Vehicle color            | `color`         |
| `yearOfManufacture`| Year of manufacture     | `year`          |
| `fuelType`        | Fuel type                | `fuelType`      |
| `engineCapacity`  | Engine capacity (cc)     | `engineCapacity` |
| `taxStatus`       | Tax status               | `taxStatus`     |
| `motStatus`       | MOT status               | `motStatus`     |
| `co2Emissions`    | CO2 emissions (g/km)     | `co2Emissions`  |
| `motExpiryDate`   | MOT expiry date          | `testValidUntil`|
| `wheelplan`       | Body/wheel plan          | `bodyType`      |
| `monthOfFirstRegistration` | First registration | `onRoadDate`   |
| `taxDueDate`      | Tax due date             | `taxDueDate`    |
| `markedForExport` | Export status             | `markedForExport` |
| `dateOfLastV5CIssued` | V5C certificate date | `v5cDate`       |
| `typeApproval`    | Type approval code       | `typeApproval`  |

**Important Limitation:** The DVLA API does **not** return the vehicle model name. Only the manufacturer (make) is available.

**Example Request:**

```bash
curl -X POST \
  "https://driver-vehicle-licensing.api.gov.uk/vehicle-enquiry/v1/vehicles" \
  -H "x-api-key: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"registrationNumber":"MT09NKS"}'
```

---

## 7. OCR & Plate Detection

### ML Kit Configuration

- **Library:** Google ML Kit Text Recognition (Latin script), version 16.0.1.
- **Processing:** On-device only; no cloud calls.
- **Input:** YUV frames from CameraX `ImageAnalysis` pipeline.

### Plate Regex by Country

| Country      | Regex Pattern                      | Description                              |
|--------------|------------------------------------|------------------------------------------|
| Israel       | `^\d{7,8}$`                        | 7 or 8 digits only                       |
| Netherlands  | `^[A-Z0-9]{6}$`                   | Exactly 6 uppercase alphanumeric chars   |
| UK           | `^[A-Z]{2}\d{2}[A-Z]{3}$`         | Current format (e.g., AB12CDE); additional patterns handle older formats |

### Text Cleaning Pipeline

Before regex matching, all recognized text blocks undergo the following cleaning steps:

1. Convert to uppercase.
2. Remove all dashes (`-`).
3. Remove all spaces.
4. Remove all dots (`.`).
5. Remove all commas (`,`).
6. Remove all colons (`:`).
7. Remove all semicolons (`;`).
8. Remove all quotes (`'`, `"`).
9. Remove all pipes (`|`).
10. Remove middle dots (`·`).
11. Trim whitespace.

### OCR Correction Algorithms

#### Israel

Israeli plates are purely numeric. Common OCR misreads include:

| Misread Character | Corrected To |
|-------------------|-------------|
| `O` (letter O)   | `0` (zero)  |
| `I` (letter I)   | `1` (one)   |
| `l` (lowercase L)| `1` (one)   |
| `B`              | `8`         |
| `S`              | `5`         |
| `Z`              | `2`         |
| `G`              | `6`         |

All non-digit characters are stripped after correction.

#### Netherlands

Dutch plates mix letters and digits. The correction strategy:

| Misread Character | Corrected To            |
|-------------------|------------------------|
| `O` (letter O)    | `0` (zero) -- context-dependent |
| `0` (zero)        | `O` (letter O) -- context-dependent |
| `I` (letter I)    | `1` (one) -- context-dependent |
| `1` (one)         | `I` (letter I) -- context-dependent |

If the first API call returns no results, the app swaps `O`/`0` and `I`/`1` characters and retries the query once as a fallback.

#### United Kingdom

UK plates have a known structure (two letters, two digits, three letters in the current format). The corrector uses positional awareness:

| Position       | Misread | Corrected To |
|----------------|---------|-------------|
| Letter position| `0`     | `O`         |
| Letter position| `1`     | `I`         |
| Letter position| `5`     | `S`         |
| Letter position| `8`     | `B`         |
| Digit position | `O`     | `0`         |
| Digit position | `I`     | `1`         |
| Digit position | `S`     | `5`         |
| Digit position | `B`     | `8`         |

### Debounce & Exact-Match Counting

Plate detection uses an **exact-match counting** system (no fuzzy grouping):

1. **Exact counting:** Each OCR reading is counted separately by its exact string value. `21262602` and `82262602` are tracked independently. This prevents OCR misreads (e.g., `B`→`8` at first digit) from contaminating the correct plate number.
2. **Threshold:** A minimum of **3 identical readings** of the exact same plate string is required before triggering an API call. Plates already in `VehicleCache` (previously looked up in this session) bypass this threshold.
3. **Duplicate prevention:** Before adding a new overlay, the system checks if a similar plate (within 2-char tolerance via `isSimilarPlate`) already exists in the overlay list. This prevents duplicate cards when OCR eventually reads a variant that differs slightly from the already-displayed plate.

This prevents:

- **Wrong vehicle identification** from OCR misreads that happen to match a different real plate number.
- False positives from partial reads.
- Redundant API calls from flickering detections.
- Duplicate cards from OCR reading slightly different variants of the same plate.
- Processing of non-plate text that momentarily matches the regex.

The exact-match counters are cleared when the user presses the Reset button.

### Dynamic Country Provider

The `PlateAnalyzer` receives a `countryProvider: () -> Country` lambda that is evaluated on every frame. This allows the active country to change in real time (for example, if the user changes the region in settings while the camera is active) without restarting the analyzer.

---

## 8. Motion Tracking (Optical Flow) — DEPRECATED

> **Status:** The optical flow motion tracking system has been **replaced** with a fixed-position card layout. The `FrameMotionTracker` class still exists in the codebase but is no longer used by `CameraScreen`.

### Why Deprecated

After extensive testing on a Redmi 15C (budget device), the AR overlay approach was abandoned because:

1. **Gyro-based stabilization** failed — the device lacks a raw gyroscope (only GAME_ROTATION_VECTOR available).
2. **Optical flow** was too aggressive — overlays moved too fast with camera movement.
3. **OCR-only positioning** made overlays appear stuck to the screen.

### Current Approach

Vehicle info cards are now displayed in a **scrollable list** at the bottom of the screen (bottom 45%), not anchored to plate positions. This provides a stable, readable display regardless of camera movement. See [Section 11](#11-ar-overlay-system) for details.

---

## 9. Camera Configuration

### CameraX Setup

| Setting              | Value                           |
|----------------------|---------------------------------|
| Use Cases            | Preview + ImageAnalysis         |
| Lifecycle Binding    | Activity lifecycle              |
| Preferred Resolution | 1280 x 720                     |
| Backpressure Strategy| `STRATEGY_KEEP_ONLY_LATEST`     |
| Lens Facing          | Back camera                     |

### Backpressure

The `KEEP_ONLY_LATEST` strategy ensures that if the analyzer is still processing the previous frame when a new frame arrives, the old queued frame is dropped and replaced with the newest one. This prevents frame queue buildup and keeps the analysis pipeline responsive.

### Zoom Controls

| Gesture          | Behavior                                      |
|------------------|------------------------------------------------|
| Pinch            | Continuous zoom via `ScaleGestureDetector`     |
| Double-tap       | Toggle between 1x and 2x zoom via `GestureDetector` |

Both gesture detectors are attached to the camera preview surface. Zoom state is applied to the CameraX `Camera` control interface.

---

## 10. UI Screens

### 10.1 SplashScreen

| Property      | Value                                              |
|---------------|----------------------------------------------------|
| Duration      | 2.2 seconds                                        |
| Elements      | Animated car icon, app title "CarInfoAR", horizontal scan line animation |
| Transition    | After timer completes, navigates to Onboarding or Camera |

The car icon fades in and scales up. A horizontal scan line sweeps across the icon to suggest scanning. The app title appears below with a subtle fade-in.

### 10.2 OnboardingScreen

Three horizontally-paged screens with dot indicators at the bottom:

| Page | Title     | Content                                             |
|------|-----------|-----------------------------------------------------|
| 1    | Welcome   | App introduction, what CarInfoAR does               |
| 2    | Country   | Country selection (Israel, Netherlands, UK)          |
| 3    | Ready     | Confirmation, "Get Started" button                  |

- Dot indicators show current page position.
- Swiping or tapping "Next" advances pages.
- "Get Started" on the final page writes `onboarding_complete = true` and `selected_country` to DataStore, then navigates to Camera.

### 10.3 CameraScreen

The primary screen of the app. Components:

| Element             | Description                                                |
|---------------------|------------------------------------------------------------|
| Live camera preview | Full-screen CameraX preview surface                        |
| Viewfinder overlay  | Semi-transparent frame guiding where to point the camera   |
| AR overlay cards    | `FloatingCarInfo`, `LoadingPlateIndicator`, or `PlateNotFoundIndicator` positioned at detected plate locations |
| Top toolbar         | Reset button (red, visible only when overlays exist), Manual input button (pencil icon), History button (clock icon), and Settings button (gear icon) |
| Manual input FAB    | Opens a dialog for typing a plate number manually          |
| Reset button        | Clears all current overlays                                |

**Manual Input Dialog:** Allows the user to type a plate number directly. The app runs the same API lookup and displays results as if the plate had been scanned. Useful for plates that are difficult to scan (dirty, damaged, or at an awkward angle).

### 10.4 SettingsScreen

Settings are displayed in this order:

| #  | Setting         | Type           | Details                                              |
|----|-----------------|----------------|------------------------------------------------------|
| 1  | Language        | Dropdown       | 15 options (14 languages + device default)           |
| 2  | Sound & Feedback| Toggle switch  | Enables/disables scan sounds and vibration           |
| 3  | History         | Navigation row | Opens the HistoryScreen (chevron indicator)          |
| 4  | Remove Ads      | Purchase card  | Shows price + Buy button (hidden if already purchased) |
| 5  | About           | Info section   | App version 1.2.1, credits                           |
| 6  | Region          | Selector       | Country override; placed **last** in the list        |

**Region hint text:** "Select the country you are currently in, not your home country."

**Note:** The country flag is shown only in Settings (Region selector). It has been removed from the Camera screen.

Changing the language triggers `Activity.recreate()` to apply the new locale immediately.

### 10.5 HistoryScreen

| Feature            | Implementation                                        |
|--------------------|-------------------------------------------------------|
| List display       | `LazyColumn` with scan records                        |
| Per-item display   | Plate number (header), manufacturer, model, year, color, fuel type, country flag, trim level, test status |
| Expandable details | Tap on item to expand/collapse showing all vehicle fields |
| Info button        | "מידע" text button next to color/fuel opens Google search for vehicle |
| Date expired highlighting | Expired test/tax dates shown in red              |
| Swipe-to-delete    | Swipe **left only** to reveal delete action           |
| Per-item delete    | Explicit delete button/icon on each row               |
| Clear all          | **Text button** at top; shows a confirmation dialog before clearing |
| Sound effects      | Sound plays on single-item delete and clear-all actions |

History is capped at 100 records. Newest records appear first.

---

## 11. Vehicle Info Display System

### Layout

Vehicle info cards are displayed in a **scrollable `LazyColumn`** anchored to the **bottom 45%** of the camera screen. This replaces the earlier AR-anchored overlay approach, which was unstable on budget devices.

Cards are sorted by **most recently detected** (newest first). The list scrolls vertically if there are more cards than fit on screen.

### PlateOverlayState

```
data class PlateOverlayState(
    val plateNumber: String,
    val screenX: Float = 0f,     // Legacy, not used for positioning
    val screenY: Float = 0f,     // Legacy, not used for positioning
    val vehicleInfo: VehicleInfo?,
    val isLoading: Boolean,
    val lastSeenTime: Long
)
```

Each detected plate gets its own `PlateOverlayState` instance. The state tracks loading status and the fetched vehicle info.

### Card Components

| Component                 | Shown When                        | Description                              |
|---------------------------|-----------------------------------|------------------------------------------|
| `FloatingCarInfo`         | Vehicle info loaded successfully  | Glass-morphism card showing manufacturer, model, year, color, fuel type, country flag. Includes Save and Info buttons. |
| `LoadingPlateIndicator`   | API call in progress              | Spinning progress indicator + pulsing plate number text. |
| `PlateNotFoundIndicator`  | API returned no results           | Red-colored "not found" text. Auto-removed after 3 seconds. |

### FloatingCarInfo Card

The glass-morphism card displays:

**Header (banner):**
- Country flag (20sp) + ♿ disabled-tag chip (if present) + Manufacturer + Model (18sp title)
- Year + Trim Level (13sp subtitle, in brand color)
- Save + Close pills on the right
- `disabledTag == true` renders a small orange-tinted ♿ chip next to the flag so the user sees it at a glance without scrolling.

**After the accent line (section order — identical in overlay and history):**

Every section uses the same `SectionHeader`/`SectionDivider` helpers (exported from `CarInfoOverlay.kt`), so the overlay and `HistoryScreen` expanded view are visually identical.

1. **Test / Validity** (`label_section_test`) — rendered as a two-column `Row`:
   - **Start column** (right in RTL): motStatus, testValidUntil, lastTestDate, `lastTestKm` *(moved up from Internal Details; row is hidden when value is 0 or null — some records return 0 from data.gov.il which would otherwise render as "0 km" and look broken)*.
   - **End column** (left in RTL): `CompactEstimateCard` — ₪ mid price (18sp ExtraBold), low–high range, confidence dot.
2. **Ownership history** (`label_ownership_history`) — table of ownership dates and types (IL).
3. **Price** — highlighted card with ₪ price at registration (IL, `priceAtRegistration`).
4. **Disabled tag** — highlighted card with ♿ emoji, orange if present (IL).
5. **Basic info** (`label_section_basic`) — color, secondaryColor, fuelType, ownership, bodyType, onRoadDate, ownerRegistrationDate, countryOfOrigin, importerName, euCategory, isTaxi, isExported, plateNumber, trimLevel.
6. **Engine** (`label_section_engine`) — horsepower, enginePowerKw, engineDisplacement, engineCapacity, engineModel, numCylinders, co2Emissions, euroEmissionClass, emissionGroup, greenIndex, fuelEfficiencyClass.
7. **Fuel consumption** (`label_section_fuel`) — combined, city, highway (l/100km) (NL).
8. **Specs** (`label_section_specs`) — driveType, driveTechnology, transmission, standardType, numDoors, numSeats, weight, emptyMass, dimensions (L×W×H), wheelbase, catalogPrice, purchaseTax (BPM), licensingGroup.
9. **Odometer** (`label_section_odometer`) — odometerJudgment, odometerYear (NL).
10. **Recall** (`label_section_recall`) — highlighted card, red if open recall ⚠️, green if none ✅ (NL).
11. **Equipment** (`label_section_equipment`) — electricWindows, sunroof, alloyWheels, tirePressureSensors, reverseCamera (IL).
12. **Safety systems** (`label_section_safety`) — airbagCount, ABS, stabilityControl, laneDeparture, forwardDistanceMonitoring, adaptiveCruise, pedestrianDetection, blindSpotDetection, safetyScore (IL).
13. **Tax** (`label_section_tax`) — taxStatus, taxDueDate (UK, expired in red).
14. **UK extra** — v5cDate, typeApproval, markedForExport.
15. **Towing** (`label_section_towing`) — towHook, towingWithBrakes, towingWithoutBrakes, maxTowingBraked, maxTowingUnbraked.
16. **Tires** (`label_section_tires`) — frontTires, rearTires (IL).
17. **Internal details** (`label_section_internal`) — chassisNumber, engineNumber, lpgAdded, colorChanged, tiresChanged, originality, modelCode, registrationDirective (IL). `lastTestKm` is **not** here — it lives with Test/Validity.
18. **Statistics** (`label_section_statistics`) — activeVehiclesCount (IL).
19. **Insurance** (`label_section_insurance`) — insured status (NL).
20. **Data source** — government data source URL.

### Estimated Market Value (PriceEstimator v3d)

**v3d refinements (Apr 2026)** — 5 targeted structural fixes on top of v3c after
deeper outlier analysis. MAD dropped from 12.20% (v3c) to **11.74%** on the full
80-car LY set, and from 10.76% to **9.73%** on the 45-car no-km subset (first time
under 10% — the threshold where LY's own internal noise dominates). Held-out MAD
improved from 15.43% to **13.70%**. ±10% coverage: 50% → 54%. ±20%: 89% → 91%.

Changes vs v3c:
1. **Mid-reliable split by body** — Sentra Y5.9 sedan was +24% overestimate under
   flat Mid-reliable Y5+ 1.08, because Nissan's SUVs (X-Trail/Qashqai) hold value
   but compact sedans don't. Sedans/hatches now get 1.02, SUVs keep 1.08.
2. **Premium-reliable non-SUV Y13+ stronger fade** — Y13-15 was 1.05, now 1.00;
   Y15+ was 1.00, now 0.95 (non-SUV only). Mazda 3 Y13-15 was +24% over; Accord
   Y17 sedan was +23%. Y13+ SUV still gets 1.15 (Forester 4WD demand).
3. **Japanese-SUV Y15+ retention floor** — post-multiply check: if IL + Japanese
   Premium-reliable + SUV + Y≥15 and combined factor < 0.14, floor at 0.14.
   Forester Y17 (Subaru, SUV) was -28% under because raw 0.089 × 1.15 × 1.05 still
   capped at ~10% retention. LY values Forester Y17 at 23% retention.
4. **Suzuki subtier by model** — flat 1.05 was wrong for cheap subcompacts.
   CELERIO Y9.9 was +27% over. Split: budget (SWIFT/CELERIO/ALTO/SPLASH/IGNIS)
   gets 1.00; solid (JIMNY/SX4/VITARA/S-CROSS/BALENO) keeps 1.05.
5. **Jeep/Chrysler-old tier** — previously hit Standard 1.00. COMPASS Y14 was +18%.
   Now Y10+ Jeep/Chrysler/Dodge gets 0.85 (American mid-size SUVs depreciate
   harder than Japanese past Y10).
6. **Crossover-model SUV classification** — data.gov.il classifies several
   crossovers as MPV/sedan/hatchback. QASHQAI PLUS 2 is labeled "MPV" — pre-fix
   it was caught by Mid-reliable sedan tier (1.02 vs 1.08 SUV), regressing -6.5pp
   vs v3c. Fix: explicit crossover model list (QASHQAI, TIGUAN, KODIAQ, KUGA,
   OUTLANDER, …) treated as SUV regardless of bodyType. MAD 10.02% → **9.89%**
   on 45-car no-km subset.

### Estimated Market Value (PriceEstimator v3c — earlier iteration)

**v3c refinements (Apr 2026)** — data-driven targeted fixes after validation on 80 cars
revealed systematic biases. MAD dropped from 13.15% (v3b) to **11.82%**. Mean bias dropped
from +5.16% to **+0.52%** (near-zero). ±10% coverage went from 42% → 52%.

Changes vs v3b:
1. **Commercial-Van double-boost guard** — old diesel vans were getting both `fuel × 1.20`
   (commercial-diesel-Y8+) AND `brand × 1.15` (Commercial tier), stacking to +38%. Berlingo
   Y13.9 was +25% overestimate. Fixed: Commercial diesel Y8+ fuel factor lowered 1.20 → 1.00
   (brand tier already handles retention).
2. **Premium-Lux Y10+ new tier → 0.70** (was 0.85 for Y5+). Volvo XC60 Y11.9 dropped from
   +26% to ~+9%. European luxury past Y10 depreciates sharply in IL due to maintenance cost.
3. **Premium-Lux Y<3 tightened 1.00 → 0.92**. Audi Q5 Y2 was +18%, Tesla Model Y Y1 was +17%.
   Young premium/EV luxury drops fast in the first few years.
4. **Premium-reliable Y13-15 fade → 1.05** (was 1.10). Mazda 3 Y13-15 was consistently +24-31%
   overestimate. Non-Toyota Premium-reliable brands don't hold as strongly past Y13.
5. **Japanese-SUV-Y13+ boost → 1.15**. Subaru Forester Y16.9 was -36% underestimate. Old
   Japanese SUVs with 4WD retain disproportionate value. Rule: Premium-reliable + SUV body
   + Y≥13 → 1.15 (overrides the Y15+ = 1.00 fade).
6. **Tesla added to Premium-Lux tier**. Tesla Y1 was +17% overestimate — EV depreciation
   is well-documented; tier gives the same young-luxury treatment.
7. **Conditional scrap floor**: ₪10,000 for Y13+ with catalog ≥ ₪100k (was flat ₪8k).
   Hyundai i30 Y17.9 on ₪111k catalog was hitting the ₪8k floor but LY valued it at ₪11.3k.

### Estimated Market Value (PriceEstimator v3 — earlier iteration)

`util/PriceEstimator.kt` produces an **offline** multi-factor estimate of the car's current private-sale value. No network call, no scraping — uses only fields already fetched from data.gov.il / RDW / DVLA.

**Output:** `Estimate(low, mid, high, currency, confidence)` rounded to clean numbers (₪100 / €500 / £1000). Rendered by `CompactEstimateCard` inline with the Test section (mid price + low–high + colored confidence dot: green ≥0.85, amber ≥0.65, orange else).

**v3 calibration (Apr 2026)** — retuned against combined **55 Levi-Yitzhak** pricings (43 from earlier v2 round + 12 new cars scanned Apr 22 that exposed v2's weaknesses). MAD dropped from the previously-deployed **27.0% (stale Kotlin curve) → 13.1%** (v3) — a 52% improvement. ±20% coverage jumped from 47% → 89%.

**Why v3 happened:** the reference Python `calibrate_v2.py` had been retuned against 43 LY cars but the matching edits **were never ported to Kotlin** — so the app was running an older pre-v2 curve. v3 is both the port and a fresh retune against 55 cars. Going forward, keep Python and Kotlin in sync by running `python ../CarInfoARData/tools/tune_v3.py` after any estimator change and expecting MAD ≤ 14%. Calibration tooling lives in `C:\Users\ASUS\Desktop\CarInfoARData\tools\` — outside the Android project folder so the repo stays pure Android/docs.

**Factors** (multiplied together — chain, not sum):

| # | Factor | v3 logic |
|---|--------|-------|
| 1 | Age curve (IL) | `Y0→0.92, Y1→0.85, Y5→0.622, Y10→0.249, Y15→0.110`. Piecewise: `0.925^yr` Y1-5, `0.83^yr` Y5-10, **`0.85^yr` Y10+** (softened from v2's 0.83 — LY values old cars a bit higher). |
| 2 | Hybrid badge | Detect HSD/HEV/HYBRID/PHEV/SELF-CHARGING in **model name** (data.gov.il lists Toyota HSD / Hyundai HEV as `fuelType="בנזין"`). **IL boost staged**: ×1.10 Y0-3, **×1.15 Y3+** (Y3+ compounds with Premium-reliable / Korean). |
| 3 | EV fuel modifier | IL: ×1.00 Y0-1 (no penalty — Chinese EVs priced competitively), ×0.93 Y1-3, ×0.96 Y3+. PHEV: ×1.02/0.98/1.00. |
| 4 | Diesel | Staged: ×0.95 Y<4, ×0.88 Y4-7, ×0.80 Y7+. **Commercial diesel vans** (Berlingo/Vito/Caddy/Transit/…) get ×1.20 past Y8 — they stay useful as work tools. |
| 5 | Ownership usage | Private = 1.00. Lease/rental ×0.90, company ×0.92, government/driving-school ×0.80, taxi ×0.72. Chain-stacked with next penalty ×0.5, third ×0.25. |
| 6 | Hand count | **v3 excludes "סוחר" (dealer) entries** — dealers are middlemen, not owners. `[פרטי,סוחר,פרטי,סוחר,פרטי]` = hand 3, not hand 5. Penalties: h2 −3%, h3 −6%, h4 −9%, h5 −11%, h6+ −13%. |
| 7 | Mileage vs expected | Baseline 15,000 km/yr (IL/UK), 13,000 (NL). Slope −1%/10k km. **v3 caps negative at −5%** (was −12%) — LY barely penalizes high-km cars (Grand Coupe 227k km Y9 still ₪47k; Kodiaq 242k km Y8 still ₪79k). Positive cap +8%. |
| 8 | Body type | SUV/crossover +5%, sedan 1.0, hatchback −2%, MPV/van −6%, coupe/cabrio −3%. |
| 9 | **Brand tier** (11 tiers, prefix match — see below) | First-match wins. |
| 10 | Trim | Top (luxury/premium/inspire/supreme/…) +3%. Base (expression/pop/essential/…) −3%. |
| 11 | Safety score | 7–8 +2%, 1–3 −4%. |
| 12 | Open recall (NL) | −6%. |
| 13 | Test / MOT expired | −6%. |
| 14 | Emissions | **Guarded** — IL `greenIndex` only if 1–15; NL A/B +1–2%, F/G −5%; UK CO2 <100 +2%, ≥200 −5%. |
| 15 | Originality / color / tires | `לא מקורי` −15%, `colorChanged` −8%, `tiresChanged` −2%. |
| 16 | LPG / Parallel import / Taxi / Exported | −12% / −3% / −30% / −15%. |
| 17 | IL scrap floor | `max(mid, ₪8,000)`. |

**Brand tiers (v3)** — matched by **prefix** against `manufacturer` (after `.substringBefore(' ')`). Prefix match is critical: `"טורקיה"` (Turkey) ends with the chars of `"קיה"` (Kia), so substring match would mis-classify Renault Turkey as Korean.

| Tier | Brands | Factor |
|------|--------|--------|
| Performance-Lux | BMW M/M850, AMG, Audi RS — detected from **model name** | 0.95 <Y3 / 0.82 Y3-5 / **0.70 Y5+** |
| Commercial | Berlingo/Vito/Caddy/Transit/Sprinter/… — by **model** | 1.00 <Y5 / **1.15 Y5+** |
| Chinese-IL | BYD, Chery, Geely, MG, Jaecoo, Zeekr, ג'אקו, מ.ג, … | **1.00/0.95/0.88/0.78** by Y bracket (Y0-1/1-3/3-5/5+) |
| Premium-reliable | Toyota, Lexus, Honda, Mazda (**both מזדה and מאזדה**), Subaru | **1.00 <Y2, 1.10 Y2+** |
| Suzuki-solid | Suzuki | 1.00 <Y2, 1.05 Y2+ |
| Korean-IL | Hyundai, Kia, Genesis | **1.10** (bumped from 1.06) |
| Premium-Lux | BMW (**ב.מ.וו and ב מ וו and BMW**), Mercedes, Audi, Porsche, Volvo, Land/Range Rover, Jaguar | 1.00 <Y3 / 0.92 Y3-5 / 0.85 Y5+ |
| Weak-resale | Fiat, Alfa, Renault, Citroen, Peugeot, Dacia, Lancia | **0.92 Y≤10, 0.78 Y>10** (v2 was flat 0.92) |
| **Old-generic** (new in v3) | Chevrolet, Opel, Daewoo, Holden | 1.00 Y≤10, **0.70 Y>10**. Cruze/Sonic/Corsa Y11+ sell at ₪13-16k off ₪100-125k catalog. |
| **Mid-reliable** (new in v3) | VW, Skoda, Ford, Mitsubishi, Nissan | 1.02 |
| Standard | (fallback) | 1.00 |

**Confidence** starts at 0.5 and adds 0.1–0.2 per data field present (base price, km, ownership, history, onRoadDate, bodyType). Spread = 12% + (1 − confidence) × 8%.

**Debug logging** (DEBUG builds only, tag `PriceEstimator`): logs every factor + combined + mid/low/high + confidence. `adb logcat -s PriceEstimator:*`.

**v3 metrics (n=55 LY-priced cars):** MAD 13.1% · median delta +4.4% · mean +5.0% · ±10% coverage 45% · ±20% coverage **89%**. Remaining outliers are small-sample edge cases (Subaru Forester Y16 +43%, Renault Grand Coupe 227k km +80%).

**Known weaknesses:**
1. Very old reliable Japanese SUVs (Forester Y16) hold value better than the curve predicts — the Y15+ floor catches too aggressively.
2. Extremely high-km cars (>220k km) still under-estimated because LY essentially ignores km while we apply −5% cap.
3. Niche luxury (BMW M850, Audi Q5 at specific configurations) sometimes misses by 15-25% because sample too small to tune per-model.

**Drift prevention:** `../CarInfoARData/tools/tune_v3.py` is the canonical reference. After ANY change to `PriceEstimator.kt` run:
```
python ../CarInfoARData/tools/tune_v3.py
```
Expect the three tiers ("ALL / old / new") to all show MAD ≤ 14% and ±20% coverage ≥ 85%. If metrics regress, either revert or re-tune in Python first.

**Calibration artifacts** (all in `C:\Users\ASUS\Desktop\CarInfoARData\` — kept outside the Android repo to keep it pure Android/docs):
- `calibration/price_calibration_v3_final.xlsx` — all 55 LY cars with Kotlin-old vs v3 side-by-side, factor breakdown, delta.
- `tools/tune_v3.py` · `tune_v4.py` · `tune_v5.py` · `tune_v6.py` · `tune_v3b.py` — iterative Python tunings that led to v3b.
- `tools/calibration_data.json` — 48 original LY+Yad2 reference cars.
- `scan_history/` — 3 snapshots of the user's `scan_history.json` pulled from the phone.
- `levi_yitzhak_screenshots/` — 25 screenshots of LY prices (Apr-22 hold-out batch) — source for `apply_levi_and_analyze.py` LY_PRICES dict.
- `calibration/price_calibration_check_v2_with_levieitshak.xlsx` — user-filled LY prices for the 12 new cars.

`ScanHistory.toVehicleInfo()` extension lets `HistoryScreen` recompute the estimate from a stored `ScanRecord` — the estimate re-ages every time you open the record, so old saves show current-year prices.

### Save Behavior

**Camera scan:** Vehicle detections are **NOT** auto-saved. The user must tap the **Save button** on the info card to save to history. This triggers a flying animation from the save button to the history button, a sound effect, and a toast confirmation.

**Manual input:** When the user types a plate number via the manual input dialog and the API returns data, the result **IS** auto-saved to history immediately (no manual save step needed).

### Save Animation (Flying Badge)

When the user taps Save on a camera-scanned card, a "Saved!" badge animates from the save button position to the history button in the top toolbar. The animation uses:

- **`Modifier.layout`** with `placeable.place(rawPixelX, pixelY)` for absolute pixel positioning that works correctly in both LTR and RTL layouts.
- **700ms tween** for the flight path (linear interpolation from start to end position).
- **Scale:** Shrinks from 1.2x to 0.4x during flight.
- **Alpha:** Fades from 1.0 to 0.7 during flight.
- **RTL support:** The history button end position is computed differently for RTL vs LTR layouts using `LocalLayoutDirection.current`.

### History Button Pulse Effect

When the save animation completes, the history button in the top toolbar pulses:
- Scales up to **1.4x** over 150ms, then back to **1.0x** over 200ms.
- Uses a separate `Animatable` (`historyPulseScale`) applied via `Modifier.scale()`.

### Stale Overlay Removal

- Cards with **vehicle info loaded** persist until the user presses Reset.
- Cards that are **loading or not found** are removed after **5 seconds** if the plate is not re-detected.

### Reset

The Reset button (top-left, red) clears all cards and vote groups. It is only visible when there are active cards on screen.

### Scan Hint

Two display modes, both driven by the same `rememberInfiniteTransition` blink (alpha 1.0 ↔ 0.4 over 800ms):

- **No cards visible:** big centered text, each word on its own line (32sp ExtraBold). Localized via `camera_scan_plate_title`.
- **At least one card visible:** compact pill with a BrandPrimary border, anchored `Alignment.TopCenter` with 70dp top padding (sits just under the toolbar, above the cards). 13sp SemiBold, single line. Tells the user they can keep scanning even when a result is on screen.

---

## 12. Country Detection

### Detection Priority

The app determines the user's country using the following priority chain (no permissions required):

| Priority | Source                      | API                                          |
|----------|-----------------------------|----------------------------------------------|
| 1        | Network country ISO         | `TelephonyManager.getNetworkCountryIso()`    |
| 2        | SIM country ISO             | `TelephonyManager.getSimCountryIso()`        |
| 3        | Device locale               | `Locale.getDefault().country`                |

### Fallback Mapping

If none of the above yield a supported country code directly, the following fallbacks apply:

| Locale / Language Code | Maps To      |
|------------------------|-------------|
| `he` (Hebrew)          | Israel      |
| `iw` (legacy Hebrew)  | Israel      |
| `nl` (Dutch)          | Netherlands |
| `GB` (country code)   | UK          |

### No Permission Required

`TelephonyManager.getNetworkCountryIso()` and `getSimCountryIso()` do not require `READ_PHONE_STATE` or any location permission. They return the Mobile Country Code (MCC) derived ISO country code, which is publicly available information.

### Independence from Language

Language and Country/API are **separate, independent settings**:

- **Language** follows the device language by default, or can be manually overridden in Settings via the language dropdown.
- **Country/API** follows the detection priority: network country -> SIM country -> device locale -> manual override in Settings.

Changing one does not affect the other.

---

## 13. Language System

### Supported Languages (14)

| # | Language    | Code | Values Directory |
|---|-------------|------|-----------------|
| 1 | Hebrew      | `he` | `values-he`     |
| 2 | English     | `en` | `values` (default) |
| 3 | Dutch       | `nl` | `values-nl`     |
| 4 | French      | `fr` | `values-fr`     |
| 5 | German      | `de` | `values-de`     |
| 6 | Spanish     | `es` | `values-es`     |
| 7 | Italian     | `it` | `values-it`     |
| 8 | Portuguese  | `pt` | `values-pt`     |
| 9 | Arabic      | `ar` | `values-ar`     |
| 10| Turkish     | `tr` | `values-tr`     |
| 11| Russian     | `ru` | `values-ru`     |
| 12| Chinese     | `zh` | `values-zh`     |
| 13| Japanese    | `ja` | `values-ja`     |
| 14| Korean      | `ko` | `values-ko`     |

### Language Picker

The language selector is a **dropdown** in the Settings screen (not a tap-to-cycle control). It lists all 14 languages plus a "Device Default" option.

### Independence from Country/API

Language and Country/API are **independent settings**. Changing the display language does not affect which vehicle API is used, and changing the region/country does not affect the display language. For example, a user can set the language to French while querying the Israeli vehicle API.

### Implementation

- **Locale Override:** The app overrides the locale in `attachBaseContext()` of `MainActivity`. The selected language preference is read from DataStore, and a new `Configuration` with the desired `Locale` is applied via `createConfigurationContext()`.
- **Device Default:** If the user selects "Device Default" in settings, the override is removed and the system locale is used.
- **Activity Recreate:** When the user changes the language in settings, `Activity.recreate()` is called to reload all string resources with the new locale.
- **String Resources:** All user-facing text is stored in `res/values-XX/strings.xml` directories. No hardcoded strings in Kotlin code.

---

## 14. Sound System

### SoundManager

The `SoundManager` is a singleton that wraps `MediaPlayer` and `Vibrator` to provide audio and haptic feedback.

### Sound Source

The app uses the system's default **notification ringtone** (`RingtoneManager.TYPE_NOTIFICATION`) as the sound source. This ensures a familiar, non-intrusive sound that matches the user's device configuration.

### Events

| Event           | Sound | Vibration | Description                        |
|-----------------|-------|-----------|------------------------------------|
| Scan detected   | Yes   | 50ms      | Plate number recognized by OCR     |
| Info loaded     | Yes   | No        | Vehicle info returned from API     |
| Saved           | Yes   | No        | Scan saved to history              |
| Delete          | Yes   | No        | Single history item deleted        |
| Delete all      | Yes   | No        | All history cleared                |

### Vibration

A short **50-millisecond** haptic pulse is triggered when a plate is first detected. This provides immediate tactile feedback that the camera has recognized a plate.

### User Control

Sound and vibration are controlled by the `sound_enabled` preference in DataStore. When disabled, both audio and haptic feedback are suppressed. The toggle is available in the Settings screen.

### Implementation Detail

The app uses `MediaPlayer` with the system's default notification ringtone (via `RingtoneManager.TYPE_NOTIFICATION`) as the primary sound source. If `MediaPlayer.create()` returns `null` (which happens on some budget devices that lack a default notification sound), it falls back to `ToneGenerator` with `TONE_PROP_BEEP` (150ms duration, volume 80). The `ToneGenerator` is released after a 200ms delay via `Handler`. This two-tier fallback ensures sound works on all devices from API 24+.

The sound enabled/disabled state is synced from DataStore preferences via `LaunchedEffect` in `CameraScreen`.

---

## 15. Data Storage

### 15.1 DataStore Preferences

The app uses Jetpack DataStore (Preferences) for lightweight key-value storage.

| Key                  | Type    | Default   | Description                          |
|----------------------|---------|-----------|--------------------------------------|
| `onboarding_complete`| Boolean | `false`   | Whether onboarding has been completed |
| `selected_country`   | String  | `""`      | ISO code of selected country          |
| `dvla_api_key`       | String  | `""`      | DVLA API key for UK lookups           |
| `sound_enabled`      | Boolean | `true`    | Sound and vibration toggle            |
| `app_language`       | String  | `""`      | Language code override (empty = device default) |

### 15.2 Scan History (JSON)

| Property          | Value                                |
|-------------------|--------------------------------------|
| File location     | `context.filesDir/scan_history.json` |
| Format            | JSON array of scan records           |
| Max records       | 100                                  |
| Order             | Newest first                         |
| Deduplication     | By `plateNumber` (newer replaces older) |

**Gson Deserialization:** `ScanHistory.load()` uses `gson.fromJson(json, Array<ScanRecord>::class.java)` instead of `TypeToken<List<ScanRecord>>`. This avoids R8/ProGuard issues where `TypeToken` generic type information is stripped during minification, causing deserialization to fail silently in release builds.

**Operations:**

| Operation     | Description                                                   |
|---------------|---------------------------------------------------------------|
| Load          | Read and parse JSON file; uses `Array<ScanRecord>::class.java` for R8 compatibility |
| Save          | Add new record; if plate already exists, replace it; trim to 100 |
| Delete single | Remove one record by plate number                             |
| Clear all     | Delete all records (with user confirmation dialog)            |

### 15.3 Vehicle Cache

| Property       | Value                                    |
|----------------|------------------------------------------|
| Type           | `HashMap<String, VehicleInfo?>` (allows null values) |
| Thread safety  | `synchronized(cacheLock)` for reads/writes + `Mutex` for in-flight dedup |
| TTL            | None (cached for the entire app session) |
| Scope          | In-memory only; cleared on app restart   |

The cache key is the cleaned plate number. A `null` value indicates the plate was looked up but no vehicle was found, preventing repeated API calls for unknown plates.

---

## 16. Ad System

### Current State

AdMob is **active** with real production ad unit IDs.

### Configuration

| Property            | Value                                          |
|---------------------|------------------------------------------------|
| SDK                 | Google Play Services Ads 23.6.0                |
| App ID              | `ca-app-pub-6755700667333024~9262386386`       |
| Banner Ad Unit ID   | `ca-app-pub-6755700667333024/9070814697`       |
| Interstitial Ad Unit ID | `ca-app-pub-6755700667333024/6137529598`   |
| Banner placement    | Bottom of camera screen                        |
| Interstitial trigger| Every 3 unique plate detections (persisted across sessions) |
| Detection counter   | Persisted in `SharedPreferences("ad_prefs", "detection_count")` |
| Status              | Active with real production IDs                |

### Interstitial Ad Logic

The interstitial ad is shown every **3 unique plate detections** (successful API lookups only). The detection counter:

- Is **persisted** in `SharedPreferences("ad_prefs", "detection_count")` so it survives app restarts.
- Is restored in `AdManager.initialize()`.
- Is incremented in `AdManager.onNewPlateDetected()` (called from `CameraScreen` when a vehicle info result is loaded).
- Resets to 0 after showing an interstitial, and also resets to 0 when the user purchases "remove ads" (via `AdManager.resetCount()`).
- Premium users (`BillingManager.adsRemoved.value == true`) skip AdMob initialization entirely, and `onNewPlateDetected` returns immediately.

### How to Disable

Ads are currently **enabled** in production. To disable them:

1. **Comment out** the interstitial ad loading and showing code in `CameraScreen.kt`.
2. **Comment out** the banner ad composable in the camera screen layout.
3. **Replace** the production ad unit IDs with AdMob test IDs if you want to keep the code but prevent real ad serving during development.

---

## 16b. Privacy Policy

| Property       | Value                                              |
|----------------|----------------------------------------------------|
| URL            | https://carinfoar.com                              |
| Hosted via     | GitHub Pages from `docs/index.html`                |
| Required for   | Google Play Store submission                       |

Custom domain `carinfoar.com` purchased via Namecheap, pointed to GitHub Pages via CNAME record in `docs/CNAME`.

### Coverage

The privacy policy covers the following areas:

- **Camera usage:** How the app uses the device camera for plate scanning.
- **Plate data:** How scanned license plate numbers are processed.
- **Local storage:** How scan history is stored on-device.
- **AdMob:** Google AdMob advertising SDK and data collection.
- **Third-party APIs:** Data sent to government vehicle registration APIs (data.gov.il, RDW, DVLA).

---

## 16c. Firebase (Crashlytics & Analytics)

### Firebase Project

| Property          | Value                                                    |
|-------------------|----------------------------------------------------------|
| Project ID        | `carinfo-ar`                                             |
| Project Number    | `640581146754`                                           |
| App ID            | `1:640581146754:android:e50f473cdd170ca6dc3c9b`          |
| Analytics Property| `carinfo-ar` (Property ID: 529769932)                    |
| Analytics Stream  | `14212121707`                                            |
| Console URL       | https://console.firebase.google.com/project/carinfo-ar   |
| Plan              | Spark (free)                                             |

### Configuration Files

| File                    | Location                          | Purpose                              |
|-------------------------|-----------------------------------|--------------------------------------|
| `google-services.json`  | `app/google-services.json`        | Firebase SDK configuration           |
| `firebase.json`         | Project root                      | Firebase CLI configuration           |
| `.firebaserc`           | Project root                      | Firebase project alias mapping       |

### Crashlytics

| Property              | Value                                              |
|-----------------------|----------------------------------------------------|
| SDK                   | `firebase-crashlytics-ktx` (via Firebase BOM 33.9.0) |
| Gradle Plugin         | `com.google.firebase.crashlytics` v3.0.3           |
| Mapping File Upload   | Automatic on release builds (`uploadCrashlyticsMappingFileRelease`) |
| ProGuard Rules        | `keepattributes SourceFile,LineNumberTable` for readable stack traces |
| Initialization        | Automatic — no code needed, SDK initializes via `ContentProvider` |

Crashlytics automatically reports:
- **Fatal crashes** (unhandled exceptions)
- **ANRs** (Application Not Responding)
- **Non-fatal exceptions** (if logged manually via `FirebaseCrashlytics.getInstance().recordException()`)

### Analytics

| Property              | Value                                              |
|-----------------------|----------------------------------------------------|
| SDK                   | `firebase-analytics-ktx` (via Firebase BOM 33.9.0) |
| Account               | Default Account for Firebase                       |
| Initialization        | Automatic — no code needed                         |

Analytics automatically tracks:
- **Sessions** (app opens, session duration)
- **Screen views** (which screens users visit)
- **User demographics** (country, device, OS version)
- **App version** distribution
- **Retention** metrics

### GA4 MCP Server (Claude Code Integration)

A GA4 MCP server is configured in Claude Code settings to query Analytics data directly:

| Property | Value |
|----------|-------|
| MCP Server | `google-analytics-mcp` (PyPI) |
| Service Account | `ga4-mcp-reader@carinfo-ar.iam.gserviceaccount.com` |
| Key File | `C:\Users\ASUS\.config\ga4-mcp\service-account.json` |
| Property ID | `529769932` |
| Role | Viewer on GA4 property |

This allows Claude Code to answer analytics questions directly (users by version, engagement time, countries, etc.) without browser screenshots.

### Custom Analytics Events (AnalyticsManager)

All custom events are sent via `AnalyticsManager` (`analytics/AnalyticsManager.kt`), a singleton initialized in `MainActivity`. Event categories:

| Category           | Events                                                                                     |
|--------------------|--------------------------------------------------------------------------------------------|
| App Lifecycle      | `app_opened`, `app_backgrounded`, `app_resumed`                                           |
| Onboarding         | `onboarding_started`, `onboarding_country_selected`, `onboarding_completed`                |
| Camera / Scanning  | `camera_opened`, `camera_permission_granted`, `camera_permission_denied`, `plate_detected_ocr`, `plate_accepted`, `vehicle_info_loaded`, `vehicle_not_found`, `api_error`, `scan_reset` |
| Zoom               | `pinch_zoom_used`, `double_tap_zoom_used`                                                  |
| Manual Input       | `manual_input_opened`, `manual_input_searched`, `manual_input_cancelled`                   |
| History            | `history_opened`, `history_saved_camera`, `history_item_expanded`, `history_item_collapsed`, `history_item_deleted`, `history_cleared_all`, `history_info_clicked` |
| Settings           | `settings_opened`, `language_changed`, `country_changed`, `sound_toggled`                  |
| Ads                | `banner_ad_loaded`, `banner_ad_failed`, `interstitial_shown`, `interstitial_dismissed`, `interstitial_failed` |
| Premium / Billing  | `remove_ads_clicked`, `purchase_started`, `purchase_completed`, `purchase_failed`, `purchase_restored` |
| Model Info         | `model_info_clicked`                                                                       |
| Country Detection  | `country_auto_detected`, `country_detection_failed`                                        |
| Errors             | `ssl_error`, `crash_recovered`                                                             |

### Gradle Plugins

Both plugins are declared in `libs.versions.toml` and applied in `app/build.gradle.kts`:

```
# Project-level build.gradle.kts
alias(libs.plugins.google.services) apply false
alias(libs.plugins.firebase.crashlytics.plugin) apply false

# App-level build.gradle.kts
alias(libs.plugins.google.services)
alias(libs.plugins.firebase.crashlytics.plugin)
```

### Dependencies

```kotlin
// Firebase BOM manages all Firebase library versions
implementation(platform(libs.firebase.bom))    // 33.9.0
implementation(libs.firebase.analytics)         // version from BOM
implementation(libs.firebase.crashlytics)       // version from BOM
```

---

## 16d. In-App Purchase (Remove Ads)

### Overview

The app offers a one-time in-app purchase to permanently remove all ads (banner + interstitial). This is implemented via Google Play Billing Library.

### Configuration

| Property              | Value                                              |
|-----------------------|----------------------------------------------------|
| Library               | Google Play Billing 7.1.1 (`billing-ktx`)          |
| Product ID            | `remove_ads`                                       |
| Product Type          | `INAPP` (one-time, not subscription)               |
| Manager               | `BillingManager.kt` (singleton)                    |
| State                 | `adsRemoved: StateFlow<Boolean>`                   |
| Local Cache           | `SharedPreferences("purchases", "remove_ads_purchased")` |

### Pricing (set in Play Console)

| Country      | Price    |
|--------------|----------|
| Israel       | 24.90 ILS |
| Netherlands  | 6.99 EUR  |
| UK           | 4.99 GBP  |

### Purchase Flow

1. User taps "Buy" in Settings screen
2. `BillingManager.launchPurchase(activity)` checks client is ready and product details loaded
3. Google Play purchase dialog appears
4. On success: `handlePurchase` → `acknowledgePurchase` (required within 3 days) → `grantRemoveAds`
5. `_adsRemoved.value = true` → banner disappears, interstitials stop
6. `AdManager.resetCount()` clears the detection counter

### Security & Edge Cases

| Scenario                        | Handling                                                    |
|---------------------------------|-------------------------------------------------------------|
| App restart                     | `SharedPreferences` read on init (instant, no flicker)      |
| Refund via Google Play          | `restorePurchases` finds no valid purchase → `revokeRemoveAds` → ads resume |
| Billing connection failure      | Exponential backoff retry (3 attempts, 1s/2s/4s delays)    |
| Product details not loaded      | Toast "Loading price, please wait..." + retry query         |
| Store not available             | Toast "Store not available, please try again" + reconnect   |
| Pending payment (bank transfer) | Toast "Purchase pending" + `_purchasePending` state         |
| Acknowledge failure             | Retry up to 3 times with 3s delay                          |
| Activity destroyed mid-purchase | `restorePurchases` on next init recovers the purchase       |
| Premium user — AdManager init   | `AdManager.initialize` skips AdMob if `adsRemoved` is true  |
| Premium user — interstitial     | `onNewPlateDetected` returns immediately if `adsRemoved`     |
| Premium user — banner           | `CameraScreen` hides banner via `if (!adsRemoved)` check    |

### Integration Points

- **MainActivity.kt**: `BillingManager.initialize(this)` in `onCreate`, `BillingManager.disconnect()` in `onDestroy`
- **AdManager.kt**: Checks `BillingManager.adsRemoved.value` before init and before each interstitial trigger
- **CameraScreen.kt**: Collects `adsRemoved` StateFlow, conditionally renders banner
- **SettingsScreen.kt**: Shows "Remove Ads" section with price and Buy button (hidden if already purchased)

### Play Console Setup Required

To activate the purchase, create an in-app product in Play Console:
1. Go to Monetize → In-app products
2. Create product with ID `remove_ads`
3. Set price for each country
4. Activate the product

---

## 17. Theme & Design

### Design Philosophy

- **Dark theme only**, optimized for OLED displays (true black backgrounds save battery).
- **Glass morphism** style for AR overlay cards (semi-transparent with blur effect).
- **Material 3** design system via Jetpack Compose Material3.

### Color Palette

| Color Name      | Hex       | Usage                              |
|-----------------|-----------|------------------------------------|
| BrandPrimary    | `#00E5C3` | Primary accent, buttons, highlights |
| BrandDark       | `#0A0A0A` | Primary background                 |
| BrandSurface    | `#1A1A2E` | Card and surface backgrounds       |

### Country Accent Colors

| Country      | Accent Color | Usage                             |
|--------------|-------------|-----------------------------------|
| Israel       | Blue        | Flag indicator, country highlights |
| Netherlands  | Orange      | Flag indicator, country highlights |
| UK           | Red         | Flag indicator, country highlights |

### Typography

Material 3 typography scale using the system default font. Key text styles:

- **Title:** Large, bold -- used for app name and section headers.
- **Body:** Medium weight -- used for vehicle info text.
- **Label:** Small, lighter weight -- used for field labels in AR cards.

---

## 18. Debugging

### Logcat Tags

| Tag                  | Component              | Logs                                   |
|----------------------|------------------------|----------------------------------------|
| `PlateAnalyzer`      | OCR pipeline           | Detected text, regex matches, debounce counts |
| `VehicleCache`       | Cache layer            | Cache hits, misses, insertions         |
| `FrameMotionTracker` | Motion tracking        | Motion vectors, tracking quality       |
| `CameraScreen`       | UI state               | Overlay state changes, user actions    |

All debug log statements are wrapped in `if (BuildConfig.DEBUG)` checks, so they are stripped from release builds.

### ADB Commands

**Filter logcat for app-specific tags:**

```bash
adb logcat | grep "PlateAnalyzer\|VehicleCache\|FrameMotionTracker\|CameraScreen"
```

**Take a screenshot:**

```bash
adb exec-out screencap -p > screenshot.png
```

**Clear app data:**

```bash
adb shell pm clear com.carinfo.ar
```

**Launch the app:**

```bash
adb shell am start -n com.carinfo.ar/.MainActivity
```

### Test APIs Directly

**Israel:**

```bash
curl "https://data.gov.il/api/3/action/datastore_search?resource_id=053cea08-09bc-40ec-8f7a-156f0677aff3&filters={\"mispar_rechev\":1234567}"
```

**Netherlands:**

```bash
curl "https://opendata.rdw.nl/resource/m9d7-ebf2.json?kenteken=3XKH01"
```

**UK:**

```bash
curl -X POST \
  "https://driver-vehicle-licensing.api.gov.uk/vehicle-enquiry/v1/vehicles" \
  -H "x-api-key: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"registrationNumber":"MT09NKS"}'
```

---

## 19. Testing Checklist

### Functional Tests

- [ ] Scan an Israeli plate (7 or 8 digits). Verify manufacturer, model, year, color, and fuel type are displayed.
- [ ] Scan a Dutch plate (6 alphanumeric characters). Verify brand, model, color, and registration date are displayed.
- [ ] Scan a UK plate (AB12CDE format). Verify make, color, year, fuel type, and MOT/tax status are displayed. Confirm model is absent.
- [ ] Test OCR correction: present a plate like `BD51SMR`; verify it is correctly read and looked up.
- [ ] Test country detection: verify the flag shown matches the SIM/network country.
- [ ] Test manual plate input: open the dialog, type a known plate number, confirm results match a camera scan.

### Settings Tests

- [ ] Change language in settings. Verify the entire UI updates to the selected language.
- [ ] Toggle sound off. Verify no sound or vibration on scan. Toggle back on and verify they resume.
- [ ] Change region in settings. Verify the OCR and API switch to the new country.

### History Tests

- [ ] Scan a plate and tap Save. Verify it appears in the History screen.
- [ ] Swipe-to-delete a history item. Verify it is removed.
- [ ] Use per-item delete button. Verify it is removed.
- [ ] Use Clear All. Verify the confirmation dialog appears and all items are removed after confirmation.

### Camera Tests

- [ ] Test pinch-to-zoom. Verify smooth zoom in and out.
- [ ] Test double-tap zoom. Verify toggle between 1x and 2x.
- [ ] Test with camera moving. Verify vehicle info cards appear in scrollable list at bottom of screen.

### Edge Case Tests

- [ ] Disable WiFi and mobile data. Scan a plate. Verify graceful error handling (no crash).
- [ ] Deny camera permission. Verify the app shows a rationale and does not crash.
- [ ] Scan a plate that does not exist in the database. Verify "not found" indicator appears.
- [ ] Rapidly scan multiple different plates. Verify debounce prevents excessive API calls.

---

## 20. Known Issues & Limitations

| #  | Issue                                      | Severity | Details                                                       |
|----|--------------------------------------------|----------|---------------------------------------------------------------|
| 1  | DVLA API key hardcoded in source           | High     | The UK API key is stored in the source code. Should be moved to a secure backend or build config. |
| 2  | No model info from DVLA (UK)               | Medium   | The DVLA VES API does not return the vehicle model, only the make. This is an API limitation. |
| 3  | OCR can misread handwritten/damaged plates  | Medium   | ML Kit performs best on clean, printed plates. Handwritten, dirty, or damaged plates may not be recognized. |
| 4  | Debounce may miss fast scans               | Low      | Requiring 3 sightings means very brief exposures may not trigger a lookup. |
| 5  | No offline mode                            | Medium   | The app requires an internet connection for API calls. No cached vehicle database exists. |
| 6  | ~~No crash reporting~~                     | ~~Done~~ | ~~Firebase Crashlytics is now integrated and active.~~ |
| 7  | AdMob payment setup required               | Medium   | AdMob account requires payment setup completion for ads to actually serve in production. |
| 8  | ~~ProGuard rules minimal~~                 | ~~Done~~ | ~~ProGuard rules added for Retrofit, Gson, Billing, Firebase, ML Kit.~~ |
| 9  | No unit or instrumentation tests           | Medium   | The project has no automated test coverage.                   |
| 10 | Vehicle cache has no TTL                   | Low      | Cached vehicle data (including failures) persists for the entire session. Failed lookups cached as null — user must restart app to retry. |
| 11 | ScanHistory file I/O on main thread        | Low      | save/delete/load use synchronized file I/O. Fast enough for 100 records but not ideal. |
| 12 | Keystore password in build.gradle.kts      | Medium   | Signing passwords hardcoded in source. Acceptable for private repo but should use local.properties for public repos. |

---

## 21. Future Roadmap

| Priority | Feature                        | Description                                                        |
|----------|--------------------------------|--------------------------------------------------------------------|
| ~~Done~~ | ~~Extended Israel data~~       | ~~8 data.gov.il resources: WLTP specs, history, ownership, disabled tag, importer/price, statistics~~ |
| ~~Done~~ | ~~Extended Netherlands data~~  | ~~3 RDW resources: fuel/emissions, recalls, 20+ extra fields from main dataset~~ |
| ~~Done~~ | ~~Extended UK data~~           | ~~V5C date, type approval, export status from existing DVLA API~~ |
| ~~Done~~ | ~~GA4 MCP integration~~        | ~~Service account + MCP server for direct Analytics queries from Claude Code~~ |
| ~~Done~~ | ~~History screen all fields~~  | ~~Expanded view shows all extended fields with highlighted price and disabled tag~~ |
| Medium   | UK MOT History API             | Register for DVSA MOT History API for full test history + odometer readings. Requires separate API key from https://documentation.history.mot.api.gov.uk/mot-history-api/register |
| Medium   | Add more countries             | USA (NHTSA VIN decode), Poland (CEPiK). See FindOut research at `C:\Users\ASUS\Desktop\FindOut\RESEARCH.md` |
| ~~Done~~ | ~~Firebase Crashlytics~~       | ~~Integrated and active. Console: https://console.firebase.google.com/project/carinfo-ar~~ |
| High     | Secure API key storage         | Move the DVLA API key to a backend proxy or encrypted build config. |
| ~~Done~~ | ~~Real AdMob IDs~~             | ~~Production ad unit IDs are now active.~~                         |
| ~~Done~~ | ~~Privacy policy~~             | ~~Hosted at https://carinfoar.com~~                                |
| ~~Done~~ | ~~Play Store listing~~         | ~~Screenshots, descriptions, and icon ready. See Section 22.~~     |
| ~~Done~~ | ~~Remove ads purchase~~        | ~~One-time IAP via Google Play Billing 7.1.1. See Section 16d.~~   |
| ~~Done~~ | ~~Core library desugaring~~    | ~~java.time backport for Android 7.0-7.1 (API 24-25).~~           |
| ~~Done~~ | ~~Thread safety hardening~~    | ~~VehicleCache, ScanHistory, BillingManager — all synchronized.~~  |
| ~~Done~~ | ~~Retrofit timeouts~~          | ~~5s connect, 8s read on all API clients.~~                        |
| Medium   | Unit tests                     | Add unit tests for OCR correction, API parsing, and cache logic.   |
| Medium   | Instrumentation tests          | Add UI tests for navigation flow and screen rendering.             |
| Low      | Offline vehicle database       | Cache a subset of vehicle data for offline lookups.                |
| Low      | Dark/light theme toggle        | Add a light theme option for outdoor use.                          |
| Low      | Export history                 | Allow users to export scan history as CSV or PDF.                  |

---

## 22. Play Store Listing

### App Name
**CarInfo AR**

### Short Description (80 chars max)
Scan any license plate with your camera and instantly see vehicle details in AR.

### Full Description

CarInfo AR turns your phone camera into a powerful vehicle identification tool. Simply point your camera at any license plate and instantly see detailed vehicle information floating right above the car in augmented reality.

🔍 HOW IT WORKS
Just open the app and point your camera at a license plate. CarInfo AR uses advanced on-device OCR to read the plate number, then queries official government databases to retrieve vehicle details — all in real time.

🚗 VEHICLE INFORMATION
• Manufacturer & Model
• Year of manufacture
• Color
• Fuel type
• And more depending on the country

🌍 SUPPORTED COUNTRIES
• Israel — Government vehicle registry (data.gov.il)
• Netherlands — RDW Open Data
• United Kingdom — DVLA Vehicle Enquiry Service

📱 KEY FEATURES
• Real-time AR overlay with vehicle info
• Automatic country detection via SIM/network
• Manual plate number input
• Scan history with search and management
• Pinch-to-zoom and double-tap zoom
• Sound and haptic feedback
• 14 languages supported
• Dark theme optimized for OLED displays

🔒 PRIVACY FIRST
• Camera images are processed on-device only — never uploaded
• Plate numbers are sent only to official government APIs
• No user accounts required
• No personal data collected
• Scan history stored locally on your device
• Full privacy policy: https://carinfoar.com

🌐 LANGUAGES
Hebrew, English, Dutch, French, German, Spanish, Italian, Portuguese, Arabic, Turkish, Russian, Chinese, Japanese, Korean

The app automatically detects your location and language. You can also manually change the country and language in settings.

⚠️ DISCLAIMER
This app is not affiliated with, endorsed by, or associated with any government entity. Vehicle data is sourced from publicly available government databases.

📊 DATA SOURCES
• Israel: https://data.gov.il
• Netherlands: https://opendata.rdw.nl
• United Kingdom: https://driver-vehicle-licensing.api.gov.uk

### Store Metadata

| Field              | Value                                              |
|--------------------|----------------------------------------------------|
| Category           | Auto & Vehicles                                    |
| Content Rating     | Everyone                                           |
| Contact Email      | contact@carinfo-ar.app                             |
| Privacy Policy URL | https://carinfoar.com                              |
| Tags               | license plate, vehicle info, car scanner, AR, augmented reality, plate reader, vehicle lookup, car details, DVLA, RDW |

### Screenshots

| #  | File                          | Content                          |
|----|-------------------------------|----------------------------------|
| 1  | `screenshots/01_splash.jpg`   | Splash screen — CarInfo AR logo  |
| 2  | `screenshots/02_scan_mini.jpg`| Mini Cooper detected             |
| 3  | `screenshots/03_scan_multi.jpg`| Two vehicles detected           |
| 4  | `screenshots/02_settings.png` | Settings screen                  |

### Release Files

| File                  | Path                                                    |
|-----------------------|---------------------------------------------------------|
| Release AAB           | `app/build/outputs/bundle/release/app-release.aab`      |
| Release APK           | `app/build/outputs/apk/release/app-release.apk`         |
| Keystore              | `~/.keystores/carinfo-release.keystore` (NOT in repo)   |
| Play Store Icon       | `app/src/main/ic_launcher_playstore.png` (512x512)      |

---

## 23. Google Play Console

### Developer Account

| Property              | Value                                              |
|-----------------------|----------------------------------------------------|
| Developer ID          | `7085638337730191412`                               |
| Developer Name        | benziheler                                          |
| App ID                | `4972478906413110413`                               |
| Package Name          | `com.carinfo.ar`                                    |
| Console URL           | https://play.google.com/console/u/0/developers/7085638337730191412/app/4972478906413110413 |

### Release History

| Version Code | Version Name | Track            | Date          | Status          | Notes                                    |
|-------------|-------------|------------------|---------------|-----------------|------------------------------------------|
| 1           | 1.0         | Closed testing   | Mar 26, 2026  | Superseded      | First upload, had splash crash on API 27 |
| 4           | 1.0.3       | Internal testing | Mar 26, 2026  | Superseded      | Fixed splash crash, ConcurrentHashMap NPE |
| 5           | 1.0.4       | Internal testing | Mar 26, 2026  | Superseded      | SSL cert fix for Android 8.x            |
| 8           | 1.0.7       | Internal testing | Mar 26, 2026  | Superseded      | ProGuard/R8 Gson fix for history         |
| 11          | 1.1.0       | Internal testing | Mar 26, 2026  | Superseded      | Analytics, save animation, sound fallback |
| 12          | 1.1.1       | Closed testing   | Mar 26, 2026  | Superseded      | First submission for review              |
| 14          | 1.2.0       | Internal testing | Mar 27, 2026  | Superseded      | Disclaimer fix for Play Store            |
| 15          | 1.2.1       | Internal testing | Mar 27, 2026  | Superseded      | Extended data: 8 IL resources, 3 NL resources, UK extra fields, history screen with all fields, 45% scroll area |
| 16          | 1.2.2       | Production       | Mar 28, 2026  | Superseded      | Image-based plate scanning, Billing PendingIntent NPE guard |
| 17          | 1.2.3       | Production       | Apr 17, 2026  | Superseded      | Fix ghost reset button when lookup returns null; show PlateNotFoundIndicator briefly then auto-remove |
| 18          | 1.2.4       | Internal testing | Apr 17, 2026  | Superseded      | Camera toolbar redesign: labeled pill buttons (Image/Manual/History/Settings) in all 14 languages, SpaceBetween full-width layout, larger text. Removed global Reset button — each car card has its own X dismiss with scale+fade+shrink exit animation and LazyColumn animateItem for smooth reflow |
| 19          | 1.2.5       | Production       | Apr 20, 2026  | Superseded      | Toolbar restructured: Image + Manual collapsed into a single "Scan Options" expandable pill with a dropdown menu. Settings downgraded to a circular icon-only button. New `toolbar_scan_options` string ("More scan options") in all 14 languages. Scan hint now renders each word on its own line (32sp ExtraBold). Live in 177 countries. |
| 20          | 1.2.6       | Production       | Apr 21, 2026  | **Active**      | Scan hint + viewfinder now shift up into the empty band above the car card when a result is visible (was previously a tiny unintended pill at the top). Price estimator calibrated against Levi Itzhak pricelist: IL depreciation curve past Y6 steepened (0.93→0.90 per year) and Dacia added to the weak-resale brand set. A 2016 Duster now estimates ~₪36k instead of ~₪44k (real ≈ ₪31.5k). |

### Play Store Setup Completed

| Item                        | Status | Details                                              |
|-----------------------------|--------|------------------------------------------------------|
| App name & descriptions     | ✅     | English (en-US) — short + full description           |
| App icon (512x512)          | ✅     | Custom car + camera lens icon                        |
| Feature graphic (1024x500)  | ✅     | CarInfo AR branding with tagline                     |
| Phone screenshots (4)       | ✅     | Scan, multi-scan, history, settings                  |
| Tablet screenshots 7" (4)   | ✅     | Same as phone screenshots                            |
| Tablet screenshots 10" (4)  | ✅     | Same as phone screenshots                            |
| Privacy policy URL          | ✅     | https://carinfoar.com                                |
| Content rating              | ✅     | Completed questionnaire                              |
| Target audience             | ✅     | Set                                                  |
| Data safety                 | ✅     | No data collection declared                          |
| Government apps             | ✅     | No                                                   |
| Financial features          | ✅     | No financial features                                |
| Health apps                 | ✅     | No health features                                   |
| Advertising ID              | ✅     | Yes — Analytics + Advertising/marketing              |
| App category                | ✅     | Auto & Vehicles                                      |
| Contact email               | ✅     | Set in store listing                                 |
| Countries/regions           | ✅     | 176 countries                                        |
| Testers                     | ✅     | Avner + Internal Testers list                        |
| App signing                 | ✅     | Google Play App Signing enabled                      |
| App integrity               | ✅     | Automatic protection on                              |

### What's Needed for Production Access

Google requires a **Closed testing** release to be reviewed and approved before granting Production access. Once the Closed testing review passes:

1. Go to Dashboard → "Apply for access to production"
2. Answer questions about the closed test
3. Once approved, promote the Closed testing release to Production

### Domain

| Property       | Value                    |
|----------------|--------------------------|
| Domain         | carinfoar.com            |
| Registrar      | Namecheap                |
| Cost           | $11.48/year              |
| DNS            | CNAME → GitHub Pages     |
| Privacy        | WhoisGuard (free)        |
| Hosts          | Privacy policy via GitHub Pages (`docs/`) |

---

*This document is the single source of truth for the CarInfoAR project. Keep it updated as the codebase evolves.*
