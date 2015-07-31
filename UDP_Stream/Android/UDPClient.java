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

import java.io.IOException;
import java.math.BigDecimal;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

public class UDPClient extends AsyncTask<Object, Bitmap, Void> {

    private DatagramSocket socket;  // UDP Client socket

    // Used to calculate FPS
    private int count = 0;
    private long last = 0;
    int framesDropped = 0;
    Context context;

    public UDPClient(Context context) {
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

        // variables to store the data when using the socket
        byte[] receiveData = new byte[65536];
        byte[] sendData = ("start " + params[2].toString()).getBytes();
        try {
            socket = new DatagramSocket(); // create a new client UDP socket

            // send the "start" text to the server to start receiving frames.
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, PORT);
            socket.send(sendPacket);


            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            last = System.nanoTime();
            while (true) { // continuously retrieve frames from the server

                socket.receive(receivePacket);
                byte[] packetBytes = new byte[receivePacket.getLength()];
                System.arraycopy(receivePacket.getData(), 0, packetBytes, 0, packetBytes.length); // get the byte value of the frame

                boolean gatherChunks = false;
                boolean fullFrame = false;
                short currentFrame = (short) (packetBytes[0] & 0xFF);
                short currentChunk = (short) (packetBytes[1] & 0xFF);
                short totalChunks = (short) (packetBytes[2] & 0xFF);

                System.out.println("currentFrame: " + currentFrame + " Chunk " + currentChunk + "/" + totalChunks);

                byte[][] chunks = null;
                short chunksReceived = 0;

                if (currentChunk == 1) {
                    // start putting the frame together
                    if (totalChunks == 1) {
                        fullFrame = true;
                    } else {
                        gatherChunks = true;
                    }
                    chunks = new byte[totalChunks][];
                    chunksReceived = 1;
                    chunks[currentChunk-1] = Arrays.copyOfRange(packetBytes, 3, packetBytes.length);
                }

                while (gatherChunks) {
                    socket.receive(receivePacket);
                    packetBytes = new byte[receivePacket.getLength()];
                    System.arraycopy(receivePacket.getData(), 0, packetBytes, 0, packetBytes.length); // get the byte value of the frame

                    short mCurrentFrame = (short) (packetBytes[0] & 0xFF);
                    short mCurrentChunk = (short) (packetBytes[1] & 0xFF);

                    System.out.println("WHILE currentFrame: " + mCurrentFrame + " Chunk " + mCurrentChunk + "/" + totalChunks);


                    if (currentFrame != mCurrentFrame) {
                        framesDropped++;
                        break;
                    }

                    chunksReceived++;
                    chunks[mCurrentChunk - 1] = Arrays.copyOfRange(packetBytes, 3, packetBytes.length);

                    if (chunksReceived == totalChunks) {
                        fullFrame = true;
                        break;
                    }
                }

                if (fullFrame) {
                    byte[] fullImage = concatenate(chunks);
                    // decode from byte to an actual Bitmap image and resize to expand on the phone
                    Bitmap bmp = BitmapFactory.decodeByteArray(fullImage, 0, fullImage.length);

                    Matrix matrix = new Matrix();
                    matrix.postRotate(-90);
                    bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);

                    WindowManager wm = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
                    Display display = wm.getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);
                    int width = size.x;

                    // used for fps
                    count++;

                    // update the ImageView to the new frame
                    publishProgress(new Bitmap[]{bmp});
                }
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
            last = now;
            MainActivity.updateText(fps.toString()); // update the FPS TextView on the Main UI Thread
        }

    }

    protected void onPostExecute(Void... result) {
        //do nothing
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
}
