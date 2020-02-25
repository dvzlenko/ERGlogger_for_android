package com.ERG.erglogger;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ErrorActivity extends AppCompatActivity {
    Boolean exit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error);
        // activity coloring
        //View relativeLayout = findViewById(R.id.relativeLayout);
        //relativeLayout.setBackgroundColor(Color.RED);
        Intent intent = getIntent();
        exit = intent.getBooleanExtra(Global.EXTRA_Flag, true);
        // Capture the layout's TextView and set the string as its text
        TextView textView = findViewById(R.id.fNameField);
        textView.setText(Global.EXTRA_Message);
        textView.setTextColor(Color.BLACK);
    }

    public void buttonClick(View view) {
        if (exit) {
            finishAffinity();
        }
        else
            finish();
    }
}