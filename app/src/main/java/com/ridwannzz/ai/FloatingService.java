package com.ridwannzz.ai;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.View;
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

public class FloatingService extends Service {

    private WindowManager wm;
    private TextView bubble;
    private View panel;
    private LinearLayout messages;
    private EditText questionInput;
    private Button sendButton;
    private ScrollView scroll;

    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams panelParams;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final String WEB_APP_URL =
            "https://script.google.com/macros/s/AKfycbxMKhsxxI1eEUwolU27xE5PPG2PhI22U_ClJn52VE8WtArgtAlnnKEPuekssDJWJTE/exec";

    private final int purple = Color.rgb(124, 92, 255);
    private final int bg = Color.rgb(15, 17, 30);

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        showBubble();
    }

    private int getWindowType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private WindowManager.LayoutParams makeParams(
            int width, int height, boolean focusable) {

        int flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        if (!focusable) {
            flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }

        return new WindowManager.LayoutParams(
                width,
                height,
                getWindowType(),
                flags,
                PixelFormat.TRANSLUCENT
        );
    }

    private void showBubble() {
        bubble = new TextView(this);
        bubble.setText("R");
        bubble.setTextSize(23);
        bubble.setTypeface(null, Typeface.BOLD);
        bubble.setTextColor(Color.WHITE);
        bubble.setGravity(Gravity.CENTER);
        bubble.setBackgroundColor(purple);

        bubbleParams = makeParams(60, 60, false);
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = 30;
        bubbleParams.y = 250;

        bubble.setOnTouchListener(new View.OnTouchListener() {
            int startX, startY;
            float downX, downY;
            boolean moved;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = bubbleParams.x;
                        startY = bubbleParams.y;
                        downX = e.getRawX();
                        downY = e.getRawY();
                        moved = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (e.getRawX() - downX);
                        int dy = (int) (e.getRawY() - downY);

                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            moved = true;
                        }

                        if (moved) {
                            bubbleParams.x = startX + dx;
                            bubbleParams.y = startY + dy;
                            wm.updateViewLayout(bubble, bubbleParams);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!moved) {
                            showMenu();
                        }
                        return true;
                }
                return true;
            }
        });

        wm.addView(bubble, bubbleParams);
    }

    // Menu utama saat tombol R ditekan
    private void showMenu() {
        if (panel != null) return;

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(24, 20, 24, 20);
        box.setBackgroundColor(bg);

        TextView title = new TextView(this);
        title.setText("Ridwannzz AI");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        box.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Pilih fitur yang ingin digunakan");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 8, 0, 20);
        box.addView(subtitle);

        Button chatButton = new Button(this);
        chatButton.setText("💬 Chat AI Biasa");
        box.addView(chatButton, new LinearLayout.LayoutParams(
                -1, 55
        ));

        Button autoButton = new Button(this);
        autoButton.setText("⚡ Jawab Otomatis");
        box.addView(autoButton, new LinearLayout.LayoutParams(
                -1, 55
        ));

        Button closeButton = new Button(this);
        closeButton.setText("Tutup");
        box.addView(closeButton, new LinearLayout.LayoutParams(
                -1, 48
        ));

        panel = box;

        panelParams = makeParams(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.85),
                -2,
                true
        );
        panelParams.gravity = Gravity.CENTER;

        try {
            wm.addView(panel, panelParams);
        } catch (Exception e) {
            panel = null;
            Toast.makeText(this, "Menu gagal dibuka",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        chatButton.setOnClickListener(v -> {
            hidePanel();
            showChatPanel();
        });

        autoButton.setOnClickListener(v -> {
            hidePanel();
            startAutoAnswer();
        });

        closeButton.setOnClickListener(v -> hidePanel());
    }

    // Chat biasa tetap memakai backend yang sudah ada
    private void showChatPanel() {
        if (panel != null) return;

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(16, 12, 16, 12);
        box.setBackgroundColor(bg);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Chat AI Biasa");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, 50, 1));

        Button close = new Button(this);
        close.setText("Tutup");
        header.addView(close);
        box.addView(header);

        scroll = new ScrollView(this);
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(4, 8, 4, 8);

        TextView welcome = new TextView(this);
        welcome.setText("Halo! Tulis soalmu di bawah.");
        welcome.setTextColor(Color.WHITE);
        welcome.setTextSize(15);
        welcome.setPadding(10, 12, 10, 12);
        messages.addView(welcome);
        scroll.addView(messages);

        box.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER_VERTICAL);

        questionInput = new EditText(this);
        questionInput.setHint("Tulis soal...");
        questionInput.setTextColor(Color.WHITE);
        questionInput.setHintTextColor(Color.LTGRAY);
        questionInput.setTextSize(14);
        questionInput.setBackgroundColor(Color.rgb(35, 38, 58));

        bottom.addView(questionInput,
                new LinearLayout.LayoutParams(0, 52, 1));

        sendButton = new Button(this);
        sendButton.setText("Kirim");
        bottom.addView(sendButton,
                new LinearLayout.LayoutParams(-2, 52));

        box.addView(bottom);
        panel = box;

        panelParams = makeParams(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.90),
                (int) (getResources().getDisplayMetrics().heightPixels * 0.65),
                true
        );
        panelParams.gravity = Gravity.CENTER;

        try {
            wm.addView(panel, panelParams);
        } catch (Exception e) {
            panel = null;
            Toast.makeText(this, "Chat gagal dibuka",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        close.setOnClickListener(v -> hidePanel());
        sendButton.setOnClickListener(v -> sendQuestion());
    }

    // Placeholder: sistem tangkap layar dan OCR perlu ditambahkan
    private void startAutoAnswer() {
        Toast.makeText(
                this,
                "Jawab Otomatis belum terhubung ke tangkap layar dan OCR.",
                Toast.LENGTH_LONG
        ).show();

        // Berikutnya: hubungkan ke MainActivity untuk meminta
        // izin tangkap layar, kemudian jalankan OCR.
    }

    private void addFloatingMessage(String text, int color) {
        if (messages == null || scroll == null) return;

        TextView message = new TextView(this);
        message.setText(text);
        message.setTextColor(color);
        message.setTextSize(14);
        message.setPadding(10, 10, 10, 10);

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 5, 0, 5);

        messages.addView(message, lp);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void sendQuestion() {
        if (questionInput == null || sendButton == null) return;

        String question = questionInput.getText().toString().trim();

        if (question.isEmpty()) {
            Toast.makeText(this, "Tulis soal dulu!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        addFloatingMessage("Kamu: " + question, Color.WHITE);
        questionInput.setText("");
        sendButton.setEnabled(false);
        sendButton.setText("...");

        executor.execute(() -> {
            String answer;

            try {
                answer = askGemini(question);
            } catch (Exception e) {
                answer = "Gagal menghubungi AI: " + e.getMessage();
            }

            final String result = answer;

            new android.os.Handler(getMainLooper()).post(() -> {
                if (panel != null && messages != null
                        && sendButton != null) {
                    addFloatingMessage(
                            "Ridwannzz AI: " + result,
                            Color.rgb(190, 175, 255)
                    );
                    sendButton.setEnabled(true);
                    sendButton.setText("Kirim");
                }
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

    private void hidePanel() {
        if (panel != null) {
            try {
                wm.removeView(panel);
            } catch (Exception ignored) {
            }

            panel = null;
            messages = null;
            questionInput = null;
            sendButton = null;
            scroll = null;
        }
    }

    @Override
    public void onDestroy() {
        hidePanel();

        if (bubble != null) {
            try {
                wm.removeView(bubble);
            } catch (Exception ignored) {
            }
            bubble = null;
        }

        executor.shutdownNow();
        super.onDestroy();
    }
}
