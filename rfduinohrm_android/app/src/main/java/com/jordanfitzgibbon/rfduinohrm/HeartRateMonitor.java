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

    // After a peak is detected we mus wait for a zero crossing (from + to -) before we can count the next one
    private boolean waitingForZeroCross = false;

    // Data structures to store samples
    private ArrayList<Float> rfduinoSamples;
    private ArrayList<Float> deMeanedSamples;
    private ArrayList<Boolean> peaks;

    // Size of the FFT. Effective size is half of this.
//    public static final int FFT_SIZE = 128;

    // A sample must change slope and cross above this threshold to be considered a heartbeat
    public static final double PEAK_DETECTION_THRESHOLD = 25;

    // A sample must cross below this threshold to be counted as a zero crossing
    public static final double ZERO_CROSS_THRESHOLD = 0;

    public HeartRateMonitor(Float firstSample) {

        // Initialize recent values with copies of the first frame
        this.rfduinoSamples = new ArrayList<Float>(Collections.nCopies(RECENT_VALUES_SIZE, firstSample));
        this.deMeanedSamples = new ArrayList<Float>(Collections.nCopies(RECENT_VALUES_SIZE, GetLastDemeanedSampleHelper()));
        this.peaks = new ArrayList<Boolean>(Collections.nCopies(RECENT_VALUES_SIZE, false));

    }

    // Store several different copies of the current frame
    public void AddNewSample(Float sample) {

        this.rfduinoSamples.remove(0);
        this.rfduinoSamples.add(sample);

        this.deMeanedSamples.remove(0);
        this.deMeanedSamples.add(this.GetLastDemeanedSampleHelper());

        // Check if we've crossed zero and can start counting peaks again
        if (this.waitingForZeroCross && this.GetLastDeMeanedSample() < HeartRateMonitor.ZERO_CROSS_THRESHOLD)
        {
            this.waitingForZeroCross = false;
        }

        this.peaks.remove(0);
        this.peaks.set(RECENT_VALUES_SIZE-2, this.DetectPeak()); // You need 3 values to detect a peak. Calculate the second from last item in the array.
        this.peaks.add(false);

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

    // Gets the most peak value in the second from last index (the last will always be false since it can't be calculated yet)
    public Boolean GetSecondFromLastPeak() {
        Boolean peak = peaks.get(RECENT_VALUES_SIZE - 2);
        return peak;
    }

    // A default overload that is applied to the latest value
    public boolean DetectPeak() {
        return this.DetectPeak(this.RECENT_VALUES_SIZE - 3, this.RECENT_VALUES_SIZE - 2, this.RECENT_VALUES_SIZE -1);
    }

    // Use the de-meaned values for peak detection
    public boolean DetectPeak(int previousX, int x, int nextX)
    {
        // After a peak is detected we must wait for a sample value to go below zero before we can count another peak
        // Otherwise, we may count false heartbeats
        if (this.waitingForZeroCross)
        {
            return false;
        }

        double previousSlope = this.deMeanedSamples.get(x) - this.deMeanedSamples.get(previousX);
        double nextSlope = this.deMeanedSamples.get(nextX) - this.deMeanedSamples.get(x);

        if (previousSlope > 0 && nextSlope < 0 && this.deMeanedSamples.get(x) >= this.PEAK_DETECTION_THRESHOLD) {
            // this is a peak
            this.waitingForZeroCross = true;
            return true;
        }
        else {
            return false;
        }

    }

    // NOT USED (GetHrFromAvgPeakInterval is used instead)
    private int CountPeaks(int windowInSeconds, int sampleRateHz) {

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

        for (int i = RECENT_VALUES_SIZE - totalSamplesUsed; i < RECENT_VALUES_SIZE; i++) {
            if ( this.peaks.get(i) == true ) {
                totalPeaks++;
            }
        }

        Log.d(TAG, "Total Peaks in " + windowInSeconds + " seconds: " + totalPeaks);

        return totalPeaks;
    }

    // Calculates the avg time in ms between peaks. Uses the windowInSeconds to look back on samples in that window
    private int GetHrFromAvgPeakInterval(int windowInSeconds, int sampleRateHz) {

        // Figure out how many samples to pull in order to calculate a heartrate from the desired window time
        // Eg. if we sample at 20Hz and want to calculate hr based on the last 10 seconds then we need to pull 20*10 samples
        int totalSamplesNeeded = Math.min(RECENT_VALUES_SIZE, windowInSeconds * sampleRateHz);

        boolean firstPeakFound = false;
        ArrayList<Float> intervals = new ArrayList<>();
        int currentIntervalSize = 0;

        for (int i = RECENT_VALUES_SIZE - totalSamplesNeeded; i < RECENT_VALUES_SIZE; i++) {

            // Loop until we find the first peak. We ignore data before it since we don't know when the previous peak occurred.
            if (firstPeakFound == false) {
                if (this.peaks.get(i)) {
                    firstPeakFound = true;
                }
                continue;
            }

            // Increment interval, even if the next will be a peak
            currentIntervalSize++;

            // If we find another peak, store the interval size between this and the previous
            if (this.peaks.get(i)) {
                intervals.add((float)currentIntervalSize);
                currentIntervalSize = 0;
            }

            // If we get to the end of the loop, throw out the rest of the data

        }

        // Convert each value to a time
        float sampleRateInMs = 1000f / sampleRateHz;
        for (int i=0; i < intervals.size(); i++) {
            intervals.set(i, intervals.get(i) * sampleRateInMs);
        }

        // Calculate the mean sample count in each interval
        int sum = 0;
        int numIntervals = 0;
        for (int i=0; i < intervals.size(); i++) {
            if (intervals.get(i) < 200 || intervals.get(i) > 2000) {
                // Ignore the value
                ;
            }
            else {
                sum += intervals.get(i);
                numIntervals++;
            }
        }

        float heartRate = 0;
        if (numIntervals != 0) {
            float mean = sum / numIntervals;

            // Figure out how many time spans fit in 60 seconds
            heartRate = 60 * 1000 / mean;
        }
        
        return (int)Math.round(heartRate);
    }

    // Count peaks within this many recent seconds
    public int GetHeartRate(int windowInSeconds, int sampleRateHz) {

        // Peak Counting
        // Multiple this by windowInSeconds to get the peaks within 60 seconds
        // Float windowMultiplier = 60f / windowInSeconds;
        // int heartRate = (int) (this.CountPeaks(windowInSeconds, sampleRateHz) * windowMultiplier);

        // Peak Intervals
        int heartRate = this.GetHrFromAvgPeakInterval(windowInSeconds, sampleRateHz);

        return  heartRate;
    }

}
