package com.example.livetvapp.paneller;

import android.app.Activity;
import android.graphics.Typeface;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.example.livetvapp.R;

public class OzelKlavye {
    private static final int PANEL_GENISLIK_DP = 400;

    private static final int RENK_TUS_NORMAL  = 0xFF1A3A5A;
    private static final int RENK_TUS_FOCUS   = 0xFFFF0000;
    private static final int RENK_TUS_OZEL    = 0xFF2A2A4A;
    private static final int RENK_TUS_ENTER   = 0xFF2E8B57;
    private static final int RENK_TUS_KISAYOL = 0xFF2A5A8A;
    private static final int RENK_METIN       = 0xFFEEF5FF;

    private Runnable onBackListener;
    private Runnable onKapatListener;
    public void setOnBackListener(Runnable r) { this.onBackListener = r; }
    public void setOnKapatListener(Runnable r) { this.onKapatListener = r; }

    private static final String[][] KUCUK_HARF = {
            {"🇹🇷", "⌫ SIL", "←", "→", "http://","https://", ".com", ".net", ".org", ".m3u", ".ts", ":8080" },
            {"q","w","e","r","t","y","u","ı","o","p","ğ","ü"},
            {"a","s","d","f","g","h","j","k","l","ş","i","@"},
            {"z","x","c","v","b","n","m","ö","ç",".","_","-"},
            {"CAPS","SAYI","?",":","/","BOSLUK","ENTER","AZSH"}
    };

    private static final String[][] BUYUK_HARF = {
            {"🇹🇷", "⌫ SIL", "←", "→", "https://", ".com", ".net", ".org", ".m3u", ".ts", ":8080", "http://"},
            {"Q","W","E","R","T","Y","U","I","O","P","Ğ","Ü"},
            {"A","S","D","F","G","H","J","K","L","Ş","İ","@"},
            {"Z","X","C","V","B","N","M","Ö","Ç",".","_","-"},
            {"caps","SAYI","?",":","/","BOSLUK","ENTER","AZSH"}
    };

    private static final String[][] SAYI_SEMBOL = {
            {"🇹🇷", "⌫ SIL", "←", "→", "1","2","3","4","5","6","7","8","9","0"},
            {"!","@","#","$","%","^","&","*","(",")","[","]"},
            {"-","_","=","+","{","}",";",":","'","\"",",","."},
            {"/","\\","<",">","?","~","^","&","*","(","!","#"},
            {"HARF","BOSLUK","ENTER","AZSH"}
    };

    private final Activity activity;
    private EditText aktifEditText;
    private android.view.ViewTreeObserver.OnGlobalFocusChangeListener focusListener;
    private EditText       hedefEditText;
    private boolean        buyukHarf     = false;
    private boolean        sayiModu      = false;
    private View           klavyeRoot;
    private LinearLayout   icKap;
    private boolean        gorunuyor     = false;
    private int            kursorPozisyon = -1;
    private int sakliKursorPozisyon = -1;

    public boolean isVisible() { return gorunuyor; }

    public interface OnEnterListener { void onEnter(String metin); }
    private OnEnterListener enterListener;
    public void setOnEnterListener(OnEnterListener l) { this.enterListener = l; }

    public interface OnFocusPaneleGecListener { void onFocusPaneleGec(); }
    private OnFocusPaneleGecListener focusPaneleGecListener;
    public void setOnFocusPaneleGecListener(OnFocusPaneleGecListener l) { this.focusPaneleGecListener = l; }

    public OzelKlavye(Activity activity) {
        this.activity = activity;
    }

    public void ekranaEkle() {
        if (klavyeRoot != null) return;

        klavyeRoot = LayoutInflater.from(activity)
                .inflate(R.layout.panel_klavye, null, false);
        icKap = klavyeRoot.findViewById(R.id.klavyeIcKap);

        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int ekranW  = dm.widthPixels;
        int ekranH  = dm.heightPixels;
        int panelPx = dp(PANEL_GENISLIK_DP);
        int klavyeW = ekranW - panelPx;
        int klavyeH = ekranH / 2;

        FrameLayout.LayoutParams icLP = new FrameLayout.LayoutParams(klavyeW, klavyeH);
        icLP.leftMargin = 0;
        icLP.topMargin  = ekranH - klavyeH;
        icKap.setLayoutParams(icLP);

        klavyeRoot.setVisibility(View.GONE);
        activity.addContentView(
                klavyeRoot,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));

        klavyeyiCiz();
    }

    public void goster(EditText hedef) {
        hedefEditText  = hedef;
        kursorPozisyon = (hedef == aktifEditText || aktifEditText == null)
                ? sakliKursorPozisyon
                : -1;
        hedef.setInputType(InputType.TYPE_NULL);
        hedef.setShowSoftInputOnFocus(false);
        hedef.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) {}
            @Override public void onViewDetachedFromWindow(View v) { gizle(); }
        });

        if (klavyeRoot != null) {
            klavyeRoot.setVisibility(View.VISIBLE);
            gorunuyor = true;
            klavyeRoot.post(this::ilkButonFocus);
        }
        if (hedefEditText != null) {
            hedefEditText.setBackgroundColor(0xFFFFDD00);
            hedefEditText.setTextColor(0xFF000000);
            hedefEditText.setHintTextColor(0xFF000000);
            aktifEditText = hedefEditText;
        }
        if (focusListener == null) {
            focusListener = (oldFocus, newFocus) -> {
                if (!gorunuyor) return;
                if (newFocus instanceof EditText) {
                    EditText yeni = (EditText) newFocus;
                    if (aktifEditText != null && aktifEditText != yeni) {
                        aktifEditText.setBackgroundColor(0xCC000000);
                        aktifEditText.setTextColor(0xFFFFFFFF);
                        aktifEditText.setHintTextColor(0x88FFFFFF);
                    }
                    yeni.setBackgroundColor(0xFFFFDD00);
                    yeni.setTextColor(0xFF000000);
                    yeni.setHintTextColor(0xFF000000);
                    aktifEditText = yeni;
                    if (hedefEditText != yeni) {
                        hedefEditText  = yeni;
                        kursorPozisyon = -1;
                        hedefEditText.setInputType(InputType.TYPE_NULL);
                        hedefEditText.setShowSoftInputOnFocus(false);
                    }
                }
            };
            activity.getWindow().getDecorView().getRootView()
                    .getViewTreeObserver().addOnGlobalFocusChangeListener(focusListener);
        }
    }

    public void gizle() {
        if (focusListener != null) {
            activity.getWindow().getDecorView().getRootView()
                    .getViewTreeObserver().removeOnGlobalFocusChangeListener(focusListener);
            focusListener = null;
        }
        if (aktifEditText != null) {
            aktifEditText.setBackgroundColor(0xCC000000);
            aktifEditText.setTextColor(0xFFFFFFFF);
            aktifEditText.setHintTextColor(0x88FFFFFF);
            aktifEditText = null;
        }
        if (hedefEditText != null) {
            hedefEditText.setBackgroundColor(0xCC000000);
            hedefEditText.setTextColor(0xFFFFFFFF);
            hedefEditText.setHintTextColor(0x88FFFFFF);
        }
        sakliKursorPozisyon = kursorPozisyon;
        if (klavyeRoot != null) klavyeRoot.setVisibility(View.GONE);
        gorunuyor = false;
    }

    public void setHedefEditText(EditText yeni) {
        if (gorunuyor) {
            if (aktifEditText != null && aktifEditText != yeni) {
                aktifEditText.setBackgroundColor(0xCC000000);
                aktifEditText.setTextColor(0xFFFFFFFF);
                aktifEditText.setHintTextColor(0x88FFFFFF);
            }
            yeni.setBackgroundColor(0xFFFFDD00);
            aktifEditText = yeni;
        }
        hedefEditText  = yeni;
        kursorPozisyon = -1;
        yeni.setTextColor(0xFF000000);
        yeni.setHintTextColor(0xFF000000);
    }

    public void focusKlavyeye() {
        if (klavyeRoot != null && gorunuyor) ilkButonFocus();
    }

    public void focusEditTexte() {
        if (hedefEditText != null) hedefEditText.requestFocus();
    }


    public boolean handleCursorKey(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (kursorPozisyon < 0 && hedefEditText != null)
                kursorPozisyon = hedefEditText.getText().length();
            if (kursorPozisyon > 0) kursorPozisyon--;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (kursorPozisyon < 0 && hedefEditText != null)
                kursorPozisyon = hedefEditText.getText().length();
            if (hedefEditText != null && kursorPozisyon < hedefEditText.getText().length())
                kursorPozisyon++;
        }
        cursorPozisyonuVurgula();
        return true;
    }

    public void cursorPozisyonuVurgula() {
        if (hedefEditText == null) return;
        android.text.Spannable sp = hedefEditText.getText();
        android.text.style.BackgroundColorSpan[] eskiSpanlar =
                sp.getSpans(0, sp.length(), android.text.style.BackgroundColorSpan.class);
        for (android.text.style.BackgroundColorSpan span : eskiSpanlar) {
            sp.removeSpan(span);
        }
        int pos = kursorPozisyon >= 0 ? kursorPozisyon : hedefEditText.getSelectionStart();
        pos = Math.max(0, Math.min(pos, sp.length()));
        kursorPozisyon = pos;
        if (pos < sp.length()) {
            sp.setSpan(new android.text.style.BackgroundColorSpan(0xFFFF6600),
                    pos, pos + 1, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (pos > 0) {
            sp.setSpan(new android.text.style.BackgroundColorSpan(0xFFFF6600),
                    pos - 1, pos, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void ilkButonFocus() {
        if (!satirButonlar.isEmpty() && !satirButonlar.get(0).isEmpty()) {
            satirButonlar.get(0).get(0).requestFocus();
        }
    }

    private final java.util.List<java.util.List<Button>> satirButonlar = new java.util.ArrayList<>();

    private void klavyeyiCiz() {
        if (icKap == null) return;
        icKap.removeAllViews();
        satirButonlar.clear();

        String[][] layout;
        if (sayiModu)       layout = SAYI_SEMBOL;
        else if (buyukHarf) layout = BUYUK_HARF;
        else                layout = KUCUK_HARF;

        for (String[] satir : layout) {
            LinearLayout satirView = new LinearLayout(activity);
            satirView.setOrientation(LinearLayout.HORIZONTAL);
            satirView.setFocusable(false);
            satirView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            LinearLayout.LayoutParams sLP = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
            sLP.setMargins(0, 2, 0, 2);
            satirView.setLayoutParams(sLP);

            java.util.List<Button> satirListesi = new java.util.ArrayList<>();
            for (String tus : satir) {
                if (tus.isEmpty()) continue;
                Button btn = tusBtnOlustur(tus);
                satirView.addView(btn);
                satirListesi.add(btn);
            }

            if (!satirListesi.isEmpty()) {
                satirButonlar.add(satirListesi);
                icKap.addView(satirView);
            }
        }

        navigasyonuKur();
    }

    private void navigasyonuKur() {
        for (int s = 0; s < satirButonlar.size(); s++) {
            java.util.List<Button> satir = satirButonlar.get(s);
            for (int t = 0; t < satir.size(); t++) {
                Button btn = satir.get(t);
                final int satirIdx = s;
                final int tusIdx   = t;

                btn.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() != KeyEvent.ACTION_DOWN) return false;

                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_RIGHT: {
                            int sonraki = tusIdx + 1;
                            if (sonraki < satir.size()) satir.get(sonraki).requestFocus();
                            else                        satir.get(0).requestFocus();
                            return true;
                        }
                        case KeyEvent.KEYCODE_BACK:
                        case KeyEvent.KEYCODE_ESCAPE:
                            if (onBackListener != null) onBackListener.run();
                            return true;
                        case KeyEvent.KEYCODE_DPAD_LEFT: {
                            int onceki = tusIdx - 1;
                            if (onceki >= 0) satir.get(onceki).requestFocus();
                            else             satir.get(satir.size() - 1).requestFocus();
                            return true;
                        }
                        case KeyEvent.KEYCODE_DPAD_DOWN: {
                            if (satirIdx + 1 < satirButonlar.size()) {
                                java.util.List<Button> altSatir = satirButonlar.get(satirIdx + 1);
                                altSatir.get(Math.min(tusIdx, altSatir.size() - 1)).requestFocus();
                            }
                            return true;
                        }
                        case KeyEvent.KEYCODE_DPAD_UP: {
                            if (satirIdx - 1 >= 0) {
                                java.util.List<Button> ustSatir = satirButonlar.get(satirIdx - 1);
                                ustSatir.get(Math.min(tusIdx, ustSatir.size() - 1)).requestFocus();
                                return true;
                            }
                            if (focusPaneleGecListener != null) focusPaneleGecListener.onFocusPaneleGec();
                            return true;
                        }
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER: {
                            tusaBasildi((String) btn.getTag());
                            return true;
                        }
                    }
                    return false;
                });
            }
        }
    }

    private Button tusBtnOlustur(String tus) {
        Button btn = new Button(activity);
        btn.setText(gorselMetin(tus));
        btn.setTag(tus);
        btn.setTextColor(RENK_METIN);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setAllCaps(false);
        btn.setPadding(4, 4, 4, 4);
        btn.setFocusable(true);
        btn.setFocusableInTouchMode(true);
        btn.setTextSize(kisayolTusMu(tus) ? 12 : 15);

        int agirlik = genislikAgirlik(tus);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, agirlik);
        lp.setMargins(2, 0, 2, 0);
        btn.setLayoutParams(lp);

        int renkN;
        if (tus.equals("ENTER"))                          renkN = RENK_TUS_ENTER;
        else if (kisayolTusMu(tus) || tus.equals("⌫ SIL")) renkN = RENK_TUS_KISAYOL;
        else if (ozelTusMu(tus))                          renkN = RENK_TUS_OZEL;
        else                                              renkN = RENK_TUS_NORMAL;

        btn.setBackgroundColor(renkN);
        final int fr = renkN;
        btn.setOnFocusChangeListener((v, f) -> v.setBackgroundColor(f ? RENK_TUS_FOCUS : fr));
        btn.setOnClickListener(v -> tusaBasildi(tus));
        return btn;
    }

    private void tusaBasildi(String tus) {
        if (hedefEditText == null && !tus.equals("KAPAT")) return;

        switch (tus) {
            case "AZSH":
                break;
            case "🇹🇷":
                break;
            case "⌫ SIL":
                silSonKarakter();
                break;
            case "←":
                if (kursorPozisyon < 0 && hedefEditText != null)
                    kursorPozisyon = hedefEditText.getText().length();
                if (kursorPozisyon > 0) kursorPozisyon--;
                cursorPozisyonuVurgula();
                break;
            case "→":
                if (kursorPozisyon < 0 && hedefEditText != null)
                    kursorPozisyon = hedefEditText.getText().length();
                if (hedefEditText != null && kursorPozisyon < hedefEditText.getText().length())
                    kursorPozisyon++;
                cursorPozisyonuVurgula();
                break;
            case "BOSLUK":
                metineEkle(" ");
                break;
            case "CAPS":
            case "caps":
                buyukHarf = !buyukHarf;
                sayiModu  = false;
                klavyeyiCiz();
                icKap.post(this::ilkButonFocus);
                break;
            case "SAYI":
                sayiModu = true;
                klavyeyiCiz();
                icKap.post(this::ilkButonFocus);
                break;
            case "HARF":
                sayiModu = false;
                klavyeyiCiz();
                icKap.post(this::ilkButonFocus);
                break;
            case "ENTER":
                focusEditTexte();
                if (enterListener != null && hedefEditText != null)
                    enterListener.onEnter(hedefEditText.getText().toString());
                break;
            case "KAPAT":
                gizle();
                if (onKapatListener != null) onKapatListener.run();
                else if (focusPaneleGecListener != null) focusPaneleGecListener.onFocusPaneleGec();
                break;
            default:
                metineEkle(tus);
                if (buyukHarf && !sayiModu && tus.length() == 1 && Character.isLetter(tus.charAt(0))) {
                    buyukHarf = false;
                    klavyeyiCiz();
                    icKap.post(this::ilkButonFocus);
                }
                break;
        }
    }

    private void metineEkle(String k) {
        int pos = kursorPozisyon >= 0 ? kursorPozisyon : hedefEditText.getText().length();
        pos = Math.max(0, Math.min(pos, hedefEditText.getText().length()));
        hedefEditText.getText().insert(pos, k);
        kursorPozisyon = pos + k.length();
        cursorPozisyonuVurgula();
    }

    private void silSonKarakter() {
        int pos = kursorPozisyon >= 0 ? kursorPozisyon : hedefEditText.getText().length();
        pos = Math.max(0, Math.min(pos, hedefEditText.getText().length()));
        if (pos > 0) {
            hedefEditText.getText().delete(pos - 1, pos);
            kursorPozisyon = pos - 1;
        }
        cursorPozisyonuVurgula();
    }

    private boolean kisayolTusMu(String t) {
        switch (t) {
            case "http://": case "https://": case ".com": case ".net":
            case ".org": case ".m3u": case ".ts": case ":8080": case ":80":
            case "⌫ SIL": return true;
            default: return false;
        }
    }

    private boolean ozelTusMu(String t) {
        switch (t) {
            case "CAPS": case "caps": case "SAYI": case "HARF":
            case "BOSLUK": case "KAPAT": case "ENTER":
            case "?": case ":": case "/": return true;
            default: return false;
        }
    }

    private int genislikAgirlik(String t) {
        switch (t) {
            case "BOSLUK":  return 4;
            case "ENTER":   return 3;
            case "⌫ SIL":   return 3;
            case "KAPAT":   return 2;
            case "http://": return 3;
            case "https://":return 3;
            default:        return 2;
        }
    }

    private String gorselMetin(String t) {
        switch (t) {
            case "⌫ SIL":  return "⌫ SIL";
            case "BOSLUK": return "SPACE";
            case "CAPS":   return "⇧";
            case "caps":   return "⇩";
            case "SAYI":   return "123";
            case "HARF":   return "ABC";
            case "ENTER":  return "✓ TAMAM";
            case "KAPAT":  return "✕";
            default:       return t;
        }
    }

    private int dp(int dp) {
        return Math.round(dp * activity.getResources().getDisplayMetrics().density);
    }
}