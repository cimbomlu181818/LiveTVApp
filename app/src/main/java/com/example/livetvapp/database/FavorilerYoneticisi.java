package com.example.livetvapp.database;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
public class FavorilerYoneticisi {
    private static final String PREFS_NAME   = "FavorilerPrefs";
    private static final String KEY_PREFIX   = "favs_";
    public static final String FAVORI_KATEGORI_ADI = "⭐ Favoriler";
    private static FavorilerYoneticisi instance;
    private final SharedPreferences prefs;
    private FavorilerYoneticisi(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    public static synchronized FavorilerYoneticisi getInstance(Context context) {
        if (instance == null) instance = new FavorilerYoneticisi(context);
        return instance;
    }
    public void ekle(String icerikTipi, String anahtar) {
        if (icerikTipi == null || anahtar == null || anahtar.isEmpty()) return;
        List<String> liste = getSet(icerikTipi);
        if (!liste.contains(anahtar)) liste.add(anahtar);
        kaydet(icerikTipi, liste);
        System.out.println("⭐ Favoriye eklendi [" + icerikTipi + "]: " + anahtar);
    }
    public void cikar(String icerikTipi, String anahtar) {
        if (icerikTipi == null || anahtar == null) return;
        List<String> liste = getSet(icerikTipi);
        liste.remove(anahtar);
        kaydet(icerikTipi, liste);
        System.out.println("☆ Favoriden çıkarıldı [" + icerikTipi + "]: " + anahtar);
    }
    public boolean toggle(String icerikTipi, String anahtar) {
        if (isFavori(icerikTipi, anahtar)) {
            cikar(icerikTipi, anahtar);
            return false;
        } else {
            ekle(icerikTipi, anahtar);
            return true;
        }
    }
    public void sirayiGuncelle(String icerikTipi, List<String> yeniSira) {
        if (icerikTipi == null || yeniSira == null) return;
        List<String> mevcutListe = getSet(icerikTipi);
        List<String> yeniListe = new ArrayList<>();
        for (String anahtar : yeniSira) {
            if (mevcutListe.contains(anahtar)) yeniListe.add(anahtar);
        }
        for (String anahtar : mevcutListe) {
            if (!yeniListe.contains(anahtar)) yeniListe.add(anahtar);
        }
        kaydet(icerikTipi, yeniListe);
        System.out.println("🔀 Favori sırası güncellendi [" + icerikTipi + "]: " + yeniListe.size() + " öğe");
    }
    public boolean isFavori(String icerikTipi, String anahtar) {
        if (icerikTipi == null || anahtar == null) return false;
        return getSet(icerikTipi).contains(anahtar);
    }
    public List<String> getHepsi(String icerikTipi) {
        return new ArrayList<>(getSet(icerikTipi));
    }
    public boolean bos(String icerikTipi) {
        return getSet(icerikTipi).isEmpty();
    }
    public int sayim(String icerikTipi) {
        return getSet(icerikTipi).size();
    }
    public void temizle(String icerikTipi) {
        prefs.edit().remove(KEY_PREFIX + icerikTipi).apply();
        System.out.println("🗑️ Favoriler temizlendi [" + icerikTipi + "]");
    }
    public void tumunuTemizle() {
        prefs.edit().clear().apply();
        System.out.println("🗑️ Tüm favoriler temizlendi");
    }
    private static final String AYRAC = "|||";
    private List<String> getSet(String icerikTipi) {
        String key = KEY_PREFIX + icerikTipi;
        try {
            String kayit = prefs.getString(key, null);
            if (kayit == null || kayit.isEmpty()) return new ArrayList<>();
            List<String> liste = new ArrayList<>();
            for (String s : kayit.split("\\|\\|\\|")) {
                if (!s.isEmpty()) liste.add(s);
            }
            return liste;
        } catch (ClassCastException e) {
            try {
                java.util.Set<String> eskiSet = prefs.getStringSet(key, null);
                if (eskiSet == null || eskiSet.isEmpty()) {
                    prefs.edit().remove(key).apply();
                    return new ArrayList<>();
                }
                List<String> liste = new ArrayList<>(eskiSet);
                kaydet(icerikTipi, liste);
                System.out.println("🔄 Favori göç tamamlandı [" + icerikTipi + "]: " + liste.size() + " öğe");
                return liste;
            } catch (Exception e2) {
                prefs.edit().remove(key).apply();
                return new ArrayList<>();
            }
        }
    }
    private void kaydet(String icerikTipi, List<String> liste) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < liste.size(); i++) {
            if (i > 0) sb.append(AYRAC);
            sb.append(liste.get(i));
        }
        prefs.edit().putString(KEY_PREFIX + icerikTipi, sb.toString()).apply();
    }
}