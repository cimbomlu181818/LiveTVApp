package com.example.livetvapp;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.WindowManager;
import android.media.AudioManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.example.livetvapp.database.AppDatabase;
import com.example.livetvapp.dosyatransferi.FileTransferServer;
import com.example.livetvapp.remotecontrol.RemoteCommandServer;
import com.example.livetvapp.tvbox.Anakontrol;
import com.example.livetvapp.remotecontrol.DeviceDetector;
import android.app.Dialog;
import org.json.JSONObject;
public class MainActivity extends AppCompatActivity {

    private Anakontrol      anakontrol;
    private AppDatabase     database;
    private ApiHelper       apiHelper;

    
    private com.example.livetvapp.dosyatransferi.RemoteControlServer remoteServer;
    private FileTransferServer fileServer;
    private RemoteCommandServer remoteCommandServer;  
    public boolean isRemoteClientConnected() {
        return remoteCommandServer != null && remoteCommandServer.hasActiveClient();
    }
    private boolean dosyaSeciciAktif = false;

    public void setDosyaSeciciAktif(boolean aktif) {
        dosyaSeciciAktif = aktif;
    }
    
    private EditText activeEditText = null;

    
    private final Handler  kontrolHandler  = new Handler(Looper.getMainLooper());
    private       Runnable kontrolRunnable10;
    private       Runnable kontrolRunnable20;

    private static final long DAK_10 = 10 * 60 * 1000L; 
    private static final long DAK_20 = 20 * 60 * 1000L; 

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        
        String deviceType = DeviceDetector.getDeviceTypeString(this);
        Log.d("DEVICE_TYPE", "Cihaz Tipi: " + deviceType);
        System.out.println("📱 Cihaz Tipi: " + deviceType);

        
        if (!DeviceDetector.isPhone(this)) {
            Log.d("DEVICE_TYPE", "→ SUNUCU MODU (kontrol edilen cihaz)");
        } else {
            Log.d("DEVICE_TYPE", "→ İSTEMCİ MODU (kontrol eden cihaz)");
        }

        
        apiHelper = new ApiHelper();

        SharedPreferences girisTercihleri = getSharedPreferences("azsh_giris", Context.MODE_PRIVATE);
        String kayitliEmail = girisTercihleri.getString("email", null);

        if (kayitliEmail == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        apiHelper.erisimKontrol(kayitliEmail, cihazIdGetir(), new ApiHelper.ApiListener() {
            @Override
            public void onBasarili(JSONObject sonuc) {
                boolean bakimModu = sonuc.optBoolean("bakim_modu", false);
                if (bakimModu) {
                    bakimEkraniniGoster(sonuc.optString("mesaj", "Uygulama bakımda."), null);
                    return;
                }
                String durum = sonuc.optString("durum", "");
                boolean erisim = sonuc.optBoolean("erisim", false);

                if (erisim) {
                    
                } else if ("mail_dogrulanmadi".equals(durum)) {
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                } else {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.putExtra("trial_doldu", true);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onHata(String hata) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.putExtra("hata_mesaji", "İnternet bağlantısı yok. Lütfen bağlantınızı kontrol edin.");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        super.onCreate(savedInstanceState);

        database = AppDatabase.getInstance(this);
        System.out.println("✅ Database initialized");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setupFullScreen();

        getOnBackPressedDispatcher().addCallback(
                this, new androidx.activity.OnBackPressedCallback(true) {
                    @Override public void handleOnBackPressed() { }
                });

        anakontrol = new Anakontrol(this);
        anakontrol.baslat();

        
        startRemoteServer();
        startFileServer();

        
        periyodikKontrolBaslat();

        
        if (!DeviceDetector.isPhone(this)) {
            startRemoteControlServer();
        }
    }

    


    private void startRemoteControlServer() {
        if (remoteCommandServer == null) {
            remoteCommandServer = new RemoteCommandServer(this,
                    new RemoteCommandServer.CommandCallback() {
                        @Override
                        public void onCommandReceived(String command, String data) {
                            Log.d("RemoteControl", "Komut alındı: " + command + " data: " + data);
                            runOnUiThread(() -> handleRemoteCommand(command, data));
                        }

                        @Override
                        public void onClientConnected() {
                            runOnUiThread(() -> {
                                if (anakontrol != null && anakontrol.getBar() != null)
                                    anakontrol.getBar().setRemoteAktif(true);
                            });
                        }

                        @Override
                        public void onClientDisconnected() {
                            runOnUiThread(() -> {
                                if (anakontrol != null && anakontrol.getBar() != null)
                                    anakontrol.getBar().setRemoteAktif(false);
                            });
                        }
                    });
            
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
                    .registerReceiver(
                            new android.content.BroadcastReceiver() {
                                @Override
                                public void onReceive(android.content.Context ctx, android.content.Intent intent) {
                                    dosyaSeciciAktif = intent.getBooleanExtra("aktif", false);
                                    Log.d("RemoteControl", "DosyaSecici durumu: " + dosyaSeciciAktif);
                                }
                            },
                            new android.content.IntentFilter("com.example.livetvapp.DOSYA_SECICI_DURUM")
                    );
            remoteCommandServer.start();
        }
    }

    private void stopRemoteControlServer() {
        if (remoteCommandServer != null) {
            remoteCommandServer.stop();
            remoteCommandServer = null;
        }
    }
    
    private boolean isInsideDialog(View view) {
        ViewParent parent = view.getParent();
        while (parent != null) {
            if (parent instanceof Dialog) {
                return true;
            }
            if (parent instanceof View) {
                parent = ((View) parent).getParent();
            } else {
                break;
            }
        }
        return false;
    }
    private void handleRemoteCommand(String command, String data) {
        
        android.content.Intent broadcastIntent = new android.content.Intent("com.example.livetvapp.REMOTE_COMMAND");
        broadcastIntent.putExtra("command", command);
        broadcastIntent.putExtra("data", data);
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

        
        if (dosyaSeciciAktif) return;
        View focused = getCurrentFocus();
        if (focused != null && isInsideDialog(focused)) {
            int keyCode = getKeyCodeFromCommand(command);
            if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                KeyEvent downEvent = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
                focused.dispatchKeyEvent(downEvent);
                KeyEvent upEvent = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
                focused.dispatchKeyEvent(upEvent);
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                    focused.performClick();
                }
                Log.d("RemoteControl", "Komut dialog'a iletildi: " + command);
                return;
            }
        }
        Log.d("RemoteControl", "İşlenecek komut: " + command + " data: " + data);

        if (anakontrol == null) return;

        switch (command) {
            case "DPAD_UP":
            case "DPAD_DOWN":
            case "DPAD_LEFT":
            case "DPAD_RIGHT":
                KeyEvent eventDir = new KeyEvent(KeyEvent.ACTION_DOWN, getKeyCodeFromCommand(command));
                anakontrol.onKeyDown(getKeyCodeFromCommand(command), eventDir);
                break;
            case "DPAD_CENTER":
                KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, getKeyCodeFromCommand(command));
                anakontrol.onKeyDown(getKeyCodeFromCommand(command), event);
                break;
            case "MENU":
                anakontrol.ayarlarPaneliAc();
                break;
            case "BACK":
                anakontrol.onKeyDown(KeyEvent.KEYCODE_BACK, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
                break;
            case "VOLUME_UP":
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                if (audioManager != null) {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                }
                break;
            case "VOLUME_DOWN":
                AudioManager audioManager2 = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                if (audioManager2 != null) {
                    audioManager2.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                }
                break;
            case "START_KEYBOARD_MODE":
                
                if (focused == null) focused = getCurrentFocus();
                if (focused instanceof EditText) {
                    activeEditText = (EditText) focused;
                    activeEditText.setBackgroundColor(0xFFFFDD00);
                    activeEditText.setTextColor(0xFF000000);
                    activeEditText.setHintTextColor(0xFF000000);
                    Log.d("RemoteControl", "Klavye modu başlatıldı: " + activeEditText.getClass().getSimpleName());
                } else {
                    activeEditText = null;
                    Log.w("RemoteControl", "Klavye modu başlatılamadı: focuslanmış EditText yok");
                }
                break;
            case "TEXT_CHANGED":
                if (activeEditText != null) {
                    int selectionStart = activeEditText.getSelectionStart();
                    activeEditText.setText(data);
                    if (selectionStart >= 0 && selectionStart <= data.length()) {
                        activeEditText.setSelection(selectionStart);
                    } else {
                        activeEditText.setSelection(data.length());
                    }
                    Log.d("RemoteControl", "Metin güncellendi: " + data);
                } else {
                    Log.w("RemoteControl", "TEXT_CHANGED alındı ama aktif EditText yok");
                }
                break;
            case "PLAY_PAUSE":
                if (anakontrol != null && anakontrol.getBar() != null)
                    anakontrol.getBar().oynatDuraklat();
                break;
            case "REWIND":
                if (anakontrol != null && anakontrol.getBar() != null)
                    anakontrol.getBar().geriSar();
                break;
            case "FORWARD":
                if (anakontrol != null && anakontrol.getBar() != null)
                    anakontrol.getBar().ileriSar();
                break;
            case "MUTE":
                if (anakontrol != null && anakontrol.getBar() != null)
                    anakontrol.getBar().toggleMute();
                break;
            case "KANALLAR":
                if (anakontrol != null)
                    anakontrol.onKeyDown(KeyEvent.KEYCODE_ENTER,
                            new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                break;
            default:
                Log.w("RemoteControl", "Bilinmeyen komut: " + command);
                break;
        }
    }

    
    private int getKeyCodeFromCommand(String command) {
        switch (command) {
            case "DPAD_UP": return KeyEvent.KEYCODE_DPAD_UP;
            case "DPAD_DOWN": return KeyEvent.KEYCODE_DPAD_DOWN;
            case "DPAD_LEFT": return KeyEvent.KEYCODE_DPAD_LEFT;
            case "DPAD_RIGHT": return KeyEvent.KEYCODE_DPAD_RIGHT;
            case "DPAD_CENTER": return KeyEvent.KEYCODE_DPAD_CENTER;
            default: return KeyEvent.KEYCODE_UNKNOWN;
        }
    }

    
    private void startRemoteServer() {
        if (remoteServer == null) {
            remoteServer = new com.example.livetvapp.dosyatransferi.RemoteControlServer(this);
            remoteServer.start();
            android.util.Log.d("📡[RemoteServer]", "✅ Server başlatıldı (MainActivity)");
        }
    }

    private void stopRemoteServer() {
        if (remoteServer != null) {
            remoteServer.stop();
            remoteServer = null;
            android.util.Log.d("📡[RemoteServer]", "✅ Server durduruldu");
        }
    }

    private void startFileServer() {
        if (fileServer == null) {
            fileServer = new FileTransferServer(this);
            fileServer.setCallback(new FileTransferServer.FileTransferCallback() {
                @Override
                public void onTransferStarted(String fileName, long fileSize) {
                    runOnUiThread(() -> {
                        android.util.Log.d("📥[FileServer]", "Transfer başladı: " + fileName);
                    });
                }

                @Override
                public void onTransferProgress(int percent) {
                    android.util.Log.d("📥[FileServer]", "İlerleme: " + percent + "%");
                }

                @Override
                public void onTransferComplete(String filePath) {
                    runOnUiThread(() -> {
                        android.util.Log.d("📥[FileServer]", "✅ Dosya kaydedildi: " + filePath);
                    });
                }

                @Override
                public void onTransferError(String error) {
                    runOnUiThread(() -> {
                        android.util.Log.e("📥[FileServer]", "❌ Hata: " + error);
                    });
                }
            });
            fileServer.start();
            android.util.Log.d("📥[FileServer]", "✅ FileTransferServer başlatıldı (MainActivity)");
        }
    }

    private void stopFileServer() {
        if (fileServer != null) {
            fileServer.stop();
            fileServer = null;
            android.util.Log.d("📥[FileServer]", "✅ FileServer durduruldu");
        }
    }

    
    private void periyodikKontrolBaslat() {
        kontrolRunnable10 = () -> erisimKontrolYap();
        kontrolRunnable20 = () -> erisimKontrolYap();

        kontrolHandler.postDelayed(kontrolRunnable10, DAK_10);
        kontrolHandler.postDelayed(kontrolRunnable20, DAK_20);
    }

    private void periyodikKontrolIptal() {
        if (kontrolRunnable10 != null) kontrolHandler.removeCallbacks(kontrolRunnable10);
        if (kontrolRunnable20 != null) kontrolHandler.removeCallbacks(kontrolRunnable20);
    }

    
    private String cihazIdGetir() {
        return android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
    }

    
    private void erisimKontrolYap() {
        if (apiHelper == null) return;

        SharedPreferences girisTercihleri = getSharedPreferences("azsh_giris", Context.MODE_PRIVATE);
        String kayitliEmail = girisTercihleri.getString("email", null);
        if (kayitliEmail == null) return;

        apiHelper.erisimKontrol(kayitliEmail, cihazIdGetir(), new ApiHelper.ApiListener() {
            @Override
            public void onBasarili(JSONObject sonuc) {
                boolean bakimModu = sonuc.optBoolean("bakim_modu", false);
                if (bakimModu) {
                    bakimEkraniniGoster(sonuc.optString("mesaj", "Uygulama bakımda."), null);
                    return;
                }
                String durum = sonuc.optString("durum", "");
                boolean erisim = sonuc.optBoolean("erisim", false);

                if (erisim) {
                    
                } else if ("mail_dogrulanmadi".equals(durum)) {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.putExtra("trial_doldu", true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onHata(String hata) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.putExtra("hata_mesaji", "İnternet bağlantısı yok. Lütfen bağlantınızı kontrol edin.");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    
    @Override
    public void onPictureInPictureModeChanged(
            boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (anakontrol != null) {
            anakontrol.onPictureInPictureModeChanged(isInPictureInPictureMode);
        }
    }

    private void setupFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            WindowInsetsControllerCompat controller =
                    new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        boolean inPiP = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && isInPictureInPictureMode();
        if (hasFocus && !inPiP) {
            setupFullScreen();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (anakontrol != null) {
            anakontrol.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (anakontrol != null && anakontrol.dispatchTouchEvent(ev)) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode()) {
            return super.onKeyDown(keyCode, event);
        }
        if (anakontrol != null) {
            boolean handled = anakontrol.onKeyDown(keyCode, event);
            if (handled) return true;
            if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                finish();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupFullScreen();
        if (anakontrol != null) anakontrol.onResume();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        periyodikKontrolIptal();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        
        stopRemoteServer();
        stopFileServer();
        stopRemoteControlServer();  

        if (anakontrol != null) {
            anakontrol.temizlikYap();
        }
    }

    
    private void bakimEkraniniGoster(String mesaj, String url) {
        runOnUiThread(() -> {
            android.widget.FrameLayout kaplama = new android.widget.FrameLayout(this);
            kaplama.setBackgroundColor(0xFF0A0A0A);
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
            ikon.setText("⚠️");
            ikon.setTextSize(64);
            ikon.setGravity(android.view.Gravity.CENTER);
            icerik.addView(ikon);

            android.widget.Space bosluk1 = new android.widget.Space(this);
            bosluk1.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 40));
            icerik.addView(bosluk1);

            android.widget.TextView tvMesaj = new android.widget.TextView(this);
            tvMesaj.setText(mesaj);
            tvMesaj.setTextColor(0xFFFFFFFF);
            tvMesaj.setTextSize(20);
            tvMesaj.setGravity(android.view.Gravity.CENTER);
            tvMesaj.setLineSpacing(8, 1.2f);
            icerik.addView(tvMesaj);

            if (url != null && !url.isEmpty()) {
                android.widget.Space bosluk2 = new android.widget.Space(this);
                bosluk2.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 60));
                icerik.addView(bosluk2);

                android.widget.Button btnIndir = new android.widget.Button(this);
                btnIndir.setText("İNDİR / DAHA FAZLA BİLGİ");
                btnIndir.setTextColor(0xFFFFFFFF);
                btnIndir.setBackgroundColor(0xFFCC0000);
                btnIndir.setTextSize(16);
                btnIndir.setPadding(40, 20, 40, 20);
                btnIndir.setOnClickListener(v -> {
                    android.content.Intent browserIntent = new android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(url));
                    startActivity(browserIntent);
                });
                icerik.addView(btnIndir);
            }

            kaplama.addView(icerik);
            addContentView(kaplama, new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

            getOnBackPressedDispatcher().addCallback(
                    this, new androidx.activity.OnBackPressedCallback(true) {
                        @Override public void handleOnBackPressed() { }
                    });
        });
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (anakontrol != null) {
            anakontrol.onRequestPermissionsResult(requestCode, grantResults);
        }
    }

    
    public Anakontrol getAnakontrol() {
        return anakontrol;
    }

}