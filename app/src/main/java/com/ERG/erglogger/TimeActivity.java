package com.ERG.erglogger;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.util.Calendar;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class TimeActivity extends AppCompatActivity {
    TextView hostTimeField;
    TextView ergUField;
    TextView ergCField;
    TextView ergSField;
    TextView prsclCField;
    TextView prsclRField;

    String[] ergtime;
    String timestring;
    Calendar calendar;

    long hTime;
    long sTime;
    float prescaler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time);

        String string;
        TextView textView;

        // device
        textView = findViewById(R.id.Time_DevNameW);
        string = Global.devinfo[2].replace(" ","")+"-"+Global.devinfo[3].replace(" ", "");
        textView.setText(string);

        // fields for live thread
        hostTimeField = findViewById(R.id.Time_HostTimeV);
        ergUField = findViewById(R.id.Time_UncorTimeV);
        ergCField = findViewById(R.id.Time_CorrTimeV);
        ergSField = findViewById(R.id.Time_TimeSetV);
        prsclCField = findViewById(R.id.Time_PrsclCurrentV);
        prsclRField = findViewById(R.id.Time_PrsclRecommendedV);

        // run the thread that must be done only once at activity creation
        Global.KillWatchThreadFlag = false;
        liveClockThread.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // SetTime and current Presacaler
        ergtime = getTime();
        sTime = Long.parseLong(ergtime[1]) * 1000;
        calendar = Calendar.getInstance();
        calendar.setTimeInMillis(sTime);
        timestring = getDateString(calendar);
        ergSField.setText(timestring);
        timestring = String.format(Locale.getDefault(), "%9.7f", Float.parseFloat(ergtime[4]) / 10000000);
        prsclCField.setText(timestring);
        // check if some buttons were pressed
        if (Global.SetTimeFlag) {
            onSetTimeClick(findViewById(R.id.Time_SetTimeB));
        }
        if (Global.SetPrsclFlag) {
            onSetPrsclClick(findViewById(R.id.Time_SetPrsclB));
        }
        Global.AllowLiveClockFlag = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Global.AllowLiveClockFlag = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Global.KillWatchThreadFlag = true;
    }

    public Thread liveClockThread = new Thread(new Runnable() {
        public void run() {
            while (!Global.KillWatchThreadFlag) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Global.port != null) {
                            if (Global.AllowLiveClockFlag) {
                                if (ergtime.length == 6) {
                                    ergtime = getTime();
                                    //
                                    calendar = Calendar.getInstance();
                                    hTime = calendar.getTimeInMillis();
                                    timestring = getDateString(calendar);
                                    hostTimeField.setText(timestring);
                                    //
                                    long uTime = Long.parseLong(ergtime[2]) * 1000;
                                    calendar.setTimeInMillis(uTime);
                                    timestring = getDateString(calendar);
                                    ergUField.setText(timestring);
                                    //
                                    long cTime = Long.parseLong(ergtime[3]) * 1000;
                                    calendar.setTimeInMillis(cTime);
                                    timestring = getDateString(calendar);
                                    ergCField.setText(timestring);
                                    //
                                    prescaler = 1 + (float) (hTime - uTime) / (hTime - sTime);
                                    timestring = String.format(Locale.getDefault(), "%9.7f", prescaler);
                                    prsclRField.setText(timestring);
                                    //Log.i("live",  "liveClockThread.isAlive = "+liveClockThread.isAlive());
                                }
                            }
                        }
                        else {
                            Global.AllowLiveClockFlag = false;
                        }
                    }
                });
                SystemClock.sleep(200);
            }
        }
    });

    public String getDateString(Calendar date) {
        String timeString = String.format(Locale.getDefault(),
                "%2d:%2d:%2d",
                date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE), date.get(Calendar.SECOND)).replace(" ", "0");
        String dateString = String.format(Locale.getDefault(),
                "%2d.%2d.%4d",
                date.get(Calendar.DAY_OF_MONTH), date.get(Calendar.MONTH)+1, date.get(Calendar.YEAR)).replace(" ", "0");
        return(dateString+" "+timeString);
    }

    public String[] getTime() {
        String reply = "";
        try {
            Global.CDC_Send("GetTime\r\n", this);
            reply = Global.CDC_Get_String(this);
        }
        catch (Exception exp) {
            Global.EXTRA_Message = "Error in time reading!!!\n\nExiting :(";
            Global.RiseError(this,true);
        }
        return(reply.split("\r\n"));
    }

    public void onSetTimeClick(View view) {
        if (!Global.SetTimeFlag) {
            Global.EXTRA_Message = "You are going to adjust the internal watch of the Device.\n" +
                    "You should remember that the Time Prescaler value can be adjusted only " +
                    "TWO WEEKS after the Date & Time were set!";
            Global.RiseWarning(this, "SetTime");
            return;
        }
        // stop the live monitor and sleep a bit to avoid multiple access on device
        Global.AllowLiveClockFlag = false;
        SystemClock.sleep(250);
        Global.CDC_Send("SetTime\r", this);
        Global.CDC_Get_String(this);
        Double cTime;
        Double dTime;
        cTime = (double) Calendar.getInstance().getTimeInMillis()/1000;
        dTime = cTime - cTime.intValue();
        while (dTime > 0.001) {
            cTime = (double) Calendar.getInstance().getTimeInMillis()/1000;
            dTime = cTime - cTime.intValue();
        }
        // TODO: correct the logger internal software!!!!
        Long cTimeInt = (long) cTime.intValue();
        byte[] command = {
                (byte)((cTimeInt >> 24) & 0xFF),
                (byte)((cTimeInt >> 16) & 0xFF),
                (byte)((cTimeInt >>  8) & 0xFF),
                (byte)((cTimeInt >>  0) & 0xFF)
        };
        Log.i("time", cTimeInt.toString());
        Log.i("time", String.format("%02x%02x%02x%02x", command[0], command[1], command[2], command[3]));
        // send it!!!
        try {
            Global.port.write(command, 50);
        }
        catch (IOException exp) {
            Global.EXTRA_Message = "Port write failed.\n\nException message is:\n\n" + exp.getMessage();
            Global.RiseError(this, true);
            return;
        }
        String reply = Global.CDC_Get_String(this);
        // flags!!!
        Global.SetTimeFlag = false;
        Global.AllowLiveClockFlag = true;
        // messages
        if (reply.contains("OK")) {
            Global.EXTRA_Message = "Date & Time were set succesfully";
            Global.RiseMessage(this, "Empty");
        }
        else {
            Global.EXTRA_Message = "Error setting time. Please, try again!";
            Global.RiseError(this, false);
            return;
        }
        // turn live monitor ON
        Global.AllowLiveClockFlag = true;
    }

    public void onSetPrsclClick(View view) {
        if ((hTime-sTime) < (long)(14*24*3600*1000)) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(sTime);
            Global.EXTRA_Message = "The Time Prescaler Value can be set " +
                    "only TWO WEEKS after the Date & Time were set last time\n\n" +
                    "You have set Date & Time \n"+getDateString(calendar);
            Global.RiseError(this, false);
            return;
        }
        if (!Global.SetPrsclFlag) {
            Global.EXTRA_Message = "You are going to adjust the Time Prescaler value, " +
                    "which should be done if only since the last Date & Time " +
                    "adjustment, the Device was incubated at the temperature close " +
                    "to the temperatures that should be measured.\n\n" +
                    "Meeting this condition would significantly improve " +
                    "the accuracy of the internal clock.";
            Global.RiseWarning(this, "SetPrscl");
            return;
        }
        // stop the live monitor and sleep a bit to avoid multiple access on device
        Global.AllowLiveClockFlag = false;
        SystemClock.sleep(250);
        String command = String.format(Locale.getDefault(), "SetTimePrescaler %10.8f\r\n", prescaler);
        Global.CDC_Send(command, this);
        String reply = Global.CDC_Get_String(this);
        Global.SetPrsclFlag = false;
        if (reply.contains("OK")) {
            Global.EXTRA_Message = "Time prescaler was set succesfully";
            Global.RiseMessage(this, "Empty");
        }
        else {
            Global.EXTRA_Message = "Error setting Time Prescaler. Please, try again!";
            Global.RiseError(this, false);
            return;
        }
    }

    public void onCancelClick(View view) {
        finish();
    }
}
