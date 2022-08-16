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
import javax.swing.JTabbedPane;

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
import org.jlab.groot.fitter.ParallelSliceFitter;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
//import org.jlab.calib.temp.DataGroup;
import org.jlab.groot.math.F1D;
import org.jlab.groot.ui.TCanvas;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataEventType;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.task.DataSourceProcessorPane;
import org.jlab.io.task.IDataEventListener;
import org.jlab.utils.groups.IndexedList;

public class TofTWPosEventListener extends TOFCalibrationEngine {

	public final int TW1_OVERRIDE = 0;
	public final int TW2_OVERRIDE = 1;
	
	private String fitOption = "RQ";
	int backgroundSF = -1;
	boolean showSlices = false;

	public TofTWPosEventListener() {

		stepName = "TW position";
		histTitle = "TWPOS";
		fileNamePrefix = "FTOF_CALIB_TWPOS_";
		// get file name here so that each timer update overwrites it
		filename = nextFileName();

		calib = new CalibrationConstants(3,
				"tw1pos/F:tw2pos/F");
		calib.setName("/calibration/ftof/time_walk_pos");
		calib.setPrecision(7);

		// assign constraints to all paddles

	}
	
	@Override
	public void populatePrevCalib() {

		System.out.println("Populating "+stepName+" previous calibration values");
		if (calDBSource==CAL_FILE) {

			System.out.println("File: "+prevCalFilename);
			// read in the values from the text file			
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
					double tw1pos = Double.parseDouble(lineValues[3]);
					double tw2pos = Double.parseDouble(lineValues[4]);
					
					twposValues.addEntry(sector, layer, paddle);
					twposValues.setDoubleValue(tw1pos,
							"tw1pos", sector, layer, paddle);
					twposValues.setDoubleValue(tw2pos,
							"tw2pos", sector, layer, paddle);
					
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
				return;
			}			
		}
		else if (calDBSource==CAL_DEFAULT) {
			System.out.println("Default");
			for (int sector = 1; sector <= 6; sector++) {
				for (int layer = 1; layer <= 3; layer++) {
					int layer_index = layer - 1;
					for (int paddle = 1; paddle <= NUM_PADDLES[layer_index]; paddle++) {
						twposValues.addEntry(sector, layer, paddle);
						twposValues.setDoubleValue(0.0,
								"tw1pos", sector, layer, paddle);
						twposValues.setDoubleValue(0.0,
								"tw2pos", sector, layer, paddle);
						
					}
				}
			}			
		}
		else if (calDBSource==CAL_DB) {
			System.out.println("Database Run No: "+prevCalRunNo);
			DatabaseConstantProvider dcp = new DatabaseConstantProvider(prevCalRunNo, "default");
			twposValues = dcp.readConstants("/calibration/ftof/time_walk_pos");
			dcp.disconnect();
		}
		prevCalRead = true;
		System.out.println(stepName+" previous calibration values populated successfully");
	}

	@Override
	public void resetEventListener() {

		// perform init processing
    	double bb = BEAM_BUCKET;
		int bins = (int) (bb/2.004)*160;
		
		// LC perform init processing
		for (int sector = 1; sector <= 6; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int layer_index = layer - 1;
				for (int paddle = 1; paddle <= NUM_PADDLES[layer_index]; paddle++) {

					// create all the histograms
					H2F twposHist = 
							new H2F("twposHist",histTitle(sector,layer,paddle),
									100, -paddleLength(sector,layer,paddle)*0.55, paddleLength(sector,layer,paddle)*0.55,
									bins, -bb*0.5, bb*0.5);
					twposHist.setTitleX("hit position (cm)");
					twposHist.setTitleY("delta T (ns)");

					// create all the functions and graphs
	
					//String funcText = "[a]*x*x+[b]*x-("+tw1+"*x*x+"+tw2+"*x+[c])";
					String funcText = "[a]*x*x+[b]*x+[c]";
					//System.out.println("funcTextL "+funcTextL);
					F1D twposFunc = new F1D("twposFunc", funcText, -250.0, 250.0);
					
					GraphErrors twposGraph = new GraphErrors("twposGraph");
					twposGraph.setName("twposGraph");
					twposGraph.setTitle(histTitle(sector,layer,paddle));
					twposFunc.setLineColor(FUNC_COLOUR);
					twposFunc.setLineWidth(FUNC_LINE_WIDTH);
					twposGraph.setMarkerSize(MARKER_SIZE);
					twposGraph.setLineThickness(MARKER_LINE_WIDTH);

					DataGroup dg = new DataGroup(2,1);
					dg.addDataSet(twposHist, 0);
					dg.addDataSet(twposGraph, 1);
					dg.addDataSet(twposFunc, 1);
					dataGroups.add(dg, sector,layer,paddle);

					setPlotTitle(sector,layer,paddle);

					// initialize the constants array
					Double[] consts = {UNDEFINED_OVERRIDE, UNDEFINED_OVERRIDE};
					// override values

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

			if (paddle.goodTrackFound()) {

				dataGroups.getItem(sector,layer,component).getH2F("twposHist").fill(
						 paddle.paddleY(),
						 (paddle.refTimeRFCorr()+(1000*BEAM_BUCKET) + (0.5*BEAM_BUCKET))%BEAM_BUCKET - 0.5*BEAM_BUCKET);
				
			}
		}
	}
	
	@Override
	public void timerUpdate() {
		if (fitMethod!=FIT_METHOD_SF) {
			// only analyze at end of file for slice fitter - takes too long
			analyze();
		}
		save();
		calib.fireTableDataChanged();
	}	

	@Override
	public void fit(int sector, int layer, int paddle,
			double minRange, double maxRange) {

		H2F twposHist = dataGroups.getItem(sector,layer,paddle).getH2F("twposHist");

		// find the range for the fit
		double lowLimit;
		double highLimit;

		if (minRange != UNDEFINED_OVERRIDE) {
			// use custom values for fit
			lowLimit = minRange;
		}
		else {
			lowLimit = paddleLength(sector,layer,paddle) * -0.42;
		}

		if (maxRange != UNDEFINED_OVERRIDE) {
			// use custom values for fit
			highLimit = maxRange;
		}
		else {
			highLimit = paddleLength(sector,layer,paddle) * 0.42;
		}

		// fit function to the graph of means
		GraphErrors twposGraph = (GraphErrors) dataGroups.getItem(sector,layer,paddle).getData("twposGraph");
		
		if (fitMethod==FIT_METHOD_SF) {
			
			ParallelSliceFitter psfL = new ParallelSliceFitter(twposHist);
			psfL.setFitMode(fitMode);
			psfL.setMinEvents(fitMinEvents);
			psfL.setBackgroundOrder(backgroundSF);
			psfL.setNthreads(1);
			setOutput(false);
			psfL.fitSlicesX();
			setOutput(true);
			if (showSlices) {
				psfL.inspectFits();
				showSlices = false;
			}
			fitSliceMaxError = 2.0;
			twposGraph.copy(fixGraph(psfL.getMeanSlices(),"twposGraph"));
		}
		else if (fitMethod==FIT_METHOD_MAX) {
			maxGraphError = 0.3;
			twposGraph.copy(maxGraph(twposHist, "twposGraph"));
		}
		else {
			twposGraph.copy(twposHist.getProfileX());
		}
				
		F1D twposFunc = dataGroups.getItem(sector,layer,paddle).getF1D("twposFunc");
		twposFunc.setRange(lowLimit, highLimit);

		twposFunc.setParameter(0, 0.0);
		twposFunc.setParameter(1, 0.0);
		twposFunc.setParameter(2, 0.0);

		try {
			DataFitter.fit(twposFunc, twposGraph, fitOption);

		} catch (Exception e) {
			System.out.println("Fit error with sector "+sector+" layer "+layer+" paddle "+paddle);
			e.printStackTrace();
		}
		
		// LC Mar 2020 Set function parameters to override value	
		Double[] consts = constants.getItem(sector, layer, paddle);
		if (consts[TW1_OVERRIDE] != UNDEFINED_OVERRIDE) {
			twposFunc.setParameter(0, consts[TW1_OVERRIDE]);
		}
		if (consts[TW2_OVERRIDE] != UNDEFINED_OVERRIDE) {
			twposFunc.setParameter(1, consts[TW2_OVERRIDE]);
		}
	}

	public void customFit(int sector, int layer, int paddle){

		String[] fields = { "Min range for fit:", "Max range for fit:", "SPACE",
				"Min Events per slice:", "Background order for slicefitter(-1=no background, 0=p0 etc):","SPACE",
				"Override tw1:", "Override tw2:"};

		TOFCustomFitPanel panel = new TOFCustomFitPanel(fields,sector,layer, TOFCustomFitPanel.USE_RANGE_Y);

		int result = JOptionPane.showConfirmDialog(null, panel, 
				"Adjust Fit / Override for paddle "+paddle, JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {

			double minRange = toDouble(panel.textFields[0].getText());
			double maxRange = toDouble(panel.textFields[1].getText());
			if (panel.textFields[2].getText().compareTo("") !=0) {
				fitMinEvents = Integer.parseInt(panel.textFields[2].getText());
			}
			if (panel.textFields[3].getText().compareTo("") !=0) {
				backgroundSF = Integer.parseInt(panel.textFields[3].getText());
			}
			
			double overrideTW1 = toDouble(panel.textFields[4].getText());
			double overrideTW2 = toDouble(panel.textFields[5].getText());
			
			
			int minP = paddle;
			int maxP = paddle;
			int minL = layer;
			int maxL = layer;
			int minS = sector;
			int maxS = sector;
			if (panel.applyLevel == panel.APPLY_R) {
				if (panel.minS.getText().compareTo("") !=0) {
					minS =Integer.parseInt(panel.minS.getText());
				}
				if (panel.maxS.getText().compareTo("") !=0) {
					maxS =Integer.parseInt(panel.maxS.getText()); 
				}
				if (panel.minL.getText().compareTo("") !=0) {
					minL =Integer.parseInt(panel.minL.getText());
				}
				if (panel.maxL.getText().compareTo("") !=0) {
					maxL =Integer.parseInt(panel.maxL.getText()); 
				}
				if (panel.minP.getText().compareTo("") !=0) {
					minP =Integer.parseInt(panel.minP.getText());
				}
				if (panel.maxP.getText().compareTo("") !=0) {
					maxP =Integer.parseInt(panel.maxP.getText()); 
				}
			}
			else {
				if (panel.applyLevel == panel.APPLY_P) {
					// if fitting one paddle then show inspectFits view
					showSlices = true;
				}
				else {
					minP = 1;
					maxP = NUM_PADDLES[layer-1];
				}
				if (panel.applyLevel == panel.APPLY_L) {
					minS = 1;
					maxS = 6;
				}
			}
			
			System.out.println("minS "+minS+
					"maxS "+maxS+
					"minL "+minL+
					"maxL "+maxL+
					"minP "+minP+
					"maxP "+maxP
					);
	
			for (int s=minS; s<=maxS; s++) {
				for (int l=minL; l<=maxL; l++) {
					for (int p=minP; p<=maxP; p++) {
						// save the override values
						Double[] consts = constants.getItem(s, l, p);
						consts[TW1_OVERRIDE] = overrideTW1;
						consts[TW2_OVERRIDE] = overrideTW2;
	
						fit(s, l, p, minRange, maxRange);
	
						// update the table
						saveRow(s,l,p);
					}
				}
			}
			calib.fireTableDataChanged();

		}	 
	}


	public Double getTW1(int sector, int layer, int paddle) {

		double tw1 = 0.0;
		double overrideVal = constants.getItem(sector, layer, paddle)[TW1_OVERRIDE];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			tw1 = overrideVal;
		}
		else {
			tw1 = dataGroups.getItem(sector,layer,paddle).getF1D("twposFunc").getParameter(0);
		}
		return tw1;
	}

	public Double getTW2(int sector, int layer, int paddle) {

		double tw2 = 0.0;
		double overrideVal = constants.getItem(sector, layer, paddle)[TW2_OVERRIDE];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			tw2 = overrideVal;
		}
		else {
			tw2 = dataGroups.getItem(sector,layer,paddle).getF1D("twposFunc").getParameter(1);
		}
		return tw2;
	}
	
	@Override
	public void saveRow(int sector, int layer, int paddle) {
		calib.setDoubleValue(getTW1(sector,layer,paddle),
				"tw1pos", sector, layer, paddle);
		calib.setDoubleValue(getTW2(sector,layer,paddle),
				"tw2pos", sector, layer, paddle);

	}

	@Override
	public boolean isGoodPaddle(int sector, int layer, int paddle) {

		return true;

	}

	@Override
	public void setPlotTitle(int sector, int layer, int paddle) {
		// reset hist title as may have been set to null by show all 
	}
	
	@Override
	public void drawPlots(int sector, int layer, int paddle, EmbeddedCanvas canvas) {

		H2F hist = dataGroups.getItem(sector,layer,paddle).getH2F("twposHist");
		F1D func = dataGroups.getItem(sector,layer,paddle).getF1D("twposFunc");

		canvas.draw(hist);    
		canvas.draw(func, "same");
	}
	
	@Override
	public DataGroup getSummary(int sector, int layer) {
		
		int layer_index = layer-1;
		double[] paddleNumbers = new double[NUM_PADDLES[layer_index]];
		double[] paddleUncs = new double[NUM_PADDLES[layer_index]];
		double[] tw1s = new double[NUM_PADDLES[layer_index]];
		double[] zeroUncs = new double[NUM_PADDLES[layer_index]];
		double[] tw2s = new double[NUM_PADDLES[layer_index]];

		for (int p = 1; p <= NUM_PADDLES[layer_index]; p++) {

			paddleNumbers[p - 1] = (double) p;
			paddleUncs[p - 1] = 0.0;
			tw1s[p - 1] = getTW1(sector, layer, p);
			tw2s[p - 1] = getTW2(sector, layer, p);
			zeroUncs[p - 1] = 0.0;
		}

		GraphErrors tw1Summ = new GraphErrors("tw1Summ", paddleNumbers,
				tw1s, paddleUncs, zeroUncs);

		tw1Summ.setTitleX("Paddle Number");
		tw1Summ.setTitleY("TW1");
		tw1Summ.setMarkerSize(MARKER_SIZE);
		tw1Summ.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors tw2Summ = new GraphErrors("tw2Summ", paddleNumbers,
				tw2s, paddleUncs, zeroUncs);

		tw2Summ.setTitleX("Paddle Number");
		tw2Summ.setTitleY("TW2");
		tw2Summ.setMarkerSize(MARKER_SIZE);
		tw2Summ.setLineThickness(MARKER_LINE_WIDTH);

		DataGroup dg = new DataGroup(2,1);
		dg.addDataSet(tw1Summ, 0);
		dg.addDataSet(tw2Summ, 1);

		return dg;

	}

//	Think setAutoScale in the Engine class is enough
//    @Override
//	public void rescaleGraphs(EmbeddedCanvas canvas, int sector, int layer, int paddle) {
//    	
//    	canvas.getPad(1).setAxisRange(-paddleLength(sector,layer,paddle)*0.55, paddleLength(sector,layer,paddle)*0.55,
//    			-BEAM_BUCKET*0.5, BEAM_BUCKET*0.5);
//    	
//	}	
	
}
