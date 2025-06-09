package org.jlab.calib.services;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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

import org.jlab.detector.calib.tasks.CalibrationEngine;

public class TofPrevConfigPanel extends JPanel 
implements ActionListener, FocusListener {

	TOFCalibrationEngine engine;
	JTextField fileDisp = new JTextField(20); 
	JTextField runText = new JTextField(5);
	public JFileChooser fc = new JFileChooser();
	
	JRadioButton defaultRad = new JRadioButton("DEFAULT");
	JRadioButton fileRad = new JRadioButton("FILE");
	JRadioButton dbRad = new JRadioButton("DB");
	
	public TofPrevConfigPanel(TOFCalibrationEngine engineIn) {

		engine = engineIn;
		File workDir = new File(System.getProperty("user.dir"));
		fc.setCurrentDirectory(workDir);

		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weighty = 1;
		c.anchor = c.NORTHWEST;
		c.insets = new Insets(2,2,2,2);

		defaultRad.setSelected(true);
		ButtonGroup radGroup = new ButtonGroup();
		radGroup.add(defaultRad);
		radGroup.add(fileRad);
		radGroup.add(dbRad);
		defaultRad.addActionListener(this);
		fileRad.addActionListener(this);
		dbRad.addActionListener(this);

		c.gridx = 0;
		c.gridy = 0;
		add(defaultRad,c);
		c.gridx = 0;
		c.gridy = 1;
		add(fileRad,c);
		c.gridx = 1;
		c.gridy = 1;
		fileDisp.setEditable(false);
		fileDisp.setText("None selected");
		add(new JLabel("Selected file: "),c);
		c.gridx = 2;
		c.gridy = 1;
		add(fileDisp,c);
		c.gridx = 3;
		c.gridy = 1;
		JButton fileButton = new JButton("Select File");
		fileButton.addActionListener(this);
		add(fileButton,c);

		c.gridx = 0;
		c.gridy = 2;
		add(dbRad,c);
		JLabel runLabel = new JLabel("Run number:");
		c.gridx = 1;
		c.gridy = 2;
		add(runLabel,c);
		c.gridx = 2;
		c.gridy = 2;
		add(runText,c);
		runText.addFocusListener(this);

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
				for (int i=0; i<TOFCalibration.engPanels.length; i++) {
					TOFCalibration.engPanels[i].fc.setCurrentDirectory(fc.getSelectedFile());
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
