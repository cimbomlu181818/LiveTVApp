package com.example.livetvapp.paneller;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.livetvapp.R;
import java.util.List;

public class KisayolKutusu extends LinearLayout {

    private LinearLayout satirKonteyner;

    public KisayolKutusu(Context context) {
        super(context);
        init();
    }

    public KisayolKutusu(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        LayoutInflater.from(getContext()).inflate(R.layout.kisayol_kutusu, this, true);
        satirKonteyner = findViewById(R.id.satirKonteyner);
    }

    public void setAciklamalar(List<String> aciklamalar) {
        satirKonteyner.removeAllViews();
        if (aciklamalar == null || aciklamalar.size() != 6) return;
        String[] tuslar = {"▲ YUKARI", "▼ AŞAĞI", "◀ SOL", "▶ SAĞ", "● OK/ENTER", "⎋ GERİ"};
        for (int i = 0; i < 6; i++) {
            View satir = LayoutInflater.from(getContext()).inflate(R.layout.kisayol_satiri, null);
            TextView sembol = satir.findViewById(R.id.tus_sembol);
            TextView aciklama = satir.findViewById(R.id.tus_aciklama);
            sembol.setText(tuslar[i]);
            aciklama.setText(aciklamalar.get(i));
            satirKonteyner.addView(satir);
        }
    }
}