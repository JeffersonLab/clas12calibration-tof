package org.jlab.calib.services;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jlab.detector.base.DetectorType;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.CalibrationConstantsListener;
import org.jlab.detector.calib.utils.CalibrationConstantsView;
import org.jlab.detector.view.DetectorListener;
import org.jlab.detector.view.DetectorPane2D;
import org.jlab.detector.view.DetectorShape2D;
import org.jlab.groot.base.GStyle;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataEventType;
import org.jlab.io.task.DataSourceProcessorPane;
import org.jlab.io.task.IDataEventListener;
import org.jlab.utils.groups.IndexedList;

public class TOFCalibration
		implements IDataEventListener, ActionListener, CalibrationConstantsListener, DetectorListener, ChangeListener {

        public static DetectorType TYPE = DetectorType.FTOF;

        // main panel
	JPanel pane = null;
	JFrame innerConfigFrame = new JFrame("Configure FTOF calibration settings");
	JDialog configFrame = new JDialog(innerConfigFrame, "Configure FTOF calibration settings");
	JTabbedPane configPane = new JTabbedPane();

	// detector panel
	DetectorPane2D detectorView = null;

	// event reading panel
	DataSourceProcessorPane processorPane = null;
	public final int UPDATE_RATE = 2000000;

	// calibration view
	EmbeddedCanvas canvas = null;
	CalibrationConstantsView ccview = null;

	TOFCalibrationEngine[] engines = { new TofHVEventListener(), new TofAttenEventListener(),
			new TofTdcConvEventListener(), new TofLeftRightEventListener(), new TofVeffEventListener(),
			new TofTimeWalkEventListener(), new TofTWPosEventListener(), new TofRFPadEventListener(),
			new TofP2PEventListener(), new TofFadcEventListener(), new TofCheckEventListener() };
	
	public static TofPrevConfigPanel[] engPanels = { new TofPrevConfigPanel(new TOFCalibrationEngine()),
			new TofPrevConfigPanel(new TOFCalibrationEngine()), new TofPrevConfigPanel(new TOFCalibrationEngine()),
			new TofPrevConfigPanel(new TOFCalibrationEngine()), new TofPrevConfigPanel(new TOFCalibrationEngine()),
			new TofPrevConfigPanel(new TOFCalibrationEngine()), new TofPrevConfigPanel(new TOFCalibrationEngine()),
			new TofPrevConfigPanel(new TOFCalibrationEngine()) };


	// engine indices
	public final int HV = 0;
	public final int ATTEN = 1;
	public final int TDC_CONV = 2;
	public final int LEFT_RIGHT = 3;
	public final int VEFF = 4;
	public final int TW = 5;
	public final int TWPOS = 6;
	public final int RFPAD = 7;
	public final int P2P = 8;
	public final int FADC = 9;
	public final int CHECK = 10;

	String[] dirs = { "/calibration/ftof/gain_balance", "/calibration/ftof/attenuation", "/calibration/ftof/tdc_conv",
			"/calibration/ftof/timing_offset/left_right", "/calibration/ftof/effective_velocity",
			"/calibration/ftof/time_walk", "/calibration/ftof/time_walk_pos", "/calibration/ftof/timing_offset/rfpad",
			"/calibration/ftof/timing_offset/P2P", "/calibration/ftof/fadc_offset",
			"/calibration/ftof/timing_offset/check" };

	String selectedDir = "None";
	int selectedSector = 1;
	int selectedLayer = 1;
	int selectedPaddle = 1;

	String[] buttons = { "View all", "Adjust Fit/Override", "Adjust HV", "Write" };
	// button indices
	public final int VIEW_ALL = 0;
	public final int FIT_OVERRIDE = 1;
	public final int ADJUST_HV = 2;
	public final int WRITE = 3;

	// configuration settings
	JCheckBox[] stepChecks = { new JCheckBox(), new JCheckBox(), new JCheckBox(), new JCheckBox(), new JCheckBox(),
			new JCheckBox(), new JCheckBox(), new JCheckBox(), new JCheckBox(), new JCheckBox(), new JCheckBox() };

	// Target GMEAN channel
	private JTextField[] targetGMean = { new JTextField(6), new JTextField(6), new JTextField(6) };

	// Path length normalisation setting
	JComboBox<String> pathNormList = new JComboBox<String>();
	public static int pathNorm = 0;
	public final static int PATH_NORM_YES = 0;
	public final static int PATH_NORM_NO = 1;

	private JTextField rcsText = new JTextField(5);
	private JTextField rcsText2 = new JTextField(7); // panel 2
	public static double maxRcs = 0.0;
	public static double maxRcs2 = 0.0; // panel 2
	private JTextField minVText = new JTextField(5);
	public static double minV = -9999.0;
	private JTextField maxVText = new JTextField(5);
	public static double maxV = 9999.0;

	// Vertex time correction setting
	JComboBox<String> vertexCorrList = new JComboBox<String>();
	public static int vertexCorr = 0;
	public final static int VERTEX_CORR_YES = 0;
	public final static int VERTEX_CORR_NO = 1;
	
	private JTextField targetPosText = new JTextField(5);
	public static double targetPos = -3.0;

	JComboBox<String> massAssList = new JComboBox<String>();
	public static int massAss = 3;
	public final static int MASS_PION = 0;
	public final static int MASS_PROTON = 1;
	public final static int MASS_ELECTRON = 2;
	public final static int USE_PID = 3;

	private JTextField minPText = new JTextField(5);
	public static double minP = 0.0;
	private JTextField maxPText = new JTextField(5);
	public static double maxP = 0.0;

	private JTextField minEText = new JTextField(5);
	public static double minE = 0.0;

	JComboBox<String> trackChargeList = new JComboBox<String>();
	public static int trackCharge = 0;
	public final static int TRACK_BOTH = 0;
	public final static int TRACK_NEG = 1;
	public final static int TRACK_POS = 2;
	JComboBox<Double> trfList = new JComboBox<Double>();
	JComboBox<String> pidList = new JComboBox<String>();
	JComboBox<String> pidList2 = new JComboBox<String>();
	public static int trackPid = 0;
	public static int trackPid2 = 0;
	public final static int PID_ALL = 0;
	public final static int PID_L = 1;
	public final static int PID_PI = 2;
	public final static int PID_P = 3;
	public final static int PID_LPI = 4;

	private JTextField triggerText = new JTextField(10);
	public static int triggerBit = 0;

	JComboBox<String> fitList = new JComboBox<String>();
	JComboBox<String> fitModeList = new JComboBox<String>();
	private JTextField minEventsText = new JTextField(5);
	private JTextField minTDCText = new JTextField(6);
	private JTextField maxTDCText = new JTextField(6);

	private JTextField minFADCxText = new JTextField(6);
	private JTextField maxFADCxText = new JTextField(6);
	private JTextField widthFADCText = new JTextField(6);

	public final static PrintStream oldStdout = System.out;

	public TOFCalibration() {
	
		GStyle.getAxisAttributesX().setLabelFontName("Avenir");
		GStyle.getAxisAttributesY().setLabelFontName("Avenir");
		GStyle.getAxisAttributesZ().setLabelFontName("Avenir");
		GStyle.getAxisAttributesX().setTitleFontName("Avenir");
		GStyle.getAxisAttributesY().setTitleFontName("Avenir");
		GStyle.getAxisAttributesZ().setTitleFontName("Avenir");
		GStyle.setGraphicsFrameLineWidth(1);
		GStyle.getH1FAttributes().setLineWidth(1);

		configFrame.setModalityType(ModalityType.APPLICATION_MODAL);
		configure();

		pane = new JPanel();
		pane.setLayout(new BorderLayout());

		JSplitPane splitPane = new JSplitPane();

		// combined panel for detector view and button panel
		JPanel combined = new JPanel();
		combined.setLayout(new BorderLayout());

		detectorView = new DetectorPane2D();
		detectorView.getView().addDetectorListener(this);

		JPanel butPanel = new JPanel();
		for (int i = 0; i < buttons.length; i++) {
			JButton button = new JButton(buttons[i]);
			button.addActionListener(this);
			butPanel.add(button);
		}
		combined.add(detectorView, BorderLayout.CENTER);
		combined.add(butPanel, BorderLayout.PAGE_END);

		this.updateDetectorView(true);

		splitPane.setLeftComponent(combined);

		// Create the engine views with this GUI as listener
		JPanel engineView = new JPanel();
		JSplitPane enginePane = null;
		engineView.setLayout(new BorderLayout());
		enginePane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

		canvas = new EmbeddedCanvas();
		ccview = new CalibrationConstantsView();
		ccview.getTabbedPane().addChangeListener(this);

		for (int i = 0; i < engines.length; i++) {
			if (engines[i].engineOn) {
				ccview.addConstants(engines[i].getCalibrationConstants().get(0), this);
				ccview.getTabbedPane().setEnabled(false);
			}
		}

		enginePane.setTopComponent(canvas);
		enginePane.setBottomComponent(ccview);
		enginePane.setDividerLocation(0.6);
		enginePane.setResizeWeight(0.6);
		engineView.add(splitPane, BorderLayout.CENTER);

		splitPane.setRightComponent(enginePane);
		pane.add(splitPane, BorderLayout.CENTER);

		processorPane = new DataSourceProcessorPane();
		processorPane.setUpdateRate(UPDATE_RATE);

		// only add the gui as listener so that extracting paddle list from event is
		// only done once per event
		this.processorPane.addEventListener(this);

		// this.processorPane.addEventListener(engines[0]);
		// this.processorPane.addEventListener(this); // add gui listener second so
		// detector view updates
		// // as soon as 1st analyze is done
		// for (int i=1; i< engines.length; i++) {
		// this.processorPane.addEventListener(engines[i]);
		// }
		pane.add(processorPane, BorderLayout.PAGE_END);

		JFrame frame = new JFrame("FTOF Calibration");
		frame.setSize(1600, 900);

		frame.add(pane);
		// frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	}

	public TOFCalibrationEngine getSelectedEngine() {

		TOFCalibrationEngine engine = null; // = engines[HV];

		if (selectedDir == dirs[HV]) {
			engine = engines[HV];
		} else if (selectedDir == dirs[ATTEN]) {
			engine = engines[ATTEN];
		} else if (selectedDir == dirs[TDC_CONV]) {
			engine = engines[TDC_CONV];
		} else if (selectedDir == dirs[LEFT_RIGHT]) {
			engine = engines[LEFT_RIGHT];
		} else if (selectedDir == dirs[VEFF]) {
			engine = engines[VEFF];
		} else if (selectedDir == dirs[TW]) {
			engine = engines[TW];
		} else if (selectedDir == dirs[RFPAD]) {
			engine = engines[RFPAD];
		} else if (selectedDir == dirs[P2P]) {
			engine = engines[P2P];
		} else if (selectedDir == dirs[FADC]) {
			engine = engines[FADC];
		} else if (selectedDir == dirs[CHECK]) {
			engine = engines[CHECK];
		} else if (selectedDir == dirs[TWPOS]) {
			engine = engines[TWPOS];
		}

		return engine;
	}

	public void actionPerformed(ActionEvent e) {

		TOFCalibrationEngine engine = getSelectedEngine();

		if (e.getActionCommand().compareTo(buttons[VIEW_ALL]) == 0) {

			engine.showPlots(selectedSector, selectedLayer);

		} else if (e.getActionCommand().compareTo(buttons[FIT_OVERRIDE]) == 0) {

			engine.customFit(selectedSector, selectedLayer, selectedPaddle);
			updateDetectorView(false);
			this.updateCanvas();
		} else if (e.getActionCommand().compareTo(buttons[ADJUST_HV]) == 0) {

			if (engines[HV].engineOn) {
				JFrame hvFrame = new JFrame("Adjust HV");
				hvFrame.add(new TOFHVAdjustPanel((TofHVEventListener) engines[HV], this));
				hvFrame.setSize(1000, 800);
				hvFrame.setVisible(true);
				hvFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
			} else {
				JOptionPane.showMessageDialog(new JPanel(),
						"Adjust HV is available only when the HV calibration step has been included.");
			}

		} else if (e.getActionCommand().compareTo(buttons[WRITE]) == 0) {

			String outputFilename = engine.nextFileName();
			engine.writeFile(outputFilename);
			JOptionPane.showMessageDialog(new JPanel(),
					engine.stepName + " calibration values written to " + outputFilename);
			// update the files that contain the snapshot of latest values saved
			writeFiles();
		}

		// config settings
		// if (e.getSource() == stepChecks[TDC_CONV]) {
		// configPane.setEnabledAt(3, stepChecks[TDC_CONV].isSelected());
		// }
		// if (e.getSource() == stepChecks[TW]) {
		// configPane.setEnabledAt(4, stepChecks[TW].isSelected());
		// }
		if (e.getActionCommand().compareTo("Next") == 0) {
			int currentTab = configPane.getSelectedIndex();
			for (int i = currentTab + 1; i < configPane.getTabCount(); i++) {
				if (configPane.isEnabledAt(i)) {
					configPane.setSelectedIndex(i);
					break;
				}
			}
		}
		if (e.getActionCommand().compareTo("Back") == 0) {
			int currentTab = configPane.getSelectedIndex();
			for (int i = currentTab - 1; i >= 0; i--) {
				if (configPane.isEnabledAt(i)) {
					configPane.setSelectedIndex(i);
					break;
				}
			}
		}
		if (e.getActionCommand().compareTo("Cancel") == 0) {
			System.exit(0);
		}

		if (e.getActionCommand().compareTo("Finish") == 0) {
			configFrame.setVisible(false);

			System.out.println("");
			System.out.println(todayString());
			System.out.println("Configuration settings - Selected steps");
			System.out.println("---------------------------------------");
			// step selection
			for (int i = 0; i < engines.length; i++) {
				engines[i].engineOn = stepChecks[i].isSelected();
				if (selectedDir.compareTo("None") == 0 && engines[i].engineOn) {
					selectedDir = dirs[i];
				}
				System.out.println(engines[i].stepName + " " + engines[i].engineOn);
			}

			System.out.println("");
			System.out.println("Configuration settings - Previous calibration values");
			System.out.println("----------------------------------------------------");
			// get the previous iteration calibration values
			for (int i = 0; i < engines.length; i++) {
				engines[i].populatePrevCalib();

				if (!engines[i].prevCalRead) {
					System.out.println(
							"Problem populating " + engines[i].stepName + " previous calibration values - exiting");
					System.exit(0);
				}
			}

			// set the config values
			TofHVEventListener hvEngine = (TofHVEventListener) engines[HV];
			hvEngine.EXPECTED_MIP_CHANNEL[0] = Integer.parseInt(targetGMean[0].getText());
			hvEngine.EXPECTED_MIP_CHANNEL[1] = Integer.parseInt(targetGMean[1].getText());
			hvEngine.EXPECTED_MIP_CHANNEL[2] = Integer.parseInt(targetGMean[2].getText());
			hvEngine.NEWHV_MIP_CHANNEL[0] = Integer.parseInt(targetGMean[0].getText());
			hvEngine.NEWHV_MIP_CHANNEL[1] = Integer.parseInt(targetGMean[1].getText());
			hvEngine.NEWHV_MIP_CHANNEL[2] = Integer.parseInt(targetGMean[2].getText());
			hvEngine.setConstraints();

			pathNorm = pathNormList.getSelectedIndex();
			if (rcsText.getText().compareTo("") != 0) {
				maxRcs = Double.parseDouble(rcsText.getText());
			}
			if (rcsText2.getText().compareTo("") != 0) {
				maxRcs2 = Double.parseDouble(rcsText2.getText());
			}
			if (minVText.getText().compareTo("") != 0) {
				minV = Double.parseDouble(minVText.getText());
			}
			if (maxVText.getText().compareTo("") != 0) {
				maxV = Double.parseDouble(maxVText.getText());
			}
			vertexCorr = vertexCorrList.getSelectedIndex();

			if (targetPosText.getText().compareTo("") != 0) {
				targetPos = Double.parseDouble(targetPosText.getText());
			}
			
			// momentum range
			if (minPText.getText().compareTo("") != 0) {
				minP = Double.parseDouble(minPText.getText());
			}
			if (maxPText.getText().compareTo("") != 0) {
				maxP = Double.parseDouble(maxPText.getText());
			}

			if (minEText.getText().compareTo("") != 0) {
				minE = Double.parseDouble(minEText.getText());
			}

			massAss = massAssList.getSelectedIndex();
			trackCharge = trackChargeList.getSelectedIndex();
			TOFCalibrationEngine.BEAM_BUCKET = (Double) trfList.getSelectedItem();
			trackPid = pidList.getSelectedIndex();
			trackPid2 = pidList2.getSelectedIndex();

			if (triggerText.getText().compareTo("") != 0) {
				triggerBit = Integer.parseInt(triggerText.getText());
			}

			engines[ATTEN].fitMethod = fitList.getSelectedIndex();
			engines[ATTEN].fitMode = (String) fitModeList.getSelectedItem();
			if (minEventsText.getText().compareTo("") != 0) {
				engines[ATTEN].fitMinEvents = Integer.parseInt(minEventsText.getText());
			}

			engines[TW].fitMethod = fitList.getSelectedIndex();
			engines[TW].fitMode = (String) fitModeList.getSelectedItem();
			if (minEventsText.getText().compareTo("") != 0) {
				engines[TW].fitMinEvents = Integer.parseInt(minEventsText.getText());
			}

			engines[TWPOS].fitMethod = fitList.getSelectedIndex();
			engines[TWPOS].fitMode = (String) fitModeList.getSelectedItem();
			if (minEventsText.getText().compareTo("") != 0) {
				engines[TWPOS].fitMinEvents = Integer.parseInt(minEventsText.getText());
			}

			engines[VEFF].fitMethod = fitList.getSelectedIndex();
			engines[VEFF].fitMode = (String) fitModeList.getSelectedItem();
			if (minEventsText.getText().compareTo("") != 0) {
				engines[VEFF].fitMinEvents = Integer.parseInt(minEventsText.getText());
			}

			engines[TDC_CONV].fitMethod = fitList.getSelectedIndex();
			engines[TDC_CONV].fitMode = (String) fitModeList.getSelectedItem();
			if (minEventsText.getText().compareTo("") != 0) {
				engines[TDC_CONV].fitMinEvents = Integer.parseInt(minEventsText.getText());
			}

			// min and max TDC
			TofTdcConvEventListener tdcEngine = (TofTdcConvEventListener) engines[TDC_CONV];
			tdcEngine.TDC_MIN = Integer.parseInt(minTDCText.getText());
			tdcEngine.TDC_MAX = Integer.parseInt(maxTDCText.getText());
			tdcEngine.FIT_MIN = tdcEngine.TDC_MIN + 100;
			tdcEngine.FIT_MAX = tdcEngine.TDC_MAX - 100;

			// FADC settings
			TofFadcEventListener fadcEngine = (TofFadcEventListener) engines[FADC];
			fadcEngine.MIN_X = Double.parseDouble(minFADCxText.getText());
			fadcEngine.MAX_X = Double.parseDouble(maxFADCxText.getText());
			fadcEngine.WIDTH = Double.parseDouble(widthFADCText.getText());

			System.out.println("");
			System.out.println("Configuration settings - Tracking/General");
			System.out.println("-----------------------------------------");
			System.out.println("Target GMEAN channel 1a/1b/2: " + targetGMean[0].getText() + "/"
					+ targetGMean[1].getText() + "/" + targetGMean[2].getText());
			System.out.println("Path length normalisation for gmean?: " + pathNormList.getItemAt(pathNorm));
			System.out.println("Maximum reduced chi squared for tracks: " + maxRcs + " (1a/1b) " + maxRcs2 + " (2)");
			System.out.println("Minimum vertex z: " + minV);
			System.out.println("Maximum vertex z: " + maxV);
			System.out.println("Vertex time correction?: " + vertexCorrList.getItemAt(vertexCorr));
			System.out.println("Target position (cm): " + targetPos);
			System.out.println("Momentum range (GeV): " + minP + "-" + maxP);
			System.out.println("Minimum energy deposit (MeV): " + minE);
			System.out.println("Mass assumption for beta calculation: " + massAssList.getItemAt(massAss));
			System.out.println("Track charge: " + trackChargeList.getItemAt(trackCharge));
			System.out.println("RF period: " + TOFCalibrationEngine.BEAM_BUCKET);
			System.out.println(
					"PID: " + pidList.getItemAt(trackPid) + " (1a/1b) " + pidList2.getItemAt(trackPid2) + " (2)");
			System.out.println("Trigger: " + triggerBit);
			System.out.println("2D histogram graph method: " + fitList.getSelectedItem());
			System.out.println("Slicefitter mode: " + fitModeList.getSelectedItem());
			System.out.println("Minimum events per slice: " + minEventsText.getText());
			System.out.println("TDC range: " + minTDCText.getText() + "-" + maxTDCText.getText());
			System.out.println("FADC x range / width: " + minFADCxText.getText() + " - " + maxFADCxText.getText() + " / "
					+ widthFADCText.getText());
			System.out.println("");
						
		}
	}

	public void dataEventAction(DataEvent event) {

		List<TOFPaddle> paddleList = DataProvider.getPaddleList(event);

		for (int i = 0; i < engines.length; i++) {

			if (engines[i].engineOn) {

				if (event.getType() == DataEventType.EVENT_START) {
					engines[i].resetEventListener();
					engines[i].processPaddleList(paddleList);

				} else if (event.getType() == DataEventType.EVENT_ACCUMULATE) {
					engines[i].processPaddleList(paddleList);
				} else if (event.getType() == DataEventType.EVENT_STOP) {
					System.setOut(oldStdout);
					System.out.println("EVENT_STOP for " + engines[i].stepName + " " + todayString());
					engines[i].analyze();
					ccview.getTabbedPane().setEnabled(true);
					System.setOut(oldStdout);
				}

				if (event.getType() == DataEventType.EVENT_STOP) {
					this.updateDetectorView(false);
					this.updateCanvas();
					// if (i==0) this.showTestHists();
				}
			}
		}
		if (event.getType() == DataEventType.EVENT_STOP) {
			writeFiles();
		}
	}

	private void writeFiles() {

		TofTimingOptionsPanel panel = new TofTimingOptionsPanel(TYPE);
		int result = JOptionPane.showConfirmDialog(null, panel, "Choose file output options",
				JOptionPane.OK_CANCEL_OPTION);

		if (result == JOptionPane.OK_OPTION) {

			// tres
			TofRFPadEventListener rfpadEng = (TofRFPadEventListener) engines[RFPAD];
			rfpadEng.writeSigmaFile("FTOF_CALIB_TRES.txt");

			// time offsets
			writeTimeOffsets("FTOF_CALIB_TIME_OFFSETS.txt", panel.stepOptions);

			// TDC conv
			// engines[TDC_CONV].writeFile("FTOF_CALIB_TDC_CONV.txt");
		}
	}

	public void writeTimeOffsets(String filename, int[] stepOptions) {

		try {

			// Open the output file
			File outputFile = new File(filename);
			FileWriter outputFw = new FileWriter(outputFile.getAbsoluteFile());
			BufferedWriter outputBw = new BufferedWriter(outputFw);

			// get the engines
			TofLeftRightEventListener lrEng = (TofLeftRightEventListener) engines[LEFT_RIGHT];
			TofRFPadEventListener rfpadEng = (TofRFPadEventListener) engines[RFPAD];
			TofP2PEventListener p2pEng = (TofP2PEventListener) engines[P2P];

			for (int sector = 1; sector <= 6; sector++) {
				for (int layer = 1; layer <= 3; layer++) {
					int layer_index = layer - 1;
					for (int paddle = 1; paddle <= TOFCalibrationEngine.NUM_PADDLES[layer_index]; paddle++) {

						double leftRight = 0.0;
						if (stepOptions[0] == 0) {
							leftRight = lrEng.leftRightValues.getDoubleValue("left_right", sector, layer, paddle);
						} else {
							leftRight = lrEng.getCentroid(sector, layer, paddle);
						}
						double rfpad = 0.0;
						if (stepOptions[1] == 0) {
							rfpad = rfpadEng.rfpadValues.getDoubleValue("rfpad", sector, layer, paddle);
						} else {
							rfpad = rfpadEng.getOffset(sector, layer, paddle);
						}
						double p2p = 0.0;
						if (stepOptions[2] == 0) {
							p2p = p2pEng.p2pValues.getDoubleValue("paddle2paddle", sector, layer, paddle);
						} else {
							p2p = p2pEng.getOffset(sector, layer, paddle);
						}

						String line = new String();
						line = sector + " " + layer + " " + paddle + " " + new DecimalFormat("0.000").format(leftRight)
								+ " " + new DecimalFormat("0.000").format(rfpad) + " "
								+ new DecimalFormat("0.000").format(p2p);
						outputBw.write(line);
						outputBw.newLine();
					}
				}
			}

			outputBw.close();
		} catch (IOException ex) {
			System.out.println("Error writing file '");
			// Or we could just do this:
			ex.printStackTrace();
		}

	}

	private String todayString() {
		Date today = new Date();
		DateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy HH:mm:ss");
		String todayString = dateFormat.format(today);

		return todayString;
	}

	public void resetEventListener() {
		for (int i = 0; i < engines.length; i++) {
			engines[i].resetEventListener();
		}
		this.timerUpdate();
		this.innerConfigFrame = new JFrame("Configure FTOF calibration settings");
		this.configFrame = new JDialog(innerConfigFrame, "Configure FTOF calibration settings");
		this.configPane = new JTabbedPane();
		this.configure();
	}

	public void timerUpdate() {

		for (int i = 0; i < engines.length; i++) {
			if (engines[i].engineOn) {
				engines[i].timerUpdate();
				ccview.getTabbedPane().setEnabled(true);
			}
		}

		this.updateDetectorView(false);
		this.updateCanvas();
	}

	public final void updateDetectorView(boolean isNew) {

		TOFCalibrationEngine engine = getSelectedEngine();

		double FTOFSize = 500.0;
		int[] npaddles = new int[] { 23, 62, 5 };
		int[] widths = new int[] { 6, 15, 25 };
		int[] lengths = new int[] { 6, 15, 25 };

		String[] names = new String[] { "FTOF 1A", "FTOF 1B", "FTOF 2" };
		for (int sector = 1; sector <= 6; sector++) {
			double rotation = Math.toRadians((sector - 1) * (360.0 / 6) + 90.0);

			for (int layer = 1; layer <= 3; layer++) {

				int width = widths[layer - 1];
				int length = lengths[layer - 1];

				for (int paddle = 1; paddle <= npaddles[layer - 1]; paddle++) {

					DetectorShape2D shape = new DetectorShape2D();
					shape.getDescriptor().setType(DetectorType.FTOF);
					shape.getDescriptor().setSectorLayerComponent(sector, layer, paddle);
					shape.createBarXY(20 + length * paddle, width);
					shape.getShapePath().translateXYZ(0.0, 40 + width * paddle, 0.0);
					shape.getShapePath().rotateZ(rotation);
					if (!isNew) {
						if (engine.isGoodPaddle(sector, layer, paddle)) {
							shape.setColor(101, 200, 59); // green
						} else {
							shape.setColor(225, 75, 60); // red
						}
					}
					detectorView.getView().addShape(names[layer - 1], shape);
				}
			}
		}

		if (isNew) {
			detectorView.updateBox();
		}
		detectorView.repaint();

	}

	public JPanel getPanel() {
		return pane;
	}

	public void constantsEvent(CalibrationConstants cc, int col, int row) {

		String str_sector = (String) cc.getValueAt(row, 0);
		String str_layer = (String) cc.getValueAt(row, 1);
		String str_component = (String) cc.getValueAt(row, 2);

		if (cc.getName() != selectedDir) {
			selectedDir = cc.getName();
			this.updateDetectorView(false);
		}

		selectedSector = Integer.parseInt(str_sector);
		selectedLayer = Integer.parseInt(str_layer);
		selectedPaddle = Integer.parseInt(str_component);

		updateCanvas();
	}

	public void updateCanvas() {

		IndexedList<DataGroup> group = getSelectedEngine().getDataGroup();
		getSelectedEngine().setPlotTitle(selectedSector, selectedLayer, selectedPaddle);

		if (group.hasItem(selectedSector, selectedLayer, selectedPaddle) == true) {
			DataGroup dataGroup = group.getItem(selectedSector, selectedLayer, selectedPaddle);
			this.canvas.clear();
			this.canvas.draw(dataGroup);
			getSelectedEngine().rescaleGraphs(canvas, selectedSector, selectedLayer, selectedPaddle);
			// canvas.getPad(0).setTitle(TOFCalibrationEngine.LAYER_NAME[selectedLayer-1]+"
			// Sector "+selectedSector+" Paddle "+selectedPaddle);
			this.canvas.update();
		} else {
			System.out.println(" ERROR: can not find the data group");
		}

	}

	public void processShape(DetectorShape2D shape) {

		// show summary
		selectedSector = shape.getDescriptor().getSector();
		selectedLayer = shape.getDescriptor().getLayer();
		selectedPaddle = 1;

		this.canvas.clear();
		this.canvas.draw(getSelectedEngine().getSummary(selectedSector, selectedLayer));
		canvas.getPad(0).setTitle("Calibration values for " + TOFCalibrationEngine.LAYER_NAME[selectedLayer - 1]
				+ " Sector " + selectedSector);
		this.canvas.update();

	}

	public void stateChanged(ChangeEvent e) {
		int i = ccview.getTabbedPane().getSelectedIndex();
		String tabTitle = ccview.getTabbedPane().getTitleAt(i);

		if (tabTitle != selectedDir) {
			selectedDir = tabTitle;
			this.updateDetectorView(false);
			this.updateCanvas();
		}
	}

	public void configure() {

		configFrame.setSize(900, 900);
		// configFrame.setSize(1000, 600); // vnc size
		configFrame.setLocationRelativeTo(pane);
		configFrame.setDefaultCloseOperation(configFrame.DO_NOTHING_ON_CLOSE);

		// Which steps
		JPanel stepOuterPanel = new JPanel(new BorderLayout());
		JPanel stepPanel = new JPanel(new GridBagLayout());
		stepOuterPanel.add(stepPanel, BorderLayout.NORTH);
		GridBagConstraints c = new GridBagConstraints();

		for (int i = 0; i < engines.length; i++) {
			c.gridx = 0;
			c.gridy = i;
			c.anchor = c.WEST;
			stepChecks[i].setName(engines[i].stepName);
			stepChecks[i].setText(engines[i].stepName);
			stepChecks[i].setSelected(true);
			stepChecks[i].addActionListener(this);
			stepPanel.add(stepChecks[i], c);
		}
		JPanel butPage1 = new configButtonPanel(this, false, "Next");
		stepOuterPanel.add(butPage1, BorderLayout.SOUTH);

		// configPane.add("Select steps", stepOuterPanel);

		// Previous calibration values
		JPanel confOuterPanel = new JPanel(new BorderLayout());
		Box confPanel = new Box(BoxLayout.Y_AXIS);
//		TofPrevConfigPanel[] engPanels = { new TofPrevConfigPanel(new TOFCalibrationEngine()),
//				new TofPrevConfigPanel(new TOFCalibrationEngine()), new TofPrevConfigPanel(new TOFCalibrationEngine()),
//				new TofPrevConfigPanel(new TOFCalibrationEngine()), new TofPrevConfigPanel(new TOFCalibrationEngine()),
//				new TofPrevConfigPanel(new TOFCalibrationEngine()), new TofPrevConfigPanel(new TOFCalibrationEngine()),
//				new TofPrevConfigPanel(new TOFCalibrationEngine()) };

		int j = 0;
		for (int i = 0; i < engines.length - 2; i++) { // skip HV, TDC, FADC, check
			if (i != 1 && i != 2) {
				engPanels[j] = new TofPrevConfigPanel(engines[i]);
				confPanel.add(engPanels[j]);
				j++;
			}
		}
		// // add TDC Conv at the end
		// engPanels[engPanels.length-1] = new TofPrevConfigPanel(engines[TDC_CONV]);
		// confPanel.add(engPanels[engPanels.length-1]);

		JPanel butPage2 = new configButtonPanel(this, false, "Next");
		confOuterPanel.add(confPanel, BorderLayout.NORTH);
		confOuterPanel.add(butPage2, BorderLayout.SOUTH);

		configPane.add("Previous calibration values", confOuterPanel);

		int y = 0;
		// Tracking options
		JPanel trOuterPanel = new JPanel(new BorderLayout());
		JPanel trPanel = new JPanel(new GridBagLayout());
		trOuterPanel.add(trPanel, BorderLayout.NORTH);
		c.weighty = 1;
		c.anchor = c.NORTHWEST;
		c.insets = new Insets(3, 3, 3, 3);

		// Target GMEAN channel
		c.gridx = 0;
		c.gridy = y;
		trPanel.add(new JLabel("Target GMEAN channel 1a/1b/2:"), c);
		c.gridx = 1;
		c.gridy = y;
		JPanel tgmPanel = new JPanel();
		targetGMean[0].addActionListener(this);
		targetGMean[1].addActionListener(this);
		targetGMean[2].addActionListener(this);
		targetGMean[0].setText("650");
		targetGMean[1].setText("700");
		targetGMean[2].setText("650");
		tgmPanel.add(targetGMean[0]);
		tgmPanel.add(targetGMean[1]);
		tgmPanel.add(targetGMean[2]);
		trPanel.add(tgmPanel, c);
		c.gridx = 2;
		c.gridy = y;
		trPanel.add(new JLabel(""), c);

		// Path length normalisation
		y++;
		c.gridx = 0;
		c.gridy = y;
		trPanel.add(new JLabel("Path length normalisation for gmean?:"), c);
		pathNormList.addItem("Yes");
		pathNormList.addItem("No");
		pathNormList.addActionListener(this);
		c.gridx = 1;
		c.gridy = y;
		trPanel.add(pathNormList, c);
		c.gridx = 2;
		c.gridy = y;
		trPanel.add(new JLabel(""), c);

		// Chi squared
		y++;
		c.gridx = 0;
		c.gridy = y;
		trPanel.add(new JLabel("Maximum reduced chi squared for track (1a and 1b / 2):"), c);
		c.gridx = 1;
		c.gridy = y;
		JPanel rcsPanel = new JPanel();
		rcsText.addActionListener(this);
		rcsText2.addActionListener(this);
		rcsText.setText("75.0");
		rcsText2.setText("5000.0");
		rcsPanel.add(rcsText);
		rcsPanel.add(rcsText2);
		trPanel.add(rcsPanel, c);
		c.gridx = 2;
		c.gridy = y;
		trPanel.add(new JLabel("Enter 0 for no cut"), c);
		// vertex min
		y++;
		c.gridx = 0;
		c.gridy = y;
		trPanel.add(new JLabel("Minimum vertex z:"), c);
		minVText.addActionListener(this);
		minVText.setText("-10.0");
		c.gridx = 1;
		c.gridy = y;
		trPanel.add(minVText, c);
		// vertex max
		y++;
		c.gridx = 0;
		c.gridy = y;
		trPanel.add(new JLabel("Maximum vertex z:"), c);
		maxVText.addActionListener(this);
		maxVText.setText("5.0");
		c.gridx = 1;
		c.gridy = y;
		trPanel.add(maxVText, c);
		// Vertex time correction
		y++;
		c.gridx = 0;
		c.gridy = y;
		trPanel.add(new JLabel("Vertex time correction?:"), c);
		vertexCorrList.addItem("Yes");
		vertexCorrList.addItem("No");
		vertexCorrList.addActionListener(this);
		c.gridx = 1;
		c.gridy = y;
		trPanel.add(vertexCorrList, c);
		c.gridx = 2;
		c.gridy = y;
		trPanel.add(new JLabel(""), c);
		// Target position
		y++;
		c.gridx = 0;
		c.gridy = y;
		trPanel.add(new JLabel("Target position (cm):"), c);
		targetPosText.addActionListener(this);
		targetPosText.setText("-3.0");
		c.gridx = 1;
		c.gridy = y;
		trPanel.add(targetPosText, c);
		// p min
		y++;
		c.gridx = 0;
		c.gridy = y;
		trPanel.add(new JLabel("Momentum range (GeV):"), c);

		JPanel pPanel = new JPanel();
		minPText.addActionListener(this);
		minPText.setText("0.4");
		maxPText.addActionListener(this);
		maxPText.setText("10.0");
		pPanel.add(minPText);
		pPanel.add(new JLabel(" - "));
		pPanel.add(maxPText);
		c.gridx = 1;
		c.gridy = y;
		trPanel.add(pPanel, c);

		// min E
		y++;
		c.gridx = 0;
		c.gridy = y;
		trPanel.add(new JLabel("Minimum energy deposition (MeV):"), c);
		c.gridx = 1;
		c.gridy = y;
		minEText.addActionListener(this);
		minEText.setText("0.5");
		trPanel.add(minEText, c);

		// mass assumption
		y++;
		c.gridx = 0;
		c.gridy = y;
		trPanel.add(new JLabel("Mass assumption for beta calculation:"), c);
		c.gridx = 1;
		c.gridy = y;
		massAssList.addItem("Pion");
		massAssList.addItem("Proton");
		massAssList.addItem("Electron");
		massAssList.addItem("Use PID");
		massAssList.setSelectedIndex(USE_PID);
		massAssList.addActionListener(this);
		trPanel.add(massAssList, c);

		// track charge
		y++;
		c.gridx = 0;
		c.gridy = y;
		trPanel.add(new JLabel("Track charge:"), c);
		trackChargeList.addItem("Both");
		trackChargeList.addItem("Negative");
		trackChargeList.addItem("Positive");
		trackChargeList.addActionListener(this);
		c.gridx = 1;
		c.gridy = y;
		trPanel.add(trackChargeList, c);
		// RF period
		y++;
		c.gridx = 0;
		c.gridy = y;
		trPanel.add(new JLabel("RF Period:"), c);
		trfList.addItem(4.008);
		trfList.addItem(2.004);
		trfList.addActionListener(this);
		c.gridx = 1;
		c.gridy = y;
		trPanel.add(trfList, c);

		// PID
		y++;
		c.gridx = 0;
		c.gridy = y;
		trPanel.add(new JLabel("PID (1a and 1b / 2):"), c);
		c.gridx = 1;
		c.gridy = y;
		JPanel pidPanel = new JPanel();
		pidList.addItem("All");
		pidList.addItem("Leptons");
		pidList.addItem("Pions");
		pidList.addItem("Protons");
		pidList.addItem("Leptons and pions");
		pidList.setSelectedIndex(PID_LPI);
		pidList.addActionListener(this);
		pidPanel.add(pidList);
		pidList2.addItem("All");
		pidList2.addItem("Leptons");
		pidList2.addItem("Pions");
		pidList2.addItem("Protons");
		pidList2.addItem("Leptons and pions");
		pidList2.setSelectedIndex(PID_ALL);
		pidList2.addActionListener(this);
		pidPanel.add(pidList2, c);
		trPanel.add(pidPanel, c);
		c.gridx = 2;
		c.gridy = y;
		trPanel.add(new JLabel(""), c);

		// trigger
		y++;
		c.gridx = 0;
		c.gridy = y;
		trPanel.add(new JLabel("Trigger:"), c);
		triggerText.addActionListener(this);
		c.gridx = 1;
		c.gridy = y;
		trPanel.add(triggerText, c);
		c.gridx = 2;
		c.gridy = y;
		trPanel.add(new JLabel("Not currently used"), c);

		// graph type
		y++;
		c.gridx = 0;
		c.gridy = y;
		trPanel.add(new JLabel("2D histogram graph method:"), c);
		c.gridx = 1;
		c.gridy = y;
		fitList.addItem("Max position of slices");
		fitList.addItem("Gaussian mean of slices");
		fitList.addActionListener(this);
		trPanel.add(fitList, c);
		// fit mode
		y++;
		c.gridx = 0;
		c.gridy = y;
		trPanel.add(new JLabel("Slicefitter mode:"), c);
		c.gridx = 1;
		c.gridy = y;
		// fitModeList.addItem("L");
		fitModeList.addItem("");
		fitModeList.addItem("N");
		trPanel.add(fitModeList, c);
		fitModeList.addActionListener(this);
		// min events
		y++;
		c.gridx = 0;
		c.gridy = y;
		trPanel.add(new JLabel("Minimum events per slice:"), c);
		minEventsText.addActionListener(this);
		minEventsText.setText("2");
		c.gridx = 1;
		c.gridy = y;
		trPanel.add(minEventsText, c);

		// TDC range
		y++;
		c.gridx = 0;
		c.gridy = y;
		trPanel.add(new JLabel("TDC range:"), c);
		c.gridx = 1;
		c.gridy = y;
		JPanel tdcPanel = new JPanel();
		minTDCText.addActionListener(this);
		minTDCText.setText("8500");
		tdcPanel.add(minTDCText);
		tdcPanel.add(new JLabel(" - "));
		maxTDCText.addActionListener(this);
		maxTDCText.setText("15000");
		tdcPanel.add(maxTDCText);
		trPanel.add(tdcPanel, c);
		c.gridx = 2;
		c.gridy = y;
		trPanel.add(new JLabel(""), c);

		// FADC range and width
		y++;
		c.gridx = 0;
		c.gridy = y;
		trPanel.add(new JLabel("FADC x range / width:"), c);
		c.gridx = 1;
		c.gridy = y;
		JPanel fadcPanel = new JPanel();
		minFADCxText.addActionListener(this);
		minFADCxText.setText("-10.0");
		fadcPanel.add(minFADCxText);
		fadcPanel.add(new JLabel(" - "));
		maxFADCxText.addActionListener(this);
		maxFADCxText.setText("60.0");
		fadcPanel.add(maxFADCxText);
		widthFADCText.addActionListener(this);
		widthFADCText.setText("10.0");
		fadcPanel.add(widthFADCText);
		trPanel.add(fadcPanel, c);
		c.gridx = 2;
		c.gridy = y;
		trPanel.add(new JLabel(""), c);

		JPanel butPage3 = new configButtonPanel(this, true, "Finish");
		trOuterPanel.add(butPage3, BorderLayout.SOUTH);

		configPane.add("Tracking / General", trOuterPanel);

		configFrame.add(configPane);
		configFrame.setVisible(true);

	}

	public static void main(String[] args) {

		TOFCalibration calibGUI = new TOFCalibration();

	}

}
