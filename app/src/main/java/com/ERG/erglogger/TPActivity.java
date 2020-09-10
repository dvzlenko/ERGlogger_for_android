package com.ERG.erglogger;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Interpolator;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.math.MathUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TPActivity extends AppCompatActivity {
    private static final int CREATE_SCHEDULE_FILE_CODE = 12;
    private static final int OPEN_SCHEDULE_FILE_CODE = 21;
    // some global text-view
    TextView textView;
    // chek-boxes
    CheckBox dwnldT;
    CheckBox dwnldP;
    // schedule pickers
    DatePicker startDatePicker;
    DatePicker stopDatePicker;
    TimePicker startTimePicker;
    TimePicker stopTimePicker;
    // device detach monitor
    BroadcastReceiver detachReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tp);
        // detach intent
        final Activity obj = this;
        detachReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                    Global.EXTRA_Message = "ERG logger was detached!\nSorry, the application must be closed!!!";
                    Global.RiseError(obj, true);
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(detachReceiver, filter);
        // Device name
        String devinfo = Global.devinfo[2].replace(" ","")+"-"+Global.devinfo[3].replace(" ", "");
        textView = findViewById(R.id.TP_DevNameW);
        textView.setText(devinfo);
        // date-time pickers settings
        startDatePicker = findViewById(R.id.TP_StartDatePicker);
        stopDatePicker = findViewById(R.id.TP_StopDatePicker);
        startTimePicker = findViewById(R.id.TP_StartTimePicker);
        startTimePicker.setIs24HourView(Boolean.TRUE);
        stopTimePicker = findViewById(R.id.TP_StopTimePicker);
        stopTimePicker.setIs24HourView(Boolean.TRUE);
        // check boxes
        dwnldT = findViewById(R.id.TP_TcheckBx);
        dwnldP = findViewById(R.id.TP_PcheckBx);
        // disable progress bars
        View text= findViewById(R.id.TP_DownloadingW);
        text.setVisibility(View.INVISIBLE);
        text.bringToFront();
        ProgressBar cBar = findViewById(R.id.TP_CircularBar);
        cBar.setVisibility(View.INVISIBLE);
        cBar.getIndeterminateDrawable().setColorFilter(0xFF0099FF, android.graphics.PorterDuff.Mode.MULTIPLY);
        ProgressBar lBar = findViewById(R.id.TP_LinearBar);
        lBar.setVisibility(View.INVISIBLE);
        lBar.getProgressDrawable().setColorFilter(Color.parseColor("#0099FF"), android.graphics.PorterDuff.Mode.SRC_IN);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // schedule basic part
        getSchedule();
        refreshSchedule();
        // data part
        String message = String.format(Locale.getDefault(),"Internal memory contains:\n%7d T-samples and \n%7d P-samples", Global.dataCollectedT / 16, Global.dataCollectedP / 8);
        textView = findViewById(R.id.TP_DataCollectedW);
        textView.setText(message);
        // run setSchedule if necessary
        if (Global.SetLongScheduleFlag |
                Global.SetScheduleFlag |
                Global.SetStartInPastFlag |
                Global.SetStopInPastFlag |
                Global.SetFrequentFlag) {
            setSchedule(findViewById(R.id.TP_SetScheduleB));
        }
        //
        if (Global.SaveTFileFlag | Global.SavePFileFlag) {
            onDownloadClick(findViewById(R.id.TP_DataBDownlodB));
        }
        if (Global.AdvancedDownloadFlag) {
            onDownloadClick(findViewById(R.id.TP_DataADownlodB));
        }
    }

    @Override
    protected void onPause () {
        super.onPause();
        grabSchedule();
    }

    @Override
    protected void onDestroy () {
        super.onDestroy();
        Global.ReReadScheduleFlag = true;
        unregisterReceiver(detachReceiver);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == CREATE_SCHEDULE_FILE_CODE && resultCode == Activity.RESULT_OK) {
            // get the schedule
            Log.i("schedule", Global.startTime.getTimeInMillis()+" "+Global.stopTime.getTimeInMillis()+" "+Global.intervalT+" "+Global.intervalP);
            String schedule = String.format("%d\n%d\n%d\n%d\n", Global.startTime.getTimeInMillis(), Global.stopTime.getTimeInMillis(), Global.intervalT, Global.intervalP);
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                try {
                    // file descriptor and stream definition
                    ParcelFileDescriptor pfd = this.getContentResolver().openFileDescriptor(uri, "w");
                    FileOutputStream scheduleOutputStream = null;
                    if (pfd != null) {
                        scheduleOutputStream = new FileOutputStream(pfd.getFileDescriptor());
                        scheduleOutputStream.write((schedule).getBytes());
                        // Let the document provider know you're done by closing the stream.
                        scheduleOutputStream.close();
                        pfd.close();
                    }
                    else {
                        Global.EXTRA_Message = "Failed to open file descriptor!";
                        Global.RiseError(this, false);
                    }
                } catch (IOException exp) {
                    exp.printStackTrace();
                    Global.EXTRA_Message = "Error in saving schedule file!\n\n" +
                            "Exception message:\n\n" + exp.toString();
                    Global.RiseError(this, false);
                }
            }
        }
        else if (requestCode == OPEN_SCHEDULE_FILE_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                try {
                    // file descriptor and stream definition
                    ParcelFileDescriptor pfd = this.getContentResolver().openFileDescriptor(uri, "r");
                    FileInputStream scheduleInputStream = null;
                    if (pfd != null) {
                        scheduleInputStream = new FileInputStream(pfd.getFileDescriptor());
                        StringBuilder reply = new StringBuilder();
                        int num = scheduleInputStream.read();
                        while (num != -1) {
                            reply.append((char) num);
                            num = scheduleInputStream.read();
                        }
                        Log.i("schedule", reply.toString());
                        String[] schedule = reply.toString().split("\n");
                        if (schedule.length == 4) {
                            Global.startTime.setTimeInMillis(Long.parseLong(schedule[0]));
                            Global.stopTime.setTimeInMillis(Long.parseLong(schedule[1]));
                            Global.intervalT = Integer.parseInt(schedule[2]);
                            Global.intervalP = Integer.parseInt(schedule[3]);
                            refreshSchedule();
                        }
                        else {
                            Global.EXTRA_Message = "Error reading ERG-schedule file!!!!";
                            Global.RiseError(this, false);
                        }
                        scheduleInputStream.close();
                        pfd.close();
                    }
                    else {
                        Global.EXTRA_Message = "Failed to open file descriptor!";
                        Global.RiseError(this, false);
                    }
                } catch (IOException exp) {
                    exp.printStackTrace();
                    Global.EXTRA_Message = "Error in saving schedule file!\n\n" +
                            "Exception message:\n\n" + exp.toString();
                    Global.RiseError(this, false);
                }
            }
        }
    }

    public void setSchedule(View view) {
        grabSchedule();

        String startDate = String.format(Locale.US, "%2d.%2d.%4d",startDatePicker.getDayOfMonth(), startDatePicker.getMonth()+1,startDatePicker.getYear()).replace(" ","0");
        String stopDate = String.format(Locale.US,"%2d.%2d.%4d",stopDatePicker.getDayOfMonth(), stopDatePicker.getMonth()+1,stopDatePicker.getYear()).replace(" ","0");
        String startTime = String.format(Locale.US,"%2d:%2d:00",startTimePicker.getCurrentHour(), startTimePicker.getCurrentMinute()).replace(" ","0");
        String stopTime = String.format(Locale.US,"%2d:%2d:00",stopTimePicker.getCurrentHour(), stopTimePicker.getCurrentMinute()).replace(" ","0");

        // check if the data collection interval is shorter than 3 sec. The device could be unstable due to the discrete time counter in MCU
        if (Global.intervalT < 3 | Global.intervalP < 3) {
            Global.EXTRA_Message = String.format(Locale.getDefault(),
                    "The data collection interval MUST be at least 3 second, " +
                            "while you have requested %d and %d seconds\n\n" +
                            "So short interval may force the device to brick out :(\n\n" +
                            "Please, increase the interval!", Global.intervalT, Global.intervalP);
            Global.RiseError(this, false);
            return;
        }

        long maxScheduleLength = 2*365*24*3600;
        long scheduleLength = Math.round((Global.stopTime.getTimeInMillis() - Global.startTime.getTimeInMillis()) / 1000);
        long scheduleTVolume = Math.round((Global.stopTime.getTimeInMillis() - Global.startTime.getTimeInMillis())/(1000*Global.intervalT));
        long schedulePVolume = Math.round((Global.stopTime.getTimeInMillis() - Global.startTime.getTimeInMillis())/(1000*Global.intervalP));
        long flashSize = Global.getFlashSize(this);
        Log.i("schedule", "schedule length = " + String.valueOf(scheduleLength));
        Log.i("schedule", "schedule volume = " + String.valueOf(scheduleTVolume));
        Log.i("schedule", "schedule volume = " + String.valueOf(schedulePVolume));

        // check if the number of data points to collect is positive!
        if (scheduleTVolume < 0 | schedulePVolume < 0) {
            Global.EXTRA_Message = String.format(Locale.getDefault(),
                    "You have requested too sparce schedule, so no data will be collected!!!\n\n" +
                            "Please, decrease the Data Collecting Intervals");
            Global.RiseError(this, false);
            return;
        }

        // check the amount of data points requested. Max available provided by getFlashSize()
        if (flashSize < (16*scheduleTVolume + 8*schedulePVolume)) {
            Global.EXTRA_Message = String.format(Locale.getDefault(),
                    "You have requested\n%d T-samples and %d P-samples,\n" +
                            "while the device's internal memory capacity is only\n" +
                            "%d T-samples or %d P-samples\n\nThe schedule cannot be set!!!",
                    scheduleTVolume, schedulePVolume, flashSize / 16, flashSize / 8);
            Global.RiseError(this, false);
            return;
        }

        // check if the start precedings the finish :)
        if (Global.stopTime.getTimeInMillis() <= Global.startTime.getTimeInMillis()) {
            Global.EXTRA_Message =
                    "You are tying to create a schedule that starts at:\n" +
                            startDate + " " + startTime + "\n" +
                            "that is later than it finishes:\n" +
                            stopDate + " " + stopTime + "\n\n" +
                            "Such a schedule is impossible!!!";
            Global.RiseError(this, false);
            return;
        }

        // chek if the schedule finish is in the past
        if (Calendar.getInstance().getTimeInMillis() > Global.stopTime.getTimeInMillis() & !Global.SetStopInPastFlag) {
            Global.EXTRA_Message =
                    "You are going to set the schedule " +
                            "that lay completely in the past!\n\n" +
                            "Start Date & Time is\n"
                            + startDate +" "+startTime+"\n\n" +
                            "Stop Date & Time is\n"
                            + stopDate +" "+ stopTime+"\n\n" +
                            "No data will be collected!";
            Global.RiseWarning(this, "SetStopInPast");
            return;
        }

        // chek if the schedule start is in the past
        if (Calendar.getInstance().getTimeInMillis() > Global.startTime.getTimeInMillis() & !Global.SetStartInPastFlag) {
            Global.EXTRA_Message =
                    "You are trying to set the schedule that has already started!\n\n" +
                            "Start Date & Time is\n" + startDate +" "+ startTime + "\n\n" +
                            "Are you sure?";
            Global.RiseWarning(this, "SetStartInPast");
            return;
        }

        // check the data collection interval again, warning is necessary if it is shorter than a minute
        if (Global.intervalT < 60 & !Global.SetFrequentFlag) {
            Global.EXTRA_Message = String.format(Locale.getDefault(),
                    "You are going to collect the data each %d seconds\n\n" +
                            "Such a short (shorter than a minute) interval " +
                            "will cause some overestimate " +
                            "of the absolute value of the temperature",
                    Global.intervalT);
            Global.RiseWarning(this, "SetFrequent");
            return;
        }

        // check the length of the schedule
        if (scheduleLength > maxScheduleLength & !Global.SetLongScheduleFlag) {
            Global.EXTRA_Message = String.format(Locale.getDefault(),
                    "You are going to set the schedule of %d days long " +
                            "thas is longer than TWO YEARS. It could be " +
                            "too long for the device power cell.\n\n" +
                            "Are you sure?",
                    scheduleLength / (24 * 3600));
            Global.RiseWarning(this, "SetLongSchedule");
            return;
        }

        // show the schedule and ask again!
        if (!Global.SetScheduleFlag) {
            Global.EXTRA_Message =
                    "Data collection will start at:\n" +
                            startDate + " " + startTime + "\nand will finish at:\n" +
                            stopDate + " " + stopTime + "\n\n" +
                            "T-data collecting interval is:\n" +
                            Global.intervalT.toString()+" sec\n" +
                            "P-data collecting interval is:\n" +
                            Global.intervalP.toString()+" sec\n\n" +
                            "There will be collected\n" +
                            scheduleTVolume + " T-data points, and\n" +
                            schedulePVolume + " P-data points, per\n" +
                            scheduleLength/(24*3600) + " days.";
            Global.RiseMessage(this, "SetSchedule");
            return;
        }

        // calculate the address in the flash to start P-data from
        Global.AddressP1 = ((int) ((16 * scheduleTVolume) / 4096) + 1) * 4096;
        // set the schedule
        String command = String.format(Locale.getDefault(),
                "SetProgramm %d %d %d %d %d\r", Global.intervalT, Global.intervalP, Global.AddressP1, Global.startTime.getTimeInMillis()/1000, Global.stopTime.getTimeInMillis()/1000);
        Global.CDC_Send(command, this);
        String reply = Global.CDC_Get_String(this);
        Log.i("schedule", reply);
        // re-read schedule
        Global.ReReadScheduleFlag = true;
        getSchedule();
        refreshSchedule();
        // set flags back
        Global.SetScheduleFlag = false;
        Global.SetLongScheduleFlag = false;
        Global.SetStartInPastFlag = false;
        Global.SetStopInPastFlag = false;
        Global.SetFrequentFlag = false;

    }

    public void getSchedule() {
        if (Global.ReReadScheduleFlag) {
            Global.CDC_Send("GetProgramm\r", this);
            String reply = Global.CDC_Get_String(this);
            String[] schedule =  reply.split("\r\n");
            Log.i("schedule", reply);
            // collection intervals and data available
            Global.AddressT1 = 0;                                         // initial T address
            Global.AddressT2 = Integer.parseInt(schedule[1], 16);   // final T address
            Global.AddressP1 = Integer.parseInt(schedule[3], 16);   // initial P address
            Global.AddressP2 = Integer.parseInt(schedule[2], 16);   // final P address
            // data collected
            Global.dataCollectedT = Integer.parseInt(schedule[1], 16);
            Global.dataCollectedP = Integer.parseInt(schedule[2], 16) - Integer.parseInt(schedule[3], 16);
            // intervals
            Global.intervalT = Integer.parseInt(schedule[6]);
            Global.intervalP = Integer.parseInt(schedule[7]);
            // start and stop date&time
            Global.startTime.setTimeInMillis(Long.parseLong(schedule[4])*1000);
            Global.stopTime.setTimeInMillis(Long.parseLong(schedule[5])*1000);
            Global.ReReadScheduleFlag = false;
        }
    }

    public void grabSchedule() {
        textView = findViewById(R.id.TP_T_DataIntValue);
        if (textView.getText().toString().isEmpty())
            Global.intervalT = 60;
        else
            Global.intervalT = Integer.parseInt(textView.getText().toString());
        textView = findViewById(R.id.TP_P_DataIntValue);
        if (textView.getText().toString().isEmpty())
            Global.intervalP = 60;
        else
            Global.intervalP = Integer.parseInt(textView.getText().toString());

        Global.startTime.set(startDatePicker.getYear(), startDatePicker.getMonth(),startDatePicker.getDayOfMonth(),startTimePicker.getCurrentHour(), startTimePicker.getCurrentMinute(),0);
        Global.stopTime.set(stopDatePicker.getYear(), stopDatePicker.getMonth(),stopDatePicker.getDayOfMonth(),stopTimePicker.getCurrentHour(), stopTimePicker.getCurrentMinute(),0);
    }

    public void refreshSchedule() {
        // refreshing the interval and schedule
        textView = findViewById(R.id.TP_T_DataIntValue);
        textView.setText(String.valueOf(Global.intervalT));
        // refreshing the interval and schedule
        textView = findViewById(R.id.TP_P_DataIntValue);
        textView.setText(String.valueOf(Global.intervalP));
        //
        startDatePicker.updateDate(Global.startTime.get(Calendar.YEAR), Global.startTime.get(Calendar.MONTH), Global.startTime.get(Calendar.DAY_OF_MONTH));
        startTimePicker.setCurrentHour(Global.startTime.get(Calendar.HOUR_OF_DAY));
        startTimePicker.setCurrentMinute(Global.startTime.get(Calendar.MINUTE));
        //
        stopDatePicker.updateDate(Global.stopTime.get(Calendar.YEAR), Global.stopTime.get(Calendar.MONTH), Global.stopTime.get(Calendar.DAY_OF_MONTH));
        stopTimePicker.setCurrentHour(Global.stopTime.get(Calendar.HOUR_OF_DAY));
        stopTimePicker.setCurrentMinute(Global.stopTime.get(Calendar.MINUTE));
    }

    public void onDeviceClick(View view) {
        Intent intent = new Intent(this, DeviceTPActivity.class);
        startActivity(intent);
    }

    public void onTimeClick(View view) {
        Intent intent = new Intent(this, TimeActivity.class);
        startActivity(intent);
    }

    public void onCancelClick(View view)
    {
        finish();
    }

    public void onDownloadClick(View view) {
        // define the amount of the data to download
        Log.i("checkbox", "dwnldT = " + dwnldT.isChecked() + "; dwnldP = " + dwnldP.isChecked());
        if (view.getId() == findViewById(R.id.TP_DataADownlodB).getId()) {
            if (!Global.AdvancedDownloadFlag) {
                Intent intent = new Intent(this, AdvancedDownloadActivity.class);
                if (dwnldP.isChecked()) {
                    intent.putExtra("volume", Global.dataCollectedP);
                    intent.putExtra("mode", "Pressure");
                }
                else if (dwnldT.isChecked()) {
                    intent.putExtra("volume", Global.dataCollectedT);
                    intent.putExtra("mode", "Temperature");
                }
                else {
                    Global.EXTRA_Message = String.format(Locale.getDefault(),
                            "Please, check, which kind of the DATA would you like to download in Advanced mode!");
                    Global.RiseWarning(this, "Adwnld");
                    return;
                }
                startActivity(intent);
                return;
            }
            else
                Global.AdvancedDownloadFlag = false;
        }
        else {
            if (Global.BasicDownloadFlag) {
                Global.VolumeT = Global.dataCollectedT;
                Global.VolumeP = Global.dataCollectedP;
            }
        }
        // pick the file name
        if (!Global.SaveTFileFlag) {
            if (dwnldT.isChecked()) {
                Global.AskFileName(this, "T");
                return;
            }
            else {
                Global.EXTRA_TName = "TMP";
            }
        }
        if (!Global.SavePFileFlag & dwnldP.isChecked()) {
            Global.AskFileName(this, "P");
            return;
        }
        Global.SaveTFileFlag = false;
        Global.SavePFileFlag = false;
        Global.BasicDownloadFlag = true;
        // run the data download sequence
        if (!dwnldT.isChecked() & !dwnldP.isChecked()) {
            Global.EXTRA_Message = String.format(Locale.getDefault(),
                    "Please, check< which kind of the DATA would you like to download!");
            Global.RiseWarning(this, "none");
            return;
        }
        else
            dataTdownload();

    }

    public void dataTdownload() {
        // i need them  here!!!
        final float[][] T = new float[3][Global.VolumeT / 16];
        final long[] D = new long[Global.VolumeT / 16];
        final long[] V = new long[3];
        final DateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss YYYY");
        // disable all and start the progress bar
        disableActivity();
        findViewById(R.id.TP_CircularBar).setVisibility(View.VISIBLE);
        findViewById(R.id.TP_LinearBar).setVisibility(View.VISIBLE);
        findViewById(R.id.TP_DownloadingW).setVisibility(View.VISIBLE);
        // finally defined activity is necessary for activities started from inside the thread
        final Activity this_activity = this;
        // downloading thread, UI will be released at this point, excepting two RunOnUiThread processes inside this thread :)
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i("cycle", "volT = " + Global.VolumeT + "; addressT = " + Global.AddressT1);
                    // parse the calibration coefficients as floats
                    float A11 = Float.parseFloat(Global.devinfo[9]);
                    float A12 = Float.parseFloat(Global.devinfo[10]);
                    float A13 = Float.parseFloat(Global.devinfo[11]);
                    float R1 = Float.parseFloat(Global.devinfo[12]);
                    float A21 = Float.parseFloat(Global.devinfo[13]);
                    float A22 = Float.parseFloat(Global.devinfo[14]);
                    float A23 = Float.parseFloat(Global.devinfo[15]);
                    float R2 = Float.parseFloat(Global.devinfo[16]);
                    float A31 = Float.parseFloat(Global.devinfo[17]);
                    float A32 = Float.parseFloat(Global.devinfo[18]);
                    float A33 = Float.parseFloat(Global.devinfo[19]);
                    float R3 = Float.parseFloat(Global.devinfo[20]);
                    // run the download itself for the TEMPERATURE (per Global.pagesize pieces)
                    int countT = 0;
                    int address = Global.AddressT1;
                    int num;
                    long[] reply;
                    String dateString;
                    StringBuilder voltTStringBuilder = new StringBuilder();
                    StringBuilder dataTStringBuilder = new StringBuilder();
                    while (address < Global.VolumeT) {
                        // number of bytes to read at this iteration of while
                        num = Global.pagesize * (((int) (address / Global.pagesize) < (int) (Global.VolumeT / Global.pagesize)) ? 1 : 0) +
                                (Global.VolumeT % Global.pagesize) * (((int) (address / Global.pagesize) == (int) (Global.VolumeT / Global.pagesize)) ? 1 : 0);
                        // get the data
                        String command = String.format("SendDataToX86 %d %06x\r\n", num, address);
                        Global.CDC_Send(command, this_activity);
                        reply = Global.CDC_Get_Data(this_activity, num, command.length());
                        address += num;
                        Log.i("cylce", command);
                        Log.i("cycle", "num = " + num + "; address = " + address);
                        Log.i("cycle", "reply length = " + reply.length / (16));
                        //
                        for (int p = 0; p < reply.length / 16; p++) {
                            D[countT] = reply[4 * p] * 1000;
                            dateString = dateFormat.format(D[countT]);
                            V[0] = reply[4 * p + 1] - 4294967295L * ((reply[4 * p + 1] > 2147483648L) ? 1 : 0);
                            V[1] = reply[4 * p + 2] - 4294967295L * ((reply[4 * p + 2] > 2147483648L) ? 1 : 0);
                            V[2] = reply[4 * p + 3] - 4294967295L * ((reply[4 * p + 3] > 2147483648L) ? 1 : 0);
                            // convert to temperatures
                            T[0][countT] = Global.tenz2temp(V[0], A11, A12, A13, R1);
                            T[1][countT] = Global.tenz2temp(V[1], A21, A22, A23, R2);
                            T[2][countT] = Global.tenz2temp(V[2], A31, A32, A33, R3);
                            // strings
                            if (dwnldT.isChecked()) {
                                voltTStringBuilder.append(dateString).append("\t").append(V[0]).append("\t").append(V[1]).append("\t").append(V[2]).append("\n");
                                dataTStringBuilder.append(dateString).append("\t").append(T[0][countT]).append("\t\t").append(T[1][countT]).append("\t\t").append(T[2][countT]).append("\n");
                            }
                            countT += 1;
                        }

                        // move a progress a bit. 100 - is set as a maximum in the corresponding xml
                        final int progress;
                        if (dwnldP.isChecked()) {
                            progress = 100*address/(Global.VolumeT + Global.VolumeP);
                            Log.i("progress", "yes");
                        }
                        else {
                            progress = 100*address/(Global.VolumeT);
                            Log.i("progress", "no");
                        }
                        Log.i("progress", "barrT = " + progress);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ProgressBar progressBar = (ProgressBar) findViewById(R.id.TP_LinearBar);
                                progressBar.setProgress(progress);
                            }
                        });
                    }
                    // files writing
                    if (dwnldT.isChecked()) {
                        // temperature files and all the stuff
                        final File dataTFile = new File(Global.directory, Global.EXTRA_TName);
                        final File voltTFile = new File(Global.directory, "VOLTAGE-" + Global.EXTRA_TName);
                        // kick up a user not to overwrite
                        // TODO: ugly FUCK!!! It downloads and then crashes, if the file exists.
                        // TODO: The application hangs due to disable/enable activity
                        if (dataTFile.exists() & address != 0) {
                            Global.EXTRA_Message =
                                    "You have chosen the existing TEMPERATURE file name\n\n" +
                                            "To avoid any possible data loss, " +
                                            "overwriting was prohibited.\n\n" +
                                            "Plase, choose another file name!";
                            Global.RiseError( this_activity, false);
                            // bring the functionality back, otherwise it crashes :(
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    enableActivity();
                                    findViewById(R.id.TP_CircularBar).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.TP_LinearBar).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.TP_DownloadingW).setVisibility(View.INVISIBLE);
                                }
                            });
                            return;
                        }
                        // try to create new files
                        try {
                            dataTFile.createNewFile();
                            voltTFile.createNewFile();
                        } catch (IOException exp) {
                            exp.printStackTrace();
                            Global.EXTRA_Message = "The TEMPERATURE log-files cannot be created:\n" + exp + "\n\n" +
                                    "Did you granted the storage access permission to ERGlogger application???";
                            Global.RiseError(this_activity, false);
                            return;
                        }
                        // open output streams
                        FileOutputStream voltTOutputStream = new FileOutputStream(voltTFile);
                        FileOutputStream dataTOutputStream = new FileOutputStream(dataTFile);
                        //
                        String header;
                        String cDate = dateFormat.format(new Date());
                        // temperature file header
                        header =
                                "# Temperature ERG log-file\n# created " + cDate + "\n" +
                                        "# Device model: " +
                                        Global.devinfo[2].replace(" ", "") + "-" +
                                        Global.devinfo[3].replace(" ", "") + "\n#\t\tDr. Zoidberg\n#\n";
                        dataTOutputStream.write((header).getBytes());
                        // temperature voltage file header
                        header =
                                "# Voltage (Temperature) ERG log-file\n# created " + cDate + "\n" +
                                        "# Device model: " +
                                        Global.devinfo[2].replace(" ", "") + "-" +
                                        Global.devinfo[3].replace(" ", "") + "\n#\t\tDr. Zoidberg\n#\n";
                        voltTOutputStream.write((header).getBytes());
                        // write the data to files
                        voltTOutputStream.write((voltTStringBuilder.toString()).getBytes());
                        dataTOutputStream.write((dataTStringBuilder.toString()).getBytes());
                        // close the streams
                        dataTOutputStream.close();
                        voltTOutputStream.close();
                    }
                }
                catch (FileNotFoundException exp) {
                    exp.printStackTrace();
                    Global.EXTRA_Message = "Error in file write, \"file not found\" exception :(\n\n" +
                            "Exception message:\n\n" + exp.toString();
                    Global.RiseError(this_activity, false);
                }
                catch (IOException exp) {
                    exp.printStackTrace();
                    Global.EXTRA_Message = "Error in file write, \"I/O error\" exception :(" +
                            "Exception message:\n\n" + exp.toString();;
                    Global.RiseError(this_activity, false);
                }
                if (dwnldP.isChecked()) {
                    // temperature stuff
                    float[] TT = new float[T[0].length];
                    for (int p = 0; p < T[0].length; p++)
                        TT[p] = (T[0][p] + T[1][p] + T[2][p]) / 3;
                    dataPdownload(TT);
                }
                else {
                    // bring the functionality back
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            enableActivity();
                            findViewById(R.id.TP_CircularBar).setVisibility(View.INVISIBLE);
                            findViewById(R.id.TP_LinearBar).setVisibility(View.INVISIBLE);
                            findViewById(R.id.TP_DownloadingW).setVisibility(View.INVISIBLE);
                        }
                    });

                    Global.EXTRA_Message = "Download complete!!!";
                    Global.RiseMessage(this_activity, "Empty");
                }
            }
        }).start();
    }

    public void dataPdownload(final float[] T) {
        // string date formatter
        final DateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss YYYY");
        //final Interpolate interpT = new Interpolate();
        //interpT.createLinear(D, T);
        // file writing stuff
        final File dataPFile = new File(Global.directory, Global.EXTRA_PName);
        final File voltPFile = new File(Global.directory, "VOLTAGE-"+Global.EXTRA_PName);
        // kick up a user not to overwrite
        if (dataPFile.exists()) {
            Global.EXTRA_Message =
                    "You have chosen the existing PRESSURE file name\n\n" +
                            "To avoid any possible data loss, " +
                            "overwriting was prohibited.\n\n" +
                            "Plase, choose another file name!";
            Global.RiseError(this, false);
            return;
        }
        // try to create new files
        try {
            dataPFile.createNewFile();
            voltPFile.createNewFile();
        }
        catch (IOException exp) {
            exp.printStackTrace();
            Global.EXTRA_Message = "The PRESSURE log-files cannot be created:\n" + exp + "\n\n" +
                    "Did you granted the storage access permission to ERGlogger application???";
            Global.RiseError(this, false);
            return;
        }
        // finally defined activity is necessary for activities started from inside the thread
        final Activity this_activity = this;
        // downloading thread, UI will be released at this point, excepting two RunOnUiThread processes inside this thread :)
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // open output streams
                    FileOutputStream voltPOutputStream = new FileOutputStream(voltPFile);
                    FileOutputStream dataPOutputStream = new FileOutputStream(dataPFile);
                    // headers
                    final DateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss YYYY");
                    String header;
                    String cDate = dateFormat.format(new Date());
                    header =
                            "# Pressure ERG log-file\n# created " + cDate + "\n" +
                                    "# Device model: " +
                                    Global.devinfo[2].replace(" ", "") + "-" +
                                    Global.devinfo[3].replace(" ", "") + "\n#\t\tDr. Zoidberg\n#\n";
                    dataPOutputStream.write((header).getBytes());
                    // pressure voltage file header
                    header =
                            "# Voltage (Pressure) ERG log-file\n# created " + cDate + "\n" +
                                    "# Device model: " +
                                    Global.devinfo[2].replace(" ", "") + "-" +
                                    Global.devinfo[3].replace(" ", "") + "\n#\t\tDr. Zoidberg\n#\n";
                    voltPOutputStream.write((header).getBytes());

                    // parse the calibration coefficients as floats
                    float P1 = Float.parseFloat(Global.devinfo[21]);
                    float P2 = Float.parseFloat(Global.devinfo[22]);
                    float P3 = Float.parseFloat(Global.devinfo[23]);
                    // run the download itself for the PRESSURE (per Global.pagesize pieces)
                    int countP = 0;
                    int address = Global.AddressP1;
                    int num;
                    long D;
                    long[] V = new long[2];
                    long[] reply;
                    float P;
                    float ratio = (float) Global.intervalP / (float) Global.intervalT;
                    // date stuff
                    String dateString;
                    Log.i("init", "volP = " + Global.VolumeP + "; addressP = " + Global.AddressP1);
                    while ((address - Global.AddressP1) < Global.VolumeP) {
                        // number of bytes to read at this iteration of while
                        num = Global.pagesize * (((int) ((address - Global.AddressP1) / Global.pagesize) < (int) (Global.VolumeP / Global.pagesize)) ? 1 : 0) +
                                (Global.VolumeP % Global.pagesize) * (((int) ((address - Global.AddressP1) / Global.pagesize) == (int) (Global.VolumeP / Global.pagesize)) ? 1 : 0);
                        // get the data
                        String command = String.format("SendDataToX86 %d %06x\r\n", num, address);
                        Log.i("cycle", command);
                        Log.i("cycle", "num = " + num + "; address = " + address);
                        Global.CDC_Send(command, this_activity);
                        reply = Global.CDC_Get_Data(this_activity, num, command.length());
                        address += num;
                        Log.i("cycle", "reply length = " + reply.length / (8));
                        // row by row :(
                        StringBuilder voltPStringBuilder = new StringBuilder();
                        StringBuilder dataPStringBuilder = new StringBuilder();
                        for (int p = 0; p < reply.length / 8; p++) {
                            D = reply[2 * p] * 1000;
                            dateString = dateFormat.format(D);
                            V[0] = reply[2 * p + 1] - 4294967295L * ((reply[2 * p + 1] > 2147483648L) ? 1 : 0);
                            // temperature correction to the point, closest to the pressure point
                            // TODO: The linear interpolation is better, but slower :(
                            V[1] = V[0] + (long) (P1 * T[Math.round(countP * ratio)]);
                            // convert to pressure
                            P = P2 * V[1] + P3;
                            countP += 1;
                            // strings
                            voltPStringBuilder.append(dateString).append("\t").append(V[0]).append("\n");
                            dataPStringBuilder.append(dateString).append("\t").append(P / 1000).append("\n");
                        }
                        // files writing
                        voltPOutputStream.write((voltPStringBuilder.toString()).getBytes());
                        dataPOutputStream.write((dataPStringBuilder.toString()).getBytes());
                        // move a progress a bit. 100 - is set as a maximum in the corresponding xml
                        final int progress = 100 * (address - Global.AddressP1 + Global.VolumeT) / (Global.VolumeT + Global.VolumeP);
                        Log.i("progress", "barrP = " + progress);
                        runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ProgressBar progressBar = (ProgressBar) findViewById(R.id.TP_LinearBar);
                                    progressBar.setProgress(progress);
                                }
                        });
                    }
                    // close the streams
                    dataPOutputStream.close();
                    voltPOutputStream.close();
                }
                catch (FileNotFoundException exp) {
                    exp.printStackTrace();
                    Global.EXTRA_Message = "Error in file write, \"file not found\" exception :(\n\n" +
                            "Exception message:\n\n" + exp.toString();
                    Global.RiseError(this_activity, false);
                }
                catch (IOException exp) {
                    exp.printStackTrace();
                    Global.EXTRA_Message = "Error in file write, \"I/O error\" exception :(" +
                            "Exception message:\n\n" + exp.toString();;
                    Global.RiseError(this_activity, false);
                }
                // bring the functionality back
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        enableActivity();
                        findViewById(R.id.TP_CircularBar).setVisibility(View.INVISIBLE);
                        findViewById(R.id.TP_LinearBar).setVisibility(View.INVISIBLE);
                        findViewById(R.id.TP_DownloadingW).setVisibility(View.INVISIBLE);
                    }
                });
                Global.EXTRA_Message = "Download complete!!!";
                Global.RiseMessage(this_activity,"Empty");
            }
        }).start();
    }

    public void onScheduleClick(View view) {
        if (view.getId() == R.id.TP_SaveScheduleB) {
            //grabSchedule();
            Intent intent = new Intent("android.intent.action.CREATE_DOCUMENT");
            ((Intent) intent).addCategory("android.intent.category.OPENABLE");
            ((Intent) intent).setType("text/plain");
            // create default file-name
            String suggestedName = String.format("ERG_schedule-%04d.%02d.%02d-%02d:%02d-%04d.%02d.%02d-%02d:%02d.txt",
                    Global.startTime.get(Calendar.YEAR), Global.startTime.get(Calendar.MONTH)+1, Global.startTime.get(Calendar.DAY_OF_MONTH),
                    Global.startTime.get(Calendar.HOUR_OF_DAY), Global.startTime.get(Calendar.MINUTE),
                    Global.stopTime.get(Calendar.YEAR), Global.stopTime.get(Calendar.MONTH)+1, Global.stopTime.get(Calendar.DAY_OF_MONTH),
                    Global.stopTime.get(Calendar.HOUR_OF_DAY), Global.stopTime.get(Calendar.MINUTE));
            Log.i("schedule", "Suggested name: " + suggestedName);
            //Log.i("schedule", "Year: " + Global.startTime.YEAR);
            //Log.i("schedule", "millis: " + Global.startTime.getTimeInMillis());
            intent.putExtra(Intent.EXTRA_TITLE, suggestedName);
            startActivityForResult(intent, CREATE_SCHEDULE_FILE_CODE);
        }
        else if (view.getId() == R.id.TP_LoadScheduleB) {
            Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
            ((Intent) intent).addCategory("android.intent.category.OPENABLE");
            ((Intent) intent).setType("text/plain");
            startActivityForResult(intent, OPEN_SCHEDULE_FILE_CODE);
        }
    }

    private void disableActivity() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        findViewById(R.id.TP_DevConW).setAlpha(0.1f);
        findViewById(R.id.TP_DevNameW).setAlpha(0.1f);
        findViewById(R.id.TP_DevB).setAlpha(0.1f);
        findViewById(R.id.TP_TimeB).setAlpha(0.1f);
        findViewById(R.id.TP_LogoP).setAlpha(0.1f);
        findViewById(R.id.TP_DataCSW).setAlpha(0.1f);
        findViewById(R.id.TP_StartDatePicker).setAlpha(0.1f);
        findViewById(R.id.TP_StartTimePicker).setAlpha(0.1f);
        findViewById(R.id.TP_StopDatePicker).setAlpha(0.1f);
        findViewById(R.id.TP_StopTimePicker).setAlpha(0.1f);
        findViewById(R.id.TP_SchStartW).setAlpha(0.1f);
        findViewById(R.id.TP_SchFinishW).setAlpha(0.1f);
        findViewById(R.id.TP_T_SchIntW).setAlpha(0.1f);
        findViewById(R.id.TP_P_SchIntW).setAlpha(0.1f);
        findViewById(R.id.TP_T_DataIntValue).setAlpha(0.1f);
        findViewById(R.id.TP_P_DataIntValue).setAlpha(0.1f);
        findViewById(R.id.TP_SetScheduleB).setAlpha(0.1f);
        findViewById(R.id.TP_SaveScheduleB).setAlpha(0.1f);
        findViewById(R.id.TP_LoadScheduleB).setAlpha(0.1f);
        findViewById(R.id.TP_DataCollectedW).setAlpha(0.1f);
        findViewById(R.id.TP_DataBDownlodB).setAlpha(0.1f);
        findViewById(R.id.TP_DataADownlodB).setAlpha(0.1f);
        findViewById(R.id.TP_DataDwnldW).setAlpha(0.1f);
        findViewById(R.id.TP_TcheckBx).setAlpha(0.1f);
        findViewById(R.id.TP_PcheckBx).setAlpha(0.1f);
        findViewById(R.id.TP_CancelB).setAlpha(0.1f);
        findViewById(R.id.TP_T_SchIntervals).setAlpha(0.1f);
    }

    private void enableActivity() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        findViewById(R.id.TP_DevConW).setAlpha(1.0f);
        findViewById(R.id.TP_DevNameW).setAlpha(1.0f);
        findViewById(R.id.TP_DevB).setAlpha(1.0f);
        findViewById(R.id.TP_TimeB).setAlpha(1.0f);
        findViewById(R.id.TP_LogoP).setAlpha(1.0f);
        findViewById(R.id.TP_DataCSW).setAlpha(1.0f);
        findViewById(R.id.TP_StartDatePicker).setAlpha(1.0f);
        findViewById(R.id.TP_StartTimePicker).setAlpha(1.0f);
        findViewById(R.id.TP_StopDatePicker).setAlpha(1.0f);
        findViewById(R.id.TP_StopTimePicker).setAlpha(1.0f);
        findViewById(R.id.TP_SchStartW).setAlpha(1.0f);
        findViewById(R.id.TP_SchFinishW).setAlpha(1.0f);
        findViewById(R.id.TP_T_SchIntW).setAlpha(1.0f);
        findViewById(R.id.TP_P_SchIntW).setAlpha(1.0f);
        findViewById(R.id.TP_T_DataIntValue).setAlpha(1.0f);
        findViewById(R.id.TP_P_DataIntValue).setAlpha(1.0f);
        findViewById(R.id.TP_SetScheduleB).setAlpha(1.0f);
        findViewById(R.id.TP_SaveScheduleB).setAlpha(1.0f);
        findViewById(R.id.TP_LoadScheduleB).setAlpha(1.0f);
        findViewById(R.id.TP_DataCollectedW).setAlpha(1.0f);
        findViewById(R.id.TP_DataBDownlodB).setAlpha(1.0f);
        findViewById(R.id.TP_DataADownlodB).setAlpha(1.0f);
        findViewById(R.id.TP_DataDwnldW).setAlpha(1.0f);
        findViewById(R.id.TP_TcheckBx).setAlpha(1.0f);
        findViewById(R.id.TP_PcheckBx).setAlpha(1.0f);
        findViewById(R.id.TP_CancelB).setAlpha(1.0f);
        findViewById(R.id.TP_T_SchIntervals).setAlpha(1.0f);
    }
}

