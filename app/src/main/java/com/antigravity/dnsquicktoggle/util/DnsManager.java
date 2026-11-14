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

import com.antigravity.dnsquicktoggle.model.DnsProvider;
import com.antigravity.dnsquicktoggle.service.DnsTileService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
    public static final String PREF_CUSTOM_PROVIDERS_JSON = "custom_providers_json";

    // Toggle behaviors
    public static final int BEHAVIOR_HOSTNAME_OFF = 0; // Hostname <-> Off
    public static final int BEHAVIOR_AUTO_OFF = 1;     // Automatic <-> Off
    public static final int BEHAVIOR_CYCLE_ALL = 2;    // Hostname -> Auto -> Off -> Hostname

    // Default Provider
    public static final String DEFAULT_HOSTNAME = "dns.adguard.com";
    public static final String DEFAULT_LABEL = "AdGuard";

    // Built-in presets (not editable / not deletable)
    public static final List<DnsProvider> PRESET_PROVIDERS = Collections.unmodifiableList(Arrays.asList(
            new DnsProvider("preset_adguard", "AdGuard", "dns.adguard.com", true),
            new DnsProvider("preset_cloudflare", "Cloudflare", "1dot1dot1dot1.cloudflare-dns.com", true),
            new DnsProvider("preset_google", "Google DNS", "dns.google", true),
            new DnsProvider("preset_quad9", "Quad9", "dns.quad9.net", true),
            new DnsProvider("preset_mullvad", "Mullvad", "base.dns.mullvad.net", true)
    ));

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
        if (!TextUtils.isEmpty(saved)) {
            return saved;
        }
        String currentHost = getSavedHostname();
        for (DnsProvider p : getAllProviders()) {
            if (p.getHostname().equalsIgnoreCase(currentHost)) {
                return p.getName();
            }
        }
        return inferLabelFromHostname(currentHost);
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
     * Retrieves all custom user-defined DNS providers.
     */
    @NonNull
    public List<DnsProvider> getCustomProviders() {
        List<DnsProvider> list = new ArrayList<>();
        String jsonStr = prefs.getString(PREF_CUSTOM_PROVIDERS_JSON, null);
        if (TextUtils.isEmpty(jsonStr)) {
            return list;
        }
        try {
            JSONArray array = new JSONArray(jsonStr);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                DnsProvider provider = DnsProvider.fromJson(obj);
                if (provider != null) {
                    list.add(provider);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing custom providers", e);
        }
        return list;
    }

    /**
     * Persists the custom providers list.
     */
    public void saveCustomProviders(@NonNull List<DnsProvider> customProviders) {
        JSONArray array = new JSONArray();
        for (DnsProvider p : customProviders) {
            try {
                array.put(p.toJson());
            } catch (JSONException e) {
                Log.e(TAG, "Error serializing provider: " + p.getName(), e);
            }
        }
        prefs.edit().putString(PREF_CUSTOM_PROVIDERS_JSON, array.toString()).apply();
    }

    /**
     * Returns combined list of built-in presets and user custom providers.
     */
    @NonNull
    public List<DnsProvider> getAllProviders() {
        List<DnsProvider> all = new ArrayList<>(PRESET_PROVIDERS);
        all.addAll(getCustomProviders());
        return all;
    }

    /**
     * Sanitizes hostname by stripping protocol prefix (http://, https://, tls://),
     * paths, ports, and whitespace.
     */
    @NonNull
    public static String sanitizeHostname(@Nullable String hostname) {
        if (hostname == null) {
            return "";
        }
        String clean = hostname.trim().toLowerCase();
        if (clean.startsWith("https://")) clean = clean.substring(8);
        else if (clean.startsWith("http://")) clean = clean.substring(7);
        else if (clean.startsWith("tls://")) clean = clean.substring(6);

        // Remove trailing slashes or path
        int slashIdx = clean.indexOf('/');
        if (slashIdx != -1) {
            clean = clean.substring(0, slashIdx);
        }
        // Remove port if present
        int colonIdx = clean.indexOf(':');
        if (colonIdx != -1) {
            clean = clean.substring(0, colonIdx);
        }
        return clean.trim();
    }

    /**
     * Validates that a string is a syntactically valid DNS hostname.
     */
    public static boolean isValidHostname(@Nullable String hostname) {
        if (hostname == null) return false;
        String clean = sanitizeHostname(hostname);
        if (clean.isEmpty() || clean.length() > 253) {
            return false;
        }
        if (!clean.contains(".")) {
            return false;
        }
        String regex = "^(?=.{1,253}$)(?!-)[a-zA-Z0-9-]{1,63}(?<!-)(\\.(?!-)[a-zA-Z0-9-]{1,63}(?<!-))+$";
        return clean.matches(regex);
    }

    /**
     * Checks if a provider with the given label already exists (case-insensitive).
     * @param label The label to check
     * @param excludeId Optional provider ID to exclude (used when editing)
     */
    public boolean isLabelExists(@NonNull String label, @Nullable String excludeId) {
        String trimmed = label.trim();
        for (DnsProvider p : getAllProviders()) {
            if (excludeId != null && excludeId.equals(p.getId())) {
                continue;
            }
            if (p.getName().trim().equalsIgnoreCase(trimmed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a provider with the given hostname already exists (case-insensitive).
     * @param hostname The hostname to check
     * @param excludeId Optional provider ID to exclude (used when editing)
     */
    public boolean isHostnameExists(@NonNull String hostname, @Nullable String excludeId) {
        String clean = sanitizeHostname(hostname);
        for (DnsProvider p : getAllProviders()) {
            if (excludeId != null && excludeId.equals(p.getId())) {
                continue;
            }
            if (sanitizeHostname(p.getHostname()).equalsIgnoreCase(clean)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a new custom provider.
     */
    @NonNull
    public DnsProvider addCustomProvider(@NonNull String name, @NonNull String hostname) {
        List<DnsProvider> customList = getCustomProviders();
        String id = "custom_" + UUID.randomUUID().toString();
        String cleanHost = sanitizeHostname(hostname);
        DnsProvider newProvider = new DnsProvider(id, name.trim(), cleanHost, false);
        customList.add(newProvider);
        saveCustomProviders(customList);
        return newProvider;
    }

    /**
     * Updates an existing custom provider.
     */
    public boolean updateCustomProvider(@NonNull String id, @NonNull String newName, @NonNull String newHostname) {
        List<DnsProvider> customList = getCustomProviders();
        boolean found = false;
        String oldHost = null;
        String cleanHost = sanitizeHostname(newHostname);
        for (DnsProvider p : customList) {
            if (p.getId().equals(id)) {
                oldHost = p.getHostname();
                p.setName(newName.trim());
                p.setHostname(cleanHost);
                found = true;
                break;
            }
        }
        if (found) {
            saveCustomProviders(customList);
            if (oldHost != null && oldHost.equalsIgnoreCase(getSavedHostname())) {
                saveProvider(newName, cleanHost);
                if (MODE_HOSTNAME.equalsIgnoreCase(getPrivateDnsMode()) && hasPermission()) {
                    applyDnsMode(MODE_HOSTNAME, cleanHost);
                } else {
                    requestTileUpdate();
                }
            }
        }
        return found;
    }

    /**
     * Deletes a custom provider by ID.
     */
    public boolean deleteCustomProvider(@NonNull String id) {
        List<DnsProvider> customList = getCustomProviders();
        DnsProvider removed = null;
        for (int i = 0; i < customList.size(); i++) {
            if (customList.get(i).getId().equals(id)) {
                removed = customList.remove(i);
                break;
            }
        }
        if (removed != null) {
            saveCustomProviders(customList);
            if (removed.getHostname().equalsIgnoreCase(getSavedHostname())) {
                DnsProvider defaultPreset = PRESET_PROVIDERS.get(0);
                selectProvider(defaultPreset);
            }
            return true;
        }
        return false;
    }

    /**
     * Selects a provider as the active target, saves it, and applies it immediately if hostname mode is active.
     */
    public void selectProvider(@NonNull DnsProvider provider) {
        saveProvider(provider.getName(), provider.getHostname());
        if (MODE_HOSTNAME.equalsIgnoreCase(getPrivateDnsMode()) && hasPermission()) {
            applyDnsMode(MODE_HOSTNAME, provider.getHostname());
        } else {
            requestTileUpdate();
        }
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
