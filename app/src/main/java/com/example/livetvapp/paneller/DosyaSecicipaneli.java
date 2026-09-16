package com.example.livetvapp.paneller;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.livetvapp.dosyatransferi.DeviceScanActivity;
import com.example.livetvapp.dosyatransferi.FileTransferServer;
import com.example.livetvapp.dosyatransferi.FileTransferClient;
import com.example.livetvapp.dosyatransferi.NetworkScanner;
import com.example.livetvapp.dosyatransferi.RemoteDevice;
import com.example.livetvapp.dosyatransferi.FileTransferActivity;
import android.app.ProgressDialog;
import com.example.livetvapp.R;
import com.example.livetvapp.dosyatransferi.RemoteControlServer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
public class DosyaSecicipaneli extends AppCompatActivity {

    
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int MANAGE_STORAGE_REQUEST  = 101;

    
    private RemoteControlServer remoteServer;
    private BroadcastReceiver remoteCommandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            String command = intent.getStringExtra("command");
            String data    = intent.getStringExtra("data");
            if (command != null) handleRemoteCommand(command, data);
        }
    };
    private FileTransferServer fileServer;
    private FileTransferClient fileClient;
    private ProgressDialog transferDialog;
    private String pendingFileToSend = null;
    private File pendingFile = null;
    private String mode;
    private static final int SIRALAMA_AD_ASC     = 0;
    private static final int SIRALAMA_AD_DESC    = 1;
    private static final int SIRALAMA_TARIH_DESC = 2;
    private static final int SIRALAMA_TARIH_ASC  = 3;
    private static final int SIRALAMA_BOYUT_DESC = 4;
    private static final int SIRALAMA_BOYUT_ASC  = 5;
    private static final int SIRALAMA_TUR        = 6;

    public static final int GORUNUM_LISTE  = 0;
    public static final int GORUNUM_IZGARA = 1;

    private static final int PANO_YOK   = 0;
    private static final int PANO_KOPYA = 1;
    private static final int PANO_KES   = 2;

    
    private RecyclerView  recyclerView;
    private DosyaAdapter  adapter;
    private TextView      tvYol, tvBilgi, tvBos, tvPanoBilgi, tvSecimSayisi;
    private EditText      etArama;
    private ImageButton   btnGeri, btnAnaDizin, btnGorunum, btnSiralama, btnArama;
    private LinearLayout  btnYeniKlasor, btnYapistir, btnSecimMod;  
    private LinearLayout btnTumunuSec;
    private LinearLayout  aramaBar, breadcrumbLayout, altToolbar;

    
    
    
    
    private File            dahiliDepolamaYolu;
    private File            currentDir;
    private Stack<File>     gecmis       = new Stack<>();
    private List<DosyaOge> tumDosyalar  = new ArrayList<>();
    private List<DosyaOge> filtrelenmis = new ArrayList<>();
    private int             gorunum      = GORUNUM_LISTE;
    private int             siralama     = SIRALAMA_AD_ASC;
    private boolean         aramaAcik    = false;
    private String          aramaMetni   = "";

    
    private boolean      secimModu = false;
    private Set<String>  secilen   = new HashSet<>();

    
    private File panoKaynak = null;
    private int  panoIslem  = PANO_YOK;

    
    private int sonFocusPozisyonu = 0;

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dosya_secici);
        baglaView();
        mode = getIntent().getStringExtra("mode");
        if (mode == null) mode = "browser";
        dinleyicileriAyarla();
        mode = getIntent().getStringExtra("mode");
        if (mode == null) mode = "browser";
        izinKontrol();
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener((eskiFocus, yeniFocus) -> {
            String eski = "null";
            String yeni = "null";
            try {
                if (eskiFocus != null && eskiFocus.getId() != View.NO_ID)
                    eski = getResources().getResourceEntryName(eskiFocus.getId()) + " (" + eskiFocus.getClass().getSimpleName() + ")";
                if (yeniFocus != null && yeniFocus.getId() != View.NO_ID)
                    yeni = getResources().getResourceEntryName(yeniFocus.getId()) + " (" + yeniFocus.getClass().getSimpleName() + ")";
            } catch (Exception ignored) {}
            Log.d("FOCUS", "ESKİ: " + eski + " → YENİ: " + yeni);
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        
        android.content.Intent iAcik = new android.content.Intent("com.example.livetvapp.DOSYA_SECICI_DURUM");
        iAcik.putExtra("aktif", true);
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).sendBroadcast(iAcik);

        LocalBroadcastManager.getInstance(this).registerReceiver(
                remoteCommandReceiver,
                new IntentFilter("com.example.livetvapp.REMOTE_COMMAND")
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        android.content.Intent iKapali = new android.content.Intent("com.example.livetvapp.DOSYA_SECICI_DURUM");
        iKapali.putExtra("aktif", false);
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).sendBroadcast(iKapali);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(remoteCommandReceiver);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    
    private void baglaView() {
        recyclerView    = findViewById(R.id.recyclerView);
        tvYol           = findViewById(R.id.tvYol);
        tvBilgi         = findViewById(R.id.tvBilgi);
        tvBos           = findViewById(R.id.tvBos);
        tvPanoBilgi     = findViewById(R.id.tvPanoBilgi);
        tvSecimSayisi   = findViewById(R.id.tvSecimSayisi);
        etArama         = findViewById(R.id.etArama);
        btnGeri         = findViewById(R.id.btnGeri);
        btnAnaDizin     = findViewById(R.id.btnAnaDizin);
        btnGorunum      = findViewById(R.id.btnGorunum);
        btnSiralama     = findViewById(R.id.btnSiralama);
        btnArama        = findViewById(R.id.btnArama);
        btnYeniKlasor   = findViewById(R.id.btnYeniKlasor);   
        btnYapistir     = findViewById(R.id.btnYapistir);     
        btnSecimMod     = findViewById(R.id.btnSecimMod);     
        btnTumunuSec   = findViewById(R.id.btnTumunuSec);
        aramaBar        = findViewById(R.id.aramaBar);
        breadcrumbLayout= findViewById(R.id.breadcrumbLayout);
        altToolbar      = findViewById(R.id.altToolbar);
    }

    
    private void dinleyicileriAyarla() {
        btnGeri.setOnClickListener(v -> geriGit());
        btnAnaDizin.setOnClickListener(v -> anaDizineGit());
        btnGorunum.setOnClickListener(v -> {
            gorunum = (gorunum == GORUNUM_LISTE) ? GORUNUM_IZGARA : GORUNUM_LISTE;
            gorununGuncelle();
        });
        btnSiralama.setOnClickListener(v -> siralamaDialogGoster());
        btnArama.setOnClickListener(v -> aramaToggle());
        btnYeniKlasor.setOnClickListener(v -> yeniKlasorOlustur());
        btnYapistir.setOnClickListener(v -> yapistir());
        btnSecimMod.setOnClickListener(v -> secimModuToggle());
        btnTumunuSec.setOnClickListener(v -> tumunuSec());
        etArama.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {
                aramaMetni = s.toString().toLowerCase(new Locale("tr"));
                listeyiFiltrele();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        etArama.setOnKeyListener((v, k, e) -> {
            if (e.getAction() == KeyEvent.ACTION_DOWN && k == KeyEvent.KEYCODE_ESCAPE) {
                aramaToggle(); return true;
            }
            return false;
        });
    }
    private void tumunuSec() {
        if (!secimModu) return;
        secilen.clear();
        for (DosyaOge oge : filtrelenmis) {
            secilen.add(oge.dosya.getAbsolutePath());
        }
        adapter.setSecilen(secilen);
        adapter.notifyDataSetChanged();
        if (tvSecimSayisi != null)
            tvSecimSayisi.setText(secilen.size() + " seçili");
        toast(secilen.size() + " öğe seçildi");
    }
    private void aramaToggle() {
        aramaAcik = !aramaAcik;
        if (aramaAcik) {
            aramaBar.setVisibility(View.VISIBLE);
            etArama.requestFocus();
        } else {
            aramaBar.setVisibility(View.GONE);
            aramaMetni = "";
            etArama.setText("");
            listeyiFiltrele();
        }
    }

    
    private void izinKontrol() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) baslatDosyaGezgini();
            else izinDialogGoster();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                baslatDosyaGezgini();
            else izinIste();
        } else {
            baslatDosyaGezgini();
        }
    }

    private void izinDialogGoster() {
        new AlertDialog.Builder(this)
                .setTitle("Depolama İzni Gerekli")
                .setMessage("Dosyalara erişebilmek için depolama izni gereklidir.")
                .setPositiveButton("İzin Ver", (d, w) -> izinIste())
                .setNegativeButton("İptal", (d, w) -> finish())
                .setCancelable(false).show();
    }

    private void izinIste() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                startActivityForResult(new Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName())), MANAGE_STORAGE_REQUEST);
            } catch (Exception e) {
                startActivityForResult(
                        new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
                        MANAGE_STORAGE_REQUEST);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void testNetworkScanner() {
        Log.d("🔍[NetworkScanner]", "========== AĞ TARAMA TESTİ ==========");
        Log.d("🔍[NetworkScanner]", "📱 Bu cihaz: " + android.os.Build.MODEL);

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Cihazlar aranıyor...\n(Lütfen diğer cihazda da uygulamayı açın)");
        progressDialog.setCancelable(false);
        progressDialog.show();

        NetworkScanner scanner = new NetworkScanner(this);
        scanner.scanForDevices(new NetworkScanner.ScanCallback() {
            @Override
            public void onDevicesFound(List<RemoteDevice> devices) {
                progressDialog.dismiss();

                Log.d("🔍[NetworkScanner]", "✅ Tarama tamamlandı!");
                Log.d("🔍[NetworkScanner]", "📡 Bulunan cihaz sayısı: " + devices.size());

                if (devices.isEmpty()) {
                    Log.w("🔍[NetworkScanner]", "⚠️ Hiç cihaz bulunamadı!");
                    toast("⚠️ Cihaz bulunamadı!\nAynı ağda başka bir cihazda uygulamayı açın.");
                } else {
                    for (int i = 0; i < devices.size(); i++) {
                        RemoteDevice d = devices.get(i);
                        Log.d("🔍[NetworkScanner]", "📱 Cihaz " + (i+1) + ": " + d.getName() + " - " + d.getIpAddress());
                    }
                    toast("✅ " + devices.size() + " cihaz bulundu!\nLogcat'ten detayları görebilirsiniz.");
                }
            }

            @Override
            public void onScanFailed(String error) {
                progressDialog.dismiss();
                Log.e("🔍[NetworkScanner]", "❌ Tarama başarısız: " + error);
                toast("❌ Tarama hatası: " + error);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] p, @NonNull int[] r) {
        super.onRequestPermissionsResult(req, p, r);
        if (req == PERMISSION_REQUEST_CODE) {
            if (r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) baslatDosyaGezgini();
            else izinReddedildiDialog();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == MANAGE_STORAGE_REQUEST) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                    && Environment.isExternalStorageManager()) baslatDosyaGezgini();
            else izinReddedildiDialog();
        }
    }

    private void izinReddedildiDialog() {
        new AlertDialog.Builder(this)
                .setTitle("İzin Reddedildi")
                .setMessage("Dosya erişimi olmadan devam edilemiyor.")
                .setPositiveButton("Tekrar Dene", (d, w) -> izinIste())
                .setNegativeButton("Çıkış", (d, w) -> finish())
                .setCancelable(false).show();
    }

    
    private void baslatDosyaGezgini() {
        adapter = new DosyaAdapter(
                filtrelenmis,
                this::onDosyaAc,
                this::islemMenusuGoster,
                this::onDosyaSecimDegisti
        );
        recyclerView.setAdapter(adapter);
        gorununGuncelle();
        anaDiziniOlusturVeGit();
    }

    
    private boolean erisilebilirMi(File dir) {
        if (dir == null) return false;
        try {
            if (!dir.exists()) {
                Log.d("DosyaSecici", "erisilebilirMi: yok -> " + dir.getAbsolutePath());
                return false;
            }
            String[] icerik = dir.list();
            boolean sonuc = icerik != null;
            Log.d("DosyaSecici", "erisilebilirMi: " + dir.getAbsolutePath()
                    + " exists=" + dir.exists()
                    + " canRead=" + dir.canRead()
                    + " list()=" + (icerik == null ? "null" : icerik.length + " öğe")
                    + " -> " + sonuc);
            return sonuc;
        } catch (SecurityException e) {
            Log.w("DosyaSecici", "erisilebilirMi: SecurityException -> " + dir.getAbsolutePath() + " : " + e.getMessage());
            return false;
        }
    }

    private void anaDiziniOlusturVeGit() {
        List<DosyaOge> depolamalar = new ArrayList<>();

        
        File dahili = Environment.getExternalStorageDirectory();
        Log.d("DosyaSecici", "getExternalStorageDirectory() -> "
                + (dahili != null ? dahili.getAbsolutePath() : "null"));

        List<File> dahiliAdaylar = new ArrayList<>();
        if (dahili != null) dahiliAdaylar.add(dahili);
        dahiliAdaylar.add(new File("/storage/emulated/0"));
        dahiliAdaylar.add(new File("/sdcard"));
        dahiliAdaylar.add(new File("/storage/self/primary"));
        dahiliAdaylar.add(new File("/data/media/0"));

        
        
        
        
        try {
            File[] extDirs = getExternalFilesDirs(null);
            if (extDirs != null && extDirs.length > 0 && extDirs[0] != null) {
                File kok = extDirs[0];
                
                for (int i = 0; i < 4 && kok != null; i++) kok = kok.getParentFile();
                if (kok != null) {
                    dahiliAdaylar.add(kok);
                    Log.d("DosyaSecici", "getExternalFilesDirs türetilmiş kök: " + kok.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            Log.w("DosyaSecici", "getExternalFilesDirs hatası: " + e.getMessage());
        }

        File bulunanDahili = null;
        for (File aday : dahiliAdaylar) {
            if (erisilebilirMi(aday)) {
                bulunanDahili = aday;
                break;
            }
        }

        if (bulunanDahili != null) {
            depolamalar.add(new DosyaOge(bulunanDahili));
            dahili = bulunanDahili; 
            dahiliDepolamaYolu = bulunanDahili;
            Log.d("DosyaSecici", "✅ Dahili depolama bulundu: " + bulunanDahili.getAbsolutePath());
        } else {
            dahili = null;
            dahiliDepolamaYolu = null;
            Log.w("DosyaSecici", "❌ Hiçbir dahili depolama adayına erişilemedi: " + dahiliAdaylar);
        }

        
        android.os.storage.StorageManager sm =
                (android.os.storage.StorageManager) getSystemService(android.content.Context.STORAGE_SERVICE);
        if (sm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            List<android.os.storage.StorageVolume> volumes = sm.getStorageVolumes();
            for (android.os.storage.StorageVolume vol : volumes) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    
                    File dir = vol.getDirectory();
                    if (erisilebilirMi(dir)) {
                        if (dahili != null && dir.getAbsolutePath().equals(dahili.getAbsolutePath())) continue;
                        boolean zatenVar = false;
                        for (DosyaOge o : depolamalar) {
                            if (o.dosya.getAbsolutePath().equals(dir.getAbsolutePath())) { zatenVar = true; break; }
                        }
                        if (!zatenVar) {
                            depolamalar.add(new DosyaOge(dir));
                            Log.d("DosyaSecici", "StorageVolume: " + dir.getAbsolutePath());
                        }
                    }
                } else {
                    
                    try {
                        java.lang.reflect.Method getPath = vol.getClass().getMethod("getPath");
                        String path = (String) getPath.invoke(vol);
                        if (path != null) {
                            File dir = new File(path);
                            if (erisilebilirMi(dir)) {
                                if (dahili != null && dir.getAbsolutePath().equals(dahili.getAbsolutePath())) continue;
                                boolean zatenVar = false;
                                for (DosyaOge o : depolamalar) {
                                    if (o.dosya.getAbsolutePath().equals(dir.getAbsolutePath())) { zatenVar = true; break; }
                                }
                                if (!zatenVar) {
                                    depolamalar.add(new DosyaOge(dir));
                                    Log.d("DosyaSecici", "StorageVolume (reflection): " + path);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.w("DosyaSecici", "Reflection hatası: " + e.getMessage());
                    }
                }
            }
        }

        
        try {
            File storage = new File("/storage");
            File[] volumes = storage.listFiles();
            if (volumes != null) {
                for (File vol : volumes) {
                    if (vol.isDirectory() && vol.canRead()
                            && vol.getName().matches("(?i)[A-F0-9]{4}-[A-F0-9]{4}")) {
                        boolean zatenVar = false;
                        for (DosyaOge o : depolamalar) {
                            if (o.dosya.getAbsolutePath().equals(vol.getAbsolutePath())) { zatenVar = true; break; }
                        }
                        if (!zatenVar) {
                            depolamalar.add(new DosyaOge(vol));
                            Log.d("DosyaSecici", "USB (UUID fallback): " + vol.getAbsolutePath());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("DosyaSecici", "Fallback tarama hatası: " + e.getMessage());
        }

        
        File storage = new File("/storage");
        if (storage.exists() && storage.isDirectory()) {
            File[] storageFiles = storage.listFiles();
            if (storageFiles != null) {
                for (File f : storageFiles) {
                    if (f.isDirectory() && f.canRead()) {
                        if (dahili != null && f.getAbsolutePath().equals(dahili.getAbsolutePath())) continue;
                        if (f.getAbsolutePath().equals("/storage/emulated")) continue;
                        boolean zatenVar = false;
                        for (DosyaOge o : depolamalar) {
                            if (o.dosya.getAbsolutePath().equals(f.getAbsolutePath())) { zatenVar = true; break; }
                        }
                        if (!zatenVar) {
                            depolamalar.add(new DosyaOge(f));
                            Log.d("DosyaSecici", "/storage altı: " + f.getAbsolutePath());
                        }
                    }
                }
            }
        }

        
        File mnt = new File("/mnt");
        if (mnt.exists() && mnt.isDirectory()) {
            File[] mntFiles = mnt.listFiles();
            if (mntFiles != null) {
                for (File f : mntFiles) {
                    if (f.isDirectory() && f.canRead()) {
                        boolean zatenVar = false;
                        for (DosyaOge o : depolamalar) {
                            if (o.dosya.getAbsolutePath().equals(f.getAbsolutePath())) { zatenVar = true; break; }
                        }
                        if (!zatenVar) {
                            depolamalar.add(new DosyaOge(f));
                            Log.d("DosyaSecici", "/mnt altı: " + f.getAbsolutePath());
                        }
                    }
                }
            }
        }

        
        if (depolamalar.isEmpty()) {
            Toast.makeText(this, "Depolama alanı bulunamadı!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        
        currentDir = new File("/storage");
        secilen.clear();
        tumDosyalar.clear();
        tumDosyalar.addAll(depolamalar);
        sirala();
        listeyiFiltrele();
        tvYol.setText("/storage");
        breadcrumbGuncelle();
        arayuzGuncelle();
    }

    
    private void dizineGit(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) { toast("Klasör açılamadı"); return; }
        currentDir = dir;
        secilen.clear();
        dosyalariYukle();
        breadcrumbGuncelle();
        arayuzGuncelle();
    }

    private void geriGit() {
        if (secimModu) { secimModuKapat(); return; }

        String mevcutYol = currentDir.getAbsolutePath();
        String dahiliYol = dahiliDepolamaYolu != null ? dahiliDepolamaYolu.getAbsolutePath() : "";

        
        if (mevcutYol.equals("/storage")) {
            finish();
            return;
        }

        
        if (!gecmis.isEmpty()) {
            currentDir = gecmis.pop();
            
            int hedefPoz = sonFocusPozisyonu;
            sonFocusPozisyonu = 0;
            if (currentDir.getAbsolutePath().equals("/storage")) {
                anaDiziniOlusturVeGit();
            } else {
                dosyalariYukleVeFocusVer(hedefPoz);
                breadcrumbGuncelle();
            }
            return;
        }

        
        if (mevcutYol.equals(dahiliYol)
                || (currentDir.getParentFile() != null
                && currentDir.getParentFile().getAbsolutePath().equals("/storage"))) {
            anaDiziniOlusturVeGit();
            return;
        }

        finish();
    }

    private void anaDizineGit() {
        gecmis.clear();
        if (dahiliDepolamaYolu != null && erisilebilirMi(dahiliDepolamaYolu)) {
            currentDir = dahiliDepolamaYolu;
            dosyalariYukle();
            breadcrumbGuncelle();
        } else {
            
            
            Log.w("DosyaSecici", "anaDizineGit: dahili depolama yok, sanal köke dönülüyor");
            anaDiziniOlusturVeGit();
        }
    }

    
    private void dosyalariYukle() {
        tumDosyalar.clear();
        File[] dosyalar = currentDir.listFiles();
        if (dosyalar != null)
            for (File f : dosyalar)
                if (!f.getName().startsWith("."))
                    tumDosyalar.add(new DosyaOge(f));
        sirala();
        listeyiFiltrele();
        tvYol.setText(currentDir.getAbsolutePath());
    }

    private void dosyalariYukleVeFocusVer(int hedefPozisyon) {
        tumDosyalar.clear();
        File[] dosyalar = currentDir.listFiles();
        if (dosyalar != null)
            for (File f : dosyalar)
                if (!f.getName().startsWith("."))
                    tumDosyalar.add(new DosyaOge(f));
        sirala();
        
        filtrelenmis.clear();
        for (DosyaOge o : tumDosyalar)
            if (aramaMetni.isEmpty() || o.dosya.getName().toLowerCase(new Locale("tr")).contains(aramaMetni))
                filtrelenmis.add(o);
        if (adapter != null) adapter.notifyDataSetChanged();

        int kls = 0, dos = 0;
        for (DosyaOge o : filtrelenmis) { if (o.dosya.isDirectory()) kls++; else dos++; }
        tvBilgi.setText(kls + " klasör, " + dos + " dosya");
        tvBos.setVisibility(filtrelenmis.isEmpty() ? View.VISIBLE : View.GONE);
        tvYol.setText(currentDir.getAbsolutePath());

        if (!filtrelenmis.isEmpty()) {
            
            final int poz = (hedefPozisyon >= 0 && hedefPozisyon < filtrelenmis.size())
                    ? hedefPozisyon : 0;
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                recyclerView.scrollToPosition(poz);
                recyclerView.post(() -> {
                    RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(poz);
                    if (vh != null) {
                        View layoutAc = vh.itemView.findViewById(R.id.layoutAc);
                        if (layoutAc != null) layoutAc.requestFocus();
                        else vh.itemView.requestFocus();
                    }
                });
            }, 150);
        }
    }

    private void sirala() {
        Collections.sort(tumDosyalar, (a, b) -> {
            if (a.dosya.isDirectory() && !b.dosya.isDirectory()) return -1;
            if (!a.dosya.isDirectory() && b.dosya.isDirectory()) return 1;
            switch (siralama) {
                case SIRALAMA_AD_ASC:     return a.dosya.getName().compareToIgnoreCase(b.dosya.getName());
                case SIRALAMA_AD_DESC:    return b.dosya.getName().compareToIgnoreCase(a.dosya.getName());
                case SIRALAMA_TARIH_DESC: return Long.compare(b.dosya.lastModified(), a.dosya.lastModified());
                case SIRALAMA_TARIH_ASC:  return Long.compare(a.dosya.lastModified(), b.dosya.lastModified());
                case SIRALAMA_BOYUT_DESC: return Long.compare(b.dosya.length(), a.dosya.length());
                case SIRALAMA_BOYUT_ASC:  return Long.compare(a.dosya.length(), b.dosya.length());
                case SIRALAMA_TUR:        return uzantiAl(a.dosya).compareTo(uzantiAl(b.dosya));
                default: return 0;
            }
        });
    }

    private void listeyiFiltrele() {
        filtrelenmis.clear();
        for (DosyaOge o : tumDosyalar)
            if (aramaMetni.isEmpty() || o.dosya.getName().toLowerCase(new Locale("tr")).contains(aramaMetni))
                filtrelenmis.add(o);
        if (adapter != null) adapter.notifyDataSetChanged();

        int kls = 0, dos = 0;
        for (DosyaOge o : filtrelenmis) { if (o.dosya.isDirectory()) kls++; else dos++; }
        tvBilgi.setText(kls + " klasör, " + dos + " dosya");
        tvBos.setVisibility(filtrelenmis.isEmpty() ? View.VISIBLE : View.GONE);

        if (!filtrelenmis.isEmpty())
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(0);
                if (vh != null) {
                    View layoutAc = vh.itemView.findViewById(R.id.layoutAc);
                    if (layoutAc != null) layoutAc.requestFocus();
                    else vh.itemView.requestFocus();
                }
                
                btnGeri.setNextFocusDownId(R.id.layoutAc);
                btnAnaDizin.setNextFocusDownId(R.id.layoutAc);
                btnArama.setNextFocusDownId(R.id.layoutAc);
                btnSiralama.setNextFocusDownId(R.id.layoutAc);
                btnGorunum.setNextFocusDownId(R.id.layoutAc);
            }, 150);
    }

    
    private void gorununGuncelle() {
        if (gorunum == GORUNUM_LISTE) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        } else {
            
            int spanCount = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? 2 : 4;
            recyclerView.setLayoutManager(new GridLayoutManager(this, spanCount));
        }
        if (adapter != null) { adapter.setGorunum(gorunum); adapter.notifyDataSetChanged(); }
    }

    private void siralamaDialogGoster() {
        String[] s = {"İsim (A→Z)", "İsim (Z→A)", "Tarih (Yeni)", "Tarih (Eski)", "Boyut (Büyük)", "Boyut (Küçük)", "Tür"};
        new AlertDialog.Builder(this).setTitle("Sıralama")
                .setSingleChoiceItems(s, siralama, (d, w) -> { siralama = w; sirala(); listeyiFiltrele(); d.dismiss(); })
                .setNegativeButton("İptal", null).show();
    }

    
    private void onDosyaAc(DosyaOge oge) {
        if ("browser".equals(mode)) {
            if (oge.dosya.isDirectory()) {
                
                sonFocusPozisyonu = filtrelenmis.indexOf(oge);
                gecmis.push(currentDir);
                dizineGit(oge.dosya);
            }
            return;
        }
        if (secimModu) { onDosyaSecimDegisti(oge); return; }
        if (oge.dosya.isDirectory()) {
            sonFocusPozisyonu = filtrelenmis.indexOf(oge);
            gecmis.push(currentDir);
            dizineGit(oge.dosya);
        }
        else dosyaSec(oge.dosya);
    }

    
    private void onDosyaSecimDegisti(DosyaOge oge) {
        String yol = oge.dosya.getAbsolutePath();
        if (secilen.contains(yol)) secilen.remove(yol); else secilen.add(yol);
        adapter.setSecilen(secilen);

        
        int pos = filtrelenmis.indexOf(oge);
        if (pos >= 0) {
            RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(pos);
            if (vh != null) {
                View itemView = vh.itemView;
                CheckBox cb = itemView.findViewById(R.id.cbSecim);
                if (cb != null) cb.setChecked(secilen.contains(yol));
                boolean secili = secilen.contains(yol);
                itemView.setBackgroundColor(secili ? 0x557c4dff : 0x00000000);
            }
        }

        if (tvSecimSayisi != null)
            tvSecimSayisi.setText(secilen.isEmpty() ? "" : secilen.size() + " seçili");
    }

    private void dosyaSec(File f) {
        if ("m3u_import".equals(mode) || "backup_import".equals(mode)) {
            
            Intent resultIntent = new Intent();
            resultIntent.putExtra("selected_file_path", f.getAbsolutePath());
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            
            onDosyaSecimDegisti(new DosyaOge(f));
        }
    }

    
    private void islemMenusuGoster(DosyaOge oge) {
        File dosya = oge.dosya;

        final List<String>   etiketler = new ArrayList<>();
        final List<Runnable> eylemler  = new ArrayList<>();

        if (!dosya.isDirectory()) {
            etiketler.add("✅  Seç ve Aç");
            eylemler.add(() -> dosyaSec(dosya));
        }

        etiketler.add("📋  Kopyala");
        eylemler.add(() -> kopyala(dosya));

        etiketler.add("✂️  Kes");
        eylemler.add(() -> kes(dosya));

        etiketler.add("✏️  Yeniden Adlandır");
        eylemler.add(() -> yenidenAdlandir(dosya));

        
        if (!dosya.isDirectory()) {
            etiketler.add("📡  WiFi ile Gönder");
            eylemler.add(() -> tekDosyaGonder(dosya));
        }

        etiketler.add("🗑  Sil");
        eylemler.add(() -> { List<File> l = new ArrayList<>(); l.add(dosya); silOnay(l); });

        etiketler.add("ℹ️  Bilgi");
        eylemler.add(() -> bilgiGoster(dosya));

        new AlertDialog.Builder(this)
                .setTitle(dosya.getName())
                .setItems(etiketler.toArray(new String[0]), (d, i) -> eylemler.get(i).run())
                .setNegativeButton("İptal", null)
                .show();
    }

    
    private void tekDosyaGonder(File dosya) {
        
        List<File> tekListe = new ArrayList<>();
        tekListe.add(dosya);
        startDeviceScanActivity(tekListe, false);
    }

    
    private void kopyala(File dosya) {
        panoKaynak = dosya;
        panoIslem  = PANO_KOPYA;
        panoBilgiGuncelle();
        toast("Kopyalandı: " + dosya.getName());
    }

    
    private void kes(File dosya) {
        panoKaynak = dosya;
        panoIslem  = PANO_KES;
        panoBilgiGuncelle();
        toast("Kesildi: " + dosya.getName());
    }

    
    private void yapistir() {
        if (panoKaynak == null || panoIslem == PANO_YOK) {
            toast("Panoda öğe yok. Önce Kopyala veya Kes yapın.");
            return;
        }
        if (panoIslem == PANO_KES
                && panoKaynak.getParent() != null
                && panoKaynak.getParent().equals(currentDir.getAbsolutePath())) {
            toast("Aynı klasöre taşınamaz.");
            return;
        }

        File hedef = cakismasiCoz(new File(currentDir, panoKaynak.getName()));
        boolean ok;

        if (panoIslem == PANO_KOPYA) {
            ok = dosyaKopyala(panoKaynak, hedef);
            if (ok) toast("Yapıştırıldı: " + hedef.getName());
        } else {
            ok = panoKaynak.renameTo(hedef);
            if (!ok) { ok = dosyaKopyala(panoKaynak, hedef); if (ok) dosyaSilGercek(panoKaynak); }
            if (ok) {
                toast("Taşındı: " + hedef.getName());
                panoKaynak = null;
                panoIslem  = PANO_YOK;
                panoBilgiGuncelle();
            }
        }
        if (!ok) toast("İşlem başarısız!");
        dosyalariYukle();
    }

    
    private void yenidenAdlandir(File dosya) {
        EditText et = inputField(dosya.getName());
        new AlertDialog.Builder(this).setTitle("✏️ Yeniden Adlandır").setView(et)
                .setPositiveButton("Kaydet", (d, w) -> {
                    String yeni = et.getText().toString().trim();
                    if (yeni.isEmpty())               { toast("Ad boş olamaz"); return; }
                    if (yeni.equals(dosya.getName())) return;
                    File yeniDosya = new File(dosya.getParent(), yeni);
                    if (yeniDosya.exists())            { toast("Bu isim zaten kullanımda"); return; }
                    if (dosya.renameTo(yeniDosya))    { toast("Yeniden adlandırıldı"); dosyalariYukle(); }
                    else                               toast("Başarısız!");
                })
                .setNegativeButton("İptal", null).show();
    }

    
    private void silOnay(List<File> dosyalar) {
        String mesaj = dosyalar.size() == 1
                ? "\"" + dosyalar.get(0).getName() + "\" silinsin mi?\nBu işlem geri alınamaz!"
                : dosyalar.size() + " öğe silinsin mi?\nBu işlem geri alınamaz!";

        new AlertDialog.Builder(this).setTitle("⚠️ Sil").setMessage(mesaj)
                .setPositiveButton("Sil", (d, w) -> {
                    int ok = 0;
                    for (File f : dosyalar) if (dosyaSilGercek(f)) ok++;
                    toast(ok + " öğe silindi");
                    secilen.clear();
                    if (secimModu) { secimModu = false; adapter.setSecimModu(false); adapter.setSecilen(secilen); }
                    dosyalariYukle();
                    arayuzGuncelle();
                })
                .setNegativeButton("İptal", null).show();
    }

    
    private void yeniKlasorOlustur() {
        EditText et = inputField("Yeni Klasör");
        new AlertDialog.Builder(this).setTitle("📁 Yeni Klasör").setView(et)
                .setPositiveButton("Oluştur", (d, w) -> {
                    String ad = et.getText().toString().trim();
                    if (ad.isEmpty())         { toast("Ad boş olamaz"); return; }
                    File yeni = new File(currentDir, ad);
                    if (yeni.exists())        { toast("Bu klasör zaten var"); return; }
                    if (yeni.mkdir())         { toast("Klasör oluşturuldu"); dosyalariYukle(); }
                    else                      toast("Oluşturulamadı!");
                })
                .setNegativeButton("İptal", null).show();
    }

    
    private void secimModuToggle() {
        if (secimModu) {
            if (!secilen.isEmpty()) secimIslemMenusu();
            else secimModuKapat();
        } else {
            secimModu = true;
            secilen.clear();
            adapter.setSecimModu(true);
            adapter.setSecilen(secilen);
            adapter.notifyDataSetChanged();
            arayuzGuncelle();
            toast("Seçim modu — öğelere tıklayın, sonra ✓ butonuna basın");
        }
    }

    private void secimModuKapat() {
        secimModu = false;
        secilen.clear();
        adapter.setSecimModu(false);
        adapter.setSecilen(secilen);
        adapter.notifyDataSetChanged();
        arayuzGuncelle();
    }

    private void secimIslemMenusu() {
        if (secilen.isEmpty()) { toast("Hiçbir öğe seçilmedi"); return; }

        
        String[] islemler = {"📋  Kopyala", "✂️  Kes", "📡  Seçilenleri Gönder", "🗑  Hepsini Sil", "☐  Seçimi Temizle", "✖  İptal"};

        new AlertDialog.Builder(this)
                .setTitle(secilen.size() + " öğe seçili")
                .setItems(islemler, (d, i) -> {
                    switch (i) {
                        case 0: 
                            if (secilen.size() == 1) kopyala(new File(secilen.iterator().next()));
                            else toast("Çoklu kopyalama için tek tek seçin");
                            secimModuKapat(); break;
                        case 1: 
                            if (secilen.size() == 1) kes(new File(secilen.iterator().next()));
                            else toast("Çoklu kesme için tek tek seçin");
                            secimModuKapat(); break;
                        case 2: 
                            cokluDosyaGonder();
                            secimModuKapat(); break;
                        case 3: 
                            List<File> l = new ArrayList<>();
                            for (String y : secilen) l.add(new File(y));
                            silOnay(l); break;
                        case 4: 
                            secilen.clear();
                            adapter.setSecilen(secilen);
                            adapter.notifyDataSetChanged();
                            if (tvSecimSayisi != null) tvSecimSayisi.setText("");
                            toast("Seçimler temizlendi");
                            break;
                        case 5: break;
                    }
                }).show();
    }
    private void startDeviceScanActivity(List<File> dosyalar, boolean multiFileMode) {
        Intent intent = new Intent(this, DeviceScanActivity.class);
        intent.putExtra("multi_file_mode", multiFileMode);
        ArrayList<String> filePaths = new ArrayList<>();
        for (File f : dosyalar) filePaths.add(f.getAbsolutePath());
        intent.putStringArrayListExtra("file_paths", filePaths);
        startActivity(intent);
    }
    
    private void cokluDosyaGonder() {
        if (secilen.isEmpty()) {
            toast("Lütfen önce dosyaları seçin");
            return;
        }
        final List<File> seciliDosyalar = new ArrayList<>();
        for (String yol : secilen) {
            File f = new File(yol);
            if (f.isFile()) seciliDosyalar.add(f);
        }
        if (seciliDosyalar.isEmpty()) {
            toast("Seçili öğeler arasında gönderilebilir dosya yok");
            return;
        }
        startDeviceScanActivity(seciliDosyalar, true);
    }

    
    private void startMultiFileTransfer(RemoteDevice target, List<File> dosyalar) {
        Log.d("📤[MultiFile]", "Çoklu transfer başlatılıyor: " + dosyalar.size() + " dosya -> " + target.getIpAddress());

        Intent intent = new Intent(this, FileTransferActivity.class);
        intent.putExtra("target_ip", target.getIpAddress());
        intent.putExtra("target_name", target.getName());
        intent.putExtra("multi_file_mode", true);

        
        ArrayList<String> filePaths = new ArrayList<>();
        for (File f : dosyalar) {
            filePaths.add(f.getAbsolutePath());
        }
        intent.putStringArrayListExtra("file_paths", filePaths);

        startActivity(intent);
    }

    
    private void bilgiGoster(File f) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle("ℹ️ Dosya Bilgisi")
                .setMessage("Bilgi yükleniyor...")
                .setNegativeButton("Kapat", null);
        if (!f.isDirectory()) b.setPositiveButton("Seç ve Aç", (d, w) -> dosyaSec(f));

        AlertDialog dialog = b.show();

        if (f.isDirectory()) {
            
            new Thread(() -> {
                long[] sonuc = klasorBilgisiHesapla(f);
                long toplamDosya = sonuc[0];
                long toplamBoyut = sonuc[1];
                String bilgi = "Ad: "           + f.getName()                    + "\n\n"
                        + "Yol: "              + f.getAbsolutePath()             + "\n\n"
                        + "Toplam Dosya: "     + toplamDosya + " dosya"          + "\n\n"
                        + "Toplam Boyut: "     + boyutFormatla(toplamBoyut)      + "\n\n"
                        + "Değiştirilme: "     + sdf.format(new Date(f.lastModified())) + "\n\n"
                        + "Tür: Klasör"                                          + "\n\n"
                        + "Okunabilir: "       + (f.canRead()  ? "✓" : "✗")
                        + "   Yazılabilir: "   + (f.canWrite() ? "✓" : "✗");
                runOnUiThread(() -> {
                    if (dialog.isShowing()) dialog.setMessage(bilgi);
                });
            }).start();
        } else {
            String bilgi = "Ad: "         + f.getName()              + "\n\n"
                    + "Yol: "            + f.getAbsolutePath()       + "\n\n"
                    + "Boyut: "          + boyutFormatla(f.length()) + "\n\n"
                    + "Değiştirilme: "   + sdf.format(new Date(f.lastModified())) + "\n\n"
                    + "Tür: "            + uzantiAl(f).toUpperCase() + " dosyası" + "\n\n"
                    + "Okunabilir: "     + (f.canRead()  ? "✓" : "✗")
                    + "   Yazılabilir: " + (f.canWrite() ? "✓" : "✗");
            dialog.setMessage(bilgi);
        }
    }
    private long[] klasorBilgisiHesapla(File dir) {
        long dosyaSayisi = 0;
        long toplamBoyut = 0;
        File[] icerik = dir.listFiles();
        if (icerik != null) {
            for (File f : icerik) {
                if (f.isDirectory()) {
                    long[] alt = klasorBilgisiHesapla(f);
                    dosyaSayisi += alt[0];
                    toplamBoyut += alt[1];
                } else {
                    dosyaSayisi++;
                    toplamBoyut += f.length();
                }
            }
        }
        return new long[]{dosyaSayisi, toplamBoyut};
    }
    
    private void breadcrumbGuncelle() {
        breadcrumbLayout.removeAllViews();
        List<File> yol = new ArrayList<>();
        File tmp = currentDir;
        while (tmp != null) { yol.add(0, tmp); if (tmp.getParentFile() == null) break; tmp = tmp.getParentFile(); }

        int bas = Math.max(0, yol.size() - 4);
        if (bas > 0) {
            TextView dots = new TextView(this);
            dots.setText("… › "); dots.setTextColor(0x66FFFFFF);
            dots.setTextSize(11f); dots.setPadding(4, 4, 4, 4);
            breadcrumbLayout.addView(dots);
        }
        for (int i = bas; i < yol.size(); i++) {
            final File hedef = yol.get(i);
            boolean son = (i == yol.size() - 1);
            String ad = hedef.getName().isEmpty() ? "Kök" : hedef.getName();
            TextView tv = new TextView(this);
            tv.setText(son ? ad : ad + " › ");
            tv.setTextColor(son ? 0xFFFFFFFF : 0xAAFFFFFF);
            tv.setTextSize(12f); tv.setPadding(4, 4, 4, 4);
            tv.setFocusable(!son); tv.setClickable(!son);
            if (!son) tv.setOnClickListener(v -> { currentDir = hedef; dosyalariYukle(); breadcrumbGuncelle(); });
            breadcrumbLayout.addView(tv);
        }
    }

    
    private void panoBilgiGuncelle() {
        if (tvPanoBilgi == null) return;
        if (panoKaynak == null || panoIslem == PANO_YOK) {
            tvPanoBilgi.setVisibility(View.GONE);
        } else {
            tvPanoBilgi.setVisibility(View.VISIBLE);
            tvPanoBilgi.setText((panoIslem == PANO_KOPYA ? "📋 " : "✂️ ") + panoKaynak.getName());
        }
    }

    private void arayuzGuncelle() {
        panoBilgiGuncelle();
        if (tvSecimSayisi != null)
            tvSecimSayisi.setText(secimModu && !secilen.isEmpty() ? secilen.size() + " seçili" : "");

        
        if (btnTumunuSec != null) {
            btnTumunuSec.setVisibility(secimModu ? View.VISIBLE : View.GONE);
        }
    }
    private boolean isAlertDialogAcik() {
        View focused = getCurrentFocus();
        if (focused == null) return false;
        
        
        android.view.ViewParent parent = focused.getParent();
        while (parent != null) {
            if (parent instanceof android.widget.ListView) return true; 
            if (parent instanceof android.widget.ScrollView) return true;
            if (parent instanceof android.widget.Button) return true;
            parent = parent instanceof android.view.View
                    ? ((android.view.View) parent).getParent() : null;
        }
        return false;
    }
    private void handleRemoteCommand(String command, String data) {
        runOnUiThread(() -> {
            View focused = getCurrentFocus();

            
            if (focused != null) {
                int keyCode = -1;
                switch (command) {
                    case "DPAD_UP":     keyCode = KeyEvent.KEYCODE_DPAD_UP;     break;
                    case "DPAD_DOWN":   keyCode = KeyEvent.KEYCODE_DPAD_DOWN;   break;
                    case "DPAD_LEFT":   keyCode = KeyEvent.KEYCODE_DPAD_LEFT;   break;
                    case "DPAD_RIGHT":  keyCode = KeyEvent.KEYCODE_DPAD_RIGHT;  break;
                    case "DPAD_CENTER": keyCode = KeyEvent.KEYCODE_DPAD_CENTER; break;
                    case "BACK":        keyCode = KeyEvent.KEYCODE_BACK;        break;
                }

                boolean focusAnaWindowda = getWindow().getDecorView()
                        .findViewById(focused.getId()) != null;

                
                if (!focusAnaWindowda && keyCode != -1) {
                    focused.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
                    focused.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
                    return;
                }
            }

            
            switch (command) {
                case "BACK":
                    onKeyDown(KeyEvent.KEYCODE_BACK,
                            new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
                    break;
                case "DPAD_CENTER":
                    if (focused != null) {
                        focused.dispatchKeyEvent(
                                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER));
                        focused.dispatchKeyEvent(
                                new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER));
                        focused.performClick();
                    }
                    break;
                case "DPAD_UP":    navigate(View.FOCUS_UP);    break;
                case "DPAD_DOWN":  navigate(View.FOCUS_DOWN);  break;
                case "DPAD_LEFT":  navigate(View.FOCUS_LEFT);  break;
                case "DPAD_RIGHT": navigate(View.FOCUS_RIGHT); break;
            }
        });
    }

    private void navigate(int direction) {
        View focused = getCurrentFocus();
        if (focused == null) {
            
            if (recyclerView != null && recyclerView.getChildCount() > 0) {
                View first = recyclerView.getChildAt(0);
                View layoutAc = first.findViewById(R.id.layoutAc);
                if (layoutAc != null) layoutAc.requestFocus();
                else first.requestFocus();
            }
            return;
        }
        View next = focused.focusSearch(direction);
        if (next != null) {
            next.requestFocus();
        } else {
            
            focused.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
                    direction == View.FOCUS_UP ? KeyEvent.KEYCODE_DPAD_UP :
                            direction == View.FOCUS_DOWN ? KeyEvent.KEYCODE_DPAD_DOWN :
                                    direction == View.FOCUS_LEFT ? KeyEvent.KEYCODE_DPAD_LEFT :
                                            KeyEvent.KEYCODE_DPAD_RIGHT));
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (aramaAcik)  { aramaToggle(); return true; }
            if (secimModu)  { secimModuKapat(); return true; }
            geriGit(); return true;
        }
        if (!aramaAcik) {
            if (keyCode == KeyEvent.KEYCODE_1) { siralama = SIRALAMA_AD_ASC;     sirala(); listeyiFiltrele(); return true; }
            if (keyCode == KeyEvent.KEYCODE_2) { siralama = SIRALAMA_TARIH_DESC; sirala(); listeyiFiltrele(); return true; }
            if (keyCode == KeyEvent.KEYCODE_3) { siralama = SIRALAMA_BOYUT_DESC; sirala(); listeyiFiltrele(); return true; }
            if (keyCode == KeyEvent.KEYCODE_SLASH) { aramaToggle(); return true; }
            if (keyCode == KeyEvent.KEYCODE_V) { yapistir(); return true; }
            if (keyCode == KeyEvent.KEYCODE_N) { yeniKlasorOlustur(); return true; }
            if (secimModu && (keyCode == KeyEvent.KEYCODE_DEL || keyCode == KeyEvent.KEYCODE_FORWARD_DEL)) {
                List<File> l = new ArrayList<>();
                for (String y : secilen) l.add(new File(y));
                silOnay(l); return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    
    private boolean dosyaKopyala(File kaynak, File hedef) {
        if (kaynak.isDirectory()) return klasorKopyala(kaynak, hedef);
        try {
            if (hedef.getParentFile() != null) hedef.getParentFile().mkdirs();
            InputStream in   = new FileInputStream(kaynak);
            OutputStream out = new FileOutputStream(hedef);
            byte[] buf = new byte[8192]; int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close(); out.close(); return true;
        } catch (IOException e) { return false; }
    }

    private boolean klasorKopyala(File kaynak, File hedef) {
        if (!hedef.mkdirs()) return false;
        File[] ic = kaynak.listFiles();
        if (ic == null) return true;
        boolean ok = true;
        for (File f : ic) ok &= dosyaKopyala(f, new File(hedef, f.getName()));
        return ok;
    }

    private boolean dosyaSilGercek(File dosya) {
        if (dosya.isDirectory()) {
            File[] ic = dosya.listFiles();
            if (ic != null) for (File f : ic) dosyaSilGercek(f);
        }
        return dosya.delete();
    }

    private File cakismasiCoz(File hedef) {
        if (!hedef.exists()) return hedef;
        String ad = hedef.getName(), uzanti = "";
        int dot = ad.lastIndexOf('.');
        if (dot >= 0) { uzanti = ad.substring(dot); ad = ad.substring(0, dot); }
        int n = 1; File yeni;
        do { yeni = new File(hedef.getParent(), ad + " (" + n + ")" + uzanti); n++; } while (yeni.exists());
        return yeni;
    }

    private EditText inputField(String varsayilan) {
        EditText et = new EditText(this);
        et.setText(varsayilan);
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setSelectAllOnFocus(true);
        et.setTextColor(0xFFEEEEFF);
        et.setBackgroundColor(0xFF2f304a);
        et.setPadding(24, 16, 24, 16);
        return et;
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }

    public static String boyutFormatla(long bytes) {
        if (bytes <= 0) return "0 B";
        String[] b = {"B", "KB", "MB", "GB", "TB"};
        int i = Math.min((int)(Math.log10(bytes) / Math.log10(1024)), b.length - 1);
        return new DecimalFormat("#,##0.#").format(bytes / Math.pow(1024, i)) + " " + b[i];
    }

    public static String uzantiAl(File f) {
        String n = f.getName(); int d = n.lastIndexOf('.');
        return (d >= 0) ? n.substring(d + 1).toLowerCase() : "";
    }

    
    private void wifiIleGonder() {
        if (secilen.isEmpty()) {
            toast("Lütfen önce bir dosya seçin");
            return;
        }

        
        String firstSelected = secilen.iterator().next();
        pendingFile = new File(firstSelected);
        pendingFileToSend = pendingFile.getName();

        Log.d("📤[FileClient]", "Gönderilecek dosya: " + pendingFileToSend);
        toast("Cihaz aranıyor...");

        
        NetworkScanner scanner = new NetworkScanner(this);
        scanner.scanForDevices(new NetworkScanner.ScanCallback() {
            @Override
            public void onDevicesFound(List<RemoteDevice> devices) {
                runOnUiThread(() -> {
                    if (devices.isEmpty()) {
                        toast("❌ Cihaz bulunamadı!\nAynı ağda başka bir cihazda uygulamayı açın.");
                        return;
                    }
                    showDeviceListForTransfer(devices);
                });
            }

            @Override
            public void onScanFailed(String error) {
                runOnUiThread(() -> toast("❌ Tarama hatası: " + error));
            }
        });
    }

    private void showDeviceListForTransfer(List<RemoteDevice> devices) {
        String[] deviceNames = new String[devices.size()];
        for (int i = 0; i < devices.size(); i++) {
            RemoteDevice d = devices.get(i);
            deviceNames[i] = d.getName() + "\n" + d.getIpAddress();
        }

        new AlertDialog.Builder(this)
                .setTitle("Dosyayı gönder: " + pendingFileToSend)
                .setItems(deviceNames, (dialog, which) -> {
                    RemoteDevice target = devices.get(which);
                    startFileTransfer(target);
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void startFileTransfer(RemoteDevice target) {
        if (pendingFile == null || !pendingFile.exists()) {
            toast("❌ Dosya bulunamadı!");
            return;
        }

        Log.d("📤[FileClient]", "Transfer başlatılıyor: " + pendingFile.getName() + " -> " + target.getIpAddress());

        
        FileTransferActivity.start(this, target.getIpAddress(), target.getName(), pendingFile.getAbsolutePath());

        
        pendingFile = null;
        pendingFileToSend = null;
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }



    
    
    
    public static class DosyaOge {
        public final File dosya;
        public DosyaOge(File d) { this.dosya = d; }
    }

    
    
    
    public static class DosyaAdapter extends RecyclerView.Adapter<DosyaAdapter.VH> {

        interface AcListener      { void onAc(DosyaOge o); }
        interface IslemListener   { void onIslem(DosyaOge o); }
        interface SecimListener   { void onSecimDegisti(DosyaOge o); }

        private final List<DosyaOge> liste;
        private final AcListener     ac;
        private final IslemListener  islem;
        private final SecimListener  secim;
        private int         gorunum   = GORUNUM_LISTE;
        private boolean     secimModu = false;
        private Set<String> secilen   = new HashSet<>();
        private String mode = "browser"; 

        DosyaAdapter(List<DosyaOge> l, AcListener a, IslemListener i, SecimListener s) {
            liste = l; ac = a; islem = i; secim = s;
        }

        void setGorunum(int g)          { this.gorunum   = g; }
        void setSecimModu(boolean b)    { this.secimModu = b; }
        void setSecilen(Set<String> s)  { this.secilen   = s; }

        @Override public int getItemViewType(int pos) { return gorunum; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            int layout = (vt == GORUNUM_IZGARA)
                    ? R.layout.item_dosya_izgara
                    : R.layout.item_dosya_liste;
            return new VH(LayoutInflater.from(p.getContext()).inflate(layout, p, false));
        }
        private long[] klasorBilgisiHesapla(File dir) {
            long dosyaSayisi = 0;
            long toplamBoyut = 0;
            File[] icerik = dir.listFiles();
            if (icerik != null) {
                for (File f : icerik) {
                    if (f.isDirectory()) {
                        long[] alt = klasorBilgisiHesapla(f);
                        dosyaSayisi += alt[0];
                        toplamBoyut += alt[1];
                    } else {
                        dosyaSayisi++;
                        toplamBoyut += f.length();
                    }
                }
            }
            return new long[]{dosyaSayisi, toplamBoyut};
        }
        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            DosyaOge oge = liste.get(pos);
            File f = oge.dosya;

            h.ivIkon.setImageResource(ikonSec(f));
            h.tvAd.setText(f.getName());

            if (h.tvMeta != null) {
                if (f.isDirectory()) {
                    h.tvMeta.setText("Hesaplanıyor...");
                    final TextView meta = h.tvMeta;
                    new Thread(() -> {
                        long[] sonuc = klasorBilgisiHesapla(f);
                        long toplamDosya = sonuc[0];
                        long toplamBoyut = sonuc[1];
                        meta.post(() ->
                                meta.setText(toplamDosya + " dosya  ·  " + boyutFormatla(toplamBoyut))
                        );
                    }).start();
                } else {
                    h.tvMeta.setText(boyutFormatla(f.length()) + "  ·  "
                            + new SimpleDateFormat("dd.MM.yy", Locale.getDefault())
                            .format(new Date(f.lastModified())));
                }
            }

            if (h.tvTip != null) {
                String uzanti = uzantiAl(f);
                if (!f.isDirectory() && !uzanti.isEmpty()) {
                    h.tvTip.setVisibility(View.VISIBLE);
                    h.tvTip.setText(uzanti.toUpperCase());
                    h.tvTip.setBackgroundColor(uzantiRenk(uzanti));
                } else {
                    h.tvTip.setVisibility(View.GONE);
                }
            }

            if (h.cbSecim != null) {
                h.cbSecim.setVisibility(secimModu ? View.VISIBLE : View.GONE);
                h.cbSecim.setChecked(secilen.contains(f.getAbsolutePath()));
            }

            if (h.layoutAc != null) {
                boolean secilidirMi = secimModu && secilen.contains(f.getAbsolutePath());
                if (secilidirMi)
                    h.itemView.setBackgroundColor(0x557c4dff);
                else
                    h.itemView.setBackgroundResource(R.drawable.item_focus_bg);

                h.layoutAc.setOnFocusChangeListener((v, hf) -> {
                    v.setScaleX(hf ? 1.02f : 1.0f);
                    v.setScaleY(hf ? 1.02f : 1.0f);
                    v.setAlpha(hf ? 1.0f : 0.88f);
                });
                if (h.layoutAc != null) {
                    h.layoutAc.setNextFocusUpId(pos == 0 ? R.id.btnAnaDizin : View.NO_ID);
                }
                h.layoutAc.setOnClickListener(v -> {
                    Log.d("SECIM", "Click öncesi focus: " + v.getRootView().findFocus());
                    ac.onAc(oge);
                    Log.d("SECIM", "ac.onAc sonrası focus: " + v.getRootView().findFocus());
                    v.post(() -> {
                        Log.d("SECIM", "post içi focus (requestFocus öncesi): " + v.getRootView().findFocus());
                        v.requestFocus();
                        Log.d("SECIM", "requestFocus sonrası focus: " + v.getRootView().findFocus());
                    });
                });
            }

            if (h.btnIslem != null) {
                h.btnIslem.setOnFocusChangeListener((v, hf) -> {
                    v.setScaleX(hf ? 1.15f : 1.0f);
                    v.setScaleY(hf ? 1.15f : 1.0f);
                });
                h.btnIslem.setOnClickListener(v -> islem.onIslem(oge));
            }
        }

        @Override public int getItemCount() { return liste.size(); }

        private int ikonSec(File f) {
            if (f.isDirectory()) return android.R.drawable.ic_menu_more;
            switch (uzantiAl(f)) {
                case "m3u": case "m3u8":                          return android.R.drawable.ic_media_play;
                case "mp4": case "mkv": case "avi": case "mov":   return android.R.drawable.ic_media_ff;
                case "mp3": case "flac": case "aac": case "ogg":  return android.R.drawable.ic_lock_silent_mode_off;
                case "jpg": case "jpeg": case "png": case "gif": case "webp": return android.R.drawable.ic_menu_gallery;
                case "pdf":                                        return android.R.drawable.ic_menu_agenda;
                case "txt": case "log": case "json": case "xml":  return android.R.drawable.ic_menu_edit;
                case "zip": case "rar": case "7z":                return android.R.drawable.ic_popup_sync;
                case "apk":                                        return android.R.drawable.sym_def_app_icon;
                default:                                           return android.R.drawable.ic_menu_help;
            }
        }

        private int uzantiRenk(String ext) {
            switch (ext) {
                case "m3u": case "m3u8":                          return 0xFF1DB954;
                case "mp4": case "mkv": case "avi": case "mov":   return 0xFF1565C0;
                case "mp3": case "flac": case "aac":              return 0xFF6A1B9A;
                case "jpg": case "jpeg": case "png": case "gif":  return 0xFFE65100;
                case "pdf":                                        return 0xFFB71C1C;
                case "txt": case "log":                           return 0xFF37474F;
                case "zip": case "rar": case "7z":                return 0xFF4E342E;
                case "apk":                                        return 0xFF2E7D32;
                case "json": case "xml":                          return 0xFF00695C;
                default:                                           return 0xFF546E7A;
            }
        }

        static class VH extends RecyclerView.ViewHolder {
            ImageView    ivIkon;
            TextView     tvAd, tvMeta, tvTip;
            CheckBox     cbSecim;
            LinearLayout layoutAc;
            ImageButton  btnIslem;

            VH(@NonNull View v) {
                super(v);
                ivIkon   = v.findViewById(R.id.ivIkon);
                tvAd     = v.findViewById(R.id.tvAd);
                tvMeta   = v.findViewById(R.id.tvMeta);
                tvTip    = v.findViewById(R.id.tvTip);
                cbSecim  = v.findViewById(R.id.cbSecim);
                layoutAc = v.findViewById(R.id.layoutAc);
                btnIslem = v.findViewById(R.id.btnIslem);
            }
        }
    }
}