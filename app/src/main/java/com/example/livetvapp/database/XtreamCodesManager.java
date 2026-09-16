package com.example.livetvapp.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import java.util.Collections;
import java.util.Iterator;
import com.example.livetvapp.Channel;
import com.example.livetvapp.KanalListesi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XtreamCodesManager {
    private static final String PREFS_NAME     = "XtreamAccounts";
    private static final String KEY_ACCOUNTS   = "accounts_json";
    private static final int    TIMEOUT_MS     = 15_000;

    public interface OnXtreamLoadListener {
        void onStarted();
        void onProgress(String message);
        void onSuccess(List<Channel> channels, String sourceName);
        void onError(String error);
    }

    public static void saveAccount(Context ctx, String name, String server,
                                   String username, String password) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        try {
            JSONArray arr = getAccountsJson(prefs);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (o.optString("name").equals(name)) {
                    arr.remove(i);
                    break;
                }
            }
            JSONObject obj = new JSONObject();
            obj.put("name",     name);
            obj.put("server",   server);
            obj.put("username", username);
            obj.put("password", password);
            arr.put(obj);
            prefs.edit().putString(KEY_ACCOUNTS, arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String[]> getAccounts(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        List<String[]> list = new ArrayList<>();
        try {
            JSONArray arr = getAccountsJson(prefs);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new String[]{
                        o.optString("name"),
                        o.optString("server"),
                        o.optString("username"),
                        o.optString("password")
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void deleteAccount(Context ctx, String name) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        try {
            JSONArray arr    = getAccountsJson(prefs);
            JSONArray newArr = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (!o.optString("name").equals(name)) newArr.put(o);
            }
            prefs.edit().putString(KEY_ACCOUNTS, newArr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static JSONArray getAccountsJson(SharedPreferences prefs) {
        try {
            String json = prefs.getString(KEY_ACCOUNTS, "[]");
            return new JSONArray(json);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public static void loadChannels(Context ctx,
                                    String server,
                                    String username,
                                    String password,
                                    String sourceName,
                                    OnXtreamLoadListener listener) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            mainHandler.post(() -> {
                if (listener != null) listener.onStarted();
            });

            String baseUrl = server.trim();
            if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                baseUrl = "http://" + baseUrl;
            }
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            final String finalBase = baseUrl;

            mainHandler.post(() -> {
                if (listener != null) listener.onProgress("Player API deneniyor...");
            });

            try {
                List<Channel> channels = loadViaPlayerApi(finalBase, username, password,
                        sourceName, mainHandler, listener);
                if (channels != null && !channels.isEmpty()) {
                    final List<Channel> result = channels;
                    mainHandler.post(() -> {
                        if (listener != null) listener.onSuccess(result, sourceName);
                    });
                    return;
                }
            } catch (Exception e) {
                System.out.println("⚠️ Player API başarısız: " + e.getMessage());
            }

            mainHandler.post(() -> {
                if (listener != null) listener.onProgress("M3U Plus formatı deneniyor...");
            });

            try {
                List<Channel> channels = loadViaM3UPlus(finalBase, username, password, sourceName);
                if (channels != null && !channels.isEmpty()) {
                    final List<Channel> result = channels;
                    mainHandler.post(() -> {
                        if (listener != null) listener.onSuccess(result, sourceName);
                    });
                    return;
                }
            } catch (Exception e) {
                System.out.println("⚠️ M3U Plus başarısız: " + e.getMessage());
            }

            mainHandler.post(() -> {
                if (listener != null)
                    listener.onError("Sunucuya bağlanılamadı. Bilgileri kontrol edin.");
            });
        }).start();
    }

    private static List<Channel> loadViaM3UPlus(String baseUrl,
                                                String username,
                                                String password,
                                                String sourceName) throws Exception {
        String urlStr = baseUrl
                + "/get.php?username=" + encode(username)
                + "&password="         + encode(password)
                + "&type=m3u_plus&output=ts";
        String content = fetchText(urlStr);
        if (content == null || !content.contains("#EXTM3U")) {
            throw new Exception("Geçerli M3U içeriği alınamadı");
        }
        return KanalListesi.parseM3U(content, sourceName);
    }

    private static List<Channel> loadViaPlayerApi(String baseUrl,
                                                  String username,
                                                  String password,
                                                  String sourceName,
                                                  Handler mainHandler,
                                                  OnXtreamLoadListener listener) throws Exception {
        List<Channel> allChannels = new ArrayList<>();
        String apiBase = baseUrl
                + "/player_api.php?username=" + encode(username)
                + "&password="                + encode(password);

        String authJson = fetchText(apiBase + "&action=get_live_categories");
        if (authJson == null || authJson.trim().startsWith("<")) {
            throw new Exception("Player API yanıt vermedi");
        }

        Map<String, String> liveKategoriMap   = kategoriMapOlustur(authJson);
        String vodCatJson    = fetchText(apiBase + "&action=get_vod_categories");
        Map<String, String> vodKategoriMap    = kategoriMapOlustur(vodCatJson    != null ? vodCatJson    : "[]");
        String seriesCatJson = fetchText(apiBase + "&action=get_series_categories");
        Map<String, String> seriesKategoriMap = kategoriMapOlustur(seriesCatJson != null ? seriesCatJson : "[]");

        int[] globalPosition = {0};

        mainHandler.post(() -> {
            if (listener != null) listener.onProgress("Canlı kanallar yükleniyor...");
        });

        try {
            String liveJson = fetchText(apiBase + "&action=get_live_streams");
            if (liveJson != null) {
                allChannels.addAll(parsePlayerApiStreams(liveJson, baseUrl, username, password,
                        sourceName, "LIVE", liveKategoriMap, globalPosition));
            }
        } catch (Exception e) {
            System.out.println("⚠️ Live streams: " + e.getMessage());
        }

        mainHandler.post(() -> {
            if (listener != null) listener.onProgress("Filmler yükleniyor...");
        });

        try {
            String vodJson = fetchText(apiBase + "&action=get_vod_streams");
            if (vodJson != null) {
                allChannels.addAll(parsePlayerApiStreams(vodJson, baseUrl, username, password,
                        sourceName, "MOVIE", vodKategoriMap, globalPosition));
            }
        } catch (Exception e) {
            System.out.println("⚠️ VOD streams: " + e.getMessage());
        }

        mainHandler.post(() -> {
            if (listener != null) listener.onProgress("Diziler yükleniyor (API ile)...");
        });

        try {
            String seriesJson = fetchText(apiBase + "&action=get_series");
            if (seriesJson != null && !seriesJson.isEmpty()) {
                JSONArray seriesArr = new JSONArray(seriesJson);
                for (int i = 0; i < seriesArr.length(); i++) {
                    JSONObject series = seriesArr.getJSONObject(i);
                    String seriesId = series.optString("series_id");
                    String name = series.optString("name");
                    String cover = series.optString("cover", "");
                    String catId = series.optString("category_id", "");

                    String categoryName;
                    if (catId != null && !catId.isEmpty() && seriesKategoriMap.containsKey(catId)) {
                        categoryName = seriesKategoriMap.get(catId);
                    } else {
                        categoryName = "Diziler";
                    }

                    if (seriesId.isEmpty() || name.isEmpty()) continue;

                    String url = baseUrl + "/series/" + encode(username) + "/" + encode(password)
                            + "/" + seriesId + ".mkv";

                    Channel ch = new Channel(name, url, categoryName, cover, sourceName);
                    ch.setContentType("SERIES");
                    ch.setPosition(globalPosition[0]++);
                    allChannels.add(ch);
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Series API hatası: " + e.getMessage());
        }

        return allChannels;
    }

    private static List<Channel> parsePlayerApiStreams(String json,
                                                       String baseUrl,
                                                       String username,
                                                       String password,
                                                       String sourceName,
                                                       String contentType,
                                                       Map<String, String> kategoriMap,
                                                       int[] globalPosition) {
        List<Channel> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject item = arr.getJSONObject(i);
                String name     = item.optString("name", "");
                String logo     = item.optString("stream_icon", "");
                String catId    = item.optString("category_id", "");
                String categoryName = kategoriMap.containsKey(catId) ? kategoriMap.get(catId) : "Genel";
                int    streamId     = item.optInt("stream_id", 0);
                String containerExt = item.optString("container_extension", "ts");
                if (streamId == 0 || name.isEmpty()) continue;

                String url;
                if ("LIVE".equals(contentType)) {
                    url = baseUrl + "/" + encode(username) + "/" + encode(password)
                            + "/" + streamId;
                } else {
                    url = baseUrl + "/movie/" + encode(username) + "/" + encode(password)
                            + "/" + streamId + "." + containerExt;
                }

                Channel ch = new Channel(name, url, categoryName, logo, sourceName);
                ch.setPosition(globalPosition[0]++);
                ch.setContentType(contentType);
                list.add(ch);
            }
        } catch (Exception e) {
            System.out.println("❌ parsePlayerApiStreams: " + e.getMessage());
        }
        return list;
    }

    private static Map<String, String> kategoriMapOlustur(String json) {
        Map<String, String> map = new HashMap<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String id  = obj.optString("category_id", "");
                String ad  = obj.optString("category_name", "");
                if (!id.isEmpty() && !ad.isEmpty()) map.put(id, ad);
            }
        } catch (Exception e) {
            System.out.println("⚠️ kategoriMapOlustur: " + e.getMessage());
        }
        return map;
    }
    public static void loadSeriesInfo(String seriesUrl,
                                      String sourceName,
                                      Context ctx,
                                      OnSeriesInfoLoadedListener listener) {

        String seriesId = null;
        try {
            String path = new URL(seriesUrl).getPath();
            String[] parts = path.split("/");
            String last = parts[parts.length - 1];
            seriesId = last.contains(".") ? last.substring(0, last.lastIndexOf('.')) : last;
        } catch (Exception e) {
            if (listener != null) listener.onError("series_id çıkarılamadı");
            return;
        }


        String server = null;
        String username = null;
        String password = null;
        try {
            URL u = new URL(seriesUrl);
            server = u.getProtocol() + "://" + u.getHost()
                    + (u.getPort() != -1 ? ":" + u.getPort() : "");
            String[] parts = u.getPath().split("/");

            if (parts.length >= 4) {
                username = parts[2];
                password = parts[3];
            }
        } catch (Exception e) {
            if (listener != null) listener.onError("Sunucu bilgisi çıkarılamadı");
            return;
        }

        final String finalServer   = server;
        final String finalUser     = username;
        final String finalPass     = password;
        final String finalSeriesId = seriesId;

        new Thread(() -> {
            try {
                String url = finalServer
                        + "/player_api.php?username=" + encode(finalUser)
                        + "&password=" + encode(finalPass)
                        + "&action=get_series_info&series_id=" + finalSeriesId;
                String json = fetchText(url);
                if (json == null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (listener != null) listener.onError("Boş yanıt");
                    });
                    return;
                }
                JSONObject root = new JSONObject(json);
                JSONObject episodes = root.optJSONObject("episodes");
                List<com.example.livetvapp.Adapter.Sezon> sezonlar = new ArrayList<>();
                if (episodes != null) {
                    Iterator<String> keys = episodes.keys();
                    while (keys.hasNext()) {
                        String sezonNo = keys.next();
                        JSONArray bolumArr = episodes.getJSONArray(sezonNo);
                        com.example.livetvapp.Adapter.Sezon sezon =
                                new com.example.livetvapp.Adapter.Sezon(
                                        Integer.parseInt(sezonNo), "Sezon " + sezonNo);
                        for (int i = 0; i < bolumArr.length(); i++) {
                            JSONObject ep = bolumArr.getJSONObject(i);
                            int epNum  = ep.optInt("episode_num", i + 1);
                            String epTitle = ep.optString("title", epNum + ". Bölüm");
                            String epId    = ep.optString("id", "");
                            String ext     = ep.optString("container_extension", "mkv");
                            String epUrl   = finalServer + "/series/"
                                    + encode(finalUser) + "/" + encode(finalPass)
                                    + "/" + epId + "." + ext;
                            sezon.bolumEkle(new com.example.livetvapp.Adapter.Bolum(epNum, epTitle, epUrl));
                        }
                        sezonlar.add(sezon);
                    }

                    Collections.sort(sezonlar, (a, b) -> Integer.compare(a.getSezonNo(), b.getSezonNo()));
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (listener != null) listener.onSeriesInfoLoaded(sezonlar);
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
            }
        }).start();
    }

    public interface OnSeriesInfoLoadedListener {
        void onSeriesInfoLoaded(List<com.example.livetvapp.Adapter.Sezon> sezonlar);
        void onError(String hata);
    }
    private static String fetchText(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        int code = conn.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP " + code);
        }
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        reader.close();
        conn.disconnect();
        String result = sb.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private static String encode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

}