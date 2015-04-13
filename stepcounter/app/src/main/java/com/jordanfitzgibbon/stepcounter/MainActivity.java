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

import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XValueMarker;
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
    SimpleXYSeries seriesFilteredX;
    SimpleXYSeries seriesFilteredY;
    SimpleXYSeries seriesFilteredZ;
    SimpleXYSeries stepMarkersSeries; // Keeps track of where a step was detected

    // The number of values  to store in the series. Add one so the plot shows a nicer number at the end of the x domain
    private final int SERIES_BUFFER_SIZE = 200 + 1;

    // Variables to store recent values for median filtering
    private final int MEDIAN_FILTER_SIZE = 5;

    // Store this many of the most recent sensor values. This is used for filtering and de-meaning.
    private final int RECENT_VALUES_SIZE = 100;

    // Initialize ArrayLists to store recent values recorded by the sensor
    private ArrayList<Float> recentValuesX = new ArrayList<Float>(Collections.nCopies(RECENT_VALUES_SIZE,(float)0));
    private ArrayList<Float> recentValuesY = new ArrayList<Float>(Collections.nCopies(RECENT_VALUES_SIZE,(float)0));
    private ArrayList<Float> recentValuesZ = new ArrayList<Float>(Collections.nCopies(RECENT_VALUES_SIZE,(float)0));

    // In order to count as a step, an accelerometer value must have this magnitude above and below the 0 line.
    private final double ZERO_CROSS_THRESHOLD = 0.5;

    // Keep track of whether a value has gone above  the threshold
    private boolean aboveThreshold = false;

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

        // Remove values from the series. Assume all series are the same size so use seriesRawX to do this check.
        if (seriesRawX.size() >= SERIES_BUFFER_SIZE)
        {
            seriesRawX.removeFirst();
            seriesRawY.removeFirst();
            seriesRawZ.removeFirst();

            seriesFilteredX.removeFirst();
            seriesFilteredY.removeFirst();
            seriesFilteredZ.removeFirst();
            stepMarkersSeries.removeFirst();
        }

        // Add raw values to the series so they get drawn. De-mean them so they get plotted with the filtered values.
        seriesRawX.addLast(null, this.DeMean(this.recentValuesX, rawX));
        seriesRawY.addLast(null, this.DeMean(this.recentValuesY, rawY));
        seriesRawZ.addLast(null, this.DeMean(this.recentValuesZ, rawZ));


        // Apply a median filter
        float filteredX = this.ApplyMedianFilter(this.recentValuesX, rawX);
        float filteredY = this.ApplyMedianFilter(this.recentValuesY, rawY);
        float filteredZ = this.ApplyMedianFilter(this.recentValuesZ, rawZ);

        // De-Mean filtered values
        filteredX = this.DeMean(this.recentValuesX, filteredX);
        filteredY = this.DeMean(this.recentValuesY, filteredY);
        filteredZ = this.DeMean(this.recentValuesZ, filteredZ);

        seriesFilteredX.addLast(null, filteredX);
        seriesFilteredY.addLast(null, filteredY);
        seriesFilteredZ.addLast(null, filteredZ);

        // Zero Crossing calculation
        if (filteredZ >= ZERO_CROSS_THRESHOLD) {
            // The value has gone above the threshold
            this.aboveThreshold = true;

            // Add a 'invisible' value outside the range this is drawn on the plot
            stepMarkersSeries.addLast(null, 100);
        }
        else if (filteredZ <= -1 * ZERO_CROSS_THRESHOLD && this.aboveThreshold == true) {
            // The value was above the threshold at some point and then went below
            // This implies a zero crossing

            this.aboveThreshold = false; // Reset this variable

            // Now handle the zero crossing event
            //stepMarkersSeries.addLast(null, filteredZ);
            stepMarkersSeries.addLast(null, 0);
        }
        else {
            // Add a 'invisible' value outside the range this is drawn on the plot
            stepMarkersSeries.addLast(null, 100);
        }


        plotX.redraw();
        plotY.redraw();
        plotZ.redraw();
    }

    public float ApplyMedianFilter(ArrayList<Float> recentValues, float newValue)
    {
        // Remove the oldest value from the front and append the newest to the end
        recentValues.remove(0);
        recentValues.add(newValue);

        // Create a copy of the list into newValues so we don't disrupt the ordering after we sort. Only take the most recent values that we need for median filtering.
        int subListStart = recentValues.size() - MEDIAN_FILTER_SIZE;
        ArrayList<Float> newValues = new ArrayList<Float>(recentValues.subList(subListStart, subListStart + MEDIAN_FILTER_SIZE));

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

    public float DeMean(ArrayList<Float> recentValues, float valueToDeMean) {

        int size = recentValues.size();
        float sum = 0;
        for (int i=0; i<size; i++) {
            sum += recentValues.get(i);
        }

        float mean = sum / size;
        return valueToDeMean - mean;
    }

    private void configurePlots() {
        plotX = (XYPlot)findViewById(R.id.accelerometerXYPlot_X);
        plotY = (XYPlot)findViewById(R.id.accelerometerXYPlot_Y);
        plotZ = (XYPlot)findViewById(R.id.accelerometerXYPlot_Z);

        double rangeBoundary = 1.5;
        plotX.setRangeBoundaries(-1 * rangeBoundary, rangeBoundary, BoundaryMode.FIXED);
        plotY.setRangeBoundaries(-1 * rangeBoundary, rangeBoundary, BoundaryMode.FIXED);
        plotZ.setRangeBoundaries(-1 * rangeBoundary, rangeBoundary, BoundaryMode.FIXED);

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
        seriesFilteredX = new SimpleXYSeries(
                Arrays.asList(seriesXMedianNumbers), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Median X Values" // series title
        );
        LineAndPointFormatter seriesXMedianFormatter = new LineAndPointFormatter(Color.RED, null, null, null);
        plotX.addSeries(seriesFilteredX, seriesXMedianFormatter);

        // Median Y
        Number[] seriesYMedianNumbers = {0};
        seriesFilteredY = new SimpleXYSeries(
                Arrays.asList(seriesYMedianNumbers), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Median Y Values" // series title
        );
        LineAndPointFormatter seriesYMedianFormatter = new LineAndPointFormatter(Color.GREEN, null, null, null);
        plotY.addSeries(seriesFilteredY, seriesYMedianFormatter);

        // Median Z
        Number[] seriesZMedianNumbers = {0};
        seriesFilteredZ = new SimpleXYSeries(
                Arrays.asList(seriesZMedianNumbers), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Median Z Values" // series title
        );
        LineAndPointFormatter seriesZMedianFormatter = new LineAndPointFormatter(Color.BLUE, null, null, null);
        plotZ.addSeries(seriesFilteredZ, seriesZMedianFormatter);

        // Steps
        Number[] stepMarkerNumbers = {0};
        stepMarkersSeries = new SimpleXYSeries(
                Arrays.asList(stepMarkerNumbers), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Step Markers" // series title
        );
        LineAndPointFormatter stepMarkerFormatter = new LineAndPointFormatter(null, Color.WHITE, null, null);
        stepMarkerFormatter.getVertexPaint().setStrokeWidth(PixelUtils.dpToPix(10));
        plotZ.addSeries(stepMarkersSeries, stepMarkerFormatter);
    }


}
