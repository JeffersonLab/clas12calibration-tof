package org.jlab.calib.services;

import java.util.ArrayList;
import java.util.List;
import org.jlab.io.base.DataBank;
//import org.jlab.calib.temp.DataGroup;
import org.jlab.io.base.DataEvent;

/**
 *
 * @author gavalian
 */
public class DataProvider {

	private static boolean test = false;

	public static void setOutput(boolean outputOn) {
		if (outputOn) {
			System.setOut(TOFCalibration.oldStdout);
		} else {
			System.setOut(new java.io.PrintStream(
					new java.io.OutputStream() {
						public void write(int b) {
						}
					}));
		}
	}

	public static List<TOFPaddle> getPaddleList(DataEvent event) {

		List<TOFPaddle> paddleList = new ArrayList<TOFPaddle>();

		if (test) {
			// event.show();
		}
		// EvioDataEvent e = (EvioDataEvent) event;
		// e.show();

		paddleList = getPaddleListHipo(event);
		// paddleList = getPaddleListDgtzNew(event);

		return paddleList;

	}

	public static List<TOFPaddle> getPaddleListHipo(DataEvent event) {

		if (test) {

			event.show();
			if (event.hasBank("FTOF::adc")) {
				event.getBank("FTOF::adc").show();
			}
			if (event.hasBank("FTOF::tdc")) {
				event.getBank("FTOF::tdc").show();
			}
			if (event.hasBank("FTOF::hits")) {
				event.getBank("FTOF::hits").show();
			}
			if (event.hasBank("TimeBasedTrkg::TBTracks")) {
				event.getBank("TimeBasedTrkg::TBTracks").show();
			}
			if (event.hasBank("RUN::rf")) {
				event.getBank("RUN::rf").show();
			}
			if (event.hasBank("RUN::config")) {
				event.getBank("RUN::config").show();
			}
			if (event.hasBank("REC::Particle")) {
				event.getBank("REC::Particle").show();
			}
			if (event.hasBank("REC::Scintillator")) {
				event.getBank("REC::Scintillator").show();
			}
			if (event.hasBank("FTOF::calib")) {
				event.getBank("FTOF::calib").show();
			}
			try {
				if (event.hasBank("REC::Track")) {
					event.getBank("REC::Track").show();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		ArrayList<TOFPaddle> paddleList = new ArrayList<>();

		// Set the status flags
		if (event.hasBank("FTOF::adc")) {
			DataBank adcBank = event.getBank("FTOF::adc");

			for (int i = 0; i < adcBank.rows(); i++) {
				int sector = adcBank.getByte("sector", i);
				int layer = adcBank.getByte("layer", i);
				int component = adcBank.getShort("component", i);
				int order = adcBank.getByte("order", i);
				int adc = adcBank.getInt("ADC", i);
				if (order == 0 && adc != 0) {
					TOFCalibrationEngine.adcLeftStatus.add(0, sector, layer, component);
				}
				if (order == 1 && adc != 0) {
					TOFCalibrationEngine.adcRightStatus.add(0, sector, layer, component);
				}
			}
		}
		if (event.hasBank("FTOF::tdc")) {
			DataBank tdcBank = event.getBank("FTOF::tdc");

			for (int i = 0; i < tdcBank.rows(); i++) {
				int sector = tdcBank.getByte("sector", i);
				int layer = tdcBank.getByte("layer", i);
				int component = tdcBank.getShort("component", i);
				int order = tdcBank.getByte("order", i);
				int tdc = tdcBank.getInt("TDC", i);
				if (order == 2 && tdc != 0) {
					TOFCalibrationEngine.tdcLeftStatus.add(0, sector, layer, component);
				}
				if (order == 3 && tdc != 0) {
					TOFCalibrationEngine.tdcRightStatus.add(0, sector, layer, component);
				}
			}
		}
		
		// Only continue if we have calib banks
		if (!event.hasBank("FTOF::calib") || !event.hasBank("RUN::config")) {
			return paddleList;
		}

                DataBank  configBank = event.getBank("RUN::config");
                long triggerBit = configBank.getLong("trigger", 0);
                int  run        = configBank.getInt("run", 0);
                long timeStamp  = configBank.getLong("timestamp", 0);

                // iterate through hits bank getting corresponding adc and tdc
		if (event.hasBank("FTOF::calib") && event.hasBank("REC::Event") && event.hasBank("RUN::config")) {

                        DataBank eventBank = event.getBank("REC::Event");
					double trf = eventBank.getFloat("RFTime",0);
			DataBank  hitsBank = event.getBank("FTOF::calib");
                
                        for (int hitIndex=0; hitIndex<hitsBank.rows(); hitIndex++) {

				TOFPaddle  paddle = new TOFPaddle(
						(int) hitsBank.getByte("sector", hitIndex),
						(int) hitsBank.getByte("layer", hitIndex),
						(int) hitsBank.getShort("component", hitIndex));
                    
                                paddle.setRun(run, triggerBit, timeStamp);
                                
				paddle.setAdcTdc(
						hitsBank.getInt("adc1", hitIndex),
						hitsBank.getInt("adc2", hitIndex),
						hitsBank.getInt("tdc1", hitIndex),
						hitsBank.getInt("tdc2", hitIndex));

                                paddle.setPos(
                                                hitsBank.getFloat("tx", hitIndex),
                                                hitsBank.getFloat("ty", hitIndex),
                                                hitsBank.getFloat("tz", hitIndex)); 
//				paddle.ADC_TIMEL = adcBank.getFloat("time", hitsBank.getShort("adc_idx1", hitIndex));
//				paddle.ADC_TIMER = adcBank.getFloat("time", hitsBank.getShort("adc_idx2", hitIndex));
						
                                paddle.setRECON_TIME(hitsBank.getFloat("time", hitIndex));
				paddle.setENERGY(hitsBank.getFloat("energy", hitIndex));
						
				//System.out.println("Paddle created "+paddle.getDescriptor().getSector()+paddle.getDescriptor().getLayer()+paddle.getDescriptor().getComponent());

                                paddle.setPATH_LENGTH(hitsBank.getFloat("pathLength", hitIndex));
                                paddle.setPATH_LENGTH_BAR(hitsBank.getFloat("pathLengthThruBar", hitIndex));
                                paddle.setRF_TIME(trf);

                                paddle.setP(hitsBank.getFloat("p", hitIndex));
                                paddle.setTRACK_ID(hitsBank.getInt("trackid", hitIndex));
                                paddle.setVERTEX_Z(hitsBank.getFloat("vz", hitIndex));
                                paddle.setPARTICLE_ID(hitsBank.getInt("pid", hitIndex));
                                paddle.setCHARGE(hitsBank.getByte("charge", hitIndex));

                                if (TOFCalibration.maxRcs != 0.0) {
                                        paddle.setTRACK_REDCHI2(hitsBank.getFloat("chi2", hitIndex)/hitsBank.getShort("NDF", hitIndex));
                                }
	
                                if (paddle.includeInCalib()) {
					paddle.init();
                                        paddleList.add(paddle);
					if (test) {
						paddle.show();
					}
				}
				
                        }
		}
		else if(event.hasBank("FTOF::adc") && event.hasBank("FTOF::tdc") ) {
			// no hits bank, so just use adc and tdc

			// based on cosmic data
			// am getting entry for every PMT in ADC bank
			// ADC R two indices after ADC L (will assume right is always after left)
			// TDC bank only has actual hits, so can just search the whole bank for matching
			// SLC

			DataBank adcBank = event.getBank("FTOF::adc");
			DataBank tdcBank = event.getBank("FTOF::tdc");
			for (int i = 0; i < adcBank.rows(); i++) {
				int order = adcBank.getByte("order", i);
				int adc = adcBank.getInt("ADC", i);
				if (order == 0 && adc != 0) {

					int sector = adcBank.getByte("sector", i);
					int layer = adcBank.getByte("layer", i);
					int component = adcBank.getShort("component", i);
					int adcL = adc;
					int adcR = 0;
					float adcTimeL = adcBank.getFloat("time", i);
					float adcTimeR = 0;
					int tdcL = 0;
					int tdcR = 0;

					for (int j = 0; j < adcBank.rows(); j++) {
						int s = adcBank.getByte("sector", j);
						int l = adcBank.getByte("layer", j);
						int c = adcBank.getShort("component", j);
						int o = adcBank.getByte("order", j);
						if (s == sector && l == layer && c == component && o == 1) {
							// matching adc R
							adcR = adcBank.getInt("ADC", j);
							adcTimeR = adcBank.getFloat("time", j);
							break;
						}
					}

					// Now get matching TDCs
					// can search whole bank as it has fewer rows (only hits)
					// break when you find so always take the first one found
					for (int tdci = 0; tdci < tdcBank.rows(); tdci++) {
						int s = tdcBank.getByte("sector", tdci);
						int l = tdcBank.getByte("layer", tdci);
						int c = tdcBank.getShort("component", tdci);
						int o = tdcBank.getByte("order", tdci);
						if (s == sector && l == layer && c == component && o == 2) {
							// matching tdc L
							tdcL = tdcBank.getInt("TDC", tdci);
							break;
						}
					}
					for (int tdci = 0; tdci < tdcBank.rows(); tdci++) {
						int s = tdcBank.getByte("sector", tdci);
						int l = tdcBank.getByte("layer", tdci);
						int c = tdcBank.getShort("component", tdci);
						int o = tdcBank.getByte("order", tdci);
						if (s == sector && l == layer && c == component && o == 3) {
							// matching tdc R
							tdcR = tdcBank.getInt("TDC", tdci);
							break;
						}
					}

					// set status to ok if at least one reading
					if (adcL != 0) {
						TOFCalibrationEngine.adcLeftStatus.add(0, sector, layer, component);
					}
					if (adcR != 0) {
						TOFCalibrationEngine.adcRightStatus.add(0, sector, layer, component);
					}
					if (tdcL != 0) {
						TOFCalibrationEngine.tdcLeftStatus.add(0, sector, layer, component);
					}
					if (tdcR != 0) {
						TOFCalibrationEngine.tdcRightStatus.add(0, sector, layer, component);
					}

					if (test) {
						System.out.println("Values found " + sector + layer + component);
						System.out.println(adcL + " " + adcR + " " + tdcL + " " + tdcR);
					}

					if (adcL > 100 && adcR > 100) {

						TOFPaddle paddle = new TOFPaddle(
								sector,
								layer,
								component);
						paddle.setAdcTdc(adcL, adcR, tdcL, tdcR);
						paddle.setADC_TIMEL(adcTimeL);
						paddle.setADC_TIMER(adcTimeR);
						paddle.setRun(run, triggerBit, timeStamp);
						paddle.init();

						if (paddle.includeInCalib()) {

							if (test) {
								System.out.println("Adding paddle " + sector + layer + component);
								System.out.println(adcL + " " + adcR + " " + tdcL + " " + tdcR);
							}
							paddleList.add(paddle);
						}
					}
				}
			}
		}

		return paddleList;
	}

	public static void systemOut(String text) {
		boolean test = false;
		if (test) {
			System.out.println(text);
		}
	}

}
