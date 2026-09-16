package com.example.livetvapp.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.livetvapp.R;
import com.example.livetvapp.database.XtreamBilgiCekici;
import com.example.livetvapp.database.M3UItem;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public class M3UDurumAdapter extends RecyclerView.Adapter<M3UDurumAdapter.KartVH> {
    private static final int RENK_DOSYA_ARKAPLAN       = 0xFF1A2A3A;
    private static final int RENK_DOSYA_FOCUS_ARKAPLAN = 0x88FF0000 ;
    private static final int RENK_DOSYA_BTN_GUN        = 0xFF2A4A72;
    private static final int RENK_DOSYA_BTN_ISM        = 0xFF2A4A72;
    private static final int RENK_AKTIF_ARKAPLAN       = 0xFF1A3A5A;
    private static final int RENK_FOCUS_ARKAPLAN       = 0x88FF0000 ;
    private static final int RENK_AKTIF_BTN_GUN        = 0xFF2A4A72;
    private static final int RENK_AKTIF_BTN_ISM        = 0xFF2A4A72;
    private static final int RENK_BTN_FOCUS_GUN = 0xFF3A6A92;
    private static final int RENK_BTN_FOCUS_ISM = 0xFF3A6A92;
    public interface OnKartAksiyonListener {
        void onGuncelle(int pozisyon);
        void onIsmDegistir(int pozisyon);
    }
    private OnKartAksiyonListener aksiyonListener;
    public void setOnKartAksiyonListener(OnKartAksiyonListener l) { this.aksiyonListener = l; }
    private int focusKartPoz  = -1;
    private int focusButonPoz = -1;
    public void setFocusKart(int kartPoz) {
        int eski = focusKartPoz;
        focusKartPoz  = kartPoz;
        focusButonPoz = -1;
        if (eski >= 0)    notifyItemChanged(eski);
        if (kartPoz >= 0) notifyItemChanged(kartPoz);
    }
    public void setFocusButon(int kartPoz, int butonPoz) {
        int eskiKart = focusKartPoz;
        focusKartPoz  = kartPoz;
        focusButonPoz = butonPoz;
        if (eskiKart >= 0 && eskiKart != kartPoz) notifyItemChanged(eskiKart);
        if (kartPoz >= 0) notifyItemChanged(kartPoz);
    }
    public void focusTemizle() {
        int eski = focusKartPoz;
        focusKartPoz  = -1;
        focusButonPoz = -1;
        if (eski >= 0) notifyItemChanged(eski);
    }
    public static class KartVeri {
        public M3UItem m3u;
        public boolean yukleniyor         = true;
        public boolean yenileniyor        = false;
        public boolean kanalGuncelleniyor = false;
        public String  kanalGuncelleSonuc = null;
        public XtreamBilgiCekici.XtreamHesapBilgisi bilgi = null;
        public String sonGuncelleme = null;
        public KartVeri(M3UItem m3u) { this.m3u = m3u; }
        public boolean farkliMi(XtreamBilgiCekici.XtreamHesapBilgisi yeni) {
            if (bilgi == null && yeni == null) return false;
            if (bilgi == null || yeni == null) return true;
            if (bilgi.basarili() != yeni.basarili()) return true;
            if (!bilgi.basarili()) return !eq(bilgi.hata, yeni.hata);
            return !eq(bilgi.durum, yeni.durum)
                    || bilgi.aktifBaglanti != yeni.aktifBaglanti
                    || bilgi.maxBaglanti  != yeni.maxBaglanti
                    || !eq(bilgi.bitisTarihi, yeni.bitisTarihi)
                    || !eq(bilgi.kullanici, yeni.kullanici)
                    || !eq(bilgi.sunucu, yeni.sunucu)
                    || !eq(bilgi.olusturmaTarihi, yeni.olusturmaTarihi);
        }
        private static boolean eq(String a, String b) {
            if (a == null && b == null) return true;
            if (a == null || b == null) return false;
            return a.equals(b);
        }
    }
    private final List<KartVeri> liste = new ArrayList<>();
    private final SimpleDateFormat saatFmt =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    public void setListe(List<M3UItem> m3uListesi) {
        liste.clear();
        focusKartPoz  = -1;
        focusButonPoz = -1;
        for (M3UItem m : m3uListesi) liste.add(new KartVeri(m));
        notifyDataSetChanged();
    }
    public void yenilemeBasladi(int pozisyon) {
        if (pozisyon < 0 || pozisyon >= liste.size()) return;
        KartVeri kart = liste.get(pozisyon);
        if (kart.yukleniyor) return;
        kart.yenileniyor = true;
        notifyItemChanged(pozisyon);
    }
    public void bilgiGuncelle(int pozisyon, XtreamBilgiCekici.XtreamHesapBilgisi yeni) {
        if (pozisyon < 0 || pozisyon >= liste.size()) return;
        KartVeri kart = liste.get(pozisyon);
        boolean degisti = kart.farkliMi(yeni);
        kart.bilgi         = yeni;
        kart.yukleniyor    = false;
        kart.yenileniyor   = false;
        kart.sonGuncelleme = saatFmt.format(new Date());
        if (degisti || kart.sonGuncelleme != null) notifyItemChanged(pozisyon);
    }
    public void kanalGuncellemeyeBasladi(int pozisyon) {
        if (pozisyon < 0 || pozisyon >= liste.size()) return;
        liste.get(pozisyon).kanalGuncelleniyor = true;
        liste.get(pozisyon).kanalGuncelleSonuc = null;
        notifyItemChanged(pozisyon);
    }
    public void kanalGuncellemeSonucuGoster(int pozisyon, String sonuc) {
        if (pozisyon < 0 || pozisyon >= liste.size()) return;
        liste.get(pozisyon).kanalGuncelleniyor = false;
        liste.get(pozisyon).kanalGuncelleSonuc = sonuc;
        notifyItemChanged(pozisyon);
    }
    @NonNull
    @Override
    public KartVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_m3u_durum_kart, parent, false);
        return new KartVH(v);
    }
    @Override
    public void onBindViewHolder(@NonNull KartVH h, int position) {
        h.aksiyonListener = aksiyonListener;
        boolean kartFocus = (position == focusKartPoz);
        int     butonPoz  = kartFocus ? focusButonPoz : -1;
        h.bind(liste.get(position), kartFocus, butonPoz);
    }
    @Override
    public int getItemCount() { return liste.size(); }
    static class KartVH extends RecyclerView.ViewHolder {
        TextView tvSonGuncelleme, tvM3UAdi, tvDurum, tvBaglanti, tvBitis;
        TextView tvKullanici, tvSunucu, tvOlusturulma, tvHata;
        TextView tvKanalSayisi, tvGuncelleSonuc, tvDosyaUyari;
        Button   btnGuncelle, btnIsmDegistir;
        OnKartAksiyonListener aksiyonListener;
        KartVH(View v) {
            super(v);
            tvSonGuncelleme  = v.findViewById(R.id.tvSonGuncelleme);
            tvM3UAdi         = v.findViewById(R.id.tvM3UAdi);
            tvDurum          = v.findViewById(R.id.tvDurum);
            tvBaglanti       = v.findViewById(R.id.tvBaglanti);
            tvBitis          = v.findViewById(R.id.tvBitis);
            tvKullanici      = v.findViewById(R.id.tvKullanici);
            tvSunucu         = v.findViewById(R.id.tvSunucu);
            tvOlusturulma    = v.findViewById(R.id.tvOlusturulma);
            tvHata           = v.findViewById(R.id.tvHata);
            tvKanalSayisi    = v.findViewById(R.id.tvKanalSayisi);
            tvGuncelleSonuc  = v.findViewById(R.id.tvGuncelleSonuc);
            tvDosyaUyari     = v.findViewById(R.id.tvDosyaUyari);
            btnGuncelle      = v.findViewById(R.id.btnGuncelle);
            btnIsmDegistir   = v.findViewById(R.id.btnIsmDegistir);
        }
        void bind(KartVeri kart, boolean kartFocus, int butonPoz) {
            boolean dosyaMi = kart.m3u.isLocalFile();
            tvM3UAdi.setText(kart.m3u.getName());
            if (tvKanalSayisi != null) {
                int sayi = kart.m3u.getChannelCount();
                tvKanalSayisi.setText(sayi > 0 ? String.valueOf(sayi) : "-");
            }
            if (kartFocus) {
                itemView.setBackgroundColor(
                        dosyaMi ? RENK_DOSYA_FOCUS_ARKAPLAN : RENK_FOCUS_ARKAPLAN);
            } else {
                itemView.setBackgroundColor(
                        dosyaMi ? RENK_DOSYA_ARKAPLAN : RENK_AKTIF_ARKAPLAN);
            }
            if (kart.yukleniyor) {
                tvSonGuncelleme.setText("Bilgi alınıyor...");
            } else if (kart.yenileniyor) {
                tvSonGuncelleme.setText("Yenileniyor...");
            } else if (kart.sonGuncelleme != null) {
                tvSonGuncelleme.setText("Son güncelleme: " + kart.sonGuncelleme);
            }
            if (kart.yukleniyor) {
                tvDurum.setText("...");
                tvDurum.setBackgroundColor(0xCC444444);
                tvBaglanti.setText("-");
                tvBitis.setText("-");
                tvKullanici.setText("-");
                tvSunucu.setText("-");
                tvOlusturulma.setText("-");
                tvHata.setVisibility(View.GONE);
                butonlarAyarla(kart, dosyaMi, butonPoz);
                return;
            }
            XtreamBilgiCekici.XtreamHesapBilgisi b = kart.bilgi;
            if (b == null || !b.basarili()) {
                tvDurum.setText("?");
                tvDurum.setBackgroundColor(0xCC555555);
                tvBaglanti.setText("-");
                tvBitis.setText("-");
                tvKullanici.setText("-");
                tvSunucu.setText("-");
                tvOlusturulma.setText("-");
                if (!dosyaMi) {
                    tvHata.setVisibility(View.VISIBLE);
                    tvHata.setText(b != null ? b.hata : "Bilgi alinamadi");
                } else {
                    tvHata.setVisibility(View.GONE);
                }
                butonlarAyarla(kart, dosyaMi, butonPoz);
                return;
            }
            tvHata.setVisibility(View.GONE);
            tvDurum.setText(durumMetni(b.durum));
            tvDurum.setBackgroundColor(durumRenk(b.durum));
            tvBaglanti.setText(b.aktifBaglanti + "/" + b.maxBaglanti);
            tvBitis.setText(b.bitisTarihi != null ? b.bitisTarihi.replace(" ", "\n") : "-");
            tvKullanici.setText(b.kullanici != null ? b.kullanici : "-");
            tvSunucu.setText(b.sunucu != null ? b.sunucu : "-");
            tvOlusturulma.setText(b.olusturmaTarihi != null ? b.olusturmaTarihi : "-");
            butonlarAyarla(kart, dosyaMi, butonPoz);
        }
        private void butonlarAyarla(KartVeri kart, boolean dosyaMi, int butonPoz) {
            if (btnGuncelle != null) {
                btnGuncelle.setFocusable(false);
                btnGuncelle.setFocusableInTouchMode(false);
                boolean guncelleAktif = !dosyaMi && !kart.kanalGuncelleniyor;
                boolean gunFocus = (butonPoz == 0);
                btnGuncelle.setEnabled(guncelleAktif);
                btnGuncelle.setText(kart.kanalGuncelleniyor ? "↻ Güncelleniyor..." : "↻ Güncelle");
                btnGuncelle.setTextColor(0xFFFFFFFF);  
                btnGuncelle.setBackgroundColor(gunFocus
                        ? RENK_BTN_FOCUS_GUN
                        : (dosyaMi ? RENK_DOSYA_BTN_GUN : RENK_AKTIF_BTN_GUN));
                btnGuncelle.setAlpha(gunFocus ? 1f
                        : (dosyaMi ? 0.35f : (kart.kanalGuncelleniyor ? 0.6f : 1f)));
                btnGuncelle.setOnClickListener(null);
            }
            if (btnIsmDegistir != null) {
                btnIsmDegistir.setFocusable(false);
                btnIsmDegistir.setFocusableInTouchMode(false);
                boolean ismFocus = (butonPoz == 1);
                btnIsmDegistir.setTextColor(0xFFFFFFFF);  
                btnIsmDegistir.setAlpha(1f);
                btnIsmDegistir.setBackgroundColor(ismFocus
                        ? RENK_BTN_FOCUS_ISM
                        : (dosyaMi ? RENK_DOSYA_BTN_ISM : RENK_AKTIF_BTN_ISM));
                btnIsmDegistir.setOnClickListener(null);

            }
            if (btnIsmDegistir != null) {
                btnIsmDegistir.setFocusable(false);
                btnIsmDegistir.setFocusableInTouchMode(false);
                boolean ismFocus = (butonPoz == 1);
                btnIsmDegistir.setTextColor(0xFFFFFFFF);
                btnIsmDegistir.setAlpha(1f);
                btnIsmDegistir.setBackgroundColor(ismFocus
                        ? RENK_BTN_FOCUS_ISM
                        : (dosyaMi ? RENK_DOSYA_BTN_ISM : RENK_AKTIF_BTN_ISM));
                btnIsmDegistir.setOnClickListener(null);
            }
            if (tvGuncelleSonuc != null) {
                if (kart.kanalGuncelleSonuc != null) {
                    tvGuncelleSonuc.setVisibility(View.VISIBLE);
                    tvGuncelleSonuc.setText(kart.kanalGuncelleSonuc);
                    tvGuncelleSonuc.setTextColor(dosyaMi ? 0xFF888888 : 0x88FFFFFF);
                } else {
                    tvGuncelleSonuc.setVisibility(View.GONE);
                }
            }
            if (tvDosyaUyari != null) {
                tvDosyaUyari.setVisibility(dosyaMi ? View.VISIBLE : View.GONE);
            }
        }
        private String durumMetni(String durum) {
            if (durum == null) return "Bilinmiyor";
            switch (durum.toLowerCase()) {
                case "active":   return "Aktif";
                case "banned":   return "Yasakli";
                case "disabled": return "Devre Disi";
                case "expired":  return "Suresi Doldu";
                default:         return durum;
            }
        }
        private int durumRenk(String durum) {
            if (durum == null) return 0xCC555555;
            switch (durum.toLowerCase()) {
                case "active":   return 0xCC006600;
                case "banned":   return 0xCC880000;
                case "disabled": return 0xCC555500;
                case "expired":  return 0xCC882200;
                default:         return 0xCC444444;
            }
        }
    }
}