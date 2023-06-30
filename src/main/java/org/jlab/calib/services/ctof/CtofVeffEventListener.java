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

public class CtofVeffEventListener extends CTOFCalibrationEngine {

    public final int VEFF_OVERRIDE = 0;
    public final int VEFF_UNC_OVERRIDE = 1;
    public final int VEFF_LR_OVERRIDE = 2;

    public final double EXPECTED_VEFF = 16.0;
    final double MIN_VEFF = 13.5;
    final double MAX_VEFF = 16.5;
    
    private String fitOption = "RQ";
    int backgroundSF = -1;
    boolean showSlices = false;

    public CtofVeffEventListener() {

        stepName = "Effective Velocity";
        fileNamePrefix = "CTOF_CALIB_VEFF_";
        // get file name here so that each timer update overwrites it
        filename = nextFileName();

        calib = new CalibrationConstants(3,
                "veff_upstream/F:veff_downstream/F:veff_upstream_err/F:veff_downstream_err/F:veff_ud/F");
        calib.setName("/calibration/ctof/effective_velocity");
        calib.setPrecision(3);

        // assign constraints to all paddles
        // effective velocity to be within 10% of 16.0 cm/ns
        calib.addConstraint(3, MIN_VEFF, MAX_VEFF);
        calib.addConstraint(4, MIN_VEFF, MAX_VEFF);

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
                    double veff = Double.parseDouble(lineValues[3]);

                    veffValues.addEntry(sector, layer, paddle);
                    veffValues.setDoubleValue(veff,
                            "veff_upstream", sector, layer, paddle);
                    
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
                veffValues.addEntry(1, 1, paddle);
                veffValues.setDoubleValue(EXPECTED_VEFF,
                        "veff_upstream", 1, 1, paddle);
                        
            }
        }
        else if (calDBSource==CAL_DB) {
            System.out.println("Database Run No: "+prevCalRunNo);
            DatabaseConstantProvider dcp = new DatabaseConstantProvider(prevCalRunNo, "default");
            veffValues = dcp.readConstants("/calibration/ctof/effective_velocity");
            dcp.disconnect();
        }
        prevCalRead = true;
        System.out.println(stepName+" previous calibration values populated successfully");
    }

    @Override
    public void resetEventListener() {

        // perform init processing
        for (int paddle = 1; paddle <= NUM_PADDLES[0]; paddle++) {

            // create all the histograms
            int numBins = (int) (paddleLength(1,1,paddle)*0.6);  // 1 bin per 2cm + 10% either side
	        double min = paddleLength(1,1,paddle) * -0.6;
            double max = paddleLength(1,1,paddle) * 0.6;
            
            H2F hist = 
                    new H2F("veff",
                            "veff",
                            numBins, min, max, 
                            //200, -15.0, 15.0);
                            100, -15.0, -8.0);

            hist.setName("veff");
            hist.setTitle("Half Time Diff vs Position : Paddle "+paddle);
            hist.setTitleX("Hit position from tracking (cm)");
            hist.setTitleY("Half Time Diff (ns)");

            // create all the functions and graphs
            F1D veffFunc = new F1D("veffFunc", "[a]+[b]*x", -250.0, 250.0);
            //F1D veffFunc = new F1D("veffFunc", "[a]+[b]*x", 0.0, 500.0);
            //GraphErrors veffGraph = new GraphErrors("veffGraph",dummyPoint,dummyPoint,dummyPoint,dummyPoint);
            GraphErrors veffGraph = new GraphErrors("veffGraph");
            veffGraph.setName("veffGraph");
            veffFunc.setLineColor(FUNC_COLOUR);
            veffFunc.setLineWidth(FUNC_LINE_WIDTH);
            veffGraph.setMarkerSize(MARKER_SIZE);
            veffGraph.setLineThickness(MARKER_LINE_WIDTH);
            veffGraph.setTitle("VEFF Paddle "+paddle);

            DataGroup dg = new DataGroup(2,1);
            dg.addDataSet(hist, 0);
            dg.addDataSet(veffGraph, 1);
            dg.addDataSet(veffFunc, 1);
            dataGroups.add(dg, 1,1,paddle);

            setPlotTitle(1,1,paddle);

            // initialize the constants array
            Double[] consts = {UNDEFINED_OVERRIDE, UNDEFINED_OVERRIDE, UNDEFINED_OVERRIDE};
            // override values

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
           
            if (paddle.goodTrackFound() && paddle.includeInTiming()) {
                dataGroups.getItem(sector,layer,component).getH2F("veff").fill(
                        paddle.zPosCTOF(), 
                        paddle.veffHalfTimeDiff());
                
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

        H2F veffHist = dataGroups.getItem(sector,layer,paddle).getH2F("veff");


        // find the range for the fit
        double lowLimit;
        double highLimit;

        if (minRange != UNDEFINED_OVERRIDE) {
            // use custom values for fit
            lowLimit = minRange;
        }
        else {
            lowLimit = paddleLength(sector,layer,paddle) * -0.25;
            //lowLimit = paddleLength(sector,layer,paddle) * -0.4;
            //lowLimit = paddleLength(sector,layer,paddle) * 0.15;
        }

        if (maxRange != UNDEFINED_OVERRIDE) {
            // use custom values for fit
            highLimit = maxRange;
        }
        else {
            highLimit = paddleLength(sector,layer,paddle) * 0.25;
            //highLimit = paddleLength(sector,layer,paddle) * 0.4;
            //highLimit = paddleLength(sector,layer,paddle) * 0.85;
        }

        // fit function to the graph of means
        GraphErrors veffGraph = (GraphErrors) dataGroups.getItem(sector,layer,paddle).getData("veffGraph");
        
        if (fitMethod==FIT_METHOD_SF) {
            ParallelSliceFitter psf = new ParallelSliceFitter(veffHist);
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
            veffGraph.copy(fixGraph(psf.getMeanSlices(),"veffGraph"));
        }
        else if (fitMethod==FIT_METHOD_MAX) {
            maxGraphError = 0.3;
            veffGraph.copy(maxGraph(veffHist, "veffGraph"));
        }
        else {
            veffGraph.copy(veffHist.getProfileX());
        }
        
        double min = paddleLength(1,1,paddle) * -0.6;
        double max = paddleLength(1,1,paddle) * 0.6;

        //CTOFCalibration.canvas.getPad(1).setAxisRange(min, max, -12.0, -9.0);
        F1D veffFunc = dataGroups.getItem(sector,layer,paddle).getF1D("veffFunc");
        veffFunc.setRange(lowLimit, highLimit);

        veffFunc.setParameter(0, 0.0);
        veffFunc.setParameter(1, 1.0/16.0);
        //        veffFunc.setParLimits(0, -5.0, 5.0);
        //        veffFunc.setParLimits(1, 1.0/20.0, 1.0/12.0);
        try {
            DataFitter.fit(veffFunc, veffGraph, fitOption);

        } catch (Exception e) {
            System.out.println("Fit error with sector "+sector+" layer "+layer+" paddle "+paddle);
            e.printStackTrace();
        }
        
		// LC Mar 2020 Set function parameters to override value
		Double[] consts = constants.getItem(sector, layer, paddle);
		if (consts[VEFF_OVERRIDE] != UNDEFINED_OVERRIDE) {
			veffFunc.setParameter(1, 1.0/consts[VEFF_OVERRIDE]);
		}
		if (consts[VEFF_LR_OVERRIDE] != UNDEFINED_OVERRIDE) {
			veffFunc.setParameter(0, consts[VEFF_LR_OVERRIDE]/2.0);
		}		

    }

    public void customFit(int sector, int layer, int paddle){

        String[] fields = { "Min range for fit:", "Max range for fit:", "SPACE",
                "Min Events per slice:", "Background order for slicefitter(-1=no background, 0=p0 etc):","SPACE",
                "Override Effective Velocity:", "Override Effective Velocity uncertainty:",
        		"Override Effective Velocity UD:"};

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
            double overrideLR = toDouble(panel.textFields[6].getText());
            
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
				consts[VEFF_OVERRIDE] = overrideValue;
				consts[VEFF_UNC_OVERRIDE] = overrideUnc;
				consts[VEFF_LR_OVERRIDE] = overrideLR;

				fit(sector, layer, p, minRange, maxRange);

				// update the table
				saveRow(sector,layer,p);
			}
			
            calib.fireTableDataChanged();

        }     
    }


    public Double getVeff(int sector, int layer, int paddle) {

        double veff = 0.0;
        double overrideVal = constants.getItem(sector, layer, paddle)[VEFF_OVERRIDE];

        if (overrideVal != UNDEFINED_OVERRIDE) {
            veff = overrideVal;
        }
        else {
            double gradient = dataGroups.getItem(sector,layer,paddle).getF1D("veffFunc") 
                    .getParameter(1);
            if (gradient==0.0) {
                veff=0.0;
            }
            else {
                veff = 1/gradient;
            }
        }
        return veff;
    }

    public Double getVeffError(int sector, int layer, int paddle){

        double veffError = 0.0;
        double overrideVal = constants.getItem(sector, layer, paddle)[VEFF_UNC_OVERRIDE];

        if (overrideVal != UNDEFINED_OVERRIDE) {
            veffError = overrideVal;
        }
        else {
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
        }
        return veffError;
    }    
    
	public Double getLR(int sector, int layer, int paddle){

		double lr = 0.0;
		double overrideVal = constants.getItem(sector, layer, paddle)[VEFF_LR_OVERRIDE];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			lr = overrideVal;
		}
		else {

			double intercept = dataGroups.getItem(sector,layer,paddle).getF1D("veffFunc")
					.getParameter(0);
			lr = 2.0 * intercept;
		}
		return lr;
	}	    

    @Override
    public void saveRow(int sector, int layer, int paddle) {
        calib.setDoubleValue(getVeff(sector,layer,paddle),
                "veff_upstream", sector, layer, paddle);
        calib.setDoubleValue(getVeff(sector,layer,paddle),
                "veff_downstream", sector, layer, paddle);
        calib.setDoubleValue(getVeffError(sector,layer,paddle),
                "veff_upstream_err", sector, layer, paddle);
        calib.setDoubleValue(getVeffError(sector,layer,paddle),
                "veff_downstream_err", sector, layer, paddle);
		calib.setDoubleValue(getLR(sector,layer,paddle),
				"veff_ud", sector, layer, paddle);

    }
    
	@Override
	public void writeFile(String filename) {
		
		boolean[] writeCols = {true,true,true,
							   true,true,true,true,false};

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
    public boolean isGoodPaddle(int sector, int layer, int paddle) {

        return (getVeff(sector,layer,paddle) >= MIN_VEFF
                &&
                getVeff(sector,layer,paddle) <= MAX_VEFF);

    }

    @Override
    public void setPlotTitle(int sector, int layer, int paddle) {
        // reset hist title as may have been set to null by show all 
        dataGroups.getItem(sector,layer,paddle).getGraph("veffGraph").setTitleX("Hit position from tracking (cm)");
        dataGroups.getItem(sector,layer,paddle).getGraph("veffGraph").setTitleY("Half Time Diff (ns)");
    }

    @Override
    public void drawPlots(int sector, int layer, int paddle, EmbeddedCanvas canvas) {
        
        GraphErrors graph = dataGroups.getItem(sector,layer,paddle).getGraph("veffGraph");
        if (graph.getDataSize(0) != 0) {
            graph.setTitleX("");
            graph.setTitleY("");
            canvas.draw(graph);
            canvas.draw(dataGroups.getItem(sector,layer,paddle).getF1D("veffFunc"), "same");
        }
    }

    @Override
    public DataGroup getSummary(int sector, int layer) {

        // draw the stats
        //        TCanvas c1 = new TCanvas("Veff Stats",1200,800);
        //        c1.setDefaultCloseOperation(c1.HIDE_ON_CLOSE);
        //        c1.cd(0);
        //        c1.draw(veffStatHist);        

        int layer_index = layer-1;
        double[] paddleNumbers = new double[NUM_PADDLES[0]];
        double[] paddleUncs = new double[NUM_PADDLES[0]];
        double[] veffs = new double[NUM_PADDLES[0]];
        double[] veffUncs = new double[NUM_PADDLES[0]];
		double[] lrs = new double[NUM_PADDLES[layer_index]];
		double[] lrUncs = new double[NUM_PADDLES[layer_index]];
        
        for (int p = 1; p <= NUM_PADDLES[0]; p++) {

            paddleNumbers[p - 1] = (double) p;
            paddleUncs[p - 1] = 0.0;
            veffs[p - 1] = getVeff(sector, layer, p);
            //veffUncs[p - 1] = getVeffError(sector, layer, p);
            veffUncs[p - 1] = 0.0;
			lrs[p-1] = getLR(sector,layer,p);
			lrUncs[p-1] = 0.0;
        }

        GraphErrors summ = new GraphErrors("summ", paddleNumbers,
                veffs, paddleUncs, veffUncs);
		GraphErrors lrSumm = new GraphErrors("lrSumm", paddleNumbers,
				lrs, paddleUncs, lrUncs);
        
        // add the previous calibration values
//        summ.setMarkerStyle(1);
//        for (int p = 1; p <= NUM_PADDLES[layer_index]; p++) {
//
//            summ.addPoint(p, TOFCalibrationEngine.veffValues.getDoubleValue("veff_left", sector, layer, p),
//                          0.0, 0.0);
//        }
        

        summ.setTitleX("Paddle Number");
        summ.setTitleY("Effective velocity (cm/ns)");
        summ.setMarkerSize(MARKER_SIZE);
        summ.setLineThickness(MARKER_LINE_WIDTH);
		lrSumm.setTitleX("Paddle Number");
		lrSumm.setTitleY("Up down (ns)");
		lrSumm.setMarkerSize(MARKER_SIZE);
		lrSumm.setLineThickness(MARKER_LINE_WIDTH);

        DataGroup dg = new DataGroup(2,1);
        dg.addDataSet(summ, 0);
		dg.addDataSet(lrSumm, 1);
        return dg;

    }
    
//  @Override
//    public void rescaleGraphs(EmbeddedCanvas canvas, int sector, int layer, int paddle) {
//        
//        double min = paddleLength(sector,layer,paddle) * -0.1;
//        double max = paddleLength(sector,layer,paddle) * 1.1;
//
//        canvas.getPad(1).setAxisRange(min, max, 0.0, 30.0);
//        
//    }

}
