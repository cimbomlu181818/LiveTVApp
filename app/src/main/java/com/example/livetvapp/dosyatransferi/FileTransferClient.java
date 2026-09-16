package com.example.livetvapp.dosyatransferi;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class FileTransferClient {
    private static final String TAG = "📤[FileClient]";
    private static final int TRANSFER_PORT = 8892;

    private Socket socket;
    private FileTransferCallback callback;

    public interface FileTransferCallback {
        void onTransferStarted(String fileName, long fileSize);
        void onTransferProgress(int percent);
        void onTransferComplete();
        void onTransferError(String error);
    }

    public void setCallback(FileTransferCallback callback) {
        this.callback = callback;
    }

    public void sendFile(final String targetIp, final File file) {
        if (file == null || !file.exists()) {
            Log.e(TAG, "❌ Dosya bulunamadı: " + (file != null ? file.getAbsolutePath() : "null"));
            if (callback != null) callback.onTransferError("Dosya bulunamadı");
            return;
        }

        new Thread(() -> {
            try {
                Log.d(TAG, "📤 Dosya gönderimi başlatılıyor: " + file.getName() + " -> " + targetIp);
                Log.d(TAG, "📊 Dosya boyutu: " + file.length() + " byte");

                if (callback != null) {
                    callback.onTransferStarted(file.getName(), file.length());
                }

                socket = new Socket(targetIp, TRANSFER_PORT);
                Log.d(TAG, "📡 Bağlandı: " + targetIp + ":" + TRANSFER_PORT);

                OutputStream outputStream = socket.getOutputStream();

                
                
                String originalName = file.getName();
                String encodedName = android.util.Base64.encodeToString(originalName.getBytes("UTF-8"), android.util.Base64.NO_WRAP);
                byte[] nameBytes = encodedName.getBytes("UTF-8");
                outputStream.write(nameBytes);
                outputStream.write(0); 
                Log.d(TAG, "📄 Orijinal ad: " + originalName + " → Encoded: " + encodedName);
                Log.d(TAG, "📄 Dosya adı gönderildi: " + file.getName());

                
                long fileSize = file.length();
                byte[] sizeBytes = new byte[8];
                sizeBytes[0] = (byte) (fileSize >> 56);
                sizeBytes[1] = (byte) (fileSize >> 48);
                sizeBytes[2] = (byte) (fileSize >> 40);
                sizeBytes[3] = (byte) (fileSize >> 32);
                sizeBytes[4] = (byte) (fileSize >> 24);
                sizeBytes[5] = (byte) (fileSize >> 16);
                sizeBytes[6] = (byte) (fileSize >> 8);
                sizeBytes[7] = (byte) fileSize;
                outputStream.write(sizeBytes);
                Log.d(TAG, "📊 Dosya boyutu gönderildi: " + fileSize);

                
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[8192];
                long totalSent = 0;
                int bytesRead;
                int lastPercent = -1;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalSent += bytesRead;

                    int percent = (int) (totalSent * 100 / fileSize);
                    if (percent != lastPercent && percent % 10 == 0) {
                        Log.d(TAG, "📤 İlerleme: " + percent + "% (" + totalSent + "/" + fileSize + ")");
                        lastPercent = percent;
                    }

                    if (callback != null) {
                        callback.onTransferProgress(percent);
                    }
                }

                fis.close();
                outputStream.flush();
                Log.d(TAG, "✅ Dosya gönderimi tamamlandı");

                if (callback != null) {
                    callback.onTransferComplete();
                }

                socket.close();

            } catch (IOException e) {
                Log.e(TAG, "❌ Dosya gönderme hatası: " + e.getMessage(), e);
                if (callback != null) {
                    callback.onTransferError(e.getMessage());
                }
            }
        }).start();
    }

    public void cancel() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                Log.d(TAG, "⏹️ Transfer iptal edildi");
            }
        } catch (IOException e) {
            Log.e(TAG, "❌ İptal hatası: " + e.getMessage());
        }
    }
}