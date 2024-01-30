package com.ERG.erglogger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.util.List;

import static android.app.PendingIntent.FLAG_MUTABLE;

public class MainActivity extends AppCompatActivity {
    UsbManager manager;
    UsbSerialDriver driver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        askForPermissions();
        //setContentView(R.layout.activity_main);
    }


    // do all the job
    @Override
    protected void onResume() {
        super.onResume();
        //Getting the storage permission status
        int result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            Log.i("storage permission", "GRANTED");
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            Log.i("storage permission", "DENIED");
            return;
        }
        // creating a directory for files storing
        if (!Global.directory.exists()) {
            boolean success = Global.directory.mkdirs();
            if (!success) {
                Global.EXTRA_Message = "The directory for file storage was not created :(\n\n" +
                        "The ERGlogger application requires the permission to access the storage of the device!";
                Global.RiseError(this, false);
            }
            Log.i("directory", "CREATED");
        } else {
            Log.i("directory", "EXISTS");
        }

        // usb initial stuff and permissions
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (manager.getDeviceList().isEmpty()) {
            Global.EXTRA_Message = "Cannot find any USB device";
            Global.RiseError(this, true);
            finish();
        } else {
            List<UsbSerialDriver> availableDrivers = CustomProber.getCustomProber().findAllDrivers(manager);
            if (availableDrivers.isEmpty()) {
                Global.EXTRA_Message = "The device connected to the SmartPhone is unsupported:\n\nDevice 0483:5740 was not found";
                Global.RiseError(this, true);
                return;
            } else {
                // check the permission and explicitly ask for it
                // TODO: Fix the  twice-asking trouble!!!
                driver = availableDrivers.get(0);
                if (!manager.hasPermission(driver.getDevice())) {
                    Log.i("usb permission", "DENIED");
                    manager.requestPermission(
                            driver.getDevice(),
                            PendingIntent.getBroadcast(this, 0, new Intent("com.android.example.USB_PERMISSION"),
                                    FLAG_MUTABLE));
                                    //PendingIntent.FLAG_ONE_SHOT));
                    return;
                } else {
                    Log.i("usb permission", "GRANTED");
                    goOn();
                }
            }
        }
    }



    // quit main activity after device was connected and all of the permissions were granted
    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    private void goOn() {
        // ask for file access!!!
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        Global.port = driver.getPorts().get(0);
        try {
            Global.port.open(connection);
        } catch (Exception exp) {
            exp.printStackTrace();
            Log.i("usb permission", "Port Open Failed!");
            Global.EXTRA_Message = "Port open failed.\n\nException message is:\n\n" + exp.getMessage();
            Global.RiseError(this, true);
        }
        try {
            Global.port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (Exception exp) {
            exp.printStackTrace();
            Global.EXTRA_Message = "Port adjustment failed.\n\nException message is:\n\n" + exp.getMessage();
            Global.RiseError(this, true);
        }
        // initial hand-shake
        Global.CDC_Send("hello\r\n", this);
        String reply = Global.CDC_Get_String(this);
        Global.devinfo = reply.split("\r\n");
        // TODO: make an additional check!!!
        if (Integer.valueOf(Global.devinfo[1]) == 167321907) {
            if (Global.devinfo[2].contains("ERG-T-")) {
                Intent intent = new Intent(this, TActivity.class);
                startActivity(intent);
            } else if (Global.devinfo[2].contains("ERG-TP-")) {
                Intent intent = new Intent(this, TPActivity.class);
                startActivity(intent);
            } else {
                Global.EXTRA_Message = "Device was not recognized :(";
                Global.RiseError(this, true);
            }
        }
    }
    public void askForPermissions() {
        int APP_STORAGE_ACCESS_REQUEST_CODE = 501;
        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                //Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                //startActivityForResult(intent);
                //Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                //startActivityForResult(intent, APP_STORAGE_ACCESS_REQUEST_CODE);
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, APP_STORAGE_ACCESS_REQUEST_CODE);
            }
        }
    }
}

