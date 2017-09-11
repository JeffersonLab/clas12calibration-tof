package org.jlab.calib.services;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog.ModalityType;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;

import org.jlab.detector.base.DetectorType;
import org.jlab.detector.calib.tasks.CalibrationEngine;
import org.jlab.detector.calib.tasks.CalibrationEngineView;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.CalibrationConstantsListener;
import org.jlab.detector.calib.utils.CalibrationConstantsView;
import org.jlab.detector.view.DetectorListener;
import org.jlab.detector.view.DetectorPane2D;
import org.jlab.detector.view.DetectorShape2D;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
import org.jlab.groot.math.F1D;
import org.jlab.groot.ui.TCanvas;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataEventType;
import org.jlab.io.task.DataSourceProcessorPane;
import org.jlab.io.task.IDataEventListener;
import org.jlab.utils.groups.IndexedList;

public class TOFCalibration implements IDataEventListener, ActionListener, 
CalibrationConstantsListener, DetectorListener,
ChangeListener {

	// main panel
	JPanel          pane         = null;
	JFrame  innerConfigFrame = new JFrame("Configure FTOF calibration settings");
	JDialog configFrame = new JDialog(innerConfigFrame, "Configure FTOF calibration settings");
	JTabbedPane configPane = new JTabbedPane();

	// detector panel
	DetectorPane2D            detectorView        = null;

	// event reading panel
	DataSourceProcessorPane processorPane = null;
	public final int UPDATE_RATE = 200000;

	// calibration view
	EmbeddedCanvas     canvas = null;   
	CalibrationConstantsView ccview = null;

	TOFCalibrationEngine[] engines = {
			new TofHVEventListener(),
			new TofAttenEventListener(),
			new TofTdcConvEventListener(),
			new TofLeftRightEventListener(),
			new TofVeffEventListener(),
			new TofTimeWalkEventListener(),
			new TofRFPadEventListener(),
			new TofP2PEventListener(),
			new TofCheckEventListener()};

	// engine indices
	public final int HV = 0;
	public final int ATTEN = 1;
	public final int TDC_CONV = 2;    
	public final int LEFT_RIGHT = 3;
	public final int VEFF = 4;
	public final int TW = 5;
	public final int RFPAD = 6;
	public final int P2P = 7;
	public final int CHECK = 8;

	String[] dirs = {"/calibration/ftof/gain_balance",
			"/calibration/ftof/attenuation",
			"/calibration/ftof/tdc_conv",
			"/calibration/ftof/timing_offset/left_right",
			"/calibration/ftof/effective_velocity",
			"/calibration/ftof/time_walk",
			"/calibration/ftof/timing_offset/rfpad",
	"/calibration/ftof/timing_offset/P2P",
	"/calibration/ftof/timing_offset/check"};

	String selectedDir = "None";
	int selectedSector = 1;
	int selectedLayer = 1;
	int selectedPaddle = 1;

	String[] buttons = {"View all", "Adjust Fit/Override", "Adjust HV", "Write"};
	// button indices
	public final int VIEW_ALL = 0;
	public final int FIT_OVERRIDE = 1;
	public final int ADJUST_HV = 2;
	public final int WRITE = 3;

	// test histograms
	//    public static H1F trackRCS = new H1F("red_chi_sq","Reduced chi^2 for tracks", 
	//            200, 0.0, 200.0);
	//    public static H1F trackRCS2 = new H1F("red_chi_sq2","Reduced chi^2 for tracks", 
	//            100, 0.0, 40.0);
	//    public static H1F vertexHist = new H1F("vertex_hist","Vertex z", 
	//            160, -40.0, 40.0);

	// configuration settings
	JCheckBox[] stepChecks = {new JCheckBox(),new JCheckBox(),new JCheckBox(),new JCheckBox(),
			new JCheckBox(),new JCheckBox(),new JCheckBox(),new JCheckBox(), new JCheckBox()};    
	private JTextField rcsText = new JTextField(5);
	public static double maxRcs = 0.0;
	private JTextField minVText = new JTextField(5);
	public static double minV = -9999.0;
	private JTextField maxVText = new JTextField(5);
	public static double maxV = 9999.0;
	private JTextField minPText = new JTextField(5);
	public static double minP = 0.0;
	JComboBox<String> trackChargeList = new JComboBox<String>();
	public static int trackCharge = 0;
	public final static int TRACK_BOTH = 0;
	public final static int TRACK_NEG = 1;
	public final static int TRACK_POS = 2;
	JComboBox<String> pidList = new JComboBox<String>();
	public static int trackPid = 0;
	public final static int PID_BOTH = 0;
	public final static int PID_E = 1;
	public final static int PID_PI = 2;
	private JTextField triggerText = new JTextField(10);
	public static int triggerBit = 0;    
	JComboBox<String> counterSectionList = new JComboBox<String>();
	private JTextField sectionWidthText = new JTextField(5);

	// Time walk method
	JComboBox<String> twMethodList = new JComboBox<String>();
	public static int twMethod = 0;
	public final static int TW_POS_INDEP = 0;
	public final static int TW_POS_DEP = 1;



	JComboBox<String> fitList = new JComboBox<String>();
	JComboBox<String> fitModeList = new JComboBox<String>();
	private JTextField minEventsText = new JTextField(5);

	public final static PrintStream oldStdout = System.out;

	public static int DATA_TYPE = 0;
	public static final int REAL_DATA = 0;
	public static final int GEMC_DATA = 1;
	public static boolean dataTypeKnown = false;

	public TOFCalibration() {

		configFrame.setModalityType(ModalityType.APPLICATION_MODAL);
		configure();

		pane = new JPanel();
		pane.setLayout(new BorderLayout());

		JSplitPane   splitPane = new JSplitPane();

		// combined panel for detector view and button panel
		JPanel combined = new JPanel();
		combined.setLayout(new BorderLayout());

		detectorView = new DetectorPane2D();
		detectorView.getView().addDetectorListener(this);

		JPanel butPanel = new JPanel();
		for (int i=0; i < buttons.length; i++) {
			JButton button = new JButton(buttons[i]);
			button.addActionListener(this);
			butPanel.add(button);            
		}
		combined.add(detectorView, BorderLayout.CENTER);
		combined.add(butPanel,BorderLayout.PAGE_END);

		this.updateDetectorView(true);

		splitPane.setLeftComponent(combined);

		// Create the engine views with this GUI as listener
		JPanel engineView = new JPanel();
		JSplitPane          enginePane = null;
		engineView.setLayout(new BorderLayout());
		enginePane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

		canvas = new EmbeddedCanvas();
		ccview = new CalibrationConstantsView();
		ccview.getTabbedPane().addChangeListener(this);

		for (int i=0; i < engines.length; i++) {
			if (engines[i].engineOn) {
				ccview.addConstants(engines[i].getCalibrationConstants().get(0),this);
				ccview.getTabbedPane().setEnabled(false);
			}
		}

		enginePane.setTopComponent(canvas);
		enginePane.setBottomComponent(ccview);
		enginePane.setDividerLocation(0.6);
		enginePane.setResizeWeight(0.6);
		engineView.add(splitPane,BorderLayout.CENTER);

		splitPane.setRightComponent(enginePane);
		pane.add(splitPane,BorderLayout.CENTER);

		processorPane = new DataSourceProcessorPane();
		processorPane.setUpdateRate(UPDATE_RATE);

		// only add the gui as listener so that extracting paddle list from event is only done once per event
		this.processorPane.addEventListener(this);

		//        this.processorPane.addEventListener(engines[0]);
		//        this.processorPane.addEventListener(this); // add gui listener second so detector view updates 
		//                                                   // as soon as 1st analyze is done
		//        for (int i=1; i< engines.length; i++) {
		//            this.processorPane.addEventListener(engines[i]);
		//        }
		pane.add(processorPane,BorderLayout.PAGE_END);

		JFrame frame = new JFrame("FTOF Calibration");
		frame.setSize(1800, 1000);

		frame.add(pane);
		//frame.pack();
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
        } else if (selectedDir == dirs[CHECK]) {
            engine = engines[CHECK];
		}

		return engine;
	}    

	public void actionPerformed(ActionEvent e) {

		TOFCalibrationEngine engine = getSelectedEngine();

		if (e.getActionCommand().compareTo(buttons[VIEW_ALL])==0) {

			engine.showPlots(selectedSector, selectedLayer);

		}
		else if (e.getActionCommand().compareTo(buttons[FIT_OVERRIDE])==0) {

			engine.customFit(selectedSector, selectedLayer, selectedPaddle);
			updateDetectorView(false);
			this.updateCanvas();
		}
		else if (e.getActionCommand().compareTo(buttons[ADJUST_HV])==0) {

			if (engines[HV].engineOn) {
				JFrame hvFrame = new JFrame("Adjust HV");
				hvFrame.add(new TOFHVAdjustPanel((TofHVEventListener) engines[HV]));
				hvFrame.setSize(1000, 800);
				hvFrame.setVisible(true);
				hvFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
			}
			else {
				JOptionPane.showMessageDialog(new JPanel(),
						"Adjust HV is available only when the HV calibration step has been included.");
			}

		}
		else if (e.getActionCommand().compareTo(buttons[WRITE])==0) {

			String outputFilename = engine.nextFileName();
			engine.writeFile(outputFilename);
			JOptionPane.showMessageDialog(new JPanel(),
					engine.stepName + " calibration values written to "+outputFilename);
		}

		// config settings
		//        if (e.getSource() == stepChecks[TDC_CONV]) {
		//            configPane.setEnabledAt(3, stepChecks[TDC_CONV].isSelected());
		//        }
		//        if (e.getSource() == stepChecks[TW]) {
		//            configPane.setEnabledAt(4, stepChecks[TW].isSelected());
		//        }
		if (e.getActionCommand().compareTo("Next")==0) {
			int currentTab = configPane.getSelectedIndex();
			for (int i=currentTab+1; i<configPane.getTabCount(); i++) {
				if (configPane.isEnabledAt(i)) {
					configPane.setSelectedIndex(i);
					break;
				}
			}
		}
		if (e.getActionCommand().compareTo("Back")==0) {
			int currentTab = configPane.getSelectedIndex();
			for (int i=currentTab-1; i>=0; i--) {
				if (configPane.isEnabledAt(i)) {
					configPane.setSelectedIndex(i);
					break;
				}
			}        
		}
		if (e.getActionCommand().compareTo("Cancel")==0) {
			System.exit(0);
		}

		if (e.getActionCommand().compareTo("Finish")==0) {
			configFrame.setVisible(false);

			System.out.println("");
			System.out.println(todayString());
			System.out.println("Configuration settings - Selected steps");
			System.out.println("---------------------------------------");
			// step selection
			for (int i=0; i < engines.length; i++) {
				engines[i].engineOn = stepChecks[i].isSelected();
				if (selectedDir.compareTo("None")==0 && engines[i].engineOn) {
					selectedDir = dirs[i];
				}
				System.out.println(engines[i].stepName+" "+engines[i].engineOn);
			}

			System.out.println("");
			System.out.println("Configuration settings - Previous calibration values");
			System.out.println("----------------------------------------------------");
			// get the previous iteration calibration values
			for (int i=0; i< engines.length; i++) {
				engines[i].populatePrevCalib();

				if (!engines[i].prevCalRead) {
					System.out.println("Problem populating "+engines[i].stepName+" previous calibration values - exiting");
					System.exit(0);
				}
			}

			// set the config values
			if (rcsText.getText().compareTo("") != 0) {
				maxRcs = Double.parseDouble(rcsText.getText());
			}
			if (minVText.getText().compareTo("") != 0) {
				minV = Double.parseDouble(minVText.getText());
			}
			if (maxVText.getText().compareTo("") != 0) {
				maxV = Double.parseDouble(maxVText.getText());
			}    
			if (minPText.getText().compareTo("") != 0) {
				minP = Double.parseDouble(minPText.getText());
			}
			trackCharge = trackChargeList.getSelectedIndex();
			trackPid = pidList.getSelectedIndex();
			twMethod = twMethodList.getSelectedIndex();

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
			engines[TW].counterSection = counterSectionList.getSelectedIndex();
			engines[RFPAD].counterSection = counterSectionList.getSelectedIndex();
			if (sectionWidthText.getText().compareTo("") != 0) {
				engines[TW].sectionWidth = Double.parseDouble(sectionWidthText.getText());
				engines[RFPAD].sectionWidth = Double.parseDouble(sectionWidthText.getText());
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

			System.out.println("");
			System.out.println("Configuration settings - Tracking/General");
			System.out.println("-----------------------------------------");
			System.out.println("Maximum reduced chi squared for tracks: "+maxRcs);
			System.out.println("Minimum vertex z: "+minV);
			System.out.println("Maximum vertex z: "+maxV);
			System.out.println("Minimum momentum from tracking (GeV): "+minP);
			System.out.println("Track charge: "+trackChargeList.getItemAt(trackCharge));
			System.out.println("PID: "+pidList.getItemAt(trackPid));
			System.out.println("Trigger: "+triggerBit);
			System.out.println("Counter section: "+counterSectionList.getSelectedItem());
			System.out.println("Position dependent TW correction?: "+twMethodList.getItemAt(twMethod));
			System.out.println("Width of section (cm): "+engines[TW].sectionWidth);
			System.out.println("2D histogram graph method: "+fitList.getSelectedItem());
			System.out.println("Slicefitter mode: "+fitModeList.getSelectedItem());
			System.out.println("Minimum events per slice: "+minEventsText.getText());
			System.out.println("");
		}
	}

	public void dataEventAction(DataEvent event) {

		// Set the data type
		if (!dataTypeKnown) {
			if (event.hasBank("FTOF::adc")) { // not just a run config bank
				if (event.hasBank("MC::Particle")) {
					DATA_TYPE = GEMC_DATA;
				}
				else {
					DATA_TYPE = REAL_DATA;
				}
				dataTypeKnown = true;
			}
		}

		List<TOFPaddle> paddleList = DataProvider.getPaddleList(event);

		for (int i=0; i< engines.length; i++) {

			if (engines[i].engineOn) {

				if (event.getType()==DataEventType.EVENT_START) {
					engines[i].resetEventListener();
					engines[i].processPaddleList(paddleList);

				}
				else if (event.getType()==DataEventType.EVENT_ACCUMULATE) {
					engines[i].processPaddleList(paddleList);
				}
				else if (event.getType()==DataEventType.EVENT_STOP) {
					System.setOut(oldStdout);
					System.out.println("EVENT_STOP for "+engines[i].stepName+" "+todayString());
					engines[i].analyze();
					ccview.getTabbedPane().setEnabled(true);
					System.setOut(oldStdout);
				} 

				if (event.getType()==DataEventType.EVENT_STOP) {
					this.updateDetectorView(false);
					this.updateCanvas();
					//if (i==0) this.showTestHists();
				}
			}
		}
	}

	private String todayString() {
		Date today = new Date();
		DateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy HH:mm:ss");
		String todayString = dateFormat.format(today);

		return todayString;
	}

	public void resetEventListener() {

	}

	public void timerUpdate() {

		for (int i=0; i< engines.length; i++) {
			if (engines[i].engineOn) {
				engines[i].timerUpdate();
				ccview.getTabbedPane().setEnabled(true);
			}
		}

		this.updateDetectorView(false);
		this.updateCanvas();
	}

	public final void updateDetectorView(boolean isNew){

		TOFCalibrationEngine engine = getSelectedEngine();

		double FTOFSize = 500.0;
		int[]     npaddles = new int[]{23,62,5};
		int[]     widths   = new int[]{6,15,25};
		int[]     lengths  = new int[]{6,15,25};

		String[]  names    = new String[]{"FTOF 1A","FTOF 1B","FTOF 2"};
		for(int sector = 1; sector <= 6; sector++){
			double rotation = Math.toRadians((sector-1)*(360.0/6)+90.0);

			for(int layer = 1; layer <=3; layer++){

				int width  = widths[layer-1];
				int length = lengths[layer-1];

				for(int paddle = 1; paddle <= npaddles[layer-1]; paddle++){

					DetectorShape2D shape = new DetectorShape2D();
					shape.getDescriptor().setType(DetectorType.FTOF);
					shape.getDescriptor().setSectorLayerComponent(sector, layer, paddle);
					shape.createBarXY(20 + length*paddle, width);
					shape.getShapePath().translateXYZ(0.0, 40 + width*paddle , 0.0);
					shape.getShapePath().rotateZ(rotation);
					if (!isNew) {
						if (engine.isGoodPaddle(sector, layer, paddle)) {
							shape.setColor(101,200,59); //green
						}
						else {
							shape.setColor(225,75,60); //red
						}
					}    
					detectorView.getView().addShape(names[layer-1], shape);
				}
			}
		}

		if (isNew) {
			detectorView.updateBox();
		}
		detectorView.repaint();

	}

	public JPanel  getPanel(){
		return pane;
	}

	public void constantsEvent(CalibrationConstants cc, int col, int row) {

		String str_sector    = (String) cc.getValueAt(row, 0);
		String str_layer     = (String) cc.getValueAt(row, 1);
		String str_component = (String) cc.getValueAt(row, 2);

		if (cc.getName() != selectedDir) {
			selectedDir = cc.getName();
			this.updateDetectorView(false);
		}

		selectedSector    = Integer.parseInt(str_sector);
		selectedLayer     = Integer.parseInt(str_layer);
		selectedPaddle = Integer.parseInt(str_component);

		updateCanvas();
	}

	public void updateCanvas() {

		IndexedList<DataGroup> group = getSelectedEngine().getDataGroup();
		getSelectedEngine().setPlotTitle(selectedSector,selectedLayer,selectedPaddle);

		if(group.hasItem(selectedSector,selectedLayer,selectedPaddle)==true){
			DataGroup dataGroup = group.getItem(selectedSector,selectedLayer,selectedPaddle);
			this.canvas.clear();
			this.canvas.draw(dataGroup);
			getSelectedEngine().rescaleGraphs(canvas, selectedSector, selectedLayer, selectedPaddle);
			canvas.getPad(0).setTitle(TOFCalibrationEngine.LAYER_NAME[selectedLayer-1]+" Sector "+selectedSector+" Paddle "+selectedPaddle);
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
		canvas.getPad(0).setTitle("Calibration values for "
				+TOFCalibrationEngine.LAYER_NAME[selectedLayer-1]+" Sector "+selectedSector);
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

	//    public void showTestHists() {
	//        JFrame frame = new JFrame("Track Reduced Chi Squared");
	//        frame.setSize(1000, 800);
	//        EmbeddedCanvas canvas = new EmbeddedCanvas();
	//        canvas.cd(0);
	//        canvas.draw(trackRCS);
	//        frame.add(canvas);
	//        frame.setVisible(true);
	//        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
	//        
	//        JFrame frame2 = new JFrame("Track Reduced Chi Squared");
	//        frame2.setSize(1000, 800);
	//        EmbeddedCanvas canvas2 = new EmbeddedCanvas();
	//        canvas2.cd(0);
	//        canvas2.draw(trackRCS2);
	//        frame2.add(canvas2);
	//        frame2.setVisible(true);
	//        frame2.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
	//        
	//        JFrame frame3 = new JFrame("Vertex z");
	//        frame3.setSize(1000, 800);
	//        EmbeddedCanvas canvas3 = new EmbeddedCanvas();
	//        canvas3.cd(0);
	//        canvas3.draw(vertexHist);
	//        frame3.add(canvas3);
	//        frame3.setVisible(true);
	//        frame3.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
	//
	//    }

	public void configure() {

		configFrame.setSize(900, 800);
		//configFrame.setSize(1000, 600);
		configFrame.setLocationRelativeTo(pane);
		configFrame.setDefaultCloseOperation(configFrame.DO_NOTHING_ON_CLOSE);

		// Which steps    
		JPanel stepOuterPanel = new JPanel(new BorderLayout());
		JPanel stepPanel = new JPanel(new GridBagLayout());
		stepOuterPanel.add(stepPanel, BorderLayout.NORTH);
		GridBagConstraints c = new GridBagConstraints();

		for (int i=0; i< engines.length; i++) {
			c.gridx = 0; c.gridy = i;
			c.anchor = c.WEST;
			stepChecks[i].setName(engines[i].stepName);
			stepChecks[i].setText(engines[i].stepName);
			stepChecks[i].setSelected(true);
			stepChecks[i].addActionListener(this);
			stepPanel.add(stepChecks[i],c);
		}
		JPanel butPage1 = new configButtonPanel(this, false, "Next");
		stepOuterPanel.add(butPage1, BorderLayout.SOUTH);

		configPane.add("Select steps", stepOuterPanel);    

		// Previous calibration values
		JPanel confOuterPanel = new JPanel(new BorderLayout());
		Box confPanel = new Box(BoxLayout.Y_AXIS);
		TofPrevConfigPanel[] engPanels = {new TofPrevConfigPanel(new TOFCalibrationEngine()), 
				new TofPrevConfigPanel(new TOFCalibrationEngine()), 
				new TofPrevConfigPanel(new TOFCalibrationEngine()),
				new TofPrevConfigPanel(new TOFCalibrationEngine()),
				new TofPrevConfigPanel(new TOFCalibrationEngine())};

		for (int i=3; i< engines.length-1; i++) {  // skip HV, attenuation, TDC, check
			engPanels[i-3] = new TofPrevConfigPanel(engines[i]);
			confPanel.add(engPanels[i-3]);
		}

		JPanel butPage2 = new configButtonPanel(this, true, "Next");
		confOuterPanel.add(confPanel, BorderLayout.NORTH);
		confOuterPanel.add(butPage2, BorderLayout.SOUTH);

		configPane.add("Previous calibration values", confOuterPanel);

		// Tracking options
		JPanel trOuterPanel = new JPanel(new BorderLayout());
		JPanel trPanel = new JPanel(new GridBagLayout());
		trOuterPanel.add(trPanel, BorderLayout.NORTH);
		c.weighty = 1;
		c.anchor = c.NORTHWEST;
		c.insets = new Insets(3,3,3,3);
		// Chi squared
		c.gridx = 0;
		c.gridy = 0;
		trPanel.add(new JLabel("Maximum reduced chi squared for track:"),c);
		rcsText.addActionListener(this);
		rcsText.setText("75.0");
		c.gridx = 1;
		c.gridy = 0;
		trPanel.add(rcsText,c);
		c.gridx = 2;
		c.gridy = 0;
		trPanel.add(new JLabel("Enter 0 for no cut"),c);
		// vertex min
		c.gridx = 0;
		c.gridy = 1;
		trPanel.add(new JLabel("Minimum vertex z:"),c);
		minVText.addActionListener(this);
		minVText.setText("-10.0");
		c.gridx = 1;
		c.gridy = 1;
		trPanel.add(minVText,c);
		// vertex max
		c.gridx = 0;
		c.gridy = 2;
		trPanel.add(new JLabel("Maximum vertex z:"),c);
		maxVText.addActionListener(this);
		maxVText.setText("10.0");
		c.gridx = 1;
		c.gridy = 2;
		trPanel.add(maxVText,c);
		// p min
		c.gridx = 0;
		c.gridy = 3;
		trPanel.add(new JLabel("Minimum momentum from tracking (GeV):"),c);
		minPText.addActionListener(this);
		minPText.setText("1.0");
		c.gridx = 1;
		c.gridy = 3;
		trPanel.add(minPText,c);
		// track charge
		c.gridx = 0;
		c.gridy = 4;
		trPanel.add(new JLabel("Track charge:"),c);
		trackChargeList.addItem("Both");
		trackChargeList.addItem("Negative");
		trackChargeList.addItem("Positive");
		trackChargeList.addActionListener(this);
		c.gridx = 1;
		c.gridy = 4;
		trPanel.add(trackChargeList,c);
		// PID
		c.gridx = 0;
		c.gridy = 5;
		trPanel.add(new JLabel("PID:"),c);
		pidList.addItem("Both");
		pidList.addItem("Electrons");
		pidList.addItem("Pions");
		pidList.addActionListener(this);
		c.gridx = 1;
		c.gridy = 5;
		trPanel.add(pidList,c);
		c.gridx = 2;
		c.gridy = 5;
		trPanel.add(new JLabel("Not currently used"),c);
		// trigger
		c.gridx = 0;
		c.gridy = 6;
		trPanel.add(new JLabel("Trigger:"),c);
		triggerText.addActionListener(this);
		c.gridx = 1;
		c.gridy = 6;
		trPanel.add(triggerText,c);        
		c.gridx = 2;
		c.gridy = 6;
		trPanel.add(new JLabel("Not currently used"),c);
		// counter section
		c.gridx = 0;
		c.gridy = 7;
		trPanel.add(new JLabel("Counter section:"),c);
		c.gridx = 1;
		c.gridy = 7;
		counterSectionList.addItem("Full counter");
		counterSectionList.addItem("Center section");
		counterSectionList.addItem("Left end");
		counterSectionList.addItem("Right end");
		trPanel.add(counterSectionList,c);
		counterSectionList.addActionListener(this);
		c.gridx = 2;
		c.gridy = 7;
		trPanel.add(new JLabel("Applied to Time walk and RF paddle"),c);
		// position cut
		c.gridx = 0;
		c.gridy = 8;
		trPanel.add(new JLabel("Section width (cm):"),c);
		sectionWidthText.addActionListener(this);
		sectionWidthText.setText("20.0");
		c.gridx = 1;
		c.gridy = 8;
		trPanel.add(sectionWidthText,c);            
		c.gridx = 2;
		c.gridy = 8;
		trPanel.add(new JLabel("Applied to Time walk and RF paddle"),c);

		// graph type
		c.gridx = 0;
		c.gridy = 9;
		trPanel.add(new JLabel("2D histogram graph method:"),c);
		c.gridx = 1;
		c.gridy = 9;
		fitList.addItem("Max position of slices");
		fitList.addItem("Gaussian mean of slices");
		fitList.addActionListener(this);
		trPanel.add(fitList,c);
		// fit mode
		c.gridx = 0;
		c.gridy = 10;
		trPanel.add(new JLabel("Slicefitter mode:"),c);
		c.gridx = 1;
		c.gridy = 10;
		//fitModeList.addItem("L");
		fitModeList.addItem("");
		fitModeList.addItem("N");
		trPanel.add(fitModeList,c);
		fitModeList.addActionListener(this);
		// min events
		c.gridx = 0;
		c.gridy = 11;
		trPanel.add(new JLabel("Minimum events per slice:"),c);
		minEventsText.addActionListener(this);
		minEventsText.setText("10");
		c.gridx = 1;
		c.gridy = 11;
		trPanel.add(minEventsText,c);

		// Position dependent time walk
		c.gridx = 0;
		c.gridy = 12;
		trPanel.add(new JLabel("Position dependent TW correction?:"),c);
		twMethodList.addItem("No");
		twMethodList.addItem("Yes");
		twMethodList.addActionListener(this);
		c.gridx = 1;
		c.gridy = 12;
		trPanel.add(twMethodList,c);
		c.gridx = 2;
		c.gridy = 12;
		//trPanel.add(new JLabel("Applied to subtracted nominal correction"),c);
		trPanel.add(new JLabel("<html>Applied to subtracted nominal correction<br/> in time walk plot</html>"),c);

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
