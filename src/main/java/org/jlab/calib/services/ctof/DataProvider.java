package org.jlab.calib.services.ctof;

import java.util.ArrayList;
import java.util.List;
import org.jlab.calib.services.TOFPaddle;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;

/**
 *
 * @author gavalian
 */
public class DataProvider {

	private static boolean test = false;

	public static List<TOFPaddle> getPaddleList(DataEvent event) {

		if (test) {
			String[] bankList = event.getBankList();
			for (int bi = 0; bi < bankList.length; bi++) {
				System.out.println("Bank : " + bankList[bi]);
			}
			event.show();
			if (event.hasBank("REC::Event")) {
				event.getBank("REC::Event").show();
			}
			if (event.hasBank("CTOF::adc")) {
				event.getBank("CTOF::adc").show();
			}
			if (event.hasBank("CTOF::tdc")) {
				event.getBank("CTOF::tdc").show();
			}
			if (event.hasBank("CTOF::hits")) {
				event.getBank("CTOF::hits").show();
			}
			if (event.hasBank("CVTRec::Tracks")) {
				event.getBank("CVTRec::Tracks").show();
			}
			if (event.hasBank("RUN::rf")) {
				event.getBank("RUN::rf").show();
			}
			if (event.hasBank("RUN::config")) {
				event.getBank("RUN::config").show();
			}
			if (event.hasBank("MC::Particle")) {
				event.getBank("MC::Particle").show();
			}
			if (event.hasBank("REC::Track")) {
				event.getBank("REC::Track").show();
			}
			if (event.hasBank("REC::Scintillator")) {
				event.getBank("REC::Scintillator").show();
			}
			if (event.hasBank("FTOF::calib")) {
				event.getBank("FTOF::calib").show();
                        }
		}

		List<TOFPaddle> paddleList = getPaddleListHipo(event);
		return paddleList;

	}
	

	public static List<TOFPaddle> getPaddleListHipo(DataEvent event) {

		ArrayList<TOFPaddle> paddleList = new ArrayList<TOFPaddle>();
		// Set the status flags
		if (event.hasBank("CTOF::adc")) {
			DataBank adcBank = event.getBank("CTOF::adc");

			for (int i = 0; i < adcBank.rows(); i++) {
				int component = adcBank.getShort("component", i);
				int order = adcBank.getByte("order", i);
				int adc = adcBank.getInt("ADC", i);
				if (order == 0 && adc != 0) {
					CTOFCalibrationEngine.adcLeftStatus.add(0, 1, 1, component);
				}
				if (order == 1 && adc != 0) {
					CTOFCalibrationEngine.adcRightStatus.add(0, 1, 1, component);
				}
			}
		}
		if (event.hasBank("CTOF::tdc")) {
			DataBank tdcBank = event.getBank("CTOF::tdc");

			for (int i = 0; i < tdcBank.rows(); i++) {
				int component = tdcBank.getShort("component", i);
				int order = tdcBank.getByte("order", i);
				int tdc = tdcBank.getInt("TDC", i);
				if (order == 2 && tdc != 0) {
					CTOFCalibrationEngine.tdcLeftStatus.add(0, 1, 1, component);
				}
				if (order == 3 && tdc != 0) {
					CTOFCalibrationEngine.tdcRightStatus.add(0, 1, 1, component);
				}
			}
		}

		if (!event.hasBank("RUN::config")) {
			return paddleList;
		}

                
                DataBank configBank = event.getBank("RUN::config");
                long triggerBit = configBank.getLong("trigger", 0);
                int run = configBank.getInt("run", 0);
                long timeStamp = configBank.getLong("timestamp", 0);

                // iterate through hits bank getting corresponding adc and tdc
                if (event.hasBank("CTOF::calib")) {

                        DataBank hitsBank = event.getBank("CTOF::calib");

                        for (int hitIndex = 0; hitIndex < hitsBank.rows(); hitIndex++) {

                                TOFPaddle  paddle = new TOFPaddle(1, 1, (int) hitsBank.getShort("component", hitIndex));

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

                                if (paddle.energy() > 0.5 && event.hasBank("REC::Event")) {


                                        DataBank eventBank = event.getBank("REC::Event");
                                        double trf = eventBank.getFloat("RFTime",0);

                                        paddle.setPATH_LENGTH(hitsBank.getFloat("pathLength", hitIndex));
                                        paddle.setPATH_LENGTH_BAR(hitsBank.getFloat("pathLengthThruBar", hitIndex));
                                        paddle.setRF_TIME(trf);

                                        paddle.setP(Math.sqrt(Math.pow(hitsBank.getFloat("px", hitIndex),2)+
                                                              Math.pow(hitsBank.getFloat("py", hitIndex),2)+
                                                              Math.pow(hitsBank.getFloat("pz", hitIndex),2)));
                                        paddle.setP(hitsBank.getFloat("px", hitIndex));
                                        paddle.setTRACK_ID(hitsBank.getInt("trackid", hitIndex));
                                        paddle.setVERTEX_Z(hitsBank.getFloat("vz", hitIndex));
                                        paddle.setPARTICLE_ID(hitsBank.getInt("pid", hitIndex));
                        		paddle.setST_TIME(hitsBank.getFloat("vt", hitIndex));
                                        paddle.setCHARGE(hitsBank.getByte("charge", hitIndex));

                                        if (CTOFCalibration.maxRcs != 0.0) {
                                                paddle.setTRACK_REDCHI2(hitsBank.getFloat("chi2", hitIndex)/hitsBank.getShort("NDF", hitIndex));
                                        }

                                        if (paddle.includeInCalib()) {
                                                paddle.init();
                                                paddleList.add(paddle);
                                                if (test)
                                                        paddle.show();
                                        }        
                                }
                        }
                }
		else if(event.hasBank("CTOF::adc") && event.hasBank("CTOF::tdc") ) {
				// no hits bank, so just use adc and tdc

				// based on cosmic data
				// am getting entry for every PMT in ADC bank
				// ADC R two indices after ADC L (will assume right is always after left)
				// TDC bank only has actual hits, so can just search the whole bank for matching
				// SLC

			DataBank adcBank = event.getBank("CTOF::adc");
			DataBank tdcBank = event.getBank("CTOF::tdc");
                        for (int i = 0; i < adcBank.rows(); i++) {
                                int order = adcBank.getByte("order", i);
                                int adc = adcBank.getInt("ADC", i);
                                if (order == 0 && adc != 0) {

                                        int component = adcBank.getShort("component", i);
                                        int adcL = adc;
                                        int adcR = 0;
                                        float adcTimeL = adcBank.getFloat("time", i);
                                        float adcTimeR = 0;
                                        int tdcL = 0;
                                        int tdcR = 0;

                                        for (int j = 0; j < adcBank.rows(); j++) {
                                                int c = adcBank.getShort("component", j);
                                                int o = adcBank.getByte("order", j);
                                                if (c == component && o == 1) {
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
                                                int c = tdcBank.getShort("component", tdci);
                                                int o = tdcBank.getByte("order", tdci);
                                                if (c == component && o == 2) {
                                                        // matching tdc L
                                                        tdcL = tdcBank.getInt("TDC", tdci);
                                                        break;
                                                }
                                        }
                                        for (int tdci = 0; tdci < tdcBank.rows(); tdci++) {
                                                int c = tdcBank.getShort("component", tdci);
                                                int o = tdcBank.getByte("order", tdci);
                                                if (c == component && o == 3) {
                                                        // matching tdc R
                                                        tdcR = tdcBank.getInt("TDC", tdci);
                                                        break;
                                                }
                                        }

                                        // set status to ok if at least one reading
                                        if (adcL != 0) {
                                                CTOFCalibrationEngine.adcLeftStatus.add(0, 1, 1, component);
                                        }
                                        if (adcR != 0) {
                                                CTOFCalibrationEngine.adcRightStatus.add(0, 1, 1, component);
                                        }
                                        if (tdcL != 0) {
                                                CTOFCalibrationEngine.tdcLeftStatus.add(0, 1, 1, component);
                                        }
                                        if (tdcR != 0) {
                                                CTOFCalibrationEngine.tdcRightStatus.add(0, 1, 1, component);
                                        }

                                        if (test) {
                                                System.out.println("Values found " + component);
                                                System.out.println(adcL + " " + adcR + " " + tdcL + " " + tdcR);
                                        }

                                        if (adcL > 100 && adcR > 100) {

                                                TOFPaddle paddle = new TOFPaddle(1, 1, component);
                                                paddle.setAdcTdc(adcL, adcR, tdcL, tdcR);
                                                paddle.setRun(run, triggerBit, timeStamp);

                                                paddle.setADC_TIMEL(adcTimeL);
                                                paddle.setADC_TIMER(adcTimeR);

                                                paddle.init();

                                                // if (paddle.includeInCalib()) {

                                                if (test) {
                                                        System.out.println("Adding paddle " + component);
                                                        System.out.println(adcL + " " + adcR + " " + tdcL + " " + tdcR);
                                                }
                                                paddleList.add(paddle);
                                                // }
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
