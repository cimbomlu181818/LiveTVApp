package com.example.livetvapp.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.example.livetvapp.Channel;
import com.example.livetvapp.stalker.StalkerCategoryProgress;
import com.example.livetvapp.stalker.StalkerPortal;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class YedekYonetici {
    private static final int VERSIYON = 3; 
    private final Context         context;
    private final ExecutorService executor    = Executors.newSingleThreadExecutor();
    private final Handler         mainHandler = new Handler(Looper.getMainLooper());

    public interface YedekCallback {
        void onBasarili(String mesaj);
        void onHata(String hata);
        void onIlerleme(String mesaj);
    }

    public YedekYonetici(Context context) {
        this.context = context.getApplicationContext();
    }

    public void disaAktar(YedekCallback callback) {
        executor.execute(() -> {
            try {
                mainHandler.post(() -> callback.onIlerleme("⏳ Yedek hazırlanıyor..."));
                JSONObject yedek = new JSONObject();
                yedek.put("versiyon", VERSIYON);
                yedek.put("tarih", new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

                
                M3UManager m3uManager = new M3UManager(context);
                List<M3UItem> m3uListesi = m3uManager.getAllM3UItems();
                JSONArray m3uArray = new JSONArray();
                for (M3UItem m3u : m3uListesi) {
                    JSONObject obj = new JSONObject();
                    obj.put("name",         m3u.getName());
                    obj.put("url",          m3u.getUrl() != null ? m3u.getUrl() : "");
                    obj.put("channelCount", m3u.getChannelCount());
                    obj.put("isLocalFile",  m3u.isLocalFile());
                    obj.put("isHidden",     m3u.isHidden());
                    obj.put("addedDate",    m3u.getAddedDate() != null ? m3u.getAddedDate().getTime() : 0);
                    m3uArray.put(obj);
                }
                yedek.put("m3uListesi", m3uArray);

                
                mainHandler.post(() -> callback.onIlerleme("⏳ Kanallar yazılıyor..."));
                AppDatabase db = AppDatabase.getInstance(context);
                JSONArray kanalArray = new JSONArray();
                for (M3UItem m3u : m3uListesi) {
                    List<Channel> kanallar = db.channelDao().getAllChannelsByM3U(m3u.getName());
                    for (Channel ch : kanallar) {
                        JSONObject chObj = new JSONObject();
                        chObj.put("name",        ch.getName());
                        chObj.put("url",         ch.getUrl());
                        chObj.put("category",    ch.getCategory());
                        chObj.put("logo",        ch.getLogo() != null ? ch.getLogo() : "");
                        chObj.put("sourceName",  ch.getSourceName());
                        chObj.put("contentType", ch.getContentType());
                        chObj.put("position",    ch.getPosition());
                        kanalArray.put(chObj);
                    }
                }
                yedek.put("kanallar", kanalArray);

                
                SharedPreferences gizliPrefs =
                        context.getSharedPreferences("gizli_kanallar", Context.MODE_PRIVATE);
                JSONArray gizliKanalArray = new JSONArray();
                for (Map.Entry<String, ?> e : gizliPrefs.getAll().entrySet()) {
                    if (Boolean.TRUE.equals(e.getValue())) gizliKanalArray.put(e.getKey());
                }
                yedek.put("gizliKanallar", gizliKanalArray);

                
                SharedPreferences gizliKatPrefs =
                        context.getSharedPreferences("gizli_kategoriler", Context.MODE_PRIVATE);
                JSONArray gizliKatArray = new JSONArray();
                for (Map.Entry<String, ?> e : gizliKatPrefs.getAll().entrySet()) {
                    if (Boolean.TRUE.equals(e.getValue())) gizliKatArray.put(e.getKey());
                }
                yedek.put("gizliKategoriler", gizliKatArray);

                
                SharedPreferences gizliM3UPrefs =
                        context.getSharedPreferences("gizli_m3ular", Context.MODE_PRIVATE);
                JSONArray gizliM3UArray = new JSONArray();
                for (Map.Entry<String, ?> e : gizliM3UPrefs.getAll().entrySet()) {
                    if (Boolean.TRUE.equals(e.getValue())) gizliM3UArray.put(e.getKey());
                }
                yedek.put("gizliM3Ular", gizliM3UArray);

                
                SharedPreferences siralamaPrefs =
                        context.getSharedPreferences("kategori_siralama", Context.MODE_PRIVATE);
                JSONObject siralamaObj = new JSONObject();
                for (Map.Entry<String, ?> e : siralamaPrefs.getAll().entrySet()) {
                    siralamaObj.put(e.getKey(), e.getValue().toString());
                }
                yedek.put("kategoriSiralama", siralamaObj);

                
                SharedPreferences favoriPrefs =
                        context.getSharedPreferences("favoriler", Context.MODE_PRIVATE);
                JSONObject favoriObj = new JSONObject();
                for (Map.Entry<String, ?> e : favoriPrefs.getAll().entrySet()) {
                    favoriObj.put(e.getKey(), e.getValue().toString());
                }
                yedek.put("favoriler", favoriObj);

                
                SharedPreferences appPrefs =
                        context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
                JSONObject appSettingsObj = new JSONObject();
                for (Map.Entry<String, ?> e : appPrefs.getAll().entrySet()) {
                    Object val = e.getValue();
                    if (val instanceof Boolean) appSettingsObj.put(e.getKey(), (Boolean) val);
                    else if (val instanceof String) appSettingsObj.put(e.getKey(), (String) val);
                    else if (val instanceof Integer) appSettingsObj.put(e.getKey(), (Integer) val);
                    else if (val instanceof Long) appSettingsObj.put(e.getKey(), (Long) val);
                    else if (val instanceof Float) appSettingsObj.put(e.getKey(), (Float) val);
                    else appSettingsObj.put(e.getKey(), val.toString());
                }
                yedek.put("appSettings", appSettingsObj);

                
                SharedPreferences stepPrefs =
                        context.getSharedPreferences("LiveTVAppPrefs", Context.MODE_PRIVATE);
                JSONObject stepObj = new JSONObject();
                stepObj.put("rewindStep", stepPrefs.getInt("rewindStep", 10));
                stepObj.put("forwardStep", stepPrefs.getInt("forwardStep", 10));
                yedek.put("stepPrefs", stepObj);

                
                SharedPreferences xtreamPrefs =
                        context.getSharedPreferences("XtreamAccounts", Context.MODE_PRIVATE);
                String xtreamJson = xtreamPrefs.getString("accounts_json", "[]");
                yedek.put("xtreamHesaplari", new JSONArray(xtreamJson));

                
                mainHandler.post(() -> callback.onIlerleme("⏳ İzleme pozisyonları yazılıyor..."));
                List<IzlemePozisyonu> pozisyonlar = db.channelDao().getAllPlaybackPositions();
                JSONArray pozisyonArray = new JSONArray();
                for (IzlemePozisyonu p : pozisyonlar) {
                    JSONObject pObj = new JSONObject();
                    pObj.put("url", p.url);
                    pObj.put("pozisyonMs", p.pozisyonMs);
                    pObj.put("kaydedilmeZamani", p.kaydedilmeZamani);
                    pozisyonArray.put(pObj);
                }
                yedek.put("izlemePozisyonlari", pozisyonArray);

                
                mainHandler.post(() -> callback.onIlerleme("⏳ Stalker portalları yazılıyor..."));
                List<StalkerPortal> stalkerPortals = db.stalkerPortalDao().getActivePortals();
                JSONArray stalkerPortalArray = new JSONArray();
                for (StalkerPortal p : stalkerPortals) {
                    JSONObject pObj = new JSONObject();
                    pObj.put("id", p.getId());
                    pObj.put("name", p.getName());
                    pObj.put("portalUrl", p.getPortalUrl());
                    pObj.put("macAddress", p.getMacAddress());
                    pObj.put("token", p.getToken() != null ? p.getToken() : "");
                    pObj.put("tokenExpiry", p.getTokenExpiry());
                    pObj.put("isActive", p.isActive());
                    pObj.put("lastSync", p.getLastSync());
                    stalkerPortalArray.put(pObj);
                }
                yedek.put("stalkerPortallar", stalkerPortalArray);

                
                mainHandler.post(() -> callback.onIlerleme("⏳ Stalker kategori ilerleme yazılıyor..."));
                List<StalkerCategoryProgress> stalkerProgress = db.stalkerCategoryProgressDao().getAll();
                JSONArray progressArray = new JSONArray();
                for (StalkerCategoryProgress prog : stalkerProgress) {
                    JSONObject progObj = new JSONObject();
                    progObj.put("m3uName", prog.getM3uName());
                    progObj.put("category", prog.getCategory());
                    progObj.put("categoryId", prog.getCategoryId());
                    progObj.put("lastFetchedPage", prog.getLastFetchedPage());
                    progObj.put("totalPages", prog.getTotalPages());
                    progObj.put("totalItems", prog.getTotalItems());
                    progObj.put("lastSyncTime", prog.getLastSyncTime());
                    progressArray.put(progObj);
                }
                yedek.put("stalkerKategoriProgress", progressArray);

                
                mainHandler.post(() -> callback.onIlerleme("⏳ Dosya yazılıyor..."));
                String dosyaAdi = "AZSH_yedek_"
                        + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date())
                        + ".json";
                File indir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!indir.exists()) indir.mkdirs();
                File dosya = new File(indir, dosyaAdi);
                FileWriter fw = new FileWriter(dosya);
                fw.write(yedek.toString(2));
                fw.close();

                final String yol = dosya.getAbsolutePath();
                System.out.println("✅ Yedek kaydedildi: " + yol
                        + " | M3U: " + m3uListesi.size()
                        + " | Kanal: " + kanalArray.length()
                        + " | Pozisyon: " + pozisyonlar.size()
                        + " | Stalker portal: " + stalkerPortals.size()
                        + " | Stalker progress: " + stalkerProgress.size());
                mainHandler.post(() -> callback.onBasarili(
                        "✅ Yedek kaydedildi!\n\nDosya: " + dosyaAdi
                                + "\nKonum: İndirilenler\n"
                                + m3uListesi.size() + " M3U, "
                                + kanalArray.length() + " kanal, "
                                + pozisyonlar.size() + " izleme pozisyonu,\n"
                                + stalkerPortals.size() + " Stalker portal, "
                                + stalkerProgress.size() + " kategori ilerleme kaydedildi."));
            } catch (Exception e) {
                System.out.println("❌ disaAktar hatası: " + e.getMessage());
                mainHandler.post(() -> callback.onHata("❌ GENEL hatası: " + e.getMessage()));
            }
        });
    }

    public void iceAktar(String dosyaYolu, YedekCallback callback) {
        executor.execute(() -> {
            try {
                mainHandler.post(() -> callback.onIlerleme("⏳ Dosya okunuyor..."));
                StringBuilder sb = new StringBuilder();
                BufferedReader br = new BufferedReader(new FileReader(dosyaYolu));
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                JSONObject yedek = new JSONObject(sb.toString());
                int versiyon = yedek.optInt("versiyon", 1);
                System.out.println("📦 Yedek versiyonu: " + versiyon
                        + " | Tarih: " + yedek.optString("tarih"));

                AppDatabase db = AppDatabase.getInstance(context);
                mainHandler.post(() -> callback.onIlerleme("⏳ Mevcut veriler temizleniyor..."));

                
                db.channelDao().deleteAll();
                db.m3uDao().deleteAll();
                db.channelDao().deleteAllPlaybackPositions();
                db.stalkerPortalDao().deleteAll();
                db.stalkerCategoryProgressDao().deleteAll();

                M3UManager m3uManager = new M3UManager(context);
                m3uManager.clearAll();

                
                context.getSharedPreferences("gizli_kanallar",    Context.MODE_PRIVATE).edit().clear().apply();
                context.getSharedPreferences("gizli_kategoriler", Context.MODE_PRIVATE).edit().clear().apply();
                context.getSharedPreferences("gizli_m3ular",      Context.MODE_PRIVATE).edit().clear().apply();
                context.getSharedPreferences("kategori_siralama", Context.MODE_PRIVATE).edit().clear().apply();
                context.getSharedPreferences("favoriler",         Context.MODE_PRIVATE).edit().clear().apply();
                context.getSharedPreferences("AppSettings",       Context.MODE_PRIVATE).edit().clear().apply();
                context.getSharedPreferences("LiveTVAppPrefs",    Context.MODE_PRIVATE).edit().clear().apply();
                context.getSharedPreferences("XtreamAccounts",    Context.MODE_PRIVATE).edit().clear().apply();

                
                mainHandler.post(() -> callback.onIlerleme("⏳ M3U listesi yükleniyor..."));
                JSONArray m3uArray = yedek.getJSONArray("m3uListesi");
                List<M3UItem> m3uListesi = new ArrayList<>();
                for (int i = 0; i < m3uArray.length(); i++) {
                    JSONObject obj = m3uArray.getJSONObject(i);
                    M3UItem m3u = new M3UItem();
                    m3u.setName(obj.getString("name"));
                    m3u.setUrl(obj.optString("url", ""));
                    m3u.setChannelCount(obj.optInt("channelCount", 0));
                    m3u.setLocalFile(false);
                    m3u.setHidden(obj.optBoolean("isHidden", false));
                    long addedDateMs = obj.optLong("addedDate", 0);
                    if (addedDateMs > 0) m3u.setAddedDate(new Date(addedDateMs));
                    m3uListesi.add(m3u);
                }
                db.m3uDao().insertAll(m3uListesi);
                for (M3UItem m3u : m3uListesi) m3uManager.addM3U(m3u);

                
                mainHandler.post(() -> callback.onIlerleme("⏳ Kanallar yükleniyor..."));
                JSONArray kanalArray = yedek.getJSONArray("kanallar");
                List<Channel> toplu = new ArrayList<>();
                for (int i = 0; i < kanalArray.length(); i++) {
                    JSONObject chObj = kanalArray.getJSONObject(i);
                    Channel ch = new Channel();
                    ch.setName(chObj.getString("name"));
                    ch.setUrl(chObj.getString("url"));
                    ch.setCategory(chObj.optString("category", "Genel"));
                    ch.setLogo(chObj.optString("logo", ""));
                    ch.setSourceName(chObj.getString("sourceName"));
                    ch.setContentType(chObj.optString("contentType", "LIVE"));
                    ch.setPosition(chObj.optInt("position", 0));
                    toplu.add(ch);
                    if (toplu.size() >= 500) {
                        db.channelDao().insertAll(new ArrayList<>(toplu));
                        toplu.clear();

                    }
                }
                if (!toplu.isEmpty()) db.channelDao().insertAll(toplu);

                
                JSONArray gizliKanalArray = yedek.optJSONArray("gizliKanallar");
                if (gizliKanalArray != null && gizliKanalArray.length() > 0) {
                    SharedPreferences.Editor ed =
                            context.getSharedPreferences("gizli_kanallar", Context.MODE_PRIVATE).edit();
                    for (int i = 0; i < gizliKanalArray.length(); i++) {
                        ed.putBoolean(gizliKanalArray.getString(i), true);
                    }
                    ed.apply();
                }

                
                JSONArray gizliKatArray = yedek.optJSONArray("gizliKategoriler");
                if (gizliKatArray != null && gizliKatArray.length() > 0) {
                    SharedPreferences.Editor ed =
                            context.getSharedPreferences("gizli_kategoriler", Context.MODE_PRIVATE).edit();
                    for (int i = 0; i < gizliKatArray.length(); i++) {
                        ed.putBoolean(gizliKatArray.getString(i), true);
                    }
                    ed.apply();
                }

                
                JSONArray gizliM3UArray = yedek.optJSONArray("gizliM3Ular");
                if (gizliM3UArray != null && gizliM3UArray.length() > 0) {
                    SharedPreferences.Editor ed =
                            context.getSharedPreferences("gizli_m3ular", Context.MODE_PRIVATE).edit();
                    for (int i = 0; i < gizliM3UArray.length(); i++) {
                        ed.putBoolean(gizliM3UArray.getString(i), true);
                    }
                    ed.apply();
                }

                
                JSONObject siralamaObj = yedek.optJSONObject("kategoriSiralama");
                if (siralamaObj != null) {
                    SharedPreferences.Editor ed =
                            context.getSharedPreferences("kategori_siralama", Context.MODE_PRIVATE).edit();
                    Iterator<String> keys = siralamaObj.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        ed.putString(key, siralamaObj.getString(key));
                    }
                    ed.apply();
                }

                
                JSONObject favoriObj = yedek.optJSONObject("favoriler");
                if (favoriObj != null && favoriObj.length() > 0) {
                    SharedPreferences.Editor ed =
                            context.getSharedPreferences("favoriler", Context.MODE_PRIVATE).edit();
                    Iterator<String> keys = favoriObj.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        ed.putString(key, favoriObj.getString(key));
                    }
                    ed.apply();
                }

                
                JSONObject appSettingsObj = yedek.optJSONObject("appSettings");
                if (appSettingsObj != null) {
                    SharedPreferences.Editor ed =
                            context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE).edit();
                    Iterator<String> keys = appSettingsObj.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        Object val = appSettingsObj.get(key);
                        if (val instanceof Boolean) ed.putBoolean(key, (Boolean) val);
                        else if (val instanceof Integer) ed.putInt(key, (Integer) val);
                        else if (val instanceof Long) ed.putLong(key, (Long) val);
                        else if (val instanceof String) ed.putString(key, (String) val);
                        else ed.putString(key, val.toString());
                    }
                    ed.apply();
                }

                
                JSONObject stepObj = yedek.optJSONObject("stepPrefs");
                if (stepObj != null) {
                    SharedPreferences.Editor ed =
                            context.getSharedPreferences("LiveTVAppPrefs", Context.MODE_PRIVATE).edit();
                    ed.putInt("rewindStep", stepObj.optInt("rewindStep", 10));
                    ed.putInt("forwardStep", stepObj.optInt("forwardStep", 10));
                    ed.apply();
                }

                
                JSONArray xtreamArray = yedek.optJSONArray("xtreamHesaplari");
                if (xtreamArray != null && xtreamArray.length() > 0) {
                    context.getSharedPreferences("XtreamAccounts", Context.MODE_PRIVATE)
                            .edit().putString("accounts_json", xtreamArray.toString()).apply();
                }

                
                mainHandler.post(() -> callback.onIlerleme("⏳ İzleme pozisyonları yükleniyor..."));
                JSONArray pozisyonArray = yedek.optJSONArray("izlemePozisyonlari");
                if (pozisyonArray != null) {
                    for (int i = 0; i < pozisyonArray.length(); i++) {
                        JSONObject pObj = pozisyonArray.getJSONObject(i);
                        IzlemePozisyonu poz = new IzlemePozisyonu(
                                pObj.getString("url"),
                                pObj.getLong("pozisyonMs")
                        );
                        poz.kaydedilmeZamani = pObj.optLong("kaydedilmeZamani", System.currentTimeMillis());
                        db.channelDao().pozisyonKaydet(poz);
                    }
                }

                
                mainHandler.post(() -> callback.onIlerleme("⏳ Stalker portalları yükleniyor..."));
                JSONArray stalkerPortalArray = yedek.optJSONArray("stalkerPortallar");
                if (stalkerPortalArray != null) {
                    for (int i = 0; i < stalkerPortalArray.length(); i++) {
                        JSONObject pObj = stalkerPortalArray.getJSONObject(i);
                        StalkerPortal portal = new StalkerPortal();
                        portal.setId(0);  
                        portal.setName(pObj.getString("name"));
                        portal.setPortalUrl(pObj.getString("portalUrl"));
                        portal.setMacAddress(pObj.getString("macAddress"));
                        portal.setToken(null);       
                        portal.setTokenExpiry(0);
                        portal.setActive(pObj.optBoolean("isActive", true));
                        portal.setLastSync(pObj.optLong("lastSync", 0));
                        db.stalkerPortalDao().insert(portal);
                    }
                }

                
                mainHandler.post(() -> callback.onIlerleme("⏳ Stalker kategori ilerleme yükleniyor..."));
                JSONArray progressArray = yedek.optJSONArray("stalkerKategoriProgress");
                if (progressArray != null) {
                    for (int i = 0; i < progressArray.length(); i++) {
                        JSONObject progObj = progressArray.getJSONObject(i);
                        StalkerCategoryProgress prog = new StalkerCategoryProgress(
                                progObj.getString("m3uName"),
                                progObj.getString("category")
                        );
                        prog.setCategoryId(progObj.optString("categoryId", ""));
                        prog.setLastFetchedPage(progObj.optInt("lastFetchedPage", -1));
                        prog.setTotalPages(progObj.optInt("totalPages", -1));
                        prog.setTotalItems(progObj.optInt("totalItems", -1));
                        prog.setLastSyncTime(progObj.optLong("lastSyncTime", System.currentTimeMillis()));
                        db.stalkerCategoryProgressDao().insert(prog);
                    }
                }

                System.out.println("✅ İçe aktarma tamamlandı: "
                        + m3uListesi.size() + " M3U, "
                        + kanalArray.length() + " kanal, "
                        + (pozisyonArray != null ? pozisyonArray.length() : 0) + " izleme pozisyonu, "
                        + (stalkerPortalArray != null ? stalkerPortalArray.length() : 0) + " Stalker portal, "
                        + (progressArray != null ? progressArray.length() : 0) + " kategori ilerleme");
                mainHandler.post(() -> callback.onBasarili(
                        "✅ Yedek başarıyla yüklendi!\n\n"
                                + m3uListesi.size() + " M3U, "
                                + kanalArray.length() + " kanal, "
                                + (pozisyonArray != null ? pozisyonArray.length() : 0) + " izleme pozisyonu,\n"
                                + (stalkerPortalArray != null ? stalkerPortalArray.length() : 0) + " Stalker portal, "
                                + (progressArray != null ? progressArray.length() : 0) + " kategori ilerleme geri yüklendi.\n\n"
                                + "Uygulamayı yeniden başlatın."));
            } catch (Exception e) {
                System.out.println("❌ iceAktar hatası: " + e.getMessage());
                mainHandler.post(() -> callback.onHata("❌ İçe aktarma hatası: " + e.getMessage()));
            }
        });
    }
}