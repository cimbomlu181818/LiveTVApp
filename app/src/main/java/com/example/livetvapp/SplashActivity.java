package com.example.livetvapp;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.json.JSONObject;

public class SplashActivity extends AppCompatActivity {

    private static final long TIMEOUT_MS = 15_000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean resolved = false;

    private View progressFill;
    private View progressContainer;
    private TextView tvStatus;
    private View logoBox;

    private final String[] statusMessages = {
            "Kimlik doğrulanıyor...",
            "Firebase bağlantısı...",
            "Erişim kontrol ediliyor...",
            "Lisans doğrulanıyor..."
    };
    private int statusIdx = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat ctrl =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        ctrl.hide(WindowInsetsCompat.Type.systemBars());
        ctrl.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        setContentView(R.layout.activity_splash);

        progressFill      = findViewById(R.id.progressFill);
        progressContainer = findViewById(R.id.progressContainer);
        tvStatus          = findViewById(R.id.tvStatus);
        logoBox           = findViewById(R.id.logoBox);
        View outerRing    = findViewById(R.id.outerRing);
        View innerRing    = findViewById(R.id.innerRing);
        View sweepView    = findViewById(R.id.sweepView);
        TextView tvName   = findViewById(R.id.tvAppName);

        
        ObjectAnimator outerAnim = ObjectAnimator.ofFloat(outerRing, "rotation", 0f, 360f);
        outerAnim.setDuration(3000);
        outerAnim.setRepeatCount(ValueAnimator.INFINITE);
        outerAnim.setInterpolator(new LinearInterpolator());
        outerAnim.start();

        
        ObjectAnimator innerAnim = ObjectAnimator.ofFloat(innerRing, "rotation", 0f, -360f);
        innerAnim.setDuration(5000);
        innerAnim.setRepeatCount(ValueAnimator.INFINITE);
        innerAnim.setInterpolator(new LinearInterpolator());
        innerAnim.start();

        
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(logoBox, "scaleX", 0.96f, 1.04f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(logoBox, "scaleY", 0.96f, 1.04f);
        scaleX.setDuration(2800);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setRepeatMode(ValueAnimator.REVERSE);
        scaleX.start();

        scaleY.setDuration(2800);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatMode(ValueAnimator.REVERSE);
        scaleY.start();

        
        ObjectAnimator sweep = ObjectAnimator.ofFloat(sweepView, "translationX",
                -dpToPx(60), dpToPx(120));
        sweep.setDuration(2500);
        sweep.setRepeatCount(ValueAnimator.INFINITE);
        sweep.setInterpolator(new LinearInterpolator());
        sweep.start();

        
        tvName.animate().alpha(1f).translationY(0f).setStartDelay(500).setDuration(700).start();
        tvName.setTranslationY(dpToPx(12));

        
        handler.postDelayed(() -> {
            tvStatus.animate().alpha(1f).setDuration(500).start();
            progressContainer.animate().alpha(1f).setDuration(500).start();
        }, 1200);

        
        handler.postDelayed(statusDongusu, 2200);

        
        ValueAnimator progressAnim = ValueAnimator.ofInt(0, (int)(dpToPx(160) * 0.92f));
        progressAnim.setDuration(TIMEOUT_MS);
        progressAnim.setInterpolator(new LinearInterpolator());
        progressAnim.addUpdateListener(a -> {
            if (!resolved) {
                ViewGroup.LayoutParams lp = progressFill.getLayoutParams();
                lp.width = (int) a.getAnimatedValue();
                progressFill.setLayoutParams(lp);
            }
        });
        progressAnim.start();
        
        SharedPreferences tercihler = getSharedPreferences("azsh_giris", Context.MODE_PRIVATE);
        String kayitliEmail = tercihler.getString("email", null);

        if (kayitliEmail == null) {
            
            handler.postDelayed(() -> gitLogin(null), 1500);
            return;
        }

        ApiHelper apiHelper = new ApiHelper();
        String cihazId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        apiHelper.erisimKontrol(kayitliEmail, cihazId, new ApiHelper.ApiListener() {
            @Override
            public void onBasarili(JSONObject sonuc) {
                boolean bakimModu = sonuc.optBoolean("bakim_modu", false);
                if (bakimModu) {
                    gitLoginHata(sonuc.optString("mesaj", "Uygulama bakımda."));
                    return;
                }
                String durum = sonuc.optString("durum", "");
                boolean erisim = sonuc.optBoolean("erisim", false);

                if (erisim) {
                    gitMain();
                } else if ("mail_dogrulanmadi".equals(durum)) {
                    gitLogin(null);
                } else {
                    gitLogin("trial_doldu");
                }
            }

            @Override
            public void onHata(String hata) {
                gitLoginHata("Bağlantı hatası. Lütfen tekrar deneyin.");
            }
        });

        
        handler.postDelayed(() -> {
            if (!resolved) gitLogin(null);
        }, TIMEOUT_MS);
    }

    private final Runnable statusDongusu = new Runnable() {
        @Override
        public void run() {
            if (resolved) return;
            statusIdx = (statusIdx + 1) % statusMessages.length;
            tvStatus.animate().alpha(0f).setDuration(250).withEndAction(() -> {
                tvStatus.setText(statusMessages[statusIdx]);
                tvStatus.animate().alpha(1f).setDuration(250).start();
            }).start();
            handler.postDelayed(this, 2200);
        }
    };

    private void gitMain() {
        if (resolved) return;
        resolved = true;
        runOnUiThread(() -> {
            ViewGroup.LayoutParams lp = progressFill.getLayoutParams();
            lp.width = dpToPx(160);
            progressFill.setLayoutParams(lp);
            tvStatus.setText("Erişim onaylandı!");

            handler.postDelayed(() -> {
                boolean hedefTelefon = com.example.livetvapp.remotecontrol.DeviceDetector.isPhone(SplashActivity.this);
                Intent i;
                if (!TermsActivity.guncelSozlesmeOnaylandiMi(SplashActivity.this)) {
                    i = new Intent(SplashActivity.this, TermsActivity.class);
                    i.putExtra(TermsActivity.EXTRA_HEDEF_TELEFON, hedefTelefon);
                } else if (hedefTelefon) {
                    i = new Intent(SplashActivity.this, LauncherActivity.class);
                } else {
                    i = new Intent(SplashActivity.this, MainActivity.class);
                }
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }, 400);
        });
    }

    private void gitLogin(String extra) {
        if (resolved) return;
        resolved = true;
        runOnUiThread(() -> {
            Intent i = new Intent(SplashActivity.this, LoginActivity.class);
            if ("trial_doldu".equals(extra)) i.putExtra("trial_doldu", true);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }

    private void gitLoginHata(String mesaj) {
        if (resolved) return;
        resolved = true;
        runOnUiThread(() -> {
            Intent i = new Intent(SplashActivity.this, LoginActivity.class);
            i.putExtra("hata_mesaji", mesaj);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}