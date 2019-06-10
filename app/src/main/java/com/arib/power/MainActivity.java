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
import android.widget.Chronometer;
import android.media.MediaScannerConnection;

import android.os.Build;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;


import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

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
            //create booleans to create flags for if 30, 45, or 60 has been reached, resets when speed = 0
            private boolean first30 = false;
            private boolean first45 = false;
            private boolean first60 = false;
            //stores start time of when speed reaches zero
            private long zeroStart = 0;
            @Override
            public void onLocationChanged(Location location) {
                //get the calculated speed from the location object
                float speed = location.getSpeed();
                //convert from m/s to mph
                speed *= 2.23694;

                //update the UI
                updateUI(speed, first30, first45, first60, zeroStart);

                //get the latitude and longitude from the location object
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                long currTime = System.currentTimeMillis();

                // add latitude and longitude to logger
                if(currTime - startTime > 500) {
                    logger.add(new Value("latitude", latitude, currTime-startTime));
                    logger.add(new Value("longitude", longitude, currTime - startTime));
                }


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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch (id) {
            case R.id.action_savedata : saveData(); break;
        }

        return super.onOptionsItemSelected(item);
    }

    //update UI
    public void updateUI(float speed, boolean first30, boolean first45, boolean first60, long zeroStart) {
        updateSpeed(speed);
        updateBoxes(speed, first30, first45, first60, zeroStart);
    }

    //update 0-30, 0-45, 0-60 boxes
    public void updateBoxes(float speed, boolean first30, boolean first45, boolean first60, long zeroStart) {
        //get the textviews
        Chronometer thirtyView = (Chronometer) findViewById(R.id.zeroThirty);
        Chronometer fortyfiveView = (Chronometer) findViewById(R.id.zeroFortyfive);
        Chronometer sixtyView = (Chronometer) findViewById(R.id.zeroSixty);
        TextView top30 = (TextView) findViewById(R.id.topZero30);
        TextView top45 = (TextView) findViewById(R.id.topZero45);
        TextView top60 = (TextView) findViewById(R.id.topZero60);
        //variable to long millis for when car reaches certain speeds

        if(speed == 30){
            //set flag
            first30 = true;

            //variable to long millis for when car reaches certain speeds
            long end = System.currentTimeMillis();

            //get the best by parsing it from its textview
            long best30 = (long)Long.parseLong(top30.getText().toString());

            //compare timer times
            if((end-zeroStart) < best30){
                //update bestTime
                top30.setText((end-zeroStart) + "");
            }

        }else if(speed == 45){
            //set flag
            first45 = true;

            //variable to long millis for when car reaches certain speeds
            long end = System.currentTimeMillis();

            //get the best by parsing it from its textview
            long best45 = (long)Long.parseLong(top45.getText().toString());

            //compare timer times
            if((end-zeroStart) < best45){
                //update bestTime
                top45.setText((end-zeroStart) + "");
            }

        }else if(speed == 60){
            //set flag
            first60 = true;

            //variable to long millis for when car reaches certain speeds
            long end = System.currentTimeMillis();

            //get the best by parsing it from its textview
            long best60 = (long)Long.parseLong(top60.getText().toString());

            //compare timer times
            if((end-zeroStart) < best60){
                //update bestTime
                top60.setText((end-zeroStart) + "");
            }

        //if speed = 0 display "READY!" in *all* textviews
        }else if(speed == 0){
            //reset flags once speed = 60
            first30 = false;
            first45 = false;
            first60 = false;
            //update textviews
            thirtyView.setText("READY!");
            fortyfiveView.setText("READY!");
            sixtyView.setText("READY!");
            //get zeroStart time here
            zeroStart = System.currentTimeMillis();
        }
        if(speed < 30 && speed > 0 && first30 == false){
            //continuously update 30 timer box

            //variable to long millis for when car reaches certain speeds
            long end = System.currentTimeMillis();

            //update timer
            thirtyView.setText((end-zeroStart)/1000.0 + "");

        }
        if(speed < 45 && speed > 0 && first45 == false){
            //continuously update 45 timer box

            //variable to long millis for when car reaches certain speeds
            long end = System.currentTimeMillis();

            //update timer
            fortyfiveView.setText((end-zeroStart)/1000.0 + "");

        }
        if(speed < 60 && speed > 0 && first60 == false){
            //continuously update 60 timer box

            //variable to long millis for when car reaches certain speeds
            long end = System.currentTimeMillis();

            //update timer
            sixtyView.setText((end-zeroStart)/1000.0 + "");


        }
    }

    //update speed
    public void updateSpeed(float speed) {
        //get the textviews
        TextView speedView = (TextView) findViewById(R.id.speed);
        TextView topSpeedView = (TextView) findViewById(R.id.topSpeed);

        long currTime = System.currentTimeMillis();
        if(currTime - startTime > 500) {
            logger.add(new Value("speed", speed, currTime - startTime));
        }
        //get the topspeed by parsing it from its textview
        int topSpeed = (int)Double.parseDouble(topSpeedView.getText().toString());

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
        Log.d(LOG_TAG, "beforeGPS: " + GPSPermission + "");

        int readExtPermission = this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        int writeExtPermission = this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        Log.d(LOG_TAG, "beforeREAD: " + readExtPermission + "");
        Log.d(LOG_TAG, "beforeWRITE: " + writeExtPermission + "");


        if(GPSPermission != PackageManager.PERMISSION_GRANTED || readExtPermission != PackageManager.PERMISSION_GRANTED || writeExtPermission != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 20);

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

        //*** %250 updates like every couple seconds
        //*** %2.5 looks better, around four changes a second.. why why why?
        if((currTime - startTime) > 500) {
            //Get the textViews for the current acceleration values
            TextView xView = (TextView) findViewById(R.id.xAccel);
            TextView yView = (TextView) findViewById(R.id.yAccel);
            TextView zView = (TextView) findViewById(R.id.zAccel);

            //set the acceleration values to the textviews
            xView.setText(formatter.format(xAccel) + "");
            yView.setText(formatter.format(yAccel) + "");
            zView.setText(formatter.format(zAccel) + "");
        }


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

    private void saveData() {
        //save the data to a .csv file when clicked
        try{
            //get path to base directory
            String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();

            PrintWriter pw = new PrintWriter(new File(baseDir + File.separator + "test.csv"));
            Log.d(LOG_TAG, baseDir + File.separator + "test.csv");

            //write to csv file in the following sections time,speed.... time,xAccel.... time,yAccel.... time, zAccel
            pw.write("Time,speed\n");
            parseLogger(pw, logger, "speed");
            pw.write("Time,xAccel\n");
            parseLogger(pw, logger, "xAccel");
            pw.write("Time,yAccel\n");
            parseLogger(pw, logger, "yAccel");
            pw.write("Time,zAccel\n");
            parseLogger(pw, logger, "zAccel");
            //write long lat to csv file
            pw.write("Time,latitude\n");
            parseLogger(pw, logger, "latitude");
            pw.write("Time,longitude\n");
            parseLogger(pw, logger, "longitude");
            pw.close();

            //read from csv file
            BufferedReader bufferedReader = new BufferedReader(new FileReader(baseDir+File.separator+"test.csv"));
            Log.d(LOG_TAG, bufferedReader.readLine());
            String line = "";
            //while not EOF
            while ( (line=bufferedReader.readLine())!= null ) {
                Log.d(LOG_TAG, line); //can remove later, just don't have a device to check this on
            }


        }catch(Exception e){
            Log.e(LOG_TAG, e.getMessage());
        }

    }
    private void parseLogger(PrintWriter pw2, ArrayList<Value> log, String type2){
        //this function does not include header
        //write information to csv file for values in logger that are of type2
        for(int i=0; i< log.size(); i++)
        {
            if (log.get(i).getType() == type2 )
            {
                double val = log.get(i).getValue();
                long tim = log.get(i).getTime();
                pw2.write(tim+","+val+"\n");
            }
        }
        pw2.write("END\n\n"); //end each section with end

    }

}
