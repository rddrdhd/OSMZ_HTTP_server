package com.vsb.kru13.osmzhttpserver;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer extends Thread {

    ServerSocket serverSocket = null;
    public final int port = 12345;
    boolean bRunning;
    final int permits_count = 5;
    public ClientThread runningThread = null;

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
        ServerSocket serverSocket = null;
        try {
            int remaining_threads = 5;
            Log.d("SERVER", "Creating Socket");
            serverSocket = new ServerSocket(port); // port, na kterem server nasloucha
            serverSocket.setReuseAddress(true);
            bRunning = true;

            while (bRunning) {
                Log.d("SERVER", "Accepting Socket");
                Socket s = serverSocket.accept(); // cekam na prichozi pripojeni, .accept() je blokuje vlakno
                Log.d("SERVER","New client connected: "+s.getInetAddress().getHostAddress());

               // Log.d("SERVER", "Socket Waiting for  connection");
                ClientThread t = new ClientThread(s, remaining_threads);
                Log.d("SERVER","Starting thread. Remaining threads: "+ remaining_threads );
                //remaining_threads--;
                new Thread(t).start();

                // t.run();
                //remaining_threads++;
                Log.d("SERVER","Killing thread. Remaining threads: "+ remaining_threads );
                Log.d("SERVER",".");

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
        }
    }

}

