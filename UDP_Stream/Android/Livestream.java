package com.anthonyalves.sandbox.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.AsyncTask;
import android.view.Display;
import android.view.WindowManager;

import com.anthonyalves.sandbox.MainActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class Livestream extends AsyncTask<Object, Bitmap, Void> {

    private Socket tcpSocket;
    private DatagramSocket udpSocket;

    int MAX_UDP_BUFFER_SIZE = 65535;
    short HEADER_SIZE = 3;

    // Used to calculate FPS
    private int count = 0;
    private long last = 0;
    Context context;

    public Livestream(Context context) {
        this.context = context;
    }


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

        try {
            tcpSocket = new Socket(IPAddress, PORT); // create a new client UDP tcpSocket

            // send the resolution to the server to start receiving frames.
            OutputStream outToServer = tcpSocket.getOutputStream();
            DataOutputStream out = new DataOutputStream(outToServer);
            out.writeUTF("quality=" + params[2].toString());

            // make a UDP socket
            udpSocket = new DatagramSocket(); // create a new client UDP socket

            // Send UDP addr info to server
            byte[] sendData = ("connect").getBytes();
            for (int i = 0; i < 10; i++) {
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, PORT);
                udpSocket.send(sendPacket);
                Thread.sleep(100);
            }

            // Start getting the video stream
            byte[] receiveData = new byte[MAX_UDP_BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            last = System.nanoTime();
            HashMap<Short, FrameHolder> hashMap = new LinkedHashMap<>();

            while (tcpSocket.isConnected()) { // continuously retrieve frames from the server

                udpSocket.receive(receivePacket);
                byte[] packetBytes = new byte[receivePacket.getLength()];
                System.arraycopy(receivePacket.getData(), 0, packetBytes, 0, packetBytes.length); // get the byte value of the frame

                short currentFrame = (short) (packetBytes[0] & 0xFF);
                short currentChunk = (short) (packetBytes[1] & 0xFF);
                short totalChunks = (short) (packetBytes[2] & 0xFF);
                byte[] imgBytes = Arrays.copyOfRange(packetBytes, HEADER_SIZE, packetBytes.length);

                System.out.println("currentFrame: " + currentFrame + " Chunk " + currentChunk + "/" + totalChunks);

                if (totalChunks == 1) {
                    putImage(imgBytes);
                    continue;
                }

                if (!hashMap.containsKey(currentFrame)) {
                    hashMap.put(currentFrame, new FrameHolder(totalChunks));
                }

                int chunkCount = hashMap.get(currentFrame)
                        .addChunk(imgBytes,
                                currentChunk);

                if (chunkCount == totalChunks) {
                    putImage(concatenate(hashMap.get(currentFrame).chunks));
                    hashMap.remove(currentFrame);
                }

                if (hashMap.size() > 100) {
                    // It will always return keys in same order (as insertion) when calling keySet()
                    List keys = new ArrayList(hashMap.keySet());
                    for (int i = 0; i < keys.size() / 2; i++) {
                        hashMap.remove(keys.get(i));
                        // do stuff here
                    }
                }

            }

            outToServer.close();
            out.close();
            tcpSocket.close();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] concatenate (byte[]... arrays) {
        int length = 0;
        for (byte[] array : arrays) {
            length += array.length;
        }

        byte[] c = new byte[length];
        int offset = 0;
        for (byte[] a : arrays) {
            System.arraycopy(a, 0, c, offset, a.length);
            offset += a.length;
        }
        return c;
    }

    private void putImage(byte[] fullImage) {
        // decode from byte to an actual Bitmap image and resize to expand on the phone
        Bitmap bmp = BitmapFactory.decodeByteArray(fullImage, 0, fullImage.length);

        Matrix matrix = new Matrix();
        matrix.postRotate(-180);
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);

        // used for fps
        count++;

        // update the ImageView to the new frame
        publishProgress(new Bitmap[]{bmp});
    }

    protected void onProgressUpdate(Bitmap... bmp) {
        MainActivity.updateImage(bmp[0]); // update the ImageView on the Main UI thread

        long now = System.nanoTime();
        long delta = (now - last);
        BigDecimal framesOverDelta = new BigDecimal(count).divide(new BigDecimal(delta), 20, BigDecimal.ROUND_HALF_UP);
        BigDecimal fps = new BigDecimal(1000000000).multiply(framesOverDelta);
        fps = fps.divide(new BigDecimal(1), 2, BigDecimal.ROUND_HALF_UP);
        MainActivity.updateText(fps.toString()); // update the FPS TextView on the Main UI Thread
    }

    protected void onPostExecute(Void... result) {
        //do nothing
    }
}
