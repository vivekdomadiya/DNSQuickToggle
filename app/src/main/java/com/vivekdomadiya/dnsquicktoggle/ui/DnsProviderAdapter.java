package com.vivekdomadiya.dnsquicktoggle.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.vivekdomadiya.dnsquicktoggle.R;
import com.vivekdomadiya.dnsquicktoggle.model.DnsProvider;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.radiobutton.MaterialRadioButton;

import java.util.ArrayList;
import java.util.List;

public class DnsProviderAdapter extends RecyclerView.Adapter<DnsProviderAdapter.ViewHolder> {

    public interface OnProviderActionListener {
        void onSelectProvider(@NonNull DnsProvider provider);
        void onEditProvider(@NonNull DnsProvider provider);
        void onDeleteProvider(@NonNull DnsProvider provider);
    }

    private final List<DnsProvider> providers = new ArrayList<>();
    @Nullable
    private String selectedHostname;
    private final OnProviderActionListener listener;

    public DnsProviderAdapter(@NonNull OnProviderActionListener listener) {
        this.listener = listener;
    }

    public void setProviders(@NonNull List<DnsProvider> newProviders, @Nullable String selectedHostname) {
        this.providers.clear();
        this.providers.addAll(newProviders);
        this.selectedHostname = selectedHostname;
        notifyDataSetChanged();
    }

    public void setSelectedHostname(@Nullable String selectedHostname) {
        this.selectedHostname = selectedHostname;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dns_provider, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DnsProvider provider = providers.get(position);
        Context context = holder.itemView.getContext();

        holder.tvProviderName.setText(provider.getName());
        holder.tvProviderHostname.setText(provider.getHostname());

        // Highlight if this is the selected hostname
        boolean isSelected = selectedHostname != null && 
                selectedHostname.equalsIgnoreCase(provider.getHostname());
        holder.rbSelected.setChecked(isSelected);

        if (isSelected) {
            holder.cardView.setStrokeColor(Color.parseColor("#1976D2"));
            holder.cardView.setStrokeWidth((int) (2 * context.getResources().getDisplayMetrics().density));
        } else {
            holder.cardView.setStrokeColor(Color.parseColor("#20000000"));
            holder.cardView.setStrokeWidth((int) (1 * context.getResources().getDisplayMetrics().density));
        }

        // Preset items cannot be edited or deleted
        if (provider.isPreset()) {
            holder.tvPresetBadge.setVisibility(View.VISIBLE);
            holder.layoutActions.setVisibility(View.GONE);
        } else {
            holder.tvPresetBadge.setVisibility(View.GONE);
            holder.layoutActions.setVisibility(View.VISIBLE);
        }

        // Card click selects this provider
        holder.itemView.setOnClickListener(v -> listener.onSelectProvider(provider));

        // Action buttons
        holder.btnEditProvider.setOnClickListener(v -> listener.onEditProvider(provider));
        holder.btnDeleteProvider.setOnClickListener(v -> listener.onDeleteProvider(provider));
    }

    @Override
    public int getItemCount() {
        return providers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardView;
        final MaterialRadioButton rbSelected;
        final TextView tvProviderName;
        final TextView tvPresetBadge;
        final TextView tvProviderHostname;
        final LinearLayout layoutActions;
        final ImageView btnEditProvider;
        final ImageView btnDeleteProvider;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            rbSelected = itemView.findViewById(R.id.rbSelected);
            tvProviderName = itemView.findViewById(R.id.tvProviderName);
            tvPresetBadge = itemView.findViewById(R.id.tvPresetBadge);
            tvProviderHostname = itemView.findViewById(R.id.tvProviderHostname);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnEditProvider = itemView.findViewById(R.id.btnEditProvider);
            btnDeleteProvider = itemView.findViewById(R.id.btnDeleteProvider);
        }
    }
}