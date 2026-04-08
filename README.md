# GhostTouch — Android Overlay Attack: Real Risk, Real Defense

> **Educational Security Demo** — This project demonstrates how Android overlay attacks
> work and how production apps can defend against them. Strictly for local demonstration,
> security awareness, and educational purposes only. NOT intended for malicious use.

---

## Overview

GhostTouch is a proof-of-concept Android project that demonstrates:

1. **How overlay attacks work** — A "disguise" app monitors the foreground, detects when
   a target app opens, and displays a pixel-perfect fake UI on top to capture credentials
2. **How data is exfiltrated** — Captured data is Base64-encoded, disguised as analytics
   telemetry, and "sent" to a fake server (simulation only — no real network requests)
3. **How production apps can defend** — Touch filtering, overlay detection, FLAG_SECURE,
   biometric verification, and focus monitoring

---

## Architecture

```
GhostTouch/
├── common/          # Shared data models (CaptureSession)
├── attacker/        # Overlay attack demo app
│   ├── service/     # OverlayService, ForegroundDetector, NotificationHelper
│   ├── overlay/     # OverlayManager, FakeLoginOverlay, FakePaymentOverlay, TapjackingOverlay
│   ├── capture/     # SessionRepository (in-memory storage)
│   ├── exfil/       # DataExfiltrator (fake server communication)
│   └── ui/          # LauncherActivity (tap game + control panel), SessionListActivity
├── defender/        # WcDonald's target app with defense mechanisms
│   ├── security/    # SecureTouchModifier, OverlayDetector, BiometricHelper, etc.
│   └── ui/          # MainActivity, LoginActivity, PaymentActivity, SettingsActivity
└── design/          # Pencil presentation files
```

### Two Separate APKs

The project builds two independent apps:

| App | Package | Purpose |
|-----|---------|---------|
| **GhostTouch** (Attacker) | `com.ghosttouch.attacker` | Overlay engine disguised as a tap game |
| **WcDonald's** (Defender) | `com.wcdonalds.app` | Target app with toggleable defenses |

---

## How the Attack Works

### Step 1: Disguise
The attacker app presents itself as a harmless tap-counter game ("Tap Rush"). This
gives the app a legitimate reason to exist on the device and request background execution.

### Step 2: Permission Acquisition
The app requests two permissions (both require manual user action in Settings):
- **SYSTEM_ALERT_WINDOW** — Draw over other apps
- **PACKAGE_USAGE_STATS** — Detect which app is in the foreground

### Step 3: Foreground Detection
A foreground service polls `UsageStatsManager.queryEvents()` every 700ms to detect
when the target app (`com.wcdonalds.app`) enters the foreground.

### Step 4: Overlay Display
When the target is detected, `WindowManager.addView()` displays a fullscreen overlay
using `TYPE_APPLICATION_OVERLAY`. The overlay is a pixel-perfect replica of the target
app's login or payment screen.

### Step 5: Credential Capture
User input goes to the overlay's Compose TextFields instead of the real app.
The captured data is stored in `SessionRepository`.

### Step 6: Fake Exfiltration
`DataExfiltrator` Base64-encodes the credentials, wraps them in an innocent-looking
JSON payload disguised as analytics telemetry, and logs it (no real network request):

```json
{
  "event": "app_session_metric",
  "client_id": "a8f3...",
  "payload": "eyJ1IjoiZXhhbXBsZUBlbWFpbC5jb20iLCJwIjoicGFzc3dvcmQxMjMifQ==",
  "ts": 1712678400,
  "version": "2.1.0"
}
```

The `payload` field contains the stolen data — decoded:
```json
{"u": "example@email.com", "p": "password123", "t": "com.wcdonalds.app"}
```

---

## Defense Mechanisms

All defenses are toggleable in WcDonald's Settings screen for demo comparison.

### 1. Filter Obscured Touches (MOST IMPORTANT)
```kotlin
// Custom Compose Modifier checks FLAG_WINDOW_IS_OBSCURED
Modifier.pointerInteropFilter { event ->
    if (event.flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED != 0) {
        // Block the touch — overlay detected
        true
    } else false
}
```
- Simple, effective, catches ALL overlay types
- The flag is set by the OS and cannot be spoofed

### 2. Overlay Detection
```kotlin
Settings.canDrawOverlays(context)  // Check own permission
PackageManager.getInstalledPackages()  // Scan for apps with SYSTEM_ALERT_WINDOW
```
Shows warning banner when potential overlay apps are detected.

### 3. FLAG_SECURE
```kotlin
window.setFlags(FLAG_SECURE, FLAG_SECURE)
```
Prevents screenshots and screen recording of sensitive screens.

### 4. Biometric Verification
```kotlin
BiometricPrompt.authenticate(promptInfo)
```
The system biometric dialog renders ABOVE overlay windows — overlays cannot cover it.

### 5. Focus Monitoring
```kotlin
override fun onWindowFocusChanged(hasFocus: Boolean) {
    if (!hasFocus) { /* Potential overlay activity */ }
}
```

### 6. UX Warning Banner
When any defense detects an overlay, a prominent red banner appears:
> "Screen interaction blocked for security reasons"

---

## Setup

### Prerequisites
- Android Studio (or command-line Android SDK)
- Android SDK API 26+ (minSdk)
- An emulator or device running Android 8.0+
- Java 21

### Build
```bash
# Set Android SDK path
export ANDROID_HOME=$HOME/Library/Android/sdk

# Build both APKs
./gradlew assembleDebug

# APK locations:
# attacker/build/outputs/apk/debug/attacker-debug.apk
# defender/build/outputs/apk/debug/defender-debug.apk
```

### Install
```bash
adb install attacker/build/outputs/apk/debug/attacker-debug.apk
adb install defender/build/outputs/apk/debug/defender-debug.apk
```

---

## Demo Walkthrough

### Demo 1 — Attack (Defenses OFF)
1. Open **WcDonald's** app > Settings > Disable all defenses
2. Open **GhostTouch** app > Control tab
3. Grant "Draw Over Apps" permission
4. Grant "Usage Access" permission
5. Select "Fake Login" mode
6. Tap "Start Overlay Service"
7. Press Home, open **WcDonald's**
8. Observe: Fake login screen appears over the real app
9. Enter test credentials and tap "Sign In"
10. Return to **GhostTouch** > Sessions tab
11. View captured credentials + encoded exfiltration payload

### Demo 2 — Defense (Defenses ON)
1. Open **WcDonald's** > Settings > Enable all defenses
2. Repeat the attack steps
3. Observe:
   - Input fields are disabled
   - Red warning banner appears
   - Buttons don't respond
   - Payment requires biometric auth (system prompt, overlay-proof)

### Demo 3 — Tapjacking
1. In **GhostTouch** > Control tab, select "Tapjacking" mode
2. Start the service
3. Open **WcDonald's** — overlay is invisible but present
4. With defenses ON: touches are still blocked (FLAG_WINDOW_IS_OBSCURED)

---

## File Reference

### Attacker — Key Files
| File | Description |
|------|-------------|
| `OverlayService.kt` | Foreground service: polls foreground app, triggers overlay |
| `ForegroundDetector.kt` | UsageStatsManager polling with OEM fallback |
| `OverlayManager.kt` | WindowManager lifecycle + ComposeView in service context |
| `FakeLoginOverlay.kt` | Pixel-perfect WcDonald's login screen replica |
| `FakePaymentOverlay.kt` | Fake payment form for card capture |
| `TapjackingOverlay.kt` | Invisible overlay for transparent tap interception |
| `DataExfiltrator.kt` | Base64 encoding + fake analytics payload disguise |
| `SessionRepository.kt` | In-memory captured session storage |

### Defender — Key Files
| File | Description |
|------|-------------|
| `SecureTouchModifier.kt` | Compose Modifier for FLAG_WINDOW_IS_OBSCURED filtering |
| `OverlayDetector.kt` | Scans installed apps for overlay permission |
| `SecureScreenHelper.kt` | FLAG_SECURE application helper |
| `BiometricHelper.kt` | AndroidX BiometricPrompt wrapper |
| `FocusMonitor.kt` | Window focus change detection |
| `OverlayWarningBanner.kt` | Red warning banner composable |
| `DefenseSettings` | Singleton with toggleable defense flags |

---

## Key Takeaway

> If your UI can be mimicked, your users can be tricked.
>
> `filterTouchesWhenObscured` is the single most effective defense against overlay attacks.
> It's simple, catches all overlay types, and the OS flag cannot be spoofed.
> Apply it to login, payment, and personal data forms.

---

## Disclaimer

This project is created exclusively for **educational and security awareness purposes**.
It demonstrates known Android security concepts that are publicly documented by Google.
Do NOT use this code for unauthorized access, credential theft, or any malicious activity.
The authors are not responsible for misuse of this code.
