package com.example.livetvapp.stalker;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface StalkerPortalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(StalkerPortal portal);

    @Update
    void update(StalkerPortal portal);

    @Query("SELECT * FROM stalker_portals WHERE isActive = 1")
    List<StalkerPortal> getActivePortals();

    @Query("SELECT * FROM stalker_portals WHERE id = :id")
    StalkerPortal getById(long id);

    @Query("DELETE FROM stalker_portals WHERE id = :id")
    void deleteById(long id);

    
    @Query("DELETE FROM stalker_portals")
    void deleteAll();
}