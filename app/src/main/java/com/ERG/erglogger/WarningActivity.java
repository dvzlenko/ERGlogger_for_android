package com.ERG.erglogger;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class WarningActivity extends AppCompatActivity {
    String flag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warning);
        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        flag = intent.getStringExtra(Global.EXTRA_Flag);
        // put the message
        TextView textView = findViewById(R.id.fNameField);
        textView.setText(Global.EXTRA_Message);
        textView.setTextColor(Color.BLACK);
    }

    public void buttonClick(View button) {
        if (button.getId() == R.id.warning_OK_B) {
            if (flag.equals("SetStopInPast")) {
                Global.SetStartInPastFlag = true;
                Global.SetStopInPastFlag = true;
            }
            if (flag.equals("SetStartInPast"))
                Global.SetStartInPastFlag = true;
            if (flag.equals("SetFrequent"))
                Global.SetFrequentFlag = true;
            if (flag.equals("SetLongSchedule"))
                Global.SetLongScheduleFlag = true;
            if (flag.equals("SetTime"))
                Global.SetTimeFlag = true;
            if (flag.equals("SetPrscl"))
                Global.SetPrsclFlag = true;
        }
        else {
            // schedule
            Global.SetStopInPastFlag = false;
            Global.SetStartInPastFlag = false;
            Global.SetFrequentFlag = false;
            Global.SetLongScheduleFlag = false;
            // time?
        }
        finish();
    }
}