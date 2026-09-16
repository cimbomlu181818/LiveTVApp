package com.example.livetvapp.paneller;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.livetvapp.R;
import com.example.livetvapp.Adapter.GizleAdapter;
import com.example.livetvapp.database.AppDatabase;
import com.example.livetvapp.database.GizleItem;
import com.example.livetvapp.database.M3UItem;
import com.example.livetvapp.database.M3UManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class GizlePaneli {
    private static final String PREF_GIZLI         = "gizli_kanallar";
    private boolean dialogAcik = false;
    private boolean degisiklikVar = false;
    private static final String PREF_GIZLI_KATEGORI = "gizli_kategoriler";
    private final AppCompatActivity activity;
    private final M3UManager        m3uManager;
    private final ExecutorService   executor    = Executors.newSingleThreadExecutor();
    private final Handler           mainHandler = new Handler(Looper.getMainLooper());
    private View         panelView;
    private RecyclerView recyclerView;
    private TextView     baslikView;
    private GizleAdapter adapter;
    private List<M3UItem> tumM3UListesi = new ArrayList<>();
    private final Set<String>                  acikM3Ular      = new HashSet<>();
    private final Set<String>                  acikKategoriler = new HashSet<>();
    private final Map<String, List<GizleItem>> kanalCache      = new HashMap<>();
    private final Map<String, Boolean>         gizliKanallar   = new HashMap<>();
    private final Set<String>                  gizliKategoriler = new HashSet<>();
    private final Map<String, Set<String>> m3uIleGizlenenKategoriler = new HashMap<>();
    private final Map<String, List<String>> m3uKategoriOnbellegi = new HashMap<>();
    private final Set<String> seriesKategoriler = new HashSet<>();
    private int focusPoz = 0;
    private static final int KANAL_SAYFA_BOYUT  = 9999990;
    private static final int KANAL_YUKLE_ONCESI = 10;
    private final Map<String, List<GizleItem>> kanalTumCache        = new HashMap<>();
    private final Map<String, Integer>         kanalYuklenmisSayisi = new HashMap<>();
    private       String                       lazyYukleniyor       = null;
    public interface PanelKapandiCallback { void onKapandi(); }
    private PanelKapandiCallback kapandiCallback;
    public void setPanelKapandiCallback(PanelKapandiCallback cb) { this.kapandiCallback = cb; }
    public void centerTiklandi() {
        GizleItem item = adapter.getItem(focusPoz);
        if (item == null) return;
        if (item.getTip() == GizleItem.TIP_M3U) {
            m3uToggle(item, focusPoz);
        } else if (item.getTip() == GizleItem.TIP_KATEGORI) {
            kategoriToggle(item, focusPoz);
        }
    }
    public interface PanelIptalCallback { void onIptal(); }
    private PanelIptalCallback iptalCallback;
    public void setPanelIptalCallback(PanelIptalCallback cb) { this.iptalCallback = cb; }
    public GizlePaneli(AppCompatActivity activity) {
        this.activity   = activity;
        this.m3uManager = new M3UManager(activity);
    }
    public void olustur() {
        panelView    = LayoutInflater.from(activity).inflate(R.layout.panel_gizle, null);
        recyclerView = panelView.findViewById(R.id.gizleRecyclerView);
        baslikView   = panelView.findViewById(R.id.gizlePanelBaslik);
        panelView.setVisibility(View.GONE);
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity) {
            @Override
            public boolean requestChildRectangleOnScreen(
                    RecyclerView parent, View child,
                    android.graphics.Rect rect, boolean immediate,
                    boolean focusedChildVisible) {
                int parentHeight = parent.getHeight();
                int childTop     = child.getTop();
                int childBottom  = child.getBottom();
                int childCenter  = (childTop + childBottom) / 2;
                int parentCenter = parentHeight / 2;
                int itemCount = getItemCount();
                int pos       = parent.getChildAdapterPosition(child);
                int childH    = child.getHeight() > 0 ? child.getHeight() : 1;
                boolean sonlaraYakin = pos >= itemCount - parentHeight / childH / 2;
                if (sonlaraYakin) {
                    return super.requestChildRectangleOnScreen(
                            parent, child, rect, immediate, focusedChildVisible);
                }
                int kaydirma = childCenter - parentCenter;
                if (kaydirma == 0) return false;
                if (immediate) parent.scrollBy(0, kaydirma);
                else           parent.smoothScrollBy(0, kaydirma);
                return true;
            }
        };
        recyclerView.setLayoutManager(layoutManager);
        adapter = new GizleAdapter();
        adapter.setOnFocusDegisti((item, poz) -> focusPoz = poz);
        adapter.setOnBaslikTiklandi((item, poz) -> {
            if (item.getTip() == GizleItem.TIP_M3U)           m3uToggle(item, poz);
            else if (item.getTip() == GizleItem.TIP_KATEGORI) kategoriToggle(item, poz);
        });
        adapter.setOnGizlemeDegisti((item, gizli) -> {
            if (item.getTip() == GizleItem.TIP_KANAL
                    && item.getUrl() != null
                    && !item.getUrl().startsWith("DIZI:")) {
                gizliKanallar.put(item.getUrl(), gizli);
            }
            degisiklikVar = true;
        });
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                if (dy <= 0) return;
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm == null) return;
                int gorunan    = lm.getChildCount();
                int toplam     = lm.getItemCount();
                int ilkGorunen = lm.findFirstVisibleItemPosition();
                if ((gorunan + ilkGorunen) >= toplam - KANAL_YUKLE_ONCESI)
                    scrollIleKanalYukle(ilkGorunen + gorunan);
            }
        });
        activity.addContentView(panelView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
        System.out.println("✅ GizlePaneli oluşturuldu");
    }
    public void ac() {
        degisiklikVar = false;
        tumM3UListesi = m3uManager.getAllM3UItems();
        gizliKategoriler.clear();
        gizliKanallar.clear();
        acikM3Ular.clear();
        acikKategoriler.clear();
        kanalCache.clear();
        kanalTumCache.clear();
        kanalYuklenmisSayisi.clear();
        m3uIleGizlenenKategoriler.clear();
        m3uKategoriOnbellegi.clear();
        seriesKategoriler.clear();
        lazyYukleniyor = null;
        SharedPreferences prefs = activity.getSharedPreferences(PREF_GIZLI, Context.MODE_PRIVATE);
        for (Map.Entry<String, ?> e : prefs.getAll().entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) gizliKanallar.put(e.getKey(), true);
        }
        SharedPreferences prefsKat = activity.getSharedPreferences(PREF_GIZLI_KATEGORI, Context.MODE_PRIVATE);
        for (Map.Entry<String, ?> e : prefsKat.getAll().entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) gizliKategoriler.add(e.getKey());
        }
        List<GizleItem> ilkListe = new ArrayList<>();
        for (M3UItem m3u : tumM3UListesi) {
            ilkListe.add(GizleItem.m3u(m3u.getName(), false));
        }
        adapter.setListe(ilkListe);
        if (baslikView != null) baslikView.setText("GİZLE / GÖSTER");
        focusPoz = 0;
        panelView.setVisibility(View.VISIBLE);
        for (M3UItem m3u : tumM3UListesi) {
            m3uKategorileriniYukleVeGuncelle(m3u.getName());
        }
        focusVer(0);
    }
    private void m3uKategorileriniYukleVeGuncelle(String m3uAdi) {
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(activity);
                List<String> tumKat = db.channelDao().getAllCategoriesByM3U(m3uAdi);
                mainHandler.post(() -> {
                    m3uKategoriOnbellegi.put(m3uAdi, tumKat);
                    m3uGizlilikDurumuGuncelle(m3uAdi);
                });
            } catch (Exception e) {
                System.out.println("❌ m3uKategorileriniYukle: " + e.getMessage());
            }
        });
    }
    private void m3uToggle(GizleItem m3uItem, int poz) {
        String ad = m3uItem.getAd();
        if (acikM3Ular.contains(ad)) {
            int n = altSayisiM3U(ad, poz);
            acikM3Ular.remove(ad);
            Iterator<String> ki = acikKategoriler.iterator();
            while (ki.hasNext()) { if (ki.next().startsWith(ad + "|")) ki.remove(); }
            Iterator<Map.Entry<String, Integer>> si = kanalYuklenmisSayisi.entrySet().iterator();
            while (si.hasNext()) { if (si.next().getKey().startsWith(ad + "|")) si.remove(); }
            m3uItem.setAcik(false);
            adapter.kaldir(poz + 1, n);
            adapter.guncelle(poz);
        } else {
            acikM3Ular.add(ad);
            m3uItem.setAcik(true);
            adapter.guncelle(poz);
            lazyKategoriler(ad, poz);
        }
    }
    private void lazyKategoriler(final String m3uAdi, int m3uPoz) {
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(activity);
                final List<String> tumKat = db.channelDao().getAllCategoriesByM3U(m3uAdi);
                final Set<String> liveSet   = new LinkedHashSet<>(db.channelDao().getCategoriesByType(m3uAdi, "LIVE"));
                final Set<String> movieSet  = new LinkedHashSet<>(db.channelDao().getCategoriesByType(m3uAdi, "MOVIE"));
                final Set<String> seriesSet = new LinkedHashSet<>(db.channelDao().getCategoriesByType(m3uAdi, "SERIES"));
                mainHandler.post(() -> {
                    m3uKategoriOnbellegi.put(m3uAdi, tumKat);
                    int gercekPoz = adapterPozBulM3U(m3uAdi);
                    if (gercekPoz == -1) return;
                    List<String> siraliTumKat = siralamaKaydiUygula(tumKat, liveSet, movieSet, seriesSet);
                    List<GizleItem> katOge = new ArrayList<>();
                    for (String kat : siraliTumKat) {
                        String key = m3uAdi + "|" + kat;
                        boolean g  = gizliKategoriler.contains(key);
                        String tur = "";
                        if (liveSet.contains(kat))   tur = "Canlı TV";
                        if (movieSet.contains(kat))  tur = tur.isEmpty() ? "Film"  : tur + "/Film";
                        if (seriesSet.contains(kat)) {
                            tur = tur.isEmpty() ? "Dizi"  : tur + "/Dizi";
                            seriesKategoriler.add(key);
                        }
                        String adGoster = tur.isEmpty() ? kat : kat + " (" + tur + ")";
                        katOge.add(GizleItem.kategori(adGoster, kat, m3uAdi, g));
                    }
                    adapter.ekle(gercekPoz + 1, katOge);
                    System.out.println("📂 " + m3uAdi + ": " + tumKat.size() + " kategori yüklendi");
                });
            } catch (Exception e) {
                System.out.println("❌ lazyKategoriler: " + e.getMessage());
            }
        });
    }
    private void kategoriToggle(GizleItem katItem, int poz) {
        String key = katItem.getM3uAdi() + "|" + katItem.getRawAd();
        if (acikKategoriler.contains(key)) {
            int n = altSayisiKategori(poz);
            acikKategoriler.remove(key);
            katItem.setAcik(false);
            adapter.kaldir(poz + 1, n);
            adapter.guncelle(poz);
            kanalYuklenmisSayisi.remove(key);
        } else {
            acikKategoriler.add(key);
            katItem.setAcik(true);
            adapter.guncelle(poz);
            lazyKanallar(katItem, key);
        }
    }
    private static String diziAdiniCikar(String kanalAdi) {
        if (kanalAdi == null) return "";
        String temiz = kanalAdi.trim();
        java.util.regex.Matcher m1 = java.util.regex.Pattern
                .compile("(?i)[\\s\\-]+[Ss]\\d+\\s*[Ee]\\d+.*$")
                .matcher(temiz);
        if (m1.find()) {
            String ad = temiz.substring(0, m1.start()).trim();
            return ad.isEmpty() ? temiz : ad;
        }
        java.util.regex.Matcher m2 = java.util.regex.Pattern
                .compile("(?i)[\\s\\-]+[Ss]ezon\\s*\\d+.*$")
                .matcher(temiz);
        if (m2.find()) {
            String ad = temiz.substring(0, m2.start()).trim();
            return ad.isEmpty() ? temiz : ad;
        }
        return temiz;
    }
    private void lazyKanallar(GizleItem katItem, String cacheKey) {
        final String m3uAdi         = katItem.getM3uAdi();
        final String rawKategoriAdi = katItem.getRawAd();
        boolean isSeries = "SERIES".equals(rawKategoriTipiBul(m3uAdi, rawKategoriAdi));
        if (kanalTumCache.containsKey(cacheKey)) {
            List<GizleItem> tumListe = kanalTumCache.get(cacheKey);
            if (tumListe == null) return;
            if (isSeries) {
                List<GizleItem> diziListesi = diziGorunumListesiOlustur(tumListe, m3uAdi, rawKategoriAdi);
                int gercekPoz = adapterPozBulKategori(katItem.getAd(), m3uAdi);
                if (gercekPoz == -1) return;
                adapter.ekle(gercekPoz + 1, diziListesi);
                kanalYuklenmisSayisi.put(cacheKey, diziListesi.size());
            } else {
                boolean katGizli1 = gizliKategoriler.contains(m3uAdi + "|" + rawKategoriAdi);
                for (GizleItem k : tumListe)
                    k.setGizli(katGizli1 || Boolean.TRUE.equals(gizliKanallar.get(k.getUrl())));
                int gercekPoz = adapterPozBulKategori(katItem.getAd(), m3uAdi);
                if (gercekPoz == -1) return;
                int ilkSayfaBitis = Math.min(KANAL_SAYFA_BOYUT, tumListe.size());
                adapter.ekle(gercekPoz + 1, new ArrayList<>(tumListe.subList(0, ilkSayfaBitis)));
                kanalYuklenmisSayisi.put(cacheKey, ilkSayfaBitis);
                kanalCache.put(cacheKey, tumListe);
                System.out.println("⚡ Kanal cache HIT: " + ilkSayfaBitis + "/" + tumListe.size());
            }
            return;
        }
        final String kategoriGorunen = katItem.getAd();
        final boolean finalIsSeries  = isSeries;
        lazyYukleniyor = cacheKey;
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(activity);
                List<com.example.livetvapp.Channel> hepsi = new ArrayList<>();
                if (finalIsSeries) {
                    hepsi.addAll(db.channelDao().getChannelsByTypeAndCategory(m3uAdi, "SERIES", rawKategoriAdi));
                } else {
                    hepsi.addAll(db.channelDao().getChannelsByTypeAndCategory(m3uAdi, "LIVE",   rawKategoriAdi));
                    hepsi.addAll(db.channelDao().getChannelsByTypeAndCategory(m3uAdi, "MOVIE",  rawKategoriAdi));
                    hepsi.addAll(db.channelDao().getChannelsByTypeAndCategory(m3uAdi, "SERIES", rawKategoriAdi));
                }
                Collections.sort(hepsi, (a, b) -> Integer.compare(a.getPosition(), b.getPosition()));
                List<GizleItem> tumKanalOge = new ArrayList<>();
                for (com.example.livetvapp.Channel ch : hepsi) {
                    boolean katGizli2 = gizliKategoriler.contains(m3uAdi + "|" + rawKategoriAdi);
                    boolean g = katGizli2 || Boolean.TRUE.equals(gizliKanallar.get(ch.getUrl()));
                    tumKanalOge.add(GizleItem.kanal(ch.getName(), m3uAdi, rawKategoriAdi, ch.getUrl(), g));
                }
                kanalTumCache.put(cacheKey, tumKanalOge);
                kanalCache.put(cacheKey, tumKanalOge);
                mainHandler.post(() -> {
                    int gercekPoz = adapterPozBulKategori(kategoriGorunen, m3uAdi);
                    if (gercekPoz == -1) { lazyYukleniyor = null; return; }
                    if (finalIsSeries) {
                        List<GizleItem> diziListesi = diziGorunumListesiOlustur(
                                tumKanalOge, m3uAdi, rawKategoriAdi);
                        adapter.ekle(gercekPoz + 1, diziListesi);
                        kanalYuklenmisSayisi.put(cacheKey, diziListesi.size());
                        System.out.println("📺 " + m3uAdi + "/" + rawKategoriAdi
                                + ": " + diziListesi.size() + " dizi listelendi");
                    } else {
                        int ilkSayfaBitis = Math.min(KANAL_SAYFA_BOYUT, tumKanalOge.size());
                        List<GizleItem> ilkSayfa = new ArrayList<>(tumKanalOge.subList(0, ilkSayfaBitis));
                        adapter.ekle(gercekPoz + 1, ilkSayfa);
                        kanalYuklenmisSayisi.put(cacheKey, ilkSayfaBitis);
                        System.out.println("📺 " + m3uAdi + "/" + rawKategoriAdi
                                + ": " + ilkSayfaBitis + "/" + tumKanalOge.size() + " kanal yüklendi");
                    }
                    lazyYukleniyor = null;
                });
            } catch (Exception e) {
                lazyYukleniyor = null;
                System.out.println("❌ lazyKanallar: " + e.getMessage());
            }
        });
    }
    private List<GizleItem> diziGorunumListesiOlustur(
            List<GizleItem> gercekKanallar, String m3uAdi, String rawKategoriAdi) {
        LinkedHashSet<String> diziAdlari = new LinkedHashSet<>();
        for (GizleItem k : gercekKanallar) {
            diziAdlari.add(diziAdiniCikar(k.getAd()));
        }
        List<GizleItem> liste = new ArrayList<>();
        for (String diziAdi : diziAdlari) {
            boolean tumGizli = true;
            boolean enAzBir  = false;
            for (GizleItem k : gercekKanallar) {
                if (diziAdiniCikar(k.getAd()).equals(diziAdi)) {
                    enAzBir = true;
                    if (!k.isGizli()) { tumGizli = false; break; }
                }
            }
            boolean diziGizli = enAzBir && tumGizli;
            liste.add(GizleItem.kanal(diziAdi, m3uAdi, rawKategoriAdi,
                    "DIZI:" + diziAdi, diziGizli));
        }
        return liste;
    }
    private String rawKategoriTipiBul(String m3uAdi, String rawKategoriAdi) {
        String key = m3uAdi + "|" + rawKategoriAdi;
        return seriesKategoriler.contains(key) ? "SERIES" : "OTHER";
    }
    private void scrollIleKanalYukle(int sonGorunenPoz) {
        List<GizleItem> lst = adapter.getListe();
        if (lst.isEmpty()) return;
        int baslangic = Math.min(sonGorunenPoz, lst.size() - 1);
        for (int i = baslangic; i >= 0; i--) {
            GizleItem item = lst.get(i);
            if (item.getTip() == GizleItem.TIP_KANAL) {
                String gorununAd = kategoriGorununAdBul(item.getM3uAdi(), item.getKategoriAdi());
                String cacheKey  = item.getM3uAdi() + "|" + gorununAd;
                dahlaFazlaKanalEkle(cacheKey, item.getM3uAdi(), item.getKategoriAdi());
                return;
            } else if (item.getTip() == GizleItem.TIP_KATEGORI
                    || item.getTip() == GizleItem.TIP_M3U) {
                break;
            }
        }
    }
    private String kategoriGorununAdBul(String m3uAdi, String rawKategoriAdi) {
        for (GizleItem g : adapter.getListe()) {
            if (g.getTip() == GizleItem.TIP_KATEGORI
                    && m3uAdi.equals(g.getM3uAdi())
                    && rawKategoriAdi.equals(g.getRawAd())) {
                return g.getAd();
            }
        }
        return rawKategoriAdi;
    }
    private void dahlaFazlaKanalEkle(String cacheKey, String m3uAdi, String rawKategoriAdi) {
        List<GizleItem> tumListe = kanalTumCache.get(cacheKey);
        if (tumListe == null) return;
        Integer yuklenenObj = kanalYuklenmisSayisi.get(cacheKey);
        final int yuklenen  = (yuklenenObj != null) ? yuklenenObj : 0;
        if (yuklenen >= tumListe.size()) return;
        final int bitis      = Math.min(yuklenen + KANAL_SAYFA_BOYUT, tumListe.size());
        final int toplamSayi = tumListe.size();
        final List<GizleItem> yeniSayfa = new ArrayList<>(tumListe.subList(yuklenen, bitis));
        List<GizleItem> lst = adapter.getListe();
        int eklenecekPoz = -1;
        for (int i = lst.size() - 1; i >= 0; i--) {
            GizleItem g = lst.get(i);
            if (g.getTip() == GizleItem.TIP_KANAL
                    && m3uAdi.equals(g.getM3uAdi())
                    && rawKategoriAdi.equals(g.getKategoriAdi())) {
                eklenecekPoz = i + 1;
                break;
            }
        }
        if (eklenecekPoz == -1) return;
        final int finalEklenecekPoz = eklenecekPoz;
        final int finalBitis        = bitis;
        mainHandler.post(() -> {
            adapter.ekle(finalEklenecekPoz, yeniSayfa);
            kanalYuklenmisSayisi.put(cacheKey, finalBitis);
            System.out.println("📥 Scroll lazy: " + m3uAdi + "/" + rawKategoriAdi
                    + " " + yuklenen + "→" + finalBitis + "/" + toplamSayi);
        });
    }
    public boolean dpadAsagi() {
        if (recyclerView == null || adapter == null) return false;
        List<GizleItem> lst = adapter.getListe();
        if (lst.isEmpty()) return false;
        int hedef = focusPoz + 1;
        if (hedef >= lst.size()) return true;
        if (hedef >= lst.size() - KANAL_YUKLE_ONCESI) scrollIleKanalYukle(hedef);
        focusVer(hedef);
        return true;
    }
    public boolean dpadYukari() {
        if (recyclerView == null || adapter == null) return false;
        List<GizleItem> lst = adapter.getListe();
        if (lst.isEmpty()) return false;
        int hedef = focusPoz - 1;
        if (hedef < 0) return true;
        focusVer(hedef);
        return true;
    }
    public boolean handleSolSag(boolean gizle) {
        GizleItem item = adapter.getItem(focusPoz);
        if (item == null) return true;
        boolean yeniDurum = gizle;
        switch (item.getTip()) {
            case GizleItem.TIP_M3U:      topluM3UGizle(item, yeniDurum);      break;
            case GizleItem.TIP_KATEGORI: topluKategoriGizle(item, yeniDurum); break;
            case GizleItem.TIP_KANAL:
                if (item.getUrl() != null && item.getUrl().startsWith("DIZI:")) {
                    topluDiziGizle(item, yeniDurum);
                } else {
                    tekKanalGizle(item, yeniDurum);
                }
                break;
        }
        return true;
    }
    private void topluM3UGizle(GizleItem m3uItem, boolean gizle) {
        degisiklikVar = true;
        String m3uAdi = m3uItem.getAd();
        List<String> kategoriler = m3uKategoriOnbellegi.get(m3uAdi);
        if (kategoriler == null) {
            executor.execute(() -> {
                try {
                    AppDatabase db = AppDatabase.getInstance(activity);
                    List<String> live   = db.channelDao().getCategoriesByType(m3uAdi, "LIVE");
                    List<String> movie  = db.channelDao().getCategoriesByType(m3uAdi, "MOVIE");
                    List<String> series = db.channelDao().getCategoriesByType(m3uAdi, "SERIES");
                    LinkedHashSet<String> set = new LinkedHashSet<>();
                    set.addAll(live); set.addAll(movie); set.addAll(series);
                    List<String> tumKat = new ArrayList<>(set);
                    mainHandler.post(() -> {
                        m3uKategoriOnbellegi.put(m3uAdi, tumKat);
                        m3uKategorileriUygula(m3uItem, m3uAdi, tumKat, gizle);
                    });
                } catch (Exception e) {
                    System.out.println("❌ topluM3UGizle yükle: " + e.getMessage());
                }
            });
        } else {
            m3uKategorileriUygula(m3uItem, m3uAdi, kategoriler, gizle);
        }
    }
    private void m3uKategorileriUygula(GizleItem m3uItem, String m3uAdi,
                                       List<String> kategoriler, boolean gizle) {
        if (kategoriler == null) return;
        if (gizle) {
            Set<String> gizlenenler = m3uIleGizlenenKategoriler.get(m3uAdi);
            if (gizlenenler == null) {
                gizlenenler = new HashSet<>();
                m3uIleGizlenenKategoriler.put(m3uAdi, gizlenenler);
            }
            for (String rawKat : kategoriler) {
                String key = m3uAdi + "|" + rawKat;
                if (!gizliKategoriler.contains(key)) {
                    gizliKategoriler.add(key);
                    gizlenenler.add(rawKat);
                }
            }
        } else {
            for (String rawKat : kategoriler) {
                gizliKategoriler.remove(m3uAdi + "|" + rawKat);
            }
            m3uIleGizlenenKategoriler.remove(m3uAdi);
        }
        if (acikM3Ular.contains(m3uAdi)) {
            List<GizleItem> lst = adapter.getListe();
            int m3uPoz = adapterPozBulM3U(m3uAdi);
            if (m3uPoz != -1) {
                for (int i = m3uPoz + 1; i < lst.size(); i++) {
                    GizleItem alt = lst.get(i);
                    if (alt.getTip() == GizleItem.TIP_M3U) break;
                    if (alt.getTip() == GizleItem.TIP_KATEGORI) {
                        alt.setGizli(gizliKategoriler.contains(m3uAdi + "|" + alt.getRawAd()));
                        adapter.guncelle(i);
                    }
                }
            }
        }
        m3uGizlilikDurumuGuncelle(m3uAdi);
        System.out.println((gizle ? "🚫" : "✓") + " M3U toplu: " + m3uAdi
                + " (" + kategoriler.size() + " kategori)");
    }
    private void m3uGizlilikDurumuGuncelle(String m3uAdi) {
        List<String> kategoriler = m3uKategoriOnbellegi.get(m3uAdi);
        if (kategoriler == null) return;
        boolean tumGizli = !kategoriler.isEmpty();
        for (String rawKat : kategoriler) {
            if (!gizliKategoriler.contains(m3uAdi + "|" + rawKat)) {
                tumGizli = false;
                break;
            }
        }
        int poz = adapterPozBulM3U(m3uAdi);
        if (poz != -1) {
            GizleItem m3uItem = adapter.getItem(poz);
            if (m3uItem != null) {
                m3uItem.setGizli(tumGizli);
                adapter.guncelle(poz);
            }
        }
    }
    private void topluKategoriGizle(GizleItem katItem, boolean gizle) {
        degisiklikVar = true;
        String key = katItem.getM3uAdi() + "|" + katItem.getRawAd();
        katItem.setGizli(gizle);
        if (gizle) gizliKategoriler.add(key); else gizliKategoriler.remove(key);
        adapter.guncelle(focusPoz);
        if (acikKategoriler.contains(key)) {
            List<GizleItem> lst = adapter.getListe();
            for (int i = focusPoz + 1; i < lst.size(); i++) {
                GizleItem alt = lst.get(i);
                if (alt.getTip() != GizleItem.TIP_KANAL) break;
                alt.setGizli(gizle);
                gizliKanallar.put(alt.getUrl(), gizle);
                adapter.guncelle(i);
            }
        }
        List<GizleItem> cachedTum = kanalTumCache.get(key);
        if (cachedTum != null) {
            for (GizleItem k : cachedTum) {
                k.setGizli(gizle);
                gizliKanallar.put(k.getUrl(), gizle);
            }
        } else {
            final String m3uAdi = katItem.getM3uAdi();
            final String rawKat = katItem.getRawAd();
            final boolean gizleF = gizle;
            executor.execute(() -> {
                try {
                    AppDatabase db = AppDatabase.getInstance(activity);
                    List<com.example.livetvapp.Channel> hepsi = new ArrayList<>();
                    hepsi.addAll(db.channelDao().getChannelsByTypeAndCategory(m3uAdi, "LIVE",   rawKat));
                    hepsi.addAll(db.channelDao().getChannelsByTypeAndCategory(m3uAdi, "MOVIE",  rawKat));
                    hepsi.addAll(db.channelDao().getChannelsByTypeAndCategory(m3uAdi, "SERIES", rawKat));
                    List<GizleItem> tumKanalOge = new ArrayList<>();
                    mainHandler.post(() -> {
                        for (com.example.livetvapp.Channel ch : hepsi) {
                            gizliKanallar.put(ch.getUrl(), gizleF);
                            tumKanalOge.add(GizleItem.kanal(ch.getName(), m3uAdi, rawKat, ch.getUrl(), gizleF));
                        }
                        kanalTumCache.put(key, tumKanalOge);
                        System.out.println("📦 Kategori gizle DB yükle: " + m3uAdi + "/" + rawKat
                                + " → " + hepsi.size() + " kanal " + (gizleF ? "gizlendi" : "gösterildi"));
                    });
                } catch (Exception e) {
                    System.out.println("❌ topluKategoriGizle DB yükle: " + e.getMessage());
                }
            });
        }
        m3uGizlilikDurumuGuncelle(katItem.getM3uAdi());
        System.out.println((gizle ? "🚫" : "✓") + " Kategori toplu: " + katItem.getAd());
    }
    private void topluDiziGizle(GizleItem diziItem, boolean gizle) {
        degisiklikVar = true;
        String diziAdi      = diziItem.getAd();
        String m3uAdi       = diziItem.getM3uAdi();
        String rawKategori  = diziItem.getKategoriAdi();
        String cacheKey     = m3uAdi + "|" + rawKategori;
        diziItem.setGizli(gizle);
        adapter.guncelle(focusPoz);
        List<GizleItem> cachedTum = kanalTumCache.get(cacheKey);
        if (cachedTum != null) {
            for (GizleItem k : cachedTum) {
                if (diziAdiniCikar(k.getAd()).equals(diziAdi)) {
                    k.setGizli(gizle);
                    gizliKanallar.put(k.getUrl(), gizle);
                }
            }
        } else {
            final boolean gizleF = gizle;
            executor.execute(() -> {
                try {
                    AppDatabase db = AppDatabase.getInstance(activity);
                    List<com.example.livetvapp.Channel> hepsi =
                            db.channelDao().getChannelsByTypeAndCategory(m3uAdi, "SERIES", rawKategori);
                    mainHandler.post(() -> {
                        List<GizleItem> yeniCache = new ArrayList<>();
                        for (com.example.livetvapp.Channel ch : hepsi) {
                            boolean g = diziAdiniCikar(ch.getName()).equals(diziAdi) ? gizleF
                                    : Boolean.TRUE.equals(gizliKanallar.get(ch.getUrl()));
                            if (diziAdiniCikar(ch.getName()).equals(diziAdi)) {
                                gizliKanallar.put(ch.getUrl(), gizleF);
                            }
                            yeniCache.add(GizleItem.kanal(ch.getName(), m3uAdi, rawKategori, ch.getUrl(), g));
                        }
                        kanalTumCache.put(cacheKey, yeniCache);
                        System.out.println("📦 Dizi gizle DB yükle: " + diziAdi
                                + " → " + hepsi.size() + " bölüm " + (gizleF ? "gizlendi" : "gösterildi"));
                    });
                } catch (Exception e) {
                    System.out.println("❌ topluDiziGizle DB: " + e.getMessage());
                }
            });
        }
        System.out.println((gizle ? "🚫" : "✓") + " Dizi toplu: " + diziAdi);
    }
    private void tekKanalGizle(GizleItem kanalItem, boolean gizle) {
        degisiklikVar = true;
        kanalItem.setGizli(gizle);
        gizliKanallar.put(kanalItem.getUrl(), gizle);
        adapter.guncelle(focusPoz);
        if (!gizle) {
            String katKey = kanalItem.getM3uAdi() + "|" + kanalItem.getKategoriAdi();
            if (gizliKategoriler.contains(katKey)) {
                Set<String> m3uGizlenenler = m3uIleGizlenenKategoriler.get(kanalItem.getM3uAdi());
                gizliKategoriler.remove(katKey);
                if (m3uGizlenenler != null) m3uGizlenenler.remove(kanalItem.getKategoriAdi());
                List<GizleItem> katKanallar = kanalTumCache.get(katKey);
                if (katKanallar != null) {
                    for (GizleItem k : katKanallar) {
                        if (!k.getUrl().equals(kanalItem.getUrl())) {
                            k.setGizli(true);
                            gizliKanallar.put(k.getUrl(), true);
                        }
                    }
                }
                int katPoz = adapterPozBulKategoriByRaw(kanalItem.getKategoriAdi(), kanalItem.getM3uAdi());
                if (katPoz != -1) {
                    GizleItem katItem = adapter.getItem(katPoz);
                    if (katItem != null) {
                        katItem.setGizli(false);
                        adapter.guncelle(katPoz);
                    }
                }
                m3uGizlilikDurumuGuncelle(kanalItem.getM3uAdi());
            }
        }
        System.out.println((gizle ? "🚫" : "✓") + " Kanal: " + kanalItem.getAd());
    }
    public boolean handleBack() {
        System.out.println("🔙 GizlePaneli.handleBack çağrıldı focusPoz=" + focusPoz);
        GizleItem item = adapter.getItem(focusPoz);
        System.out.println("🔙 GizlePaneli item=" + (item == null ? "null" : "tip=" + item.getTip()));
        if (item == null) { kaydetOnayiGoster(); return true; }
        switch (item.getTip()) {
            case GizleItem.TIP_KANAL:    geriKategoriKapat(item); return true;
            case GizleItem.TIP_KATEGORI: geriM3UKapat(item);      return true;
            case GizleItem.TIP_M3U:      kaydetOnayiGoster();     return true;
            default:                     return false;
        }
    }
    private void kaydetOnayiGoster() {
        if (!degisiklikVar) {
            iptalVeKapat();
            return;
        }
        if (dialogAcik) return;
        dialogAcik = true;
        mainHandler.postDelayed(() ->
                        new androidx.appcompat.app.AlertDialog.Builder(activity)
                                .setMessage("Yaptığınız değişiklikleri kaydetmek istiyor musunuz?")
                                .setPositiveButton("Evet", (dialog, which) -> { dialogAcik = false; kaydetVeKapat(); })
                                .setNegativeButton("Hayır", (dialog, which) -> { dialogAcik = false; iptalVeKapat(); })
                                .setCancelable(false)
                                .show()
                , 300);
    }
    private void iptalVeKapat() {
        gizle();
        if (iptalCallback != null) iptalCallback.onIptal();
    }
    private void geriKategoriKapat(GizleItem kanalItem) {
        int katPoz = adapterPozBulKategoriByRaw(kanalItem.getKategoriAdi(), kanalItem.getM3uAdi());
        if (katPoz == -1) return;
        GizleItem katItem = adapter.getItem(katPoz);
        if (katItem == null) return;
        String key = katItem.getM3uAdi() + "|" + katItem.getRawAd();
        int n = altSayisiKategori(katPoz);
        acikKategoriler.remove(key);
        kanalYuklenmisSayisi.remove(key);
        katItem.setAcik(false);
        adapter.kaldir(katPoz + 1, n);
        adapter.guncelle(katPoz);
        focusVer(katPoz);
    }
    private void geriM3UKapat(GizleItem katItem) {
        int m3uPoz = adapterPozBulM3U(katItem.getM3uAdi());
        if (m3uPoz == -1) return;
        GizleItem m3uItem = adapter.getItem(m3uPoz);
        if (m3uItem == null) return;
        String m3uAdi = m3uItem.getAd();
        int n = altSayisiM3U(m3uAdi, m3uPoz);
        acikM3Ular.remove(m3uAdi);
        Iterator<String> ki = acikKategoriler.iterator();
        while (ki.hasNext()) { if (ki.next().startsWith(m3uAdi + "|")) ki.remove(); }
        Iterator<Map.Entry<String, Integer>> si = kanalYuklenmisSayisi.entrySet().iterator();
        while (si.hasNext()) { if (si.next().getKey().startsWith(m3uAdi + "|")) si.remove(); }
        m3uItem.setAcik(false);
        adapter.kaldir(m3uPoz + 1, n);
        adapter.guncelle(m3uPoz);
        focusVer(m3uPoz);
    }
    private void kaydetVeKapat() {
        executor.execute(() -> {
            mainHandler.post(() -> {
                SharedPreferences.Editor edKat =
                        activity.getSharedPreferences(PREF_GIZLI_KATEGORI, Context.MODE_PRIVATE).edit();
                edKat.clear();
                for (String key : gizliKategoriler) edKat.putBoolean(key, true);
                edKat.apply();
                SharedPreferences.Editor ed =
                        activity.getSharedPreferences(PREF_GIZLI, Context.MODE_PRIVATE).edit();
                ed.clear();
                for (Map.Entry<String, Boolean> e : gizliKanallar.entrySet()) {
                    if (Boolean.TRUE.equals(e.getValue())
                            && e.getKey() != null
                            && !e.getKey().startsWith("DIZI:")) {
                        ed.putBoolean(e.getKey(), true);
                    }
                }
                ed.apply();
                activity.getSharedPreferences("gizli_m3ular", Context.MODE_PRIVATE).edit().clear().apply();
                for (M3UItem m3u : tumM3UListesi) {
                    if (m3u.isHidden()) m3uManager.setM3UVisibility(m3u.getName(), true);
                }
                System.out.println("💾 GizlePaneli kaydedildi: " + gizliKategoriler.size() + " gizli kategori");
                gizle();
                if (kapandiCallback != null) kapandiCallback.onKapandi();
            });
        });
    }
    private int adapterPozBulM3U(String ad) {
        List<GizleItem> lst = adapter.getListe();
        for (int i = 0; i < lst.size(); i++) {
            GizleItem g = lst.get(i);
            if (g.getTip() == GizleItem.TIP_M3U && ad.equals(g.getAd())) return i;
        }
        return -1;
    }
    private int adapterPozBulKategori(String adGoster, String m3uAdi) {
        List<GizleItem> lst = adapter.getListe();
        for (int i = 0; i < lst.size(); i++) {
            GizleItem g = lst.get(i);
            if (g.getTip() == GizleItem.TIP_KATEGORI
                    && adGoster.equals(g.getAd())
                    && m3uAdi.equals(g.getM3uAdi())) return i;
        }
        return -1;
    }
    private int adapterPozBulKategoriByRaw(String rawAd, String m3uAdi) {
        List<GizleItem> lst = adapter.getListe();
        for (int i = 0; i < lst.size(); i++) {
            GizleItem g = lst.get(i);
            if (g.getTip() == GizleItem.TIP_KATEGORI
                    && rawAd.equals(g.getRawAd())
                    && m3uAdi.equals(g.getM3uAdi())) return i;
        }
        return -1;
    }
    private int altSayisiM3U(String m3uAdi, int m3uPoz) {
        List<GizleItem> lst = adapter.getListe();
        int sayi = 0;
        for (int i = m3uPoz + 1; i < lst.size(); i++) {
            if (lst.get(i).getTip() == GizleItem.TIP_M3U) break;
            sayi++;
        }
        return sayi;
    }
    private int altSayisiKategori(int katPoz) {
        List<GizleItem> lst = adapter.getListe();
        int sayi = 0;
        for (int i = katPoz + 1; i < lst.size(); i++) {
            if (lst.get(i).getTip() != GizleItem.TIP_KANAL) break;
            sayi++;
        }
        return sayi;
    }
    private void focusVer(int poz) {
        focusPoz = poz;
        recyclerView.postDelayed(() -> {
            recyclerView.scrollToPosition(poz);
            RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(poz);
            if (vh != null) {
                View btn = vh.itemView.findViewById(R.id.btnGizleM3U);
                if (btn == null) btn = vh.itemView.findViewById(R.id.btnGizleKategori);
                if (btn == null) btn = vh.itemView.findViewById(R.id.btnGizleKanal);
                if (btn != null) btn.requestFocus(); else vh.itemView.requestFocus();
            }
        }, 100);
    }
    public static Set<String> getGizliKanalUrlleri(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_GIZLI, Context.MODE_PRIVATE);
        Set<String> result = new HashSet<>();
        for (Map.Entry<String, ?> e : prefs.getAll().entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) result.add(e.getKey());
        }
        return result;
    }
    public static Set<String> getGizliKategoriler(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_GIZLI_KATEGORI, Context.MODE_PRIVATE);
        Set<String> result = new HashSet<>();
        for (Map.Entry<String, ?> e : prefs.getAll().entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) result.add(e.getKey());
        }
        return result;
    }
    private List<String> siralamaKaydiUygula(List<String> tumKat,
                                             Set<String> liveSet,
                                             Set<String> movieSet,
                                             Set<String> seriesSet) {
        try {
            android.content.SharedPreferences prefs =
                    activity.getSharedPreferences("kategori_siralama", Context.MODE_PRIVATE);
            Map<String, Integer> siraMap = new LinkedHashMap<>();
            
            
            
            for (int idx = 0; idx < tumKat.size(); idx++) {
                String kat = tumKat.get(idx);
                int grupOnceligı;
                if (liveSet.contains(kat))        grupOnceligı = 0;
                else if (movieSet.contains(kat))  grupOnceligı = 1;
                else if (seriesSet.contains(kat)) grupOnceligı = 2;
                else                              grupOnceligı = 3;
                siraMap.put(kat, grupOnceligı * 100000 + idx);
            }
            
            
            String[] turKodlari = {"LIVE", "MOVIE", "SERIES"};
            for (String turKodu : turKodlari) {
                String kayit = prefs.getString(turKodu + "_sirasi", null);
                if (kayit == null || kayit.isEmpty()) continue;
                String[] parcalar = kayit.split("###");
                
                int siraNo = 0;
                for (int i = 1; i < parcalar.length; i += 2) {
                    String katAdi = parcalar[i].trim();
                    if (!katAdi.isEmpty() && siraMap.containsKey(katAdi)) {
                        
                        if (siraNo < siraMap.get(katAdi)) {
                            siraMap.put(katAdi, siraNo);
                        }
                    }
                    siraNo++;
                }
            }
            List<String> sirali = new ArrayList<>(tumKat);
            Collections.sort(sirali, (a, b) -> {
                Integer siraA = siraMap.get(a);
                Integer siraB = siraMap.get(b);
                if (siraA == null) siraA = Integer.MAX_VALUE;
                if (siraB == null) siraB = Integer.MAX_VALUE;
                return Integer.compare(siraA, siraB);
            });
            return sirali;
        } catch (Exception e) {
            System.out.println("❌ siralamaKaydiUygula: " + e.getMessage());
            return tumKat;
        }
    }
    public Map<String, List<String>> getM3uKategoriOnbellegi() {
        return Collections.unmodifiableMap(m3uKategoriOnbellegi);
    }
    public void gizle() {
        if (panelView != null) { panelView.setVisibility(View.GONE); panelView.clearFocus(); }
        kanalTumCache.clear();
        kanalCache.clear();
        kanalYuklenmisSayisi.clear();
        acikM3Ular.clear();
        acikKategoriler.clear();
        m3uKategoriOnbellegi.clear();
        seriesKategoriler.clear();
        m3uIleGizlenenKategoriler.clear();
        gizliKanallar.clear();
        gizliKategoriler.clear();
        lazyYukleniyor = null;
        focusPoz = 0;
        System.out.println("🧹 GizlePaneli cache temizlendi");
    }
    public boolean isVisible() {
        return panelView != null && panelView.getVisibility() == View.VISIBLE;
    }
    public View getPanelView()            { return panelView; }
    public GizleItem getFocusItem() { return adapter.getItem(focusPoz); }
}