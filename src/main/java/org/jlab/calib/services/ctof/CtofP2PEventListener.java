package org.jlab.calib.services.ctof;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.jlab.calib.services.TOFCustomFitPanel;
import org.jlab.calib.services.TOFPaddle;
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
import org.jlab.io.base.DataEventType;
import org.jlab.utils.groups.IndexedList;

public class CtofP2PEventListener extends CTOFCalibrationEngine {

	// indices for constants
	public final int OFFSET_OVERRIDE = 0;

	final double MAX_OFFSET = 10.0;

	public CtofP2PEventListener() {

		stepName = "P2P";
		fileNamePrefix = "CTOF_CALIB_P2P_";
		// get file name here so that each timer update overwrites it
		filename = nextFileName();

		calib = 
				new CalibrationConstants(3,
						"paddle2paddle/F");

		calib.setName("/calibration/ctof/timing_offset/P2P");
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
			for (int paddle = 1; paddle <= NUM_PADDLES[0]; paddle++) {
				p2pValues.addEntry(1, 1, paddle);
				p2pValues.setDoubleValue(0.0,
						"paddle2paddle", 1, 1, paddle);
			}
		}
		else if (calDBSource==CAL_DB) {
			System.out.println("Database Run No: "+prevCalRunNo);
			DatabaseConstantProvider dcp = new DatabaseConstantProvider(prevCalRunNo, "default");
			p2pValues = dcp.readConstants("/calibration/ftof/timing_offset");
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

		for (int paddle = 1; paddle <= NUM_PADDLES[0]; paddle++) {

			DataGroup dg = new DataGroup(1,1);

			// create all the histograms and functions
			H1F vertexDiffHist = 
					new H1F("vertexDiffHist","Vertex time difference Paddle "+paddle, 
							99,-49.5*BEAM_BUCKET,49.5*BEAM_BUCKET);
			vertexDiffHist.setTitleX("#Delta t (vertex) (ns)");
			dg.addDataSet(vertexDiffHist, 0);

			dataGroups.add(dg,1,1,paddle);    

			// initialize the constants array
			Double[] consts = {UNDEFINED_OVERRIDE};
			// override values
			constants.add(consts, 1, 1, paddle);

		}
	}

	@Override
	public void processEvent(DataEvent event) {

		List<TOFPaddle> ctofPaddleList = DataProvider.getPaddleList(event);
		List<TOFPaddle> ftofPaddleList = org.jlab.calib.services.DataProvider.getPaddleList(event);
		processP2PPaddleList(ctofPaddleList, ftofPaddleList);

	}

	public void processP2PPaddleList(List<TOFPaddle> ctofPaddleList, List<TOFPaddle> ftofPaddleList) {

		for (TOFPaddle ctofPaddle : ctofPaddleList) {

			if (ctofPaddle.goodTrackFound()) {   

				int sector = ctofPaddle.getDescriptor().getSector();
				int layer = ctofPaddle.getDescriptor().getLayer();
				int component = ctofPaddle.getDescriptor().getComponent();

				for (TOFPaddle ftofPaddle : ftofPaddleList) {
					
					if (ftofPaddle.goodTrackFound()) {
						dataGroups.getItem(sector,layer,component).getH1F("vertexDiffHist").fill(
								ctofPaddle.startTimeP2PCorr() - ftofPaddle.reconStartTime());
//						ctofPaddle.show();
//						dataGroups.getItem(sector,layer,component).getH1F("vertexDiffHist").fill(
//						ctofPaddle.startTimeP2PCorr() - 124.25);
					}
				}
			}
		}
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
			if (panel.applyToAll) {
				minP = 1;
				maxP = NUM_PADDLES[layer-1];
			}

			for (int p=minP; p<=maxP; p++) {
				// save the override values
				Double[] consts = constants.getItem(sector, layer, p);
				consts[OFFSET_OVERRIDE] = override;

				// update the table
				saveRow(sector,layer,p);
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
		hist.setTitle("Paddle "+paddle);
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
		double[] paddleUncs = new double[NUM_PADDLES[layer_index]];
		double[] values = new double[NUM_PADDLES[layer_index]];
		double[] valueUncs = new double[NUM_PADDLES[layer_index]];

		for (int p = 1; p <= NUM_PADDLES[layer_index]; p++) {

			paddleNumbers[p - 1] = (double) p;
			paddleUncs[p - 1] = 0.0;
			values[p - 1] = getOffset(sector, layer, p);
			valueUncs[p - 1] = 0.0;
		}

		GraphErrors summ = new GraphErrors("summ", paddleNumbers,
				values, paddleUncs, valueUncs);
		summ.setMarkerSize(MARKER_SIZE);
		summ.setLineThickness(MARKER_LINE_WIDTH);

		DataGroup dg = new DataGroup(1,1);
		dg.addDataSet(summ, 0);
		return dg;

	}

}