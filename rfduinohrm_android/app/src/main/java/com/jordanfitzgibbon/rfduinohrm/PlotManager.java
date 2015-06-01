package com.jordanfitzgibbon.rfduinohrm;

import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;

import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlotManager {

    // The number of points to show on the plot
    private final int plotSize = 300;

    // Plots de-meaned values
    XYPlot filteredPlot;
    SimpleXYSeries seriesDeMeaned;

    // Used for peak detection and goes on the filtered plot
    SimpleXYSeries seriesPeakDetectionThreshold;
    SimpleXYSeries seriesPeaks;

    // Plots raw samples from the rfduino
    XYPlot rawPlot;
    SimpleXYSeries seriesRaw;

    // Plots median filtered values
    SimpleXYSeries seriesMedian;

    // FFT plot
    XYPlot fftPlot;
    SimpleXYSeries seriesFFT;

    public PlotManager(ActionBarActivity parent) {
        this.filteredPlot = (XYPlot)parent.findViewById(R.id.filteredPlot);
        this.rawPlot = (XYPlot)parent.findViewById(R.id.rawPlot);
//        this.fftPlot = (XYPlot)parent.findViewById(R.id.fftPlot);
    }

    public void UpdateFilteredPlot(
            Float demeanedSample, boolean isPeak) {

        // Remove values from the series. Assume all series are the same size so use any series to do this check.
        if (seriesDeMeaned.size() >= plotSize) {
            seriesDeMeaned.removeFirst();
            seriesPeaks.removeFirst();
        }

        seriesDeMeaned.addLast(null, demeanedSample);

        if (isPeak) {
            seriesPeaks.setY(0, this.plotSize - 2);
        } else {
            // setting to 1000 makes it not visible on the plot
            seriesPeaks.setY(1000, this.plotSize - 2);
        }
        seriesPeaks.addLast(null, 1000);

        filteredPlot.redraw();
    }

    public void ConfigureFilteredPlot() {

        double rangeBoundary = 100;
        filteredPlot.setRangeBoundaries(-1 * rangeBoundary, rangeBoundary, BoundaryMode.FIXED);
        filteredPlot.setDomainBoundaries(0, plotSize, BoundaryMode.FIXED);

        // Median
//        seriesMedian = new SimpleXYSeries(
//                Collections.nCopies(plotSize, 0), // convert array to a list
//                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
//                "Median R" // title
//        );
//        LineAndPointFormatter seriesFormatterMedianR = new LineAndPointFormatter(Color.RED, null, null, null);
//        filteredPlot.addSeries(seriesMedian, seriesFormatterMedianR);

        // DeMeaned
        seriesDeMeaned = new SimpleXYSeries(
                Collections.nCopies(plotSize, 0), // fill the series with 0
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "DeMeaned" // title
        );
        LineAndPointFormatter seriesFormatterDemeaned = new LineAndPointFormatter(Color.RED, null, null, null);
        filteredPlot.addSeries(seriesDeMeaned, seriesFormatterDemeaned);


        // Draws the threshold line for peak detection
        List<Double> thresholdNumbers = Collections.nCopies(this.plotSize,HeartRateMonitor.PEAK_DETECTION_THRESHOLD);
        seriesPeakDetectionThreshold = new SimpleXYSeries(
                thresholdNumbers,
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Peak Threshold " + HeartRateMonitor.PEAK_DETECTION_THRESHOLD // series title
        );
        LineAndPointFormatter thresholdFormatter = new LineAndPointFormatter(Color.WHITE, null, null, null);
        filteredPlot.addSeries(seriesPeakDetectionThreshold, thresholdFormatter);

        List<Double> peakDetectionDotNumbers = Collections.nCopies(this.plotSize,1000.0); // Set to some high value that will be off the chart
        seriesPeaks = new SimpleXYSeries(
                peakDetectionDotNumbers,
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Peaks" // series title
        );
        LineAndPointFormatter peakFormatter = new LineAndPointFormatter(null, Color.WHITE, null, null);
        peakFormatter.getVertexPaint().setStrokeWidth(PixelUtils.dpToPix(5));
        filteredPlot.addSeries(seriesPeaks, peakFormatter);
    }



    public void UpdateRawPlot(Float sample) {

        // Remove values from the series. Assume all series are the same size so use seriesRaw to do this check.
        if (seriesRaw.size() >=  plotSize) {
            seriesRaw.removeFirst();
       }

        seriesRaw.addLast(null, sample);

        rawPlot.redraw();
    }

    public void ConfigureRawPlot() {

        double rangeBoundary = 700;
        rawPlot.setRangeBoundaries(0, rangeBoundary, BoundaryMode.FIXED);
        rawPlot.setDomainBoundaries(0, this.plotSize, BoundaryMode.FIXED);

        seriesRaw = new SimpleXYSeries(
                Collections.nCopies(this.plotSize, 0), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "RFduino samples" // title
        );
        LineAndPointFormatter seriesFormatterR = new LineAndPointFormatter(Color.RED, null, null, null);
        rawPlot.addSeries(seriesRaw, seriesFormatterR);
    }


    public void UpdateFFTPlot(float[] fftMags) {

        ArrayList<Float> newSeriesValues = new ArrayList<>();
        for (int i=fftMags.length/2; i<fftMags.length; i++) {
            newSeriesValues.add(fftMags[i]);
        }

        seriesFFT.setModel(newSeriesValues, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
        fftPlot.redraw();
    }

    public void ConfigureFFTPlot(int fftSize) {

        double rangeBoundary = 30;
        fftPlot.setRangeBoundaries(0, rangeBoundary, BoundaryMode.FIXED);
        //fftPlot.setDomainBoundaries(0, plotSize, BoundaryMode.FIXED);

        seriesFFT = new SimpleXYSeries(
                Collections.nCopies(fftSize, 0), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Frequency R" // title
        );
        LineAndPointFormatter seriesFormatter = new LineAndPointFormatter(Color.RED, null, null, null);
        fftPlot.addSeries(seriesFFT, seriesFormatter);
    }
}
