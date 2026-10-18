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
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class FloatingService extends Service {

    private WindowManager wm;
    private TextView bubble;
    private View panel;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams panelParams;

    private int purple = Color.rgb(124, 92, 255);
    private int bg = Color.rgb(15, 17, 30);

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

    private WindowManager.LayoutParams makeParams(int width, int height) {
        return new WindowManager.LayoutParams(
                width,
                height,
                getWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
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

        bubbleParams = makeParams(60, 60);
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
                            showPanel();
                        }
                        return true;
                }
                return true;
            }
        });

        wm.addView(bubble, bubbleParams);
    }

    private void showPanel() {
        if (panel != null) {
            return;
        }

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(16, 16, 16, 12);
        box.setBackgroundColor(bg);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Ridwannzz AI");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);

        header.addView(title, new LinearLayout.LayoutParams(
                0, -2, 1
        ));

        Button close = new Button(this);
        close.setText("X");
        header.addView(close, new LinearLayout.LayoutParams(
                -2, 45
        ));

        box.addView(header);

        TextView info = new TextView(this);
        info.setText("Untuk chat lengkap, buka aplikasi Ridwannzz AI.");
        info.setTextColor(Color.LTGRAY);
        info.setTextSize(14);
        info.setPadding(0, 12, 0, 12);
        box.addView(info);

        Button openChat = new Button(this);
        openChat.setText("Buka Chat AI");
        openChat.setTextColor(Color.WHITE);
        openChat.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(purple)
        );
        box.addView(openChat);

        close.setOnClickListener(v -> hidePanel());

        openChat.setOnClickListener(v -> {
            Intent intent = new Intent(
                    FloatingService.this,
                    MainActivity.class
            );
            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
            );
            startActivity(intent);
        });

        panel = box;

        panelParams = makeParams(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                -2
        );
        panelParams.gravity = Gravity.CENTER;
        panelParams.flags =
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        try {
            wm.addView(panel, panelParams);
        } catch (Exception e) {
            panel = null;
            Toast.makeText(this, "Gagal membuka panel", Toast.LENGTH_SHORT).show();
        }
    }

    private void hidePanel() {
        if (panel != null) {
            wm.removeView(panel);
            panel = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (panel != null) {
            wm.removeView(panel);
            panel = null;
        }

        if (bubble != null) {
            wm.removeView(bubble);
            bubble = null;
        }
    }
}
