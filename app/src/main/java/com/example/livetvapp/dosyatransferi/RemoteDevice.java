package com.example.livetvapp.dosyatransferi;

import android.util.Log;


public class RemoteDevice {
    private static final String TAG = "📦[RemoteDevice]";

    private String name;
    private String ipAddress;
    private int port;
    private long lastSeen;

    public RemoteDevice(String name, String ipAddress, int port) {
        this.name = name;
        this.ipAddress = ipAddress;
        this.port = port;
        this.lastSeen = System.currentTimeMillis();
        Log.d(TAG, "✅ Cihaz oluşturuldu: " + name + " (" + ipAddress + ":" + port + ")");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        Log.d(TAG, "📝 Cihaz adı değiştirildi: " + name);
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        Log.d(TAG, "📝 IP adresi güncellendi: " + ipAddress);
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
        Log.d(TAG, "📝 Port güncellendi: " + port);
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
        Log.d(TAG, "🕐 Son görülme güncellendi: " + name);
    }

    @Override
    public String toString() {
        return name + " (" + ipAddress + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RemoteDevice) {
            RemoteDevice other = (RemoteDevice) obj;
            return ipAddress.equals(other.ipAddress) && port == other.port;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ipAddress.hashCode() + port;
    }
}