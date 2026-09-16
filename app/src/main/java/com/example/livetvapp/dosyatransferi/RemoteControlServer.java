package com.example.livetvapp.dosyatransferi;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemoteControlServer {
    private static final String TAG = "📡[RemoteServer]";
    private static final int DISCOVERY_PORT = 8890;

    private Context context;
    private ExecutorService executorService;
    private DatagramSocket discoverySocket;
    private boolean isRunning = false;

    public RemoteControlServer(Context context) {
        this.context = context;
        this.executorService = Executors.newCachedThreadPool();
        Log.d(TAG, "🔧 RemoteControlServer oluşturuldu");
    }

    public void start() {
        if (isRunning) {
            Log.d(TAG, "⚠️ Server zaten çalışıyor");
            return;
        }

        isRunning = true;
        startDiscoveryListener();
        Log.d(TAG, "✅ RemoteControlServer başlatıldı");
    }

    public void stop() {
        isRunning = false;

        try {
            if (discoverySocket != null && !discoverySocket.isClosed()) {
                discoverySocket.close();
                Log.d(TAG, "📡 Discovery socket kapatıldı");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Socket kapatma hatası: " + e.getMessage());
        }

        if (executorService != null) {
            executorService.shutdown();
        }

        Log.d(TAG, "✅ RemoteControlServer durduruldu");
    }

    private void startDiscoveryListener() {
        executorService.execute(() -> {
            try {
                
                discoverySocket = new DatagramSocket(null);
                discoverySocket.setReuseAddress(true);
                discoverySocket.bind(new InetSocketAddress(DISCOVERY_PORT));
                discoverySocket.setSoTimeout(1000);

                Log.d(TAG, "📡 Discovery listener başlatıldı: port " + DISCOVERY_PORT);

                byte[] buffer = new byte[1024];

                while (isRunning) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        discoverySocket.receive(packet);

                        String message = new String(packet.getData(), 0, packet.getLength());
                        String senderIP = packet.getAddress().getHostAddress();

                        Log.d(TAG, "📡 Discovery mesajı alındı: " + message + " - IP: " + senderIP);

                        if ("LIVETV_DISCOVERY".equals(message)) {
                            String deviceName = getDeviceName();
                            String response = "LIVETV_RESPONSE:" + deviceName;

                            byte[] responseData = response.getBytes();
                            DatagramPacket responsePacket = new DatagramPacket(
                                    responseData, responseData.length,
                                    packet.getAddress(), packet.getPort()
                            );

                            discoverySocket.send(responsePacket);
                            Log.d(TAG, "✅ Discovery yanıtı gönderildi: " + response + " -> " + senderIP);
                        } else {
                            Log.d(TAG, "📡 Tanınmayan mesaj: " + message);
                        }

                    } catch (SocketException e) {
                        if (isRunning) {
                            Log.e(TAG, "❌ Discovery socket hatası: " + e.getMessage());
                        }
                        break;
                    } catch (java.net.SocketTimeoutException e) {
                        
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Discovery dinleme hatası: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Discovery listener başlatma hatası: " + e.getMessage(), e);
            }
        });
    }

    private String getDeviceName() {
        String deviceName = Build.MODEL;
        if (deviceName == null || deviceName.trim().isEmpty()) {
            deviceName = "Android Cihaz";
        }

        
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                int ip = wifiManager.getConnectionInfo().getIpAddress();
                if (ip != 0) {
                    String ipStr = String.format("%d.%d.%d.%d",
                            (ip & 0xff),
                            (ip >> 8 & 0xff),
                            (ip >> 16 & 0xff),
                            (ip >> 24 & 0xff));
                    deviceName = deviceName + " (" + ipStr + ")";
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "⚠️ IP adresi alınamadı: " + e.getMessage());
        }

        Log.d(TAG, "📱 Cihaz adı: " + deviceName);
        return deviceName;
    }

    public boolean isRunning() {
        return isRunning;
    }
}