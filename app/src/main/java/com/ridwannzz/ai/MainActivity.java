package com.ridwannzz.ai;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;

public class MainActivity extends Activity {

    LinearLayout chatLayout;
    EditText input;

    int bg = Color.rgb(15, 17, 30);
    int purple = Color.rgb(124, 92, 255);

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
        root.addView(title, new LinearLayout.LayoutParams(
            -1, 60
        ));

        TextView subtitle = new TextView(this);
        subtitle.setText("Asisten AI untuk membantu belajar");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setTextSize(14);
        root.addView(subtitle);

        ScrollView scroll = new ScrollView(this);
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
        bottom.setOrientation(LinearLayout.HORIZONTAL);

        input = new EditText(this);
        input.setSingleLine(false);
        input.setHint("Tulis soal di sini...");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        input.setTextSize(15);
        input.setPadding(12, 8, 12, 8);
        input.setBackgroundColor(Color.rgb(35, 38, 58));

        bottom.addView(input, new LinearLayout.LayoutParams(
            0, 55, 1
        ));

        Button send = new Button(this);
        send.setText("Kirim");
        send.setTextColor(Color.WHITE);
        send.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(purple)
        );

        bottom.addView(send, new LinearLayout.LayoutParams(
            -2, 55
        ));

        root.addView(bottom);

        send.setOnClickListener(v -> {
            String question = input.getText().toString().trim();

            if (question.isEmpty()) {
                Toast.makeText(this, "Masukkan soal dulu!", Toast.LENGTH_SHORT).show();
                return;
            }

            addMessage("Kamu: " + question, Color.WHITE);

            addMessage(
                "Ridwannzz AI: Fitur AI sedang disiapkan. " +
                "Nanti aku akan membantu menjawab dan menjelaskan soalmu.",
                Color.rgb(190, 175, 255)
            );

            input.setText("");

            ((InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE
            )).hideSoftInputFromWindow(input.getWindowToken(), 0);
        });

        setContentView(root);
    }

    private void addMessage(String text, int color) {
        TextView message = new TextView(this);
        message.setText(text);
        message.setTextColor(color);
        message.setTextSize(15);
        message.setPadding(12, 14, 12, 14);

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(-1, -2);

        params.setMargins(0, 8, 0, 8);
        chatLayout.addView(message, params);
    }
}
