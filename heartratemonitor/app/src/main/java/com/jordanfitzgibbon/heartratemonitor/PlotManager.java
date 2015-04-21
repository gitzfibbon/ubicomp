package com.jordanfitzgibbon.heartratemonitor;

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

    private final int RED = 0;
    private final int GREEN = 1;
    private final int BLUE = 2;

    private final int plotSize = 200;

    XYPlot filteredPlot;

    // Plots de-meaned values
    SimpleXYSeries seriesDeMeanedR;
//    SimpleXYSeries seriesDeMeanedG;
//    SimpleXYSeries seriesDeMeanedB;

    // Plots median filtered values
    SimpleXYSeries seriesMedianR;
//    SimpleXYSeries seriesMedianG;
//    SimpleXYSeries seriesMedianB;
//    SimpleXYSeries seriesMedianRGB;

    // Used for peak detection
    SimpleXYSeries seriesPeakDetectionThreshold;
    SimpleXYSeries seriesPeaks;

    // Plots raw RGB data (or means of the full frame)
    XYPlot rawPlot;
    SimpleXYSeries seriesRawR;
    SimpleXYSeries seriesRawG;
    SimpleXYSeries seriesRawB;
    SimpleXYSeries seriesRawRGB;

    XYPlot fftPlot;
    SimpleXYSeries seriesFFT;

    public PlotManager(ActionBarActivity parent) {
        this.rawPlot = (XYPlot)parent.findViewById(R.id.rawPlot);
        this.filteredPlot = (XYPlot)parent.findViewById(R.id.filteredPlot);
        this.fftPlot = (XYPlot)parent.findViewById(R.id.fftPlot);
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

    public void UpdateFilteredPlot(
            double meanR, double meanG, double meanB,
            double medianR, double medianG, double medianB,
            boolean isPeak) {

        // Remove values from the series. Assume all seriesDeMeaned are the same size so use seriesDeMeaned to do this check.
        if (seriesDeMeanedR.size() >= plotSize) {
            seriesDeMeanedR.removeFirst();
//            seriesDeMeanedG.removeFirst();
//            seriesDeMeanedB.removeFirst();
//            seriesDeMeanedRGB.removeFirst();
//
            seriesMedianR.removeFirst();
//            seriesMedianG.removeFirst();
//            seriesMedianB.removeFirst();

            seriesPeaks.removeFirst();
        }

        seriesDeMeanedR.addLast(null, meanR);
//        seriesDeMeanedG.addLast(null, meanG);
//        seriesDeMeanedB.addLast(null, meanB);
//        seriesDeMeanedRGB.addLast(null, (meanR + meanG + meanB) / 3);
//
        seriesMedianR.addLast(null, medianR);
//        seriesMedianG.addLast(null, medianG);
//        seriesMedianB.addLast(null, medianB);

        if (isPeak) {
            seriesPeaks.setY(0, this.plotSize - 2);
        } else {
            seriesPeaks.setY(100, this.plotSize - 2);
        }
        seriesPeaks.addLast(null, 100);

        filteredPlot.redraw();
    }

    // Note: only plot green values
    public void ConfigureFilteredPlot() {

        double rangeBoundary = 4;
        filteredPlot.setRangeBoundaries(-1 * rangeBoundary, rangeBoundary, BoundaryMode.FIXED);
        rawPlot.setDomainBoundaries(0, plotSize, BoundaryMode.FIXED);

//        // Median Green
//        seriesMedianG = new SimpleXYSeries(
//                Collections.nCopies(plotSize, 0), // convert array to a list
//                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
//                "Median G" // title
//        );
//        LineAndPointFormatter seriesFormatterMedianG = new LineAndPointFormatter(Color.GREEN, null, null, null);
//        filteredPlot.addSeries(seriesMedianG, seriesFormatterMedianG);

//        // Mean Green
//        seriesDeMeanedG = new SimpleXYSeries(
//                Collections.nCopies(plotSize, 0), // convert array to a list
//                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
//                "DeMeaned G" // title
//        );
//        LineAndPointFormatter seriesFormatterMeanG = new LineAndPointFormatter(Color.LTGRAY, null, null, null);
//        filteredPlot.addSeries(seriesDeMeanedG, seriesFormatterMeanG);

        // Median Red
        seriesMedianR = new SimpleXYSeries(
                Collections.nCopies(plotSize, 0), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Median R" // title
        );
        LineAndPointFormatter seriesFormatterMedianR = new LineAndPointFormatter(Color.RED, null, null, null);
        filteredPlot.addSeries(seriesMedianR, seriesFormatterMedianR);

        // Mean Red
        seriesDeMeanedR = new SimpleXYSeries(
                Collections.nCopies(plotSize, 0), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "DeMeaned R" // title
        );
        LineAndPointFormatter seriesFormatterR = new LineAndPointFormatter(Color.LTGRAY, null, null, null);
        filteredPlot.addSeries(seriesDeMeanedR, seriesFormatterR);
//
//        // Mean Blue
//        seriesDeMeanedB = new SimpleXYSeries(
//                Collections.nCopies(plotSize, 0), // convert array to a list
//                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
//                "DeMeaned B" // title
//        );
//        LineAndPointFormatter seriesFormatterB = new LineAndPointFormatter(Color.BLUE, null, null, null);
//        filteredPlot.addSeries(seriesDeMeanedB, seriesFormatterB);
//
//        // All
//        seriesDeMeanedRGB = new SimpleXYSeries(
//                Collections.nCopies(plotSize, 0), // convert array to a list
//                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
//                "DeMeaned RBG" // title
//        );
//        LineAndPointFormatter seriesFormatterAll = new LineAndPointFormatter(Color.LTGRAY, null, null, null);
//        filteredPlot.addSeries(seriesDeMeanedRGB, seriesFormatterAll);

        List<Double> thresholdNumbers = Collections.nCopies(this.plotSize,HeartRateMonitor.PEAK_DETECTION_THRESHOLD);
        seriesPeakDetectionThreshold = new SimpleXYSeries(
                thresholdNumbers,
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Threshold " + HeartRateMonitor.PEAK_DETECTION_THRESHOLD // series title
        );
        LineAndPointFormatter thresholdFormatter = new LineAndPointFormatter(Color.WHITE, null, null, null);
        filteredPlot.addSeries(seriesPeakDetectionThreshold, thresholdFormatter);

        List<Double> peakDetectionDotNumbers = Collections.nCopies(this.plotSize,1000.0); // Set to some high value that will be off the chart
        seriesPeaks = new SimpleXYSeries(
                peakDetectionDotNumbers,
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Peaks" + HeartRateMonitor.PEAK_DETECTION_THRESHOLD // series title
        );
        LineAndPointFormatter peakFormatter = new LineAndPointFormatter(null, Color.WHITE, null, null);
        peakFormatter.getVertexPaint().setStrokeWidth(PixelUtils.dpToPix(5));
        filteredPlot.addSeries(seriesPeaks, peakFormatter);
    }



    public void UpdateRawPlot(double r, double g, double b) {

        // Remove values from the series. Assume all seriesRawR are the same size so use seriesRawR to do this check.
        if (seriesRawR.size() >=  plotSize) {
            seriesRawR.removeFirst();
            seriesRawG.removeFirst();
            seriesRawB.removeFirst();
            seriesRawRGB.removeFirst();
        }

        seriesRawR.addLast(null, r);
        seriesRawG.addLast(null, g);
        seriesRawB.addLast(null, b);
        seriesRawRGB.addLast(null, (r + g + b) / 3);

        rawPlot.redraw();
    }

    public void ConfigureRawPlot() {

        double rangeBoundary = 255;
        rawPlot.setRangeBoundaries(0, rangeBoundary, BoundaryMode.FIXED);

        rawPlot.setDomainBoundaries(0, plotSize, BoundaryMode.FIXED);

        // Red
        seriesRawR = new SimpleXYSeries(
                Collections.nCopies(plotSize, 0), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Mean R" // title
        );
        LineAndPointFormatter seriesFormatterR = new LineAndPointFormatter(Color.RED, null, null, null);
        rawPlot.addSeries(seriesRawR, seriesFormatterR);

        // Green
        seriesRawG = new SimpleXYSeries(
                Collections.nCopies(plotSize, 0), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Mean G" // title
        );
        LineAndPointFormatter seriesFormatterG = new LineAndPointFormatter(Color.GREEN, null, null, null);
        rawPlot.addSeries(seriesRawG, seriesFormatterG);

        // Blue
        seriesRawB = new SimpleXYSeries(
                Collections.nCopies(plotSize, 0), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Mean B" // title
        );
        LineAndPointFormatter seriesFormatterB = new LineAndPointFormatter(Color.BLUE, null, null, null);
        rawPlot.addSeries(seriesRawB, seriesFormatterB);

        // All
        seriesRawRGB = new SimpleXYSeries(
                Collections.nCopies(plotSize, 0), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Mean RBG" // title
        );
        LineAndPointFormatter seriesFormatterAll = new LineAndPointFormatter(Color.LTGRAY, null, null, null);
        rawPlot.addSeries(seriesRawRGB, seriesFormatterAll);
    }


}
