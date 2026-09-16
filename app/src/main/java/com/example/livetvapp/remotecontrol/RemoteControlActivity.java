package com.example.livetvapp.remotecontrol;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.livetvapp.R;

public class RemoteControlActivity extends AppCompatActivity {

    private RemoteControlClient client;
    private TextView statusText;
    private RemoteDevice targetDevice;

    
    private View keyboardPanel;
    private EditText etRemoteInput;
    private boolean isKeyboardMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_remote_control_land);
        } else {
            setContentView(R.layout.activity_remote_control);
        }

        String deviceName = getIntent().getStringExtra("device_name");
        String deviceIP = getIntent().getStringExtra("device_ip");
        int devicePort = getIntent().getIntExtra("device_port", 8891);

        targetDevice = new RemoteDevice(deviceName, deviceIP, devicePort);

        initViews();
        if (savedInstanceState != null) {
            isKeyboardMode = savedInstanceState.getBoolean("isKeyboardMode", false);
            if (isKeyboardMode) {
                keyboardPanel.setVisibility(View.VISIBLE);
            }
        }
        connectToDevice();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isKeyboardMode", isKeyboardMode);
    }
    private void initViews() {
        statusText = findViewById(R.id.statusText);
        statusText.setText("Bağlanıyor: " + targetDevice.getName());

        findViewById(R.id.btnDpadUp).setOnClickListener(v -> sendCommand("DPAD_UP"));
        findViewById(R.id.btnDpadDown).setOnClickListener(v -> sendCommand("DPAD_DOWN"));
        findViewById(R.id.btnDpadLeft).setOnClickListener(v -> sendCommand("DPAD_LEFT"));
        findViewById(R.id.btnDpadRight).setOnClickListener(v -> sendCommand("DPAD_RIGHT"));
        findViewById(R.id.btnDpadCenter).setOnClickListener(v -> sendCommand("DPAD_CENTER"));
        findViewById(R.id.btnVolumeUp).setOnClickListener(v -> sendCommand("VOLUME_UP"));
        findViewById(R.id.btnVolumeDown).setOnClickListener(v -> sendCommand("VOLUME_DOWN"));
        findViewById(R.id.btnMenu).setOnClickListener(v -> sendCommand("MENU"));
        findViewById(R.id.btnBack).setOnClickListener(v -> sendCommand("BACK"));
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
        findViewById(R.id.btnPlayPause).setOnClickListener(v -> sendCommand("PLAY_PAUSE"));
        findViewById(R.id.btnRewind).setOnClickListener(v -> sendCommand("REWIND"));
        findViewById(R.id.btnForward).setOnClickListener(v -> sendCommand("FORWARD"));
        findViewById(R.id.btnMute).setOnClickListener(v -> sendCommand("MUTE"));
        findViewById(R.id.btnKanallar).setOnClickListener(v -> sendCommand("KANALLAR"));
        
        findViewById(R.id.btnKeyboardMode).setOnClickListener(v -> startKeyboardMode());

        
        keyboardPanel = findViewById(R.id.keyboardPanel);
        etRemoteInput = findViewById(R.id.etRemoteInput);
        findViewById(R.id.btnCloseKeyboard).setOnClickListener(v -> stopKeyboardMode());

        
        etRemoteInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (isKeyboardMode && client != null && client.isConnected()) {
                    sendCommand("TEXT_CHANGED", s.toString());
                }
            }
        });
    }

    private void startKeyboardMode() {
        if (client == null || !client.isConnected()) {
            Toast.makeText(this, "Cihaza bağlı değil", Toast.LENGTH_SHORT).show();
            return;
        }
        isKeyboardMode = true;
        keyboardPanel.setVisibility(View.VISIBLE);
        etRemoteInput.requestFocus();

        
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etRemoteInput, InputMethodManager.SHOW_IMPLICIT);
        }

        
        sendCommand("START_KEYBOARD_MODE", "");
        statusText.setText("⌨️ Klavye modu aktif - TV'de metin kutusuna tıklayın");
    }

    private void stopKeyboardMode() {
        if (!isKeyboardMode) return;

        isKeyboardMode = false;
        keyboardPanel.setVisibility(View.GONE);
        etRemoteInput.setText("");

        
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        
        sendCommand("END_KEYBOARD_MODE", "");
        statusText.setText("Bağlandı: " + targetDevice.getName());
    }

    private void connectToDevice() {
        client = new RemoteControlClient();
        client.connect(targetDevice, new RemoteControlClient.ConnectionCallback() {
            @Override
            public void onConnected() {
                runOnUiThread(() -> {
                    statusText.setText("Bağlandı: " + targetDevice.getName());
                    findViewById(R.id.controlsContainer).setVisibility(View.VISIBLE);
                });
            }
            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    statusText.setText("Bağlantı kesildi");
                    findViewById(R.id.controlsContainer).setVisibility(View.GONE);
                    if (isKeyboardMode) stopKeyboardMode();
                });
            }
            @Override
            public void onConnectionFailed(String error) {
                runOnUiThread(() -> {
                    statusText.setText("Bağlantı başarısız: " + error);
                    Toast.makeText(RemoteControlActivity.this, "Bağlantı kurulamadı", Toast.LENGTH_LONG).show();
                    findViewById(R.id.controlsContainer).setVisibility(View.GONE);
                    if (isKeyboardMode) stopKeyboardMode();
                });
            }
        });
    }

    private void sendCommand(String command) {
        sendCommand(command, "");
    }

    private void sendCommand(String command, String data) {
        if (client != null && client.isConnected()) {
            client.sendCommand(command, data);
        } else if (!command.equals("END_KEYBOARD_MODE")) {
            Toast.makeText(this, "Bağlantı yok", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isChangingConfigurations()) {
            return;
        }
        if (client != null) client.disconnect();
    }
}