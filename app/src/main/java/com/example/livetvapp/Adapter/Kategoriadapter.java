package com.example.livetvapp.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.livetvapp.R;
import com.example.livetvapp.database.KategoriItem;
import java.util.ArrayList;
import java.util.List;
public class Kategoriadapter extends RecyclerView.Adapter<Kategoriadapter.KategoriViewHolder> {
    private static final int RENK_NORMAL  = 0xFF1A3A5A;
    private static final int RENK_FOCUS   = 0xFFFF0000;
    private static final int RENK_AYARLAR = 0x801A3A6A;
    private static final int VIEW_TYPE_NORMAL  = 0;
    private static final int VIEW_TYPE_AYARLAR = 1;
    private List<KategoriItem> kategoriler = new ArrayList<>();
    private int aktifPozisyon = -1;
    private boolean ayarlarButonuGoster = false;
    public void setAyarlarButonuGoster(boolean goster) {
        this.ayarlarButonuGoster = goster;
        notifyDataSetChanged();
    }
    private OnKategoriTiklandi    tiklandi;
    private OnKategoriFocusDegisti focusDegisti;
    private OnAyarlarTiklandi     ayarlarTiklandi;
    private OnAyarlarFocusDegisti ayarlarFocusDegisti;
    public interface OnKategoriTiklandi {
        void onTiklandi(String kategoriAdi);
    }
    public interface OnKategoriFocusDegisti {
        void onFocusDegisti(String kategoriAdi);
    }
    public interface OnAyarlarTiklandi {
        void onAyarlarTiklandi();
    }
    public interface OnAyarlarFocusDegisti {
        void onAyarlarFocus(boolean hasFocus);
    }
    public Kategoriadapter(OnKategoriTiklandi tiklandi) {
        this.tiklandi = tiklandi;
    }
    public void setOnKategoriFocusDegisti(OnKategoriFocusDegisti focusDegisti) {
        this.focusDegisti = focusDegisti;
    }
    public void setOnAyarlarTiklandi(OnAyarlarTiklandi listener) {
        this.ayarlarTiklandi = listener;
    }
    public void setOnAyarlarFocusDegisti(OnAyarlarFocusDegisti listener) {
        this.ayarlarFocusDegisti = listener;
    }
    public void setKategoriler(List<KategoriItem> liste) {
        this.kategoriler = liste != null ? liste : new ArrayList<>();
        notifyDataSetChanged();
    }
    public void addKategoriler(List<KategoriItem> yenilar) {
        if (yenilar == null || yenilar.isEmpty()) return;
        int baslangic = kategoriler.size();
        kategoriler.addAll(yenilar);
        notifyItemRangeInserted(baslangic, yenilar.size());
    }
    public void addKategorilerBasa(List<KategoriItem> yenilar) {
        if (yenilar == null || yenilar.isEmpty()) return;
        kategoriler.addAll(0, yenilar);
        notifyItemRangeInserted(0, yenilar.size());
    }
    public void setAktifPozisyon(int pozisyon) {
        int eski = aktifPozisyon;
        aktifPozisyon = pozisyon;
        if (eski >= 0 && eski < getItemCount()) notifyItemChanged(eski);
        if (pozisyon >= 0 && pozisyon < getItemCount()) notifyItemChanged(pozisyon);
    }
    public int getAktifPozisyon() {
        return aktifPozisyon;
    }
    public KategoriItem getKategoriItem(int pozisyon) {
        if (pozisyon >= 0 && pozisyon < kategoriler.size()) return kategoriler.get(pozisyon);
        return null;
    }
    public String getKategori(int pozisyon) {
        KategoriItem item = getKategoriItem(pozisyon);
        return item != null ? item.getKategoriAdi() : null;
    }
    @NonNull
    @Override
    public KategoriViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.kategori_buton, parent, false);
        return new KategoriViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull KategoriViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_AYARLAR) {
            holder.bindAyarlar();
        } else {
            holder.bind(kategoriler.get(position), position);
        }
    }
    @Override
    public int getItemCount() {
        return ayarlarButonuGoster ? kategoriler.size() + 1 : kategoriler.size();
    }
    @Override
    public int getItemViewType(int position) {
        return (ayarlarButonuGoster && position == kategoriler.size()) ? VIEW_TYPE_AYARLAR : VIEW_TYPE_NORMAL;
    }
    class KategoriViewHolder extends RecyclerView.ViewHolder {
        Button button;
        KategoriViewHolder(View itemView) {
            super(itemView);
            button = (Button) itemView;
        }
        void bind(KategoriItem item, int position) {
            String metin = item.getGorunumMetni();
            button.setText((position + 1) + " - " + item.getGorunumMetni());
            button.setAlpha(1.0f);
            button.setFocusable(true);
            button.setFocusableInTouchMode(true);
            button.setClickable(true);

            
            button.setBackgroundColor(RENK_NORMAL);

            button.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.setBackgroundColor(RENK_FOCUS);
                    if (focusDegisti != null) {
                        focusDegisti.onFocusDegisti(item.getKategoriAdi());
                    }
                } else {
                    
                    v.setBackgroundColor(RENK_NORMAL);
                }
            });

            button.setOnClickListener(v -> {
                if (tiklandi != null) {
                    tiklandi.onTiklandi(item.getKategoriAdi());
                }
            });
        }
        void bindAyarlar() {
            button.setText("⚙ Ayarlar");
            button.setAlpha(1.0f);
            button.setFocusable(true);
            button.setFocusableInTouchMode(true);
            button.setClickable(true);
            button.setBackgroundColor(RENK_AYARLAR);
            button.setOnFocusChangeListener((v, hasFocus) -> {
                v.setBackgroundColor(hasFocus ? RENK_FOCUS : RENK_AYARLAR);
                if (ayarlarFocusDegisti != null) ayarlarFocusDegisti.onAyarlarFocus(hasFocus);
            });
            button.setOnClickListener(v -> {
                if (ayarlarTiklandi != null) ayarlarTiklandi.onAyarlarTiklandi();
            });
        }
    }
}