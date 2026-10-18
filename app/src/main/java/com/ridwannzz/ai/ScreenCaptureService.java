package com.ridwannzz.ai;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

public class ScreenCaptureService extends Service {

    public static final String ACTION_START =
            "com.ridwannzz.ai.START_CAPTURE";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(
                this,
                "Layanan tangkap layar disiapkan.",
                Toast.LENGTH_SHORT
        ).show();

        stopSelf();
        return START_NOT_STICKY;
    }
}
