package com.antigravity.dnsquicktoggle.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class DnsProvider {
    private final String id;
    private String name;
    private String hostname;
    private final boolean isPreset;

    public DnsProvider(@NonNull String id, @NonNull String name, @NonNull String hostname, boolean isPreset) {
        this.id = id;
        this.name = name;
        this.hostname = hostname;
        this.isPreset = isPreset;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public String getHostname() {
        return hostname;
    }

    public void setHostname(@NonNull String hostname) {
        this.hostname = hostname;
    }

    public boolean isPreset() {
        return isPreset;
    }

    @NonNull
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("hostname", hostname);
        json.put("isPreset", isPreset);
        return json;
    }

    @Nullable
    public static DnsProvider fromJson(@NonNull JSONObject json) {
        try {
            String id = json.getString("id");
            String name = json.getString("name");
            String hostname = json.getString("hostname");
            boolean isPreset = json.optBoolean("isPreset", false);
            return new DnsProvider(id, name, hostname, isPreset);
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DnsProvider that = (DnsProvider) o;
        return Objects.equals(hostname.toLowerCase(), that.hostname.toLowerCase());
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname.toLowerCase());
    }
}
