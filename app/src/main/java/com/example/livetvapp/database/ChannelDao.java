package com.example.livetvapp.database;

import androidx.paging.PagingSource;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.livetvapp.Channel;

import java.util.List;

@Dao
public interface ChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Channel channel);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Channel> channels);

    @Update
    void update(Channel channel);

    @Delete
    void delete(Channel channel);

    @Query("SELECT * FROM channels WHERE sourceName = :m3uName ORDER BY position ASC")
    PagingSource<Integer, Channel> getChannelsByM3UPaged(String m3uName);

    @Query("SELECT * FROM channels WHERE sourceName = :m3uName ORDER BY position ASC LIMIT :limit OFFSET :offset")
    List<Channel> getChannelsByM3UPaginated(String m3uName, int limit, int offset);

    @Query("SELECT * FROM channels WHERE sourceName = :m3uName ORDER BY position ASC")
    List<Channel> getAllChannelsByM3U(String m3uName);

    @Query("SELECT * FROM channels WHERE sourceName = :m3uName AND category = :category ORDER BY position ASC")
    List<Channel> getChannelsByCategory(String m3uName, String category);

    @Query("SELECT * FROM channels WHERE sourceName = :m3uName AND contentType = :contentType ORDER BY position ASC")
    List<Channel> getAllChannelsByType(String m3uName, String contentType);

    @Query("SELECT * FROM channels WHERE sourceName = :m3uName AND contentType = :contentType AND category = :category ORDER BY position ASC")
    List<Channel> getChannelsByTypeAndCategory(String m3uName, String contentType, String category);

    @Query("SELECT * FROM channels WHERE sourceName = :m3uName AND contentType = :contentType AND category = :category ORDER BY position ASC LIMIT :limit OFFSET :offset")
    List<Channel> getChannelsByTypeAndCategoryPaged(String m3uName, String contentType, String category, int limit, int offset);

    @Query("SELECT * FROM channels WHERE contentType = :contentType AND category = :category ORDER BY position ASC LIMIT :limit OFFSET :offset")
    List<Channel> getChannelsByTypeAndCategoryAllM3UPaged(String contentType, String category, int limit, int offset);

    @Query("SELECT category FROM channels WHERE sourceName = :m3uName AND contentType = :contentType GROUP BY category ORDER BY MIN(position) ASC")
    List<String> getCategoriesByType(String m3uName, String contentType);

    @Query("SELECT DISTINCT category FROM channels WHERE sourceName = :m3uName GROUP BY category ORDER BY MIN(position) ASC")
    List<String> getAllCategoriesByM3U(String m3uName);

    @Query("SELECT * FROM channels WHERE contentType = :contentType ORDER BY position ASC")
    List<Channel> getAllChannelsByTypeAllM3U(String contentType);

    @Query("SELECT * FROM channels WHERE contentType = :contentType AND category = :category ORDER BY position ASC")
    List<Channel> getChannelsByTypeAndCategoryAllM3U(String contentType, String category);

    @Query("SELECT category FROM channels WHERE contentType = :contentType GROUP BY category ORDER BY MIN(position) ASC")
    List<String> getCategoriesByTypeAllM3U(String contentType);

    @Query("SELECT sourceName, category FROM channels " +
            "WHERE sourceName = :m3uName AND contentType = :contentType " +
            "GROUP BY sourceName, category ORDER BY MIN(position) ASC")
    List<KategoriWithSource> getCategoriesWithSourceByType(String m3uName, String contentType);

    @Query("SELECT sourceName, category FROM channels " +
            "WHERE contentType = :contentType " +
            "GROUP BY sourceName, category ORDER BY (SELECT id FROM m3u_items WHERE name = sourceName) ASC, MIN(position) ASC")
    List<KategoriWithSource> getCategoriesWithSourceAllM3U(String contentType);

    @Query("SELECT sourceName, category FROM channels " +
            "WHERE sourceName = :m3uName AND contentType = :contentType " +
            "GROUP BY sourceName, category ORDER BY MIN(position) ASC, sourceName ASC, category ASC LIMIT :limit OFFSET :offset")
    List<KategoriWithSource> getCategoriesWithSourceByTypePaged(String m3uName, String contentType, int limit, int offset);

    @Query("SELECT sourceName, category FROM channels " +
            "WHERE contentType = :contentType " +
            "GROUP BY sourceName, category ORDER BY MIN(position) ASC, sourceName ASC, category ASC LIMIT :limit OFFSET :offset")
    List<KategoriWithSource> getCategoriesWithSourceAllM3UPaged(String contentType, int limit, int offset);

    class KategoriWithSource {
        @androidx.room.ColumnInfo(name = "sourceName")
        public String sourceName;
        @androidx.room.ColumnInfo(name = "category")
        public String category;
    }

    @androidx.room.Query("SELECT category, COUNT(*) AS channelCount FROM channels " +
            "WHERE contentType = :contentType GROUP BY category ORDER BY MIN(position) ASC")
    List<KategoriSayisi> getCategoryCountsForType(String contentType);

    @androidx.room.Query("SELECT contentType, category, COUNT(*) AS channelCount FROM channels " +
            "GROUP BY contentType, category ORDER BY contentType, MIN(position) ASC")
    List<KategoriSayisi> getAllCategoryCounts();

    class KategoriSayisi {
        @androidx.room.ColumnInfo(name = "category")
        public String category;
        @androidx.room.ColumnInfo(name = "channelCount")
        public int channelCount;
        @androidx.room.ColumnInfo(name = "contentType")
        public String contentType;
    }

    @Query("SELECT DISTINCT sourceName, category, contentType, MIN(position) AS position, id, url, logo, name FROM channels GROUP BY sourceName, category ORDER BY sourceName, MIN(position) ASC")
    List<Channel> getDistinctM3UCategoryList();

    @Query("SELECT url FROM channels")
    List<String> getAllUrls();

    @Query("SELECT url FROM channels WHERE sourceName = :m3uName")
    List<String> getAllUrlsByM3U(String m3uName);

    @Query("SELECT * FROM channels WHERE url = :url LIMIT 1")
    Channel getChannelByUrl(String url);

    @Query("SELECT COUNT(*) FROM channels WHERE sourceName = :m3uName")
    int getChannelCount(String m3uName);

    @Query("DELETE FROM channels WHERE sourceName = :m3uName")
    void deleteChannelsByM3U(String m3uName);

    @Query("DELETE FROM channels")
    void deleteAll();

    @Query("SELECT * FROM channels WHERE url IN (:urls)")
    List<Channel> getChannelsByUrls(List<String> urls);

    @Query("SELECT DISTINCT category FROM channels WHERE sourceName = :m3uName ORDER BY category ASC")
    List<String> getCategories(String m3uName);

    @Query("SELECT * FROM channels WHERE sourceName = :m3uName AND name LIKE '%' || :query || '%' ORDER BY position ASC")
    List<Channel> searchChannels(String m3uName, String query);

    @Query("SELECT * FROM channels WHERE name LIKE '%' || :query || '%' ORDER BY contentType ASC, position ASC")
    List<Channel> searchChannelsAllTypes(String query);

    @Query("SELECT * FROM channels WHERE name LIKE '%' || :query || '%' AND contentType = :contentType ORDER BY position ASC LIMIT :limit OFFSET :offset")
    List<Channel> searchChannelsByTypePaged(String query, String contentType, int limit, int offset);

    @Query("UPDATE channels SET position = :yeniPozisyon WHERE id = :kanalId")
    void updateChannelPosition(long kanalId, int yeniPozisyon);

    @Query("UPDATE channels SET position = :yeniPozisyon WHERE id = :kanalId AND contentType = :contentType AND category = :category")
    void updateChannelPositionFull(long kanalId, int yeniPozisyon, String contentType, String category);

    @Query("UPDATE channels SET position = id")
    void resetAllPositions();

    @Query("DELETE FROM izleme_pozisyonu")
    void deleteAllPlaybackPositions();

    @Query("DELETE FROM channels WHERE sourceName = :sourceName")
    void deleteBySourceName(String sourceName);

    @Query("SELECT COUNT(*) FROM channels WHERE sourceName = :sourceName")
    int getChannelCountBySource(String sourceName);

    @Query("UPDATE channels SET sourceName = :yeniIsim WHERE sourceName = :eskiIsim")
    void updateSourceName(String eskiIsim, String yeniIsim);

    @androidx.room.Insert(onConflict = OnConflictStrategy.REPLACE)
    void pozisyonKaydet(IzlemePozisyonu pozisyon);

    @androidx.room.Query("SELECT * FROM izleme_pozisyonu WHERE url = :url LIMIT 1")
    IzlemePozisyonu pozisyonGetir(String url);

    @androidx.room.Query("DELETE FROM izleme_pozisyonu WHERE url = :url")
    void pozisyonSil(String url);

    
    @Query("SELECT * FROM izleme_pozisyonu")
    List<IzlemePozisyonu> getAllPlaybackPositions();
    @Query("SELECT * FROM channels WHERE name = :name AND contentType = 'SERIES' LIMIT 1")
    Channel getChannelByName(String name);
    @Query("SELECT COUNT(*) FROM channels WHERE sourceName = :sourceName AND contentType = :contentType AND category = :category AND position <= :position")
    int getKategoriIciSira(String sourceName, String contentType, String category, int position);
}