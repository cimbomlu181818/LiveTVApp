package com.example.livetvapp.dosyatransferi;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileTransferServer {
    private static final String TAG = "📥[FileServer]";
    private static final int TRANSFER_PORT = 8892;

    private Context context;
    private ExecutorService executorService;
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private FileTransferCallback callback;

    public interface FileTransferCallback {
        void onTransferStarted(String fileName, long fileSize);
        void onTransferProgress(int percent);
        void onTransferComplete(String filePath);
        void onTransferError(String error);
    }

    public FileTransferServer(Context context) {
        this.context = context;
        this.executorService = Executors.newCachedThreadPool();
        Log.d(TAG, "🔧 FileTransferServer oluşturuldu");
    }

    public void setCallback(FileTransferCallback callback) {
        this.callback = callback;
    }

    public void start() {
        if (isRunning) {
            Log.d(TAG, "⚠️ Server zaten çalışıyor");
            return;
        }
        isRunning = true;
        executorService.execute(this::listenForConnections);
        Log.d(TAG, "✅ FileTransferServer başlatıldı, port: " + TRANSFER_PORT);
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                Log.d(TAG, "📡 Server socket kapatıldı");
            }
        } catch (IOException e) {
            Log.e(TAG, "❌ Server socket kapatma hatası: " + e.getMessage());
        }
        Log.d(TAG, "✅ FileTransferServer durduruldu");
    }

    private void listenForConnections() {
        try {
            serverSocket = new ServerSocket(TRANSFER_PORT);
            Log.d(TAG, "📡 Dinleniyor: " + TRANSFER_PORT);

            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    String clientIP = clientSocket.getInetAddress().getHostAddress();
                    Log.d(TAG, "📡 Bağlantı alındı: " + clientIP);
                    executorService.execute(() -> receiveFile(clientSocket));
                } catch (IOException e) {
                    if (isRunning) {
                        Log.e(TAG, "❌ Bağlantı kabul hatası: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "❌ Server başlatma hatası: " + e.getMessage(), e);
        }
    }

    private void receiveFile(Socket socket) {
        try {
            InputStream inputStream = socket.getInputStream();

            
            StringBuilder nameBuilder = new StringBuilder();
            int ch;
            while ((ch = inputStream.read()) != 0) {
                if (ch == -1) throw new IOException("Dosya adı okunamadı");
                nameBuilder.append((char) ch);
            }
            String encodedName = nameBuilder.toString();

            
            String fileName;
            try {
                byte[] decodedBytes = android.util.Base64.decode(encodedName, android.util.Base64.DEFAULT);
                fileName = new String(decodedBytes, "UTF-8");
                Log.d(TAG, "📄 Orijinal ad: " + fileName + " (Encoded: " + encodedName + ")");
            } catch (Exception e) {
                fileName = encodedName;
                Log.w(TAG, "⚠️ Base64 decode hatası, ham ad kullanıldı: " + fileName);
            }

            
            byte[] sizeBytes = new byte[8];
            int totalRead = 0;
            while (totalRead < 8) {
                int read = inputStream.read(sizeBytes, totalRead, 8 - totalRead);
                if (read == -1) throw new IOException("Dosya boyutu okunamadı");
                totalRead += read;
            }
            long fileSize = ((long)(sizeBytes[0] & 0xFF) << 56)
                    | ((long)(sizeBytes[1] & 0xFF) << 48)
                    | ((long)(sizeBytes[2] & 0xFF) << 40)
                    | ((long)(sizeBytes[3] & 0xFF) << 32)
                    | ((long)(sizeBytes[4] & 0xFF) << 24)
                    | ((long)(sizeBytes[5] & 0xFF) << 16)
                    | ((long)(sizeBytes[6] & 0xFF) <<  8)
                    |  (long)(sizeBytes[7] & 0xFF);
            Log.d(TAG, "📊 Dosya boyutu: " + fileSize + " byte");

            if (callback != null) callback.onTransferStarted(fileName, fileSize);

            
            String nameWithoutExt = fileName;
            String extension = "";
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0) {
                nameWithoutExt = fileName.substring(0, dotIndex);
                extension = fileName.substring(dotIndex);
            }

            
            OutputStream outputStream;
            String finalPath;
            File outputFile = null;  

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                
                String uniqueName = fileName;
                int counter = 1;

                Uri fileUri = null;
                while (fileUri == null) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, uniqueName);
                    values.put(MediaStore.Downloads.MIME_TYPE, getMimeType(uniqueName));
                    values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                    try {
                        fileUri = context.getContentResolver()
                                .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                        if (fileUri == null) {
                            uniqueName = nameWithoutExt + " (" + counter + ")" + extension;
                            counter++;
                            Log.w(TAG, "⚠️ insert null döndü, yeni isim deneniyor: " + uniqueName);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "⚠️ insert hatası: " + e.getMessage() + ", yeni isim: " + uniqueName);
                        uniqueName = nameWithoutExt + " (" + counter + ")" + extension;
                        counter++;
                        if (counter > 100) throw new IOException("MediaStore insert sürekli başarısız");
                    }
                }

                outputStream = context.getContentResolver().openOutputStream(fileUri);
                if (outputStream == null) throw new IOException("openOutputStream null döndü");
                finalPath = fileUri.toString();
                
                Log.d(TAG, "📁 MediaStore URI: " + finalPath);

            } else {
                
                File downloadDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS);

                if (!downloadDir.exists()) {
                    boolean created = downloadDir.mkdirs();
                    Log.d(TAG, "📁 Downloads klasörü oluşturuldu: " + created);
                }

                if (!downloadDir.canWrite()) {
                    Log.w(TAG, "⚠️ Downloads klasörüne yazma izni yok! Fallback: app klasörü");
                    downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                    if (downloadDir == null) downloadDir = context.getFilesDir();
                    if (!downloadDir.exists()) downloadDir.mkdirs();
                    Log.w(TAG, "⚠️ Fallback klasör: " + downloadDir.getAbsolutePath());
                }

                String uniqueFileName = fileName;
                outputFile = new File(downloadDir, uniqueFileName);
                int counter = 1;
                while (outputFile.exists()) {
                    uniqueFileName = nameWithoutExt + " (" + counter + ")" + extension;
                    outputFile = new File(downloadDir, uniqueFileName);
                    counter++;
                }

                outputStream = new FileOutputStream(outputFile);
                finalPath = outputFile.getAbsolutePath();
                Log.d(TAG, "📁 Kayıt yolu: " + finalPath);
            }

            
            byte[] buffer = new byte[8192];
            long written = 0;
            int bytesRead;
            int lastPercent = -1;

            while (written < fileSize && (bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                written += bytesRead;

                if (fileSize > 0) {
                    int percent = (int)(written * 100 / fileSize);
                    if (percent != lastPercent) {
                        if (percent % 10 == 0)
                            Log.d(TAG, "📥 İlerleme: " + percent + "% (" + written + "/" + fileSize + ")");
                        lastPercent = percent;
                        if (callback != null) callback.onTransferProgress(percent);
                    }
                }
            }

            outputStream.flush();
            outputStream.close();

            
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && outputFile != null) {
                android.media.MediaScannerConnection.scanFile(context,
                        new String[]{outputFile.getAbsolutePath()},
                        null, null);
            }

            socket.close();
            Log.d(TAG, "✅ Dosya kaydedildi: " + finalPath + " (" + written + " byte)");
            if (callback != null) callback.onTransferComplete(finalPath);

        } catch (IOException e) {
            Log.e(TAG, "❌ Dosya alma hatası: " + e.getMessage(), e);
            if (callback != null) callback.onTransferError(e.getMessage());
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private String getMimeType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            switch (fileName.substring(dotIndex + 1).toLowerCase()) {
                case "txt":  return "text/plain";
                case "jpg":
                case "jpeg": return "image/jpeg";
                case "png":  return "image/png";
                case "gif":  return "image/gif";
                case "mp4":  return "video/mp4";
                case "mkv":  return "video/x-matroska";
                case "avi":  return "video/x-msvideo";
                case "mp3":  return "audio/mpeg";
                case "flac": return "audio/flac";
                case "aac":  return "audio/aac";
                case "pdf":  return "application/pdf";
                case "apk":  return "application/vnd.android.package-archive";
                case "zip":  return "application/zip";
                case "m3u":
                case "m3u8": return "application/x-mpegURL";
            }
        }
        return "application/octet-stream";
    }

    public boolean isRunning() {
        return isRunning;
    }
}