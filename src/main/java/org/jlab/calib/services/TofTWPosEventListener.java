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

	public final int TW1L_OVERRIDE = 0;
	public final int TW2L_OVERRIDE = 1;
	public final int TW1R_OVERRIDE = 2;
	public final int TW2R_OVERRIDE = 3;
	
	private String fitOption = "RQ";
	int backgroundSF = -1;
	boolean showSlices = false;
	private String showPlotType = "TWPOS_LEFT";

	public TofTWPosEventListener() {

		stepName = "TW position";
		histTitle = "TWPOS";
		fileNamePrefix = "FTOF_CALIB_TWPOS_";
		// get file name here so that each timer update overwrites it
		filename = nextFileName();

		calib = new CalibrationConstants(3,
				"tw1_left/F:tw2_left/F:tw1_right/F:tw2_right/F");
		calib.setName("/calibration/ftof/time_walk/position");
		calib.setPrecision(5);

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
					double tw1L = Double.parseDouble(lineValues[3]);
					double tw2L = Double.parseDouble(lineValues[4]);
					double tw1R = Double.parseDouble(lineValues[5]);
					double tw2R = Double.parseDouble(lineValues[6]);
					
					twposValues.addEntry(sector, layer, paddle);
					twposValues.setDoubleValue(tw1L,
							"tw1_left", sector, layer, paddle);
					twposValues.setDoubleValue(tw2L,
							"tw2_left", sector, layer, paddle);
					twposValues.setDoubleValue(tw1R,
							"tw1_right", sector, layer, paddle);
					twposValues.setDoubleValue(tw2R,
							"tw2_right", sector, layer, paddle);
					
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
								"tw1_left", sector, layer, paddle);
						twposValues.setDoubleValue(0.0,
								"tw2_left", sector, layer, paddle);
						twposValues.setDoubleValue(0.0,
								"tw1_right", sector, layer, paddle);
						twposValues.setDoubleValue(0.0,
								"tw2_right", sector, layer, paddle);
						
					}
				}
			}			
		}
		else if (calDBSource==CAL_DB) {
			System.out.println("Database Run No: "+prevCalRunNo);
			DatabaseConstantProvider dcp = new DatabaseConstantProvider(prevCalRunNo, "default");
			twposValues = dcp.readConstants("/calibration/ftof/time_walk");
			dcp.disconnect();
		}
		prevCalRead = true;
		System.out.println(stepName+" previous calibration values populated successfully");
	}

	@Override
	public void resetEventListener() {

		// perform init processing
		
		// LC perform init processing
		for (int sector = 1; sector <= 6; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int layer_index = layer - 1;
				for (int paddle = 1; paddle <= NUM_PADDLES[layer_index]; paddle++) {

					// create all the histograms
					H2F twposLHist = 
							new H2F("twposLHist",histTitle(sector,layer,paddle),
									100, -paddleLength(sector,layer,paddle)*0.55, paddleLength(sector,layer,paddle)*0.55,
									160, -2.0, 2.0);
					twposLHist.setTitleX("hit position (cm)");
					twposLHist.setTitleY("delta T (ns)");

					H2F twposRHist = 
							new H2F("twposRHist",histTitle(sector,layer,paddle),
									100, -paddleLength(sector,layer,paddle)*0.55, paddleLength(sector,layer,paddle)*0.55,
									160, -2.0, 2.0);
					twposRHist.setTitleX("hit position (cm)");
					twposRHist.setTitleY("delta T (ns)");

					// create all the functions and graphs
					double tw1L = TOFCalibrationEngine.twposValues.getDoubleValue("tw1_left", sector, layer, paddle);
					double tw2L = TOFCalibrationEngine.twposValues.getDoubleValue("tw2_left", sector, layer, paddle);
					String funcTextL = "[a]*x*x+[b]*x-("+tw1L+"*x*x+"+tw2L+"*x+[c])";
					//System.out.println("funcTextL "+funcTextL);
					F1D twposLFunc = new F1D("twposLFunc", funcTextL, -250.0, 250.0);
					
					GraphErrors twposLGraph = new GraphErrors("twposLGraph");
					twposLGraph.setName("twposLGraph");
					twposLGraph.setTitle(histTitle(sector,layer,paddle));
					twposLFunc.setLineColor(FUNC_COLOUR);
					twposLFunc.setLineWidth(FUNC_LINE_WIDTH);
					twposLGraph.setMarkerSize(MARKER_SIZE);
					twposLGraph.setLineThickness(MARKER_LINE_WIDTH);

					double tw1R = TOFCalibrationEngine.twposValues.getDoubleValue("tw1_right", sector, layer, paddle);
					double tw2R = TOFCalibrationEngine.twposValues.getDoubleValue("tw2_right", sector, layer, paddle);
					String funcTextR = "[a]*x*x+[b]*x-("+tw1R+"*x*x+"+tw2R+"*x+[c])";
					//System.out.println("funcTextR "+funcTextR);
					F1D twposRFunc = new F1D("twposRFunc", funcTextR, -250.0, 250.0);
					
					GraphErrors twposRGraph = new GraphErrors("twposRGraph");
					twposRGraph.setName("twposRGraph");
					twposRGraph.setTitle(histTitle(sector,layer,paddle));
					twposRFunc.setLineColor(FUNC_COLOUR);
					twposRFunc.setLineWidth(FUNC_LINE_WIDTH);
					twposRGraph.setMarkerSize(MARKER_SIZE);
					twposRGraph.setLineThickness(MARKER_LINE_WIDTH);

					DataGroup dg = new DataGroup(2,2);
					dg.addDataSet(twposLHist, 0);
					dg.addDataSet(twposLGraph, 2);
					dg.addDataSet(twposLFunc, 2);
					dg.addDataSet(twposRHist, 1);
					dg.addDataSet(twposRGraph, 3);
					dg.addDataSet(twposRFunc, 3);
					dataGroups.add(dg, sector,layer,paddle);

					setPlotTitle(sector,layer,paddle);

					// initialize the constants array
					Double[] consts = {UNDEFINED_OVERRIDE, UNDEFINED_OVERRIDE, UNDEFINED_OVERRIDE,
							UNDEFINED_OVERRIDE, UNDEFINED_OVERRIDE, UNDEFINED_OVERRIDE};
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

				dataGroups.getItem(sector,layer,component).getH2F("twposLHist").fill(
						 paddle.paddleY(),
						(paddle.deltaTLeft(0.0)+(1000*BEAM_BUCKET) + (0.5*BEAM_BUCKET))%BEAM_BUCKET - 0.5*BEAM_BUCKET);
				dataGroups.getItem(sector,layer,component).getH2F("twposRHist").fill(
						 paddle.paddleY(),
						(paddle.deltaTRight(0.0)+(1000*BEAM_BUCKET) + (0.5*BEAM_BUCKET))%BEAM_BUCKET - 0.5*BEAM_BUCKET);
				
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

		H2F twposLHist = dataGroups.getItem(sector,layer,paddle).getH2F("twposLHist");
		H2F twposRHist = dataGroups.getItem(sector,layer,paddle).getH2F("twposRHist");

		// find the range for the fit
		double lowLimit;
		double highLimit;

		if (minRange != UNDEFINED_OVERRIDE) {
			// use custom values for fit
			lowLimit = minRange;
		}
		else {
			lowLimit = paddleLength(sector,layer,paddle) * -0.35;
		}

		if (maxRange != UNDEFINED_OVERRIDE) {
			// use custom values for fit
			highLimit = maxRange;
		}
		else {
			highLimit = paddleLength(sector,layer,paddle) * 0.35;
		}

		// fit function to the graph of means
		GraphErrors twposLGraph = (GraphErrors) dataGroups.getItem(sector,layer,paddle).getData("twposLGraph");
		GraphErrors twposRGraph = (GraphErrors) dataGroups.getItem(sector,layer,paddle).getData("twposRGraph");
		
		if (fitMethod==FIT_METHOD_SF) {
			// left
			ParallelSliceFitter psfL = new ParallelSliceFitter(twposLHist);
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
			twposLGraph.copy(fixGraph(psfL.getMeanSlices(),"twposLGraph"));
			// right
			ParallelSliceFitter psfR = new ParallelSliceFitter(twposRHist);
			psfR.setFitMode(fitMode);
			psfR.setMinEvents(fitMinEvents);
			psfR.setBackgroundOrder(backgroundSF);
			psfR.setNthreads(1);
			setOutput(false);
			psfR.fitSlicesX();
			setOutput(true);
			if (showSlices) {
				psfR.inspectFits();
				showSlices = false;
			}
			fitSliceMaxError = 2.0;
			twposRGraph.copy(fixGraph(psfR.getMeanSlices(),"twposRGraph"));
		}
		else if (fitMethod==FIT_METHOD_MAX) {
			maxGraphError = 0.3;
			twposLGraph.copy(maxGraph(twposLHist, "twposLGraph"));
			twposRGraph.copy(maxGraph(twposRHist, "twposRGraph"));
		}
		else {
			twposLGraph.copy(twposLHist.getProfileX());
			twposRGraph.copy(twposRHist.getProfileX());
		}
				
		F1D twposLFunc = dataGroups.getItem(sector,layer,paddle).getF1D("twposLFunc");
		F1D twposRFunc = dataGroups.getItem(sector,layer,paddle).getF1D("twposRFunc");
		twposLFunc.setRange(lowLimit, highLimit);
		twposRFunc.setRange(lowLimit, highLimit);

		twposLFunc.setParameter(0, 0.0);
		twposLFunc.setParameter(1, 0.0);
		twposLFunc.setParameter(2, 0.0);
		twposRFunc.setParameter(0, 0.0);
		twposRFunc.setParameter(1, 0.0);
		twposRFunc.setParameter(2, 0.0);

		try {
			DataFitter.fit(twposLFunc, twposLGraph, fitOption);
			DataFitter.fit(twposRFunc, twposRGraph, fitOption);

		} catch (Exception e) {
			System.out.println("Fit error with sector "+sector+" layer "+layer+" paddle "+paddle);
			e.printStackTrace();
		}
	}

	public void customFit(int sector, int layer, int paddle){

		String[] fields = { "Min range for fit:", "Max range for fit:", "SPACE",
				"Min Events per slice:", "Background order for slicefitter(-1=no background, 0=p0 etc):","SPACE",
				"Override tw1_left:", "Override tw2_left:", "SPACE",
				"Override tw1_right:", "Override tw2_right:"};

		TOFCustomFitPanel panel = new TOFCustomFitPanel(fields,sector,layer);

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
			
			double overrideTW1L = toDouble(panel.textFields[4].getText());
			double overrideTW2L = toDouble(panel.textFields[5].getText());
			double overrideTW1R = toDouble(panel.textFields[6].getText());
			double overrideTW2R = toDouble(panel.textFields[7].getText());
			
			int minP = paddle;
			int maxP = paddle;
			int minS = sector;
			int maxS = sector;
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
	
			for (int s=minS; s<=maxS; s++) {
				for (int p=minP; p<=maxP; p++) {
					// save the override values
					Double[] consts = constants.getItem(s, layer, p);
					consts[TW1L_OVERRIDE] = overrideTW1L;
					consts[TW2L_OVERRIDE] = overrideTW2L;
					consts[TW1R_OVERRIDE] = overrideTW1R;
					consts[TW2R_OVERRIDE] = overrideTW2R;

					fit(s, layer, p, minRange, maxRange);

					// update the table
					saveRow(s,layer,p);
				}
			}
			calib.fireTableDataChanged();

		}	 
	}


	public Double getTW1L(int sector, int layer, int paddle) {

		double tw1l = 0.0;
		double overrideVal = constants.getItem(sector, layer, paddle)[TW1L_OVERRIDE];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			tw1l = overrideVal;
		}
		else {
			tw1l = dataGroups.getItem(sector,layer,paddle).getF1D("twposLFunc").getParameter(0);
		}
		return tw1l;
	}

	public Double getTW2L(int sector, int layer, int paddle) {

		double tw2l = 0.0;
		double overrideVal = constants.getItem(sector, layer, paddle)[TW2L_OVERRIDE];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			tw2l = overrideVal;
		}
		else {
			tw2l = dataGroups.getItem(sector,layer,paddle).getF1D("twposLFunc").getParameter(1);
		}
		return tw2l;
	}
	
	public Double getTW1R(int sector, int layer, int paddle) {

		double tw1r= 0.0;
		double overrideVal = constants.getItem(sector, layer, paddle)[TW1R_OVERRIDE];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			tw1r = overrideVal;
		}
		else {
			tw1r = dataGroups.getItem(sector,layer,paddle).getF1D("twposRFunc").getParameter(0);
		}
		return tw1r;
	}

	public Double getTW2R(int sector, int layer, int paddle) {

		double tw2r = 0.0;
		double overrideVal = constants.getItem(sector, layer, paddle)[TW2R_OVERRIDE];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			tw2r = overrideVal;
		}
		else {
			tw2r = dataGroups.getItem(sector,layer,paddle).getF1D("twposRFunc").getParameter(1);
		}
		return tw2r;
	}

	@Override
	public void saveRow(int sector, int layer, int paddle) {
		calib.setDoubleValue(getTW1L(sector,layer,paddle),
				"tw1_left", sector, layer, paddle);
		calib.setDoubleValue(getTW2L(sector,layer,paddle),
				"tw2_left", sector, layer, paddle);
		calib.setDoubleValue(getTW1R(sector,layer,paddle),
				"tw1_right", sector, layer, paddle);
		calib.setDoubleValue(getTW2R(sector,layer,paddle),
				"tw2_right", sector, layer, paddle);

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

		H2F hist = new H2F();
		F1D func = new F1D("twposFunc");
		if (showPlotType == "TWPOS_LEFT") { 
			hist = dataGroups.getItem(sector,layer,paddle).getH2F("twposLHist");
			func = dataGroups.getItem(sector,layer,paddle).getF1D("twposLFunc");
		}
		else {
			hist = dataGroups.getItem(sector,layer,paddle).getH2F("twposRHist");
			func = dataGroups.getItem(sector,layer,paddle).getF1D("twposRFunc");
		}

		canvas.draw(hist);    
		canvas.draw(func, "same");
	}
	
	@Override
	public void showPlots(int sector, int layer) {

		showPlotType = "TWPOS_LEFT";
		stepName = "Time walk position - left";
		super.showPlots(sector, layer);
		showPlotType = "TWPOS_RIGHT";
		stepName = "Time walk position - right";
		super.showPlots(sector, layer);

	}

	@Override
	public DataGroup getSummary(int sector, int layer) {

		DataGroup dg = new DataGroup(2,1);
		return dg;

	}
	
}
