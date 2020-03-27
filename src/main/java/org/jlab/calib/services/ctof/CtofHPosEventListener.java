package org.jlab.calib.services.ctof; 

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

import org.jlab.calib.services.TOFCustomFitPanel;
import org.jlab.calib.services.TOFPaddle;
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

public class CtofHPosEventListener extends CTOFCalibrationEngine {

	public final int HPOSA_OVERRIDE = 0;
	public final int HPOSB_OVERRIDE = 1;
	public final int HPOSC_OVERRIDE = 2;
	
	private String fitOption = "RQ";
	int backgroundSF = -1;
	boolean showSlices = false;

	public CtofHPosEventListener() {

		stepName = "HPOS";
		fileNamePrefix = "CTOF_CALIB_HPOS_";
		// get file name here so that each timer update overwrites it
		filename = nextFileName();

		calib = new CalibrationConstants(3,
				"hposa/F:hposb/F:hposc/F:hposd/F:hpose/F");
		calib.setName("/calibration/ctof/hpos");
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
					double hposA = Double.parseDouble(lineValues[3]);
					double hposB = Double.parseDouble(lineValues[4]);
					double hposC = Double.parseDouble(lineValues[5]);
					double hposD = Double.parseDouble(lineValues[6]);
					
					hposValues.addEntry(sector, layer, paddle);
					hposValues.setDoubleValue(hposA,
							"hposa", sector, layer, paddle);
					hposValues.setDoubleValue(hposB,
							"hposb", sector, layer, paddle);
					hposValues.setDoubleValue(hposC,
							"hposc", sector, layer, paddle);
					hposValues.setDoubleValue(hposD,
							"hposd", sector, layer, paddle);
					
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
			for (int paddle = 1; paddle <= NUM_PADDLES[0]; paddle++) {
				hposValues.addEntry(1, 1, paddle);
				hposValues.setDoubleValue(0.0,
						"hposa", 1, 1, paddle);
				hposValues.setDoubleValue(0.0,
						"hposb", 1, 1, paddle);
				hposValues.setDoubleValue(0.0,
						"hposc", 1, 1, paddle);
				hposValues.setDoubleValue(0.0,
						"hposd", 1, 1, paddle);
						
			}			
		}
		else if (calDBSource==CAL_DB) {
			System.out.println("Database Run No: "+prevCalRunNo);
			DatabaseConstantProvider dcp = new DatabaseConstantProvider(prevCalRunNo, "default");
			hposValues = dcp.readConstants("/calibration/ctof/hpos");
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
		
		for (int paddle = 1; paddle <= NUM_PADDLES[0]; paddle++) {

			// create all the histograms
			H2F hposHist = 
					new H2F("hposHist","HPOS P"+paddle,
							100, -paddleLength(1,1,paddle)*0.55, paddleLength(1,1,paddle)*0.55,
							bins, -bb*0.5, bb*0.5);
			hposHist.setTitleX("hit position (cm)");
			hposHist.setTitleY("delta T (ns)");

			// create all the functions and graphs
		
			String funcText = "([a]*exp([b]*x))+[c]";
			// create all the functions
			F1D hposFunc = new F1D("hposFunc", funcText, -50.0, 50.0);
			
			GraphErrors hposGraph = new GraphErrors("hposGraph");
			hposGraph.setName("hposGraph");
			hposGraph.setTitle("HPOS P"+paddle);
			hposFunc.setLineColor(FUNC_COLOUR);
			hposFunc.setLineWidth(FUNC_LINE_WIDTH);
			hposGraph.setMarkerSize(MARKER_SIZE);
			hposGraph.setLineThickness(MARKER_LINE_WIDTH);

			DataGroup dg = new DataGroup(2,1);
			dg.addDataSet(hposHist, 0);
			dg.addDataSet(hposGraph, 1);
			dg.addDataSet(hposFunc, 1);
			dataGroups.add(dg, 1,1,paddle);

			setPlotTitle(1,1,paddle);

			// initialize the constants array
			Double[] consts = {UNDEFINED_OVERRIDE, UNDEFINED_OVERRIDE, UNDEFINED_OVERRIDE};
			// override values

			constants.add(consts, 1, 1, paddle);

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

			if (paddle.goodTrackFound() && paddle.includeInCTOFTiming()) {
				
				dataGroups.getItem(sector,layer,component).getH2F("hposHist").fill(
						 paddle.paddleY(),
						 (paddle.refSTTimeRFCorr()+(1000*BEAM_BUCKET) + (0.5*BEAM_BUCKET))%BEAM_BUCKET - 0.5*BEAM_BUCKET);
				
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

		H2F hposHist = dataGroups.getItem(sector,layer,paddle).getH2F("hposHist");

		// find the range for the fit
		double lowLimit;
		double highLimit;

		if (minRange != UNDEFINED_OVERRIDE) {
			// use custom values for fit
			lowLimit = minRange;
		}
		else {
			lowLimit = paddleLength(sector,layer,paddle) * -0.3;
		}

		if (maxRange != UNDEFINED_OVERRIDE) {
			// use custom values for fit
			highLimit = maxRange;
		}
		else {
			highLimit = paddleLength(sector,layer,paddle) * 0.4;
		}

		// fit function to the graph of means
		GraphErrors hposGraph = (GraphErrors) dataGroups.getItem(sector,layer,paddle).getData("hposGraph");
		
		if (fitMethod==FIT_METHOD_SF) {
			
			ParallelSliceFitter psfL = new ParallelSliceFitter(hposHist);
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
			hposGraph.copy(fixGraph(psfL.getMeanSlices(),"hposGraph"));
		}
		else if (fitMethod==FIT_METHOD_MAX) {
			maxGraphError = 0.3;
			hposGraph.copy(maxGraph(hposHist, "twposGraph"));
		}
		else {
			hposGraph.copy(hposHist.getProfileX());
		}
				
		F1D hposFunc = dataGroups.getItem(sector,layer,paddle).getF1D("hposFunc");
		hposFunc.setRange(lowLimit, highLimit);

		double hposA = CTOFCalibrationEngine.hposValues.getDoubleValue("hposa",1, 1, paddle);
		double hposB = CTOFCalibrationEngine.hposValues.getDoubleValue("hposb",1, 1, paddle);
		hposFunc.setParameter(0, 0.005); //hposA);
		hposFunc.setParameter(1, 0.1); //hposB
		hposFunc.setParameter(2, 0.0);

		try {
			DataFitter.fit(hposFunc, hposGraph, fitOption);

		} catch (Exception e) {
			System.out.println("Fit error with sector "+sector+" layer "+layer+" paddle "+paddle);
			e.printStackTrace();
		}
		
		// LC Mar 2020 Set function parameters to override value
		Double[] consts = constants.getItem(sector, layer, paddle);
		if (consts[HPOSA_OVERRIDE] != UNDEFINED_OVERRIDE) {
			hposFunc.setParameter(0, consts[HPOSA_OVERRIDE]);
		}
		if (consts[HPOSB_OVERRIDE] != UNDEFINED_OVERRIDE) {
			hposFunc.setParameter(1, consts[HPOSB_OVERRIDE]);
		}

	}

	public void customFit(int sector, int layer, int paddle){

		String[] fields = { "Min range for fit:", "Max range for fit:", "SPACE",
				"Min Events per slice:", "Background order for slicefitter(-1=no background, 0=p0 etc):","SPACE",
				"Override hposA:", "Override hposB:"};

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
			
			double overrideA = toDouble(panel.textFields[4].getText());
			double overrideB = toDouble(panel.textFields[5].getText());
			
			int minP = paddle;
			int maxP = paddle;

			if (panel.applyLevel == panel.APPLY_P) {
				// if fitting one paddle then show inspectFits view
				showSlices = true;
			}
			else {
				minP = 1;
				maxP = NUM_PADDLES[layer-1];
			}
				
			for (int p=minP; p<=maxP; p++) {
				// save the override values
				Double[] consts = constants.getItem(sector, layer, p);
				consts[HPOSA_OVERRIDE] = overrideA;
				consts[HPOSB_OVERRIDE] = overrideB;

				fit(sector, layer, p, minRange, maxRange);

				// update the table
				saveRow(sector,layer,p);
			}
			calib.fireTableDataChanged();

		}	 
	}

	public Double getHPOSA(int sector, int layer, int paddle) {

		return getHPOS(sector, layer, paddle, 0);
	}
	
	public Double getHPOSB(int sector, int layer, int paddle) {

		return getHPOS(sector, layer, paddle, 1);
	}

	public Double getHPOS(int sector, int layer, int paddle, int param) {

		double val = 0.0;
		double overrideVal = constants.getItem(sector, layer, paddle)[param];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			val = overrideVal;
		}
		else {
			val = dataGroups.getItem(sector,layer,paddle).getF1D("hposFunc").getParameter(param);
		}
		return val;
	}
	
	@Override
	public void saveRow(int sector, int layer, int paddle) {
		calib.setDoubleValue(getHPOSA(sector,layer,paddle),
				"hposa", sector, layer, paddle);
		calib.setDoubleValue(getHPOSB(sector,layer,paddle),
				"hposb", sector, layer, paddle);
		calib.setDoubleValue(0.0,
				"hposc", sector, layer, paddle);
		calib.setDoubleValue(0.0,
				"hposd", sector, layer, paddle);
		calib.setDoubleValue(0.0,
				"hpose", sector, layer, paddle);

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

		H2F hist = dataGroups.getItem(sector,layer,paddle).getH2F("hposHist");
		F1D func = dataGroups.getItem(sector,layer,paddle).getF1D("hposFunc");

		canvas.draw(hist);    
		canvas.draw(func, "same");
	}
	
	@Override
	public DataGroup getSummary(int sector, int layer) {
		
		int layer_index = layer-1;
		double[] paddleNumbers = new double[NUM_PADDLES[layer_index]];
		double[] paddleUncs = new double[NUM_PADDLES[layer_index]];
		double[] hposas = new double[NUM_PADDLES[layer_index]];
		double[] zeroUncs = new double[NUM_PADDLES[layer_index]];
		double[] hposbs = new double[NUM_PADDLES[layer_index]];

		for (int p = 1; p <= NUM_PADDLES[layer_index]; p++) {

			paddleNumbers[p - 1] = (double) p;
			paddleUncs[p - 1] = 0.0;
			hposas[p - 1] = getHPOSA(sector, layer, p);
			hposbs[p - 1] = getHPOSB(sector, layer, p);
			zeroUncs[p - 1] = 0.0;
		}

		GraphErrors aSumm = new GraphErrors("aSumm", paddleNumbers,
				hposas, paddleUncs, zeroUncs);

		aSumm.setTitleX("Paddle Number");
		aSumm.setTitleY("HPOSA");
		aSumm.setMarkerSize(MARKER_SIZE);
		aSumm.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors bSumm = new GraphErrors("bSumm", paddleNumbers,
				hposbs, paddleUncs, zeroUncs);

		bSumm.setTitleX("Paddle Number");
		bSumm.setTitleY("HPOSB");
		bSumm.setMarkerSize(MARKER_SIZE);
		bSumm.setLineThickness(MARKER_LINE_WIDTH);

		DataGroup dg = new DataGroup(2,1);
		dg.addDataSet(aSumm, 0);
		dg.addDataSet(bSumm, 1);

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
