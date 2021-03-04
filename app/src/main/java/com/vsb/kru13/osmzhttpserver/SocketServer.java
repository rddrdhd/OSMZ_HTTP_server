package com.vsb.kru13.osmzhttpserver;

import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
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

    @RequiresApi(api = Build.VERSION_CODES.O)
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
                //BufferedWriter out = new BufferedWriter(new OutputStreamWriter(o)); // vystup klientovi
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream())); // vstup ze socketu

                String tmp = "";
                String request_header = "";

                // read request header
                do {
                    tmp = in.readLine();
                    request_header += tmp + "\r\n";
                } while(!isEmpty(tmp));

                // parse info
                String request = request_header.split("HTTP....", 2)[0];
                String path_to_file = request.split(" ")[1];
                String client_method = request.split(" ")[0];

                if(path_to_file.equals("/")) path_to_file = "/index.html";

                String filename = path_to_file.split("/",2)[1];
                Log.d("SERVER", "Request method is "+client_method);

                // get files
                File file_sdcard = Environment.getExternalStorageDirectory();
                File my_html_file = new File(file_sdcard,"android_web/"+filename);

                Log.d("SERVER", "Looking for file "+my_html_file.getAbsolutePath().toString());

                String response_http_code;
                String response_date;

                // if file not found, response is 404
                if(!my_html_file.exists()) {
                    my_html_file = new File(file_sdcard,"android_web/404.html");
                    response_http_code = "404";
                    Log.d("SERVER", "File not found, response will be 404");

                } else {
                    response_http_code = "200";
                    Log.d("SERVER", "File found, response will be 200");
                }

                // prepare response header
                response_date = getServerTime();
                URLConnection connection = my_html_file.toURL().openConnection();
                String content_type = connection.getContentType();
                String response_header = "HTTP/1.1 "+response_http_code+"\r\n"+
                        "Date: "+ response_date +"\r\n" +
                        "Server: Apache\r\n" +
                        "Last-Modified: "+ response_date +"\r\n" +
                        "Content-Length: "+ my_html_file.length() +"\r\n" +
                        "Content-Type: "+ content_type +"\r\n\r\n";

                // send header
                o.write(response_header.getBytes());

                // send content
                byte[] bytes = new byte[1024];
                BufferedInputStream from_file = new BufferedInputStream(new FileInputStream(my_html_file));

                int size;
                while ((size = from_file.read(bytes)) != -1) {
                    o.write(bytes, 0, size);
                }

                o.flush();

                // close socket
                s.close();
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

