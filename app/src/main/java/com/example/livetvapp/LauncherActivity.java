package com.example.livetvapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.livetvapp.remotecontrol.DeviceDetector;
import com.example.livetvapp.remotecontrol.RemoteDeviceScanActivity;
import com.example.livetvapp.paneller.DosyaSecicipaneli;

public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (!DeviceDetector.isPhone(this)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_launcher);

        findViewById(R.id.btnKumanda).setOnClickListener(v -> {
            startActivity(new Intent(this, RemoteDeviceScanActivity.class));
        });

        findViewById(R.id.btnDosyaYoneticisi).setOnClickListener(v -> {
            startActivity(new Intent(this, DosyaSecicipaneli.class));
        });

        findViewById(R.id.btnPlayer).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
        });
    }
}