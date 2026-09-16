package com.example.livetvapp.paneller;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.livetvapp.R;
import com.example.livetvapp.other.AspectRatioManager;
import com.example.livetvapp.other.PiPHelper;
import com.example.livetvapp.other.StreamMonitor;
import com.example.livetvapp.tvbox.Dpad;
import org.videolan.libvlc.MediaPlayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
public class bar {
    private static final long CONTROLS_TIMEOUT  = 5000;
    private static final int  SEEK_STEP_MS      = 5000;
    private static final long SEEK_COMMIT_DELAY = 1000;
    public static final String TIP_CANLI_TV = "LIVE";
    private AppCompatActivity activity;
    private MediaPlayer mediaPlayer;
    private LinearLayout playerControls;
    private Dpad dpad;
    private Runnable aramaAcilisListener;
    public void setAramaAcilisListener(Runnable r) { this.aramaAcilisListener = r; }
    private String aktifIcerikTipi = TIP_CANLI_TV;
    private AspectRatioManager aspectRatioManager;
    private PiPHelper          pipHelper;
    private TextView tvIcerikAdi;
    private Button btnKanallar;
    private Button btnRewindStep;
    private Button btnRewind;
    private Button btnPlayPause;
    private Button btnForward;
    private Button btnForwardStep;
    private Button btnMute;
    private Button btnFavori;
    private Button btnSearch;
    private Button btnSettings;
    private Button btnPiP;
    private Button btnFullscreen;
    private Button btnCloseBar;
    private boolean isMuted = false;
    private boolean remoteAktif = false;
    private SeekBar  seekBar;
    private TextView tvElapsed;
    private TextView tvRemaining;
    private TextView tvDuration;
    private LinearLayout seekBarRow;
    private List<Button> buttons = new ArrayList<>();
    private StreamMonitor streamMonitor;
    private int rewindStep  = 10;
    private int forwardStep = 10;
    private long    seekPreviewMs   = 0;
    private boolean seekIng         = false;
    private boolean seekBarHasFocus = false;
    private long liveDvrDurationMs = 0;
    private final android.os.Handler handler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable hideControlsRunnable = this::gizle;
    private final Runnable seekCommitRunnable = () -> {
        commitSeek();
    };
    private final Runnable seekBarUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (isVisible() && !seekIng) {
                updateSeekBarFromPlayer();
            }
            if (isVisible()) {
                handler.postDelayed(this, 500);
            }
        }
    };
    public bar(AppCompatActivity activity, MediaPlayer mediaPlayer) {
        this.activity    = activity;
        this.mediaPlayer = mediaPlayer;
        loadStepPreferences();
        this.pipHelper        = new PiPHelper(activity);
    }
    public void setAspectRatioManager(AspectRatioManager manager) {
        this.aspectRatioManager = manager;
    }
    public void setAktifIcerikTipi(String tip) {
        this.aktifIcerikTipi = tip != null ? tip : TIP_CANLI_TV;
        updateSeekBarVisibility();
    }
    public void setStreamMonitor(StreamMonitor sm) {
        this.streamMonitor = sm;
    }
    public void setIcerikAdi(String ad) {
        if (tvIcerikAdi == null) return;
        boolean goster = ad != null && !ad.isEmpty()
                && !TIP_CANLI_TV.equals(aktifIcerikTipi);
        tvIcerikAdi.setVisibility(goster ? View.VISIBLE : View.GONE);
        if (goster) tvIcerikAdi.setText(ad);
    }
    public void favoriDurumunuGuncelle(boolean favoriMi) {
        if (btnFavori == null) return;
        btnFavori.setText(favoriMi ? "⭐" : "☆");
    }
    public void olustur() {
        playerControls = activity.findViewById(R.id.playerControls);
        if (playerControls == null) {
            System.out.println("❌ HATA: playerControls NULL!");
            return;
        }
        tvIcerikAdi = activity.findViewById(R.id.tvIcerikAdi);
        seekBar     = activity.findViewById(R.id.seekBar);
        tvElapsed   = activity.findViewById(R.id.tvElapsed);
        tvRemaining = activity.findViewById(R.id.tvRemaining);
        tvDuration  = activity.findViewById(R.id.tvDuration);
        seekBarRow  = activity.findViewById(R.id.seekBarRow);
        btnKanallar    = activity.findViewById(R.id.btnKanallar);
        btnRewindStep  = activity.findViewById(R.id.btnRewindStep);
        btnRewind      = activity.findViewById(R.id.btnRewind);
        btnPlayPause   = activity.findViewById(R.id.btnPlayPause);
        btnForward     = activity.findViewById(R.id.btnForward);
        btnForwardStep = activity.findViewById(R.id.btnForwardStep);
        btnMute        = activity.findViewById(R.id.btnMute);
        btnFavori      = activity.findViewById(R.id.btnFavori);
        btnSearch      = activity.findViewById(R.id.btnSearch);
        btnSettings    = activity.findViewById(R.id.btnSettings);
        btnPiP         = activity.findViewById(R.id.btnPiP);
        btnFullscreen  = activity.findViewById(R.id.btnFullscreen);
        btnCloseBar    = activity.findViewById(R.id.btnCloseBar);
        buttons.clear();
        if (btnKanallar    != null) buttons.add(0, btnKanallar);
        if (btnRewindStep  != null) buttons.add(btnRewindStep);
        if (btnRewind      != null) buttons.add(btnRewind);
        if (btnPlayPause   != null) buttons.add(btnPlayPause);
        if (btnForward     != null) buttons.add(btnForward);
        if (btnForwardStep != null) buttons.add(btnForwardStep);
        if (btnMute        != null) buttons.add(btnMute);
        if (btnFavori      != null) buttons.add(btnFavori);
        if (btnSettings    != null) buttons.add(btnSettings);
        if (btnSearch      != null) buttons.add(btnSearch);
        if (btnPiP         != null) buttons.add(btnPiP);
        if (btnFullscreen  != null) buttons.add(btnFullscreen);
        if (btnCloseBar    != null) buttons.add(btnCloseBar);
        setupSeekBar();
        setupButtonListeners();
        setupFocusListeners();
        playerControls.setVisibility(View.GONE);
        if (seekBar != null) seekBar.setVisibility(View.GONE);
        updateStepButtonsText();
        updateSeekBarVisibility();
        System.out.println("✅ Bar oluşturuldu - " + buttons.size() + " buton");
    }
    private void setupSeekBar() {
        if (seekBar == null) return;
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) {
                    long duration = getEffectiveDuration();
                    if (duration <= 0) return;
                    seekPreviewMs = (long) (progress / 1000.0 * duration);
                    seekIng       = true;
                    updateSeekUI(seekPreviewMs, duration);
                    scheduleSeekCommit();
                    resetControlsTimeout();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { seekIng = true; }
            @Override public void onStopTrackingTouch(SeekBar sb)  { scheduleSeekCommit(); }
        });
        seekBar.setOnFocusChangeListener((v, hasFocus) -> {
            seekBarHasFocus = hasFocus;
            if (hasFocus) {
                seekBar.setBackgroundColor(android.graphics.Color.argb(50, 220, 50, 50));
                seekPreviewMs = getCurrentPositionMs();
                resetControlsTimeout();
                System.out.println("🎯 SeekBar focus aldı");
                
                if (dpad != null) dpad.guncelleKisayol();
            } else {
                seekBar.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                if (seekIng) {
                    seekIng = false;
                    handler.removeCallbacks(seekCommitRunnable);
                    commitSeek();
                }
                
                if (dpad != null) dpad.guncelleKisayol();
            }
        });
        seekBar.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != android.view.KeyEvent.ACTION_DOWN) return false;
            return handleSeekBarKey(keyCode);
        });
    }
    public boolean handleSeekBarKey(int keyCode) {
        if (!seekBarHasFocus || seekBar == null) return false;
        
        if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP
                || keyCode == android.view.KeyEvent.KEYCODE_BACK
                || keyCode == android.view.KeyEvent.KEYCODE_ESCAPE) {
            seekBar.clearFocus();
            gizle();
            return true;
        }
        long duration = getEffectiveDuration();
        if (duration <= 0) return false;
        switch (keyCode) {
            case android.view.KeyEvent.KEYCODE_DPAD_LEFT:
                seekPreviewMs = Math.max(0, seekPreviewMs - SEEK_STEP_MS);
                seekIng       = true;
                updateSeekUI(seekPreviewMs, duration);
                scheduleSeekCommit();
                resetControlsTimeout();
                return true;
            case android.view.KeyEvent.KEYCODE_DPAD_RIGHT:
                seekPreviewMs = Math.min(duration, seekPreviewMs + SEEK_STEP_MS);
                seekIng       = true;
                updateSeekUI(seekPreviewMs, duration);
                scheduleSeekCommit();
                resetControlsTimeout();
                return true;
            case android.view.KeyEvent.KEYCODE_DPAD_DOWN:
                seekBar.clearFocus();
                
                if (btnPlayPause != null && btnPlayPause.getVisibility() == View.VISIBLE) {
                    btnPlayPause.postDelayed(() -> btnPlayPause.requestFocus(), 50);
                } else if (!buttons.isEmpty()) {
                    buttons.get(0).postDelayed(() -> buttons.get(0).requestFocus(), 50);
                }
                return true;
            case android.view.KeyEvent.KEYCODE_DPAD_CENTER:
            case android.view.KeyEvent.KEYCODE_ENTER:
                handler.removeCallbacks(seekCommitRunnable);
                commitSeek();
                return true;
            default:
                return false;
        }
    }
    public boolean isSeekBarFocused() {
        return seekBarHasFocus;
    }
    private void scheduleSeekCommit() {
        handler.removeCallbacks(seekCommitRunnable);
        handler.postDelayed(seekCommitRunnable, SEEK_COMMIT_DELAY);
    }
    private void commitSeek() {
        if (mediaPlayer == null) return;
        seekIng = false;
        long duration = getEffectiveDuration();
        if (duration <= 0) return;
        long target = Math.max(0, Math.min(seekPreviewMs, duration));
        if (TIP_CANLI_TV.equals(aktifIcerikTipi) && liveDvrDurationMs > 0) {
            mediaPlayer.setTime(target);
            System.out.println("📡 [SeekBar] Canlı TV DVR seek: " + target + "ms");
        } else {
            mediaPlayer.setTime(target);
            System.out.println("▶️ [SeekBar] Seek commit: " + target + "ms");
        }
    }
    private void updateSeekBarFromPlayer() {
        if (mediaPlayer == null || seekBar == null) return;
        long duration = getEffectiveDuration();
        long current  = getCurrentPositionMs();
        if (duration > 0) {
            seekPreviewMs = current;
            updateSeekUI(current, duration);
        }
    }
    private void updateSeekUI(long positionMs, long durationMs) {
        if (seekBar == null) return;
        int progress = durationMs > 0
                ? (int) (positionMs * 1000L / durationMs)
                : 0;
        seekBar.setProgress(progress);
        if (tvElapsed   != null) tvElapsed.setText(formatMs(positionMs));
        if (tvDuration  != null) tvDuration.setText(formatMs(durationMs));
        if (tvRemaining != null) {
            long remaining = Math.max(0, durationMs - positionMs);
            tvRemaining.setText("-" + formatMs(remaining));
        }
    }
    private void updateSeekBarVisibility() {
        if (seekBarRow == null) return;
        seekBarRow.setVisibility(View.VISIBLE);
        System.out.println("📊 SeekBar görünürlük: VISIBLE [tip=" + aktifIcerikTipi + "]");
    }
    private long getEffectiveDuration() {
        if (mediaPlayer == null) return 0;
        return Math.max(0, mediaPlayer.getLength());
    }
    public long getCurrentPositionMs() {
        if (mediaPlayer == null) return 0;
        return Math.max(0, mediaPlayer.getTime());
    }
    private String formatMs(long ms) {
        if (ms < 0) ms = 0;
        long totalSec = ms / 1000;
        long hours    = totalSec / 3600;
        long minutes  = (totalSec % 3600) / 60;
        long seconds  = totalSec % 60;
        if (hours > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }
    private void setupButtonListeners() {
        if (btnKanallar != null) {
            btnKanallar.setOnClickListener(v -> {
                gizle();
                if (dpad != null) {
                    dpad.handleKeyDown(android.view.KeyEvent.KEYCODE_ENTER,
                            new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,
                                    android.view.KeyEvent.KEYCODE_ENTER));
                    dpad.handleKeyDownDokunmatik(android.view.KeyEvent.KEYCODE_ENTER);
                }
            });
        }
        if (btnRewindStep != null) {
            btnRewindStep.setOnClickListener(v -> {
                showRewindStepMenu();
                resetControlsTimeout();
            });
        }
        if (btnRewind != null) {
            btnRewind.setOnClickListener(v -> {
                geriSar();
                resetControlsTimeout();
            });
        }
        if (btnPlayPause != null) {
            btnPlayPause.setOnClickListener(v -> {
                oynatDuraklat();
                resetControlsTimeout();
            });
        }
        if (btnForward != null) {
            btnForward.setOnClickListener(v -> {
                ileriSar();
                resetControlsTimeout();
            });
        }
        if (btnForwardStep != null) {
            btnForwardStep.setOnClickListener(v -> {
                showForwardStepMenu();
                resetControlsTimeout();
            });
        }
        if (btnMute != null) {
            btnMute.setOnClickListener(v -> {
                isMuted = !isMuted;
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(isMuted ? 0 : 100);
                }
                btnMute.setText(isMuted ? "🔇" : "🔊");
                resetControlsTimeout();
            });
        }
        if (btnFavori != null) {
            btnFavori.setOnClickListener(v -> {
                if (dpad != null && dpad.getAnakontrol() != null) {
                    dpad.getAnakontrol().favoriToggleAktifIcerik();
                }
                resetControlsTimeout();
            });
        }
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                gizle();
                if (dpad != null) dpad.ayarlarPaneliAc();
            });
        }
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> {
                gizle();
                if (aramaAcilisListener != null) aramaAcilisListener.run();
            });
        }
        if (btnPiP != null) {
            btnPiP.setOnClickListener(v -> {
                girisYapPiP();
                resetControlsTimeout();
            });
        }
        if (btnFullscreen != null) {
            btnFullscreen.setOnClickListener(v -> {
                toggleAspectRatio();  
                resetControlsTimeout();
            });
            
        }
        if (btnCloseBar != null) {
            btnCloseBar.setOnClickListener(v -> gizle());
        }
    }
    private void setupFocusListeners() {
        for (int i = 0; i < buttons.size(); i++) {
            final int idx = i;
            final Button btn = buttons.get(i);
            btn.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.setBackgroundColor(android.graphics.Color.RED);
                    resetControlsTimeout();
                } else {
                    restoreButtonBackground((Button) v);
                }
            });
            btn.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() != android.view.KeyEvent.ACTION_DOWN) return false;
                switch (keyCode) {
                    case android.view.KeyEvent.KEYCODE_DPAD_RIGHT: {
                        int next = (idx + 1) % buttons.size();
                        buttons.get(next).requestFocus();
                        return true;
                    }
                    case android.view.KeyEvent.KEYCODE_DPAD_LEFT: {
                        int prev = (idx - 1 + buttons.size()) % buttons.size();
                        buttons.get(prev).requestFocus();
                        return true;
                    }
                    case android.view.KeyEvent.KEYCODE_DPAD_UP:
                        handleBarUpKey();
                        return true;
                    case android.view.KeyEvent.KEYCODE_BACK:
                    case android.view.KeyEvent.KEYCODE_ESCAPE:
                        gizle();
                        return true;
                    case android.view.KeyEvent.KEYCODE_DPAD_DOWN:
                        gizle();
                        return true;
                    case android.view.KeyEvent.KEYCODE_DPAD_CENTER:
                    case android.view.KeyEvent.KEYCODE_ENTER:
                        v.performClick();
                        return true;
                    default:
                        return false;
                }
            });
        }
    }
    private void restoreButtonBackground(Button v) {
        if (v == btnPlayPause) {
            v.setBackgroundResource(R.drawable.play_pause_button_background);
        } else if (v == btnRewind || v == btnForward) {
            v.setBackgroundResource(R.drawable.control_button_background_primary);
        } else if (v == btnRewindStep || v == btnForwardStep) {
            v.setBackgroundResource(R.drawable.control_button_background);
        } else {
            v.setBackgroundResource(R.drawable.secondary_button_background);
        }
    }
    public void geriSar() {
        if (mediaPlayer != null) {
            long current     = mediaPlayer.getTime();
            long newPosition = Math.max(0, current - (rewindStep * 1000L));
            mediaPlayer.setTime(newPosition);
            seekPreviewMs = newPosition;
            updateSeekBarFromPlayer();
            Toast.makeText(activity, "-" + rewindStep + "s", Toast.LENGTH_SHORT).show();
        }
    }
    public void ileriSar() {
        if (mediaPlayer != null) {
            long current     = mediaPlayer.getTime();
            long duration    = getEffectiveDuration();
            long newPosition = duration > 0
                    ? Math.min(duration, current + (forwardStep * 1000L))
                    : current + (forwardStep * 1000L);
            mediaPlayer.setTime(newPosition);
            seekPreviewMs = newPosition;
            updateSeekBarFromPlayer();
            Toast.makeText(activity, "+" + forwardStep + "s", Toast.LENGTH_SHORT).show();
        }
    }
    public void oynatDuraklat() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                if (btnPlayPause != null) btnPlayPause.setText("▶");
                Toast.makeText(activity, "Duraklatıldı", Toast.LENGTH_SHORT).show();
                if (streamMonitor != null) streamMonitor.onUserPaused();
                if (dpad != null && dpad.getAnakontrol() != null) dpad.getAnakontrol().setKullaniciDurdurdu(true);
            } else {
                mediaPlayer.play();
                if (btnPlayPause != null) btnPlayPause.setText("⏸");
                Toast.makeText(activity, "Oynatılıyor", Toast.LENGTH_SHORT).show();
                if (streamMonitor != null) streamMonitor.onUserResumed();
                if (dpad != null && dpad.getAnakontrol() != null) dpad.getAnakontrol().setKullaniciDurdurdu(false);
            }
        }
    }
    public void updatePlayPauseButton(boolean isPlaying) {
        if (btnPlayPause != null) {
            btnPlayPause.setText(isPlaying ? "⏸" : "▶");
        }
    }
    private void girisYapPiP() {
        if (pipHelper == null) return;
        if (!pipHelper.isPiPSupported()) {
            Toast.makeText(activity, "PiP bu cihazda desteklenmiyor", Toast.LENGTH_SHORT).show();
            return;
        }
        gizle();
        boolean basarili = pipHelper.enterPictureInPictureMode();
        if (!basarili) Toast.makeText(activity, "PiP başlatılamadı", Toast.LENGTH_SHORT).show();
    }
    private void toggleAspectRatio() {
        if (aspectRatioManager == null) {
            Toast.makeText(activity, "Görüntü oranı ayarlanamıyor", Toast.LENGTH_SHORT).show();
            return;
        }
        aspectRatioManager.toggleStretchMode();
    }


    private void showRewindStepMenu() {
        final String[] options = {"10s", "1dk", "10dk"};
        final int[]    values  = {10, 60, 600};
        int selectedIndex = 0;
        for (int i = 0; i < values.length; i++) {
            if (rewindStep == values[i]) { selectedIndex = i; break; }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Geri Sarma Süresi");
        builder.setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
            rewindStep = values[which];
            saveStepPreferences();
            updateStepButtonsText();
            Toast.makeText(activity, "Geri sarma: " + options[which], Toast.LENGTH_SHORT).show();
            resetControlsTimeout();
            dialog.dismiss();
        });
        builder.setNegativeButton("İptal", (dialog, which) -> resetControlsTimeout());
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(d -> resetControlsTimeout());
        dialog.show();
    }
    private void showForwardStepMenu() {
        final String[] options = {"10s", "1dk", "10dk"};
        final int[]    values  = {10, 60, 600};
        int selectedIndex = 0;
        for (int i = 0; i < values.length; i++) {
            if (forwardStep == values[i]) { selectedIndex = i; break; }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("İleri Sarma Süresi");
        builder.setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
            forwardStep = values[which];
            saveStepPreferences();
            updateStepButtonsText();
            Toast.makeText(activity, "İleri sarma: " + options[which], Toast.LENGTH_SHORT).show();
            resetControlsTimeout();
            dialog.dismiss();
        });
        builder.setNegativeButton("İptal", (dialog, which) -> resetControlsTimeout());
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(d -> resetControlsTimeout());
        dialog.show();
    }
    private void updateStepButtonsText() {
        if (btnRewindStep  != null) btnRewindStep.setText(formatStepText(rewindStep));
        if (btnForwardStep != null) btnForwardStep.setText(formatStepText(forwardStep));
    }
    private String formatStepText(int seconds) {
        if (seconds < 60) return seconds + "s";
        if (seconds % 60 == 0) return (seconds / 60) + "dk";
        return seconds + "s";
    }
    private void loadStepPreferences() {
        android.content.SharedPreferences prefs =
                activity.getSharedPreferences("LiveTVAppPrefs", 0);
        rewindStep  = prefs.getInt("rewindStep",  10);
        forwardStep = prefs.getInt("forwardStep", 10);
    }

    private void saveStepPreferences() {
        activity.getSharedPreferences("LiveTVAppPrefs", 0).edit()
                .putInt("rewindStep",  rewindStep)
                .putInt("forwardStep", forwardStep)
                .apply();
    }
    public View getPanelView() {
        return playerControls;
    }
    public void goster() {
        if (playerControls == null) return;
        playerControls.setVisibility(View.VISIBLE);
        if (btnPlayPause != null && mediaPlayer != null) {
            btnPlayPause.setText(mediaPlayer.isPlaying() ? "⏸" : "▶");
        }
        if (seekBarRow != null) seekBarRow.setVisibility(View.VISIBLE);
        if (seekBar != null) seekBar.setVisibility(View.VISIBLE);
        seekPreviewMs = getCurrentPositionMs();
        updateSeekBarFromPlayer();
        handler.removeCallbacks(seekBarUpdateRunnable);
        handler.post(seekBarUpdateRunnable);
        resetControlsTimeout();
        
        if (btnSearch   != null) btnSearch.setVisibility(remoteAktif   ? View.GONE : View.VISIBLE);
        if (btnSettings != null) btnSettings.setVisibility(remoteAktif ? View.GONE : View.VISIBLE);

        
        playerControls.postDelayed(this::focusIlkButon, 100);
        System.out.println("✅ Bar gösterildi");
        if (dpad != null) dpad.guncelleKisayol();
        if (dpad != null && dpad.getAnakontrol() != null) {
            playerControls.post(() -> {
                dpad.getAnakontrol().kutuMargininiAktifPaneleGoreAyarla();
            });
        }
    }
    public void gizle() {
        if (playerControls == null) return;
        playerControls.setVisibility(View.GONE);
        if (seekBar    != null) seekBar.setVisibility(View.GONE);
        if (seekBarRow != null) seekBarRow.setVisibility(View.GONE);
        handler.removeCallbacks(hideControlsRunnable);
        handler.removeCallbacks(seekBarUpdateRunnable);
        handler.removeCallbacks(seekCommitRunnable);
        if (seekIng) {
            seekIng = false;
            commitSeek();
        }
        seekBarHasFocus = false;
        System.out.println("Bar gizlendi");
        if (dpad != null) dpad.guncelleKisayol();

        
        if (dpad != null && dpad.getAnakontrol() != null) {
            dpad.getAnakontrol().kutuMargininiAktifPaneleGoreAyarla();
        }
    }
    public void resetControlsTimeout() {
        handler.removeCallbacks(hideControlsRunnable);
        handler.postDelayed(hideControlsRunnable, CONTROLS_TIMEOUT);
    }
    public void handleBarUpKey() {
        if (seekBarRow != null && seekBarRow.getVisibility() == View.VISIBLE) {
            seekPreviewMs = getCurrentPositionMs();
            if (seekBar != null) {
                seekBar.postDelayed(() -> seekBar.requestFocus(), 50);
            }
            System.out.println("⬆️ Bar: butonlardan seek bar'a geçildi");
        } else {
            gizle();
            System.out.println("⬆️ Bar: seek bar yok, bar kapatıldı");
        }
    }
    public void focusIlkButon() {
        if (btnPlayPause != null) {
            btnPlayPause.postDelayed(() -> btnPlayPause.requestFocus(), 50);
        } else if (!buttons.isEmpty()) {
            buttons.get(0).postDelayed(() -> buttons.get(0).requestFocus(), 50);
        }
    }
    public void onPictureInPictureModeChanged(boolean isInPiPMode) {
        if (pipHelper != null) pipHelper.onPictureInPictureModeChanged(isInPiPMode, null);
        if (isInPiPMode) gizle();
    }
    public boolean isVisible() {
        return playerControls != null
                && playerControls.getVisibility() == View.VISIBLE;
    }
    public void setRemoteAktif(boolean aktif) {
        this.remoteAktif = aktif;
    }

    public void setDpad(Dpad dpad) {
        this.dpad = dpad;
    }
    public void toggleMute() {
        isMuted = !isMuted;
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(isMuted ? 0 : 100);
        }
        if (btnMute != null) btnMute.setText(isMuted ? "🔇" : "🔊");
        resetControlsTimeout();
    }
    public void applyMuteState() {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(isMuted ? 0 : 100);
        }
        if (btnMute != null) {
            btnMute.setText(isMuted ? "🔇" : "🔊");
        }
    }
    public void temizle() {
        handler.removeCallbacks(hideControlsRunnable);
        handler.removeCallbacks(seekBarUpdateRunnable);
        handler.removeCallbacks(seekCommitRunnable);
        System.out.println("Bar temizlendi");
    }
}