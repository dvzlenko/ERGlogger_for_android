package com.ERG.erglogger;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MessageActivity extends AppCompatActivity {
    String flag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        // activity coloring
        //View relativeLayout = findViewById(R.id.relativeLayout);
        //relativeLayout.setBackgroundColor(Color.RED);
        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        flag = intent.getStringExtra(Global.EXTRA_Flag);
        // Capture the layout's TextView and set the string as its text
        TextView textView = findViewById(R.id.fNameField);
        textView.setText(Global.EXTRA_Message);
        textView.setTextColor(Color.BLACK);
    }

    public void buttonClick(View button) {
        if (button.getId() == R.id.name_OK_B) {
            if (flag.equals("SetSchedule")) {
                Global.SetScheduleFlag = true;
            }
        }
        else {
            Global.SetStopInPastFlag = false;
            Global.SetStartInPastFlag = false;
            Global.SetFrequentFlag = false;
            Global.SetLongScheduleFlag = false;
        }
        finish();
    }
}