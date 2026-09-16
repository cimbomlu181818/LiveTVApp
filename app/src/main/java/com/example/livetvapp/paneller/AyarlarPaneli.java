package com.example.livetvapp.paneller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Gravity;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.livetvapp.stalker.StalkerManager;
import com.example.livetvapp.Channel;
import com.example.livetvapp.KanalListesi;
import com.example.livetvapp.R;
import com.example.livetvapp.Adapter.M3USilmeAdapter;
import com.example.livetvapp.database.ChannelRepository;
import com.example.livetvapp.database.M3UItem;
import com.example.livetvapp.database.M3UManager;
import com.example.livetvapp.database.XtreamCodesManager;
import com.example.livetvapp.database.YedekYonetici;
import com.example.livetvapp.remotecontrol.DeviceDetector;
import java.util.List;
import java.util.Map;

public class AyarlarPaneli {
    public interface DpadModuSaglayici { boolean isDpadModu(); }
    private DpadModuSaglayici dpadModuSupplier;
    public void setDpadModuSupplier(DpadModuSaglayici s) { this.dpadModuSupplier = s; }


    public interface OnShortcutBoxVisibilityChangedListener {
        void onVisibilityChanged(boolean visible);
    }
    private OnShortcutBoxVisibilityChangedListener shortcutBoxListener;
    public void setOnShortcutBoxVisibilityChangedListener(OnShortcutBoxVisibilityChangedListener l) {
        this.shortcutBoxListener = l;
    }



    public interface OnMenuChangedListener {
        void onMenuChanged(MenuSeviyesi yeniMenu);
    }
    private OnMenuChangedListener menuChangedListener;
    public void setOnMenuChangedListener(OnMenuChangedListener listener) {
        this.menuChangedListener = listener;
    }
    private void menuDegisti(MenuSeviyesi yeniMenu) {
        mevcutMenu = yeniMenu;
        if (menuChangedListener != null) menuChangedListener.onMenuChanged(yeniMenu);
    }


    private static final int DOSYA_SECICI_REQUEST = 200;
    private static final int YEDEK_SECICI_REQUEST = 201;
    private static final int RENK_NORMAL   = 0xFF1A3A5A;
    private static final int RENK_FOCUS    = 0xFFFF0000;
    private static final int RENK_ALT_OGE  = 0xFF1A3A5A;
    private static final int RENK_GERI     = 0xFF1A3A5A;


    private static final String PREFS_NAME = "AyarlarPaneliPrefs";
    private static final String KEY_LAST_M3U_URL = "last_m3u_url";
    private static final String KEY_LAST_XTREAM_SERVER = "last_xtream_server";
    private static final String KEY_LAST_XTREAM_USER = "last_xtream_user";
    private static final String KEY_LAST_XTREAM_PASS = "last_xtream_pass";
    private static final String KEY_LAST_STALKER_URL = "last_stalker_url";
    private static final String KEY_LAST_STALKER_MAC = "last_stalker_mac";

    private AppCompatActivity activity;
    private View panelView;
    private ScrollView scrollView;
    private LinearLayout buttonContainer;
    private M3UManager m3uManager;
    private YedekYonetici yedekYonetici;
    private RecyclerView silmeRecyclerView;
    private M3USilmeAdapter silmeAdapter;
    private GizlePaneli gizlePaneli;
    private SiralamaPaneli siralamaPaneli;
    private com.example.livetvapp.paneller.M3UDurumPaneli m3uDurumPaneli;
    private LinearLayout ayarlarContainer;
    private LinearLayout listeMenuContainer;

    private int dp(int dp) {
        return Math.round(dp * activity.getResources().getDisplayMetrics().density);
    }

    private void panelGenislikAyarla(boolean genis) {
        int genislik = dp(genis ? 400 : 200);
        if (scrollView != null) {
            ViewGroup.LayoutParams lp = scrollView.getLayoutParams();
            lp.width = genislik;
            scrollView.setLayoutParams(lp);
        }
        View baslik = panelView.findViewById(R.id.panelBaslik);
        if (baslik != null) {
            ViewGroup.LayoutParams lp = baslik.getLayoutParams();
            lp.width = genislik;
            baslik.setLayoutParams(lp);
        }
    }

    public enum MenuSeviyesi {
        ANA_MENU,
        M3U_YONETIMI,
        M3U_YUKLE,
        YEDEKLEME,
        M3U_SILME,
        GIZLE_PANELI,
        SIRALAMA_PANELI,
        XTREAM_MENU,
        M3U_DURUM_PANELI,
        M3U_URL
    }

    private MenuSeviyesi mevcutMenu = MenuSeviyesi.ANA_MENU;
    private Runnable sanalDpadCallback;
    public void setSanalDpadCallback(Runnable cb) { this.sanalDpadCallback = cb; }
    private OzelKlavye ozelKlavye;

    public interface M3UYuklemeListener {
        void onM3UYuklendi(String dosyaYolu);
    }
    private M3UYuklemeListener m3uYuklemeListener;
    public void setM3UYuklemeListener(M3UYuklemeListener listener) {
        this.m3uYuklemeListener = listener;
    }

    public interface M3USilmeListener {
        void onM3USilindi();
    }
    private M3USilmeListener m3uSilmeListener;
    private Runnable m3uGuncellendiListener;
    public void setM3UGuncellendiListener(Runnable listener) {
        this.m3uGuncellendiListener = listener;
    }
    public void setM3USilmeListener(M3USilmeListener listener) {
        this.m3uSilmeListener = listener;
    }

    public interface GizleKapandiListener {
        void onGizleKapandi(Map<String, List<String>> m3uKategoriHaritasi);
    }
    private GizleKapandiListener gizleKapandiListener;
    public void setGizleKapandiListener(GizleKapandiListener listener) {
        this.gizleKapandiListener = listener;
    }

    public interface PanelKapandiListener {
        void onAyarlarKapandi();
    }
    private PanelKapandiListener panelKapandiListener;
    public void setPanelKapandiListener(PanelKapandiListener listener) {
        this.panelKapandiListener = listener;
    }

    public interface SifirlamaListener {
        void onAyarlarSifirlandı();
    }
    private SifirlamaListener sifirlamaListener;
    public void setSifirlamaListener(SifirlamaListener listener) {
        this.sifirlamaListener = listener;
    }

    public interface YuklemeListener {
        void yuklemeBasladi(String m3uAdi);
        void yuklemeBitti();
    }
    private YuklemeListener yuklemeListener;
    public void setYuklemeListener(YuklemeListener listener) {
        this.yuklemeListener = listener;
    }
    public YuklemeListener getYuklemeListener() { return yuklemeListener; }

    public AyarlarPaneli(AppCompatActivity activity) {
        this.activity      = activity;
        this.m3uManager    = new M3UManager(activity);
        this.yedekYonetici = new YedekYonetici(activity);
    }

    public void olustur() {
        panelView = LayoutInflater.from(activity).inflate(R.layout.panel_ayarlar, null);
        scrollView         = panelView.findViewById(R.id.panelScrollView);
        buttonContainer    = panelView.findViewById(R.id.Paneller);
        ayarlarContainer   = buttonContainer;
        listeMenuContainer = panelView.findViewById(R.id.listeMenuContainer);
        panelView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        if (scrollView      != null) scrollView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        if (buttonContainer != null) buttonContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        panelView.setVisibility(View.GONE);
        anaMenuyuOlustur();
        activity.addContentView(
                panelView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
        gizlePaneli = new GizlePaneli(activity);
        gizlePaneli.olustur();
        gizlePaneli.setPanelKapandiCallback(() -> {
            menuDegisti(MenuSeviyesi.ANA_MENU);
            if (scrollView != null) scrollView.setVisibility(View.VISIBLE);
            listeYonetimiMenuGoster();
            if (sanalDpadCallback != null) sanalDpadCallback.run();
            if (gizleKapandiListener != null) {
                gizleKapandiListener.onGizleKapandi(gizlePaneli.getM3uKategoriOnbellegi());
            }
        });
        gizlePaneli.setPanelIptalCallback(() -> {
            menuDegisti(MenuSeviyesi.ANA_MENU);
            if (scrollView != null) scrollView.setVisibility(View.VISIBLE);
            listeYonetimiMenuGoster();
            if (sanalDpadCallback != null) sanalDpadCallback.run();
        });
        siralamaPaneli = new SiralamaPaneli(activity);
        siralamaPaneli.olustur();
        siralamaPaneli.setPanelKapandiCallback(() -> {
            menuDegisti(MenuSeviyesi.ANA_MENU);
            if (scrollView != null) scrollView.setVisibility(View.VISIBLE);
            listeYonetimiMenuGoster();
            if (sanalDpadCallback != null) sanalDpadCallback.run();
        });
        m3uDurumPaneli = new com.example.livetvapp.paneller.M3UDurumPaneli(activity);
        m3uDurumPaneli.olustur();
        m3uDurumPaneli.setM3UGuncellendiCallback(() -> {
            com.example.livetvapp.database.KategoriCache.getInstance().invalidateAll();
            com.example.livetvapp.database.DiziCache.getInstance().invalidateAll();
            if (siralamaPaneli != null) siralamaPaneli.veriGecersizKil();
            if (m3uGuncellendiListener != null) m3uGuncellendiListener.run();
            System.out.println("✅ [M3U-GÜN] Cache temizlendi, liste yenileniyor");
        });
        m3uDurumPaneli.setKapandiCallback(() -> {
            menuDegisti(MenuSeviyesi.M3U_YONETIMI);
            if (panelView != null) panelView.setVisibility(View.VISIBLE);
            if (scrollView != null) scrollView.setVisibility(View.VISIBLE);
            m3uYonetimMenuGoster();
            if (sanalDpadCallback != null) sanalDpadCallback.run();
        });
        ozelKlavye = new OzelKlavye(activity);
        ozelKlavye.ekranaEkle();
        ozelKlavye.setOnFocusPaneleGecListener(() -> {
            if (ayarlarContainer != null && ayarlarContainer.getChildCount() > 0) {
                ayarlarContainer.getChildAt(0).requestFocus();
            }
        });
        ozelKlavye.setOnBackListener(() -> {
            if (ayarlarContainer != null && ayarlarContainer.getChildCount() > 0) {
                ayarlarContainer.getChildAt(0).requestFocus();
            }
        });
    }

    private void anaMenuyuOlustur() {
        ayarlarContainer.removeAllViews();
        menuDegisti(MenuSeviyesi.ANA_MENU);
        normalModuGoster();
        TextView baslik = panelView.findViewById(R.id.panelBaslik);
        if (baslik != null) baslik.setText("AYARLAR");

        Button btn1 = anaKategoriButonOlustur("1 - Genel Ayarlar");
        btn1.setOnClickListener(v -> yedeklemeMenuGoster());
        ayarlarContainer.addView(btn1);

        Button btn2 = anaKategoriButonOlustur("2 - M3U Ayarları");
        btn2.setOnClickListener(v -> m3uYonetimMenuGoster());
        ayarlarContainer.addView(btn2);

        Button btn3m3u = anaKategoriButonOlustur("3 - Liste Yönetimi");
        btn3m3u.setOnClickListener(v -> listeYonetimiMenuGoster());
        ayarlarContainer.addView(btn3m3u);

        ayarlarContainer.addView(ayiriciOlustur());

        Button btn3 = anaKategoriButonOlustur("4 - Uygulamayı Kapat");
        btn3.setOnClickListener(v -> uygulamayiKapat());
        ayarlarContainer.addView(btn3);


        if (DeviceDetector.isPhone(activity)) {
            Button btnRemote = anaKategoriButonOlustur("5 - Kumanda");
            btnRemote.setOnClickListener(v -> {
                Intent intent = new Intent(activity, com.example.livetvapp.remotecontrol.RemoteDeviceScanActivity.class);
                activity.startActivity(intent);
            });
            ayarlarContainer.addView(btnRemote);
        }

        SharedPreferences girisTercihleriAyarlar = activity.getSharedPreferences("azsh_giris", Context.MODE_PRIVATE);
        String emailBilgisi = girisTercihleriAyarlar.getString("email", "");
        if (emailBilgisi == null) emailBilgisi = "";
        String cikisButonMetni = emailBilgisi.isEmpty()
                ? "6 - Çıkış Yap"
                : "6 - Çıkış Yap\n" + emailBilgisi.toLowerCase();

        Button btn4 = anaKategoriButonOlustur(cikisButonMetni);
        btn4.setBackgroundColor(0xFF8B0000);
        btn4.setOnFocusChangeListener((v, hasFocus) ->
                v.setBackgroundColor(hasFocus ? RENK_FOCUS : 0xFF8B0000));
        btn4.setOnClickListener(v -> cikisYapButonuTiklandi());
        ayarlarContainer.addView(btn4);

        TextView footer = new TextView(activity);
        footer.setText("AZSH YAZILIM\nMade in Turkey");
        footer.setTextColor(0xAAFFFFFF);
        footer.setTextSize(14);
        footer.setTypeface(null, Typeface.BOLD);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(8), 0, dp(8));
        footer.setFocusable(false);
        ayarlarContainer.addView(footer);

        btn1.postDelayed(() -> btn1.requestFocus(), 100);
    }

    private void listeYonetimiMenuGoster() {
        ayarlarContainer.removeAllViews();
        menuDegisti(MenuSeviyesi.M3U_YONETIMI);
        normalModuGoster();
        if (gizlePaneli    != null && gizlePaneli.isVisible())    gizlePaneli.gizle();
        if (siralamaPaneli != null && siralamaPaneli.isVisible()) siralamaPaneli.gizle();
        TextView baslik = panelView.findViewById(R.id.panelBaslik);
        if (baslik != null) baslik.setText("1 - LİSTE YÖNETİMİ");
        Button btnGeri = altMenuGeriButonOlustur("← Ana Menü");
        btnGeri.setOnClickListener(v -> anaMenuyuOlustur());
        ayarlarContainer.addView(btnGeri);
        ayarlarContainer.addView(ayiriciOlustur());
        Button btn2 = aktifAltOgeOlustur("1 - Gizle / Göster");
        btn2.setOnClickListener(v -> gizlePaneliniAc());
        ayarlarContainer.addView(btn2);
        Button btn3 = aktifAltOgeOlustur("2 - Sıralama");
        btn3.setOnClickListener(v -> siralamaPaneliniAc());
        ayarlarContainer.addView(btn3);
        btn2.postDelayed(() -> btn2.requestFocus(), 100);
    }

    public void siralamaPaneliniAc() {
        List<M3UItem> tumM3UListesi = m3uManager.getAllM3UItems();
        if (tumM3UListesi == null || tumM3UListesi.isEmpty()) {
            Toast.makeText(activity, "Henüz M3U listesi yok!", Toast.LENGTH_SHORT).show();
            return;
        }
        menuDegisti(MenuSeviyesi.SIRALAMA_PANELI);
        if (panelView != null) panelView.setVisibility(View.VISIBLE);
        if (scrollView != null) scrollView.setVisibility(View.INVISIBLE);
        siralamaPaneli.ac();
        if (sanalDpadCallback != null) sanalDpadCallback.run();
    }

    private void m3uYonetimMenuGoster() {
        ayarlarContainer.removeAllViews();
        menuDegisti(MenuSeviyesi.M3U_YONETIMI);
        normalModuGoster();
        if (gizlePaneli    != null && gizlePaneli.isVisible())    gizlePaneli.gizle();
        if (siralamaPaneli != null && siralamaPaneli.isVisible()) siralamaPaneli.gizle();
        TextView baslik = panelView.findViewById(R.id.panelBaslik);
        if (baslik != null) baslik.setText("3 - M3U AYARLARI");
        Button btnGeri = altMenuGeriButonOlustur("← Ana Menü");
        btnGeri.setOnClickListener(v -> anaMenuyuOlustur());
        ayarlarContainer.addView(btnGeri);
        ayarlarContainer.addView(ayiriciOlustur());
        Button btn1 = aktifAltOgeOlustur("1 - M3U Durumu");
        btn1.setOnClickListener(v -> m3uDurumuMenuGoster());
        ayarlarContainer.addView(btn1);
        Button btn2 = aktifAltOgeOlustur("2 - M3U Yükle");
        btn2.setOnClickListener(v -> m3uYukleMenuGoster());
        ayarlarContainer.addView(btn2);
        Button btn3 = aktifAltOgeOlustur("3 - M3U Sil");
        btn3.setOnClickListener(v -> m3uListeleriGoster());
        ayarlarContainer.addView(btn3);
        btn1.postDelayed(() -> btn1.requestFocus(), 100);
    }

    private void m3uYukleMenuGoster() {
        ayarlarContainer.removeAllViews();
        menuDegisti(MenuSeviyesi.M3U_YUKLE);
        normalModuGoster();
        TextView baslik = panelView.findViewById(R.id.panelBaslik);
        if (baslik != null) baslik.setText("1 - M3U YÜKLE");
        Button btnGeri = altMenuGeriButonOlustur("← M3U Ayarları");
        btnGeri.setOnClickListener(v -> m3uYonetimMenuGoster());
        ayarlarContainer.addView(btnGeri);
        ayarlarContainer.addView(ayiriciOlustur());
        Button btnYukle = aktifAltOgeOlustur("a - M3U Dosyadan Yükle");
        btnYukle.setOnClickListener(v -> dosyaSeciciAc());
        ayarlarContainer.addView(btnYukle);
        Button btnUrlEkle = aktifAltOgeOlustur("b - M3U URL'den Yükle");
        btnUrlEkle.setOnClickListener(v -> m3uUrlDiyaloguGoster());
        ayarlarContainer.addView(btnUrlEkle);
        Button btnXtream = aktifAltOgeOlustur("c - Xtream Codes Ekle");
        btnXtream.setOnClickListener(v -> xtreamHesapDiyaloguGoster());
        ayarlarContainer.addView(btnXtream);
        Button btnStalker = aktifAltOgeOlustur("d - Stalker Portal Ekle");
        btnStalker.setOnClickListener(v -> stalkerHesapDiyaloguGoster());
        ayarlarContainer.addView(btnStalker);
        btnGeri.postDelayed(() -> btnGeri.requestFocus(), 100);

        TextView footer = new TextView(activity);
        footer.setText("AZSH YAZILIM\nMade in Turkey");
        footer.setTextColor(0xAAFFFFFF);
        footer.setTextSize(14);
        footer.setTypeface(null, Typeface.BOLD);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(8), 0, dp(8));
        footer.setFocusable(false);
        ayarlarContainer.addView(footer);
    }

    private void m3uDurumuMenuGoster() {
        System.out.println("⚙️ m3uDurumuMenuGoster çağrıldı, panelView="
                + (panelView != null ? panelView.getVisibility() : "null")
                + " scrollView=" + (scrollView != null ? scrollView.getVisibility() : "null"));
        List<M3UItem> tumM3UListesi = m3uManager.getAllM3UItems();
        if (tumM3UListesi == null || tumM3UListesi.isEmpty()) {
            Toast.makeText(activity, "Henüz M3U listesi yok!", Toast.LENGTH_SHORT).show();
            return;
        }
        menuDegisti(MenuSeviyesi.M3U_DURUM_PANELI);
        if (scrollView != null) scrollView.setVisibility(View.INVISIBLE);
        panelView.setVisibility(View.VISIBLE);
        m3uDurumPaneli.ac();
        if (sanalDpadCallback != null) sanalDpadCallback.run();
    }


    private void m3uUrlDiyaloguGoster() {
        new AlertDialog.Builder(activity)
                .setTitle("M3U URL Yükle")
                .setItems(new String[]{"Yeni Ekle", "Sonuncuyu Düzenle"}, (dialog, which) -> {
                    if (which == 0) {
                        m3uUrlFormunuGoster(null);
                    } else {
                        String lastUrl = getLastM3UUrl();
                        if (lastUrl == null || lastUrl.isEmpty()) {
                            Toast.makeText(activity, "Kayıtlı son M3U URL bulunamadı.", Toast.LENGTH_SHORT).show();
                            m3uUrlFormunuGoster(null);
                        } else {
                            m3uUrlFormunuGoster(lastUrl);
                        }
                    }
                })
                .setNegativeButton("İptal", null)
                .show();
    }


    private void m3uUrlFormunuGoster(String prefillUrl) {
        ayarlarContainer.removeAllViews();
        normalModuGoster();
        panelGenislikAyarla(true);
        menuDegisti(MenuSeviyesi.M3U_URL);
        TextView baslik = panelView.findViewById(R.id.panelBaslik);
        if (baslik != null) baslik.setText("b - URL'den Yükle");
        Button btnGeri = altMenuGeriButonOlustur("← M3U Ayarları");
        btnGeri.setOnClickListener(v -> {
            ozelKlavye.gizle();
            panelGenislikAyarla(false);
            m3uYonetimMenuGoster();
        });
        ayarlarContainer.addView(btnGeri);
        ayarlarContainer.addView(ayiriciOlustur());
        ayarlarContainer.addView(etiketOlustur("M3U URL"));
        EditText etUrl = formAlanOlustur("https://example.com/playlist.m3u");
        if (prefillUrl != null && !prefillUrl.isEmpty()) {
            etUrl.setText(prefillUrl);
        }
        ayarlarContainer.addView(etUrl);
        ayarlarContainer.addView(ayiriciOlustur());
        Button btnYukle = aktifAltOgeOlustur("✓  Yükle");
        btnYukle.setBackgroundColor(0x88006600);
        btnYukle.setOnFocusChangeListener((v, hasFocus) ->
                v.setBackgroundColor(hasFocus ? RENK_FOCUS : 0x88006600));
        btnYukle.setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim();
            if (url.isEmpty()) { etUrl.setError("URL boş olamaz"); etUrl.requestFocus(); return; }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                etUrl.setError("Geçerli bir URL girin (http:// veya https://)");
                etUrl.requestFocus();
                return;
            }

            saveLastM3UUrl(url);
            ozelKlavye.gizle();
            panelGenislikAyarla(false);
            m3uUrldenYukle(url);
        });
        ayarlarContainer.addView(btnYukle);
        etUrl.setOnFocusChangeListener((v, f) -> { renkveklavye((EditText)v, f, etUrl); });
        editTexteKlavyeGecisEkle(etUrl);
        ozelKlavye.setOnEnterListener(metin -> {
            String url = metin.trim();
            if (!url.isEmpty() && (url.startsWith("http://") || url.startsWith("https://"))) {
                ozelKlavye.gizle();
                panelGenislikAyarla(false);
                m3uUrldenYukle(url);
            }
        });
        if (dpadModuSupplier == null || !dpadModuSupplier.isDpadModu()) {
            etUrl.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
            etUrl.requestFocus();
        }
        android.util.Log.d("RENK", "URL formu dpadModu=" + (dpadModuSupplier != null && dpadModuSupplier.isDpadModu()));
        if (dpadModuSupplier != null && dpadModuSupplier.isDpadModu()) {
            ozelKlavye.goster(etUrl);
        } else {
            ozelKlavye.gizle();
            etUrl.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
            etUrl.requestFocus();
            etUrl.postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager)
                        activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(etUrl, InputMethodManager.SHOW_FORCED);
            }, 200);
        }
        etUrl.postDelayed(() -> etUrl.requestFocus(), 150);
    }


    private void xtreamHesapDiyaloguGoster() {
        new AlertDialog.Builder(activity)
                .setTitle("Xtream Codes Ekle")
                .setItems(new String[]{"Yeni Ekle", "Sonuncuyu Düzenle"}, (dialog, which) -> {
                    if (which == 0) {
                        xtreamFormunuGoster(null, null, null);
                    } else {
                        String[] last = getLastXtream();
                        if (last == null) {
                            Toast.makeText(activity, "Kayıtlı son Xtream hesabı bulunamadı.", Toast.LENGTH_SHORT).show();
                            xtreamFormunuGoster(null, null, null);
                        } else {
                            xtreamFormunuGoster(last[0], last[1], last[2]);
                        }
                    }
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void xtreamFormunuGoster(String prefillServer, String prefillUser, String prefillPass) {
        android.util.Log.d("RENK", "xtreamFormunuGoster ÇAĞRILDI");
        ayarlarContainer.removeAllViews();
        menuDegisti(MenuSeviyesi.XTREAM_MENU);
        normalModuGoster();
        panelGenislikAyarla(true);
        TextView baslik = panelView.findViewById(R.id.panelBaslik);
        if (baslik != null) baslik.setText("Xtream Codes Ekle");

        Button btnGeri = altMenuGeriButonOlustur("← M3U Yükle");
        btnGeri.setOnClickListener(v -> {
            ozelKlavye.gizle();
            panelGenislikAyarla(false);
            m3uYukleMenuGoster();
        });
        ayarlarContainer.addView(btnGeri);
        ayarlarContainer.addView(ayiriciOlustur());


        EditText etServer = formAlanOlustur("http://sunucu.com:8080");
        EditText etUser   = formAlanOlustur("Kullanıcı Adı");
        EditText etPass   = formAlanOlustur("Şifre");

        if (prefillServer != null) etServer.setText(prefillServer);
        if (prefillUser != null) etUser.setText(prefillUser);
        if (prefillPass != null) etPass.setText(prefillPass);

        ayarlarContainer.addView(etiketOlustur("Sunucu URL"));
        ayarlarContainer.addView(etServer);
        ayarlarContainer.addView(etiketOlustur("Kullanıcı Adı"));
        ayarlarContainer.addView(etUser);
        ayarlarContainer.addView(etiketOlustur("Şifre"));
        ayarlarContainer.addView(etPass);
        ayarlarContainer.addView(ayiriciOlustur());

        Button btnYukle = aktifAltOgeOlustur("✓  Yükle");
        btnYukle.setBackgroundColor(0x88006600);
        btnYukle.setOnFocusChangeListener((v, hasFocus) ->
                v.setBackgroundColor(hasFocus ? RENK_FOCUS : 0x88006600));
        btnYukle.setOnClickListener(v -> {
            String server = etServer.getText().toString().trim();
            String user   = etUser.getText().toString().trim();
            String pass   = etPass.getText().toString().trim();

            if (server.isEmpty()) {
                etServer.setError("Sunucu gerekli");
                etServer.requestFocus();
                return;
            }
            if (user.isEmpty()) {
                etUser.setError("Kullanıcı adı gerekli");
                etUser.requestFocus();
                return;
            }
            if (pass.isEmpty()) {
                etPass.setError("Şifre gerekli");
                etPass.requestFocus();
                return;
            }


            saveLastXtream(server, user, pass);


            String accName;
            try {
                java.net.URL parsedUrl = new java.net.URL(server);
                String host = parsedUrl.getHost();
                accName = kisaltHost(host) + "(" + user + ")";
            } catch (Exception e) {
                accName = "Xtream_" + System.currentTimeMillis();
            }

            ozelKlavye.gizle();
            panelGenislikAyarla(false);
            xtreamYukle(server, user, pass, accName);
        });
        ayarlarContainer.addView(btnYukle);


        etServer.setOnFocusChangeListener((v, f) -> { renkveklavye((EditText)v, f, etServer); });
        etUser.setOnFocusChangeListener((v, f) -> { renkveklavye((EditText)v, f, etUser); });
        etPass.setOnFocusChangeListener((v, f) -> { renkveklavye((EditText)v, f, etPass); });

        editTexteKlavyeGecisEkle(etServer);
        editTexteKlavyeGecisEkle(etUser);
        editTexteKlavyeGecisEkle(etPass);
        android.util.Log.d("RENK", "dpadModuSupplier=" + dpadModuSupplier + " isDpadModu=" + (dpadModuSupplier != null && dpadModuSupplier.isDpadModu()));
        if (dpadModuSupplier != null && dpadModuSupplier.isDpadModu()) {
            ozelKlavye.goster(etServer);
            android.util.Log.d("RENK", "goster çağrıldı, etServer textColor=" + Integer.toHexString(etServer.getCurrentTextColor()));
        } else {
            ozelKlavye.gizle();
            etServer.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
            etServer.requestFocus();
            etServer.postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager)
                        activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(etServer, InputMethodManager.SHOW_FORCED);
            }, 200);
        }

        etServer.postDelayed(() -> {
            android.util.Log.d("RENK", "requestFocus çağrıldı, önceki textColor=" + Integer.toHexString(etServer.getCurrentTextColor()));
            etServer.requestFocus();
            android.util.Log.d("RENK", "requestFocus sonrası textColor=" + Integer.toHexString(etServer.getCurrentTextColor()) + " hasFocus=" + etServer.hasFocus());
        }, 150);
    }
    private void stalkerFormunuGoster(String prefillUrl, String prefillMac) {
        ayarlarContainer.removeAllViews();
        menuDegisti(MenuSeviyesi.XTREAM_MENU);
        normalModuGoster();
        panelGenislikAyarla(true);

        TextView baslik = panelView.findViewById(R.id.panelBaslik);
        if (baslik != null) baslik.setText("Stalker Portal Ekle");

        Button btnGeri = altMenuGeriButonOlustur("← M3U Yükle");
        btnGeri.setOnClickListener(v -> {
            ozelKlavye.gizle();
            panelGenislikAyarla(false);
            m3uYukleMenuGoster();
        });
        ayarlarContainer.addView(btnGeri);
        ayarlarContainer.addView(ayiriciOlustur());

        EditText etUrl = formAlanOlustur("http://portal.example.com/c/");
        EditText etMac = formAlanOlustur("00:1A:79:XX:XX:XX");
        if (prefillUrl != null) etUrl.setText(prefillUrl);
        if (prefillMac != null) etMac.setText(prefillMac);
        ayarlarContainer.addView(etiketOlustur("Portal URL"));
        ayarlarContainer.addView(etUrl);
        ayarlarContainer.addView(etiketOlustur("MAC Adresi"));
        ayarlarContainer.addView(etMac);
        ayarlarContainer.addView(ayiriciOlustur());

        Button btnYukle = aktifAltOgeOlustur("✓  Bağlan ve Yükle");
        btnYukle.setBackgroundColor(0x88006600);
        btnYukle.setOnFocusChangeListener((v, hasFocus) ->
                v.setBackgroundColor(hasFocus ? RENK_FOCUS : 0x88006600));
        btnYukle.setOnClickListener(v -> {
            String portalUrl = etUrl.getText().toString().trim();
            String mac       = etMac.getText().toString().trim();

            if (portalUrl.isEmpty()) {
                etUrl.setError("Portal URL boş olamaz");
                etUrl.requestFocus();
                return;
            }
            if (!portalUrl.startsWith("http://") && !portalUrl.startsWith("https://")) {
                etUrl.setError("Geçerli bir URL girin (http:// veya https://)");
                etUrl.requestFocus();
                return;
            }
            if (mac.isEmpty()) {
                etMac.setError("MAC adresi boş olamaz");
                etMac.requestFocus();
                return;
            }


            String portalAdi;
            try {
                java.net.URL parsed = new java.net.URL(portalUrl);
                portalAdi = kisaltHost(parsed.getHost());
            } catch (Exception e) {
                portalAdi = "Stalker_" + System.currentTimeMillis();
            }
            final String finalPortalAdi = "Stalker:" + portalAdi;
            saveLastStalker(portalUrl, mac);
            ozelKlavye.gizle();
            panelGenislikAyarla(false);

            if (yuklemeListener != null) yuklemeListener.yuklemeBasladi(finalPortalAdi);

            StalkerManager stalkerManager = new StalkerManager(activity);
            stalkerManager.addPortalAndSync(finalPortalAdi, portalUrl, mac,
                    new StalkerManager.SyncListener() {
                        @Override
                        public void onSuccess(int channelCount) {
                            if (yuklemeListener != null) yuklemeListener.yuklemeBitti();
                            Toast.makeText(activity,
                                    "✅ " + channelCount + " içerik yüklendi: " + finalPortalAdi,
                                    Toast.LENGTH_LONG).show();
                            if (m3uYuklemeListener != null)
                                m3uYuklemeListener.onM3UYuklendi(portalUrl);
                        }
                        @Override
                        public void onError(String error) {
                            if (yuklemeListener != null) yuklemeListener.yuklemeBitti();
                            Toast.makeText(activity,
                                    "❌ Stalker hatası: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
        });
        ayarlarContainer.addView(btnYukle);

        etUrl.setOnFocusChangeListener((v, f) -> renkveklavye((EditText) v, f, etUrl));
        etMac.setOnFocusChangeListener((v, f) -> renkveklavye((EditText) v, f, etMac));
        editTexteKlavyeGecisEkle(etUrl);
        editTexteKlavyeGecisEkle(etMac);

        if (dpadModuSupplier != null && dpadModuSupplier.isDpadModu()) {
            ozelKlavye.goster(etUrl);
        } else {
            ozelKlavye.gizle();
            etUrl.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
            etUrl.requestFocus();
            etUrl.postDelayed(() -> {
                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager)
                                activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(etUrl,
                        android.view.inputmethod.InputMethodManager.SHOW_FORCED);
            }, 200);
        }
        etUrl.postDelayed(() -> etUrl.requestFocus(), 150);
    }

    private void editTexteKlavyeGecisEkle(EditText et) {
        et.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                    || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                return ozelKlavye.handleCursorKey(keyCode);
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                ozelKlavye.setHedefEditText(et);
                ozelKlavye.focusKlavyeye();
                return true;
            }
            return false;
        });
    }

    private EditText formAlanOlustur(String hint) {
        EditText et = new EditText(activity);
        et.setHint(hint);
        et.setHintTextColor(0x88FFFFFF);
        et.setTextColor(0xFFFFFFFF);
        et.setTextSize(15);
        et.setSingleLine(true);
        et.setPadding(20, 16, 20, 16);
        et.setFocusable(true);
        et.setFocusableInTouchMode(true);
        et.setBackgroundColor(0xCC000000);
        et.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && ozelKlavye != null && ozelKlavye.isVisible()) return;
            v.setBackgroundColor(hasFocus ? 0xFFFFDD00 : 0xCC000000);
            ((EditText) v).setTextColor(hasFocus ? 0xFF000000 : 0xFFFFFFFF);
            ((EditText) v).setHintTextColor(hasFocus ? 0xFF000000 : 0x88FFFFFF);
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 2, 0, 2);
        et.setLayoutParams(lp);
        return et;
    }
    private void renkveklavye(EditText et, boolean hasFocus, EditText hedef) {
        if (!hasFocus && ozelKlavye != null && ozelKlavye.isVisible()) return;
        et.setBackgroundColor(hasFocus ? 0xFFFFDD00 : 0xCC000000);
        et.setTextColor(hasFocus ? 0xFF000000 : 0xFFFFFFFF);
        et.setHintTextColor(hasFocus ? 0xFF000000 : 0x88FFFFFF);
        if (hasFocus) ozelKlavye.setHedefEditText(hedef);
    }
    private TextView etiketOlustur(String text) {
        TextView tv = new TextView(activity);
        tv.setText(text);
        tv.setTextColor(0xFFDDDDDD);
        tv.setTextSize(13);
        tv.setPadding(20, 12, 20, 2);
        return tv;
    }

    private void xtreamYukle(String server, String username, String password, String sourceName) {
        Toast.makeText(activity, "⏳ Xtream bağlanıyor: " + sourceName, Toast.LENGTH_LONG).show();
        if (yuklemeListener != null) yuklemeListener.yuklemeBasladi(sourceName);

        ChannelRepository channelRepository = new ChannelRepository(activity);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        XtreamCodesManager.loadChannels(activity, server, username, password, sourceName,
                new XtreamCodesManager.OnXtreamLoadListener() {
                    @Override
                    public void onStarted() {
                        mainHandler.post(() ->
                                Toast.makeText(activity, "⏳ Xtream yükleniyor...", Toast.LENGTH_SHORT).show());
                    }
                    @Override
                    public void onProgress(String message) {
                        mainHandler.post(() ->
                                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show());
                    }
                    @Override
                    public void onSuccess(List<Channel> channels, String srcName) {
                        mainHandler.post(() -> {
                            if (channels == null || channels.isEmpty()) {
                                if (yuklemeListener != null) yuklemeListener.yuklemeBitti();
                                Toast.makeText(activity, "❌ Hiç kanal bulunamadı", Toast.LENGTH_LONG).show();
                                return;
                            }
                            M3UItem m3uItem = new M3UItem();
                            m3uItem.setName(srcName);
                            m3uItem.setUrl(server + " [Xtream]");
                            m3uItem.setLocalFile(false);
                            m3uItem.setChannelCount(channels.size());
                            System.out.println("🔍 [XTREAM-EKLE] m3uManager.addM3U çağrılıyor | srcName='" + srcName + "'");
                            m3uManager.addM3U(m3uItem);
                            System.out.println("🔍 [XTREAM-EKLE] m3uManager.addM3U sonrası gerçek M3U adı='" + m3uItem.getName() + "'");
                            System.out.println("🔍 [XTREAM-EKLE] XtreamCodesManager.saveAccount çağrılıyor | name='" + m3uItem.getName() + "' | server='" + server + "'");
                            XtreamCodesManager.saveAccount(activity, m3uItem.getName(), server, username, password);
                            if (siralamaPaneli != null) siralamaPaneli.veriGecersizKil();
                            channelRepository.insertChannels(channels, new ChannelRepository.OnCompleteListener() {
                                @Override
                                public void onSuccess() {
                                    mainHandler.post(() -> {
                                        if (yuklemeListener != null) yuklemeListener.yuklemeBitti();
                                        Toast.makeText(activity,
                                                "✅ " + channels.size() + " kanal yüklendi: " + srcName,
                                                Toast.LENGTH_LONG).show();
                                        if (m3uYuklemeListener != null)
                                            m3uYuklemeListener.onM3UYuklendi("https://xtream-loaded");
                                        saveLastXtream(server, username, password);
                                        System.out.println("✅ [XTREAM-EKLE] Tamamlandı. saveAccount ve insertChannels başarılı.");
                                    });
                                }
                                @Override
                                public void onError(Exception e) {
                                    mainHandler.post(() -> {
                                        if (yuklemeListener != null) yuklemeListener.yuklemeBitti();
                                        Toast.makeText(activity,
                                                "❌ DB kayıt hatası: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                        System.out.println("❌ [XTREAM-EKLE] insertChannels hatası: " + e.getMessage());
                                    });
                                }
                            });
                        });
                    }
                    @Override
                    public void onError(String error) {
                        mainHandler.post(() -> {
                            if (yuklemeListener != null) yuklemeListener.yuklemeBitti();
                            Toast.makeText(activity, "❌ " + error, Toast.LENGTH_LONG).show();
                            menuDegisti(MenuSeviyesi.ANA_MENU);
                            System.out.println("❌ [XTREAM-EKLE] loadChannels hatası: " + error);
                        });
                    }
                });
    }
    public void gizlePaneliniAc() {
        List<M3UItem> tumM3UListesi = m3uManager.getAllM3UItems();
        if (tumM3UListesi == null || tumM3UListesi.isEmpty()) {
            Toast.makeText(activity, "Henüz M3U listesi yok!", Toast.LENGTH_SHORT).show();
            return;
        }
        menuDegisti(MenuSeviyesi.GIZLE_PANELI);
        if (panelView != null) panelView.setVisibility(View.VISIBLE);
        if (scrollView != null) scrollView.setVisibility(View.INVISIBLE);
        gizlePaneli.ac();
        if (sanalDpadCallback != null) sanalDpadCallback.run();
    }

    private void yedeklemeMenuGoster() {
        ayarlarContainer.removeAllViews();
        menuDegisti(MenuSeviyesi.YEDEKLEME);
        normalModuGoster();

        TextView baslik = panelView.findViewById(R.id.panelBaslik);
        if (baslik != null) baslik.setText("1 - GENEL AYARLAR");

        Button btnGeri = altMenuGeriButonOlustur("← Ana Menü");
        btnGeri.setOnClickListener(v -> anaMenuyuOlustur());
        ayarlarContainer.addView(btnGeri);

        ayarlarContainer.addView(ayiriciOlustur());
        Button btnDosyaYonetici = aktifAltOgeOlustur("a - Dosya Yöneticisi");
        btnDosyaYonetici.setOnClickListener(v -> {
            Intent intent = new Intent(activity, DosyaSecicipaneli.class);
            intent.putExtra("mode", "browser");
            activity.startActivity(intent);
        });
        ayarlarContainer.addView(btnDosyaYonetici);

        ayarlarContainer.addView(ayiriciOlustur());

        boolean initial = activity.getSharedPreferences("AppSettings", 0)
                .getBoolean("show_shortcut_box", true);

        Button btnKisayol = aktifAltOgeOlustur(
                initial ? "b - Yardım Kutusunu Gizle" : "b - Yardım Kutusunu Göster"
        );
        btnKisayol.setOnClickListener(v -> {
            boolean current = activity.getSharedPreferences("AppSettings", 0)
                    .getBoolean("show_shortcut_box", true);
            boolean newState = !current;
            activity.getSharedPreferences("AppSettings", 0)
                    .edit()
                    .putBoolean("show_shortcut_box", newState)
                    .apply();
            btnKisayol.setText(
                    newState ? "b - Yardım Kutusunu Gizle" : "b - Yardım Kutusunu Göster"
            );
            if (shortcutBoxListener != null) {
                shortcutBoxListener.onVisibilityChanged(newState);
            }
        });
        ayarlarContainer.addView(btnKisayol);

        Button btnDisaAktar = aktifAltOgeOlustur("c - Ayarları Dışa Aktar");
        btnDisaAktar.setOnClickListener(v -> disaAktarIzinKontrol());
        ayarlarContainer.addView(btnDisaAktar);

        Button btnIceAktar = aktifAltOgeOlustur("d - Ayarları İçe Aktar");
        btnIceAktar.setOnClickListener(v -> yedekDosyaSeciciAc());
        ayarlarContainer.addView(btnIceAktar);

        Button btnSifirla = aktifAltOgeOlustur("e - Tüm Ayarları Sıfırla");
        btnSifirla.setBackgroundColor(0xFF1A3A5A);
        btnSifirla.setOnClickListener(v -> tumAyarlariSifirla());
        ayarlarContainer.addView(btnSifirla);

        btnGeri.postDelayed(() -> btnGeri.requestFocus(), 100);
    }

    private void tumAyarlariSifirla() {
        new AlertDialog.Builder(activity)
                .setTitle("Tüm Ayarları Sıfırla")
                .setMessage("Gizleme ve sıralama ayarlarının tamamı sıfırlanacak. Emin misiniz?")
                .setPositiveButton("Evet", (dialog, which) -> {
                    activity.getSharedPreferences("gizli_kanallar", android.content.Context.MODE_PRIVATE)
                            .edit().clear().apply();
                    activity.getSharedPreferences("gizli_kategoriler", android.content.Context.MODE_PRIVATE)
                            .edit().clear().apply();
                    activity.getSharedPreferences("gizli_m3ular", android.content.Context.MODE_PRIVATE)
                            .edit().clear().apply();
                    activity.getSharedPreferences("kategori_siralama", android.content.Context.MODE_PRIVATE)
                            .edit().clear().apply();
                    activity.getSharedPreferences("favoriler", android.content.Context.MODE_PRIVATE)
                            .edit().clear().apply();
                    activity.getSharedPreferences("AppSettings", android.content.Context.MODE_PRIVATE)
                            .edit().remove("last_played_url").apply();
                    com.example.livetvapp.database.KategoriCache.getInstance().invalidateAll();
                    com.example.livetvapp.database.DiziCache.getInstance().invalidateAll();
                    if (siralamaPaneli != null) siralamaPaneli.veriGecersizKil();
                    new Thread(() -> {
                        try {
                            com.example.livetvapp.database.AppDatabase db =
                                    com.example.livetvapp.database.AppDatabase.getInstance(activity);
                            db.channelDao().resetAllPositions();
                            db.channelDao().deleteAllPlaybackPositions();
                            System.out.println("✅ Tüm kanal pozisyonları sıfırlandı");
                        } catch (Exception e) {
                            System.out.println("❌ resetAllPositions hatası: " + e.getMessage());
                        }
                    }).start();
                    if (gizleKapandiListener != null) {
                        gizleKapandiListener.onGizleKapandi(
                                gizlePaneli != null ? gizlePaneli.getM3uKategoriOnbellegi()
                                        : new java.util.HashMap<>());
                    }
                    if (sifirlamaListener != null) sifirlamaListener.onAyarlarSifirlandı();
                    Toast.makeText(activity, "✅ Tüm ayarlar sıfırlandı (favoriler ve son izlenen dahil)", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void m3uListeleriGoster() {
        List<M3UItem> tumM3UListesi = m3uManager.getAllM3UItems();
        if (tumM3UListesi.isEmpty()) {
            Toast.makeText(activity, "Henüz M3U listesi yok!", Toast.LENGTH_SHORT).show();
            return;
        }
        m3uSilmeMenusuGoster();
    }

    public void m3uSilmeMenusuGoster() {
        menuDegisti(MenuSeviyesi.M3U_SILME);
        listeModuGoster();
        TextView baslik = panelView.findViewById(R.id.panelBaslik);
        if (baslik != null) baslik.setText("M3U SİL");
        listeMenuContainer.removeAllViews();
        Button btnGeri = altMenuGeriButonOlustur("← M3U Yönetimi");
        btnGeri.setOnClickListener(v -> m3uYonetimMenuGoster());
        listeMenuContainer.addView(btnGeri);
        listeMenuContainer.addView(ayiriciOlustur());
        List<M3UItem> m3uItems = m3uManager.getAllM3UItems();
        if (m3uItems == null || m3uItems.isEmpty()) {
            TextView mesaj = new TextView(activity);
            mesaj.setText("Silinecek M3U bulunamadı");
            mesaj.setTextColor(0xFFAAAAAA);
            mesaj.setTextSize(14);
            mesaj.setPadding(16, 20, 16, 20);
            mesaj.setFocusable(false);
            listeMenuContainer.addView(mesaj);
            return;
        }
        silmeRecyclerView = new RecyclerView(activity);
        silmeRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
        silmeAdapter = new M3USilmeAdapter(m3uItems);
        silmeRecyclerView.setAdapter(silmeAdapter);
        silmeRecyclerView.setFocusable(false);
        silmeRecyclerView.setFocusableInTouchMode(false);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0);
        rp.weight = 1;
        rp.setMargins(0, 8, 0, 8);
        silmeRecyclerView.setLayoutParams(rp);
        listeMenuContainer.addView(silmeRecyclerView);
        Button tumunuSecBtn = aktifAltOgeOlustur("✓ Tümünü Seç");
        tumunuSecBtn.setOnClickListener(v -> {
            if (silmeAdapter != null) {
                silmeAdapter.selectAll();
                Toast.makeText(activity, "Tümü seçildi", Toast.LENGTH_SHORT).show();
            }
        });
        listeMenuContainer.addView(tumunuSecBtn);
        Button secimiTemizleBtn = aktifAltOgeOlustur("✗ Seçimi Temizle");
        secimiTemizleBtn.setOnClickListener(v -> {
            if (silmeAdapter != null) {
                silmeAdapter.clearSelection();
                Toast.makeText(activity, "Seçim temizlendi", Toast.LENGTH_SHORT).show();
            }
        });
        listeMenuContainer.addView(secimiTemizleBtn);
        Button silBtn = aktifAltOgeOlustur("🗑️ Seçilenleri Sil");
        silBtn.setBackgroundColor(0xFF1A3A5A);
        silBtn.setOnClickListener(v -> {
            if (silmeAdapter != null) {
                List<String> secilenler = silmeAdapter.getSelectedM3UNames();
                if (secilenler.isEmpty()) {
                    Toast.makeText(activity, "Lütfen silinecek M3U'ları seçin", Toast.LENGTH_SHORT).show();
                    return;
                }
                new AlertDialog.Builder(activity)
                        .setTitle("M3U Sil")
                        .setMessage(secilenler.size() + " M3U silinecek. Emin misiniz?")
                        .setPositiveButton("Evet", (dialog, which) -> {
                            int silinenSayisi = m3uManager.deleteMultipleM3U(secilenler);
                            silinenlereAitPrefsTemizle(secilenler);
                            Toast.makeText(activity, silinenSayisi + " M3U silindi", Toast.LENGTH_SHORT).show();
                            if (siralamaPaneli != null) siralamaPaneli.veriGecersizKil();
                            if (m3uSilmeListener != null) m3uSilmeListener.onM3USilindi();
                            m3uSilmeMenusuGoster();
                        })
                        .setNegativeButton("İptal", null)
                        .show();
            }
        });
        listeMenuContainer.addView(silBtn);
        silmeRecyclerView.postDelayed(() -> {
            RecyclerView.ViewHolder holder = silmeRecyclerView.findViewHolderForAdapterPosition(0);
            if (holder != null && holder.itemView != null) holder.itemView.requestFocus();
        }, 300);
    }

    private void silinenlereAitPrefsTemizle(List<String> silinenM3UAdlari) {
        if (silinenM3UAdlari == null || silinenM3UAdlari.isEmpty()) return;



        new Thread(() -> {
            try {
                com.example.livetvapp.database.AppDatabase db =
                        com.example.livetvapp.database.AppDatabase.getInstance(activity);


                java.util.Set<String> silinecekUrller = new java.util.HashSet<>();
                java.util.Set<String> silinecekKategoriler = new java.util.HashSet<>();
                for (String m3uAdi : silinenM3UAdlari) {
                    List<String> urls = db.channelDao().getAllUrlsByM3U(m3uAdi);
                    if (urls != null) silinecekUrller.addAll(urls);
                    List<String> kats = db.channelDao().getAllCategoriesByM3U(m3uAdi);
                    if (kats != null) silinecekKategoriler.addAll(kats);
                }


                android.content.SharedPreferences spKanallar =
                        activity.getSharedPreferences("gizli_kanallar", android.content.Context.MODE_PRIVATE);
                android.content.SharedPreferences.Editor edKanallar = spKanallar.edit();
                for (String url : silinecekUrller) {
                    edKanallar.remove(url);
                }
                edKanallar.apply();



                java.util.Set<String> korunacakKategoriler = new java.util.HashSet<>();
                List<com.example.livetvapp.database.M3UItem> kalanM3Ular = m3uManager.getAllM3UItems();
                for (com.example.livetvapp.database.M3UItem m3u : kalanM3Ular) {
                    List<String> kats = db.channelDao().getAllCategoriesByM3U(m3u.getName());
                    if (kats != null) korunacakKategoriler.addAll(kats);
                }
                android.content.SharedPreferences spKategoriler =
                        activity.getSharedPreferences("gizli_kategoriler", android.content.Context.MODE_PRIVATE);
                android.content.SharedPreferences.Editor edKategoriler = spKategoriler.edit();
                for (String kat : silinecekKategoriler) {
                    if (!korunacakKategoriler.contains(kat)) {
                        edKategoriler.remove(kat);
                    }
                }
                edKategoriler.apply();


                android.content.SharedPreferences spM3ular =
                        activity.getSharedPreferences("gizli_m3ular", android.content.Context.MODE_PRIVATE);
                android.content.SharedPreferences.Editor edM3ular = spM3ular.edit();
                for (String m3uAdi : silinenM3UAdlari) {
                    edM3ular.remove(m3uAdi);
                }
                edM3ular.apply();




                android.content.SharedPreferences spSiralama =
                        activity.getSharedPreferences("kategori_siralama", android.content.Context.MODE_PRIVATE);
                android.content.SharedPreferences.Editor edSiralama = spSiralama.edit();
                String[] turKodlari = {"LIVE", "MOVIE", "SERIES"};
                for (String tur : turKodlari) {
                    String kayit = spSiralama.getString(tur + "_sirasi", null);
                    if (kayit == null || kayit.isEmpty()) continue;
                    String[] parcalar = kayit.split("###");


                    StringBuilder yeniKayit = new StringBuilder();
                    for (int i = 0; i + 1 < parcalar.length; i += 2) {
                        String m3uAdi = parcalar[i].trim();
                        String katAdi = parcalar[i + 1].trim();
                        if (!silinenM3UAdlari.contains(m3uAdi)) {
                            if (yeniKayit.length() > 0) yeniKayit.append("###");
                            yeniKayit.append(m3uAdi).append("###").append(katAdi);
                        }
                    }
                    edSiralama.putString(tur + "_sirasi", yeniKayit.toString());
                }
                edSiralama.apply();

                System.out.println("🧹 [SİLME] Prefs temizlendi → M3U: " + silinenM3UAdlari
                        + " | URL: " + silinecekUrller.size()
                        + " | Kategori: " + silinecekKategoriler.size());
            } catch (Exception e) {
                System.out.println("❌ [SİLME] Prefs temizleme hatası: " + e.getMessage());
            }
        }).start();
    }

    private void m3uUrldenYukle(String url) {


        String[] xtreamAuth = xtreamKimliginiCikar(url);
        if (xtreamAuth != null) {

            String server = xtreamAuth[0];
            String username = xtreamAuth[1];
            String password = xtreamAuth[2];
            String sourceName;
            try {
                java.net.URL parsedUrl = new java.net.URL(url);
                String host = parsedUrl.getHost();
                String kisaHost = kisaltHost(host);
                sourceName = kisaHost + "(" + username + ")";
            } catch (Exception e) {
                sourceName = "Xtream_" + System.currentTimeMillis();
            }
            final String finalSourceName = sourceName;
            Toast.makeText(activity, "⏳ Xtream API ile yükleniyor: " + finalSourceName, Toast.LENGTH_LONG).show();
            if (yuklemeListener != null) yuklemeListener.yuklemeBasladi(finalSourceName);
            xtreamYukle(server, username, password, finalSourceName);
            return;
        }
        String m3uAdi;
        try {
            java.net.URL parsedUrl = new java.net.URL(url);
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
                m3uAdi = kisaHost + "(" + kullaniciAdi + ")";
            } else {
                String path = parsedUrl.getPath();
                String sonParca = path.substring(path.lastIndexOf('/') + 1)
                        .replaceAll("\\.[^.]+$", "");
                m3uAdi = (!sonParca.isEmpty()) ? kisaHost + "(" + sonParca + ")" : kisaHost;
            }
        } catch (Exception e) {
            m3uAdi = "URL_" + System.currentTimeMillis();
        }

        final String finalM3uAdi = m3uAdi;
        Toast.makeText(activity, "⏳ M3U yükleniyor: " + finalM3uAdi, Toast.LENGTH_LONG).show();
        if (yuklemeListener != null) yuklemeListener.yuklemeBasladi(finalM3uAdi);

        Handler mainHandler = new Handler(Looper.getMainLooper());
        ChannelRepository channelRepository = new ChannelRepository(activity);

        new Thread(() -> {
            try {
                List<Channel> kanallar = KanalListesi.getChannelsFromUrl(url);
                mainHandler.post(() -> {
                    if (kanallar == null || kanallar.isEmpty()) {
                        if (yuklemeListener != null) yuklemeListener.yuklemeBitti();
                        Toast.makeText(activity, "❌ Kanal bulunamadı.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    for (Channel ch : kanallar) ch.setSourceName(finalM3uAdi);


                    boolean xtreamDenenmeli = xtreamDenemesiGerekiyor(kanallar, url);
                    System.out.println("🔍 [M3U-URL] Xtream denemesi gerekiyor mu: " + xtreamDenenmeli);

                    if (xtreamDenenmeli) {

                        String[] kimlik = xtreamKimliginiCikar(url);
                        if (kimlik != null) {
                            String server   = kimlik[0];
                            String username = kimlik[1];
                            String password = kimlik[2];
                            System.out.println("🔍 [M3U-URL] Xtream bilgileri bulundu → sessizce deneniyor");


                            XtreamCodesManager.loadChannels(activity, server, username, password,
                                    finalM3uAdi,
                                    new XtreamCodesManager.OnXtreamLoadListener() {
                                        @Override
                                        public void onStarted() {}

                                        @Override
                                        public void onProgress(String message) {
                                            System.out.println("🔄 [M3U-URL Xtream] " + message);
                                        }

                                        @Override
                                        public void onSuccess(List<Channel> xtreamKanallar, String srcName) {
                                            mainHandler.post(() -> {
                                                System.out.println("✅ [M3U-URL] Xtream başarılı → "
                                                        + xtreamKanallar.size() + " kanal");

                                                kaydetVeBitir(xtreamKanallar, finalM3uAdi, url,
                                                        server, username, password,
                                                        channelRepository, mainHandler, true);
                                            });
                                        }

                                        @Override
                                        public void onError(String error) {
                                            mainHandler.post(() -> {
                                                System.out.println("⚠️ [M3U-URL] Xtream başarısız → "
                                                        + "normal parse kullanılıyor. Hata: " + error);

                                                kaydetVeBitir(kanallar, finalM3uAdi, url,
                                                        null, null, null,
                                                        channelRepository, mainHandler, false);
                                            });
                                        }
                                    });
                            return;
                        }
                    }



                    kaydetVeBitir(kanallar, finalM3uAdi, url,
                            null, null, null,
                            channelRepository, mainHandler, false);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (yuklemeListener != null) yuklemeListener.yuklemeBitti();
                    Toast.makeText(activity,
                            "❌ Yükleme hatası: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    menuDegisti(MenuSeviyesi.ANA_MENU);
                });
            }
        }).start();
    }


    private boolean xtreamDenemesiGerekiyor(List<Channel> kanallar, String url) {
        if (kanallar == null || kanallar.isEmpty()) return false;


        if (xtreamKimliginiCikar(url) == null) return false;

        int toplamKanal = kanallar.size();
        int liveSayisi  = 0;
        java.util.Set<String> kategoriler = new java.util.HashSet<>();

        for (Channel ch : kanallar) {
            if ("LIVE".equals(ch.getContentType())) liveSayisi++;
            if (ch.getCategory() != null && !ch.getCategory().isEmpty()) {
                kategoriler.add(ch.getCategory());
            }
        }

        double liveOrani = (double) liveSayisi / toplamKanal;


        boolean tumKanallarlive = liveOrani >= 0.95;
        boolean kategoriZayif   = kategoriler.size() <= 3;

        System.out.println("🔍 [KALİTE] toplamKanal=" + toplamKanal
                + " liveSayisi=" + liveSayisi
                + " liveOrani=" + String.format("%.2f", liveOrani)
                + " kategoriSayisi=" + kategoriler.size()
                + " tumKanallarlive=" + tumKanallarlive
                + " kategoriZayif=" + kategoriZayif);

        return tumKanallarlive || kategoriZayif;
    }


    private String[] xtreamKimliginiCikar(String urlStr) {
        try {
            java.net.URL parsedUrl = new java.net.URL(urlStr);
            String server   = parsedUrl.getProtocol() + "://" + parsedUrl.getHost()
                    + (parsedUrl.getPort() != -1 ? ":" + parsedUrl.getPort() : "");
            String username = null;
            String password = null;


            String query = parsedUrl.getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("username="))
                        username = param.substring("username=".length());
                    else if (param.startsWith("password="))
                        password = param.substring("password=".length());
                }
            }


            if (username == null || password == null) {
                String path = parsedUrl.getPath();
                String[] parcalar = path.split("/");

                if (parcalar.length >= 3) {
                    String p1 = parcalar[1];
                    String p2 = parcalar[2];
                    boolean p1Gecerli = !p1.isEmpty()
                            && !p1.equals("get.php")
                            && !p1.equals("live")
                            && !p1.equals("movie")
                            && !p1.equals("series");
                    boolean p2Gecerli = !p2.isEmpty()
                            && !p2.equals("get.php")
                            && !p2.equals("live")
                            && !p2.equals("movie")
                            && !p2.equals("series");
                    if (p1Gecerli && p2Gecerli) {
                        username = p1;
                        password = p2;
                    }
                }
            }

            if (username != null && !username.isEmpty()
                    && password != null && !password.isEmpty()) {
                return new String[]{server, username, password};
            }
        } catch (Exception e) {
            System.out.println("⚠️ [xtreamKimliginiCikar] " + e.getMessage());
        }
        return null;
    }


    private void kaydetVeBitir(List<Channel> kanallar,
                               String m3uAdi,
                               String url,
                               String xtreamServer,
                               String xtreamUser,
                               String xtreamPass,
                               ChannelRepository channelRepository,
                               Handler mainHandler,
                               boolean xtreamBasarili) {
        M3UItem yeniM3U = new M3UItem();
        yeniM3U.setName(m3uAdi);
        yeniM3U.setUrl(url);
        yeniM3U.setLocalFile(false);
        yeniM3U.setChannelCount(kanallar.size());
        m3uManager.addM3U(yeniM3U);

        if (siralamaPaneli != null) siralamaPaneli.veriGecersizKil();


        if (xtreamBasarili && xtreamServer != null) {
            XtreamCodesManager.saveAccount(activity, m3uAdi, xtreamServer, xtreamUser, xtreamPass);
            System.out.println("✅ [M3U-URL] Xtream hesabı kaydedildi: " + m3uAdi);
        }

        channelRepository.insertChannels(kanallar, new ChannelRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                mainHandler.post(() -> {
                    if (yuklemeListener != null) yuklemeListener.yuklemeBitti();
                    String mesaj = xtreamBasarili
                            ? "✅ " + kanallar.size() + " kanal yüklendi (Xtream): " + m3uAdi
                            : "✅ " + kanallar.size() + " kanal yüklendi: " + m3uAdi;
                    Toast.makeText(activity, mesaj, Toast.LENGTH_LONG).show();
                    if (m3uYuklemeListener != null) m3uYuklemeListener.onM3UYuklendi(url);
                    saveLastM3UUrl(url);
                });
            }

            @Override
            public void onError(Exception e) {
                mainHandler.post(() -> {
                    if (yuklemeListener != null) yuklemeListener.yuklemeBitti();
                    Toast.makeText(activity,
                            "❌ DB kayıt hatası: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    menuDegisti(MenuSeviyesi.ANA_MENU);
                });
            }
        });
    }

    private void uygulamayiKapat() {
        activity.finishAffinity();
        System.exit(0);
    }

    private void cikisYapButonuTiklandi() {
        SharedPreferences girisTercihleri = activity.getSharedPreferences("azsh_giris", Context.MODE_PRIVATE);
        String emailBilgisi = girisTercihleri.getString("email", "Oturum açık");
        if (emailBilgisi == null || emailBilgisi.isEmpty()) emailBilgisi = "Oturum açık";
        new AlertDialog.Builder(activity)
                .setTitle("Çıkış Yap")
                .setMessage("Oturumu kapatmak istediğinize emin misiniz?\n(" + emailBilgisi + ")")
                .setPositiveButton("Evet", (dialog, which) -> {
                    girisTercihleri.edit().clear().apply();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        Intent intent = new Intent(activity, com.example.livetvapp.LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        activity.startActivity(intent);
                        activity.finish();
                    }, 800);
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void normalModuGoster() {
        if (scrollView         != null) scrollView.setVisibility(View.VISIBLE);
        if (listeMenuContainer != null) listeMenuContainer.setVisibility(View.GONE);
    }

    private void listeModuGoster() {
        if (scrollView         != null) scrollView.setVisibility(View.GONE);
        if (listeMenuContainer != null) listeMenuContainer.setVisibility(View.VISIBLE);
    }

    private View ayiriciOlustur() {
        View cizgi = new View(activity);
        cizgi.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        cizgi.setBackgroundColor(0x44FFFFFF);
        return cizgi;
    }

    private Button anaKategoriButonOlustur(String text) {
        Button btn = (Button) LayoutInflater.from(activity)
                .inflate(R.layout.kanal_buton, ayarlarContainer, false);
        btn.setText(text);
        btn.setBackgroundColor(RENK_NORMAL);
        btn.setTextColor(0xFFFFFFFF);
        btn.setTextSize(13);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setAllCaps(false);
        btn.setPadding(12, 12, 12, 12);
        btn.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        btn.setOnFocusChangeListener((v, hasFocus) -> {
            v.setBackgroundColor(hasFocus ? RENK_FOCUS : RENK_NORMAL);
            v.setPadding(hasFocus ? 16 : 12, 12, 12, 12);
        });
        return btn;
    }

    private Button altMenuGeriButonOlustur(String text) {
        Button btn = (Button) LayoutInflater.from(activity)
                .inflate(R.layout.kanal_buton, ayarlarContainer, false);
        btn.setText(text);
        btn.setBackgroundColor(RENK_GERI);
        btn.setTextColor(0xFFFFFFFF);
        btn.setTextSize(13);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setPadding(12, 12, 12, 12);
        btn.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        btn.setOnFocusChangeListener((v, hasFocus) -> {
            v.setBackgroundColor(hasFocus ? RENK_FOCUS : RENK_GERI);
            v.setPadding(hasFocus ? 16 : 12, 12, 12, 12);
        });
        return btn;
    }

    private Button aktifAltOgeOlustur(String text) {
        Button btn = (Button) LayoutInflater.from(activity)
                .inflate(R.layout.kanal_buton, ayarlarContainer, false);
        btn.setText(text);
        btn.setBackgroundColor(RENK_ALT_OGE);
        btn.setTextColor(0xFFFFFFFF);
        btn.setTextSize(12);
        btn.setPadding(16, 10, 10, 10);
        btn.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        btn.setOnFocusChangeListener((v, hasFocus) -> {
            v.setBackgroundColor(hasFocus ? RENK_FOCUS : RENK_ALT_OGE);
            v.setPadding(hasFocus ? 20 : 16, 10, 10, 10);
        });
        return btn;
    }

    private String hostFromUrl(String urlStr) {
        try {
            java.net.URL url = new java.net.URL(urlStr);
            String host = url.getHost();
            if (host != null && !host.isEmpty()) {
                return host.startsWith("www.") ? host.substring(4) : host;
            }
        } catch (Exception ignored) {}
        return "Xtream_" + System.currentTimeMillis();
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

    private void dosyaSeciciAc() {
        Intent intent = new Intent(activity, DosyaSecicipaneli.class);
        intent.putExtra("mode", "m3u_import");
        activity.startActivityForResult(intent, DOSYA_SECICI_REQUEST);
    }

    private void yedekDosyaSeciciAc() {
        Intent intent = new Intent(activity, DosyaSecicipaneli.class);
        intent.putExtra("mode", "backup_import");
        activity.startActivityForResult(intent, YEDEK_SECICI_REQUEST);
    }
    private void disaAktarIzinKontrol() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (android.os.Environment.isExternalStorageManager()) {
                disaAktarBaslat();
            } else {
                new androidx.appcompat.app.AlertDialog.Builder(activity)
                        .setTitle("Dosya Erişim İzni Gerekli")
                        .setMessage("Yedek dosyasını Downloads klasörüne kaydedebilmek için depolama erişim izni gerekiyor.")
                        .setPositiveButton("İzin Ver", (dialog, which) -> {
                            try {
                                android.content.Intent intent = new android.content.Intent(
                                        android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                                intent.setData(android.net.Uri.parse("package:" + activity.getPackageName()));
                                activity.startActivityForResult(intent, 998);
                            } catch (Exception e) {
                                android.content.Intent intent = new android.content.Intent(
                                        android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                                activity.startActivityForResult(intent, 998);
                            }
                        })
                        .setNegativeButton("İptal", null)
                        .show();
            }
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(activity,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                disaAktarBaslat();
            } else {
                androidx.core.app.ActivityCompat.requestPermissions(activity,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        997);
            }
        } else {
            disaAktarBaslat();
        }
    }

    public void disaAktarBaslat() {
        Toast.makeText(activity, "⏳ Yedekleniyor...", Toast.LENGTH_SHORT).show();
        yedekYonetici.disaAktar(new YedekYonetici.YedekCallback() {
            @Override
            public void onBasarili(String mesaj) {
                new android.app.AlertDialog.Builder(activity)
                        .setTitle("Yedekleme Tamamlandı")
                        .setMessage(mesaj)
                        .setPositiveButton("Tamam", null)
                        .show();
            }
            @Override
            public void onHata(String hata) {
                Toast.makeText(activity, hata, Toast.LENGTH_LONG).show();
            }
            @Override
            public void onIlerleme(String mesaj) {}
        });
    }




    private void saveLastM3UUrl(String url) {
        activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_LAST_M3U_URL, url).apply();
    }

    private String getLastM3UUrl() {
        return activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LAST_M3U_URL, null);
    }

    private void saveLastXtream(String server, String username, String password) {
        activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_XTREAM_SERVER, server)
                .putString(KEY_LAST_XTREAM_USER, username)
                .putString(KEY_LAST_XTREAM_PASS, password)
                .apply();
    }

    private String[] getLastXtream() {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String server = prefs.getString(KEY_LAST_XTREAM_SERVER, null);
        String user = prefs.getString(KEY_LAST_XTREAM_USER, null);
        String pass = prefs.getString(KEY_LAST_XTREAM_PASS, null);
        if (server == null || user == null || pass == null) return null;
        return new String[]{server, user, pass};
    }

    private void stalkerHesapDiyaloguGoster() {
        new AlertDialog.Builder(activity)
                .setTitle("Stalker Portal Ekle")
                .setItems(new String[]{"Yeni Ekle", "Sonuncuyu Düzenle"}, (dialog, which) -> {
                    if (which == 0) {
                        stalkerFormunuGoster(null, null);
                    } else {
                        String[] last = getLastStalker();
                        if (last == null) {
                            Toast.makeText(activity, "Kayıtlı son Stalker portalı bulunamadı.", Toast.LENGTH_SHORT).show();
                            stalkerFormunuGoster(null, null);
                        } else {
                            stalkerFormunuGoster(last[0], last[1]);
                        }
                    }
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void saveLastStalker(String url, String mac) {
        activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_STALKER_URL, url)
                .putString(KEY_LAST_STALKER_MAC, mac)
                .apply();
    }

    private String[] getLastStalker() {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String url = prefs.getString(KEY_LAST_STALKER_URL, null);
        String mac = prefs.getString(KEY_LAST_STALKER_MAC, null);
        if (url == null || mac == null) return null;
        return new String[]{url, mac};
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 998) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                if (android.os.Environment.isExternalStorageManager()) {
                    disaAktarBaslat();
                } else {
                    Toast.makeText(activity, "İzin verilmedi, dışa aktarma iptal edildi.", Toast.LENGTH_SHORT).show();
                }
            }
            return;
        }
        if (requestCode == YEDEK_SECICI_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            String dosyaYolu = data.getStringExtra("selected_file_path");
            if (dosyaYolu != null) {
                Toast.makeText(activity, "⏳ Yedek yükleniyor...", Toast.LENGTH_LONG).show();
                yedekYonetici.iceAktar(dosyaYolu, new YedekYonetici.YedekCallback() {
                    @Override
                    public void onBasarili(String mesaj) {
                        new android.app.AlertDialog.Builder(activity)
                                .setTitle("İçe Aktarma Tamamlandı")
                                .setMessage(mesaj)
                                .setPositiveButton("Yeniden Başlat", (dialog, which) -> {
                                    activity.finishAffinity();
                                    Intent intent = new Intent(activity, com.example.livetvapp.MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    activity.startActivity(intent);
                                })
                                .setNegativeButton("Daha Sonra", null)
                                .show();
                    }
                    @Override
                    public void onHata(String hata) {
                        Toast.makeText(activity, hata, Toast.LENGTH_LONG).show();
                    }
                    @Override
                    public void onIlerleme(String mesaj) {
                        Toast.makeText(activity, mesaj, Toast.LENGTH_SHORT).show();
                    }
                });
            }
            return;
        }
        if (requestCode == DOSYA_SECICI_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            String dosyaYolu = data.getStringExtra("selected_file_path");
            if (dosyaYolu != null && m3uYuklemeListener != null) {
                if (siralamaPaneli != null) siralamaPaneli.veriGecersizKil();
                gizle();
                m3uYuklemeListener.onM3UYuklendi(dosyaYolu);
                Toast.makeText(activity, "M3U yükleniyor...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void anaMenuyeDon() { anaMenuyuOlustur(); }

    public void goster() {

        Button btnCikis = panelView.findViewById(R.id.btnCikisYap);
        if (btnCikis != null) {
            btnCikis.setVisibility(View.GONE);
        }
        if (panelView != null) {
            panelView.setVisibility(View.VISIBLE);
            anaMenuyuOlustur();
            if (scrollView != null) scrollView.postDelayed(() -> scrollView.scrollTo(0, 0), 50);
        }
    }

    public void gizle() {
        if (panelView != null) {
            panelView.setVisibility(View.GONE);
            panelView.clearFocus();
            if (ayarlarContainer != null) ayarlarContainer.clearFocus();
            if (scrollView != null) scrollView.clearFocus();
        }
        if (gizlePaneli    != null) gizlePaneli.gizle();
        if (siralamaPaneli != null) siralamaPaneli.gizle();
        if (ozelKlavye     != null) { ozelKlavye.gizle(); panelGenislikAyarla(false); }
        menuDegisti(MenuSeviyesi.ANA_MENU);
        if (panelKapandiListener != null) panelKapandiListener.onAyarlarKapandi();
    }

    public boolean isVisible() {
        return panelView != null && panelView.getVisibility() == View.VISIBLE;
    }

    public boolean handleBack() {
        switch (mevcutMenu) {
            case ANA_MENU:
                return false;
            case M3U_YONETIMI:
                anaMenuyuOlustur();
                return true;
            case YEDEKLEME:
                anaMenuyuOlustur();
                return true;
            case M3U_YUKLE:
                ozelKlavye.gizle();
                panelGenislikAyarla(false);
                m3uYonetimMenuGoster();
                return true;
            case M3U_SILME:
                m3uYonetimMenuGoster();
                return true;
            case XTREAM_MENU:
                ozelKlavye.gizle();
                panelGenislikAyarla(false);
                m3uYukleMenuGoster();
                return true;
            case M3U_URL:
                ozelKlavye.gizle();
                panelGenislikAyarla(false);
                m3uYukleMenuGoster();
                return true;
            case GIZLE_PANELI:
                System.out.println("🔙 AyarlarPaneli.handleBack GIZLE_PANELI");
                System.out.println("🔙 m3uDurumPaneli visible=" + (m3uDurumPaneli != null && m3uDurumPaneli.isVisible()));
                System.out.println("🔙 gizlePaneli null=" + (gizlePaneli == null));
                if (gizlePaneli != null) System.out.println("🔙 gizlePaneli visible=" + gizlePaneli.isVisible());
                if (m3uDurumPaneli != null && m3uDurumPaneli.isVisible()) return m3uDurumPaneli.handleBack();
                if (gizlePaneli != null) return gizlePaneli.handleBack();
                anaMenuyuOlustur();
                return true;
            case SIRALAMA_PANELI:
                System.out.println("🔙 AyarlarPaneli.handleBack SIRALAMA_PANELI");
                System.out.println("🔙 siralamaPaneli null=" + (siralamaPaneli == null));
                if (siralamaPaneli != null) System.out.println("🔙 siralamaPaneli visible=" + siralamaPaneli.isVisible());
                if (siralamaPaneli != null) return siralamaPaneli.handleBack();
                anaMenuyuOlustur();
                return true;
            case M3U_DURUM_PANELI:
                if (m3uDurumPaneli != null) return m3uDurumPaneli.handleBack();
                m3uYonetimMenuGoster();
                return true;
            default:
                anaMenuyuOlustur();
                return true;
        }
    }

    public View getPanelView()               { return panelView; }
    public MenuSeviyesi getMevcutMenu()      { return mevcutMenu; }
    public M3USilmeAdapter getSilmeAdapter() { return silmeAdapter; }
    public GizlePaneli getGizlePaneli()      { return gizlePaneli; }
    public SiralamaPaneli getSiralamaPaneli()                                  { return siralamaPaneli; }
    public com.example.livetvapp.paneller.M3UDurumPaneli getM3UDurumPaneli() { return m3uDurumPaneli; }

    public boolean panelFocusAsagi() {
        return panelFocusHareketEt(true);
    }

    public boolean panelFocusYukari() {
        return panelFocusHareketEt(false);
    }

    private boolean panelFocusHareketEt(boolean asagi) {
        if (ayarlarContainer == null) return false;
        java.util.List<android.view.View> odaklanabilir = new java.util.ArrayList<>();
        for (int i = 0; i < ayarlarContainer.getChildCount(); i++) {
            android.view.View child = ayarlarContainer.getChildAt(i);
            if (child.isFocusable() && child.getVisibility() == android.view.View.VISIBLE) {
                odaklanabilir.add(child);
            }
        }
        if (odaklanabilir.isEmpty()) return false;
        android.view.View simdiki = activity.getCurrentFocus();
        int simdikiIdx = odaklanabilir.indexOf(simdiki);
        if (simdikiIdx < 0) {
            odaklanabilir.get(asagi ? 0 : odaklanabilir.size() - 1).requestFocus();
            return true;
        }
        int hedefIdx = asagi ? simdikiIdx + 1 : simdikiIdx - 1;
        if (hedefIdx < 0 || hedefIdx >= odaklanabilir.size()) return true;
        odaklanabilir.get(hedefIdx).requestFocus();
        return true;
    }

    public void m3uYuklePanelineGit() {
        m3uYukleMenuGoster();
    }
}