package com.example.livetvapp.stalker;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "stalker_category_progress")
public class StalkerCategoryProgress {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String m3uName;
    @NonNull
    private String category;
    
    @NonNull
    private String categoryId = "";
    private int lastFetchedPage;
    private int totalPages;
    private int totalItems;
    private long lastSyncTime;

    public StalkerCategoryProgress() {}

    public StalkerCategoryProgress(@NonNull String m3uName, @NonNull String category) {
        this.m3uName = m3uName;
        this.category = category;
        this.categoryId = "";
        this.lastFetchedPage = -1;
        this.totalPages = -1;
        this.totalItems = -1;
        this.lastSyncTime = System.currentTimeMillis();
    }

    
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    @NonNull public String getM3uName() { return m3uName; }
    public void setM3uName(@NonNull String m3uName) { this.m3uName = m3uName; }
    @NonNull public String getCategory() { return category; }
    public void setCategory(@NonNull String category) { this.category = category; }
    @NonNull public String getCategoryId() { return categoryId; }
    public void setCategoryId(@NonNull String categoryId) { this.categoryId = categoryId; }
    public int getLastFetchedPage() { return lastFetchedPage; }
    public void setLastFetchedPage(int lastFetchedPage) { this.lastFetchedPage = lastFetchedPage; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public int getTotalItems() { return totalItems; }
    public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
    public long getLastSyncTime() { return lastSyncTime; }
    public void setLastSyncTime(long lastSyncTime) { this.lastSyncTime = lastSyncTime; }
}