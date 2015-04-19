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

    XYPlot plot;
    SimpleXYSeries seriesR;
    SimpleXYSeries seriesG;
    SimpleXYSeries seriesB;
    SimpleXYSeries seriesAll;

    public PlotManager(ActionBarActivity parent) {
        this.plot = (XYPlot)parent.findViewById(R.id.fooPlot);
    }

    public void UpdatePlots(double r, double g, double b) {

        // Remove values from the series. Assume all seriesR are the same size so use seriesR to do this check.
        if (seriesR.size() >=  plotSize) {
            seriesR.removeFirst();
            seriesG.removeFirst();
            seriesB.removeFirst();
            seriesAll.removeFirst();

        }

        seriesR.addLast(null, r);
        seriesG.addLast(null, g);
        seriesB.addLast(null, b);
        seriesAll.addLast(null, (r + g + b) / 3);

        plot.redraw();
    }

    public void ConfigurePlots() {

        double rangeBoundary = 255;
//        plot.setRangeBoundaries(-1 * rangeBoundary, rangeBoundary, BoundaryMode.FIXED);
        plot.setRangeBoundaries(0, rangeBoundary, BoundaryMode.FIXED);

        plot.setDomainBoundaries(0, plotSize, BoundaryMode.FIXED);

        // Red
        seriesR = new SimpleXYSeries(
                Collections.nCopies(plotSize, 0), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Mean R" // title
        );
        LineAndPointFormatter seriesFormatterR = new LineAndPointFormatter(Color.RED, null, null, null);
        plot.addSeries(seriesR, seriesFormatterR);

        // Green
        seriesG = new SimpleXYSeries(
                Collections.nCopies(plotSize, 0), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Mean G" // title
        );
        LineAndPointFormatter seriesFormatterG = new LineAndPointFormatter(Color.GREEN, null, null, null);
        plot.addSeries(seriesG, seriesFormatterG);

        // Blue
        seriesB = new SimpleXYSeries(
                Collections.nCopies(plotSize, 0), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Mean B" // title
        );
        LineAndPointFormatter seriesFormatterB = new LineAndPointFormatter(Color.BLUE, null, null, null);
        plot.addSeries(seriesB, seriesFormatterB);

        // All
        seriesAll = new SimpleXYSeries(
                Collections.nCopies(plotSize, 0), // convert array to a list
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Mean RBG" // title
        );
        LineAndPointFormatter seriesFormatterAll = new LineAndPointFormatter(Color.LTGRAY, null, null, null);
        plot.addSeries(seriesAll, seriesFormatterAll);
    }


}
