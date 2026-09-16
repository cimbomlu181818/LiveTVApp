package com.example.livetvapp.dosyatransferi;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.livetvapp.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DeviceScanActivity extends AppCompatActivity {

    private static final String EXTRA_FILE_PATHS = "file_paths";
    private static final String EXTRA_MULTI_FILE_MODE = "multi_file_mode";

    
    private TextView tvDosyaBadge;
    private TextView tvTaramaDurum;
    private TextView tvSure;
    private TextView tvBulunanSayisi;
    private TextView tvYenileMetin;
    private ProgressBar progressBar;
    private RecyclerView rvCihazlar;
    private Button btnYenile;
    private Button btnIptal;

    private CihazAdapter adapter;
    private NetworkScanner networkScanner;
    private List<RemoteDevice> deviceList = new ArrayList<>();
    private List<File> filesToSend = new ArrayList<>();
    private boolean isMultiFileMode = false;

    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private int scanSeconds = 0;
    private boolean isScanning = false;
    private boolean scanCompleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);

        initViews();
        getIntentData();
        startScan();
    }

    private void initViews() {
        tvDosyaBadge = findViewById(R.id.tvDosyaBadge);
        tvTaramaDurum = findViewById(R.id.tvTaramaDurum);
        tvSure = findViewById(R.id.tvSure);
        tvBulunanSayisi = findViewById(R.id.tvBulunanSayisi);
        tvYenileMetin = findViewById(R.id.tvYenileMetin);
        progressBar = findViewById(R.id.progressBar);
        rvCihazlar = findViewById(R.id.rvCihazlar);
        btnYenile = findViewById(R.id.btnYenile);
        btnIptal = findViewById(R.id.btnIptal);

        rvCihazlar.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new CihazAdapter(deviceList, device -> {
            if (isMultiFileMode) {
                startMultiFileTransfer(device);
            } else {
                startSingleFileTransfer(device);
            }
        });
        rvCihazlar.setAdapter(adapter);

        btnYenile.setOnClickListener(v -> {
            if (isScanning) return;
            startScan();
        });
        btnIptal.setOnClickListener(v -> finish());

        tvYenileMetin.setOnClickListener(v -> {
            if (isScanning) return;
            startScan();
        });
        tvYenileMetin.setFocusable(true);
        tvYenileMetin.setClickable(true);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        isMultiFileMode = intent.getBooleanExtra(EXTRA_MULTI_FILE_MODE, false);
        ArrayList<String> filePaths = intent.getStringArrayListExtra(EXTRA_FILE_PATHS);
        if (filePaths != null) {
            for (String path : filePaths) {
                File f = new File(path);
                if (f.exists() && f.isFile()) {
                    filesToSend.add(f);
                }
            }
        }
        int dosyaSayisi = filesToSend.size();
        long toplamBoyut = 0;
        for (File f : filesToSend) toplamBoyut += f.length();
        tvDosyaBadge.setText(dosyaSayisi + " dosya (" + formatFileSize(toplamBoyut) + ")");
    }

    private void startScan() {
        
        deviceList.clear();
        adapter.notifyDataSetChanged();
        tvBulunanSayisi.setText("📡 Bulunan cihazlar: 0");
        tvTaramaDurum.setText("🔍 Ağ taranıyor...");
        tvSure.setText("⏱️ 0/8 sn");
        progressBar.setIndeterminate(true);
        progressBar.setProgress(0);
        isScanning = true;
        scanCompleted = false;
        rvCihazlar.setVisibility(View.GONE);

        
        scanSeconds = 0;
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isScanning && scanSeconds < 8) {
                    scanSeconds++;
                    tvSure.setText("⏱️ " + scanSeconds + "/8 sn");
                    mainHandler.postDelayed(this, 1000);
                } else if (scanSeconds >= 8 && !scanCompleted) {
                    tvTaramaDurum.setText("✅ Tarama tamamlandı");
                    progressBar.setIndeterminate(false);
                    progressBar.setProgress(100);
                    isScanning = false;
                    scanCompleted = true;
                    if (deviceList.isEmpty()) {
                        tvTaramaDurum.setText("❌ Cihaz bulunamadı");
                    }
                }
            }
        }, 1000);

        networkScanner = new NetworkScanner(this);

        
        networkScanner.scanForDevicesRealtime(new NetworkScanner.RealTimeScanCallback() {
            @Override
            public void onDeviceFound(RemoteDevice device) {
                runOnUiThread(() -> {
                    Log.d("DeviceScan", "🎉 Cihaz bulundu: " + device.getName() + " - " + device.getIpAddress());

                    
                    boolean exists = false;
                    for (RemoteDevice d : deviceList) {
                        if (d.getIpAddress().equals(device.getIpAddress())) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        deviceList.add(device);
                        adapter.notifyDataSetChanged();
                        tvBulunanSayisi.setText("📡 Bulunan cihazlar: " + deviceList.size());
                        rvCihazlar.setVisibility(View.VISIBLE);
                        tvTaramaDurum.setText("✅ Cihaz bulundu, seçiniz");

                        
                        progressBar.setIndeterminate(false);
                        progressBar.setProgress(100);
                    }
                });
            }

            @Override
            public void onScanComplete(List<RemoteDevice> devices) {
                runOnUiThread(() -> {
                    Log.d("DeviceScan", "✅ Tarama tamamlandı. Toplam: " + devices.size() + " cihaz");
                    scanCompleted = true;
                    isScanning = false;
                    progressBar.setIndeterminate(false);
                    progressBar.setProgress(100);

                    if (deviceList.isEmpty()) {
                        tvTaramaDurum.setText("❌ Cihaz bulunamadı");
                        tvSure.setText("⏱️ " + scanSeconds + "/8 sn (Tamamlandı)");
                    } else {
                        tvTaramaDurum.setText("✅ " + deviceList.size() + " cihaz bulundu");
                        tvSure.setText("⏱️ " + scanSeconds + "/8 sn (Tamamlandı)");
                    }
                });
            }

            @Override
            public void onScanFailed(String error) {
                runOnUiThread(() -> {
                    Log.e("DeviceScan", "❌ Tarama hatası: " + error);
                    tvTaramaDurum.setText("❌ Tarama hatası: " + error);
                    Toast.makeText(DeviceScanActivity.this, "Tarama hatası: " + error, Toast.LENGTH_SHORT).show();
                    isScanning = false;
                    scanCompleted = true;
                    progressBar.setIndeterminate(false);
                });
            }
        });
    }
    private void startSingleFileTransfer(RemoteDevice target) {
        if (filesToSend.isEmpty()) return;
        File file = filesToSend.get(0);
        Intent intent = new Intent(this, FileTransferActivity.class);
        intent.putExtra("target_ip", target.getIpAddress());
        intent.putExtra("target_name", target.getName());
        intent.putExtra("file_path", file.getAbsolutePath());
        intent.putExtra("multi_file_mode", false);
        startActivity(intent);
        finish();
    }

    private void startMultiFileTransfer(RemoteDevice target) {
        if (filesToSend.isEmpty()) return;
        Intent intent = new Intent(this, FileTransferActivity.class);
        intent.putExtra("target_ip", target.getIpAddress());
        intent.putExtra("target_name", target.getName());
        intent.putExtra("multi_file_mode", true);
        ArrayList<String> paths = new ArrayList<>();
        for (File f : filesToSend) paths.add(f.getAbsolutePath());
        intent.putStringArrayListExtra("file_paths", paths);
        startActivity(intent);
        finish();
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB"};
        int i = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, i), units[i]);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);
        if (networkScanner != null) {
            networkScanner.stopScan();
        }
    }

    
    public static class CihazAdapter extends RecyclerView.Adapter<CihazAdapter.ViewHolder> {
        private List<RemoteDevice> devices;
        private OnDeviceClickListener listener;

        public interface OnDeviceClickListener {
            void onDeviceClick(RemoteDevice device);
        }

        public CihazAdapter(List<RemoteDevice> devices, OnDeviceClickListener listener) {
            this.devices = devices;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device_scan, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RemoteDevice device = devices.get(position);
            holder.tvAd.setText(device.getName());
            holder.tvIp.setText(device.getIpAddress());
            holder.itemView.setOnClickListener(v -> listener.onDeviceClick(device));
            holder.itemView.setFocusable(true);
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvAd, tvIp;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvAd = itemView.findViewById(R.id.device_name);
                tvIp = itemView.findViewById(R.id.device_ip);
            }
        }
    }
}