package com.example.livetvapp;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Build;
import android.util.Log;

import java.security.MessageDigest;

public class AzshApplication extends Application {

    // Gerçek imzamızın SHA-256 fingerprint'i (küçük harf, iki nokta üst üste olmadan).
    private static final String BEKLENEN_IMZA_SHA256 =
            "a9cdc6823f9f5149f493ccee5ed1c8390cd11170f705bd6d7c553f9f462b6c4b";

    @Override
    public void onCreate() {
        super.onCreate();
        if (!imzaDogruMu()) {
            Log.e("AzshApplication", "APK imzasi beklenenle eslesmiyor. Uygulama kapatiliyor.");
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    private boolean imzaDogruMu() {
        try {
            PackageManager pm = getPackageManager();
            String paketAdi = getPackageName();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageInfo pInfo = pm.getPackageInfo(paketAdi, PackageManager.GET_SIGNING_CERTIFICATES);
                SigningInfo signingInfo = pInfo.signingInfo;
                Signature[] signatures = signingInfo.hasMultipleSigners()
                        ? signingInfo.getApkContentsSigners()
                        : signingInfo.getSigningCertificateHistory();
                for (Signature sig : signatures) {
                    if (BEKLENEN_IMZA_SHA256.equalsIgnoreCase(sha256Hex(sig.toByteArray()))) {
                        return true;
                    }
                }
            } else {
                @SuppressWarnings("deprecation")
                PackageInfo pInfo = pm.getPackageInfo(paketAdi, PackageManager.GET_SIGNATURES);
                @SuppressWarnings("deprecation")
                Signature[] signatures = pInfo.signatures;
                for (Signature sig : signatures) {
                    if (BEKLENEN_IMZA_SHA256.equalsIgnoreCase(sha256Hex(sig.toByteArray()))) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            Log.e("AzshApplication", "Imza kontrolu sirasinda hata: " + e.getMessage());
            return false;
        }
    }

    private String sha256Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}