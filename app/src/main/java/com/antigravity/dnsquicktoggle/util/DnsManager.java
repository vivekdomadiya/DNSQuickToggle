package com.antigravity.dnsquicktoggle.util;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.service.quicksettings.TileService;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.antigravity.dnsquicktoggle.service.DnsTileService;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class DnsManager {
    private static final String TAG = "DnsManager";

    // System Settings Global keys for Private DNS (Android 9+)
    public static final String SETTING_PRIVATE_DNS_MODE = "private_dns_mode";
    public static final String SETTING_PRIVATE_DNS_SPECIFIER = "private_dns_specifier";

    // Standard Android Private DNS mode values
    public static final String MODE_OFF = "off";
    public static final String MODE_OPPORTUNISTIC = "opportunistic"; // Automatic
    public static final String MODE_HOSTNAME = "hostname"; // Custom Provider

    // App Preferences
    private static final String PREFS_NAME = "dns_toggle_prefs";
    public static final String PREF_CUSTOM_HOSTNAME = "custom_hostname";
    public static final String PREF_PROVIDER_LABEL = "provider_label";
    public static final String PREF_TOGGLE_BEHAVIOR = "toggle_behavior";

    // Toggle behaviors
    public static final int BEHAVIOR_HOSTNAME_OFF = 0; // Hostname <-> Off
    public static final int BEHAVIOR_AUTO_OFF = 1;     // Automatic <-> Off
    public static final int BEHAVIOR_CYCLE_ALL = 2;    // Hostname -> Auto -> Off -> Hostname

    // Default Provider
    public static final String DEFAULT_HOSTNAME = "dns.adguard.com";
    public static final String DEFAULT_LABEL = "AdGuard";

    private final Context appContext;
    private final SharedPreferences prefs;

    public DnsManager(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Checks whether the app has the WRITE_SECURE_SETTINGS permission.
     */
    public boolean hasPermission() {
        return appContext.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) 
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Gets the current system Private DNS mode.
     * @return "off", "opportunistic", "hostname", or "unknown"
     */
    @NonNull
    public String getPrivateDnsMode() {
        try {
            String mode = Settings.Global.getString(appContext.getContentResolver(), SETTING_PRIVATE_DNS_MODE);
            if (mode == null) {
                return MODE_OPPORTUNISTIC; // Android default is usually opportunistic
            }
            return mode.toLowerCase();
        } catch (Exception e) {
            Log.e(TAG, "Error reading private_dns_mode", e);
            return MODE_OFF;
        }
    }

    /**
     * Gets the currently active Private DNS provider hostname.
     */
    @Nullable
    public String getPrivateDnsSpecifier() {
        try {
            return Settings.Global.getString(appContext.getContentResolver(), SETTING_PRIVATE_DNS_SPECIFIER);
        } catch (Exception e) {
            Log.e(TAG, "Error reading private_dns_specifier", e);
            return null;
        }
    }

    /**
     * Checks if Private DNS is currently enabled (either Hostname or Opportunistic).
     */
    public boolean isPrivateDnsActive() {
        String mode = getPrivateDnsMode();
        return !MODE_OFF.equalsIgnoreCase(mode);
    }

    /**
     * Gets the configured target hostname for "hostname" mode.
     */
    @NonNull
    public String getSavedHostname() {
        String saved = prefs.getString(PREF_CUSTOM_HOSTNAME, null);
        if (TextUtils.isEmpty(saved)) {
            // If user hasn't saved one yet, check if system already has a specifier set
            String currentSys = getPrivateDnsSpecifier();
            if (!TextUtils.isEmpty(currentSys)) {
                return currentSys;
            }
            return DEFAULT_HOSTNAME;
        }
        return saved;
    }

    /**
     * Saves user preferred hostname.
     */
    public void saveHostname(@NonNull String hostname) {
        prefs.edit().putString(PREF_CUSTOM_HOSTNAME, hostname.trim()).apply();
    }

    /**
     * Gets the friendly label for the active/target DNS provider.
     */
    @NonNull
    public String getSavedLabel() {
        String saved = prefs.getString(PREF_PROVIDER_LABEL, null);
        if (TextUtils.isEmpty(saved)) {
            return inferLabelFromHostname(getSavedHostname());
        }
        return saved;
    }

    /**
     * Saves user preferred label for the Quick Settings tile.
     */
    public void saveLabel(@NonNull String label) {
        prefs.edit().putString(PREF_PROVIDER_LABEL, label.trim()).apply();
    }

    /**
     * Saves both provider label and hostname.
     */
    public void saveProvider(@NonNull String label, @NonNull String hostname) {
        prefs.edit()
                .putString(PREF_PROVIDER_LABEL, label.trim())
                .putString(PREF_CUSTOM_HOSTNAME, hostname.trim())
                .apply();
    }

    /**
     * Infers a friendly label from a hostname when no label has been saved.
     */
    @NonNull
    public static String inferLabelFromHostname(@Nullable String hostname) {
        if (TextUtils.isEmpty(hostname)) {
            return DEFAULT_LABEL;
        }
        String lower = hostname.toLowerCase();
        if (lower.contains("adguard")) return "AdGuard";
        if (lower.contains("cloudflare") || lower.contains("1dot1dot1dot1") || lower.contains("one.one")) return "Cloudflare";
        if (lower.contains("google")) return "Google";
        if (lower.contains("quad9")) return "Quad9";
        if (lower.contains("mullvad")) return "Mullvad";
        if (lower.contains("nextdns")) return "NextDNS";
        if (lower.contains("controld")) return "ControlD";

        String[] parts = hostname.split("\\.");
        if (parts.length > 0 && !TextUtils.isEmpty(parts[0])) {
            return parts[0].substring(0, 1).toUpperCase() + parts[0].substring(1);
        }
        return hostname;
    }

    /**
     * Gets preferred toggle behavior (0: Hostname <-> Off, 1: Auto <-> Off, 2: Cycle All).
     */
    public int getToggleBehavior() {
        return prefs.getInt(PREF_TOGGLE_BEHAVIOR, BEHAVIOR_HOSTNAME_OFF);
    }

    /**
     * Saves preferred toggle behavior.
     */
    public void saveToggleBehavior(int behavior) {
        prefs.edit().putInt(PREF_TOGGLE_BEHAVIOR, behavior).apply();
    }

    /**
     * Sets private DNS mode and optionally provider hostname in Settings.Global.
     * Requires WRITE_SECURE_SETTINGS permission.
     */
    public boolean applyDnsMode(@NonNull String targetMode, @Nullable String targetHostname) {
        if (!hasPermission()) {
            Log.w(TAG, "Cannot apply DNS mode: WRITE_SECURE_SETTINGS not granted");
            return false;
        }

        try {
            if (MODE_HOSTNAME.equalsIgnoreCase(targetMode)) {
                String host = !TextUtils.isEmpty(targetHostname) ? targetHostname.trim() : getSavedHostname();
                Settings.Global.putString(appContext.getContentResolver(), SETTING_PRIVATE_DNS_SPECIFIER, host);
                Settings.Global.putString(appContext.getContentResolver(), SETTING_PRIVATE_DNS_MODE, MODE_HOSTNAME);
            } else if (MODE_OPPORTUNISTIC.equalsIgnoreCase(targetMode)) {
                Settings.Global.putString(appContext.getContentResolver(), SETTING_PRIVATE_DNS_MODE, MODE_OPPORTUNISTIC);
            } else {
                Settings.Global.putString(appContext.getContentResolver(), SETTING_PRIVATE_DNS_MODE, MODE_OFF);
            }

            requestTileUpdate();
            return true;
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while changing private DNS mode", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error applying DNS mode", e);
            return false;
        }
    }

    /**
     * Toggles Private DNS according to the user's configured behavior.
     * @return ToggleResult containing new status and whether change was successful.
     */
    public ToggleResult toggle() {
        if (!hasPermission()) {
            return new ToggleResult(false, getPrivateDnsMode(), getPrivateDnsSpecifier(), "Permission missing");
        }

        String currentMode = getPrivateDnsMode();
        int behavior = getToggleBehavior();
        String nextMode;
        String nextHostname = null;

        switch (behavior) {
            case BEHAVIOR_AUTO_OFF:
                // Auto <-> Off
                if (MODE_OFF.equalsIgnoreCase(currentMode)) {
                    nextMode = MODE_OPPORTUNISTIC;
                } else {
                    nextMode = MODE_OFF;
                }
                break;

            case BEHAVIOR_CYCLE_ALL:
                // Hostname -> Auto -> Off -> Hostname
                if (MODE_HOSTNAME.equalsIgnoreCase(currentMode)) {
                    nextMode = MODE_OPPORTUNISTIC;
                } else if (MODE_OPPORTUNISTIC.equalsIgnoreCase(currentMode)) {
                    nextMode = MODE_OFF;
                } else {
                    nextMode = MODE_HOSTNAME;
                    nextHostname = getSavedHostname();
                }
                break;

            case BEHAVIOR_HOSTNAME_OFF:
            default:
                // Hostname <-> Off
                if (MODE_OFF.equalsIgnoreCase(currentMode)) {
                    nextMode = MODE_HOSTNAME;
                    nextHostname = getSavedHostname();
                } else {
                    nextMode = MODE_OFF;
                }
                break;
        }

        boolean success = applyDnsMode(nextMode, nextHostname);
        String message;
        if (success) {
            if (MODE_OFF.equals(nextMode)) {
                message = "Private DNS turned OFF";
            } else if (MODE_HOSTNAME.equals(nextMode)) {
                message = "Private DNS: " + (nextHostname != null ? nextHostname : getSavedHostname());
            } else {
                message = "Private DNS set to Automatic";
            }
        } else {
            message = "Failed to toggle Private DNS";
        }

        return new ToggleResult(success, nextMode, nextHostname, message);
    }

    /**
     * Requests the System UI to update the Quick Settings tile immediately.
     */
    public void requestTileUpdate() {
        try {
            TileService.requestListeningState(
                    appContext,
                    new ComponentName(appContext, DnsTileService.class)
            );
        } catch (Exception e) {
            Log.d(TAG, "Could not request tile update: " + e.getMessage());
        }
    }

    /**
     * Checks if the device has a su (root) binary accessible.
     */
    public static boolean isRootAvailable() {
        String[] paths = {
                "/system/bin/su",
                "/system/xbin/su",
                "/sbin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/data/local/su"
        };
        for (String path : paths) {
            if (new File(path).exists()) {
                return true;
            }
        }
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"which", "su"});
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Tries to grant WRITE_SECURE_SETTINGS using root su shell.
     * @return true if permission was granted successfully.
     */
    public boolean grantPermissionWithRoot() {
        String packageName = appContext.getPackageName();
        String command = "pm grant " + packageName + " android.permission.WRITE_SECURE_SETTINGS";
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                requestTileUpdate();
                return hasPermission();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error running su command", e);
        }
        return false;
    }

    public static class ToggleResult {
        public final boolean success;
        public final String mode;
        public final String hostname;
        public final String message;

        public ToggleResult(boolean success, String mode, String hostname, String message) {
            this.success = success;
            this.mode = mode;
            this.hostname = hostname;
            this.message = message;
        }
    }
}
