package com.example.livetvapp.Adapter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.livetvapp.R;
import com.example.livetvapp.database.M3UItem;
import java.util.List;
public class M3UIsmDegistirAdapter extends
        RecyclerView.Adapter<M3UIsmDegistirAdapter.ViewHolder> {
    public interface OnM3USecildi {
        void onSecildi(M3UItem item);
    }
    private final List<M3UItem> liste;
    private final OnM3USecildi listener;
    public M3UIsmDegistirAdapter(List<M3UItem> liste, OnM3USecildi listener) {
        this.liste    = liste;
        this.listener = listener;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.m3u_item_layout, parent, false);
        return new ViewHolder(v);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(liste.get(position));
    }
    @Override
    public int getItemCount() { return liste.size(); }
    class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView countText;
        View containerView;
        ViewHolder(View v) {
            super(v);
            nameText      = v.findViewById(R.id.m3uNameText);
            countText     = v.findViewById(R.id.channelCountText);
            containerView = v.findViewById(R.id.m3uItemContainer);
            containerView.setFocusable(true);
            containerView.setFocusableInTouchMode(true);
        }
        void bind(M3UItem item) {
            nameText.setText(item.getName());
            countText.setText(item.getChannelCount() + " kanal");
            containerView.setBackgroundColor(0x00000000);
            nameText.setTextColor(0xFF888888);
            countText.setTextColor(0xFF555555);
            containerView.setOnFocusChangeListener((v, hasFocus) -> {
                containerView.setBackgroundColor(
                        hasFocus ? 0x22E50914 : 0x00000000);
                nameText.setTextColor(hasFocus ? 0xFFFFFFFF : 0xFF888888);
                countText.setTextColor(hasFocus ? 0xFFAAAAAA : 0xFF555555);
            });
            containerView.setOnClickListener(v -> {
                if (listener != null) listener.onSecildi(item);
            });
            containerView.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                            || keyCode == KeyEvent.KEYCODE_ENTER) {
                        if (listener != null) listener.onSecildi(item);
                        return true;
                    }
                }
                return false;
            });
        }
    }
}