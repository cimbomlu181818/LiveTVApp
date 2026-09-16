package com.example.livetvapp.tvbox;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
public class Dokunmatik {
    private static final int   UZUN_BASMA_SURE  = 500;   
    private static final int   TIKLA_BEKLE_SURE = 120;
    private static final float SWIPE_ESIK_DP    = 40f;   
    private static final float SWIPE_ORAN       = 1.5f;
    private final AppCompatActivity activity;
    private final Dpad              dpad;
    private final Handler           handler = new Handler(Looper.getMainLooper());
    private SanalDpad sanalDpad;
    private boolean aktif        = true;
    private boolean hareketVar   = false;
    private boolean islemYapildi = false;
    private float   baslangicX, baslangicY;
    private float   swipeEsikPx; 

    private final Runnable tiklamaRunnable = () -> {
        if (!hareketVar && !islemYapildi) {
            islemYapildi = true;
            handleTekTik();
        }
    };
    private final Runnable uzunBasmaRunnable = () -> {
        if (!hareketVar && !islemYapildi) {
            islemYapildi = true;
            handleUzunBasma();
        }
    };
    public Dokunmatik(AppCompatActivity activity, Dpad dpad) {
        this.activity   = activity;
        this.dpad       = dpad;
        this.swipeEsikPx = dp2px(SWIPE_ESIK_DP);
    }
    public void aktifYap() {
        this.aktif = true;
        System.out.println("🖐️ Dokunmatik AKTİF");
    }
    public void setSanalDpad(SanalDpad sd) { this.sanalDpad = sd; }
    public void pasifYap() {
        this.aktif = false;
        System.out.println("🖐️ Dokunmatik PASİF");
    }
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (sanalDpad != null && sanalDpad.isVisible()) return false;
        if (!aktif) return false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!aktif) return false;
                baslangicX   = event.getX();
                baslangicY   = event.getY();
                hareketVar   = false;
                islemYapildi = false;
                handler.postDelayed(tiklamaRunnable,   TIKLA_BEKLE_SURE);
                handler.postDelayed(uzunBasmaRunnable, UZUN_BASMA_SURE);
                return false;
            case MotionEvent.ACTION_MOVE:
                if (!aktif) return false;
                float dx = event.getX() - baslangicX;
                float dy = event.getY() - baslangicY;
                if (!hareketVar && (Math.abs(dx) > swipeEsikPx || Math.abs(dy) > swipeEsikPx)) {
                    hareketVar = true;
                    handler.removeCallbacks(tiklamaRunnable);
                    handler.removeCallbacks(uzunBasmaRunnable);
                }
                return false;
            case MotionEvent.ACTION_UP:
                handler.removeCallbacks(tiklamaRunnable);
                handler.removeCallbacks(uzunBasmaRunnable);
                if (hareketVar && !islemYapildi) {
                    islemYapildi = true;
                    float dx2 = event.getX() - baslangicX;
                    float dy2 = event.getY() - baslangicY;
                    handleSwipe(dx2, dy2);
                    return true;
                }
                return false;
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(tiklamaRunnable);
                handler.removeCallbacks(uzunBasmaRunnable);
                return false;
        }
        return false;
    }
    private void handleTekTik() {
        Dpad.AktifPanel panel = aktifPanel();
        System.out.println("👆 Tek tık | panel=" + panel);
        if (panel == Dpad.AktifPanel.ARAMA) return;
        if (panel == Dpad.AktifPanel.AYARLAR) {
            View focused = activity.getCurrentFocus();
            if (focused instanceof Button) {
                focused.performClick();
            }
            return;
        }
        if (dpad != null && dpad.getOynaticiBar() != null && dpad.getOynaticiBar().isVisible()) {
            View focused = activity.getCurrentFocus();
            if (focused instanceof Button) {
                focused.performClick();
                dpad.getOynaticiBar().resetControlsTimeout();
            } else {
                dpad.getOynaticiBar().gizle();
            }
            return;
        }
        if (panel == Dpad.AktifPanel.HICBIRI) {
            if (dpad != null && dpad.getOynaticiBar() != null) {
                dpad.getOynaticiBar().goster();
            }
        } else {
            gonder(KeyEvent.KEYCODE_DPAD_CENTER);
        }
    }
    private void handleUzunBasma() {
        System.out.println("🖐️ Uzun basma → MENU/AYARLAR");
        Dpad.AktifPanel panel = aktifPanel();
        if (panel == Dpad.AktifPanel.AYARLAR) return;
        if (dpad != null && dpad.getOynaticiBar() != null && dpad.getOynaticiBar().isVisible()) {
            dpad.getOynaticiBar().gizle();
        }
        gonder(KeyEvent.KEYCODE_MENU);
    }
    private void handleSwipe(float dx, float dy) {
        boolean yatayBaskın = Math.abs(dx) > Math.abs(dy) * SWIPE_ORAN;
        boolean dikeybaskın = Math.abs(dy) > Math.abs(dx) * SWIPE_ORAN;
        if (yatayBaskın) {
            boolean sagaSwipe = dx > 0;
            handleYataySwipe(sagaSwipe);
        } else if (dikeybaskın) {
            boolean asagiSwipe = dy > 0;
            handleDikeySwipe(asagiSwipe);
        }
    }
    private void handleYataySwipe(boolean saga) {
        Dpad.AktifPanel panel = aktifPanel();
        System.out.println("↔️ Yatay swipe | " + (saga ? "SAĞA" : "SOLA") + " | panel=" + panel);
        if (dpad != null && dpad.getOynaticiBar() != null && dpad.getOynaticiBar().isVisible()) {
            if (dpad.getOynaticiBar().isSeekBarFocused()) {
                gonder(saga ? KeyEvent.KEYCODE_DPAD_RIGHT : KeyEvent.KEYCODE_DPAD_LEFT);
                return;
            }
            dpad.getOynaticiBar().gizle();
            return;
        }
        if (panel == Dpad.AktifPanel.ARAMA) return;
        if (panel == Dpad.AktifPanel.AYARLAR) {
            handleAyarlarYataySwipe(saga);
            return;
        }
        if (isGizlePanelAcik()) {
            handleGizlePaneliSwipe(saga);
            return;
        }
        if (saga) {
            gonderSagSol(KeyEvent.KEYCODE_DPAD_LEFT);
        } else {
            gonderSagSol(KeyEvent.KEYCODE_DPAD_RIGHT);
        }
    }
    private void handleDikeySwipe(boolean asagi) {
        Dpad.AktifPanel panel = aktifPanel();
        System.out.println("↕️ Dikey swipe | " + (asagi ? "AŞAĞI" : "YUKARI") + " | panel=" + panel);
        if (dpad != null && dpad.getOynaticiBar() != null && dpad.getOynaticiBar().isVisible()) {
            dpad.getOynaticiBar().gizle();
            return;
        }
        if (panel == Dpad.AktifPanel.ARAMA || panel == Dpad.AktifPanel.AYARLAR) {
            return;
        }
        if (isGizlePanelAcik()) {
            if (asagi) {
                dpad.handleKeyDownDokunmatik(KeyEvent.KEYCODE_DPAD_DOWN);
            } else {
                dpad.handleKeyDownDokunmatik(KeyEvent.KEYCODE_DPAD_UP);
            }
            return;
        }
        if (panel == Dpad.AktifPanel.HICBIRI) {
            if (asagi) {
                gonder(KeyEvent.KEYCODE_DPAD_DOWN); 
            } else {
                gonder(KeyEvent.KEYCODE_DPAD_UP);   
            }
            return;
        }
        View focused = activity.getCurrentFocus();
        if (focused != null) {
            int keyCode = asagi ? KeyEvent.KEYCODE_DPAD_DOWN : KeyEvent.KEYCODE_DPAD_UP;
            focused.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
            focused.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,   keyCode));
        }
    }
    private void handleAyarlarYataySwipe(boolean saga) {
        View focused = activity.getCurrentFocus();
        if (focused != null) {
            android.view.ViewParent parent = focused.getParent();
            while (parent != null) {
                if (parent instanceof RecyclerView) return;
                if (parent instanceof View) parent = ((View) parent).getParent();
                else break;
            }
        }

        gonder(KeyEvent.KEYCODE_BACK);
    }
    private void handleGizlePaneliSwipe(boolean saga) {
        if (dpad != null) {
            dpad.handleKeyDownDokunmatik(
                    saga ? KeyEvent.KEYCODE_DPAD_RIGHT : KeyEvent.KEYCODE_DPAD_LEFT);
        }
    }
    private Dpad.AktifPanel aktifPanel() {
        if (dpad == null) return Dpad.AktifPanel.HICBIRI;
        return dpad.getAktifPanel();
    }
    private boolean isGizlePanelAcik() {
        if (dpad == null) return false;
        Dpad.AktifPanel panel = dpad.getAktifPanel();
        if (panel != Dpad.AktifPanel.AYARLAR) return false;
        return false; 
    }

    private void gonderSagSol(int keyCode) {
        if (dpad == null) return;
        if (aktifPanel() == Dpad.AktifPanel.AYARLAR) {
            View focused = activity.getCurrentFocus();
            if (focused != null) {
                focused.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
                focused.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,   keyCode));
                return;
            }
        }
        gonder(keyCode);
    }
    private void gonder(int keyCode) {
        if (dpad == null) return;
        dpad.handleKeyDownDokunmatik(keyCode);
    }
    public void temizle() {
        handler.removeCallbacks(tiklamaRunnable);
        handler.removeCallbacks(uzunBasmaRunnable);
    }
    private float dp2px(float dp) {
        return dp * activity.getResources().getDisplayMetrics().density;
    }
}