package com.example.livetvapp.database;
import android.content.Context;
import android.content.SharedPreferences;
import com.example.livetvapp.KanalListesi;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
public class M3UManager {
    private static final String PREFS_NAME    = "M3UManager";
    private static final String M3U_LIST_KEY  = "m3u_list";
    private static final String ACTIVE_M3U_KEY = "active_m3u";
    private Context context;
    private SharedPreferences prefs;
    private Gson gson;
    private DatabaseHelper databaseHelper;
    public M3UManager(Context context) {
        this.context        = context;
        this.prefs          = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson           = new Gson();
        this.databaseHelper = new DatabaseHelper(context);
    }
    public List<M3UItem> getAllM3UItems() {
        String json = prefs.getString(M3U_LIST_KEY, "[]");
        Type type = new TypeToken<List<M3UItem>>() {}.getType();
        List<M3UItem> items = gson.fromJson(json, type);
        if (items == null) items = new ArrayList<>();
        return items;
    }
    public List<M3UItem> getVisibleM3UItems() {
        List<M3UItem> visible = new ArrayList<>();
        for (M3UItem item : getAllM3UItems()) {
            if (!item.isHidden()) visible.add(item);
        }
        return visible;
    }
    public List<String> getHiddenM3UNames() {
        List<String> hidden = new ArrayList<>();
        for (M3UItem item : getAllM3UItems()) {
            if (item.isHidden()) hidden.add(item.getName());
        }
        return hidden;
    }
    public void addM3U(M3UItem m3uItem) {
        List<M3UItem> items = getAllM3UItems();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getName().equals(m3uItem.getName())) {
                items.set(i, m3uItem);
                saveM3UList(items);
                saveToDatabase(m3uItem);
                return;
            }
        }
        String orijinalIsim = m3uItem.getName();
        String yeniIsim = orijinalIsim;
        int sayac = 2;
        while (hasM3U(yeniIsim)) {
            yeniIsim = orijinalIsim + sayac;
            sayac++;
        }
        m3uItem.setName(yeniIsim);
        items.add(m3uItem);
        saveM3UList(items);
        saveToDatabase(m3uItem);
    }
    private void saveToDatabase(M3UItem m3uItem) {
        databaseHelper.insertM3U(m3uItem, new DatabaseHelper.OnM3UInsertedListener() {
            @Override
            public void onM3UInserted(long id) {
                System.out.println("✅ M3U database'e eklendi: " + m3uItem.getName() + " (ID: " + id + ")");
            }
            @Override
            public void onError(Exception e) {
                System.out.println("❌ M3U database ekleme hatası: " + e.getMessage());
            }
        });
    }
    public boolean deleteM3U(String m3uName) {
        List<M3UItem> items = getAllM3UItems();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getName().equals(m3uName)) {
                items.remove(i);
                saveM3UList(items);
                String activeM3U = getActiveM3UName();
                if (activeM3U.equals(m3uName)) {
                    List<M3UItem> remaining = getAllM3UItems();
                    setActiveM3U(!remaining.isEmpty() ? remaining.get(0).getName() : "");
                }
                deleteFromDatabase(m3uName);
                return true;
            }
        }
        return false;
    }
    private void deleteFromDatabase(String m3uName) {
        databaseHelper.deleteM3U(m3uName, new DatabaseHelper.OnCompleteListener() {
            @Override public void onSuccess() { System.out.println("✅ M3U database'den silindi: " + m3uName); }
            @Override public void onError(Exception e) { System.out.println("❌ M3U database silme hatası: " + e.getMessage()); }
        });
    }
    public int deleteMultipleM3U(List<String> m3uNames) {
        int deletedCount = 0;
        List<M3UItem> items    = getAllM3UItems();
        List<M3UItem> newItems = new ArrayList<>();
        for (M3UItem item : items) {
            if (m3uNames.contains(item.getName())) {
                deletedCount++;
                deleteFromDatabase(item.getName());
            } else {
                newItems.add(item);
            }
        }
        if (deletedCount > 0) {
            saveM3UList(newItems);
            String activeM3U = getActiveM3UName();
            if (m3uNames.contains(activeM3U)) {
                setActiveM3U(!newItems.isEmpty() ? newItems.get(0).getName() : "");
            }
        }
        return deletedCount;
    }
    public void setM3UVisibility(String m3uName, boolean isVisible) {
        List<M3UItem> items = getAllM3UItems();
        for (M3UItem item : items) {
            if (item.getName().equals(m3uName)) {
                item.setHidden(!isVisible);
                break;
            }
        }
        saveM3UList(items);
        new Thread(() -> {
            try {
                AppDatabase.getInstance(context).m3uDao().updateIsHidden(m3uName, !isVisible);
                System.out.println("✅ M3U görünürlük güncellendi: " + m3uName + " → " + (isVisible ? "görünür" : "gizli"));
            } catch (Exception e) {
                System.out.println("❌ M3U görünürlük güncelleme hatası: " + e.getMessage());
            }
        }).start();
    }
    public String getActiveM3UName() {
        String activeName = prefs.getString(ACTIVE_M3U_KEY, "");
        if (activeName.isEmpty()) {
            List<M3UItem> items = getAllM3UItems();
            if (!items.isEmpty()) {
                activeName = items.get(0).getName();
                setActiveM3U(activeName);
            }
        }
        return activeName;
    }
    public void setActiveM3U(String m3uName) {
        prefs.edit().putString(ACTIVE_M3U_KEY, m3uName).apply();
    }
    public M3UItem getActiveM3UItem() {
        String activeName = getActiveM3UName();
        List<M3UItem> items = getAllM3UItems();
        for (M3UItem item : items) {
            if (item.getName().equals(activeName)) return item;
        }
        return items.isEmpty() ? null : items.get(0);
    }
    public void updateM3U(String m3uName, int channelCount) {
        List<M3UItem> items = getAllM3UItems();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getName().equals(m3uName)) {
                items.get(i).setChannelCount(channelCount);
                saveM3UList(items);
                break;
            }
        }
        databaseHelper.updateChannelCount(m3uName, channelCount, new DatabaseHelper.OnCompleteListener() {
            @Override public void onSuccess() { System.out.println("✅ M3U kanal sayısı güncellendi: " + m3uName); }
            @Override public void onError(Exception e) { System.out.println("❌ M3U güncelleme hatası: " + e.getMessage()); }
        });
    }
    public boolean m3uIsminiDegistir(Context ctx, String eskiIsim, String yeniIsim) {
        if (hasM3U(yeniIsim)) return false;
        List<M3UItem> items = getAllM3UItems();
        for (M3UItem item : items) {
            if (item.getName().equals(eskiIsim)) {
                item.setName(yeniIsim);
                break;
            }
        }
        saveM3UList(items);
        if (getActiveM3UName().equals(eskiIsim)) {
            setActiveM3U(yeniIsim);
        }
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(ctx);
                db.m3uDao().updateName(eskiIsim, yeniIsim);
                db.channelDao().updateSourceName(eskiIsim, yeniIsim);
                System.out.println("✅ M3U ismi değiştirildi: " + eskiIsim + " → " + yeniIsim);

                
                List<String[]> accounts = XtreamCodesManager.getAccounts(ctx);
                boolean xtreamKaydiVarMi = false;
                for (String[] acc : accounts) {
                    if (acc[0].equals(eskiIsim)) {
                        xtreamKaydiVarMi = true;
                        System.out.println("✅ [ISM-DEG] Xtream hesabı bulundu, isim güncelleniyor: '" + eskiIsim + "' → '" + yeniIsim + "'");
                        XtreamCodesManager.saveAccount(ctx, yeniIsim, acc[1], acc[2], acc[3]);
                        XtreamCodesManager.deleteAccount(ctx, eskiIsim);
                        break;
                    }
                }
                if (!xtreamKaydiVarMi) {
                    System.out.println("ℹ️ [ISM-DEG] Bu M3U için Xtream kaydı yok (normal URL veya dosya olabilir)");
                }
            } catch (Exception e) {
                System.out.println("❌ M3U isim değiştirme DB hatası: " + e.getMessage());
            }
        }).start();
        return true;
    }
    private void saveM3UList(List<M3UItem> items) {
        prefs.edit().putString(M3U_LIST_KEY, gson.toJson(items)).apply();
    }
    public boolean hasM3U(String m3uName) {
        for (M3UItem item : getAllM3UItems()) {
            if (item.getName().equals(m3uName)) return true;
        }
        return false;
    }
    public void clearAll() {
        saveM3UList(new ArrayList<>());
        setActiveM3U("");
        KanalListesi.clearAllM3UChannels(context);
    }
}