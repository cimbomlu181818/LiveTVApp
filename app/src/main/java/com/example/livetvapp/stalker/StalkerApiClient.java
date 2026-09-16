package com.example.livetvapp.stalker;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class StalkerApiClient {
    private static final String TAG     = "StalkerApi";
    private static final int    TIMEOUT = 15000;

    private static final String USER_AGENT =
            "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 " +
                    "(KHTML, like Gecko) MAG200 stbapp ver: 2 rev: 250 Safari/533.3";


    public static JSONObject handshakeSync(String baseUrl, String mac) throws Exception {
        String urlStr = normalizeUrl(baseUrl) + "server/load.php"
                + "?action=handshake&type=stb&token=&JsHttpRequest=1-xml";
        JSONObject resp = doGet(urlStr, mac, null);
        Log.d(TAG, "Handshake yanıtı: " + resp.toString());
        if (!resp.has("js")) throw new Exception("Handshake: 'js' alanı yok");
        Object jsObj = resp.get("js");
        if (jsObj instanceof JSONObject) {
            String token = ((JSONObject) jsObj).optString("token", "");
            if (token.isEmpty()) throw new Exception("Handshake: token boş");
        }
        return resp;
    }

    public static JSONObject getProfileSync(String baseUrl, String token, String mac) throws Exception {
        String urlStr = normalizeUrl(baseUrl) + "server/load.php"
                + "?action=get_profile&type=stb&token=" + URLEncoder.encode(token, "UTF-8")
                + "&JsHttpRequest=1-xml";
        return doGet(urlStr, mac, token);
    }

    public static JSONObject getAllChannelsSync(String baseUrl, String token, String mac) throws Exception {
        return getAllByType(baseUrl, token, mac, "itv");
    }

    public static JSONObject getAllByType(String baseUrl, String token, String mac, String type) throws Exception {
        String urlStr = normalizeUrl(baseUrl)
                + "server/load.php"
                + "?action=get_all_channels"
                + "&type=" + type
                + "&token=" + URLEncoder.encode(token, "UTF-8")
                + "&JsHttpRequest=1-xml";
        return doGet(urlStr, mac, token);
    }

    public static JSONObject getGenresSync(String baseUrl, String token, String mac) throws Exception {
        String urlStr = normalizeUrl(baseUrl) + "server/load.php"
                + "?action=get_genres&type=itv&token=" + URLEncoder.encode(token, "UTF-8")
                + "&JsHttpRequest=1-xml";
        return doGet(urlStr, mac, token);
    }

    public static String createLinkSync(String baseUrl, String token, String mac, String cmd) throws Exception {
        String urlStr = normalizeUrl(baseUrl) + "server/load.php"
                + "?action=create_link&type=itv&token=" + URLEncoder.encode(token, "UTF-8")
                + "&cmd=" + URLEncoder.encode(cmd, "UTF-8")
                + "&JsHttpRequest=1-xml";
        JSONObject resp = doGet(urlStr, mac, token);
        JSONObject js = resp.optJSONObject("js");
        if (js != null) {
            String streamCmd = js.optString("cmd", "");
            if (!streamCmd.isEmpty()) return extractUrl(streamCmd);
        }
        return extractUrl(cmd);
    }




    public static JSONObject getVodCategoriesSync(String baseUrl, String token, String mac) throws Exception {
        String urlStr = normalizeUrl(baseUrl) + "server/load.php"
                + "?action=get_categories"
                + "&type=vod"
                + "&token=" + URLEncoder.encode(token, "UTF-8")
                + "&JsHttpRequest=1-xml";
        return doGet(urlStr, mac, token);
    }


    public static JSONObject getVodListSync(String baseUrl, String token, String mac,
                                            String categoryId, int page) throws Exception {
        String urlStr = normalizeUrl(baseUrl) + "server/load.php"
                + "?action=get_ordered_list"
                + "&type=vod"
                + "&category=" + URLEncoder.encode(categoryId, "UTF-8")
                + "&p=" + page
                + "&token=" + URLEncoder.encode(token, "UTF-8")
                + "&JsHttpRequest=1-xml";
        return doGet(urlStr, mac, token);
    }


    public static String createVodLinkSync(String baseUrl, String token, String mac, String cmd) throws Exception {
        String urlStr = normalizeUrl(baseUrl) + "server/load.php"
                + "?action=create_link"
                + "&type=vod"
                + "&token=" + URLEncoder.encode(token, "UTF-8")
                + "&cmd=" + URLEncoder.encode(cmd, "UTF-8")
                + "&JsHttpRequest=1-xml";
        JSONObject resp = doGet(urlStr, mac, token);
        JSONObject js = resp.optJSONObject("js");
        if (js != null) {
            String streamCmd = js.optString("cmd", "");
            if (!streamCmd.isEmpty()) return extractUrl(streamCmd);
        }
        return extractUrl(cmd);
    }




    public static JSONObject getSeriesCategoriesSync(String baseUrl, String token, String mac) throws Exception {
        String urlStr = normalizeUrl(baseUrl) + "server/load.php"
                + "?action=get_categories"
                + "&type=series"
                + "&token=" + URLEncoder.encode(token, "UTF-8")
                + "&JsHttpRequest=1-xml";
        return doGet(urlStr, mac, token);
    }


    public static JSONObject getSeriesListSync(String baseUrl, String token, String mac,
                                               String categoryId, int page) throws Exception {
        String urlStr = normalizeUrl(baseUrl) + "server/load.php"
                + "?action=get_ordered_list"
                + "&type=series"
                + "&category=" + URLEncoder.encode(categoryId, "UTF-8")
                + "&p=" + page
                + "&token=" + URLEncoder.encode(token, "UTF-8")
                + "&JsHttpRequest=1-xml";
        return doGet(urlStr, mac, token);
    }


    public static JSONObject getSeriesSeasonsSync(String baseUrl, String token, String mac,
                                                  String movieId) throws Exception {
        String urlStr = normalizeUrl(baseUrl) + "server/load.php"
                + "?action=get_seasons"
                + "&type=series"
                + "&movie_id=" + URLEncoder.encode(movieId, "UTF-8")
                + "&token=" + URLEncoder.encode(token, "UTF-8")
                + "&JsHttpRequest=1-xml";
        return doGet(urlStr, mac, token);
    }


    public static JSONObject getSeriesEpisodesSync(String baseUrl, String token, String mac,
                                                   String movieId, int season) throws Exception {
        String urlStr = normalizeUrl(baseUrl) + "server/load.php"
                + "?action=get_ordered_list"
                + "&type=series"
                + "&movie_id=" + URLEncoder.encode(movieId, "UTF-8")
                + "&season=" + season
                + "&episode=-1"
                + "&token=" + URLEncoder.encode(token, "UTF-8")
                + "&JsHttpRequest=1-xml";
        return doGet(urlStr, mac, token);
    }


    public static String createSeriesLinkSync(String baseUrl, String token, String mac, String cmd) throws Exception {
        String urlStr = normalizeUrl(baseUrl) + "server/load.php"
                + "?action=create_link"
                + "&type=series"
                + "&token=" + URLEncoder.encode(token, "UTF-8")
                + "&cmd=" + URLEncoder.encode(cmd, "UTF-8")
                + "&JsHttpRequest=1-xml";
        JSONObject resp = doGet(urlStr, mac, token);
        JSONObject js = resp.optJSONObject("js");
        if (js != null) {
            String streamCmd = js.optString("cmd", "");
            if (!streamCmd.isEmpty()) return extractUrl(streamCmd);
        }
        return extractUrl(cmd);
    }




    public static String extractUrl(String cmd) {
        if (cmd == null) return "";
        cmd = cmd.trim();
        if (cmd.startsWith("ffmpeg ")) return cmd.substring(7).trim();
        if (cmd.startsWith("ffrt2 ")) return cmd.substring(6).trim();
        if (cmd.startsWith("ffrt "))  return cmd.substring(5).trim();
        return cmd;
    }

    private static JSONObject doGet(String urlStr, String mac, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(TIMEOUT);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        String cookie = "mac=" + URLEncoder.encode(mac, "UTF-8") + "; stb_lang=en; timezone=Europe/London";
        conn.setRequestProperty("Cookie", cookie);
        conn.setRequestProperty("Referer", extractBase(urlStr));
        conn.setRequestProperty("X-User-Agent", "Model: MAG250; Link: WiFi");
        if (token != null && !token.isEmpty())
            conn.setRequestProperty("Authorization", "Bearer " + token);
        int code = conn.getResponseCode();
        if (code != 200) throw new Exception("HTTP " + code + " — URL: " + urlStr);
        BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) sb.append(line);
        r.close();
        conn.disconnect();
        String body = sb.toString().trim();
        Log.d(TAG, "Yanıt (" + urlStr + "): " + body.substring(0, Math.min(body.length(), 300)));
        if (body.isEmpty()) throw new Exception("Boş yanıt");
        return new JSONObject(body);
    }

    public static String normalizeUrl(String url) {
        if (url == null) return "";
        url = url.trim();
        if (!url.endsWith("/")) url = url + "/";
        return url;
    }

    private static String extractBase(String urlStr) {
        try {
            URL u = new URL(urlStr);
            return u.getProtocol() + "://" + u.getHost() + (u.getPort() != -1 ? ":" + u.getPort() : "") + "/";
        } catch (Exception e) {
            return urlStr;
        }
    }
}