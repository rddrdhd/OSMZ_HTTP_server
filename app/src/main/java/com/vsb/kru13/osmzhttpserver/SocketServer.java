package com.vsb.kru13.osmzhttpserver;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer extends Thread {

    ServerSocket serverSocket;
    public final int port = 12345;
    boolean bRunning;

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
            int counter = 0;
            Log.d("SERVER", "Creating Socket");
            serverSocket = new ServerSocket(port); // port, na kterem aplikace nasloucha
            bRunning = true;

            while (!serverSocket.isClosed()) {
                counter++;
                Log.d("SERVER", "Socket Waiting for "+counter+". connection");
                Socket s = serverSocket.accept(); // cekam na prichozi pripojeni, .accept() je blokuje vlakno
                Log.d("SERVER", "Socket Accepted");

                DataOutputStream o = new DataOutputStream(s.getOutputStream());
                DataInputStream in = new DataInputStream(s.getInputStream()); // vstup ze socketu

                Log.d("SERVER","Assigning new thread ("+counter+")");

                Thread t = new ClientThread(s, in, o, counter);
                 t.start();
            } // endwhile
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
                    Log.e("SERVER", "Error, serverSocked closed.");
                } catch (IOException f){
                    Log.e("SERVER", "Error, serverSocked not closed.");
                    f.printStackTrace();
                }
            } else {
                Log.e("SERVER", "Error");
                e.printStackTrace();
            }
        } finally {
            serverSocket = null;
            bRunning = false;
        }
    }

}

