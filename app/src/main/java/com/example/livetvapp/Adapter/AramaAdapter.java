package com.example.livetvapp.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.livetvapp.Channel;
import java.util.ArrayList;
import java.util.List;
public class AramaAdapter extends RecyclerView.Adapter<AramaAdapter.VH> {
    public interface OnItemClickListener {
        void onClick(Channel kanal);
    }
    private final List<Channel> liste;
    private final OnItemClickListener listener;
    private Runnable kapatListener;
    private java.util.Map<String, Integer> siraMap = new java.util.HashMap<>();
    public void setKapatListener(Runnable r) { this.kapatListener = r; }
    public void ekleSiraBilgisi(java.util.Map<String, Integer> yeniSiralar) {
        if (yeniSiralar != null) this.siraMap.putAll(yeniSiralar);
    }
    public AramaAdapter(List<Channel> liste, OnItemClickListener listener) {
        this.liste    = new ArrayList<>(liste);
        this.listener = listener;
    }
    public void appendItems(List<Channel> yeniOgeler) {
        if (yeniOgeler == null || yeniOgeler.isEmpty()) return;
        int baslangic = liste.size();
        liste.addAll(yeniOgeler);
        notifyItemRangeInserted(baslangic, yeniOgeler.size());
    }
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        v.setFocusable(true);
        v.setFocusableInTouchMode(true);
        v.setBackgroundColor(0xAA330000);
        v.setOnFocusChangeListener((view, hasFocus) -> {
            view.setBackgroundColor(hasFocus ? 0xFFFFDD00 : 0xAA330000);
            TextView tv = view.findViewById(android.R.id.text1);
            if (tv != null) tv.setTextColor(hasFocus ? 0xFF000000 : 0xFFFFFFFF);
        });
        return new VH(v);
    }
    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Channel kanal = liste.get(position);
        Integer kayitliSira = kanal.getUrl() != null ? siraMap.get(kanal.getUrl()) : null;
        int sira = kayitliSira != null ? kayitliSira : (kanal.getPosition() + 1);
        String logMsg = "🔍 [ARAMA-SIRA] kanal=" + kanal.getName()
                + " | position=" + kanal.getPosition()
                + " | source=" + kanal.getSourceName()
                + " | category=" + kanal.getCategory();
        System.out.println(logMsg);
        holder.tv.setText(sira + " - " + kanal.getName());
        holder.tv.setTextColor(0xFFFFFFFF);
        holder.itemView.setOnClickListener(v -> listener.onClick(kanal));
        holder.itemView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != android.view.KeyEvent.ACTION_DOWN) return false;
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK
                    || keyCode == android.view.KeyEvent.KEYCODE_ESCAPE) {
                if (kapatListener != null) kapatListener.run();
                return true;
            }
            if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER
                    || keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                listener.onClick(kanal);
                return true;
            }
            return false;
        });
    }
    @Override
    public int getItemCount() { return liste != null ? liste.size() : 0; }
    static class VH extends RecyclerView.ViewHolder {
        TextView tv;
        VH(View v) {
            super(v);
            tv = v.findViewById(android.R.id.text1);
        }
    }
}