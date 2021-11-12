package com.lotogram.conference.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;

import com.lotogram.conference.R;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, ViewSwitcher.ViewFactory {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String[] PUSH = new String[]{
            "rtmp://push.zhuagewawa.com/record/w253?wsSecret=43a9faee5de385b387a76a94da00bc57&wsABSTime=1651133406",
            "rtmp://push.zhuagewawa.com/record/w055?wsSecret=dab42f5f514f5916fe2bb323dbaa1c57&wsABSTime=1663230897"
    };

    private static final String[] PULL = new String[]{
            "http://pull.zhuagewawa.com/record/w055.flv",
            "http://pull.zhuagewawa.com/record/w253.flv",
    };

    private static final String[] PERMISSION = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final String[] USER = new String[]{
            "用户一",
            "用户二"
    };

    private static final String[] ORIENTATION = new String[]{
            "前置",
            "后置"
    };

    private AppCompatSpinner mSpinner;
    private AppCompatSpinner mOrientation;
    private TextSwitcher mPullAddress;
    private TextSwitcher mPushAddress;

    private int userIndex = 0;
    private int orientationIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSpinner = findViewById(R.id.spinner);
        mOrientation = findViewById(R.id.orientation);
        mPullAddress = findViewById(R.id.pull);
        mPushAddress = findViewById(R.id.push);

        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(this, R.layout.item_user, USER);
        mSpinner.setAdapter(adapter1);
        mSpinner.setOnItemSelectedListener(this);

        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(this, R.layout.item_user, ORIENTATION);
        mOrientation.setAdapter(adapter2);
        mOrientation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                orientationIndex = position;
                Toast.makeText(MainActivity.this, ORIENTATION[orientationIndex], Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mPushAddress.setFactory(this);
        mPullAddress.setFactory(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Toast.makeText(this, USER[position], Toast.LENGTH_SHORT).show();
        userIndex = position;
        mPullAddress.setText(PULL[position]);
        mPushAddress.setText(PUSH[position]);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Log.d(TAG, "未选择任何选项");
        Toast.makeText(this, "未选择任何选项", Toast.LENGTH_SHORT).show();
    }

    @Override
    public View makeView() {
        TextView textView = new TextView(this);
        textView.setGravity(Gravity.START | Gravity.TOP);
        return textView;
    }

    private void toConference() {
        Intent intent = new Intent();
        intent.setClass(this, ConferenceActivity.class);
        intent.putExtra("pull", PULL[userIndex]);
        intent.putExtra("push", PUSH[userIndex]);
        intent.putExtra("user", USER[userIndex]);
        intent.putExtra("orientation", ORIENTATION[orientationIndex]);
        startActivity(intent);
    }

    private boolean hasPermission() {
        for (String permission : PERMISSION) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void onStart(View view) {
        if (hasPermission()) {
            toConference();
        } else {
            requestPermissions(PERMISSION, 1000);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED ||
                    grantResults[1] == PackageManager.PERMISSION_GRANTED ||
                    grantResults[2] == PackageManager.PERMISSION_GRANTED ||
                    grantResults[3] == PackageManager.PERMISSION_GRANTED) {
                toConference();
            }
        }
    }
}