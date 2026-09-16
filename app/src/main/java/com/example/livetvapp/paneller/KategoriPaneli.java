package com.example.livetvapp.paneller;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.livetvapp.R;
import com.example.livetvapp.Adapter.Kategoriadapter;
import com.example.livetvapp.database.FavorilerYoneticisi;
import com.example.livetvapp.database.KategoriCache;
import com.example.livetvapp.database.KategoriItem;
import java.util.ArrayList;
import java.util.List;
public class KategoriPaneli {
    private AppCompatActivity activity;
    private View panelView;
    private RecyclerView recyclerView;
    private Kategoriadapter adapter;
    private TextView bosText;
    private String  aktifIcerikTipi       = null;
    private int     kategoriOffset        = 0;
    private boolean kategoriYukleniyor    = false;
    private boolean tumKategorilerYuklendi = false;
    private KategoriSayfaYukleyici sayfaYukleyici;
    private int kategoriUstOffset = 0;
    private boolean ustKategoriYukleniyor = false;
    private boolean tumUstKategorilerYuklendi = false;
    public interface KategoriSayfaYukleyici {
        void sonrakiSayfayiYukle(String icerikTipi, int offset);
        void oncekiSayfayiYukle(String icerikTipi, int offset);
    }
    private boolean dinleyiciAktif = true;
    public interface KategoriSecimDinleyici {
        void onKategoriSecildi(String kategori);
    }
    private KategoriSecimDinleyici dinleyici;
    public KategoriPaneli(AppCompatActivity activity) {
        this.activity = activity;
    }
    public void setKategoriSecimDinleyici(KategoriSecimDinleyici dinleyici) {
        this.dinleyici = dinleyici;
    }
    public void setAktifIcerikTipi(String icerikTipi) {
        if (!icerikTipi.equals(this.aktifIcerikTipi)) {
            kategoriOffset         = 0;
            kategoriYukleniyor     = false;
            tumKategorilerYuklendi = false;
        }
        this.aktifIcerikTipi = icerikTipi;
    }
    public void olustur(List<KategoriItem> kategoriler) {
        System.out.println("📂 KategoriPaneli.olustur() çağrıldı");
        panelView    = LayoutInflater.from(activity).inflate(R.layout.panel_kategoriler, null);
        recyclerView = panelView.findViewById(R.id.kategoriRecyclerView);
        bosText      = panelView.findViewById(R.id.kategoriBosText);
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
        Kategoriadapter.OnKategoriTiklandi tiklandi = kategoriAdi -> {
            System.out.println("✅ Kategori tıklandı: " + kategoriAdi);
            if (dinleyiciAktif && dinleyici != null) dinleyici.onKategoriSecildi(kategoriAdi);
        };
        adapter = new Kategoriadapter(tiklandi);
        adapter.setOnKategoriFocusDegisti(kategoriAdi -> {
            System.out.println("🔍 Kategori focus: " + kategoriAdi);
            if (dinleyiciAktif && dinleyici != null) dinleyici.onKategoriSecildi(kategoriAdi);
        });
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                if (kategoriYukleniyor || tumKategorilerYuklendi) return;
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm == null) return;
                int gorunan    = lm.getChildCount();
                int toplam     = lm.getItemCount();
                int ilkGorunen = lm.findFirstVisibleItemPosition();
                
                if (dy > 0 && (gorunan + ilkGorunen) >= toplam - 5) {
                    kategoriYukleniyor = true;
                    if (sayfaYukleyici != null)
                        sayfaYukleyici.sonrakiSayfayiYukle(aktifIcerikTipi, kategoriOffset);
                }

            }
        });
        List<KategoriItem> liste = buildKategoriListesi(kategoriler);
        adapter.setKategoriler(liste);
        guncelleBosGorunum(liste);
        System.out.println("✅ Kategori listesi oluşturuldu: " + liste.size() + " kategori");
        activity.addContentView(
                panelView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
    }
    public void kategorileriGuncelle(List<KategoriItem> kategoriler) {
        kategoriYukleniyor     = false;
        tumKategorilerYuklendi = false;
        if (adapter == null) return;
        System.out.println("🔄 KategoriPaneli.kategorileriGuncelle() - " + kategoriler.size() + " kategori");
        List<KategoriItem> liste = buildKategoriListesi(kategoriler);
        adapter.setKategoriler(liste);
        kategoriOffset = liste.size() > 1 ? liste.size() - 1 : 0;
        adapter.setAktifPozisyon(-1);
        kategoriUstOffset          = 0;
        ustKategoriYukleniyor      = false;
        tumUstKategorilerYuklendi  = false;
        if (recyclerView != null) recyclerView.scrollToPosition(0);
        guncelleBosGorunum(liste);
        if (aktifIcerikTipi != null && !liste.isEmpty()) {
            KategoriCache.getInstance().put(aktifIcerikTipi, liste);
        }
    }
    private void guncelleBosGorunum(List<KategoriItem> liste) {
        if (liste == null || liste.isEmpty()) {
            if (recyclerView != null) recyclerView.setVisibility(View.GONE);
            if (bosText      != null) bosText.setVisibility(View.VISIBLE);
        } else {
            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
            if (bosText      != null) bosText.setVisibility(View.GONE);
        }
    }
    public void setSayfaYukleyici(KategoriSayfaYukleyici yukleyici) {
        this.sayfaYukleyici = yukleyici;
    }
    public void sayfaEkle(List<KategoriItem> yeniKategoriler) {
        if (adapter == null) return;
        if (yeniKategoriler == null || yeniKategoriler.isEmpty()) {
            tumKategorilerYuklendi = true;
            kategoriYukleniyor     = false;
            System.out.println("✅ KategoriPaneli: tüm kategoriler yüklendi");
            return;
        }
        List<KategoriItem> filtrelenmis = buildKategoriListesiEkSiz(yeniKategoriler);
        List<KategoriItem> tekilFiltrelenmis = new ArrayList<>();
        for (KategoriItem item : filtrelenmis) {
            boolean zatenVar = false;
            for (int i = 0; i < adapter.getItemCount(); i++) {
                KategoriItem mevcut = adapter.getKategoriItem(i);
                if (mevcut != null && mevcut.equals(item)) {
                    zatenVar = true;
                    break;
                }
            }
            if (!zatenVar) tekilFiltrelenmis.add(item);
        }
        adapter.addKategoriler(tekilFiltrelenmis);
        kategoriOffset    += yeniKategoriler.size();
        kategoriYukleniyor = false;
        System.out.println("📥 KategoriPaneli sayfaEkle: +" + tekilFiltrelenmis.size() + " | offset=" + kategoriOffset);
    }
    public void usteSayfaEkle(List<KategoriItem> yeniKategoriler) {
        if (adapter == null) return;
        if (yeniKategoriler == null || yeniKategoriler.isEmpty()) {
            tumUstKategorilerYuklendi = true;
            ustKategoriYukleniyor     = false;
            return;
        }
        List<KategoriItem> filtrelenmis = buildKategoriListesiEkSiz(yeniKategoriler);
        if (filtrelenmis.isEmpty()) {
            tumUstKategorilerYuklendi = true;
            ustKategoriYukleniyor     = false;
            return;
        }
        LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
        int eskiPoz    = lm != null ? lm.findFirstVisibleItemPosition() : 0;
        View eskiView  = lm != null ? lm.findViewByPosition(eskiPoz) : null;
        int eskiOffset = eskiView != null ? eskiView.getTop() : 0;
        adapter.addKategorilerBasa(filtrelenmis);
        kategoriUstOffset     -= filtrelenmis.size();
        ustKategoriYukleniyor  = false;
        if (lm != null)
            lm.scrollToPositionWithOffset(eskiPoz + filtrelenmis.size(), eskiOffset);
    }
    public void setAktifKategori(String kategoriAdi) {
        if (adapter == null || kategoriAdi == null) return;
        for (int i = 0; i < adapter.getItemCount(); i++) {
            if (kategoriAdi.equals(adapter.getKategori(i))) {
                adapter.setAktifPozisyon(i);
                return;
            }
        }
        adapter.setAktifPozisyon(-1);
    }
    private List<KategoriItem> buildKategoriListesi(List<KategoriItem> kategoriler) {
        List<KategoriItem> liste = new ArrayList<>();
        liste.add(new KategoriItem(FavorilerYoneticisi.FAVORI_KATEGORI_ADI, ""));
        if (kategoriler != null) {
            for (KategoriItem k : kategoriler) {
                if (k != null && !k.getKategoriAdi().isEmpty()
                        && !FavorilerYoneticisi.FAVORI_KATEGORI_ADI.equals(k.getKategoriAdi())) {
                    liste.add(k);
                }
            }
        }
        return liste;
    }
    private List<KategoriItem> buildKategoriListesiEkSiz(List<KategoriItem> kategoriler) {
        List<KategoriItem> liste = new ArrayList<>();
        if (kategoriler != null) {
            for (KategoriItem k : kategoriler) {
                if (k != null && !k.getKategoriAdi().isEmpty()
                        && !FavorilerYoneticisi.FAVORI_KATEGORI_ADI.equals(k.getKategoriAdi())) {
                    liste.add(k);
                }
            }
        }
        return liste;
    }
    public void setMarginStart(int marginDp) {
        if (recyclerView == null) return;
        int marginPx = dpToPx(marginDp);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) recyclerView.getLayoutParams();
        if (params != null) {
            params.leftMargin = marginPx;
            recyclerView.setLayoutParams(params);
        }
        if (bosText != null) {
            FrameLayout.LayoutParams bp = (FrameLayout.LayoutParams) bosText.getLayoutParams();
            if (bp != null) { bp.leftMargin = marginPx; bosText.setLayoutParams(bp); }
        }
        System.out.println("📐 Kategori listesi marginStart: " + marginDp + "dp (" + marginPx + "px)");
    }
    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                activity.getResources().getDisplayMetrics()));
    }
    public void kategoriAdınaFocusVer(String kategoriAdi) {
        if (adapter == null || recyclerView == null || kategoriAdi == null) {
            aktifKategoriyeFocusVer();
            return;
        }
        for (int i = 0; i < adapter.getItemCount(); i++) {
            if (kategoriAdi.equals(adapter.getKategori(i))) {
                final int hedefPoz = i;
                recyclerView.postDelayed(() -> {
                    recyclerView.scrollToPosition(hedefPoz);
                    recyclerView.postDelayed(() -> {
                        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(hedefPoz);
                        if (holder != null && holder.itemView != null) {
                            holder.itemView.requestFocus();
                        } else {
                            recyclerView.requestFocus();
                        }
                    }, 150);
                }, 100);
                return;
            }
        }
        aktifKategoriyeFocusVer();
    }
    public void aktifKategoriyeFocusVer() {
        if (adapter == null || recyclerView == null) return;
        int aktifPoz = adapter.getAktifPozisyon();
        int hedefPoz = aktifPoz >= 0 ? aktifPoz : 0;
        recyclerView.postDelayed(() -> {
            recyclerView.scrollToPosition(hedefPoz);
            recyclerView.postDelayed(() -> {
                RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(hedefPoz);
                if (holder != null && holder.itemView != null) {
                    holder.itemView.requestFocus();
                } else {
                    recyclerView.requestFocus();
                }
            }, 150);
        }, 100);
    }
    public void ilkKategoriyeFocusVer() {
        if (recyclerView == null || adapter == null || adapter.getItemCount() == 0) return;
        recyclerView.postDelayed(() -> {
            int hedef = adapter.getItemCount() > 1 ? 1 : 0;
            recyclerView.scrollToPosition(hedef);
            recyclerView.postDelayed(() -> {
                RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(hedef);
                if (holder != null && holder.itemView != null) {
                    holder.itemView.requestFocus();
                } else {
                    recyclerView.requestFocus();
                }
            }, 150);
        }, 100);
    }
    public void goster() {
        dinleyiciAktif = true;
        if (panelView != null) panelView.setVisibility(View.VISIBLE);
    }
    public void gizle() {
        dinleyiciAktif = false;
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
    public Kategoriadapter getAdapter()   { return adapter; }
}