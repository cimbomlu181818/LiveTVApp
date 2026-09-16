package com.example.livetvapp.database;
import android.content.Context;
import com.example.livetvapp.Channel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class DatabaseHelper {
    private static final ExecutorService databaseExecutor = Executors.newFixedThreadPool(4);
    private final Context context;
    private final AppDatabase database;
    public DatabaseHelper(Context context) {
        this.database = AppDatabase.getInstance(context);
        this.context = context;
    }
    public void execute(Runnable task) {
        databaseExecutor.execute(task);
    }
    public void insertChannels(List<Channel> channels, OnCompleteListener listener) {
        databaseExecutor.execute(() -> {
            try {
                database.channelDao().insertAll(channels);
                if (listener != null) listener.onSuccess();
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }
    public void getChannelsByM3U(String m3uName, int limit, int offset, OnChannelsLoadedListener listener) {
        databaseExecutor.execute(() -> {
            try {
                List<Channel> channels = database.channelDao().getChannelsByM3UPaginated(m3uName, limit, offset);
                if (listener != null) listener.onChannelsLoaded(channels);
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }
    public void getAllChannelsByM3U(String m3uName, OnChannelsLoadedListener listener) {
        databaseExecutor.execute(() -> {
            try {
                List<Channel> channels = database.channelDao().getAllChannelsByM3U(m3uName);
                if (listener != null) listener.onChannelsLoaded(channels);
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }
    public void getAllChannelsByType(String m3uName, String contentType, OnChannelsLoadedListener listener) {
        databaseExecutor.execute(() -> {
            try {
                List<Channel> channels;
                if (m3uName == null || m3uName.isEmpty()) {
                    channels = database.channelDao().getAllChannelsByTypeAllM3U(contentType);
                } else {
                    channels = database.channelDao().getAllChannelsByType(m3uName, contentType);
                }
                if (listener != null) listener.onChannelsLoaded(channels);
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }
    public void getChannelsByTypeAndCategory(String m3uName, String contentType, String category,
                                             OnChannelsLoadedListener listener) {
        databaseExecutor.execute(() -> {
            try {
                List<Channel> channels;
                if (m3uName == null || m3uName.isEmpty()) {
                    channels = database.channelDao().getChannelsByTypeAndCategoryAllM3U(contentType, category);
                } else {
                    channels = database.channelDao().getChannelsByTypeAndCategory(m3uName, contentType, category);
                }
                if (listener != null) listener.onChannelsLoaded(channels);
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }
    public void getCategoriesByType(String m3uName, String contentType, OnCategoriesLoadedListener listener) {
        databaseExecutor.execute(() -> {
            try {
                List<String> categories;
                if (m3uName == null || m3uName.isEmpty()) {
                    categories = database.channelDao().getCategoriesByTypeAllM3U(contentType);
                } else {
                    categories = database.channelDao().getCategoriesByType(m3uName, contentType);
                }
                if (listener != null) listener.onCategoriesLoaded(categories);
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }
    public void getCategoriesWithSource(String m3uName, String contentType,
                                        OnKategoriItemsLoadedListener listener) {
        databaseExecutor.execute(() -> {
            try {
                List<ChannelDao.KategoriWithSource> rows;
                if (m3uName == null || m3uName.isEmpty()) {
                    rows = database.channelDao().getCategoriesWithSourceAllM3U(contentType);
                } else {
                    rows = database.channelDao().getCategoriesWithSourceByType(m3uName, contentType);
                }
                List<KategoriItem> items = new ArrayList<>();
                for (ChannelDao.KategoriWithSource row : rows) {
                    if (row.category != null && !row.category.isEmpty()) {
                        items.add(new KategoriItem(row.category, row.sourceName));
                    }
                }
                android.content.SharedPreferences prefs =
                        context.getSharedPreferences("kategori_siralama", Context.MODE_PRIVATE);
                String kayit = prefs.getString(contentType + "_sirasi", null);
                if (kayit != null && !kayit.isEmpty()) {
                    String[] parcalar = kayit.split("###");
                    List<String> kayitliSira = new ArrayList<>();
                    for (int i = 1; i < parcalar.length; i += 2) {
                        kayitliSira.add(parcalar[i]);
                    }
                    List<KategoriItem> sirali = new ArrayList<>();
                    for (String katAdi : kayitliSira) {
                        for (KategoriItem item : items) {
                            if (item.getKategoriAdi().equals(katAdi)) {
                                sirali.add(item);
                                break;
                            }
                        }
                    }
                    for (KategoriItem item : items) {
                        boolean var = false;
                        for (KategoriItem s : sirali) {
                            if (s.getKategoriAdi().equals(item.getKategoriAdi())) {
                                var = true;
                                break;
                            }
                        }
                        if (!var) sirali.add(item);
                    }
                    items = sirali;
                }
                if (listener != null) listener.onKategoriItemsLoaded(items);
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }
    public void getCategoriesWithSourcePaged(String m3uName, String contentType,
                                             int limit, int offset,
                                             OnKategoriItemsLoadedListener listener) {
        execute(() -> {
            try {
                List<ChannelDao.KategoriWithSource> raw;
                if (m3uName == null || m3uName.isEmpty()) {
                    raw = database.channelDao()
                            .getCategoriesWithSourceAllM3UPaged(contentType, limit, offset);
                } else {
                    raw = database.channelDao()
                            .getCategoriesWithSourceByTypePaged(m3uName, contentType, limit, offset);
                }
                List<KategoriItem> items = new ArrayList<>();
                for (ChannelDao.KategoriWithSource row : raw) {
                    items.add(new KategoriItem(
                            row.category   != null ? row.category   : "",
                            row.sourceName != null ? row.sourceName : ""));
                }
                if (listener != null) listener.onKategoriItemsLoaded(items);
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }
    public void getChannelByUrl(String url, OnChannelLoadedListener listener) {
        databaseExecutor.execute(() -> {
            try {
                Channel channel = database.channelDao().getChannelByUrl(url);
                if (listener != null) listener.onChannelLoaded(channel);
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }
    public void deleteChannelsByM3U(String m3uName, OnCompleteListener listener) {
        databaseExecutor.execute(() -> {
            try {
                database.channelDao().deleteChannelsByM3U(m3uName);
                if (listener != null) listener.onSuccess();
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }
    public void getChannelCount(String m3uName, OnCountLoadedListener listener) {
        databaseExecutor.execute(() -> {
            try {
                int count = database.channelDao().getChannelCount(m3uName);
                if (listener != null) listener.onCountLoaded(count);
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }
    public void insertM3U(M3UItem m3uItem, OnM3UInsertedListener listener) {
        databaseExecutor.execute(() -> {
            try {
                long id = database.m3uDao().insert(m3uItem);
                if (listener != null) listener.onM3UInserted(id);
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }
    public void getAllM3U(OnM3UListLoadedListener listener) {
        databaseExecutor.execute(() -> {
            try {
                List<M3UItem> items = database.m3uDao().getAllM3U();
                if (listener != null) listener.onM3UListLoaded(items);
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }
    public void deleteM3U(String m3uName, OnCompleteListener listener) {
        databaseExecutor.execute(() -> {
            try {
                database.m3uDao().deleteByName(m3uName);
                database.channelDao().deleteChannelsByM3U(m3uName);
                if (listener != null) listener.onSuccess();
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }
    public void updateChannelCount(String m3uName, int count, OnCompleteListener listener) {
        databaseExecutor.execute(() -> {
            try {
                database.m3uDao().updateChannelCount(m3uName, count);
                if (listener != null) listener.onSuccess();
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }
    public interface OnCompleteListener {
        void onSuccess();
        void onError(Exception e);
    }
    public interface OnChannelsLoadedListener {
        void onChannelsLoaded(List<Channel> channels);
        void onError(Exception e);
    }
    public interface OnChannelLoadedListener {
        void onChannelLoaded(Channel channel);
        void onError(Exception e);
    }
    public interface OnCategoriesLoadedListener {
        void onCategoriesLoaded(List<String> categories);
        void onError(Exception e);
    }
    public interface OnKategoriItemsLoadedListener {
        void onKategoriItemsLoaded(List<KategoriItem> items);
        void onError(Exception e);
    }
    public interface OnCountLoadedListener {
        void onCountLoaded(int count);
        void onError(Exception e);
    }
    public interface OnM3UInsertedListener {
        void onM3UInserted(long id);
        void onError(Exception e);
    }
    public interface OnM3UListLoadedListener {
        void onM3UListLoaded(List<M3UItem> items);
        void onError(Exception e);
    }
}