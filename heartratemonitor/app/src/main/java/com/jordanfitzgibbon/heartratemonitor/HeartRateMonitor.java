package com.jordanfitzgibbon.heartratemonitor;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.Collections;

// This class manages all the frames, does filtering and measures heart rate
public class HeartRateMonitor {

    private final int RECENT_VALUES_SIZE = 300;
    private ArrayList<CameraBridgeViewBase.CvCameraViewFrame> frames;
    private ArrayList<Mat> mats;

    public HeartRateMonitor(CameraBridgeViewBase.CvCameraViewFrame firstFrame) {

        // Initialize recent values with copies of the first frame
        this.frames = new ArrayList<CameraBridgeViewBase.CvCameraViewFrame>(Collections.nCopies(RECENT_VALUES_SIZE, firstFrame));
        this.mats = new ArrayList<Mat>(Collections.nCopies(RECENT_VALUES_SIZE, firstFrame.rgba()));
    }

    // Updates this object with the newest frame
    public void NewFrame(CameraBridgeViewBase.CvCameraViewFrame frame) {

        this.frames.remove(0);
        this.frames.add(frame);

        this.mats.remove(0);
        this.mats.add(frame.rgba());
    }

    // Gets the most recently added frame converted to Mat (for playback)
    public Mat GetLastMat() {
        return this.mats.get(RECENT_VALUES_SIZE-1);
    }

    // Gets the mean of the last frame
    public Scalar GetLastMean() {
        Mat mat = mats.get(RECENT_VALUES_SIZE-1);
        //Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGRA );

        return Core.mean(mat);
    }

}
