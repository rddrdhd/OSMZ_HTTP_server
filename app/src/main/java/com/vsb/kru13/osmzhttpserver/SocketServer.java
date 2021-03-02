package com.vsb.kru13.osmzhttpserver;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static android.text.TextUtils.isEmpty;

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
            Log.d("SERVER", "Creating Socket");
            serverSocket = new ServerSocket(port); // port, na kterem aplikace nasloucha
            bRunning = true;

            while (bRunning) {
                Log.d("SERVER", "Socket Waiting for connection");
                Socket s = serverSocket.accept(); // cekam na prichozi pripojeni, .accept() je blokuje vlakno
                Log.d("SERVER", "Socket Accepted");

                OutputStream o = s.getOutputStream();
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(o)); // vystup klientovi
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream())); // vstup ze socketu

                // cist celou hlavicku http requestu - konci prazdnym radkem
                // zpatky jednoduchou html stranku

                String tmp = "";
                String request_header = "";

                do {
                    tmp = in.readLine();
                    request_header += tmp + "\r\n";
                } while(!isEmpty(tmp));

                String client_get = request_header.split("HTTP....", 2)[0];
                String path_to_file = client_get.split(" ")[1];
                if(path_to_file.equals("/")) path_to_file = "/index.html";
                String filename = path_to_file.split("/",2)[1];


                File sdcard = Environment.getExternalStorageDirectory();
                File file = new File(sdcard,"android_web/"+filename);

                String response_http_code;
                String response_date;
                String response_date_modified;
                String content_length;
                String content_type;
                Log.d("aaaaa",file.getPath());
                    if(!file.exists()) {
                        file = new File(sdcard,"android_web/404.html");
                        response_http_code = "404";
                    } else {
                        response_http_code = "200";
                    }


                Log.d("aaaaa",file.getPath());

                response_date = getServerTime();

                String response_header = "HTTP/1.1 "+response_http_code+"\r\n"+
                        "Date: "+response_date+"\r\n" +
                        "Server: Apache\r\n" +
                        "Last-Modified: "+response_date+"\r\n" +
                        "ETag: \"51142bc1-7449-479b075b2891b\"\r\n" +
                        "Accept-Ranges: bytes\r\n" +
                        "Content-Length: 29769\r\n" +
                        "Content-Type: text/html\r\n\r\n";

                String response, response_body = "";
                response_body = "<html><body><h1>yes</h1><p>client:</p><pre>"+request_header+"</pre><p>server:</p><pre>"+response_header+"</pre></body></html>";

                response = response_header + response_body;

                out.write(response);

                out.flush(); // odeslat odpoved

                s.close(); // zavrit spojeni
                Log.d("SERVER", "Socket Closed");
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

    private String getServerTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(calendar.getTime());
    }
}

