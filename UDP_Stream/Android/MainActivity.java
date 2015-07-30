package com.anthonyalves.sandbox;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.anthonyalves.sandbox.util.UDPClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;


public class MainActivity extends ActionBarActivity {

    static ImageView img;
    static TextView textView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        img = (ImageView) findViewById(R.id.imageView);
        textView = (TextView) findViewById(R.id.textView);

        img.setImageResource(R.drawable.abc_btn_check_material);

        /*
            Here is hardcoded the IP of my router IP to the UDP Server.
            If you want to use this class, you will have to change the IP to the
            local IP of the Raspi and the port that python script is running on.
         */
        new UDPClient().execute("173.78.123.74", 8675); // also run the client on a separate thread


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Update the FPS
     * @param fps
     */
    public static void updateText(String fps) {
        textView.setText(fps + " FPS");
    }

    /**
     * Update the ImageView
     * @param bmp
     */
    public static void updateImage(Bitmap bmp) {
        if (bmp == null){ // just incase we get a null bitmap, set it to a random image...
            img.setImageResource(R.drawable.abc_btn_check_material);
        } else {
            img.setImageBitmap(bmp);
        }
    }
}
