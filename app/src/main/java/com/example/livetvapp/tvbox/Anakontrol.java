package com.example.livetvapp.tvbox;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.livetvapp.Channel;
import com.example.livetvapp.KanalListesi;
import com.example.livetvapp.MainActivity;
import com.example.livetvapp.R;
import com.example.livetvapp.database.AppDatabase;
import com.example.livetvapp.database.DiziCache;
import com.example.livetvapp.database.IzlemePozisyonu;
import com.example.livetvapp.database.KategoriCache;
import com.example.livetvapp.database.KategoriItem;
import com.example.livetvapp.database.M3UManager;
import com.example.livetvapp.database.M3UItem;
import com.example.livetvapp.database.M3UParser;
import com.example.livetvapp.database.ChannelRepository;
import com.example.livetvapp.metadata.metadataPanel;
import com.example.livetvapp.other.AspectRatioManager;
import com.example.livetvapp.other.StreamMonitor;
import com.example.livetvapp.paneller.AramaPaneli;
import com.example.livetvapp.paneller.AyarlarPaneli;
import com.example.livetvapp.paneller.DiziPaneli;
import com.example.livetvapp.paneller.GizlePaneli;
import com.example.livetvapp.paneller.Icerikpaneli;
import com.example.livetvapp.paneller.KategoriPaneli;
import com.example.livetvapp.paneller.SiralamaPaneli;
import com.example.livetvapp.paneller.normalkanallistesipaneli;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;
import com.example.livetvapp.paneller.bar;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.example.livetvapp.database.FavorilerYoneticisi;
import com.example.livetvapp.paneller.KisayolKutusu;
import android.view.Gravity;
import com.example.livetvapp.stalker.StalkerApiClient;
import com.example.livetvapp.stalker.StalkerPortal;
import com.example.livetvapp.stalker.StalkerPortalDao;
import com.example.livetvapp.stalker.StalkerTokenCache;
public class Anakontrol implements StreamMonitor.StreamMonitorListener {
    private KisayolKutusu kisayolKutusu;
    private AramaPaneli aramaPaneli;
    private bar oynaticiBar;
    private static final String PREFS_NAME          = "AppSettings";
    private static final String KEY_LAST_PLAYED_URL = "last_played_url";
    private static final int MARGIN_ICERIK_VE_KATEGORI = 340;
    private static final int MARGIN_YALNIZ_KATEGORI    = 170;
    private static final int MARGIN_YALNIZ_KANAL = 200;
    private static final int MARGIN_YALNIZ_DIZI  = 280;
    private AppCompatActivity activity;
    private LibVLC libVLC;
    private MediaPlayer mediaPlayer;
    private VLCVideoLayout videoLayout;
    private Dpad dpad;
    private Dokunmatik dokunmatik;
    private StreamMonitor      streamMonitor;
    private AspectRatioManager aspectRatioManager;
    private normalkanallistesipaneli kanalPaneli;
    private AyarlarPaneli            ayarlarPaneli;
    private KategoriPaneli           kategoriPaneli;
    private Icerikpaneli             icerikPaneli;
    private DiziPaneli               diziPaneli;
    private metadataPanel metadataPanel;
    private List<Channel> tumKanallar;
    private List<Channel> filtreliKanallar;
    private String aktifKategori = null;
    private Media         currentMedia = null;
    private M3UManager    m3uManager;
    private ChannelRepository channelRepository;
    private Handler           mainHandler;
    private String  aktifIcerikTipi        = Icerikpaneli.TIP_CANLI_TV;
    private String  aktifOynaticSourceName = null;
    private String  aktifStreamUrl         = null;
    private String  aktifKanalAdi          = "";
    private String  aktifDiziAdi           = "";
    private String  aktifDbUrl             = null;
    private boolean stalkerCozumdenGeliyor = false;
    private String sonDiziUrl = null;
    private long bekleyenBaslangicPozisyonu = 0;
    private String sonFilmUrl = null;
    private boolean ilkAcilisOtomatikOynat = false;
    private boolean kanalFavoridenAcildi = false;
    public boolean isKanalFavoridenAcildi() { return kanalFavoridenAcildi; }
    private boolean kullaniciDurdurdu = false;
    public void setKullaniciDurdurdu(boolean deger) { this.kullaniciDurdurdu = deger; }

    public boolean isVideoPaused() {
        return mediaPlayer != null && kullaniciDurdurdu && aktifStreamUrl != null;
    }

    public void resetUiForRemoteControl() {
        if (!dpadModu) {
            dpadModu = true;
            if (dokunmatik != null) dokunmatik.pasifYap();
            if (dpad != null) dpad.aktifYap();
            if (sanalDpad != null) sanalDpad.gizle();
            kutuGorunurlugunuGuncelle();
        }
    }
    public boolean isBarVisible() {
        return oynaticiBar != null && oynaticiBar.isVisible();
    }
    public bar getBar() {
        return oynaticiBar;
    }
    public boolean isVideoAvailable() {
        return aktifStreamUrl != null && mediaPlayer != null;
    }

    public void resumeVideo() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying() && aktifStreamUrl != null) {
            mediaPlayer.play();
            if (oynaticiBar != null) {
                oynaticiBar.updatePlayPauseButton(true);
            }
        }
    }
    private boolean m3uZorunluAcilisModu   = false;
    private boolean dpadModu               = false;
    private SanalDpad sanalDpad;
    private Runnable zorunluAcilisRunnable = null;
    private Runnable bekleyenKategoriYukle    = null;
    private Runnable bekleyenDiziKategoriYukle = null;
    private static final int KATEGORI_DEBOUNCE_MS = 300;
    private Runnable otomatikKayitRunnable = null;
    private Runnable ayarlarBekciRunnable     = null;
    private static final int BEKCI_KONTROL_MS = 250;
    private static final int BEKCI_SURE_MS    = 60_000;
    private static final int OTOMATIK_KAYIT_ARALIK_MS = 10_000;
    private static final int STREAM_TIMEOUT_MS    = 15000;
    private Runnable         streamTimeoutRunnable = null;
    private boolean          streamOynuyor         = false;
    private android.widget.FrameLayout streamHataLayout    = null;
    private android.widget.FrameLayout yuklemOverlayLayout = null;
    private MediaPlayer.EventListener  vlcEventListener    = null;
    private FavorilerYoneticisi favorilerYoneticisi;

    private void logTipDegisimi(String nereden, String eskiTip, String yeniTip) {
        System.out.println("🔺🔺🔺 [AKTİF TİP DEĞİŞİYOR] " + nereden);
        System.out.println("    Eski: " + eskiTip + "  →  Yeni: " + yeniTip);
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (int i = 3; i < Math.min(stack.length, 9); i++) {
            System.out.println("    [" + i + "] " + stack[i]);
        }
        System.out.println("🔺🔺🔺");
    }

    public Anakontrol(AppCompatActivity activity) {
        this.activity          = activity;
        this.m3uManager        = new M3UManager(activity);
        this.channelRepository = new ChannelRepository(activity);
        this.mainHandler       = new Handler(Looper.getMainLooper());
        this.streamMonitor     = new StreamMonitor(this);
    }

    public void kutuMargininiBarUstuYap() {
        if (kisayolKutusu == null || oynaticiBar == null) return;
        View barView = oynaticiBar.getPanelView();

        barView.post(() -> {
            barView.postDelayed(() -> {
                int barHeight = barView.getHeight();
                if (barHeight > 0) {
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) kisayolKutusu.getLayoutParams();
                    lp.bottomMargin = barHeight + dpToPx(8);
                    kisayolKutusu.setLayoutParams(lp);
                    System.out.println("📏 Kutu margin güncellendi: bottomMargin=" + lp.bottomMargin + "px (bar yüksekliği=" + barHeight + "px)");
                }
            }, 50);
        });
    }
    public void baslat() {
        int orientation = activity.getResources().getConfiguration().orientation;
        activity.setContentView(
                orientation == Configuration.ORIENTATION_LANDSCAPE
                        ? R.layout.activity_main_land
                        : R.layout.activity_main);
        videoLayout = activity.findViewById(R.id.videoLayout);
        if (videoLayout == null) return;
        ArrayList<String> options = new ArrayList<>();
        options.add("--network-caching=1500");
        options.add("--live-caching=1000");
        options.add("--clock-jitter=0");
        options.add("--clock-synchro=0");
        options.add("--codec=mediacodec,iomx,any");
        options.add("--fullscreen");
        options.add("--vout=android-opaque");
        options.add("--android-display-chroma=RV32");
        libVLC = new LibVLC(activity, options);
        mediaPlayer = new MediaPlayer(libVLC);
        vlcEventDinleyicisiBaslat();
        videoLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (right > 0 && bottom > 0) {
                    videoLayout.removeOnLayoutChangeListener(this);
                    mediaPlayer.attachViews(videoLayout, null, false, false);
                }
            }
        });
        aspectRatioManager = new AspectRatioManager(activity, mediaPlayer, videoLayout);
        oynaticiBar = new bar(activity, mediaPlayer);
        oynaticiBar.setStreamMonitor(streamMonitor);
        oynaticiBar.olustur();
        oynaticiBar.setAspectRatioManager(aspectRatioManager);
        icerikPaneli = new Icerikpaneli(activity);
        icerikPaneli.setIcerikSecimDinleyici(icerikTipi -> {
            System.out.println("🟡 [ANA] IcerikPaneli callback: " + icerikTipi);
            icerikTipiDegisti(icerikTipi);
        });
        icerikPaneli.olustur();
        icerikPaneli.setAyarlarButonuDinleyici(new Icerikpaneli.AyarlarButonuDinleyici() {
            public void onAyarlarAcildi() {
                sanalDpadDurumGuncelle();
                if (bekleyenKategoriYukle != null) {
                    mainHandler.removeCallbacks(bekleyenKategoriYukle);
                    bekleyenKategoriYukle = null;
                }
                if (bekleyenDiziKategoriYukle != null) {
                    mainHandler.removeCallbacks(bekleyenDiziKategoriYukle);
                    bekleyenDiziKategoriYukle = null;
                }
                if (kategoriPaneli != null) kategoriPaneli.gizle();
                if (kanalPaneli != null) kanalPaneli.gizle();
                if (diziPaneli != null) diziPaneli.gizle();
                if (dpad != null) dpad.ayarlarPaneliAc();
                ayarlarBekciBaslat();
            }

            @Override
            public void onAyarlarFocus(boolean hasFocus) {
                if (hasFocus) {
                    if (kategoriPaneli != null) kategoriPaneli.gizle();
                    if (kanalPaneli != null) kanalPaneli.gizle();
                    if (diziPaneli != null) diziPaneli.gizle();
                } else {
                    if (kategoriPaneli != null) kategoriPaneli.goster();
                    if (Icerikpaneli.TIP_DIZI.equals(aktifIcerikTipi)) {
                        if (diziPaneli != null) diziPaneli.goster();
                    } else {
                        if (kanalPaneli != null) kanalPaneli.goster();
                    }
                }
            }
        });
        kategoriPaneli = new KategoriPaneli(activity);
        kategoriPaneli.setKategoriSecimDinleyici(kategori -> {
            System.out.println("📂 Kategori seçildi: " + kategori);
            if (ayarlarPaneli != null && ayarlarPaneli.isVisible()) return;
            if (Icerikpaneli.TIP_DIZI.equals(aktifIcerikTipi)) {
                if (diziPaneli != null) {
                    if (!FavorilerYoneticisi.FAVORI_KATEGORI_ADI.equals(kategori)) {
                        aktifKategori = kategori;
                    }
                    diziKategoriDebounceIleYukle(kategori);
                }
            } else {
                kategoriDebounceIleYukle(kategori);
            }
        });
        kategoriPaneli.olustur(new ArrayList<>());
        kategoriPaneli.setSayfaYukleyici(new KategoriPaneli.KategoriSayfaYukleyici() {
            @Override
            public void sonrakiSayfayiYukle(String icerikTipi, int offset) {
                channelRepository.loadCategoriesWithSourcePaged(null, icerikTipi, offset,
                        new ChannelRepository.OnKategoriItemsLoadedListener() {
                            @Override
                            public void onKategoriItemsLoaded(List<KategoriItem> items) {
                                gizliKategorileriFiltreeleAsync(items, filtrelenmis ->
                                        kategoriPaneli.sayfaEkle(filtrelenmis));
                            }

                            @Override
                            public void onError(Exception e) {
                                mainHandler.post(() -> kategoriPaneli.sayfaEkle(new ArrayList<>()));
                            }
                        });
            }

            @Override
            public void oncekiSayfayiYukle(String icerikTipi, int offset) {

            }
        });
        kanalPaneli = new normalkanallistesipaneli(activity);
        kanalPaneli.setKanalSecimDinleyici(kanal -> {
            if (kanal == null) return;
            if (Icerikpaneli.TIP_FILM.equals(kanal.getContentType())) {
                if (dpad != null) dpad.kanalPaneliKapat();
                filmOynatPozisyonKontrollu(kanal);
            } else {
                oynatKanal(kanal);
            }
            if (kategoriPaneli != null) {
                if (FavorilerYoneticisi.FAVORI_KATEGORI_ADI.equals(aktifKategori)) {
                    kategoriPaneli.setAktifKategori(FavorilerYoneticisi.FAVORI_KATEGORI_ADI);
                    kanalFavoridenAcildi = true;
                } else if (kanal.getCategory() != null) {
                    kategoriPaneli.setAktifKategori(kanal.getCategory());
                    kanalFavoridenAcildi = false;
                }
            }
        });
        kanalPaneli.olustur(new ArrayList<>());
        favorilerYoneticisi = FavorilerYoneticisi.getInstance(activity);
        if (kanalPaneli.getAdapter() != null) {
            kanalPaneli.getAdapter().setFavorilerYoneticisi(
                    favorilerYoneticisi, aktifIcerikTipi);
        }
        kanalPaneli.getAdapter().setOnFavoriDegistiListener((icerikTipi, eklendi) -> {
            if (!eklendi && favorilerYoneticisi.bos(icerikTipi)) {
                kanalFavoridenAcildi = false;
                if (aktifStreamUrl != null) {
                    channelRepository.loadChannelByUrl(aktifStreamUrl,
                            new ChannelRepository.OnChannelLoadedListener() {
                                @Override
                                public void onChannelLoaded(Channel channel) {
                                    mainHandler.post(() -> {
                                        if (channel != null) {
                                            aktifKategori = channel.getCategory();
                                            kategoriPaneli.setAktifKategori(aktifKategori);
                                        }
                                    });
                                }

                                @Override
                                public void onError(Exception e) {
                                }
                            });
                }
            }
        });
        diziPaneli = new DiziPaneli(activity);
        diziPaneli.olustur();
        if (diziPaneli.getAdapter() != null) {
            diziPaneli.getAdapter().setOnFocusDegistiListener(diziAdi -> {
                if (metadataPanel != null && dpad != null
                        && dpad.getAktifPanel() == Dpad.AktifPanel.SADECE_DIZI
                        && !dpad.isPanelYeniAcildi()) {
                    metadataPanel.focusDegisti(diziAdi, Icerikpaneli.TIP_DIZI);
                }
            });
        }
        diziPaneli.setBolumSecimDinleyici((dizi, sezon, bolum) -> {
            if (bolum == null) return;
            String barMetni = dizi.getAd()
                    + " · S" + String.format("%02d", sezon.getSezonNo())
                    + " E" + String.format("%02d", bolum.getBolumNo());
            System.out.println("▶️ Bölüm oynatılıyor: " + barMetni);
            if (dpad != null) dpad.setAktifIcerikTipi(Icerikpaneli.TIP_DIZI);
            if (oynaticiBar != null) oynaticiBar.setAktifIcerikTipi(Icerikpaneli.TIP_DIZI);
            if (oynaticiBar != null) oynaticiBar.setIcerikAdi(barMetni);
            logTipDegisimi("BolumSecimDinleyici", aktifIcerikTipi, Icerikpaneli.TIP_DIZI);
            aktifIcerikTipi = Icerikpaneli.TIP_DIZI;
            aktifDiziAdi = dizi.getAd() != null ? dizi.getAd() : "";
            kanalFavoridenAcildi = FavorilerYoneticisi.FAVORI_KATEGORI_ADI.equals(aktifKategori);
            bolumOynatPozisyonKontrollu(dizi, sezon, bolum, barMetni);
        });
        ayarlarPaneli = new AyarlarPaneli(activity);
        ayarlarPaneli.olustur();
        ayarlarPaneli.setYuklemeListener(new AyarlarPaneli.YuklemeListener() {
            @Override
            public void yuklemeBasladi(String m3uAdi) {
                mainHandler.post(() -> {
                    if (dokunmatik != null) dokunmatik.pasifYap();
                    if (dpad != null) dpad.pasifYap();
                    View overlay = activity.findViewById(R.id.yuklemeOverlay);
                    android.widget.TextView altText = activity.findViewById(R.id.yuklemeAlt);
                    android.widget.ProgressBar pb = activity.findViewById(R.id.yuklemeProgressBar);
                    if (overlay != null) overlay.setVisibility(View.VISIBLE);
                    if (altText != null) altText.setText(m3uAdi);
                    if (pb != null) {
                        pb.setIndeterminate(true);
                    }
                });
            }

            @Override
            public void yuklemeBitti() {
                mainHandler.post(() -> {
                    View overlay = activity.findViewById(R.id.yuklemeOverlay);
                    if (overlay != null) overlay.setVisibility(View.GONE);
                    if (dpadModu) {
                        if (dpad != null) dpad.aktifYap();
                        kutuGorunurlugunuGuncelle();
                    } else {
                        if (dokunmatik != null) dokunmatik.aktifYap();
                    }
                });
            }
        });
        ayarlarPaneli.setDpadModuSupplier(() -> dpadModu);
        ayarlarPaneli.setM3UYuklemeListener(dosyaYolu -> yeniM3UYukle(dosyaYolu));
        ayarlarPaneli.setM3USilmeListener(() -> {
            System.out.println("🗑️ [ANA] M3U silindi, paneller güncelleniyor");
            SiralamaPaneli sp = ayarlarPaneli.getSiralamaPaneli();
            if (sp != null) sp.veriGecersizKil();
            m3uSilindiktenSonraGuncelle();
        });
        ayarlarPaneli.setM3UGuncellendiListener(() -> {
            System.out.println("🔄 [ANA] M3U güncellendi → liste yenileniyor");

            KategoriCache.getInstance().invalidateAll();
            DiziCache.getInstance().invalidateAll();
            diziPaneli.m3uDegisti();
            mainHandler.post(() -> {
                if (aktifKategori != null && !aktifKategori.isEmpty()) {
                    kategoriFiltrele(aktifKategori);
                } else {
                    loadDefaultChannels();
                }
            });
        });
        ayarlarPaneli.setGizleKapandiListener(m3uKategoriHaritasi -> {
            if (!dpadModu && dokunmatik != null) dokunmatik.aktifYap();
            else if (dpadModu && dpad != null) dpad.aktifYap();
            System.out.println("🔒 [ANA] GizlePaneli kapandı → KategoriCache invalidate + sadece liste yenile");
            KategoriCache.getInstance().invalidateAll();
            DiziCache.getInstance().invalidateAll();
            SiralamaPaneli spGizle = ayarlarPaneli.getSiralamaPaneli();
            if (spGizle != null) spGizle.veriGecersizKil();
            gizleSonrasiListeyiYenile();
        });
        ayarlarPaneli.setPanelKapandiListener(() -> {
            ayarlarBekciDurdur();
            if (m3uZorunluAcilisModu) {
                List<M3UItem> kontrolListesi = m3uManager.getAllM3UItems();
                if (kontrolListesi == null || kontrolListesi.isEmpty()) {
                    zorunluAcilisDongusuPlanla();
                } else {
                    zorunluAcilisDongusuIptal();
                }
            }
            ayarlarPaneli.setSifirlamaListener(() -> {
                diziPaneli.m3uDegisti();
                favorilerYoneticisi = FavorilerYoneticisi.getInstance(activity);
                if (kanalPaneli.getAdapter() != null)
                    kanalPaneli.getAdapter().setFavorilerYoneticisi(favorilerYoneticisi, aktifIcerikTipi);
                if (diziPaneli.getAdapter() != null)
                    diziPaneli.getAdapter().setFavorilerYoneticisi(favorilerYoneticisi);
            });
        });
        ayarlarPaneli.setOnMenuChangedListener(menu -> {
            if (dpad != null) {

                if (menu == AyarlarPaneli.MenuSeviyesi.GIZLE_PANELI) {
                    dpad.aktifPanelAyarla(Dpad.AktifPanel.AYARLAR_GIZLE);
                    dpad.guncelleKisayol();
                }

                else if (menu == AyarlarPaneli.MenuSeviyesi.SIRALAMA_PANELI) {
                    dpad.aktifPanelAyarla(Dpad.AktifPanel.AYARLAR_SIRALAMA);
                    dpad.guncelleKisayol();
                }

                else {
                    Dpad.AktifPanel aktif = dpad.getAktifPanel();
                    if ((aktif == Dpad.AktifPanel.AYARLAR_GIZLE || aktif == Dpad.AktifPanel.AYARLAR_SIRALAMA)
                            && menu == AyarlarPaneli.MenuSeviyesi.ANA_MENU) {
                        dpad.aktifPanelAyarla(Dpad.AktifPanel.AYARLAR);
                    }
                    dpad.guncelleKisayol();
                }
            }
        });

        SiralamaPaneli spKategori = ayarlarPaneli.getSiralamaPaneli();
        if (spKategori != null) {
            spKategori.setKategoriGuncelleCallback((turKodu, yeniSira) -> {
                if (turKodu.equals(aktifIcerikTipi)) {
                    List<KategoriItem> yeniItems = new ArrayList<>();
                    for (String ad : yeniSira) {
                        yeniItems.add(new KategoriItem(ad, ""));
                    }
                    kategoriPaneli.kategorileriGuncelle(yeniItems);
                    System.out.println("✅ [ANA] KategoriPaneli anlik guncellendi [" + turKodu + "]: " + yeniItems.size());
                }
            });
        }
        dpad = new Dpad(activity, kanalPaneli, ayarlarPaneli, kategoriPaneli, icerikPaneli, diziPaneli);
        dpad.setAnakontrol(this);
        aramaPaneli = new AramaPaneli(activity);

        if (activity instanceof MainActivity) {
            MainActivity ma = (MainActivity) activity;
            aramaPaneli.setRemoteKontrolSupplier(() -> ma.isRemoteClientConnected());
        }
        aramaPaneli.olustur();
        aramaPaneli.setDpadModuSupplier(() -> dpadModu);
        aramaPaneli.setOnKanalSecimListener(kanal -> {
            aramaPaneli.gizle();
            if (dpad != null) dpad.panelDurumuSifirla();
            if (Icerikpaneli.TIP_CANLI_TV.equals(kanal.getContentType())) {
                oynatKanal(kanal);
            } else if (Icerikpaneli.TIP_FILM.equals(kanal.getContentType())) {
                filmOynatPozisyonKontrollu(kanal);
            } else {
                aramaKanalPaneliAc(kanal);
            }
        });
        aramaPaneli.setOnDiziSecimListener((diziAdi, temsilKanal) -> {
            aramaPaneli.gizle();
            if (dpad != null) dpad.panelDurumuSifirla();
            aramaDiziPaneliAc(diziAdi, temsilKanal);
        });
        aramaPaneli.setOnKapatCallback(() -> {
            if (dpad != null) dpad.panelDurumuSifirla();
        });
        aramaPaneli.setOnAramaIstekListener((sorgu, contentType, offset) -> {
            System.out.println("🔎 [ARAMA] istek: sorgu=" + sorgu + " tip=" + contentType + " offset=" + offset);
            channelRepository.searchChannelsByTypePaged(sorgu, contentType, offset,
                    new ChannelRepository.OnChannelsLoadedListener() {

                        @Override
                        public void onChannelsLoaded(List<Channel> channels, boolean fromCache) {
                            System.out.println("✅ [ARAMA] sonuç: tip=" + contentType + " adet=" + channels.size());
                            java.util.Map<String, Integer> siraMap =
                                    channelRepository.kategoriIciSiralariHesaplaSync(channels);
                            mainHandler.post(() -> {
                                List<Channel> gorunenler = gizliKanallariFiltreele(channels);
                                Set<String> gizliKategoriler = GizlePaneli.getGizliKategoriler(activity);
                                if (!gizliKategoriler.isEmpty()) {
                                    java.util.Iterator<Channel> iter = gorunenler.iterator();
                                    while (iter.hasNext()) {
                                        Channel ch = iter.next();
                                        String anahtar = ch.getSourceName() + "|" + ch.getCategory();
                                        if (gizliKategoriler.contains(anahtar)) iter.remove();
                                    }
                                }
                                aramaPaneli.sayfaEkle(contentType, gorunenler, siraMap);
                            });
                        }
                        @Override
                        public void onError(Exception e) {
                            System.out.println("❌ [ARAMA] hata: tip=" + contentType + " " + e.getMessage());
                        }
                    });
        });
        dpad.setAramaPaneli(aramaPaneli);
        dokunmatik = new Dokunmatik(activity, dpad);
        sanalDpad = new SanalDpad(activity, dpad);
        sanalDpad.setSiralamaPaneli(ayarlarPaneli.getSiralamaPaneli());
        ayarlarPaneli.setSanalDpadCallback(() -> sanalDpadDurumGuncelle());
        View rootView = activity.findViewById(android.R.id.content);
        oynaticiBar.setAramaAcilisListener(() -> {
            if (dpad != null) dpad.aramaPaneliAc();
        });
        dpad.setOynaticiBar(oynaticiBar);
        oynaticiBar.setDpad(dpad);
        dpad.setKanalDegistirmeCallback(new Dpad.KanalDegistirmeCallback() {
            @Override
            public void sonrakiKanalaGec() {
                if (filtreliKanallar == null || filtreliKanallar.isEmpty()) return;
                if (kanalPaneli.getAdapter() == null) return;
                int aktifPoz = kanalPaneli.getAdapter().getAktifPozisyon();
                if (aktifPoz == -1) return;
                int yeniPoz = aktifPoz + 1;
                int toplamKanal = kanalPaneli.getAdapter().getItemCount();
                if (yeniPoz >= toplamKanal) {
                    System.out.println("⬆️ Liste sonu — işlem yok");
                    return;
                }
                Channel yeniKanal = filtreliKanallar.get(yeniPoz);
                System.out.println("⬆️ Sonraki kanal: " + yeniKanal.getName() + " (poz=" + yeniPoz + ")");
                kanalPaneli.getAdapter().setAktifKanalUrl(yeniKanal.getUrl());
                oynatKanal(yeniKanal);
                if (kategoriPaneli != null && yeniKanal.getCategory() != null)
                    kategoriPaneli.setAktifKategori(yeniKanal.getCategory());
            }

            @Override
            public void oncekiKanalaGec() {
                if (filtreliKanallar == null || filtreliKanallar.isEmpty()) return;
                if (kanalPaneli.getAdapter() == null) return;
                int aktifPoz = kanalPaneli.getAdapter().getAktifPozisyon();
                if (aktifPoz == -1) return;
                int yeniPoz = aktifPoz - 1;
                if (yeniPoz < 0) {
                    System.out.println("⬇️ Liste başı — işlem yok");
                    return;
                }
                Channel yeniKanal = filtreliKanallar.get(yeniPoz);
                System.out.println("⬇️ Önceki kanal: " + yeniKanal.getName() + " (poz=" + yeniPoz + ")");
                kanalPaneli.getAdapter().setAktifKanalUrl(yeniKanal.getUrl());
                oynatKanal(yeniKanal);
                if (kategoriPaneli != null && yeniKanal.getCategory() != null)
                    kategoriPaneli.setAktifKategori(yeniKanal.getCategory());
            }
        });
        kanalPaneli.gizle();
        ayarlarPaneli.gizle();
        kategoriPaneli.gizle();
        icerikPaneli.gizle();
        diziPaneli.gizle();
        if (kanalPaneli.getPanelView() != null) kanalPaneli.getPanelView().setVisibility(View.GONE);
        if (ayarlarPaneli.getPanelView() != null)
            ayarlarPaneli.getPanelView().setVisibility(View.GONE);
        if (kategoriPaneli.getPanelView() != null)
            kategoriPaneli.getPanelView().setVisibility(View.GONE);
        if (icerikPaneli.getPanelView() != null)
            icerikPaneli.getPanelView().setVisibility(View.GONE);
        if (diziPaneli.getPanelView() != null) diziPaneli.getPanelView().setVisibility(View.GONE);
        metadataPanel = new metadataPanel(activity);
        metadataPanel.olustur();
        dpad.setMetadataPanel(metadataPanel);
        if (kanalPaneli.getAdapter() != null) {
            kanalPaneli.getAdapter().setOnFocusDegistiListener(kanal -> {
                if (metadataPanel != null && dpad != null
                        && dpad.getAktifPanel() == Dpad.AktifPanel.SADECE_KANAL
                        && !Icerikpaneli.TIP_CANLI_TV.equals(aktifIcerikTipi))
                    metadataPanel.focusDegisti(kanal.getName(), aktifIcerikTipi);
            });
        }
        if (diziPaneli.getAdapter() != null) {
            diziPaneli.getAdapter().setOnFocusDegistiListener(diziAdi -> {
                if (metadataPanel != null && dpad != null
                        && dpad.getAktifPanel() == Dpad.AktifPanel.SADECE_DIZI
                        && !dpad.isPanelYeniAcildi()) {
                    metadataPanel.focusDegisti(diziAdi, Icerikpaneli.TIP_DIZI);
                }
            });
        }
        dokunmatik.setSanalDpad(sanalDpad);




        kisayolKutusu = new KisayolKutusu(activity);
        kisayolKutusu.setVisibility(View.GONE);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        lp.bottomMargin = dpToPx(20);
        lp.rightMargin = dpToPx(20);
        activity.addContentView(kisayolKutusu, lp);
        dpad.setKisayolKutusu(kisayolKutusu);


        oynaticiBar.getPanelView().post(() -> {
            int barHeight = oynaticiBar.getPanelView().getHeight();
            if (barHeight > 0 && kisayolKutusu != null) {
                FrameLayout.LayoutParams lpKutu = (FrameLayout.LayoutParams) kisayolKutusu.getLayoutParams();
                lpKutu.bottomMargin = barHeight + dpToPx(8);
                kisayolKutusu.setLayoutParams(lpKutu);
                System.out.println("📏 Kısayol kutusu margin ayarlandı: bottomMargin=" + lpKutu.bottomMargin + "px (bar yüksekliği=" + barHeight + "px)");
            }
        });

        ayarlarPaneli.setOnShortcutBoxVisibilityChangedListener(visible -> {
            kutuGorunurlugunuGuncelle();
        });
        dokunmatik.aktifYap();
        dpad.pasifYap();
        System.out.println("🖐️ Başlangıç modu: DOKUNMATIK aktif, DPAD pasif");
        loadChannelsFromDatabase();
    }

    public void sanalDpadDurumGuncelle() {
        if (sanalDpad == null || dpadModu) return;
        if (ayarlarPaneli == null || !ayarlarPaneli.isVisible()) {
            sanalDpad.gizle();
            System.out.println("🔧 SanalDpadDurumGuncelle → Ayarlar kapalı, gizlendi");
            return;
        }
        AyarlarPaneli.MenuSeviyesi seviye = ayarlarPaneli.getMevcutMenu();
        System.out.println("🔧 SanalDpadDurumGuncelle → mevcutMenu=" + seviye);
        if (seviye == AyarlarPaneli.MenuSeviyesi.GIZLE_PANELI
                || seviye == AyarlarPaneli.MenuSeviyesi.SIRALAMA_PANELI
                || seviye == AyarlarPaneli.MenuSeviyesi.M3U_DURUM_PANELI) {
            sanalDpad.goster();
            kutuGorunurlugunuGuncelle();
            if (seviye == AyarlarPaneli.MenuSeviyesi.GIZLE_PANELI) {
                if (dokunmatik != null) dokunmatik.pasifYap();
                if (dpad != null) dpad.pasifYap();
                System.out.println("🔧 GizlePaneli modu: Dokunmatik ve Dpad PASİF, SanalDpad AKTİF");
            }
        } else {
            sanalDpad.gizle();
            kutuGorunurlugunuGuncelle();
            if (seviye != AyarlarPaneli.MenuSeviyesi.GIZLE_PANELI) {
                if (!dpadModu && dokunmatik != null) dokunmatik.aktifYap();
                else if (dpadModu && dpad != null) dpad.aktifYap();
                System.out.println("🔧 GizlePaneli kapatıldı: mod geri yüklendi");
            }
        }
    }

    public void metadataPaneliMarginGuncelle() {
        if (metadataPanel == null || dpad == null) return;

        metadataPanel.solMarginAyarla(0);
        System.out.println("📐 MetadataPanel margin: 0dp (sağa yaslı)");
    }


    public void kutuMargininiAktifPaneleGoreAyarla() {
        if (kisayolKutusu == null || oynaticiBar == null) return;


        if (!oynaticiBar.isVisible()) {
            kutuMargininiSifirla();
            return;
        }

        View barView = oynaticiBar.getPanelView();
        if (barView == null) return;

        barView.post(() -> {
            int barHeight = barView.getHeight();
            if (barHeight == 0) return;

            View seekBarRow = barView.findViewById(R.id.seekBarRow);
            boolean seekBarVisible = (seekBarRow != null && seekBarRow.getVisibility() == View.VISIBLE);
            System.out.println("🔍 [MARGIN] barHeight=" + barHeight + ", seekBarVisible=" + seekBarVisible);

            int marginBottom = barHeight;
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) kisayolKutusu.getLayoutParams();
            lp.bottomMargin = marginBottom + dpToPx(8);
            kisayolKutusu.setLayoutParams(lp);
            System.out.println("📏 Kutu margin ayarlandı: bottomMargin=" + lp.bottomMargin + "px (barHeight=" + barHeight + ", seekVisible=" + seekBarVisible + ")");
        });
    }
    public void kutuMargininiSifirla() {
        if (kisayolKutusu == null) return;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) kisayolKutusu.getLayoutParams();
        lp.bottomMargin = dpToPx(20);
        kisayolKutusu.setLayoutParams(lp);
        System.out.println("📏 Kutu margin sıfırlandı: bottomMargin=" + lp.bottomMargin + "px");
    }

    @Override
    public void onRetryNeeded(String url) {
        System.out.println("🔄 [StreamMonitor] PRE_RETRY: yeniden oynatılıyor: " + aktifStreamUrl);

        final long currentPosition = (mediaPlayer != null && mediaPlayer.isPlaying()) ? mediaPlayer.getTime() : 0;
        mainHandler.post(() -> {
            if (aktifStreamUrl == null || aktifStreamUrl.isEmpty()) return;
            try {
                mediaPlayer.stop();
                if (currentMedia != null) { currentMedia.release(); currentMedia = null; }
                currentMedia = new Media(libVLC, Uri.parse(aktifStreamUrl));
                mediaPlayer.setMedia(currentMedia);
                mediaPlayer.play();

                if (currentPosition > 0) {
                    mediaPlayer.setTime(currentPosition);
                    bekleyenBaslangicPozisyonu = currentPosition;
                }
                if (oynaticiBar != null) oynaticiBar.applyMuteState();
                streamTimeoutIptal();
                streamTimeoutBaslat(aktifStreamUrl);
            } catch (Exception e) {
                System.out.println("❌ [StreamMonitor] Retry hatası: " + e.getMessage());
            }
        });
    }

    @Override
    public void onStreamStarted() {
        System.out.println("✅ [StreamMonitor] Yayın başladı (Playing)");
        mainHandler.post(() -> {
            streamTimeoutIptal();
            yuklemeOverlayGizle();
            streamHataBildirimiGizle();
        });
    }

    @Override
    public void onStreamFailed(String error) {
        System.out.println("❌ [StreamMonitor] Kalıcı hata: " + error);
        mainHandler.post(() -> {
            streamHataBildirimiGoster(aktifKanalAdi, error);
            yuklemeOverlayGizle();
        });
    }

    @Override
    public MediaPlayer getCurrentMediaPlayer() { return mediaPlayer; }

    @Override
    public String getCurrentStreamUrl() { return aktifStreamUrl; }

    private void zorunluAcilisModunuBaslat() {
        m3uZorunluAcilisModu = true;
        System.out.println("🔴 [ZORUNLU] Mod aktif — 800ms sonra ayarlar paneli açılacak");
        mainHandler.postDelayed(this::ayarlarPaneliniZorunluAc, 800);
    }

    private void ayarlarPaneliniZorunluAc() {
        if (!m3uZorunluAcilisModu) return;
        System.out.println("🔴 [ZORUNLU] Ayarlar paneli açılıyor → M3U Yükle ekranına yönlendiriliyor");
        if (dpad != null) {
            dpad.ayarlarPaneliAc();
        } else {
            ayarlarPaneli.goster();
            View pv = ayarlarPaneli.getPanelView();
            if (pv != null) pv.setVisibility(View.VISIBLE);
        }

        mainHandler.postDelayed(() -> {
            ayarlarPaneli.m3uYuklePanelineGit();
        }, 300);
    }

    private void zorunluAcilisDongusuPlanla() {
        if (!m3uZorunluAcilisModu) return;
        if (ayarlarPaneli != null && ayarlarPaneli.isVisible()) {
            System.out.println("🔴 [ZORUNLU] Panel hâlâ açık — zamanlayıcı başlatılmadı");
            return;
        }
        if (zorunluAcilisRunnable != null) {
            mainHandler.removeCallbacks(zorunluAcilisRunnable);
            zorunluAcilisRunnable = null;
        }
        zorunluAcilisRunnable = () -> {
            if (!m3uZorunluAcilisModu) return;
            List<M3UItem> kontrol = m3uManager.getAllM3UItems();
            if (kontrol == null || kontrol.isEmpty()) {
                System.out.println("🔴 [ZORUNLU] 5 sn doldu — ayarlar paneli yeniden açılıyor");
                ayarlarPaneliniZorunluAc();
            } else {
                zorunluAcilisDongusuIptal();
            }
        };
        mainHandler.postDelayed(zorunluAcilisRunnable, 5000);
        System.out.println("🔴 [ZORUNLU] 5 sn sonra yeniden açılacak");
    }

    private void zorunluAcilisDongusuIptal() {
        if (!m3uZorunluAcilisModu) return;
        m3uZorunluAcilisModu = false;
        if (zorunluAcilisRunnable != null) {
            mainHandler.removeCallbacks(zorunluAcilisRunnable);
            zorunluAcilisRunnable = null;
        }
        System.out.println("✅ [ZORUNLU] Döngü iptal edildi — M3U mevcut");
    }

    private void icerikTipiDegisti(String icerikTipi) {
        System.out.println("🟡 [ANA] icerikTipiDegisti: " + icerikTipi + " | mevcut=" + aktifIcerikTipi);
        if (dpad != null && dpad.getAktifPanel() == Dpad.AktifPanel.HICBIRI) {
            System.out.println("🟡 [ANA] panel kapalı, işlem yok");
            return;
        }
        if (icerikTipi.equals(aktifIcerikTipi)) {
            System.out.println("🟡 [ANA] AYNI TİP, çıkılıyor");
            return;
        }
        logTipDegisimi("icerikTipiDegisti", aktifIcerikTipi, icerikTipi);
        aktifIcerikTipi = icerikTipi;
        if (kanalPaneli != null) kanalPaneli.baslikGuncelle(icerikTipi);
        if (kanalPaneli.getAdapter() != null) {
            kanalPaneli.getAdapter().setFavorilerYoneticisi(
                    favorilerYoneticisi, aktifIcerikTipi);
        }
        icerikPaneli.setAktifTip(icerikTipi);
        if (dpad != null) dpad.setAktifIcerikTipi(icerikTipi);
        if (oynaticiBar != null) oynaticiBar.setAktifIcerikTipi(icerikTipi);
        if (Icerikpaneli.TIP_CANLI_TV.equals(icerikTipi)) {
            if (metadataPanel != null) metadataPanel.gizle();
        }
        List<M3UItem> tumM3UListesi = m3uManager.getAllM3UItems();
        if (tumM3UListesi == null || tumM3UListesi.isEmpty()) return;
        kategoriPaneli.setAktifIcerikTipi(icerikTipi);
        aktifKategori = null;
        List<KategoriItem> cachedKategoriler = KategoriCache.getInstance().get(icerikTipi);
        if (Icerikpaneli.TIP_DIZI.equals(icerikTipi)) {
            if (kanalPaneli.getPanelView() != null) kanalPaneli.getPanelView().setVisibility(View.GONE);
            kanalPaneli.gizle();
            if (cachedKategoriler != null && !cachedKategoriler.isEmpty()) {
                System.out.println("⚡ [ANA] SERIES kategori cache HIT: " + cachedKategoriler.size() + " kategori");
                kategoriPaneli.kategorileriGuncelle(cachedKategoriler);
                String ilkKategori = cachedKategoriler.size() > 1
                        ? cachedKategoriler.get(1).getKategoriAdi()
                        : null;
                setDiziPaneliMargin();
                metadataPaneliMarginGuncelle();
                diziPaneli.switchToSeriesMode(null, ilkKategori);
            } else {
                channelRepository.loadCategoriesWithSource(null, "SERIES",
                        new ChannelRepository.OnKategoriItemsLoadedListener() {
                            @Override
                            public void onKategoriItemsLoaded(List<KategoriItem> items) {
                                gizliKategorileriFiltreeleAsync(items, filtrelenmis -> {
                                    KategoriCache.getInstance().put("SERIES", filtrelenmis);
                                    kategoriPaneli.kategorileriGuncelle(filtrelenmis);
                                    String ilkKategori = filtrelenmis.size() > 1
                                            ? filtrelenmis.get(1).getKategoriAdi()
                                            : null;
                                    setDiziPaneliMargin();
                                    metadataPaneliMarginGuncelle();
                                    diziPaneli.switchToSeriesMode(null, ilkKategori);
                                });
                            }
                            @Override
                            public void onError(Exception e) {
                                mainHandler.post(() -> diziPaneli.switchToSeriesMode(null, null));
                            }
                        });
            }
        } else {
            diziPaneli.switchToNormalMode();
            if (kanalPaneli.getPanelView() != null) kanalPaneli.getPanelView().setVisibility(View.VISIBLE);
            kanalPaneli.goster();
            setKanalPaneliMargin();
            metadataPaneliMarginGuncelle();
            if (cachedKategoriler != null && !cachedKategoriler.isEmpty()) {
                System.out.println("⚡ [ANA] " + icerikTipi + " kategori cache HIT: " + cachedKategoriler.size() + " kategori");
                kategoriPaneli.kategorileriGuncelle(cachedKategoriler);
                String ilkKategori = cachedKategoriler.size() > 1
                        ? cachedKategoriler.get(1).getKategoriAdi()
                        : null;
                kategoriFiltrele(ilkKategori);
            } else {
                channelRepository.loadCategoriesWithSource(null, icerikTipi,
                        new ChannelRepository.OnKategoriItemsLoadedListener() {
                            @Override
                            public void onKategoriItemsLoaded(List<KategoriItem> items) {
                                gizliKategorileriFiltreeleAsync(items, filtrelenmis -> {
                                    KategoriCache.getInstance().put(icerikTipi, filtrelenmis);
                                    kategoriPaneli.kategorileriGuncelle(filtrelenmis);
                                    String ilkKategori = filtrelenmis.size() > 1
                                            ? filtrelenmis.get(1).getKategoriAdi()
                                            : null;
                                    if (ilkKategori != null) {
                                        kategoriFiltrele(ilkKategori);
                                    } else {
                                        tumKanallar      = new ArrayList<>();
                                        filtreliKanallar = new ArrayList<>();
                                        kanalPaneli.kanallarıGuncelle(new ArrayList<>());
                                    }
                                });
                            }
                            @Override
                            public void onError(Exception e) {
                                channelRepository.loadAllChannelsByType(null, icerikTipi,
                                        new ChannelRepository.OnChannelsLoadedListener() {
                                            @Override
                                            public void onChannelsLoaded(List<Channel> channels, boolean fromCache) {
                                                mainHandler.post(() -> {
                                                    List<Channel> gorunenler = gizliKanallariFiltreele(channels);
                                                    tumKanallar      = gorunenler;
                                                    filtreliKanallar = new ArrayList<>(gorunenler);
                                                    List<String> kategoriAdlari = kategorileriCikar(gorunenler);
                                                    List<KategoriItem> items2 = kategoriAdlariniItemeCevir(kategoriAdlari, "");
                                                    gizliKategorileriFiltreeleAsync(items2, filtrelenmis -> {
                                                        KategoriCache.getInstance().put(icerikTipi, filtrelenmis);
                                                        kategoriPaneli.kategorileriGuncelle(filtrelenmis);
                                                        kanalPaneli.kanallarıGuncelle(gorunenler);
                                                        System.out.println("✅ İçerik: " + icerikTipi + " → " + gorunenler.size() + " kanal");
                                                    });
                                                });
                                            }
                                            @Override public void onError(Exception e2) {}
                                        });
                            }
                        });
            }
        }
        kutuGorunurlugunuGuncelle();
    }

    private void setDiziPaneliMargin() {
        if (dpad == null) return;
        Dpad.AktifPanel panel = dpad.getAktifPanel();
        if (panel == Dpad.AktifPanel.ICERIK_KATEGORI_KANAL
                || panel == Dpad.AktifPanel.ICERIK_KATEGORI_DIZI) {
            diziPaneli.setMarginStart(MARGIN_ICERIK_VE_KATEGORI);
        } else if (panel == Dpad.AktifPanel.KATEGORI_VE_KANAL
                || panel == Dpad.AktifPanel.KATEGORI_VE_DIZI) {
            diziPaneli.setMarginStart(MARGIN_YALNIZ_KATEGORI);
        } else {
            diziPaneli.setMarginStart(0);
        }
        kutuGorunurlugunuGuncelle();
    }

    private void setKanalPaneliMargin() {
        if (dpad == null) return;
        Dpad.AktifPanel panel = dpad.getAktifPanel();
        if (panel == Dpad.AktifPanel.ICERIK_KATEGORI_KANAL
                || panel == Dpad.AktifPanel.ICERIK_KATEGORI_DIZI) {
            kanalPaneli.setMarginStart(MARGIN_ICERIK_VE_KATEGORI);
        } else if (panel == Dpad.AktifPanel.KATEGORI_VE_KANAL
                || panel == Dpad.AktifPanel.KATEGORI_VE_DIZI) {
            kanalPaneli.setMarginStart(MARGIN_YALNIZ_KATEGORI);
        } else {
            kanalPaneli.setMarginStart(0);
        }
        kutuGorunurlugunuGuncelle();
    }

    private void kategoriDebounceIleYukle(String kategori) {
        if (bekleyenKategoriYukle != null) {
            mainHandler.removeCallbacks(bekleyenKategoriYukle);
        }
        bekleyenKategoriYukle = () -> {
            bekleyenKategoriYukle = null;
            kategoriFiltrele(kategori);
        };
        mainHandler.postDelayed(bekleyenKategoriYukle, KATEGORI_DEBOUNCE_MS);
    }

    private void diziKategoriDebounceIleYukle(String kategori) {
        if (bekleyenDiziKategoriYukle != null) {
            mainHandler.removeCallbacks(bekleyenDiziKategoriYukle);
        }
        bekleyenDiziKategoriYukle = () -> {
            bekleyenDiziKategoriYukle = null;
            if (diziPaneli != null) diziPaneli.kategoriFiltrele(kategori);
        };
        mainHandler.postDelayed(bekleyenDiziKategoriYukle, KATEGORI_DEBOUNCE_MS);
    }

    private void kategoriFiltrele(String kategori) {
        this.aktifKategori = kategori;
        if (FavorilerYoneticisi.FAVORI_KATEGORI_ADI.equals(kategori)) {
            favorileriYukle();
            return;
        }
        List<M3UItem> tumM3UListesi = m3uManager.getAllM3UItems();
        if (tumM3UListesi == null || tumM3UListesi.isEmpty()) return;
        channelRepository.loadChannelsByTypeAndCategory(null, aktifIcerikTipi, kategori,
                new ChannelRepository.OnChannelsLoadedListener() {
                    @Override
                    public void onChannelsLoaded(List<Channel> channels, boolean fromCache) {
                        mainHandler.post(() -> {
                            List<Channel> gorunenler = gizliKanallariFiltreele(channels);
                            filtreliKanallar = gorunenler;
                            tumKanallar      = gorunenler;
                            kanalPaneli.setDbSayfaCallback((offset, sonuc) ->
                                    channelRepository.loadChannelsByTypeAndCategoryPaged(
                                            null, aktifIcerikTipi, aktifKategori, offset,
                                            new ChannelRepository.OnChannelsLoadedListener() {
                                                public void onChannelsLoaded(List<Channel> channels, boolean fromCache) {
                                                    boolean sayfaBitti = channels.size() < 50;
                                                    List<Channel> g = gizliKanallariFiltreele(channels);
                                                    mainHandler.post(() -> {
                                                        sonuc.sayfaGeldi(g, sayfaBitti);
                                                    });
                                                }
                                                @Override public void onError(Exception e) {
                                                    mainHandler.post(() -> sonuc.sayfaGeldi(new ArrayList<>(), true));
                                                }
                                            }));
                            kanalPaneli.kanallarıGuncelle(gorunenler);
                            System.out.println("🔍 Kategori filtresi: " + kategori
                                    + " [" + aktifIcerikTipi + "] → " + gorunenler.size() + " kanal");
                            System.out.println("🔍 [KATEGORİFİLTRE] ilkAcilisOtomatikOynat=" + ilkAcilisOtomatikOynat + " gorunenler.size=" + gorunenler.size());
                            if (ilkAcilisOtomatikOynat && !gorunenler.isEmpty()) {
                                ilkAcilisOtomatikOynat = false;
                                Channel ilkKanal = gorunenler.get(0);
                                System.out.println("▶️ İlk açılış — otomatik oynatılıyor: " + ilkKanal.getName());
                                if (Icerikpaneli.TIP_FILM.equals(ilkKanal.getContentType())) {
                                    filmOynatPozisyonKontrollu(ilkKanal);
                                } else if (Icerikpaneli.TIP_DIZI.equals(ilkKanal.getContentType())) {
                                    diziAcilisOynatPozisyonKontrollu(ilkKanal);
                                } else {
                                    oynatKanal(ilkKanal);
                                }
                                kanalPaneli.ilkKanalıAktifYap(ilkKanal);
                            }
                            kutuGorunurlugunuGuncelle();
                        });
                    }
                    @Override
                    public void onError(Exception e) {
                        mainHandler.post(() ->
                                System.out.println("❌ Kategori filtre hatası: " + e.getMessage()));
                    }
                });
    }

    private void favorileriYukle() {
        System.out.println("⭐ [FAVORİ] Yükleniyor: " + aktifIcerikTipi);
        if (favorilerYoneticisi == null) {
            kanalPaneli.kanallarıGuncelle(new ArrayList<>());
            return;
        }
        if (Icerikpaneli.TIP_DIZI.equals(aktifIcerikTipi)) {
            List<String> favoriDiziAdlari = favorilerYoneticisi.getHepsi(Icerikpaneli.TIP_DIZI);
            System.out.println("⭐ [FAVORİ] Dizi: " + favoriDiziAdlari.size() + " favori");
            if (diziPaneli != null) diziPaneli.switchToFavoriModu(favoriDiziAdlari);
            kutuGorunurlugunuGuncelle();
            return;
        }
        List<String> favoriUrller = favorilerYoneticisi.getHepsi(aktifIcerikTipi);
        System.out.println("⭐ [FAVORİ] " + aktifIcerikTipi + ": " + favoriUrller.size() + " favori");
        if (favoriUrller.isEmpty()) {
            mainHandler.post(() -> {
                kanalPaneli.kanallarıGuncelle(new ArrayList<>());
                kutuGorunurlugunuGuncelle();
            });
            return;
        }
        channelRepository.loadChannelsByUrls(favoriUrller,
                new ChannelRepository.OnChannelsLoadedListener() {
                    @Override
                    public void onChannelsLoaded(List<Channel> channels, boolean fromCache) {
                        mainHandler.post(() -> {
                            List<Channel> gorunenler = gizliKanallariFiltreele(channels);
                            Set<String> gizliKategoriler = GizlePaneli.getGizliKategoriler(activity);
                            if (!gizliKategoriler.isEmpty()) {
                                java.util.Iterator<Channel> iter = gorunenler.iterator();
                                while (iter.hasNext()) {
                                    Channel ch = iter.next();
                                    String anahtar = ch.getSourceName() + "|" + ch.getCategory();
                                    if (gizliKategoriler.contains(anahtar)) iter.remove();
                                }
                            }
                            Collections.sort(gorunenler, new java.util.Comparator<Channel>() {
                                @Override
                                public int compare(Channel a, Channel b) {
                                    int ia = favoriUrller.indexOf(a.getUrl());
                                    int ib = favoriUrller.indexOf(b.getUrl());
                                    return Integer.compare(ia, ib);
                                }
                            });
                            filtreliKanallar = gorunenler;
                            tumKanallar      = gorunenler;
                            kanalPaneli.setDbSayfaCallback(null);
                            kanalPaneli.kanallarıGuncelle(gorunenler);
                            System.out.println("⭐ [FAVORİ] " + gorunenler.size() + " kanal yüklendi");
                            kutuGorunurlugunuGuncelle();
                        });
                    }
                    @Override
                    public void onError(Exception e) {
                        mainHandler.post(() -> {
                            kanalPaneli.kanallarıGuncelle(new ArrayList<>());
                            kutuGorunurlugunuGuncelle();
                        });
                    }
                });
    }
    public void onRequestPermissionsResult(int requestCode, int[] grantResults) {
        if (requestCode == 997) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                if (ayarlarPaneli != null) ayarlarPaneli.disaAktarBaslat();
            } else {
                android.widget.Toast.makeText(activity, "İzin verilmedi, dışa aktarma iptal edildi.", android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadChannelsFromDatabase() {
        List<M3UItem> tumM3UListesi = m3uManager.getAllM3UItems();
        if (tumM3UListesi == null || tumM3UListesi.isEmpty()) {
            tumKanallar      = new ArrayList<>();
            filtreliKanallar = new ArrayList<>();
            Toast.makeText(activity,
                    "Henüz M3U listesi yok. Ayarlar → M3U Yönetimi → M3U Dosyası Yükle'den M3U ekleyin.",
                    Toast.LENGTH_LONG).show();
            zorunluAcilisModunuBaslat();
            return;
        }
        kategoriPaneli.setAktifIcerikTipi(aktifIcerikTipi);
        if (kanalPaneli != null) kanalPaneli.baslikGuncelle(aktifIcerikTipi);
        String lastPlayedUrl = getLastPlayedUrl();
        if (lastPlayedUrl != null && !lastPlayedUrl.isEmpty()) {
            channelRepository.loadChannelByUrl(lastPlayedUrl, new ChannelRepository.OnChannelLoadedListener() {
                @Override
                public void onChannelLoaded(Channel channel) {
                    mainHandler.post(() -> {
                        if (channel != null) {
                            aktifIcerikTipi = channel.getContentType();
                            icerikPaneli.setAktifTip(aktifIcerikTipi);
                            if (dpad != null) dpad.setAktifIcerikTipi(aktifIcerikTipi);
                            if (oynaticiBar != null) oynaticiBar.setAktifIcerikTipi(aktifIcerikTipi);
                            kategoriPaneli.setAktifIcerikTipi(aktifIcerikTipi);
                            loadChannelsByTypeAndPlay(aktifIcerikTipi, channel);
                        } else {
                            ilkAcilisOtomatikOynat = true;
                            loadDefaultChannels();
                        }
                    });
                }
                @Override public void onError(Exception e) {
                    mainHandler.post(() -> {
                        ilkAcilisOtomatikOynat = true;
                        loadDefaultChannels();
                    });
                }
            });
        } else {
            ilkAcilisOtomatikOynat = true;
            loadDefaultChannels();
        }
    }

    private void loadChannelsByTypeAndPlay(String contentType, Channel hedefKanal) {
        channelRepository.loadCategoriesWithSource(null, contentType,
                new ChannelRepository.OnKategoriItemsLoadedListener() {
                    @Override
                    public void onKategoriItemsLoaded(List<KategoriItem> items) {
                        gizliKategorileriFiltreeleAsync(items, filtrelenmis -> {
                            KategoriCache.getInstance().put(contentType, filtrelenmis);
                            kategoriPaneli.kategorileriGuncelle(filtrelenmis);
                            String hedefKategori = hedefKanal.getCategory();
                            aktifKategori = hedefKategori;
                            channelRepository.loadChannelsByTypeAndCategory(null, contentType, hedefKategori,
                                    new ChannelRepository.OnChannelsLoadedListener() {
                                        @Override
                                        public void onChannelsLoaded(List<Channel> channels, boolean fromCache) {
                                            mainHandler.post(() -> {
                                                List<Channel> gorunenler = gizliKanallariFiltreele(channels);
                                                tumKanallar      = gorunenler;
                                                filtreliKanallar = new ArrayList<>(gorunenler);
                                                if (kanalPaneli != null)
                                                    kanalPaneli.setDbSayfaCallback((offset, sonuc) ->
                                                            channelRepository.loadChannelsByTypeAndCategoryPaged(
                                                                    null, aktifIcerikTipi, aktifKategori, offset,
                                                                    new ChannelRepository.OnChannelsLoadedListener() {
                                                                        public void onChannelsLoaded(List<Channel> channels, boolean fromCache) {
                                                                            boolean sayfaBitti = channels.size() < 50;
                                                                            List<Channel> g = gizliKanallariFiltreele(channels);
                                                                            mainHandler.post(() -> {
                                                                                sonuc.sayfaGeldi(g, sayfaBitti);
                                                                            });
                                                                        }
                                                                        @Override public void onError(Exception e) {
                                                                            mainHandler.post(() -> sonuc.sayfaGeldi(new ArrayList<>(), true));
                                                                        }
                                                                    }));
                                                if (kanalPaneli != null) kanalPaneli.kanallarıGuncelle(gorunenler);
                                                if (Icerikpaneli.TIP_FILM.equals(hedefKanal.getContentType())) {
                                                    filmOynatPozisyonKontrollu(hedefKanal);
                                                } else if (Icerikpaneli.TIP_DIZI.equals(hedefKanal.getContentType())) {
                                                    diziAcilisOynatPozisyonKontrollu(hedefKanal);
                                                } else {
                                                    oynatKanal(hedefKanal);
                                                }
                                                if (kanalPaneli != null) kanalPaneli.ilkKanalıAktifYap(hedefKanal);
                                            });
                                        }
                                        @Override public void onError(Exception e) {
                                            mainHandler.post(() -> loadDefaultChannels());
                                        }
                                    });
                        });
                    }
                    @Override public void onError(Exception e) {
                        mainHandler.post(() -> loadDefaultChannels());
                    }
                });
    }

    private void gizleSonrasiListeyiYenile() {
        if (aktifStreamUrl != null && !aktifStreamUrl.isEmpty()) {
            Set<String> gizliUrller      = GizlePaneli.getGizliKanalUrlleri(activity);
            Set<String> gizliKategoriler = GizlePaneli.getGizliKategoriler(activity);
            List<M3UItem> gorununM3U     = m3uManager.getVisibleM3UItems();
            boolean kanalGizli = gizliUrller.contains(aktifStreamUrl);
            boolean kategoriGizli = false;
            if (aktifKategori != null) {
                for (M3UItem m : gorununM3U) {
                    if (gizliKategoriler.contains(m.getName() + "|" + aktifKategori)) {
                        kategoriGizli = true;
                        break;
                    }
                }
            }
            boolean m3uGizli = gorununM3U.isEmpty();
            if (!m3uGizli) {
                boolean bulundu = false;
                for (M3UItem m : gorununM3U) {
                    if (m.getName().equals(aktifOynaticSourceName)) {
                        bulundu = true;
                        break;
                    }
                }
                m3uGizli = !bulundu;
            }
            if (kanalGizli || kategoriGizli || m3uGizli) {
                yedegeGecisYap(gizliUrller, gizliKategoriler, gorununM3U,
                        kanalGizli, kategoriGizli, m3uGizli);
                kutuGorunurlugunuGuncelle();
                return;
            }
        }
        kanalListeyiYenileGizleSonrasi();
        kutuGorunurlugunuGuncelle();
    }

    private void yedegeGecisYap(Set<String> gizliUrller, Set<String> gizliKategoriler,
                                List<M3UItem> gorununM3U,
                                boolean kanalGizli, boolean kategoriGizli, boolean m3uGizli) {
        String hedefM3U      = aktifOynaticSourceName;
        String hedefKategori = aktifKategori;
        if (m3uGizli) {
            if (gorununM3U.isEmpty()) {
                System.out.println("🚫 Tüm M3U'lar gizli → player durduruluyor");
                if (mediaPlayer != null) mediaPlayer.stop();
                aktifStreamUrl = null;
                kanalListeyiYenileGizleSonrasi();
                return;
            }
            hedefM3U      = gorununM3U.get(0).getName();
            hedefKategori = null;
        }
        final String finalHedefM3U = hedefM3U;
        if (kanalGizli && !kategoriGizli && !m3uGizli) {
            channelRepository.loadChannelsByTypeAndCategory(finalHedefM3U, aktifIcerikTipi, hedefKategori,
                    new ChannelRepository.OnChannelsLoadedListener() {
                        @Override
                        public void onChannelsLoaded(List<Channel> channels, boolean fromCache) {
                            mainHandler.post(() -> {
                                List<Channel> gorunenler = gizliKanallariFiltreele(channels);
                                if (!gorunenler.isEmpty()) {
                                    System.out.println("🔄 Senaryo 1: Aynı kategoride ilk kanala geçiliyor");
                                    oynatKanal(gorunenler.get(0));
                                    kanalListeyiYenileGizleSonrasi();
                                } else {
                                    yedegeGecisKategoriAra(finalHedefM3U, gizliKategoriler, gorununM3U);
                                }
                            });
                        }
                        @Override public void onError(Exception e) {
                            mainHandler.post(() -> yedegeGecisKategoriAra(finalHedefM3U, gizliKategoriler, gorununM3U));
                        }
                    });
            return;
        }
        yedegeGecisKategoriAra(finalHedefM3U, gizliKategoriler, gorununM3U);
    }

    private void yedegeGecisKategoriAra(String hedefM3U, Set<String> gizliKategoriler,
                                        List<M3UItem> gorununM3U) {
        channelRepository.loadCategoriesWithSource(hedefM3U, aktifIcerikTipi,
                new ChannelRepository.OnKategoriItemsLoadedListener() {
                    @Override
                    public void onKategoriItemsLoaded(List<KategoriItem> items) {
                        gizliKategorileriFiltreeleAsync(items, filtrelenmis -> {
                            String ilkKat = null;
                            for (KategoriItem k : filtrelenmis) {
                                if (!FavorilerYoneticisi.FAVORI_KATEGORI_ADI.equals(k.getKategoriAdi())) {
                                    ilkKat = k.getKategoriAdi();
                                    break;
                                }
                            }
                            if (ilkKat == null) {
                                if (gorununM3U.size() > 1) {
                                    String digerM3U = null;
                                    for (M3UItem m : gorununM3U) {
                                        if (!m.getName().equals(hedefM3U)) {
                                            digerM3U = m.getName();
                                            break;
                                        }
                                    }
                                    if (digerM3U != null) {
                                        final String finalDigerM3U = digerM3U;
                                        yedegeGecisKategoriAra(finalDigerM3U, gizliKategoriler, gorununM3U);
                                        return;
                                    }
                                }
                                System.out.println("🚫 Görünür kategori bulunamadı → player durduruluyor");
                                if (mediaPlayer != null) mediaPlayer.stop();
                                aktifStreamUrl = null;
                                kanalListeyiYenileGizleSonrasi();
                                return;
                            }
                            final String ilkKategori = ilkKat;
                            final String finalM3U    = hedefM3U;
                            channelRepository.loadChannelsByTypeAndCategory(finalM3U, aktifIcerikTipi, ilkKategori,
                                    new ChannelRepository.OnChannelsLoadedListener() {
                                        @Override
                                        public void onChannelsLoaded(List<Channel> channels, boolean fromCache) {
                                            mainHandler.post(() -> {
                                                List<Channel> gorunenler = gizliKanallariFiltreele(channels);
                                                if (!gorunenler.isEmpty()) {
                                                    System.out.println("🔄 Senaryo 2/3: " + ilkKategori + " → " + gorunenler.get(0).getName());
                                                    aktifKategori = ilkKategori;
                                                    oynatKanal(gorunenler.get(0));
                                                } else {
                                                    System.out.println("🚫 İlk kategoride de kanal yok → player durduruluyor");
                                                    if (mediaPlayer != null) mediaPlayer.stop();
                                                    aktifStreamUrl = null;
                                                }
                                                kanalListeyiYenileGizleSonrasi();
                                            });
                                        }
                                        @Override public void onError(Exception e) {
                                            mainHandler.post(() -> {
                                                if (mediaPlayer != null) mediaPlayer.stop();
                                                aktifStreamUrl = null;
                                                kanalListeyiYenileGizleSonrasi();
                                            });
                                        }
                                    });
                        });
                    }
                    @Override public void onError(Exception e) {
                        mainHandler.post(() -> {
                            if (mediaPlayer != null) mediaPlayer.stop();
                            aktifStreamUrl = null;
                            kanalListeyiYenileGizleSonrasi();
                        });
                    }
                });
    }

    private void kanalListeyiYenileGizleSonrasi() {
        kanalPaneli.kanallarıGuncelle(new ArrayList<>());
        channelRepository.loadCategoriesWithSource(null, aktifIcerikTipi,
                new ChannelRepository.OnKategoriItemsLoadedListener() {
                    @Override
                    public void onKategoriItemsLoaded(List<KategoriItem> items) {
                        gizliKategorileriFiltreeleAsync(items, filtrelenmis -> {
                            KategoriCache.getInstance().put(aktifIcerikTipi, filtrelenmis);
                            kategoriPaneli.kategorileriGuncelle(filtrelenmis);
                            System.out.println("🔒 GizleSonrası liste: " + filtrelenmis.size() + " kategori");
                            String aktifKat = aktifKategori != null ? aktifKategori
                                    : (!filtrelenmis.isEmpty() ? filtrelenmis.get(0).getKategoriAdi() : null);
                            if (aktifKat == null) return;
                            final String kat = aktifKat;
                            channelRepository.loadChannelsByTypeAndCategory(null, aktifIcerikTipi, kat,
                                    new ChannelRepository.OnChannelsLoadedListener() {
                                        @Override
                                        public void onChannelsLoaded(List<Channel> channels, boolean fromCache) {
                                            mainHandler.post(() -> {
                                                List<Channel> gorunenler = gizliKanallariFiltreele(channels);
                                                filtreliKanallar = gorunenler;
                                                tumKanallar      = gorunenler;
                                                kanalPaneli.setDbSayfaCallback((offset, sonuc) ->
                                                        channelRepository.loadChannelsByTypeAndCategoryPaged(
                                                                null, aktifIcerikTipi, aktifKategori, offset,
                                                                new ChannelRepository.OnChannelsLoadedListener() {
                                                                    public void onChannelsLoaded(List<Channel> ch, boolean fc) {
                                                                        boolean bitti = ch.size() < 50;
                                                                        List<Channel> g = gizliKanallariFiltreele(ch);
                                                                        mainHandler.post(() -> sonuc.sayfaGeldi(g, bitti));
                                                                    }
                                                                    @Override public void onError(Exception e) {
                                                                        mainHandler.post(() -> sonuc.sayfaGeldi(new ArrayList<>(), true));
                                                                    }
                                                                }));
                                                kanalPaneli.kanallarıGuncelle(gorunenler);
                                                System.out.println("🔒 GizleSonrası kanal: " + kat + " → " + gorunenler.size() + " kanal");
                                            });
                                        }
                                        @Override public void onError(Exception e) {}
                                    });
                        });
                    }
                    @Override public void onError(Exception e) {
                        System.out.println("❌ kanalListeyiYenileGizleSonrasi hata: " + e.getMessage());
                    }
                });
    }

    private void loadDefaultChannels() {
        channelRepository.loadCategoriesWithSource(null, aktifIcerikTipi,
                new ChannelRepository.OnKategoriItemsLoadedListener() {
                    @Override
                    public void onKategoriItemsLoaded(List<KategoriItem> items) {
                        gizliKategorileriFiltreeleAsync(items, filtrelenmis -> {
                            KategoriCache.getInstance().put(aktifIcerikTipi, filtrelenmis);
                            kategoriPaneli.kategorileriGuncelle(filtrelenmis);
                            String ilkKategori = filtrelenmis.size() > 1
                                    ? filtrelenmis.get(1).getKategoriAdi()
                                    : (filtrelenmis.isEmpty() ? null : filtrelenmis.get(0).getKategoriAdi());
                            if (ilkKategori != null) {
                                kategoriFiltrele(ilkKategori);
                            }
                        });
                    }
                    @Override
                    public void onError(Exception e) {
                        mainHandler.post(() ->
                                System.out.println("❌ Kategori yükleme hatası: " + e.getMessage()));
                    }
                });
    }

    private void yeniM3UYukle(String dosyaYolu) {
        if (dosyaYolu != null && (dosyaYolu.startsWith("http://") || dosyaYolu.startsWith("https://"))) {
            mainHandler.post(() -> {
                zorunluAcilisDongusuIptal();
                aktifIcerikTipi = Icerikpaneli.TIP_CANLI_TV;
                icerikPaneli.setAktifTip(aktifIcerikTipi);
                if (dpad != null) dpad.setAktifIcerikTipi(aktifIcerikTipi);
                if (oynaticiBar != null) oynaticiBar.setAktifIcerikTipi(aktifIcerikTipi);
                KategoriCache.getInstance().invalidateAll();
                DiziCache.getInstance().invalidateAll();
                SiralamaPaneli spUrl = ayarlarPaneli.getSiralamaPaneli();
                if (spUrl != null) spUrl.veriGecersizKil();
                diziPaneli.m3uDegisti();
                diziPaneli.switchToNormalMode();
                ilkAcilisOtomatikOynat = true;
                System.out.println("🔍 [YENIM3U-URL] ilkAcilisOtomatikOynat=" + ilkAcilisOtomatikOynat);
                System.out.println("🔍 [LOADCHANNELS] ilkAcilisOtomatikOynat=" + ilkAcilisOtomatikOynat + " lastUrl=" + getLastPlayedUrl());
                loadChannelsFromDatabase();
                if (ayarlarPaneli != null && ayarlarPaneli.getYuklemeListener() != null)
                    ayarlarPaneli.getYuklemeListener().yuklemeBitti();
                ayarlarPaneliniKapat();
                kutuGorunurlugunuGuncelle();
            });
            return;
        }
        try {
            String m3uContent = readM3UFromFile(dosyaYolu);
            if (m3uContent == null || m3uContent.isEmpty()) {
                Toast.makeText(activity, "Dosya okunamadı!", Toast.LENGTH_SHORT).show();
                return;
            }
            String dosyaAdi = new File(dosyaYolu).getName()
                    .replace(".m3u", "").replace(".m3u8", "").replace(".txt", "").trim();
            if (dosyaAdi.isEmpty()) dosyaAdi = "M3U_" + System.currentTimeMillis();
            String sourceName = dosyaAdi;
            try {
                String[] satirlar = m3uContent.split("\n");
                for (String satir : satirlar) {
                    satir = satir.trim();
                    if (satir.startsWith("http://") || satir.startsWith("https://")) {
                        java.net.URL parsedUrl = new java.net.URL(satir);
                        String host = parsedUrl.getHost();
                        String kisaHost = kisaltHost(host);
                        String kullaniciAdi = null;
                        String query = parsedUrl.getQuery();
                        if (query != null) {
                            for (String param : query.split("&")) {
                                if (param.startsWith("username=")) {
                                    kullaniciAdi = param.substring("username=".length());
                                    break;
                                }
                            }
                        }
                        if (kullaniciAdi == null) {
                            String path = parsedUrl.getPath();
                            String[] parcalar = path.split("/");
                            if (parcalar.length >= 2 && !parcalar[1].isEmpty()
                                    && !parcalar[1].equals("get.php")
                                    && !parcalar[1].equals("live")
                                    && !parcalar[1].equals("movie")
                                    && !parcalar[1].equals("series")) {
                                kullaniciAdi = parcalar[1];
                            }
                        }
                        if (kullaniciAdi != null && !kullaniciAdi.isEmpty()) {
                            sourceName = kisaHost + "(" + kullaniciAdi + ")";
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ Dosya isim tespiti hatası: " + e.getMessage());
            }
            final String finalSourceName = sourceName;
            Toast.makeText(activity, "M3U parse ediliyor...", Toast.LENGTH_SHORT).show();
            if (ayarlarPaneli != null) {
                AyarlarPaneli.YuklemeListener l = ayarlarPaneli.getYuklemeListener();
                if (l != null) l.yuklemeBasladi(finalSourceName);
            }
            M3UParser parser = new M3UParser(m3uContent, sourceName, new M3UParser.OnParseCompleteListener() {
                @Override public void onParseStarted()              {}
                @Override public void onParseProgress(int progress) {}
                @Override
                public void onParseComplete(List<Channel> yeniKanallar) {
                    mainHandler.post(() -> {
                        if (yeniKanallar == null || yeniKanallar.isEmpty()) {
                            Toast.makeText(activity, "Dosyada kanal bulunamadı!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        com.example.livetvapp.database.M3UItem m3uItem =
                                new com.example.livetvapp.database.M3UItem(
                                        finalSourceName, dosyaYolu, "", yeniKanallar.size(), true);
                        m3uManager.addM3U(m3uItem);
                        channelRepository.insertChannels(yeniKanallar, new ChannelRepository.OnCompleteListener() {
                            @Override
                            public void onSuccess() {
                                mainHandler.post(() -> {
                                    zorunluAcilisDongusuIptal();
                                    resetMediaPlayer();
                                    aktifIcerikTipi = Icerikpaneli.TIP_CANLI_TV;
                                    icerikPaneli.setAktifTip(aktifIcerikTipi);
                                    if (dpad != null) dpad.setAktifIcerikTipi(aktifIcerikTipi);
                                    if (oynaticiBar != null) oynaticiBar.setAktifIcerikTipi(aktifIcerikTipi);
                                    KategoriCache.getInstance().invalidateAll();
                                    DiziCache.getInstance().invalidateAll();
                                    SiralamaPaneli spParse = ayarlarPaneli.getSiralamaPaneli();
                                    if (spParse != null) spParse.veriGecersizKil();
                                    diziPaneli.m3uDegisti();
                                    diziPaneli.switchToNormalMode();
                                    ilkAcilisOtomatikOynat = true;
                                    loadDefaultChannels();
                                    Toast.makeText(activity,
                                            yeniKanallar.size() + " kanal yüklendi! (" + finalSourceName + ")",
                                            Toast.LENGTH_SHORT).show();
                                    if (ayarlarPaneli != null && ayarlarPaneli.getYuklemeListener() != null)
                                        ayarlarPaneli.getYuklemeListener().yuklemeBitti();
                                    ayarlarPaneliniKapat();
                                    kutuGorunurlugunuGuncelle();
                                });
                            }
                            @Override
                            public void onError(Exception e) {
                                mainHandler.post(() -> {
                                    zorunluAcilisDongusuIptal();
                                    KanalListesi.saveChannelsForM3U(activity, yeniKanallar, finalSourceName);
                                    List<Channel> liveKanallar = new ArrayList<>();
                                    for (Channel c : yeniKanallar) {
                                        if (Icerikpaneli.TIP_CANLI_TV.equals(c.getContentType())) liveKanallar.add(c);
                                    }
                                    tumKanallar      = liveKanallar;
                                    filtreliKanallar = new ArrayList<>(liveKanallar);
                                    List<String> kategoriAdlari = kategorileriCikar(tumKanallar);
                                    List<KategoriItem> items = kategoriAdlariniItemeCevir(kategoriAdlari, finalSourceName);
                                    List<KategoriItem> filtrelenmis = gizliKategorileriFiltreeleSync(items);
                                    KategoriCache.getInstance().put(aktifIcerikTipi, filtrelenmis);
                                    kategoriPaneli.kategorileriGuncelle(filtrelenmis);
                                    kanalPaneli.kanallarıGuncelle(tumKanallar);
                                    Toast.makeText(activity, yeniKanallar.size() + " kanal yüklendi (fallback)", Toast.LENGTH_SHORT).show();
                                    if (ayarlarPaneli != null && ayarlarPaneli.getYuklemeListener() != null)
                                        ayarlarPaneli.getYuklemeListener().yuklemeBitti();
                                    ayarlarPaneliniKapat();
                                    kutuGorunurlugunuGuncelle();
                                });
                            }
                        });
                    });
                }
                @Override
                public void onParseError(Exception e) {
                    mainHandler.post(() ->
                            Toast.makeText(activity, "M3U parse hatası: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    if (ayarlarPaneli != null && ayarlarPaneli.getYuklemeListener() != null)
                        ayarlarPaneli.getYuklemeListener().yuklemeBitti();
                }
            });
            parser.execute();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(activity, "M3U yüklenirken hata: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private List<String> kategorileriCikar(List<Channel> kanallar) {
        Set<String> set = new LinkedHashSet<>();
        if (kanallar != null) {
            for (Channel kanal : kanallar) {
                String kat = kanal.getCategory();
                if (kat != null && !kat.isEmpty()) set.add(kat);
            }
        }
        return new ArrayList<>(set);
    }

    private List<KategoriItem> kategoriAdlariniItemeCevir(List<String> adlar, String kaynakAdi) {
        List<KategoriItem> items = new ArrayList<>();
        for (String ad : adlar) {
            items.add(new KategoriItem(ad, kaynakAdi != null ? kaynakAdi : ""));
        }
        return items;
    }

    private List<Channel> gizliKanallariFiltreele(List<Channel> kanallar) {
        if (kanallar == null || kanallar.isEmpty()) return kanallar != null ? kanallar : new ArrayList<>();
        Set<String> gizliUrller = GizlePaneli.getGizliKanalUrlleri(activity);
        if (gizliUrller.isEmpty()) return kanallar;
        List<Channel> sonuc = new ArrayList<>(kanallar);
        Iterator<Channel> iter = sonuc.iterator();
        while (iter.hasNext()) {
            Channel ch = iter.next();
            if (gizliUrller.contains(ch.getUrl())) iter.remove();
        }
        System.out.println("🚫 Gizli filtre: " + kanallar.size() + " → " + sonuc.size());
        return sonuc;
    }

    interface KategoriItemListeCallback {
        void onSonuc(List<KategoriItem> kategoriler);
    }

    private void gizliKategorileriFiltreeleAsync(
            List<KategoriItem> kategoriler,
            KategoriItemListeCallback callback) {
        if (kategoriler == null || kategoriler.isEmpty()) {
            final List<KategoriItem> bos = kategoriler != null ? kategoriler : new ArrayList<>();
            mainHandler.post(() -> callback.onSonuc(bos));
            return;
        }
        Set<String> gizliKategoriler = GizlePaneli.getGizliKategoriler(activity);
        Set<String> gizliUrller      = GizlePaneli.getGizliKanalUrlleri(activity);
        List<M3UItem> tumM3U         = m3uManager.getAllM3UItems();
        if (gizliKategoriler.isEmpty()) {
            mainHandler.post(() -> callback.onSonuc(kategoriler));
            return;
        }
        new Thread(() -> {
            List<KategoriItem> sirali = new ArrayList<>();
            try {
                for (KategoriItem item : kategoriler) {
                    String kategori  = item.getKategoriAdi();
                    String kaynakAdi = item.getKaynakAdi();
                    boolean tumM3UdaGizli = true;
                    for (M3UItem m3uItem : tumM3U) {
                        if (!kaynakAdi.isEmpty() && !kaynakAdi.equals(m3uItem.getName())) continue;
                        if (!gizliKategoriler.contains(m3uItem.getName() + "|" + kategori)) {
                            tumM3UdaGizli = false;
                            break;
                        }
                    }
                    if (!tumM3UdaGizli) {
                        sirali.add(item);
                    }
                }
            } catch (Exception e) {
                System.out.println("❌ gizliKategorileriFiltreeleAsync: " + e.getMessage());
            }
            System.out.println("🚫 Kategori gizli filtre (async): " + kategoriler.size() + " → " + sirali.size());
            mainHandler.post(() -> callback.onSonuc(sirali));
        }).start();
    }

    private List<KategoriItem> gizliKategorileriFiltreeleSync(List<KategoriItem> kategoriler) {
        if (kategoriler == null || kategoriler.isEmpty())
            return kategoriler != null ? kategoriler : new ArrayList<>();
        Set<String> gizliKategoriler = GizlePaneli.getGizliKategoriler(activity);
        if (gizliKategoriler.isEmpty()) return kategoriler;
        List<M3UItem> tumM3U = m3uManager.getAllM3UItems();
        List<KategoriItem> sonuc = new ArrayList<>();
        for (KategoriItem item : kategoriler) {
            for (M3UItem m3uItem : tumM3U) {
                if (!gizliKategoriler.contains(m3uItem.getName() + "|" + item.getKategoriAdi())) {
                    sonuc.add(item);
                    break;
                }
            }
        }
        System.out.println("🚫 Kategori gizli filtre (sync): " + kategoriler.size() + " → " + sonuc.size());
        return sonuc;
    }

    private void ayarlarPaneliniKapat() {
        ayarlarPaneli.gizle();
        if (sanalDpad != null) sanalDpad.gizle();
        View panelView = ayarlarPaneli.getPanelView();
        if (panelView != null) { panelView.setVisibility(View.GONE); panelView.clearFocus(); }
        ayarlarPaneli.anaMenuyeDon();
        if (dpad != null) dpad.panelDurumuSifirla();
        mainHandler.postDelayed(() -> {
            View contentView = activity.findViewById(android.R.id.content);
            if (contentView != null) contentView.requestFocus();
        }, 100);
        kutuGorunurlugunuGuncelle();
    }

    private void resetMediaPlayer() {
        if (streamMonitor != null) streamMonitor.stop();
        if (mediaPlayer != null && videoLayout != null) {
            mediaPlayer.stop();
            if (currentMedia != null) { currentMedia.release(); currentMedia = null; }
            mediaPlayer.detachViews();
            try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }
            mediaPlayer.attachViews(videoLayout, null, false, false);
        }
        aktifStreamUrl = null;
    }

    private String readM3UFromFile(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) return null;
            BufferedReader reader  = new BufferedReader(new FileReader(file));
            StringBuilder  builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) builder.append(line).append("\n");
            reader.close();
            return builder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void m3uSilindiktenSonraGuncelle() {
        List<M3UItem> tumM3UListesi = m3uManager.getAllM3UItems();
        resetMediaPlayer();
        aktifOynaticSourceName = null;
        KategoriCache.getInstance().invalidateAll();
        DiziCache.getInstance().invalidateAll();
        diziPaneli.m3uDegisti();
        diziPaneli.switchToNormalMode();
        if (kanalPaneli.getRecyclerView()    != null) kanalPaneli.getRecyclerView().clearFocus();
        if (diziPaneli.getRecyclerView()     != null) diziPaneli.getRecyclerView().clearFocus();
        if (kategoriPaneli.getRecyclerView() != null) kategoriPaneli.getRecyclerView().clearFocus();
        if (tumM3UListesi.isEmpty()) {
            tumKanallar      = new ArrayList<>();
            filtreliKanallar = new ArrayList<>();
            if (kanalPaneli    != null) kanalPaneli.kanallarıGuncelle(tumKanallar);
            if (kategoriPaneli != null) kategoriPaneli.kategorileriGuncelle(new ArrayList<>());
            if (metadataPanel  != null) metadataPanel.gizle();
            Toast.makeText(activity,
                    "Tüm M3U listeleri silindi. Yeni M3U yüklemek için Ayarlar → M3U Yönetimi'ni kullanın.",
                    Toast.LENGTH_LONG).show();
            zorunluAcilisModunuBaslat();
            kutuGorunurlugunuGuncelle();
            return;
        }
        aktifIcerikTipi = Icerikpaneli.TIP_CANLI_TV;
        icerikPaneli.setAktifTip(aktifIcerikTipi);
        if (dpad != null) dpad.setAktifIcerikTipi(aktifIcerikTipi);
        if (oynaticiBar != null) oynaticiBar.setAktifIcerikTipi(aktifIcerikTipi);
        kategoriPaneli.setAktifIcerikTipi(aktifIcerikTipi);
        loadChannelsFromDatabase();
        kutuGorunurlugunuGuncelle();
    }

    private void oynatKanal(Channel kanal) {
        if (kanal == null || mediaPlayer == null || libVLC == null) return;

        if (kanal.getUrl() != null &&
                (kanal.getUrl().startsWith("vod:") ||
                        kanal.getUrl().startsWith("series:") ||
                        kanal.getUrl().startsWith("http://localhost/") ||
                        kanal.getUrl().startsWith("https://localhost/"))) {
            stalkerLinkCozVeOynat(kanal);
            return;
        }
        if (dpad != null) dpad.setAktifIcerikTipi(kanal.getContentType());
        mevcutBolumPozisyonuKaydet();
        mevcutFilmPozisyonuKaydet();
        zorunluAcilisDongusuIptal();
        if (oynaticiBar != null) oynaticiBar.setAktifIcerikTipi(kanal.getContentType());
        aktifIcerikTipi        = kanal.getContentType();
        aktifOynaticSourceName = kanal.getSourceName();
        aktifStreamUrl         = kanal.getUrl();
        if (!stalkerCozumdenGeliyor) {
            aktifDbUrl = null;
        }
        stalkerCozumdenGeliyor = false;
        aktifKanalAdi          = kanal.getName() != null ? kanal.getName() : "";
        if (Icerikpaneli.TIP_FILM.equals(kanal.getContentType())) {
            sonFilmUrl = kanal.getUrl();
        }
        try {
            if (metadataPanel != null) metadataPanel.gizle();
            if (streamMonitor != null) streamMonitor.stop();
            streamHataBildirimiGizle();
            yuklemeOverlayGoster(aktifKanalAdi);
            streamTimeoutBaslat(kanal.getUrl());
            mediaPlayer.stop();
            if (currentMedia != null) { currentMedia.release(); currentMedia = null; }
            currentMedia = new Media(libVLC, Uri.parse(kanal.getUrl()));
            mediaPlayer.setMedia(currentMedia);
            mediaPlayer.play();

            if (aspectRatioManager != null) {
                aspectRatioManager.refreshAspectRatio();
            }
            if (streamMonitor != null) {
                if (Icerikpaneli.TIP_CANLI_TV.equals(kanal.getContentType())) {
                    streamMonitor.startWithIdle(kanal.getUrl());
                } else {
                    streamMonitor.stop();
                }
            }
            if (oynaticiBar != null) oynaticiBar.applyMuteState();
            if (oynaticiBar != null) {
                oynaticiBar.setIcerikAdi(kanal.getName());
            }
            saveLastPlayedUrl(kanal.getUrl());
            if (Icerikpaneli.TIP_FILM.equals(kanal.getContentType())) {
                otomatikKayitBaslat();
            } else {
                otomatikKayitDurdur();
            }
        } catch (Exception e) {
            System.out.println("❌ Oynatma hatası: " + e.getMessage());
            streamTimeoutIptal();
            yuklemeOverlayGizle();
            streamHataBildirimiGoster(aktifKanalAdi, "Yayın açılamadı.");
        }
        kutuGorunurlugunuGuncelle();
        if (oynaticiBar != null) oynaticiBar.favoriDurumunuGuncelle(aktifIcerikFavoriMi());
    }
    private void stalkerLinkCozVeOynat(Channel kanal) {
        String sourceName = kanal.getSourceName();
        String placeholder = kanal.getUrl();
        yuklemeOverlayGoster(kanal.getName());

        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(activity);
                StalkerPortalDao portalDao = db.stalkerPortalDao();
                List<StalkerPortal> portals = portalDao.getActivePortals();

                StalkerPortal hedefPortal = null;
                for (StalkerPortal p : portals) {
                    if (sourceName.equals("Stalker:" + p.getName())
                            || sourceName.equals(p.getName())) {
                        hedefPortal = p;
                        break;
                    }
                }

                if (hedefPortal == null) {
                    mainHandler.post(() -> {
                        yuklemeOverlayGizle();
                        streamHataBildirimiGoster(kanal.getName(), "Stalker portal bulunamadı.");
                    });
                    return;
                }

                final StalkerPortal portal = hedefPortal;


                String token = StalkerTokenCache.getInstance().getToken(portal.getId());
                if (token == null || token.isEmpty()) {
                    org.json.JSONObject hs = StalkerApiClient.handshakeSync(
                            portal.getPortalUrl(), portal.getMacAddress());
                    org.json.JSONObject js = hs.optJSONObject("js");
                    if (js != null) token = js.optString("token", "");
                    if (token != null && !token.isEmpty()) {
                        StalkerTokenCache.getInstance().put(portal.getId(), token,
                                System.currentTimeMillis() + 3_600_000L);
                    }
                }

                if (token == null || token.isEmpty()) {
                    mainHandler.post(() -> {
                        yuklemeOverlayGizle();
                        streamHataBildirimiGoster(kanal.getName(), "Stalker token alınamadı.");
                    });
                    return;
                }

                String gercekUrl;
                String itemId = placeholder.contains(":")
                        ? placeholder.substring(placeholder.indexOf(':') + 1) : "";
                if (placeholder.startsWith("vod:")) {
                    String cmd = "ffmpeg http://localhost/vod/" + itemId;
                    gercekUrl = StalkerApiClient.createVodLinkSync(
                            portal.getPortalUrl(), token, portal.getMacAddress(), cmd);
                } else if (placeholder.startsWith("series:")) {
                    String cmd = "ffmpeg http://localhost/series/" + itemId;
                    gercekUrl = StalkerApiClient.createSeriesLinkSync(
                            portal.getPortalUrl(), token, portal.getMacAddress(), cmd);
                } else {

                    String cmd = "ffmpeg " + placeholder;
                    gercekUrl = StalkerApiClient.createLinkSync(
                            portal.getPortalUrl(), token, portal.getMacAddress(), cmd);
                }

                gercekUrl = StalkerApiClient.extractUrl(gercekUrl);

                if (gercekUrl == null || gercekUrl.isEmpty()
                        || gercekUrl.startsWith("vod:") || gercekUrl.startsWith("series:")) {
                    mainHandler.post(() -> {
                        yuklemeOverlayGizle();
                        streamHataBildirimiGoster(kanal.getName(), "Stream URL çözülemedi.");
                    });
                    return;
                }

                final String finalUrl = gercekUrl;

                Channel cozulmus = new Channel(kanal.getName(), finalUrl,
                        kanal.getCategory(), kanal.getLogo(), kanal.getSourceName());
                cozulmus.setContentType(kanal.getContentType());

                mainHandler.post(() -> {
                    aktifDbUrl = placeholder;
                    stalkerCozumdenGeliyor = true;
                    yuklemeOverlayGizle();
                    oynatKanal(cozulmus);
                });
            } catch (Exception e) {
                System.out.println("❌ [STALKER] Link çözme hatası: " + e.getMessage());
                mainHandler.post(() -> {
                    yuklemeOverlayGizle();
                    streamHataBildirimiGoster(kanal.getName(),
                            "Stalker bağlantı hatası: " + e.getMessage());
                });
            }
        }).start();
    }

    private void oynatUrl(String url, long baslangicPozisyonMs) {
        if (url == null || url.isEmpty() || mediaPlayer == null || libVLC == null) return;
        if (streamMonitor != null) streamMonitor.stop();

        aktifStreamUrl = url;
        try {
            if (metadataPanel != null) metadataPanel.gizle();
            streamHataBildirimiGizle();
            yuklemeOverlayGoster(aktifKanalAdi);
            streamTimeoutBaslat(url);
            mediaPlayer.stop();
            if (currentMedia != null) { currentMedia.release(); currentMedia = null; }
            currentMedia = new Media(libVLC, Uri.parse(url));
            mediaPlayer.setMedia(currentMedia);
            mediaPlayer.play();

            if (aspectRatioManager != null) {
                aspectRatioManager.refreshAspectRatio();
            }
            if (oynaticiBar != null) oynaticiBar.applyMuteState();
            if (baslangicPozisyonMs > 0) {
                mediaPlayer.setTime(baslangicPozisyonMs);
                bekleyenBaslangicPozisyonu = baslangicPozisyonMs;
            } else {
                bekleyenBaslangicPozisyonu = 0;
            }
            saveLastPlayedUrl(url);
            otomatikKayitBaslat();
        } catch (Exception e) {
            System.out.println("❌ URL oynatma hatası: " + e.getMessage());
            streamTimeoutIptal();
            yuklemeOverlayGizle();
            streamHataBildirimiGoster("", "Yayın açılamadı.");
        }
        kutuGorunurlugunuGuncelle();
        if (oynaticiBar != null) oynaticiBar.favoriDurumunuGuncelle(aktifIcerikFavoriMi());
    }

    private void saveLastPlayedUrl(String url) {
        try {
            activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putString(KEY_LAST_PLAYED_URL, url).apply();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String getLastPlayedUrl() {
        try {
            return activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getString(KEY_LAST_PLAYED_URL, null);
        } catch (Exception e) { return null; }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (ayarlarPaneli != null) ayarlarPaneli.onActivityResult(requestCode, resultCode, data);
    }

    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN && dpadModu) {

            dpadModu = false;
            if (dpad != null) dpad.pasifYap();
            if (dokunmatik != null) dokunmatik.aktifYap();
            if (sanalDpad != null) sanalDpad.gizle();
            kutuGorunurlugunuGuncelle();
        }
        if (dokunmatik != null) return dokunmatik.dispatchTouchEvent(ev);
        return false;
    }
    public void ayarlarPaneliAc() {
        if (dpad != null) {
            dpad.ayarlarPaneliAc();
        }
    }
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event == null) {
            return onKeyDown(keyCode, new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
        }
        if ((keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE)
                && ayarlarPaneli != null && ayarlarPaneli.isVisible()) {
            boolean handled = ayarlarPaneli.handleBack();
            if (!handled) {
                ayarlarPaneli.gizle();
                if (sanalDpad != null) sanalDpad.gizle();
                if (dpad != null) dpad.panelDurumuSifirla();
            }
            return true;
        }

        if (!dpadModu && keyCode != KeyEvent.KEYCODE_BACK
                && keyCode != KeyEvent.KEYCODE_VOLUME_UP
                && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN
                && keyCode != KeyEvent.KEYCODE_VOLUME_MUTE) {
            dpadModu = true;
            if (dokunmatik != null) dokunmatik.pasifYap();
            if (dpad != null) dpad.aktifYap();
            if (sanalDpad != null) sanalDpad.gizle();
            kutuGorunurlugunuGuncelle();
        }
        if (dpad != null) {
            if (!dpadModu && keyCode == KeyEvent.KEYCODE_BACK) {
                return dpad.handleBack_dokunmatikMod();
            }
            return dpad.handleKeyDown(keyCode, event);
        }
        return false;
    }

    public void onPictureInPictureModeChanged(boolean isInPiPMode) {
        if (oynaticiBar != null) oynaticiBar.onPictureInPictureModeChanged(isInPiPMode);
    }

    private void aramaKanalPaneliAc(Channel kanal) {
        if (kanal == null) return;
        String tip      = kanal.getContentType() != null ? kanal.getContentType() : Icerikpaneli.TIP_CANLI_TV;
        String kategori = kanal.getCategory();
        aktifIcerikTipi = tip;
        icerikPaneli.setAktifTip(tip);
        if (dpad != null) dpad.setAktifIcerikTipi(tip);
        if (oynaticiBar != null) oynaticiBar.setAktifIcerikTipi(tip);
        channelRepository.loadCategoriesWithSource(null, tip,
                new ChannelRepository.OnKategoriItemsLoadedListener() {
                    @Override
                    public void onKategoriItemsLoaded(List<KategoriItem> items) {
                        gizliKategorileriFiltreeleAsync(items, filtrelenmis -> {
                            KategoriCache.getInstance().put(tip, filtrelenmis);
                            kategoriPaneli.setAktifIcerikTipi(tip);
                            kategoriPaneli.kategorileriGuncelle(filtrelenmis);
                            if (kategori != null) kategoriPaneli.setAktifKategori(kategori);
                            aktifKategori = kategori;
                            channelRepository.loadChannelsByTypeAndCategory(null, tip, kategori,
                                    new ChannelRepository.OnChannelsLoadedListener() {
                                        @Override
                                        public void onChannelsLoaded(List<Channel> channels, boolean fromCache) {
                                            mainHandler.post(() -> {
                                                List<Channel> gorunenler = gizliKanallariFiltreele(channels);
                                                tumKanallar      = gorunenler;
                                                filtreliKanallar = new ArrayList<>(gorunenler);
                                                kanalPaneli.setDbSayfaCallback((offset, sonuc) ->
                                                        channelRepository.loadChannelsByTypeAndCategoryPaged(
                                                                null, aktifIcerikTipi, aktifKategori, offset,
                                                                new ChannelRepository.OnChannelsLoadedListener() {
                                                                    public void onChannelsLoaded(List<Channel> ch, boolean fc) {
                                                                        boolean bitti = ch.size() < 50;
                                                                        List<Channel> g = gizliKanallariFiltreele(ch);
                                                                        mainHandler.post(() -> sonuc.sayfaGeldi(g, bitti));
                                                                    }
                                                                    @Override public void onError(Exception e) {
                                                                        mainHandler.post(() -> sonuc.sayfaGeldi(new ArrayList<>(), true));
                                                                    }
                                                                }));
                                                kanalPaneli.kanallarıGuncelle(gorunenler);
                                                if (dpad != null) dpad.kanalPaneliAramadanAc(kanal);
                                                kutuGorunurlugunuGuncelle();
                                            });
                                        }
                                        @Override public void onError(Exception e) {}
                                    });
                        });
                    }
                    @Override public void onError(Exception e) {}
                });
    }

    private void aramaDiziPaneliAc(String diziAdi, Channel temsilKanal) {
        if (diziAdi == null) return;
        String kategori = temsilKanal != null ? temsilKanal.getCategory() : null;
        aktifIcerikTipi = Icerikpaneli.TIP_DIZI;
        icerikPaneli.setAktifTip(Icerikpaneli.TIP_DIZI);
        if (dpad != null) dpad.setAktifIcerikTipi(Icerikpaneli.TIP_DIZI);
        if (oynaticiBar != null) oynaticiBar.setAktifIcerikTipi(Icerikpaneli.TIP_DIZI);
        channelRepository.loadCategoriesWithSource(null, "SERIES",
                new ChannelRepository.OnKategoriItemsLoadedListener() {
                    @Override
                    public void onKategoriItemsLoaded(List<KategoriItem> items) {
                        gizliKategorileriFiltreeleAsync(items, filtrelenmis -> {
                            KategoriCache.getInstance().put("SERIES", filtrelenmis);
                            kategoriPaneli.setAktifIcerikTipi(Icerikpaneli.TIP_DIZI);
                            kategoriPaneli.kategorileriGuncelle(filtrelenmis);
                            if (kategori != null) kategoriPaneli.setAktifKategori(kategori);
                            diziPaneli.switchToSeriesMode(null, kategori);
                            mainHandler.postDelayed(() -> {
                                if (dpad != null) dpad.diziPaneliAramadanAc(diziAdi);
                            }, 400);
                            kutuGorunurlugunuGuncelle();
                        });
                    }
                    @Override public void onError(Exception e) {
                        mainHandler.post(() -> {
                            diziPaneli.switchToSeriesMode(null, kategori);
                            mainHandler.postDelayed(() -> {
                                if (dpad != null) dpad.diziPaneliAramadanAc(diziAdi);
                            }, 400);
                            kutuGorunurlugunuGuncelle();
                        });
                    }
                });
    }

    public void panelAcilirkenAktifOgeYukle() {
        if (aktifStreamUrl == null || aktifStreamUrl.isEmpty()) {
            ilkAcilisOtomatikOynat = true;
            loadDefaultChannels();
            kutuGorunurlugunuGuncelle();
            return;
        }
        String aramaUrl = (aktifDbUrl != null && !aktifDbUrl.isEmpty()) ? aktifDbUrl : aktifStreamUrl;
        channelRepository.loadChannelByUrl(aramaUrl,
                new ChannelRepository.OnChannelLoadedListener() {
                    @Override
                    public void onChannelLoaded(Channel channel) {
                        mainHandler.post(() -> {
                            if (channel == null) {
                                ilkAcilisOtomatikOynat = true;
                                loadDefaultChannels();
                                kutuGorunurlugunuGuncelle();
                                return;
                            }
                            String tip      = channel.getContentType();
                            String kategori = channel.getCategory();
                            List<Channel> tekli = new ArrayList<>();
                            tekli.add(channel);
                            List<Channel> gorunen = gizliKanallariFiltreele(tekli);
                            if (gorunen.isEmpty()) {
                                ilkAcilisOtomatikOynat = true;
                                loadDefaultChannels();
                                kutuGorunurlugunuGuncelle();
                                return;
                            }
                            aktifIcerikTipi = tip;
                            icerikPaneli.setAktifTip(tip);
                            if (dpad != null) dpad.setAktifIcerikTipi(tip);
                            if (oynaticiBar != null) oynaticiBar.setAktifIcerikTipi(tip);
                            if (Icerikpaneli.TIP_DIZI.equals(tip)) {
                                String diziAdiTemp;
                                try {
                                    com.example.livetvapp.database.DiziParser.DiziBilgisi bilgi =
                                            com.example.livetvapp.database.DiziParser.extractDiziBilgisi(channel.getName());
                                    diziAdiTemp = (bilgi.getDiziAdi() != null && !bilgi.getDiziAdi().isEmpty())
                                            ? bilgi.getDiziAdi() : channel.getName();
                                } catch (Exception e) {
                                    diziAdiTemp = channel.getName();
                                }
                                final String finalDiziAdi = diziAdiTemp;
                                channelRepository.loadCategoriesWithSource(null, "SERIES",
                                        new ChannelRepository.OnKategoriItemsLoadedListener() {
                                            @Override
                                            public void onKategoriItemsLoaded(List<KategoriItem> items) {
                                                gizliKategorileriFiltreeleAsync(items, filtrelenmis -> {
                                                    KategoriCache.getInstance().put("SERIES", filtrelenmis);
                                                    kategoriPaneli.setAktifIcerikTipi(Icerikpaneli.TIP_DIZI);
                                                    kategoriPaneli.kategorileriGuncelle(filtrelenmis);
                                                    if (kategori != null)
                                                        kategoriPaneli.setAktifKategori(kategori);
                                                    diziPaneli.switchToSeriesMode(null, kategori);
                                                    mainHandler.postDelayed(() -> {
                                                        if (dpad != null)
                                                            dpad.diziPaneliAramadanAc(finalDiziAdi);
                                                    }, 400);
                                                    kutuGorunurlugunuGuncelle();
                                                });
                                            }
                                            @Override public void onError(Exception e) {
                                                ilkAcilisOtomatikOynat = true;
                                                loadDefaultChannels();
                                                kutuGorunurlugunuGuncelle();
                                            }
                                        });
                            } else {
                                channelRepository.loadCategoriesWithSource(null, tip,
                                        new ChannelRepository.OnKategoriItemsLoadedListener() {
                                            @Override
                                            public void onKategoriItemsLoaded(List<KategoriItem> items) {
                                                gizliKategorileriFiltreeleAsync(items, filtrelenmis -> {
                                                    KategoriCache.getInstance().put(tip, filtrelenmis);
                                                    kategoriPaneli.setAktifIcerikTipi(tip);
                                                    kategoriPaneli.kategorileriGuncelle(filtrelenmis);
                                                    if (kategori != null) {
                                                        if (kanalFavoridenAcildi) {
                                                            kategoriPaneli.setAktifKategori(FavorilerYoneticisi.FAVORI_KATEGORI_ADI);
                                                            aktifKategori = FavorilerYoneticisi.FAVORI_KATEGORI_ADI;
                                                            favorileriYukle();
                                                            if (dpad != null) dpad.kanalPaneliAramadanAc(channel);
                                                            kutuGorunurlugunuGuncelle();
                                                            return;
                                                        }
                                                        kategoriPaneli.setAktifKategori(kategori);
                                                        List<KategoriItem> gorunurKat = KategoriCache.getInstance().get(tip);
                                                        boolean kategoriGorunur = false;
                                                        if (gorunurKat != null) {
                                                            for (KategoriItem k : gorunurKat) {
                                                                if (kategori.equals(k.getKategoriAdi())) { kategoriGorunur = true; break; }
                                                            }
                                                        }
                                                        if (gorunurKat != null && !kategoriGorunur) {
                                                            tumKanallar      = new ArrayList<>();
                                                            filtreliKanallar = new ArrayList<>();
                                                            kanalPaneli.setDbSayfaCallback(null);
                                                            if (kanalPaneli.getAdapter() != null) kanalPaneli.getAdapter().setKanallar(new ArrayList<>());
                                                            System.out.println("🚫 panelAcilirken: kategori gizli, kanal listesi boş bırakıldı");
                                                            if (dpad != null) dpad.kanalPaneliAramadanAc(channel);
                                                            kutuGorunurlugunuGuncelle();
                                                            return;
                                                        }
                                                        aktifKategori = kategori;
                                                    }
                                                    channelRepository.loadChannelsByTypeAndCategory(
                                                            null, tip, kategori,
                                                            new ChannelRepository.OnChannelsLoadedListener() {
                                                                @Override
                                                                public void onChannelsLoaded(List<Channel> channels, boolean fromCache) {
                                                                    mainHandler.post(() -> {
                                                                        List<Channel> gorunenler = gizliKanallariFiltreele(channels);
                                                                        tumKanallar      = gorunenler;
                                                                        filtreliKanallar = new ArrayList<>(gorunenler);
                                                                        kanalPaneli.setDbSayfaCallback(null);
                                                                        kanalPaneli.kanallarıGuncelle(gorunenler);
                                                                        if (dpad != null)
                                                                            dpad.kanalPaneliAramadanAc(channel);
                                                                        kutuGorunurlugunuGuncelle();
                                                                    });
                                                                }
                                                                @Override public void onError(Exception e) {
                                                                    ilkAcilisOtomatikOynat = true;
                                                                    loadDefaultChannels();
                                                                    kutuGorunurlugunuGuncelle();
                                                                }
                                                            });
                                                });
                                            }
                                            @Override public void onError(Exception e) {
                                                ilkAcilisOtomatikOynat = true;
                                                loadDefaultChannels();
                                                kutuGorunurlugunuGuncelle();
                                            }
                                        });
                            }
                        });
                    }
                    @Override public void onError(Exception e) {
                        mainHandler.post(() -> {
                            ilkAcilisOtomatikOynat = true;
                            loadDefaultChannels();
                            kutuGorunurlugunuGuncelle();
                        });
                    }
                });
    }

    public void onResume() {


        boolean inPiP = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N
                && activity.isInPictureInPictureMode();
        if (!inPiP && mediaPlayer != null && videoLayout != null
                && videoLayout.getWidth() > 0 && videoLayout.getHeight() > 0) {
            try {
                mediaPlayer.detachViews();
            } catch (Exception ignored) {}
            mediaPlayer.attachViews(videoLayout, null, false, false);
        }
        kutuGorunurlugunuGuncelle();
    }

    public void temizlikYap() {
        mevcutBolumPozisyonuKaydet();
        mevcutFilmPozisyonuKaydet();
        zorunluAcilisDongusuIptal();
        otomatikKayitDurdur();
        streamTimeoutIptal();
        yuklemeOverlayGizle();
        streamHataBildirimiGizle();
        if (mediaPlayer    != null) mediaPlayer.setEventListener(null);
        if (streamMonitor  != null) { streamMonitor.cleanup();  streamMonitor  = null; }
        if (metadataPanel  != null) { metadataPanel.temizle();  metadataPanel  = null; }
        if (dokunmatik     != null) { dokunmatik.temizle();     dokunmatik     = null; }
        if (oynaticiBar    != null) { oynaticiBar.temizle();    oynaticiBar    = null; }
        if (currentMedia   != null) { currentMedia.release();   currentMedia   = null; }
        if (mediaPlayer    != null) { mediaPlayer.stop(); mediaPlayer.detachViews(); mediaPlayer.release(); mediaPlayer = null; }
        if (libVLC         != null) { libVLC.release();         libVLC         = null; }
    }

    private void mevcutBolumPozisyonuKaydet() {
        if (sonDiziUrl == null || sonDiziUrl.isEmpty()) return;
        if (mediaPlayer == null) return;
        if (bekleyenBaslangicPozisyonu > 0) {
            long anlikPozisyon = oynaticiBar != null
                    ? oynaticiBar.getCurrentPositionMs()
                    : Math.max(0, mediaPlayer.getTime());
            if (Math.abs(anlikPozisyon - bekleyenBaslangicPozisyonu) > 10_000) {
                System.out.println("⏳ Pozisyon henüz stabil değil, kayıt atlandı: "
                        + anlikPozisyon + "ms (beklenen≈" + bekleyenBaslangicPozisyonu + "ms)");
                return;
            } else {
                bekleyenBaslangicPozisyonu = 0;
            }
        }
        long pozisyon = oynaticiBar != null
                ? oynaticiBar.getCurrentPositionMs()
                : Math.max(0, mediaPlayer.getTime());
        long sure = mediaPlayer.getLength();
        if (sure > 0 && pozisyon >= sure - 30_000) {
            final String url = sonDiziUrl;
            new Thread(() -> {
                try {
                    AppDatabase.getInstance(activity).channelDao().pozisyonSil(url);
                } catch (Exception e) { }
            }).start();
            return;
        }
        if (pozisyon < 30_000) return;
        final String kayitUrl      = sonDiziUrl;
        final long   kayitPozisyon = pozisyon;
        new Thread(() -> {
            try {
                AppDatabase.getInstance(activity).channelDao()
                        .pozisyonKaydet(new IzlemePozisyonu(kayitUrl, kayitPozisyon));
                System.out.println("💾 Pozisyon kaydedildi: " + kayitPozisyon + "ms → " + kayitUrl);
            } catch (Exception e) {
                System.out.println("❌ Pozisyon kayıt hatası: " + e.getMessage());
            }
        }).start();
    }

    private void bolumOynatPozisyonKontrollu(
            com.example.livetvapp.Adapter.Dizi dizi,
            com.example.livetvapp.Adapter.Sezon sezon,
            com.example.livetvapp.Adapter.Bolum bolum,
            String barMetni) {
        mevcutBolumPozisyonuKaydet();
        final String url = bolum.getUrl();
        sonDiziUrl = url;
        new Thread(() -> {
            IzlemePozisyonu kayit = null;
            try {
                kayit = AppDatabase.getInstance(activity).channelDao().pozisyonGetir(url);
            } catch (Exception e) {
                System.out.println("❌ Pozisyon okuma hatası: " + e.getMessage());
            }
            final IzlemePozisyonu sonKayit = kayit;
            mainHandler.post(() -> {
                if (sonKayit != null && sonKayit.pozisyonMs > 30_000) {
                    String sure = formatSure(sonKayit.pozisyonMs);
                    new androidx.appcompat.app.AlertDialog.Builder(activity)
                            .setTitle("Kaldığın Yerden Devam Et")
                            .setMessage(barMetni + "\n\n" + sure + " konumundan devam etmek ister misin?")
                            .setPositiveButton("Devam Et", (d, w) -> {
                                aktifKanalAdi = barMetni;
                                oynatUrl(url, sonKayit.pozisyonMs);
                            })
                            .setNegativeButton("Baştan Başla", (d, w) -> {
                                new Thread(() -> {
                                    try {
                                        AppDatabase.getInstance(activity).channelDao().pozisyonSil(url);
                                    } catch (Exception ignored) {}
                                }).start();
                                aktifKanalAdi = barMetni;
                                oynatUrl(url, 0);
                            })
                            .setCancelable(false)
                            .show();
                } else {
                    aktifKanalAdi = barMetni;
                    oynatUrl(url, 0);
                }
            });
        }).start();
    }

    private String formatSure(long ms) {
        long toplamSaniye = ms / 1000;
        long saat   = toplamSaniye / 3600;
        long dakika = (toplamSaniye % 3600) / 60;
        long saniye = toplamSaniye % 60;
        if (saat > 0) {
            return String.format("%d:%02d:%02d", saat, dakika, saniye);
        }
        return String.format("%d:%02d", dakika, saniye);
    }

    private void mevcutFilmPozisyonuKaydet() {
        if (sonFilmUrl == null || sonFilmUrl.isEmpty()) return;
        if (mediaPlayer == null) return;
        if (bekleyenBaslangicPozisyonu > 0) {
            long anlikPozisyon = oynaticiBar != null
                    ? oynaticiBar.getCurrentPositionMs()
                    : Math.max(0, mediaPlayer.getTime());
            if (Math.abs(anlikPozisyon - bekleyenBaslangicPozisyonu) > 10_000) {
                System.out.println("⏳ Pozisyon henüz stabil değil, kayıt atlandı: "
                        + anlikPozisyon + "ms (beklenen≈" + bekleyenBaslangicPozisyonu + "ms)");
                return;
            } else {
                bekleyenBaslangicPozisyonu = 0;
            }
        }
        long pozisyon = oynaticiBar != null
                ? oynaticiBar.getCurrentPositionMs()
                : Math.max(0, mediaPlayer.getTime());
        long sure = mediaPlayer.getLength();
        if (sure > 0 && pozisyon >= sure - 30_000) {
            final String url = sonFilmUrl;
            new Thread(() -> {
                try {
                    AppDatabase.getInstance(activity).channelDao().pozisyonSil(url);
                } catch (Exception e) {  }
            }).start();
            return;
        }
        if (pozisyon < 30_000) return;
        final String kayitUrl      = sonFilmUrl;
        final long   kayitPozisyon = pozisyon;
        new Thread(() -> {
            try {
                AppDatabase.getInstance(activity).channelDao()
                        .pozisyonKaydet(new IzlemePozisyonu(kayitUrl, kayitPozisyon));
                System.out.println("💾 Film pozisyon kaydedildi: " + kayitPozisyon + "ms → " + kayitUrl);
            } catch (Exception e) {
                System.out.println("❌ Film pozisyon kayıt hatası: " + e.getMessage());
            }
        }).start();
    }

    private void diziAcilisOynatPozisyonKontrollu(Channel kanal) {
        if (kanal == null) return;
        final String url     = kanal.getUrl();
        final String diziAdi = kanal.getName() != null ? kanal.getName() : "";
        sonDiziUrl = url;


        String barMetni;
        try {
            com.example.livetvapp.database.DiziParser.DiziBilgisi bilgi =
                    com.example.livetvapp.database.DiziParser.extractDiziBilgisi(diziAdi);
            if (bilgi.getDiziAdi() != null && !bilgi.getDiziAdi().isEmpty()
                    && bilgi.getSezonNo() > 0 && bilgi.getBolumNo() > 0) {
                barMetni = bilgi.getDiziAdi()
                        + " · S" + String.format("%02d", bilgi.getSezonNo())
                        + " E" + String.format("%02d", bilgi.getBolumNo());
            } else {
                barMetni = diziAdi;
            }
        } catch (Exception e) {
            barMetni = diziAdi;
        }
        final String finalBarMetni = barMetni;

        new Thread(() -> {
            IzlemePozisyonu kayit = null;
            try {
                kayit = AppDatabase.getInstance(activity).channelDao().pozisyonGetir(url);
            } catch (Exception e) {
                System.out.println("❌ Dizi açılış pozisyon okuma hatası: " + e.getMessage());
            }
            final IzlemePozisyonu sonKayit = kayit;
            mainHandler.post(() -> {

                if (oynaticiBar != null) oynaticiBar.setIcerikAdi(finalBarMetni);

                if (sonKayit != null && sonKayit.pozisyonMs > 30_000) {
                    String sure = formatSure(sonKayit.pozisyonMs);
                    new androidx.appcompat.app.AlertDialog.Builder(activity)
                            .setTitle("Kaldığın Yerden Devam Et")
                            .setMessage(finalBarMetni + "\n\n" + sure + " konumundan devam etmek ister misin?")
                            .setPositiveButton("Devam Et", (d, w) -> {
                                aktifKanalAdi = finalBarMetni;
                                oynatUrl(url, sonKayit.pozisyonMs);
                            })
                            .setNegativeButton("Baştan Başla", (d, w) -> {
                                new Thread(() -> {
                                    try {
                                        AppDatabase.getInstance(activity).channelDao().pozisyonSil(url);
                                    } catch (Exception ignored) {}
                                }).start();
                                aktifKanalAdi = finalBarMetni;
                                oynatUrl(url, 0);
                            })
                            .setCancelable(false)
                            .show();
                } else {
                    aktifKanalAdi = finalBarMetni;
                    oynatUrl(url, 0);
                }
            });
        }).start();
    }

    private void filmOynatPozisyonKontrollu(Channel kanal) {
        mevcutFilmPozisyonuKaydet();
        sonFilmUrl = kanal.getUrl();
        final String url      = kanal.getUrl();
        final String filmAdi  = kanal.getName() != null ? kanal.getName() : "";
        new Thread(() -> {
            IzlemePozisyonu kayit = null;
            try {
                kayit = AppDatabase.getInstance(activity).channelDao().pozisyonGetir(url);
            } catch (Exception e) {
                System.out.println("❌ Film pozisyon okuma hatası: " + e.getMessage());
            }
            final IzlemePozisyonu sonKayit = kayit;
            mainHandler.post(() -> {
                if (sonKayit != null && sonKayit.pozisyonMs > 30_000) {
                    String sure = formatSure(sonKayit.pozisyonMs);
                    new androidx.appcompat.app.AlertDialog.Builder(activity)
                            .setTitle("Kaldığın Yerden Devam Et")
                            .setMessage(filmAdi + "\n\n" + sure + " konumundan devam etmek ister misin?")
                            .setPositiveButton("Devam Et", (d, w) -> oynatKanalDirekt(kanal, sonKayit.pozisyonMs))
                            .setNegativeButton("Baştan Başla", (d, w) -> {
                                new Thread(() -> {
                                    try {
                                        AppDatabase.getInstance(activity).channelDao().pozisyonSil(url);
                                    } catch (Exception ignored) {}
                                }).start();
                                oynatKanalDirekt(kanal, 0);
                            })
                            .setCancelable(false)
                            .show();
                } else {
                    oynatKanalDirekt(kanal, 0);
                }
            });
        }).start();
    }

    private void oynatKanalDirekt(Channel kanal, long baslangicPozisyonMs) {
        if (kanal == null || mediaPlayer == null || libVLC == null) return;
        aktifKanalAdi = kanal.getName() != null ? kanal.getName() : "";
        try {
            if (metadataPanel != null) metadataPanel.gizle();
            if (streamMonitor != null) streamMonitor.stop();
            streamHataBildirimiGizle();
            yuklemeOverlayGoster(kanal.getName());
            streamTimeoutBaslat(kanal.getUrl());
            mediaPlayer.stop();
            if (currentMedia != null) { currentMedia.release(); currentMedia = null; }
            currentMedia = new Media(libVLC, Uri.parse(kanal.getUrl()));
            mediaPlayer.setMedia(currentMedia);
            aktifStreamUrl = kanal.getUrl();
            mediaPlayer.play();

            if (aspectRatioManager != null) {
                aspectRatioManager.refreshAspectRatio();
            }
            if (baslangicPozisyonMs > 0) {
                mediaPlayer.setTime(baslangicPozisyonMs);
            }
            if (oynaticiBar != null) oynaticiBar.setIcerikAdi(kanal.getName());
            saveLastPlayedUrl(kanal.getUrl());
            otomatikKayitBaslat();
        } catch (Exception e) {
            System.out.println("❌ Film oynatma hatası: " + e.getMessage());
            streamTimeoutIptal();
            yuklemeOverlayGizle();
            streamHataBildirimiGoster(kanal.getName(), "Yayın açılamadı.");
        }
    }

    private void otomatikKayitBaslat() {
        otomatikKayitDurdur();
        otomatikKayitRunnable = new Runnable() {
            @Override
            public void run() {
                mevcutBolumPozisyonuKaydet();
                mevcutFilmPozisyonuKaydet();
                mainHandler.postDelayed(this, OTOMATIK_KAYIT_ARALIK_MS);
            }
        };
        mainHandler.postDelayed(otomatikKayitRunnable, OTOMATIK_KAYIT_ARALIK_MS);
    }

    private void otomatikKayitDurdur() {
        if (otomatikKayitRunnable != null) {
            mainHandler.removeCallbacks(otomatikKayitRunnable);
            otomatikKayitRunnable = null;
        }
    }

    private void ayarlarBekciBaslat() {
        ayarlarBekciDurdur();
        final long bitis = System.currentTimeMillis() + BEKCI_SURE_MS;
        ayarlarBekciRunnable = new Runnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() > bitis) return;
                if (icerikPaneli   != null && icerikPaneli.isVisible())   icerikPaneli.gizle();
                if (kategoriPaneli != null && kategoriPaneli.isVisible())  kategoriPaneli.gizle();
                if (diziPaneli     != null && diziPaneli.isVisible())     diziPaneli.gizle();
                if (kanalPaneli    != null && kanalPaneli.isVisible())    kanalPaneli.gizle();
                mainHandler.postDelayed(this, BEKCI_KONTROL_MS);
            }
        };
        mainHandler.postDelayed(ayarlarBekciRunnable, BEKCI_KONTROL_MS);
    }

    private void ayarlarBekciDurdur() {
        if (ayarlarBekciRunnable != null) {
            mainHandler.removeCallbacks(ayarlarBekciRunnable);
            ayarlarBekciRunnable = null;
        }
    }

    private void vlcEventDinleyicisiBaslat() {
        vlcEventListener = event -> {
            switch (event.type) {
                case MediaPlayer.Event.Playing:
                    streamOynuyor = true;
                    kullaniciDurdurdu = false;
                    mainHandler.post(() -> {
                        streamTimeoutIptal();
                        yuklemeOverlayGizle();
                        streamHataBildirimiGizle();
                        if (streamMonitor != null) streamMonitor.onStreamPlaying();

                        if (aspectRatioManager != null) {
                            aspectRatioManager.refreshAspectRatio();
                        }
                    });
                    break;
                case MediaPlayer.Event.EncounteredError:
                    mainHandler.post(() -> {
                        streamTimeoutIptal();
                        yuklemeOverlayGizle();
                        if (streamMonitor != null) streamMonitor.onStreamError();

                    });
                    break;
                case MediaPlayer.Event.EndReached:
                    mainHandler.post(() -> {
                        streamTimeoutIptal();
                        yuklemeOverlayGizle();
                        if (streamMonitor != null) streamMonitor.onStreamError();
                    });
                    break;
            }
        };
        if (mediaPlayer != null) mediaPlayer.setEventListener(vlcEventListener);
    }

    private void streamTimeoutBaslat(String url) {
        streamOynuyor = false;
        streamTimeoutIptal();
        streamTimeoutRunnable = () -> {
            streamTimeoutRunnable = null;
            if (!streamOynuyor) {
                yuklemeOverlayGizle();
                if (streamMonitor != null) {
                    streamMonitor.onTimeoutExpired();
                } else {
                    streamHataBildirimiGoster(aktifKanalAdi,
                            "Yayın " + STREAM_TIMEOUT_MS / 1000 + " saniyede başlamadı.");
                }
            }
        };
        mainHandler.postDelayed(streamTimeoutRunnable, STREAM_TIMEOUT_MS);
    }

    private void streamTimeoutIptal() {
        if (streamTimeoutRunnable != null) {
            mainHandler.removeCallbacks(streamTimeoutRunnable);
            streamTimeoutRunnable = null;
        }
    }

    private void yuklemeOverlayGoster(String kanalAdi) {
        if (activity == null || activity.isFinishing() || videoLayout == null) return;
        yuklemeOverlayGizle();
        android.view.ViewGroup videoParent = (android.view.ViewGroup) videoLayout.getParent();
        if (videoParent == null) return;
        int[] videoLoc  = new int[2];
        int[] parentLoc = new int[2];
        videoLayout.getLocationOnScreen(videoLoc);
        videoParent.getLocationOnScreen(parentLoc);
        int left   = videoLoc[0] - parentLoc[0];
        int top    = videoLoc[1] - parentLoc[1];
        int width  = videoLayout.getWidth();
        int height = videoLayout.getHeight();
        yuklemOverlayLayout = new android.widget.FrameLayout(activity);
        android.widget.FrameLayout.LayoutParams p =
                new android.widget.FrameLayout.LayoutParams(width, height);
        p.leftMargin = left;
        p.topMargin  = top;
        yuklemOverlayLayout.setLayoutParams(p);
        yuklemOverlayLayout.setBackgroundColor(0xFF000000);
        android.widget.LinearLayout ic = new android.widget.LinearLayout(activity);
        ic.setOrientation(android.widget.LinearLayout.VERTICAL);
        ic.setGravity(android.view.Gravity.CENTER);
        android.widget.FrameLayout.LayoutParams icP =
                new android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT);
        icP.gravity = android.view.Gravity.CENTER;
        ic.setLayoutParams(icP);
        android.widget.ProgressBar spinner = new android.widget.ProgressBar(activity);
        spinner.setIndeterminate(true);
        android.widget.LinearLayout.LayoutParams spinP =
                new android.widget.LinearLayout.LayoutParams(dpToPx(36), dpToPx(36));
        spinP.gravity       = android.view.Gravity.CENTER_HORIZONTAL;
        spinP.bottomMargin  = dpToPx(14);
        spinner.setLayoutParams(spinP);
        ic.addView(spinner);
        if (kanalAdi != null && !kanalAdi.isEmpty()) {
            android.widget.TextView kanalAdView = new android.widget.TextView(activity);
            kanalAdView.setText(kanalAdi);
            kanalAdView.setTextColor(0xCCFFFFFF);
            kanalAdView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);
            kanalAdView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            kanalAdView.setGravity(android.view.Gravity.CENTER);
            android.widget.LinearLayout.LayoutParams adP =
                    new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            adP.gravity      = android.view.Gravity.CENTER_HORIZONTAL;
            adP.bottomMargin = dpToPx(6);
            kanalAdView.setLayoutParams(adP);
            ic.addView(kanalAdView);
        }
        android.widget.TextView yukleniyorText = new android.widget.TextView(activity);
        yukleniyorText.setText("Yükleniyor...");
        yukleniyorText.setTextColor(0x66FFFFFF);
        yukleniyorText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
        yukleniyorText.setGravity(android.view.Gravity.CENTER);
        android.widget.LinearLayout.LayoutParams yukP =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        yukP.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        yukleniyorText.setLayoutParams(yukP);
        ic.addView(yukleniyorText);
        yuklemOverlayLayout.addView(ic);
        yuklemOverlayLayout.setAlpha(0f);
        videoParent.addView(yuklemOverlayLayout);
        yuklemOverlayLayout.animate().alpha(1f).setDuration(150).start();
    }

    private void yuklemeOverlayGizle() {
        if (yuklemOverlayLayout == null) return;
        final android.widget.FrameLayout k = yuklemOverlayLayout;
        yuklemOverlayLayout = null;
        k.animate().alpha(0f).setDuration(200).withEndAction(() -> {
            try {
                android.view.ViewGroup par = (android.view.ViewGroup) k.getParent();
                if (par != null) par.removeView(k);
            } catch (Exception ignored) {}
        }).start();
    }

    private void streamHataBildirimiGoster(String kanalAdi, String mesaj) {
        if (activity == null || activity.isFinishing() || videoLayout == null) return;
        streamHataBildirimiGizle();
        android.view.ViewGroup videoParent = (android.view.ViewGroup) videoLayout.getParent();
        if (videoParent == null) return;
        int[] videoLoc  = new int[2];
        int[] parentLoc = new int[2];
        videoLayout.getLocationOnScreen(videoLoc);
        videoParent.getLocationOnScreen(parentLoc);
        int left   = videoLoc[0] - parentLoc[0];
        int top    = videoLoc[1] - parentLoc[1];
        int width  = videoLayout.getWidth();
        int height = videoLayout.getHeight();
        streamHataLayout = new android.widget.FrameLayout(activity);
        android.widget.FrameLayout.LayoutParams overlayP =
                new android.widget.FrameLayout.LayoutParams(width, height);
        overlayP.leftMargin = left;
        overlayP.topMargin  = top;
        streamHataLayout.setLayoutParams(overlayP);
        streamHataLayout.setBackgroundColor(0x8C000000);
        if (kanalAdi != null && !kanalAdi.isEmpty()) {
            android.widget.TextView bgText = new android.widget.TextView(activity);
            bgText.setText(kanalAdi.toUpperCase());
            bgText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 72);
            bgText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            bgText.setTextColor(0x07FFFFFF);
            bgText.setLetterSpacing(0.05f);
            bgText.setSingleLine(true);
            bgText.setEllipsize(android.text.TextUtils.TruncateAt.END);
            android.widget.FrameLayout.LayoutParams bgP =
                    new android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT);
            bgP.gravity = android.view.Gravity.CENTER;
            bgText.setLayoutParams(bgP);
            streamHataLayout.addView(bgText);
        }
        android.widget.LinearLayout kart = new android.widget.LinearLayout(activity);
        kart.setOrientation(android.widget.LinearLayout.VERTICAL);
        kart.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        int kartW = dpToPx(300);
        android.widget.FrameLayout.LayoutParams kartP =
                new android.widget.FrameLayout.LayoutParams(kartW,
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT);
        kartP.gravity = android.view.Gravity.CENTER;
        kart.setLayoutParams(kartP);
        android.graphics.drawable.GradientDrawable kartBg = new android.graphics.drawable.GradientDrawable();
        kartBg.setColor(0xEA0A0C14);
        kartBg.setCornerRadius(dpToPx(16));
        kartBg.setStroke(dpToPx(1), 0x59DC322F);
        android.graphics.drawable.GradientDrawable solCizgi =
                new android.graphics.drawable.GradientDrawable(
                        android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0x00DC322F, 0xFFDC322F, 0x00DC322F});
        android.graphics.drawable.InsetDrawable solInset =
                new android.graphics.drawable.InsetDrawable(solCizgi, 0, dpToPx(40), kartW - dpToPx(3), dpToPx(40));
        kart.setBackground(new android.graphics.drawable.LayerDrawable(
                new android.graphics.drawable.Drawable[]{kartBg, solInset}));
        kart.setPadding(dpToPx(32), dpToPx(36), dpToPx(32), dpToPx(28));
        android.widget.FrameLayout ikonHalka = new android.widget.FrameLayout(activity);
        int halkaSize = dpToPx(56);
        android.widget.LinearLayout.LayoutParams halkaP =
                new android.widget.LinearLayout.LayoutParams(halkaSize, halkaSize);
        halkaP.gravity      = android.view.Gravity.CENTER_HORIZONTAL;
        halkaP.bottomMargin = dpToPx(20);
        ikonHalka.setLayoutParams(halkaP);
        android.graphics.drawable.GradientDrawable halkaBg = new android.graphics.drawable.GradientDrawable();
        halkaBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        halkaBg.setColor(0x1ADC322F);
        halkaBg.setStroke(dpToPx(2), 0x4DDC322F);
        ikonHalka.setBackground(halkaBg);
        android.widget.TextView ikonText = new android.widget.TextView(activity);
        ikonText.setText("!");
        ikonText.setTextColor(0xFFDC322F);
        ikonText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 24);
        ikonText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        android.widget.FrameLayout.LayoutParams ikonTP =
                new android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT);
        ikonTP.gravity = android.view.Gravity.CENTER;
        ikonText.setLayoutParams(ikonTP);
        ikonHalka.addView(ikonText);
        android.animation.ObjectAnimator pulse =
                android.animation.ObjectAnimator.ofPropertyValuesHolder(ikonHalka,
                        android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.18f, 1f),
                        android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.18f, 1f));
        pulse.setDuration(2000);
        pulse.setRepeatCount(android.animation.ObjectAnimator.INFINITE);
        pulse.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        pulse.start();
        kart.addView(ikonHalka);
        if (kanalAdi != null && !kanalAdi.isEmpty()) {
            android.widget.TextView kanalAdView = new android.widget.TextView(activity);
            kanalAdView.setText(kanalAdi.toUpperCase());
            kanalAdView.setTextColor(0xFFFFFFFF);
            kanalAdView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 20);
            kanalAdView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            kanalAdView.setLetterSpacing(0.04f);
            kanalAdView.setGravity(android.view.Gravity.CENTER);
            android.widget.LinearLayout.LayoutParams kaP =
                    new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            kaP.gravity      = android.view.Gravity.CENTER_HORIZONTAL;
            kaP.bottomMargin = dpToPx(10);
            kanalAdView.setLayoutParams(kaP);
            kart.addView(kanalAdView);
        }
        android.widget.TextView mesajView = new android.widget.TextView(activity);
        mesajView.setText(mesaj);
        mesajView.setTextColor(0x8CFFFFFF);
        mesajView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13);
        mesajView.setGravity(android.view.Gravity.CENTER);
        mesajView.setLineSpacing(dpToPx(3), 1f);
        android.widget.LinearLayout.LayoutParams meP =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        meP.gravity      = android.view.Gravity.CENTER_HORIZONTAL;
        meP.bottomMargin = dpToPx(20);
        mesajView.setLayoutParams(meP);
        kart.addView(mesajView);
        android.view.View divider = new android.view.View(activity);
        divider.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0x00FFFFFF, 0x14FFFFFF, 0x00FFFFFF}));
        android.widget.LinearLayout.LayoutParams divP =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
        divP.bottomMargin = dpToPx(16);
        divider.setLayoutParams(divP);
        kart.addView(divider);
        android.widget.TextView ipucu = new android.widget.TextView(activity);
        ipucu.setText("· Başka bir kanal seçin veya geri dönün ·");
        ipucu.setTextColor(0x40FFFFFF);
        ipucu.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11);
        ipucu.setGravity(android.view.Gravity.CENTER);
        ipucu.setLetterSpacing(0.03f);
        android.widget.LinearLayout.LayoutParams ipP =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        ipP.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        ipucu.setLayoutParams(ipP);
        kart.addView(ipucu);
        streamHataLayout.addView(kart);
        streamHataLayout.setAlpha(0f);
        kart.setScaleX(0.88f);
        kart.setScaleY(0.88f);
        videoParent.addView(streamHataLayout);
        streamHataLayout.animate().alpha(1f).setDuration(300).start();
        kart.animate().scaleX(1f).scaleY(1f)
                .setDuration(400)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                .start();
    }

    private void streamHataBildirimiGizle() {
        if (streamHataLayout == null) return;
        final android.widget.FrameLayout k = streamHataLayout;
        streamHataLayout = null;
        k.animate().alpha(0f).setDuration(200).withEndAction(() -> {
            try {
                android.view.ViewGroup par = (android.view.ViewGroup) k.getParent();
                if (par != null) par.removeView(k);
            } catch (Exception ignored) {}
        }).start();
    }

    private int dpToPx(int dp) {
        return Math.round(android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, dp,
                activity.getResources().getDisplayMetrics()));
    }

    private String kisaltHost(String host) {
        if (host == null || host.isEmpty()) return host;
        boolean ipAdresi = host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+");
        String[] parcalar = host.split("\\.");
        if (ipAdresi) {
            return parcalar.length >= 2 ? parcalar[0] + "." + parcalar[1] : host;
        } else {
            if (parcalar[0].equals("www") && parcalar.length > 1) {
                return parcalar[1];
            }
            return parcalar[0];
        }
    }





    private void kutuKonumunuGuncelle() {
        if (kisayolKutusu == null) return;

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) kisayolKutusu.getLayoutParams();
        boolean topLeft = false;

        if (dpad != null && dpad.getAktifPanel() == Dpad.AktifPanel.AYARLAR) {
            AyarlarPaneli.MenuSeviyesi menu = ayarlarPaneli.getMevcutMenu();
            if (menu == AyarlarPaneli.MenuSeviyesi.M3U_SILME ||
                    menu == AyarlarPaneli.MenuSeviyesi.XTREAM_MENU) {
                topLeft = true;
            }
        }

        if (topLeft) {
            lp.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
            lp.topMargin = dpToPx(20);
            lp.leftMargin = dpToPx(20);
            lp.bottomMargin = 0;
            lp.rightMargin = 0;
        } else {
            lp.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.END;
            lp.topMargin = 0;
            lp.leftMargin = 0;
            lp.bottomMargin = dpToPx(20);
            lp.rightMargin = dpToPx(20);
        }
        kisayolKutusu.setLayoutParams(lp);
    }
    public boolean aktifIcerikFavoriMi() {
        String anahtar = Icerikpaneli.TIP_DIZI.equals(aktifIcerikTipi) ? aktifDiziAdi : aktifStreamUrl;
        if (anahtar == null || anahtar.isEmpty() || favorilerYoneticisi == null) return false;
        return favorilerYoneticisi.isFavori(aktifIcerikTipi, anahtar);
    }

    public void favoriToggleAktifIcerik() {
        if (favorilerYoneticisi == null) return;
        String anahtar = Icerikpaneli.TIP_DIZI.equals(aktifIcerikTipi) ? aktifDiziAdi : aktifStreamUrl;
        if (anahtar == null || anahtar.isEmpty()) return;
        boolean eklendi = favorilerYoneticisi.toggle(aktifIcerikTipi, anahtar);
        if (oynaticiBar != null) oynaticiBar.favoriDurumunuGuncelle(eklendi);
    }

    public void kutuGorunurlugunuGuncelle() {

        if (kisayolKutusu == null) return;
        boolean kutuGosterilsin = activity.getSharedPreferences("AppSettings", 0)
                .getBoolean("show_shortcut_box", true);
        if (!kutuGosterilsin) {
            kisayolKutusu.setVisibility(View.GONE);
            return;
        }
        boolean panelVeyaBarAcik =
                (icerikPaneli != null && icerikPaneli.isVisible()) ||
                        (kategoriPaneli != null && kategoriPaneli.isVisible()) ||
                        (kanalPaneli != null && kanalPaneli.isVisible()) ||
                        (diziPaneli != null && diziPaneli.isVisible()) ||
                        (ayarlarPaneli != null && ayarlarPaneli.isVisible()) ||
                        (oynaticiBar != null && oynaticiBar.isVisible());
        boolean gorunmeli = dpadModu && (sanalDpad == null || !sanalDpad.isVisible()) && panelVeyaBarAcik;

        System.out.println("🔍 [KUTU] dpadModu=" + dpadModu +
                " sanalVisible=" + (sanalDpad != null && sanalDpad.isVisible()) +
                " panelVeyaBarAcik=" + panelVeyaBarAcik +
                " gorunmeli=" + gorunmeli);

        kisayolKutusu.setVisibility(gorunmeli ? View.VISIBLE : View.GONE);


        kutuKonumunuGuncelle();
    }
}
