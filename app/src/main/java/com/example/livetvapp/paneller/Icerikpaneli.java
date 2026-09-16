package com.example.livetvapp.paneller;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.livetvapp.R;
import com.example.livetvapp.Adapter.Kategoriadapter;
import com.example.livetvapp.database.KategoriItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Icerikpaneli {
    public static final String TIP_CANLI_TV = "LIVE";
    public static final String TIP_FILM     = "MOVIE";
    public static final String TIP_DIZI     = "SERIES";

    
    private static final List<String> ETIKETLER = Arrays.asList(
            "📡 CANLI TV",
            "🎬 FİLMLER",
            "📺 DİZİLER"
    );

    private static final String[] TIPLER = {
            TIP_CANLI_TV, TIP_FILM, TIP_DIZI
    };

    private AppCompatActivity activity;
    private View panelView;
    private RecyclerView recyclerView;
    private Kategoriadapter adapter;
    private boolean dinleyiciAktif = true;
    private boolean setAktifTipDevam = false;

    public interface IcerikSecimDinleyici {
        void onIcerikSecildi(String icerikTipi);
    }

    public interface AyarlarButonuDinleyici {
        void onAyarlarAcildi();
        void onAyarlarFocus(boolean hasFocus);
    }

    private AyarlarButonuDinleyici ayarlarDinleyici;

    public void setAyarlarButonuDinleyici(AyarlarButonuDinleyici dinleyici) {
        this.ayarlarDinleyici = dinleyici;
    }

    private IcerikSecimDinleyici dinleyici;

    public Icerikpaneli(AppCompatActivity activity) {
        this.activity = activity;
    }

    public void setIcerikSecimDinleyici(IcerikSecimDinleyici dinleyici) {
        this.dinleyici = dinleyici;
    }

    public void olustur() {
        panelView = LayoutInflater.from(activity).inflate(R.layout.panel_kategoriler, null);
        recyclerView = panelView.findViewById(R.id.kategoriRecyclerView);
        panelView.setVisibility(View.GONE);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));

        Kategoriadapter.OnKategoriTiklandi tiklandi = kategoriAdi -> {
            System.out.println("🟠 [ICERIK] TIKLAMA: " + kategoriAdi
                    + " | dinleyiciAktif=" + dinleyiciAktif
                    + " | panelVisible=" + isVisible());
            if (dinleyiciAktif && !setAktifTipDevam && dinleyici != null) {
                dinleyici.onIcerikSecildi(tipBul(kategoriAdi));
            }
        };

        adapter = new Kategoriadapter(tiklandi);
        adapter.setOnKategoriFocusDegisti(kategoriAdi -> {
            System.out.println("🟠 [ICERIK] FOCUS DEĞİŞTİ: " + kategoriAdi
                    + " | dinleyiciAktif=" + dinleyiciAktif
                    + " | setAktifTipDevam=" + setAktifTipDevam
                    + " | panelVisible=" + isVisible());
            if (dinleyiciAktif && !setAktifTipDevam && dinleyici != null) {
                dinleyici.onIcerikSecildi(tipBul(kategoriAdi));
            }
        });

        
        List<KategoriItem> etiketItems = new ArrayList<>();
        for (String etiket : ETIKETLER) {
            etiketItems.add(new KategoriItem(etiket, ""));
        }
        adapter.setKategoriler(etiketItems);
        adapter.setAktifPozisyon(0);
        recyclerView.setAdapter(adapter);



        activity.addContentView(
                panelView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        );
    }

    public void aktifPozisyonaFocusVer() {
        if (adapter == null || recyclerView == null) return;
        int poz = Math.max(0, adapter.getAktifPozisyon());
        recyclerView.postDelayed(() -> {
            recyclerView.scrollToPosition(poz);
            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(poz);
            if (holder != null && holder.itemView != null) {
                holder.itemView.requestFocus();
            }
        }, 100);
    }

    public void setAktifTip(String tip) {
        System.out.println("🟠 [ICERIK] setAktifTip: " + tip
                + " | panelVisible=" + isVisible()
                + " | dinleyiciAktif öncesi=" + dinleyiciAktif);
        if (adapter == null) return;
        boolean eskiDurum = dinleyiciAktif;
        dinleyiciAktif   = false;
        setAktifTipDevam = true;
        for (int i = 0; i < TIPLER.length; i++) {
            if (TIPLER[i].equals(tip)) {
                adapter.setAktifPozisyon(i);
                final boolean geriDurum = eskiDurum;
                recyclerView.post(() -> {
                    dinleyiciAktif   = geriDurum;
                    setAktifTipDevam = false;
                    System.out.println("🟠 [ICERIK] setAktifTip post tamamlandı: poz=" + getAdapterPoz(tip)
                            + " | dinleyiciAktif geri=" + geriDurum);
                });
                return;
            }
        }
        adapter.setAktifPozisyon(0);
        final boolean geriDurum = eskiDurum;
        recyclerView.post(() -> {
            dinleyiciAktif   = geriDurum;
            setAktifTipDevam = false;
            System.out.println("🟠 [ICERIK] setAktifTip: tip bulunamadı, poz=0 | dinleyiciAktif geri=" + geriDurum);
        });
    }

    private int getAdapterPoz(String tip) {
        for (int i = 0; i < TIPLER.length; i++) {
            if (TIPLER[i].equals(tip)) return i;
        }
        return 0;
    }

    public void goster() {
        System.out.println("🟠 [ICERIK] goster() çağrıldı → dinleyiciAktif=true");
        dinleyiciAktif = true;
        if (panelView != null) panelView.setVisibility(View.VISIBLE);
    }

    public void gizle() {
        System.out.println("🟠 [ICERIK] gizle() çağrıldı → dinleyiciAktif=false");
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
    public Kategoriadapter getAdapter()   { return adapter; }

    private String tipBul(String etiket) {
        for (int i = 0; i < ETIKETLER.size(); i++) {
            if (ETIKETLER.get(i).equals(etiket)) return TIPLER[i];
        }
        return TIP_CANLI_TV;
    }
}