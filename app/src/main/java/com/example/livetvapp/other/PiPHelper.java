package com.example.livetvapp.other;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Log;
import android.util.Rational;

public class PiPHelper {
    private Activity activity;
    private boolean isInPiPMode = false;

    public PiPHelper(Activity activity) {
        this.activity = activity;
    }

    public boolean enterPictureInPictureMode() {
        if (activity == null) return false;

        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                
                Rational aspectRatio = new Rational(16, 9);

                PictureInPictureParams.Builder paramsBuilder =
                        new PictureInPictureParams.Builder()
                                .setAspectRatio(aspectRatio);

                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    paramsBuilder.setAutoEnterEnabled(true);
                }

                PictureInPictureParams params = paramsBuilder.build();
                boolean success = activity.enterPictureInPictureMode(params);

                if (success) {
                    isInPiPMode = true;
                    Log.d("PiPHelper", "PiP modu başarıyla etkinleştirildi");
                }

                return success;

            } catch (Exception e) {
                Log.e("PiPHelper", "PiP modu etkinleştirme hatası: " + e.getMessage(), e);
                return false;
            }
        } else {
            Log.w("PiPHelper", "PiP modu bu Android sürümünde desteklenmiyor (API < 26)");
            return false;
        }
    }

    public boolean isPiPSupported() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return activity.getPackageManager().hasSystemFeature(
                    android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE);
        }
        return false;
    }


    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        isInPiPMode = isInPictureInPictureMode;

        if (isInPictureInPictureMode) {
            
            Log.d("PiPHelper", "PiP moduna girildi");
        } else {
            
            Log.d("PiPHelper", "PiP modundan çıkıldı");
        }
    }

}