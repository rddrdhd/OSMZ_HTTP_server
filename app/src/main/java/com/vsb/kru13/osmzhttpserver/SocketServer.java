package com.vsb.kru13.osmzhttpserver;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Semaphore;

public class SocketServer extends Thread  {
    boolean bRunning = false;
    ServerSocket serverSocket = null;
    public final int port = 12345;
    public static Semaphore semaphore = new Semaphore(5);
    public Handler handler;
    SocketServer(Handler handler){
        this.handler = handler;
    }
    public void close() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            Log.d("SERVER", "Error, probably interrupted in accept(), see log");
            e.printStackTrace();
        }
        bRunning = false;
    }

    public void run() {
        try {
            Log.d("SERVER", "Creating Socket");
            serverSocket = new ServerSocket(port); // port, na kterem server nasloucha
            serverSocket.setReuseAddress(true);
            bRunning = true;

            Bundle b = new Bundle();
            b.putInt("permits",SocketServer.semaphore.availablePermits());
            b.putString("info", SocketServer.getShortServerTime() +":Server is running \r\n");
            Message msg = handler.obtainMessage();
            msg.setData(b);
            msg.sendToTarget();


            while (bRunning) {
                Socket s = null;
                try {
                    Log.d("SERVER", "Socket waiting for connection");
                    s = serverSocket.accept(); // cekam na prichozi pripojeni, .accept() je blokuje vlakno
                    s.setKeepAlive(true);

                    Log.d("SERVER","Accepting connection from "+s.getRemoteSocketAddress());

                   /* b.putString("info", "Accepting connection from "+s.getRemoteSocketAddress()+"\r\n");
                    Message msg = handler.obtainMessage();
                    msg.setData(b);
                    msg.sendToTarget();*/



                } catch (IOException e) {
                    if (!isInterrupted()) {
                        Log.d("SERVER", getName() + ": " + e.getMessage());
                    }
                    if (serverSocket != null && serverSocket.isClosed()){
                        Log.d("SERVER", "Normal exit");
                    } else {
                        Log.e("SERVER", "Error");
                        e.printStackTrace();
                    }
                    if (s != null) {
                        try {
                            s.close();
                        } catch (IOException ignored) { }
                    }
                    continue;
                } 
                ClientThread t = new ClientThread(s, handler);
                t.start();
                Log.d("SERVER","+++ Starting thread. Remaining threads: "+ semaphore.availablePermits() );
            }
        } catch (IOException e) {
            if (serverSocket != null && serverSocket.isClosed()){
                Log.d("SERVER", "Normal exit");
            } else {
                Log.e("SERVER", "Error");
                e.printStackTrace();
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            if (serverSocket != null && !serverSocket.isClosed()){
                try {
                    serverSocket.close();
                    Log.e("SERVER", "Error, serverSocket closed.");
                } catch (IOException f){
                    Log.e("SERVER", "Error, serverSocket not closed.");
                    f.printStackTrace();
                }
            } else {
                Log.e("SERVER", "Error");
                e.printStackTrace();
            }
        } finally {
            serverSocket = null;
            bRunning = false;
            //semaphore.release();
        }
    }

    public static String getServerTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+1"));
        return dateFormat.format(calendar.getTime());
    }
    public static String getShortServerTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "HH:mm:ss", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+1"));
        return dateFormat.format(calendar.getTime());
    }

}

