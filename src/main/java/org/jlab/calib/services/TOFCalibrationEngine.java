package org.jlab.calib.services;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import org.jlab.detector.calib.tasks.CalibrationEngine;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
//import org.jlab.calib.temp.DataGroup;
import org.jlab.groot.math.F1D;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataEventType;
import org.jlab.utils.groups.IndexedList;

public class TOFCalibrationEngine extends CalibrationEngine {

	public final static int[] NUM_PADDLES = { 23, 62, 5 };
	public final static int 	NUM_LAYERS = 3;
	public final static String[] LAYER_NAME = { "FTOF1A", "FTOF1B", "FTOF2" };
	public final static String[] LAYER_PREFIX = { "", "1a-", "1b-", "2-"};
	public final static double UNDEFINED_OVERRIDE = Double.NEGATIVE_INFINITY;

	// plot settings
	public final static int		FUNC_COLOUR = 2;
	public final static int		MARKER_SIZE = 3;
	public final static int		FUNC_LINE_WIDTH = 2;
	public final static int		MARKER_LINE_WIDTH = 1;

	// Run constants
	public static double BEAM_BUCKET = 2.004; // 2.0 for simulations, 2.004 for real data

	public IndexedList<Double[]> constants = new IndexedList<Double[]>(3);

	public CalibrationConstants calib;
	public IndexedList<DataGroup> dataGroups = new IndexedList<DataGroup>(3);

	public String stepName = "Unknown";
	public String fileNamePrefix = "Unknown";
	public String filename = "Unknown.txt";
	public String histTitle = "Unknown";

	// configuration
	public int calDBSource = 0;
	public static final int CAL_DEFAULT = 0;
	public static final int CAL_FILE = 1;
	public static final int CAL_DB = 2;
	public String prevCalFilename;
	public int prevCalRunNo;
	public boolean prevCalRead = false;
	public boolean engineOn = true;

	public int fitMethod = 0; //  0=MAX 1=SLICES	
	public int FIT_METHOD_MAX = 0;
	public int FIT_METHOD_SF = 1;

	public String fitMode = "L";
	public int fitMinEvents = 0;
	public double maxGraphError = 0.1;
	public double fitSliceMaxError = 0.3;

	// Values from previous calibration
	// Need to be static as used by all engines
	public static CalibrationConstants convValues;
	public static CalibrationConstants leftRightValues;
	public static CalibrationConstants p2pValues;
	public static CalibrationConstants veffValues;
	public static CalibrationConstants timeWalkValues;
	public static CalibrationConstants rfpadValues;
	public static CalibrationConstants twposValues;

	// Calculated counter status values
	public static IndexedList<Integer> adcLeftStatus = new IndexedList<Integer>(3);
	public static IndexedList<Integer> adcRightStatus = new IndexedList<Integer>(3);
	public static IndexedList<Integer> tdcLeftStatus = new IndexedList<Integer>(3);
	public static IndexedList<Integer> tdcRightStatus = new IndexedList<Integer>(3);

	public TOFCalibrationEngine() {
		// controlled by calibration step class
		TOFPaddle.tof = "FTOF";
		convValues = new CalibrationConstants(3,
				"tdc_conv/F");
		leftRightValues = new CalibrationConstants(3,
				"left_right/F");
		veffValues = new CalibrationConstants(3,
				"veff_left/F");
		timeWalkValues = new CalibrationConstants(3,
				"tw0_left/F:tw1_left/F:tw2_left/F:tw0_right/F:tw1_right/F:tw2_right/F");
		p2pValues =	new CalibrationConstants(3,
				"paddle2paddle/F");
		rfpadValues =	new CalibrationConstants(3,
				"rfpad/F");
		twposValues = new CalibrationConstants(3,
				"tw1_left/F:tw2_left/F:tw3_left/F:tw1_right/F:tw2_right/F:tw3_right/F");
		
	}

	public void populatePrevCalib() {
		// overridden in calibration step classes
	}

	@Override
	public void dataEventAction(DataEvent event) {

		if (event.getType() == DataEventType.EVENT_START) {
			resetEventListener();
			processEvent(event);
		} else if (event.getType() == DataEventType.EVENT_ACCUMULATE) {
			processEvent(event);
		} else if (event.getType() == DataEventType.EVENT_STOP) {
			System.out.println("EVENT_STOP");
			analyze();
		}
	}

	public void processPaddleList(List<TOFPaddle> paddleList) {
		// overridden in calibration step classes
	}

	@Override
	public void timerUpdate() {
		analyze();
	}

	public void processEvent(DataEvent event) {
		// overridden in calibration step classes

	}

	public void analyze() {

		//System.out.println(stepName+" analyze");
		for (int sector = 1; sector <= 6; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int layer_index = layer - 1;
				for (int paddle = 1; paddle <= NUM_PADDLES[layer_index]; paddle++) {
					fit(sector, layer, paddle);
				}
			}
		}
		save();
		//saveCounterStatus();
		calib.fireTableDataChanged();
	}

	public void fit(int sector, int layer, int paddle) {
		// fit to default range
		fit(sector, layer, paddle, UNDEFINED_OVERRIDE, UNDEFINED_OVERRIDE);
	}

	public void fit(int sector, int layer, int paddle, double minRange,
			double maxRange) {
		// overridden in calibration step class
	}

	public void saveRow(int sector, int layer, int paddle) {
		// overridden in calibration step class
	}

	public void saveCounterStatus(String filename) {
		// overridden in HV engine
	}
//	public void saveCounterStatus() {
//
//		System.out.println("sector layer component stat_left stat_right");
//		for (int sector = 1; sector <= 6; sector++) {
//			for (int layer = 1; layer <= 3; layer++) {
//				int layer_index = layer - 1;
//				for (int paddle = 1; paddle <= NUM_PADDLES[layer_index]; paddle++) {
//
//					int adcLStat = adcLeftStatus.getItem(sector,layer,paddle);
//					int adcRStat = adcRightStatus.getItem(sector,layer,paddle);
//					int tdcLStat = tdcLeftStatus.getItem(sector,layer,paddle);
//					int tdcRStat = tdcRightStatus.getItem(sector,layer,paddle);					
//					int counterStatusLeft = 0;
//					int counterStatusRight = 0;
//
//					if (adcLStat==1 && tdcLStat==1) {
//						counterStatusLeft = 3;
//					}
//					else if (adcLStat==1) {
//						counterStatusLeft = 1;
//					}
//					else if (tdcLStat==1) {
//						counterStatusLeft = 2;
//					}
//
//					if (adcRStat==1 && tdcRStat==1) {
//						counterStatusRight = 3;
//					}
//					else if (adcRStat==1) {
//						counterStatusRight = 1;
//					}
//					else if (tdcRStat==1) {
//						counterStatusRight = 2;
//					}
//
//					System.out.println(
//							sector+" "+
//									layer+" "+
//									paddle+" "+
//									counterStatusLeft+" "+
//									counterStatusRight+" ");
//				}
//			}
//		}
//	}

	public void save() {

		for (int sector = 1; sector <= 6; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int layer_index = layer - 1;
				for (int paddle = 1; paddle <= NUM_PADDLES[layer_index]; paddle++) {
					calib.addEntry(sector, layer, paddle);
					saveRow(sector, layer, paddle);
				}
			}
		}
		//calib.save(filename);
		// current CalibrationConstants object does not write file in correct format
		// use local method for the moment
		this.writeFile(filename);
	}

	public void writeFile(String filename) {

		try { 

			// Open the output file
			File outputFile = new File(filename);
			FileWriter outputFw = new FileWriter(outputFile.getAbsoluteFile());
			BufferedWriter outputBw = new BufferedWriter(outputFw);

			for (int i=0; i<calib.getRowCount(); i++) {
				String line = new String();
				for (int j=0; j<calib.getColumnCount(); j++) {
					line = line+calib.getValueAt(i, j);
					if (j<calib.getColumnCount()-1) {
						line = line+" ";
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

	public String nextFileName() {

		// Get the next file name
		Date today = new Date();
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		String todayString = dateFormat.format(today);
		String filePrefix = fileNamePrefix + todayString;
		int newFileNum = 0;

		File dir = new File(".");
		File[] filesList = dir.listFiles();

		for (File file : filesList) {
			if (file.isFile()) {
				String fileName = file.getName();
				if (fileName.matches(filePrefix + "[.]\\d+[.]txt")) {
					String fileNumString = fileName.substring(
							fileName.indexOf('.') + 1,
							fileName.lastIndexOf('.'));
					int fileNum = Integer.parseInt(fileNumString);
					if (fileNum >= newFileNum)
						newFileNum = fileNum + 1;

				}
			}
		}

		return filePrefix + "." + newFileNum + ".txt";
	}

	public void customFit(int sector, int layer, int paddle) {
		// overridden in calibration step class
	}

	@Override
	public List<CalibrationConstants> getCalibrationConstants() {
		return Arrays.asList(calib);
	}

	@Override
	public IndexedList<DataGroup> getDataGroup() {
		return dataGroups;
	}

	public int getH1FEntries(H1F hist) {
		int n = 0;

		for (int i=0; i<hist.getxAxis().getNBins(); i++) {
			n = (int) (n+hist.getBinContent(i));
		}
		return n;
	}

	public GraphErrors maxGraph(H2F hist, String graphName) {

		ArrayList<H1F> slices = hist.getSlicesX();
		int nBins = hist.getXAxis().getNBins();
		double[] sliceMax = new double[nBins];
		double[] maxErrs = new double[nBins];
		double[] xVals = new double[nBins];
		double[] xErrs = new double[nBins];

		for (int i=0; i<nBins; i++) {

			//			System.out.println("getH1FEntries "+getH1FEntries(slices.get(i)));
			//			System.out.println("H1F getEntries "+slices.get(i).getEntries());

			if (getH1FEntries(slices.get(i)) > fitMinEvents) {
				int maxBin = slices.get(i).getMaximumBin();
				sliceMax[i] = slices.get(i).getxAxis().getBinCenter(maxBin);
				maxErrs[i] = slices.get(i).getRMS();
				//maxErrs[i] = maxGraphError;

				xVals[i] = hist.getXAxis().getBinCenter(i);
				xErrs[i] = hist.getXAxis().getBinWidth(i)/2.0;
			}
		}

		GraphErrors maxGraph = new GraphErrors(graphName, xVals, sliceMax, xErrs, maxErrs);
		maxGraph.setName(graphName);

		return maxGraph;

	}

	public GraphErrors fixGraph(GraphErrors graphIn, String graphName) {

		int n = graphIn.getDataSize(0);
		int m = 0;
		for (int i=0; i<n; i++) {
			if (graphIn.getDataEY(i) < fitSliceMaxError) {
				m++;
			}
		}		

		double[] x = new double[m];
		double[] xerr = new double[m];
		double[] y = new double[m];
		double[] yerr = new double[m];
		int j = 0;

		for (int i=0; i<n; i++) {
			if (graphIn.getDataEY(i) < fitSliceMaxError) {
				x[j] = graphIn.getDataX(i);
				xerr[j] = graphIn.getDataEX(i);
				y[j] = graphIn.getDataY(i);
				yerr[j] = graphIn.getDataEY(i);
				j++;
			}
		}

		GraphErrors fixGraph = new GraphErrors(graphName, x, y, xerr, yerr);
		fixGraph.setName(graphName);

		return fixGraph;

	}		

	public boolean isGoodPaddle(int sector, int layer, int paddle) {
		// Overridden in calibration step class
		return true;
	}

	public DataGroup getSummary(int sector, int layer) {
		// Overridden in calibration step class
		DataGroup dg = null;
		return dg;
	}

	public double paddleLength(int sector, int layer, int paddle) {
		double len = 0.0;

		if (layer == 1 && paddle <= 5) {
			len = (15.85 * paddle) + 16.43;
		} else if (layer == 1 && paddle > 5) {
			len = (15.85 * paddle) + 11.45;
		} else if (layer == 2) {
			len = (6.4 * paddle) + 10.84;
		} else if (layer == 3) {
			len = (13.73 * paddle) + 357.55;
		}

		return len;

	}

	public int paddleNumber(int sector, int layer, int component) {

		int p = 0;
		int[] paddleOffset = {0, 0, 23, 85};

		p = component + (sector-1)*90 + paddleOffset[layer]; 
		return p;
	}

	public static double toDouble(String stringVal) {

		double doubleVal;
		try {
			doubleVal = Double.parseDouble(stringVal);
		} catch (NumberFormatException e) {
			doubleVal = UNDEFINED_OVERRIDE;
		}
		return doubleVal;
	}

	public void setPlotTitle(int sector, int layer, int paddle) {
		// Overridden in calibration step classes
	}
	
	public String histTitle(int sector, int layer, int paddle) {
		return "S"+sector+" "+histTitle+" "+LAYER_PREFIX[layer]+paddle;
	}

	public void drawPlots(int sector, int layer, int paddle,
			EmbeddedCanvas canvas) {
		// Overridden in calibration step classes
	}

	public void showPlots(int sector, int layer) {
		
		int layer_index = layer - 1;
		EmbeddedCanvas[] fitCanvases;
		fitCanvases = new EmbeddedCanvas[3];
		fitCanvases[0] = new EmbeddedCanvas();
		fitCanvases[0].divide(6, 4);

		int canvasNum = 0;
		int padNum = 0;

		for (int paddleNum = 1; paddleNum <= NUM_PADDLES[layer_index]; paddleNum++) {

			fitCanvases[canvasNum].cd(padNum);
			fitCanvases[canvasNum].getPad(padNum).setTitle("Paddle "+paddleNum);
			fitCanvases[canvasNum].getPad(padNum).setOptStat(0);
			drawPlots(sector, layer, paddleNum, fitCanvases[canvasNum]);

			padNum = padNum + 1;

			if ((paddleNum) % 24 == 0) {
				// new canvas
				canvasNum = canvasNum + 1;
				padNum = 0;

				fitCanvases[canvasNum] = new EmbeddedCanvas();
				fitCanvases[canvasNum].divide(6, 4);

			}

		}
		JFrame frame = new JFrame(stepName + " " + LAYER_NAME[layer - 1]
				+ " Sector " + sector);
		frame.setSize(1000, 800);

		JTabbedPane pane = new JTabbedPane();
		for (int i = 0; i <= canvasNum; i++) {
			pane.add(
					"Paddles " + ((i * 24) + 1) + " to "
							+ Math.min(((i + 1) * 24), NUM_PADDLES[layer - 1]),
							fitCanvases[i]);
		}

		frame.add(pane);
		// frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

	}

	public void rescaleGraphs(EmbeddedCanvas canvas, int sector, int layer, int paddle) {
		// overridden in each step
	}

	public void setOutput(boolean outputOn) {
		if (outputOn) {
			System.setOut(TOFCalibration.oldStdout);
		}
		else {
			System.setOut(new java.io.PrintStream(
					new java.io.OutputStream() {
						public void write(int b){}
					}
					));
		}
	}

}
