package com.example.livetvapp.Adapter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.livetvapp.R;
import com.example.livetvapp.database.M3UItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
public class M3USilmeAdapter extends RecyclerView.Adapter<M3USilmeAdapter.ViewHolder> {
    private static final int RENK_NORMAL   = 0xFF1A3A5A;
    private static final int RENK_FOCUS   = 0xFFFF0000;
    private static final int RENK_SELECTED = 0x882A4A72;
    private List<M3UItem> m3uItems;
    private Set<String> selectedItems; 
    public M3USilmeAdapter(List<M3UItem> items) {
        this.m3uItems = items != null ? items : new ArrayList<>();
        this.selectedItems = new HashSet<>();
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.m3u_item_layout, parent, false);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        M3UItem item = m3uItems.get(position);
        holder.bind(item, position);
    }
    @Override
    public int getItemCount() {
        return m3uItems.size();
    }
    public List<String> getSelectedM3UNames() {
        return new ArrayList<>(selectedItems);
    }
    public void clearSelection() {
        selectedItems.clear();
        notifyDataSetChanged();
    }
    public void selectAll() {
        selectedItems.clear();
        for (M3UItem item : m3uItems) {
            selectedItems.add(item.getName());
        }
        notifyDataSetChanged();
    }
    class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView countText;
        CheckBox checkbox;
        View containerView;
        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.m3uNameText);
            countText = itemView.findViewById(R.id.channelCountText);
            checkbox = itemView.findViewById(R.id.m3uCheckbox);
            containerView = itemView.findViewById(R.id.m3uItemContainer);
            containerView.setFocusable(true);
            containerView.setFocusableInTouchMode(true);
            checkbox.setFocusable(false);
            checkbox.setFocusableInTouchMode(false);
        }
        void bind(M3UItem item, int position) {
            nameText.setText(item.getName());
            countText.setText(item.getChannelCount() + " kanal");
            boolean isSelected = selectedItems.contains(item.getName());
            checkbox.setChecked(isSelected);
            updateBackgroundColor(false, isSelected);
            containerView.setOnFocusChangeListener((v, hasFocus) -> {
                updateBackgroundColor(hasFocus, isSelected);
                System.out.println("🎯 M3U Item focus: " + item.getName() + " - " + hasFocus);
            });
            containerView.setOnClickListener(v -> {
                toggleSelection(item);
                System.out.println("👆 M3U Item clicked: " + item.getName());
            });
            containerView.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                            keyCode == KeyEvent.KEYCODE_ENTER) {
                        toggleSelection(item);
                        System.out.println("⌨️ D-pad ENTER/CENTER: " + item.getName());
                        return true;
                    }
                }
                return false;
            });
            checkbox.setOnClickListener(v -> {
                toggleSelection(item);
                System.out.println("☑️ Checkbox clicked: " + item.getName());
            });
        }
        private void toggleSelection(M3UItem item) {
            boolean isCurrentlySelected = selectedItems.contains(item.getName());
            if (isCurrentlySelected) {
                selectedItems.remove(item.getName());
                System.out.println("❌ Selection removed: " + item.getName());
            } else {
                selectedItems.add(item.getName());
                System.out.println("✅ Selection added: " + item.getName());
            }
            checkbox.setChecked(!isCurrentlySelected);
            updateBackgroundColor(containerView.hasFocus(), !isCurrentlySelected);
            notifyItemChanged(getAdapterPosition());
        }
        private void updateBackgroundColor(boolean hasFocus, boolean isSelected) {
            if (hasFocus) {
                containerView.setBackgroundColor(RENK_FOCUS);
            } else if (isSelected) {
                containerView.setBackgroundColor(RENK_SELECTED);
            } else {
                containerView.setBackgroundColor(RENK_NORMAL);
            }
        }
    }
}