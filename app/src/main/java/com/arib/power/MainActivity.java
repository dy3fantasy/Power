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

    //holds constant starttime. Used to find the time the data was logged.
    private long startTime;
    //holds temporary start time
    private long pseudoStartTimeSensor = 0;
    //sensor and location will both need separate variables
    private long pseudoStartTimeLocation = 0;

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
            private boolean hit30 = true;
            private boolean hit45 = true;
            private boolean hit60 = true;
            //create integer to detect if this is the first time the car has his a certain mph milestone since it was 0mph
            int sameRoundFlag = 0;
            @Override
            public void onLocationChanged(Location location) {
                //get the calculated speed from the location object
                float speed = location.getSpeed();
                //convert from m/s to mph
                speed *= 2.23694;

                //update the UI
                updateSpeed(speed);
                updateBoxes(speed);

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

            private void flagger(float speed){
                //get the textviews
                TextView thirtyView = (TextView) findViewById(R.id.zeroThirty);
                TextView fortyfiveView = (TextView) findViewById(R.id.zeroFortyfive);
                TextView sixtyView = (TextView) findViewById(R.id.zeroSixty);
                TextView top30 = (TextView) findViewById(R.id.topZero30);
                TextView top45 = (TextView) findViewById(R.id.topZero45);
                TextView top60 = (TextView) findViewById(R.id.topZero60);

                //set stop flag to true if speed has been equal to special mph and it is the first time it has happened in the round
                if(speed > 30 && sameRoundFlag == 0){
                    //set stop flag to true
                    hit30 = true;

                    //temp variable to hold new time
                    long temp = (long)Long.parseLong(thirtyView.getText().toString());

                    if ( temp < (long)Long.parseLong(top30.getText().toString()) ){
                        top30.setText( temp + "");
                    }

                    sameRoundFlag = 30;
                }
                if(speed > 45 && sameRoundFlag == 30){
                    hit45 = true;

                    //temp variable to hold new time
                    long temp = (long)Long.parseLong(fortyfiveView.getText().toString());

                    if ( temp < (long)Long.parseLong(top45.getText().toString()) ){
                        top45.setText( temp + "");
                    }

                    sameRoundFlag = 45;
                }
                if(speed > 60 && sameRoundFlag == 45){
                    hit60 = true;

                    //temp variable to hold new time
                    long temp = (long)Long.parseLong(sixtyView.getText().toString());

                    if ( temp < (long)Long.parseLong(top60.getText().toString()) ){
                        top60.setText( temp + "");
                    }

                    sameRoundFlag = 60;
                }
                if(speed == 0){
                    //update box to say ready
                    thirtyView.setText("READY!");
                    fortyfiveView.setText("READY!");
                    sixtyView.setText("READY!");

                    //set flags ready to start the timer again
                    hit30 = false;
                    hit45 = false;
                    hit60 = false;
                    sameRoundFlag = 0;
                }

            }

            //update 0-30, 0-45, 0-60 boxes
            private void updateBoxes(float speed) {
                //get the textviews
                TextView thirtyView = (TextView) findViewById(R.id.zeroThirty);
                TextView fortyfiveView = (TextView) findViewById(R.id.zeroFortyfive);
                TextView sixtyView = (TextView) findViewById(R.id.zeroSixty);

                //get currTime
                long currTime = System.currentTimeMillis();

                //get time elapsed since last check
                long timeElapsed = getTimeElapsed(currTime,2);

                //if check if first time since a reset (0 mph) ***
                if(thirtyView.getText().toString() == "READY!" && speed > 0){
                    //set flags equal to false
                    hit30 = false;
                    hit45 = false;
                    hit60 = false;
                    thirtyView.setText("0");
                    fortyfiveView.setText("0");
                    sixtyView.setText("0");

                }

                //update boxes every 250ms with current timer
                if(timeElapsed > 250) {
                    //if we have not hit time continue doing the stop watch

                    //if we haven't hit a special mph continue stopwatches
                    //else compare best time
                    if( hit30 == false ){
                        stopWatch(30, timeElapsed);
                    }
                    if( hit45 == false ){
                        stopWatch(45, timeElapsed);
                    }
                    if( hit60 == false){
                        stopWatch(60,timeElapsed);
                    }

                    //update flags
                    flagger(speed);
                }


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
        //WIP

        //END WIP
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

        //update every 250ms
        if(getTimeElapsed(currTime,1) > 250){
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

    /**
     * @param currTime don't want to redo declaration
     * @param flag = 1 if sensor calls, flag = 2 if location calls
     * @return timeElapse returns time elapsed since last pseudoStartTimeSensor & pseudoStartTimeLocation in ms
     */
    private long getTimeElapsed(long currTime, int flag){
        long timeElapsedSensor = currTime - pseudoStartTimeSensor;
        long timeElapsedLocation = currTime - pseudoStartTimeLocation;
        if (timeElapsedSensor > 250 && flag == 1){
            pseudoStartTimeSensor = currTime;
            return timeElapsedSensor;
        }else if(timeElapsedLocation > 250 && flag == 2){
            pseudoStartTimeLocation = currTime;
            return timeElapsedLocation;
        }else {
            return 0;
        }
    }

    //function to update stopWatch textBox for all boxes
    //updates based on what was originally in the text box + the elapsed time
    private void stopWatch(int boxNum, long timeElapsed){
        //get the textviews
        TextView thirtyView = (TextView) findViewById(R.id.zeroThirty);
        TextView fortyfiveView = (TextView) findViewById(R.id.zeroFortyfive);
        TextView sixtyView = (TextView) findViewById(R.id.zeroSixty);

        //make stop watch continue
        if(boxNum == 30){

            if(!thirtyView.getText().toString().equals("READY!")){//working properly
                String getString = thirtyView.getText().toString();
                thirtyView.setText("fsg"); //for some reason if this string isn't here the app crashes???
                try{
                    long original = (long) Long.parseLong(getString);
                    long sum = original + timeElapsed;
                    thirtyView.setText(sum + "");
                }catch(NumberFormatException e){
                    //do something here
                }

            }
        }else if(boxNum == 45){

            if(!fortyfiveView.getText().toString().equals("READY!")){//working properly
                String getString = fortyfiveView.getText().toString();
                fortyfiveView.setText("fsg");
                try{
                    long original = (long) Long.parseLong(getString);
                    long sum = original + timeElapsed;
                    fortyfiveView.setText(sum + "");
                }catch(NumberFormatException e){

                }
            }

        }else if(boxNum == 60){

            if(!sixtyView.getText().toString().equals("READY!")){//working properly
                String getString = sixtyView.getText().toString();
                sixtyView.setText("fsg");
                try{
                    long original = (long) Long.parseLong(getString);
                    long sum = original + timeElapsed;
                    sixtyView.setText(sum + "");
                }catch(NumberFormatException e){

                }
            }

        }else{
            //wrong box error
        }
    }

}
