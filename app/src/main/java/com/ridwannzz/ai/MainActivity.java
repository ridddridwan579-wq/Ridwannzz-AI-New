package com.ridwannzz.ai;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.provider.Settings;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String WEB_APP_URL =
    "https://script.google.com/macros/s/AKfycbxMKhsxxI1eEUwolU27xE5PPG2PhI22U_ClJn52VE8WtArgtAlnnKEPuekssDJWJTE/exec";

    private LinearLayout chatLayout;
    private EditText input;
    private Button send;
    private ScrollView scroll;
    private final ExecutorService executor =
        Executors.newSingleThreadExecutor();

    private final int bg = Color.rgb(15, 17, 30);
    private final int purple = Color.rgb(124, 92, 255);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 20, 20, 12);
        root.setBackgroundColor(bg);

        TextView title = new TextView(this);
        title.setText("Ridwannzz AI");
        title.setTextSize(26);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(title, new LinearLayout.LayoutParams(-1, 60));

        TextView subtitle = new TextView(this);
        subtitle.setText("Asisten AI untuk membantu belajar");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setTextSize(14);
        root.addView(subtitle);

        scroll = new ScrollView(this);
        chatLayout = new LinearLayout(this);
        chatLayout.setOrientation(LinearLayout.VERTICAL);
        chatLayout.setPadding(0, 20, 0, 20);

        TextView welcome = new TextView(this);
        welcome.setText(
            "Halo! 👋\n\n" +
            "Aku Ridwannzz AI.\n" +
            "Masukkan soal yang ingin kamu pelajari.\n\n" +
            "Kita mulai belajar bareng!"
        );
        welcome.setTextColor(Color.WHITE);
        welcome.setTextSize(17);
        welcome.setPadding(16, 20, 16, 20);
        welcome.setBackgroundColor(Color.rgb(35, 38, 58));

        chatLayout.addView(welcome);
        scroll.addView(chatLayout);

        root.addView(scroll, new LinearLayout.LayoutParams(
            -1, 0, 1
        ));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER_VERTICAL);

        input = new EditText(this);
        input.setHint("Tulis soal di sini...");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        input.setTextSize(15);
        input.setPadding(12, 8, 12, 8);
        input.setBackgroundColor(Color.rgb(35, 38, 58));

        bottom.addView(input, new LinearLayout.LayoutParams(
            0, 55, 1
        ));

        send = new Button(this);
        send.setText("Kirim");
        send.setTextColor(Color.WHITE);
        send.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(purple)
        );

        bottom.addView(send, new LinearLayout.LayoutParams(-2, 55));
        root.addView(bottom);

        send.setOnClickListener(v -> sendQuestion());
        Button scanBtn = new Button(this);
scanBtn.setText("📸 Scan Layar");
root.addView(scanBtn);

scanBtn.setOnClickListener(v -> {
    MediaProjectionManager manager =
        (MediaProjectionManager) getSystemService(
            Context.MEDIA_PROJECTION_SERVICE
        );

    Intent captureIntent = manager.createScreenCaptureIntent();
    startActivityForResult(captureIntent, 1001);
});
        Button floatingBtn = new Button(this);
floatingBtn.setText("Aktifkan Floating AI");
root.addView(floatingBtn);

floatingBtn.setOnClickListener(v -> {
    if (android.provider.Settings.canDrawOverlays(this)) {
        startService(new android.content.Intent(this, FloatingService.class));
        Toast.makeText(this, "Floating AI aktif!", Toast.LENGTH_SHORT).show();
    } else {
        android.content.Intent intent = new android.content.Intent(
            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:" + getPackageName())
        );
        startActivity(intent);
    }
});

        setContentView(root);
    }

    private void sendQuestion() {
        String question = input.getText().toString().trim();

        if (question.isEmpty()) {
            Toast.makeText(this, "Masukkan soal dulu!",
                Toast.LENGTH_SHORT).show();
            return;
        }

        addMessage("Kamu: " + question, Color.WHITE);
        input.setText("");

        ((InputMethodManager) getSystemService(
            Context.INPUT_METHOD_SERVICE
        )).hideSoftInputFromWindow(input.getWindowToken(), 0);

        send.setEnabled(false);
        send.setText("...");

        TextView loading = addMessage(
            "Ridwannzz AI sedang berpikir...",
            Color.rgb(190, 175, 255)
        );

        executor.execute(() -> {
            String answer;

            try {
                answer = askGemini(question);
            } catch (Exception e) {
                answer = "Gagal menghubungi AI: " + e.getMessage();
            }

            final String result = answer;

            runOnUiThread(() -> {
                chatLayout.removeView(loading);
                addMessage("Ridwannzz AI: " + result,
                    Color.rgb(190, 175, 255));
                send.setEnabled(true);
                send.setText("Kirim");
            });
        });
    }

    private String askGemini(String question) throws Exception {
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
                "Content-Type", "application/json; charset=UTF-8"
            );

            JSONObject payload = new JSONObject();
            payload.put("question", question);

            byte[] data = payload.toString().getBytes(
                StandardCharsets.UTF_8
            );

            try (OutputStream output = connection.getOutputStream()) {
                output.write(data);
            }

            int status = connection.getResponseCode();

            InputStream stream = (status >= 200 && status < 400)
                ? connection.getInputStream()
                : connection.getErrorStream();

            if (stream == null) {
                throw new Exception("Server tidak mengirim respons.");
            }

            StringBuilder response = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            if (status < 200 || status >= 400) {
                throw new Exception("HTTP " + status);
            }

            JSONObject json = new JSONObject(response.toString());

            if (!json.optBoolean("success", false)) {
                return "Backend: " +
                    json.optString("error", "Terjadi kesalahan.");
            }

            return json.optString("answer", "Jawaban kosong.");

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private TextView addMessage(String text, int color) {
        TextView message = new TextView(this);
        message.setText(text);
        message.setTextColor(color);
        message.setTextSize(15);
        message.setPadding(12, 14, 12, 14);

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 8, 0, 8);

        chatLayout.addView(message, params);
        scroll.post(() -> scroll.fullScroll(ScrollView.FOCUS_DOWN));

        return message;
    }

    @Override
protected void onActivityResult(
        int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == 1001) {
        if (resultCode == RESULT_OK && data != null) {
            Toast.makeText(
                this,
                "Izin Scan diberikan! Langkah berikutnya: membaca layar.",
                Toast.LENGTH_LONG
            ).show();
        } else {
            Toast.makeText(
                this,
                "Izin Scan dibatalkan.",
                Toast.LENGTH_SHORT
            ).show();
        }
    }
}

@Override
protected void onDestroy() {
    executor.shutdownNow();
    super.onDestroy();
}
}
