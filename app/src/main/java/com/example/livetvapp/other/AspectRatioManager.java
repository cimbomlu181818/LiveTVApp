package com.example.livetvapp.other;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.view.Gravity;
import android.widget.Toast;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;
public class AspectRatioManager {
    private Context context;
    private MediaPlayer mediaPlayer;
    private VLCVideoLayout vlcVideoLayout;
    private boolean isStretched = false;
    public AspectRatioManager(Context context, MediaPlayer mediaPlayer, VLCVideoLayout vlcVideoLayout) {
        this.context       = context;
        this.mediaPlayer   = mediaPlayer;
        this.vlcVideoLayout = vlcVideoLayout;
    }
    public void toggleStretchMode() {
        isStretched = !isStretched;
        applyStretchMode(isStretched);
        String message = isStretched ? "Tam Ekran Uzatıldı" : "Orijinal Orana Döndü";
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    public void refreshAspectRatio() {
        if (mediaPlayer != null) {
            applyStretchMode(isStretched);
        }
    }
    private void applyStretchMode(boolean stretch) {
        if (vlcVideoLayout == null) return;
        ViewGroup.LayoutParams params = vlcVideoLayout.getLayoutParams();
        params.width  = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        if (params instanceof FrameLayout.LayoutParams) {
            ((FrameLayout.LayoutParams) params).gravity = Gravity.CENTER;
        }
        if (mediaPlayer != null) {
            if (stretch) {
                int screenWidth  = context.getResources().getDisplayMetrics().widthPixels;
                int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
                mediaPlayer.setAspectRatio(screenWidth + ":" + screenHeight);
                mediaPlayer.setScale(0f);
            } else {
                mediaPlayer.setAspectRatio(null);
                mediaPlayer.setScale(1.0f);
            }
        }
        vlcVideoLayout.setLayoutParams(params);
        vlcVideoLayout.requestLayout();
    }
}