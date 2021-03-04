package com.vsb.kru13.osmzhttpserver;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static android.text.TextUtils.isEmpty;

public class ClientThread extends Thread {
    final DataInputStream dis;
    final DataOutputStream dos;
    final Socket s;
    final int client_number;

    public ClientThread(Socket s, DataInputStream dis, DataOutputStream dos, int client_number){
        this.s = s;
        this.dis = dis;
        this.dos = dos;
        this.client_number = client_number;
        Log.d("CLIENT THREAD", "Thread created");
    }

    @Override
    public void run(){
        while (this.isAlive()){
            Log.d("CLIENT THREAD", "Thread started ("+client_number+")");
            try {
                String tmp = "";
                String request_header = "";

                // read request header
                do {
                    tmp = this.dis.readLine();
                    request_header += tmp + "\r\n";
                } while(!"".equalsIgnoreCase(tmp)); // does not die while trying to compare. Instead of "isEmpty"

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
                if(!my_html_file.exists()) {
                    my_html_file = new File(file_sdcard,"android_web/404.html");
                    response_http_code = "404";
                    Log.d("CLIENT THREAD", "File not found, response will be 404");

                } else {
                    response_http_code = "200";
                    Log.d("CLIENT THREAD", "File found, response will be 200");
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
                this.dos.write(response_header.getBytes());

                // send content
                byte[] bytes = new byte[1024];
                BufferedInputStream from_file = new BufferedInputStream(new FileInputStream(my_html_file));

                int size;
                while ((size = from_file.read(bytes)) != -1) {
                    this.dos.write(bytes, 0, size);
                }

                this.dos.flush();

                this.dis.close();
                this.dos.close();
                //s.close();

                Log.d("CLIENT THREAD", "Socket Closed");
            } catch(Exception e){
             e.printStackTrace();
            }
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
