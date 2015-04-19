package com.jordanfitzgibbon.heartratemonitor;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.Collections;

// This class manages all the frames, does filtering and measures heart rate
public class HeartRateMonitor {

    private final int RECENT_VALUES_SIZE = 150; // FPS is ~15 so this is ~10 seconds
    private ArrayList<CameraBridgeViewBase.CvCameraViewFrame> frames;
    private ArrayList<Mat> mats;
    private ArrayList<Scalar> deMeanedMeans;
    private ArrayList<Scalar> medianFiltered;

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
            sumR += mean.val[0];
            sumG += mean.val[1];
            sumB += mean.val[2];
        }

        double meanR = sumR / meanWindowSize;
        double meanG = sumG / meanWindowSize;
        double meanB = sumB / meanWindowSize;

        Scalar latestMean = this.GetLastMean();
        mean.set(new double[] {latestMean.val[0] - meanR, latestMean.val[1] - meanG, latestMean.val[2] - meanB});

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
            R.add(this.deMeanedMeans.get(RECENT_VALUES_SIZE - i - 1).val[0]);
            G.add(this.deMeanedMeans.get(RECENT_VALUES_SIZE - i - 1).val[1]);
            B.add(this.deMeanedMeans.get(RECENT_VALUES_SIZE - i - 1).val[2]);
        }
        Collections.sort(R);
        Collections.sort(G);
        Collections.sort(B);

        Scalar medianValues = new Scalar(0,0,0,0);
        if (medianSize % 2 == 1) {
            int middleIndex = (medianSize - 1) / 2;

            medianValues.val[0] = R.get(middleIndex);
            medianValues.val[1] = G.get(middleIndex);
            medianValues.val[2] = B.get(middleIndex);
            return medianValues;
        }
        else
        {
            int lowIndex = (medianSize / 2) -1;
            int highIndex = medianSize / 2;

            medianValues.val[0] = (R.get(lowIndex) + R.get(highIndex)) / 2;
            medianValues.val[1] = (G.get(lowIndex) + G.get(highIndex)) / 2;
            medianValues.val[2] = (B.get(lowIndex) + B.get(highIndex)) / 2;

            return medianValues;
        }

    }

}
