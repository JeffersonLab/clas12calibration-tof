package org.jlab.calib.services.ctof; 

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
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

public class CtofHposBinEventListener extends CTOFCalibrationEngine {

	
	private String fitOption = "RQ";
	int backgroundSF = -1;
	boolean showSlices = false;
	IndexedList<Double[]> calibValues = new IndexedList<Double[]>(3);

	public CtofHposBinEventListener() {

		stepName = "HPOS(Bins)";
		fileNamePrefix = "CTOF_CALIB_HPOSBIN_";
		// get file name here so that each timer update overwrites it
		filename = nextFileName();

		calib = new CalibrationConstants(3,
				"bin1/F:bin25/F:bin50/F:bin75/F:bin100/F");
		calib.setName("/calibration/ctof/hposbin");
		calib.setPrecision(3);

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
					
					Double[] vals = new Double[100];
					for (int i=0; i<100; i++) {
						vals[i] = Double.parseDouble(lineValues[i+3]);
					}
					hposBinValues.add(vals, sector, layer, paddle);
					
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
				Double[] vals = new Double[100];
				for (int i=0; i<100; i++) {
					vals[i] = 0.0;
				}
				hposBinValues.add(vals, 1, 1, paddle);
			}			
		}
		else if (calDBSource==CAL_DB) {
			System.out.println("Database Run No: "+prevCalRunNo);
			DatabaseConstantProvider dcp = new DatabaseConstantProvider(prevCalRunNo, "default");
			//hposValues = dcp.readConstants("/calibration/ctof/hpos");
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
			H2F hposBinHist = 
					new H2F("hposBinHist","HPOSBIN P"+paddle,
							100, -paddleLength(1,1,paddle)*0.55, paddleLength(1,1,paddle)*0.55,
							bins, -bb*0.5, bb*0.5);
			hposBinHist.setTitleX("hit position (cm)");
			hposBinHist.setTitleY("delta T (ns)");

			GraphErrors hposBinGraph = new GraphErrors("hposBinGraph");
			hposBinGraph.setName("hposBinGraph");
			hposBinGraph.setTitle("HPOSBIN P"+paddle);
			hposBinGraph.setMarkerSize(MARKER_SIZE);
			hposBinGraph.setLineThickness(MARKER_LINE_WIDTH);

			DataGroup dg = new DataGroup(2,1);
			dg.addDataSet(hposBinHist, 0);
			dg.addDataSet(hposBinGraph, 1);
			dataGroups.add(dg, 1,1,paddle);

			setPlotTitle(1,1,paddle);
			
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
				
				dataGroups.getItem(sector,layer,component).getH2F("hposBinHist").fill(
						 paddle.paddleY(),
						 (paddle.refSTTimeHPosFuncCorr()+(1000*BEAM_BUCKET) + (0.5*BEAM_BUCKET))%BEAM_BUCKET - 0.5*BEAM_BUCKET);
				//System.out.println("Louise 203");
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
	public void fit(int sector, int layer, int paddle) {

		H2F hposBinHist = dataGroups.getItem(sector,layer,paddle).getH2F("hposBinHist");
		GraphErrors hposBinGraph = (GraphErrors) dataGroups.getItem(sector,layer,paddle).getData("hposBinGraph");
		
		if (fitMethod==FIT_METHOD_SF) {
			
			ParallelSliceFitter psfL = new ParallelSliceFitter(hposBinHist);
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
			hposBinGraph.copy(fixGraph(psfL.getMeanSlices(),"hposBinGraph"));
		}
		else if (fitMethod==FIT_METHOD_MAX) {
			maxGraphError = 0.3;
			hposBinGraph.copy(maxGraph(hposBinHist, "hposBinGraph"));
		}
		else {
			hposBinGraph.copy(hposBinHist.getProfileX());
		}

		// initialise the calib table (so that missing graph points have value 0.0)
		Double[] vals = new Double[100];
		for (int i=0; i<100; i++) {
			vals[i] = 0.0;
		}
		
		// Set the constants equal to the offset for each bin
		for (int i=0; i<hposBinGraph.getDataSize(0); i++) {
			int sliceNum = (int) Math.floor(hposBinGraph.getDataX(i)) + 50;
			vals[sliceNum] =hposBinGraph.getDataY(i);
			//System.out.println("Paddle "+paddle+" dataX "+ hposGraph.getDataX(i) + " sliceNum "+sliceNum
			//				+" dataY "+hposGraph.getDataY(i));
		}
		calibValues.add(vals, 1, 1, paddle);
	}

	public void customFit(int sector, int layer, int paddle){

		String[] fields = { "Slice number:", "Override value:"};

		TOFCustomFitPanel panel = new TOFCustomFitPanel(fields,sector,layer);

		int result = JOptionPane.showConfirmDialog(null, panel, 
				"Adjust Fit / Override for paddle "+paddle, JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {
			
			int sliceNum = Integer.parseInt(panel.textFields[4].getText());
			double sliceOverride = toDouble(panel.textFields[5].getText());
			
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

				// update the table
			}
			calib.fireTableDataChanged();

		}	 
	}

	public Double getHPOS(int sector, int layer, int paddle, int sliceNum) {
		
		return calibValues.getItem(sector,layer,paddle)[sliceNum];
	}
		
	@Override
	public void saveRow(int sector, int layer, int paddle) {
		Double[] vals = calibValues.getItem(1,1,paddle);
		calib.setDoubleValue(vals[0],
				"bin1", sector, layer, paddle);
		calib.setDoubleValue(vals[24],
				"bin25", sector, layer, paddle);
		calib.setDoubleValue(vals[49],
				"bin50", sector, layer, paddle);
		calib.setDoubleValue(vals[74],
				"bin75", sector, layer, paddle);
		calib.setDoubleValue(vals[99],
				"bin100", sector, layer, paddle);
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

		H2F hist = dataGroups.getItem(sector,layer,paddle).getH2F("hposBinHist");

		canvas.draw(hist);    
	}
	
	@Override
    public void writeFile(String filename) {

        try { 

            // Open the output file
            File outputFile = new File(filename);
            FileWriter outputFw = new FileWriter(outputFile.getAbsoluteFile());
            BufferedWriter outputBw = new BufferedWriter(outputFw);

			for (int paddle = 1; paddle <= NUM_PADDLES[0]; paddle++) {

				String line = 1+" "+1+" "+paddle;
				Double[] vals = calibValues.getItem(1,1,paddle);
				for (int i=0; i<100; i++) {
					Double val = 0.0;
					if (vals[i] != null) {
						val = vals[i];
					}
					line = line + " " + new DecimalFormat("0.000").format(val);
					//line = line + " " + vals[i];
				}
				outputBw.write(line);
				outputBw.newLine();

			}
            outputBw.close();
        }
        catch(IOException ex) {
            System.out.println(
                    "Error reading file '" 
                            + filename + "'");                   
            // Or we could just do this: 
            ex.printStackTrace();
        }

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
			hposas[p - 1] = 0.0;
			hposbs[p - 1] = 0.0;
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

		System.out.println("Louise 364");
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