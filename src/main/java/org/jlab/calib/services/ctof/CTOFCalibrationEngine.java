package org.jlab.calib.services.ctof;

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

import org.jlab.calib.services.TOFPaddle;
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

public class CTOFCalibrationEngine extends CalibrationEngine {

    public final static int[] NUM_PADDLES = { 48 };
    public final static int     NUM_LAYERS = 1;

    public final static double UNDEFINED_OVERRIDE = Double.NEGATIVE_INFINITY;

    // plot settings
    public final static int        FUNC_COLOUR = 2;
    public final static int        MARKER_SIZE = 3;
    public final static int        FUNC_LINE_WIDTH = 2;
    public final static int        MARKER_LINE_WIDTH = 1;

    // Run constants
    public static Double BEAM_BUCKET = 2.004; // 2.0 for simulations, 2.004 for real data

    public IndexedList<Double[]> constants = new IndexedList<Double[]>(3);

    public CalibrationConstants calib;
    public IndexedList<DataGroup> dataGroups = new IndexedList<DataGroup>(3);

    public String stepName = "Unknown";
    public String fileNamePrefix = "Unknown";
    public String filename = "Unknown.txt";

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
	public boolean logScale = false;

    // Values from previous calibration
    // Need to be static as used by all engines
	public static CalibrationConstants convValues;    
    public static CalibrationConstants leftRightValues;
    public static CalibrationConstants p2pValues;
    public static CalibrationConstants veffValues;
    public static CalibrationConstants rfpadValues;
    
    // Calculated counter status values
    public static IndexedList<Integer> adcLeftStatus = new IndexedList<Integer>(3);
    public static IndexedList<Integer> adcRightStatus = new IndexedList<Integer>(3);
    public static IndexedList<Integer> tdcLeftStatus = new IndexedList<Integer>(3);
    public static IndexedList<Integer> tdcRightStatus = new IndexedList<Integer>(3);
    public static IndexedList<Double[]> hposValues = new IndexedList<Double[]>(3);
    
    public CTOFCalibrationEngine() {
        // controlled by calibration step class
        //TOFPaddle.tof = "CTOF";
        convValues = new CalibrationConstants(3,
                "upstream/F:downstream/F");
        leftRightValues = new CalibrationConstants(3,
                "upstream_downstream/F");
        veffValues = new CalibrationConstants(3,
                "veff_upstream/F");
        p2pValues =    new CalibrationConstants(3,
                        "paddle2paddle/F");
        rfpadValues =    new CalibrationConstants(3,
                "rfpad/F");

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

        for (int paddle = 1; paddle <= NUM_PADDLES[0]; paddle++) {
            fit(1, 1, paddle);
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

    public void save() {

        for (int paddle = 1; paddle <= NUM_PADDLES[0]; paddle++) {
            calib.addEntry(1, 1, paddle);
            saveRow(1, 1, paddle);
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
        int nBinsGraph = 0;
        
        // Get the nBins to include in graph
        for (int i=0; i<nBins; i++) {
        	int maxBin = slices.get(i).getMaximumBin();
            if (slices.get(i).getBinContent(maxBin) > fitMinEvents) {
        		nBinsGraph++;
        	}
        }
        if (nBinsGraph==0) {
        	nBinsGraph=nBins; // avoid exception when no bins to include
        }
        
        double[] sliceMax = new double[nBinsGraph];
        double[] maxErrs = new double[nBinsGraph];
        double[] xVals = new double[nBinsGraph];
        double[] xErrs = new double[nBinsGraph];
        
        int j=0;
        for (int i=0; i<nBins; i++) {
            
            int maxBin = slices.get(i).getMaximumBin();
            if (slices.get(i).getBinContent(maxBin) > fitMinEvents) {
                sliceMax[j] = slices.get(i).getxAxis().getBinCenter(maxBin);
                maxErrs[j] = slices.get(i).getRMS()/Math.sqrt(slices.get(i).getBinContent(maxBin));

                xVals[j] = hist.getXAxis().getBinCenter(i);
                xErrs[j] = hist.getXAxis().getBinWidth(i)/2.0;
                j++;
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

        return 100.0;

    }

    public int paddleNumber(int sector, int layer, int component) {
        
        return component;
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
            fitCanvases[canvasNum].getPad(padNum).getAxisZ().setLog(logScale);
			drawPlots(sector, layer, paddleNum, fitCanvases[canvasNum]);

            padNum = padNum + 1;

            if ((paddleNum) == 24) {
                // new canvas
                canvasNum = canvasNum + 1;
                padNum = 0;

                fitCanvases[canvasNum] = new EmbeddedCanvas();
                fitCanvases[canvasNum].divide(6, 4);

            }

        }

        JFrame frame = new JFrame(stepName + " Sector " + sector);
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
    	for (int i=0; i<canvas.getNColumns()*canvas.getNRows(); i++) {
    		canvas.getPad(i).getAxisZ().setLog(logScale);
    	}
	}

    public void setOutput(boolean outputOn) {
        if (outputOn) {
            System.setOut(CTOFCalibration.oldStdout);
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
