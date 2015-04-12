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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;


public class MainActivity extends ActionBarActivity implements SensorEventListener {

    private final String TAG = "StepCounter";

    private SensorManager sensorManager;

    XYPlot plotX;
    XYPlot plotY;
    XYPlot plotZ;

    SimpleXYSeries seriesRawX;
    SimpleXYSeries seriesRawY;
    SimpleXYSeries seriesRawZ;
    SimpleXYSeries seriesMedianX;
    SimpleXYSeries seriesMedianY;
    SimpleXYSeries seriesMedianZ;


    // The number of values  to store in the series. Add one so the plot shows a nicer number at the end of the x domain
    private final int SERIES_BUFFER_SIZE = 200 + 1;

    //  Variables to store recent values for median filtering
    private final int MEDIAN_FILTER_SIZE = 10;
    private ArrayList<Float> recentValuesX = new ArrayList<Float>(Collections.nCopies(MEDIAN_FILTER_SIZE,(float)0));
    private ArrayList<Float> recentValuesY = new ArrayList<Float>(Collections.nCopies(MEDIAN_FILTER_SIZE,(float)0));
    private ArrayList<Float> recentValuesZ = new ArrayList<Float>(Collections.nCopies(MEDIAN_FILTER_SIZE,(float)0));


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate");

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        this.configurePlots();
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
                //SensorManager.SENSOR_DELAY_NORMAL // every 200,000 microseconds
                SensorManager.SENSOR_DELAY_UI // every 60,000 microseconds
                //SensorManager.SENSOR_DELAY_GAME // every 20,000 microseconds
                //SensorManager.SENSOR_DELAY_FASTEST // every 0 microseconds
        );
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
        float rawX = eventValues[0];
        float rawY = eventValues[1];
        float rawZ = eventValues[2];

//        Log.d(TAG, x + "," + y + "," + z);

        // Apply a median filter but
        float medianX = this.ApplyMedianFilter(this.recentValuesX, rawX);
        float medianY = this.ApplyMedianFilter(this.recentValuesY, rawY);
        float medianZ = this.ApplyMedianFilter(this.recentValuesZ, rawZ);

        // Remove values from the series. Assume all series are the same size so use seriesRawX to do this check.
        if (seriesRawX.size() >= SERIES_BUFFER_SIZE)
        {
            seriesRawX.removeFirst();
            seriesRawY.removeFirst();
            seriesRawZ.removeFirst();

            seriesMedianX.removeFirst();
            seriesMedianY.removeFirst();
            seriesMedianZ.removeFirst();
        }

        seriesRawX.addLast(null, rawX);
        seriesRawY.addLast(null, rawY);
        seriesRawZ.addLast(null, rawZ);

        seriesMedianX.addLast(null, medianX);
        seriesMedianY.addLast(null, medianY);
        seriesMedianZ.addLast(null, medianZ);

        plotX.redraw();
        plotY.redraw();
        plotZ.redraw();
    }

    public float ApplyMedianFilter(ArrayList<Float> values, float newValue)
    {
        // Remove the oldest value from the front and append the newest to the end
        values.remove(0);
        values.add(newValue);

        // Create a copy of the list so we don't disrupt the ordering after we sort
        ArrayList<Float> newValues = new ArrayList<Float>(values);

        Collections.sort(newValues);

        int arraySize = newValues.size();

        if (newValues.size() % 2 == 1) {
            int middleIndex = (arraySize - 1) / 2;
            return (float)newValues.get(middleIndex);
        }
        else
        {
            int lowIndex = (arraySize / 2) -1;
            float lowValue = (float)newValues.get(lowIndex);
            int highIndex = arraySize / 2;
            float highValue = (float)newValues.get(highIndex);

            return (lowValue + highValue) / 2;
        }
    }

    private void configurePlots() {
        plotX = (XYPlot)findViewById(R.id.accelerometerXYPlot_X);
        plotY = (XYPlot)findViewById(R.id.accelerometerXYPlot_Y);
        plotZ = (XYPlot)findViewById(R.id.accelerometerXYPlot_Z);

        // Raw X
        Number[] seriesXRawNumbers = {0};
        seriesRawX = new SimpleXYSeries(
                Arrays.asList(seriesXRawNumbers), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Raw X Values" // series title
        );
        LineAndPointFormatter seriesXRawFormatter = new LineAndPointFormatter(Color.LTGRAY, null, null, null);
        plotX.addSeries(seriesRawX, seriesXRawFormatter);

        // Raw Y
        Number[] seriesYRawNumbers = {0};
        seriesRawY = new SimpleXYSeries(
                Arrays.asList(seriesYRawNumbers), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Raw Y Values" // series title
        );
        LineAndPointFormatter seriesYRawFormatter = new LineAndPointFormatter(Color.LTGRAY, null, null, null);
        plotY.addSeries(seriesRawY, seriesYRawFormatter);

        // Raw Z
        Number[] seriesZRawNumbers = {0};
        seriesRawZ = new SimpleXYSeries(
                Arrays.asList(seriesZRawNumbers), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Raw Z Values" // series title
        );
        LineAndPointFormatter seriesZRawFormatter = new LineAndPointFormatter(Color.LTGRAY, null, null, null);
        plotZ.addSeries(seriesRawZ, seriesZRawFormatter);



        // Median X
        Number[] seriesXMedianNumbers = {0};
        seriesMedianX = new SimpleXYSeries(
                Arrays.asList(seriesXMedianNumbers), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Median X Values" // series title
        );
        LineAndPointFormatter seriesXMedianFormatter = new LineAndPointFormatter(Color.RED, null, null, null);
        plotX.addSeries(seriesMedianX, seriesXMedianFormatter);

        // Median Y
        Number[] seriesYMedianNumbers = {0};
        seriesMedianY = new SimpleXYSeries(
                Arrays.asList(seriesYMedianNumbers), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Median Y Values" // series title
        );
        LineAndPointFormatter seriesYMedianFormatter = new LineAndPointFormatter(Color.GREEN, null, null, null);
        plotY.addSeries(seriesMedianY, seriesYMedianFormatter);

        // Median Z
        Number[] seriesZMedianNumbers = {0};
        seriesMedianZ = new SimpleXYSeries(
                Arrays.asList(seriesZMedianNumbers), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Median Z Values" // series title
        );
        LineAndPointFormatter seriesZMedianFormatter = new LineAndPointFormatter(Color.BLUE, null, null, null);
        plotZ.addSeries(seriesMedianZ, seriesZMedianFormatter);

    }


}
