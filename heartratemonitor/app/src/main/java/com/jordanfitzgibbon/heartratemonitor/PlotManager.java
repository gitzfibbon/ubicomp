package com.jordanfitzgibbon.heartratemonitor;

import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class PlotManager {

    private final int RED = 0;
    private final int GREEN = 1;
    private final int BLUE = 2;

    private final int plotSize = 200;

    XYPlot filteredPlot;

    // Plots de-meaned values
//    SimpleXYSeries seriesDeMeanedR;
    SimpleXYSeries seriesDeMeanedG;
//    SimpleXYSeries seriesDeMeanedB;

    // Plots median filtered values
//    SimpleXYSeries seriesMedianR;
    SimpleXYSeries seriesMedianG;
//    SimpleXYSeries seriesMedianB;
//    SimpleXYSeries seriesMedianRGB;

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

        double rangeBoundary = 10;
        fftPlot.setRangeBoundaries(0, rangeBoundary, BoundaryMode.FIXED);
        //fftPlot.setDomainBoundaries(0, plotSize, BoundaryMode.FIXED);

        seriesFFT = new SimpleXYSeries(
                Collections.nCopies(fftSize, 0), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Frequency G" // title
        );
        LineAndPointFormatter seriesFormatter = new LineAndPointFormatter(Color.GREEN, null, null, null);
        fftPlot.addSeries(seriesFFT, seriesFormatter);
    }

    public void UpdateFilteredPlot(
            double meanR, double meanG, double meanB,
            double medianR, double medianG, double medianB) {

        // Remove values from the series. Assume all seriesDeMeanedG are the same size so use seriesDeMeanedG to do this check.
        if (seriesDeMeanedG.size() >=  plotSize) {
//            seriesDeMeanedR.removeFirst();
            seriesDeMeanedG.removeFirst();
//            seriesDeMeanedB.removeFirst();
//            seriesDeMeanedRGB.removeFirst();
//
//            seriesMedianR.removeFirst();
            seriesMedianG.removeFirst();
//            seriesMedianB.removeFirst();
        }

//        seriesDeMeanedR.addLast(null, meanR);
        seriesDeMeanedG.addLast(null, meanG);
//        seriesDeMeanedB.addLast(null, meanB);
//        seriesDeMeanedRGB.addLast(null, (meanR + meanG + meanB) / 3);
//
//        seriesMedianR.addLast(null, medianR);
        seriesMedianG.addLast(null, medianG);
//        seriesMedianB.addLast(null, medianB);

        filteredPlot.redraw();
    }

    // Note: only plot green values
    public void ConfigureFilteredPlot() {

        double rangeBoundary = 10;
        filteredPlot.setRangeBoundaries(-1 * rangeBoundary, rangeBoundary, BoundaryMode.FIXED);
        rawPlot.setDomainBoundaries(0, plotSize, BoundaryMode.FIXED);

        // Median Green
        seriesMedianG = new SimpleXYSeries(
                Collections.nCopies(plotSize, 0), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Median G" // title
        );
        LineAndPointFormatter seriesFormatterMedianG = new LineAndPointFormatter(Color.GREEN, null, null, null);
        filteredPlot.addSeries(seriesMedianG, seriesFormatterMedianG);

        // Mean Green
        seriesDeMeanedG = new SimpleXYSeries(
                Collections.nCopies(plotSize, 0), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "DeMeaned G" // title
        );
        LineAndPointFormatter seriesFormatterMeanG = new LineAndPointFormatter(Color.LTGRAY, null, null, null);
        filteredPlot.addSeries(seriesDeMeanedG, seriesFormatterMeanG);


//        // Red
//        seriesDeMeanedR = new SimpleXYSeries(
//                Collections.nCopies(plotSize, 0), // convert array to a list
//                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
//                "DeMeaned R" // title
//        );
//        LineAndPointFormatter seriesFormatterR = new LineAndPointFormatter(Color.RED, null, null, null);
//        filteredPlot.addSeries(seriesDeMeanedR, seriesFormatterR);
//
//        // Blue
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
