package com.example.livetvapp.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.livetvapp.R;
import com.example.livetvapp.database.GizleItem;
import java.util.ArrayList;
import java.util.List;
public class GizleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int RENK_M3U_NORMAL   = 0xFF1A3A5A;
    private static final int RENK_KAT_NORMAL   = 0xFF1A3A5A;
    private static final int RENK_KANAL_NORMAL  = 0xFF1A3A5A;
    private static final int RENK_FOCUS         = 0xFFFF0000;
    private static final int RENK_M3U_GIZLI    = 0x661A3A5A;
    private static final int RENK_KAT_GIZLI    = 0x661A3A5A;
    private static final int RENK_KANAL_GIZLI  = 0x661A3A5A;
    public interface OnGizlemeDegisti {
        void onDegisti(GizleItem item, boolean yeniGizliDurum);
    }
    public interface OnBaslikTiklandi {
        void onTiklandi(GizleItem item, int pozisyon);
    }
    public interface OnFocusDegisti {
        void onFocusAlindi(GizleItem item, int pozisyon);
    }
    private final List<GizleItem> liste = new ArrayList<>();
    private OnGizlemeDegisti gizlemeDinleyici;
    private OnBaslikTiklandi baslikDinleyici;
    private OnFocusDegisti   focusDinleyici;
    public void setOnGizlemeDegisti(OnGizlemeDegisti l)  { this.gizlemeDinleyici = l; }
    public void setOnBaslikTiklandi(OnBaslikTiklandi l)  { this.baslikDinleyici  = l; }
    public void setOnFocusDegisti(OnFocusDegisti l)      { this.focusDinleyici   = l; }
    public void setListe(List<GizleItem> yeni) {
        int eskiBoyut = liste.size();
        liste.clear();
        if (eskiBoyut > 0) notifyItemRangeRemoved(0, eskiBoyut);
        if (yeni != null && !yeni.isEmpty()) {
            liste.addAll(yeni);
            notifyItemRangeInserted(0, liste.size());
        }
    }
    public void ekle(int pozisyon, List<GizleItem> ogeler) {
        if (ogeler == null || ogeler.isEmpty()) return;
        liste.addAll(pozisyon, ogeler);
        notifyItemRangeInserted(pozisyon, ogeler.size());
    }
    public void kaldir(int pozisyon, int sayi) {
        if (sayi <= 0 || pozisyon < 0 || pozisyon + sayi > liste.size()) return;
        liste.subList(pozisyon, pozisyon + sayi).clear();
        notifyItemRangeRemoved(pozisyon, sayi);
    }
    public void guncelle(int pozisyon) {
        if (pozisyon >= 0 && pozisyon < liste.size()) notifyItemChanged(pozisyon);
    }
    public GizleItem getItem(int pozisyon) {
        if (pozisyon >= 0 && pozisyon < liste.size()) return liste.get(pozisyon);
        return null;
    }
    public List<GizleItem> getListe() { return liste; }
    @Override public int getItemViewType(int pos) { return liste.get(pos).getTip(); }
    @Override public int getItemCount()           { return liste.size(); }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case GizleItem.TIP_M3U:
                return new M3UVH(inf.inflate(R.layout.item_gizle_m3u, parent, false));
            case GizleItem.TIP_KATEGORI:
                return new KategoriVH(inf.inflate(R.layout.item_gizle_kategori, parent, false));
            default:
                return new KanalVH(inf.inflate(R.layout.item_gizle_kanal, parent, false));
        }
    }
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        GizleItem item = liste.get(position);
        if      (holder instanceof M3UVH)      ((M3UVH)      holder).bind(item);
        else if (holder instanceof KategoriVH) ((KategoriVH) holder).bind(item);
        else if (holder instanceof KanalVH)    ((KanalVH)    holder).bind(item);
    }
    class M3UVH extends RecyclerView.ViewHolder {
        final Button      btn;
        final SwitchCompat sw;
        M3UVH(View v) {
            super(v);
            btn = v.findViewById(R.id.btnGizleM3U);
            sw  = v.findViewById(R.id.switchGizleM3U);
        }
        void bind(GizleItem item) {
            String ok  = item.isAcik() ? "▼ " : "▶ ";
            btn.setText(ok + item.getAd());
            sw.setChecked(!item.isGizli());
            int bazRenk = item.isGizli() ? RENK_M3U_GIZLI : RENK_M3U_NORMAL;
            btn.setBackgroundColor(bazRenk);
            btn.setOnFocusChangeListener((v, hasFocus) -> {
                v.setBackgroundColor(hasFocus ? RENK_FOCUS : bazRenk);
                if (hasFocus && focusDinleyici != null) {
                    int p = getAdapterPosition();
                    if (p != RecyclerView.NO_POSITION)
                        focusDinleyici.onFocusAlindi(item, p);
                }
            });
            btn.setOnClickListener(v -> {
                int p = getAdapterPosition();
                if (p != RecyclerView.NO_POSITION && baslikDinleyici != null)
                    baslikDinleyici.onTiklandi(item, p);
            });
            sw.setOnClickListener(v -> {
                boolean yeniDurum = !item.isGizli();
                item.setGizli(yeniDurum);
                notifyItemChanged(getAdapterPosition());
                if (gizlemeDinleyici != null)
                    gizlemeDinleyici.onDegisti(item, yeniDurum);
            });
            sw.setOnClickListener(v -> {
                boolean yeniDurum = !item.isGizli();
                item.setGizli(yeniDurum);
                notifyItemChanged(getAdapterPosition());
                if (gizlemeDinleyici != null)
                    gizlemeDinleyici.onDegisti(item, yeniDurum);
            });
        }
    }
    class KategoriVH extends RecyclerView.ViewHolder {
        final Button      btn;
        final SwitchCompat sw;
        KategoriVH(View v) {
            super(v);
            btn = v.findViewById(R.id.btnGizleKategori);
            sw  = v.findViewById(R.id.switchGizleKategori);
        }
        void bind(GizleItem item) {
            String ok  = item.isAcik() ? "  ▼ " : "  ▶ ";
            btn.setText(ok + item.getAd());
            sw.setChecked(!item.isGizli());
            int bazRenk = item.isGizli() ? RENK_KAT_GIZLI : RENK_KAT_NORMAL;
            btn.setBackgroundColor(bazRenk);
            btn.setOnFocusChangeListener((v, hasFocus) -> {
                v.setBackgroundColor(hasFocus ? RENK_FOCUS : bazRenk);
                if (hasFocus && focusDinleyici != null) {
                    int p = getAdapterPosition();
                    if (p != RecyclerView.NO_POSITION)
                        focusDinleyici.onFocusAlindi(item, p);
                }
            });
            btn.setOnClickListener(v -> {
                int p = getAdapterPosition();
                if (p != RecyclerView.NO_POSITION && baslikDinleyici != null)
                    baslikDinleyici.onTiklandi(item, p);
            });
        }
    }
    class KanalVH extends RecyclerView.ViewHolder {
        final Button      btn;
        final SwitchCompat sw;
        KanalVH(View v) {
            super(v);
            btn = v.findViewById(R.id.btnGizleKanal);
            sw  = v.findViewById(R.id.switchGizleKanal);
        }
        void bind(GizleItem item) {
            btn.setText("      " + item.getAd());
            sw.setChecked(!item.isGizli());
            int bazRenk = item.isGizli() ? RENK_KANAL_GIZLI : RENK_KANAL_NORMAL;
            btn.setBackgroundColor(bazRenk);
            btn.setOnFocusChangeListener((v, hasFocus) -> {
                v.setBackgroundColor(hasFocus ? RENK_FOCUS : bazRenk);
                if (hasFocus && focusDinleyici != null) {
                    int p = getAdapterPosition();
                    if (p != RecyclerView.NO_POSITION)
                        focusDinleyici.onFocusAlindi(item, p);
                }
            });
            btn.setOnClickListener(null);
        }
    }
}