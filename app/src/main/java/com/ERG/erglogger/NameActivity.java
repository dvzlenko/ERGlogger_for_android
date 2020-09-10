package com.ERG.erglogger;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.Locale;

public class NameActivity extends AppCompatActivity {
    TextView textView;
    TextView fileName;
    String flag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_name);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        flag = intent.getStringExtra(Global.EXTRA_Flag);
        textView = findViewById(R.id.FileName);
        // default file name
        Calendar cl = Calendar.getInstance();
        String tmp = "";
        if (flag.equals("T")) {
            tmp = "TEMPERATURE-";
            textView.setText("TEMPERATURE\nfile name");
        }
        else if (flag.equals("P")) {
            tmp = "PRESSURE-";
            textView.setText("PRESSURE\nfile name");
        }
        String suggestedName = tmp +
                    Global.devinfo[2].replace(" ", "") + "-" +
                    Global.devinfo[3].replace(" ", "") +
                    String.format(Locale.getDefault(),"-%4d.%02d.%02d-%02d:%02d.txt",
                            cl.get(Calendar.YEAR), cl.get(Calendar.MONTH) + 1, cl.get(Calendar.DAY_OF_MONTH), cl.get(Calendar.HOUR_OF_DAY), cl.get(Calendar.MINUTE));


        // Capture the layout's TextView and set the string as its text
        fileName = findViewById(R.id.fNameField);
        fileName.getText();
        fileName.setText(suggestedName);
        fileName.setTextColor(Color.BLACK);
        textView = findViewById(R.id.namemessage);
        textView.setText("The data files are stored at\n" +
                "$DEFAULT_EXTRNAL_STORAGE$/ERG/\n\n" +
                "In case if you don't have SD card, or " +
                "mounted Read-Only, the files will be stored at\n" +
                "$DEFAULT_INTERNAL_STORAGE$/ERG/");
    }

    public void nameButtonClick(View button) {
        if (button.getId() == R.id.name_OK_B) {
            if (flag.equals("T")) {
                Global.EXTRA_TName = fileName.getText().toString();
                Global.SaveTFileFlag = true;
            }
            else if (flag.equals("P")) {
                Global.EXTRA_PName = fileName.getText().toString();
                Global.SavePFileFlag = true;
            }
        }
        else if (button.getId() == R.id.name_CANCEL_B) {
            Global.SaveTFileFlag = false;
            Global.SavePFileFlag = false;
        }
        finish();
    }
}