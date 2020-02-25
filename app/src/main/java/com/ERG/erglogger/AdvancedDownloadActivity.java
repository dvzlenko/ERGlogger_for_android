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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_download);
        // Device name
        String devinfo = Global.devinfo[2].replace(" ","")+"-"+Global.devinfo[3].replace(" ", "");
        TextView devField = findViewById(R.id.devName2);
        Log.i("test", "JOPA1");
        devField.setText(devinfo);
        // message
        TextView messageField = findViewById(R.id.messageView);
        messageField.setText("The Advanced download mode allows " +
                "chossing of the number of the samples to download and " +
                "the number of the first sample that should be \n" +
                "downloaded (numbering starts from the zero).");
        // data amount
        Intent intent = getIntent();
        Integer volume = intent.getIntExtra("volume", 0)/16;
        volumeField = findViewById(R.id.volume_grab);
        volumeField.setText(volume.toString());
        // first point
        firstField = findViewById(R.id.first_grab);
        firstField.setText("0");
    }

    public void onAdwDwnldClick(View view) {
        if (view.getId() == findViewById(R.id.ADWLD_OK_B).getId()) {
            Global.Volume = 16*Integer.parseInt(volumeField.getText().toString());
            Global.Address = 16*Integer.parseInt(firstField.getText().toString());
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
