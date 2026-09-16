package com.example.livetvapp.remotecontrol;

import android.os.AsyncTask;
import android.util.Log;
import java.io.PrintWriter;
import java.net.Socket;

public class RemoteControlClient {
    private static final String TAG = "RemoteControlClient";
    private RemoteDevice targetDevice;
    private Socket socket;
    private PrintWriter writer;
    private boolean isConnected = false;

    public interface ConnectionCallback {
        void onConnected();
        void onDisconnected();
        void onConnectionFailed(String error);
    }

    public void connect(RemoteDevice device, ConnectionCallback callback) {
        this.targetDevice = device;
        new ConnectTask(callback).execute();
    }

    public void disconnect() {
        try {
            isConnected = false;
            if (writer != null) writer.close();
            if (socket != null) socket.close();
        } catch (Exception e) {
            Log.e(TAG, "Disconnect error", e);
        }
    }

    public void sendCommand(String command, String data) {
        if (!isConnected || writer == null) {
            Log.w(TAG, "Not connected, cannot send: " + command);
            return;
        }
        new SendCommandTask().execute(command, data);
    }

    public boolean isConnected() { return isConnected; }
    public RemoteDevice getTargetDevice() { return targetDevice; }

    private class ConnectTask extends AsyncTask<Void, Void, Boolean> {
        private ConnectionCallback callback;
        private String errorMessage;
        ConnectTask(ConnectionCallback callback) { this.callback = callback; }
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                socket = new Socket(targetDevice.getIpAddress(), targetDevice.getPort());
                writer = new PrintWriter(socket.getOutputStream(), true);
                isConnected = true;
                return true;
            } catch (Exception e) {
                errorMessage = e.getMessage();
                return false;
            }
        }
        @Override
        protected void onPostExecute(Boolean success) {
            if (success) callback.onConnected();
            else callback.onConnectionFailed(errorMessage);
        }
    }

    private class SendCommandTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            try {
                if (!isConnected || writer == null) return null;
                String command = params[0];
                String data = params.length > 1 ? params[1] : "";
                String message = data.isEmpty() ? command : command + ":" + data;
                writer.println(message);
                writer.flush();
                Log.d(TAG, "Command sent: " + command);
            } catch (Exception e) {
                Log.e(TAG, "Send error", e);
                isConnected = false;
            }
            return null;
        }
    }
}