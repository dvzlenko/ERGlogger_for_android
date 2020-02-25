package com.ERG.erglogger;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialProber;

public class CustomProber {
    static UsbSerialProber getCustomProber() {
        ProbeTable customTable = new ProbeTable();
        customTable.addProduct(0x0483, 0x5740, CdcAcmSerialDriver.class); // STM32 CDC device
        //customTable.addProduct(0x16d0, 0x087e, CdcAcmSerialDriver.class); // e.g. Digispark CDC
        return new UsbSerialProber(customTable);
    }
}
