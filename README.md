# GhostTouch — Android Overlay Attack: Real Risk, Real Defense

> **Educational Security Demo** — This project demonstrates how Android overlay attacks
> work and how production apps can defend against them. Strictly for local demonstration,
> security awareness, and educational purposes only. NOT intended for malicious use.

---

## Overview

GhostTouch is a proof-of-concept Android project that demonstrates:

1. **How overlay attacks work** — A "game" app harvests permissions, monitors the foreground,
   detects when a target app opens, and displays a pixel-perfect fake UI to capture everything
2. **How permissions are abused** — Game-themed permission requests (leaderboards, nearby players)
   grant access to GPS, contacts, carrier info, WiFi details, and more
3. **How data is exfiltrated** — Captured credentials + 50+ device intel fields are Base64-encoded,
   disguised as analytics telemetry, and "sent" to a fake C2 server
4. **How production apps can defend** — Touch filtering, overlay detection, FLAG_SECURE,
   biometric verification, and focus monitoring
5. **How iOS compares** — Why this specific attack is impossible on iOS, but alternative
   social engineering vectors still exist

---

## Architecture

```
GhostTouch/
├── common/              # Shared data models (CaptureSession)
├── attacker/            # Overlay attack demo app (com.ghosttouch.attacker)
│   ├── service/         # OverlayService, ForegroundDetector, BootReceiver
│   ├── overlay/         # OverlayManager, FakeLogin, FakePayment, CaptureAll, Tapjacking
│   ├── capture/         # SessionRepository (in-memory storage)
│   ├── exfil/           # DataExfiltrator, DeviceIntel (50+ field collector)
│   └── ui/              # LauncherActivity (game + control), GamePermissions, SessionList
├── defender/            # WcDonald's target app (com.wcdonalds.app)
│   ├── security/        # SecureTouchModifier, OverlayDetector, BiometricHelper, etc.
│   └── ui/              # MainActivity, LoginActivity, PaymentActivity, SettingsActivity
└── design/              # Pencil presentation (22 slides)
```

### Two Separate APKs

| App | Package | Purpose |
|-----|---------|---------|
| **GhostTouch** (Attacker) | `com.ghosttouch.attacker` | Overlay engine disguised as a tap game |
| **WcDonald's** (Defender) | `com.wcdonalds.app` | Target app with toggleable defenses |

---

## How the Attack Works

### Step 1: Disguise
The attacker app presents itself as a harmless tap-counter game ("Tap Rush"). This
gives the app a legitimate reason to exist and request permissions.

### Step 2: Permission Harvesting (The Game Trick)
On first launch, the game shows a "Game Setup" card requesting permissions with
convincing justifications:

| User Sees | They Grant | Attacker Gets |
|-----------|-----------|---------------|
| Leaderboards & Multiplayer | `INTERNET`, `ACCESS_NETWORK_STATE` | Data exfiltration + local IP |
| Find Nearby Players | `ACCESS_FINE_LOCATION`, `ACCESS_WIFI_STATE` | GPS coordinates, address, WiFi SSID/BSSID, gateway, DNS |
| Share Scores with Friends | `READ_CONTACTS` | Full contacts list (names + phone numbers) |
| Do Not Disturb | `READ_PHONE_STATE` | Carrier, SIM operator, network type, phone number |
| Save Game Replays | `READ_MEDIA_IMAGES` | Photo & file scan |
| Daily Rewards | `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS` | Auto-start after reboot (persistence) |

Plus two special permissions (granted manually in Settings):
- **SYSTEM_ALERT_WINDOW** — Draw over other apps
- **PACKAGE_USAGE_STATS** — Detect which app is in the foreground

### Step 3: Foreground Detection
A foreground service polls `UsageStatsManager.queryEvents()` every 700ms using a
state machine (IDLE → SHOWING → COOLDOWN) to detect the target app without flickering.

### Step 4: Overlay Display
`WindowManager.addView()` displays a fullscreen overlay using `TYPE_APPLICATION_OVERLAY`.
The overlay uses the exact same Material3 Scaffold/TopAppBar/TextField components as the
real app. A red border is added for demo identification only.

### Step 5: Credential Capture
Four attack modes available:
- **Fake Login** — Captures email + password
- **Fake Payment** — Captures card number, expiry, CVV
- **Capture All** — Fake "Account Verification" captures everything at once (email, password,
  full name, phone, card, expiry, CVV). Even the "Skip" button captures partial data
- **Tapjacking** — Invisible transparent overlay

### Step 6: Device Intelligence
`DeviceIntel` collects 50+ fields at capture time with NO exploits — just the permissions
the user already granted:

| Category | Fields |
|----------|--------|
| **Device** | manufacturer, model, brand, fingerprint, ABIs |
| **Location** | GPS lat/long, accuracy, altitude, full reverse-geocoded address |
| **WiFi** | SSID, BSSID, IP, gateway, DNS, subnet mask, signal, frequency |
| **Contacts** | Total count + first 10 contacts (name + phone) |
| **Carrier** | Name, SIM operator, network type (5G/LTE), phone number |
| **Apps** | Total installed + flagged banking, crypto, social, 2FA, email apps |
| **OS** | Version, SDK, security patch, Android ID, developer mode, ADB |
| **Hardware** | RAM, storage, battery, display resolution/density/size |

### Step 7: Fake Exfiltration
`DataExfiltrator` encodes credentials + device intel into a disguised analytics payload:

```json
{
  "event": "app_session_metric",
  "client_id": "a8f3...",
  "payload": "eyJjcmVkcyI6eyJ1IjoiZXhhbX...",
  "ts": 1712678400,
  "os": "14",
  "model": "Pixel 8",
  "locale": "Portuguese (Brazil)"
}
```

The `payload` field contains Base64-encoded stolen credentials + full device profile.

---

## Defense Mechanisms

All defenses are toggleable in WcDonald's Settings screen for demo comparison.

### 1. Filter Obscured Touches (MOST IMPORTANT)
```kotlin
Modifier.pointerInteropFilter { event ->
    if (event.flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED != 0) {
        true // Block touch — overlay detected
    } else false
}
```
- Simple, effective, catches ALL overlay types (visible and invisible)
- The flag is set by the OS and cannot be spoofed

### 2. Overlay Detection
Scans installed packages for `SYSTEM_ALERT_WINDOW` permission. Shows warning banner.

### 3. FLAG_SECURE
Prevents screenshots and screen recording of sensitive screens.

### 4. Biometric Verification
System biometric dialog renders ABOVE overlay windows — overlays cannot cover it.

### 5. Focus Monitoring
Detects window focus loss as a supplementary overlay signal.

### 6. UX Warning Banner
Prominent red banner: *"Screen interaction blocked for security reasons"*

---

## iOS Comparison

### Why This Attack Doesn't Work on iOS

| Protection | iOS | Android |
|-----------|-----|---------|
| Overlay drawing API | Blocked at OS level | Allowed with permission |
| Foreground app detection | No API exists | UsageStatsManager |
| Background UI | Suspended — no UI allowed | Services can add views |
| App distribution | App Store review required | Sideloading trivial |
| Clipboard access | Banner since iOS 14 | Silent access |
| Keychain | Per-app isolated | SharedPreferences weaker |
| Entitlements | Declared at build time | Requested at any time |

### iOS Alternative Attack Vectors
iOS blocks overlays by design, but is still vulnerable to social engineering:

- **Push + WebView Phishing** — Fake notification opens in-app WebView mimicking login
- **Custom Keyboard Keylogger** — Themed keyboard with "Full Access" captures all keystrokes
- **Universal Link Hijacking** — Same URL patterns route user to attacker app
- **ReplayKit Screen Capture** — "Gameplay recording" permission captures other apps
- **Fake Auth Sheet** — ASWebAuthenticationSession with attacker-controlled content
- **MDM/Enterprise Certificate** — Bypass App Store, intercept network traffic

### The Real Lesson
> No OS can protect users who enter data in the wrong place.
> Android overlay attacks exploit the permission model.
> iOS social engineering exploits user trust directly.
> The user is always the last line of defense.

---

## Setup

### Prerequisites
- Android Studio (or command-line Android SDK)
- Android SDK API 26+ (minSdk)
- Emulator or device running Android 8.0+
- Java 21

### Build
```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
./gradlew assembleDebug
```

### Install
```bash
adb install attacker/build/outputs/apk/debug/attacker-debug.apk
adb install defender/build/outputs/apk/debug/defender-debug.apk
```

---

## Demo Walkthrough

### Demo 1 — Full Attack (Defenses OFF)
1. Open **WcDonald's** > Settings > Disable all defenses
2. Open **GhostTouch** > Game tab > "Enable All Features" (grants permissions)
3. Switch to Control tab > Grant overlay + usage stats
4. Select **"Capture All"** mode > Start Overlay Service
5. Press Home, open **WcDonald's**
6. Fake "Account Verification" overlay appears (with red demo border)
7. Enter test data in all fields, tap "Verify Account"
8. Return to **GhostTouch** > Sessions tab
9. Expand session to view: credentials + 50+ device intel fields + encoded payload

### Demo 2 — Defense ON
1. Open **WcDonald's** > Settings > Enable all defenses
2. Repeat the attack
3. Observe: inputs blocked, warning banner shown, biometric gate on payment

### Demo 3 — Visual Identification
- Overlay has a **red border** — in a real attack there would be none
- The overlay uses the exact same Material3 components as the real app

---

## File Reference

### Attacker — Key Files
| File | Description |
|------|-------------|
| `OverlayService.kt` | State machine (IDLE→SHOWING→COOLDOWN), triggers overlays |
| `ForegroundDetector.kt` | UsageStatsManager polling with caching to prevent flicker |
| `OverlayManager.kt` | WindowManager + ComposeView lifecycle + pending flag race prevention |
| `FakeLoginOverlay.kt` | Pixel-perfect WcDonald's login replica with red demo border |
| `FakePaymentOverlay.kt` | Pixel-perfect payment form replica |
| `CaptureAllOverlay.kt` | Combined "Account Verification" capturing all fields at once |
| `TapjackingOverlay.kt` | Invisible overlay for transparent tap interception |
| `DeviceIntel.kt` | 50+ field device intelligence collector (GPS, contacts, carrier, WiFi, apps) |
| `DataExfiltrator.kt` | Base64 encoding + disguised analytics payload with device intel |
| `SessionRepository.kt` | In-memory session storage with StateFlow |
| `GamePermissions.kt` | Permission groups disguised as game features |
| `BootReceiver.kt` | Auto-start after device reboot |

### Defender — Key Files
| File | Description |
|------|-------------|
| `SecureTouchModifier.kt` | Compose Modifier for FLAG_WINDOW_IS_OBSCURED filtering |
| `OverlayDetector.kt` | Scans installed apps for overlay permission |
| `SecureScreenHelper.kt` | FLAG_SECURE application helper |
| `BiometricHelper.kt` | AndroidX BiometricPrompt wrapper (overlay-proof) |
| `FocusMonitor.kt` | Window focus change detection |
| `OverlayWarningBanner.kt` | Red warning banner composable |
| `DefenseSettings` | Singleton with toggleable defense flags |

---

## Presentation

22-slide Pencil presentation with cyberpunk terminal theme (navy/cyan/slate):

| Slides | Section |
|--------|---------|
| 1-4 | Introduction: title, problem, 5-step attack flow, key insight |
| 5-7 | Attack deep-dive: live demo steps, impact, why it works |
| 8-10 | Defense: strategy, filterTouchesWhenObscured, defense demo |
| 11-12 | Trade-offs and per-screen recommendations |
| 13-15b | New attack features: permission trick, data harvested, capture-all, red border |
| 16b | Full defense checklist (developers + users) |
| 18-20 | iOS comparison: security model table, 6 attack vectors, cross-platform lesson |
| 21-22 | Final message + Q&A |

---

## Key Takeaway

> If your UI can be mimicked, your users can be tricked.
>
> `filterTouchesWhenObscured` is the single most effective defense against overlay attacks.
> It's simple, catches all overlay types, and the OS flag cannot be spoofed.
> Apply it to login, payment, and personal data forms.
>
> No OS — Android or iOS — can fully protect users from social engineering.
> Defense in depth is the only answer.

---

## Disclaimer

This project is created exclusively for **educational and security awareness purposes**.
It demonstrates known Android security concepts that are publicly documented by Google.
Do NOT use this code for unauthorized access, credential theft, or any malicious activity.
The authors are not responsible for misuse of this code.
