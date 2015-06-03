package com.jordanfitzgibbon.rfduinohrm;

import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;

import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

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
    SimpleXYSeries seriesZeroCrossThreshold;
    SimpleXYSeries seriesPeaks;

    public PlotManager(ActionBarActivity parent) {
        this.filteredPlot = (XYPlot)parent.findViewById(R.id.filteredPlot);
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

//    public void UpdatePeakDetectionLine()
//    {
//        // Draws the threshold line for peak detection
//        List<Double> peakDetectionThresholdNumbers = Collections.nCopies(this.plotSize, (double)HeartRateMonitor.PEAK_DETECTION_THRESHOLD);
//        seriesPeakDetectionThreshold = new SimpleXYSeries(
//                peakDetectionThresholdNumbers,
//                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
//                "Peak Cross " + HeartRateMonitor.PEAK_DETECTION_THRESHOLD // series title
//        );
//        LineAndPointFormatter peakDetectionThresholdFormatter = new LineAndPointFormatter(Color.WHITE, null, null, null);
//        filteredPlot.redraw();
//    }

    public void ConfigureFilteredPlot() {

        double rangeBoundary = 125;
        filteredPlot.setRangeBoundaries(-1 * rangeBoundary, rangeBoundary, BoundaryMode.FIXED);
        filteredPlot.setDomainBoundaries(0, plotSize, BoundaryMode.FIXED);

        // DeMeaned
        seriesDeMeaned = new SimpleXYSeries(
                Collections.nCopies(plotSize, 0), // fill the series with 0
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "DeMeaned" // title
        );
        LineAndPointFormatter seriesFormatterDemeaned = new LineAndPointFormatter(Color.RED, null, null, null);
        filteredPlot.addSeries(seriesDeMeaned, seriesFormatterDemeaned);

        // Draws peaks
        List<Double> peakDetectionDotNumbers = Collections.nCopies(this.plotSize,1000.0); // Set to some high value that will be off the chart
        seriesPeaks = new SimpleXYSeries(
                peakDetectionDotNumbers,
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Peaks" // series title
        );
        LineAndPointFormatter peakFormatter = new LineAndPointFormatter(null, Color.WHITE, null, null);
        peakFormatter.getVertexPaint().setStrokeWidth(PixelUtils.dpToPix(5));
        filteredPlot.addSeries(seriesPeaks, peakFormatter);

        // Draws the threshold line for zero crossing
        List<Double> zeroCrossThresholdNumbers = Collections.nCopies(this.plotSize,HeartRateMonitor.ZERO_CROSS_THRESHOLD);
        seriesZeroCrossThreshold = new SimpleXYSeries(
                zeroCrossThresholdNumbers,
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Zero Cross " + HeartRateMonitor.ZERO_CROSS_THRESHOLD // series title
        );
        LineAndPointFormatter zeroCrossThresholdFormatter = new LineAndPointFormatter(Color.WHITE, null, null, null);
        filteredPlot.addSeries(seriesZeroCrossThreshold, zeroCrossThresholdFormatter);

        // Draws the threshold line for peak detection
        List<Double> peakDetectionThresholdNumbers = Collections.nCopies(this.plotSize, (double)HeartRateMonitor.PEAK_DETECTION_THRESHOLD);
        seriesPeakDetectionThreshold = new SimpleXYSeries(
                peakDetectionThresholdNumbers,
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // use array indices as x values and array values as y values
                "Peak Cross " + HeartRateMonitor.PEAK_DETECTION_THRESHOLD // series title
        );
        LineAndPointFormatter peakDetectionThresholdFormatter = new LineAndPointFormatter(Color.WHITE, null, null, null);
        filteredPlot.addSeries(seriesPeakDetectionThreshold, peakDetectionThresholdFormatter);

    }

}
