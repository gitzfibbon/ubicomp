package com.jordanfitzgibbon.heartratemonitor;

import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

import java.util.Collections;

public class PlotManager {

    private final int plotSize = 200;

    // Plots de-meaned and median filtered values
    XYPlot filteredPlot;
    SimpleXYSeries seriesDeMeanedR;
    SimpleXYSeries seriesDeMeanedG;
    SimpleXYSeries seriesDeMeanedB;
    SimpleXYSeries seriesDeMeanedRGB;

    // Plots raw RGB data (or means of the full frame)
    XYPlot rawPlot;
    SimpleXYSeries seriesRawR;
    SimpleXYSeries seriesRawG;
    SimpleXYSeries seriesRawB;
    SimpleXYSeries seriesRawRGB;

    public PlotManager(ActionBarActivity parent) {
        this.rawPlot = (XYPlot)parent.findViewById(R.id.rawPlot);
        this.filteredPlot = (XYPlot)parent.findViewById(R.id.filteredPlot);
    }

    public void UpdateFilteredPlot(double r, double g, double b) {

        // Remove values from the series. Assume all seriesDeMeanedR are the same size so use seriesDeMeanedR to do this check.
        if (seriesDeMeanedR.size() >=  plotSize) {
            seriesDeMeanedR.removeFirst();
            seriesDeMeanedG.removeFirst();
            seriesDeMeanedB.removeFirst();
            seriesDeMeanedRGB.removeFirst();
        }

        seriesDeMeanedR.addLast(null, r);
        seriesDeMeanedG.addLast(null, g);
        seriesDeMeanedB.addLast(null, b);
        seriesDeMeanedRGB.addLast(null, (r + g + b) / 3);

        filteredPlot.redraw();
    }

    public void ConfigureFilteredPlot() {

        double rangeBoundary = 10;
        filteredPlot.setRangeBoundaries(-1 * rangeBoundary, rangeBoundary, BoundaryMode.FIXED);
        rawPlot.setDomainBoundaries(0, plotSize, BoundaryMode.FIXED);

        // Red
        seriesDeMeanedR = new SimpleXYSeries(
                Collections.nCopies(plotSize, 0), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "DeMeaned R" // title
        );
        LineAndPointFormatter seriesFormatterR = new LineAndPointFormatter(Color.RED, null, null, null);
        filteredPlot.addSeries(seriesDeMeanedR, seriesFormatterR);

        // Green
        seriesDeMeanedG = new SimpleXYSeries(
                Collections.nCopies(plotSize, 0), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "DeMeaned G" // title
        );
        LineAndPointFormatter seriesFormatterG = new LineAndPointFormatter(Color.GREEN, null, null, null);
        filteredPlot.addSeries(seriesDeMeanedG, seriesFormatterG);

        // Blue
        seriesDeMeanedB = new SimpleXYSeries(
                Collections.nCopies(plotSize, 0), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "DeMeaned B" // title
        );
        LineAndPointFormatter seriesFormatterB = new LineAndPointFormatter(Color.BLUE, null, null, null);
        filteredPlot.addSeries(seriesDeMeanedB, seriesFormatterB);

        // All
        seriesDeMeanedRGB = new SimpleXYSeries(
                Collections.nCopies(plotSize, 0), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "DeMeaned RBG" // title
        );
        LineAndPointFormatter seriesFormatterAll = new LineAndPointFormatter(Color.LTGRAY, null, null, null);
        filteredPlot.addSeries(seriesDeMeanedRGB, seriesFormatterAll);
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
