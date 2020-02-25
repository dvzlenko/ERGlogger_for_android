package com.ERG.erglogger;

import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.Locale;

//public class TActivity extends AppCompatActivity implements View.OnClickListener {
public class TPActivity extends AppCompatActivity {
    Integer interval = 0;
    // TODO: this is just a copy of TActivity!!! It must be updated!!!
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tp);

        // Get the Intent that started this activity and extract the string
        //Intent intent = getIntent();
        //String[] devinfo = intent.getStringArrayExtra(Global.EXTRA_DevInfo);
        // Device name
        String devname = Global.devinfo[2].replace(" ","")+"-"+Global.devinfo[3].replace(" ", "");
        TextView textView;
        textView = findViewById(R.id.DevName);
        textView.setText(devname);
        // get the schedule
        getSchedule();
        textView = findViewById(R.id.interval);
        textView.setText(String.valueOf(interval));
        //text = findViewById(R.id.test1);
        //text.setText(String.valueOf(Global.ReReadFlag));
    }

    public void setSchedule(View view) {
        // collecting new schedule
        DatePicker startDatePicker = findViewById(R.id.startDate);
        DatePicker stopDatePicker = findViewById(R.id.stopDate);
        TimePicker startTimePicker = findViewById(R.id.startTime);
        TimePicker stopTimePicker = findViewById(R.id.stopTime);
        String startDate = String.format(Locale.US, "%4d.%2d.%2d", startDatePicker.getYear(), startDatePicker.getMonth() + 1, startDatePicker.getDayOfMonth()).replace(" ", "0");
        String stopDate = String.format(Locale.US, "%4d.%2d.%2d", stopDatePicker.getYear(), stopDatePicker.getMonth() + 1, stopDatePicker.getDayOfMonth()).replace(" ", "0");
        String startTime = String.format(Locale.US, "%2d:%2d:00", startTimePicker.getCurrentHour(), startTimePicker.getCurrentMinute()).replace(" ", "0");
        String stopTime = String.format(Locale.US, "%2d:%2d:00", stopTimePicker.getCurrentHour(), stopTimePicker.getCurrentMinute()).replace(" ", "0");
        // collecting interval
        TextView textView = findViewById(R.id.interval);
        interval = Integer.parseInt(textView.getText().toString());
        // set globals
        Global.startTime.set(startDatePicker.getYear(), startDatePicker.getMonth(), startDatePicker.getDayOfMonth(), startTimePicker.getCurrentHour(), startTimePicker.getCurrentMinute(), 0);
        Global.stopTime.set(stopDatePicker.getYear(), stopDatePicker.getMonth(), stopDatePicker.getDayOfMonth(), stopTimePicker.getCurrentHour(), stopTimePicker.getCurrentMinute(), 0);
    }

    public void getSchedule() {
        //if (Global.ReReadScheduleFlag.get()) {
        //    //Global.CDC_Send("GetProgramm\r\n", this);
        //    //String[] schedule = Global.CDC_Get_String(this).split("\r\n");
        //    Global.schedule = "GetProgramm\r\nad23f\r\n1480752135\r\n1530752149\r\n60\r\nOK".split("\r\n");
        //    Global.ReReadScheduleFlag.setFalse();
        //}
        // collection interval
        //interval = Integer.parseInt(Global.schedule[4]);
        // start time
        //Global.startTime.setTimeInMillis(Long.parseLong(Global.schedule[2])*1000);
        DatePicker startDatePicker = findViewById(R.id.startDate);
        startDatePicker.updateDate(Global.startTime.get(Calendar.YEAR), Global.startTime.get(Calendar.MONTH), Global.startTime.get(Calendar.DAY_OF_MONTH));
        TimePicker startTimePicker = findViewById(R.id.startTime);
        startTimePicker.setIs24HourView(Boolean.TRUE);
        startTimePicker.setCurrentHour(Global.startTime.get(Calendar.HOUR_OF_DAY));
        startTimePicker.setCurrentMinute(Global.startTime.get(Calendar.MINUTE));
        // stop time
        //Global.stopTime.setTimeInMillis(Long.parseLong(Global.schedule[3])*1000);
        DatePicker stopDatePicker = findViewById(R.id.stopDate);
        stopDatePicker.updateDate(Global.stopTime.get(Calendar.YEAR), Global.stopTime.get(Calendar.MONTH), Global.stopTime.get(Calendar.DAY_OF_MONTH));
        TimePicker stopTimePicker = findViewById(R.id.stopTime);
        stopTimePicker.setIs24HourView(Boolean.TRUE);
        stopTimePicker.setCurrentHour(Global.stopTime.get(Calendar.HOUR_OF_DAY));
        stopTimePicker.setCurrentMinute(Global.stopTime.get(Calendar.MINUTE));
    }
}
