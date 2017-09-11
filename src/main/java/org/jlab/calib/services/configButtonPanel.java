package org.jlab.calib.services;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class configButtonPanel extends JPanel {

	
	public configButtonPanel(ActionListener l, boolean backEnabled, String nextText) {
		this.setLayout(new BorderLayout());
		
		JButton nextButton = new JButton(nextText);
		nextButton.addActionListener(l);
		JButton backButton = new JButton("Back");
		backButton.setEnabled(backEnabled);
		backButton.addActionListener(l);
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(l);

		Box buttonBox = new Box(BoxLayout.X_AXIS);
	    buttonBox.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10))); 
	    buttonBox.add(backButton);
	    buttonBox.add(Box.createHorizontalStrut(10));
	    buttonBox.add(nextButton);
	    buttonBox.add(Box.createHorizontalStrut(30));
	    buttonBox.add(cancelButton);
	    this.add(buttonBox, java.awt.BorderLayout.EAST);
	}

}
