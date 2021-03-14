package com.vsb.kru13.osmzhttpserver;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientThread extends Thread {
    private final Socket s;
    private DataInputStream dis;
    private DataOutputStream dos;
    private final Handler handler;
    private final boolean is_waiting;
    public boolean is_stream;
    private static final int BUFFER_SIZE = 1024;

    public ClientThread(Socket s, Handler handler){
        this.s = s;
        this.handler = handler;
        this.is_waiting = !SocketServer.semaphore.tryAcquire();

        // TextView update
        updateTexts("Thread created"+((this.is_waiting ?" and will wait.":".")));

        Log.d("CLIENT THREAD" +s.getRemoteSocketAddress(),
                "Thread created"+ ((this.is_waiting)?", but will wait":""));
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
            Log.d("CLIENT THREAD" +s.getRemoteSocketAddress(), "??? "+request);

            // TextView update - User-Agent info
            String ua_value = request_header.split("User-Agent: ")[1];
            ua_value = ua_value.split("\r\n")[0];
            updateTexts(ua_value);

            String path_to_file = request.split(" ")[1];

            if(path_to_file != null){
                if(path_to_file.equals("/")) path_to_file = "/index.html";
                if(path_to_file.equals("/camera/stream")) path_to_file = "/camera.jpg";
            }
            String filename = path_to_file.split("/",2)[1];

            this.is_stream = (filename.equals("camera.jpg"));

            // get files
            File file_sdcard = Environment.getExternalStorageDirectory();
            File my_html_file = new File(file_sdcard,"android_web/"+filename);

            String response_http_code, response_date, response_header;
            String page503 = "<h1>503 Service Temporarily Unavailable</h1>";

            if(this.is_waiting){
                response_http_code = "503";
                Log.d("CLIENT THREAD" +s.getRemoteSocketAddress(),
                        "!!! Server too busy, response will be 503");
            } else if(!my_html_file.exists()) {
                my_html_file = new File(file_sdcard,"android_web/404.html");
                response_http_code = "404";
                Log.d("CLIENT THREAD" +s.getRemoteSocketAddress(),
                        "!!! File not found, response will be 404");
            } else {
                response_http_code = "200";
                Log.d("CLIENT THREAD" +s.getRemoteSocketAddress(),
                        "!!! File found, response will be 200");
            }

            // TextView update
            updateTexts(request+" => "+response_http_code);

            response_date = SocketServer.getServerTime();
            String content_type = getMimeType(my_html_file.getPath());
            int content_length;

            // send content
            if(this.is_waiting) {
                content_length = page503.getBytes().length ;
                response_header = "HTTP/1.1 "+response_http_code+"\r\n"+
                        "Date: "+ response_date +"\r\n" +
                        "Server: Apache\r\n" +
                        "Last-Modified: "+ response_date +"\r\n" +
                        "Content-Length: "+ content_length +"\r\n"+
                        "Content-Type: "+ content_type +"\r\n" +
                        "\r\n";
                // send header
                this.dos.write(response_header.getBytes());
                // send body
                this.dos.write(page503.getBytes());

            } else if(this.is_stream){
                String boundary = "OSMZ_boundary";
                content_type = "multipart/x-mixed-replace; boundary=\""+boundary+"\"";
                updateTexts("Creating streaming header");

                response_header = "HTTP/1.1 "+response_http_code+"\r\n"+
                        "Date: "+ response_date +"\r\n" +
                        "Server: Apache\r\n" +
                        "Last-Modified: "+ response_date +"\r\n" +
                        "Cache-Control: no-store, no-cache, must-revalidate, max-age=0\r\n" +
                        "Pragma: no-cache\r\n" +
                        "Content-Type: "+ content_type +"\r\n" +
                        "\r\n";

                // send header
                this.dos.write(response_header.getBytes());

                while(true){
                    this.dos.write(("--"+boundary+"\r\n").getBytes());

                    content_length =  (int)my_html_file.length();
                    this.dos.write(("Content-type: image/jpeg\r\n").getBytes());
                    this.dos.write(("Content-length: "+content_length+"\r\n\r\n").getBytes());

                    updateTexts("Start of sending data");

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    SocketServer.newest_img.compress(Bitmap.CompressFormat.PNG, 50, stream);
                    byte[] byteArray = stream.toByteArray();
                    this.dos.write(byteArray);
                /*while ((size = SocketServer.newest_img.read(bytes)) != -1) {
                    try {
                        this.dos.write(bytes, 0, size);
                    } catch(Exception ignored){
                        //
                    }
                }*/
                    this.dos.write(("\r\n--"+boundary+"\r\n").getBytes());
                    this.dos.flush();

                    updateTexts("Img sent");
                }
                // start with first img


            } else { // get html page from storage
                content_length =  (int)my_html_file.length();

                response_header = "HTTP/1.1 "+response_http_code+"\r\n"+
                        "Date: "+ response_date +"\r\n" +
                        "Server: Apache\r\n" +
                        "Last-Modified: "+ response_date +"\r\n"+
                        "Content-Length: "+ content_length +"\r\n" +
                        "Content-Type: "+ content_type +"\r\n" +
                        "\r\n";

                // send header
                this.dos.write(response_header.getBytes());

                // send body
                byte[] bytes = new byte[BUFFER_SIZE];
                FileInputStream fis = new FileInputStream(my_html_file);
                BufferedInputStream from_file = new BufferedInputStream(fis);
                int size;

                while ((size = from_file.read(bytes)) != -1) {
                    try {
                        dos.write(bytes, 0, size);
                    } catch(Exception ignored){
                        //
                    }
                }
                from_file.close();
                fis.close();
            }
            dos.flush();
            updateTexts("End of sending data");
        } catch (ArrayIndexOutOfBoundsException ignored){
            //
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            if(!is_waiting) SocketServer.semaphore.release();
            String type = " normal";
            if(is_stream) type=" stream";
            updateTexts("Killing thread"+type);
            Log.d("CLIENT THREAD" +s.getRemoteSocketAddress(),
                    "--- Killing thread. Remaining threads: "
                            + SocketServer.semaphore.availablePermits() );
            try {
                if(dos != null) {
                    dos.close();
                }
                if(dis != null) {
                    dis.close();
                    s.close();
                    Log.d("CLIENT THREAD" +s.getRemoteSocketAddress(), "Socket Closed");
                }
            } catch (IOException e) {
                Log.e("CLIENT THREAD" +s.getRemoteSocketAddress(),
                        "Couldn't close DIS or DOS");
                e.printStackTrace();
            }
        }
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

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    public void updateTexts(String info){
        Bundle b = new Bundle();
        b.putInt("permits", SocketServer.semaphore.availablePermits());
        b.putString("info",
                SocketServer.getShortServerTime()+" T"+s.getRemoteSocketAddress()+": "+info);
        Message msg = handler.obtainMessage();
        msg.setData(b);
        msg.sendToTarget();
    }
}
