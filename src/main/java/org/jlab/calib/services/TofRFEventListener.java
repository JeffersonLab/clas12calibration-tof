package org.jlab.calib.services;


import java.text.DecimalFormat;
import java.util.List;

import javax.swing.JOptionPane;

import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
//import org.jlab.calib.temp.DataGroup;
import org.jlab.groot.math.F1D;
import org.jlab.io.base.DataEvent;

public class TofRFEventListener extends TOFCalibrationEngine {

	// indices for constants
	public final int OFFSET_OVERRIDE = 0;

	final double MAX_OFFSET = 10.0;

	public TofRFEventListener() {

		stepName = "RF";
		fileNamePrefix = "FTOF_CALIB_RF_";
		// get file name here so that each timer update overwrites it
		filename = nextFileName();

		calib = 
				new CalibrationConstants(3,
						"RF_offset/F");

		calib.setName("/calibration/ftof/RF_offset");
		calib.setPrecision(3);

		// assign constraints
		calib.addConstraint(3, -MAX_OFFSET, MAX_OFFSET);

	}

	@Override
	public void resetEventListener() {

		// create the histograms for the first iteration
		createHists();
	}

	public void createHists() {

		// LC perform init processing

		DataGroup dg = new DataGroup(2,1);

		// create all the histograms and functions
		H1F fineHistRaw = 
				new H1F("fineHistRaw","Fine offset",
						100, -2.0, 2.0);
		fineHistRaw.setTitleX("Vertex time - RF time modulo beam bucket (ns)");
		dg.addDataSet(fineHistRaw, 0);

		H1F fineHist = 
				new H1F("fineHist","Fine offset",
						100, -2.0, 2.0);
		fineHist.setTitleX("Vertex time - RF time modulo beam bucket (ns)");
		dg.addDataSet(fineHist, 1);

		// create a dummy function in case there's no data to fit 
		F1D fineFunc = new F1D("fineFunc","[amp]*gaus(x,[mean],[sigma])", -1.0, 1.0);
		dg.addDataSet(fineFunc, 1);

		dataGroups.add(dg,1,1,1);    

		// initialize the constants array
		Double[] consts = {UNDEFINED_OVERRIDE};
		// override values
		constants.add(consts, 1, 1, 1);

	}

	@Override
	public void processEvent(DataEvent event) {

		List<TOFPaddle> paddleList = DataProvider.getPaddleList(event);
		processPaddleList(paddleList);

	}

	@Override
	public void processPaddleList(List<TOFPaddle> paddleList) {

		for (TOFPaddle pad : paddleList) {

			// fill the fine hist
			if (pad.goodTrackFound()) {
				dataGroups.getItem(1,1,1).getH1F("fineHistRaw").fill(
						(pad.refTime()+(1000*BEAM_BUCKET) + (0.5*BEAM_BUCKET))%BEAM_BUCKET - 0.5*BEAM_BUCKET);
				dataGroups.getItem(1,1,1).getH1F("fineHist").fill(
						(pad.refTime()+(1000*BEAM_BUCKET) + (0.5*BEAM_BUCKET))%BEAM_BUCKET - 0.5*BEAM_BUCKET);
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
	public void analyze() {
		fit(1, 1, 1);
		save();
		calib.fireTableDataChanged();
	}	

	@Override
	public void fit(int sector, int layer, int paddle) {

		H1F fineHist = dataGroups.getItem(sector,layer,paddle).getH1F("fineHist");

		int maxBin = fineHist.getMaximumBin();
		double maxPos = fineHist.getxAxis().getBinCenter(maxBin);

		// arrangeFine

		// if maxPos > 0.65 move bin contents of (-1,0) to (1,2)
		if (maxPos > 0.65) {
			int iBin=fineHist.getxAxis().getBin(-1.0+1.0e-10);
			int jBin=fineHist.getxAxis().getBin(1.0+1.0e-10);
			do {
				fineHist.setBinContent(jBin, fineHist.getBinContent(iBin));
				//fineHist.setBinContent(iBin,0);
				iBin++;
				jBin++;
			}
			while (fineHist.getXaxis().getBinCenter(iBin) < 0);
		}

		// if maxPos < -0.65 move bin contents of (0,1) to (-2,-1)
		if (maxPos < -0.65) {
			int iBin=fineHist.getxAxis().getBin(0.0+1.0e-10);
			int jBin=fineHist.getxAxis().getBin(-2.0+1.0e-10);
			do {
				fineHist.setBinContent(jBin, fineHist.getBinContent(iBin));
				//fineHist.setBinContent(iBin,0);
				iBin++;
				jBin++;
			}
			while (fineHist.getXaxis().getBinCenter(iBin) < 1);
		}

		// fit gaussian
		F1D fineFunc = dataGroups.getItem(sector,layer,paddle).getF1D("fineFunc");

		fineFunc.setRange(maxPos-0.5, maxPos+0.5);
		fineFunc.setParameter(0, fineHist.getBinContent(maxBin));
		fineFunc.setParameter(1, maxPos);
		fineFunc.setParameter(2, 0.5);

		try {
			DataFitter.fit(fineFunc, fineHist, "RQ");
			fineHist.setTitle(fineHist.getTitle() + " Fine offset = " + formatDouble(fineFunc.getParameter(1)));
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}	
	}

	private Double formatDouble(double val) {
		return Double.parseDouble(new DecimalFormat("0.000").format(val));
	}

	@Override
	public void customFit(int sector, int layer, int paddle){

		String[] fields = { "Override offset:"};
		TOFCustomFitPanel panel = new TOFCustomFitPanel(fields,sector,layer);

		int result = JOptionPane.showConfirmDialog(null, panel, 
				"Adjust Fit / Override for paddle "+paddle, JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {

			double override = toDouble(panel.textFields[0].getText());

			// save the override values
			Double[] consts = constants.getItem(sector, layer, paddle);
			consts[OFFSET_OVERRIDE] = override;

			// update the table
			saveRow(sector,layer,paddle);
			calib.fireTableDataChanged();

		}     
	}

	public Double getOffset(int sector, int layer, int paddle) {

		System.out.println("getOffset "+sector+layer+paddle);
		double offset = 0.0;
		double overrideVal = constants.getItem(sector, layer, paddle)[OFFSET_OVERRIDE];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			offset = overrideVal;
		}
		else {
			F1D fineFunc = dataGroups.getItem(sector,layer,paddle).getF1D("fineFunc");
			H1F fineHist = dataGroups.getItem(sector,layer,paddle).getH1F("fineHist");
			double fineOffset = 0.0;
			if (fineHist.getEntries() != 0){
				fineOffset= fineFunc.getParameter(1);
			}
			offset = fineOffset;

		}
		return offset;
	}    
	
	@Override
	public void save() {

		calib.addEntry(1, 1, 1);
		saveRow(1, 1, 1);
		calib.save(filename);
	}	

	@Override
	public void saveRow(int sector, int layer, int paddle) {

		calib.setDoubleValue(getOffset(sector,layer,paddle),
				"RF_offset", sector, layer, paddle);

	}

	@Override
	public void drawPlots(int sector, int layer, int paddle, EmbeddedCanvas canvas) {

		// TO DO

	}
	
	@Override
	public void showPlots(int sector, int layer) {
		// show all makes no sense for RF
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

		//        summary.setTitle("Left Right centroids: "
		//                + LAYER_NAME[layer - 1] + " Sector "
		//                + sector);
		//        summary.setTitleX("Paddle Number");
		//        summary.setYTitle("Centroid (cm)");
		//        summary.setMarkerSize(5);
		//        summary.setMarkerStyle(2);

		DataGroup dg = new DataGroup(1,1);
		dg.addDataSet(summ, 0);
		return dg;

	}


}