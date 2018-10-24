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
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import org.jlab.detector.calib.tasks.CalibrationEngine;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.CalibrationConstantsView;

public class TOFHVAdjustPanel 	extends JPanel
								implements ActionListener {
	
	JFileChooser fc;
	CalibrationConstants calib;
	TofHVEventListener hv;
	TOFCalibration tofCal;
	private JTextField mipPeakText[] = {new JTextField(5),new JTextField(5),new JTextField(5)};


	public TOFHVAdjustPanel(TofHVEventListener hvIn, TOFCalibration tofCalIn) {
		
		hv = hvIn;
		tofCal = tofCalIn;
		
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
		
		// Desired MIP peak position
		buttonPanel.add(new JLabel("Desired MIP peak position 1a/1b/2:"));
		JPanel mipPeakPanel = new JPanel();
		mipPeakText[0].addActionListener(this);
		mipPeakText[0].setText(Integer.toString(hv.NEWHV_MIP_CHANNEL[0]));
		mipPeakPanel.add(mipPeakText[0]);
		mipPeakText[1].addActionListener(this);
		mipPeakText[1].setText(Integer.toString(hv.NEWHV_MIP_CHANNEL[1]));
		mipPeakPanel.add(mipPeakText[1]);
		mipPeakText[2].addActionListener(this);
		mipPeakText[2].setText(Integer.toString(hv.NEWHV_MIP_CHANNEL[2]));
		mipPeakPanel.add(mipPeakText[2]);
		buttonPanel.add(mipPeakPanel);
		
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
				
				for (int i=0; i<3; i++) {
					hv.NEWHV_MIP_CHANNEL[i] = Integer.parseInt(mipPeakText[i].getText());
				}
				
				String outputFileName = processFile();
				JOptionPane.showMessageDialog(new JPanel(),"High voltage values written to "+outputFileName);
				calib.fireTableDataChanged();
				hv.calib.fireTableDataChanged();
				tofCal.updateDetectorView(false);
				
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
			// my header
//			outputBw.write("--- Start BURT header");
//			outputBw.newLine();
//			outputBw.write("FTOF Calibration HV recalculation");
//			outputBw.newLine();
//			Date today = new Date();
//			DateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy HH:mm:ss");
//			String todayString = dateFormat.format(today);
//			outputBw.write("Date and time: "+todayString);
//			outputBw.newLine();
//			outputBw.write("Login ID: "+System.getProperty("user.name"));
//			outputBw.newLine();
//			outputBw.write("Input HV file: "+fc.getSelectedFile().getAbsolutePath());
//			outputBw.newLine();
//			outputBw.write("--- End BURT header");
//			outputBw.newLine();
			
			// replicate slow controls header exactly
			outputBw.write("--- Start BURT header");
			outputBw.newLine();
			Date today = new Date();
			DateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
			String todayString = dateFormat.format(today);
			outputBw.write("Time:     "+todayString);
			outputBw.newLine();
			outputBw.write("Login ID: clasrun (Online DAQ)");
			outputBw.newLine();
			outputBw.write("Eff  UID: 2508");
			outputBw.newLine();
			outputBw.write("Group ID: 9998");
			outputBw.newLine();
			outputBw.write("Keywords: ");
			outputBw.newLine();
			outputBw.write("Comments: ");
			outputBw.newLine();
			outputBw.write("Type:     Absolute");
			outputBw.newLine();
			outputBw.write("Directory /home/clasrun");
			outputBw.newLine();
			outputBw.write("Req File: /usr/clas12/release/1.3.0/epics/tools/burtreq/CTOF_HV.req");
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
                	outputBw.write(commandString+" 1 "+ new DecimalFormat("0.00000E00").format(newHV).replace("E", "e+"));
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
		
		// now write a text file of the new and old HV values
		String filename = outputFileName.replace(".snp", ".txt");
		try { 

			// Open the output file
			File outputFile = new File(filename);
			FileWriter outputFw = new FileWriter(outputFile.getAbsoluteFile());
			BufferedWriter outputBw = new BufferedWriter(outputFw);

			for (int i=0; i<calib.getRowCount(); i++) {
				String hvline = new String();
				for (int j=0; j<calib.getColumnCount(); j++) {
					hvline = hvline+calib.getValueAt(i, j);
					if (j<calib.getColumnCount()-1) {
						hvline = hvline+" ";
					}
				}
				outputBw.write(hvline);
				outputBw.newLine();
			}

			outputBw.close();
		}
		catch(IOException ex) {
			System.out.println(
					"Error reading file '" 
							+ filename + "'");                   
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