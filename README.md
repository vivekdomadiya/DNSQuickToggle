# DNS Quick Toggle

**DNS Quick Toggle** is a lightweight Android application and Quick Settings (QS) Tile that allows you to instantly toggle Android's **Private DNS** (DNS-over-TLS) on and off with a single tap from your notification shade.

---

## ✨ Features

- **⚡ Quick Settings Tile (`DnsTileService`)**:
  - Direct 1-tap toggle from your notification shade.
  - Live status indicator: Displays active DNS provider hostname (e.g. `dns.adguard.com`) or `Off` in the tile subtitle.
  - Custom vector icons for active vs. inactive states.
  - Automatically collapses shade and prompts if permissions are missing.

- **🔄 Real-Time System Synchronization (`ContentObserver`)**:
  - Automatically updates the Quick Tile and Dashboard whenever Private DNS settings are changed in Android System Settings or by another app.

- **🛡️ Popular DNS Provider Presets**:
  - **AdGuard DNS** (`dns.adguard.com`): Blocks ads and tracking domains system-wide.
  - **Cloudflare 1.1.1.1** (`1dot1dot1dot1.cloudflare-dns.com`): High speed and privacy.
  - **Google DNS** (`dns.google`): Reliable global DNS.
  - **Quad9** (`dns.quad9.net`): Blocks malicious domains and phishing.
  - **Mullvad** (`base.dns.mullvad.net`): Privacy-oriented ad-blocking DNS.
  - **Custom Hostname Support**: Supports any DNS-over-TLS endpoint (e.g. your NextDNS profile `xxx.dns.nextdns.io` or personal AdGuard Home).

- **🎛️ Configurable Toggle Behaviors**:
  1. **Provider ↔ Off** *(Recommended)*: Toggles between your configured DNS provider and disabled.
  2. **Automatic ↔ Off**: Toggles between Android's default opportunistic DNS and disabled.
  3. **3-Way Cycle**: Cycles through Provider → Automatic → Off.

- **🔑 Effortless Permission Setup (`WRITE_SECURE_SETTINGS`)**:
  - **1-Click Root Grant**: If your device has root access (`su`), tap the in-app button to grant permission instantly without a PC!
  - **One-Tap ADB Copy**: Easily copy the exact `pm grant` command to paste in ADB.
  - **Android 13+ Add-Tile Prompt**: Prompts the native system dialog to add the tile with 1 tap using `StatusBarManager.requestAddTileService`.

---

## 🔒 Permission Requirement

Android treats system-wide DNS configuration as a protected setting. Changing `Settings.Global.private_dns_mode` requires the `WRITE_SECURE_SETTINGS` permission.

### How to Grant Permission:

#### Method 1: In-App Root Grant (No PC required)
If your phone is rooted (Magisk / KernelSU / APatch):
1. Open **DNS Quick Toggle**.
2. Tap **Grant with Root (su)**.
3. Grant root privileges when prompted.

#### Method 2: Via ADB (PC / Mac / Linux)
For the app to work properly, you'll need to provide it permissions via ADB:

1. **Download Platform Tools**: Get the official Android SDK Platform-Tools from Google [here](https://developer.android.com/tools/releases/platform-tools).
2. **Extract & Open Terminal**:
   - Extract the downloaded zip file.
   - Open a terminal / command prompt inside the extracted `platform-tools` folder:
     - **Windows**: Hold <kbd>Shift</kbd>, right-click inside the folder, and select *Open in Terminal* or *Open PowerShell window here*.
     - **macOS / Linux**: Open Terminal and run `cd /path/to/platform-tools`.
3. **Turn on USB Debugging on your phone**:
   - Go to **Settings** &rarr; **About phone** and tap **Build number** 7 times to unlock Developer options.
   - Go to **Settings** &rarr; **System** &rarr; **Developer options** and enable **USB debugging**.
   - *(Note for Xiaomi / MIUI / HyperOS users: You must also enable **USB debugging (Security settings)**).*
4. **Connect your phone to your PC** using a USB cable (and tap *Allow* on the authorization popup on your phone).
5. **Run this command in the terminal**:

   ```bash
   adb shell pm grant com.vivekdomadiya.dnsquicktoggle android.permission.WRITE_SECURE_SETTINGS
   ```
   *(Note: On macOS or Linux, use `./adb` if platform-tools is not in your system PATH).*

6. That's it! The permission will be granted immediately without needing a reboot.

---

## 🛠️ Building & Installing

### Automated Install Script
From this directory, run:
```bash
./install.sh
```
This builds the APK and automatically copies it to `/sdcard/Download/DNSQuickToggle.<version>.apk` (or installs via ADB / Root if connected).

### Manual Build
```bash
./gradlew assembleDebug
```
The resulting APK is generated at:
`app/build/outputs/apk/debug/DNSQuickToggle.<version>.apk`

---

## 📱 Quick Settings Setup

1. Swipe down twice from the top of your screen to open Quick Settings.
2. Tap the **Edit (pencil)** button.
3. Find the **Private DNS** tile in the inactive list and drag it into your active tiles.
*(On Android 13+, you can also tap "Add Tile to Quick Settings" inside the app).*

---

## 📂 Project Structure

```
dns-quick-toggle/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/vivekdomadiya/dnsquicktoggle/
│   │   │   ├── model/
│   │   │   ├── service/
│   │   │   │   └── DnsTileService.java       # Quick Settings Tile service
│   │   │   ├── ui/
│   │   │   │   └── MainActivity.java         # Main management UI
│   │   │   └── util/
│   │   │       └── DnsManager.java           # Settings.Global controller & root helper
│   │   └── res/
│   │       ├── drawable/                      # Vector icons & tile graphics
│   │       ├── layout/
│   │       │   └── activity_main.xml         # Modern Material 3 dashboard layout
│   │       └── values/                        # Strings, colors, and Material 3 themes
│   ├── build.gradle
│   └── proguard-rules.pro
├── build.gradle
├── settings.gradle
├── gradle.properties
├── local.properties
├── install.sh                                # Automated build & install helper
└── README.md
```
