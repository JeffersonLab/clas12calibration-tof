package org.jlab.calib.services;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

public class TOFCustomFitPanel extends JPanel implements ActionListener {
	
	public JTextField[] textFields;
	public boolean applyToAll = false;

	public TOFCustomFitPanel(String[] fields, int sector, int layer){
		
		this.setLayout(new BorderLayout());
		
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
		singleRad.setSelected(true);
		ButtonGroup radGroup = new ButtonGroup();
		radGroup.add(singleRad);
		radGroup.add(allRad);
		singleRad.addActionListener(this);
		allRad.addActionListener(this);
		
		JPanel radPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		radPanel.add(singleRad);
		radPanel.add(allRad);
		
		this.add(fieldsPanel, BorderLayout.NORTH);
		this.add(radPanel, BorderLayout.SOUTH);
				
	}

	public void actionPerformed(ActionEvent e) {

		if (e.getActionCommand().startsWith("Single")) {
			applyToAll = false;
		}
		else if (e.getActionCommand().startsWith("All paddles")) {
			applyToAll = true;
		}
		
		
	}	
	
}

