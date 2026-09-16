package com.example.livetvapp.remotecontrol;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemoteCommandServer {
    private static final String TAG = "RemoteControlServer";
    private static final int DISCOVERY_PORT = 8890;
    private static final int SERVER_PORT = 8891;
    private static final String DISCOVERY_MESSAGE = "LIVETV_DISCOVERY";
    private static final String RESPONSE_PREFIX = "LIVETV_RESPONSE:";

    private Context context;
    private CommandCallback callback;
    private ExecutorService executorService;
    private DatagramSocket discoverySocket;
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    
    private int activeClientCount = 0;

    
    public boolean hasActiveClient() {
        return activeClientCount > 0;
    }
    public interface CommandCallback {
        void onCommandReceived(String command, String data);
        default void onClientConnected() {}
        default void onClientDisconnected() {}
    }

    public RemoteCommandServer(Context context, CommandCallback callback) {
        this.context = context;
        this.callback = callback;
        this.executorService = Executors.newCachedThreadPool();
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;
        startDiscoveryListener();
        startCommandServer();
        Log.d(TAG, "✅ RemoteControlServer başlatıldı (port: " + SERVER_PORT + ")");
    }

    public void stop() {
        isRunning = false;
        try {
            if (discoverySocket != null) discoverySocket.close();
            if (serverSocket != null) serverSocket.close();
        } catch (Exception e) {
            Log.e(TAG, "Server durdurma hatası", e);
        }
        executorService.shutdown();
        Log.d(TAG, "✅ RemoteControlServer durduruldu");
    }

    public boolean isRunning() {
        return isRunning;
    }

    private void startDiscoveryListener() {
        executorService.execute(() -> {
            try {
                discoverySocket = new DatagramSocket(null);
                discoverySocket.setReuseAddress(true);
                discoverySocket.bind(new InetSocketAddress(DISCOVERY_PORT));
                Log.d(TAG, "📡 Discovery dinleniyor: port " + DISCOVERY_PORT);

                byte[] buffer = new byte[1024];

                while (isRunning) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        discoverySocket.receive(packet);

                        String message = new String(packet.getData(), 0, packet.getLength());
                        String senderIP = packet.getAddress().getHostAddress();

                        if (DISCOVERY_MESSAGE.equals(message)) {
                            String deviceName = getDeviceName();
                            String response = RESPONSE_PREFIX + deviceName;

                            byte[] responseData = response.getBytes();
                            DatagramPacket responsePacket = new DatagramPacket(
                                    responseData, responseData.length,
                                    packet.getAddress(), packet.getPort()
                            );
                            discoverySocket.send(responsePacket);
                            Log.d(TAG, "📡 Discovery yanıtı gönderildi: " + deviceName + " -> " + senderIP);
                        }
                    } catch (SocketException e) {
                        if (isRunning) Log.e(TAG, "Discovery socket hatası", e);
                        break;
                    } catch (Exception e) {
                        Log.e(TAG, "Discovery dinleme hatası", e);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Discovery listener başlatma hatası", e);
            }
        });
    }

    private void startCommandServer() {
        executorService.execute(() -> {
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
                Log.d(TAG, "🔌 Komut sunucusu dinleniyor: port " + SERVER_PORT);

                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        executorService.execute(() -> handleClient(clientSocket));
                    } catch (SocketException e) {
                        if (isRunning) Log.e(TAG, "Komut server socket hatası", e);
                        break;
                    } catch (Exception e) {
                        Log.e(TAG, "Komut server hatası", e);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Komut server başlatma hatası", e);
            }
        });
    }

    private void handleClient(Socket clientSocket) {
        activeClientCount++;
        if (callback != null) callback.onClientConnected();
        try {
            String clientIP = clientSocket.getInetAddress().getHostAddress();
            Log.d(TAG, "🔌 Bağlantı alındı: " + clientIP);

            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null && isRunning) {
                Log.d(TAG, "📨 Komut alındı (" + clientIP + "): " + line);

                String command = line;
                String data = "";

                int colonIndex = line.indexOf(':');
                if (colonIndex > 0) {
                    command = line.substring(0, colonIndex);
                    data = line.substring(colonIndex + 1);
                }

                if (callback != null) {
                    callback.onCommandReceived(command, data);
                }
            }
            clientSocket.close();
        } catch (Exception e) {
            Log.e(TAG, "Client handling hatası", e);
        } finally {
            activeClientCount--;
            if (callback != null) callback.onClientDisconnected();
        }
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
            Log.w(TAG, "IP adresi alınamadı: " + e.getMessage());
        }

        return deviceName;
    }
}