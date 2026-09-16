package com.example.livetvapp.database;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
@Entity(tableName = "izleme_pozisyonu")
public class IzlemePozisyonu {
    @PrimaryKey
    @NonNull
    public String url;          
    public long pozisyonMs;     
    public long kaydedilmeZamani; 
    public IzlemePozisyonu(@NonNull String url, long pozisyonMs) {
        this.url               = url;
        this.pozisyonMs        = pozisyonMs;
        this.kaydedilmeZamani  = System.currentTimeMillis();
    }
}