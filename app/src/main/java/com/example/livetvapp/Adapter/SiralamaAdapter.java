package com.example.livetvapp.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.example.livetvapp.R;
import com.example.livetvapp.database.SiralamaItem;
import java.util.ArrayList;
import java.util.List;
public class SiralamaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int RENK_TUR      = 0xFF1A3A5A;
    private static final int RENK_KATEGORI = 0xFF1A3A5A;
    private static final int RENK_DIZI     = 0xFF1A3A5A;
    private static final int RENK_KANAL    = 0xFF1A3A5A;
    private static final int RENK_FOCUS   = 0xFFFF0000;
    private static final int RENK_SECILI   = 0xBB00CC00;
    private List<SiralamaItem> liste = new ArrayList<>();
    public interface OgeAcKapatCallback { void onAcKapat(int adapterPozisyon); }
    public interface OgeTasimaCallback  { void onTasi(int hedefAdapterPoz); }
    public interface SagTusCallback     { void onSagTus(int adapterPozisyon); }
    public interface SolTusCallback     { void onSolTus(int adapterPozisyon); }
    private OgeAcKapatCallback acKapatCallback;
    private OgeTasimaCallback  tasimaCallback;
    private SagTusCallback     sagTusCallback;
    private SolTusCallback     solTusCallback;
    public void setAcKapatCallback(OgeAcKapatCallback cb) { this.acKapatCallback = cb; }
    public void setTasimaCallback(OgeTasimaCallback cb)   { this.tasimaCallback  = cb; }
    public void setSagTusCallback(SagTusCallback cb)      { this.sagTusCallback  = cb; }
    public void setSolTusCallback(SolTusCallback cb)      { this.solTusCallback  = cb; }
    public void setTumListe(List<SiralamaItem> yeniListe) {
        guncelleListeyi(yeniListe != null ? yeniListe : new ArrayList<>());
    }
    private void guncelleListeyi(final List<SiralamaItem> yeniListe) {
        final List<SiralamaItem> eskiListe = new ArrayList<>(liste);
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return eskiListe.size(); }
            @Override public int getNewListSize() { return yeniListe.size(); }
            @Override
            public boolean areItemsTheSame(int op, int np) {
                SiralamaItem o = eskiListe.get(op);
                SiralamaItem n = yeniListe.get(np);
                if (o.getTip() != n.getTip()) return false;
                switch (o.getTip()) {
                    case SiralamaItem.TIP_KANAL:
                        return o.getKanalId() == n.getKanalId();
                    case SiralamaItem.TIP_KATEGORI:
                        return o.getAd().equals(n.getAd())
                                && o.getTur().equals(n.getTur());
                    case SiralamaItem.TIP_TUR:
                        return o.getTur().equals(n.getTur());
                    case SiralamaItem.TIP_DIZI:
                        return o.getAd().equals(n.getAd())
                                && o.getTur().equals(n.getTur());
                    default:
                        return o.getAd().equals(n.getAd());
                }
            }
            @Override
            public boolean areContentsTheSame(int op, int np) {
                SiralamaItem o = eskiListe.get(op);
                SiralamaItem n = yeniListe.get(np);
                return o.getGoruntuMetni().equals(n.getGoruntuMetni())
                        && o.isSecili() == n.isSecili()
                        && o.isAcik()   == n.isAcik();
            }
        });
        liste.clear();
        liste.addAll(yeniListe);
        diff.dispatchUpdatesTo(this);
    }
    public List<SiralamaItem> getListe() { return liste; }
    public int getSeciliSayisi() {
        int sayi = 0;
        for (SiralamaItem item : liste) {
            if (item.isSecili()) sayi++;
        }
        return sayi;
    }
    public void secimleriTemizle() {
        boolean degisti = false;
        for (SiralamaItem item : liste) {
            if (item.isSecili()) {
                item.setSecili(false);
                degisti = true;
            }
        }
        if (degisti) guncelleListeyi(new ArrayList<>(liste));
    }
    @Override public int getItemViewType(int pos) { return liste.get(pos).getTip(); }
    @Override public int getItemCount()           { return liste.size(); }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        View view = inf.inflate(R.layout.kanal_buton, parent, false);
        return new OgeVH(view);
    }
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        OgeVH vh = (OgeVH) holder;
        SiralamaItem item = liste.get(position);
        vh.bind(item, position);
    }
    class OgeVH extends RecyclerView.ViewHolder {
        Button btn;
        OgeVH(View v) {
            super(v);
            btn = (Button) v;
        }
        void bind(SiralamaItem item, int position) {
            btn.setText(item.getGoruntuMetni());
            btn.setBackgroundColor(normalRenk(item));
            btn.setOnFocusChangeListener((v, hasFocus) ->
                    v.setBackgroundColor(hasFocus ? RENK_FOCUS : normalRenk(item)));
            btn.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                System.out.println("🟡 SA.onClick | pos=" + pos + " | seciliSayisi=" + getSeciliSayisi());
                if (getSeciliSayisi() > 0) {
                    System.out.println("🟡 SA.onClick → tasi(" + pos + ")");
                    if (tasimaCallback != null) tasimaCallback.onTasi(pos);
                } else {
                    System.out.println("🟡 SA.onClick → acKapat(" + pos + ")");
                    if (acKapatCallback != null) acKapatCallback.onAcKapat(pos);
                }
            });
            btn.setOnLongClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return true;
                SiralamaItem it = liste.get(pos);
                if (it.getTip() == SiralamaItem.TIP_TUR) return true; 
                boolean yeniSecim = !it.isSecili();
                it.setSecili(yeniSecim);
                notifyItemChanged(pos);
                System.out.println("🖐️ Uzun basma: pos=" + pos + " secili=" + yeniSecim);
                return true;
            });
            btn.setOnKeyListener((v, keyCode, event) -> {
                System.out.println("🔵 SA.onKeyListener | keyCode=" + keyCode + " | action=" + event.getAction() + " | pos=" + getAdapterPosition());
                if (event.getAction() != android.view.KeyEvent.ACTION_DOWN) return false;
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return false;
                if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
                    System.out.println("🔵 SA.onKeyListener → sagTus(" + pos + ")");
                    if (sagTusCallback != null) sagTusCallback.onSagTus(pos);
                    return true;
                }
                if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT) {
                    System.out.println("🔵 SA.onKeyListener → solTus(" + pos + ")");
                    if (solTusCallback != null) solTusCallback.onSolTus(pos);
                    return true;
                }
                System.out.println("🔵 SA.onKeyListener → return false (keyCode=" + keyCode + ")");
                return false;
            });
            btn.setFocusable(true);
            btn.setFocusableInTouchMode(true);
        }
        private int normalRenk(SiralamaItem item) {
            if (item.isSecili()) return RENK_SECILI;
            switch (item.getTip()) {
                case SiralamaItem.TIP_TUR:      return RENK_TUR;
                case SiralamaItem.TIP_KATEGORI: return RENK_KATEGORI;
                case SiralamaItem.TIP_DIZI:     return RENK_DIZI;
                default:                        return RENK_KANAL;
            }
        }
    }
}