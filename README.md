# Carputer Android

Android companion app for the Carputer dashboard system. Connects to ESP32 sensor module and body controller over WiFi to display vehicle data, control HVAC/locks/windows, play music, record dashcam, and more.

## Features

### Dashboard (DASH)
- Analog speed & RPM gauges
- Mini gauges: fuel, TPS, MAP, oil pressure, coolant temp
- Center panel cycles: Now Playing / Trip Computer / Performance
- Shift advice indicator
- Quick controls: HVAC, fan, AC, door locks, windows, remote start
- Status bar with door indicators, alerts, health score
- Warning popup for critical alerts

### Media Player (MEDIA)
- Play music from phone storage (mp3, flac, wav, aac, m4a, wma)
- ExoPlayer with seek, shuffle, repeat
- Spectrum analyzer (32 bars)
- Playlist and album browsing
- Volume control

### Rear Camera (REAR)
- Live camera preview via Camera2 API
- Connect/disconnect toggle

### Dashcam (DVR)
- H.264 + AAC video recording to MP4
- Video library with playback
- Camera selection

### Settings (SETUP)
- **Theme**: 7 color themes with gauge border customization
- **WiFi**: Scan & connect to Carputer_ECU network
- **DTC**: Diagnostic trouble code scanning with history
- **System**: Version, uptime, system load, diagnostics report
- **Logging**: Sensor data CSV logging
- **Flash**: ESP32 firmware flasher over USB serial

## Connections

| Service | Protocol | Address | Purpose |
|---------|----------|---------|---------|
| SensorClient | UDP | 192.168.4.3:5001 | Sensor data from ESP32 sensor module |
| CarControlClient | TCP | 192.168.4.1:5000 | Body controller commands & status |
| DtcService | UDP | 192.168.4.20:5001 | DTC scan commands |

## Build

```bash
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleDebug
```

APK at `app/build/outputs/apk/debug/app-debug.apk`

## Requirements

- Android 8.0+ (API 26)
- WiFi connection to Carputer_ECU network
- USB OTG for ESP32 firmware flashing (optional)

## Permissions

- Internet (WiFi UDP/TCP)
- Camera (rear view, dashcam)
- Audio recording (dashcam)
- Location (WiFi scanning)
- Notifications (foreground service)
- Boot completed (auto-start)
- USB peripheral (ESP32 flashing)

## Related Repositories

| Repo | Role |
|------|------|
| [esp32_sensor_module](https://github.com/carputer-project/esp32_sensor_module) | Sends engine sensor data via UDP (192.168.4.20 → 192.168.4.3:5001) |
| [esp32_body_controller](https://github.com/carputer-project/esp32_body_controller) | Receives relay/audio commands via TCP (192.168.4.1:5000) |
| [carputer-pcb](https://github.com/carputer-project/carputer-pcb) | Physical PCB design hosting both ESP32 modules |
| [carputer](https://github.com/carputer-project/carputer) | Qt5 desktop head unit (alternative to this Android app) |
