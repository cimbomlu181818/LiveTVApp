package com.example.livetvapp.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.example.livetvapp.Channel;
import com.example.livetvapp.R;
public class M3UPagingAdapter extends PagingDataAdapter<Channel, M3UPagingAdapter.ChannelViewHolder> {
    private static final int RENK_NORMAL = 0xFF1A3A5A;
    private static final int RENK_FOCUS  = 0x88FF0000;
    private OnChannelClickListener clickListener;
    public interface OnChannelClickListener {
        void onChannelClick(Channel channel, int position);
    }
    public M3UPagingAdapter() {
        super(DIFF_CALLBACK);
    }
    public void setOnChannelClickListener(OnChannelClickListener listener) {
        this.clickListener = listener;
    }
    private static final DiffUtil.ItemCallback<Channel> DIFF_CALLBACK = new DiffUtil.ItemCallback<Channel>() {
        @Override
        public boolean areItemsTheSame(@NonNull Channel oldItem, @NonNull Channel newItem) {
            return oldItem.getName().equals(newItem.getName()) &&
                    oldItem.getUrl().equals(newItem.getUrl());
        }
        @Override
        public boolean areContentsTheSame(@NonNull Channel oldItem, @NonNull Channel newItem) {
            return oldItem.getName().equals(newItem.getName()) &&
                    oldItem.getUrl().equals(newItem.getUrl()) &&
                    oldItem.getCategory().equals(newItem.getCategory());
        }
    };
    @NonNull
    @Override
    public ChannelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.kanal_buton, parent, false);
        return new ChannelViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ChannelViewHolder holder, int position) {
        Channel channel = getItem(position);
        if (channel != null) {
            holder.bind(channel, position);
        }
    }
    class ChannelViewHolder extends RecyclerView.ViewHolder {
        Button button;
        ChannelViewHolder(View itemView) {
            super(itemView);
            button = (Button) itemView;
        }
        void bind(Channel channel, int position) {
            button.setText(channel.getName());
            button.setBackgroundColor(RENK_NORMAL);
            button.setOnFocusChangeListener((v, hasFocus) -> {
                v.setBackgroundColor(hasFocus ? RENK_FOCUS : RENK_NORMAL);
            });
            button.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onChannelClick(channel, position);
                }
            });
            button.setFocusable(true);
            button.setFocusableInTouchMode(true);
        }
    }
}