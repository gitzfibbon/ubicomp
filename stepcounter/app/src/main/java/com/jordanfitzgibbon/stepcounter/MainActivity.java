package com.jordanfitzgibbon.stepcounter;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class MainActivity extends ActionBarActivity implements SensorEventListener {

    private final String TAG = "StepCounter";

    private SensorManager sensorManager;

    // The hint value for the sensor sampling rate in microseconds
    private final int SENSOR_DELAY_MICROSECONDS = 10000;

    XYPlot plotX;
    XYPlot plotY;
    XYPlot plotZ;

    SimpleXYSeries seriesRawX;
    SimpleXYSeries seriesRawY;
    SimpleXYSeries seriesRawZ;
    SimpleXYSeries seriesFilteredX;
    SimpleXYSeries seriesFilteredY;
    SimpleXYSeries seriesFilteredZ;
    SimpleXYSeries seriesStepMarker; // Keeps track of where a step was detected
    SimpleXYSeries seriesThresholdUpperZ; // Used to draw a horizontal line for upper threshold
    SimpleXYSeries seriesThresholdLowerZ; // Used to draw a horizontal line for lower threshold

    // The number of values  to store in the series
    private final int SERIES_BUFFER_SIZE = 1000;

    // Variables to store recent values for median filtering
    private final int MEDIAN_FILTER_SIZE = 20;

    // Store this many of the most recent sensor values. This is used for filtering and de-meaning.
    private final int RECENT_VALUES_SIZE = 50;

    // Initialize ArrayLists to store recent values recorded by the sensor
    private ArrayList<Float> recentValuesX = new ArrayList<Float>(Collections.nCopies(RECENT_VALUES_SIZE,(float)0));
    private ArrayList<Float> recentValuesY = new ArrayList<Float>(Collections.nCopies(RECENT_VALUES_SIZE,(float)0));
    private ArrayList<Float> recentValuesZ = new ArrayList<Float>(Collections.nCopies(RECENT_VALUES_SIZE,(float)0));

    // In order to count as a step, an accelerometer value must have this magnitude above and below the 0 line.
    private final double ZERO_CROSS_THRESHOLD = 0.3;

    // Keep track of whether a value has gone above  the threshold
    private boolean aboveThreshold = false;

    // Keeps track of total steps
    private long stepsTaken = 0;

    // View objects
    TextView stepsTakenTextView;
    TextView settingsTextView;

    // Used to play a sound for each step
    ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate");

        stepsTakenTextView = (TextView)findViewById(R.id.stepsTakenTextView);
        settingsTextView = (TextView)findViewById(R.id.settingsTextView);
        settingsTextView.setText("Device must be held flat in your palm"
            + "\nMedian Filter Size: " + MEDIAN_FILTER_SIZE
            + "\nMean Size: " + RECENT_VALUES_SIZE
            + "\nZero Crossing Threshold: " + ZERO_CROSS_THRESHOLD
            + "\nSample Rate Hz: " + 1000000 / SENSOR_DELAY_MICROSECONDS);

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
                //SensorManager.SENSOR_DELAY_UI // every 60,000 microseconds
                //SensorManager.SENSOR_DELAY_GAME // every 20,000 microseconds
                //SensorManager.SENSOR_DELAY_FASTEST // every 0 microseconds
                SENSOR_DELAY_MICROSECONDS
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
            seriesStepMarker.removeFirst();
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
            seriesStepMarker.addLast(null, 100);
        }
        else if (filteredZ <= -1 * ZERO_CROSS_THRESHOLD && this.aboveThreshold == true) {
            // The value was above the threshold at some point and then went below
            // This implies a zero crossing

            this.aboveThreshold = false; // Reset this variable

            // Now handle the zero crossing event
            seriesStepMarker.addLast(null, 0);

            // Increment steps taken
            stepsTaken++;
            stepsTakenTextView.setText(String.valueOf(stepsTaken));

            // Play a sound
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
        }
        else {
            // Add a 'invisible' value outside the range this is drawn on the plot
            seriesStepMarker.addLast(null, 100);
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

        double rangeBoundary = 1.3;
        plotX.setRangeBoundaries(-1 * rangeBoundary, rangeBoundary, BoundaryMode.FIXED);
        plotY.setRangeBoundaries(-1 * rangeBoundary, rangeBoundary, BoundaryMode.FIXED);
        plotZ.setRangeBoundaries(-1 * rangeBoundary, rangeBoundary, BoundaryMode.FIXED);

        plotX.setDomainBoundaries(0, SERIES_BUFFER_SIZE, BoundaryMode.FIXED);
        plotY.setDomainBoundaries(0, SERIES_BUFFER_SIZE, BoundaryMode.FIXED);
        plotZ.setDomainBoundaries(0, SERIES_BUFFER_SIZE, BoundaryMode.FIXED);

        // Raw X
        Number[] seriesXRawNumbers = {0};
        seriesRawX = new SimpleXYSeries(
                Arrays.asList(seriesXRawNumbers), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Raw X" // series title
        );
        LineAndPointFormatter seriesXRawFormatter = new LineAndPointFormatter(Color.LTGRAY, null, null, null);
        plotX.addSeries(seriesRawX, seriesXRawFormatter);

        // Raw Y
        Number[] seriesYRawNumbers = {0};
        seriesRawY = new SimpleXYSeries(
                Arrays.asList(seriesYRawNumbers), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Raw Y" // series title
        );
        LineAndPointFormatter seriesYRawFormatter = new LineAndPointFormatter(Color.LTGRAY, null, null, null);
        plotY.addSeries(seriesRawY, seriesYRawFormatter);

        // Raw Z
        Number[] seriesZRawNumbers = {0};
        seriesRawZ = new SimpleXYSeries(
                Arrays.asList(seriesZRawNumbers), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Raw Z" // series title
        );
        LineAndPointFormatter seriesZRawFormatter = new LineAndPointFormatter(Color.LTGRAY, null, null, null);
        plotZ.addSeries(seriesRawZ, seriesZRawFormatter);



        // Median X
        Number[] seriesXMedianNumbers = {0};
        seriesFilteredX = new SimpleXYSeries(
                Arrays.asList(seriesXMedianNumbers), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Median X" // series title
        );
        LineAndPointFormatter seriesXMedianFormatter = new LineAndPointFormatter(Color.RED, null, null, null);
        plotX.addSeries(seriesFilteredX, seriesXMedianFormatter);

        // Median Y
        Number[] seriesYMedianNumbers = {0};
        seriesFilteredY = new SimpleXYSeries(
                Arrays.asList(seriesYMedianNumbers), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Median Y" // series title
        );
        LineAndPointFormatter seriesYMedianFormatter = new LineAndPointFormatter(Color.GREEN, null, null, null);
        plotY.addSeries(seriesFilteredY, seriesYMedianFormatter);

        // Median Z
        Number[] seriesZMedianNumbers = {0};
        seriesFilteredZ = new SimpleXYSeries(
                Arrays.asList(seriesZMedianNumbers), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Median Z" // series title
        );
        LineAndPointFormatter seriesZMedianFormatter = new LineAndPointFormatter(Color.BLUE, null, null, null);
        plotZ.addSeries(seriesFilteredZ, seriesZMedianFormatter);

        // Steps
        Number[] stepMarkerNumbers = {0};
        seriesStepMarker = new SimpleXYSeries(
                Arrays.asList(stepMarkerNumbers), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Steps" // series title
        );
        LineAndPointFormatter stepMarkerFormatter = new LineAndPointFormatter(null, Color.WHITE, null, null);
        stepMarkerFormatter.getVertexPaint().setStrokeWidth(PixelUtils.dpToPix(10));
        plotZ.addSeries(seriesStepMarker, stepMarkerFormatter);

        // Threshold Lines
        LineAndPointFormatter thresholdFormatter = new LineAndPointFormatter(Color.WHITE, null, null, null);

        List<Double> thresholdUpperNumbers = Collections.nCopies(SERIES_BUFFER_SIZE,ZERO_CROSS_THRESHOLD);
        seriesThresholdUpperZ = new SimpleXYSeries(
                thresholdUpperNumbers,
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Upper" // series title
        );
        plotZ.addSeries(seriesThresholdUpperZ, thresholdFormatter);

        List<Double> thresholdLowerNumbers = Collections.nCopies(SERIES_BUFFER_SIZE,-1 * ZERO_CROSS_THRESHOLD);
        seriesThresholdLowerZ = new SimpleXYSeries(
                thresholdLowerNumbers,
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Lower" // series title
        );
        plotZ.addSeries(seriesThresholdLowerZ, thresholdFormatter);
    }


}
