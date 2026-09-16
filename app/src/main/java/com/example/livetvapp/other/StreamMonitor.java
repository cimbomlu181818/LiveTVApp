package com.example.livetvapp.other;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.videolan.libvlc.MediaPlayer;

public class StreamMonitor {
    private static final String TAG = "StreamMonitor";

    public enum Mode { IDLE, PRE_RETRY, MONITORING }

    private static final int PRE_RETRY_INTERVAL_MS = 5000;   
    private static final int MAX_PRE_RETRY_COUNT = 20;
    private static final int MONITORING_INTERVAL_MS = 5000;

    private Mode currentMode = Mode.IDLE;
    private Handler mainHandler;
    private Handler bgHandler;
    private StreamMonitorListener listener;

    private int preRetryAttempts = 0;
    private String currentStreamUrl;
    private long lastMediaTime = -1;
    private boolean isFreezeHandled = false;
    private boolean userPaused = false;
    private Runnable preRetryRunnable;
    private Runnable monitoringRunnable;

    public interface StreamMonitorListener {
        void onRetryNeeded(String url);
        void onStreamStarted();
        void onStreamFailed(String error);
        MediaPlayer getCurrentMediaPlayer();
        String getCurrentStreamUrl();
    }
    public void onUserPaused() {
        userPaused = true;
    }

    public void onUserResumed() {
        userPaused = false;
    }
    public StreamMonitor(StreamMonitorListener listener) {
        this.listener = listener;
        this.mainHandler = new Handler(Looper.getMainLooper());

        Thread bgThread = new Thread(() -> {
            Looper.prepare();
            bgHandler = new Handler(Looper.myLooper());
            Looper.loop();
        });
        bgThread.setDaemon(true);
        bgThread.start();

        while (bgHandler == null) {
            try { Thread.sleep(5); } catch (InterruptedException ignored) {}
        }
    }

    public void startWithIdle(String url) {
        stop();
        this.currentStreamUrl = url;
        this.currentMode = Mode.IDLE;
        this.preRetryAttempts = 0;
        this.lastMediaTime = -1;
        this.isFreezeHandled = false;
        Log.d(TAG, "IDLE modu başlatıldı, 15 saniye bekleniyor: " + url);
    }

    public void onTimeoutExpired() {
        if (currentMode == Mode.IDLE) {
            Log.i(TAG, "Timeout sonrası PRE_RETRY moduna geçiliyor");
            startPreRetry(currentStreamUrl);
        }
    }

    public void onStreamPlaying() {
        if (currentMode == Mode.PRE_RETRY) {
            Log.i(TAG, "Yayın başladı, MONITORING moduna geçiliyor");
            switchToMonitoring();
            if (listener != null) listener.onStreamStarted();
        } else if (currentMode == Mode.MONITORING) {
            isFreezeHandled = false;
            lastMediaTime = -1;
        } else if (currentMode == Mode.IDLE) {
            Log.i(TAG, "Yayın IDLE modunda başladı, MONITORING'e geçiliyor");
            switchToMonitoring();
            if (listener != null) listener.onStreamStarted();
        }
    }

    public void onStreamError() {
        if (currentMode == Mode.MONITORING || currentMode == Mode.IDLE) {
            Log.w(TAG, "Yayın hatası, PRE_RETRY moduna geçiliyor");
            
            mainHandler.post(() -> {
                if (listener instanceof com.example.livetvapp.tvbox.Anakontrol) {
                    
                }
            });
            startPreRetry(currentStreamUrl);
        }
    }

    private void startPreRetry(String url) {
        stopMonitoringTasks();
        currentMode = Mode.PRE_RETRY;
        currentStreamUrl = url;
        preRetryAttempts = 0;
        schedulePreRetry();
    }

    private void switchToMonitoring() {
        stopMonitoringTasks();
        currentMode = Mode.MONITORING;
        lastMediaTime = -1;
        isFreezeHandled = false;
        scheduleMonitoring();
    }

    private void schedulePreRetry() {
        if (currentMode != Mode.PRE_RETRY) return;

        preRetryRunnable = () -> {
            if (currentMode != Mode.PRE_RETRY) return;

            if (preRetryAttempts >= MAX_PRE_RETRY_COUNT) {
                Log.e(TAG, "Maksimum pre-retry deneme sayısı aşıldı: " + MAX_PRE_RETRY_COUNT);
                stop();
                mainHandler.post(() -> {
                    if (listener != null) listener.onStreamFailed("Yayın açılamadı (çok fazla deneme)");
                });
                return;
            }

            preRetryAttempts++;
            Log.i(TAG, "PRE_RETRY denemesi #" + preRetryAttempts + " - URL: " + currentStreamUrl);
            mainHandler.post(() -> {
                if (listener != null) listener.onRetryNeeded(currentStreamUrl);
            });

            if (currentMode == Mode.PRE_RETRY) {
                bgHandler.postDelayed(this::schedulePreRetry, PRE_RETRY_INTERVAL_MS);
            }
        };
        bgHandler.post(preRetryRunnable);
    }

    private void scheduleMonitoring() {
        System.out.println("StreamMonitor: scheduleMonitoring çalıştı, mod=" + currentMode);
        if (currentMode != Mode.MONITORING) return;

        monitoringRunnable = () -> {
            if (currentMode != Mode.MONITORING) return;

            MediaPlayer mp = (listener != null) ? listener.getCurrentMediaPlayer() : null;
            if (mp == null) {
                scheduleMonitoringLater();
                return;
            }

            boolean playing = mp.isPlaying();
            long currentTime = mp.getTime();

            if (!playing) {
                if (userPaused) {
                    
                    lastMediaTime = -1;
                    scheduleMonitoringLater();
                    return;
                }
                Log.w(TAG, "MONITORING: isPlaying false -> donma tespiti");
                handleFreeze();
                return;
            }

            if (currentTime > 0 && lastMediaTime > 0 && currentTime == lastMediaTime) {
                Log.w(TAG, "MONITORING: zaman ilerlemiyor -> donma tespiti");
                handleFreeze();
                return;
            }

            lastMediaTime = currentTime;
            scheduleMonitoringLater();
        };
        bgHandler.post(monitoringRunnable);
    }

    private void handleFreeze() {
        if (isFreezeHandled) return;
        isFreezeHandled = true;
        Log.w(TAG, "Stream dondu, restart için PRE_RETRY moduna geçiliyor");

        mainHandler.post(() -> {
            MediaPlayer mp = (listener != null) ? listener.getCurrentMediaPlayer() : null;
            if (mp != null) mp.stop();
        });
        startPreRetry(currentStreamUrl);
    }

    private void scheduleMonitoringLater() {
        if (currentMode == Mode.MONITORING && bgHandler != null) {
            bgHandler.postDelayed(monitoringRunnable, MONITORING_INTERVAL_MS);
        }
    }

    private void stopMonitoringTasks() {
        if (preRetryRunnable != null && bgHandler != null) bgHandler.removeCallbacks(preRetryRunnable);
        if (monitoringRunnable != null && bgHandler != null) bgHandler.removeCallbacks(monitoringRunnable);
        preRetryRunnable = null;
        monitoringRunnable = null;
    }

    public void stop() {
        stopMonitoringTasks();
        currentMode = Mode.IDLE;
        preRetryAttempts = 0;
        currentStreamUrl = null;
        lastMediaTime = -1;
        isFreezeHandled = false;
        Log.d(TAG, "StreamMonitor durduruldu");
    }

    public boolean isActive() {
        return currentMode != Mode.IDLE;
    }

    public void cleanup() {
        stop();
        if (bgHandler != null) {
            bgHandler.getLooper().quit();
        }
    }
}