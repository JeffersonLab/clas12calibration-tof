package org.jlab.calib.services;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.time.format.DateTimeFormatter;
import java.util.Date;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.jlab.detector.calib.tasks.CalibrationEngine;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.CalibrationConstantsView;

public class TOFHVAdjustPanel 	extends JPanel
								implements ActionListener {
	
	JFileChooser fc;
	CalibrationConstants calib;
	TofHVEventListener hv;

	public TOFHVAdjustPanel(TofHVEventListener hvIn) {
		
		hv = hvIn;
		
		setLayout(new BorderLayout());
		
		JSplitPane   splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		JPanel buttonPanel = new JPanel();
		
		// create the table
		CalibrationConstantsView ccview = new CalibrationConstantsView();
		calib = new CalibrationConstants(3,
				"current_HV_left/F:current_HV_right/F:new_HV_left/F:new_HV_right/F");
		calib.setName("/calibration/ftof/hv");
		calib.setPrecision(3);
		ccview.addConstants(calib);
		
		for (int sector = 1; sector <= 6; sector++) {
			for (int layer = 1; layer <= hv.NUM_LAYERS; layer++) {
				int layer_index = layer - 1;
				for (int paddle = 1; paddle <= hv.NUM_PADDLES[layer_index]; paddle++) {
					calib.addEntry(sector, layer, paddle);
					calib.setDoubleValue(0.0,"current_HV_left", sector, layer, paddle);
					calib.setDoubleValue(0.0,"current_HV_right", sector, layer, paddle);
					calib.setDoubleValue(0.0,"new_HV_left", sector, layer, paddle);
					calib.setDoubleValue(0.0,"new_HV_right", sector, layer, paddle);
				}
			}
		}
		
		// Create field for file selection
		buttonPanel.add(new JLabel("Select EPICS snapshot file to calculate new HV values:"));
		fc = new JFileChooser();
	    JButton fileButton = new JButton("Select File");
	    fileButton.addActionListener(this);
	    buttonPanel.add(fileButton);
	    
	    splitPane.setTopComponent(buttonPanel);
	    splitPane.setBottomComponent(ccview);
	    add(splitPane);
	}
	
	
	public void actionPerformed(ActionEvent e) {
		
		if(e.getActionCommand().compareTo("Select File")==0) {
			
			//fc.setCurrentDirectory(new File("/home/louise/FTOF_calib_rewrite/input_files/hvfiles"));
			int returnValue = fc.showOpenDialog(null);
			if (returnValue == JFileChooser.APPROVE_OPTION) {
				
				String outputFileName = processFile();
				JOptionPane.showMessageDialog(new JPanel(),"High voltage values written to "+outputFileName);
				calib.fireTableDataChanged();
				
			}
		
		}
	}
	
	public String processFile() {
		String line = null;
		String outputFileName = nextFileName();
		
		try { 

			// Open the input file
			FileReader fileReader = 
					new FileReader(fc.getSelectedFile().getAbsolutePath());
			BufferedReader bufferedReader = 
					new BufferedReader(fileReader);
			
			// Open the output file
			File outputFile = new File(outputFileName);
			FileWriter outputFw = new FileWriter(outputFile.getAbsoluteFile());
			BufferedWriter outputBw = new BufferedWriter(outputFw);

			// Write the header
			outputBw.write("--- Start BURT header");
			outputBw.newLine();
			outputBw.write("FTOF Calibration HV recalculation");
			outputBw.newLine();
			Date today = new Date();
			DateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy HH:mm:ss");
			String todayString = dateFormat.format(today);
			outputBw.write("Date and time: "+todayString);
			outputBw.newLine();
			outputBw.write("Login ID: "+System.getProperty("user.name"));
			outputBw.newLine();
			outputBw.write("Input HV file: "+fc.getSelectedFile().getAbsolutePath());
			outputBw.newLine();
			outputBw.write("--- End BURT header");
			outputBw.newLine();
			
			line = bufferedReader.readLine();
			while (line != null) {
				
				if (!line.contains(":vset")) {
					// only process the vset lines
					line = bufferedReader.readLine();
					continue;
				}

				String[] lineValues;
                lineValues = line.split("_| |:");
                
                int sector = Integer.parseInt(lineValues[4].replace("SEC", ""));
                
                String panelName = lineValues[5];
                int layer = 0;
                if (panelName.equals("PANEL1A")) {
                	layer = 1;
                }
                else if (panelName.equals("PANEL1B")) {
                	layer = 2;
                }
                else if (panelName.equals("PANEL2")) {
                	layer = 3;
                }
                
                String pmt = lineValues[6];
                int paddle = Integer.parseInt(lineValues[7].replace("E", ""));

                double origVoltage = Double.parseDouble(lineValues[10]);
                if (origVoltage==0.0) {
                	line = bufferedReader.readLine();
                	continue;
                }

                double newHV = hv.newHV(sector, layer, paddle, origVoltage, pmt);
                
                // write to file only if voltage is changing
                if (newHV != origVoltage) {
                	String commandString = line.split(" ")[0];
                	outputBw.write(commandString+" 1 "+ new DecimalFormat("0.00000E00").format(newHV).replace("E", "+e"));
                	outputBw.newLine();

                }
                
                // update table
            	if (pmt.equals("L")) {
            		calib.setDoubleValue(origVoltage,"current_HV_left", sector, layer, paddle);
            		calib.setDoubleValue(newHV,"new_HV_left", sector, layer, paddle);
            	}
            	else {
            		calib.setDoubleValue(origVoltage,"current_HV_right", sector, layer, paddle);
            		calib.setDoubleValue(newHV,"new_HV_right", sector, layer, paddle);
            	}

                line = bufferedReader.readLine();
			}
		
            bufferedReader.close();    
    		outputBw.close();
    		calib.fireTableDataChanged();
        }
		catch(FileNotFoundException ex) {
			ex.printStackTrace();
            System.out.println(
                "Unable to open file '" + 
                outputFileName + "'");                
        }
        catch(IOException ex) {
            System.out.println(
                "Error reading file '" 
                + outputFileName + "'");                   
            // Or we could just do this: 
            ex.printStackTrace();
		}
		
		return outputFileName;
	}
	 
	public String nextFileName() {

		// Get the next file name
		Date today = new Date();
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		String todayString = dateFormat.format(today);
		String filePrefix = hv.hvSetPrefix + todayString;
		int newFileNum = 0;

		File dir = new File(".");
		File[] filesList = dir.listFiles();

		for (File file : filesList) {
			if (file.isFile()) {
				String fileName = file.getName();
				if (fileName.matches(filePrefix + "[.]\\d+[.]snp")) {
					String fileNumString = fileName.substring(
							fileName.indexOf('.') + 1,
							fileName.lastIndexOf('.'));
					int fileNum = Integer.parseInt(fileNumString);
					if (fileNum >= newFileNum)
						newFileNum = fileNum + 1;

				}
			}
		}

		return filePrefix + "." + newFileNum + ".snp";
	}	
	
}