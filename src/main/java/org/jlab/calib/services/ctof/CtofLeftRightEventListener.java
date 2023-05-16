package org.jlab.calib.services.ctof;


import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
import org.jlab.detector.base.DetectorDescriptor;
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
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
//import org.jlab.calib.temp.DataGroup;
import org.jlab.groot.math.F1D;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataEventType;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.task.DataSourceProcessorPane;
import org.jlab.io.task.IDataEventListener;
import org.jlab.utils.groups.IndexedList;

public class CtofLeftRightEventListener extends CTOFCalibrationEngine {

    // indices for override values
    public final int LEFTRIGHT_OVERRIDE = 0;

    final double LEFT_RIGHT_RATIO = 0.2;
    final double MIN_DIST = -35.0;
    final double MAX_DIST = -15.0;
    final double MIN_LEFTRIGHT = -30.0;
    final double MAX_LEFTRIGHT = -20.0;
    private String fitOption = "RQ";

    public CtofLeftRightEventListener() {

        stepName = "Up Down";
        fileNamePrefix = "CTOF_CALIB_UPDOWN_";
        // get file name here so that each timer update overwrites it
        filename = nextFileName();

        calib = new CalibrationConstants(3,
                "upstream_downstream/F:tdc_upstream_downstream/F");
        calib.setName("/calibration/ctof/time_offsets/upstream_downstream");
        calib.setPrecision(3);

        calib.addConstraint(3, MIN_LEFTRIGHT, MAX_LEFTRIGHT);
        
    }

    @Override
    public void populatePrevCalib() {

        System.out.println("Populating "+stepName+" previous calibration values");
        if (calDBSource==CAL_FILE) {

            System.out.println("File: "+prevCalFilename);
            // read in the left right values from the text file            
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
//                    System.out.println(line);

                    int sector = Integer.parseInt(lineValues[0]);
                    int layer = Integer.parseInt(lineValues[1]);
                    int paddle = Integer.parseInt(lineValues[2]);
                    double lr = Double.parseDouble(lineValues[3]);
                    
//                    System.out.println(sector+" "+layer+" "+paddle+" "+lr);

                    leftRightValues.addEntry(sector, layer, paddle);
                    leftRightValues.setDoubleValue(lr,
                            "upstream_downstream", sector, layer, paddle);
                    
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
                ex.printStackTrace();
                return;
            }            
        }
        else if (calDBSource==CAL_DEFAULT) {
            System.out.println("Default");
            for (int paddle = 1; paddle <= NUM_PADDLES[0]; paddle++) {
                leftRightValues.addEntry(1, 1, paddle);
                leftRightValues.setDoubleValue(0.0,
                        "upstream_downstream", 1, 1, paddle);
                        
            }
        }
        else if (calDBSource==CAL_DB) {
            System.out.println("Database Run No: "+prevCalRunNo);
            DatabaseConstantProvider dcp = new DatabaseConstantProvider(prevCalRunNo, "default");
            leftRightValues = dcp.readConstants("/calibration/ctof/time_offsets");
            dcp.disconnect();
        }
        prevCalRead = true;
        System.out.println(stepName+" previous calibration values populated successfully");
    }

    public void resetEventListener() {

        // perform init processing
        for (int i=0; i<leftRightValues.getRowCount(); i++) {
            String line = new String();
            for (int j=0; j<leftRightValues.getColumnCount(); j++) {
                line = line+leftRightValues.getValueAt(i, j);
                if (j<leftRightValues.getColumnCount()-1) {
                    line = line+" ";
                }
            }
            //System.out.println(line);
        }
        
        // create histograms
        for (int paddle = 1; paddle <= NUM_PADDLES[0]; paddle++) {

            // create all the histograms
			H1F hist = new H1F("left_right","Up Down: Paddle "+paddle,
					1001, -35, -15);
            H1F tdcHist = new H1F("tdc_left_right","Up Down: Paddle "+paddle, 
                    2001, -40, -10);

            hist.setTitle("Up Down  : Paddle "+paddle);

            // create all the functions
            F1D edgeToEdgeFunc = new F1D("edgeToEdgeFunc","[height]",
                    -100.0, 100.0);
            edgeToEdgeFunc.setLineColor(FUNC_COLOUR);
            edgeToEdgeFunc.setLineWidth(FUNC_LINE_WIDTH);
            
            F1D lrFunc = new F1D("lrFunc","[amp]*gaus(x,[mean],[sigma])", -1.0, 1.0);
			
			lrFunc.setLineColor(FUNC_COLOUR);
			lrFunc.setLineWidth(FUNC_LINE_WIDTH);
			lrFunc.setOptStat(1110);	

            DataGroup dg = new DataGroup(2,1);
            dg.addDataSet(hist, 0);
            dg.addDataSet(lrFunc, 0);
            dg.addDataSet(tdcHist, 1);
            dg.addDataSet(edgeToEdgeFunc, 1);
            dataGroups.add(dg, 1,1,paddle);

            setPlotTitle(1,1,paddle);

            // initialize the constants array
            Double[] consts = {UNDEFINED_OVERRIDE};
            // override value
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
            
            if (paddle.includeInTiming()) {
            	dataGroups.getItem(sector,layer,component).getH1F("tdc_left_right").fill(
                    paddle.leftRight());
            }
            
			if (paddle.goodTrackFound()) {
				dataGroups.getItem(sector,layer,component).getH1F("left_right").fill(
						2.0*(paddle.veffHalfTimeDiff() - (paddle.paddleY()- paddle.ctofCenter())/paddle.veff()));
			}
        }
    }

    @Override
    public void fit(int sector, int layer, int paddle,
            double minRange, double maxRange) {
    	
    	// centroid of tdc time - time from position method
    	H1F lrHist = dataGroups.getItem(sector,layer,paddle).getH1F("left_right");
		int maxBin = lrHist.getMaximumBin();
		double maxPos = lrHist.getXaxis().getBinCenter(maxBin);
		
		// fit gaussian 
		F1D lrFunc = dataGroups.getItem(sector,layer,paddle).getF1D("lrFunc");
		// find the range for the fit
		double lowLimit;
		double highLimit;
		if (minRange != UNDEFINED_OVERRIDE) {
			// use custom values for fit
			lowLimit = minRange;
		}
		else {
			//lowLimit = maxPos-0.65;
			lowLimit = maxPos-1.0;
		}
		if (maxRange != UNDEFINED_OVERRIDE) {
			// use custom values for fit
			highLimit = maxRange;
		}
		else {
			highLimit = maxPos+1.0;
		}
		lrFunc.setRange(lowLimit, highLimit);
		lrFunc.setParameter(0, lrHist.getBinContent(maxBin));
		lrFunc.setParLimits(0, lrHist.getBinContent(maxBin)*0.5, lrHist.getBinContent(maxBin)*1.2);
		lrFunc.setParameter(1, maxPos);
		lrFunc.setParameter(2, 1.0);
		//lrFunc.setParLimits(2, 0.5, 5.0);
		if (lrHist.getEntries() > 50) {
			try {
				DataFitter.fit(lrFunc, lrHist, fitOption);
			}
			catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		lrHist.setFunction(null);
		
		// LC Mar 2020 Set function parameters to override value
		Double[] consts = constants.getItem(sector, layer, paddle);
		if (consts[LEFTRIGHT_OVERRIDE] != UNDEFINED_OVERRIDE) {
			lrFunc.setParameter(1, consts[LEFTRIGHT_OVERRIDE]);
		}
    	
    	// centroid of time difference distribution method
    	H1F leftRightHist = dataGroups.getItem(sector,layer,paddle).getH1F("tdc_left_right");
        int nBin = leftRightHist.getXaxis().getNBins();

        // calculate the average of all bins
        // calculate the mean bin content for non-zero bins
        int nonZeroBins = 0;
        double meanBinContent=0;
        for(int i=1;i<=nBin;i++) {
        	if (leftRightHist.getXaxis().getBinCenter(i)>MIN_DIST && leftRightHist.getXaxis().getBinCenter(i)<MAX_DIST) {
        		meanBinContent+=leftRightHist.getBinContent(i);
        		if (leftRightHist.getBinContent(i) > 0) nonZeroBins++;
        	}
        }
        meanBinContent/=nonZeroBins;
        
        // find the edges with bin content < 0.1 * mean bin content for non-zero bins
        double threshold=meanBinContent*LEFT_RIGHT_RATIO;
        int leftEdgeBin=0, rightEdgeBin=nBin;
        for(int i=0; i<nBin; i++){
            if(leftRightHist.getBinContent(i)>threshold 
            		&& leftRightHist.getXaxis().getBinCenter(i)>MIN_DIST && leftRightHist.getXaxis().getBinCenter(i)<MAX_DIST){
                leftEdgeBin=i;
                break;
            }
        }
        for(int i=nBin; i>0; i--){
            if(leftRightHist.getBinContent(i)>threshold
            		&& leftRightHist.getXaxis().getBinCenter(i)>MIN_DIST && leftRightHist.getXaxis().getBinCenter(i)<MAX_DIST){
                rightEdgeBin=i;
                break;
            }
        }

        // create the function showing the width of the spread
        F1D edgeToEdgeFunc = dataGroups.getItem(sector,layer,paddle).getF1D("edgeToEdgeFunc");
        edgeToEdgeFunc.setRange(leftRightHist.getAxis().getBinCenter(leftEdgeBin), 
        		leftRightHist.getAxis().getBinCenter(rightEdgeBin));

        edgeToEdgeFunc.setParameter(0, meanBinContent*LEFT_RIGHT_RATIO); // height to draw line at
        
    }

    @Override
    public void customFit(int sector, int layer, int paddle){

        //System.out.println("Left right value from file is "+leftRightAdjustment(sector,layer,paddle));

    	String[] fields = { "Min range for fit:", "Max range for fit:", "SPACE",
		"Override centroid:"};
		TOFCustomFitPanel panel = new TOFCustomFitPanel(fields,sector,layer);

        int result = JOptionPane.showConfirmDialog(null, panel, 
                "Adjust Fit / Override for paddle "+paddle, JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {

        	double minRange = toDouble(panel.textFields[0].getText());
			double maxRange = toDouble(panel.textFields[1].getText());
			double overrideValue = toDouble(panel.textFields[2].getText());
            
			int minP = paddle;
			int maxP = paddle;
			if (panel.applyLevel == panel.APPLY_P) {
				// 
			}
			else {
				minP = 1;
				maxP = NUM_PADDLES[layer-1];
			}
			
			for (int p=minP; p<=maxP; p++) {
				// save the override values
				Double[] consts = constants.getItem(sector, layer, p);
				consts[LEFTRIGHT_OVERRIDE] = overrideValue;

				fit(sector, layer, p, minRange, maxRange);

				// update the table
				saveRow(sector,layer,p);
			}
            calib.fireTableDataChanged();

        }     
    }

    public Double getCentroid(int sector, int layer, int paddle) {

        double leftRight = 0.0;
        double overrideVal = constants.getItem(sector, layer, paddle)[LEFTRIGHT_OVERRIDE];

        if (overrideVal != UNDEFINED_OVERRIDE) {
            leftRight = overrideVal;
        }
        else {

			F1D lrFunc = dataGroups.getItem(sector,layer,paddle).getF1D("lrFunc");
			H1F lrHist = dataGroups.getItem(sector,layer,paddle).getH1F("left_right");
			if (lrHist.getEntries() != 0){
				leftRight = lrFunc.getParameter(1);
			}

        }

        return leftRight;
    }
    
    public Double getTdcCentroid(int sector, int layer, int paddle) {

        double leftRight = 0.0;

        double min = dataGroups.getItem(sector,layer,paddle).getF1D("edgeToEdgeFunc").getMin(); 
        double max = dataGroups.getItem(sector,layer,paddle).getF1D("edgeToEdgeFunc").getMax();
        leftRight = (min+max)/2.0;

        //leftRight = dataGroups.getItem(sector,layer,paddle).getH1F("left_right").getMean();

        return leftRight;
    }    

    @Override
    public void saveRow(int sector, int layer, int paddle) {
        calib.setDoubleValue(getCentroid(sector,layer,paddle),
                "upstream_downstream", sector, layer, paddle);
        calib.setDoubleValue(getTdcCentroid(sector,layer,paddle),
                "tdc_upstream_downstream", sector, layer, paddle);
    }

    @Override
    public boolean isGoodPaddle(int sector, int layer, int paddle) {

        return (getCentroid(sector,layer,paddle) >= MIN_LEFTRIGHT
                &&
                getCentroid(sector,layer,paddle) <= MAX_LEFTRIGHT);
    }

    @Override
    public void setPlotTitle(int sector, int layer, int paddle) {
        // reset hist title as may have been set to null by show all 
        dataGroups.getItem(sector,layer,paddle).getH1F("left_right").setTitleX("Upstream downstream offset (ns)");
        dataGroups.getItem(sector,layer,paddle).getH1F("tdc_left_right").setTitleX("Upstream downstream offset (ns)");
    }

    @Override
    public void drawPlots(int sector, int layer, int paddle, EmbeddedCanvas canvas) {

        H1F hist = dataGroups.getItem(sector,layer,paddle).getH1F("left_right");
        hist.setTitleX("");
        canvas.draw(hist);
        //canvas.draw(dataGroups.getItem(sector,layer,paddle).getF1D("edgeToEdgeFunc"), "same");
        canvas.draw(dataGroups.getItem(sector,layer,paddle).getF1D("lrFunc"), "same");

    }
    
    @Override
	public void writeFile(String filename) {
		
		boolean[] writeCols = {true,true,true,
							   true,false};
		try { 
			// Open the output file
			File outputFile = new File(filename);
			FileWriter outputFw = new FileWriter(outputFile.getAbsoluteFile());
			BufferedWriter outputBw = new BufferedWriter(outputFw);
			for (int i=0; i<calib.getRowCount(); i++) {
				String line = new String();
				for (int j=0; j<calib.getColumnCount(); j++) {
					if (writeCols[j]) {
						line = line+calib.getValueAt(i, j);
						if (j<calib.getColumnCount()-1) {
							line = line+" ";
						}
					}
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

        double[] paddleNumbers = new double[NUM_PADDLES[0]];
        double[] paddleUncs = new double[NUM_PADDLES[0]];
        double[] values = new double[NUM_PADDLES[0]];
        double[] valueUncs = new double[NUM_PADDLES[0]];

        for (int p = 1; p <= NUM_PADDLES[0]; p++) {

            paddleNumbers[p - 1] = (double) p;
            paddleUncs[p - 1] = 0.0;
            values[p - 1] = getCentroid(sector, layer, p);
            valueUncs[p - 1] = 0.0;
        }

        GraphErrors summ = new GraphErrors("summ", paddleNumbers,
                values, paddleUncs, valueUncs);

        summ.setTitleX("Paddle Number");
        summ.setTitleY("Centroid");
        summ.setMarkerSize(MARKER_SIZE);
        summ.setLineThickness(MARKER_LINE_WIDTH);

        DataGroup dg = new DataGroup(1,1);
        dg.addDataSet(summ, 0);
        return dg;

    }
}
