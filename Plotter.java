/*
Plotter.java
Nick Crews
4/7/16
A lot of this code stolen from https://stackoverflow.com/questions/16714738/xy-plotting-with-java
*/
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataItem;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.ChartUtilities;
import java.awt.BasicStroke;
import java.awt.Color;


@SuppressWarnings("serial")
public class Plotter extends ApplicationFrame
    {
    // the data for cities and optimal paths. other path Series are just stored in this.data
    XYSeries cities;
    XYSeries optimal;
   
    // where the series of data live
    XYSeriesCollection data;
    // where the series live
    JFreeChart chart;
    // save the renderer so we can adjust it easily
    XYLineAndShapeRenderer renderer;

    public Plotter(final String title) {

        super(title);

        // make our data series
        // cities should not be autosorted (ie second arg is false)
        // optimal is empty, and we don't display it yet until add_optimal_path() is called
        this.cities = new XYSeries("Cities", false);
        this.optimal = new XYSeries("NULL", false);
        this.data = new XYSeriesCollection();
        this.data.addSeries(this.cities);
        this.data.addSeries(this.optimal);

        // the chart 
        this.chart = ChartFactory.createXYLineChart(
            title,
            "", 
            "", 
            this.data,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );

        // set up the look of the plot
        XYPlot plot = (XYPlot) this.chart.getPlot();
        this.renderer = new XYLineAndShapeRenderer();
        // get rid of gridlines
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);
        // get rid of tickmarks on the axes
        plot.getRangeAxis().setVisible(false);
        plot.getDomainAxis().setVisible(false);

        // cities are red points
        this.renderer.setSeriesLinesVisible(0, false);
        this.renderer.setSeriesShapesVisible(0, true);
        // optimal path is green dashed line, with no dots
        this.renderer.setSeriesShapesVisible(1, false);
        this.renderer.setSeriesPaint(1, Color.RED);
        this.renderer.setSeriesStroke(1, 
            new BasicStroke(
                1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1.0f, new float[] {1.0f, 8.0f}, 0.0f
            ));
        // make the optimal path invisible in the legend. we can change this if we actually add an potimal path
        this.renderer.setSeriesVisibleInLegend(1, false);

        // apply this renderer to the chart
        plot.setRenderer(this.renderer);
            
        
        // package this so it goes on the screen
        ChartPanel chartPanel = new ChartPanel(this.chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(700, 700));
        setContentPane(chartPanel);
        this.pack();          
        RefineryUtilities.centerFrameOnScreen(this);          
        this.setVisible(true);
    }

    /* Add the cities to our plot*/
    public void set_cities(int[][] cities_array)
    {
        this.cities.clear();
        int x, y;
        for (int[] city: cities_array)
        {
            x=city[0];
            y=city[1];
            this.cities.add(x, y);
        }
    }

    /* Add the optimal path to our plot. Optional*/
    public void add_optimal_path(GeneticSolver.Path p)
    {
        int[] pathdata = p.cities;
    
        this.optimal.setKey("Optimal Path of Length " + p.length);
        this.optimal.clear();

        for (int cityindex: pathdata)
        {
            XYDataItem city = this.cities.getDataItem(cityindex);
            int x = (int) city.getXValue();
            int y = (int) city.getYValue();
            this.optimal.add(x,y);
        }
        // and add in the first city again to make a loop
        XYDataItem firstCity = this.cities.getDataItem(pathdata[0]);
        int x = (int) firstCity.getXValue();
        int y = (int) firstCity.getYValue();
        this.optimal.add(x,y);

        // make it so this actually show up
        this.renderer.setSeriesVisibleInLegend(1, true);
    }

    /* Add a path to our plot, labeled with a generation*/
    public void show_path(GeneticSolver.Path p, int gen)
    {
        // remove the old paths
        for (int i=2; i<this.data.getSeriesCount(); i++)
        {
            this.data.removeSeries(i);
        }

        // create a new series, with autosort disabled
        String seriesTitle = gen>=0 ? "Fittest Individual in Generation " + gen + " of Length " + p.length : 
            "Path of Length " + p.length;
        XYSeries path = new XYSeries(seriesTitle, false);

        for (int cityindex: p.cities)
        {
            XYDataItem city = this.cities.getDataItem(cityindex);
            int x = (int) city.getXValue();
            int y = (int) city.getYValue();
            path.add(x,y);
        }
        // and add in the first city again to make a loop
        XYDataItem firstCity = this.cities.getDataItem(p.cities[0]);
        int x = (int) firstCity.getXValue();
        int y = (int) firstCity.getYValue();
        path.add(x,y);

        // add this data series, and make it so it displays with no dots
        this.data.addSeries(path);
        this.renderer.setSeriesShapesVisible(this.data.getSeriesCount()-1, false);
    }

    /*In case you want to add a path with no generation label*/
    public void show_path(GeneticSolver.Path p)
    {
        this.show_path(p, -1);
    }


    /*Saves this plot to a file*/
    public boolean save(String filepath)
    {
        try
        {
            ChartUtilities.saveChartAsPNG(new File(filepath), this.chart, 700, 600);
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }

}