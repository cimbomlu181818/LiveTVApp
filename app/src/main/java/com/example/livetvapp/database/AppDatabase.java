package com.example.livetvapp.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.example.livetvapp.Channel;
import com.example.livetvapp.stalker.StalkerCategoryProgress;
import com.example.livetvapp.stalker.StalkerCategoryProgressDao;
import com.example.livetvapp.stalker.StalkerPortal;
import com.example.livetvapp.stalker.StalkerPortalDao;

@Database(
        entities = {
                Channel.class,
                M3UItem.class,
                IzlemePozisyonu.class,
                StalkerPortal.class,
                StalkerCategoryProgress.class
        },
        version = 6,
        exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase INSTANCE;

    public abstract ChannelDao channelDao();
    public abstract M3UDao m3uDao();
    public abstract StalkerPortalDao stalkerPortalDao();
    public abstract StalkerCategoryProgressDao stalkerCategoryProgressDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "livetv_database"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return INSTANCE;
    }
}