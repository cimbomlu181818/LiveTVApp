package com.example.livetvapp.dosyatransferi;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.livetvapp.R;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FileTransferActivity extends AppCompatActivity implements FileTransferClient.FileTransferCallback {

    private static final String EXTRA_TARGET_IP       = "target_ip";
    private static final String EXTRA_TARGET_NAME     = "target_name";
    private static final String EXTRA_FILE_PATH       = "file_path";
    private static final String EXTRA_MULTI_FILE_MODE = "multi_file_mode";
    private static final String EXTRA_FILE_PATHS      = "file_paths";

    
    private TextView    tvHedefCihaz, tvToplamBoyut, tvSpeed, tvStatus, tvRemaining;
    private ProgressBar pbToplam;
    private Button      btnCancel;
    private RecyclerView rvDosyalar;

    
    private TransferAdapter adapter;
    private List<TransferItem> transferItems = new ArrayList<>();

    
    private FileTransferClient transferClient;
    private String targetIp;
    private String targetName;

    private boolean isMultiFileMode = false;
    private List<File> filesToSend  = new ArrayList<>();
    private int currentFileIndex    = 0;
    private long totalFileSize      = 0;
    private long totalBytesSent     = 0;
    private int successCount = 0;
    private int errorCount   = 0;
    
    private File fileToSend;

    private long startTime;
    private long currentFileSize;
    private Handler handler = new Handler(Looper.getMainLooper());
    private long lastBytesTransferred = 0;

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_transfer);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, android.R.color.black));

        initViews();
        getIntentData();
        startNextFile();
    }

    private void initViews() {
        tvHedefCihaz  = findViewById(R.id.tvHedefCihaz);
        tvToplamBoyut = findViewById(R.id.tvToplamBoyut);
        tvSpeed       = findViewById(R.id.tvSpeed);
        tvStatus      = findViewById(R.id.tvStatus);
        tvRemaining   = findViewById(R.id.tvRemaining);
        pbToplam      = findViewById(R.id.pbToplam);
        btnCancel     = findViewById(R.id.btnCancel);
        rvDosyalar    = findViewById(R.id.rvDosyalar);

        rvDosyalar.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransferAdapter(transferItems);
        rvDosyalar.setAdapter(adapter);

        btnCancel.setOnClickListener(v -> cancelTransfer());
    }

    private void getIntentData() {
        Intent intent = getIntent();
        targetIp      = intent.getStringExtra(EXTRA_TARGET_IP);
        targetName    = intent.getStringExtra(EXTRA_TARGET_NAME);
        isMultiFileMode = intent.getBooleanExtra(EXTRA_MULTI_FILE_MODE, false);

        tvHedefCihaz.setText(targetName != null ? targetName : targetIp);

        if (isMultiFileMode) {
            ArrayList<String> filePaths = intent.getStringArrayListExtra(EXTRA_FILE_PATHS);
            if (filePaths != null) {
                for (String path : filePaths) {
                    File f = new File(path);
                    if (f.exists() && f.isFile()) {
                        filesToSend.add(f);
                        totalFileSize += f.length();
                        transferItems.add(new TransferItem(f.getName(), f.length()));
                    }
                }
            }
        } else {
            String filePath = intent.getStringExtra(EXTRA_FILE_PATH);
            if (filePath != null) {
                fileToSend = new File(filePath);
                currentFileSize = fileToSend.length();
                totalFileSize   = currentFileSize;
                transferItems.add(new TransferItem(fileToSend.getName(), fileToSend.length()));
            }
        }

        adapter.notifyDataSetChanged();
        updateToplamUI();
    }

    

    private void startNextFile() {
        if (!isMultiFileMode) {
            startSingleFileTransfer();
            return;
        }

        if (currentFileIndex >= filesToSend.size()) {
            onAllTransfersComplete();
            return;
        }

        File current = filesToSend.get(currentFileIndex);
        currentFileSize       = current.length();
        lastBytesTransferred  = 0;
        startTime             = System.currentTimeMillis();

        transferItems.get(currentFileIndex).setDurum(TransferItem.DURUM_AKTIF);
        adapter.notifyItemChanged(currentFileIndex);

        tvStatus.setText("Gönderiliyor: " + current.getName());

        transferClient = new FileTransferClient();
        transferClient.setCallback(this);
        transferClient.sendFile(targetIp, current);
    }

    private void startSingleFileTransfer() {
        if (fileToSend == null || targetIp == null) {
            Toast.makeText(this, "Transfer başlatılamadı", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        lastBytesTransferred = 0;
        startTime = System.currentTimeMillis();

        transferItems.get(0).setDurum(TransferItem.DURUM_AKTIF);
        adapter.notifyItemChanged(0);

        tvStatus.setText("Gönderiliyor...");

        transferClient = new FileTransferClient();
        transferClient.setCallback(this);
        transferClient.sendFile(targetIp, fileToSend);
    }

    

    @Override
    public void onTransferStarted(String fileName, long fileSize) {
        runOnUiThread(() -> tvStatus.setText("Bağlanıyor..."));
    }

    @Override
    public void onTransferProgress(final int percent) {
        runOnUiThread(() -> {
            int idx = isMultiFileMode ? currentFileIndex : 0;

            
            TransferItem item = transferItems.get(idx);
            item.setProgress(percent);
            adapter.notifyItemChanged(idx);

            
            lastBytesTransferred = (currentFileSize * percent) / 100;
            long now      = System.currentTimeMillis();
            long timeDelta = (now - startTime) / 1000;

            if (timeDelta > 0) {
                long speed = lastBytesTransferred / timeDelta;
                tvSpeed.setText(formatSpeed(speed));

                if (speed > 0 && percent < 100) {
                    long remainingBytes   = currentFileSize - lastBytesTransferred;
                    long remainingSeconds = remainingBytes / speed;
                    tvRemaining.setText(formatTime(remainingSeconds));
                }
            }

            
            long sentSoFar = totalBytesSent + lastBytesTransferred;
            int  totalPct  = (int) (sentSoFar * 100 / totalFileSize);
            pbToplam.setProgress(totalPct);
            tvToplamBoyut.setText(formatFileSize(sentSoFar) + " / " + formatFileSize(totalFileSize));
        });
    }

    @Override
    public void onTransferComplete() {
        runOnUiThread(() -> {
            int idx = isMultiFileMode ? currentFileIndex : 0;

            
            TransferItem item = transferItems.get(idx);
            item.setProgress(100);
            item.setDurum(TransferItem.DURUM_TAMAM);
            adapter.notifyItemChanged(idx);

            totalBytesSent += currentFileSize;
            successCount++;
            if (isMultiFileMode) {
                currentFileIndex++;
                if (currentFileIndex < filesToSend.size()) {
                    
                    tvStatus.setText("Hazırlanıyor...");
                    tvSpeed.setText("0 B/s");
                    tvRemaining.setText("Hesaplanıyor...");
                    new Handler().postDelayed(this::startNextFile, 600);
                } else {
                    onAllTransfersComplete();
                }
            } else {
                onAllTransfersComplete();
            }
        });
    }

    @Override
    public void onTransferError(String error) {
        runOnUiThread(() -> {
            int idx = isMultiFileMode ? currentFileIndex : 0;
            if (idx < transferItems.size()) {
                transferItems.get(idx).setDurum(TransferItem.DURUM_HATA);
                adapter.notifyItemChanged(idx);
            }

            errorCount++;

            if (isMultiFileMode) {
                
                currentFileIndex++;
                if (currentFileIndex < filesToSend.size()) {
                    tvStatus.setText("Hata oluştu, sonraki dosyaya geçiliyor...");
                    tvSpeed.setText("0 B/s");
                    tvRemaining.setText("Hesaplanıyor...");
                    new Handler().postDelayed(this::startNextFile, 1500);
                } else {
                    
                    showTransferSummary();
                }
            } else {
                
                btnCancel.setText("Kapat");
                btnCancel.setOnClickListener(v -> finish());
                tvStatus.setText("❌ Hata: " + error);
                Toast.makeText(this, "❌ Transfer hatası: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
    private void showTransferSummary() {
        pbToplam.setProgress(100);
        tvToplamBoyut.setText(formatFileSize(totalBytesSent) + " / " + formatFileSize(totalFileSize));
        tvStatus.setText(String.format("✅ Tamamlandı: %d başarılı, %d hata", successCount, errorCount));
        tvSpeed.setText("");
        tvRemaining.setText("");
        btnCancel.setText("✓ TAMAM");
        btnCancel.setOnClickListener(v -> finish());

        Toast.makeText(this,
                String.format(Locale.getDefault(),
                        "Transfer tamamlandı.\n✅ Başarılı: %d dosya\n❌ Hatalı: %d dosya",
                        successCount, errorCount),
                Toast.LENGTH_LONG).show();
    }
    private void onAllTransfersComplete() {
        pbToplam.setProgress(100);
        tvToplamBoyut.setText(formatFileSize(totalFileSize) + " / " + formatFileSize(totalFileSize));
        tvStatus.setText("✅ Tüm dosyalar başarıyla gönderildi!");
        tvSpeed.setText("Tamamlandı");
        tvRemaining.setText("");
        btnCancel.setText("✓ TAMAM");
        btnCancel.setOnClickListener(v -> finish());

        Toast.makeText(this,
                "✅ " + (isMultiFileMode ? filesToSend.size() + " dosya" : fileToSend.getName()) + " başarıyla gönderildi!",
                Toast.LENGTH_LONG).show();
    }

    private void cancelTransfer() {
        if (transferClient != null) transferClient.cancel();
        Toast.makeText(this, "Transfer iptal edildi", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void updateToplamUI() {
        tvToplamBoyut.setText("0 B / " + formatFileSize(totalFileSize));
        pbToplam.setProgress(0);
    }

    

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int i = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, i)) + " " + units[i];
    }

    private String formatSpeed(long bps) {
        return formatFileSize(bps) + "/s";
    }

    private String formatTime(long seconds) {
        if (seconds <= 0) return "0 sn";
        long h = seconds / 3600, m = (seconds % 3600) / 60, s = seconds % 60;
        if (h > 0) return String.format(Locale.getDefault(), "%d sa %d dk", h, m);
        if (m > 0) return String.format(Locale.getDefault(), "%d dk %d sn", m, s);
        return String.format(Locale.getDefault(), "%d sn", s);
    }

    

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (transferClient != null) transferClient.cancel();
    }

    

    public static void start(Context context, String targetIp, String targetName, String filePath) {
        Intent intent = new Intent(context, FileTransferActivity.class);
        intent.putExtra(EXTRA_TARGET_IP,       targetIp);
        intent.putExtra(EXTRA_TARGET_NAME,     targetName);
        intent.putExtra(EXTRA_FILE_PATH,       filePath);
        intent.putExtra(EXTRA_MULTI_FILE_MODE, false);
        context.startActivity(intent);
    }

    
    
    

    public static class TransferItem {
        public static final int DURUM_BEKLIYOR = 0;
        public static final int DURUM_AKTIF    = 1;
        public static final int DURUM_TAMAM    = 2;
        public static final int DURUM_HATA     = 3;

        private final String dosyaAd;
        private final long   dosyaBoyut;
        private int          progress = 0;
        private int          durum    = DURUM_BEKLIYOR;

        public TransferItem(String dosyaAd, long dosyaBoyut) {
            this.dosyaAd    = dosyaAd;
            this.dosyaBoyut = dosyaBoyut;
        }

        public String getDosyaAd()   { return dosyaAd; }
        public long   getDosyaBoyut(){ return dosyaBoyut; }
        public int    getProgress()  { return progress; }
        public int    getDurum()     { return durum; }

        public void setProgress(int p) { this.progress = p; }
        public void setDurum(int d)    { this.durum    = d; }
    }

    
    
    

    public static class TransferAdapter extends RecyclerView.Adapter<TransferAdapter.VH> {

        private final List<TransferItem> liste;

        public TransferAdapter(List<TransferItem> liste) {
            this.liste = liste;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_transfer_dosya, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            TransferItem item = liste.get(pos);

            h.tvDosyaAd.setText(item.getDosyaAd());
            h.pbDosya.setProgress(item.getProgress());

            
            h.ivDosyaIkon.setImageResource(ikonSec(item.getDosyaAd()));

            
            switch (item.getDurum()) {
                case TransferItem.DURUM_BEKLIYOR:
                    h.tvDosyaDurum.setText("Bekliyor");
                    h.tvDosyaDurum.setTextColor(0xFF666688);
                    setProgressColor(h.pbDosya, 0xFF2a2b3f);
                    break;
                case TransferItem.DURUM_AKTIF:
                    h.tvDosyaDurum.setText("%" + item.getProgress());
                    h.tvDosyaDurum.setTextColor(0xFF4FC3F7);
                    setProgressColor(h.pbDosya, 0xFF4FC3F7);
                    break;
                case TransferItem.DURUM_TAMAM:
                    h.tvDosyaDurum.setText("✓ Tamam");
                    h.tvDosyaDurum.setTextColor(0xFF1DB954);
                    setProgressColor(h.pbDosya, 0xFF1DB954);
                    break;
                case TransferItem.DURUM_HATA:
                    h.tvDosyaDurum.setText("✗ Hata");
                    h.tvDosyaDurum.setTextColor(0xFFDC322F);
                    setProgressColor(h.pbDosya, 0xFFDC322F);
                    break;
            }
        }

        @Override
        public int getItemCount() { return liste.size(); }

        private int ikonSec(String ad) {
            int dot = ad.lastIndexOf('.');
            if (dot < 0) return android.R.drawable.ic_menu_send;
            switch (ad.substring(dot + 1).toLowerCase()) {
                case "mp4": case "mkv": case "avi": case "mov":
                    return android.R.drawable.ic_media_ff;
                case "mp3": case "flac": case "aac": case "ogg":
                    return android.R.drawable.ic_lock_silent_mode_off;
                case "jpg": case "jpeg": case "png": case "gif":
                    return android.R.drawable.ic_menu_gallery;
                case "pdf":
                    return android.R.drawable.ic_menu_agenda;
                case "apk":
                    return android.R.drawable.sym_def_app_icon;
                case "zip": case "rar": case "7z":
                    return android.R.drawable.ic_popup_sync;
                case "m3u": case "m3u8":
                    return android.R.drawable.ic_media_play;
                default:
                    return android.R.drawable.ic_menu_send;
            }
        }

        private void setProgressColor(ProgressBar pb, int color) {
            android.graphics.drawable.Drawable d = pb.getProgressDrawable();
            if (d != null) {
                androidx.core.graphics.drawable.DrawableCompat.setTint(
                        androidx.core.graphics.drawable.DrawableCompat.wrap(d).mutate(), color);
            }
        }

        static class VH extends RecyclerView.ViewHolder {
            ImageView  ivDosyaIkon;
            TextView   tvDosyaAd, tvDosyaDurum;
            ProgressBar pbDosya;

            VH(@NonNull View v) {
                super(v);
                ivDosyaIkon  = v.findViewById(R.id.ivDosyaIkon);
                tvDosyaAd    = v.findViewById(R.id.tvDosyaAd);
                tvDosyaDurum = v.findViewById(R.id.tvDosyaDurum);
                pbDosya      = v.findViewById(R.id.pbDosya);
            }
        }
    }
}