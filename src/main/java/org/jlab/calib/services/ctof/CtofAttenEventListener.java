package org.jlab.calib.services.ctof;


import java.awt.BorderLayout;
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.jlab.calib.services.TOFCustomFitPanel;
import org.jlab.calib.services.TOFPaddle;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.calib.tasks.CalibrationEngine;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.CalibrationConstantsListener;
import org.jlab.detector.calib.utils.CalibrationConstantsView;
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


public class CtofAttenEventListener extends CTOFCalibrationEngine { 

	// constants for indexing the constants array
	public final int ATTEN_OVERRIDE = 0;
	public final int ATTEN_UNC_OVERRIDE = 1;
	public final int OFFSET_OVERRIDE = 2;

	private String fitOption = "RQ";
	int backgroundSF = -1;
	boolean showSlices = false;

	public CtofAttenEventListener() {

		stepName = "Attenuation Length";
		fileNamePrefix = "CTOF_CALIB_ATTEN_";
		// get file name here so that each timer update overwrites it
		filename = nextFileName();

		calib = new CalibrationConstants(3,
				"attlen_upstream/F:attlen_downstream/F:attlen_upstream_err/F:attlen_downstream_err/F:y_offset/F");
		calib.setName("/calibration/ctof/attenuation");
		calib.setPrecision(3);

		// assign constraints corresponding to layer 1 values for now
		// need addConstraint to be able to check layer and paddle
		for (int paddle=1; paddle<=NUM_PADDLES[0]; paddle++) {
			calib.addConstraint(3, expectedAttlen(1,1,paddle)*0.9,
					expectedAttlen(1,1,paddle)*1.1,
					2,
					paddle);
			calib.addConstraint(4, expectedAttlen(1,1,paddle)*0.9,
					expectedAttlen(1,1,paddle)*1.1,
					2,
					paddle);
		}

	}

	@Override
	public void populatePrevCalib() {
		prevCalRead = true;
	}	

	@Override
	public void resetEventListener() {

		//System.out.println("Atten resetEventListener");

		// perform init processing
		for (int paddle = 1; paddle <= NUM_PADDLES[0]; paddle++) {

			// create all the histograms
			int numBins = (int) (paddleLength(1,1,paddle)*0.6);  // 1 bin per 2cm + 10% either side
			double min = paddleLength(1,1,paddle) * -0.6;
			double max = paddleLength(1,1,paddle) * 0.6;
			H2F hist = new H2F("atten", "Log Ratio vs Position : Paddle "
					+ paddle, numBins, min, max, 100, -1.5, 1.5);

			hist.setName("atten");
			hist.setTitle("Log Ratio vs Position :  Paddle "+paddle);
			hist.setTitleX("Position (cm)");
			hist.setTitleY("ln(ADC D / ADC U)");

			// create all the functions and graphs
			F1D attenFunc = new F1D("attenFunc", "[a]+[b]*x", -50.0, 50.0);
			attenFunc.setParameter(1, 2.0/expectedAttlen(1,1,paddle));
			GraphErrors meanGraph = new GraphErrors("meanGraph");
			meanGraph.setName("meanGraph");
			attenFunc.setLineColor(FUNC_COLOUR);
			attenFunc.setLineWidth(FUNC_LINE_WIDTH);
			meanGraph.setMarkerSize(MARKER_SIZE);
			meanGraph.setLineThickness(MARKER_LINE_WIDTH);

			DataGroup dg = new DataGroup(2,1);
			dg.addDataSet(hist, 0);
			dg.addDataSet(meanGraph, 1);
			dg.addDataSet(attenFunc, 1);
			dataGroups.add(dg, 1,1,paddle);

			setPlotTitle(1,1,paddle);

			// initialize the constants array
			Double[] consts = {UNDEFINED_OVERRIDE, UNDEFINED_OVERRIDE, UNDEFINED_OVERRIDE};
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

			// dgtz data only
			
//			System.out.println("position "+paddle.position());
//			System.out.println("halfTimeDiff "+paddle.halfTimeDiff());
//			System.out.println("veff "+paddle.veff()+" leftRightAdjustment "+paddle.leftRightAdjustment());
//			System.out.println("timeLeftAfterTW "+paddle.timeLeftAfterTW()+" timeRightAfterTW "+paddle.timeRightAfterTW());
//			
//			paddle.show();
			dataGroups.getItem(sector,layer,component).getH2F("atten").fill(
					paddle.position(), paddle.logRatio());

			// cooked data - position from timing and veff
			//			if (paddle.recPosition()!=0) {
			//				dataGroups.getItem(sector,layer,component).getH2F("atten").fill(
			//						paddle.recPosition(), paddle.logRatio());
			//			}


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
	public void fit(int sector, int layer, int paddle, double minRange,
			double maxRange) {

		H2F attenHist = dataGroups.getItem(sector,layer,paddle).getH2F("atten");

		double lowLimit;
		double highLimit;

		if (minRange == UNDEFINED_OVERRIDE) {
			// default value
			lowLimit = paddleLength(sector,layer,paddle) * -0.3;
		} 
		else {
			// custom value
			lowLimit = minRange;
		}


		if (maxRange == UNDEFINED_OVERRIDE) {
			// default value
			highLimit = paddleLength(sector,layer,paddle) * 0.4;
		} 
		else {
			// custom value
			highLimit = maxRange;
		}

		// fit function to the graph of means
		GraphErrors meanGraph = (GraphErrors) dataGroups.getItem(sector,layer,paddle).getData("meanGraph");

		if (fitMethod==FIT_METHOD_SF) {
			ParallelSliceFitter psf = new ParallelSliceFitter(attenHist);
			psf.setFitMode(fitMode);
			psf.setMinEvents(fitMinEvents);
			psf.setBackgroundOrder(backgroundSF);
			psf.setNthreads(1);
			setOutput(false);
			psf.fitSlicesX();
			setOutput(true);
			if (showSlices) {
				psf.inspectFits();
				showSlices = false;
			}
			fitSliceMaxError = 2.0;
			meanGraph.copy(fixGraph(psf.getMeanSlices(),"meanGraph"));
		}
		else if (fitMethod==FIT_METHOD_MAX) {
			maxGraphError = 0.15;
			meanGraph.copy(maxGraph(attenHist, "meanGraph"));
		}
		else {
			meanGraph.copy(attenHist.getProfileX());
		}		

		F1D attenFunc = dataGroups.getItem(sector,layer,paddle).getF1D("attenFunc");
		attenFunc.setRange(lowLimit, highLimit);
		attenFunc.setParameter(0, 0.0);
		attenFunc.setParameter(1, 2.0/expectedAttlen(sector,layer,paddle));
		attenFunc.setParLimits(0, -5.0, 5.0);
		attenFunc.setParLimits(1, 2.0/500.0, 2.0/10.0);
		if (sector==1 && layer==1 &&paddle==8) {
			//System.out.println("SLC "+sector+layer+paddle);
			DataFitter.fit(attenFunc, meanGraph, fitOption);
			//			System.out.println("Param 0 is "+attenFunc.getParameter(0));
			//			System.out.println("Param 0 error is "+attenFunc.parameter(0).error());
			//			System.out.println("Param 1 is "+attenFunc.getParameter(1));
			//			System.out.println("Param 1 error is "+attenFunc.parameter(1).error());
			//			System.out.println("exp attlen is "+expectedAttlen(sector,layer,paddle));
			//			System.out.println("2/exp attlen is "+2/expectedAttlen(sector,layer,paddle));
			//			System.out.println("par 1 lower limit "+2.0/500.0);
			//			System.out.println("par 1 upper limit "+2.0/10.0);

		}
		else {
			DataFitter.fit(attenFunc, meanGraph, fitOption);
		}


		//		if (sector==1 && layer==1 && paddle==10) {
		//			TCanvas c1 = new TCanvas("c1",1,1);
		//			c1.draw(meanGraph);
		//			c1.draw(attenFunc,"same");
		//
		//			System.out.println("SLC "+sector+layer+paddle);
		//			System.out.println("Param 1 is "+ attenFunc.getParameter(1));
		//			System.out.println("Param 1 error is "+ attenFunc.parameter(1).error());
		//
		//		}

	}

	public void customFit(int sector, int layer, int paddle){

		outputGraph(sector, layer, paddle);

		String[] fields = { "Min range for fit:", "Max range for fit:", "SPACE",
				"Min Events per slice:", "Background order for slicefitter(-1=no background, 0=p0 etc):","SPACE",
				"Override Attenuation Length:", "Override Attenuation Length uncertainty:",
		"Override offset:" };
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

			double overrideValue = toDouble(panel.textFields[4].getText());
			double overrideUnc = toDouble(panel.textFields[5].getText());			
			double overrideOffset = toDouble(panel.textFields[6].getText());		

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
				consts[ATTEN_OVERRIDE] = overrideValue;
				consts[ATTEN_UNC_OVERRIDE] = overrideUnc;
				consts[OFFSET_OVERRIDE] = overrideOffset;

				fit(sector, layer, p, minRange, maxRange);

				// update the table
				saveRow(sector,layer,p);
			}
			calib.fireTableDataChanged();
		}	 
	}

	public Double getAttlen(int sector, int layer, int paddle) {

		double attLen = 0.0;
		double overrideVal = constants.getItem(sector, layer, paddle)[ATTEN_OVERRIDE];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			attLen = overrideVal;
		}
		else {
			double gradient = dataGroups.getItem(sector,layer,paddle).getF1D("attenFunc")
					.getParameter(1);
			if (gradient == 0.0) {
				attLen = 0.0;
			} else {
				attLen = 2 / gradient;
			}
		}

		return attLen;
	}

	public Double getAttlenError(int sector, int layer, int paddle) {

		double attLenUnc = 0.0;
		double overrideVal = constants.getItem(sector, layer, paddle)[ATTEN_UNC_OVERRIDE];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			attLenUnc = overrideVal;
		}
		else {
			double gradient = dataGroups.getItem(sector,layer,paddle).getF1D("attenFunc")
					.getParameter(1);
			double gradientErr = dataGroups.getItem(sector,layer,paddle).getF1D("attenFunc")
					.parameter(1).error();
			double attlen = getAttlen(sector, layer, paddle);
			if (gradient == 0.0) {
				attLenUnc = 0.0;
			} else {
				attLenUnc = (gradientErr / gradient) * attlen;
			}
		}
		return attLenUnc;
	}

	public Double getOffset(int sector, int layer, int paddle) {

		double offset = 0.0;
		double overrideVal = constants.getItem(sector, layer, paddle)[OFFSET_OVERRIDE];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			offset = overrideVal;
		}
		else {
			offset = dataGroups.getItem(sector,layer,paddle).getF1D("attenFunc")
					.getParameter(0);
		}

		return offset;

	}

	@Override
	public void saveRow(int sector, int layer, int paddle) {
		calib.setDoubleValue(getAttlen(sector,layer,paddle),
				"attlen_upstream", sector, layer, paddle);
		calib.setDoubleValue(getAttlen(sector,layer,paddle),
				"attlen_downstream", sector, layer, paddle);
		calib.setDoubleValue(getAttlenError(sector,layer,paddle),
				"attlen_upstream_err", sector, layer, paddle);
		calib.setDoubleValue(getAttlenError(sector,layer,paddle),
				"attlen_downstream_err", sector, layer, paddle);
		calib.setDoubleValue(getOffset(sector,layer,paddle),
				"y_offset", sector, layer, paddle);

	}

	public double expectedAttlen(int sector, int layer, int paddle) {

		return 140.0;
	}

	@Override
	public boolean isGoodPaddle(int sector, int layer, int paddle) {

		double attlen = getAttlen(sector,layer,paddle);
		double expAttlen = expectedAttlen(sector,layer,paddle);

		return (attlen >= (0.9*expAttlen)) && (attlen <= (1.1*expAttlen));

	}

	@Override
	public void setPlotTitle(int sector, int layer, int paddle) {
		// reset hist title as may have been set to null by show all 
		dataGroups.getItem(sector,layer,paddle).getGraph("meanGraph").setTitleX("Position (cm)");
		dataGroups.getItem(sector,layer,paddle).getGraph("meanGraph").setTitleY("ln(ADC D / ADC U)");

	}

	@Override
	public void drawPlots(int sector, int layer, int paddle, EmbeddedCanvas canvas) {

		GraphErrors meanGraph = dataGroups.getItem(sector,layer,paddle).getGraph("meanGraph");
		if (meanGraph.getDataSize(0) != 0) {
			meanGraph.setTitleX("");
			meanGraph.setTitleY("");
			canvas.draw(meanGraph);
			canvas.draw(dataGroups.getItem(sector,layer,paddle).getF1D("attenFunc"), "same");
		}
	}

	@Override
	public DataGroup getSummary(int sector, int layer) {

		double[] paddleNumbers = new double[NUM_PADDLES[0]];
		double[] paddleUncs = new double[NUM_PADDLES[0]];
		double[] Attlens = new double[NUM_PADDLES[0]];
		double[] AttlenUncs = new double[NUM_PADDLES[0]];

		for (int p = 1; p <= NUM_PADDLES[0]; p++) {

			paddleNumbers[p - 1] = (double) p;
			paddleUncs[p - 1] = 0.0;
			Attlens[p - 1] = getAttlen(sector, layer, p);
			//			AttlenUncs[p - 1] = getAttlenError(sector, layer, p);
			AttlenUncs[p - 1] = 0.0;

		}

		GraphErrors attSumm = new GraphErrors("attSumm", paddleNumbers,
				Attlens, paddleUncs, AttlenUncs);

		attSumm.setTitle("Attenuation Length");
		attSumm.setTitleX("Paddle Number");
		attSumm.setTitleY("Attenuation Length (cm)");
		attSumm.setMarkerSize(MARKER_SIZE);
		attSumm.setLineThickness(MARKER_LINE_WIDTH);

		DataGroup dg = new DataGroup(1,1);
		dg.addDataSet(attSumm, 0);
		return dg;

	}

	public void outputGraph(int sector, int layer, int paddle) {
		String outputFileName = "CtofGraphSLC"+sector+layer+paddle+".txt";
		String histFileName = "CtofHistSLC"+sector+layer+paddle+".txt";

		try { 

			// Open the output file
			File outputFile = new File(outputFileName);
			FileWriter outputFw = new FileWriter(outputFile.getAbsoluteFile());
			BufferedWriter outputBw = new BufferedWriter(outputFw);
			File histFile = new File(histFileName);
			FileWriter histFw = new FileWriter(histFile.getAbsoluteFile());
			BufferedWriter histBw = new BufferedWriter(histFw);

			H2F attenHist = dataGroups.getItem(sector,layer,paddle).getH2F("atten");

			for (int i=0; i<attenHist.getXAxis().getNBins(); i++) {
				H1F h1 = attenHist.sliceX(i);
				if (h1.integral()>1.0) {
					outputBw.write(attenHist.getXAxis().getBinCenter(i)+" "+
							h1.getMean()+" "+
							attenHist.getXAxis().getBinWidth(i)/2.0+" "+
							h1.getRMS());
					outputBw.newLine();
				}
				for (int j=0; j<attenHist.getYAxis().getNBins(); j++) {

					histBw.write(attenHist.getXAxis().getBinCenter(i)+" "+
							attenHist.getYAxis().getBinCenter(j)+" "+
							attenHist.getBinContent(i, j));
					histBw.newLine();
				}
			}

			outputBw.close();
			histBw.close();
		}
		catch(IOException ex) {
			ex.printStackTrace();
		}

		//		System.out.println("attlen "+getAttlen(sector,layer,paddle));
		//		System.out.println("error in attlen "+getAttlenError(sector,layer,paddle));
		//		System.out.println("gradient "+dataGroups.getItem(sector,layer,paddle).getF1D("attenFunc")
		//				.getParameter(1));
		//		System.out.println(" error in gradient "+dataGroups.getItem(sector,layer,paddle).getF1D("attenFunc")
		//				.parameter(1).error());
		//		System.out.println("lowLimit "+paddleLength(sector,layer,paddle) * -0.4);
		//		System.out.println("highLimit "+paddleLength(sector,layer,paddle) * 0.4);

	}

}
