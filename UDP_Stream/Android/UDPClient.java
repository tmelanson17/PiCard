package com.anthonyalves.sandbox.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import com.anthonyalves.sandbox.MainActivity;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UDPClient extends AsyncTask<Object, Bitmap, Void> {

    private DatagramSocket socket;  // UDP Client socket

    // Used to calculate FPS
    private int count = 0;
    private long last = 0;

    @Override
    protected Void doInBackground(Object... params) {

        // set the IP and port of the UDP server
        InetAddress IPAddress = null;
        try {
            IPAddress = InetAddress.getByName(params[0].toString());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        int PORT = (int) params[1];

        // variables to store the data when using the socket
        byte[] receiveData = new byte[65536];
        byte[] sendData = ("start").getBytes();
        Bitmap bmp = null;
        try {
            socket = new DatagramSocket(); // create a new client UDP socket

            // send the "start" text to the server to start receiving frames.
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, PORT);
            socket.send(sendPacket);


            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            last = System.nanoTime();
            while (true) { // continuously retrieve frames from the server

                socket.receive(receivePacket);
                byte[] img = receivePacket.getData(); // get the byte value of the frame

                // decode from byte to an actual Bitmap image and resize to expand on the phone
                bmp = BitmapFactory.decodeByteArray(img, 0, receivePacket.getLength());
                double scale = 15;
                bmp = Bitmap.createScaledBitmap(bmp, (int) (bmp.getWidth() * scale), (int) (bmp.getHeight() * scale), true);

                // used for fps
                count++;

                // update the ImageView to the new frame
                publishProgress(new Bitmap[]{bmp});

            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        socket.close();
        return null;
    }

    protected void onProgressUpdate(Bitmap... bmp) {
        MainActivity.updateImage(bmp[0]); // update the ImageView on the Main UI thread

        int frames = 10;
        if (count % frames == 0) { // calc the FPS every 10 frames
            long now = System.nanoTime();
            long delta = (now - last);
            BigDecimal oneOverDelta = new BigDecimal(frames).divide(new BigDecimal(delta), 20, BigDecimal.ROUND_HALF_UP);
            BigDecimal fps = new BigDecimal(1000000000).multiply(oneOverDelta);
            fps = fps.divide(new BigDecimal(1), 2, BigDecimal.ROUND_HALF_UP);
            System.out.print("Delta: " + delta + "; ");
            System.out.print("1/delta: " + oneOverDelta + "; ");
            System.out.println("fps: " + fps + "; ");
            last = now;
            MainActivity.updateText(fps.toString()); // update the FPS TextView on the Main UI Thread
        }

    }

    protected void onPostExecute(Void... result) {
        //do nothing
    }
}
