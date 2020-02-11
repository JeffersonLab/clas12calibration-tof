package org.jlab.calib.services;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

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

public class TofP2PEventListener extends TOFCalibrationEngine {

	// indices for constants
	public final int OFFSET_OVERRIDE = 0;

	final double MAX_OFFSET = 30.0;

	private String showPlotType = "VERTEX_DT";

	public TofP2PEventListener() {

		stepName = "P2P";
		histTitle = "P2P";
		fileNamePrefix = "FTOF_CALIB_P2P_";
		// get file name here so that each timer update overwrites it
		filename = nextFileName();

		calib = 
				new CalibrationConstants(3,
						"paddle2paddle/F");

		calib.setName("/calibration/ftof/timing_offset/P2P");
		calib.setPrecision(3);

		// assign constraints
		calib.addConstraint(3, -MAX_OFFSET, MAX_OFFSET);

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
					double p2p = Double.parseDouble(lineValues[3]);

					p2pValues.addEntry(sector, layer, paddle);
					p2pValues.setDoubleValue(p2p,
							"paddle2paddle", sector, layer, paddle);

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
						p2pValues.addEntry(sector, layer, paddle);
						p2pValues.setDoubleValue(0.0,
								"paddle2paddle", sector, layer, paddle);
					}
				}
			}			
		}
		else if (calDBSource==CAL_DB) {
			System.out.println("Database Run No: "+prevCalRunNo);
			DatabaseConstantProvider dcp = new DatabaseConstantProvider(prevCalRunNo, "default");
			p2pValues = dcp.readConstants("/calibration/ftof/time_offsets");
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

		int bins = (int) ((2.004/BEAM_BUCKET)*100)-1;
		double binLimit = (bins/2.0)*BEAM_BUCKET;
		
		for (int sector = 1; sector <= 6; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int layer_index = layer - 1;
				for (int paddle = 1; paddle <= NUM_PADDLES[layer_index]; paddle++) {

					DataGroup dg = new DataGroup(1,1);

					// create all the histograms and functions
					H1F vertexDiffHist = 
							new H1F("vertexDiffHist",histTitle(sector,layer,paddle), 
									bins,-binLimit,binLimit);
					vertexDiffHist.setTitleX("#Delta t (vertex) (ns)");
					dg.addDataSet(vertexDiffHist, 0);

					dataGroups.add(dg,sector,layer,paddle);    

					// initialize the constants array
					Double[] consts = {UNDEFINED_OVERRIDE};
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

		for (TOFPaddle currentPad : paddleList) {

			if (currentPad.goodTrackFound()) {
				int sector = currentPad.getDescriptor().getSector();
				int layer = currentPad.getDescriptor().getLayer();
				int component = currentPad.getDescriptor().getComponent();

				for (TOFPaddle otherPad : paddleList) {

					if (otherPad.goodTrackFound() 
							&& otherPad.paddleNumber() != currentPad.paddleNumber()
							&& otherPad.TRACK_ID != currentPad.TRACK_ID) {

						dataGroups.getItem(sector,layer,component).getH1F("vertexDiffHist").fill(
								currentPad.startTimeP2PCorr() - otherPad.startTimeP2PCorr());

					}
				}
			}
		}
	}    

	@Override
	public void timerUpdate() {
		// don't analyze until the end or it will mess up the fine hists
		save();
		calib.fireTableDataChanged();
	}

	@Override
	public void customFit(int sector, int layer, int paddle){

		String[] fields = { "Override offset:"};
		TOFCustomFitPanel panel = new TOFCustomFitPanel(fields,sector,layer);

		int result = JOptionPane.showConfirmDialog(null, panel, 
				"Adjust Fit / Override for paddle "+paddle, JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {

			double override = toDouble(panel.textFields[0].getText());

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
					consts[OFFSET_OVERRIDE] = override;

					// update the table
					saveRow(s,layer,p);
				}
			}
			calib.fireTableDataChanged();			

		}     
	}

	public Double getOffset(int sector, int layer, int paddle) {

		double plotOffset = 0.0;
		double oldOffset = p2pValues.getDoubleValue("paddle2paddle", sector, layer, paddle);
		double newOffset = 0.0;
		double overrideVal = constants.getItem(sector, layer, paddle)[OFFSET_OVERRIDE];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			newOffset = overrideVal;
		}
		else {
			H1F dtHist = dataGroups.getItem(sector,layer,paddle).getH1F("vertexDiffHist");			
			int maxBin = dtHist.getMaximumBin();
			if (dtHist.getEntries() != 0) {
				plotOffset = dtHist.getXaxis().getBinCenter(maxBin);
			}
			newOffset = oldOffset - plotOffset;
		}
		return newOffset;
	}    

	@Override
	public void saveRow(int sector, int layer, int paddle) {

		calib.setDoubleValue(getOffset(sector,layer,paddle),
				"paddle2paddle", sector, layer, paddle);

	}

	@Override
	public void drawPlots(int sector, int layer, int paddle, EmbeddedCanvas canvas) {

		H1F hist = dataGroups.getItem(sector,layer,paddle).getH1F("vertexDiffHist");
		//hist.setTitle("Paddle "+paddle);
		hist.setTitleX("");
		hist.setTitleY("");
		canvas.draw(hist); 
	}

	@Override
	public boolean isGoodPaddle(int sector, int layer, int paddle) {

		return (getOffset(sector,layer,paddle) >= -MAX_OFFSET
				&&
				getOffset(sector,layer,paddle) <= MAX_OFFSET);

	}

	@Override
	public DataGroup getSummary(int sector, int layer) {

		int layer_index = layer-1;
		double[] paddleNumbers = new double[NUM_PADDLES[layer_index]];
		double[] offsets = new double[NUM_PADDLES[layer_index]];
		double[] zeroUncs = new double[NUM_PADDLES[layer_index]];
		double[] centroids = new double[NUM_PADDLES[layer_index]];

		for (int p = 1; p <= NUM_PADDLES[layer_index]; p++) {

			paddleNumbers[p - 1] = (double) p;
			offsets[p - 1] = getOffset(sector, layer, p);
			H1F dtHist = dataGroups.getItem(sector,layer,p).getH1F("vertexDiffHist");
			if (dtHist.getEntries() != 0) {
				int maxBin = dtHist.getMaximumBin();
				centroids[p - 1] = dtHist.getXaxis().getBinCenter(maxBin);
			}
			else {
				centroids[p - 1] = 0.0;
			}
				
			zeroUncs[p - 1] = 0.0;
		}

		GraphErrors offsetSumm = new GraphErrors("offsetSumm", paddleNumbers,
				offsets, zeroUncs, zeroUncs);
		offsetSumm.setTitleX("Paddle Number");
		offsetSumm.setTitleY("New P2P value");
		offsetSumm.setMarkerSize(MARKER_SIZE);
		offsetSumm.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors centroidSumm = new GraphErrors("centroidSumm", paddleNumbers,
				centroids, zeroUncs, zeroUncs);
		centroidSumm.setTitleX("Paddle Number");
		centroidSumm.setTitleY("Centroid");
		centroidSumm.setMarkerSize(MARKER_SIZE);
		centroidSumm.setLineThickness(MARKER_LINE_WIDTH);

		DataGroup dg = new DataGroup(2,1);
		dg.addDataSet(offsetSumm, 0);
		dg.addDataSet(centroidSumm, 1);
		return dg;

	}


}