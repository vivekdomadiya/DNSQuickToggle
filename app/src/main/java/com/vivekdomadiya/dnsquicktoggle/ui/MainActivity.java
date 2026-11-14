package com.vivekdomadiya.dnsquicktoggle.ui;

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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vivekdomadiya.dnsquicktoggle.R;
import com.vivekdomadiya.dnsquicktoggle.model.DnsProvider;
import com.vivekdomadiya.dnsquicktoggle.service.DnsTileService;
import com.vivekdomadiya.dnsquicktoggle.util.DnsManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements DnsProviderAdapter.OnProviderActionListener {

    private DnsManager dnsManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private ContentObserver contentObserver;
    private Toast currentToast;

    // UI references
    private MaterialToolbar topAppBar;

    // Permission Card
    private MaterialCardView cardPermission;
    private ImageView ivPermIcon;
    private TextView tvPermTitle;
    private TextView tvPermDesc;
    private LinearLayout layoutAdbCommand;
    private TextView tvAdbCommand;
    private MaterialButton btnCopyAdb;
    private MaterialButton btnGrantRoot;
    private MaterialButton btnRefreshPerm;

    // Status Card
    private TextView tvStatusBadge;
    private TextView tvCurrentMode;
    private TextView tvCurrentLabel;
    private TextView tvCurrentHost;
    private MaterialButton btnStatusAddToList;
    private MaterialButton btnToggleDns;

    // Quick Settings Tile Helper Card
    private MaterialCardView cardQuickTile;
    private MaterialButton btnAddTile;

    // DNS Providers List
    private MaterialButton btnAddDns;
    private RecyclerView rvDnsProviders;
    private DnsProviderAdapter providerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dnsManager = new DnsManager(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
    }

    private void showToast(@NonNull String message) {
        if (currentToast != null) {
            currentToast.cancel();
        }
        currentToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        currentToast.show();
    }

    private void showToast(int resId) {
        showToast(getString(resId));
    }

    private void initViews() {
        topAppBar = findViewById(R.id.topAppBar);

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
        btnStatusAddToList = findViewById(R.id.btnStatusAddToList);
        btnToggleDns = findViewById(R.id.btnToggleDns);

        cardQuickTile = findViewById(R.id.cardQuickTile);
        btnAddTile = findViewById(R.id.btnAddTile);

        btnAddDns = findViewById(R.id.btnAddDns);
        rvDnsProviders = findViewById(R.id.rvDnsProviders);
    }

    private void setupToolbar() {
        topAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_toggle_behavior) {
                showToggleBehaviorDialog();
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerView() {
        providerAdapter = new DnsProviderAdapter(this);
        rvDnsProviders.setLayoutManager(new LinearLayoutManager(this));
        rvDnsProviders.setAdapter(providerAdapter);
    }

    private void setupListeners() {
        btnToggleDns.setOnClickListener(v -> {
            if (!dnsManager.hasPermission()) {
                showToast(R.string.permission_required_toast);
                return;
            }
            DnsManager.ToggleResult result = dnsManager.toggle();
            showToast(result.message);
            refreshUI();
        });

        btnAddDns.setOnClickListener(v -> showAddDnsDialog());
        btnAddTile.setOnClickListener(v -> handleAddQuickTile());

        btnCopyAdb.setOnClickListener(v -> {
            String command = tvAdbCommand.getText().toString();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("ADB Command", command);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                showToast(R.string.adb_copied_toast);
            }
        });

        btnGrantRoot.setOnClickListener(v -> {
            btnGrantRoot.setEnabled(false);
            executorService.execute(() -> {
                boolean granted = dnsManager.grantPermissionWithRoot();
                mainHandler.post(() -> {
                    btnGrantRoot.setEnabled(true);
                    if (granted) {
                        showToast(R.string.root_success_toast);
                    } else {
                        showToast(R.string.root_failed_toast);
                    }
                    refreshUI();
                });
            });
        });

        btnRefreshPerm.setOnClickListener(v -> {
            refreshUI();
            showToast(dnsManager.hasPermission() ? R.string.permission_granted : R.string.permission_missing);
        });
    }

    // --- Provider Actions ---

    @Override
    public void onSelectProvider(@NonNull DnsProvider provider) {
        dnsManager.selectProvider(provider);
        showToast(getString(R.string.dns_selected_toast, provider.getName(), provider.getHostname()));
        refreshUI();
    }

    @Override
    public void onEditProvider(@NonNull DnsProvider provider) {
        showEditDnsDialog(provider);
    }

    @Override
    public void onDeleteProvider(@NonNull DnsProvider provider) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_delete_dns_title)
                .setMessage(getString(R.string.dialog_delete_dns_msg, provider.getName()))
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    dnsManager.deleteCustomProvider(provider.getId());
                    showToast(R.string.dns_deleted_toast);
                    refreshUI();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showAddDnsDialog() {
        showAddDnsDialog(null, null);
    }

    private void showAddDnsDialog(@Nullable String initialHost, @Nullable String initialName) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_dns, null);
        TextInputLayout tilName = dialogView.findViewById(R.id.dialogTilName);
        TextInputEditText etName = dialogView.findViewById(R.id.dialogEtName);
        TextInputLayout tilHost = dialogView.findViewById(R.id.dialogTilHostname);
        TextInputEditText etHost = dialogView.findViewById(R.id.dialogEtHostname);

        if (!TextUtils.isEmpty(initialName)) {
            etName.setText(initialName);
        }
        if (!TextUtils.isEmpty(initialHost)) {
            etHost.setText(initialHost);
        }

        // Clear error as soon as user types
        setupErrorClearingWatcher(etName, tilName);
        setupErrorClearingWatcher(etHost, tilHost);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_add_dns_title)
                .setView(dialogView)
                .setPositiveButton(R.string.action_add, null)
                .setNegativeButton(R.string.action_cancel, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveBtn.setOnClickListener(v -> {
                String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                String rawHost = etHost.getText() != null ? etHost.getText().toString().trim() : "";
                String cleanHost = DnsManager.sanitizeHostname(rawHost);

                boolean hasError = false;

                // 1. Validate label / name
                if (TextUtils.isEmpty(name)) {
                    tilName.setError(getString(R.string.err_label_empty));
                    hasError = true;
                } else if (dnsManager.isLabelExists(name, null)) {
                    tilName.setError(getString(R.string.err_label_exists));
                    hasError = true;
                } else {
                    tilName.setError(null);
                }

                // 2. Validate hostname
                if (TextUtils.isEmpty(rawHost)) {
                    tilHost.setError(getString(R.string.err_host_empty));
                    hasError = true;
                } else if (!DnsManager.isValidHostname(cleanHost)) {
                    tilHost.setError(getString(R.string.err_host_invalid));
                    hasError = true;
                } else if (dnsManager.isHostnameExists(cleanHost, null)) {
                    tilHost.setError(getString(R.string.err_host_exists));
                    hasError = true;
                } else {
                    tilHost.setError(null);
                }

                if (hasError) {
                    return; // Keep dialog open so user can fix
                }

                DnsProvider created = dnsManager.addCustomProvider(name, cleanHost);
                dnsManager.selectProvider(created);
                showToast(getString(R.string.dns_added_toast, name));
                refreshUI();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void showEditDnsDialog(@NonNull DnsProvider provider) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_dns, null);
        TextInputLayout tilName = dialogView.findViewById(R.id.dialogTilName);
        TextInputEditText etName = dialogView.findViewById(R.id.dialogEtName);
        TextInputLayout tilHost = dialogView.findViewById(R.id.dialogTilHostname);
        TextInputEditText etHost = dialogView.findViewById(R.id.dialogEtHostname);

        etName.setText(provider.getName());
        etHost.setText(provider.getHostname());

        setupErrorClearingWatcher(etName, tilName);
        setupErrorClearingWatcher(etHost, tilHost);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_edit_dns_title)
                .setView(dialogView)
                .setPositiveButton(R.string.action_save, null)
                .setNegativeButton(R.string.action_cancel, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveBtn.setOnClickListener(v -> {
                String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                String rawHost = etHost.getText() != null ? etHost.getText().toString().trim() : "";
                String cleanHost = DnsManager.sanitizeHostname(rawHost);

                boolean hasError = false;

                // 1. Validate label (exclude current provider ID)
                if (TextUtils.isEmpty(name)) {
                    tilName.setError(getString(R.string.err_label_empty));
                    hasError = true;
                } else if (dnsManager.isLabelExists(name, provider.getId())) {
                    tilName.setError(getString(R.string.err_label_exists));
                    hasError = true;
                } else {
                    tilName.setError(null);
                }

                // 2. Validate hostname (exclude current provider ID)
                if (TextUtils.isEmpty(rawHost)) {
                    tilHost.setError(getString(R.string.err_host_empty));
                    hasError = true;
                } else if (!DnsManager.isValidHostname(cleanHost)) {
                    tilHost.setError(getString(R.string.err_host_invalid));
                    hasError = true;
                } else if (dnsManager.isHostnameExists(cleanHost, provider.getId())) {
                    tilHost.setError(getString(R.string.err_host_exists));
                    hasError = true;
                } else {
                    tilHost.setError(null);
                }

                if (hasError) {
                    return; // Keep dialog open so user can fix
                }

                dnsManager.updateCustomProvider(provider.getId(), name, cleanHost);
                showToast(getString(R.string.dns_updated_toast, name));
                refreshUI();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void setupErrorClearingWatcher(TextInputEditText editText, TextInputLayout inputLayout) {
        editText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (inputLayout.getError() != null) {
                    inputLayout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void showToggleBehaviorDialog() {
        CharSequence[] options = new CharSequence[]{
                getString(R.string.behavior_hostname_off),
                getString(R.string.behavior_auto_off),
                getString(R.string.behavior_cycle_all)
        };

        int currentBehavior = dnsManager.getToggleBehavior();
        int checkedItem = 0;
        if (currentBehavior == DnsManager.BEHAVIOR_AUTO_OFF) {
            checkedItem = 1;
        } else if (currentBehavior == DnsManager.BEHAVIOR_CYCLE_ALL) {
            checkedItem = 2;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_toggle_behavior_title)
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    int selected = DnsManager.BEHAVIOR_HOSTNAME_OFF;
                    if (which == 1) {
                        selected = DnsManager.BEHAVIOR_AUTO_OFF;
                    } else if (which == 2) {
                        selected = DnsManager.BEHAVIOR_CYCLE_ALL;
                    }
                    dnsManager.saveToggleBehavior(selected);
                    showToast(options[which].toString());
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
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
                                dnsManager.setTileAdded(true);
                                showToast(R.string.tile_added_success);
                                refreshUI();
                            } else if (resultCode == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                                dnsManager.setTileAdded(true);
                                showToast(R.string.tile_already_added);
                                refreshUI();
                            }
                        }
                );
                return;
            }
        }
        // Fallback for older versions
        showToast(R.string.qs_tile_guide);
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
        String activeHostname = dnsManager.getSavedHostname();
        if (isActive) {
            tvStatusBadge.setText(R.string.status_active);
            setBadgeColor(tvStatusBadge, Color.parseColor("#2E7D32")); // Green
            if (DnsManager.MODE_HOSTNAME.equalsIgnoreCase(mode)) {
                tvCurrentMode.setText(getString(R.string.mode_label, getString(R.string.dns_mode_hostname)));

                String host = !TextUtils.isEmpty(currentSpecifier) ? currentSpecifier : activeHostname;
                activeHostname = host;

                DnsProvider provider = dnsManager.findProviderByHostname(host);
                if (provider != null) {
                    tvCurrentLabel.setText(getString(R.string.label_display, provider.getName()));
                    tvCurrentLabel.setVisibility(View.VISIBLE);
                    btnStatusAddToList.setVisibility(View.GONE);
                } else {
                    // Not in our list: show "Not in list" and provide button to add it
                    tvCurrentLabel.setText(getString(R.string.label_display, getString(R.string.status_label_not_in_list)));
                    tvCurrentLabel.setVisibility(View.VISIBLE);
                    btnStatusAddToList.setVisibility(View.VISIBLE);
                    final String hostToAdd = host;
                    btnStatusAddToList.setOnClickListener(v -> 
                            showAddDnsDialog(hostToAdd, DnsManager.inferLabelFromHostname(hostToAdd)));
                }

                tvCurrentHost.setText(getString(R.string.hostname_display, host));
                tvCurrentHost.setVisibility(View.VISIBLE);
            } else {
                tvCurrentMode.setText(getString(R.string.mode_label, getString(R.string.dns_mode_auto)));
                tvCurrentLabel.setVisibility(View.GONE);
                tvCurrentHost.setVisibility(View.GONE);
                btnStatusAddToList.setVisibility(View.GONE);
            }
            btnToggleDns.setText("Turn OFF Private DNS");
            btnToggleDns.setIconResource(R.drawable.ic_dns_tile_on);
        } else {
            tvStatusBadge.setText(R.string.status_inactive);
            setBadgeColor(tvStatusBadge, Color.parseColor("#757575")); // Gray
            tvCurrentMode.setText(getString(R.string.mode_label, getString(R.string.dns_status_off)));
            tvCurrentLabel.setVisibility(View.GONE);
            tvCurrentHost.setVisibility(View.GONE);
            btnStatusAddToList.setVisibility(View.GONE);
            btnToggleDns.setText("Turn ON Private DNS");
            btnToggleDns.setIconResource(R.drawable.ic_dns_tile_off);
        }

        btnToggleDns.setEnabled(hasPerm);

        // 3. Quick Settings Tile Card (shown only if tile is NOT already present)
        boolean tilePresent = dnsManager.isTilePresent();
        cardQuickTile.setVisibility(tilePresent ? View.GONE : View.VISIBLE);

        // 4. DNS Providers List
        providerAdapter.setProviders(dnsManager.getAllProviders(), activeHostname);
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