package org.jlab.calib.services;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public class TofTimingOptionsPanel extends JPanel implements ActionListener {

	public int[] stepOptions = {1,1,1,1,1};
	public static final int UPDOWN = 0;
	public static final int RFPAD = 1;
	public static final int P2P = 2;
	public static final int USE_PREV = 0;
	public static final int USE_NEW = 1;
	JRadioButton[]   stepRadiosPrev = {new JRadioButton(), new JRadioButton(), new JRadioButton(), new JRadioButton(), new JRadioButton()};
	JRadioButton[]   stepRadiosNew = {new JRadioButton(), new JRadioButton(), new JRadioButton(), new JRadioButton(), new JRadioButton()};

	public TofTimingOptionsPanel(){

		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(3,3,3,3);

		ButtonGroup[] radGroups = {new ButtonGroup(), new ButtonGroup(), new ButtonGroup(), new ButtonGroup(), new ButtonGroup()};
		c.gridx = 1;
		c.gridy = 0;
		add(new JLabel("Previous"),c);
		c.gridx = 2;
		add(new JLabel("New"),c);

		String[] stepNames = {"Left Right","RF Paddle","P2P"};
		int numSteps = 3;
		if (TOFPaddle.tof == "CTOF") {
			stepNames[0] = "Up Down";
			numSteps = 3;
		}
		for (int i=0; i< numSteps; i++) {
			c.gridx = 0; c.gridy = i+1;
			c.anchor = c.WEST;

			add(new JLabel(stepNames[i]),c);
			//singleRad.setSelected(true);
			radGroups[i].add(stepRadiosPrev[i]);
			radGroups[i].add(stepRadiosNew[i]);
			stepRadiosPrev[i].addActionListener(this);
			stepRadiosNew[i].addActionListener(this);
			stepRadiosPrev[i].setName("choice"+i+""+0);
			stepRadiosNew[i].setName("choice"+i+""+1);
			stepRadiosNew[i].setSelected(true);

			c.gridx = 1;
			add(stepRadiosPrev[i],c);
			c.gridx = 2;
			add(stepRadiosNew[i],c);

		}
	}

	public void actionPerformed(ActionEvent e) {

		for (int i=0; i<5; i++) {
			if (stepRadiosPrev[i].isSelected()) {
				stepOptions[i] = 0;
			}
			else {
				stepOptions[i] = 1;
			}
		}

	}	

}

