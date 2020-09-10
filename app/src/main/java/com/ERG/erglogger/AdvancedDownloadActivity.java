package com.ERG.erglogger;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class AdvancedDownloadActivity extends AppCompatActivity {

    TextView volumeField;
    TextView firstField;
    Integer volume = 0;
    String mode = "Temperature";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_download);
        // Device name
        String devinfo = Global.devinfo[2].replace(" ","")+"-"+Global.devinfo[3].replace(" ", "");
        TextView devField = findViewById(R.id.devName2);
        devField.setText(devinfo);
        // message
        TextView messageField = findViewById(R.id.messageView);
        messageField.setText("The Advanced download mode allows " +
                "choosing of the amount of the samples to download and " +
                "the number of the first sample that should be \n" +
                "downloaded (numbering starts from the zero).");
        // data amount
        Intent intent = getIntent();
        mode = intent.getStringExtra("mode");
        Log.i("mode:", mode);
        if (mode.equals("Temperature"))
            volume = intent.getIntExtra("volume", 0)/16;
        else if (mode.equals("Pressure"))
            volume = intent.getIntExtra("volume", 0)/8;
        volumeField = findViewById(R.id.volume_grab);
        volumeField.setText(volume.toString());
        // first point
        firstField = findViewById(R.id.first_grab);
        firstField.setText("0");
    }

    public void onAdwDwnldClick(View view) {
        if (view.getId() == findViewById(R.id.ADWLD_OK_B).getId()) {
            if (mode.equals("Temperature")) {
                Global.VolumeT = 16 * Integer.parseInt(volumeField.getText().toString());
                Global.AddressT1 = 16 * Integer.parseInt(firstField.getText().toString());
            } else if (mode.equals("Pressure")) {
                Global.VolumeP = 8 * Integer.parseInt(volumeField.getText().toString());
                Global.AddressP1 = Global.AddressP1 + 8 * Integer.parseInt(firstField.getText().toString());
            }
            Global.AdvancedDownloadFlag = true;
            Global.BasicDownloadFlag = false;
        }
        else {
            Global.AdvancedDownloadFlag = false;
            Global.BasicDownloadFlag = true;
        }
        finish();
    }
}
