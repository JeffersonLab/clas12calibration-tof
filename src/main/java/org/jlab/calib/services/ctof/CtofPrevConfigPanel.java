package org.jlab.calib.services.ctof;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.jlab.calib.services.TOFCalibration;
import org.jlab.detector.calib.tasks.CalibrationEngine;

public class CtofPrevConfigPanel extends JPanel 
implements ActionListener, FocusListener {

	CTOFCalibrationEngine engine;
	JTextField fileDisp = new JTextField(20); 
	JTextField runText = new JTextField(5);
	JFileChooser fc = new JFileChooser();
	
	JRadioButton defaultRad = new JRadioButton("DEFAULT");
	JRadioButton fileRad = new JRadioButton("FILE");
	JRadioButton dbRad = new JRadioButton("DB");
	
	public CtofPrevConfigPanel(CTOFCalibrationEngine engineIn) {

		engine = engineIn;
		File workDir = new File(System.getProperty("user.dir"));
		fc.setCurrentDirectory(workDir);

		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		defaultRad.setSelected(true);
		ButtonGroup radGroup = new ButtonGroup();
		radGroup.add(defaultRad);
		radGroup.add(fileRad);
		radGroup.add(dbRad);
		defaultRad.addActionListener(this);
		fileRad.addActionListener(this);
		dbRad.addActionListener(this);

		JPanel drPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		drPanel.add(defaultRad);
		c.anchor = c.LINE_START;
		c.gridx = 0;
		c.gridy = 0;
		this.add(drPanel,c);
		c.gridx = 1;
		c.gridy = 0;
		this.add(new JPanel(),c);

		JPanel frPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		frPanel.add(fileRad);
		c.gridx = 0;
		c.gridy = 1;
		this.add(frPanel,c);
		JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		fileDisp.setEditable(false);
		fileDisp.setText("None selected");
		filePanel.add(new JLabel("Selected file: "));
		filePanel.add(fileDisp);
		JButton fileButton = new JButton("Select File");
		fileButton.addActionListener(this);
		filePanel.add(fileButton,c);
		c.gridx = 1;
		c.gridy = 1;
		this.add(filePanel,c);

		JPanel dbrPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		dbrPanel.add(dbRad);
		c.gridx = 0;
		c.gridy = 2;
		this.add(dbrPanel,c);
		JPanel runPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel runLabel = new JLabel("Run number:");
		runPanel.add(runLabel);
		runPanel.add(runText);
		runText.addFocusListener(this);
		c.gridx = 1;
		c.gridy = 2;
		this.add(runPanel,c);

		this.setBorder(BorderFactory.createTitledBorder(engine.stepName));

	}

	public void actionPerformed(ActionEvent e) {
		
		if (e.getActionCommand() == "Select File") {
			int returnValue = fc.showOpenDialog(null);
			if (returnValue == JFileChooser.APPROVE_OPTION) {
				engine.calDBSource = engine.CAL_FILE;
				engine.prevCalFilename = fc.getSelectedFile().getAbsolutePath();
				fileDisp.setText("Selected file: "+ fc.getSelectedFile().getAbsolutePath());
				fileRad.setSelected(true);

				// Set all the working directories to the one just selected
				for (int i=0; i<CTOFCalibration.engPanels.length; i++) {
					CTOFCalibration.engPanels[i].fc.setCurrentDirectory(fc.getSelectedFile());
				}

			}
		}

		if (e.getActionCommand() == "DB") {
			engine.calDBSource = engine.CAL_DB;
		}
		else if (e.getActionCommand() == "FILE") {
			engine.calDBSource = engine.CAL_FILE;
		}
		else if (e.getActionCommand() == "DEFAULT") {
			engine.calDBSource = engine.CAL_DEFAULT;
		}
	}

	public void focusGained(FocusEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void focusLost(FocusEvent e) {
		engine.prevCalRunNo = Integer.parseInt(runText.getText());
	}

}
