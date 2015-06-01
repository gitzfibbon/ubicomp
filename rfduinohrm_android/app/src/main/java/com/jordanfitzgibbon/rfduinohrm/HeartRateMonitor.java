package com.jordanfitzgibbon.rfduinohrm;

import android.util.Log;

//import com.badlogic.gdx.audio.analysis.FFT;

import java.util.ArrayList;
import java.util.Collections;

// This class manages all the rfduinoSamples, does filtering and measures heart rate
public class HeartRateMonitor {

    private final String TAG = "HRM_HeartRateMonitor";

    // How many samples to keep in memory
    private final int RECENT_VALUES_SIZE = 500;

    // Data structures to store samples
    private ArrayList<Float> rfduinoSamples;
    private ArrayList<Float> deMeanedSamples;
//    private ArrayList<Scalar> medianFiltered;

    // Size of the FFT. Effective size is half of this.
    public static final int FFT_SIZE = 128;

    // A sample must change slope and cross above this threshold to be considered a heartbeat
    public static final double PEAK_DETECTION_THRESHOLD = 25;

    public HeartRateMonitor(Float firstSample) {

        // Initialize recent values with copies of the first frame
        this.rfduinoSamples = new ArrayList<Float>(Collections.nCopies(RECENT_VALUES_SIZE, firstSample));
        this.deMeanedSamples = new ArrayList<Float>(Collections.nCopies(RECENT_VALUES_SIZE, GetLastDemeanedSampleHelper()));
//        this.medianFiltered = new ArrayList<Scalar>(Collections.nCopies(RECENT_VALUES_SIZE, this.GetLastMedianFilteredHelper()));
    }

    // Store several different copies of the current frame
    public void AddNewSample(Float sample) {

        this.rfduinoSamples.remove(0);
        this.rfduinoSamples.add(sample);

        this.deMeanedSamples.remove(0);
        this.deMeanedSamples.add(this.GetLastDemeanedSampleHelper());

//        this.medianFiltered.remove(0);
//        this.medianFiltered.add(this.GetLastMedianFilteredHelper());
    }

    // Gets the most recent sample
    public Float GetLastSample() {
        Float sample = rfduinoSamples.get(RECENT_VALUES_SIZE - 1);
        return sample;
    }

    // Gets the most recently stored de-meaned mean of the RGB values
    public Float GetLastDeMeanedSample() {
        return this.deMeanedSamples.get(RECENT_VALUES_SIZE -1);
    }

    // Calculates a de-meaned value of the most recent samples
    private Float GetLastDemeanedSampleHelper() {

        // How many previous samples we will use to de-mean
        int meanWindowSize = 15;

        // Calculate the mean of previous values
        Float sum = 0f;
        for (int i = RECENT_VALUES_SIZE - meanWindowSize; i < RECENT_VALUES_SIZE; i++) {
            sum += rfduinoSamples.get(i);
        }
        Float mean = sum / meanWindowSize;

        // Subtract the mean from the raw rfduino sample
        Float demeanedSample = this.GetLastSample() - mean;

        return demeanedSample;
    }

    // Get the most recently stored median filtered mean Scalar
//    public Scalar GetLastMedianFiltered() {
//        return this.medianFiltered.get(RECENT_VALUES_SIZE -1);
//    }

    // Calculates the median filtered de-meaned mean of the frame
//    private Scalar GetLastMedianFilteredHelper() {
//        int medianSize = 2;
//
//        // Get medianSize values from the stored means and put them into separate arrays so they can be sorted.
//        ArrayList<Double> R = new ArrayList<>(medianSize);
//        ArrayList<Double> G = new ArrayList<>(medianSize);
//        ArrayList<Double> B = new ArrayList<>(medianSize);
//        for (int i = 0; i < medianSize ; i++)
//        {
//            R.add(this.deMeanedMeans.get(RECENT_VALUES_SIZE - i - 1).val[RED]);
//            G.add(this.deMeanedMeans.get(RECENT_VALUES_SIZE - i - 1).val[GREEN]);
//            B.add(this.deMeanedMeans.get(RECENT_VALUES_SIZE - i - 1).val[BLUE]);
//        }
//        Collections.sort(R);
//        Collections.sort(G);
//        Collections.sort(B);
//
//        Scalar medianValues = new Scalar(0,0,0,0);
//        if (medianSize % 2 == 1) {
//            int middleIndex = (medianSize - 1) / 2;
//
//            medianValues.val[RED] = R.get(middleIndex);
//            medianValues.val[GREEN] = G.get(middleIndex);
//            medianValues.val[BLUE] = B.get(middleIndex);
//            return medianValues;
//        }
//        else
//        {
//            int lowIndex = (medianSize / 2) -1;
//            int highIndex = medianSize / 2;
//
//            medianValues.val[RED] = (R.get(lowIndex) + R.get(highIndex)) / 2;
//            medianValues.val[GREEN] = (G.get(lowIndex) + G.get(highIndex)) / 2;
//            medianValues.val[BLUE] = (B.get(lowIndex) + B.get(highIndex)) / 2;
//
//            return medianValues;
//        }
//    }

//    // Use only one color channel
//    public float[] FFT(int windowInSeconds, int sampleRate) {
//
//        int sampleSize = windowInSeconds * sampleRate;
//
//        // Start by using de-meaned, median filtered values
//
//        float[] fftInput = new float[FFT_SIZE];
//
//        // Fill up fftInput and zero pad
//        for (int i=0; i < FFT_SIZE; i++) {
//            if (i < sampleSize) {
//                fftInput[i] = (float)this.medianFiltered.get(RECENT_VALUES_SIZE - sampleSize + i).val[RED];
//            }
//            else {
//                fftInput[i] = 0;
//            }
//
//        }
//
//        FFT fft = new FFT(FFT_SIZE, sampleRate);
//        fft.forward(fftInput);
//
//        // float[] fft_cpx = fft.getSpectrum();
//        float[] imaginary = fft.getImaginaryPart();
//        float[] real = fft.getRealPart();
//        float[] magnitude = new float[FFT_SIZE];
//
//        for (int i = 0; i < FFT_SIZE; i++) {
//            magnitude[i] = (float)Math.sqrt((real[i] * real[i]) + (imaginary[i] * imaginary[i]));
//        }
//
//        return magnitude;
//    }

    // A default overload that is applied to the latest value
    public boolean DetectPeak() {
        return this.DetectPeak(this.RECENT_VALUES_SIZE - 3, this.RECENT_VALUES_SIZE - 2, this.RECENT_VALUES_SIZE -1);
    }

    // Use the de-meaned values for peak detection
    public boolean DetectPeak(int previousX, int x, int nextX)
    {
//        // Ignore samples that exceed the max feasible value
//        Float maxValue = 175f;
//        if (this.deMeanedSamples.get(x) > maxValue)
//        {
//            return false;
//        }

        double previousSlope = this.deMeanedSamples.get(x) - this.deMeanedSamples.get(previousX);
        double nextSlope = this.deMeanedSamples.get(nextX) - this.deMeanedSamples.get(x);

        if (previousSlope > 0 && nextSlope < 0 && this.deMeanedSamples.get(x) >= this.PEAK_DETECTION_THRESHOLD) {
            // this is a peak
            return true;
        }
        else {
            return false;
        }

    }

    private int GetPeaks(int windowInSeconds, int sampleRateHz) {

        // Figure out how many samples to pull in order to calculate a heartrate from the desired window time
        // Eg. if we sample at 20Hz and want to calculate hr based on the last 10 seconds then we need to pull 20*10 samples
        int totalSamplesNeeded = windowInSeconds * sampleRateHz;

        // If we don't have enough samples available, adjust the window
        if (totalSamplesNeeded > RECENT_VALUES_SIZE)
        {
            windowInSeconds = RECENT_VALUES_SIZE / sampleRateHz;
        }

        int totalSamplesUsed = Math.min(RECENT_VALUES_SIZE, totalSamplesNeeded);
        int totalPeaks = 0;

        Log.d(TAG, "Total Samples being used: " + totalSamplesUsed);

        for (int i = 0; i < totalSamplesUsed-2; i++) {
            if ( this.DetectPeak(RECENT_VALUES_SIZE-i-3,RECENT_VALUES_SIZE-i-2,RECENT_VALUES_SIZE-1) ) {
                totalPeaks++;
            }
        }

        Log.d(TAG, "Total Peaks in " + windowInSeconds + " seconds: " + totalPeaks);

        return totalPeaks;
    }

    public int GetHeartRate(int sampleRateHz) {

        int windowInSeconds = 10; // Count peaks within this many recent seconds
        Float windowMultiplier = 60f / windowInSeconds; // Multiple this by windowInSeconds to get the peaks within 60 seconds

        int heartRate = (int) (this.GetPeaks(windowInSeconds, sampleRateHz) * windowMultiplier);

//        if (heartRate < 45 || heartRate > 220)
//        {
//            // There is no useful heart rate data
//            return 0;
//        }

        return  heartRate;
    }

}
