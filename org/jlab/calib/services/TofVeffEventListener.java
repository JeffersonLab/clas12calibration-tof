package org.jlab.calib.services;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

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
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
import org.jlab.groot.math.F1D;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataEventType;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.task.DataSourceProcessorPane;
import org.jlab.io.task.IDataEventListener;
import org.jlab.utils.groups.IndexedList;

public class TofVeffEventListener extends TOFCalibrationEngine {

//	public final static int[]		NUM_PADDLES = {23,62,5};
//	public final static String[]	LAYER_NAME = {"FTOF1A","FTOF1B","FTOF2"};
//
//	CalibrationConstants calib;
//	IndexedList<DataGroup> dataGroups = new IndexedList<DataGroup>(3);

	public TofVeffEventListener() {

		calib = new CalibrationConstants(3,
				"veff_left/F:veff_right/F:veff_left_err/F:veff_right_err/F");
		calib.setName("/calibration/ftof/effective_velocity");

	}
	
//	@Override
//	public void dataEventAction(DataEvent event) {
//
//		if (event.getType()==DataEventType.EVENT_START) {
//			resetEventListener();
//			processEvent(event);
//		}
//		else if (event.getType()==DataEventType.EVENT_ACCUMULATE) {
//			processEvent(event);
//		}
//		else if (event.getType()==DataEventType.EVENT_STOP) {
//			analyze();
//		} 
//
//	}

	public void timerUpdate() {
		analyze();
	}

	private double halfLength(int paddle) {
		return 85.0;
		// *** hard coded for paddle 10 at the moment - read from geometry???
	}
	
	public double lowY(int paddle) {
		return -halfLength(paddle);
	}
	
	public double highY(int paddle) {
		return halfLength(paddle);
	}	
	
	public void resetEventListener() {

		// LC perform init processing
		for (int sector = 1; sector <= 6; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int layer_index = layer - 1;
				for (int paddle = 1; paddle <= NUM_PADDLES[layer_index]; paddle++) {

					// create all the histograms
					H2F hist = 
					new H2F("veff",
							"veff",
							100, lowY(paddle), highY(paddle), 
							//100, -210.0, 210.0,
							200, -10.0, 10.0);
					
					hist.setName("veff");
					
					hist.setTitle("Half Time Diff vs Position : " + LAYER_NAME[layer_index] 
							+ " Sector "+sector+" Paddle "+paddle);
					hist.setXTitle("Position (cm)");
					hist.setYTitle("Half Time Diff (ns)");

					// create all the functions
					F1D veffFunc = new F1D("veffFunc", "[a]+[b]*x", lowY(paddle), highY(paddle));
					
					DataGroup dg = new DataGroup(1,1);
					dg.addDataSet(hist, 0);
					dg.addDataSet(veffFunc, 0);
					dataGroups.add(dg, sector,layer,paddle);
					
				}
			}
		}
	}

	public void processEvent(DataEvent event) {

		List<TOFPaddle> paddleList = DataProvider.getPaddleList(event);
		for (TOFPaddle paddle : paddleList) {

			int sector = paddle.getDescriptor().getSector();
			int layer = paddle.getDescriptor().getLayer();
			int component = paddle.getDescriptor().getComponent();

			dataGroups.getItem(sector,layer,component).getH2F("veff").fill(
					paddle.paddleY(), paddle.halfTimeDiff());
		}
	}

	public void analyze() {
		for (int sector = 1; sector <= 6; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int layer_index = layer - 1;
				for (int paddle = 1; paddle <= NUM_PADDLES[layer_index]; paddle++) {
					fit(sector, layer, paddle, 0.0, 0.0);
				}
			}
		}
		save();
	}

	public void fit(int sector, int layer, int paddle,
			double minRange, double maxRange) {
		
		H2F veffHist = dataGroups.getItem(sector,layer,paddle).getH2F("veff");
		
		// fit function to the graph of means
		GraphErrors meanGraph = veffHist.getProfileX();

		// find the range for the fit
		double lowLimit;
		double highLimit;
		
		if (minRange != 0.0 && maxRange != 0.0) {

			// use custom values for fit
			lowLimit = minRange;
			highLimit = maxRange;
		}
		else {

			int lowIndex = 20;
			int highIndex = 80;
			int graphMaxIndex = meanGraph.getDataSize(0)-1;
			
			// get the position with max entries
			int maxCounts = 0;
			int centreIndex = meanGraph.getDataSize(0)/2;
			int maxIndex = 0;
			for (int i=3; i<meanGraph.getDataSize(0)-3; i++) {
				if (veffHist.getSlicesX().get(i).getEntries() > maxCounts) {
					maxIndex = i;
					maxCounts = veffHist.getSlicesX().get(i).getEntries(); 
				}
			}
			
			for (int pos=centreIndex; pos < graphMaxIndex; pos++) {
				
			    if(veffHist.getSlicesX().get(pos).getEntries() < 0.2*maxCounts){
				      highIndex = pos;
				      break;
			    }
			}
			
			for (int pos=centreIndex; pos>=1; pos--) {
			    if(veffHist.getSlicesX().get(pos).getEntries() < 0.2*maxCounts){
				      lowIndex = pos;
				      break;
			    }
			}
			
			lowLimit = meanGraph.getDataX(lowIndex);
			highLimit = meanGraph.getDataX(highIndex);
		}

		F1D veffFunc = dataGroups.getItem(sector,layer,paddle).getF1D("veffFunc");
		veffFunc.setRange(lowLimit, highLimit);
		
		veffFunc.setParameter(0, 0.0);
		veffFunc.setParameter(1, 1.0/16.0);
		veffFunc.setParLimits(0, -5.0, 5.0);
		veffFunc.setParLimits(1, 1.0/20.0, 1.0/12.0);
		
		DataFitter.fit(veffFunc, meanGraph, "RNQ");
		
	}

	public Double getVeff(int sector, int layer, int paddle) {
		
		double veff = 0.0;
		double gradient = dataGroups.getItem(sector,layer,paddle).getF1D("veffFunc") 
				.getParameter(1);
		if (gradient==0.0) {
			veff=0.0;
		}
		else {
			veff = 1/gradient;
		}
		return veff;
	}
	
	public Double getVeffError(int sector, int layer, int paddle){
		
		double veffError = 0.0;
		
		// Calculate the error
		// fractional error in veff = fractional error in 1/veff
		
		double gradient = dataGroups.getItem(sector,layer,paddle).getF1D("veffFunc")
				.getParameter(1);
		double gradientErr = dataGroups.getItem(sector,layer,paddle).getF1D("veffFunc")
				.parameter(1).error();
		double veff = getVeff(sector, layer, paddle);
		
		if (gradient==0.0) {
			veffError = 0.0;
		}
		else {
			veffError = (gradientErr/gradient) * veff;
		}

		return veffError;
	}	

	public void save() {

		for (int sector = 1; sector <= 6; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int layer_index = layer - 1;
				for (int paddle = 1; paddle <= NUM_PADDLES[layer_index]; paddle++) {
					calib.addEntry(sector, layer, paddle);
					calib.setDoubleValue(getVeff(sector,layer,paddle),
							"veff_left", sector, layer, paddle);
					calib.setDoubleValue(getVeff(sector,layer,paddle),
							"veff_right", sector, layer, paddle);
					calib.setDoubleValue(getVeffError(sector,layer,paddle),
							"veff_left_err", sector, layer, paddle);
					calib.setDoubleValue(getVeffError(sector,layer,paddle),
							"veff_right_err", sector, layer, paddle);
				}
			}
		}
		calib.save("FTOF_CALIB_VEFF.txt");
	}

	@Override
	public List<CalibrationConstants> getCalibrationConstants() {
		
		return Arrays.asList(calib);
	}

	@Override
	public IndexedList<DataGroup>  getDataGroup(){
		return dataGroups;
	}

	public JPanel getView(CalibrationConstantsListener listener, EmbeddedCanvas canvas) {
		
		JPanel panel = new JPanel();
		
	    JSplitPane          splitPane = null;
	    CalibrationConstantsView ccview = null;
	    
	    panel.setLayout(new BorderLayout());
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        ccview = new CalibrationConstantsView();
        ccview.addConstants(this.getCalibrationConstants().get(0),listener);
        splitPane.setTopComponent(canvas);
        splitPane.setBottomComponent(ccview);
        panel.add(splitPane,BorderLayout.CENTER);
        splitPane.setDividerLocation(0.5);
		
		return panel;
	}
}

