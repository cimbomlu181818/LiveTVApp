package com.example.livetvapp.remotecontrol;

public class RemoteDevice {
    private String name;
    private String ipAddress;
    private int port;

    public RemoteDevice(String name, String ipAddress, int port) {
        this.name = name;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public String getName() { return name; }
    public String getIpAddress() { return ipAddress; }
    public int getPort() { return port; }

    public void setName(String name) { this.name = name; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setPort(int port) { this.port = port; }

    @Override
    public String toString() {
        return name + " (" + ipAddress + ":" + port + ")";
    }
}