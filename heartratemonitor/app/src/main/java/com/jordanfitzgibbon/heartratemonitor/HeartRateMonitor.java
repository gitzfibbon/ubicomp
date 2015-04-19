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

    public HeartRateMonitor(CameraBridgeViewBase.CvCameraViewFrame firstFrame) {

        // Initialize recent values with copies of the first frame
        this.frames = new ArrayList<CameraBridgeViewBase.CvCameraViewFrame>(Collections.nCopies(RECENT_VALUES_SIZE, firstFrame));
        this.mats = new ArrayList<Mat>(Collections.nCopies(RECENT_VALUES_SIZE, firstFrame.rgba()));
    }

    // Updates this object with the newest frame
    public void AddNewFrame(CameraBridgeViewBase.CvCameraViewFrame frame) {

        this.frames.remove(0);
        this.frames.add(frame);

        this.mats.remove(0);
        this.mats.add(frame.rgba());
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

    // Gets a de-meaned value of the Scalar of the most recent means
    public Scalar GetLastMeanDeMeaned() {

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

}
