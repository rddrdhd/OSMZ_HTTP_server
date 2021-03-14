package com.vsb.kru13.osmzhttpserver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
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
    private CameraPreview mPreview;
    private Camera mCamera;
    private TimerTask timerTask;
    private static final int READ_EXTERNAL_STORAGE = 1;
    private static final int WRITE_EXTERNAL_STORAGE = 2;

    Handler handler = new Handler() {
        @SuppressLint("HandlerLeak")
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

        Button btn1 = (Button)findViewById(R.id.buttStartServer);
        Button btn2 = (Button)findViewById(R.id.buttStopServer);
        Button btn3 = (Button)findViewById(R.id.buttStartStream);
        Button btn4 = (Button)findViewById(R.id.buttStopStream);

        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
        btn3.setOnClickListener(this);
        btn4.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.buttStartServer) {

            int permissionCheck = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE);

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        READ_EXTERNAL_STORAGE);
            } else {
                startSocketServer();
            }
        }
        if (v.getId() == R.id.buttStopServer) {
            if(s!=null){
                stopSocketServer();
                try {
                    s.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

        if(v.getId() == R.id.buttStartStream){
            int permissionCheck = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        WRITE_EXTERNAL_STORAGE);
            } else {
                runCameraStream();
            }
        }
        if(v.getId() == R.id.buttStopStream){
            stopCameraStream();
        }
    }

    public void startSocketServer(){s = new SocketServer(handler);
        s.start();

        // UI
        Button startButt = (Button) findViewById(R.id.buttStartServer);
        startButt.setTextColor(Color.GRAY);
        Button stopButt = (Button) findViewById(R.id.buttStopServer);
        stopButt.setTextColor(Color.RED);
    }

    public void stopSocketServer(){
        s.close();

        // UI
        TextView permitsTxt = (TextView)findViewById(R.id.infoPermits);
        permitsTxt.setText("");
        Button stopButt = (Button) findViewById(R.id.buttStopServer);
        stopButt.setTextColor(Color.GRAY);
        Button startButt = (Button) findViewById(R.id.buttStartServer);
        startButt.setTextColor(Color.GREEN);
    }

    public void stopCameraStream(){
        timerTask.cancel();
        mCamera.stopPreview();
        mCamera.release();
        Button stopButt = (Button) findViewById(R.id.buttStopStream);
        stopButt.setTextColor(Color.GRAY);
        Button startButt = (Button) findViewById(R.id.buttStartStream);
        startButt.setTextColor(Color.GREEN);
    }

    public void runCameraStream(){
        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        Timer timer = new Timer();
        timerTask  = new TimerTask(){
            @Override
            public void run(){
                mCamera.startPreview();
                mCamera.takePicture(null, null, mPicture);
            }
        };
        timer.schedule(timerTask, 5000, 2000);

        // UI
        Button startButt = (Button) findViewById(R.id.buttStartStream);
        startButt.setTextColor(Color.GRAY);
        Button stopButt = (Button) findViewById(R.id.buttStopStream);
        stopButt.setTextColor(Color.RED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case READ_EXTERNAL_STORAGE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    startSocketServer();
                }
                break;

            case WRITE_EXTERNAL_STORAGE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    runCameraStream();
                }
                break;

            default:
                break;
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

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d("MAIN", "Picture taken");

            Bitmap tmp = BitmapFactory.decodeByteArray(data, 0, data.length);

            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(tmp, tmp.getWidth(), tmp.getHeight(), true);
            Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);

            SocketServer.newest_img = rotatedBitmap;

            /*File pictureFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath().toString()+"/android_web/camera.jpg");
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
           }*/
        }
    };
}
