package com.example.livetvapp.metadata;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.livetvapp.paneller.DiziPaneli;
import com.example.livetvapp.paneller.Icerikpaneli;
import com.example.livetvapp.paneller.KategoriPaneli;
import com.example.livetvapp.paneller.normalkanallistesipaneli;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class metadataPanel {
    private static final int DEBOUNCE_MS = 450;
    private KategoriPaneli           kategoriPaneli     = null;
    private normalkanallistesipaneli normalKanalListesi = null;
    private DiziPaneli               diziPaneli         = null;
    private final AppCompatActivity activity;
    private final Handler           mainHandler   = new Handler(Looper.getMainLooper());
    private final ExecutorService   imageExecutor = Executors.newFixedThreadPool(6);
    private Runnable bekleyenIstek = null;
    private FrameLayout  rootView;
    private ImageView    posterView;
    private ImageView    backdropView;
    private TextView     baslikView;
    private TextView     yilSureDilView;
    private TextView     yonetmenView;
    private TextView     turlerView;
    private TextView     imdbView;
    private TextView     tmdbView;
    private TextView     yasView;
    private TextView     konuView;
    private LinearLayout oyuncularLayout;
    private ProgressBar  yukleniyorView;
    private LinearLayout bosKapsayici;
    private TextView     bosIkon;
    private TextView     bosView;
    private LinearLayout     ustSatir;
    private LinearLayout     puanlarSatir;
    private TextView         oyuncularBaslik;
    private HorizontalScrollView oyuncularHsv;
    private static final int RENK_IMDB        = 0xFFf5c518;
    private static final int RENK_TMDB        = 0xFF01d277;
    private static final int RENK_METIN       = 0xFFf0f0f5;
    private static final int RENK_SOLUK       = 0xFF6b6b80;
    private static final int RENK_KONU        = 0xBBf0f0f5;
    private static final int RENK_ARKA        = 0xFF0f0f1a;
    private static final int RENK_BOLUM_ARKA  = 0x22ffffff;
    public metadataPanel(AppCompatActivity activity) {
        this.activity = activity;
    }
    public void olustur() {
        rootView = new FrameLayout(activity);
        rootView.setBackgroundColor(RENK_ARKA);
        rootView.setVisibility(View.GONE);
        backdropView = new ImageView(activity);
        backdropView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        backdropView.setAlpha(0.18f);
        FrameLayout.LayoutParams fullParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        rootView.addView(backdropView, fullParams);
        View gradyan = new View(activity);
        gradyan.setBackground(gradyanOlustur());
        rootView.addView(gradyan, fullParams);
        yukleniyorView = new ProgressBar(activity);
        yukleniyorView.setIndeterminate(true);
        yukleniyorView.setVisibility(View.GONE);
        FrameLayout.LayoutParams pbParams = new FrameLayout.LayoutParams(dp(36), dp(36));
        pbParams.gravity = Gravity.CENTER;
        rootView.addView(yukleniyorView, pbParams);
        ScrollView scrollView = new ScrollView(activity);
        scrollView.setVerticalScrollBarEnabled(false);
        rootView.addView(scrollView, fullParams);
        LinearLayout icerik = new LinearLayout(activity);
        icerik.setOrientation(LinearLayout.VERTICAL);
        int p = dp(20);
        icerik.setPadding(p, dp(24), p, dp(20));
        scrollView.addView(icerik, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
        ustSatir = new LinearLayout(activity);
        ustSatir.setOrientation(LinearLayout.HORIZONTAL);
        posterView = new ImageView(activity);
        posterView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        posterView.setBackgroundColor(0x22ffffff);
        LinearLayout.LayoutParams posterParams = new LinearLayout.LayoutParams(dp(110), dp(165));
        posterParams.setMarginEnd(dp(16));
        LinearLayout sagBilgiler = new LinearLayout(activity);
        sagBilgiler.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams sagParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        sagParams.gravity = Gravity.CENTER_VERTICAL;
        baslikView = new TextView(activity);
        baslikView.setTextSize(20);
        baslikView.setTextColor(RENK_METIN);
        baslikView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        baslikView.setMaxLines(2);
        baslikView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        yilSureDilView = new TextView(activity);
        yilSureDilView.setTextSize(22);
        yilSureDilView.setTextColor(RENK_SOLUK);
        LinearLayout.LayoutParams yilParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        yilParams.topMargin = dp(5);
        yonetmenView = new TextView(activity);
        yonetmenView.setTextSize(22);
        yonetmenView.setTextColor(RENK_SOLUK);
        yonetmenView.setVisibility(View.GONE);
        LinearLayout.LayoutParams yonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        yonParams.topMargin = dp(3);
        turlerView = new TextView(activity);
        turlerView.setTextSize(22);
        turlerView.setTextColor(0xFFe50914);
        LinearLayout.LayoutParams turParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        turParams.topMargin = dp(8);
        sagBilgiler.addView(baslikView);
        sagBilgiler.addView(yilSureDilView, yilParams);
        sagBilgiler.addView(yonetmenView,   yonParams);
        sagBilgiler.addView(turlerView,     turParams);
        ustSatir.addView(posterView,  posterParams);
        ustSatir.addView(sagBilgiler, sagParams);
        icerik.addView(ustSatir);
        konuView = new TextView(activity);
        konuView.setTextSize(24);
        konuView.setTextColor(RENK_KONU);
        konuView.setLineSpacing(dp(3), 1f);
        konuView.setMaxLines(6);
        konuView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams konuParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        konuParams.topMargin = dp(14);
        icerik.addView(konuView, konuParams);
        puanlarSatir = new LinearLayout(activity);
        puanlarSatir.setOrientation(LinearLayout.HORIZONTAL);
        puanlarSatir.setBackgroundColor(RENK_BOLUM_ARKA);
        int puanPadY = dp(0);
        puanlarSatir.setPadding(0, puanPadY, 0, puanPadY);
        LinearLayout.LayoutParams puanlarParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        puanlarParams.topMargin = dp(14);
        imdbView = bilgiKutuOlustur(RENK_IMDB);
        tmdbView = bilgiKutuOlustur(RENK_TMDB);
        yasView  = bilgiKutuOlustur(RENK_METIN);
        puanlarSatir.addView(bilgiKutuKapsayiciOlustur("IMDb", imdbView, 1f));
        puanlarSatir.addView(ayiriciOlustur());
        puanlarSatir.addView(bilgiKutuKapsayiciOlustur("TMDB", tmdbView, 1f));
        puanlarSatir.addView(ayiriciOlustur());
        puanlarSatir.addView(bilgiKutuKapsayiciOlustur("Yaş",  yasView,  1f));
        icerik.addView(puanlarSatir, puanlarParams);
        oyuncularBaslik = new TextView(activity);
        oyuncularBaslik.setText("OYUNCULAR");
        oyuncularBaslik.setTextSize(9);
        oyuncularBaslik.setTextColor(RENK_SOLUK);
        oyuncularBaslik.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams oyBParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        oyBParams.topMargin = dp(16);
        icerik.addView(oyuncularBaslik, oyBParams);
        oyuncularHsv = new HorizontalScrollView(activity);
        oyuncularHsv.setHorizontalScrollBarEnabled(false);
        oyuncularHsv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout.LayoutParams hsvParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hsvParams.topMargin = dp(10);
        oyuncularLayout = new LinearLayout(activity);
        oyuncularLayout.setOrientation(LinearLayout.HORIZONTAL);
        oyuncularHsv.addView(oyuncularLayout, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
        icerik.addView(oyuncularHsv, hsvParams);
        bosKapsayici = new LinearLayout(activity);
        bosKapsayici.setOrientation(LinearLayout.VERTICAL);
        bosKapsayici.setGravity(Gravity.CENTER);
        bosKapsayici.setVisibility(View.GONE);
        LinearLayout.LayoutParams bosKapParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        bosKapParams.topMargin    = dp(40);
        bosKapParams.bottomMargin = dp(20);
        bosIkon = new TextView(activity);
        bosIkon.setText("🎬");
        bosIkon.setTextSize(48);
        bosIkon.setGravity(Gravity.CENTER);
        TextView bosBaslik = new TextView(activity);
        bosBaslik.setText("İçerik Bulunamadı");
        bosBaslik.setTextSize(16);
        bosBaslik.setTextColor(RENK_METIN);
        bosBaslik.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        bosBaslik.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams bosBaslikParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        bosBaslikParams.topMargin = dp(12);
        bosView = new TextView(activity);
        bosView.setText("Bu içerik için bilgi\nveritabanında bulunamadı.");
        bosView.setTextColor(RENK_SOLUK);
        bosView.setTextSize(12);
        bosView.setGravity(Gravity.CENTER);
        bosView.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams bosAciklamaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        bosAciklamaParams.topMargin = dp(6);
        View bosAyirici = new View(activity);
        bosAyirici.setBackgroundColor(0x22ffffff);
        LinearLayout.LayoutParams bosAyiriciParams = new LinearLayout.LayoutParams(dp(60), dp(1));
        bosAyiriciParams.gravity      = Gravity.CENTER_HORIZONTAL;
        bosAyiriciParams.topMargin    = dp(16);
        bosAyiriciParams.bottomMargin = dp(16);
        TextView bosKaynak = new TextView(activity);
        bosKaynak.setText("TMDB · IMDb");
        bosKaynak.setTextSize(9);
        bosKaynak.setTextColor(0x44ffffff);
        bosKaynak.setGravity(Gravity.CENTER);
        bosKaynak.setLetterSpacing(0.15f);
        bosKapsayici.addView(bosIkon);
        bosKapsayici.addView(bosBaslik,  bosBaslikParams);
        bosKapsayici.addView(bosView,    bosAciklamaParams);
        bosKapsayici.addView(bosAyirici, bosAyiriciParams);
        bosKapsayici.addView(bosKaynak);
        icerik.addView(bosKapsayici, bosKapParams);
        

        int genislikDp = 450;
        int genislikPx = dp(genislikDp);
        FrameLayout.LayoutParams metaParams = new FrameLayout.LayoutParams(
                genislikPx,
                FrameLayout.LayoutParams.MATCH_PARENT);
        metaParams.gravity = Gravity.END | Gravity.TOP;
        activity.addContentView(rootView, metaParams);
        System.out.println("✅ MetadataPanel oluşturuldu");
    }
    public void setPanelReferanslari(
            KategoriPaneli           kategoriPaneli,
            normalkanallistesipaneli normalKanalListesi,
            DiziPaneli               diziPaneli) {
        this.kategoriPaneli     = kategoriPaneli;
        this.normalKanalListesi = normalKanalListesi;
        this.diziPaneli         = diziPaneli;
    }
    public void focusDegisti(String kanalAdi, String icerikTipi) {
        if (Icerikpaneli.TIP_CANLI_TV.equals(icerikTipi)) {
            return;  
        }
        if (kategoriPaneli != null && !kategoriPaneli.isVisible()) {
            gizle();
            return;
        }
        if (Icerikpaneli.TIP_FILM.equals(icerikTipi)) {
            if (normalKanalListesi != null && !normalKanalListesi.isVisible()) {
                gizle();
                return;
            }
        } else if (Icerikpaneli.TIP_DIZI.equals(icerikTipi)) {
            if (diziPaneli != null && !diziPaneli.isVisible()) {
                gizle();
                return;
            }
        }
        if (bekleyenIstek != null) mainHandler.removeCallbacks(bekleyenIstek);
        bekleyenIstek = () -> metadataYukle(kanalAdi, icerikTipi);
        mainHandler.postDelayed(bekleyenIstek, DEBOUNCE_MS);
    }
    private void metadataYukle(String kanalAdi, String icerikTipi) {
        yukleniyorGoster();
        TmdbService.getInstance().ara(kanalAdi, icerikTipi, new TmdbService.MetadataCallback() {
            @Override public void onBasarili(MetadataModel model) { panelGuncelle(model); }
            @Override public void onHata(String hata) {
                System.out.println("⚠️ [MetadataPanel] " + hata);
                mainHandler.post(() -> bosGoster());
            }
        });
    }
    private void panelGuncelle(MetadataModel model) {
        if (rootView == null || rootView.getVisibility() == View.GONE) return;
        System.out.println("🟢 [META] panelGuncelle() çağrıldı | rootView visibility=" + (rootView != null ? rootView.getVisibility() : "?"));
        if (rootView == null) return;
        bosKapsayici.setVisibility(View.GONE);
        ustSatir.setVisibility(View.VISIBLE);
        konuView.setVisibility(View.VISIBLE);
        puanlarSatir.setVisibility(View.VISIBLE);
        oyuncularBaslik.setVisibility(View.VISIBLE);
        oyuncularHsv.setVisibility(View.VISIBLE);
        backdropView.setAlpha(0.18f);
        rootView.setVisibility(View.VISIBLE);
        yukleniyorView.setVisibility(View.GONE);
        baslikView.setText(model.getBaslik() != null ? model.getBaslik() : "");
        StringBuilder yilSure = new StringBuilder();
        if (model.getYil()  != null) yilSure.append(model.getYil());
        if (model.getSure() != null) { if (yilSure.length() > 0) yilSure.append(" · "); yilSure.append(model.getSure()); }
        if (model.getUlke() != null) { if (yilSure.length() > 0) yilSure.append(" · "); yilSure.append(model.getUlke()); }
        yilSureDilView.setText(yilSure.toString());
        if (model.getYonetmen() != null && !model.getYonetmen().isEmpty()) {
            yonetmenView.setText("🎬 " + model.getYonetmen());
            yonetmenView.setVisibility(View.VISIBLE);
        } else {
            yonetmenView.setVisibility(View.GONE);
        }
        String turler = model.getTurlerMetni();
        turlerView.setText(turler != null ? turler : "");
        konuView.setText(model.getKonu() != null ? model.getKonu() : "");
        imdbView.setText(model.getImdbPuan() != null ? "⭐ " + model.getImdbPuan() : "—");
        tmdbView.setText(model.getTmdbPuan() != null ? "● " + model.getTmdbPuan() : "—");
        yasView.setText(model.getYasSiniri() != null ? model.getYasSiniri() : "—");
        gorselYukle(posterView,   model.getPosterTamUrl(),   true);
        gorselYukle(backdropView, model.getBackdropTamUrl(), false);
        oyuncularGuncelle(model.getOyuncular());
    }
    private void oyuncularGuncelle(List<MetadataModel.Oyuncu> oyuncular) {
        oyuncularLayout.removeAllViews();
        if (oyuncular == null || oyuncular.isEmpty()) return;
        for (MetadataModel.Oyuncu oyuncu : oyuncular) {
            LinearLayout kart = new LinearLayout(activity);
            kart.setOrientation(LinearLayout.VERTICAL);
            kart.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams kartParams = new LinearLayout.LayoutParams(
                    dp(60), LinearLayout.LayoutParams.WRAP_CONTENT);
            kartParams.setMarginEnd(dp(8));
            ImageView foto = new ImageView(activity);
            foto.setScaleType(ImageView.ScaleType.CENTER_CROP);
            foto.setClipToOutline(true);
            android.graphics.drawable.GradientDrawable daire =
                    new android.graphics.drawable.GradientDrawable();
            daire.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            daire.setColor(0x33ffffff);
            foto.setBackground(daire);
            LinearLayout.LayoutParams fotoParams = new LinearLayout.LayoutParams(dp(44), dp(44));
            fotoParams.gravity = Gravity.CENTER_HORIZONTAL;
            TextView isimView = new TextView(activity);
            isimView.setText(oyuncu.getAd());
            isimView.setTextSize(9);
            isimView.setTextColor(RENK_SOLUK);
            isimView.setMaxLines(2);
            isimView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            isimView.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams isimParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            isimParams.topMargin = dp(4);
            kart.addView(foto,     fotoParams);
            kart.addView(isimView, isimParams);
            oyuncularLayout.addView(kart, kartParams);
            String fotoUrl = oyuncu.getFotoUrl();
            if (fotoUrl != null && !fotoUrl.isEmpty()) {
                gorselYukle(foto, fotoUrl, false);
            }
        }
    }
    private void gorselYukle(ImageView imageView, String url, boolean isPoster) {
        if (url == null || url.isEmpty() || url.endsWith("null")) {
            return;
        }
        imageExecutor.execute(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(4000);
                conn.connect();
                InputStream is     = conn.getInputStream();
                Bitmap      bitmap = BitmapFactory.decodeStream(is);
                is.close();
                conn.disconnect();
                if (bitmap != null) {
                    mainHandler.post(() -> {
                        if (isPoster) {
                            imageView.setAlpha(0f);
                            imageView.setImageBitmap(bitmap);
                            imageView.animate().alpha(1f).setDuration(300).start();
                        } else {
                            imageView.setImageBitmap(bitmap);
                        }
                    });
                }
            } catch (Exception e) {
                System.out.println("⚠️ [MetadataPanel] Görsel yüklenemedi: " + url);
            }
        });
    }
    public void solMarginAyarla(int marginPx) {
        if (rootView == null) return;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) rootView.getLayoutParams();
        if (params == null) return;
        
        params.rightMargin = marginPx;
        rootView.setLayoutParams(params);
    }
    private void yukleniyorGoster() {
        if (rootView == null) return;
        rootView.setVisibility(View.VISIBLE);
        yukleniyorView.setVisibility(View.VISIBLE);
        bosKapsayici.setVisibility(View.GONE);
    }
    private void bosGoster() {
        if (rootView == null || rootView.getVisibility() == View.GONE) return;
        rootView.setVisibility(View.VISIBLE);
        yukleniyorView.setVisibility(View.GONE);
        ustSatir.setVisibility(View.GONE);
        konuView.setVisibility(View.GONE);
        puanlarSatir.setVisibility(View.GONE);
        oyuncularBaslik.setVisibility(View.GONE);
        oyuncularHsv.setVisibility(View.GONE);
        oyuncularLayout.removeAllViews();
        backdropView.setImageBitmap(null);
        backdropView.setAlpha(0.06f);
        bosKapsayici.setAlpha(0f);
        bosKapsayici.setVisibility(View.VISIBLE);
        bosKapsayici.animate().alpha(1f).setDuration(400).start();
        bosIkon.setScaleX(1f);
        bosIkon.setScaleY(1f);
        bosIkon.animate()
                .scaleX(1.2f).scaleY(1.2f)
                .setDuration(350)
                .withEndAction(() ->
                        bosIkon.animate()
                                .scaleX(1f).scaleY(1f)
                                .setDuration(250)
                                .start())
                .start();
    }
    public void goster() {
        if (rootView != null) rootView.setVisibility(View.VISIBLE);
    }
    public void gizle() {
        System.out.println("🔴 [META] gizle() çağrıldı | rootView=" + (rootView != null ? "VAR" : "NULL") + " | visibility=" + (rootView != null ? rootView.getVisibility() : "?"));
        if (rootView != null) rootView.setVisibility(View.GONE);
        if (bekleyenIstek != null) {
            mainHandler.removeCallbacks(bekleyenIstek);
            bekleyenIstek = null;
        }
        System.out.println("🔴 [META] gizle() tamamlandı");
    }
    public boolean isVisible() {
        return rootView != null && rootView.getVisibility() == View.VISIBLE;
    }
    public View getRootView() { return rootView; }
    private TextView bilgiKutuOlustur(int renk) {
        TextView tv = new TextView(activity);
        tv.setTextSize(14);
        tv.setTextColor(renk);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        return tv;
    }
    private LinearLayout bilgiKutuKapsayiciOlustur(String etiket, TextView degerView, float weight) {
        LinearLayout ll = new LinearLayout(activity);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER);
        ll.setPadding(dp(8), dp(3), dp(8), dp(3));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, weight);
        ll.setLayoutParams(params);
        TextView etiketView = new TextView(activity);
        etiketView.setText(etiket);
        etiketView.setTextSize(9);
        etiketView.setTextColor(RENK_SOLUK);
        etiketView.setLetterSpacing(0.1f);
        etiketView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams etiketParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        etiketParams.bottomMargin = dp(1);
        ll.addView(etiketView, etiketParams);
        ll.addView(degerView);
        return ll;
    }
    private View ayiriciOlustur() {
        View v = new View(activity);
        v.setBackgroundColor(0x15ffffff);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(1),
                LinearLayout.LayoutParams.MATCH_PARENT);
        p.topMargin    = dp(6);
        p.bottomMargin = dp(6);
        v.setLayoutParams(p);
        return v;
    }
    private android.graphics.drawable.Drawable gradyanOlustur() {
        return new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00000000, 0xFF0f0f1a});
    }
    private int dp(int dp) {
        return Math.round(android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, dp,
                activity.getResources().getDisplayMetrics()));
    }
    public void temizle() {
        if (bekleyenIstek != null) {
            mainHandler.removeCallbacks(bekleyenIstek);
            bekleyenIstek = null;
        }
        imageExecutor.shutdown();
    }
}