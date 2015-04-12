package com.jordanfitzgibbon.stepcounter;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

import java.util.Arrays;


public class MainActivity extends ActionBarActivity implements SensorEventListener {

    private final String TAG = "StepCounter";

    private SensorManager sensorManager;

    XYPlot plot;
    SimpleXYSeries seriesX;
    SimpleXYSeries seriesY;
    SimpleXYSeries seriesZ;

    // The number of values  to store in the series
    private final int GRAPH_BUFFER_SIZE = 100;
    private final int SENSOR_DELAY_HERTZ = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate");

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        this.configurePlots();
    }

    private void configurePlots()
    {
        plot = (XYPlot)findViewById(R.id.accelerometerXYPlot);

        // X
        Number[] seriesXNumbers = {1};
        seriesX = new SimpleXYSeries(
                Arrays.asList(seriesXNumbers), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Accelerometer X Values" // series title
        );
        LineAndPointFormatter seriesXFormatter = new LineAndPointFormatter(Color.RED, Color.RED, null, null);
        plot.addSeries(seriesX, seriesXFormatter);

        // Y
        Number[] seriesYNumbers = {0};
        seriesY = new SimpleXYSeries(
                Arrays.asList(seriesYNumbers), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Accelerometer Y Values" // series title
        );
        LineAndPointFormatter seriesYFormatter = new LineAndPointFormatter(Color.GREEN, Color.GREEN, null, null);
        plot.addSeries(seriesY, seriesYFormatter);

        // Z
        Number[] seriesZNumbers = {-1};
        seriesZ = new SimpleXYSeries(
                Arrays.asList(seriesZNumbers), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Accelerometer Z Values" // series title
        );
        LineAndPointFormatter seriesZFormatter = new LineAndPointFormatter(Color.BLUE, Color.BLUE, null, null);
        plot.addSeries(seriesZ, seriesZFormatter);

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

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        sensorManager.registerListener(
                this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");

        sensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }

    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            this.handleAccelerometer(event);
        }
    }

    private void handleAccelerometer(SensorEvent event) {
        float[] eventValues = event.values;
        float x = eventValues[0];
        float y = eventValues[1];
        float z = eventValues[2];

        Log.d(TAG, x + "," + y + "," + z);

        // Remove values from the series. Assume all series are the same size so use seriesX to do this check.
        if (seriesX.size() >= GRAPH_BUFFER_SIZE)
        {
            seriesX.removeFirst();
            seriesY.removeFirst();
            seriesZ.removeFirst();
        }

        seriesX.addLast(null, x);
        seriesY.addLast(null, y);
        seriesZ.addLast(null, z);

        plot.redraw();
    }


}
