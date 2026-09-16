package com.example.livetvapp.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.livetvapp.R;
import com.example.livetvapp.database.FavorilerYoneticisi;
import com.example.livetvapp.paneller.Icerikpaneli;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
public class DiziAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TIP_DIZI  = 0;
    private static final int TIP_SEZON = 1;
    private static final int TIP_BOLUM = 2;

    
    private static final int RENK_NORMAL      = 0xFF1A3A5A;   
    private static final int RENK_FOCUS       = 0x88FF0000;   
    private static final int RENK_DIZI_BASLIK = 0xFF1A3A5A;   
    private static final int RENK_SEZON       = 0xFF1A3A5A;   

    private static class ListeOgesi {
        int   tip;
        Dizi  dizi;
        Sezon sezon;
        Bolum bolum;
        int   diziIndex;
        static ListeOgesi dizi(Dizi d, int idx) {
            ListeOgesi o = new ListeOgesi(); o.tip = TIP_DIZI; o.dizi = d; o.diziIndex = idx; return o;
        }
        static ListeOgesi sezon(Sezon s, Dizi d, int idx) {
            ListeOgesi o = new ListeOgesi(); o.tip = TIP_SEZON; o.sezon = s; o.dizi = d; o.diziIndex = idx; return o;
        }
        static ListeOgesi bolum(Bolum b, Dizi d, Sezon s) {
            ListeOgesi o = new ListeOgesi(); o.tip = TIP_BOLUM; o.bolum = b; o.dizi = d; o.sezon = s; return o;
        }
    }
    public interface DiziSecimDinleyici  { void onDiziSecildi(Dizi dizi, int pozisyon); }
    public interface BolumSecimDinleyici { void onBolumSecildi(Dizi dizi, Sezon sezon, Bolum bolum); }
    public interface OnFocusDegistiListener {
        void onFocusDegisti(String diziAdi);
    }
    public interface OnFavoriDegistiListener {
        void onFavoriDegisti(boolean eklendi);
    }
    public interface OnDiziDetayIstekListener {
        void onDetayIstendi(Dizi dizi, int pozisyon);
    }

    private OnDiziDetayIstekListener detayIstekListener;

    public void setOnDiziDetayIstekListener(OnDiziDetayIstekListener listener) {
        this.detayIstekListener = listener;
    }
    private OnFavoriDegistiListener favoriDegistiListener;
    public void setOnFavoriDegistiListener(OnFavoriDegistiListener listener) {
        this.favoriDegistiListener = listener;
    }
    private OnFocusDegistiListener focusDegistiListener;
    public void setOnFocusDegistiListener(OnFocusDegistiListener listener) {
        this.focusDegistiListener = listener;
    }
    private RecyclerView recyclerViewRef;
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerViewRef = recyclerView;
    }
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (this.recyclerViewRef == recyclerView) this.recyclerViewRef = null;
    }
    
    private void focusiKorumayaAl(int gosterimarPozisyon) {
        if (recyclerViewRef == null) return;
        View odaklanan = recyclerViewRef.getFocusedChild();
        boolean focusKayboldu = (odaklanan == null);
        if (!focusKayboldu) return; 
        recyclerViewRef.post(() -> {
            RecyclerView.ViewHolder holder =
                    recyclerViewRef.findViewHolderForAdapterPosition(gosterimarPozisyon);
            if (holder != null && holder.itemView != null) {
                holder.itemView.requestFocus();
            } else {
                recyclerViewRef.requestFocus();
            }
        });
    }
    private List<Dizi>       tumDiziler  = new ArrayList<>();
    private List<ListeOgesi> gosterilen  = new ArrayList<>();
    private Set<Integer> acikDiziler  = new HashSet<>();
    private Set<String>  acikSezonlar = new HashSet<>();
    private DiziSecimDinleyici  diziDinleyici;
    private BolumSecimDinleyici bolumDinleyici;

    private FavorilerYoneticisi favorilerYoneticisi;
    private boolean panelAcik = false;
    private String aktifDiziAdi = null;
    public void setFavorilerYoneticisi(FavorilerYoneticisi fy) {
        this.favorilerYoneticisi = fy;
    }
    public void setPanelAcik(boolean acik) {
        this.panelAcik = acik;
    }
    public void ilkFocusDiziAdiniGonder() {
        if (!gosterilen.isEmpty() && focusDegistiListener != null) {
            ListeOgesi ilk = gosterilen.get(0);
            if (ilk.tip == TIP_DIZI && ilk.dizi != null) {
                focusDegistiListener.onFocusDegisti(ilk.dizi.getAd());
            }
        }
    }
    public DiziAdapter(DiziSecimDinleyici diziDinleyici, BolumSecimDinleyici bolumDinleyici) {
        this.diziDinleyici  = diziDinleyici;
        this.bolumDinleyici = bolumDinleyici;
    }
    public void setDiziler(List<Dizi> diziler) {
        this.tumDiziler = diziler != null ? diziler : new ArrayList<>();
        acikDiziler.clear();
        acikSezonlar.clear();
        int eskiBoyut = gosterilen.size();
        gosterilen.clear();
        if (eskiBoyut > 0) notifyItemRangeRemoved(0, eskiBoyut);
        ekleGoruntulenecek(0, tumDiziler.size());
    }
    public void addDiziler(List<Dizi> yeniDiziler) {
        if (yeniDiziler == null || yeniDiziler.isEmpty()) return;
        int baslangicIndex = tumDiziler.size();
        tumDiziler.addAll(yeniDiziler);
        ekleGoruntulenecek(baslangicIndex, tumDiziler.size());
    }
    public void sezonlarYuklendi(Dizi dizi, int pozisyon) {
        if (pozisyon < 0 || pozisyon >= gosterilen.size()) return;
        diziToggle(gosterilen.get(pozisyon).diziIndex, pozisyon);
    }
    private void ekleGoruntulenecek(int baslangic, int bitis) {
        int eklenenBaslangic = gosterilen.size();
        for (int i = baslangic; i < bitis; i++) {
            Dizi dizi = tumDiziler.get(i);
            gosterilen.add(ListeOgesi.dizi(dizi, i));
            if (acikDiziler.contains(i)) {
                for (Sezon sezon : dizi.getSezonlar()) {
                    gosterilen.add(ListeOgesi.sezon(sezon, dizi, i));
                    String sezonKey = i + "_" + sezon.getSezonNo();
                    if (acikSezonlar.contains(sezonKey)) {
                        for (Bolum bolum : sezon.getBolumler()) {
                            gosterilen.add(ListeOgesi.bolum(bolum, dizi, sezon));
                        }
                    }
                }
            }
        }
        int eklenenAdet = gosterilen.size() - eklenenBaslangic;
        if (eklenenAdet > 0) notifyItemRangeInserted(eklenenBaslangic, eklenenAdet);
    }
    private void diziToggle(int diziIndex, int gosterimarPozisyon) {
        Dizi dizi = tumDiziler.get(diziIndex);
        if (acikDiziler.contains(diziIndex)) {
            int kaldirilacak = altOgeSayisi(diziIndex);
            acikDiziler.remove(diziIndex);
            for (Sezon s : dizi.getSezonlar()) {
                acikSezonlar.remove(diziIndex + "_" + s.getSezonNo());
            }
            gosterilen.subList(gosterimarPozisyon + 1, gosterimarPozisyon + 1 + kaldirilacak).clear();
            notifyItemChanged(gosterimarPozisyon);
            notifyItemRangeRemoved(gosterimarPozisyon + 1, kaldirilacak);
            focusiKorumayaAl(gosterimarPozisyon);
        } else {
            acikDiziler.add(diziIndex);
            int eklenenBaslangic = gosterimarPozisyon + 1;
            List<ListeOgesi> yeniOgeler = new ArrayList<>();
            for (Sezon sezon : dizi.getSezonlar()) {
                yeniOgeler.add(ListeOgesi.sezon(sezon, dizi, diziIndex));
            }
            gosterilen.addAll(eklenenBaslangic, yeniOgeler);
            notifyItemChanged(gosterimarPozisyon);
            notifyItemRangeInserted(eklenenBaslangic, yeniOgeler.size());
        }
    }
    private void sezonToggle(int diziIndex, Sezon sezon, int gosterimarPozisyon) {
        Dizi dizi = tumDiziler.get(diziIndex);
        String sezonKey = diziIndex + "_" + sezon.getSezonNo();
        if (acikSezonlar.contains(sezonKey)) {
            int bolumSayisi = sezon.getBolumler().size();
            acikSezonlar.remove(sezonKey);
            gosterilen.subList(gosterimarPozisyon + 1, gosterimarPozisyon + 1 + bolumSayisi).clear();
            notifyItemChanged(gosterimarPozisyon);
            notifyItemRangeRemoved(gosterimarPozisyon + 1, bolumSayisi);
            focusiKorumayaAl(gosterimarPozisyon);
        } else {
            acikSezonlar.add(sezonKey);
            int eklenenBaslangic = gosterimarPozisyon + 1;
            List<ListeOgesi> yeniOgeler = new ArrayList<>();
            for (Bolum bolum : sezon.getBolumler()) {
                yeniOgeler.add(ListeOgesi.bolum(bolum, dizi, sezon));
            }
            gosterilen.addAll(eklenenBaslangic, yeniOgeler);
            notifyItemChanged(gosterimarPozisyon);
            notifyItemRangeInserted(eklenenBaslangic, yeniOgeler.size());
        }
    }
    private int altOgeSayisi(int diziIndex) {
        Dizi dizi = tumDiziler.get(diziIndex);
        int sayi = 0;
        for (Sezon sezon : dizi.getSezonlar()) {
            sayi++;
            String sezonKey = diziIndex + "_" + sezon.getSezonNo();
            if (acikSezonlar.contains(sezonKey)) {
                sayi += sezon.getBolumler().size();
            }
        }
        return sayi;
    }
    private String gorunumAdi(Dizi dizi) {
        if (favorilerYoneticisi == null) return dizi.getAd();
        if (favorilerYoneticisi.isFavori(Icerikpaneli.TIP_DIZI, dizi.getAd())) {
            return "⭐ " + dizi.getAd();
        }
        return dizi.getAd();
    }

    @Override public int getItemViewType(int position) { return gosterilen.get(position).tip; }
    @Override public int getItemCount()                { return gosterilen.size(); }
    public int diziAdindanPozisyonBul(String diziAdi) {
        if (diziAdi == null) return 0;
        for (int i = 0; i < gosterilen.size(); i++) {
            ListeOgesi oge = gosterilen.get(i);
            if (oge.tip == TIP_DIZI && oge.dizi != null
                    && diziAdi.equals(oge.dizi.getAd())) {
                return i;
            }
        }
        return 0;
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TIP_DIZI) {
            return new DiziVH(inf.inflate(R.layout.dizi_item, parent, false));
        } else if (viewType == TIP_SEZON) {
            return new SezonVH(inf.inflate(R.layout.sezon_item, parent, false));
        } else {
            return new BolumVH(inf.inflate(R.layout.bolum_item, parent, false));
        }
    }
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ListeOgesi oge = gosterilen.get(position);
        if      (holder instanceof DiziVH)  ((DiziVH)  holder).bind(oge, position);
        else if (holder instanceof SezonVH) ((SezonVH) holder).bind(oge, position);
        else if (holder instanceof BolumVH) ((BolumVH) holder).bind(oge);
    }
    class DiziVH extends RecyclerView.ViewHolder {
        Button btn;
        DiziVH(View v) { super(v); btn = v.findViewById(R.id.btnDizi); }
        void bind(ListeOgesi oge, int position) {
            boolean acik = acikDiziler.contains(oge.diziIndex);
            String sezonBilgisi = "";
            if (!xtreamDiziMi(oge.dizi)) {
                sezonBilgisi = "  (" + oge.dizi.getSezonlar().size() + " Sezon)";
            } else if (acik) {
                sezonBilgisi = "  (" + oge.dizi.getSezonlar().size() + " Sezon)";
            }
            btn.setText((acik ? "▼ " : "▶ ") + gorunumAdi(oge.dizi) + sezonBilgisi);
            btn.setBackgroundColor(RENK_DIZI_BASLIK);
            btn.setOnFocusChangeListener((v, hasFocus) -> {
                btn.setBackgroundColor(hasFocus ? RENK_FOCUS : RENK_DIZI_BASLIK);
                if (hasFocus && focusDegistiListener != null) {
                    focusDegistiListener.onFocusDegisti(oge.dizi.getAd());
                }
            });
            btn.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                aktifDiziAdi = oge.dizi.getAd();
                System.out.println("🔍 [DİZİ-TIK] dizi=" + oge.dizi.getAd()
                        + " | sezonSayisi=" + oge.dizi.getSezonlar().size()
                        + " | detayListener=" + (detayIstekListener != null ? "VAR" : "YOK"));
                if (detayIstekListener != null && xtreamDiziMi(oge.dizi)) {
                    System.out.println("🌐 [DİZİ-TIK] get_series_info isteği tetikleniyor");
                    detayIstekListener.onDetayIstendi(oge.dizi, pos);
                } else {
                    System.out.println("⚠️ [DİZİ-TIK] detay isteği atlanıyor | sezonDolu="
                            + !oge.dizi.getSezonlar().isEmpty()
                            + " | listenerYok=" + (detayIstekListener == null));
                    diziToggle(oge.diziIndex, pos);
                    if (diziDinleyici != null) diziDinleyici.onDiziSecildi(oge.dizi, pos);
                }
            });
        }
    }
    class SezonVH extends RecyclerView.ViewHolder {
        Button btn;
        SezonVH(View v) { super(v); btn = v.findViewById(R.id.btnSezon); }
        void bind(ListeOgesi oge, int position) {
            String sezonKey = oge.diziIndex + "_" + oge.sezon.getSezonNo();
            boolean acik    = acikSezonlar.contains(sezonKey);
            btn.setText("  " + (acik ? "▼ " : "▶ ") + oge.sezon.getAd()
                    + "  (" + oge.sezon.getBolumler().size() + " Bölüm)");
            btn.setBackgroundColor(RENK_SEZON);
            btn.setOnFocusChangeListener((v, hasFocus) ->
                    btn.setBackgroundColor(hasFocus ? RENK_FOCUS : RENK_SEZON));
            btn.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                sezonToggle(oge.diziIndex, oge.sezon, pos);
            });
        }
    }
    class BolumVH extends RecyclerView.ViewHolder {
        Button btn;
        BolumVH(View v) { super(v); btn = v.findViewById(R.id.btnBolum); }
        void bind(ListeOgesi oge) {
            btn.setText("    " + oge.bolum.getTamAd());
            btn.setBackgroundColor(RENK_NORMAL);
            btn.setOnFocusChangeListener((v, hasFocus) ->
                    btn.setBackgroundColor(hasFocus ? RENK_FOCUS : RENK_NORMAL));
            btn.setOnClickListener(v -> {
                if (bolumDinleyici != null) bolumDinleyici.onBolumSecildi(oge.dizi, oge.sezon, oge.bolum);
            });
        }
    }
    private boolean xtreamDiziMi(Dizi dizi) {
        for (com.example.livetvapp.Adapter.Sezon sezon : dizi.getSezonlar()) {
            for (com.example.livetvapp.Adapter.Bolum bolum : sezon.getBolumler()) {
                String url = bolum.getUrl();
                if (url != null && url.contains("/series/")) return true;
            }
        }
        return false;
    }
}