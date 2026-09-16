package com.example.livetvapp.tvbox;

import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.example.livetvapp.paneller.SiralamaPaneli;

public class SanalDpad {
    private final AppCompatActivity activity;
    private final Dpad dpad;
    private SiralamaPaneli siralamaPaneli;

    public void setSiralamaPaneli(SiralamaPaneli sp) { this.siralamaPaneli = sp; }

    private FrameLayout containerView;
    private FrameLayout engelOverlay;
    private boolean gorünür = false;

    public SanalDpad(AppCompatActivity activity, Dpad dpad) {
        this.activity = activity;
        this.dpad = dpad;
        olustur();
    }

    private int dp(int dp) {
        return Math.round(dp * activity.getResources().getDisplayMetrics().density);
    }

    private void olustur() {
        
        engelOverlay = new FrameLayout(activity);
        engelOverlay.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        engelOverlay.setOnTouchListener((v, e) -> true);
        engelOverlay.setVisibility(View.GONE);
        activity.addContentView(engelOverlay, engelOverlay.getLayoutParams());

        
        containerView = new FrameLayout(activity);
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                dp(170), FrameLayout.LayoutParams.WRAP_CONTENT);
        containerParams.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
        containerParams.rightMargin = dp(8);
        containerView.setLayoutParams(containerParams);

        
        LinearLayout dikey = new LinearLayout(activity);
        dikey.setOrientation(LinearLayout.VERTICAL);
        dikey.setGravity(Gravity.CENTER_HORIZONTAL);

        
        Button btnyukari = butonOlustur("▲");
        Button btnAsagi = butonOlustur("▼");
        Button btnSol = butonOlustur("◀");
        Button btnSag = butonOlustur("▶");
        Button btnMerkez = butonMerkezOlustur("●");

        
        LinearLayout orta = new LinearLayout(activity);
        orta.setOrientation(LinearLayout.HORIZONTAL);
        orta.setGravity(Gravity.CENTER);

        
        Button btnGeri = butonGeriOlustur("✕ Geri");

        
        orta.addView(btnSol);
        orta.addView(btnMerkez);
        orta.addView(btnSag);

        dikey.addView(btnyukari);
        dikey.addView(orta);
        dikey.addView(btnAsagi);
        dikey.addView(btnGeri);

        containerView.addView(dikey);

        
        btnyukari.setOnClickListener(v -> tusGonder(KeyEvent.KEYCODE_DPAD_UP));
        btnAsagi.setOnClickListener(v -> tusGonder(KeyEvent.KEYCODE_DPAD_DOWN));
        btnSol.setOnClickListener(v -> tusGonder(KeyEvent.KEYCODE_DPAD_LEFT));
        btnSag.setOnClickListener(v -> tusGonder(KeyEvent.KEYCODE_DPAD_RIGHT));
        btnMerkez.setOnClickListener(v -> tusGonder(KeyEvent.KEYCODE_DPAD_CENTER));
        btnGeri.setOnClickListener(v -> tusGonder(KeyEvent.KEYCODE_BACK));

        containerView.setVisibility(View.GONE);
        activity.addContentView(containerView, containerView.getLayoutParams());
    }

    
    private Button butonOlustur(String metin) {
        Button btn = new Button(activity);
        btn.setText(metin);
        btn.setTextSize(22);  
        btn.setTextColor(0xFFEEF5FF);  
        btn.setBackgroundColor(0xFF2A2A4A);  
        btn.setAllCaps(false);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(52), dp(52));
        lp.setMargins(dp(6), dp(6), dp(6), dp(6));
        btn.setLayoutParams(lp);
        btn.setPadding(0, 0, 0, 0);
        btn.setFocusable(false);

        
        btn.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.setBackgroundColor(0xFF3A3A6A);
            } else {
                v.setBackgroundColor(0xFF2A2A4A);
            }
        });

        return btn;
    }

    
    private Button butonMerkezOlustur(String metin) {
        Button btn = new Button(activity);
        btn.setText(metin);
        btn.setTextSize(26);  
        btn.setTextColor(0xFFEEF5FF);
        btn.setBackgroundColor(0xFF3A3A6A);  
        btn.setAllCaps(false);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(52), dp(52));
        lp.setMargins(dp(6), dp(6), dp(6), dp(6));
        btn.setLayoutParams(lp);
        btn.setPadding(0, 0, 0, 0);
        btn.setFocusable(false);

        btn.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.setBackgroundColor(0xFF5A5A8A);
            } else {
                v.setBackgroundColor(0xFF3A3A6A);
            }
        });

        return btn;
    }

    
    private Button butonGeriOlustur(String metin) {
        Button btn = new Button(activity);
        btn.setText(metin);
        btn.setTextSize(13);  
        btn.setTextColor(0xFFEEF5FF);
        btn.setBackgroundColor(0xFF2A2A4A);
        btn.setAllCaps(false);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(117), dp(39));
        lp.setMargins(dp(4), dp(8), dp(4), dp(4));
        btn.setLayoutParams(lp);
        btn.setPadding(0, 0, 0, 0);
        btn.setFocusable(false);

        btn.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.setBackgroundColor(0xFF3A3A6A);
            } else {
                v.setBackgroundColor(0xFF2A2A4A);
            }
        });

        return btn;
    }

    public void goster() {
        if (containerView != null) {
            engelOverlay.setVisibility(View.VISIBLE);
            containerView.setVisibility(View.VISIBLE);
            gorünür = true;
            System.out.println("📱 SanalDpad GÖSTERİLDİ");
        }
    }

    public void gizle() {
        if (containerView != null) {
            engelOverlay.setVisibility(View.GONE);
            containerView.setVisibility(View.GONE);
            gorünür = false;
            System.out.println("📱 SanalDpad GİZLENDİ");
        }
    }

    public boolean isVisible() {
        return gorünür;
    }

    private void tusGonder(int keyCode) {
        
        if (siralamaPaneli != null && siralamaPaneli.isVisible()) {
            androidx.recyclerview.widget.RecyclerView rv = siralamaPaneli.getRecyclerView();
            if (rv != null) {
                if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP
                        || keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN) {
                    View focused = rv.getFocusedChild();
                    if (focused != null) {
                        int yön = (keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP)
                                ? View.FOCUS_UP : View.FOCUS_DOWN;
                        View sonraki = focused.focusSearch(yön);
                        if (sonraki != null) {
                            sonraki.requestFocus();
                        }
                    } else {
                        rv.requestFocus();
                    }
                    return;
                }
                if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                    siralamaPaneli.handleBack();
                    return;
                }
                View focused = rv.getFocusedChild();
                View hedef = (focused != null) ? focused : rv;
                hedef.dispatchKeyEvent(new android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN, keyCode));
                hedef.dispatchKeyEvent(new android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_UP, keyCode));
                return;
            }
        }

        
        dpad.handleKeyDownDokunmatik(keyCode);
    }
}