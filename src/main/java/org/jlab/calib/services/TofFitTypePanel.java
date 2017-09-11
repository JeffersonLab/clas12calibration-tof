package org.jlab.calib.services;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

public class TofFitTypePanel extends JPanel 
implements ActionListener {

	TOFCalibrationEngine engine;
	JComboBox<String> fitList = new JComboBox<String>();
	JComboBox<String> fitModeList = new JComboBox<String>();
	private JTextField minEventsText = new JTextField(5);
	
	public TofFitTypePanel(TOFCalibrationEngine engineIn) {

		engine = engineIn;

		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		JPanel outerPanel = new JPanel(new BorderLayout());
		JPanel panel = new JPanel(new GridBagLayout());
		outerPanel.add(panel, BorderLayout.NORTH);
		c.weighty = 1;
		c.anchor = c.NORTHWEST;
		c.insets = new Insets(3,3,3,3);
		// graph type
		c.gridx = 0;
		c.gridy = 0;
		panel.add(new JLabel("Create graph from 2D histogram using:"),c);
		c.gridx = 1;
		c.gridy = 0;
		fitList.addItem("Gaussian mean of slices");
		fitList.addItem("Max position of slices");
		fitList.addActionListener(this);
		panel.add(fitList,c);
		// fit mode
		c.gridx = 0;
		c.gridy = 1;
		panel.add(new JLabel("Slicefitter mode:"),c);
		c.gridx = 1;
		c.gridy = 1;
		//fitModeList.addItem("L");
		fitModeList.addItem("N");
		panel.add(fitModeList,c);
		fitModeList.addActionListener(this);
		// min events
		c.gridx = 0;
		c.gridy = 2;
		panel.add(new JLabel("Minimum events per slice:"),c);
		minEventsText.addActionListener(this);
		minEventsText.setText("100");
		c.gridx = 1;
		c.gridy = 2;
		panel.add(minEventsText,c);

		

		this.setBorder(BorderFactory.createTitledBorder(engine.stepName));

	}

	public void actionPerformed(ActionEvent e) {
		

	}

}
