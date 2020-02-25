package com.ERG.erglogger;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // detach intent
        /*final Activity obj = this;
        BroadcastReceiver detachReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED))
                    Global.EXTRA_Message = "ERG logger was detached!\nSorry, the application must be closed!!!";
                    Global.RiseError(obj, true);
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(detachReceiver, filter);*/

        // find the driver for the device 0483:5740 or 1155:22336 described in CustomProber class
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (manager.getDeviceList().isEmpty()) {
            /*Global.devinfo = (
                    "hello\r\n" +
                    "167321907\r\n" +
                    "ERG-T-07\r\n" +
                    "6731\r\n" +
                    "STM32F103CBT6 (72 MHz, 20 kB)\r\n" +
                    "AD7799 (24 bit, 3 channel)\r\n" +
                    "MX25L12835VF (16 MB, FLASH-NOR)\r\n" +
                    "700-102BAA-B00 (0.06%, class A)\r\n" +
                    "                  0.003017108615\r\n" +
                    "                 -0.000662285424\r\n" +
                    "                  0.000640361011\r\n" +
                    "                999.877900207255\r\n" +
                    "                  0.002442093427\r\n" +
                    "                 -0.000881547672\r\n" +
                    "                 -0.000398092095\r\n" +
                    "               1000.529654264777\r\n" +
                    "                  0.051532188710\r\n" +
                    "                  0.597681144814\r\n" +
                    "                 -0.287017806897\r\n" +
                    "                999.612500351621\r\n").split("\r\n");
            Intent intent = new Intent(this, TActivity.class);
            startActivity(intent);*/
            Global.EXTRA_Message = "Cannot find any USB device";
            Global.RiseError(this, true);
            finish();
        } else {
            List<UsbSerialDriver> availableDrivers = CustomProber.getCustomProber().findAllDrivers(manager);
            if (availableDrivers.isEmpty()) {
                Global.EXTRA_Message = "The device connected to the smartphone is unsupported:\n\nDevice 0483:5740 was not found";
                Global.RiseError(this, true);
                finish();
            }
            else {
                // open connection
                UsbSerialDriver driver = availableDrivers.get(0);
                UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
                if (connection == null) {
                    //manager.requestPermission(driver.getDevice(), this);
                    Global.EXTRA_Message = "Failed to connect the logger\n\nProbably, you need to add the appropriate permission?";
                    Global.RiseError(this, true);
                    finish();
                }
                Global.port = driver.getPorts().get(0); // Most devices have just one port (port 0)
                try {
                    Global.port.open(connection);
                } catch (Exception exp) {
                    Global.EXTRA_Message = "Port open failed.\n\nException message is:\n\n" + exp.getMessage();
                    Global.RiseError(this, true);
                    finish();
                }
                try {
                    Global.port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                } catch (Exception exp) {
                    Global.EXTRA_Message = "Port adjustment failed.\n\nException messagew is:\n\n" + exp.getMessage();
                    Global.RiseError(this, true);
                    finish();
                }
                // initial hand-shake
                Global.CDC_Send("hello\r\n", this);
                String reply = Global.CDC_Get_String(this);
                Global.devinfo = reply.split("\r\n");
            }
        }
    }

    // start T/PT Activity
    @Override
    protected void onStart() {
        super.onStart();
        startActivity();
    }

    // quit the main activity
    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    private void startActivity() {
        if (Integer.valueOf(Global.devinfo[1]) == 167321907) {
            if (Global.devinfo[2].contains("ERG-T-")) {
                Intent intent = new Intent(this, TActivity.class);
                startActivity(intent);
            } else if (Global.devinfo[2].contains("ERG-TP-")) {
                Intent intent = new Intent(this, TPActivity.class);
                startActivity(intent);
            }
        }
    }
}

