package com.example.livetvapp.stalker;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "stalker_portals")
public class StalkerPortal {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String name;          
    @NonNull
    private String portalUrl;     
    @NonNull
    private String macAddress;    
    private String token;
    private long tokenExpiry;
    private boolean isActive;
    private long lastSync;

    public StalkerPortal() {}

    public StalkerPortal(@NonNull String name, @NonNull String portalUrl, @NonNull String macAddress) {
        this.name = name;
        this.portalUrl = portalUrl;
        this.macAddress = macAddress;
        this.isActive = true;
    }

    
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    @NonNull public String getName() { return name; }
    public void setName(@NonNull String name) { this.name = name; }
    @NonNull public String getPortalUrl() { return portalUrl; }
    public void setPortalUrl(@NonNull String portalUrl) { this.portalUrl = portalUrl; }
    @NonNull public String getMacAddress() { return macAddress; }
    public void setMacAddress(@NonNull String macAddress) { this.macAddress = macAddress; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public long getTokenExpiry() { return tokenExpiry; }
    public void setTokenExpiry(long tokenExpiry) { this.tokenExpiry = tokenExpiry; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public long getLastSync() { return lastSync; }
    public void setLastSync(long lastSync) { this.lastSync = lastSync; }
}