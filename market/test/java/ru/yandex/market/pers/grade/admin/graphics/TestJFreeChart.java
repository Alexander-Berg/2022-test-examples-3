package ru.yandex.market.pers.grade.admin.graphics;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.Month;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * @author Vladimir Gorovoy vgorovoy@yandex-team.ru
 *         An example of a time series chart.  For the most part, default settings are used, except that
 *         the renderer is modified to show filled shapes (as well as lines) at each data point.
 */
public class TestJFreeChart {
    public static byte[] createTestChart() {
        // create a title...
        final String chartTitle = "Статистика сервиса \"Любимый магазин\"";
        final XYDataset dataset = createDataset1();

        final JFreeChart chart = ChartFactory.createTimeSeriesChart(
            chartTitle,
            "Дата",
            "Количество оценок",
            dataset,
            true,
            true,
            false
        );

        final XYPlot plot = chart.getXYPlot();
        final NumberAxis axis2 = new NumberAxis("Средняя оценка");
        axis2.setAutoRangeIncludesZero(false);
        plot.setRangeAxis(1, axis2);
        plot.setDataset(1, createDataset2());
        plot.mapDatasetToRangeAxis(1, 1);
        final XYItemRenderer renderer = plot.getRenderer();
        renderer.setToolTipGenerator(StandardXYToolTipGenerator.getTimeSeriesInstance());
        if (renderer instanceof StandardXYItemRenderer) {
            final StandardXYItemRenderer rr = (StandardXYItemRenderer) renderer;
            rr.setShapesFilled(true);
        }

        final StandardXYItemRenderer renderer2 = new StandardXYItemRenderer();
        renderer2.setSeriesPaint(0, Color.black);
        renderer.setToolTipGenerator(StandardXYToolTipGenerator.getTimeSeriesInstance());
        plot.setRenderer(1, renderer2);

        final DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("MMM-yyyy"));

        final ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(500, 270));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            //ChartUtilities.saveChartAsJPEG(new File("d:\\chart.jpg"), chart, 500, 300);
            ChartUtilities.writeChartAsJPEG(outputStream, chart, 500, 300);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return outputStream.toByteArray();
    }

    /**
     * Creates a sample dataset.
     *
     * @return The dataset.
     */
    private static XYDataset createDataset1() {

        final TimeSeries s1 = new TimeSeries("Количество оценок", Month.class);
        s1.add(new Month(2, 2001), 181.8);
        s1.add(new Month(3, 2001), 167.3);
        s1.add(new Month(4, 2001), 153.8);
        s1.add(new Month(5, 2001), 167.6);
        s1.add(new Month(6, 2001), 158.8);
        s1.add(new Month(7, 2001), 148.3);
        s1.add(new Month(8, 2001), 153.9);
        s1.add(new Month(9, 2001), 142.7);
        s1.add(new Month(10, 2001), 123.2);
        s1.add(new Month(11, 2001), 131.8);
        s1.add(new Month(12, 2001), 139.6);
        s1.add(new Month(1, 2002), 142.9);
        s1.add(new Month(2, 2002), 138.7);
        s1.add(new Month(3, 2002), 137.3);
        s1.add(new Month(4, 2002), 143.9);
        s1.add(new Month(5, 2002), 139.8);
        s1.add(new Month(6, 2002), 137.0);
        s1.add(new Month(7, 2002), 132.8);

        final TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(s1);

        return dataset;

    }

    /**
     * Creates a sample dataset.
     *
     * @return The dataset.
     */
    private static XYDataset createDataset2() {


        final TimeSeries s2 = new TimeSeries("Средняя оценка", Month.class);
        s2.add(new Month(2, 2001), 429.6);
        s2.add(new Month(3, 2001), 323.2);
        s2.add(new Month(4, 2001), 417.2);
        s2.add(new Month(5, 2001), 624.1);
        s2.add(new Month(6, 2001), 422.6);
        s2.add(new Month(7, 2001), 619.2);
        s2.add(new Month(8, 2001), 416.5);
        s2.add(new Month(9, 2001), 512.7);
        s2.add(new Month(10, 2001), 501.5);
        s2.add(new Month(11, 2001), 306.1);
        s2.add(new Month(12, 2001), 410.3);
        s2.add(new Month(1, 2002), 511.7);
        s2.add(new Month(2, 2002), 611.0);
        s2.add(new Month(3, 2002), 709.6);
        s2.add(new Month(4, 2002), 613.2);
        s2.add(new Month(5, 2002), 711.6);
        s2.add(new Month(6, 2002), 708.8);
        s2.add(new Month(7, 2002), 501.6);

        final TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(s2);

        return dataset;

    }
}
