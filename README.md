# DNS Quick Toggle

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="DNS Quick Toggle Logo" width="100" height="100" />
</p>

<p align="center">
  <strong>Fast, seamless Private DNS Quick Settings tile switcher for Android 9+</strong>
</p>

<p align="center">
  <a href="https://github.com/vivekdomadiya/DNSQuickToggle/releases/latest">
    <img src="https://img.shields.io/github/v/release/vivekdomadiya/DNSQuickToggle?style=for-the-badge&logo=github&color=2563eb" alt="Latest Release" />
  </a>
  <a href="https://github.com/vivekdomadiya/DNSQuickToggle/releases">
    <img src="https://img.shields.io/github/downloads/vivekdomadiya/DNSQuickToggle/total?style=for-the-badge&color=10b981" alt="Total Downloads" />
  </a>
  <img src="https://img.shields.io/badge/Android-9.0%2B%20(API%2028%2B)-34d399?style=for-the-badge&logo=android&logoColor=white" alt="Android 9+" />
</p>

---

## ⚡ What is DNS Quick Toggle?

**DNS Quick Toggle** is a lightweight, open-source Android utility that lets you toggle and cycle your **Private DNS (DNS-over-TLS / DoT)** directly from your Android Quick Settings notification shade with a single tap.

Easily turn on ad-blocking DNS servers (like AdGuard or NextDNS) when browsing or gaming, switch to high-speed privacy resolvers (like Cloudflare or Quad9), or turn Private DNS off when connected to captive portals—all without navigating through Android's deep system settings menus.

---

## 📥 Download

Get the latest signed, installable APK directly from GitHub Releases:

<p align="center">
  <a href="https://github.com/vivekdomadiya/DNSQuickToggle/releases/latest">
    <img src="https://img.shields.io/badge/Download-Latest_APK-blue?style=for-the-badge&logo=android&logoColor=white" height="40" alt="Download APK" />
  </a>
</p>

- **Supported Android Versions:** Android 9.0 (Pie) up to Android 15+ (API 28 - 35)
- **Root Status:** **Not required** (works on all non-rooted devices via a one-time ADB command).

---

## ✨ Features

- ⚡ **One-Tap Quick Settings Tile:**
  - Toggle Private DNS on/off or cycle through your favorite DNS providers directly from your notification panel.
  - Real-time tile subtitle shows your active DNS hostname (e.g. `AdGuard`, `1.1.1.1`, or `Off`).

- 🛡️ **Built-in Popular DNS Presets:**
  - **AdGuard DNS** (`dns.adguard.com`) — System-wide ad and tracker blocking.
  - **Cloudflare 1.1.1.1** (`1dot1dot1dot1.cloudflare-dns.com`) — Ultra-fast DNS with strict privacy.
  - **Google Public DNS** (`dns.google`) — Fast and globally distributed resolver.
  - **Quad9** (`dns.quad9.net`) — Threat-blocking DNS with malicious domain filtering.
  - **Mullvad** (`base.dns.mullvad.net`) — Privacy-oriented ad-blocking DNS.
  - **Control D** (`p2.freedns.controld.com`) — Multi-engine ad-blocking DNS.

- ✏️ **Custom DNS Providers:**
  - Add, edit, and delete any custom DNS-over-TLS (DoT) provider hostname (e.g. your personal **NextDNS** ID `xxx.dns.nextdns.io`, **Pi-hole**, or self-hosted **AdGuard Home**).
  - Built-in validation prevents duplicates, auto-sanitizes ports and URLs, and verifies hostname formatting.

- 🔄 **Real-Time System Sync:**
  - If you change Private DNS in Android System Settings, the app and Quick Settings tile detect the external change instantly and update their status.
  - Displays a convenient `+ Add to DNS list` button if your active system DNS is not yet in your preset list.

- 🔒 **100% Private & Open Source:**
  - **Zero analytics, zero trackers, zero ads.**
  - The app requires **no internet permission** (`android.permission.INTERNET` is not even declared). It operates entirely locally on your device.

---

## 🔑 How to Grant Permission

Android treats system-wide Private DNS configuration as a protected setting. To change it from a Quick Settings tile, Android requires the `WRITE_SECURE_SETTINGS` permission.

### Granting via ADB (One-Time Setup)

You only need to perform this **once** after installing the app. It does not require root and takes less than 2 minutes:

1. **Enable Developer Options on your phone:**
   - Open **Settings** &rarr; **About Phone**.
   - Tap **Build Number** 7 times until you see *"You are now a developer!"*.
2. **Enable USB Debugging:**
   - Go to **Settings** &rarr; **System** &rarr; **Developer Options**.
   - Turn on **USB Debugging**.
   - *(Xiaomi / MIUI / HyperOS users: Also enable **USB debugging (Security settings)**).*
3. **Connect your phone to your PC / Mac:**
   - Connect using a USB cable (or Wireless Debugging).
   - Download Google's [Platform Tools (ADB)](https://developer.android.com/tools/releases/platform-tools) if you don't have it installed.
4. **Run this single command in terminal / command prompt:**

   ```bash
   adb shell pm grant com.vivekdomadiya.dnsquicktoggle android.permission.WRITE_SECURE_SETTINGS
   ```

   *(Rooted users can also simply execute `su -c pm grant com.vivekdomadiya.dnsquicktoggle android.permission.WRITE_SECURE_SETTINGS` in Termux or an Android terminal app).*

5. **Done!** Open DNS Quick Toggle on your phone; the permission warning will disappear and the tile is fully active!

---

## 📱 How to Add the Quick Settings Tile

1. Pull down your notification shade twice to open the full Quick Settings panel.
2. Tap the **Pencil (Edit)** icon at the bottom or top corner.
3. Scroll down to find the **DNS Quick Toggle** tile.
4. Drag and drop it into your active tiles section.
5. *(Android 13+ only)*: You can also tap the **"Add Tile to Quick Settings"** button directly inside the app.

---

## ❓ Frequently Asked Questions (FAQ)

<details>
<summary><strong>Why is the <code>WRITE_SECURE_SETTINGS</code> permission required?</strong></summary>
Android does not allow standard third-party apps to change system-wide DNS settings without explicit elevated permission. Granting this permission allows the tile to toggle between Opportunistic, Strict, and Off modes on your behalf.
</details>

<details>
<summary><strong>Does this app run in the background or drain battery?</strong></summary>
No. The app does not run any persistent background service or battery-draining daemon. It only updates its state when you tap the Quick Settings tile or when Android broadcasts an external DNS setting change.
</details>

<details>
<summary><strong>Does this app route my traffic through a VPN?</strong></summary>
No. DNS Quick Toggle does <strong>not</strong> use the Android VPN API. It configures Android's native, built-in Private DNS (DNS-over-TLS) feature at the operating system level, which uses hardware-accelerated TLS encryption and leaves your VPN slot free.
</details>

<details>
<summary><strong>Can I use my personal NextDNS or Pi-hole configuration?</strong></summary>
Yes! Tap the floating <strong>+</strong> button in the app, type a label (e.g. <code>My NextDNS</code>), enter your unique DoT endpoint (e.g. <code>xxxxxx.dns.nextdns.io</code>), and save it. It will immediately appear in your list and can be selected on the tile.
</details>

---

## 📄 License

This project is open-source under the [MIT License](LICENSE). Contributions, bug reports, and feature requests are welcome!
