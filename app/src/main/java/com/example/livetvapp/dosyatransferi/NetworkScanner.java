package com.example.livetvapp.dosyatransferi;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkScanner {
    private static final String TAG = "🔍[NetworkScanner]";
    private static final int DISCOVERY_PORT = 8890;
    private static final int SERVER_PORT = 8891;
    private static final String DISCOVERY_MESSAGE = "LIVETV_DISCOVERY";
    private static final String RESPONSE_PREFIX = "LIVETV_RESPONSE:";

    private Context context;
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private AtomicBoolean isScanning = new AtomicBoolean(false);
    private DatagramSocket currentSocket;

    
    public interface ScanCallback {
        void onDevicesFound(List<RemoteDevice> devices);
        void onScanFailed(String error);
    }

    
    public interface RealTimeScanCallback {
        void onDeviceFound(RemoteDevice device);
        void onScanComplete(List<RemoteDevice> devices);
        void onScanFailed(String error);
    }

    public NetworkScanner(Context context) {
        this.context = context;
        Log.d(TAG, "🔧 NetworkScanner oluşturuldu");
    }

    
    public void scanForDevicesRealtime(RealTimeScanCallback callback) {
        if (isScanning.get()) {
            Log.d(TAG, "⚠️ Tarama zaten devam ediyor");
            return;
        }

        Log.d(TAG, "🔍 Gerçek zamanlı cihaz taraması başlatılıyor...");
        isScanning.set(true);
        List<RemoteDevice> foundDevices = new ArrayList<>();

        executorService.execute(() -> {
            DatagramSocket socket = null;
            try {
                
                WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                        .getSystemService(Context.WIFI_SERVICE);

                if (wifiManager == null || !wifiManager.isWifiEnabled()) {
                    mainHandler.post(() -> callback.onScanFailed("WiFi kapalı"));
                    isScanning.set(false);
                    return;
                }

                int currentIp = wifiManager.getConnectionInfo().getIpAddress();
                if (currentIp == 0) {
                    mainHandler.post(() -> callback.onScanFailed("WiFi ağına bağlı değil"));
                    isScanning.set(false);
                    return;
                }

                String currentIpStr = String.format("%d.%d.%d.%d",
                        (currentIp & 0xff),
                        (currentIp >> 8 & 0xff),
                        (currentIp >> 16 & 0xff),
                        (currentIp >> 24 & 0xff));
                Log.d(TAG, "📡 Mevcut IP: " + currentIpStr);

                
                socket = new DatagramSocket();
                socket.setBroadcast(true);
                socket.setSoTimeout(2000);
                currentSocket = socket;

                String broadcastAddress = getBroadcastAddress();
                Log.d(TAG, "📡 Broadcast adresi: " + broadcastAddress);

                
                byte[] buffer = DISCOVERY_MESSAGE.getBytes();

                
                try {
                    DatagramPacket packet = new DatagramPacket(
                            buffer, buffer.length,
                            InetAddress.getByName(broadcastAddress),
                            DISCOVERY_PORT
                    );
                    socket.send(packet);
                    Log.d(TAG, "📡 Discovery gönderildi: " + broadcastAddress);
                } catch (Exception e) {
                    Log.e(TAG, "Broadcast gönderme hatası: " + e.getMessage());
                }

                
                try {
                    DatagramPacket globalPacket = new DatagramPacket(
                            buffer, buffer.length,
                            InetAddress.getByName("255.255.255.255"),
                            DISCOVERY_PORT
                    );
                    socket.send(globalPacket);
                    Log.d(TAG, "📡 Global broadcast gönderildi: 255.255.255.255");
                } catch (Exception e) {
                    Log.e(TAG, "Global broadcast hatası: " + e.getMessage());
                }

                long startTime = System.currentTimeMillis();
                long scanDuration = 8000;
                byte[] responseBuffer = new byte[1024];

                Log.d(TAG, "⏳ Yanıt bekleniyor...");

                while (isScanning.get() && (System.currentTimeMillis() - startTime) < scanDuration) {
                    try {
                        DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
                        socket.receive(responsePacket);

                        String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
                        String senderIP = responsePacket.getAddress().getHostAddress();

                        Log.d(TAG, "📡 Yanıt alındı: " + response + " - IP: " + senderIP);

                        if (response.startsWith(RESPONSE_PREFIX)) {
                            String deviceName = response.substring(RESPONSE_PREFIX.length());

                            
                            if (senderIP.equals(currentIpStr)) {
                                Log.d(TAG, "📡 Kendi cihazım, atlanıyor: " + senderIP);
                                continue;
                            }

                            
                            boolean exists = false;
                            for (RemoteDevice d : foundDevices) {
                                if (d.getIpAddress().equals(senderIP)) {
                                    exists = true;
                                    break;
                                }
                            }

                            if (!exists) {
                                RemoteDevice device = new RemoteDevice(deviceName, senderIP, SERVER_PORT);
                                foundDevices.add(device);
                                Log.d(TAG, "✅ YENİ CİHAZ BULUNDU: " + deviceName + " - " + senderIP);

                                
                                final RemoteDevice finalDevice = device;
                                mainHandler.post(() -> {
                                    Log.d(TAG, "📢 onDeviceFound callback çağrılıyor: " + finalDevice.getName());
                                    callback.onDeviceFound(finalDevice);
                                });
                            }
                        }
                    } catch (SocketTimeoutException e) {
                        
                        Log.d(TAG, "⏳ Socket timeout, beklemeye devam...");
                    } catch (Exception e) {
                        if (isScanning.get()) {
                            Log.e(TAG, "❌ Yanıt okuma hatası: " + e.getMessage());
                        }
                    }
                }

                
                Log.d(TAG, "✅ Tarama tamamlandı. Toplam " + foundDevices.size() + " cihaz bulundu");
                final List<RemoteDevice> finalDevices = new ArrayList<>(foundDevices);
                mainHandler.post(() -> callback.onScanComplete(finalDevices));

            } catch (Exception e) {
                Log.e(TAG, "❌ Tarama hatası: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onScanFailed(e.getMessage()));
            } finally {
                isScanning.set(false);
                if (socket != null && !socket.isClosed()) {
                    try {
                        socket.close();
                    } catch (Exception e) {
                        Log.e(TAG, "Socket kapatma hatası: " + e.getMessage());
                    }
                }
                currentSocket = null;
            }
        });
    }

    
    public void stopScan() {
        Log.d(TAG, "⏹️ Tarama durduruluyor...");
        isScanning.set(false);
        if (currentSocket != null && !currentSocket.isClosed()) {
            try {
                currentSocket.close();
            } catch (Exception e) {
                Log.e(TAG, "Socket kapatma hatası: " + e.getMessage());
            }
        }
    }

    
    public void scanForDevices(ScanCallback callback) {
        Log.d(TAG, "🔍 Cihaz taraması başlatılıyor...");
        executorService.execute(() -> {
            List<RemoteDevice> devices = performDeviceScan();
            if (devices == null) {
                mainHandler.post(() -> callback.onScanFailed("Tarama başarısız"));
            } else {
                mainHandler.post(() -> callback.onDevicesFound(devices));
            }
        });
    }

    private List<RemoteDevice> performDeviceScan() {
        List<RemoteDevice> devices = new ArrayList<>();
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null || !wifiManager.isWifiEnabled()) return null;

            int currentIp = wifiManager.getConnectionInfo().getIpAddress();
            if (currentIp == 0) return null;

            String currentIpStr = String.format("%d.%d.%d.%d",
                    (currentIp & 0xff), (currentIp >> 8 & 0xff),
                    (currentIp >> 16 & 0xff), (currentIp >> 24 & 0xff));

            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            socket.setSoTimeout(2000);

            String broadcastAddress = getBroadcastAddress();
            byte[] buffer = DISCOVERY_MESSAGE.getBytes();

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                    InetAddress.getByName(broadcastAddress), DISCOVERY_PORT);
            socket.send(packet);

            DatagramPacket globalPacket = new DatagramPacket(buffer, buffer.length,
                    InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT);
            socket.send(globalPacket);

            long startTime = System.currentTimeMillis();
            byte[] responseBuffer = new byte[1024];

            while (System.currentTimeMillis() - startTime < 8000) {
                try {
                    DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
                    socket.receive(responsePacket);
                    String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
                    String senderIP = responsePacket.getAddress().getHostAddress();

                    if (response.startsWith(RESPONSE_PREFIX)) {
                        String deviceName = response.substring(RESPONSE_PREFIX.length());
                        if (!senderIP.equals(currentIpStr)) {
                            boolean exists = false;
                            for (RemoteDevice d : devices) {
                                if (d.getIpAddress().equals(senderIP)) { exists = true; break; }
                            }
                            if (!exists) {
                                devices.add(new RemoteDevice(deviceName, senderIP, SERVER_PORT));
                            }
                        }
                    }
                } catch (SocketTimeoutException e) { }
            }
            socket.close();
        } catch (Exception e) {
            return null;
        }
        return devices;
    }

    private String getBroadcastAddress() {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null || !wifiManager.isWifiEnabled()) return "255.255.255.255";

            int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
            int netmask = wifiManager.getDhcpInfo().netmask;
            if (netmask == 0) netmask = 0xFFFFFF00;

            int broadcast = (ipAddress & netmask) | (~netmask);
            broadcast = broadcast & 0xFFFFFFFF;

            return String.format(Locale.US, "%d.%d.%d.%d",
                    (broadcast >> 24) & 0xFF, (broadcast >> 16) & 0xFF,
                    (broadcast >> 8) & 0xFF, broadcast & 0xFF);
        } catch (Exception e) {
            return "255.255.255.255";
        }
    }
}