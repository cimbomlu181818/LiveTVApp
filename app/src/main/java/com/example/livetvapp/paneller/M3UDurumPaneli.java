package com.example.livetvapp.paneller;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.livetvapp.Channel;
import com.example.livetvapp.KanalListesi;
import com.example.livetvapp.R;
import com.example.livetvapp.Adapter.M3UDurumAdapter;
import com.example.livetvapp.database.AppDatabase;
import com.example.livetvapp.database.ChannelRepository;
import com.example.livetvapp.database.M3UItem;
import com.example.livetvapp.database.M3UManager;
import com.example.livetvapp.database.XtreamBilgiCekici;
import com.example.livetvapp.database.XtreamCodesManager;
import java.util.List;
import com.example.livetvapp.stalker.StalkerManager;
public class M3UDurumPaneli {
    private static final long YENILEME_SURESI_MS = 30_000L;
    private AppCompatActivity activity;
    private View              panelView;
    private RecyclerView      recyclerView;
    private Button            btnTumunuGuncelle;
    private M3UDurumAdapter   adapter;
    private M3UManager        m3uManager;
    private ChannelRepository channelRepository;
    private boolean ismDegistirAcik = false;
    public boolean isIsmDegistirAcik() { return ismDegistirAcik; }
    private final Handler handler         = new Handler(Looper.getMainLooper());
    private Runnable      yenilemeRunnable;
    private List<M3UItem> m3uListesi;
    private boolean[]     istekDevamEdiyor;
    private String focusMod = "kart";
    private int    focusPoz = 0;
    private int    butonPoz = -1;
    public interface KapandiCallback { void onKapandi(); }
    private KapandiCallback kapandiCallback;
    public void setKapandiCallback(KapandiCallback cb) { this.kapandiCallback = cb; }
    public interface M3UGuncellendiCallback { void onM3UGuncellendi(); }
    private M3UGuncellendiCallback m3uGuncellendiCallback;
    public void setM3UGuncellendiCallback(M3UGuncellendiCallback cb) { this.m3uGuncellendiCallback = cb; }
    public M3UDurumPaneli(AppCompatActivity activity) {
        this.activity          = activity;
        this.m3uManager        = new M3UManager(activity);
        this.channelRepository = new ChannelRepository(activity);
    }
    public void olustur() {
        panelView         = LayoutInflater.from(activity).inflate(R.layout.panel_m3u_durum, null);
        recyclerView      = panelView.findViewById(R.id.m3uDurumRecyclerView);
        btnTumunuGuncelle = panelView.findViewById(R.id.btnTumunuGuncelle);
        btnTumunuGuncelle.setTextColor(0xFFFFFFFF);
        panelView.setVisibility(View.GONE);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        adapter = new M3UDurumAdapter();
        recyclerView.setAdapter(adapter);
        if (btnTumunuGuncelle != null) {
            btnTumunuGuncelle.setFocusable(false);
            btnTumunuGuncelle.setClickable(false);

            btnTumunuGuncelle.setBackgroundColor(0xFF2A4A72);
            btnTumunuGuncelle.setTextColor(0xFFFFFFFF);
        }
        adapter.setOnKartAksiyonListener(new M3UDurumAdapter.OnKartAksiyonListener() {
            @Override public void onGuncelle(int poz)    { kanalListesiGuncelle(poz); }
            @Override public void onIsmDegistir(int poz) { ismDegistirDiyaloguGoster(poz); }
        });
        activity.addContentView(
                panelView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
    }
    public void ac() {
        System.out.println("📺 M3UDurumPaneli.ac() çağrıldı");
        panelView.setVisibility(View.VISIBLE);
        m3uListesi = m3uManager.getAllM3UItems();
        if (m3uListesi == null || m3uListesi.isEmpty()) {
            gizle();
            if (kapandiCallback != null) kapandiCallback.onKapandi();
            return;
        }
        istekDevamEdiyor = new boolean[m3uListesi.size()];
        adapter.setListe(m3uListesi);
        focusMod = "kart";
        focusPoz = 0;
        butonPoz = -1;
        adapter.setFocusKart(0);
        recyclerView.scrollToPosition(0);
        tumunuButonFocusAyarla(false);
        tumunuBilgiGuncelle(true);
        donguBaslat();
    }
    public boolean handleKey(int keyCode) {
        if (m3uListesi == null || m3uListesi.isEmpty()) return false;
        int kartSayisi = m3uListesi.size();
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                if ("buton".equals(focusMod)) {
                    focusMod = "kart";
                    butonPoz = -1;
                    adapter.setFocusKart(focusPoz);
                } else if ("kart".equals(focusMod)) {
                    if (focusPoz > 0) {
                        focusPoz--;
                        adapter.setFocusKart(focusPoz);
                        recyclerView.scrollToPosition(focusPoz);
                    } else {
                        focusMod = "tumunu";
                        adapter.focusTemizle();
                        tumunuButonFocusAyarla(true);
                    }
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if ("tumunu".equals(focusMod)) {
                    focusMod = "kart";
                    focusPoz = 0;
                    tumunuButonFocusAyarla(false);
                    adapter.setFocusKart(0);
                    recyclerView.scrollToPosition(0);
                } else if ("buton".equals(focusMod)) {
                    focusMod = "kart";
                    butonPoz = -1;
                    adapter.setFocusKart(focusPoz);
                } else if ("kart".equals(focusMod)) {
                    if (focusPoz < kartSayisi - 1) {
                        focusPoz++;
                        adapter.setFocusKart(focusPoz);
                        recyclerView.scrollToPosition(focusPoz);
                    }
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if ("kart".equals(focusMod)) {
                    focusMod = "buton";
                    butonPoz = 0;
                    adapter.setFocusButon(focusPoz, 0);
                } else if ("buton".equals(focusMod) && butonPoz == 0) {
                    butonPoz = 1;
                    adapter.setFocusButon(focusPoz, 1);
                    System.out.println("➡️ butonPoz=1 (İsim Değiştir) | focusPoz=" + focusPoz);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if ("buton".equals(focusMod)) {
                    if (butonPoz == 1) {
                        butonPoz = 0;
                        adapter.setFocusButon(focusPoz, 0);
                    } else {
                        focusMod = "kart";
                        butonPoz = -1;
                        adapter.setFocusKart(focusPoz);
                    }
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                System.out.println("🔑 handleKey ENTER | focusMod=" + focusMod + " | butonPoz=" + butonPoz + " | focusPoz=" + focusPoz);
                if ("tumunu".equals(focusMod)) {
                    tumunuKanalGuncelle();
                } else if ("kart".equals(focusMod)) {
                    focusMod = "buton";
                    butonPoz = 0;
                    adapter.setFocusButon(focusPoz, 0);
                }else if ("buton".equals(focusMod)) {
                    if (butonPoz == 0) kanalListesiGuncelle(focusPoz);
                    else               ismDegistirDiyaloguGoster(focusPoz);
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
                if ("buton".equals(focusMod)) {
                    focusMod = "kart";
                    butonPoz = -1;
                    adapter.setFocusKart(focusPoz);
                    return true;
                }
                gizle();
                if (kapandiCallback != null) kapandiCallback.onKapandi();
                return true;
        }
        return false;
    }
    private void tumunuButonFocusAyarla(boolean focus) {
        if (btnTumunuGuncelle == null) return;
        btnTumunuGuncelle.setBackgroundColor(focus ? 0x88FF0000  : 0xFF1A3A5A);
    }
    private void tumunuKanalGuncelle() {
        if (m3uListesi == null) return;
        int guncellenecek = 0;
        for (int i = 0; i < m3uListesi.size(); i++) {
            if (!m3uListesi.get(i).isLocalFile()) {
                kanalListesiGuncelle(i);
                guncellenecek++;
            }
        }
        if (guncellenecek == 0)
            Toast.makeText(activity, "Güncellenebilir M3U yok", Toast.LENGTH_SHORT).show();
    }
    private void donguBaslat() {
        donguDurdur();
        yenilemeRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isVisible()) return;
                tumunuBilgiGuncelle(false);
                handler.postDelayed(this, YENILEME_SURESI_MS);
            }
        };
        handler.postDelayed(yenilemeRunnable, YENILEME_SURESI_MS);
    }
    private void donguDurdur() {
        if (yenilemeRunnable != null) {
            handler.removeCallbacks(yenilemeRunnable);
            yenilemeRunnable = null;
        }
    }
    private void tumunuBilgiGuncelle(boolean ilkYukleme) {
        if (m3uListesi == null) return;
        for (int i = 0; i < m3uListesi.size(); i++) {
            final int poz = i;
            if (istekDevamEdiyor[poz]) continue;
            if (!ilkYukleme) adapter.yenilemeBasladi(poz);
            istekDevamEdiyor[poz] = true;
            channelRepository.loadChannels(m3uListesi.get(poz).getName(), 0,
                    new ChannelRepository.OnChannelsLoadedListener() {
                        @Override
                        public void onChannelsLoaded(List<Channel> channels, boolean fromCache) {
                            List<Channel> ilkOn = channels.size() > 10
                                    ? channels.subList(0, 10) : channels;
                            XtreamBilgiCekici.bilgiCek(ilkOn, bilgi -> {
                                istekDevamEdiyor[poz] = false;
                                adapter.bilgiGuncelle(poz, bilgi);
                            });
                        }
                        @Override
                        public void onError(Exception e) {
                            istekDevamEdiyor[poz] = false;
                            XtreamBilgiCekici.XtreamHesapBilgisi hata =
                                    new XtreamBilgiCekici.XtreamHesapBilgisi();
                            hata.hata = "Kanal listesi okunamadi.";
                            adapter.bilgiGuncelle(poz, hata);
                        }
                    });
        }
    }
    private void kanalListesiGuncelle(int pozisyon) {
        if (m3uListesi == null || pozisyon >= m3uListesi.size()) return;
        M3UItem m3u = m3uListesi.get(pozisyon);
        if (m3u.isLocalFile()) {
            Toast.makeText(activity, "Dosyadan yüklendi — güncellenemez", Toast.LENGTH_SHORT).show();
            return;
        }
        adapter.kanalGuncellemeyeBasladi(pozisyon);
        String url = m3u.getUrl();
        System.out.println("🔍 [XTREAM-GÜN] kanalListesiGuncelle başladı | pozisyon=" + pozisyon
                + " | m3u.getName()='" + m3u.getName() + "' | url='" + url + "'");

        if (url != null && url.startsWith("stalker:")) {
            String portalUrl = url.substring("stalker:".length());

            String portalAdi = m3u.getName().startsWith("Stalker:")
                    ? m3u.getName().substring("Stalker:".length())
                    : m3u.getName();


            new Thread(() -> {
                try {
                    com.example.livetvapp.database.AppDatabase db =
                            com.example.livetvapp.database.AppDatabase.getInstance(activity);
                    List<com.example.livetvapp.stalker.StalkerPortal> portals =
                            db.stalkerPortalDao().getActivePortals();
                    com.example.livetvapp.stalker.StalkerPortal hedef = null;
                    for (com.example.livetvapp.stalker.StalkerPortal p : portals) {
                        if (portalAdi.equals(p.getName())) { hedef = p; break; }
                    }
                    if (hedef == null) {
                        handler.post(() ->
                                adapter.kanalGuncellemeSonucuGoster(pozisyon, "❌ Portal bulunamadı"));
                        return;
                    }
                    final com.example.livetvapp.stalker.StalkerPortal finalPortal = hedef;
                    StalkerManager stalkerManager = new StalkerManager(activity);
                    handler.post(() ->
                            stalkerManager.addPortalAndSync(
                                    m3u.getName(),
                                    finalPortal.getPortalUrl(),
                                    finalPortal.getMacAddress(),
                                    new StalkerManager.SyncListener() {
                                        @Override
                                        public void onSuccess(int channelCount) {
                                            akillıGuncelle(pozisyon, m3u,
                                                    new java.util.ArrayList<>() );
                                            adapter.kanalGuncellemeSonucuGoster(pozisyon,
                                                    "✅ " + channelCount + " içerik güncellendi");
                                            m3uListesi = m3uManager.getAllM3UItems();
                                            if (m3uGuncellendiCallback != null)
                                                m3uGuncellendiCallback.onM3UGuncellendi();
                                        }
                                        @Override
                                        public void onError(String error) {
                                            adapter.kanalGuncellemeSonucuGoster(pozisyon, "❌ " + error);
                                        }
                                    }
                            )
                    );
                } catch (Exception e) {
                    handler.post(() ->
                            adapter.kanalGuncellemeSonucuGoster(pozisyon, "❌ " + e.getMessage()));
                }
            }).start();
            return;
        }

        if (url != null && url.contains("[Xtream]")) {
            String sunucu = url.replace(" [Xtream]", "").trim();
            List<String[]> accounts = XtreamCodesManager.getAccounts(activity);
            String user = null, pass = null;
            for (String[] acc : accounts) {
                if (acc[0].equals(m3u.getName())) {
                    sunucu = acc[1]; user = acc[2]; pass = acc[3];
                    break;
                }
            }
            if (user == null || pass == null) {
                adapter.kanalGuncellemeSonucuGoster(pozisyon, "❌ Hesap bilgisi bulunamadı");
                return;
            }
            final String fSunucu = sunucu, fUser = user, fPass = pass;
            XtreamCodesManager.loadChannels(activity, fSunucu, fUser, fPass, m3u.getName(),
                    new XtreamCodesManager.OnXtreamLoadListener() {
                        @Override public void onStarted() {}
                        @Override public void onProgress(String msg) {}
                        @Override
                        public void onSuccess(List<Channel> channels, String srcName) {
                            akillıGuncelle(pozisyon, m3u, channels);
                        }
                        @Override
                        public void onError(String error) {
                            handler.post(() ->
                                    adapter.kanalGuncellemeSonucuGoster(pozisyon, "❌ " + error));
                        }
                    });
            return;
        }


        if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
            System.out.println("🔍 [XTREAM-GÜN] Normal URL güncelleme başlatılıyor: " + url);
            new Thread(() -> {
                try {
                    List<Channel> yeniKanallar = KanalListesi.getChannelsFromUrl(url);
                    handler.post(() -> {
                        if (yeniKanallar == null || yeniKanallar.isEmpty()) {
                            adapter.kanalGuncellemeSonucuGoster(pozisyon, "❌ Kanal bulunamadı");
                            return;
                        }


                        String[] kimlik = xtreamKimliginiCikar(url);
                        if (kimlik != null && xtreamDenemesiGerekiyor(yeniKanallar)) {
                            System.out.println("🔍 [XTREAM-GÜN] Kalite zayıf → Xtream deneniyor");
                            String server   = kimlik[0];
                            String username = kimlik[1];
                            String password = kimlik[2];
                            XtreamCodesManager.loadChannels(activity, server, username, password,
                                    m3u.getName(),
                                    new XtreamCodesManager.OnXtreamLoadListener() {
                                        @Override public void onStarted() {}
                                        @Override public void onProgress(String msg) {
                                            System.out.println("🔄 [GÜN-Xtream] " + msg);
                                        }
                                        @Override
                                        public void onSuccess(List<Channel> xtreamKanallar, String srcName) {
                                            System.out.println("✅ [XTREAM-GÜN] Xtream başarılı → "
                                                    + xtreamKanallar.size() + " kanal");

                                            XtreamCodesManager.saveAccount(activity,
                                                    m3u.getName(), server, username, password);

                                            handler.post(() -> akillıGuncelle(pozisyon, m3u, xtreamKanallar));
                                        }
                                        @Override
                                        public void onError(String error) {
                                            System.out.println("⚠️ [XTREAM-GÜN] Xtream başarısız → "
                                                    + "normal parse kullanılıyor. Hata: " + error);

                                            handler.post(() -> akillıGuncelle(pozisyon, m3u, yeniKanallar));
                                        }
                                    });
                        } else {

                            akillıGuncelle(pozisyon, m3u, yeniKanallar);
                        }
                    });
                } catch (Exception e) {
                    handler.post(() ->
                            adapter.kanalGuncellemeSonucuGoster(pozisyon, "❌ " + e.getMessage()));
                }
            }).start();
            return;
        }

        System.out.println("❌ [XTREAM-GÜN] Güncellenebilir kaynak yok | url='" + url + "'");
        adapter.kanalGuncellemeSonucuGoster(pozisyon, "❌ Güncellenebilir kaynak yok");
    }


    private boolean xtreamDenemesiGerekiyor(List<Channel> kanallar) {
        if (kanallar == null || kanallar.isEmpty()) return false;
        int toplamKanal = kanallar.size();
        int liveSayisi  = 0;
        java.util.Set<String> kategoriler = new java.util.HashSet<>();
        for (Channel ch : kanallar) {
            if ("LIVE".equals(ch.getContentType())) liveSayisi++;
            if (ch.getCategory() != null && !ch.getCategory().isEmpty())
                kategoriler.add(ch.getCategory());
        }
        double liveOrani = (double) liveSayisi / toplamKanal;
        boolean tumKanallarlive = liveOrani >= 0.95;
        boolean kategoriZayif   = kategoriler.size() <= 3;
        System.out.println("🔍 [KALİTE-GÜN] toplamKanal=" + toplamKanal
                + " liveOrani=" + String.format("%.2f", liveOrani)
                + " kategoriSayisi=" + kategoriler.size()
                + " sonuc=" + (tumKanallarlive || kategoriZayif));
        return tumKanallarlive || kategoriZayif;
    }


    private String[] xtreamKimliginiCikar(String urlStr) {
        try {
            java.net.URL parsedUrl = new java.net.URL(urlStr);
            String server = parsedUrl.getProtocol() + "://" + parsedUrl.getHost()
                    + (parsedUrl.getPort() != -1 ? ":" + parsedUrl.getPort() : "");
            String username = null, password = null;
            String query = parsedUrl.getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("username="))
                        username = param.substring("username=".length());
                    else if (param.startsWith("password="))
                        password = param.substring("password=".length());
                }
            }
            if (username == null || password == null) {
                String path = parsedUrl.getPath();
                String[] parcalar = path.split("/");
                if (parcalar.length >= 3) {
                    String p1 = parcalar[1], p2 = parcalar[2];
                    boolean p1Ok = !p1.isEmpty() && !p1.equals("get.php")
                            && !p1.equals("live") && !p1.equals("movie") && !p1.equals("series");
                    boolean p2Ok = !p2.isEmpty() && !p2.equals("get.php")
                            && !p2.equals("live") && !p2.equals("movie") && !p2.equals("series");
                    if (p1Ok && p2Ok) { username = p1; password = p2; }
                }
            }
            if (username != null && !username.isEmpty()
                    && password != null && !password.isEmpty()) {
                return new String[]{server, username, password};
            }
        } catch (Exception e) {
            System.out.println("⚠️ [xtreamKimliginiCikar-GÜN] " + e.getMessage());
        }
        return null;
    }
    private void akillıGuncelle(int pozisyon, M3UItem m3u, List<Channel> yeniKanallar) {
        if (yeniKanallar == null || yeniKanallar.isEmpty()) {
            adapter.kanalGuncellemeSonucuGoster(pozisyon, "❌ Kanal listesi boş geldi");
            return;
        }
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(activity);
                int eskiSayi = db.channelDao().getChannelCountBySource(m3u.getName());


                List<Channel> eskiKanallar = db.channelDao().getAllChannelsByM3U(m3u.getName());
                java.util.Map<String, Integer> urlPozisyonHaritasi = new java.util.HashMap<>();
                for (Channel ch : eskiKanallar) {
                    if (ch.getUrl() != null) {
                        urlPozisyonHaritasi.put(ch.getUrl(), ch.getPosition());
                    }
                }


                for (Channel ch : yeniKanallar) {
                    ch.setSourceName(m3u.getName());
                    Integer eskiPoz = urlPozisyonHaritasi.get(ch.getUrl());
                    if (eskiPoz != null) {
                        ch.setPosition(eskiPoz);
                    }
                }

                db.channelDao().deleteBySourceName(m3u.getName());
                db.channelDao().insertAll(yeniKanallar);
                int yeniSayi = yeniKanallar.size();
                db.m3uDao().updateChannelCount(m3u.getName(), yeniSayi);
                m3uManager.updateM3U(m3u.getName(), yeniSayi);
                String sonuc = "✅ " + yeniSayi + " kanal (eski: " + eskiSayi + ")";
                handler.post(() -> {
                    adapter.kanalGuncellemeSonucuGoster(pozisyon, sonuc);
                    m3uListesi = m3uManager.getAllM3UItems();
                    if (m3uGuncellendiCallback != null) m3uGuncellendiCallback.onM3UGuncellendi();
                });
            } catch (Exception e) {
                handler.post(() ->
                        adapter.kanalGuncellemeSonucuGoster(pozisyon, "❌ " + e.getMessage()));
            }
        }).start();
    }
    private void ismDegistirDiyaloguGoster(int pozisyon) {
        if (m3uListesi == null || pozisyon >= m3uListesi.size()) return;
        System.out.println("✏️ ismDegistirDiyaloguGoster çağrıldı | pozisyon=" + pozisyon);
        M3UItem m3u = m3uListesi.get(pozisyon);
        android.widget.EditText et = new android.widget.EditText(activity);
        et.setText(m3u.getName());
        et.selectAll();
        et.setSingleLine(true);
        et.setPadding(40, 20, 40, 20);
        new AlertDialog.Builder(activity)
                .setTitle("İsim Değiştir")
                .setMessage("Mevcut: " + m3u.getName())
                .setView(et)
                .setPositiveButton("Kaydet", (dialog, which) -> {
                    String yeniIsim = et.getText().toString().trim();
                    if (yeniIsim.isEmpty()) {
                        Toast.makeText(activity, "İsim boş olamaz", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (yeniIsim.equals(m3u.getName())) {
                        Toast.makeText(activity, "İsim aynı", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String eskiIsim = m3u.getName();
                    boolean basarili = m3uManager.m3uIsminiDegistir(activity, eskiIsim, yeniIsim);
                    if (basarili) {
                        gizliKategoriIsimleriniGuncelle(activity, eskiIsim, yeniIsim);
                        Toast.makeText(activity, "✅ İsim değiştirildi: " + yeniIsim,
                                Toast.LENGTH_SHORT).show();
                        m3uListesi = m3uManager.getAllM3UItems();
                        adapter.setListe(m3uListesi);
                        istekDevamEdiyor = new boolean[m3uListesi.size()];
                        focusMod = "kart";
                        focusPoz = Math.min(pozisyon, m3uListesi.size() - 1);
                        butonPoz = -1;
                        adapter.setFocusKart(focusPoz);
                        if (m3uGuncellendiCallback != null) m3uGuncellendiCallback.onM3UGuncellendi();
                    } else {
                        Toast.makeText(activity, "❌ Bu isim zaten kullanılıyor",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("İptal", null)
                .show();
    }
    private void gizliKategoriIsimleriniGuncelle(android.content.Context ctx,
                                                 String eskiIsim, String yeniIsim) {
        android.content.SharedPreferences prefs =
                ctx.getSharedPreferences("gizli_kategoriler", android.content.Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor ed = prefs.edit();
        java.util.Map<String, ?> hepsi = prefs.getAll();
        for (java.util.Map.Entry<String, ?> entry : hepsi.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(eskiIsim + "|")) {
                String yeniKey = yeniIsim + key.substring(eskiIsim.length());
                ed.remove(key);
                if (Boolean.TRUE.equals(entry.getValue())) {
                    ed.putBoolean(yeniKey, true);
                }
            }
        }
        ed.apply();
        System.out.println("🔑 Gizli kategori anahtarları güncellendi: " + eskiIsim + " → " + yeniIsim);
    }
    public void gizle() {
        System.out.println("📺 M3UDurumPaneli.gizle() çağrıldı");
        donguDurdur();
        if (panelView != null) {
            panelView.setVisibility(View.GONE);
            panelView.clearFocus();
        }
        adapter.focusTemizle();
    }
    public boolean isVisible() {
        return panelView != null && panelView.getVisibility() == View.VISIBLE;
    }
    public boolean handleBack() {
        if ("buton".equals(focusMod)) {
            focusMod = "kart";
            butonPoz = -1;
            adapter.setFocusKart(focusPoz);
            return true;
        }
        gizle();        if (kapandiCallback != null) kapandiCallback.onKapandi();
        return true;
    }
    public View getPanelView() { return panelView; }
}