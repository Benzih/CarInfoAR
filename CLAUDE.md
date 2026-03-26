# CarInfoAR -- Complete Documentation

> **Version:** 1.1.1 (versionCode 12)
> **Platform:** Android
> **Last Updated:** 2026-03-26
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

### 6.1 Israel (data.gov.il)

| Property       | Value                                                              |
|----------------|--------------------------------------------------------------------|
| Base URL       | `https://data.gov.il/`                                             |
| Endpoint       | `GET /api/3/action/datastore_search`                               |
| Resource ID    | `053cea08-09bc-40ec-8f7a-156f0677aff3`                             |
| Authentication | None                                                               |
| Rate Limit     | Unknown (government open data)                                     |
| Cost           | Free                                                               |

**Query Parameters:**

| Parameter    | Value                                            |
|--------------|--------------------------------------------------|
| `resource_id`| `053cea08-09bc-40ec-8f7a-156f0677aff3`           |
| `filters`    | `{"mispar_rechev":PLATE_NUMBER}` (integer, no quotes around number) |

**Response Fields:**

| API Field         | Description           | Maps To              |
|-------------------|-----------------------|----------------------|
| `mispar_rechev`   | License plate number  | `plateNumber`        |
| `tozeret_nm`      | Manufacturer name     | `manufacturer`       |
| `kinuy_mishari`   | Commercial model name | `model`              |
| `shnat_yitzur`    | Year of manufacture   | `year`               |
| `tzeva_rechev`    | Vehicle color         | `color`              |
| `sug_delek_nm`    | Fuel type             | `fuelType`           |
| `ramat_gimur`     | Trim level            | `trimLevel`          |
| `baalut`          | Ownership type        | `ownership`          |
| `mivchan_acharon_dt` | Last test date     | `lastTestDate`       |
| `tokef_dt`        | Test valid until      | `testValidUntil`     |
| `degem_manoa`     | Engine model          | `engineModel`        |
| `misgeret`        | Chassis number        | `chassisNumber`      |
| `zmig_kidmi`      | Front tires           | `frontTires`         |
| `zmig_ahori`      | Rear tires            | `rearTires`          |
| `moed_aliya_lakvish` | On-road date       | `onRoadDate`         |
| `kvutzat_zihum`   | Emission group        | `emissionGroup`      |
| `degem_nm`        | Model (fallback)      | `model` (fallback)   |

**Example Request:**

```bash
curl "https://data.gov.il/api/3/action/datastore_search?resource_id=053cea08-09bc-40ec-8f7a-156f0677aff3&filters={\"mispar_rechev\":1234567}"
```

---

### 6.2 Netherlands (RDW Open Data)

| Property       | Value                                          |
|----------------|-------------------------------------------------|
| Base URL       | `https://opendata.rdw.nl/`                      |
| Endpoint       | `GET /resource/m9d7-ebf2.json`                  |
| Authentication | None                                            |
| Rate Limit     | Standard Socrata limits                         |
| Cost           | Free                                            |

**Query Parameters:**

| Parameter  | Value                        |
|------------|------------------------------|
| `kenteken` | Plate number (uppercase, no dashes) |

**Response Fields:**

| API Field                 | Description                 | Maps To         |
|---------------------------|-----------------------------|-----------------|
| `kenteken`                | License plate               | `plateNumber`   |
| `merk`                    | Brand / manufacturer        | `manufacturer`  |
| `handelsbenaming`         | Commercial model name       | `model`         |
| `eerste_kleur`            | Primary color               | `color`         |
| `datum_eerste_toelating`  | First registration date (YYYYMMDD) | `year` (parsed) |
| `brandstof_omschrijving`  | Fuel type                          | `fuelType`      |
| `cilinderinhoud`          | Engine capacity (cc)               | `engineCapacity`|
| `aantal_cilinders`        | Number of cylinders                | `numCylinders`  |
| `aantal_deuren`           | Number of doors                    | `numDoors`      |
| `aantal_zitplaatsen`      | Number of seats                    | `numSeats`      |
| `vervaldatum_apk`         | APK expiry date                    | `testValidUntil`|
| `catalogusprijs`          | Catalog price                      | `catalogPrice`  |
| `massa_rijklaar`          | Vehicle weight (kg)                | `weight`        |
| `inrichting`              | Body type                          | `bodyType`      |
| `wam_verzekerd`           | Insurance status                   | `insured`       |
| `wielbasis`               | Wheelbase (cm)                     | `wheelbase`     |

**OCR Fallback Logic:**

If the initial query returns an empty result, the app applies O/0 and I/1 character swaps and retries. This handles common OCR misreads where the letter O is read as the digit 0 (and vice versa), or the letter I is read as the digit 1 (and vice versa). The retry is performed once with the swapped variant.

**Example Request:**

```bash
curl "https://opendata.rdw.nl/resource/m9d7-ebf2.json?kenteken=3XKH01"
```

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

Vehicle info cards are now displayed in a **scrollable list** at the bottom of the screen (bottom third), not anchored to plate positions. This provides a stable, readable display regardless of camera movement. See [Section 11](#11-ar-overlay-system) for details.

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
| 5  | About           | Info section   | App version, credits                                 |
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

Vehicle info cards are displayed in a **scrollable `LazyColumn`** anchored to the **bottom third** of the camera screen. This replaces the earlier AR-anchored overlay approach, which was unstable on budget devices.

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

**Header:**
- Country flag + Manufacturer + Model (title)
- Year + Trim Level (subtitle, in brand color)
- Save button + Info button

**After accent line (sections in order):**
1. Test/MOT/APK — motStatus, testValidUntil, lastTestDate (expired dates in red)
2. Basic info — color, fuelType, ownership, bodyType, onRoadDate, plateNumber
3. Engine — engineCapacity, engineModel, numCylinders, co2Emissions, emissionGroup
4. Tax (UK) — taxStatus, taxDueDate (expired in red)
5. Specs — numDoors, numSeats, weight, wheelbase, catalogPrice
6. Tires (IL) — frontTires, rearTires
7. Insurance (NL) — insured status
8. Chassis (IL) — chassisNumber
9. Data source — shows the government data source URL (e.g., "Data source: data.gov.il")

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

When no cards are visible, a **blinking** "Scan a license plate" text appears in the center of the screen (localized to the app's language). The text pulses between full and 30% opacity.

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
| High     | Add more countries             | Extend API support to additional countries (e.g., Germany, France, USA). |
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
| 11          | 1.1.0       | Internal testing | Mar 26, 2026  | Active          | Analytics, save animation, sound fallback |
| 12          | 1.1.1       | Closed testing   | Mar 26, 2026  | **In Review**   | First submission for review              |

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
