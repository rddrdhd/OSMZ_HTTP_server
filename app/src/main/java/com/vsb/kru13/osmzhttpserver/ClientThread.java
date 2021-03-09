package com.vsb.kru13.osmzhttpserver;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class ClientThread extends Thread {
    final Socket s;
    DataInputStream dis;
    DataOutputStream dos;
    boolean hasToWait;

    public ClientThread(Socket s, boolean hasToWait){
        this.s = s;
        this.hasToWait = hasToWait;

        Log.d("CLIENT THREAD", "Thread created");
    }

    public String getRequestHeader() throws IOException{
        String tmp = "";
        StringBuilder request_header = new StringBuilder();
        do {
            tmp = this.dis.readLine();
            request_header.append(tmp).append("\r\n");
        } while(tmp != null && !tmp.isEmpty());
        request_header.append("\r\n");
        return request_header.toString();
    }

    @Override
    public void run(){
        this.dis = null;
        this.dos = null;
        try {
            this.dis = new DataInputStream(s.getInputStream());
            this.dos = new DataOutputStream(s.getOutputStream());

         String request_header = getRequestHeader();
          // parse info
            String request = request_header.split("HTTP....", 2)[0];

            String path_to_file = request.split(" ")[1];
            String client_method = request.split(" ")[0];

            if(path_to_file.equals("/")) path_to_file = "/index.html";

            String filename = path_to_file.split("/",2)[1];
            Log.d("CLIENT THREAD", "Request method is "+client_method);

            // get files
            File file_sdcard = Environment.getExternalStorageDirectory();
            File my_html_file = new File(file_sdcard,"android_web/"+filename);

            Log.d("CLIENT THREAD", "Looking for file "+my_html_file.getAbsolutePath().toString());

            String response_http_code;
            String response_date;

            // if file not found, response is 404
            if(hasToWait){
                response_http_code = "503";
                Log.d("CLIENT THREAD", "Server too busy, response will be 503");
            } else if(!my_html_file.exists()) {
                my_html_file = new File(file_sdcard,"android_web/404.html");
                response_http_code = "404";
                Log.d("CLIENT THREAD", "File not found, response will be 404");

            } else {
                response_http_code = "200";
                Log.d("CLIENT THREAD", "File found, response will be 200");
            }

            // prepare response header
            response_date = G.getServerTime();
            URLConnection connection = my_html_file.toURL().openConnection();
            String content_type = connection.getContentType();
            String response_header = "HTTP/1.1 "+response_http_code+"\r\n"+
                    "Date: "+ response_date +"\r\n" +
                    "Server: Apache\r\n" +
                    "Last-Modified: "+ response_date +"\r\n" +
                    "Content-Length: "+ my_html_file.length() +"\r\n" +
                    "Content-Type: "+ content_type +"\r\n\r\n";

            // send header
            dos.write(response_header.getBytes());

            // send content
            if(hasToWait){
                dos.write("<h1>503</h1>".getBytes());
            } else {
                byte[] bytes = new byte[1024];
                BufferedInputStream from_file = new BufferedInputStream(new FileInputStream(my_html_file));

                int size;
                while ((size = from_file.read(bytes)) != -1) {
                    try {
                        dos.write(bytes, 0, size);
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }

            dos.flush();
        } catch(Exception e){
           // e.printStackTrace();
        } finally {
           // ++G.permits;
            SocketServer.semaphore.release();
            Log.d("CLIENT THREAD","------------------------ Killing thread. Remaining threads: "+ SocketServer.semaphore.availablePermits() );
            try {
                if(dos != null) {
                    dos.close();
                }
                if(dis != null) {
                    dis.close();
                    s.close();
                    Log.d("CLIENT THREAD", "Socket Closed");
                }
            } catch (IOException e) {
                Log.e("CLIENT THREAD","Couldn't close DIS or DOS");
                e.printStackTrace();
            }
        }
    }

    public void cancel(){
        this.interrupt();
        SocketServer.clientStopped(this);
    }


}
