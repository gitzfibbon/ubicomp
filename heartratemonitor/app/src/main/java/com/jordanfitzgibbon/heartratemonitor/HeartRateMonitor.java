package com.jordanfitzgibbon.heartratemonitor;

import android.util.Log;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import com.badlogic.gdx.audio.analysis.FFT;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;

// This class manages all the frames, does filtering and measures heart rate
public class HeartRateMonitor {

    private final String TAG = "HeartRateMonitor";

    private final int RED = 0;
    private final int GREEN = 1;
    private final int BLUE = 2;


    private final int RECENT_VALUES_SIZE = 300; // FPS is ~15 so this is ~10 seconds
    private ArrayList<CameraBridgeViewBase.CvCameraViewFrame> frames;
    private ArrayList<Mat> mats;
    private ArrayList<Scalar> deMeanedMeans;
    private ArrayList<Scalar> medianFiltered;

    public static final int FFT_SIZE = 128;
    private float spec[]  = new float[FFT_SIZE];

    // A sample must change slope and cross this threshold to be considered a heartbeat
    public static final double PEAK_DETECTION_THRESHOLD = -0.8;

    public HeartRateMonitor(CameraBridgeViewBase.CvCameraViewFrame firstFrame) {

        // Initialize recent values with copies of the first frame
        this.frames = new ArrayList<CameraBridgeViewBase.CvCameraViewFrame>(Collections.nCopies(RECENT_VALUES_SIZE, firstFrame));
        this.mats = new ArrayList<Mat>(Collections.nCopies(RECENT_VALUES_SIZE, firstFrame.rgba()));
        this.deMeanedMeans = new ArrayList<Scalar>(Collections.nCopies(RECENT_VALUES_SIZE, GetLastMeanDeMeanedHelper()));
        this.medianFiltered = new ArrayList<Scalar>(Collections.nCopies(RECENT_VALUES_SIZE, this.GetLastMedianFilteredHelper()));
    }

    // Updates this object with the newest frame
    public void AddNewFrame(CameraBridgeViewBase.CvCameraViewFrame frame) {

        this.frames.remove(0);
        this.frames.add(frame);

        this.mats.remove(0);
        this.mats.add(frame.rgba());

        this.deMeanedMeans.remove(0);
        this.deMeanedMeans.add(this.GetLastMeanDeMeanedHelper());

        this.medianFiltered.remove(0);
        this.medianFiltered.add(this.GetLastMedianFilteredHelper());
    }

    // Gets the most recently added frame converted to Mat (for playback)
    public Mat GetLastMat() {
        return this.mats.get(RECENT_VALUES_SIZE - 1);
    }

    // Gets the mean of the last frame
    public Scalar GetLastMean() {
        Mat mat = mats.get(RECENT_VALUES_SIZE - 1);
        //Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGRA );

        return Core.mean(mat);
    }

    // Gets the mos recently stored de-meaned mean Scalar
    public Scalar GetLastMeanDeMeaned() {
        return this.deMeanedMeans.get(RECENT_VALUES_SIZE -1);
    }

    // Calculates a de-meaned value of the Scalar of the most recent means
    private Scalar GetLastMeanDeMeanedHelper() {

        int meanWindowSize = 30;

        double sumR = 0;
        double sumG = 0;
        double sumB = 0;

        Scalar mean = this.GetLastMean(); // Initialize to the last mean
        for (int i = RECENT_VALUES_SIZE - meanWindowSize; i < RECENT_VALUES_SIZE; i++) {
            mean = Core.mean(mats.get(i));
            sumR += mean.val[RED];
            sumG += mean.val[GREEN];
            sumB += mean.val[BLUE];
        }

        double meanR = sumR / meanWindowSize;
        double meanG = sumG / meanWindowSize;
        double meanB = sumB / meanWindowSize;

        Scalar latestMean = this.GetLastMean();
        mean.set(new double[] {latestMean.val[RED] - meanR, latestMean.val[GREEN] - meanG, latestMean.val[BLUE] - meanB});

        return mean;
    }

    public Scalar GetLastMedianFiltered() {
        return this.medianFiltered.get(RECENT_VALUES_SIZE -1);
    }

    // Calculates the median filtered de-meaned mean of the frame
    private Scalar GetLastMedianFilteredHelper() {
        int medianSize = 2;

        // Get medianSize values from the stored means and put them into separate arrays so they can be sorted.
        ArrayList<Double> R = new ArrayList<>(medianSize);
        ArrayList<Double> G = new ArrayList<>(medianSize);
        ArrayList<Double> B = new ArrayList<>(medianSize);
        for (int i = 0; i < medianSize ; i++)
        {
            R.add(this.deMeanedMeans.get(RECENT_VALUES_SIZE - i - 1).val[RED]);
            G.add(this.deMeanedMeans.get(RECENT_VALUES_SIZE - i - 1).val[GREEN]);
            B.add(this.deMeanedMeans.get(RECENT_VALUES_SIZE - i - 1).val[BLUE]);
        }
        Collections.sort(R);
        Collections.sort(G);
        Collections.sort(B);

        Scalar medianValues = new Scalar(0,0,0,0);
        if (medianSize % 2 == 1) {
            int middleIndex = (medianSize - 1) / 2;

            medianValues.val[RED] = R.get(middleIndex);
            medianValues.val[GREEN] = G.get(middleIndex);
            medianValues.val[BLUE] = B.get(middleIndex);
            return medianValues;
        }
        else
        {
            int lowIndex = (medianSize / 2) -1;
            int highIndex = medianSize / 2;

            medianValues.val[RED] = (R.get(lowIndex) + R.get(highIndex)) / 2;
            medianValues.val[GREEN] = (G.get(lowIndex) + G.get(highIndex)) / 2;
            medianValues.val[BLUE] = (B.get(lowIndex) + B.get(highIndex)) / 2;

            return medianValues;
        }
    }

    // Use only one color channel
    public float[] FFT(int sampleRate) {

        int sampleSize = 100;

        // Start by using de-meaned, median filtered values

        float[] fftInput = new float[FFT_SIZE];

        // Fill up fftInput and zero pad
        for (int i=0; i < FFT_SIZE; i++) {
            if (i < sampleSize) {
                fftInput[i] = (float)this.medianFiltered.get(RECENT_VALUES_SIZE - sampleSize + i).val[RED];
            }
            else {
                fftInput[i] = 0;
            }

        }

        FFT fft = new FFT(FFT_SIZE, sampleRate);
        fft.forward(fftInput);

        // float[] fft_cpx = fft.getSpectrum();
        float[] imag = fft.getImaginaryPart();
        float[] real = fft.getRealPart();
        float[] mag = new float[FFT_SIZE];

        for (int i = 0; i < FFT_SIZE; i++) {
            mag[i] = (float)Math.sqrt((real[i] * real[i]) + (imag[i] * imag[i]));
        }

        return mag;
    }

    public boolean DetectPeak() {
        return this.DetectPeak(this.RECENT_VALUES_SIZE - 3, this.RECENT_VALUES_SIZE - 2, this.RECENT_VALUES_SIZE -1);
    }

    // Use the de-meaned values for peak detection
    public boolean DetectPeak(int previousX, int x, int nextX)
    {
        double previousSlope = this.deMeanedMeans.get(x).val[RED] - this.deMeanedMeans.get(previousX).val[RED];
        double nextSlope = this.deMeanedMeans.get(nextX).val[RED] - this.deMeanedMeans.get(x).val[RED];

        if (previousSlope < 0 && nextSlope > 0 && this.deMeanedMeans.get(x).val[RED] <= this.PEAK_DETECTION_THRESHOLD) {
            // this is a peak
            return true;
        }

        return false;
    }

    private int GetPeaks(int windowInSeconds, double FPS) {

        int totalSamples = (int)Math.round(windowInSeconds * FPS);
        totalSamples = Math.min(RECENT_VALUES_SIZE, totalSamples);
        int totalPeaks = 0;

        Log.d(TAG, "Total Samples: " + totalSamples);

        for (int i = 0; i<totalSamples-2; i++) {
            if ( this.DetectPeak(RECENT_VALUES_SIZE-i-3,RECENT_VALUES_SIZE-i-2,RECENT_VALUES_SIZE-1) ) {
                totalPeaks++;
            }
        }

        Log.d(TAG, "Total Peaks in " + windowInSeconds + " seconds: " + totalPeaks);

        return totalPeaks;
    }

    public int GetHeartRate(double FPS) {
        int heartRate = this.GetPeaks(12, FPS) * 5;

        if (heartRate < 45 || heartRate > 220)
        {
            // There is no useful heart rate data
            return 0;
        }

        return  heartRate;
    }

}
