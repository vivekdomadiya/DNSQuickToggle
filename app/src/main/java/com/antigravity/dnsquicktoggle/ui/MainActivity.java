package com.antigravity.dnsquicktoggle.ui;

import android.app.StatusBarManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.antigravity.dnsquicktoggle.R;
import com.antigravity.dnsquicktoggle.service.DnsTileService;
import com.antigravity.dnsquicktoggle.util.DnsManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private DnsManager dnsManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private ContentObserver contentObserver;

    // UI references
    private MaterialCardView cardPermission;
    private ImageView ivPermIcon;
    private TextView tvPermTitle;
    private TextView tvPermDesc;
    private LinearLayout layoutAdbCommand;
    private TextView tvAdbCommand;
    private MaterialButton btnCopyAdb;
    private MaterialButton btnGrantRoot;
    private MaterialButton btnRefreshPerm;

    private TextView tvStatusBadge;
    private TextView tvCurrentMode;
    private TextView tvCurrentLabel;
    private TextView tvCurrentHost;
    private MaterialButton btnToggleDns;

    private MaterialButton btnAddTile;

    private ChipGroup chipGroupPresets;
    private Chip chipAdGuard;
    private Chip chipCloudflare;
    private Chip chipGoogle;
    private Chip chipQuad9;
    private Chip chipMullvad;

    private TextInputLayout tilProviderLabel;
    private TextInputEditText etProviderLabel;
    private TextInputLayout tilHostname;
    private TextInputEditText etHostname;
    private MaterialButton btnSaveHost;

    private RadioGroup rgBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dnsManager = new DnsManager(this);

        initViews();
        setupListeners();
        setupPresets();
        setupToggleBehaviors();
    }

    private void initViews() {
        cardPermission = findViewById(R.id.cardPermission);
        ivPermIcon = findViewById(R.id.ivPermIcon);
        tvPermTitle = findViewById(R.id.tvPermTitle);
        tvPermDesc = findViewById(R.id.tvPermDesc);
        layoutAdbCommand = findViewById(R.id.layoutAdbCommand);
        tvAdbCommand = findViewById(R.id.tvAdbCommand);
        btnCopyAdb = findViewById(R.id.btnCopyAdb);
        btnGrantRoot = findViewById(R.id.btnGrantRoot);
        btnRefreshPerm = findViewById(R.id.btnRefreshPerm);

        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        tvCurrentMode = findViewById(R.id.tvCurrentMode);
        tvCurrentLabel = findViewById(R.id.tvCurrentLabel);
        tvCurrentHost = findViewById(R.id.tvCurrentHost);
        btnToggleDns = findViewById(R.id.btnToggleDns);

        btnAddTile = findViewById(R.id.btnAddTile);

        chipGroupPresets = findViewById(R.id.chipGroupPresets);
        chipAdGuard = findViewById(R.id.chipAdGuard);
        chipCloudflare = findViewById(R.id.chipCloudflare);
        chipGoogle = findViewById(R.id.chipGoogle);
        chipQuad9 = findViewById(R.id.chipQuad9);
        chipMullvad = findViewById(R.id.chipMullvad);

        tilProviderLabel = findViewById(R.id.tilProviderLabel);
        etProviderLabel = findViewById(R.id.etProviderLabel);
        tilHostname = findViewById(R.id.tilHostname);
        etHostname = findViewById(R.id.etHostname);
        btnSaveHost = findViewById(R.id.btnSaveHost);

        rgBehavior = findViewById(R.id.rgBehavior);

        // Prepopulate saved label and hostname
        etProviderLabel.setText(dnsManager.getSavedLabel());
        etHostname.setText(dnsManager.getSavedHostname());
    }

    private void setupListeners() {
        btnToggleDns.setOnClickListener(v -> {
            if (!dnsManager.hasPermission()) {
                Toast.makeText(this, R.string.permission_required_toast, Toast.LENGTH_SHORT).show();
                return;
            }
            DnsManager.ToggleResult result = dnsManager.toggle();
            Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show();
            refreshUI();
        });

        btnCopyAdb.setOnClickListener(v -> {
            String command = tvAdbCommand.getText().toString();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("ADB Command", command);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, R.string.adb_copied_toast, Toast.LENGTH_SHORT).show();
            }
        });

        btnGrantRoot.setOnClickListener(v -> {
            btnGrantRoot.setEnabled(false);
            executorService.execute(() -> {
                boolean granted = dnsManager.grantPermissionWithRoot();
                mainHandler.post(() -> {
                    btnGrantRoot.setEnabled(true);
                    if (granted) {
                        Toast.makeText(this, R.string.root_success_toast, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, R.string.root_failed_toast, Toast.LENGTH_LONG).show();
                    }
                    refreshUI();
                });
            });
        });

        btnRefreshPerm.setOnClickListener(v -> {
            refreshUI();
            Toast.makeText(this, dnsManager.hasPermission() ? R.string.permission_granted : R.string.permission_missing, Toast.LENGTH_SHORT).show();
        });

        btnAddTile.setOnClickListener(v -> handleAddQuickTile());

        btnSaveHost.setOnClickListener(v -> {
            String newLabel = etProviderLabel.getText() != null ? etProviderLabel.getText().toString().trim() : "";
            String newHost = etHostname.getText() != null ? etHostname.getText().toString().trim() : "";

            if (TextUtils.isEmpty(newLabel)) {
                tilProviderLabel.setError(getString(R.string.label_invalid_toast));
                return;
            }
            tilProviderLabel.setError(null);

            if (TextUtils.isEmpty(newHost) || !newHost.contains(".")) {
                tilHostname.setError(getString(R.string.host_invalid_toast));
                return;
            }
            tilHostname.setError(null);

            dnsManager.saveProvider(newLabel, newHost);

            // If hostname mode is currently active, apply the new specifier immediately
            if (DnsManager.MODE_HOSTNAME.equalsIgnoreCase(dnsManager.getPrivateDnsMode()) && dnsManager.hasPermission()) {
                dnsManager.applyDnsMode(DnsManager.MODE_HOSTNAME, newHost);
            } else {
                dnsManager.requestTileUpdate();
            }

            Toast.makeText(this, R.string.host_saved_toast, Toast.LENGTH_SHORT).show();
            refreshUI();
        });
    }

    private void setupPresets() {
        chipAdGuard.setOnClickListener(v -> selectPreset("AdGuard", "dns.adguard.com"));
        chipCloudflare.setOnClickListener(v -> selectPreset("Cloudflare", "1dot1dot1dot1.cloudflare-dns.com"));
        chipGoogle.setOnClickListener(v -> selectPreset("Google DNS", "dns.google"));
        chipQuad9.setOnClickListener(v -> selectPreset("Quad9", "dns.quad9.net"));
        chipMullvad.setOnClickListener(v -> selectPreset("Mullvad", "base.dns.mullvad.net"));
    }

    private void selectPreset(String label, String hostname) {
        etProviderLabel.setText(label);
        etHostname.setText(hostname);
        dnsManager.saveProvider(label, hostname);
        if (DnsManager.MODE_HOSTNAME.equalsIgnoreCase(dnsManager.getPrivateDnsMode()) && dnsManager.hasPermission()) {
            dnsManager.applyDnsMode(DnsManager.MODE_HOSTNAME, hostname);
        } else {
            dnsManager.requestTileUpdate();
        }
        Toast.makeText(this, label + ": " + hostname, Toast.LENGTH_SHORT).show();
        refreshUI();
    }

    private void setupToggleBehaviors() {
        int behavior = dnsManager.getToggleBehavior();
        switch (behavior) {
            case DnsManager.BEHAVIOR_AUTO_OFF:
                rgBehavior.check(R.id.rbAutoOff);
                break;
            case DnsManager.BEHAVIOR_CYCLE_ALL:
                rgBehavior.check(R.id.rbCycleAll);
                break;
            case DnsManager.BEHAVIOR_HOSTNAME_OFF:
            default:
                rgBehavior.check(R.id.rbHostnameOff);
                break;
        }

        rgBehavior.setOnCheckedChangeListener((group, checkedId) -> {
            int selectedBehavior = DnsManager.BEHAVIOR_HOSTNAME_OFF;
            if (checkedId == R.id.rbAutoOff) {
                selectedBehavior = DnsManager.BEHAVIOR_AUTO_OFF;
            } else if (checkedId == R.id.rbCycleAll) {
                selectedBehavior = DnsManager.BEHAVIOR_CYCLE_ALL;
            }
            dnsManager.saveToggleBehavior(selectedBehavior);
        });
    }

    private void handleAddQuickTile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+ (API 33)
            StatusBarManager statusBarManager = getSystemService(StatusBarManager.class);
            if (statusBarManager != null) {
                ComponentName componentName = new ComponentName(this, DnsTileService.class);
                statusBarManager.requestAddTileService(
                        componentName,
                        getString(R.string.tile_label),
                        Icon.createWithResource(this, R.drawable.ic_dns_tile),
                        getMainExecutor(),
                        resultCode -> {
                            if (resultCode == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) {
                                Toast.makeText(this, R.string.tile_added_success, Toast.LENGTH_SHORT).show();
                            } else if (resultCode == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                                Toast.makeText(this, R.string.tile_already_added, Toast.LENGTH_SHORT).show();
                            }
                        }
                );
                return;
            }
        }
        // Fallback for older versions: Toast guide
        Toast.makeText(this, "Swipe down Quick Settings and tap Edit (pencil) to add 'Private DNS'.", Toast.LENGTH_LONG).show();
    }

    private void refreshUI() {
        boolean hasPerm = dnsManager.hasPermission();
        String mode = dnsManager.getPrivateDnsMode();
        String currentSpecifier = dnsManager.getPrivateDnsSpecifier();
        boolean isActive = dnsManager.isPrivateDnsActive();

        // 1. Permission Card: Shown at TOP only if permission is NOT granted
        if (hasPerm) {
            cardPermission.setVisibility(View.GONE);
        } else {
            cardPermission.setVisibility(View.VISIBLE);
            ivPermIcon.setImageResource(R.drawable.ic_warning);
            tvPermTitle.setText(R.string.permission_missing);
            tvPermTitle.setTextColor(Color.parseColor("#C62828"));
            tvPermDesc.setText(R.string.permission_missing_desc);
            layoutAdbCommand.setVisibility(View.VISIBLE);
            btnRefreshPerm.setVisibility(View.VISIBLE);

            // Show root option ONLY if device has su/root available
            boolean rootAvailable = DnsManager.isRootAvailable();
            btnGrantRoot.setVisibility(rootAvailable ? View.VISIBLE : View.GONE);
        }

        // 2. Status Card
        if (isActive) {
            tvStatusBadge.setText(R.string.status_active);
            setBadgeColor(tvStatusBadge, Color.parseColor("#2E7D32")); // Green
            if (DnsManager.MODE_HOSTNAME.equalsIgnoreCase(mode)) {
                tvCurrentMode.setText(getString(R.string.mode_label, getString(R.string.dns_mode_hostname)));

                String label = dnsManager.getSavedLabel();
                tvCurrentLabel.setText(getString(R.string.label_display, label));
                tvCurrentLabel.setVisibility(View.VISIBLE);

                String host = !TextUtils.isEmpty(currentSpecifier) ? currentSpecifier : dnsManager.getSavedHostname();
                tvCurrentHost.setText(getString(R.string.hostname_display, host));
                tvCurrentHost.setVisibility(View.VISIBLE);
            } else {
                tvCurrentMode.setText(getString(R.string.mode_label, getString(R.string.dns_mode_auto)));
                tvCurrentLabel.setVisibility(View.GONE);
                tvCurrentHost.setVisibility(View.GONE);
            }
            btnToggleDns.setText("Turn OFF Private DNS");
            btnToggleDns.setIconResource(R.drawable.ic_dns_tile_on);
        } else {
            tvStatusBadge.setText(R.string.status_inactive);
            setBadgeColor(tvStatusBadge, Color.parseColor("#757575")); // Gray
            tvCurrentMode.setText(getString(R.string.mode_label, getString(R.string.dns_status_off)));
            tvCurrentLabel.setVisibility(View.GONE);
            tvCurrentHost.setVisibility(View.GONE);
            btnToggleDns.setText("Turn ON Private DNS");
            btnToggleDns.setIconResource(R.drawable.ic_dns_tile_off);
        }

        btnToggleDns.setEnabled(hasPerm);
    }

    private void setBadgeColor(TextView badge, int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(16f);
        drawable.setColor(color);
        badge.setBackground(drawable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
        registerSettingsObserver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterSettingsObserver();
    }

    private void registerSettingsObserver() {
        if (contentObserver != null) return;

        contentObserver = new ContentObserver(mainHandler) {
            @Override
            public void onChange(boolean selfChange, @NonNull Uri uri) {
                super.onChange(selfChange, uri);
                refreshUI();
            }
        };

        try {
            Uri modeUri = Settings.Global.getUriFor(DnsManager.SETTING_PRIVATE_DNS_MODE);
            Uri specUri = Settings.Global.getUriFor(DnsManager.SETTING_PRIVATE_DNS_SPECIFIER);
            if (modeUri != null) {
                getContentResolver().registerContentObserver(modeUri, false, contentObserver);
            }
            if (specUri != null) {
                getContentResolver().registerContentObserver(specUri, false, contentObserver);
            }
        } catch (Exception ignored) {
        }
    }

    private void unregisterSettingsObserver() {
        if (contentObserver != null) {
            try {
                getContentResolver().unregisterContentObserver(contentObserver);
            } catch (Exception ignored) {
            }
            contentObserver = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
