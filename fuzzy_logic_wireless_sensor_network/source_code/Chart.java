import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class Chart {
	public Chart(ArrayList<Integer> energy) throws IOException {
		// create a dataset...
		XYSeries data = new XYSeries("Energy Consumption");
		for (int i = 0; i < energy.size(); i++) {
			data.add(i + 1,energy.get(i));
		}
		
        //Add XYSeries to XYSeriesCollection
        XYSeriesCollection my_data_series= new XYSeriesCollection();
        
        //add series using addSeries method
        my_data_series.addSeries(data);
		
        JFreeChart XYLineChart=ChartFactory.createXYLineChart("Energy Left Analysis","Cycle","Total Energy Left",my_data_series,PlotOrientation.VERTICAL,true,true,false);
        
         //Write line chart to a file             
         int width=640; /* Width of the image */
         int height=480; /* Height of the image */                
         SimpleDateFormat dateFormat = new SimpleDateFormat("ssSSS");
 		 String timestamp = dateFormat.format(new Date());
         File XYlineChart=new File("energy_chart_" + timestamp + ".png");              
         ChartUtilities.saveChartAsPNG(XYlineChart,XYLineChart,width,height); 		
	}
}
