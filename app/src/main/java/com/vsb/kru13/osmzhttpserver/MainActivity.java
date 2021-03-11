package com.vsb.kru13.osmzhttpserver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private SocketServer s = null;
    private static final int READ_EXTERNAL_STORAGE = 1;
    private static final int WRITE_EXTERNAL_STORAGE = 2;

    private CameraPreview mPreview;
    private Camera mCamera;

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
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE);
        } else {
            // Create an instance of Camera
            mCamera = getCameraInstance();

            // Create our Preview view and set it as the content of our activity.
            mPreview = new CameraPreview(this, mCamera);
            FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
            preview.addView(mPreview);

            Timer timer = new Timer();
            TimerTask tt = new TimerTask(){
                @Override
                public void run(){
                    mCamera.startPreview();
                    mCamera.takePicture(null, null, mPicture);
                }
            };
            timer.schedule(tt, 5000, 2000);
        }
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.buttonStart) {

            int permissionCheck = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE);

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        READ_EXTERNAL_STORAGE);
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

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
        Log.d("MAIN", "Taking pic");
            File pictureFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath().toString()+"/android_web/camera.jpg");
            if (pictureFile == null){
                Log.d("MAIN ACTIVITY", "Error creating media file, check storage permissions");
                return;
            }
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d("MAIN ACTIVITY", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("MAIN ACTIVITY", "Error accessing file: " + e.getMessage());
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case READ_EXTERNAL_STORAGE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    s = new SocketServer(handler);
                    s.start();
                    Button startButt = (Button) findViewById(R.id.buttonStart);
                    startButt.setTextColor(Color.GRAY);
                    Button stopButt = (Button) findViewById(R.id.buttonStop);
                    stopButt.setTextColor(Color.RED);
                }
                break;

            case WRITE_EXTERNAL_STORAGE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Create an instance of Camera
                    mCamera = getCameraInstance();

                    // Create our Preview view and set it as the content of our activity.
                    mPreview = new CameraPreview(this, mCamera);
                    FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
                    preview.addView(mPreview);

                    Timer timer = new Timer();
                    TimerTask tt = new TimerTask(){
                        @Override
                        public void run(){
                            mCamera.takePicture(null, null, mPicture);
                        }
                    };
                    timer.schedule(tt, 5000, 2000);
                }
                break;

            default:
                break;
        }
    }
}
