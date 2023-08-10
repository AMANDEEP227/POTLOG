package com.example.potlog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.navigation.ui.AppBarConfiguration;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;


public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;

    private ToggleButton start;
    private Button record;
    private Button type;
    private Button fire;
    private boolean isSensorOn = false;
    private SensorManager sensorManager;

   // private LocationManager locationManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private TextView read;
    private String anomaly;
    private final long RATE = 100;
    private ArrayList<String> data;
    private ArrayAdapter<String> adapter;

    //private ListView readings;

    private String X;
    private String Y;
    private String Z;
    private String roll;
    private String pitch;
    private String yaw;

    private String longitude;
    private String latitude;
    private String speed;

    private String vehicle;

    private Timer timer;

    private Workbook workbook;
    private Sheet sheet;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton info=(ImageButton)findViewById(R.id.info);

        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setMessage("It is a data collection app for Pothole Detection MTech project." +
                "\n\nClick START to start data logging."+
                "\n\nWhile driving, if there is a pothole ahead, Tap black POTHOLE button to record pothole event" +
                "\n\nOr Tap white toggle button to switch between POTHOLE and OTHER events while driving.\nPOTHOLE when driving over potholes and OTHER if road is normal." +
                "\n\nClick DOWNLOAD will save data as EXCEL sheet" +
                "\n\nEmail to:\namandeep.writes@gmail.com ");
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog alertDialog=builder.create();
                alertDialog.show();

            }
        });

        Spinner spinner=(Spinner)findViewById(R.id.vehicle);
        String[] items={"Select Vehicle","4-Wheeler","3-Wheeler","2-Wheeler","Cycle"};

        ArrayAdapter<String> adapter1=new ArrayAdapter<>(this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,items);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter1);
        vehicle="Select Vehicle";

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                vehicle=(String)parent.getItemAtPosition(position);
                if(position!=0){
                    start.setEnabled(true);
                }
                else{
                    start.setEnabled(false);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {


            }
        });



        timer = new Timer();
        data = new ArrayList<>();


        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);


        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

       // locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(this);
        locationRequest=LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        record = (Button) findViewById(R.id.record);
        start = (ToggleButton) findViewById(R.id.start);
        type = (ToggleButton) findViewById(R.id.type);
        fire=(Button)findViewById(R.id.fire);

        //  readings=(ListView)findViewById(R.id.reading);
        // adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,data);


        read = (TextView) findViewById(R.id.textView);

        anomaly = "Other";

        type.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    if (anomaly.equals("Other")) {
                        anomaly = "Pothole";
                    } else {
                        anomaly = "Other";
                    }


            }
        });

        fire.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    anomaly="Pothole";

                    Handler handler=new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            anomaly="Other";

                        }
                    },3000);


            }
        });

        long time = System.currentTimeMillis();
        String timestamp = String.valueOf(time);
        String filename = timestamp + "_POTLOG.xls";


        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                workbook = new HSSFWorkbook();
                sheet = workbook.createSheet("POTLOG_DATA");

                int i;
                int j;
                for (i = 0; i < data.size(); i++) {
                    String d = data.get(i);
                    Row row = sheet.createRow(i + 1);
                    String[] str = d.split(",");
                    for (j = 0; j < str.length; j++) {
                        if (str[j] != null) {
                            Cell cell = row.createCell(j);
                            cell.setCellValue(str[j]);
                        }

                    }

                }

                try {

                    File filedir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File file = new File(filedir, filename);
                    String filepath = file.getAbsolutePath();
                    FileOutputStream fileOutputStream = new FileOutputStream(filepath);
                    workbook.write(fileOutputStream);
                    if (file.exists()) {
                        Toast.makeText(MainActivity.this, "Download successful!", Toast.LENGTH_SHORT).show();
                    }
                    fileOutputStream.close();
                    workbook.close();


                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }


            }
        });


        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                    if (start.isChecked()) {
                        startSensorReading();

                    } else {
                        stopSensorReading();
                    }


            }
        });
    }


    private void startSensorReading() {

        sensorManager.registerListener(accelerometerListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(gyroscopeListener,gyroscope,SensorManager.SENSOR_DELAY_NORMAL);


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},100);
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
       // locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, RATE, 1, locationListener);

        fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper());

        isSensorOn=true;
        type.setEnabled(true);
        record.setEnabled(false);
        fire.setEnabled(true);


        Timer timer=new Timer();

        timer.scheduleAtFixedRate(new capture(),0,RATE);

    }

    private void stopSensorReading(){
        sensorManager.unregisterListener(accelerometerListener);
       // locationManager.removeUpdates(locationListener);
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        isSensorOn=false;
      //  anomaly="Smooth";
        timer.cancel();
        type.setEnabled(false);
        fire.setEnabled(false);
        record.setEnabled(true);

      //  readings.setAdapter(adapter);

    }

    private SensorEventListener accelerometerListener=new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {

            if(isSensorOn){
                float x=event.values[0];
                float y=event.values[1];
                float z=event.values[2];

                X=String.valueOf(x);
                Y=String.valueOf(y);
                Z=String.valueOf(z);


                long time=System.currentTimeMillis();
                String timestamp=String.valueOf(time);


            }


        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private SensorEventListener gyroscopeListener=new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {

            if(isSensorOn){
                float x=event.values[0];
                float y=event.values[1];
                float z=event.values[2];

                roll=String.valueOf(x);
                pitch=String.valueOf(y);
                yaw=String.valueOf(z);


            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

   /** private LocationListener locationListener=new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            double lati=location.getLatitude();
            double longi=location.getLongitude();

            latitude=String.valueOf(lati);
            longitude=String.valueOf(longi);


        }
    };**/

    private LocationCallback locationCallback=new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);


                for(Location location: locationResult.getLocations()){
                    double lati=location.getLatitude();
                    double longi=location.getLongitude();
                    double sp=location.getSpeed();

                    latitude=String.valueOf(lati);
                    longitude=String.valueOf(longi);
                    speed=String.valueOf(sp);
                }
               /**Location location=locationResult.getLastLocation();
               if(location!=null){
                   double lati=location.getLatitude();
                   double longi=location.getLongitude();
                   double sp=location.getSpeed();

                   latitude=String.valueOf(lati);
                   longitude=String.valueOf(longi);
                   speed=String.valueOf(sp);
               }**/



        }
    };


    private class capture extends TimerTask{

        @Override
        public void run() {

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                ZoneId z = ZoneId.of("Asia/Kolkata");
                LocalDateTime dt=LocalDateTime.now(z);
                read.post(new Runnable() {
                    @Override
                    public void run() {

                        data.add(String.valueOf(dt)+","+X+","+Y+","+Z+","+roll+","+pitch+","+yaw+","+longitude+","+latitude+","+speed+","+anomaly+","+vehicle);
                        read.setText(String.valueOf(dt)+"\t\t\t"+X+"\t\t\t"+Y+"\t\t\t"+Z+"\t\t\t"+roll+"\t\t\t"+pitch+"\t\t\t"+yaw+"\t\t\t"+longitude+"\t\t\t"+latitude+"\t\t\t"+speed+"\t\t\t"+anomaly);

                    }
                });

            }


        }
    }

    @Override
    protected void onStop() {
        super.onStop();

    }



}