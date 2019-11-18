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
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;

import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.fitter.ParallelSliceFitter;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
//import org.jlab.calib.temp.DataGroup;
import org.jlab.groot.math.F1D;
import org.jlab.io.base.DataEvent;
import org.jlab.utils.groups.IndexedList;

public class TofTimeWalkEventListener extends TOFCalibrationEngine {

	// indices for constants
	public final int TW0_OVERRIDE = 0;
	public final int TW1_OVERRIDE = 1;
	public final int TW2_OVERRIDE = 2;
	
	
	// Preferred ranges
	private final double[]        FIT_MIN = {0.0, 2.0, 2.0, 2.0};
	private final double[]        FIT_MAX = {0.0, 20.0, 20.0, 20.0};
	private final double[]        ENERGY_MIN = {0.0,  0.0,  0.0,  0.0};
	private final double[]        ENERGY_MAX = {0.0, 50.0, 50.0, 50.0};
	
	// Expected values for colour coding
	private final double[] EXPECTED_TW0 = {1.5, 1.5, 1.5};
	private final double[] EXPECTED_TW1 = {-0.03, -0.03, -0.03};
	private final double[] EXPECTED_TW2 = {3.8, 3.8, 3.8};
	
	// Preferred bins
	private int[] xbins = {0, 166, 87, 166};
	private int ybins = 40;

	final double fitTW0 = 1.5;  // default values for the constants
	final double fitTW1 = -0.03;
	final double fitTW2 = 3.8;
	
	private IndexedList<H2F> offsetHists = new IndexedList<H2F>(4);  // indexed by s,l,c, offset (in beam bucket multiples)
	private int NUM_OFFSET_HISTS = 20;
	
	private String fitOption = "RQ";
	int backgroundSF = 2;
	boolean showSlices = false;
	
	public TofTimeWalkEventListener() {
		
		stepName = "Timewalk";
		histTitle = "TW";
		fileNamePrefix = "FTOF_CALIB_TIMEWALK_";
		// get file name here so that each timer update overwrites it
		filename = nextFileName();

		calib = new CalibrationConstants(3,
				"tw0_left/F:tw1_left/F:tw2_left");

		calib.setName("/calibration/ftof/time_walk");
		calib.setPrecision(4);
		
		for (int i=0; i<3; i++) {

			int layer = i+1;
			//calib.addConstraint(3, EXPECTED_TW0[i]*0.9, 
			//		EXPECTED_TW0[i]*1.1, 1, layer);
			// calib.addConstraint(calibration column, min value, max value,
			// col to check if constraint should apply, value of col if constraint should be applied);
			// (omit last two if applying to all rows)
			//calib.addConstraint(4, EXPECTED_TW1[i]*0.9, 
			//		EXPECTED_TW1[i]*1.1, 1, layer);
			//calib.addConstraint(5, EXPECTED_TW2[i]*0.9, 
			//		EXPECTED_TW2[i]*1.1, 1, layer);

		}
		

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
					double tw0L = Double.parseDouble(lineValues[3]);
					double tw1L = Double.parseDouble(lineValues[4]);
					double tw2L = Double.parseDouble(lineValues[5]);
					double tw0R = Double.parseDouble(lineValues[6]);
					double tw1R = Double.parseDouble(lineValues[7]);
					double tw2R = Double.parseDouble(lineValues[8]);

					timeWalkValues.addEntry(sector, layer, paddle);
					timeWalkValues.setDoubleValue(tw0L,
							"tw0_left", sector, layer, paddle);
					timeWalkValues.setDoubleValue(tw1L,
							"tw1_left", sector, layer, paddle);
					timeWalkValues.setDoubleValue(tw2L,
							"tw2_left", sector, layer, paddle);
					timeWalkValues.setDoubleValue(tw0R,
							"tw0_right", sector, layer, paddle);
					timeWalkValues.setDoubleValue(tw1R,
							"tw1_right", sector, layer, paddle);
					timeWalkValues.setDoubleValue(tw2R,
							"tw2_right", sector, layer, paddle);
					
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
						timeWalkValues.addEntry(sector, layer, paddle);
						timeWalkValues.setDoubleValue(fitTW0,
								"tw0_left", sector, layer, paddle);
						timeWalkValues.setDoubleValue(fitTW1,
								"tw1_left", sector, layer, paddle);
						timeWalkValues.setDoubleValue(fitTW2,
								"tw2_left", sector, layer, paddle);
						timeWalkValues.setDoubleValue(0.0,
								"tw0_right", sector, layer, paddle);
						timeWalkValues.setDoubleValue(0.0,
								"tw1_right", sector, layer, paddle);
						timeWalkValues.setDoubleValue(0.0,
								"tw2_right", sector, layer, paddle);
					}
				}
			}			
		}
		else if (calDBSource==CAL_DB) {
			System.out.println("Database Run No: "+prevCalRunNo);
			DatabaseConstantProvider dcp = new DatabaseConstantProvider(prevCalRunNo, "default");
			timeWalkValues = dcp.readConstants("/calibration/ftof/time_walk");
			dcp.disconnect();
		}
		prevCalRead = true;
		System.out.println(stepName+" previous calibration values populated successfully");
	}
	
	@Override
	public void resetEventListener() {
		
		// perform init processing
		
		double bb = BEAM_BUCKET;
		ybins = (int) (bb/2.004)*ybins;
		
		// create the histograms
		for (int sector = 1; sector <= 6; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int layer_index = layer - 1;
				for (int paddle = 1; paddle <= NUM_PADDLES[layer_index]; paddle++) {

					// data group format is
					DataGroup dg = new DataGroup(1,3);
					
					// create all the histograms
					H2F trHist = new H2F("trHist",
							"",
							xbins[layer], ENERGY_MIN[layer], ENERGY_MAX[layer],
							ybins, -bb*0.5, bb*0.5);

					trHist.setTitleY("#Delta t");

					dg.addDataSet(trHist, 0);
					
					// create all the functions and graphs
					double tw0 = TOFCalibrationEngine.timeWalkValues.getDoubleValue("tw0_left",
											sector, layer, paddle);
					double tw1 = TOFCalibrationEngine.timeWalkValues.getDoubleValue("tw1_left",
							sector, layer, paddle);
					double tw2 = TOFCalibrationEngine.timeWalkValues.getDoubleValue("tw2_left",
							sector, layer, paddle);
					// the fit function is the difference between the new model and the old
					// so that it fits within a beam bucket window without wrapping 
					String funcText = "[a]+([b]*exp([c]*x)+[d]/x) - ("+tw0+"*exp("+tw1+"*x)+"+tw2+"/x)";
					//System.out.println("funcText "+funcText);
					
					F1D trFunc = new F1D("trFunc", funcText, FIT_MIN[layer], FIT_MAX[layer]);
										
					GraphErrors trGraph = new GraphErrors("trGraph");
					trGraph.setName("trGraph");   
					trGraph.setTitle(histTitle(sector,layer,paddle));
					trGraph.setTitleX("Energy (MeV)");
					trGraph.setTitleY("#Delta t");
					
					trFunc.setLineColor(FUNC_COLOUR);
					trFunc.setLineWidth(FUNC_LINE_WIDTH);
					trGraph.setMarkerSize(MARKER_SIZE);
					trGraph.setLineThickness(MARKER_LINE_WIDTH);

					dg.addDataSet(trFunc, 2);
					dg.addDataSet(trGraph, 2);
					
					dataGroups.add(dg,sector,layer,paddle);

					setPlotTitle(sector,layer,paddle);

					// now create the offset hists
					//NUM_OFFSET_HISTS = (int) (bb/2.004)*NUM_OFFSET_HISTS;
					for (int i=0; i<NUM_OFFSET_HISTS; i++) {

						double n = NUM_OFFSET_HISTS;
						double offset = i*(BEAM_BUCKET/n);
						H2F offHist = new H2F("offsetHist",
								histTitle(sector,layer,paddle),
								xbins[layer], ENERGY_MIN[layer], ENERGY_MAX[layer],
								ybins, -bb*0.5, bb*0.5);
						offHist.setTitleY("#Delta t");
						offHist.setTitleX("Energy (MeV)");

						offsetHists.add(offHist, sector,layer,paddle,i);
					}

					// initialize the constants array
					Double[] consts = {UNDEFINED_OVERRIDE, UNDEFINED_OVERRIDE, UNDEFINED_OVERRIDE};
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

			// Fill the histograms
			int sector = paddle.getDescriptor().getSector();
			int layer = paddle.getDescriptor().getLayer();
			int component = paddle.getDescriptor().getComponent();

			// fill timeResidual vs Energy
			if (paddle.goodTrackFound()) {
			
				dataGroups.getItem(sector,layer,component).getH2F("trHist").fill(paddle.ENERGY, paddle.deltaTTW(0.0));
				
				// fill the offset histograms
				for (int i=0; i<NUM_OFFSET_HISTS; i++) {
					double n = NUM_OFFSET_HISTS;
					double offset = i*(BEAM_BUCKET/n);
					offsetHists.getItem(sector,layer,component,i).fill(paddle.ENERGY, paddle.deltaTTW(offset));
				}
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
	public void fit(int sector, int layer, int paddle, double minRange, double maxRange) {
				
		double startChannelForFit = 0.0;
		double endChannelForFit = 0.0;
		if (minRange==UNDEFINED_OVERRIDE) {
			// default value
			startChannelForFit = FIT_MIN[layer];
		}
		else {
			// custom value
			startChannelForFit = minRange;
		}
		if (maxRange==UNDEFINED_OVERRIDE) {
			//default value
			endChannelForFit = FIT_MAX[layer];
		}
		else {
			// custom value
			endChannelForFit = maxRange;
		}		

		// Find the best offset hist
		
		int offsetIdxLeft = 0;
		double minYmaxLeft = 9999.0;

		for (int i=0; i<NUM_OFFSET_HISTS; i++) {
			
			// Get the max position of the y projection
			H1F yProjL = offsetHists.getItem(sector,layer,paddle,i).projectionY();
			int ymaxBinL = yProjL.getMaximumBin();
			double ymaxL = yProjL.getxAxis().getBinCenter(ymaxBinL);
			
			if (Math.abs(ymaxL) < minYmaxLeft) {
				minYmaxLeft = Math.abs(ymaxL);
				offsetIdxLeft = i;
			}
		}

		//System.out.println("offsetIdxLeft "+offsetIdxLeft);
		//System.out.println("offsetIdxRight "+offsetIdxRight);

		// add the correct offset hist to the data group
		DataGroup dg = dataGroups.getItem(sector,layer,paddle);
		dg.addDataSet(offsetHists.getItem(sector,layer,paddle,offsetIdxLeft), 1);
		
		H2F twL = offsetHists.getItem(sector,layer,paddle,offsetIdxLeft);
		
		GraphErrors twLGraph = (GraphErrors) dataGroups.getItem(sector, layer, paddle).getData("trGraph"); 
		if (fitMethod==FIT_METHOD_SF) {
			ParallelSliceFitter psfL = new ParallelSliceFitter(twL);
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
			twLGraph.copy(fixGraph(psfL.getMeanSlices(),"trGraph"));
		}
		else if (fitMethod==FIT_METHOD_MAX) {
			twLGraph.copy(maxGraph(twL, "trGraph"));
		}
		else {
			twLGraph.copy(twL.getProfileX());
		}
					
		// fit function to the graph of means
		F1D twLFunc = dataGroups.getItem(sector,layer,paddle).getF1D("trFunc");
		twLFunc.setRange(startChannelForFit, endChannelForFit);
		twLFunc.setParameter(1, 0.0);
		twLFunc.setParameter(1, fitTW0);
		twLFunc.setParameter(2, fitTW1);
		twLFunc.setParameter(3, fitTW2);
		try {
			DataFitter.fit(twLFunc, twLGraph, fitOption);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
				
	}
		
	public void writeSlicesFile(int sector, int layer, int paddle) {

		try { 

			// Open the output file
			File outputFile = new File("slicefitterTest"+sector+layer+paddle+".txt");
			FileWriter outputFw = new FileWriter(outputFile.getAbsoluteFile());
			BufferedWriter outputBw = new BufferedWriter(outputFw);

			H2F twL = dataGroups.getItem(sector,layer,paddle).getH2F("offsetHist");
			
			for (int i=0; i<twL.getXAxis().getNBins(); i++) {
				for (int j=0; j<twL.getYAxis().getNBins(); j++) {
					
					DecimalFormat df = new DecimalFormat("0.000");

					String line = new String();
					line = df.format(twL.getXAxis().getBinCenter(i)) + " " +
						   df.format(twL.getYAxis().getBinCenter(j)) + " " +
						   df.format(twL.getBinContent(i, j));
					outputBw.write(line);
					outputBw.newLine();
				}
			}

			outputBw.close();
		}
		catch(IOException ex) {
			ex.printStackTrace();
		}

	}
	
	@Override
	public void customFit(int sector, int layer, int paddle){
		
		//showOffsetHists(sector,layer,paddle);
		//writeSlicesFile(sector,layer,paddle);
//		showSlices(sector,layer,paddle);

		String[] fields = {"Min range for fit:", "Max range for fit:", "SPACE", 
				"Min Events per slice:", "Background order for slicefitter(-1=no background, 0=p0 etc):","SPACE",
				"Override TW0:", "Override TW1:", "Override TW2:"};

		TOFCustomFitPanel panel = new TOFCustomFitPanel(fields,sector,layer, TOFCustomFitPanel.USE_RANGE_Y);

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
			double overrideTW0 = toDouble(panel.textFields[4].getText());
			double overrideTW1 = toDouble(panel.textFields[5].getText());
			double overrideTW2 = toDouble(panel.textFields[6].getText());

			int minP = paddle;
			int maxP = paddle;
			int minL = layer;
			int maxL = layer;
			int minS = sector;
			int maxS = sector;
			if (panel.applyLevel == panel.APPLY_R) {
				if (panel.minS.getText().compareTo("") !=0) {
					minS =Integer.parseInt(panel.minS.getText());
				}
				if (panel.maxS.getText().compareTo("") !=0) {
					maxS =Integer.parseInt(panel.maxS.getText()); 
				}
				if (panel.minL.getText().compareTo("") !=0) {
					minL =Integer.parseInt(panel.minL.getText());
				}
				if (panel.maxL.getText().compareTo("") !=0) {
					maxL =Integer.parseInt(panel.maxL.getText()); 
				}
				if (panel.minP.getText().compareTo("") !=0) {
					minP =Integer.parseInt(panel.minP.getText());
				}
				if (panel.maxP.getText().compareTo("") !=0) {
					maxP =Integer.parseInt(panel.maxP.getText()); 
				}
			}
			else {
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
			}
			
			for (int s=minS; s<=maxS; s++) {
				for (int p=minP; p<=maxP; p++) {
					// save the override values
					Double[] consts = constants.getItem(s, layer, p);
					consts[TW0_OVERRIDE] = overrideTW0;
					consts[TW1_OVERRIDE] = overrideTW1;
					consts[TW2_OVERRIDE] = overrideTW2;

					fit(s, layer, p, minRange, maxRange);

					// update the table
					saveRow(s,layer,p);
				}
			}
			calib.fireTableDataChanged();

		}     
	}

	public Double getTW0(int sector, int layer, int paddle) {

		double tw0 = 0.0;
		double overrideVal = constants.getItem(sector, layer, paddle)[TW0_OVERRIDE];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			tw0 = overrideVal;
		}
		else {
			tw0 = dataGroups.getItem(sector,layer,paddle).getF1D("trFunc").getParameter(1);
		}
		return tw0;
	}     

	public Double getTW1(int sector, int layer, int paddle) {

		double tw1 = 0.0;
		double overrideVal = constants.getItem(sector, layer, paddle)[TW1_OVERRIDE];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			tw1 = overrideVal;
		}
		else {
			tw1 = dataGroups.getItem(sector,layer,paddle).getF1D("trFunc").getParameter(2);
		}
		return tw1;
	}     
	
	public Double getTW2(int sector, int layer, int paddle) {

		double tw2 = 0.0;
		double overrideVal = constants.getItem(sector, layer, paddle)[TW2_OVERRIDE];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			tw2 = overrideVal;
		}
		else {
			tw2 = dataGroups.getItem(sector,layer,paddle).getF1D("trFunc").getParameter(3);
		}
		return tw2;
	}     
	

	@Override
	public void saveRow(int sector, int layer, int paddle) {

		calib.setDoubleValue(getTW0(sector,layer,paddle),
				"tw0_left", sector, layer, paddle);
		calib.setDoubleValue(getTW1(sector,layer,paddle),
				"tw1_left", sector, layer, paddle);
		calib.setDoubleValue(getTW2(sector,layer,paddle),
				"tw2_left", sector, layer, paddle);
		
	}
	
	@Override
	public void writeFile(String filename) {

		try { 

			// Open the output file
			File outputFile = new File(filename);
			FileWriter outputFw = new FileWriter(outputFile.getAbsoluteFile());
			BufferedWriter outputBw = new BufferedWriter(outputFw);

			for (int sector = 1; sector <= 6; sector++) {
				for (int layer = 1; layer <= 3; layer++) {
					int layer_index = layer - 1;
					for (int paddle = 1; paddle <= NUM_PADDLES[layer_index]; paddle++) {
						String line = new String();
						line = sector+" "+layer+" "+paddle+" "
								+new DecimalFormat("0.000").format(getTW0(sector,layer,paddle))+" "
								+new DecimalFormat("0.000").format(getTW1(sector,layer,paddle))+" "
								+new DecimalFormat("0.000").format(getTW2(sector,layer,paddle))+" "
								+"0.000"+" 0.000"+" 0.000";
						outputBw.write(line);
						outputBw.newLine();
					}
				}
			}

			outputBw.close();
		}
		catch(IOException ex) {
			System.out.println(
					"Error writing file '" );                   
			// Or we could just do this: 
			ex.printStackTrace();
		}

	}	

	@Override
	public void setPlotTitle(int sector, int layer, int paddle) {
		// reset hist title as may have been set to null by show all 
//		dataGroups.getItem(sector,layer,paddle).getGraph("trLeftGraph").setTitleX("ADC LEFT");
//		dataGroups.getItem(sector,layer,paddle).getGraph("trLeftGraph").setTitleY("Time residual (ns)");
//		dataGroups.getItem(sector,layer,paddle).getGraph("trRightGraph").setTitleX("ADC LEFT");
//		dataGroups.getItem(sector,layer,paddle).getGraph("trRightGraph").setTitleY("Time residual (ns)");
		//System.out.println("Setting TW graph titles");
	}

	//@Override
	public void drawPlotsGraphs(int sector, int layer, int paddle, EmbeddedCanvas canvas) {

		GraphErrors graph = dataGroups.getItem(sector,layer,paddle).getGraph("trGraph");
		if (graph.getDataSize(0) != 0) {
			//graph.setTitleX("");
			//graph.setTitleY("");
			canvas.draw(graph);
			canvas.draw(dataGroups.getItem(sector,layer,paddle).getF1D("trFunc"), "same");
		}

	}

	@Override
	public void drawPlots(int sector, int layer, int paddle, EmbeddedCanvas canvas) {

		H2F hist = new H2F();
		F1D func = new F1D("trFunc");
		hist = dataGroups.getItem(sector,layer,paddle).getH2F("offsetHist");
		func = dataGroups.getItem(sector,layer,paddle).getF1D("trFunc");

		//hist.setTitleX("");
		//hist.setTitleY("");
		canvas.draw(hist);    
		canvas.draw(func, "same");
		//canvas.draw(smfunc, "same");
	}

	@Override
	public void showPlots(int sector, int layer) {

		stepName = "Time walk";
		super.showPlots(sector, layer);

	}
	
	public void showSlices(int sector, int layer, int component) {

		JFrame frame = new JFrame("Time walk slices Sector "+sector+"Layer "+layer+" Component"+component);
		frame.setSize(1000, 800);

		JTabbedPane pane = new JTabbedPane();
		EmbeddedCanvas leftCanvas = new EmbeddedCanvas();
		leftCanvas.divide(4, 4);

		H2F twL = dataGroups.getItem(sector,layer,component).getH2F("offsetHist");
		
		ArrayList<H1F> lSlices = twL.getSlicesX();
		for (int i=0; i < twL.getXAxis().getNBins(); i++) {
			leftCanvas.cd(i);
			leftCanvas.draw(lSlices.get(i));
		}
		
		pane.add("Left slices",leftCanvas);
		frame.add(pane);
		// frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

	}	

	public void showOffsetHists(int sector, int layer, int component) {

		JFrame frame = new JFrame("Time walk offset histograms Sector "+sector+"Layer "+layer+" Component"+component);
		frame.setSize(1000, 800);

		JTabbedPane pane = new JTabbedPane();
		EmbeddedCanvas leftCanvas = new EmbeddedCanvas();
		leftCanvas.divide(4, NUM_OFFSET_HISTS/2);
		EmbeddedCanvas rightCanvas = new EmbeddedCanvas();
		rightCanvas.divide(4, NUM_OFFSET_HISTS/2);

		// create function for the nominal values
		//F1D nomFunc = new F1D("nomFunc", "([a]/(x^[b]))", FIT_MIN[layer], FIT_MAX[layer]);
		//F1D nomFunc = new F1D("nomFunc", "(([a]/(x^[b]))-(40.0/(x^0.5))+[c])", FIT_MIN[layer], FIT_MAX[layer]);
		F1D nomFunc = new F1D("nomFunc", "(([a]/(x^[b]))-(40.0/(x^0.5)))", FIT_MIN[layer], FIT_MAX[layer]);
		nomFunc.setParLimits(0, 39.9, 40.1);
		nomFunc.setParLimits(1, 0.49, 0.51);
		//nomFunc.setParLimits(2, 0.99, 1.01);

		nomFunc.setLineWidth(FUNC_LINE_WIDTH);
		nomFunc.setLineColor(FUNC_COLOUR);


		for (int i=0; i < NUM_OFFSET_HISTS; i++) {

			leftCanvas.cd(2*i);
			H2F leftHist = offsetHists.getItem(sector,layer,component,i);
			leftCanvas.draw(leftHist);
			leftCanvas.draw(nomFunc, "same");

			leftCanvas.cd(2*i+1);
			GraphErrors leftGraph = leftHist.getProfileX();
			leftGraph.setMarkerSize(MARKER_SIZE);
			leftGraph.setLineThickness(MARKER_LINE_WIDTH);
			if (leftGraph.getDataSize(0) != 0) {
				try {
					DataFitter.fit(nomFunc, leftGraph, fitOption);
				}
				catch (Exception e) {
					e.printStackTrace();
				}

				//leftGraph.setTitle("Chi squared "+nomFunc.getChiSquare());
				//System.out.println("Chi Squared "+i+nomFunc.getChiSquare());
				leftCanvas.draw(leftGraph);
				leftCanvas.draw(nomFunc, "same");
			}
			
			rightCanvas.cd(2*i);
			H2F rightHist = offsetHists.getItem(sector,layer,component,i);
			rightCanvas.draw(rightHist);
			rightCanvas.draw(nomFunc, "same");

			rightCanvas.cd(2*i+1);
			GraphErrors rightGraph = rightHist.getProfileX();
			rightGraph.setMarkerSize(MARKER_SIZE);
			rightGraph.setLineThickness(MARKER_LINE_WIDTH);
			if (rightGraph.getDataSize(0) != 0) {
				try {
					DataFitter.fit(nomFunc, rightGraph, fitOption);
				}
				catch (Exception e) {
					e.printStackTrace();
				}

				//rightGraph.setTitle("Chi squared "+nomFunc.getChiSquare());
				rightCanvas.draw(rightGraph);
				rightCanvas.draw(nomFunc, "same");
				
			}
		}			

		pane.add("Time residual vs ADC (left)",leftCanvas);
		pane.add("Time residual vs ADC (right)",rightCanvas);
		frame.add(pane);
		// frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

	}

	@Override
	public boolean isGoodPaddle(int sector, int layer, int paddle) {

		return true;
//				(getTW0(sector,layer,paddle) >= EXPECTED_TW0[layer-1]*0.9  && 
//				getTW0(sector,layer,paddle) <= EXPECTED_TW0[layer-1]*1.1 &&
//				getTW1(sector,layer,paddle) >= EXPECTED_TW1[layer-1]*0.9  && 
//				getTW1(sector,layer,paddle) <= EXPECTED_TW1[layer-1]*1.1 &&
//				getTW2(sector,layer,paddle) >= EXPECTED_TW2[layer-1]*0.9  && 
//				getTW2(sector,layer,paddle) <= EXPECTED_TW2[layer-1]*1.1);
		
	}

	@Override
	public DataGroup getSummary(int sector, int layer) {
		
		int layer_index = layer-1;
		double[] paddleNumbers = new double[NUM_PADDLES[layer_index]];
		double[] paddleUncs = new double[NUM_PADDLES[layer_index]];
		double[] TW0s = new double[NUM_PADDLES[layer_index]];
		double[] zeroUncs = new double[NUM_PADDLES[layer_index]];
		double[] TW1s = new double[NUM_PADDLES[layer_index]];
		double[] TW2s = new double[NUM_PADDLES[layer_index]];

		for (int p = 1; p <= NUM_PADDLES[layer_index]; p++) {

			paddleNumbers[p - 1] = (double) p;
			paddleUncs[p - 1] = 0.0;
			TW0s[p - 1] = getTW0(sector, layer, p);
			TW1s[p - 1] = getTW1(sector, layer, p);
			TW2s[p - 1] = getTW2(sector, layer, p);
			zeroUncs[p - 1] = 0.0;
		}

		GraphErrors tw0Summ = new GraphErrors("TW0Summ", paddleNumbers,
				TW0s, paddleUncs, zeroUncs);

		tw0Summ.setTitleX("Paddle Number");
		tw0Summ.setTitleY("TW0");
		tw0Summ.setMarkerSize(MARKER_SIZE);
		tw0Summ.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors tw1Summ = new GraphErrors("TW1Summ", paddleNumbers,
				TW1s, paddleUncs, zeroUncs);

		tw1Summ.setTitleX("Paddle Number");
		tw1Summ.setTitleY("TW1");
		tw1Summ.setMarkerSize(MARKER_SIZE);
		tw1Summ.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors tw2Summ = new GraphErrors("TW2Summ", paddleNumbers,
				TW2s, paddleUncs, zeroUncs);

		tw2Summ.setTitleX("Paddle Number");
		tw2Summ.setTitleY("TW2");
		tw2Summ.setMarkerSize(MARKER_SIZE);
		tw2Summ.setLineThickness(MARKER_LINE_WIDTH);


		DataGroup dg = new DataGroup(3,1);
		dg.addDataSet(tw0Summ, 0);
		dg.addDataSet(tw1Summ, 1);
		dg.addDataSet(tw2Summ, 2);

		return dg;

	}

}