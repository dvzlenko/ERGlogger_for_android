package com.ERG.erglogger;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class DeviceTActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_t);

        TextView textView;
        String string;
        // device
        textView = findViewById(R.id.DT_DevNameW);
        string = Global.devinfo[2].replace(" ","")+"-"+Global.devinfo[3].replace(" ", "");
        textView.setText(string);
        textView = findViewById(R.id.DT_info12);
        textView.setText(Global.devinfo[4]);
        textView = findViewById(R.id.DT_info22);
        textView.setText(Global.devinfo[5]);
        textView = findViewById(R.id.DT_info32);
        textView.setText(Global.devinfo[6]);
        textView = findViewById(R.id.DT_info42);
        textView.setText(Global.devinfo[7]);
        // calibrations
        textView = findViewById(R.id.DT_r1);
        textView.setText(String.format(Locale.getDefault(),
                "R\u2081 = %11.4e;", Float.parseFloat(Global.devinfo[11])));
        textView = findViewById(R.id.DT_a1);
        textView.setText(String.format(Locale.getDefault(),
                "A\u2081\u2081 = %11.4e; A\u2081\u2082 = %11.4e; A\u2081\u2083 = %11.4e;",
                Float.parseFloat(Global.devinfo[8]), Float.parseFloat(Global.devinfo[9]),Float.parseFloat(Global.devinfo[10])));

        textView = findViewById(R.id.DT_r2);
        textView.setText(String.format(Locale.getDefault(),
                "R\u2082 = %11.4e;", Float.parseFloat(Global.devinfo[15])));
        textView = findViewById(R.id.DT_a2);
        textView.setText(String.format(Locale.getDefault(),
                "A\u2082\u2081 = %11.4e; A\u2082\u2082 = %11.4e; A\u2082\u2083 = %11.4e;",
                Float.parseFloat(Global.devinfo[12]), Float.parseFloat(Global.devinfo[13]),Float.parseFloat(Global.devinfo[14])));

        textView = findViewById(R.id.DT_r3);
        textView.setText(String.format(Locale.getDefault(),
                "R\u2083 = %11.4e;", Float.parseFloat(Global.devinfo[19])));
        textView = findViewById(R.id.DT_a3);
        textView.setText(String.format(Locale.getDefault(),
                "A\u2083\u2081 = %11.4e; A\u2083\u2082 = %11.4e; A\u2083\u2083 = %11.4e;",
                Float.parseFloat(Global.devinfo[16]), Float.parseFloat(Global.devinfo[17]),Float.parseFloat(Global.devinfo[18])));
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
