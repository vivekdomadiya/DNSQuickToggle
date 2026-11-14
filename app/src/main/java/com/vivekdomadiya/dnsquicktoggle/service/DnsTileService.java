package com.vivekdomadiya.dnsquicktoggle.service;

import android.app.PendingIntent;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.vivekdomadiya.dnsquicktoggle.R;
import com.vivekdomadiya.dnsquicktoggle.model.DnsProvider;
import com.vivekdomadiya.dnsquicktoggle.ui.MainActivity;
import com.vivekdomadiya.dnsquicktoggle.util.DnsManager;

public class DnsTileService extends TileService {
    private static final String TAG = "DnsTileService";

    private DnsManager dnsManager;
    private ContentObserver dnsSettingsObserver;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Toast currentToast;

    @Override
    public void onCreate() {
        super.onCreate();
        dnsManager = new DnsManager(this);
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        if (dnsManager == null) {
            dnsManager = new DnsManager(this);
        }
        dnsManager.setTileAdded(true);
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
        if (dnsManager == null) {
            dnsManager = new DnsManager(this);
        }
        dnsManager.setTileAdded(false);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        if (dnsManager == null) {
            dnsManager = new DnsManager(this);
        }
        dnsManager.setTileAdded(true);
        updateTileState();
        registerSettingsObserver();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        unregisterSettingsObserver();
    }

    @Override
    public void onClick() {
        super.onClick();

        if (dnsManager == null) {
            dnsManager = new DnsManager(this);
        }

        if (!dnsManager.hasPermission()) {
            showToast(getString(R.string.permission_required_toast));
            launchMainActivity();
            return;
        }

        // Permission granted, execute toggle
        DnsManager.ToggleResult result = dnsManager.toggle();
        updateTileState();

        if (!TextUtils.isEmpty(result.message)) {
            showToast(result.message);
        }
    }

    private void showToast(String message) {
        if (currentToast != null) {
            currentToast.cancel();
        }
        currentToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        currentToast.show();
    }

    /**
     * Updates the Quick Settings Tile UI according to current DNS mode.
     */
    public void updateTileState() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }

        if (dnsManager == null) {
            dnsManager = new DnsManager(this);
        }

        if (!dnsManager.hasPermission()) {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setLabel(getString(R.string.tile_permission_needed));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.setSubtitle(getString(R.string.tile_permission_needed));
            }
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_dns_tile_off));
            tile.updateTile();
            return;
        }

        String mode = dnsManager.getPrivateDnsMode();
        switch (mode) {
            case DnsManager.MODE_HOSTNAME:
                tile.setState(Tile.STATE_ACTIVE);
                String host = dnsManager.getPrivateDnsSpecifier();
                if (TextUtils.isEmpty(host)) {
                    host = dnsManager.getSavedHostname();
                }
                // If in our list: display friendly name. If NOT in our list: display hostname as label!
                DnsProvider provider = dnsManager.findProviderByHostname(host);
                String label;
                if (provider != null) {
                    label = provider.getName();
                } else {
                    label = !TextUtils.isEmpty(host) ? host : getString(R.string.tile_label);
                }
                tile.setLabel(label);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.setSubtitle(host);
                }
                tile.setIcon(Icon.createWithResource(this, R.drawable.ic_dns_tile_on));
                break;

            case DnsManager.MODE_OPPORTUNISTIC:
                tile.setState(Tile.STATE_ACTIVE);
                tile.setLabel(getString(R.string.dns_mode_auto));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.setSubtitle(getString(R.string.dns_mode_auto));
                }
                tile.setIcon(Icon.createWithResource(this, R.drawable.ic_dns_tile_on));
                break;

            case DnsManager.MODE_OFF:
            default:
                tile.setState(Tile.STATE_INACTIVE);
                tile.setLabel(getString(R.string.tile_label));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.setSubtitle(getString(R.string.dns_status_off));
                }
                tile.setIcon(Icon.createWithResource(this, R.drawable.ic_dns_tile_off));
                break;
        }

        tile.updateTile();
    }

    /**
     * Opens MainActivity and collapses the quick settings shade.
     */
    private void launchMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14 (API 34)
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            startActivityAndCollapse(pendingIntent);
        } else {
            // Android 13 and below
            try {
                startActivityAndCollapse(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting activity and collapse", e);
                startActivity(intent);
            }
        }
    }

    private void registerSettingsObserver() {
        if (dnsSettingsObserver != null) {
            return;
        }

        dnsSettingsObserver = new ContentObserver(mainHandler) {
            @Override
            public void onChange(boolean selfChange, @NonNull Uri uri) {
                super.onChange(selfChange, uri);
                updateTileState();
            }
        };

        try {
            Uri modeUri = Settings.Global.getUriFor(DnsManager.SETTING_PRIVATE_DNS_MODE);
            Uri specUri = Settings.Global.getUriFor(DnsManager.SETTING_PRIVATE_DNS_SPECIFIER);

            if (modeUri != null) {
                getContentResolver().registerContentObserver(modeUri, false, dnsSettingsObserver);
            }
            if (specUri != null) {
                getContentResolver().registerContentObserver(specUri, false, dnsSettingsObserver);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to register ContentObserver", e);
        }
    }

    private void unregisterSettingsObserver() {
        if (dnsSettingsObserver != null) {
            try {
                getContentResolver().unregisterContentObserver(dnsSettingsObserver);
            } catch (Exception ignored) {
            }
            dnsSettingsObserver = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterSettingsObserver();
    }
}
