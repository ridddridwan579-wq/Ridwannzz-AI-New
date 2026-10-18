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
import android.widget.TextView;

public class FloatingService extends Service {

    private WindowManager windowManager;
    private TextView floatingButton;
    private WindowManager.LayoutParams params;

    private float touchX;
    private float touchY;
    private int startX;
    private int startY;
    private boolean moved;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager)
                getSystemService(WINDOW_SERVICE);

        floatingButton = new TextView(this);
        floatingButton.setText("R");
        floatingButton.setTextSize(23);
        floatingButton.setTypeface(null, Typeface.BOLD);
        floatingButton.setTextColor(Color.WHITE);
        floatingButton.setGravity(Gravity.CENTER);
        floatingButton.setBackgroundColor(
                Color.rgb(124, 92, 255)
        );

        int windowType;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            windowType =
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            windowType =
                    WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                60,
                60,
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 30;
        params.y = 250;

        floatingButton.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    startX = params.x;
                    startY = params.y;
                    touchX = event.getRawX();
                    touchY = event.getRawY();
                    moved = false;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    int dx = (int) (event.getRawX() - touchX);
                    int dy = (int) (event.getRawY() - touchY);

                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        moved = true;
                    }

                    if (moved) {
                        params.x = startX + dx;
                        params.y = startY + dy;

                        windowManager.updateViewLayout(
                                floatingButton, params
                        );
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    if (!moved) {
                        Intent intent = new Intent(
                                FloatingService.this,
                                MainActivity.class
                        );

                        intent.addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        );

                        startActivity(intent);
                    }
                    return true;
            }

            return true;
        });

        windowManager.addView(floatingButton, params);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (floatingButton != null) {
            windowManager.removeView(floatingButton);
            floatingButton = null;
        }
    }
}
