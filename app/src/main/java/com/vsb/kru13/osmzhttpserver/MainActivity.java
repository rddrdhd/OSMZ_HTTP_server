package com.vsb.kru13.osmzhttpserver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private SocketServer s = null;
    private static final int READ_EXTERNAL_STORAGE = 1;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg){
            TextView infoTxt = (TextView)findViewById(R.id.infoText);
            TextView permitsTxt = (TextView)findViewById(R.id.infoPermits);
            String newInfoTxt = msg.getData().getString("info");
            if(newInfoTxt!=null&&!newInfoTxt.isEmpty())infoTxt.append(newInfoTxt+"\n");
            permitsTxt.setText("Permits remaining: "+msg.getData().getInt("permits"));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn1 = (Button)findViewById(R.id.buttonStart);
        Button btn2 = (Button)findViewById(R.id.buttonStop);

        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.buttonStart) {

            int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE);
            } else {
                s = new SocketServer(handler);
                s.start();
                Button startButt = (Button) v;
                startButt.setTextColor(Color.GRAY);
                Button stopButt = (Button) findViewById(R.id.buttonStop);
                stopButt.setTextColor(Color.RED);
            }
        }
        if (v.getId() == R.id.buttonStop) {
            if(s!=null){
                s.close();
                TextView permitsTxt = (TextView)findViewById(R.id.infoPermits);
                permitsTxt.setText("");

                Button stopButt = (Button) v;
                stopButt.setTextColor(Color.GRAY);
                Button startButt = (Button) findViewById(R.id.buttonStart);
                startButt.setTextColor(Color.GREEN);
                try {
                    s.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {

            case READ_EXTERNAL_STORAGE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    s = new SocketServer(handler);
                    s.start();
                }
                break;

            default:
                break;
        }
    }
}
