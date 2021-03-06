package com.jordanfitzgibbon.heartratemonitor;


import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;

public class MainActivity extends ActionBarActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private final String TAG = "HeartRateMonitor";

    private HeartRateMonitor heartRateMonitor;
    private PlotManager plotManager;

    // OpenCV
    CameraBridgeViewBase mOpenCvCameraView;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    // For keeping track of time
    long lastUpdateTime;

    // How often to refresh the FFT and heart rate
    private final int refreshIntervalMs = 2000;

    // Keeps track of how many samples were collected in the current interval
    private int sampleCount = 0;

    // Keeps track of the average fps from the previous interval
    // Initialize to 10 but it will get updated every interval
    private double FPS = 10;

    private int heartRate = 0;

    // Textviews that we will update
    private TextView textViewSettings;
    private TextView textViewHeartRate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the plots
        plotManager = new PlotManager(this);
        plotManager.ConfigureRawPlot();
        plotManager.ConfigureFilteredPlot();
        plotManager.ConfigureFFTPlot(HeartRateMonitor.FFT_SIZE);

        // Set up OpenCV
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.OpenCvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

//        PackageManager packageManager = this.getPackageManager();
//        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
//            // If this is a Nexus 7 it will return false to the above condition. So, use the back camera.
//            mOpenCvCameraView.setCameraIndex(0);
//        }

        textViewSettings = (TextView)findViewById(R.id.textViewSettings);
        textViewHeartRate = (TextView)findViewById(R.id.textViewHeartRate);
    }

    @Override
    protected  void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);

        // Set up a timer for recalculating heart rate
        this.lastUpdateTime = System.nanoTime();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
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
    public void onCameraViewStarted(int width, int height) {
        Log.d(TAG, "onCameraViewStarted");
    }

    @Override
    public void onCameraViewStopped() {
        Log.d(TAG, "onCameraViewStopped");
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        // Increment the sample count. We use it to calculate current FPS.
        this.sampleCount++;

        // Initialize HeartRateMonitor class
        if (this.heartRateMonitor == null) {
            this.heartRateMonitor = new HeartRateMonitor(inputFrame);
        }

        // Store the frame
        this.heartRateMonitor.AddNewFrame(inputFrame);

        // Plot the mean RGB values of the frame
        Scalar mean = this.heartRateMonitor.GetLastMean();
        plotManager.UpdateRawPlot(mean.val[0], mean.val[1], mean.val[2]);

        // Plot the de-meaned mean RGB values, median filtered mean RGB values and peaks
        boolean isPeak = heartRateMonitor.DetectPeak();
        Scalar deMeanedMean = this.heartRateMonitor.GetLastMeanDeMeaned();
        Scalar medianFiltered = this.heartRateMonitor.GetLastMedianFiltered();
        plotManager.UpdateFilteredPlot(
                deMeanedMean.val[0], deMeanedMean.val[1], deMeanedMean.val[2],
                medianFiltered.val[0], medianFiltered.val[1], medianFiltered.val[2],
                isPeak);

        // Check if the current interval is over
        long nanoTime = System.nanoTime();
        if (this.ConvertNanoToMs(nanoTime - lastUpdateTime) >= this.refreshIntervalMs) {

            // Reset the last updated time
            this.lastUpdateTime = nanoTime;

            // Update the FPS variable with the average of the actual FPS of the current interval and the FPS of the previous interval
            double previousFPS = this.FPS;
            this.FPS = (this.sampleCount / (double)(this.refreshIntervalMs / 1000) + previousFPS) / 2;
            this.sampleCount = 0;
            Log.d(TAG, "FPS: " + this.FPS);

            // Update the FFT plot
            int fftWindowInSeconds = 10;
            float[] fftMags = this.heartRateMonitor.FFT(fftWindowInSeconds, (int)Math.round(this.FPS));
            this.plotManager.UpdateFFTPlot(fftMags);

            // Get heart rate
            this.heartRate = heartRateMonitor.GetHeartRate(this.FPS);
            Log.d(TAG, "Heart Rate: " + heartRate);

            // Update the UI with the heart rate
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textViewHeartRate.setText("Heart Rate: " + heartRate);
                }
            });
        }

        // Return the frame to be displayed on the device
        return this.heartRateMonitor.GetLastMat();
    }

    private int ConvertNanoToMs(long nanoTime) {
        return (int) nanoTime / 1000000;
    }
}
