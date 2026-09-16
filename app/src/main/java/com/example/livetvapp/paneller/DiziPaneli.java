package com.example.livetvapp.paneller;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.livetvapp.Adapter.Sezon;
import com.example.livetvapp.Adapter.Dizi;
import com.example.livetvapp.Channel;
import com.example.livetvapp.R;
import com.example.livetvapp.database.ChannelRepository;
import com.example.livetvapp.Adapter.Bolum;
import com.example.livetvapp.Adapter.DiziAdapter;
import com.example.livetvapp.database.DiziCache;   
import com.example.livetvapp.database.DiziParser;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
public class DiziPaneli {
    private static final int PAGE_SIZE       = 30;
    private static final String PREF_GIZLI          = "gizli_kanallar";
    private static final String PREF_GIZLI_M3U      = "gizli_m3ular";
    private static final String PREF_GIZLI_KATEGORI = "gizli_kategoriler";
    private AppCompatActivity activity;
    private View         panelView;
    private RecyclerView recyclerView;
    private TextView     yukleniyorText;
    private DiziAdapter  adapter;
    private List<Dizi> tumDiziler      = new ArrayList<>();
    private List<Dizi> filtreliDiziler = new ArrayList<>();
    private int     currentPage   = 0;
    private boolean isLoading     = false;
    private String  aktifKategori = null;
    private boolean parseYapildi  = false;
    private String  parsedM3UName = null;
    private ChannelRepository channelRepository;
    private Handler           mainHandler;
    public DiziAdapter getAdapter() { return adapter; }
    public interface BolumSecimDinleyici { void onBolumSecildi(Dizi dizi, Sezon sezon, Bolum bolum); }
    private BolumSecimDinleyici bolumDinleyici;
    public void setBolumSecimDinleyici(BolumSecimDinleyici d) { this.bolumDinleyici = d; }
    public DiziPaneli(AppCompatActivity activity) {
        this.activity          = activity;
        this.channelRepository = new ChannelRepository(activity);
        this.mainHandler       = new Handler(Looper.getMainLooper());
    }
    public void olustur() {
        panelView      = LayoutInflater.from(activity).inflate(R.layout.panel_diziler, null);
        recyclerView   = panelView.findViewById(R.id.diziRecyclerView);
        yukleniyorText = panelView.findViewById(R.id.diziYukleniyorText);
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
        adapter = new DiziAdapter(
                (dizi, pos) -> System.out.println("📺 Dizi: " + dizi.getAd()),
                (dizi, sezon, bolum) -> {
                    if (bolumDinleyici != null) bolumDinleyici.onBolumSecildi(dizi, sezon, bolum);
                }
        );
        recyclerView.setAdapter(adapter);
        adapter.setOnDiziDetayIstekListener((dizi, pozisyon) -> {
            
            
            
            goster(); 
            if (yukleniyorText != null) {
                yukleniyorText.setText("Sezon bilgisi yükleniyor...");
                yukleniyorText.setVisibility(View.VISIBLE);
            }
            channelRepository.loadChannelByName(dizi.getAd(), new ChannelRepository.OnChannelLoadedListener() {
                public void onChannelLoaded(Channel channel) {
                    if (channel == null) {
                        mainHandler.post(() -> {
                            if (yukleniyorText != null)
                                yukleniyorText.setVisibility(View.GONE);
                            adapter.sezonlarYuklendi(dizi, pozisyon);
                        });
                        return;
                    }
                    com.example.livetvapp.database.XtreamCodesManager.loadSeriesInfo(
                            channel.getUrl(),
                            channel.getSourceName(),
                            activity,
                            new com.example.livetvapp.database.XtreamCodesManager.OnSeriesInfoLoadedListener() {
                                @Override
                                public void onSeriesInfoLoaded(List<Sezon> sezonlar) {
                                    dizi.setSezonlar(sezonlar);
                                    if (yukleniyorText != null)
                                        yukleniyorText.setVisibility(View.GONE);
                                    
                                    adapter.sezonlarYuklendi(dizi, pozisyon);
                                }
                                @Override
                                public void onError(String hata) {
                                    mainHandler.post(() -> {
                                        System.out.println("⚠️ get_series_info başarısız, regex moduna dönülüyor: " + hata);
                                        if (yukleniyorText != null)
                                            yukleniyorText.setVisibility(View.GONE);
                                        adapter.sezonlarYuklendi(dizi, pozisyon);
                                    });
                                }
                            }
                    );
                }
                @Override
                public void onError(Exception e) {}
            });
        });
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm != null && !isLoading) {
                    int visible = lm.getChildCount();
                    int total   = lm.getItemCount();
                    int first   = lm.findFirstVisibleItemPosition();
                    if ((visible + first) >= total - 5 && first >= 0
                            && total < filtreliDiziler.size()) {
                        loadMoreDiziler();
                    }
                }
            }
        });
        activity.addContentView(panelView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
        System.out.println("✅ DiziPaneli oluşturuldu");
    }
    private void guncelleBosGorunum(List<Dizi> diziler, boolean yukleniyor) {
        if (yukleniyor) {
            if (recyclerView   != null) recyclerView.setVisibility(View.GONE);
            if (yukleniyorText != null) {
                yukleniyorText.setText("Diziler yükleniyor...");
                yukleniyorText.setVisibility(View.VISIBLE);
            }
        } else if (diziler == null || diziler.isEmpty()) {
            if (recyclerView   != null) recyclerView.setVisibility(View.GONE);
            if (yukleniyorText != null) {
                yukleniyorText.setText("Bu kategoride dizi yok");
                yukleniyorText.setVisibility(View.VISIBLE);
            }
        } else {
            if (recyclerView   != null) recyclerView.setVisibility(View.VISIBLE);
            if (yukleniyorText != null) yukleniyorText.setVisibility(View.GONE);
        }
    }
    public void switchToSeriesMode(String m3uName, String kategori) {
        System.out.println("=== DiziPaneli: SERIES MODUNA GEÇİLİYOR === kategori=" + kategori);
        if (kategori == null || kategori.isEmpty()) {
            adapter.setDiziler(new ArrayList<>());
            guncelleBosGorunum(null, false);
            goster();
            return;
        }
        List<Dizi> cachedDiziler = DiziCache.getInstance().get(kategori);
        if (cachedDiziler != null) {
            tumDiziler      = cachedDiziler;
            filtreliDiziler = new ArrayList<>(cachedDiziler);
            currentPage     = 0;
            adapter.setDiziler(getPagedDiziler(0, PAGE_SIZE));
            guncelleBosGorunum(filtreliDiziler, false);
            if (recyclerView != null) recyclerView.scrollToPosition(0);
            aktifKategori = kategori;
            goster();
            return;
        }
        adapter.setDiziler(new ArrayList<>());
        guncelleBosGorunum(null, true);
        goster();
        final String finalKategori = kategori;
        final String finalM3uName  = m3uName;
        channelRepository.loadChannelsByM3UAndCategory(m3uName, kategori,
                new ChannelRepository.OnChannelsLoadedListener() {
                    @Override
                    public void onChannelsLoaded(List<Channel> channels, boolean fromCache) {
                        System.out.println("📦 Kategori [" + finalKategori + "] kanallar geldi: "
                                + channels.size());
                        new Thread(() -> {
                            List<Channel> filtrelenmis = gizliKanallariFiltreele(
                                    channels, finalM3uName, finalKategori);
                            System.out.println("🚫 Dizi gizli filtre: " + channels.size()
                                    + " → " + filtrelenmis.size());
                            List<Dizi> diziler = DiziParser.parseWithCache(finalKategori, filtrelenmis);
                            mainHandler.post(() -> {
                                tumDiziler      = diziler;
                                filtreliDiziler = new ArrayList<>(diziler);
                                aktifKategori   = finalKategori;
                                currentPage     = 0;
                                if (!isVisible()) {
                                    System.out.println("⚠️ DiziPaneli gizli, parse sonucu uygulanmadı: " + finalKategori);
                                    return;
                                }
                                List<Dizi> ilkSayfa = getPagedDiziler(0, PAGE_SIZE);
                                adapter.setDiziler(ilkSayfa);
                                guncelleBosGorunum(filtreliDiziler, false);
                                if (recyclerView != null) recyclerView.scrollToPosition(0);
                                System.out.println("✅ Kategori [" + finalKategori
                                        + "] parse tamamlandı: " + diziler.size() + " dizi");
                            });
                        }).start();
                    }
                    @Override
                    public void onError(Exception e) {
                        mainHandler.post(() -> {
                            System.out.println("❌ Dizi yükleme hatası: " + e.getMessage());
                            guncelleBosGorunum(null, false);
                        });
                    }
                });
    }
    private List<Channel> gizliKanallariFiltreele(List<Channel> kanallar,
                                                  String m3uName,
                                                  String kategori) {
        if (kanallar == null || kanallar.isEmpty()) return new ArrayList<>();
        Set<String> gizliUrller      = getGizliKanalUrlleri();
        Set<String> gizliM3Ular      = getGizliM3UAdilar();
        Set<String> gizliKategoriler = getGizliKategoriler();
        boolean kategoriGizliMi = false;
        if (kategori != null) {
            for (String key : gizliKategoriler) {
                int boru = key.indexOf('|');
                if (boru < 0) continue;
                String keyM3u = key.substring(0, boru);
                String keyKat = key.substring(boru + 1);
                boolean katEslesti = keyKat.equals(kategori)
                        || keyKat.startsWith(kategori + " (")
                        || keyKat.startsWith(kategori + "(");
                if (!katEslesti) continue;
                if (m3uName != null && !m3uName.isEmpty()) {
                    if (keyM3u.equals(m3uName)) { kategoriGizliMi = true; break; }
                } else {
                    kategoriGizliMi = true; break;
                }
            }
        }
        if (kategoriGizliMi) {
            System.out.println("🚫 Kategori gizli, tüm kanallar filtrelendi: " + kategori);
            return new ArrayList<>();
        }
        if (gizliUrller.isEmpty() && gizliM3Ular.isEmpty()) return kanallar;
        List<Channel> sonuc = new ArrayList<>(kanallar);
        Iterator<Channel> iter = sonuc.iterator();
        while (iter.hasNext()) {
            Channel ch = iter.next();
            if (gizliUrller.contains(ch.getUrl())
                    || gizliM3Ular.contains(ch.getSourceName())) {
                iter.remove();
            }
        }
        return sonuc;
    }
    private Set<String> getGizliKanalUrlleri() {
        SharedPreferences prefs = activity.getSharedPreferences(PREF_GIZLI, Context.MODE_PRIVATE);
        Set<String> result = new HashSet<>();
        for (Map.Entry<String, ?> e : prefs.getAll().entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) result.add(e.getKey());
        }
        return result;
    }
    private Set<String> getGizliM3UAdilar() {
        SharedPreferences prefs = activity.getSharedPreferences(PREF_GIZLI_M3U, Context.MODE_PRIVATE);
        Set<String> result = new HashSet<>();
        for (Map.Entry<String, ?> e : prefs.getAll().entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) result.add(e.getKey());
        }
        return result;
    }
    private Set<String> getGizliKategoriler() {
        SharedPreferences prefs = activity.getSharedPreferences(PREF_GIZLI_KATEGORI, Context.MODE_PRIVATE);
        Set<String> result = new HashSet<>();
        for (Map.Entry<String, ?> e : prefs.getAll().entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) result.add(e.getKey());
        }
        return result;
    }
    public void m3uDegisti() {
        parseYapildi  = false;
        parsedM3UName = null;
        tumDiziler.clear();
        filtreliDiziler.clear();
        DiziCache.getInstance().invalidateAll();
        System.out.println("🗑️ DiziPaneli tüm cache temizlendi");
    }
    public void switchToNormalMode() {
        System.out.println("=== DiziPaneli: NORMAL MODA GEÇİLİYOR ===");
        filtreliDiziler.clear();
        if (adapter != null) adapter.setDiziler(new ArrayList<>());
        setMarginStart(0);
        gizle();
    }
    public void kategoriFiltrele(String kategori) {
        System.out.println("🔍 DiziPaneli.kategoriFiltrele: " + kategori);
        if (com.example.livetvapp.database.FavorilerYoneticisi.FAVORI_KATEGORI_ADI.equals(kategori)) {
            com.example.livetvapp.database.FavorilerYoneticisi fy =
                    com.example.livetvapp.database.FavorilerYoneticisi.getInstance(activity);
            switchToFavoriModu(fy.getHepsi(com.example.livetvapp.paneller.Icerikpaneli.TIP_DIZI));
            return;
        }
        switchToSeriesMode(null, kategori);
    }
    private void loadMoreDiziler() {
        if (isLoading) return;
        isLoading = true;
        currentPage++;
        List<Dizi> more = getPagedDiziler(currentPage * PAGE_SIZE, PAGE_SIZE);
        mainHandler.post(() -> {
            if (!more.isEmpty()) adapter.addDiziler(more);
            isLoading = false;
        });
    }
    private List<Dizi> getPagedDiziler(int offset, int limit) {
        List<Dizi> result = new ArrayList<>();
        int start = Math.min(offset, filtreliDiziler.size());
        int end   = Math.min(offset + limit, filtreliDiziler.size());
        if (start < end) result.addAll(filtreliDiziler.subList(start, end));
        return result;
    }
    public void setMarginStart(int marginDp) {
        int marginPx = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, marginDp,
                activity.getResources().getDisplayMetrics()));
        if (panelView != null) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) panelView.getLayoutParams();
            if (params != null) {
                params.leftMargin = marginPx;
                panelView.setLayoutParams(params);
            }
        }
        System.out.println("📐 DiziPaneli marginStart: " + marginDp + "dp");
    }
    public void goster() {
        if (panelView != null) panelView.setVisibility(View.VISIBLE);
    }
    public void gizle() {
        if (panelView != null) {
            panelView.setVisibility(View.GONE);
            panelView.clearFocus();
        }
    }
    public boolean isVisible() {
        return panelView != null && panelView.getVisibility() == View.VISIBLE;
    }
    public View getPanelView()            { return panelView; }
    public RecyclerView getRecyclerView() { return recyclerView; }
    public String getAktifKategori()      { return aktifKategori; }
    public void switchToFavoriModu(List<String> favoriDiziAdlari) {
        System.out.println("⭐ DiziPaneli.switchToFavoriModu: " + favoriDiziAdlari.size() + " dizi");

        if (favoriDiziAdlari == null || favoriDiziAdlari.isEmpty()) {
            filtreliDiziler = new ArrayList<>();
            if (adapter != null) adapter.setDiziler(new ArrayList<>());
            guncelleBosGorunum(null, false);
            goster();
            return;
        }

        
        String CACHE_KEY_FAVORITES = "FAVORITES_SERIES";
        List<Dizi> cachedFavoriler = DiziCache.getInstance().get(CACHE_KEY_FAVORITES);
        if (cachedFavoriler != null) {
            
            List<Dizi> filtrelenmisFavoriler = new ArrayList<>();
            for (String favoriAd : favoriDiziAdlari) {
                for (Dizi dizi : cachedFavoriler) {
                    if (favoriAd.equals(dizi.getAd())) {
                        filtrelenmisFavoriler.add(dizi);
                        break;
                    }
                }
            }
            filtreliDiziler = filtrelenmisFavoriler;
            if (adapter != null) adapter.setDiziler(filtrelenmisFavoriler);
            guncelleBosGorunum(filtrelenmisFavoriler, false);
            if (recyclerView != null) recyclerView.scrollToPosition(0);
            goster();
            System.out.println("⭐ Favori cache HIT: " + filtrelenmisFavoriler.size() + " dizi");
            return;
        }

        
        guncelleBosGorunum(null, true);
        goster();

        channelRepository.loadAllChannelsByType(null, "SERIES",
                new ChannelRepository.OnChannelsLoadedListener() {
                    @Override
                    public void onChannelsLoaded(List<Channel> channels, boolean fromCache) {
                        new Thread(() -> {
                            try {
                                
                                List<Channel> filtrelenmisKanallar = gizliKanallariFiltreele(channels, null, null);
                                
                                List<Dizi> tumDizilerParsed = DiziParser.parseM3UForDiziler(filtrelenmisKanallar);

                                
                                List<Dizi> favoriDiziler = new ArrayList<>();
                                for (String favoriAd : favoriDiziAdlari) {
                                    for (Dizi dizi : tumDizilerParsed) {
                                        if (favoriAd.equals(dizi.getAd())) {
                                            favoriDiziler.add(dizi);
                                            break;
                                        }
                                    }
                                }

                                
                                DiziCache.getInstance().put(CACHE_KEY_FAVORITES, tumDizilerParsed);

                                mainHandler.post(() -> {
                                    filtreliDiziler = favoriDiziler;
                                    if (adapter != null) adapter.setDiziler(favoriDiziler);
                                    guncelleBosGorunum(favoriDiziler, false);
                                    if (recyclerView != null) recyclerView.scrollToPosition(0);
                                    System.out.println("⭐ Favori DB'den yüklendi: " + favoriDiziler.size() + " dizi");
                                });
                            } catch (Exception e) {
                                mainHandler.post(() -> {
                                    System.out.println("❌ Favori yükleme hatası: " + e.getMessage());
                                    guncelleBosGorunum(null, false);
                                });
                            }
                        }).start();
                    }

                    @Override
                    public void onError(Exception e) {
                        mainHandler.post(() -> {
                            System.out.println("❌ Favori kanal yükleme hatası: " + e.getMessage());
                            guncelleBosGorunum(null, false);
                        });
                    }
                });
    }
}