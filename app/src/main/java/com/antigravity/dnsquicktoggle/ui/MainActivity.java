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
    private TextView tvStatusBadge;
    private TextView tvCurrentMode;
    private TextView tvCurrentHost;
    private MaterialButton btnToggleDns;

    private ImageView ivPermIcon;
    private TextView tvPermTitle;
    private TextView tvPermDesc;
    private LinearLayout layoutAdbCommand;
    private TextView tvAdbCommand;
    private MaterialButton btnCopyAdb;
    private MaterialButton btnGrantRoot;
    private MaterialButton btnRefreshPerm;

    private MaterialButton btnAddTile;

    private ChipGroup chipGroupPresets;
    private Chip chipAdGuard;
    private Chip chipCloudflare;
    private Chip chipGoogle;
    private Chip chipQuad9;
    private Chip chipMullvad;

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
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        tvCurrentMode = findViewById(R.id.tvCurrentMode);
        tvCurrentHost = findViewById(R.id.tvCurrentHost);
        btnToggleDns = findViewById(R.id.btnToggleDns);

        ivPermIcon = findViewById(R.id.ivPermIcon);
        tvPermTitle = findViewById(R.id.tvPermTitle);
        tvPermDesc = findViewById(R.id.tvPermDesc);
        layoutAdbCommand = findViewById(R.id.layoutAdbCommand);
        tvAdbCommand = findViewById(R.id.tvAdbCommand);
        btnCopyAdb = findViewById(R.id.btnCopyAdb);
        btnGrantRoot = findViewById(R.id.btnGrantRoot);
        btnRefreshPerm = findViewById(R.id.btnRefreshPerm);

        btnAddTile = findViewById(R.id.btnAddTile);

        chipGroupPresets = findViewById(R.id.chipGroupPresets);
        chipAdGuard = findViewById(R.id.chipAdGuard);
        chipCloudflare = findViewById(R.id.chipCloudflare);
        chipGoogle = findViewById(R.id.chipGoogle);
        chipQuad9 = findViewById(R.id.chipQuad9);
        chipMullvad = findViewById(R.id.chipMullvad);

        tilHostname = findViewById(R.id.tilHostname);
        etHostname = findViewById(R.id.etHostname);
        btnSaveHost = findViewById(R.id.btnSaveHost);

        rgBehavior = findViewById(R.id.rgBehavior);

        // Prepopulate saved hostname
        String savedHost = dnsManager.getSavedHostname();
        etHostname.setText(savedHost);
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
            String newHost = etHostname.getText() != null ? etHostname.getText().toString().trim() : "";
            if (TextUtils.isEmpty(newHost) || !newHost.contains(".")) {
                tilHostname.setError(getString(R.string.host_invalid_toast));
                return;
            }
            tilHostname.setError(null);
            dnsManager.saveHostname(newHost);

            // If hostname mode is currently active, apply the new specifier immediately
            if (DnsManager.MODE_HOSTNAME.equalsIgnoreCase(dnsManager.getPrivateDnsMode()) && dnsManager.hasPermission()) {
                dnsManager.applyDnsMode(DnsManager.MODE_HOSTNAME, newHost);
            }

            Toast.makeText(this, R.string.host_saved_toast, Toast.LENGTH_SHORT).show();
            refreshUI();
        });
    }

    private void setupPresets() {
        chipAdGuard.setOnClickListener(v -> selectPreset("dns.adguard-dns.com"));
        chipCloudflare.setOnClickListener(v -> selectPreset("1dot1dot1dot1.cloudflare-dns.com"));
        chipGoogle.setOnClickListener(v -> selectPreset("dns.google"));
        chipQuad9.setOnClickListener(v -> selectPreset("dns.quad9.net"));
        chipMullvad.setOnClickListener(v -> selectPreset("base.dns.mullvad.net"));
    }

    private void selectPreset(String hostname) {
        etHostname.setText(hostname);
        dnsManager.saveHostname(hostname);
        if (DnsManager.MODE_HOSTNAME.equalsIgnoreCase(dnsManager.getPrivateDnsMode()) && dnsManager.hasPermission()) {
            dnsManager.applyDnsMode(DnsManager.MODE_HOSTNAME, hostname);
        }
        Toast.makeText(this, getString(R.string.host_label, hostname), Toast.LENGTH_SHORT).show();
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

        // Status Card
        if (isActive) {
            tvStatusBadge.setText(R.string.status_active);
            setBadgeColor(tvStatusBadge, Color.parseColor("#2E7D32")); // Green
            if (DnsManager.MODE_HOSTNAME.equalsIgnoreCase(mode)) {
                tvCurrentMode.setText(getString(R.string.mode_label, getString(R.string.dns_mode_hostname)));
                String host = !TextUtils.isEmpty(currentSpecifier) ? currentSpecifier : dnsManager.getSavedHostname();
                tvCurrentHost.setText(getString(R.string.host_label, host));
                tvCurrentHost.setVisibility(View.VISIBLE);
            } else {
                tvCurrentMode.setText(getString(R.string.mode_label, getString(R.string.dns_mode_auto)));
                tvCurrentHost.setVisibility(View.GONE);
            }
            btnToggleDns.setText("Turn OFF Private DNS");
            btnToggleDns.setIconResource(R.drawable.ic_dns_tile_on);
        } else {
            tvStatusBadge.setText(R.string.status_inactive);
            setBadgeColor(tvStatusBadge, Color.parseColor("#757575")); // Gray
            tvCurrentMode.setText(getString(R.string.mode_label, getString(R.string.dns_status_off)));
            tvCurrentHost.setVisibility(View.GONE);
            btnToggleDns.setText("Turn ON Private DNS");
            btnToggleDns.setIconResource(R.drawable.ic_dns_tile_off);
        }

        btnToggleDns.setEnabled(hasPerm);

        // Permission Card
        if (hasPerm) {
            ivPermIcon.setImageResource(R.drawable.ic_check_circle);
            tvPermTitle.setText(R.string.permission_granted);
            tvPermTitle.setTextColor(Color.parseColor("#2E7D32"));
            tvPermDesc.setText(R.string.permission_granted_desc);
            layoutAdbCommand.setVisibility(View.GONE);
            btnRefreshPerm.setVisibility(View.GONE);
        } else {
            ivPermIcon.setImageResource(R.drawable.ic_warning);
            tvPermTitle.setText(R.string.permission_missing);
            tvPermTitle.setTextColor(Color.parseColor("#C62828"));
            tvPermDesc.setText(R.string.permission_missing_desc);
            layoutAdbCommand.setVisibility(View.VISIBLE);
            btnRefreshPerm.setVisibility(View.VISIBLE);
        }
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
