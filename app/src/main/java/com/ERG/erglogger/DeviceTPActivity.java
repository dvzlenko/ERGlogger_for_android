package com.ERG.erglogger;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class DeviceTPActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_tp);

        TextView textView;
        String string;
        // device
        textView = findViewById(R.id.DTP_DevNameW);
        string = Global.devinfo[2].replace(" ","")+"-"+Global.devinfo[3].replace(" ", "");
        textView.setText(string);
        textView = findViewById(R.id.DTP_info12);
        textView.setText(Global.devinfo[4]);
        textView = findViewById(R.id.DTP_info22);
        textView.setText(Global.devinfo[5]);
        textView = findViewById(R.id.DTP_info32);
        textView.setText(Global.devinfo[6]);
        textView = findViewById(R.id.DTP_info42);
        textView.setText(Global.devinfo[7]);
        textView = findViewById(R.id.DTP_info52);
        textView.setText(Global.devinfo[8]);
        // calibrations
        textView = findViewById(R.id.DTP_r1);
        textView.setText(String.format(Locale.getDefault(),
                "R\u2081 = %11.4e;", Float.parseFloat(Global.devinfo[12])));
        textView = findViewById(R.id.DTP_a1);
        textView.setText(String.format(Locale.getDefault(),
                "A\u2081\u2081 = %11.4e; A\u2081\u2082 = %11.4e; A\u2081\u2083 = %11.4e;",
                Float.parseFloat(Global.devinfo[9]), Float.parseFloat(Global.devinfo[10]),Float.parseFloat(Global.devinfo[11])));

        textView = findViewById(R.id.DTP_r2);
        textView.setText(String.format(Locale.getDefault(),
                "R\u2082 = %11.4e;", Float.parseFloat(Global.devinfo[16])));
        textView = findViewById(R.id.DTP_a2);
        textView.setText(String.format(Locale.getDefault(),
                "A\u2082\u2081 = %11.4e; A\u2082\u2082 = %11.4e; A\u2082\u2083 = %11.4e;",
                Float.parseFloat(Global.devinfo[13]), Float.parseFloat(Global.devinfo[14]),Float.parseFloat(Global.devinfo[15])));

        textView = findViewById(R.id.DTP_r3);
        textView.setText(String.format(Locale.getDefault(),
                "R\u2083 = %11.4e;", Float.parseFloat(Global.devinfo[20])));
        textView = findViewById(R.id.DTP_a3);
        textView.setText(String.format(Locale.getDefault(),
                "A\u2083\u2081 = %11.4e; A\u2083\u2082 = %11.4e; A\u2083\u2083 = %11.4e;",
                Float.parseFloat(Global.devinfo[17]), Float.parseFloat(Global.devinfo[18]),Float.parseFloat(Global.devinfo[19])));

        textView = findViewById(R.id.DTP_PC1);
        textView.setText(String.format(Locale.getDefault(),
                "V2Ts = %11.4e;", Float.parseFloat(Global.devinfo[21])));
        textView = findViewById(R.id.DTP_PC2);
        textView.setText(String.format(Locale.getDefault(),
                "V2Ps = %11.4e;\nV2Pc = %11.4e;",
                Float.parseFloat(Global.devinfo[22]), Float.parseFloat(Global.devinfo[23])));
    }
    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    public void onButtonClick(View view) {
        finish();
    }
}
