package org.jlab.calib.services;


import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.calib.tasks.CalibrationEngine;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.CalibrationConstantsListener;
import org.jlab.detector.calib.utils.CalibrationConstantsView;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.detector.decode.CodaEventDecoder;
import org.jlab.detector.decode.DetectorDataDgtz;
import org.jlab.detector.decode.DetectorDecoderView;
import org.jlab.detector.decode.DetectorEventDecoder;
import org.jlab.detector.examples.RawEventViewer;
import org.jlab.detector.view.DetectorPane2D;
import org.jlab.detector.view.DetectorShape2D;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
//import org.jlab.calib.temp.DataGroup;
import org.jlab.groot.math.F1D;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataEventType;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.task.DataSourceProcessorPane;
import org.jlab.io.task.IDataEventListener;
import org.jlab.utils.groups.IndexedList;

public class TofLeftRightEventListener extends TOFCalibrationEngine {

	// indices for override values
	public final int LEFTRIGHT_OVERRIDE = 0;

	final double LEFT_RIGHT_RATIO = 0.15;
	final double MAX_LEFTRIGHT = 10.0;

	public TofLeftRightEventListener() {

		stepName = "Left Right";
		histTitle = "LR";
		fileNamePrefix = "FTOF_CALIB_LEFTRIGHT_";
		// get file name here so that each timer update overwrites it
		filename = nextFileName();

		calib = new CalibrationConstants(3,
				"left_right/F");
		calib.setName("/calibration/ftof/timing_offset/left_right");
		calib.setPrecision(3);

		calib.addConstraint(3, -MAX_LEFTRIGHT, MAX_LEFTRIGHT);
		
	}

	@Override
	public void populatePrevCalib() {

		System.out.println("Populating "+stepName+" previous calibration values");
		if (calDBSource==CAL_FILE) {

			System.out.println("File: "+prevCalFilename);
			// read in the left right values from the text file			
			String line = null;
			try { 

				// Open the file
				FileReader fileReader = 
						new FileReader(prevCalFilename);

				// Always wrap FileReader in BufferedReader
				BufferedReader bufferedReader = 
						new BufferedReader(fileReader);            

				line = bufferedReader.readLine();

				while (line != null) {

					String[] lineValues;
					lineValues = line.split(" ");

					int sector = Integer.parseInt(lineValues[0]);
					int layer = Integer.parseInt(lineValues[1]);
					int paddle = Integer.parseInt(lineValues[2]);
					double lr = Double.parseDouble(lineValues[3]);

					leftRightValues.addEntry(sector, layer, paddle);
					leftRightValues.setDoubleValue(lr,
							"left_right", sector, layer, paddle);
					
					line = bufferedReader.readLine();
				}

				bufferedReader.close();            
			}
			catch(FileNotFoundException ex) {
				System.out.println(
						"Unable to open file '" + 
								prevCalFilename + "'");      
				return;
			}
			catch(IOException ex) {
				System.out.println(
						"Error reading file '" 
								+ prevCalFilename + "'");                   
				ex.printStackTrace();
				return;
			}			
		}
		else if (calDBSource==CAL_DEFAULT) {
			System.out.println("Default");
			for (int sector = 1; sector <= 6; sector++) {
				for (int layer = 1; layer <= 3; layer++) {
					int layer_index = layer - 1;
					for (int paddle = 1; paddle <= NUM_PADDLES[layer_index]; paddle++) {
						leftRightValues.addEntry(sector, layer, paddle);
						leftRightValues.setDoubleValue(0.0,
								"left_right", sector, layer, paddle);
						
					}
				}
			}			
		}
		else if (calDBSource==CAL_DB) {
			System.out.println("Database Run No: "+prevCalRunNo);
			DatabaseConstantProvider dcp = new DatabaseConstantProvider(prevCalRunNo, "default");
			leftRightValues = dcp.readConstants("/calibration/ftof/timing_offset");
			dcp.disconnect();
		}
		prevCalRead = true;
		System.out.println(stepName+" previous calibration values populated successfully");
	}

	public void resetEventListener() {

		// perform init processing
		for (int i=0; i<leftRightValues.getRowCount(); i++) {
			String line = new String();
			for (int j=0; j<leftRightValues.getColumnCount(); j++) {
				line = line+leftRightValues.getValueAt(i, j);
				if (j<leftRightValues.getColumnCount()-1) {
					line = line+" ";
				}
			}
			//System.out.println(line);
		}
		
		// create histograms
		for (int sector = 1; sector <= 6; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int layer_index = layer - 1;
				for (int paddle = 1; paddle <= NUM_PADDLES[layer_index]; paddle++) {

					// create all the histograms
					H1F hist = new H1F("left_right",histTitle(sector,layer,paddle),
							2001, -50.05, 50.05);

					// create all the functions
					F1D edgeToEdgeFunc = new F1D("edgeToEdgeFunc","[height]",
							-100.0, 100.0);
					edgeToEdgeFunc.setLineColor(FUNC_COLOUR);
					edgeToEdgeFunc.setLineWidth(FUNC_LINE_WIDTH);

					DataGroup dg = new DataGroup(1,1);
					dg.addDataSet(hist, 0);
					//dg.addDataSet(edgeToEdgeFunc, 0);
					dataGroups.add(dg, sector,layer,paddle);

					setPlotTitle(sector,layer,paddle);

					// initialize the constants array
					Double[] consts = {UNDEFINED_OVERRIDE};
					// override value
					constants.add(consts, sector, layer, paddle);

				}
			}
		}
	}

	@Override
	public void processEvent(DataEvent event) {

		//DataProvider dp = new DataProvider();
		List<TOFPaddle> paddleList = DataProvider.getPaddleList(event);
		processPaddleList(paddleList);
	}

	@Override
	public void processPaddleList(List<TOFPaddle> paddleList) {

		for (TOFPaddle paddle : paddleList) {

			int sector = paddle.getDescriptor().getSector();
			int layer = paddle.getDescriptor().getLayer();
			int component = paddle.getDescriptor().getComponent();

			dataGroups.getItem(sector,layer,component).getH1F("left_right").fill(
					paddle.leftRight());
		}
	}

	@Override
	public void fit(int sector, int layer, int paddle,
			double minRange, double maxRange) {

		H1F leftRightHist = dataGroups.getItem(sector,layer,paddle).getH1F("left_right");

		int nBin = leftRightHist.getXaxis().getNBins();
		int maxBin = leftRightHist.getMaximumBin();

		// calculate the 'average' of all bins
		double averageAllBins=0;
		for(int i=1;i<=nBin;i++)
			averageAllBins+=leftRightHist.getBinContent(i);
		averageAllBins/=nBin;

		// find the first points left and right of max bin with bin content < average
		int lowRangeFirstCut=0,highRangeFirstCut=0;
		for(int i=maxBin;i>=1;i--){
			if(leftRightHist.getBinContent(i)<averageAllBins){
				lowRangeFirstCut=i;
				break;
			}
		}
		for(int i=maxBin;i<=nBin;i++){
			if(leftRightHist.getBinContent(i)<averageAllBins){
				highRangeFirstCut=i;
				break;
			}
		}

		// now calculate the 'average' in this range
		double averageCentralRange=0;
		for(int i=lowRangeFirstCut;i<=highRangeFirstCut;i++)
			averageCentralRange+=leftRightHist.getBinContent(i);
		averageCentralRange/=(highRangeFirstCut-lowRangeFirstCut+1);

		// find the first points left and right of maxBin with bin content < 0.3 * average in the range
		double threshold=averageCentralRange*LEFT_RIGHT_RATIO;
		//if(averageCentralRange<20) return;
		int lowRangeSecondCut=0, highRangeSecondCut=0;
		for(int i=maxBin;i>=1;i--){
			if(leftRightHist.getBinContent(i)<threshold){
				lowRangeSecondCut=i;
				break;
			}
		}
		for(int i=maxBin;i<=nBin;i++){
			if(leftRightHist.getBinContent(i)<threshold){
				highRangeSecondCut=i;
				break;
			}
		}


		// find error
		// find the points left and right of maxBin with bin content < 0.3 * (average + sqrt of average)
		double errorThreshold = (averageCentralRange + Math.sqrt(averageCentralRange))*LEFT_RIGHT_RATIO;
		int lowRangeError=0, highRangeError=0;
		for(int i=maxBin;i>=1;i--){
			if(leftRightHist.getBinContent(i)<errorThreshold){
				lowRangeError=i;
				break;
			}
		}
		for(int i=maxBin;i<=nBin;i++){
			if(leftRightHist.getBinContent(i)<errorThreshold){
				highRangeError=i;
				break;
			}
		}

		// create the function showing the width of the spread
		//F1D edgeToEdgeFunc = dataGroups.getItem(sector,layer,paddle).getF1D("edgeToEdgeFunc");
		//		edgeToEdgeFunc.setRange(leftRightHist.getAxis().getBinCenter(lowRangeSecondCut), 
		//							leftRightHist.getAxis().getBinCenter(highRangeSecondCut));
		//
		//		edgeToEdgeFunc.setParameter(0, averageCentralRange*LEFT_RIGHT_RATIO); // height to draw line at

		// create the function with range = error values
		// not currently used for calibration
		F1D errorFunc = new F1D("p1","[height]",
				leftRightHist.getAxis().getBinCenter(lowRangeError) -
				leftRightHist.getAxis().getBinCenter(lowRangeSecondCut),
				leftRightHist.getAxis().getBinCenter(highRangeError) -
				leftRightHist.getAxis().getBinCenter(highRangeSecondCut));
		errorFunc.setParameter(0, averageCentralRange*LEFT_RIGHT_RATIO); // height to draw line at

	}

	@Override
	public void customFit(int sector, int layer, int paddle){

		//System.out.println("Left right value from file is "+leftRightAdjustment(sector,layer,paddle));

		String[] fields = { "Override centroid:" , "SPACE"};
		TOFCustomFitPanel panel = new TOFCustomFitPanel(fields,sector,layer);

		int result = JOptionPane.showConfirmDialog(null, panel, 
				"Adjust Fit / Override for paddle "+paddle, JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {

			double overrideValue = toDouble(panel.textFields[0].getText());
			
			int minP = paddle;
			int maxP = paddle;
			int minS = sector;
			int maxS = sector;
			if (panel.applyLevel == panel.APPLY_P) {
				//
			}
			else {
				minP = 1;
				maxP = NUM_PADDLES[layer-1];
			}
			if (panel.applyLevel == panel.APPLY_L) {
				minS = 1;
				maxS = 6;
			}

			for (int s=minS; s<=maxS; s++) {
				for (int p=minP; p<=maxP; p++) {
					// save the override values
					Double[] consts = constants.getItem(s, layer, p);
					consts[LEFTRIGHT_OVERRIDE] = overrideValue;

					fit(s, layer, p);

					// update the table
					saveRow(s,layer,p);
				}
			}
			calib.fireTableDataChanged();

		}	 
	}

	public Double getCentroid(int sector, int layer, int paddle) {

		double leftRight = 0.0;
		double overrideVal = constants.getItem(sector, layer, paddle)[LEFTRIGHT_OVERRIDE];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			leftRight = overrideVal;
		}
		else {

			//double min = dataGroups.getItem(sector,layer,paddle).getF1D("edgeToEdgeFunc").getMin(); 
			//double max = dataGroups.getItem(sector,layer,paddle).getF1D("edgeToEdgeFunc").getMax();
			//leftRight = (min+max)/2.0;

			leftRight = dataGroups.getItem(sector,layer,paddle).getH1F("left_right").getMean();

		}

		return leftRight;
	}

	@Override
	public void saveRow(int sector, int layer, int paddle) {
		calib.setDoubleValue(getCentroid(sector,layer,paddle),
				"left_right", sector, layer, paddle);
	}

	@Override
	public boolean isGoodPaddle(int sector, int layer, int paddle) {

		return (getCentroid(sector,layer,paddle) >= -MAX_LEFTRIGHT
				&&
				getCentroid(sector,layer,paddle) <= MAX_LEFTRIGHT);
	}

	@Override
	public void setPlotTitle(int sector, int layer, int paddle) {
		// reset hist title as may have been set to null by show all 
		dataGroups.getItem(sector,layer,paddle).getH1F("left_right").setTitleX("(timeLeft-timeRight) (ns)");
	}

	@Override
	public void drawPlots(int sector, int layer, int paddle, EmbeddedCanvas canvas) {

		H1F hist = dataGroups.getItem(sector,layer,paddle).getH1F("left_right");
		hist.setTitleX("");
		canvas.draw(hist);
		//canvas.draw(dataGroups.getItem(sector,layer,paddle).getF1D("edgeToEdgeFunc"), "same");

	}

	@Override
	public DataGroup getSummary(int sector, int layer) {

		int layer_index = layer-1;
		double[] paddleNumbers = new double[NUM_PADDLES[layer_index]];
		double[] paddleUncs = new double[NUM_PADDLES[layer_index]];
		double[] values = new double[NUM_PADDLES[layer_index]];
		double[] valueUncs = new double[NUM_PADDLES[layer_index]];

		for (int p = 1; p <= NUM_PADDLES[layer_index]; p++) {

			paddleNumbers[p - 1] = (double) p;
			paddleUncs[p - 1] = 0.0;
			values[p - 1] = getCentroid(sector, layer, p);
			valueUncs[p - 1] = 0.0;
		}

		GraphErrors summ = new GraphErrors("summ", paddleNumbers,
				values, paddleUncs, valueUncs);

		summ.setTitleX("Paddle Number");
		summ.setTitleY("Centroid");
		summ.setMarkerSize(MARKER_SIZE);
		summ.setLineThickness(MARKER_LINE_WIDTH);

		DataGroup dg = new DataGroup(1,1);
		dg.addDataSet(summ, 0);
		return dg;

	}
}
