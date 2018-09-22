package com.arib.power;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //Location Manager and Listener
    LocationManager locManager;
    LocationListener locListener;

    //Sensor Manager and necessary objects
    SensorManager sensorManager;
    private Sensor linearAccel;

    //LOGTAG
    private final String LOG_TAG = this.getClass().getSimpleName();

    //holds the starttime. Used to find the time the data was logged.
    private long startTime;

    private ArrayList<Value> logger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startTime = System.currentTimeMillis();
        logger = new ArrayList<>();

        //Get location manager from system services
        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //set the location listener
        locListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //get the calculated speed from the location object
                float speed = location.getSpeed();
                //convert from m/s to mph
                speed *= 2.23694;
                //update the UI
                updateUI(speed);
            }


            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {
                Toast.makeText(MainActivity.this, "GPS FOUND", Toast.LENGTH_LONG).show();
            }

            //on signal lost tell user
            @Override
            public void onProviderDisabled(String s) {
                Toast.makeText(MainActivity.this, "GPS LOST", Toast.LENGTH_LONG).show();
            }
        };

        //request permissions
        askForPermissions();

        //if the sdk is value to check for permissions
        //check permission then set location listener to location manager
        if(Build.VERSION.SDK_INT >= 23 && (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION,
                Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED) &&
                (checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, Process.myPid(),
                        Process.myUid()) == PackageManager.PERMISSION_GRANTED)) {
            //assign listener to location manager. set to lowest refresh rate
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locListener);
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(this, linearAccel, SensorManager.SENSOR_DELAY_FASTEST);
    }

    //update UI
    public void updateUI(float speed) {
        //get the textviews
        TextView speedView = (TextView) findViewById(R.id.speed);
        TextView topSpeedView = (TextView) findViewById(R.id.topSpeed);

        long currTime = System.currentTimeMillis();
        if(currTime - startTime > 500) {
            logger.add(new Value("speed", speed, currTime - startTime));
        }
        //get the topspeed by parsing it from its textview
        int topSpeed = Integer.parseInt(topSpeedView.getText().toString());

        //if current speed is faster than current recorded top speed update topspeed textview
        if(speed > topSpeed) {
            topSpeedView.setText(speed + "");
        }

        //update the speedview
        speedView.setText(speed + "");
    }

    //ASk for Fine location access
    @TargetApi(23)
    private void askForPermissions() {

        int GPSPermission = this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        Log.d(LOG_TAG, "before: " + GPSPermission + "");

        if(GPSPermission != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 20);

        GPSPermission = this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        Log.d(LOG_TAG, "after: " + GPSPermission + "");

        if(Build.VERSION.SDK_INT >= 23 && (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED) && (checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED)) {
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locListener);
        }

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        DecimalFormat formatter = new DecimalFormat("##.###");

        //retrieve data from accelerometer
        double xAccel = sensorEvent.values[0];
        double yAccel = sensorEvent.values[1];
        double zAccel = sensorEvent.values[2];

        long currTime = System.currentTimeMillis();

        if(currTime - startTime > 500) {
            logger.add(new Value("xAccel", xAccel, currTime - startTime));
            logger.add(new Value("yAccel", yAccel, currTime - startTime));
            logger.add(new Value("zAccel", zAccel, currTime - startTime));
        }



        //Get the textViews for the current acceleration values
        TextView xView = (TextView) findViewById(R.id.xAccel);
        TextView yView = (TextView) findViewById(R.id.yAccel);
        TextView zView = (TextView) findViewById(R.id.zAccel);

        //set the acceleration values to the textviews
        xView.setText(formatter.format(xAccel) + "");
        yView.setText(formatter.format(yAccel) + "");
        zView.setText(formatter.format(zAccel) + "");


        //get the textviews for the peak acceleration values
        TextView peakXView = (TextView) findViewById(R.id.peakXAccel);
        TextView peakYView = (TextView) findViewById(R.id.peakYAccel);
        TextView peakZView = (TextView) findViewById(R.id.peakZAccel);

        //get the peak accelerations from the peak textview
        double peakXAccel = Double.parseDouble(peakXView.getText().toString());
        double peakYAccel = Double.parseDouble(peakYView.getText().toString());
        double peakZAccel = Double.parseDouble(peakZView.getText().toString());

        //if the current accel is larger than the peak we currently have update the peakview with new largest
        if(Math.abs(xAccel) > Math.abs(peakXAccel)) {
            peakXView.setText(formatter.format(xAccel) + "");
        }
        if(Math.abs(yAccel) > Math.abs(peakYAccel)) {
            peakYView.setText(formatter.format(yAccel) + "");
        }
        if(Math.abs(zAccel) > Math.abs(peakZAccel)) {
            peakZView.setText(formatter.format(zAccel) + "");
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)){
            //TODO: save file

            Calendar cal = Calendar.getInstance();
            Log.d(LOG_TAG, cal.getTime().toString());
            //try to save the data in object storage method
//            try {
//                FileOutputStream fos = this.openFileOutput(strMonth + year, Context.MODE_PRIVATE);
//                ObjectOutputStream oos = new ObjectOutputStream(fos);
//                oos.writeObject(dataFile);
//            } catch (Exception e) {
//                //if it fails let the user know that the save failed
//                Toast.makeText(this, "Could not save this month's file", Toast.LENGTH_SHORT).show();
//                Log.e(LOG_TAG, e.toString());
//            }
            //TODO: reset data
            return true;
        }
        return false;
    }
}
