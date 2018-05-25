package org.jlab.calib.services;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.jlab.calib.services.ctof.CTOFCalibrationEngine;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
//import org.jlab.calib.temp.DataGroup;
import org.jlab.groot.math.F1D;
import org.jlab.groot.ui.TCanvas;
import org.jlab.io.base.DataEvent;
import org.jlab.utils.groups.IndexedList;

public class TofRFPadEventListener extends TOFCalibrationEngine {

	// indices for constants
	public final int OFFSET_OVERRIDE = 0;
	public final int SIGMA_OVERRIDE = 1;

	private String showPlotType = "VERTEX_RF";

	private String fitOption = "RQ";
	
	private final double[] MIN_SIGMA = {0.0, 0.080, 0.020, 0.080};
	private final double[] MAX_SIGMA = {0.0, 0.300, 0.150, 0.300};
	

	public TofRFPadEventListener() {

		stepName = "RF paddle";
		histTitle = "RFPAD";
		fileNamePrefix = "FTOF_CALIB_RFPAD_";
		// get file name here so that each timer update overwrites it
		filename = nextFileName();

		calib = 
				new CalibrationConstants(3,
						"rfpad/F:rfpad_sigma/F");

		calib.setName("/calibration/ftof/timing_offset/rfpad");
		calib.setPrecision(3);

		// assign constraints
		for (int i=1; i<=3; i++) {
			calib.addConstraint(4, MIN_SIGMA[i], MAX_SIGMA[i], 1, i);
		}		

	}

	@Override
	public void populatePrevCalib() {

		System.out.println("Populating "+stepName+" previous calibration values");
		if (calDBSource==CAL_FILE) {

			String line = null;
			try { 
				System.out.println("File: "+prevCalFilename);
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
					double rfpad = Double.parseDouble(lineValues[3]);

					rfpadValues.addEntry(sector, layer, paddle);
					rfpadValues.setDoubleValue(rfpad,
							"rfpad", sector, layer, paddle);

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
						rfpadValues.addEntry(sector, layer, paddle);
						rfpadValues.setDoubleValue(0.0,
								"rfpad", sector, layer, paddle);
					}
				}
			}			
		}
		else if (calDBSource==CAL_DB) {
			System.out.println("Database Run No: "+prevCalRunNo);
			DatabaseConstantProvider dcp = new DatabaseConstantProvider(prevCalRunNo, "default");
			rfpadValues = dcp.readConstants("/calibration/ftof/timing_offset");
			dcp.disconnect();
		}
		prevCalRead = true;
		System.out.println(stepName+" previous calibration values populated successfully");
	}

	@Override
	public void resetEventListener() {

		// perform init processing

		// create the histograms for the first iteration
		createHists();
	}

	public void createHists() {
		
		double bb = BEAM_BUCKET;
		int bins = (int) (bb/2.004)*160;

		for (int sector = 1; sector <= 6; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int layer_index = layer - 1;
				for (int paddle = 1; paddle <= NUM_PADDLES[layer_index]; paddle++) {

					DataGroup dg = new DataGroup(2,1);

					// create all the histograms and functions
					H1F fineHistRaw = 
							new H1F("fineHistRaw",histTitle(sector, layer, paddle),
									bins, -bb, bb);
					fineHistRaw.setTitleX("RF time - vertex time modulo beam bucket (ns)");
					dg.addDataSet(fineHistRaw, 0);

					H1F fineHist = 
							new H1F("fineHist",histTitle(sector, layer, paddle),
									bins, -bb, bb);
					fineHist.setTitleX("RF time - vertex time modulo beam bucket (ns)");
					dg.addDataSet(fineHist, 1);

					//F1D fineFunc = new F1D("fineFunc","[amp]*gaus(x,[mean],[sigma])+[a]*x^2+[b]*x+[c]", -1.0, 1.0);
					F1D fineFunc = new F1D("fineFunc","[amp]*gaus(x,[mean],[sigma])+[b]*x+[c]", -1.0, 1.0);
					fineFunc.setLineColor(FUNC_COLOUR);
					fineFunc.setLineWidth(FUNC_LINE_WIDTH);
					dg.addDataSet(fineFunc, 1);
					fineFunc.setOptStat(1110);		

					dataGroups.add(dg,sector,layer,paddle);    

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

		List<TOFPaddle> paddleList = DataProvider.getPaddleList(event);
		processPaddleList(paddleList);

	}

	@Override
	public void processPaddleList(List<TOFPaddle> paddleList) {

		for (TOFPaddle pad : paddleList) {

			int sector = pad.getDescriptor().getSector();
			int layer = pad.getDescriptor().getLayer();
			int component = pad.getDescriptor().getComponent();

			//if (pad.TRACK_ID != -1)	pad.show();

			if (pad.goodTrackFound()) {
				
				dataGroups.getItem(sector,layer,component).getH1F("fineHistRaw").fill(
						(pad.refTime()+(1000*BEAM_BUCKET) + (0.5*BEAM_BUCKET))%BEAM_BUCKET - 0.5*BEAM_BUCKET);
			}
		}
	}  
	
	private double minSigma(int layer, int paddle) {
		
		double minSigma = 0.0;
		
		if (layer==1) {
			minSigma = ((paddle*5.45) + 74.55)/1000.0;
		}
		else if (layer==2) {
			minSigma = ((paddle*0.90) + 29.10)/1000.0;
		}
		else if (layer==3) {
			minSigma = ((paddle*5.00) + 145.00)/1000.0;
		}
		
		return minSigma;
		
	}

	@Override
	public void fit(int sector, int layer, int paddle, double minRange, double maxRange) {

		H1F rawHist = dataGroups.getItem(sector,layer,paddle).getH1F("fineHistRaw");
		H1F fineHist = dataGroups.getItem(sector,layer,paddle).getH1F("fineHist");
		
		// move the histogram content to +/- half beam bucket around the peak
		fineHist.reset();
		int maxBin = rawHist.getMaximumBin();
		double maxPos = rawHist.getXaxis().getBinCenter(maxBin);
		int startBin = rawHist.getXaxis().getBin(BEAM_BUCKET*-0.5);
		int endBin = rawHist.getXaxis().getBin(BEAM_BUCKET*0.5);

		for (int rawBin=startBin; rawBin<endBin; rawBin++) {

			double rawBinCenter = rawHist.getXaxis().getBinCenter(rawBin);
			int fineHistOldBin = fineHist.getXaxis().getBin(rawBinCenter);
			int fineHistNewBin = fineHist.getXaxis().getBin(rawBinCenter);
			double newBinCenter = 0.0;

			if (rawBinCenter > maxPos + 0.5*BEAM_BUCKET) {
				newBinCenter = rawBinCenter - BEAM_BUCKET;
				fineHistNewBin = fineHist.getXaxis().getBin(newBinCenter);
			}
			if (rawBinCenter < maxPos - 0.5*BEAM_BUCKET) {
				newBinCenter = rawBinCenter + BEAM_BUCKET;
				fineHistNewBin = fineHist.getXaxis().getBin(newBinCenter);
			}

			fineHist.setBinContent(fineHistOldBin, 0.0);
			fineHist.setBinContent(fineHistNewBin, rawHist.getBinContent(rawBin));
		}


		// fit gaussian +p1
		F1D fineFunc = dataGroups.getItem(sector,layer,paddle).getF1D("fineFunc");

		// find the range for the fit
		double lowLimit;
		double highLimit;
		if (minRange != UNDEFINED_OVERRIDE) {
			// use custom values for fit
			lowLimit = minRange;
		}
		else {
			//lowLimit = maxPos-0.65;
			lowLimit = maxPos-0.5;
		}
		if (maxRange != UNDEFINED_OVERRIDE) {
			// use custom values for fit
			highLimit = maxRange;
		}
		else {
			highLimit = maxPos+0.65;
			highLimit = maxPos+0.5;
		}

		fineFunc.setRange(lowLimit, highLimit);
		fineFunc.setParameter(0, fineHist.getBinContent(maxBin));
		fineFunc.setParLimits(0, fineHist.getBinContent(maxBin)*0.7, fineHist.getBinContent(maxBin)*1.2);
		fineFunc.setParameter(1, maxPos);
		fineFunc.setParameter(2, 0.1);
		fineFunc.setParLimits(2, minSigma(layer,paddle), 1.0);

		try {
			DataFitter.fit(fineFunc, fineHist, fitOption);
			//fineHist.setTitle(fineHist.getTitle() + " Fine offset = " + formatDouble(fineFunc.getParameter(1)));
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}	
		fineHist.setFunction(null);

	}

	private Double formatDouble(double val) {
		return Double.parseDouble(new DecimalFormat("0.000").format(val));
	}

	@Override
	public void customFit(int sector, int layer, int paddle){

		String[] fields = { "Min range for fit:", "Max range for fit:", "SPACE",
				//				"Amp:", "Mean:", "Sigma:", "Offset:", "SPACE",
		"Override offset:", "Override sigma"};
		TOFCustomFitPanel panel = new TOFCustomFitPanel(fields,sector,layer);

		int result = JOptionPane.showConfirmDialog(null, panel, 
				"Adjust Fit / Override for paddle "+paddle, JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {

			double minRange = toDouble(panel.textFields[0].getText());
			double maxRange = toDouble(panel.textFields[1].getText());
			double overrideOffset = toDouble(panel.textFields[2].getText());
			double overrideSigma = toDouble(panel.textFields[3].getText());
			
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
					consts[OFFSET_OVERRIDE] = overrideOffset;
					consts[SIGMA_OVERRIDE] = overrideSigma;

					fit(s, layer, p, minRange, maxRange);

					// update the table
					saveRow(s,layer,p);
				}
			}
			calib.fireTableDataChanged();

		}     
	}

	public Double getOffset(int sector, int layer, int paddle) {

		double offset = 0.0;
		double overrideVal = constants.getItem(sector, layer, paddle)[OFFSET_OVERRIDE];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			offset = overrideVal;
		}
		else {
			F1D fineFunc = dataGroups.getItem(sector,layer,paddle).getF1D("fineFunc");
			H1F rawHist = dataGroups.getItem(sector,layer,paddle).getH1F("fineHistRaw");
			double fineOffset = 0.0;
			if (rawHist.getEntries() != 0){
				fineOffset= fineFunc.getParameter(1);
			}
			offset = fineOffset;
		}
		return offset;
	}    
	
	public Double getSigma(int sector, int layer, int paddle) {
		
		double sigma = 0.0;
		double overrideVal = constants.getItem(sector, layer, paddle)[SIGMA_OVERRIDE];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			sigma = overrideVal;
		}
		else {
			F1D fineFunc = dataGroups.getItem(sector,layer,paddle).getF1D("fineFunc");
			sigma = fineFunc.getParameter(2);
		}
		return sigma;
	}    
	

	@Override
	public void saveRow(int sector, int layer, int paddle) {

		calib.setDoubleValue(getOffset(sector,layer,paddle),
				"rfpad", sector, layer, paddle);
		calib.setDoubleValue(getSigma(sector,layer,paddle),
				"rfpad_sigma", sector, layer, paddle);

	}

	public void writeSigmaFile(String filename) {

		try { 

			// Open the output file
			File outputFile = new File(filename);
			FileWriter outputFw = new FileWriter(outputFile.getAbsoluteFile());
			BufferedWriter outputBw = new BufferedWriter(outputFw);

			for (int sector = 1; sector <= 6; sector++) {
				for (int layer = 1; layer <= 3; layer++) {
					int layer_index = layer - 1;
					for (int paddle = 1; paddle <= NUM_PADDLES[layer_index]; paddle++) {
						String line = new String();
						line = sector+" "+layer+" "+paddle+" "+new DecimalFormat("0.000").format(getSigma(sector,layer,paddle));
						outputBw.write(line);
						outputBw.newLine();
					}
				}
			}

			outputBw.close();
		}
		catch(IOException ex) {
			System.out.println(
					"Error writing file '" );                   
			// Or we could just do this: 
			ex.printStackTrace();
		}

	}	

	@Override
	public void showPlots(int sector, int layer) {

		showPlotType = "VERTEX_RF";
		stepName = "RF - Vertex Time";
		super.showPlots(sector, layer);

	}

	@Override
	public void drawPlots(int sector, int layer, int paddle, EmbeddedCanvas canvas) {

		H1F hist = new H1F();
		F1D func = new F1D("fineFunc");
		if (showPlotType == "VERTEX_RF") { 
			hist = dataGroups.getItem(sector,layer,paddle).getH1F("fineHist");
			func = dataGroups.getItem(sector,layer,paddle).getF1D("fineFunc");
			//func.setOptStat(0);
			//hist.setTitle("RFPAD "+LAYER_PREFIX[layer]+paddle);
			hist.setTitleX("");
			hist.setTitleY("");
			canvas.draw(hist); 
			canvas.draw(func, "same");

		}
	}

	@Override
	public boolean isGoodPaddle(int sector, int layer, int paddle) {

		return (getSigma(sector,layer,paddle) >= MIN_SIGMA[layer]
				&&
				getSigma(sector,layer,paddle) <= MAX_SIGMA[layer]);

	}

	@Override
	public DataGroup getSummary(int sector, int layer) {

		int layer_index = layer-1;
		double[] paddleNumbers = new double[NUM_PADDLES[layer_index]];
		double[] paddleUncs = new double[NUM_PADDLES[layer_index]];
		double[] values = new double[NUM_PADDLES[layer_index]];
		double[] valueUncs = new double[NUM_PADDLES[layer_index]];
		double[] sigmas = new double[NUM_PADDLES[layer_index]];
		double[] sigmaUncs = new double[NUM_PADDLES[layer_index]];

		for (int p = 1; p <= NUM_PADDLES[layer_index]; p++) {

			paddleNumbers[p - 1] = (double) p;
			paddleUncs[p - 1] = 0.0;
			values[p - 1] = getOffset(sector, layer, p);
			valueUncs[p - 1] = 0.0;
			sigmas[p-1] = getSigma(sector,layer,p);
			sigmaUncs[p-1] = 0.0;
		}

		GraphErrors summ = new GraphErrors("summ", paddleNumbers,
				values, paddleUncs, valueUncs);
		summ.setMarkerSize(MARKER_SIZE);
		summ.setLineThickness(MARKER_LINE_WIDTH);
		summ.setTitleX("Paddle number");
		summ.setTitleY("Offset");
		
		GraphErrors sigmaSumm = new GraphErrors("sigmaSumm", paddleNumbers,
				sigmas, paddleUncs, sigmaUncs);
		sigmaSumm.setMarkerSize(MARKER_SIZE);
		sigmaSumm.setLineThickness(MARKER_LINE_WIDTH);		
		sigmaSumm.setTitleX("Paddle number");
		sigmaSumm.setTitleY("Sigma");		

		DataGroup dg = new DataGroup(2,1);
		dg.addDataSet(summ, 0);
		dg.addDataSet(sigmaSumm, 1);
		return dg;

	}
}