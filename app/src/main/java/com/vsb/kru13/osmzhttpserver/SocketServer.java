package com.vsb.kru13.osmzhttpserver;

import android.util.Log;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

class G {
    public static int permits = 5;

    public static String getServerTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(calendar.getTime());
    }
}

public class SocketServer extends Thread  {
    ServerSocket serverSocket = null;
    public final int port = 12345;
    boolean bRunning;
    public static Semaphore semaphore = new Semaphore(5);
    protected static Set<ClientThread> threads = Collections.newSetFromMap(new ConcurrentHashMap<ClientThread, Boolean>());

    boolean hasToWait = false;

    public static void clientStopped(ClientThread thread){
        threads.remove(thread);
        semaphore.release();
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

            while (bRunning) {
                Socket s = null;
                try {
                    hasToWait= !semaphore.tryAcquire();
                    Log.d("SERVER", "Socket waiting for connection");
                    s = serverSocket.accept(); // cekam na prichozi pripojeni, .accept() je blokuje vlakno
                    s.setKeepAlive(true);
                    Log.d("SERVER","Accepting connection from "+s.getRemoteSocketAddress());
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
                    semaphore.release();
                    continue;
                }

                ClientThread t = new ClientThread(s, hasToWait);
                threads.add(t);
                t.start();
                Log.d("SERVER","+++++++++++++++++++++++++ Starting thread. Remaining threads: "+ semaphore.availablePermits() );


            } // endwhile
            for(ClientThread thread: threads){
                thread.cancel();
            }
            Log.d("SERVER", "STOPPED ALL");
        /*} catch (IOException e) {
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
            }*/
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            serverSocket = null;
            bRunning = false;
            semaphore.release();
        }
    }

}

