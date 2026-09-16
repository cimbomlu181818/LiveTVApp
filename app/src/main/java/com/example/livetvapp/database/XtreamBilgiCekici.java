package com.example.livetvapp.database;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.example.livetvapp.Channel;
public class XtreamBilgiCekici {
    private static final int MAX_KANAL = 10;
    private static final int TIMEOUT_MS = 8000;
    private static final Pattern XTREAM_PATTERN = Pattern.compile(
            "^(https?://[^/]+:\\d+)/([^/]+)/([^/]+)/\\d+.*$",
            Pattern.CASE_INSENSITIVE
    );
    public static class XtreamHesapBilgisi {
        public String sunucu;
        public String kullanici;
        public String durum;
        public String bitisTarihi;
        public int    aktifBaglanti;
        public int    maxBaglanti;
        public boolean denemeMi;
        public String olusturmaTarihi;
        public String hata;
        public boolean basarili() { return hata == null; }
    }
    public interface BilgiCallback {
        void onSonuc(XtreamHesapBilgisi bilgi);
    }
    public static void bilgiCek(List<Channel> kanallar, BilgiCallback callback) {
        if (kanallar == null || kanallar.isEmpty()) {
            gondercallback(callback, hataOlustur("Kanal listesi boş."));
            return;
        }
        new Thread(() -> {
            int taranacak = Math.min(MAX_KANAL, kanallar.size());
            String sunucu   = null;
            String kullanici = null;
            String sifre    = null;
            for (int i = 0; i < taranacak; i++) {
                String url = kanallar.get(i).getUrl();
                if (url == null || url.isEmpty()) continue;
                Matcher m = XTREAM_PATTERN.matcher(url.trim());
                if (m.matches()) {
                    sunucu   = m.group(1);
                    kullanici = m.group(2);
                    sifre    = m.group(3);
                    break;
                }
            }
            if (sunucu == null) {
                gondercallback(callback,
                        hataOlustur("Bu M3U Xtream Codes formatında değil.\n" +
                                "İlk " + taranacak + " kanal URL'si kontrol edildi."));
                return;
            }
            String apiUrl = sunucu + "/player_api.php?username=" + kullanici + "&password=" + sifre;
            XtreamHesapBilgisi bilgi = apiIstekAt(apiUrl, sunucu, kullanici);
            gondercallback(callback, bilgi);
        }).start();
    }
    private static XtreamHesapBilgisi apiIstekAt(String apiUrl, String sunucu, String kullanici) {
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return hataOlustur("Sunucu yanıt vermedi. (HTTP " + responseCode + ")");
            }
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            conn.disconnect();
            return jsonParse(sb.toString(), sunucu, kullanici);
        } catch (Exception e) {
            return hataOlustur("Bağlantı hatası: " + e.getMessage());
        }
    }
    private static XtreamHesapBilgisi jsonParse(String json, String sunucu, String kullanici) {
        try {
            JSONObject root = new JSONObject(json);
            JSONObject userInfo = root.optJSONObject("user_info");
            if (userInfo == null) userInfo = root;
            XtreamHesapBilgisi bilgi = new XtreamHesapBilgisi();
            bilgi.sunucu    = sunucu;
            bilgi.kullanici = kullanici;
            bilgi.durum     = userInfo.optString("status", "Bilinmiyor");
            bilgi.denemeMi  = "1".equals(userInfo.optString("is_trial", "0"));
            bilgi.aktifBaglanti = userInfo.optInt("active_cons", 0);
            bilgi.maxBaglanti   = userInfo.optInt("max_connections", 0);
            if (bilgi.maxBaglanti == 0) {
                try { bilgi.maxBaglanti = Integer.parseInt(
                        userInfo.optString("max_connections", "0")); }
                catch (Exception ignored) {}
            }
            String expRaw = userInfo.optString("exp_date", "");
            bilgi.bitisTarihi = tarihCevir(expRaw);
            String createdRaw = userInfo.optString("created_at", "");
            bilgi.olusturmaTarihi = tarihCevir(createdRaw);
            return bilgi;
        } catch (Exception e) {
            return hataOlustur("Yanıt parse hatası: " + e.getMessage());
        }
    }
    private static String tarihCevir(String raw) {
        if (raw == null || raw.isEmpty() || raw.equals("null")) return "Belirtilmemiş";
        try {
            long ts = Long.parseLong(raw);
            if (ts <= 0) return "Süresiz";
            Date date = new Date(ts * 1000L);
            return new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(date);
        } catch (Exception e) {
            return raw;
        }
    }
    private static XtreamHesapBilgisi hataOlustur(String mesaj) {
        XtreamHesapBilgisi bilgi = new XtreamHesapBilgisi();
        bilgi.hata = mesaj;
        return bilgi;
    }
    private static void gondercallback(BilgiCallback callback, XtreamHesapBilgisi bilgi) {
        new Handler(Looper.getMainLooper()).post(() -> callback.onSonuc(bilgi));
    }
}