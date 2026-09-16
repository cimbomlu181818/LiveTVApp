package com.example.livetvapp.remotecontrol;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.livetvapp.R;
import com.example.livetvapp.dosyatransferi.NetworkScanner;
import com.example.livetvapp.dosyatransferi.RemoteDevice;
import java.util.ArrayList;
import java.util.List;

public class RemoteDeviceScanActivity extends AppCompatActivity {

    private TextView tvTaramaDurum, tvSure, tvBulunanSayisi, tvYenileMetin;
    private ProgressBar progressBar;
    private RecyclerView rvCihazlar;
    private Button btnYenile, btnIptal;

    private CihazAdapter adapter;
    private NetworkScanner networkScanner;
    private List<RemoteDevice> deviceList = new ArrayList<>();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private int scanSeconds = 0;
    private boolean isScanning = false;
    private boolean scanCompleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);

        initViews();
        startScan();
    }

    private void initViews() {
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
            Intent intent = new Intent(RemoteDeviceScanActivity.this, RemoteControlActivity.class);
            intent.putExtra("device_name", device.getName());
            intent.putExtra("device_ip", device.getIpAddress());
            intent.putExtra("device_port", 8891);
            startActivity(intent);
            finish();
        });
        rvCihazlar.setAdapter(adapter);

        btnYenile.setOnClickListener(v -> { if (!isScanning) startScan(); });
        btnIptal.setOnClickListener(v -> finish());
        tvYenileMetin.setOnClickListener(v -> { if (!isScanning) startScan(); });
        tvYenileMetin.setFocusable(true);
        tvYenileMetin.setClickable(true);
    }

    private void startScan() {
        deviceList.clear();
        adapter.notifyDataSetChanged();
        tvBulunanSayisi.setText("📡 Bulunan cihazlar: 0");
        tvTaramaDurum.setText("🔍 Ağ taranıyor...");
        tvSure.setText("⏱️ 0/8 sn");
        progressBar.setIndeterminate(true);
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
                    // Aynı IP'den tekrar ekleme
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
                        tvTaramaDurum.setText("🔍 Tarama devam ediyor... (" + deviceList.size() + " cihaz bulundu)");

                        // ✅ TARAMA DEVAM ETSİN - stopScanAndShowResults() ÇAĞRISINI KALDIR
                        // stopScanAndShowResults();  // BU SATIRI KALDIRIN VEYA YORUM YAPIN
                    }
                });
            }

            @Override
            public void onScanComplete(List<RemoteDevice> devices) {
                runOnUiThread(() -> {
                    scanCompleted = true;
                    isScanning = false;
                    progressBar.setIndeterminate(false);
                    progressBar.setProgress(100);

                    if (deviceList.isEmpty()) {
                        tvTaramaDurum.setText("❌ Cihaz bulunamadı");
                        tvSure.setText("⏱️ " + scanSeconds + "/8 sn (Tamamlandı)");
                    } else {
                        tvTaramaDurum.setText("✅ Tarama tamamlandı - " + deviceList.size() + " cihaz bulundu");
                        tvSure.setText("⏱️ " + scanSeconds + "/8 sn (Tamamlandı)");
                    }
                });
            }

            @Override
            public void onScanFailed(String error) {
                runOnUiThread(() -> {
                    tvTaramaDurum.setText("❌ Tarama hatası: " + error);
                    Toast.makeText(RemoteDeviceScanActivity.this, "Tarama hatası: " + error, Toast.LENGTH_SHORT).show();
                    isScanning = false;
                    scanCompleted = true;
                    progressBar.setIndeterminate(false);
                });
            }
        });
    }

    private void stopScanAndShowResults() {
        if (networkScanner != null) {
            networkScanner.stopScan();
        }
        isScanning = false;
        scanCompleted = true;
        progressBar.setIndeterminate(false);
        progressBar.setProgress(100);
        tvSure.setText("⏱️ " + scanSeconds + "/8 sn (Tamamlandı)");
        tvTaramaDurum.setText("✅ " + deviceList.size() + " cihaz bulundu");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);
        if (networkScanner != null) {
            networkScanner.stopScan();
        }
    }

    // RecyclerView Adapter
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
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvAd = itemView.findViewById(R.id.device_name);
                tvIp = itemView.findViewById(R.id.device_ip);
            }
        }
    }
}