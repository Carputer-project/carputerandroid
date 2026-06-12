# CarPuter Android

Android car computer dashboard application.

## Features

- **Speed Display** – Large, colour-coded GPS speedometer (green / yellow / red)
- **Unit Toggle** – Tap "km/h" or "mph" label to switch units
- **Max Speed Tracker** – Tracks trip maximum speed; long-press to reset
- **Real-Time Clock** – Shows current time and date
- **GPS Status** – Shows GPS accuracy in metres
- **Always-On Screen** – Keeps the display on while driving
- **Immersive Fullscreen** – Hides navigation and status bars
- **Landscape Orientation** – Optimised for dashboard mounting

## Requirements

- Android 8.0 (API 26) or higher
- GPS / Location permission

## Building

1. Open in Android Studio, or build from the command line:

```bash
./gradlew assembleDebug
```

2. Install on device:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Permissions

| Permission | Purpose |
|---|---|
| `ACCESS_FINE_LOCATION` | GPS speed reading |
| `ACCESS_COARSE_LOCATION` | Fallback location |
