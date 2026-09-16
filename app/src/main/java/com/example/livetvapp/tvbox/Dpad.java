package com.example.livetvapp.tvbox;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.livetvapp.Adapter.DiziAdapter;
import com.example.livetvapp.R;
import com.example.livetvapp.metadata.metadataPanel;
import com.example.livetvapp.paneller.AramaPaneli;
import com.example.livetvapp.paneller.AyarlarPaneli;
import com.example.livetvapp.paneller.DiziPaneli;
import com.example.livetvapp.paneller.GizlePaneli;
import com.example.livetvapp.paneller.Icerikpaneli;
import com.example.livetvapp.paneller.SiralamaPaneli;
import com.example.livetvapp.paneller.KategoriPaneli;
import com.example.livetvapp.paneller.normalkanallistesipaneli;
import com.example.livetvapp.paneller.bar;
import com.example.livetvapp.paneller.KisayolKutusu;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.os.Handler;
import android.os.Looper;

public class Dpad {
    private AppCompatActivity activity;
    private KisayolKutusu kisayolKutusu;
    private normalkanallistesipaneli kanalPaneli;
    private AyarlarPaneli ayarlarPaneli;
    private KategoriPaneli kategoriPaneli;
    private Icerikpaneli icerikPaneli;
    private DiziPaneli diziPaneli;
    private Anakontrol anakontrol;

    public void setAnakontrol(Anakontrol a) {
        this.anakontrol = a;
    }

    private metadataPanel metadataPanel;

    public void setMetadataPanel(metadataPanel p) {
        this.metadataPanel = p;
    }

    private static final int ICERIK_GENISLIK = 170;
    private static final int KATEGORI_GENISLIK = 170;
    private bar oynaticiBar;
    private boolean aktif = false;
    private boolean panelYeniAcildi = false;
    private String aktifIcerikTipi = Icerikpaneli.TIP_CANLI_TV;

    public interface KanalDegistirmeCallback {
        void sonrakiKanalaGec();

        void oncekiKanalaGec();
    }

    public bar getOynaticiBar() {
        return oynaticiBar;
    }

    private KanalDegistirmeCallback kanalDegistirmeCallback;

    public void aktifYap() {
        this.aktif = true;
        System.out.println("🎮 Dpad AKTİF");
    }

    public void pasifYap() {
        this.aktif = false;
        System.out.println("🎮 Dpad PASİF");
    }

    public void setKanalDegistirmeCallback(KanalDegistirmeCallback cb) {
        this.kanalDegistirmeCallback = cb;
    }

    private Runnable ayarlarKapandiCallback;

    public enum AktifPanel {
        HICBIRI,
        SADECE_KANAL,
        SADECE_DIZI,
        KATEGORI_VE_KANAL,
        KATEGORI_VE_DIZI,
        ICERIK_KATEGORI_KANAL,
        ICERIK_KATEGORI_DIZI,
        AYARLAR,
        ARAMA,
        AYARLAR_GIZLE,      
        AYARLAR_SIRALAMA    
    }

    private AktifPanel aktifPanel = AktifPanel.HICBIRI;

    public Dpad(AppCompatActivity activity,
                normalkanallistesipaneli kanalPaneli,
                AyarlarPaneli ayarlarPaneli,
                KategoriPaneli kategoriPaneli,
                Icerikpaneli icerikPaneli,
                DiziPaneli diziPaneli) {
        this.activity = activity;
        this.kanalPaneli = kanalPaneli;
        this.ayarlarPaneli = ayarlarPaneli;
        this.kategoriPaneli = kategoriPaneli;
        this.icerikPaneli = icerikPaneli;
        this.diziPaneli = diziPaneli;
        System.out.println("🎮 Dpad oluşturuldu");
    }

    public void setKisayolKutusu(KisayolKutusu kutu) {
        this.kisayolKutusu = kutu;
        guncelleKisayol();
    }

    public Anakontrol getAnakontrol() {
        return anakontrol;
    }

    public void guncelleKisayol() {
        if (oynaticiBar != null && oynaticiBar.isSeekBarFocused()) {
            kisayolKutusu.setAciklamalar(Arrays.asList("Kapat", "Bara geç", "Geri sar", "İleri sar", "-------", "Kapat"));
            return;
        }
        if (kisayolKutusu == null) return;
        boolean barAcik = (oynaticiBar != null && oynaticiBar.isVisible());
        List<String> aciklamalar = new ArrayList<>();

        switch (aktifPanel) {
            case HICBIRI:
                if (barAcik) {
                    if (Icerikpaneli.TIP_CANLI_TV.equals(aktifIcerikTipi)) {
                        aciklamalar.addAll(Arrays.asList("Zaman çubuğu", "Barı kapat", "Butonlar arasında sol", "Butonlar arasında sağ", "Butona bas", "Barı kapat"));
                    } else {
                        aciklamalar.addAll(Arrays.asList("Seek bar'a geçer", "Barı kapat", "Butonlar arasında sol", "Butonlar arasında sağ", "Butona bas", "Barı kapat"));
                    }
                } else {
                    aciklamalar.addAll(Arrays.asList("Sonraki kanal", "Önceki kanal", "Bar açar", "Bar açar", "Kanal-", "Çıkış"));
                }
                break;
            case SADECE_KANAL:
                aciklamalar.addAll(Arrays.asList("Listede yukarı", "Listede aşağı", "Kategori paneli", "Paneli kapat", "Seç / çift tık favori", "Paneli kapat"));
                break;
            case SADECE_DIZI:
                aciklamalar.addAll(Arrays.asList("Listede yukarı", "Listede aşağı", "Kategori paneli", "Paneli kapat", "Seç / çift tık favori", "Paneli kapat"));
                break;
            case KATEGORI_VE_KANAL:
                aciklamalar.addAll(Arrays.asList("Kategoride yukarı", "Kategoride aşağı", "İçerik paneli", "Kanal listesine git", "Kategori seç", "Kanal listesini aç"));
                break;
            case KATEGORI_VE_DIZI:
                aciklamalar.addAll(Arrays.asList("Kategoride yukarı", "Kategoride aşağı", "İçerik paneli", "Dizi listesine git", "Kategori seç", "Dizi listesini aç"));
                break;
            case ICERIK_KATEGORI_KANAL:
            case ICERIK_KATEGORI_DIZI:
                aciklamalar.addAll(Arrays.asList("İçerikte yukarı", "İçerikte aşağı", "—", "Kategoriye geçiş", "İçerik seç", "Kategoriye geç"));
                break;
            case AYARLAR:
                
                AyarlarPaneli.MenuSeviyesi altMenu = ayarlarPaneli.getMevcutMenu();
                boolean anaMenuMu = (altMenu == AyarlarPaneli.MenuSeviyesi.ANA_MENU);

                if (anaMenuMu) {
                    
                    aciklamalar.addAll(Arrays.asList("Menüde yukarı", "Menüde aşağı", "------", "-------", "Seçeneği seç", "Ayarlardan çık"));
                } else {
                    
                    aciklamalar.addAll(Arrays.asList("Menüde yukarı", "Menüde aşağı", "------", "-------", "Seçeneği seç", "Üst menü"));
                }
                break;
            case AYARLAR_GIZLE:
                aciklamalar.addAll(Arrays.asList(
                        "Listede yukarı",
                        "Listede aşağı",
                        "Gizle/Göster (sol/sağ)",
                        "Gizle/Göster (sol/sağ)",
                        "Aç/Kapat",
                        "Kaydet ve çık"
                ));
                break;
            case AYARLAR_SIRALAMA:
                aciklamalar.addAll(Arrays.asList(
                        "Listede yukarı",
                        "Listede aşağı",
                        "Seçimi kaldır",
                        "Seç",
                        "Taşı/Aç",
                        "Kaydet ve çık"
                ));
                break;
            case ARAMA:
                aciklamalar.addAll(Arrays.asList("Listede yukarı", "Listede aşağı", "Sola kaydır", "Sağa kaydır", "Seç", "Kapat"));
                break;
            default:
                aciklamalar.addAll(Arrays.asList("▲", "▼", "◀", "▶", "●", "⎋"));
                break;
        }

        kisayolKutusu.setAciklamalar(aciklamalar);
        if (anakontrol != null) anakontrol.kutuGorunurlugunuGuncelle();
        if (anakontrol != null) {
            anakontrol.kutuMargininiAktifPaneleGoreAyarla();
        }
    }

    public void setAktifIcerikTipi(String tip) {
        this.aktifIcerikTipi = tip != null ? tip : Icerikpaneli.TIP_CANLI_TV;
        guncelleKisayol();
        System.out.println("🎮 Dpad.aktifIcerikTipi → " + this.aktifIcerikTipi);
    }

    public void setOynaticiBar(bar bar) {
        this.oynaticiBar = bar;
        guncelleKisayol();
    }

    public boolean handleBack_dokunmatikMod() {
        return handleBack();
    }

    public boolean handleKeyDownDokunmatik(int keyCode) {
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        boolean oncekiAktif = aktif;
        aktif = true;
        boolean sonuc = handleKeyDown(keyCode, event);
        aktif = oncekiAktif;
        return sonuc;
    }

    public boolean handleKeyDown(int keyCode, KeyEvent event) {
        if (!aktif) {
            System.out.println("🎮 Dpad pasif, handleKeyDown yoksayıldı, keyCode=" + keyCode);
            return false;
        }
        if (!aktif) return false;
        System.out.println("🎮 Dpad.handleKeyDown: keyCode=" + keyCode + " | aktifPanel=" + aktifPanel);
        if (keyCode == KeyEvent.KEYCODE_MENU
                || keyCode == KeyEvent.KEYCODE_DEL
                || keyCode == KeyEvent.KEYCODE_FORWARD_DEL) {
            if (aktifPanel == AktifPanel.HICBIRI) {
                if (oynaticiBar != null && oynaticiBar.isVisible()) {
                    oynaticiBar.gizle();
                }
                ayarlarPaneliAc();
            }
            return true;
        }
        if (oynaticiBar != null && oynaticiBar.isVisible()) {
            if (oynaticiBar.isSeekBarFocused()) {
                return oynaticiBar.handleSeekBarKey(keyCode);
            }
            return handleBarTuslari(keyCode, event);
        }
        if (aktifPanel == AktifPanel.AYARLAR
                || aktifPanel == AktifPanel.AYARLAR_GIZLE
                || aktifPanel == AktifPanel.AYARLAR_SIRALAMA) {
            return ayarlarPaneliKontrol(keyCode, event);
        }
        if (aktifPanel == AktifPanel.ARAMA) return false;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                return handleLeft();
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return handleRight();
            case KeyEvent.KEYCODE_DPAD_UP:
                return handleUp();
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return handleDown();
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                return handleEnter();
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
                return handleBack();
        }
        return false;
    }

    public boolean isPanelYeniAcildi() {
        return panelYeniAcildi;
    }

    private boolean handleBarTuslari(int keyCode, KeyEvent event) {
        
        List<Button> barButtons = new ArrayList<>();
        int[] ids = {R.id.btnKanallar, R.id.btnRewindStep, R.id.btnRewind, R.id.btnPlayPause, R.id.btnForward, R.id.btnForwardStep,
                R.id.btnFavori, R.id.btnMute, R.id.btnSettings, R.id.btnSearch, R.id.btnPiP, R.id.btnFullscreen, R.id.btnCloseBar};
        for (int id : ids) {
            Button btn = activity.findViewById(id);
            if (btn != null && btn.getVisibility() == View.VISIBLE) {
                barButtons.add(btn);
                btn.setFocusable(true);
                btn.setFocusableInTouchMode(true);
            }
        }
        if (barButtons.isEmpty()) return false;

        View focused = activity.getCurrentFocus();
        int idx = barButtons.indexOf(focused);

        
        if (idx == -1) {
            barButtons.get(0).requestFocus();
            return true;
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                int next = (idx + 1) % barButtons.size();
                barButtons.get(next).requestFocus();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_UP:
                int prev = (idx - 1 + barButtons.size()) % barButtons.size();
                barButtons.get(prev).requestFocus();
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                View focusedView = activity.getCurrentFocus();
                if (focusedView instanceof Button) {
                    focusedView.performClick();
                    oynaticiBar.resetControlsTimeout();
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
                oynaticiBar.gizle();
                guncelleKisayol();
                return true;
            default:
                return false;
        }
    }

    private boolean barAcilabilir() {
        boolean hicbirPanelAcik = aktifPanel == AktifPanel.HICBIRI;
        return oynaticiBar != null
                && !oynaticiBar.isVisible()
                && hicbirPanelAcik;
    }

    private boolean handleUp() {
        if (aktifPanel == AktifPanel.HICBIRI) {
            if (Icerikpaneli.TIP_CANLI_TV.equals(aktifIcerikTipi)) {
                System.out.println("⬆️ HICBIRI + LIVE → sonraki kanala geç");
                if (kanalDegistirmeCallback != null) kanalDegistirmeCallback.sonrakiKanalaGec();
                return true;
            }
            System.out.println("⬆️ HICBIRI + " + aktifIcerikTipi + " → işlem yok");
            return true;
        }

        RecyclerView rv = getAktifRecyclerView();
        if (rv == null) return false;

        View focused = rv.getFocusedChild();
        if (focused != null) {
            int pos = rv.getChildAdapterPosition(focused);
            if (pos > 0) {
                
                View prevChild = rv.getLayoutManager().findViewByPosition(pos - 1);
                if (prevChild != null) {
                    prevChild.requestFocus();
                    return true;
                } else {
                    
                    rv.smoothScrollToPosition(pos - 1);
                    rv.post(() -> {
                        View scrolledChild = rv.getLayoutManager().findViewByPosition(pos - 1);
                        if (scrolledChild != null) scrolledChild.requestFocus();
                    });
                    return true;
                }
            }
        }

        
        if (isEnUstOge(rv)) {
            System.out.println("⬆️ En üst öğe — focus burada kalıyor");
            return true;
        }
        return false;
    }

    private boolean handleDown() {
        if (aktifPanel == AktifPanel.HICBIRI) {
            if (Icerikpaneli.TIP_CANLI_TV.equals(aktifIcerikTipi)) {
                System.out.println("⬇️ HICBIRI + LIVE → önceki kanala geç");
                if (kanalDegistirmeCallback != null) kanalDegistirmeCallback.oncekiKanalaGec();
                return true;
            }
            System.out.println("⬇️ HICBIRI + " + aktifIcerikTipi + " → işlem yok");
            return true;
        }

        RecyclerView rv = getAktifRecyclerView();
        if (rv == null) return false;

        
        if (aktifPanel == AktifPanel.SADECE_KANAL) {
            View focused = rv.getFocusedChild();
            if (focused != null) {
                int pos = rv.getChildAdapterPosition(focused);
                int itemCount = rv.getAdapter() != null ? rv.getAdapter().getItemCount() : 0;
                if (pos >= itemCount - 3 && !kanalPaneli.isLoadingMore()) {
                    System.out.println("⬇️ Son öğeye yakın, lazy loading tetikleniyor");
                    kanalPaneli.daha_fazla_yukle();
                }
            }
        }

        View focused = rv.getFocusedChild();
        if (focused != null) {
            int pos = rv.getChildAdapterPosition(focused);
            int itemCount = rv.getAdapter() != null ? rv.getAdapter().getItemCount() : 0;
            if (pos < itemCount - 1) {
                
                View nextChild = rv.getLayoutManager().findViewByPosition(pos + 1);
                if (nextChild != null) {
                    nextChild.requestFocus();
                    return true;
                } else {
                    rv.smoothScrollToPosition(pos + 1);
                    rv.post(() -> {
                        View scrolledChild = rv.getLayoutManager().findViewByPosition(pos + 1);
                        if (scrolledChild != null) scrolledChild.requestFocus();
                    });
                    return true;
                }
            }
        }

        
        if (isEnAltOge(rv)) {
            System.out.println("⬇️ En alt öğe — focus burada kalıyor");
            return true;
        }
        return false;
    }

    private RecyclerView getAktifRecyclerView() {
        switch (aktifPanel) {
            case SADECE_KANAL:
            case KATEGORI_VE_KANAL:
            case ICERIK_KATEGORI_KANAL:
            case SADECE_DIZI:
            case KATEGORI_VE_DIZI:
            case ICERIK_KATEGORI_DIZI:
                return getFocusedRecyclerView();
            default:
                return null;
        }
    }

    private RecyclerView getFocusedRecyclerView() {
        View focused = activity.getCurrentFocus();
        if (focused == null) return null;
        android.view.ViewParent parent = focused.getParent();
        while (parent != null) {
            if (parent instanceof RecyclerView) return (RecyclerView) parent;
            if (parent instanceof View) parent = ((View) parent).getParent();
            else break;
        }
        return null;
    }

    private boolean isEnUstOge(RecyclerView rv) {
        View focused = rv.getFocusedChild();
        if (focused == null) {
            LinearLayoutManager lm = getLinearLayoutManager(rv);
            if (lm == null) return false;
            return lm.findFirstVisibleItemPosition() == 0;
        }
        int pos = rv.getChildAdapterPosition(focused);
        return pos == 0;
    }

    private boolean isEnAltOge(RecyclerView rv) {
        int itemCount = rv.getAdapter() != null ? rv.getAdapter().getItemCount() : 0;
        if (itemCount == 0) return true;
        View focused = rv.getFocusedChild();
        if (focused == null) {
            LinearLayoutManager lm = getLinearLayoutManager(rv);
            if (lm == null) return false;
            return lm.findLastVisibleItemPosition() >= itemCount - 1;
        }
        int pos = rv.getChildAdapterPosition(focused);
        return pos >= itemCount - 1;
    }

    private LinearLayoutManager getLinearLayoutManager(RecyclerView rv) {
        RecyclerView.LayoutManager lm = rv.getLayoutManager();
        if (lm instanceof LinearLayoutManager) return (LinearLayoutManager) lm;
        return null;
    }

    private boolean handleLeft() {
        boolean diziModu = Icerikpaneli.TIP_DIZI.equals(aktifIcerikTipi)
                || (diziPaneli != null && diziPaneli.isVisible());
        System.out.println("⬅️ handleLeft | aktifPanel=" + aktifPanel + " | diziModu=" + diziModu);
        switch (aktifPanel) {
            case HICBIRI:
                if (barAcilabilir()) {
                    System.out.println("⬅️ HICBIRI + MOVIE/SERIES → Bar gösteriliyor");
                    oynaticiBar.goster();
                    guncelleKisayol();
                    return true;
                }
                if (anakontrol != null) {
                    anakontrol.panelAcilirkenAktifOgeYukle();
                }
                return true;
            case SADECE_KANAL:
                kategoriAcVeKanaliKaydır();
                return true;
            case SADECE_DIZI:
                kategoriAcVeDiziKaydır();
                return true;
            case KATEGORI_VE_KANAL:
                icerikAcVeHepsiniKaydır(false);
                return true;
            case KATEGORI_VE_DIZI:
                icerikAcVeHepsiniKaydır(true);
                return true;
            case ICERIK_KATEGORI_KANAL:
            case ICERIK_KATEGORI_DIZI:
                System.out.println("⬅️ En sola gelindi, işlem yok");
                return true;
            default:
                return false;
        }
    }

    private boolean handleRight() {
        boolean diziModu = Icerikpaneli.TIP_DIZI.equals(aktifIcerikTipi)
                || (diziPaneli != null && diziPaneli.isVisible());
        System.out.println("➡️ handleRight | aktifPanel=" + aktifPanel + " | diziModu=" + diziModu);
        switch (aktifPanel) {
            case HICBIRI:
                if (barAcilabilir()) {
                    System.out.println("➡️ HICBIRI + MOVIE/SERIES → Bar gösteriliyor");
                    oynaticiBar.goster();
                    guncelleKisayol();
                    return true;
                }
                if (anakontrol != null) {
                    anakontrol.panelAcilirkenAktifOgeYukle();
                }
                return true;
            case SADECE_KANAL:
                kanalPaneliKapat();
                return true;
            case SADECE_DIZI:
                diziPaneliKapat();
                return true;
            case KATEGORI_VE_KANAL:
                kategoriKapatVeKanaliSifirla();
                return true;
            case KATEGORI_VE_DIZI:
                kategoriKapatVeDiziSifirla();
                return true;
            case ICERIK_KATEGORI_KANAL:
                icerikKapatVeKategoriSifirla(diziModu);
                return true;
            case ICERIK_KATEGORI_DIZI:
                icerikKapatVeKategoriSifirla(diziModu);
                return true;
            default:
                return false;
        }
    }

    private boolean handleEnter() {
        if (aktifPanel == AktifPanel.HICBIRI) {
            
            if (oynaticiBar != null && oynaticiBar.isVisible()) {
                oynaticiBar.gizle();
            }
            if (anakontrol != null) {
                
                if (anakontrol.isVideoPaused()) {
                    anakontrol.resumeVideo();
                }
                
                else {
                    if (Icerikpaneli.TIP_DIZI.equals(aktifIcerikTipi)) {
                        diziPaneli.setMarginStart(0);
                        View v = diziPaneli.getPanelView();
                        if (v != null) v.setVisibility(View.VISIBLE);
                        diziPaneli.goster();
                        aktifPanel = AktifPanel.SADECE_DIZI;
                        if (diziPaneli.getAdapter() != null)
                            diziPaneli.getAdapter().setPanelAcik(true);
                        guncelleKisayol();
                        focusDiziPanelineVer(false);  
                    } else {
                    anakontrol.panelAcilirkenAktifOgeYukle();  
                }
                }
            } else {
                
                if (Icerikpaneli.TIP_DIZI.equals(aktifIcerikTipi)) {
                    diziPaneli.setMarginStart(0);
                    View v = diziPaneli.getPanelView();
                    if (v != null) v.setVisibility(View.VISIBLE);
                    diziPaneli.goster();
                    aktifPanel = AktifPanel.SADECE_DIZI;
                    if (diziPaneli.getAdapter() != null) diziPaneli.getAdapter().setPanelAcik(true);
                    guncelleKisayol();
                    focusDiziPanelineVer();
                } else {
                    kanalPaneliAc();
                }
            }
            return true;
        }
        
        View focusedView = activity.getCurrentFocus();
        System.out.println("🔘 handleEnter | focusedView=" + (focusedView != null ? focusedView.getClass().getSimpleName() : "null"));
        if (focusedView instanceof Button) {
            focusedView.performClick();
            return true;
        }
        if (focusedView != null && focusedView.getId() == android.R.id.content) {
            RecyclerView rv = null;
            switch (aktifPanel) {
                case SADECE_DIZI:
                case KATEGORI_VE_DIZI:
                case ICERIK_KATEGORI_DIZI:
                    rv = diziPaneli != null ? diziPaneli.getRecyclerView() : null;
                    break;
                case SADECE_KANAL:
                case KATEGORI_VE_KANAL:
                case ICERIK_KATEGORI_KANAL:
                    rv = kanalPaneli != null ? kanalPaneli.getRecyclerView() : null;
                    break;
                default:
                    break;
            }
            if (rv != null) {
                System.out.println("🩹 handleEnter: focus kurtarılıyor, listeye geri veriliyor");
                rv.requestFocus();
            }
            return true;
        }
        return false;
    }

    private boolean handleBack() {
        System.out.println("🔙 handleBack | aktifPanel=" + aktifPanel);
        switch (aktifPanel) {
            case SADECE_KANAL:
                kanalPaneliKapat();
                return true;
            case SADECE_DIZI:
                diziPaneliKapat();
                return true;
            case KATEGORI_VE_KANAL:
                kategoriKapatVeKanaliSifirla();
                return true;
            case KATEGORI_VE_DIZI:
                kategoriKapatVeDiziSifirla();
                return true;
            case ICERIK_KATEGORI_KANAL:
                icerikKapatVeKategoriSifirla(false);
                return true;
            case ICERIK_KATEGORI_DIZI:
                icerikKapatVeKategoriSifirla(true);
                return true;
            case ARAMA:
                aramaKapat();
                return true;
            case AYARLAR:
                boolean ayarlarIslendi = ayarlarPaneli.handleBack();
                if (!ayarlarIslendi) ayarlarPaneliKapat();
                return true;
            default:
                if (oynaticiBar != null && oynaticiBar.isVisible()) {
                    oynaticiBar.gizle();
                    guncelleKisayol();
                    return true;
                }
                System.out.println("🔙 HICBIRI durumunda back, işlem yok");
                return false;
        }
    }

    public void kanalPaneliAc() {
        kanalPaneli.setMarginStart(0);
        View v = kanalPaneli.getPanelView();
        if (v != null) v.setVisibility(View.VISIBLE);
        kanalPaneli.goster();
        aktifPanel = AktifPanel.SADECE_KANAL;
        if (kanalPaneli.getAdapter() != null) kanalPaneli.getAdapter().setPanelAcik(true);
        kanalPaneli.aktifKanalaFocusVer();
        if (anakontrol != null) anakontrol.metadataPaneliMarginGuncelle();
        if (metadataPanel != null && kanalPaneli.getAdapter() != null) {
            kanalPaneli.getAdapter().ilkFocusKanaliniGonder();
        }
        guncelleKisayol();
    }

    public void kanalPaneliKapat() {
        System.out.println("📺 kanalPaneliKapat()");
        aktifPanel = AktifPanel.HICBIRI;
        View v = kanalPaneli.getPanelView();
        if (v != null) {
            v.clearFocus();
            v.setVisibility(View.GONE);
        }
        kanalPaneli.gizle();
        if (kanalPaneli.getAdapter() != null) kanalPaneli.getAdapter().setPanelAcik(false);
        kanalPaneli.setMarginStart(0);
        activity.findViewById(android.R.id.content).requestFocus();
        if (metadataPanel != null && !Icerikpaneli.TIP_CANLI_TV.equals(aktifIcerikTipi))
            metadataPanel.gizle();
        if (anakontrol != null) anakontrol.metadataPaneliMarginGuncelle();
        guncelleKisayol();
    }

    private void kategoriAcVeKanaliKaydır() {
        com.example.livetvapp.Channel focusdakiKanal = kanalPaneli.getFocusdakiKanal();
        View vk = kategoriPaneli.getPanelView();
        if (vk != null) vk.setVisibility(View.VISIBLE);
        kategoriPaneli.setMarginStart(0);
        kategoriPaneli.goster();
        kanalPaneli.setMarginStart(KATEGORI_GENISLIK);
        aktifPanel = AktifPanel.KATEGORI_VE_KANAL;
        if (kanalPaneli.getAdapter() != null) kanalPaneli.getAdapter().setPanelAcik(true);
        if (metadataPanel != null && !Icerikpaneli.TIP_CANLI_TV.equals(aktifIcerikTipi))
            metadataPanel.gizle();
        if (anakontrol != null && anakontrol.isKanalFavoridenAcildi()) {
            kategoriPaneli.kategoriAdınaFocusVer(
                    com.example.livetvapp.database.FavorilerYoneticisi.FAVORI_KATEGORI_ADI);
        } else if (focusdakiKanal != null && focusdakiKanal.getCategory() != null) {
            kategoriPaneli.kategoriAdınaFocusVer(focusdakiKanal.getCategory());
        } else {
            kategoriPaneli.aktifKategoriyeFocusVer();
        }
        if (anakontrol != null) anakontrol.metadataPaneliMarginGuncelle();
        guncelleKisayol();
    }

    private void kategoriKapatVeKanaliSifirla() {
        kategoriPaneli.gizle();
        View vk = kategoriPaneli.getPanelView();
        if (vk != null) vk.setVisibility(View.GONE);
        kanalPaneli.setMarginStart(0);
        aktifPanel = AktifPanel.SADECE_KANAL;
        kanalPaneli.aktifKanalaFocusVer();
        if (kanalPaneli.getAdapter() != null) kanalPaneli.getAdapter().setPanelAcik(true);
        if (anakontrol != null) anakontrol.metadataPaneliMarginGuncelle();
        guncelleKisayol();
    }

    private void icerikAcVeHepsiniKaydır(boolean diziModu) {
        View vi = icerikPaneli.getPanelView();
        if (vi != null) vi.setVisibility(View.VISIBLE);
        icerikPaneli.goster();
        kategoriPaneli.setMarginStart(ICERIK_GENISLIK);
        if (diziModu) {
            diziPaneli.setMarginStart(ICERIK_GENISLIK + KATEGORI_GENISLIK);
            aktifPanel = AktifPanel.ICERIK_KATEGORI_DIZI;
        } else {
            kanalPaneli.setMarginStart(ICERIK_GENISLIK + KATEGORI_GENISLIK);
            aktifPanel = AktifPanel.ICERIK_KATEGORI_KANAL;
        }
        icerikPaneli.aktifPozisyonaFocusVer();
        if (anakontrol != null) anakontrol.metadataPaneliMarginGuncelle();
        guncelleKisayol();
    }

    private void icerikKapatVeKategoriSifirla(boolean diziModu) {
        icerikPaneli.gizle();
        View vi = icerikPaneli.getPanelView();
        if (vi != null) vi.setVisibility(View.GONE);
        kategoriPaneli.setMarginStart(0);
        if (diziModu) {
            diziPaneli.setMarginStart(KATEGORI_GENISLIK);
            aktifPanel = AktifPanel.KATEGORI_VE_DIZI;
            if (diziPaneli.getAdapter() != null) diziPaneli.getAdapter().setPanelAcik(true);
            kategoriPaneli.ilkKategoriyeFocusVer();
        } else {
            kanalPaneli.setMarginStart(KATEGORI_GENISLIK);
            aktifPanel = AktifPanel.KATEGORI_VE_KANAL;
            kategoriPaneli.ilkKategoriyeFocusVer();
        }
        if (anakontrol != null) anakontrol.metadataPaneliMarginGuncelle();
        guncelleKisayol();
    }

    public void diziPaneliKapat() {
        aktifPanel = AktifPanel.HICBIRI;
        View v = diziPaneli.getPanelView();
        if (v != null) {
            v.clearFocus();
            v.setVisibility(View.GONE);
        }
        diziPaneli.gizle();
        if (diziPaneli.getAdapter() != null) diziPaneli.getAdapter().setPanelAcik(false);
        diziPaneli.setMarginStart(0);
        activity.findViewById(android.R.id.content).requestFocus();
        if (metadataPanel != null && !Icerikpaneli.TIP_CANLI_TV.equals(aktifIcerikTipi))
            metadataPanel.gizle();
        if (anakontrol != null) anakontrol.metadataPaneliMarginGuncelle();
        guncelleKisayol();
    }

    private void kategoriAcVeDiziKaydır() {
        View vk = kategoriPaneli.getPanelView();
        if (vk != null) vk.setVisibility(View.VISIBLE);
        kategoriPaneli.setMarginStart(0);
        kategoriPaneli.goster();
        diziPaneli.setMarginStart(KATEGORI_GENISLIK);
        aktifPanel = AktifPanel.KATEGORI_VE_DIZI;
        if (metadataPanel != null && !Icerikpaneli.TIP_CANLI_TV.equals(aktifIcerikTipi))
            metadataPanel.gizle();
        String aktifDiziKategori = diziPaneli.getAktifKategori();
        if (aktifDiziKategori != null && !aktifDiziKategori.isEmpty()) {
            kategoriPaneli.kategoriAdınaFocusVer(aktifDiziKategori);
        } else {
            kategoriPaneli.aktifKategoriyeFocusVer();
        }
        if (anakontrol != null) anakontrol.metadataPaneliMarginGuncelle();
        guncelleKisayol();
    }

    private void kategoriKapatVeDiziSifirla() {
        kategoriPaneli.gizle();
        View vk = kategoriPaneli.getPanelView();
        if (vk != null) vk.setVisibility(View.GONE);
        diziPaneli.setMarginStart(0);
        aktifPanel = AktifPanel.SADECE_DIZI;
        if (diziPaneli.getAdapter() != null) diziPaneli.getAdapter().setPanelAcik(true);
        RecyclerView rv = diziPaneli.getRecyclerView();
        if (rv != null) {
            rv.postDelayed(() -> {
                rv.requestFocus();
                DiziAdapter adapter = (DiziAdapter) rv.getAdapter();
                if (adapter != null) {
                    adapter.ilkFocusDiziAdiniGonder();
                }
            }, 100);
        }
        if (anakontrol != null) anakontrol.metadataPaneliMarginGuncelle();
        guncelleKisayol();
    }

    private void focusDiziPanelineVer() {
        focusDiziPanelineVer(true);
    }

    private void focusDiziPanelineVer(boolean metadataTetikle) {
        RecyclerView rv = diziPaneli.getRecyclerView();
        if (rv != null) {
            rv.postDelayed(() -> {
                RecyclerView.ViewHolder holder = rv.findViewHolderForAdapterPosition(0);
                if (holder != null && holder.itemView != null) {
                    if (!metadataTetikle) {
                        panelYeniAcildi = true;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            panelYeniAcildi = false;
                        }, 500);
                    }
                    holder.itemView.requestFocus();
                } else {
                    rv.requestFocus();
                }
            }, 100);
        }
    }

    public void ayarlarPaneliAc() {
        System.out.println("⚙️ ayarlarPaneliAc()");
        if (oynaticiBar != null && oynaticiBar.isVisible()) {
            oynaticiBar.gizle();
        }
        icerikPaneli.gizle();
        View vi = icerikPaneli.getPanelView();
        if (vi != null) {
            vi.clearFocus();
            vi.setVisibility(View.GONE);
        }
        kategoriPaneli.gizle();
        View vk = kategoriPaneli.getPanelView();
        if (vk != null) {
            vk.clearFocus();
            vk.setVisibility(View.GONE);
        }
        kanalPaneli.gizle();
        View vKanal = kanalPaneli.getPanelView();
        if (vKanal != null) {
            vKanal.clearFocus();
            vKanal.setVisibility(View.GONE);
        }
        diziPaneli.gizle();
        View vDizi = diziPaneli.getPanelView();
        if (vDizi != null) {
            vDizi.clearFocus();
            vDizi.setVisibility(View.GONE);
        }
        kanalPaneli.setMarginStart(0);
        kategoriPaneli.setMarginStart(0);
        diziPaneli.setMarginStart(0);
        setPanelFocusable(icerikPaneli.getPanelView(), false);
        setPanelFocusable(kategoriPaneli.getPanelView(), false);
        setPanelFocusable(kanalPaneli.getPanelView(), false);
        setPanelFocusable(diziPaneli.getPanelView(), false);
        View v = ayarlarPaneli.getPanelView();
        if (v != null) v.setVisibility(View.VISIBLE);
        ayarlarPaneli.goster();
        aktifPanel = AktifPanel.AYARLAR;
        guncelleKisayol();
    }

    private void ayarlarPaneliKapat() {
        System.out.println("⚙️ ayarlarPaneliKapat()");
        ayarlarPaneliKapatGercek();
    }

    private void ayarlarPaneliKapatGercek() {
        View v = ayarlarPaneli.getPanelView();
        if (v != null) {
            v.clearFocus();
            v.setVisibility(View.GONE);
        }
        ayarlarPaneli.gizle();
        setPanelFocusable(icerikPaneli.getPanelView(), true);
        setPanelFocusable(kategoriPaneli.getPanelView(), true);
        setPanelFocusable(kanalPaneli.getPanelView(), true);
        setPanelFocusable(diziPaneli.getPanelView(), true);
        aktifPanel = AktifPanel.HICBIRI;
        activity.findViewById(android.R.id.content).requestFocus();
        if (ayarlarKapandiCallback != null) {
            ayarlarKapandiCallback.run();
        }
        guncelleKisayol();
    }

    private void setPanelFocusable(View panelView, boolean focusable) {
        if (panelView == null) return;
        panelView.setFocusable(focusable);
        panelView.setFocusableInTouchMode(focusable);
        if (panelView instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) panelView;
            for (int i = 0; i < group.getChildCount(); i++) {
                setPanelFocusable(group.getChildAt(i), focusable);
            }
        }
    }

    private boolean ayarlarPaneliKontrol(int keyCode, KeyEvent event) {
        System.out.println("⚙️ ayarlarPaneliKontrol: keyCode=" + keyCode + " | mevcutMenu=" + ayarlarPaneli.getMevcutMenu());
        AyarlarPaneli.MenuSeviyesi menu = ayarlarPaneli.getMevcutMenu();
        if (menu == AyarlarPaneli.MenuSeviyesi.M3U_DURUM_PANELI) {
            com.example.livetvapp.paneller.M3UDurumPaneli durumPaneli =
                    ayarlarPaneli.getM3UDurumPaneli();
            System.out.println("🔍 M3U_DURUM_PANELI | durumPaneli=" + (durumPaneli != null ? "VAR" : "NULL")
                    + " | visible=" + (durumPaneli != null && durumPaneli.isVisible())
                    + " | ismAcik=" + (durumPaneli != null && durumPaneli.isIsmDegistirAcik()));
            if (durumPaneli != null && durumPaneli.isVisible()) {
                if (durumPaneli.isIsmDegistirAcik()) return false;
                return durumPaneli.handleKey(keyCode);
            }
            return false;
        }
        if (menu == AyarlarPaneli.MenuSeviyesi.GIZLE_PANELI) {
            
            if (aktifPanel != AktifPanel.AYARLAR_GIZLE) {
                aktifPanel = AktifPanel.AYARLAR_GIZLE;
                guncelleKisayol();
            }
            GizlePaneli gizlePaneli = ayarlarPaneli.getGizlePaneli();
            if (gizlePaneli == null) return false;
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    return gizlePaneli.dpadYukari();
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    return gizlePaneli.dpadAsagi();
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    return gizlePaneli.handleSolSag(true);
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    return gizlePaneli.handleSolSag(false);
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    GizlePaneli gizlePaneliCenter = ayarlarPaneli.getGizlePaneli();
                    if (gizlePaneliCenter != null) {
                        com.example.livetvapp.database.GizleItem focusItem = gizlePaneliCenter.getFocusItem();
                        if (focusItem != null) {
                            int tip = focusItem.getTip();
                            if (tip == com.example.livetvapp.database.GizleItem.TIP_M3U
                                    || tip == com.example.livetvapp.database.GizleItem.TIP_KATEGORI) {
                                gizlePaneliCenter.centerTiklandi();
                                return true;
                            }
                        }
                    }
                    return false;
                case KeyEvent.KEYCODE_BACK:
                case KeyEvent.KEYCODE_ESCAPE:
                    boolean islendi = ayarlarPaneli.handleBack();
                    if (!islendi) {
                        ayarlarPaneliKapat();
                    } else {
                        
                        if (aktifPanel == AktifPanel.AYARLAR_GIZLE || aktifPanel == AktifPanel.AYARLAR_SIRALAMA) {
                            aktifPanel = AktifPanel.AYARLAR;
                            guncelleKisayol();
                        }
                    }
                    return true;
            }
            return false;
        }
        if (menu == AyarlarPaneli.MenuSeviyesi.SIRALAMA_PANELI) {
            
            if (aktifPanel != AktifPanel.AYARLAR_SIRALAMA) {
                aktifPanel = AktifPanel.AYARLAR_SIRALAMA;
                guncelleKisayol();
            }
            SiralamaPaneli siralamaPaneli = ayarlarPaneli.getSiralamaPaneli();
            if (siralamaPaneli == null) return false;
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    return false;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    return false;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    return false;
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    View siraFocused = activity.getCurrentFocus();
                    System.out.println("⚙️ SIRALAMA CENTER | focused=" + (siraFocused != null ? siraFocused.getClass().getSimpleName() : "null"));
                    if (siraFocused instanceof Button) {
                        System.out.println("⚙️ SIRALAMA CENTER → performClick()");
                        siraFocused.performClick();
                        return true;
                    }
                    System.out.println("⚙️ SIRALAMA CENTER → Button değil, return false");
                    return false;
                case KeyEvent.KEYCODE_BACK:
                case KeyEvent.KEYCODE_ESCAPE:
                    boolean siraIslendi = ayarlarPaneli.handleBack();
                    if (!siraIslendi) {
                        ayarlarPaneliKapat();
                    } else {
                        
                        if (aktifPanel == AktifPanel.AYARLAR_SIRALAMA) {
                            aktifPanel = AktifPanel.AYARLAR;
                            guncelleKisayol();
                        }
                    }
                    return true;
            }
            return false;
        }
        if (menu == AyarlarPaneli.MenuSeviyesi.XTREAM_MENU) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    return ayarlarPaneli.panelFocusYukari();
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    return ayarlarPaneli.panelFocusAsagi();
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    return false;
                case KeyEvent.KEYCODE_BACK:
                case KeyEvent.KEYCODE_ESCAPE:
                    boolean islendi = ayarlarPaneli.handleBack();
                    if (!islendi) ayarlarPaneliKapat();
                    return true;
            }
            return false;
        }
        if (menu == AyarlarPaneli.MenuSeviyesi.M3U_SILME) {
            View focusedView = activity.getCurrentFocus();
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    return false;
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    if (focusedView instanceof Button) {
                        focusedView.performClick();
                        return true;
                    }
                    return false;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (focusedView != null) {
                        try {
                            android.view.ViewParent parent = focusedView.getParent();
                            while (parent != null) {
                                if (parent instanceof RecyclerView) return true;
                                if (parent instanceof View) parent = ((View) parent).getParent();
                                else break;
                            }
                        } catch (Exception e) {
                        }
                    }
                    return false;
                case KeyEvent.KEYCODE_BACK:
                case KeyEvent.KEYCODE_ESCAPE:
                    boolean islendi = ayarlarPaneli.handleBack();
                    if (!islendi) ayarlarPaneliKapat();
                    return true;
            }
            return false;
        }
        View focusedView = activity.getCurrentFocus();
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                return ayarlarPaneli.panelFocusYukari();
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return ayarlarPaneli.panelFocusAsagi();
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return true;
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (focusedView instanceof Button) {
                    focusedView.performClick();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
                boolean geriIslendi = ayarlarPaneli.handleBack();
                if (!geriIslendi) ayarlarPaneliKapat();
                return true;
        }
        return false;
    }

    public void panelDurumuSifirla() {
        System.out.println("🔄 panelDurumuSifirla()");
        aktifPanel = AktifPanel.HICBIRI;
        kanalPaneli.setMarginStart(0);
        kategoriPaneli.setMarginStart(0);
        diziPaneli.setMarginStart(0);
        setPanelFocusable(icerikPaneli.getPanelView(), true);
        setPanelFocusable(kategoriPaneli.getPanelView(), true);
        setPanelFocusable(kanalPaneli.getPanelView(), true);
        setPanelFocusable(diziPaneli.getPanelView(), true);
        guncelleKisayol();
    }

    public void kanalPaneliAramadanAc(com.example.livetvapp.Channel hedefKanal) {
        kanalPaneli.setMarginStart(0);
        View v = kanalPaneli.getPanelView();
        if (v != null) v.setVisibility(View.VISIBLE);
        kanalPaneli.goster();
        aktifPanel = AktifPanel.SADECE_KANAL;
        if (kanalPaneli.getAdapter() != null) kanalPaneli.getAdapter().setPanelAcik(true);
        if (anakontrol != null) anakontrol.metadataPaneliMarginGuncelle();
        guncelleKisayol();
        if (hedefKanal == null) return;
        if (kanalPaneli.getAdapter() != null) {
            kanalPaneli.getAdapter().setAktifKanalUrl(hedefKanal.getUrl());
        }
        RecyclerView rv = kanalPaneli.getRecyclerView();
        if (rv == null) return;
        rv.postDelayed(() -> {
            int poz = kanalPaneli.getAdapter() != null
                    ? kanalPaneli.getAdapter().getAktifPozisyon() : 0;
            if (poz < 0) poz = 0;
            final int hedefPoz = poz;
            rv.scrollToPosition(hedefPoz);
            rv.postDelayed(() -> {
                RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(hedefPoz);
                if (vh != null && vh.itemView != null) {
                    vh.itemView.requestFocus();
                } else {
                    rv.requestFocus();
                }
                if (metadataPanel != null) {
                    metadataPanel.focusDegisti(hedefKanal.getName(), aktifIcerikTipi);
                }
            }, 100);
        }, 150);
    }

    public void diziPaneliAramadanAc(String diziAdi) {
        diziPaneli.setMarginStart(0);
        View v = diziPaneli.getPanelView();
        if (v != null) v.setVisibility(View.VISIBLE);
        diziPaneli.goster();
        aktifPanel = AktifPanel.SADECE_DIZI;
        if (diziPaneli.getAdapter() != null) diziPaneli.getAdapter().setPanelAcik(true);
        if (anakontrol != null) anakontrol.metadataPaneliMarginGuncelle();
        guncelleKisayol();
        if (metadataPanel != null && diziAdi != null) {
            metadataPanel.focusDegisti(diziAdi, Icerikpaneli.TIP_DIZI);
        }
        RecyclerView rv = diziPaneli.getRecyclerView();
        if (rv == null || diziAdi == null) return;
        rv.postDelayed(() -> {
            com.example.livetvapp.Adapter.DiziAdapter diziAdapter =
                    (com.example.livetvapp.Adapter.DiziAdapter) rv.getAdapter();
            if (diziAdapter == null) return;
            int poz = diziAdapter.diziAdindanPozisyonBul(diziAdi);
            rv.scrollToPosition(poz);
            rv.postDelayed(() -> {
                RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(poz);
                if (vh != null && vh.itemView != null) vh.itemView.requestFocus();
                else rv.requestFocus();
            }, 100);
        }, 300);
    }

    private AramaPaneli aramaPaneli;

    public void setAramaPaneli(AramaPaneli p) {
        this.aramaPaneli = p;
    }

    public void aramaPaneliAc() {
        if (aramaPaneli == null) return;
        aktifPanel = AktifPanel.ARAMA;
        aramaPaneli.goster();
        guncelleKisayol();
    }

    private void aramaKapat() {
        if (aramaPaneli != null) aramaPaneli.gizle();
        aktifPanel = AktifPanel.HICBIRI;
        activity.findViewById(android.R.id.content).requestFocus();
        guncelleKisayol();
    }


    public AktifPanel getAktifPanel() {
        return aktifPanel;
    }

    public void aktifPanelAyarla(AktifPanel yeniPanel) {
        this.aktifPanel = yeniPanel;
        guncelleKisayol();
    }
}