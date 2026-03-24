# CarInfoAR -- Complete Documentation

> **Version:** 1.0
> **Platform:** Android
> **Last Updated:** 2026-03-24
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
8. [Motion Tracking (Optical Flow)](#8-motion-tracking-optical-flow)
9. [Camera Configuration](#9-camera-configuration)
10. [UI Screens](#10-ui-screens)
11. [AR Overlay System](#11-ar-overlay-system)
12. [Country Detection](#12-country-detection)
13. [Language System](#13-language-system)
14. [Sound System](#14-sound-system)
15. [Data Storage](#15-data-storage)
16. [Ad System](#16-ad-system)
16b. [Privacy Policy](#16b-privacy-policy)
17. [Theme & Design](#17-theme--design)
18. [Debugging](#18-debugging)
19. [Testing Checklist](#19-testing-checklist)
20. [Known Issues & Limitations](#20-known-issues--limitations)
21. [Future Roadmap](#21-future-roadmap)

---

## 1. Overview

### What CarInfoAR Does

CarInfoAR is an Android augmented reality application that uses the device camera to scan vehicle license plates in real time. Once a plate is detected via on-device OCR, the app queries government vehicle-registration APIs to retrieve information about the vehicle (manufacturer, model, year, color, fuel type, and more). The results are displayed as floating AR overlay cards anchored to the detected plate position in the camera preview.

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
|   |-- screens/
|   |   |-- SplashScreen.kt      # Animated splash with car icon & scan line
|   |   |-- OnboardingScreen.kt  # 3-page pager (Welcome, Country, Ready)
|   |   |-- CameraScreen.kt      # Live preview, AR overlays, manual input
|   |   |-- SettingsScreen.kt    # Language, sound, about, region
|   |   |-- HistoryScreen.kt     # Saved scans list, swipe-to-delete
|   |-- components/
|   |   |-- FloatingCarInfo.kt   # Glass-morphism AR info card
|   |   |-- LoadingPlateIndicator.kt  # Pulsing plate-number animation
|   |   |-- PlateNotFoundIndicator.kt # Red "not found" indicator
|   |   |-- ViewfinderOverlay.kt      # Camera viewfinder frame
|   |-- theme/
|       |-- Theme.kt             # Material 3 dark theme definition
|       |-- Color.kt             # Brand and country-accent colors
|       |-- Type.kt              # Typography scale
|-- data/
|   |-- api/
|   |   |-- IsraelApiService.kt  # Retrofit interface for data.gov.il
|   |   |-- NetherlandsApiService.kt  # Retrofit interface for RDW
|   |   |-- UkApiService.kt      # Retrofit interface for DVLA VES
|   |-- models/
|   |   |-- VehicleInfo.kt       # Unified vehicle data class
|   |   |-- ScanRecord.kt        # History item data class
|   |   |-- Country.kt           # Enum: ISRAEL, NETHERLANDS, UK
|   |-- repository/
|   |   |-- VehicleRepository.kt # Dispatches API calls by country
|   |   |-- HistoryRepository.kt # JSON file read/write for scan history
|   |-- cache/
|       |-- VehicleCache.kt      # In-memory Map with Mutex guard
|-- camera/
|   |-- PlateAnalyzer.kt         # ImageAnalysis.Analyzer, ML Kit OCR
|   |-- FrameMotionTracker.kt    # Block-matching optical flow on Y plane
|   |-- PlateOverlayState.kt     # Data class for per-plate AR state
|-- util/
|   |-- CountryDetector.kt       # TelephonyManager-based country detection
|   |-- OcrCorrector.kt          # Per-country character-swap algorithms
|   |-- SoundManager.kt          # MediaPlayer + Vibrator wrapper
|   |-- PreferencesManager.kt    # DataStore preferences helper
|   |-- StringResources.kt       # Programmatic string lookup utility
```

### Design Patterns

| Pattern         | Where Used                         | Purpose                                                  |
|-----------------|------------------------------------|----------------------------------------------------------|
| MVVM-lite       | CameraScreen state hoisting        | UI state is hoisted into composable scope; no ViewModel class but state is separated from UI rendering. |
| Repository      | VehicleRepository, HistoryRepository | Abstracts data sources (network, file) behind a clean interface. |
| Singleton       | VehicleCache, SoundManager, PreferencesManager | Single shared instance across the app lifetime.          |
| Observer/Flow   | DataStore preferences              | Reactive preference reads via Kotlin Flow.               |
| Strategy        | OcrCorrector per country           | Different OCR correction logic selected at runtime based on the active country. |

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

### Custom App Icon

- Custom launcher icon provided at all standard densities: **mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi**.
- Play Store icon: **512x512** (`ic_launcher_playstore.png`).
- Adaptive icon XML has been removed; the app uses **PNG directly** for the launcher icon.

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
6. Trim whitespace.

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

### Debounce Logic

A plate number must be detected in **3 separate frames** before an API call is triggered. This prevents:

- False positives from partial reads.
- Redundant API calls from flickering detections.
- Processing of non-plate text that momentarily matches the regex.

The sighting counter resets if the plate is not seen for a configurable timeout period.

### Dynamic Country Provider

The `PlateAnalyzer` receives a `countryProvider: () -> Country` lambda that is evaluated on every frame. This allows the active country to change in real time (for example, if the user changes the region in settings while the camera is active) without restarting the analyzer.

---

## 8. Motion Tracking (Optical Flow)

### FrameMotionTracker

The `FrameMotionTracker` class implements a lightweight block-matching optical flow algorithm to keep AR overlays anchored to the correct screen positions as the camera moves between OCR detections.

### Algorithm Details

1. **Downsample:** The camera frame's Y (luminance) plane is downsampled to **80x60 pixels** for performance.
2. **Block Matching:** For each tracked overlay position, a small block of pixels around the previous position is compared against candidate positions in the new frame using **Sum of Absolute Differences (SAD)**.
3. **Search Window:** The search is performed within a **plus or minus 10 pixel** radius from the previous position (in the downsampled coordinate space).
4. **Motion Vector:** The candidate with the lowest SAD score yields the motion vector, which is scaled back up to full-resolution coordinates and applied to the overlay position.
5. **Per-Frame Update:** Motion tracking runs on every analysis frame and updates all active overlay positions.

### OCR Position Snap

When OCR detects a plate in a new frame, the overlay position is snapped to the ground-truth bounding box from ML Kit, correcting any drift that may have accumulated from the block-matching tracker.

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
| Top toolbar         | Settings (gear icon) and History (clock icon) buttons      |
| Manual input FAB    | Opens a dialog for typing a plate number manually          |
| Reset button        | Clears all current overlays                                |

**Manual Input Dialog:** Allows the user to type a plate number directly. The app runs the same API lookup and displays results as if the plate had been scanned. Useful for plates that are difficult to scan (dirty, damaged, or at an awkward angle).

### 10.4 SettingsScreen

Settings are displayed in this order:

| #  | Setting         | Type           | Details                                              |
|----|-----------------|----------------|------------------------------------------------------|
| 1  | Language        | Dropdown       | 15 options (14 languages + device default)           |
| 2  | Sound & Feedback| Toggle switch  | Enables/disables scan sounds and vibration           |
| 3  | About           | Info section   | App version, credits                                 |
| 4  | Region          | Selector       | Country override; placed **last** in the list        |

**Region hint text:** "Select the country you are currently in, not your home country."

**Note:** The country flag is shown only in Settings (Region selector). It has been removed from the Camera screen.

Changing the language triggers `Activity.recreate()` to apply the new locale immediately.

### 10.5 HistoryScreen

| Feature            | Implementation                                        |
|--------------------|-------------------------------------------------------|
| List display       | `LazyColumn` with scan records                        |
| Per-item display   | Plate number, manufacturer, model, year, color, flag  |
| Swipe-to-delete    | Swipe **left only** to reveal delete action           |
| Per-item delete    | Explicit delete button/icon on each row               |
| Clear all          | **Text button** at top; shows a confirmation dialog before clearing |
| Sound effects      | Sound plays on single-item delete and clear-all actions |

History is capped at 100 records. Newest records appear first.

---

## 11. AR Overlay System

### PlateOverlayState

```
data class PlateOverlayState(
    val plateNumber: String,
    val screenX: Float,
    val screenY: Float,
    val vehicleInfo: VehicleInfo?,
    val isLoading: Boolean,
    val lastSeenTime: Long
)
```

Each detected plate gets its own `PlateOverlayState` instance. The state tracks screen position (updated by motion tracking), loading status, and the fetched vehicle info.

### Overlay Components

| Component                 | Shown When                        | Description                              |
|---------------------------|-----------------------------------|------------------------------------------|
| `FloatingCarInfo`         | Vehicle info loaded successfully  | Glass-morphism card showing manufacturer, model, year, color, fuel type, country flag. Includes Save and Info buttons. |
| `LoadingPlateIndicator`   | API call in progress              | Pulsing plate number text indicating a lookup is underway. |
| `PlateNotFoundIndicator`  | API returned no results           | Red-colored "not found" text overlay.    |

### FloatingCarInfo Card

The glass-morphism card displays:

- **Country flag** (top corner).
- **Manufacturer** name.
- **Model** name (if available; not returned by DVLA).
- **Year** of manufacture.
- **Color** of the vehicle.
- **Fuel type**.
- **Save button:** Tapping saves the scan to history.
- **Info button:** Opens additional details if available.

### Stale Overlay Removal

Overlays are automatically removed if the corresponding plate has not been seen by OCR for **3 seconds**. The `lastSeenTime` field is updated on every OCR detection. A periodic check compares the current time against `lastSeenTime` and removes expired overlays.

### Interaction

- Tapping the **Save** button on a `FloatingCarInfo` card writes the scan record to the history JSON file.
- A sound and vibration confirm the save action.

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

The app uses `MediaPlayer` with the system's default notification ringtone (via `RingtoneManager.TYPE_NOTIFICATION`), **not** `ToneGenerator`. The sound enabled/disabled state is synced from DataStore preferences via `LaunchedEffect` in composables that need it.

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

**Operations:**

| Operation     | Description                                                   |
|---------------|---------------------------------------------------------------|
| Load          | Read and parse JSON file on app start                         |
| Save          | Add new record; if plate already exists, replace it; trim to 100 |
| Delete single | Remove one record by plate number                             |
| Clear all     | Delete all records (with user confirmation dialog)            |

### 15.3 Vehicle Cache

| Property       | Value                                    |
|----------------|------------------------------------------|
| Type           | `MutableMap<String, VehicleInfo?>`        |
| Thread safety  | `Mutex` guarding all read/write access   |
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
| Interstitial trigger| Every 3 successful plate detections            |
| Status              | Active with real production IDs                |

### How to Disable

Ads are currently **enabled** in production. To disable them:

1. **Comment out** the interstitial ad loading and showing code in `CameraScreen.kt`.
2. **Comment out** the banner ad composable in the camera screen layout.
3. **Replace** the production ad unit IDs with AdMob test IDs if you want to keep the code but prevent real ad serving during development.

---

## 16b. Privacy Policy

| Property       | Value                                              |
|----------------|----------------------------------------------------|
| URL            | https://benzih.github.io/CarInfoAR/                |
| Hosted via     | GitHub Pages from `docs/index.html`                |
| Required for   | Google Play Store submission                       |

### Coverage

The privacy policy covers the following areas:

- **Camera usage:** How the app uses the device camera for plate scanning.
- **Plate data:** How scanned license plate numbers are processed.
- **Local storage:** How scan history is stored on-device.
- **AdMob:** Google AdMob advertising SDK and data collection.
- **Third-party APIs:** Data sent to government vehicle registration APIs (data.gov.il, RDW, DVLA).

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
- [ ] Test with camera moving. Verify AR overlays track the plate position (motion tracking).

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
| 6  | No crash reporting                         | Medium   | Firebase Crashlytics or equivalent is not integrated. Crashes in production go unreported. |
| 7  | AdMob payment setup required               | Medium   | AdMob account requires payment setup completion for ads to actually serve in production. |
| 8  | ProGuard rules minimal                     | Low      | Release builds may not be fully optimized or obfuscated. ProGuard/R8 rules should be reviewed. |
| 9  | No unit or instrumentation tests           | Medium   | The project has no automated test coverage.                   |
| 10 | Vehicle cache has no TTL                   | Low      | Cached vehicle data persists for the entire session. Stale data is possible if a vehicle's registration changes. |

---

## 21. Future Roadmap

| Priority | Feature                        | Description                                                        |
|----------|--------------------------------|--------------------------------------------------------------------|
| High     | Add more countries             | Extend API support to additional countries (e.g., Germany, France, USA). |
| High     | Firebase Crashlytics           | Integrate crash reporting for production monitoring.               |
| High     | Secure API key storage         | Move the DVLA API key to a backend proxy or encrypted build config. |
| ~~Done~~ | ~~Real AdMob IDs~~             | ~~Production ad unit IDs are now active.~~                         |
| ~~Done~~ | ~~Privacy policy~~             | ~~Hosted at https://benzih.github.io/CarInfoAR/~~                 |
| Medium   | Play Store listing             | Prepare store listing assets (screenshots, descriptions, feature graphic). |
| Medium   | Unit tests                     | Add unit tests for OCR correction, API parsing, and cache logic.   |
| Medium   | Instrumentation tests          | Add UI tests for navigation flow and screen rendering.             |
| Low      | Premium features               | Offer ad-free experience, extended history, or additional country support as in-app purchases. |
| Low      | Offline vehicle database       | Cache a subset of vehicle data for offline lookups.                |
| Low      | Dark/light theme toggle        | Add a light theme option for outdoor use.                          |
| Low      | Export history                 | Allow users to export scan history as CSV or PDF.                  |

---

*This document is the single source of truth for the CarInfoAR project. Keep it updated as the codebase evolves.*
