package com.ERG.erglogger;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;

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

//public class TActivity extends AppCompatActivity implements View.OnClickListener {
public class TActivity extends AppCompatActivity {
    private static final int CREATE_SCHEDULE_FILE_CODE = 12;
    private static final int OPEN_SCHEDULE_FILE_CODE = 21;
    // some global text-view
    TextView textView;
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
        setContentView(R.layout.activity_t);
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
        textView = findViewById(R.id.T_DevNameW);
        textView.setText(devinfo);
        // date-time pickers settings
        startDatePicker = findViewById(R.id.T_StartDatePicker);
        stopDatePicker = findViewById(R.id.T_StopDatePicker);
        startTimePicker = findViewById(R.id.T_StartTimePicker);
        startTimePicker.setIs24HourView(Boolean.TRUE);
        stopTimePicker = findViewById(R.id.T_StopTimePicker);
        stopTimePicker.setIs24HourView(Boolean.TRUE);
        // disable progress bars
        View text= findViewById(R.id.T_DownloadingW);
        text.setVisibility(View.INVISIBLE);
        text.bringToFront();
        ProgressBar cBar = findViewById(R.id.T_CircularBar);
        cBar.setVisibility(View.INVISIBLE);
        cBar.getIndeterminateDrawable().setColorFilter(0xFF0099FF, android.graphics.PorterDuff.Mode.MULTIPLY);
        ProgressBar lBar = findViewById(R.id.T_LinearBar);
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
        String message = String.format(Locale.getDefault(),"There is %d samples in the memory", Global.dataCollectedT / 16);
        textView = findViewById(R.id.T_DataCollectedW);
        textView.setText(message);
        // run setSchedule if necessary
        if (Global.SetLongScheduleFlag |
                Global.SetScheduleFlag |
                Global.SetStartInPastFlag |
                Global.SetStopInPastFlag |
                Global.SetFrequentFlag) {
            setSchedule(findViewById(R.id.T_SetScheduleB));
        }
        //
        if (Global.SaveTFileFlag) {
            onDownloadClick(findViewById(R.id.T_DataBDownlodB));
        }
        if (Global.AdvancedDownloadFlag) {
            onDownloadClick(findViewById(R.id.T_DataADownlodB));
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
            Log.i("schedule", Global.startTime.getTimeInMillis()+" "+Global.stopTime.getTimeInMillis()+" "+Global.intervalT);
            String schedule = String.format("%d\n%d\n%d\n", Global.startTime.getTimeInMillis(), Global.stopTime.getTimeInMillis(), Global.intervalT);
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
                        if (schedule.length == 3) {
                            Global.startTime.setTimeInMillis(Long.parseLong(schedule[0]));
                            Global.stopTime.setTimeInMillis(Long.parseLong(schedule[1]));
                            Global.intervalT = Integer.parseInt(schedule[2]);
                            refreshSchedule();
                            //setSchedule(findViewById(R.id.T_SetScheduleB));
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
        if (Global.intervalT < 3) {
            Global.EXTRA_Message = String.format(Locale.getDefault(),
                    "The data collection interval MUST be at least 3 second, " +
                            "while you have requested %d seconds\n\n" +
                            "So short interval may force the device to brick out :(\n\n" +
                            "Please, increase the interval!", Global.intervalT);
            Global.RiseError(this, false);
            return;
        }

        long maxScheduleLength = 2*365*24*3600;
        long scheduleLength = Math.round((Global.stopTime.getTimeInMillis() - Global.startTime.getTimeInMillis()) / 1000);
        long scheduleVolume = Math.round((Global.stopTime.getTimeInMillis() - Global.startTime.getTimeInMillis())/(1000*Global.intervalT));
        long flashSize = Global.getFlashSize(this);
        Log.i("schedule", "schedule length = " + String.valueOf(scheduleLength));
        Log.i("schedule", "schedule volume = " + String.valueOf(scheduleVolume));

        // check if the number of data points to collect is positive!
        if (scheduleVolume < 0) {
            Global.EXTRA_Message = String.format(Locale.getDefault(),
                    "You have requested too sparce schedule, so no data will be collected!!!\n\n" +
                            "Please, decrease the Data Collecting Interval");
            Global.RiseError(this, false);
            return;
        }

        // check the amount of data points requested. Max available provided by getFlashSize()
        if (flashSize < 16*scheduleVolume) {
            Global.EXTRA_Message = String.format(Locale.getDefault(),
                    "You have requested\n%d samples,\n" +
                            "while the device's internal memory capacity is only\n" +
                            "%d samples\n\nThe schedule cannot be set!!!",
                    scheduleVolume, flashSize / 16);
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
                    stopDate + " " + stopTime + "\n\nData collecting interval is:\n" +
                    Global.intervalT.toString()+" sec\n\nThere will be collected\n" +
                    scheduleVolume + " data points, per\n" +
                    scheduleLength/(24*3600) + " days.";
            Global.RiseMessage(this, "SetSchedule");
            return;
        }

        // set the schedule
        String command = String.format("SetProgramm %d %d %d\r", Global.intervalT, Global.startTime.getTimeInMillis()/1000, Global.stopTime.getTimeInMillis()/1000);
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
            //String reply = "GetProgramm\r\n5d630\r\n1480752135\r\n1530752149\r\n56\r\nOK";
            // collection interval and data available
            Global.dataCollectedT = Integer.parseInt(schedule[1], 16);
            Global.intervalT = Integer.parseInt(schedule[4]);
            // start and stop date&time
            Global.startTime.setTimeInMillis(Long.parseLong(schedule[2])*1000);
            Global.stopTime.setTimeInMillis(Long.parseLong(schedule[3])*1000);
            Global.ReReadScheduleFlag = false;
        }
    }

    public void grabSchedule() {
        textView = findViewById(R.id.T_DataIntValue);
        if (textView.getText().toString().isEmpty())
            Global.intervalT = 60;
        else
            Global.intervalT = Integer.parseInt(textView.getText().toString());
        Global.startTime.set(startDatePicker.getYear(), startDatePicker.getMonth(),startDatePicker.getDayOfMonth(),startTimePicker.getCurrentHour(), startTimePicker.getCurrentMinute(),0);
        Global.stopTime.set(stopDatePicker.getYear(), stopDatePicker.getMonth(),stopDatePicker.getDayOfMonth(),stopTimePicker.getCurrentHour(), stopTimePicker.getCurrentMinute(),0);
    }

    public void refreshSchedule() {
        // refreshing the interval and schedule
        textView = findViewById(R.id.T_DataIntValue);
        textView.setText(String.valueOf(Global.intervalT));
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
        Intent intent = new Intent(this, DeviceTActivity.class);
        startActivity(intent);
    }

    public void onTimeClick(View view) {
        Intent intent = new Intent(this, TimeActivity.class);
        startActivity(intent);
    }

    public void onCancelClick(View view) {
        finish();
    }

    public void onDownloadClick(View view) {
        // define the amount of the data to download
        if (view.getId() == findViewById(R.id.T_DataADownlodB).getId()) {
            if (!Global.AdvancedDownloadFlag) {
                Intent intent = new Intent(this, AdvancedDownloadActivity.class);
                intent.putExtra("volume", Global.dataCollectedT);
                intent.putExtra("mode", "Temperature");
                startActivity(intent);
                return;
            } else {
                Global.AdvancedDownloadFlag = false;
            }
        }
        else {
            if (Global.BasicDownloadFlag) {
                Global.AddressT1 = 0;
                Global.VolumeT = Global.dataCollectedT;
            }
        }
        // pick the file name
        if (!Global.SaveTFileFlag) {
            Global.AskFileName(this, "T");
            return;
        }
        Global.SaveTFileFlag = false;
        Global.BasicDownloadFlag = true;
        // run the download
        dataDownload();
    }

    public void dataDownload() {
        final File tempFile = new File(Global.directory, Global.EXTRA_TName);
        final File voltFile = new File(Global.directory, "VOLTAGE-"+Global.EXTRA_TName);
        // kick up a user not to overwrite
        if (tempFile.exists() | voltFile.exists()) {
            Global.EXTRA_Message =
                    "You have chosen the existing file name\n\n" +
                    "To avoid any possible data loss, " +
                    "overwriting was prohibited.\n\n" +
                    "Plase, choose another file name!";
            Global.RiseError(this, false);
            return;
        }
        // try to create new files
        try {
            voltFile.createNewFile();
            tempFile.createNewFile();
        }
        catch (IOException exp) {
            exp.printStackTrace();
            Global.EXTRA_Message = "The log-files cannot be created:\n" + exp + "\n\n" +
                    "Did you granted the storage access permission to ERGlogger application???";
            Global.RiseError(this, false);
            return;
        }
        // disable all and start the progress bar
        disableActivity();
        findViewById(R.id.T_CircularBar).setVisibility(View.VISIBLE);
        findViewById(R.id.T_LinearBar).setVisibility(View.VISIBLE);
        findViewById(R.id.T_DownloadingW).setVisibility(View.VISIBLE);
        // finally defined activity is ne cessary for activities started from inside the thread
        final Activity this_activity = this;
        // downloading thread, UI will be released at this point, excepting two RunOnUiThread processes inside this thread :)
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // open output streams
                    FileOutputStream voltOutputStream = new FileOutputStream(voltFile);
                    FileOutputStream tempOutputStream = new FileOutputStream(tempFile);
                    // headers
                    final DateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
                    String header;
                    String cDate = dateFormat.format(new Date());
                    // temperature file header
                    header =
                            "# Temperature ERG log-file\n# created " + cDate + "\n" +
                                    "# Device model: " +
                                    Global.devinfo[2].replace(" ", "") + "-" +
                                    Global.devinfo[3].replace(" ", "") + "\n#\t\tDr. Zoidberg\n#\n";
                    tempOutputStream.write((header).getBytes());
                    // viltage file header
                    header =
                            "# Voltage ERG log-file\n# created " + cDate + "\n" +
                                    "# Device model: " +
                                    Global.devinfo[2].replace(" ", "") + "-" +
                                    Global.devinfo[3].replace(" ", "") + "\n#\t\tDr. Zoidberg\n#\n";
                    voltOutputStream.write((header).getBytes());
                    // parse the calibration coefficients as floats
                    Log.i("cycle", "vol = " + Global.VolumeT + "; address = " + Global.AddressT1);
                    float A11 = Float.parseFloat(Global.devinfo[8]);
                    float A12 = Float.parseFloat(Global.devinfo[9]);
                    float A13 = Float.parseFloat(Global.devinfo[10]);
                    float R1 = Float.parseFloat(Global.devinfo[11]);
                    float A21 = Float.parseFloat(Global.devinfo[12]);
                    float A22 = Float.parseFloat(Global.devinfo[13]);
                    float A23 = Float.parseFloat(Global.devinfo[14]);
                    float R2 = Float.parseFloat(Global.devinfo[15]);
                    float A31 = Float.parseFloat(Global.devinfo[16]);
                    float A32 = Float.parseFloat(Global.devinfo[17]);
                    float A33 = Float.parseFloat(Global.devinfo[18]);
                    float R3 = Float.parseFloat(Global.devinfo[19]);
                    // run the download itself (per Global.pagesize pieces)
                    int address = Global.AddressT1;
                    int num;
                    long dateLong;
                    long[] reply;
                    long[] V = new long[3];
                    float[] T = new float[3];
                    String command;
                    String dateString;
                    while (address < Global.VolumeT) {
                        // number of bytes to read at this iteration of while
                        num = Global.pagesize * (((int) (address / Global.pagesize) < (int) (Global.VolumeT / Global.pagesize)) ? 1 : 0) +
                                (Global.VolumeT % Global.pagesize) * (((int) (address / Global.pagesize) == (int) (Global.VolumeT / Global.pagesize)) ? 1 : 0);
                        // get the data
                        command = String.format("SendDataToX86 %d %06x\r\n", num, address);
                        Global.CDC_Send(command, this_activity);
                        reply = Global.CDC_Get_Data(this_activity, num, command.length());
                        address += num;
                        Log.i("cylce", command);
                        Log.i("cycle", "num = " + num + "; address = " + address);
                        Log.i("cycle", "reply length = " + reply.length / (16));
                        //
                        StringBuilder voltStringBuilder = new StringBuilder();
                        StringBuilder tempStringBuilder = new StringBuilder();
                        for (int p = 0; p < reply.length / (16); p++) {
                            dateLong = reply[4 * p] * 1000;
                            dateString = dateFormat.format(dateLong);
                            Log.i("date",dateLong + " -> " + dateString);
                            V[0] = reply[4 * p + 1] - 4294967295L * ((reply[4 * p + 1] > 2147483648L) ? 1 : 0);
                            V[1] = reply[4 * p + 2] - 4294967295L * ((reply[4 * p + 2] > 2147483648L) ? 1 : 0);
                            V[2] = reply[4 * p + 3] - 4294967295L * ((reply[4 * p + 3] > 2147483648L) ? 1 : 0);
                            // convert to temperatures
                            T[0] = Global.tenz2temp(V[0], A11, A12, A13, R1);
                            T[1] = Global.tenz2temp(V[1], A21, A22, A23, R2);
                            T[2] = Global.tenz2temp(V[2], A31, A32, A33, R3);
                            // strings
                            voltStringBuilder.append(dateString).append("\t").append(V[0]).append("\t").append(V[1]).append("\t").append(V[2]).append("\n");
                            tempStringBuilder.append(dateString).append("\t").append(T[0]).append("\t\t").append(T[1]).append("\t\t").append(T[2]).append("\n");
                        }
                        // files writing
                        voltOutputStream.write((voltStringBuilder.toString()).getBytes());
                        tempOutputStream.write((tempStringBuilder.toString()).getBytes());
                        // move a progress a bit. 100 - is set as a maximum in the corresponding xml
                        final int progress = 100*address/Global.VolumeT;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ProgressBar progressBar = (ProgressBar) findViewById(R.id.T_LinearBar);
                                progressBar.setProgress(progress);
                            }
                        });
                    }
                    // close the streams
                    tempOutputStream.close();
                    voltOutputStream.close();
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
                        findViewById(R.id.T_CircularBar).setVisibility(View.INVISIBLE);
                        findViewById(R.id.T_LinearBar).setVisibility(View.INVISIBLE);
                        findViewById(R.id.T_DownloadingW).setVisibility(View.INVISIBLE);
                    }
                });
                Global.EXTRA_Message = "Download complete!!!";
                Global.RiseMessage(this_activity,"Empty");
            }
        }).start();
    }

    public void onScheduleClick(View view) {
        if (view.getId() == R.id.T_SaveScheduleB) {
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
        else if (view.getId() == R.id.T_LoadScheduleB) {
            Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
            ((Intent) intent).addCategory("android.intent.category.OPENABLE");
            ((Intent) intent).setType("text/plain");
            startActivityForResult(intent, OPEN_SCHEDULE_FILE_CODE);
        }
    }

    private void disableActivity() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        findViewById(R.id.T_DevConW).setAlpha(0.1f);
        findViewById(R.id.T_DevNameW).setAlpha(0.1f);
        findViewById(R.id.T_DevB).setAlpha(0.1f);
        findViewById(R.id.T_TimeB).setAlpha(0.1f);
        findViewById(R.id.T_LogoP).setAlpha(0.1f);
        findViewById(R.id.T_DataCSW).setAlpha(0.1f);
        findViewById(R.id.T_StartDatePicker).setAlpha(0.1f);
        findViewById(R.id.T_StartTimePicker).setAlpha(0.1f);
        findViewById(R.id.T_StopDatePicker).setAlpha(0.1f);
        findViewById(R.id.T_StopTimePicker).setAlpha(0.1f);
        findViewById(R.id.T_SchStartW).setAlpha(0.1f);
        findViewById(R.id.T_SchFinishW).setAlpha(0.1f);
        findViewById(R.id.T_SchIntW).setAlpha(0.1f);
        findViewById(R.id.T_DataIntValue).setAlpha(0.1f);
        findViewById(R.id.T_SetScheduleB).setAlpha(0.1f);
        findViewById(R.id.T_SaveScheduleB).setAlpha(0.1f);
        findViewById(R.id.T_LoadScheduleB).setAlpha(0.1f);
        findViewById(R.id.T_DataCollectedW).setAlpha(0.1f);
        findViewById(R.id.T_DataBDownlodB).setAlpha(0.1f);
        findViewById(R.id.T_DataADownlodB).setAlpha(0.1f);
        findViewById(R.id.T_DataADownlodB).setAlpha(0.1f);
        findViewById(R.id.T_DataADownlodB).setAlpha(0.1f);
        findViewById(R.id.T_CancelB).setAlpha(0.1f);
    }

    private void enableActivity() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        findViewById(R.id.T_DevConW).setAlpha(1.0f);
        findViewById(R.id.T_DevNameW).setAlpha(1.0f);
        findViewById(R.id.T_DevB).setAlpha(1.0f);
        findViewById(R.id.T_TimeB).setAlpha(1.0f);
        findViewById(R.id.T_LogoP).setAlpha(1.0f);
        findViewById(R.id.T_DataCSW).setAlpha(1.0f);
        findViewById(R.id.T_StartDatePicker).setAlpha(1.0f);
        findViewById(R.id.T_StartTimePicker).setAlpha(1.0f);
        findViewById(R.id.T_StopDatePicker).setAlpha(1.0f);
        findViewById(R.id.T_StopTimePicker).setAlpha(1.0f);
        findViewById(R.id.T_SchStartW).setAlpha(1.0f);
        findViewById(R.id.T_SchFinishW).setAlpha(1.0f);
        findViewById(R.id.T_SchIntW).setAlpha(1.0f);
        findViewById(R.id.T_DataIntValue).setAlpha(1.0f);
        findViewById(R.id.T_SetScheduleB).setAlpha(1.0f);
        findViewById(R.id.T_SaveScheduleB).setAlpha(1.0f);
        findViewById(R.id.T_LoadScheduleB).setAlpha(1.0f);
        findViewById(R.id.T_DataCollectedW).setAlpha(1.0f);
        findViewById(R.id.T_DataBDownlodB).setAlpha(1.0f);
        findViewById(R.id.T_DataADownlodB).setAlpha(1.0f);
        findViewById(R.id.T_DataADownlodB).setAlpha(1.0f);
        findViewById(R.id.T_DataADownlodB).setAlpha(1.0f);
        findViewById(R.id.T_CancelB).setAlpha(1.0f);
    }
}
