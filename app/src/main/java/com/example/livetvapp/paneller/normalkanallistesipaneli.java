package com.example.livetvapp.paneller;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.livetvapp.Channel;
import com.example.livetvapp.R;
import com.example.livetvapp.Adapter.KanalAdapter;
import java.util.ArrayList;
import java.util.List;
public class normalkanallistesipaneli {
    private static final boolean BASLIK_VAR  = true;
    private static final int     PAGE_SIZE   = 100;
    private static final int     LOAD_AHEAD  = 15;
    private AppCompatActivity activity;
    private View panelView;
    private RecyclerView recyclerView;
    private KanalAdapter adapter;
    private TextView bosText;
    private List<Channel> tumKanallar    = new ArrayList<>();
    private int           yuklenenSayisi = 0;
    private boolean       isLoadingMore  = false;
    private int    ustOffset             = 0;
    private boolean ustYukleniyor        = false;
    private boolean tumUstKanallarYuklendi = false;
    public interface KanalSecimDinleyici {
        void onKanalSecildi(Channel kanal);
    }
    private KanalSecimDinleyici kanalDinleyici;
    public interface DbSayfaCallback {
        void sayfaIste(int offset, DbSayfaSonucu sonuc);
    }
    public interface DbSayfaSonucu {
        void sayfaGeldi(List<Channel> kanallar, boolean bitti);
    }
    private DbSayfaCallback dbSayfaCallback = null;
    private int             dbOffset        = 0;
    private boolean         dbBitti         = false;
    public normalkanallistesipaneli(AppCompatActivity activity) {
        this.activity = activity;
    }
    public void setKanalSecimDinleyici(KanalSecimDinleyici dinleyici) {
        this.kanalDinleyici = dinleyici;
    }
    public void setDbSayfaCallback(DbSayfaCallback callback) {
        this.dbSayfaCallback = callback;
    }
    public void olustur(List<Channel> kanallar) {
        System.out.println("📺 normalkanallistesipaneli.olustur() çağrıldı");
        panelView    = LayoutInflater.from(activity).inflate(R.layout.panel_kanallar, null);
        recyclerView = panelView.findViewById(R.id.kanalRecyclerView);
        bosText      = panelView.findViewById(R.id.kanalBosText);
        TextView baslik = panelView.findViewById(R.id.panelBaslik);
        if (baslik != null) {
            baslik.setVisibility(BASLIK_VAR ? View.VISIBLE : View.GONE);
        }
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
        adapter = new KanalAdapter((kanal, pozisyon) -> {
            System.out.println("✅ Kanal seçildi: " + kanal.getName());
            if (kanalDinleyici != null) kanalDinleyici.onKanalSecildi(kanal);
        });
        adapter.setOnLoadMoreListener(page -> {
            System.out.println("📥 Adapter load more isteği: sayfa=" + page);
        });
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm == null) return;
                int gorunan    = lm.getChildCount();
                int toplam     = lm.getItemCount();
                int ilkGorunen = lm.findFirstVisibleItemPosition();
                
                if (dy > 0 && !isLoadingMore && (gorunan + ilkGorunen) >= toplam - LOAD_AHEAD) {
                    daha_fazla_yukle();
                }
                
                if (dy < 0 && !ustYukleniyor && !tumUstKanallarYuklendi && ilkGorunen <= 5) {
                    yukari_yukle();
                }
            }
        });
        if (kanallar != null && !kanallar.isEmpty()) {
            kanallarıGuncelle(kanallar);
        }
        activity.addContentView(
                panelView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
    }
    public void daha_fazla_yukle() {
        if (isLoadingMore) return;
        if (dbSayfaCallback != null) {
            if (dbBitti) return;
            isLoadingMore = true;
            dbSayfaCallback.sayfaIste(dbOffset, (kanallar, bitti) -> {
                if (kanallar == null || kanallar.isEmpty()) {
                    dbBitti       = true;
                    isLoadingMore = false;
                    return;
                }
                if (adapter != null) {
                    if (dbOffset == 0) {
                        adapter.setKanallar(kanallar);
                    } else {
                        adapter.addKanallar(kanallar);
                    }
                }
                if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
                if (bosText      != null) bosText.setVisibility(View.GONE);
                dbOffset     += kanallar.size();
                isLoadingMore = false;
                if (bitti) dbBitti = true;
            });
            return;
        }
        if (yuklenenSayisi >= tumKanallar.size()) return;
        isLoadingMore = true;
        int bitis = Math.min(yuklenenSayisi + PAGE_SIZE, tumKanallar.size());
        adapter.addKanallar(new ArrayList<>(tumKanallar.subList(yuklenenSayisi, bitis)));
        yuklenenSayisi = bitis;
        isLoadingMore  = false;
    }
    public void yukari_yukle() {
        if (ustYukleniyor || tumUstKanallarYuklendi) return;
        if (dbSayfaCallback != null) {
            if (ustOffset <= 0) {
                tumUstKanallarYuklendi = true;
                return;
            }
            ustYukleniyor = true;
            int yeniOffset = Math.max(0, ustOffset - PAGE_SIZE);
            int alinacak   = ustOffset - yeniOffset;
            dbSayfaCallback.sayfaIste(yeniOffset, (kanallar, bitti) -> {
                if (kanallar == null || kanallar.isEmpty()) {
                    tumUstKanallarYuklendi = true;
                    ustYukleniyor          = false;
                    return;
                }
                List<Channel> eklenecek = kanallar.size() > alinacak
                        ? kanallar.subList(0, alinacak) : kanallar;
                if (adapter != null && recyclerView != null) {
                    LinearLayoutManager lm =
                            (LinearLayoutManager) recyclerView.getLayoutManager();
                    int eskiPoz   = lm != null ? lm.findFirstVisibleItemPosition() : 0;
                    View eskiView = lm != null ? lm.findViewByPosition(eskiPoz) : null;
                    int eskiOffset = eskiView != null ? eskiView.getTop() : 0;
                    adapter.addKanallarBasa(eklenecek);
                    if (lm != null)
                        lm.scrollToPositionWithOffset(eskiPoz + eklenecek.size(), eskiOffset);
                }
                ustOffset     = yeniOffset;
                ustYukleniyor = false;
                if (yeniOffset == 0) tumUstKanallarYuklendi = true;
            });
            return;
        }
        
        if (ustOffset <= 0) {
            tumUstKanallarYuklendi = true;
            return;
        }
        ustYukleniyor = true;
        int yeniOffset = Math.max(0, ustOffset - PAGE_SIZE);
        List<Channel> eklenecek = new ArrayList<>(
                tumKanallar.subList(yeniOffset, ustOffset));
        LinearLayoutManager lm =
                (LinearLayoutManager) recyclerView.getLayoutManager();
        int eskiPoz   = lm != null ? lm.findFirstVisibleItemPosition() : 0;
        View eskiView = lm != null ? lm.findViewByPosition(eskiPoz) : null;
        int eskiOffset = eskiView != null ? eskiView.getTop() : 0;
        adapter.addKanallarBasa(eklenecek);
        if (lm != null)
            lm.scrollToPositionWithOffset(eskiPoz + eklenecek.size(), eskiOffset);
        ustOffset     = yeniOffset;
        ustYukleniyor = false;
        if (yeniOffset == 0) tumUstKanallarYuklendi = true;
    }
    public void dbSifirla() {
        dbOffset      = 0;
        dbBitti       = false;
        isLoadingMore = false;
        tumKanallar   = new ArrayList<>();
        yuklenenSayisi = 0;
        if (recyclerView != null) recyclerView.scrollToPosition(0);
        if (adapter      != null) adapter.setKanallar(new ArrayList<>());
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        if (bosText      != null) bosText.setVisibility(View.VISIBLE);
        daha_fazla_yukle(); 
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
        TextView baslik = panelView != null ? panelView.findViewById(R.id.panelBaslik) : null;
        if (baslik != null) {
            FrameLayout.LayoutParams bp = (FrameLayout.LayoutParams) baslik.getLayoutParams();
            if (bp != null) { bp.leftMargin = marginPx; baslik.setLayoutParams(bp); }
        }
        System.out.println("📐 Kanal listesi marginStart: " + marginDp + "dp (" + marginPx + "px)");
    }
    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                activity.getResources().getDisplayMetrics()));
    }
    public void baslikGuncelle(String icerikTipi) {
        if (panelView == null) return;
        TextView baslik = panelView.findViewById(R.id.panelBaslik);
        if (baslik == null) return;
        switch (icerikTipi) {
            case "LIVE":   baslik.setText("KANALLAR"); break;
            case "MOVIE":  baslik.setText("FİLMLER");  break;
            case "SERIES": baslik.setText("DİZİLER");  break;
            default:       baslik.setText("KANALLAR"); break;
        }
    }
    public void kanallarıGuncelle(List<Channel> kanallar) {
        if (dbSayfaCallback != null) {
            dbSifirla();
            return;
        }
        System.out.println("=== kanallarıGuncelle() çağrıldı ===");
        if (kanallar == null) kanallar = new ArrayList<>();
        tumKanallar    = new ArrayList<>(kanallar);
        yuklenenSayisi = 0;
        isLoadingMore  = false;
        if (recyclerView != null) recyclerView.scrollToPosition(0);
        if (tumKanallar.isEmpty()) {
            if (recyclerView != null) recyclerView.setVisibility(View.GONE);
            if (bosText      != null) bosText.setVisibility(View.VISIBLE);
            if (adapter      != null) adapter.setKanallar(new ArrayList<>());
        } else {
            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
            if (bosText      != null) bosText.setVisibility(View.GONE);
            if (adapter != null) {
                int ilkBitis = PAGE_SIZE;
                String aktifUrl = adapter.getAktifKanalUrl();
                if (aktifUrl != null) {
                    for (int i = 0; i < tumKanallar.size(); i++) {
                        if (aktifUrl.equals(tumKanallar.get(i).getUrl())) {
                            ilkBitis = Math.max(PAGE_SIZE, i + 1);
                            System.out.println("📦 kanallarıGuncelle: aktif kanal poz=" + i + " → ilk yükleme=" + ilkBitis);
                            break;
                        }
                    }
                }
                int bitis = Math.min(ilkBitis, tumKanallar.size());
                adapter.setKanallar(new ArrayList<>(tumKanallar.subList(0, bitis)));
                yuklenenSayisi = bitis;
            }
        }
    }
    public void ilkKanalıAktifYap(Channel ilkKanal) {
        System.out.println("🎯 ilkKanalıAktifYap() - İlk kanal: " + ilkKanal.getName());
        if (adapter != null && ilkKanal != null) {
            recyclerView.postDelayed(() -> {
                adapter.setAktifKanalUrl(ilkKanal.getUrl());
                System.out.println("✅ İlk kanal aktif yapıldı: " + ilkKanal.getName());
            }, 200);
        }
    }
    public void aktifKanalaFocusVer() {
        System.out.println("🔍 aktifKanalaFocusVer() çağrıldı");
        if (adapter == null || recyclerView == null) return;
        int aktifPozisyon = adapter.getAktifPozisyon();
        if (aktifPozisyon == -1) {
            ilkGoruneneFocusVer();
            return;
        }
        recyclerView.scrollToPosition(aktifPozisyon);
        recyclerView.postDelayed(() -> {
            RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(aktifPozisyon);
            if (vh != null && vh.itemView != null) {
                vh.itemView.requestFocus();
            } else {
                recyclerView.postDelayed(() -> {
                    RecyclerView.ViewHolder vh2 = recyclerView.findViewHolderForAdapterPosition(aktifPozisyon);
                    if (vh2 != null && vh2.itemView != null) vh2.itemView.requestFocus();
                    else recyclerView.requestFocus();
                }, 150);
            }
        }, 100);
    }
    public Channel getFocusdakiKanal() {
        if (recyclerView == null || adapter == null) return null;
        View focused = recyclerView.getFocusedChild();
        if (focused != null) {
            int pos = recyclerView.getChildAdapterPosition(focused);
            if (pos != RecyclerView.NO_POSITION) return adapter.getKanal(pos);
        }
        LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (lm != null) {
            int ilkGorunen = lm.findFirstVisibleItemPosition();
            if (ilkGorunen != RecyclerView.NO_POSITION && ilkGorunen >= 0)
                return adapter.getKanal(ilkGorunen);
        }
        return null;
    }
    public void ilkGoruneneFocusVer() {
        System.out.println("🔍 ilkGoruneneFocusVer() çağrıldı");
        if (recyclerView != null && adapter != null && adapter.getItemCount() > 0) {
            recyclerView.postDelayed(() -> {
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                int hedefPozisyon = 0;
                if (lm != null) {
                    int ilkGorunen = lm.findFirstVisibleItemPosition();
                    if (ilkGorunen != RecyclerView.NO_POSITION && ilkGorunen >= 0)
                        hedefPozisyon = ilkGorunen;
                }
                RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(hedefPozisyon);
                if (vh != null && vh.itemView != null) vh.itemView.requestFocus();
            }, 100);
        }
    }
    public boolean isVisible() {
        return panelView != null && panelView.getVisibility() == View.VISIBLE;
    }
    public void gizle() {
        if (panelView != null) panelView.setVisibility(View.GONE);
    }
    public void goster() {
        if (panelView != null) panelView.setVisibility(View.VISIBLE);
    }
    public View getPanelView()            { return panelView; }
    public RecyclerView getRecyclerView() { return recyclerView; }
    public KanalAdapter getAdapter()      { return adapter; }
    public boolean isLoadingMore() { return isLoadingMore; }
}