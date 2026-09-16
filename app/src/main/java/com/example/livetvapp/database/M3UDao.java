package com.example.livetvapp.database;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;
@Dao
public interface M3UDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(M3UItem m3uItem);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<M3UItem> m3uItems);
    @Update
    void update(M3UItem m3uItem);
    @Delete
    void delete(M3UItem m3uItem);
    @Query("SELECT * FROM m3u_items ORDER BY addedDate DESC")
    List<M3UItem> getAllM3U();
    @Query("SELECT * FROM m3u_items WHERE isHidden = 0 ORDER BY addedDate DESC")
    List<M3UItem> getVisibleM3U();
    @Query("SELECT * FROM m3u_items WHERE name = :name LIMIT 1")
    M3UItem getM3UByName(String name);
    @Query("SELECT * FROM m3u_items WHERE id = :id LIMIT 1")
    M3UItem getM3UById(long id);
    @Query("DELETE FROM m3u_items WHERE name = :name")
    void deleteByName(String name);
    @Query("DELETE FROM m3u_items")
    void deleteAll();
    @Query("SELECT COUNT(*) FROM m3u_items")
    int getM3UCount();
    @Query("UPDATE m3u_items SET channelCount = :count WHERE name = :name")
    void updateChannelCount(String name, int count);
    @Query("UPDATE m3u_items SET isHidden = :isHidden WHERE name = :name")
    void updateIsHidden(String name, boolean isHidden);
    @Query("UPDATE m3u_items SET name = :yeniIsim WHERE name = :eskiIsim")
    void updateName(String eskiIsim, String yeniIsim);
}