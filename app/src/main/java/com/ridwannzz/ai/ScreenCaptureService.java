package com.ridwannzz.ai;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScreenCaptureService extends Service {

private static final String CHANNEL_ID = "ridwannzz_capture";
private static final int NOTIFICATION_ID = 301;

private static final String WEB_APP_URL =
        "https://script.google.com/macros/s/AKfycbxMKhsxxI1eEUwolU27xE5PPG2PhI22U_ClJn52VE8WtArgtAlnnKEPuekssDJWJTE/exec";

private MediaProjection projection;
private VirtualDisplay virtualDisplay;
private ImageReader imageReader;
private HandlerThread captureThread;
private Handler captureHandler;
private final ExecutorService networkExecutor =
        Executors.newSingleThreadExecutor();

private boolean captured = false;

public static final String ACTION_START =
        "com.ridwannzz.ai.START_CAPTURE";

public static final String ACTION_RESULT =
        "com.ridwannzz.ai.AUTO_RESULT";

@Override
public IBinder onBind(Intent intent) {
    return null;
}

@Override
public int onStartCommand(Intent intent, int flags, int startId) {
    createNotificationChannel();

    Notification notification =
            new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Ridwannzz AI")
                    .setContentText("Sedang membaca layar...")
                    .setSmallIcon(android.R.drawable.ic_menu_view)
                    .setOngoing(true)
                    .build();

    if (Build.VERSION.SDK_INT >= 29) {
        startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        );
    } else {
        startForeground(NOTIFICATION_ID, notification);
    }

    if (intent == null) {
        sendResult("", "Data tangkapan layar tidak tersedia.");
        stopCaptureService();
        return START_NOT_STICKY;
    }

    int resultCode = intent.getIntExtra("RESULT_CODE", 0);

    Intent resultData;
    if (Build.VERSION.SDK_INT >= 33) {
        resultData = intent.getParcelableExtra(
                "RESULT_DATA", Intent.class);
    } else {
        resultData = intent.getParcelableExtra("RESULT_DATA");
    }

    if (resultData == null) {
        sendResult("", "Izin tangkapan layar tidak ditemukan.");
        stopCaptureService();
        return START_NOT_STICKY;
    }

    try {
        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(
                        Context.MEDIA_PROJECTION_SERVICE);

        projection = manager.getMediaProjection(
                resultCode, resultData);

        if (projection == null) {
            sendResult("", "Gagal memulai tangkapan layar.");
            stopCaptureService();
            return START_NOT_STICKY;
        }

        startCapture();

    } catch (Exception e) {
        sendResult("", "Gagal menangkap layar: " + e.getMessage());
        stopCaptureService();
    }

    return START_NOT_STICKY;
}

private void startCapture() {
    DisplayMetrics metrics = getResources().getDisplayMetrics();

    int width = metrics.widthPixels;
    int height = metrics.heightPixels;
    int density = metrics.densityDpi;

    captureThread = new HandlerThread("RidwannzzScreenCapture");
    captureThread.start();
    captureHandler = new Handler(captureThread.getLooper());

    imageReader = ImageReader.newInstance(
            width, height, PixelFormat.RGBA_8888, 2);

    imageReader.setOnImageAvailableListener(reader -> {
        if (captured) {
            Image extra = reader.acquireLatestImage();
            if (extra != null) extra.close();
            return;
        }

        Image image = reader.acquireLatestImage();
        if (image == null) return;

        captured = true;

        Bitmap bitmap = null;

        try {
            Image.Plane plane = image.getPlanes()[0];
            ByteBuffer buffer = plane.getBuffer();

            int pixelStride = plane.getPixelStride();
            int rowStride = plane.getRowStride();
            int paddedWidth = rowStride / pixelStride;

            Bitmap paddedBitmap = Bitmap.createBitmap(
                    paddedWidth,
                    height,
                    Bitmap.Config.ARGB_8888);

            paddedBitmap.copyPixelsFromBuffer(buffer);

            bitmap = Bitmap.createBitmap(
                    paddedBitmap, 0, 0, width, height);

            paddedBitmap.recycle();

        } catch (Exception e) {
            sendResult("", "Gagal memproses gambar layar.");
        } finally {
            image.close();
            releaseProjection();
        }

        if (bitmap != null) {
            readText(bitmap);
        } else {
            stopCaptureService();
        }

    }, captureHandler);

    virtualDisplay = projection.createVirtualDisplay(
            "RidwannzzCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.getSurface(),
            null,
            captureHandler);

    captureHandler.postDelayed(() -> {
        if (!captured) {
            sendResult("", "Layar tidak berhasil ditangkap. Coba lagi.");
            releaseProjection();
            stopCaptureService();
        }
    }, 5000);
}

private void readText(Bitmap bitmap) {
    TextRecognizer recognizer = TextRecognition.getClient(
            TextRecognizerOptions.DEFAULT_OPTIONS);

    InputImage image = InputImage.fromBitmap(bitmap, 0);

    recognizer.process(image)
            .addOnSuccessListener(result -> {
                String question = result.getText().trim();

                if (question.isEmpty()) {
                    sendResult("", "Tidak ada teks soal yang terbaca. Coba tampilkan soal dengan jelas.");
                    stopCaptureService();
                    return;
                }

                askBackend(question);
            })
            .addOnFailureListener(e -> {
                sendResult("", "Gagal membaca teks: " + e.getMessage());
                stopCaptureService();
            })
            .addOnCompleteListener(task -> {
                recognizer.close();
                bitmap.recycle();
            });
}

private void askBackend(String question) {
    networkExecutor.execute(() -> {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(WEB_APP_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(60000);
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8");

            JSONObject payload = new JSONObject();
            payload.put("question", question);

            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload.toString().getBytes(
                        StandardCharsets.UTF_8));
            }

            int status = connection.getResponseCode();

            InputStream stream = status >= 200 && status < 400
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            if (stream == null) {
                throw new Exception("Server tidak mengirim jawaban.");
            }

            StringBuilder response = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            if (status < 200 || status >= 400) {
                throw new Exception("HTTP " + status);
            }

            JSONObject json = new JSONObject(response.toString());

            if (json.optBoolean("success", false)) {
                sendResult(
                        question,
                        json.optString("answer", "Jawaban kosong."));
            } else {
                sendResult(
                        question,
                        json.optString("error", "AI gagal menjawab."));
            }

        } catch (Exception e) {
            sendResult(
                    question,
                    "Gagal menghubungi AI: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            new Handler(Looper.getMainLooper()).post(
                    this::stopCaptureService);
        }
    });
}

private void sendResult(String question, String answer) {
    Intent resultIntent = new Intent(ACTION_RESULT);
    resultIntent.setPackage(getPackageName());
    resultIntent.putExtra("QUESTION", question);
    resultIntent.putExtra("ANSWER", answer);
    sendBroadcast(resultIntent);
}

private void releaseProjection() {
    if (virtualDisplay != null) {
        virtualDisplay.release();
        virtualDisplay = null;
    }

    if (imageReader != null) {
        imageReader.close();
        imageReader = null;
    }

    if (projection != null) {
        projection.stop();
        projection = null;
    }

    if (captureThread != null) {
        captureThread.quitSafely();
        captureThread = null;
    }
}

private void stopCaptureService() {
    releaseProjection();

    if (Build.VERSION.SDK_INT >= 24) {
        stopForeground(STOP_FOREGROUND_REMOVE);
    } else {
        stopForeground(true);
    }

    stopSelf();
}

private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Ridwannzz AI Screen Capture",
                NotificationManager.IMPORTANCE_LOW);

        NotificationManager manager =
                getSystemService(NotificationManager.class);

        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}

@Override
public void onDestroy() {
    releaseProjection();
    networkExecutor.shutdownNow();
    super.onDestroy();
}

}
