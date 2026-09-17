package com.example.livetvapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import android.view.KeyEvent;
import android.view.View;
import android.widget.ScrollView;

import org.json.JSONObject;
public class TermsActivity extends AppCompatActivity {

    // Sözleşme metni her değiştiğinde bu numarayı artırın.
    // Böylece daha önce eski versiyonu onaylamış kullanıcılardan da
    // yeni versiyon için tekrar onay istenir.
    public static final int SOZLESME_VERSIYONU = 1;
    private static final String PREFS_ADI = "azsh_giris";
    private static final String ANAHTAR_ON_EK = "sozlesme_onay_v";

    public static final String EXTRA_HEDEF_TELEFON = "hedef_telefon";

    private TextView tvTermsMetin;
    private Button btnKabulEt, btnReddet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);

        tvTermsMetin = findViewById(R.id.tvTermsMetin);
        btnKabulEt = findViewById(R.id.btnKabulEt);
        btnReddet = findViewById(R.id.btnReddet);

        tvTermsMetin.setText(getString(R.string.sozlesme_metni));

        final ScrollView scrollTerms = findViewById(R.id.scrollTerms);
        scrollTerms.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                View child = scrollTerms.getChildAt(0);
                int scrollAralik = child.getHeight()
                        - (scrollTerms.getHeight() - scrollTerms.getPaddingTop() - scrollTerms.getPaddingBottom());
                if (scrollTerms.getScrollY() >= scrollAralik) {
                    // Metnin en altına gelindi, artık odağı butona devret.
                    btnReddet.requestFocus();
                    return true;
                }
            }
            return false;
        });

        // İlk açılışta odak metne gelsin, kullanıcı D-pad ile okuyup aşağı insin.
        scrollTerms.requestFocus();

        btnKabulEt.setOnClickListener(v -> {
            onayiKaydet();
            uygulamayaGec();
        });

        btnReddet.setOnClickListener(v -> reddet());
    }

    @Override
    public void onBackPressed() {
        // Geri tuşu da "Reddet" ile aynı davranışı göstermeli.
        reddet();
        super.onBackPressed();
    }

    private void reddet() {
        // Hiçbir sözleşme onayı kaydedilmez. Ayrıca bu cihazdaki kayıtlı
        // oturumu (email) da temizliyoruz — aksi halde LoginActivity/SplashActivity
        // otomatik oturum açmayı deneyip kullanıcıyı tekrar bu ekrana düşürür
        // ve döngüye sokar.
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_ADI, Context.MODE_PRIVATE).edit();
        String kayitliEmail = getSharedPreferences(PREFS_ADI, Context.MODE_PRIVATE).getString("email", null);
        editor.remove("email");
        editor.apply();

        // Backend'e de bu cihazın kaydını düşürmesi için bildir (varsa).
        if (kayitliEmail != null) {
            String cihazId = android.provider.Settings.Secure.getString(
                    getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            new ApiHelper().cihazCikisYap(kayitliEmail, cihazId, new ApiHelper.ApiListener() {
                @Override
                public void onBasarili(JSONObject sonuc) { /* önemli değil, sessizce geç */ }
                @Override
                public void onHata(String hata) { /* bağlantı yoksa da giriş ekranına dönmeye engel değil */ }
            });
        }

        Intent intent = new Intent(TermsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void onayiKaydet() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_ADI, Context.MODE_PRIVATE).edit();
        editor.putBoolean(ANAHTAR_ON_EK + SOZLESME_VERSIYONU, true);
        editor.apply();
    }

    private void uygulamayaGec() {
        boolean hedefTelefon = getIntent().getBooleanExtra(EXTRA_HEDEF_TELEFON, false);
        Intent intent;
        if (hedefTelefon) {
            intent = new Intent(TermsActivity.this, LauncherActivity.class);
        } else {
            intent = new Intent(TermsActivity.this, MainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * LoginActivity'nin, bu cihazda güncel sözleşme versiyonunun
     * daha önce onaylanıp onaylanmadığını kontrol etmesi için yardımcı metod.
     */
    public static boolean guncelSozlesmeOnaylandiMi(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_ADI, Context.MODE_PRIVATE);
        return prefs.getBoolean(ANAHTAR_ON_EK + SOZLESME_VERSIYONU, false);
    }
}