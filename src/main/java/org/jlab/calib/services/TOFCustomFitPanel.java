package org.jlab.calib.services;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

public class TOFCustomFitPanel extends JPanel implements ActionListener, FocusListener {
	
	public JTextField[] textFields;
	public int applyLevel = 0;
	public static final int APPLY_P = 0;
	public static final int APPLY_SL = 1;
	public static final int APPLY_L = 2;
	public static final int APPLY_R = 3;
	
	public int useRange = 0;
	public static final int USE_RANGE_Y = 1;
	public static final int USE_RANGE_N = 0;
	JRadioButton rangeRad = new JRadioButton("Apply to range");
	public JTextField minS = new JTextField(3);
	public JTextField maxS = new JTextField(3);
	public JTextField minL = new JTextField(3);
	public JTextField maxL = new JTextField(3);
	public JTextField minP = new JTextField(3);
	public JTextField maxP = new JTextField(3);
	
	public TOFCustomFitPanel(String[] fields, int sector, int layer, int useRangeIn){
		useRange = useRangeIn;
		CreatePanel(fields, sector, layer);		
	}
	
	public TOFCustomFitPanel(String[] fields, int sector, int layer){
		useRange = USE_RANGE_N;
		CreatePanel(fields, sector, layer);
	}

	public void CreatePanel(String[] fields, int sector, int layer){
		
		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weighty = 1;
		c.anchor = c.NORTHWEST;
		c.insets = new Insets(3,3,3,3);		
		
		// how many spaces
		int numFields = fields.length;
		for (int i=0; i < fields.length; i++) {
			if (fields[i] == "SPACE") {
				numFields--;
			}
		}
		
		JTextField[] newTextFields = new JTextField[numFields];
		textFields = newTextFields;
		
		JPanel fieldsPanel = new JPanel(new GridLayout(fields.length,2));
		
		// Initialize the text fields
		for (int i=0; i< numFields; i++) { 
			textFields[i] = new JTextField(5);
		}
		
		// Create fields
		int fieldNum = 0;
		for (int i=0; i< fields.length; i++) {
			
			if (fields[i] == "SPACE") {
				fieldsPanel.add(new JLabel(""));
				fieldsPanel.add(new JLabel(""));
			}
			else {
				fieldsPanel.add(new JLabel(fields[i]));
				fieldsPanel.add(textFields[fieldNum]);
				fieldNum++;
			}
			
		}
		
		// Radio for single paddle or all in sector/layer
		JRadioButton singleRad = new JRadioButton("Single paddle");
		JRadioButton allRad = new JRadioButton("All paddles in sector "+sector
				+" layer "+layer);
		JRadioButton allLayerRad = new JRadioButton("All paddles in layer "+layer);
		singleRad.setSelected(true);
		ButtonGroup radGroup = new ButtonGroup();
		radGroup.add(singleRad);
		radGroup.add(allRad);
		radGroup.add(allLayerRad);
		singleRad.addActionListener(this);
		allRad.addActionListener(this);
		allLayerRad.addActionListener(this);
		
		JPanel radPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		radPanel.add(singleRad);
		radPanel.add(allRad);
		radPanel.add(allLayerRad);
		
		c.gridx = 0;
		c.gridy = 0;
		this.add(fieldsPanel,c);
		c.gridx = 0;
		c.gridy = 1;
		this.add(radPanel,c);
		
		if (useRange == USE_RANGE_Y) {
			
			JPanel rangePanel = new JPanel(new GridBagLayout());
			c.gridx = 0;
			c.gridy = 0;
			
			radGroup.add(rangeRad);
			rangeRad.addActionListener(this);
			rangePanel.add(rangeRad,c);
			
			c.gridx = 1;
			c.gridy = 0;
			rangePanel.add(new JLabel("Sector"),c);
			c.gridx = 1;
			c.gridy = 1;
			rangePanel.add(new JLabel("Layer"),c);
			c.gridx = 1;
			c.gridy = 2;
			rangePanel.add(new JLabel("Component"),c);
			
			c.gridx = 2;
			c.gridy = 0;
			rangePanel.add(minS,c);
			minS.addFocusListener(this);
			c.gridx = 2;
			c.gridy = 1;
			rangePanel.add(minL,c);			
			minL.addFocusListener(this);
			c.gridx = 2;
			c.gridy = 2;
			rangePanel.add(minP,c);			
			minP.addFocusListener(this);
			
			c.gridx = 3;
			c.gridy = 0;
			rangePanel.add(new JLabel(" to "),c);
			c.gridx = 3;
			c.gridy = 1;
			rangePanel.add(new JLabel(" to "),c);
			c.gridx = 3;
			c.gridy = 2;
			rangePanel.add(new JLabel(" to "),c);

			c.gridx = 4;
			c.gridy = 0;
			rangePanel.add(maxS,c);
			maxS.addFocusListener(this);
			c.gridx = 4;
			c.gridy = 1;
			rangePanel.add(maxL,c);		
			maxL.addFocusListener(this);
			c.gridx = 4;
			c.gridy = 2;
			rangePanel.add(maxP,c);
			maxP.addFocusListener(this);
			
			c.gridx = 0;
			c.gridy = 2;
			add(rangePanel,c);

		}
				
	}
	
	public void focusGained(FocusEvent e) {
		rangeRad.setSelected(true);
		applyLevel = APPLY_R;
	}
	
	public void focusLost(FocusEvent e) {
		
	}

	public void actionPerformed(ActionEvent e) {

		if (e.getActionCommand().startsWith("Single")) {
			applyLevel = APPLY_P;
		}
		else if (e.getActionCommand().startsWith("All paddles in sector")) {
			applyLevel = APPLY_SL;
		}
		else if (e.getActionCommand().startsWith("All paddles in layer")) {
			applyLevel = APPLY_L;
		}
		else if (e.getActionCommand().startsWith("Apply to range")) {
			applyLevel = APPLY_R;
		}
		
	}	
	
}

