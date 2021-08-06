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

public class TofCheckEventListener extends TOFCalibrationEngine {

	private String showPlotType = "CHECK_MOM";

	private final double[]        ADC_MIN = {0.0, 20.0,  50.0,  20.0};
	private final double[]        ADC_MAX = {0.0, 4000.0, 7000.0, 4000.0};
	private final double[]        PATH_MIN = {0.0, 690.0,  670.0,  630.0};
	private final double[]        PATH_MAX = {0.0, 760.0, 760.0, 760.0};
	
	public TofCheckEventListener() {

		stepName = "Check";
		histTitle = "";
		fileNamePrefix = "FTOF_CALIB_CHECK_";
		// get file name here so that each timer update overwrites it
		filename = nextFileName();

		calib = 
				new CalibrationConstants(3,
						"rfpad/F");

		calib.setName("/calibration/ftof/timing_offset/check");
		calib.setPrecision(3);	
		
		logScale = true;

	}

	@Override
	public void populatePrevCalib() {
		prevCalRead = true;
	}

	@Override
	public void resetEventListener() {

		// perform init processing
		createHists();
	}

	public void createHists() {
		double bb = TOFCalibrationEngine.BEAM_BUCKET;
		int bins = (int) (bb/2.004)*88;

		for (int sector = 1; sector <= 6; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int layer_index = layer - 1;
				for (int paddle = 1; paddle <= NUM_PADDLES[layer_index]; paddle++) {

					DataGroup dg = new DataGroup(3,2);

					// create all the histograms and functions
					H2F momHist = 
							new H2F("momHist",histTitle(sector,layer,paddle),
									100, TOFCalibration.minP, TOFCalibration.maxP,
									bins, -bb*0.5, bb*0.5);
					momHist.setTitleX("p (GeV)");
					momHist.setTitleY("delta T (ns)");
					dg.addDataSet(momHist, 0);

					H2F vzHist = 
							new H2F("vzHist",histTitle(sector,layer,paddle),
									100, -20.0, 20.0,
									bins, -bb*0.5, bb*0.5);
					vzHist.setTitleX("vz (cm)");
					vzHist.setTitleY("delta T (ns)");
					dg.addDataSet(vzHist, 1);
					
					H2F hitHist = 
							new H2F("hitHist",histTitle(sector,layer,paddle),
									100, -paddleLength(sector,layer,paddle)*0.55, paddleLength(sector,layer,paddle)*0.55,
									bins, -bb*0.5, bb*0.5);
					hitHist.setTitleX("hit position (cm)");
					hitHist.setTitleY("delta T (ns)");
					dg.addDataSet(hitHist, 2);

					H2F pathHist = 
							new H2F("pathHist",histTitle(sector,layer,paddle),
									100, PATH_MIN[layer], PATH_MAX[layer],
									bins, -bb*0.5, bb*0.5);
					pathHist.setTitleX("path (cm)");
					pathHist.setTitleY("delta T (ns)");
					dg.addDataSet(pathHist, 3);

					H2F adcLHist = 
							new H2F("adcLHist",histTitle(sector,layer,paddle),
									100, ADC_MIN[layer], ADC_MAX[layer],
									bins, -bb*0.5, bb*0.5);
					adcLHist.setTitleX("ADC Left");
					adcLHist.setTitleY("delta T (ns)");
					dg.addDataSet(adcLHist, 4);

//					H2F adcRHist = 
//							new H2F("adcRHist",histTitle(sector,layer,paddle),
//									100, ADC_MIN[layer], ADC_MAX[layer],
//									bins, -bb*0.5, bb*0.5);
//					adcRHist.setTitleX("ADC Right");
//					adcRHist.setTitleY("delta T (ns)");
//					dg.addDataSet(adcRHist, 5);
					
					H2F energyHist = 
							new H2F("energyHist",histTitle(sector,layer,paddle),
									100, 0.0, 50.0,
									bins, -bb*0.5, bb*0.5);
					energyHist.setTitleX("Energy");
					energyHist.setTitleY("delta T (ns)");
					dg.addDataSet(energyHist, 5);					
					
					dataGroups.add(dg,sector,layer,paddle);    

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
			
			if (pad.goodTrackFound()) {
				
				dataGroups.getItem(sector,layer,component).getH2F("momHist").fill(
						 pad.P,
						(pad.refTimeCorr()+(1000*BEAM_BUCKET) + (0.5*BEAM_BUCKET))%BEAM_BUCKET - 0.5*BEAM_BUCKET);

				dataGroups.getItem(sector,layer,component).getH2F("vzHist").fill(
						 pad.VERTEX_Z,
						(pad.refTimeCorr()+(1000*BEAM_BUCKET) + (0.5*BEAM_BUCKET))%BEAM_BUCKET - 0.5*BEAM_BUCKET);
							
				dataGroups.getItem(sector,layer,component).getH2F("hitHist").fill(
						 pad.paddleY(),
						(pad.refTimeCorr()+(1000*BEAM_BUCKET) + (0.5*BEAM_BUCKET))%BEAM_BUCKET - 0.5*BEAM_BUCKET);

				dataGroups.getItem(sector,layer,component).getH2F("pathHist").fill(
						 pad.PATH_LENGTH,
						(pad.refTimeCorr()+(1000*BEAM_BUCKET) + (0.5*BEAM_BUCKET))%BEAM_BUCKET - 0.5*BEAM_BUCKET);
				
				dataGroups.getItem(sector,layer,component).getH2F("adcLHist").fill(
						 pad.ADCL,
						(pad.refTimeCorr()+(1000*BEAM_BUCKET) + (0.5*BEAM_BUCKET))%BEAM_BUCKET - 0.5*BEAM_BUCKET);

//				dataGroups.getItem(sector,layer,component).getH2F("adcRHist").fill(
//						 pad.ADCR,
//						(pad.refTimeCorr()+(1000*BEAM_BUCKET) + (0.5*BEAM_BUCKET))%BEAM_BUCKET - 0.5*BEAM_BUCKET);
				dataGroups.getItem(sector,layer,component).getH2F("energyHist").fill(
						 pad.energy(),
						(pad.refTimeCorr()+(1000*BEAM_BUCKET) + (0.5*BEAM_BUCKET))%BEAM_BUCKET - 0.5*BEAM_BUCKET);
				
			}
		}
	}    

	@Override
	public void showPlots(int sector, int layer) {
		
		//logScale = true;
		showPlotType = "CHECK_MOM";
		stepName = "Momentum vs Vertex Time";
		super.showPlots(sector, layer);
		showPlotType = "CHECK_VZ";
		stepName = "Vertex z vs Vertex Time";
		super.showPlots(sector, layer);
		showPlotType = "CHECK_HIT";
		stepName = "Hit position vs Vertex Time";
		super.showPlots(sector, layer);
		showPlotType = "CHECK_PATH";
		stepName = "Path length vs Vertex Time";
		super.showPlots(sector, layer);
		showPlotType = "CHECK_ADCL";
		stepName = "ADC Left vs Vertex Time";
		super.showPlots(sector, layer);
//		showPlotType = "CHECK_ADCR";
//		stepName = "ADC Right vs Vertex Time";
//		super.showPlots(sector, layer);
		showPlotType = "CHECK_ENERGY";
		stepName = "Energy vs Vertex Time";
		super.showPlots(sector, layer);

	}

	@Override
	public void drawPlots(int sector, int layer, int paddle, EmbeddedCanvas canvas) {

		H2F hist = new H2F();
		if (showPlotType == "CHECK_MOM") { 
			hist = dataGroups.getItem(sector,layer,paddle).getH2F("momHist");
		}
		else if (showPlotType == "CHECK_VZ") { 
			hist = dataGroups.getItem(sector,layer,paddle).getH2F("vzHist");
		}
		else if (showPlotType == "CHECK_HIT") { 
			hist = dataGroups.getItem(sector,layer,paddle).getH2F("hitHist");
		}
		else if (showPlotType == "CHECK_PATH") { 
			hist = dataGroups.getItem(sector,layer,paddle).getH2F("pathHist");
		}
		else if (showPlotType == "CHECK_ADCL") { 
			hist = dataGroups.getItem(sector,layer,paddle).getH2F("adcLHist");
		}
//		else if (showPlotType == "CHECK_ADCR") {
//			hist = dataGroups.getItem(sector,layer,paddle).getH2F("adcRHist");
//		}
		else if (showPlotType == "CHECK_ENERGY") {
			hist = dataGroups.getItem(sector,layer,paddle).getH2F("energyHist");
		}
		
		hist.setTitleX("");
		hist.setTitleY("");
		canvas.draw(hist);    
	}
	
	@Override
	public void setPlotTitle(int sector, int layer, int paddle) {
		// reset hist title as may have been set to null by show all 
		dataGroups.getItem(sector,layer,paddle).getH2F("momHist").setTitleX("p (GeV)");
		dataGroups.getItem(sector,layer,paddle).getH2F("momHist").setTitleY("delta T (ns)");
		dataGroups.getItem(sector,layer,paddle).getH2F("vzHist").setTitleX("vz (cm)");
		dataGroups.getItem(sector,layer,paddle).getH2F("vzHist").setTitleY("delta T (ns)");
		dataGroups.getItem(sector,layer,paddle).getH2F("hitHist").setTitleX("hit position (cm)");
		dataGroups.getItem(sector,layer,paddle).getH2F("hitHist").setTitleY("delta T (ns)");
		dataGroups.getItem(sector,layer,paddle).getH2F("pathHist").setTitleX("path (cm)");
		dataGroups.getItem(sector,layer,paddle).getH2F("pathHist").setTitleY("delta T (ns)");
		dataGroups.getItem(sector,layer,paddle).getH2F("adcLHist").setTitleX("ADC Left");
		dataGroups.getItem(sector,layer,paddle).getH2F("adcLHist").setTitleY("delta T (ns)");
//		dataGroups.getItem(sector,layer,paddle).getH2F("adcRHist").setTitleX("ADC Right");
//		dataGroups.getItem(sector,layer,paddle).getH2F("adcRHist").setTitleY("delta T (ns)");
		dataGroups.getItem(sector,layer,paddle).getH2F("energyHist").setTitleX("Energy");
		dataGroups.getItem(sector,layer,paddle).getH2F("energyHist").setTitleY("delta T (ns)");
	}	

	@Override
	public boolean isGoodPaddle(int sector, int layer, int paddle) {

		return true;

	}
	
	@Override
	public void saveRow(int sector, int layer, int paddle) {
		calib.setDoubleValue(rfpadValues.getDoubleValue("rfpad",sector, layer, paddle),
				"rfpad", sector, layer, paddle);
	}	

	@Override
	public DataGroup getSummary(int sector, int layer) {
		DataGroup dg = new DataGroup(1,1);
		return dg;

	}
	
	@Override
	public void writeFile(String filename) {
		// no file required for check step
	}

}