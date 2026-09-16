package com.example.livetvapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etSifre, etDogrulamaKodu;
    private Button btnGirisYap, btnKayitOl, btnGoogle;
    private TextView tvHata, tvTrialBilgisi, tvTrialDolduBanner;
    private View tvPremiumBilgisi;
    private TextView tvIbanDeger;
    private Button btnIbanKopyala;
    private ProgressBar progressBar;

    private View layoutDogrulama;
    private TextView tvDogrulamaEmail;
    private Button btnKoduOnayla;
    private Button btnTekrarGonder;
    private Button btnGirisEkraninaGeri;

    private LinearLayout layoutTrialUyari;
    private TextView tvTrialBittiMesaji, tvUyariSolIkon, tvUyariSagIkon;

    private ApiHelper apiHelper;
    private String beklenenDogrulamaEmail = "";

    private static final int BAKIM_OVERLAY_ID = 998877;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        apiHelper = new ApiHelper();

        boolean trialDoldu = getIntent().getBooleanExtra("trial_doldu", false);
        baglantilariYap();
        olaylariAyarla();

        
        btnGoogle.setVisibility(View.GONE);

        if (trialDoldu) {
            trialBittiUyariGoster();
        }

        String kayitliEmail = kayitliEmailGetir();
        if (kayitliEmail != null) {
            erisimKontrolEt(kayitliEmail);
        }

        String hataMesaji = getIntent().getStringExtra("hata_mesaji");
        if (hataMesaji != null) {
            hataGoster(hataMesaji);
        }
    }

    private void baglantilariYap() {
        etEmail             = findViewById(R.id.etEmail);
        etSifre             = findViewById(R.id.etSifre);
        etDogrulamaKodu     = findViewById(R.id.etDogrulamaKodu);
        btnGirisYap         = findViewById(R.id.btnGirisYap);
        btnKayitOl          = findViewById(R.id.btnKayitOl);
        btnGoogle           = findViewById(R.id.btnGoogle);
        tvHata              = findViewById(R.id.tvHata);
        tvTrialBilgisi      = findViewById(R.id.tvTrialBilgisi);
        tvTrialDolduBanner  = findViewById(R.id.tvTrialDolduBanner);
        tvPremiumBilgisi    = findViewById(R.id.tvPremiumBilgisi);
        tvIbanDeger         = findViewById(R.id.tvIbanDeger);
        btnIbanKopyala      = findViewById(R.id.btnIbanKopyala);
        progressBar         = findViewById(R.id.progressBar);

        layoutDogrulama      = findViewById(R.id.layoutDogrulama);
        tvDogrulamaEmail     = findViewById(R.id.tvDogrulamaEmail);
        btnKoduOnayla        = findViewById(R.id.btnKoduOnayla);
        btnTekrarGonder      = findViewById(R.id.btnTekrarGonder);
        btnGirisEkraninaGeri = findViewById(R.id.btnGirisEkraninaGeri);

        layoutTrialUyari    = findViewById(R.id.layoutTrialUyari);
        tvTrialBittiMesaji  = findViewById(R.id.tvTrialBittiMesaji);
        tvUyariSolIkon      = findViewById(R.id.tvUyariSolIkon);
        tvUyariSagIkon      = findViewById(R.id.tvUyariSagIkon);
    }

    private void olaylariAyarla() {
        btnGirisYap.setOnClickListener(v -> islemYap(false));
        btnKayitOl.setOnClickListener(v -> islemYap(true));

        if (btnIbanKopyala != null && tvIbanDeger != null) {
            btnIbanKopyala.setOnClickListener(v -> {
                String iban = tvIbanDeger.getText().toString().replace(" ", "");
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("IBAN", iban);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "IBAN kopyalandı", Toast.LENGTH_SHORT).show();
            });
        }

        etEmail.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                etSifre.requestFocus();
                return true;
            }
            return false;
        });
        etSifre.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                islemYap(false);
                return true;
            }
            return false;
        });

        btnKoduOnayla.setOnClickListener(v -> {
            String kod = etDogrulamaKodu.getText().toString().trim();
            if (kod.isEmpty()) {
                hataGoster("Lütfen doğrulama kodunu girin.");
                return;
            }
            klavyeGizle();
            yuklemeGoster(true);
            hataGizle();
            apiHelper.dogrula(beklenenDogrulamaEmail, kod, new ApiHelper.ApiListener() {
                @Override
                public void onBasarili(JSONObject sonuc) {
                    yuklemeGoster(false);
                    dogrulamaPaneliniGizle();
                    hataGoster("Hesabınız doğrulandı! Şimdi giriş yapabilirsiniz.");
                }
                @Override
                public void onHata(String hata) {
                    yuklemeGoster(false);
                    hataGoster(hata);
                }
            });
        });

        btnTekrarGonder.setOnClickListener(v -> {
            hataGoster("Yeni kod almak için lütfen tekrar kayıt olun.");
        });

        btnGirisEkraninaGeri.setOnClickListener(v -> dogrulamaPaneliniGizle());
    }

    private void islemYap(boolean kayitModu) {
        String email = etEmail.getText().toString().trim();
        String sifre = etSifre.getText().toString().trim();

        if (email.isEmpty()) {
            hataGoster("E-posta adresi boş olamaz.");
            etEmail.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            hataGoster("Geçerli bir e-posta adresi girin.");
            etEmail.requestFocus();
            return;
        }
        if (sifre.isEmpty()) {
            hataGoster("Şifre boş olamaz.");
            etSifre.requestFocus();
            return;
        }
        if (sifre.length() < 6) {
            hataGoster("Şifre en az 6 karakter olmalıdır.");
            etSifre.requestFocus();
            return;
        }

        klavyeGizle();
        yuklemeGoster(true);
        hataGizle();

        if (kayitModu) {
            apiHelper.kayitOl(email, sifre, new ApiHelper.ApiListener() {
                @Override
                public void onBasarili(JSONObject sonuc) {
                    yuklemeGoster(false);
                    dogrulamaPaneliniGoster(email);
                }
                @Override
                public void onHata(String hata) {
                    yuklemeGoster(false);
                    hataGoster(hata);
                }
            });
        } else {
            String cihazId = cihazIdGetir();
            apiHelper.girisYap(email, sifre, cihazId, new ApiHelper.ApiListener() {
                @Override
                public void onBasarili(JSONObject sonuc) {
                    yuklemeGoster(false);
                    kayitliEmailKaydet(email);
                    erisimKontrolEt(email);
                }
                @Override
                public void onHata(String hata) {
                    yuklemeGoster(false);
                    if ("Hesabınız henüz doğrulanmamış.".equals(hata)) {
                        dogrulamaPaneliniGoster(email);
                    } else {
                        hataGoster(hata);
                    }
                }
            });
        }
    }

    private void dogrulamaPaneliniGoster(String email) {
        beklenenDogrulamaEmail = email;
        runOnUiThread(() -> {
            hataGizle();
            trialUyariGizle();

            etEmail.setVisibility(View.GONE);
            etSifre.setVisibility(View.GONE);
            btnGirisYap.setVisibility(View.GONE);
            btnKayitOl.setVisibility(View.GONE);
            btnGoogle.setVisibility(View.GONE);

            if (tvTrialBilgisi != null)   tvTrialBilgisi.setVisibility(View.GONE);
            if (tvPremiumBilgisi != null) tvPremiumBilgisi.setVisibility(View.GONE);

            tvDogrulamaEmail.setText(
                    email + " adresine bir doğrulama kodu gönderdik.\n\n" +
                            "Kodu aşağıya girip onaylayın, ardından giriş yapabilirsiniz."
            );
            layoutDogrulama.setVisibility(View.VISIBLE);
        });
    }

    private void dogrulamaPaneliniGizle() {
        runOnUiThread(() -> {
            layoutDogrulama.setVisibility(View.GONE);

            etEmail.setVisibility(View.VISIBLE);
            etSifre.setVisibility(View.VISIBLE);
            btnGirisYap.setVisibility(View.VISIBLE);
            btnKayitOl.setVisibility(View.VISIBLE);

            if (tvPremiumBilgisi != null) tvPremiumBilgisi.setVisibility(View.VISIBLE);

            hataGizle();
        });
    }

    private void erisimKontrolEt(String email) {
        yuklemeGoster(true);
        apiHelper.erisimKontrol(email, cihazIdGetir(), new ApiHelper.ApiListener() {
            @Override
            public void onBasarili(JSONObject sonuc) {
                yuklemeGoster(false);
                try {
                    boolean bakimModu = sonuc.optBoolean("bakim_modu", false);
                    if (bakimModu) {
                        bakimEkraniniTamGoster(sonuc.optString("mesaj", "Uygulama bakımda."));
                        return;
                    }
                    String durum = sonuc.optString("durum", "");
                    boolean erisim = sonuc.optBoolean("erisim", false);

                    if (erisim) {
                        uygulamayiAc();
                    } else if ("mail_dogrulanmadi".equals(durum)) {
                        dogrulamaPaneliniGoster(email);
                    } else {
                        trialBittiUyariGoster();
                    }
                } catch (Exception e) {
                    hataGoster("Beklenmeyen bir hata oluştu.");
                }
            }
            @Override
            public void onHata(String hata) {
                yuklemeGoster(false);
                hataGoster(hata);
            }
        });
    }

    
    private void trialBittiUyariGoster() {
        runOnUiThread(() -> {
            hataGizle();
            if (tvTrialBilgisi != null) tvTrialBilgisi.setVisibility(View.GONE);
            if (tvPremiumBilgisi != null) tvPremiumBilgisi.setVisibility(View.VISIBLE);

            String tamMetin = "Deneme süreniz doldu. Uygulamayı kullanmaya devam etmek için aşağıdaki hesaba 100 TL gönderin. Açıklama kısmına kayıt olurken kullandığınız e-posta adresini yazın, dekont fotoğrafını azsh181818@gmail.com adresine iletin. Ödemeniz onaylandıktan sonra hesabınız en geç 12 saat içinde aktif olacaktır.";
            String mailAdresi = "azsh181818@gmail.com";

            android.text.SpannableString spannable = new android.text.SpannableString(tamMetin);
            int baslangic = tamMetin.indexOf(mailAdresi);
            if (baslangic >= 0) {
                int bitis = baslangic + mailAdresi.length();
                spannable.setSpan(
                        new android.text.style.ForegroundColorSpan(0xFFE58A8A),
                        baslangic, bitis,
                        android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                spannable.setSpan(
                        new android.text.style.RelativeSizeSpan(1.15f),
                        baslangic, bitis,
                        android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                spannable.setSpan(
                        new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                        baslangic, bitis,
                        android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
            tvTrialBittiMesaji.setText(spannable);

            layoutTrialUyari.setVisibility(View.VISIBLE);

            blinkBaslat(tvUyariSolIkon);
            blinkBaslat(tvUyariSagIkon);
        });
    }


    private void trialUyariGizle() {
        layoutTrialUyari.setVisibility(View.GONE);
        tvUyariSolIkon.clearAnimation();
        tvUyariSagIkon.clearAnimation();
    }

    private void blinkBaslat(View view) {
        android.animation.ObjectAnimator anim =
                android.animation.ObjectAnimator.ofFloat(view, "alpha", 1f, 0.15f);
        anim.setDuration(700);
        anim.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        anim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        anim.start();
    }

    private void uygulamayiAc() {
        Intent intent;
        if (com.example.livetvapp.remotecontrol.DeviceDetector.isPhone(this)) {
            intent = new Intent(LoginActivity.this, LauncherActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, MainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String cihazIdGetir() {
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private SharedPreferences tercihler() {
        return getSharedPreferences("azsh_giris", Context.MODE_PRIVATE);
    }

    private void kayitliEmailKaydet(String email) {
        tercihler().edit().putString("email", email).apply();
    }

    private String kayitliEmailGetir() {
        return tercihler().getString("email", null);
    }

    private void yuklemeGoster(boolean goster) {
        runOnUiThread(() -> {
            progressBar.setVisibility(goster ? View.VISIBLE : View.GONE);
            btnGirisYap.setEnabled(!goster);
            btnKayitOl.setEnabled(!goster);
        });
    }

    private void hataGoster(String mesaj) {
        runOnUiThread(() -> {
            tvHata.setText(mesaj);
            tvHata.setVisibility(View.VISIBLE);
        });
    }

    private void hataGizle() {
        runOnUiThread(() -> tvHata.setVisibility(View.GONE));
    }

    
    private void bakimEkraniniTamGoster(String mesaj) {
        runOnUiThread(() -> {
            if (findViewById(BAKIM_OVERLAY_ID) != null) return; 

            klavyeGizle();

            android.widget.FrameLayout kaplama = new android.widget.FrameLayout(this);
            kaplama.setId(BAKIM_OVERLAY_ID);
            kaplama.setBackgroundColor(0xFF0A0A0A);
            kaplama.setFocusableInTouchMode(true);
            kaplama.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

            android.widget.LinearLayout icerik = new android.widget.LinearLayout(this);
            icerik.setOrientation(android.widget.LinearLayout.VERTICAL);
            icerik.setGravity(android.view.Gravity.CENTER);
            icerik.setPadding(80, 80, 80, 80);
            icerik.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

            android.widget.TextView ikon = new android.widget.TextView(this);
            ikon.setText("⚙");
            ikon.setTextSize(72);
            ikon.setTextColor(0xFFFF6B2C);
            ikon.setGravity(android.view.Gravity.CENTER);
            icerik.addView(ikon);

            android.animation.ObjectAnimator donme =
                    android.animation.ObjectAnimator.ofFloat(ikon, "rotation", 0f, 360f);
            donme.setDuration(3000);
            donme.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            donme.setInterpolator(new android.view.animation.LinearInterpolator());
            donme.start();

            android.widget.Space bosluk1 = new android.widget.Space(this);
            bosluk1.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 48));
            icerik.addView(bosluk1);

            android.widget.TextView tvBaslik = new android.widget.TextView(this);
            tvBaslik.setText("UYGULAMA BAKIMDA");
            tvBaslik.setTextColor(0xFFFFFFFF);
            tvBaslik.setTextSize(22);
            tvBaslik.setGravity(android.view.Gravity.CENTER);
            tvBaslik.setTypeface(tvBaslik.getTypeface(), android.graphics.Typeface.BOLD);
            icerik.addView(tvBaslik);

            android.widget.Space bosluk2 = new android.widget.Space(this);
            bosluk2.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 20));
            icerik.addView(bosluk2);

            android.widget.TextView tvMesaj = new android.widget.TextView(this);
            tvMesaj.setText(mesaj);
            tvMesaj.setTextColor(0xFFB0B0B0);
            tvMesaj.setTextSize(15);
            tvMesaj.setGravity(android.view.Gravity.CENTER);
            tvMesaj.setLineSpacing(6, 1.2f);
            icerik.addView(tvMesaj);

            kaplama.addView(icerik);
            addContentView(kaplama, new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
            kaplama.requestFocus();
        });
    }

    private void klavyeGizle() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            View focused = getCurrentFocus();
            if (focused instanceof Button) {
                focused.performClick();
                return true;
            } else if (focused == etEmail) {
                etSifre.requestFocus();
                return true;
            } else if (focused == etSifre) {
                islemYap(false);
                return true;
            }
        }
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (layoutDogrulama != null && layoutDogrulama.getVisibility() == View.VISIBLE) {
                dogrulamaPaneliniGizle();
                return true;
            }
            klavyeGizle();
            finishAffinity();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (layoutDogrulama != null && layoutDogrulama.getVisibility() == View.VISIBLE) {
            dogrulamaPaneliniGizle();
            return;
        }
        klavyeGizle();
        super.onBackPressed();
        finishAffinity();
    }
}