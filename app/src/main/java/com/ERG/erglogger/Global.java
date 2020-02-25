package com.ERG.erglogger;

import android.app.Activity;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;


public class Global {
    // schedule flags
    public static boolean ReReadScheduleFlag = true;
    public static boolean SetStopInPastFlag = false;
    public static boolean SetStartInPastFlag = false;
    public static boolean SetFrequentFlag = false;
    public static boolean SetLongScheduleFlag = false;
    public static boolean SetScheduleFlag = false;
    // time flags
    public static boolean SetTimeFlag = false;
    public static boolean SetPrsclFlag = false;
    public static boolean AllowLiveClockFlag = false;
    public static boolean KillWatchThreadFlag = true;
    // data download flags
    public static boolean SaveFileFlag = false;
    public static boolean AdvancedDownloadFlag = false;
    public static boolean BasicDownloadFlag = true;
    public static int Address = 0;
    public static int Volume = 0;
    //
    public static String EXTRA_Message = "message";
    public static String EXTRA_Name = "ERG";
    public static String EXTRA_Flag = "flag";
    public static String[] devinfo;
    //public static String[] schedule;
    public static Integer interval = 60;
    public static Integer dataCollected = 0;
    public static Calendar startTime = Calendar.getInstance();
    public static Calendar stopTime = Calendar.getInstance();
    public static UsbSerialPort port;
    public static int pagesize = 32*4096;
    public static byte[] usb_rx_buffer = new byte[pagesize];

    // rises the ErrorActivity with some stupid message
    public static void RiseError(Activity obj, Boolean exit) {
        //obj.finish();
        Intent intent = new Intent(obj, ErrorActivity.class);
        intent.putExtra(Global.EXTRA_Flag, exit);
        obj.startActivity(intent);
    }

    // rises the WarningActivity with some stupid message
    public static void RiseWarning(Activity obj, String flag) {
        Intent intent = new Intent(obj, WarningActivity.class);
        intent.putExtra(Global.EXTRA_Flag, flag);
        obj.startActivity(intent);
    }

    // rises the MessageActivity with some stupid message
    public static void RiseMessage(Activity obj, String flag) {
        Intent intent = new Intent(obj, MessageActivity.class);
        intent.putExtra(Global.EXTRA_Flag, flag);
        obj.startActivity(intent);
    }

    // sends a command to ERGlogger device
    public static void CDC_Send(String command, Activity obj) {
        try {
            Global.port.write(command.getBytes(), 50);
        }
        catch (IOException exp) {
            Global.EXTRA_Message = "Port write failed.\n\nException message is:\n\n" + exp.getMessage();
            obj.finish();
            Intent intent = new Intent(obj, ErrorActivity.class);
            intent.putExtra(Global.EXTRA_Flag, true);
            obj.startActivity(intent);
        }
    }

    // returns the string read from the device
    public static String CDC_Get_String(Activity obj) {
        int len = 0, rpl =-1;
        StringBuilder reply = new StringBuilder();
        while (rpl != 0) {
            try {
                SystemClock.sleep(10);
                rpl = port.read(usb_rx_buffer,50);
                len += rpl;
                if (rpl != 0) {
                    for (int p = 0; p < rpl; p++) {
                        reply.append((char) Global.usb_rx_buffer[p]);
                    }
                }
            } catch (IOException exp) {
                Global.EXTRA_Message = "Port read failed.\n\nException message is:\n\n" + exp.getMessage();
                Intent intent = new Intent(obj, ErrorActivity.class);
                intent.putExtra(Global.EXTRA_Flag, true);
                obj.startActivity(intent);
            }
        }
        return reply.toString();
    }

    // returns the string read from the device
    public static long[] CDC_Get_Data(Activity obj, int vol, int initlen) {
        int len, rpl = initlen;
        //StringBuilder reply = new StringBuilder();
        ByteArrayOutputStream replyByteArray = new ByteArrayOutputStream();
        // read the echo of the command from the device
        len = 0;
        while (len < initlen) {
            try {
                SystemClock.sleep(10);
                rpl = port.read(usb_rx_buffer, 50);
                len += rpl;
                Log.i("echo", "rpl = " + rpl + "; initlen = " + initlen);
                if (rpl == 0) {
                    EXTRA_Message =
                            "Device returned NO data :(\n\n" +
                            "Please, disconnect the device, close the application an try again!";
                    RiseError(obj, false);
                }
            }
            catch (IOException exp) {
                EXTRA_Message =
                        "Port read failed.\n\n" +
                        "Exception message is:\n\n" + exp.getMessage();
                RiseError(obj, false);
            }
        }
        // read some data returned together with the echo. the latter occurs if
        // the time necessary to read the requested amount of the bytes
        // from the internal flash of the device to its operating memory
        // is shorter than 10 msec
        len -= initlen;
        replyByteArray.write(usb_rx_buffer, initlen, len);
        while (len < vol) {
            try {
                SystemClock.sleep(10);
                rpl = port.read(usb_rx_buffer, 50);
                len += rpl;
                replyByteArray.write(usb_rx_buffer, 0, rpl);
                Log.i("data", "rpl = " + rpl + "; len = " + len);
                if (rpl == 0) {
                    EXTRA_Message =
                            "Device returned NO data :(\n\n" +
                            "Please, disconnect the device, close the application an try again!";
                    RiseError(obj, false);
                }
            }
            catch (IOException exp) {
                Global.EXTRA_Message =
                        "Port read failed.\n\n" +
                        "Exception message is:\n\n" + exp.getMessage();
                RiseError(obj, false);
            }
        }
        // to long!
        long[] replyLong = new long[vol];
        byte[] replyByte = replyByteArray.toByteArray();
        for (int p = 0; p < vol/4; p++) {
            //String msg = String.format("%02x %02x %02x %02x", replyByte[4 * p + 0], replyByte[4 * p + 1], replyByte[4 * p + 2], replyByte[4 * p + 3]);
            //Log.i("data", "hexstring: = " + msg);
            // this is the fastest way to do that
            replyLong[p] =
                    ((replyByte[4 * p + 0] & 0xff) << 24) |
                    ((replyByte[4 * p + 1] & 0xff) << 16) |
                    ((replyByte[4 * p + 2] & 0xff) <<  8) |
                    ((replyByte[4 * p + 3] & 0xff) <<  0);
        }
        return replyLong;
    }

    // returns the devices flash size in Bytes
    public static int getFlashSize(Activity obj) {
        int capacity = 0;
        if (Global.devinfo[6].contains("MX25L64"))
            capacity = Math.round(64 * 1024 * 1024 / 8);
        else if (Global.devinfo[6].contains("MX25L128"))
            capacity = Math.round(128 * 1024 * 1024 / 8);
        else if (Global.devinfo[6].contains("MX25L256"))
            capacity = Math.round(256 * 1024 * 1024 / 8);
        else {
            Global.EXTRA_Message = "The ERGlogger device flash size cannot be defined :(\nThe flash memory unit name is:\n" + Global.devinfo[5];
            Intent intent = new Intent(obj, ErrorActivity.class);
            intent.putExtra(Global.EXTRA_Flag, true);
            obj.startActivity(intent);
        }
        return capacity;
    }

    public static float tenz2temp(long u, float A0, float A1, float A2, float R0) {
        float V  = (float) 1250.0;   // mV
        float R1 = (float) 1050.0;   // Ohm
        float R2 = (float) 5000.0;   // Ohm
        float U = (float) u/1000000; // to convert it from nV to mV
        float T = ((R2/R0)*(V*R1 + U*(R1+R2))/(V*R2 - U*(R1+R2)) - 1)/A0;
        // R1 and R2 correction
        R1 =  R1*(1 + T*A1);
        R2 =  R2*(1 + T*A2);
        return (float) Math.floor(10000*((R2/R0)*(V*R1 + U*(R1+R2))/(V*R2 - U*(R1+R2)) - 1)/A0)/10000;
        //return ((R2/R0)*(V*R1 + U*(R1+R2))/(V*R2 - U*(R1+R2)) - 1)/A0;
    }
}