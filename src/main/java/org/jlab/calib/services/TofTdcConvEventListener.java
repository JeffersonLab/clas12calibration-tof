package org.jlab.calib.services; 

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

import org.jlab.calib.services.ctof.CTOFCalibrationEngine;
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

public class TofTdcConvEventListener extends TOFCalibrationEngine {

    public final int OVERRIDE_LEFT = 0;
    public final int OVERRIDE_RIGHT = 1;

    public final double EXPECTED_CONV = TOFPaddle.NS_PER_CH;
    public final double ALLOWED_DIFF = 0.02;
    
    private String fitOption = "RQ";
	private String showPlotType = "CONV_LEFT";
	int backgroundSF = 2;
	boolean showSlices = false;
	
	// TDC ranges
	double        TDC_MIN = 0.0;
	double        TDC_MAX = 0.0;
	double        FIT_MIN = 0.0;
	double        FIT_MAX = 0.0;

    public TofTdcConvEventListener() {

        stepName = "TDC - Time";
        histTitle = "TDC_CONV";
        fileNamePrefix = "FTOF_CALIB_TDC_CONV_";
        // get file name here so that each timer update overwrites it
        filename = nextFileName();

        calib = new CalibrationConstants(3,
                "left/F:right/F");
        calib.setName("/calibration/ftof/tdc_conv");
        calib.setPrecision(5);

        // assign constraints to all paddles
        calib.addConstraint(3, EXPECTED_CONV*(1-ALLOWED_DIFF),
                EXPECTED_CONV*(1+ALLOWED_DIFF));
        calib.addConstraint(4, EXPECTED_CONV*(1-ALLOWED_DIFF),
                EXPECTED_CONV*(1+ALLOWED_DIFF));

    }
    
	
	public void populatePrevCalib2() {
		prevCalRead = true;
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
                    double convLeft = Double.parseDouble(lineValues[3]);
                    double convRight = Double.parseDouble(lineValues[4]);

                    convValues.addEntry(sector, layer, paddle);
                    convValues.setDoubleValue(convLeft,
                            "left", sector, layer, paddle);
                    convValues.setDoubleValue(convRight,
                            "right", sector, layer, paddle);
                    
                    line = bufferedReader.readLine();
                }

                bufferedReader.close();            
            }
            catch(FileNotFoundException ex) {
                ex.printStackTrace();
                System.out.println(
                        "Unable to open file '" + 
                                prevCalFilename + "'");                
            }
            catch(IOException ex) {
                System.out.println(
                        "Error reading file '" 
                                + prevCalFilename + "'");                   
                ex.printStackTrace();
            }            
        }
        else if (calDBSource==CAL_DEFAULT) {
        	System.out.println("Default");
            for (int sector = 1; sector <= 6; sector++) {
                for (int layer = 1; layer <= 3; layer++) {
                    int layer_index = layer - 1;
                    for (int paddle = 1; paddle <= NUM_PADDLES[layer_index]; paddle++) {
                        convValues.addEntry(sector, layer, paddle);
                        convValues.setDoubleValue(EXPECTED_CONV,
                                "left", sector, layer, paddle);
                        convValues.setDoubleValue(EXPECTED_CONV,
                                "right", sector, layer, paddle);
                        
                    }
                }
            }            
        }
        else if (calDBSource==CAL_DB) {
        	System.out.println("Database Run No: "+prevCalRunNo);
            DatabaseConstantProvider dcp = new DatabaseConstantProvider(prevCalRunNo, "default");
            convValues = dcp.readConstants("/calibration/ftof/tdc_conv");
            dcp.disconnect();
        }
        prevCalRead = true;
		System.out.println(stepName+" previous calibration values populated successfully");
    }

    @Override
    public void resetEventListener() {
    	init();
    }
    
    public void init() {
    	
    	double bb = TOFCalibrationEngine.BEAM_BUCKET;
		int bins = (int) (bb/2.004)*50;

        // perform init processing
        for (int sector = 1; sector <= 6; sector++) {
            for (int layer = 1; layer <= 3; layer++) {
                int layer_index = layer - 1;
                for (int paddle = 1; paddle <= NUM_PADDLES[layer_index]; paddle++) {

                    // create all the histograms
                    H2F histL = new H2F("tdcConvLeft",histTitle(sector,layer,paddle),
                    		50, TDC_MIN, TDC_MAX, 
                    		bins, -bb*0.5, bb*0.5);

                    histL.setName("tdcConvLeft");
                    histL.setTitleX("TDC Left");
                    histL.setTitleY("RF offset (ns)");

                    H2F histR = new H2F("tdcConvRight",histTitle(sector,layer,paddle),
                    				50, TDC_MIN, TDC_MAX, 
                    				bins, -bb*0.5, bb*0.5);

                    histR.setName("tdcConvRight");
                    histR.setTitleX("TDC Right");
                    histR.setTitleY("RF offset (ns)");
                    
                    // create all the functions and graphs
                    F1D convFuncLeft = new F1D("convFuncLeft", "[a]+[b]*x", FIT_MIN, FIT_MAX);
                    GraphErrors convGraphLeft = new GraphErrors("convGraphLeft");
                    convGraphLeft.setName("convGraphLeft");
                    convGraphLeft.setTitle(histTitle(sector,layer,paddle));
                    convFuncLeft.setLineColor(FUNC_COLOUR);
                    convFuncLeft.setLineWidth(FUNC_LINE_WIDTH);
                    convGraphLeft.setMarkerSize(MARKER_SIZE);
                    convGraphLeft.setLineThickness(MARKER_LINE_WIDTH);

                    F1D convFuncRight = new F1D("convFuncRight", "[a]+[b]*x", FIT_MIN, FIT_MAX);
                    GraphErrors convGraphRight = new GraphErrors("convGraphRight");
                    convGraphRight.setName("convGraphRight");
                    convGraphRight.setTitle(histTitle(sector,layer,paddle));
                    convFuncRight.setLineColor(FUNC_COLOUR);
                    convFuncRight.setLineWidth(FUNC_LINE_WIDTH);
                    convGraphRight.setMarkerSize(MARKER_SIZE);
                    convGraphRight.setLineThickness(MARKER_LINE_WIDTH);                    
                    
                    DataGroup dg = new DataGroup(2,2);
                    dg.addDataSet(histL, 0);
                    dg.addDataSet(convGraphLeft, 2);
                    dg.addDataSet(convFuncLeft, 2);
                    dg.addDataSet(histR, 1);
                    dg.addDataSet(convGraphRight, 3);
                    dg.addDataSet(convFuncRight, 3);

                    dataGroups.add(dg, sector,layer,paddle);
                    
                    setPlotTitle(sector,layer,paddle);

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

            if (paddle.goodTrackFound() && paddle.TDCL!=0 && paddle.TDCR!=0) {
                dataGroups.getItem(sector,layer,component).getH2F("tdcConvLeft").fill(
                         paddle.TDCL, 
                         (paddle.refTimeCorr()+(1000*BEAM_BUCKET) + (0.5*BEAM_BUCKET))%BEAM_BUCKET - 0.5*BEAM_BUCKET);
                dataGroups.getItem(sector,layer,component).getH2F("tdcConvRight").fill(
                        paddle.TDCR, 
                        (paddle.refTimeCorr()+(1000*BEAM_BUCKET) + (0.5*BEAM_BUCKET))%BEAM_BUCKET - 0.5*BEAM_BUCKET);

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

        H2F convHistL = dataGroups.getItem(sector,layer,paddle).getH2F("tdcConvLeft");
        H2F convHistR = dataGroups.getItem(sector,layer,paddle).getH2F("tdcConvRight");
        GraphErrors convGraphLeft = (GraphErrors) dataGroups.getItem(sector,layer,paddle).getData("convGraphLeft");
        GraphErrors convGraphRight = (GraphErrors) dataGroups.getItem(sector,layer,paddle).getData("convGraphRight");

        // fit function to the graph of means
        if (fitMethod==FIT_METHOD_SF) {
			ParallelSliceFitter psfL = new ParallelSliceFitter(convHistL);
			psfL.setFitMode(fitMode);
			psfL.setMinEvents(fitMinEvents);
			psfL.setBackgroundOrder(backgroundSF);
			psfL.setNthreads(1);
			//psfL.setShowProgress(false);
			setOutput(false);
			psfL.fitSlicesX();
			setOutput(true);
			if (showSlices) {
				psfL.inspectFits();
			}
			convGraphLeft.copy(fixGraph(psfL.getMeanSlices(),"convGraphLeft"));

			ParallelSliceFitter psfR = new ParallelSliceFitter(convHistR);
			psfR.setFitMode(fitMode);
			psfR.setMinEvents(fitMinEvents);
			psfR.setBackgroundOrder(backgroundSF);
			psfR.setNthreads(1);
			//psfR.setShowProgress(false);
			setOutput(false);
			psfR.fitSlicesX();
			setOutput(true);
			if (showSlices) {
				psfR.inspectFits();
				showSlices = false;
			}
			convGraphRight.copy(fixGraph(psfR.getMeanSlices(),"convGraphRight"));
		}
		else if (fitMethod==FIT_METHOD_MAX) {
			convGraphLeft.copy(maxGraph(convHistL, "convGraphLeft"));
			convGraphRight.copy(maxGraph(convHistR, "convGraphRight"));
		}
		else {
			convGraphLeft.copy(convHistL.getProfileX());
			convGraphRight.copy(convHistR.getProfileX());
		}

        // find the range for the fit
        double lowLimit;
        double highLimit;

        if (minRange != UNDEFINED_OVERRIDE) {
            // use custom values for fit
            lowLimit = minRange;
        }
        else {
            lowLimit = FIT_MIN;
        }

        if (maxRange != UNDEFINED_OVERRIDE) {
            // use custom values for fit
            highLimit = maxRange;
        }
        else {
            highLimit = FIT_MAX;
        }

        F1D convFuncLeft = dataGroups.getItem(sector,layer,paddle).getF1D("convFuncLeft");
        convFuncLeft.setRange(lowLimit, highLimit);

        convFuncLeft.setParameter(0, 0.0);
        convFuncLeft.setParameter(1, 0.0);
        try {
            DataFitter.fit(convFuncLeft, convGraphLeft, fitOption);
        } catch (Exception e) {
            System.out.println("Fit error with sector "+sector+" layer "+layer+" paddle "+paddle);
            e.printStackTrace();
        }
        
        F1D convFuncRight = dataGroups.getItem(sector,layer,paddle).getF1D("convFuncRight");
        convFuncRight.setRange(lowLimit, highLimit);

        convFuncRight.setParameter(0, 0.0);
        convFuncRight.setParameter(1, 0.0);
        try {
            DataFitter.fit(convFuncRight, convGraphRight, fitOption);
        } catch (Exception e) {
            System.out.println("Fit error with sector "+sector+" layer "+layer+" paddle "+paddle);
            e.printStackTrace();
        }
        
    }

    public void customFit(int sector, int layer, int paddle){

        String[] fields = { "Min range for fit:", "Max range for fit:", "SPACE",
				"Min Events per slice:", "Background order for slicefitter(-1=no background, 0=p0 etc):","SPACE",
                "Override tdc_conv left:", "Override tdc_conv right"};

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
            double overrideValueL = toDouble(panel.textFields[4].getText());
            double overrideValueR = toDouble(panel.textFields[5].getText());

			int minP = paddle;
			int maxP = paddle;
			int minS = sector;
			int maxS = sector;
			if (panel.applyLevel == panel.APPLY_P) {
				// if fitting one paddle then show inspectFits view
				showSlices = true;
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
					consts[OVERRIDE_LEFT] = overrideValueL;
					consts[OVERRIDE_RIGHT] = overrideValueR;

					fit(s, layer, p, minRange, maxRange);

					// update the table
					saveRow(s,layer,p);
				}
			}
            calib.fireTableDataChanged();

        }     
    }


    public Double getConvLeft(int sector, int layer, int paddle) {

        double conv = 0.0;
        double overrideVal = constants.getItem(sector, layer, paddle)[OVERRIDE_LEFT];

        if (overrideVal != UNDEFINED_OVERRIDE) {
            conv = overrideVal;
        }
        else {
            double gradient = dataGroups.getItem(sector,layer,paddle).getF1D("convFuncLeft") 
                    .getParameter(1);
            conv = EXPECTED_CONV - gradient;
        }
        return conv;
    }

    public Double getConvRight(int sector, int layer, int paddle) {

        double conv = 0.0;
        double overrideVal = constants.getItem(sector, layer, paddle)[OVERRIDE_RIGHT];

        if (overrideVal != UNDEFINED_OVERRIDE) {
            conv = overrideVal;
        }
        else {
            double gradient = dataGroups.getItem(sector,layer,paddle).getF1D("convFuncRight") 
                    .getParameter(1);
            conv = EXPECTED_CONV - gradient;
        }
        return conv;
    }  

    @Override
    public void saveRow(int sector, int layer, int paddle) {
        calib.setDoubleValue(getConvLeft(sector,layer,paddle),
                "left", sector, layer, paddle);
        calib.setDoubleValue(getConvRight(sector,layer,paddle),
                "right", sector, layer, paddle);
    }

    @Override
    public boolean isGoodPaddle(int sector, int layer, int paddle) {

        return (getConvLeft(sector,layer,paddle) >= EXPECTED_CONV*(1-ALLOWED_DIFF)
                &&
                getConvLeft(sector,layer,paddle) <= EXPECTED_CONV*(1+ALLOWED_DIFF)
                &&
                getConvRight(sector,layer,paddle) >= EXPECTED_CONV*(1-ALLOWED_DIFF)
                &&
                getConvRight(sector,layer,paddle) <= EXPECTED_CONV*(1+ALLOWED_DIFF)
                );

    }

    @Override
    public void setPlotTitle(int sector, int layer, int paddle) {
        // reset hist title as may have been set to null by show all 
        dataGroups.getItem(sector,layer,paddle).getGraph("convGraphLeft").setTitleX("TDC Left");
        dataGroups.getItem(sector,layer,paddle).getGraph("convGraphLeft").setTitleY("RF offset (ns)");
        dataGroups.getItem(sector,layer,paddle).getGraph("convGraphRight").setTitleX("TDC Right");
        dataGroups.getItem(sector,layer,paddle).getGraph("convGraphRight").setTitleY("RF offset (ns)");

    }

    @Override
    public void drawPlots(int sector, int layer, int paddle, EmbeddedCanvas canvas) {

		if (showPlotType == "CONV_LEFT") {
			GraphErrors graph = dataGroups.getItem(sector,layer,paddle).getGraph("convGraphLeft");
			if (graph.getDataSize(0) != 0) {
				graph.setTitleX("");
				graph.setTitleY("");
				canvas.draw(graph);
				canvas.draw(dataGroups.getItem(sector,layer,paddle).getF1D("convFuncLeft"), "same");
			}
		}
		else {
			GraphErrors graph = dataGroups.getItem(sector,layer,paddle).getGraph("convGraphRight");
			if (graph.getDataSize(0) != 0) {
				graph.setTitleX("");
				graph.setTitleY("");
				canvas.draw(graph);
				canvas.draw(dataGroups.getItem(sector,layer,paddle).getF1D("convFuncRight"), "same");
			}
		}
    }
    
	@Override
	public void showPlots(int sector, int layer) {

		showPlotType = "CONV_LEFT";
		stepName = "TDC conv - Left";
		super.showPlots(sector, layer);
		showPlotType = "CONV_RIGHT";
		stepName = "TDC conv - Right";
		super.showPlots(sector, layer);

	}    

    @Override
    public DataGroup getSummary(int sector, int layer) {

        int layer_index = layer-1;
        double[] paddleNumbers = new double[NUM_PADDLES[layer_index]];
        double[] convLefts = new double[NUM_PADDLES[layer_index]];
        double[] convRights = new double[NUM_PADDLES[layer_index]];
        double[] zeroUncs = new double[NUM_PADDLES[layer_index]];

        for (int p = 1; p <= NUM_PADDLES[layer_index]; p++) {

            paddleNumbers[p - 1] = (double) p;
            convLefts[p - 1] = getConvLeft(sector, layer, p);
            zeroUncs[p - 1] = 0.0;
            convRights[p - 1] = getConvRight(sector, layer, p);
        }

        GraphErrors summL = new GraphErrors("convLeftSumm", paddleNumbers,
                convLefts, zeroUncs, zeroUncs);

        summL.setTitleX("Paddle Number");
        summL.setTitleY("TDC Conv Left (ns)");
        summL.setMarkerSize(MARKER_SIZE);
        summL.setLineThickness(MARKER_LINE_WIDTH);

        GraphErrors summR = new GraphErrors("convRightSumm", paddleNumbers,
                convRights, zeroUncs, zeroUncs);

        summR.setTitleX("Paddle Number");
        summR.setTitleY("TDC Conv Right (ns)");
        summR.setMarkerSize(MARKER_SIZE);
        summR.setLineThickness(MARKER_LINE_WIDTH);

        DataGroup dg = new DataGroup(2,1);
        dg.addDataSet(summL, 0);
        dg.addDataSet(summR, 1);
        return dg;

    }

//	Think setAutoScale in the Engine class is enough
//    @Override
//	public void rescaleGraphs(EmbeddedCanvas canvas, int sector, int layer, int paddle) {
//		
//    	canvas.getPad(1).setAxisRange(TDC_MIN, TDC_MAX, -BEAM_BUCKET*0.5, BEAM_BUCKET*0.5);
//    	canvas.getPad(2).setAxisRange(TDC_MIN, TDC_MAX, -BEAM_BUCKET*0.5, BEAM_BUCKET*0.5);
//    	canvas.getPad(3).setAxisRange(TDC_MIN, TDC_MAX, -BEAM_BUCKET*0.5, BEAM_BUCKET*0.5);
//    	
//	}

}
