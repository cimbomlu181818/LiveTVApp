package com.example.livetvapp.Adapter;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.example.livetvapp.Channel;
import com.example.livetvapp.R;
import com.example.livetvapp.database.FavorilerYoneticisi;
import java.util.ArrayList;
import java.util.List;
public class KanalAdapter extends RecyclerView.Adapter<KanalAdapter.KanalViewHolder> {


    private List<Channel> kanallar = new ArrayList<>();
    private KanalSecimDinleyici dinleyici;
    private String aktifKanalUrl = null;
    private boolean isLoadingMore = false;
    private OnLoadMoreListener loadMoreListener;
    private long sonTikZamani   = 0L;
    private int  sonTikPozisyon = -1;
    private FavorilerYoneticisi favorilerYoneticisi;
    private String aktifIcerikTipi = null;
    private boolean panelAcik = false;
    private OnFocusDegistiListener focusDegistiListener;
    public interface KanalSecimDinleyici {
        void onKanalSecildi(Channel kanal, int pozisyon);
    }
    public interface OnLoadMoreListener {
        void onLoadMore(int currentPage);
    }
    public interface OnFocusDegistiListener {
        void onFocusDegisti(Channel kanal);
    }
    public interface OnFavoriDegistiListener {
        void onFavoriDegisti(String icerikTipi, boolean eklendi);
    }
    private OnFavoriDegistiListener favoriDegistiListener;
    public void setOnFavoriDegistiListener(OnFavoriDegistiListener listener) {
        this.favoriDegistiListener = listener;
    }
    public void setOnFocusDegistiListener(OnFocusDegistiListener listener) {
        this.focusDegistiListener = listener;
    }
    public KanalAdapter(KanalSecimDinleyici dinleyici) {
        this.dinleyici = dinleyici;
        setHasStableIds(true);
    }
    public void setFavorilerYoneticisi(FavorilerYoneticisi fy, String icerikTipi) {
        this.favorilerYoneticisi = fy;
        this.aktifIcerikTipi     = icerikTipi;
    }
    public void setPanelAcik(boolean acik) {
        this.panelAcik = acik;
    }
    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        this.loadMoreListener = listener;
    }
    public void setKanallar(List<Channel> yeniKanallar) {
        final List<Channel> eskiListe = this.kanallar;
        final List<Channel> yeniListe = yeniKanallar != null ? yeniKanallar : new ArrayList<>();
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return eskiListe.size(); }
            @Override public int getNewListSize() { return yeniListe.size(); }
            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                String oldUrl = eskiListe.get(oldPos).getUrl();
                String newUrl = yeniListe.get(newPos).getUrl();
                return oldUrl != null && oldUrl.equals(newUrl);
            }
            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                Channel old = eskiListe.get(oldPos);
                Channel nov = yeniListe.get(newPos);
                boolean sameName  = old.getName().equals(nov.getName());
                boolean sameUrl   = old.getUrl().equals(nov.getUrl());
                boolean sameAktif = isAktif(old) == isAktif(nov);
                return sameName && sameUrl && sameAktif;
            }
            private boolean isAktif(Channel c) {
                return aktifKanalUrl != null && aktifKanalUrl.equals(c.getUrl());
            }
        });
        this.kanallar = yeniListe;
        diffResult.dispatchUpdatesTo(this);
    }
    public void addKanallar(List<Channel> yeniKanallar) {
        if (yeniKanallar == null || yeniKanallar.isEmpty()) return;
        int startPosition = this.kanallar.size();
        this.kanallar.addAll(yeniKanallar);
        notifyItemRangeInserted(startPosition, yeniKanallar.size());
        isLoadingMore = false;
    }
    public void addKanallarBasa(List<Channel> yeniKanallar) {
        if (yeniKanallar == null || yeniKanallar.isEmpty()) return;
        this.kanallar.addAll(0, yeniKanallar);
        notifyItemRangeInserted(0, yeniKanallar.size());
    }
    public void setAktifKanalUrl(String url) {
        String eskiUrl     = this.aktifKanalUrl;
        this.aktifKanalUrl = url;
        if (eskiUrl != null) {
            int eskiPoz = findPositionByUrl(eskiUrl);
            if (eskiPoz != -1) notifyItemChanged(eskiPoz);
        }
        if (url != null) {
            int yeniPoz = findPositionByUrl(url);
            if (yeniPoz != -1) notifyItemChanged(yeniPoz);
        }
    }
    public String getAktifKanalUrl() { return aktifKanalUrl; }
    public int getAktifPozisyon() {
        if (aktifKanalUrl == null) return -1;
        return findPositionByUrl(aktifKanalUrl);
    }
    private int findPositionByUrl(String url) {
        if (url == null) return -1;
        for (int i = 0; i < kanallar.size(); i++) {
            if (url.equals(kanallar.get(i).getUrl())) return i;
        }
        return -1;
    }
    public Channel getKanal(int pozisyon) {
        if (pozisyon >= 0 && pozisyon < kanallar.size()) return kanallar.get(pozisyon);
        return null;
    }
    @Override
    public long getItemId(int position) {
        Channel c = kanallar.get(position);
        String unique = String.format("%s|%s|%s",
                c.getUrl() != null ? c.getUrl() : "",
                c.getSourceName() != null ? c.getSourceName() : "",
                c.getCategory() != null ? c.getCategory() : "");
        return unique.hashCode();
    }
    private String gorunumAdi(Channel kanal) {
        if (favorilerYoneticisi == null || aktifIcerikTipi == null) return kanal.getName();
        if (favorilerYoneticisi.isFavori(aktifIcerikTipi, kanal.getUrl())) {
            return "⭐ " + kanal.getName();
        }
        return kanal.getName();
    }

    @NonNull
    @Override
    public KanalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.kanal_buton, parent, false);
        return new KanalViewHolder(view);
    }
    public void ilkFocusKanaliniGonder() {
        if (focusDegistiListener != null) {
            int aktifPoz = getAktifPozisyon();
            int hedef    = aktifPoz >= 0 ? aktifPoz : 0;
            if (hedef < kanallar.size()) {
                focusDegistiListener.onFocusDegisti(kanallar.get(hedef));
            }
        }
    }
    @Override
    public void onBindViewHolder(@NonNull KanalViewHolder holder, int position) {
        Channel kanal = kanallar.get(position);
        holder.bind(kanal, position);
        if (loadMoreListener != null && !isLoadingMore && position >= kanallar.size() - 5) {
            isLoadingMore = true;
            int currentPage = (kanallar.size() / 50);
            loadMoreListener.onLoadMore(currentPage);
        }
    }
    @Override
    public int getItemCount() { return kanallar.size(); }
    class KanalViewHolder extends RecyclerView.ViewHolder {
        Button button;

        KanalViewHolder(View itemView) {
            super(itemView);
            button = (Button) itemView;
        }

        void bind(Channel kanal, int position) {
            
            String ad = gorunumAdi(kanal);
            button.setText((position + 1) + " - " + ad);
            button.setTag(kanal);

            boolean isAktif = kanal.getUrl() != null && kanal.getUrl().equals(aktifKanalUrl);

            
            button.setBackgroundResource(R.drawable.kanal_button_background);
            button.setScaleX(1.0f);
            button.setScaleY(1.0f);
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            button.setTypeface(button.getTypeface(), Typeface.NORMAL);

            
            button.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    
                    button.setBackgroundResource(R.drawable.kanal_button_focused);
                    button.setScaleX(1.02f);
                    button.setScaleY(1.02f);
                    button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                    button.setTypeface(button.getTypeface(), Typeface.BOLD);

                    System.out.println("🔍 Buton focus aldı: " + kanal.getName() + " (index: " + position + ")");

                    if (focusDegistiListener != null) {
                        focusDegistiListener.onFocusDegisti(kanal);
                    }
                } else {
                    
                    button.setBackgroundResource(R.drawable.kanal_button_background);
                    button.setScaleX(1.0f);
                    button.setScaleY(1.0f);
                    button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                    button.setTypeface(button.getTypeface(), Typeface.NORMAL);
                }
            });

            
            button.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                System.out.println("👆 Buton tıklandı: " + kanal.getName());

                int eskiAktifPoz = getAktifPozisyon();
                aktifKanalUrl = kanal.getUrl();

                if (eskiAktifPoz != -1 && eskiAktifPoz != pos) {
                    notifyItemChanged(eskiAktifPoz);
                }
                notifyItemChanged(pos);

                if (dinleyici != null) dinleyici.onKanalSecildi(kanal, pos);
            });

            button.setFocusable(true);
            button.setFocusableInTouchMode(true);
        }
    }
}