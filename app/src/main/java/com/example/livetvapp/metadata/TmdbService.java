package com.example.livetvapp.metadata;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class TmdbService {
    private static final String API_KEY   = "a899bd2bbf4a75580aa07f0f0b4f1d9e";
    private static final String BASE_URL  = "https://api.themoviedb.org/3";
    private static final String DILI      = "tr-TR";
    private static final String YEDEK_DIL = "en-US";
    private static final LruCache<String, MetadataModel> onbellekCache = new LruCache<>(100);
    private static TmdbService INSTANCE;
    private final ExecutorService executor    = Executors.newFixedThreadPool(2);
    private final Handler         mainHandler = new Handler(Looper.getMainLooper());
    private TmdbService() {}
    public static synchronized TmdbService getInstance() {
        if (INSTANCE == null) INSTANCE = new TmdbService();
        return INSTANCE;
    }
    public interface MetadataCallback {
        void onBasarili(MetadataModel model);
        void onHata(String hata);
    }
    public void ara(String kanalAdi, String icerikTipi, MetadataCallback callback) {
        System.out.println("🔍 [TMDB] ara() kanalAdi=" + kanalAdi);
        if (kanalAdi == null || kanalAdi.isEmpty()) {
            callback.onHata("Kanal adı boş");
            return;
        }
        String[] parcalanmis = kanalAdiniParcala(kanalAdi);
        String temizAd = parcalanmis[0];
        String yil     = parcalanmis[1];
        String cacheAnahtari = (temizAd + "_" + yil + "_" + icerikTipi).toLowerCase();
        MetadataModel onceki = onbellekCache.get(cacheAnahtari);
        if (onceki != null) {
            System.out.println("⚡ [TMDB] Cache HIT: " + temizAd);
            mainHandler.post(() -> callback.onBasarili(onceki));
            return;
        }
        System.out.println("🔍 [TMDB] Aranıyor: " + temizAd + " (" + yil + ") [" + icerikTipi + "]");
        executor.execute(() -> {
            try {
                String tmdbId = filmAra(temizAd, yil, icerikTipi);
                if (tmdbId == null) {
                    mainHandler.post(() -> callback.onHata("Bulunamadı: " + temizAd));
                    return;
                }
                MetadataModel model = detaylariCek(tmdbId, icerikTipi);
                if (model == null) {
                    mainHandler.post(() -> callback.onHata("Detay çekilemedi"));
                    return;
                }
                List<String> altyaziDilleri = altyaziDilleriniCek(tmdbId, icerikTipi);
                model.setAltyaziDilleri(altyaziDilleri);
                onbellekCache.put(cacheAnahtari, model);
                System.out.println("✅ [TMDB] Bulundu: " + model.getBaslik());
                mainHandler.post(() -> callback.onBasarili(model));
            } catch (Exception e) {
                System.out.println("❌ [TMDB] Hata: " + e.getMessage());
                mainHandler.post(() -> callback.onHata(e.getMessage()));
            }
        });
    }
    private String filmAra(String ad, String yil, String icerikTipi) throws Exception {
        String endpoint  = "MOVIE".equals(icerikTipi) ? "/search/movie" : "/search/tv";
        String encodedAd = URLEncoder.encode(ad, "UTF-8");
        for (String dil : new String[]{DILI, YEDEK_DIL}) {
            String urlStr = BASE_URL + endpoint
                    + "?api_key=" + API_KEY
                    + "&language=" + dil
                    + "&query=" + encodedAd
                    + (yil != null ? "&year=" + yil : "");
            String yanit = httpGet(urlStr);
            if (yanit == null) continue;
            JSONArray results = new JSONObject(yanit).optJSONArray("results");
            if (results != null && results.length() > 0) {
                return String.valueOf(results.getJSONObject(0).getInt("id"));
            }
        }
        return null;
    }
    private MetadataModel detaylariCek(String tmdbId, String icerikTipi) throws Exception {
        String endpoint = "MOVIE".equals(icerikTipi)
                ? "/movie/" + tmdbId
                : "/tv/" + tmdbId;
        String urlStr = BASE_URL + endpoint
                + "?api_key=" + API_KEY
                + "&language=" + DILI
                + "&append_to_response=release_dates,content_ratings,credits";
        String yanit = httpGet(urlStr);
        if (yanit == null) return null;
        JSONObject    json  = new JSONObject(yanit);
        MetadataModel model = new MetadataModel();
        model.setTmdbId(tmdbId);
        model.setIcerikTipi(icerikTipi);
        if ("MOVIE".equals(icerikTipi)) {
            model.setBaslik(json.optString("title", ""));
            model.setOrijinalBaslik(json.optString("original_title", ""));
        } else {
            model.setBaslik(json.optString("name", ""));
            model.setOrijinalBaslik(json.optString("original_name", ""));
        }
        String overview = json.optString("overview", "");
        if (overview.isEmpty()) {
            String yedekUrl   = BASE_URL + endpoint + "?api_key=" + API_KEY + "&language=" + YEDEK_DIL;
            String yedekYanit = httpGet(yedekUrl);
            if (yedekYanit != null) {
                overview = new JSONObject(yedekYanit).optString("overview", "");
            }
        }
        model.setKonu(overview);
        String posterPath   = json.optString("poster_path",   "");
        String backdropPath = json.optString("backdrop_path", "");
        model.setPosterUrl(!posterPath.isEmpty()    ? posterPath   : null);
        model.setBackdropUrl(!backdropPath.isEmpty() ? backdropPath : null);
        String tarih = "MOVIE".equals(icerikTipi)
                ? json.optString("release_date", "")
                : json.optString("first_air_date", "");
        if (tarih.length() >= 4) model.setYil(tarih.substring(0, 4));
        if ("MOVIE".equals(icerikTipi)) {
            int dakika = json.optInt("runtime", 0);
            if (dakika > 0) model.setSure(dakika + " dk");
        }
        double tmdbPuan = json.optDouble("vote_average", 0);
        if (tmdbPuan > 0) model.setTmdbPuan(String.format("%.1f", tmdbPuan));
        String imdbId = json.optString("imdb_id", "");
        model.setImdbId(!imdbId.isEmpty() ? imdbId : null);
        JSONArray    genres = json.optJSONArray("genres");
        List<String> turler = new ArrayList<>();
        if (genres != null) {
            for (int i = 0; i < genres.length(); i++)
                turler.add(genres.getJSONObject(i).optString("name", ""));
        }
        model.setTurler(turler);
        model.setSesDili(dilKodunuCevir(json.optString("original_language", "")));
        JSONArray countries = json.optJSONArray("production_countries");
        if (countries != null && countries.length() > 0)
            model.setUlke(countries.getJSONObject(0).optString("name", ""));
        model.setYasSiniri(yasSiniriniBul(json, icerikTipi));
        JSONObject creditsObj = json.optJSONObject("credits");
        if (creditsObj != null) {
            JSONArray cast = creditsObj.optJSONArray("cast");
            if (cast != null) {
                List<MetadataModel.Oyuncu> oyuncular = new ArrayList<>();
                int sinir = Math.min(cast.length(), 8);
                for (int i = 0; i < sinir; i++) {
                    JSONObject kisi   = cast.getJSONObject(i);
                    String ad         = kisi.optString("name", "");
                    String karakter   = kisi.optString("character", "");
                    String profilYolu = kisi.optString("profile_path", "");
                    String fotoUrl    = (!profilYolu.isEmpty() && !"null".equals(profilYolu))
                            ? "https://image.tmdb.org/t/p/w185" + profilYolu
                            : null;
                    oyuncular.add(new MetadataModel.Oyuncu(ad, karakter, fotoUrl));
                }
                model.setOyuncular(oyuncular);
            }
            JSONArray crew = creditsObj.optJSONArray("crew");
            if (crew != null) {
                for (int i = 0; i < crew.length(); i++) {
                    JSONObject kisi = crew.getJSONObject(i);
                    String job = kisi.optString("job", "");
                    if ("Director".equals(job) || "Series Director".equals(job)) {
                        model.setYonetmen(kisi.optString("name", null));
                        break;
                    }
                }
            }
        }
        return model;
    }
    private List<String> altyaziDilleriniCek(String tmdbId, String icerikTipi) {
        List<String> sonuc = new ArrayList<>();
        try {
            String endpoint = "MOVIE".equals(icerikTipi)
                    ? "/movie/" + tmdbId + "/translations"
                    : "/tv/" + tmdbId + "/translations";
            String urlStr = BASE_URL + endpoint + "?api_key=" + API_KEY;
            String yanit  = httpGet(urlStr);
            if (yanit == null) return sonuc;
            JSONArray translations = new JSONObject(yanit).optJSONArray("translations");
            if (translations == null) return sonuc;
            List<String> oncelikli = new ArrayList<>();
            List<String> diger     = new ArrayList<>();
            for (int i = 0; i < translations.length(); i++) {
                JSONObject t    = translations.getJSONObject(i);
                String iso639   = t.optString("iso_639_1", "");
                JSONObject data = t.optJSONObject("data");
                String overview = data != null ? data.optString("overview", "") : "";
                if (overview.isEmpty()) continue;
                String dilAdi = dilKodunuCevir(iso639);
                if ("tr".equals(iso639) || "en".equals(iso639)) {
                    if (!oncelikli.contains(dilAdi)) oncelikli.add(dilAdi);
                } else {
                    if (!diger.contains(dilAdi)) diger.add(dilAdi);
                }
            }
            sonuc.addAll(oncelikli);
            sonuc.addAll(diger);
        } catch (Exception e) {
            System.out.println("⚠️ [TMDB] Altyazı dilleri alınamadı: " + e.getMessage());
        }
        return sonuc;
    }
    private String yasSiniriniBul(JSONObject json, String icerikTipi) {
        try {
            if ("MOVIE".equals(icerikTipi)) {
                JSONObject releaseDates = json.optJSONObject("release_dates");
                if (releaseDates == null) return "Genel";
                JSONArray results = releaseDates.optJSONArray("results");
                if (results == null) return "Genel";
                for (String aranan : new String[]{"TR", "US"}) {
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject ulke = results.getJSONObject(i);
                        if (aranan.equals(ulke.optString("iso_3166_1", ""))) {
                            JSONArray rd = ulke.optJSONArray("release_dates");
                            if (rd != null && rd.length() > 0) {
                                String cert = rd.getJSONObject(0).optString("certification", "");
                                if (!cert.isEmpty()) return cert;
                            }
                        }
                    }
                }
            } else {
                JSONObject contentRatings = json.optJSONObject("content_ratings");
                if (contentRatings == null) return "Genel";
                JSONArray results = contentRatings.optJSONArray("results");
                if (results == null) return "Genel";
                for (String aranan : new String[]{"TR", "US"}) {
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject ulke = results.getJSONObject(i);
                        if (aranan.equals(ulke.optString("iso_3166_1", ""))) {
                            String rating = ulke.optString("rating", "");
                            if (!rating.isEmpty()) return rating;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ [TMDB] Yaş sınırı alınamadı: " + e.getMessage());
        }
        return "Genel";
    }
    private String[] kanalAdiniParcala(String kanalAdi) {
        String temiz = kanalAdi.replaceAll("^[A-Z]{2}:\\s*", "").trim();
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\b(19|20)\\d{2}\\b")
                .matcher(temiz);
        String yil    = null;
        int    sonPos = 0;
        while (m.find()) { yil = m.group(); sonPos = m.start(); }
        if (yil != null)
            temiz = (temiz.substring(0, sonPos) + temiz.substring(sonPos + 4)).trim();
        temiz = temiz.replaceAll("[\\(\\)\\[\\]\\-]", "").trim();
        temiz = temiz.replaceAll("\\s{2,}", " ").trim();
        return new String[]{temiz, yil};
    }
    private String dilKodunuCevir(String kod) {
        if (kod == null || kod.isEmpty()) return "Bilinmiyor";
        switch (kod) {
            case "tr": return "Türkçe";
            case "en": return "İngilizce";
            case "de": return "Almanca";
            case "fr": return "Fransızca";
            case "es": return "İspanyolca";
            case "it": return "İtalyanca";
            case "ru": return "Rusça";
            case "ar": return "Arapça";
            case "ja": return "Japonca";
            case "ko": return "Korece";
            case "zh": return "Çince";
            case "pt": return "Portekizce";
            case "nl": return "Hollandaca";
            case "pl": return "Lehçe";
            case "sv": return "İsveççe";
            default:   return kod.toUpperCase();
        }
    }
    private String httpGet(String urlStr) {
        HttpURLConnection conn = null;
        try {
            javax.net.ssl.TrustManager[] trustAll = new javax.net.ssl.TrustManager[]{
                    new javax.net.ssl.X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
                        public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) {}
                        public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) {}
                    }
            };
            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("TLS");
            sc.init(null, trustAll, new java.security.SecureRandom());
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            if (conn instanceof javax.net.ssl.HttpsURLConnection) {
                ((javax.net.ssl.HttpsURLConnection) conn).setSSLSocketFactory(sc.getSocketFactory());
                ((javax.net.ssl.HttpsURLConnection) conn).setHostnameVerifier((h, s) -> true);
            }
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("Accept", "application/json");
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            System.out.println("❌ [TMDB] httpGet hata: " + e.getMessage());
            return null;
        } finally {
            if (conn != null) conn.disconnect();

        }
    }
}