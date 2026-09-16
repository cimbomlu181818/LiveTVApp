package com.example.livetvapp.database;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
public class BackgroundTaskManager {
    private static BackgroundTaskManager instance;
    private final ExecutorService executorService;
    private BackgroundTaskManager() {
        int threadCount = Runtime.getRuntime().availableProcessors();
        this.executorService = Executors.newFixedThreadPool(threadCount);
    }
    public static synchronized BackgroundTaskManager getInstance() {
        if (instance == null) {
            instance = new BackgroundTaskManager();
        }
        return instance;
    }
    public Future<?> execute(Runnable task) {
        return executorService.submit(task);
    }
    public <T> Future<T> execute(java.util.concurrent.Callable<T> task) {
        return executorService.submit(task);
    }
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}