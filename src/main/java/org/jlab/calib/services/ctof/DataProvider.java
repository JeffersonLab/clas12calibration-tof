package org.jlab.calib.services.ctof;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jlab.calib.services.TOFCalibrationEngine;
//import org.jlab.calib.services.TOFCalibrationEngine;
import org.jlab.calib.services.TOFPaddle;
import org.jlab.clas.physics.GenericKinematicFitter;
import org.jlab.clas.physics.Particle;
import org.jlab.clas.physics.PhysicsEvent;
import org.jlab.clas.physics.RecEvent;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.base.GeometryFactory;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.decode.CodaEventDecoder;
import org.jlab.detector.decode.DetectorDataDgtz;
import org.jlab.detector.decode.DetectorEventDecoder;
import org.jlab.geom.base.ConstantProvider;
import org.jlab.geom.base.Detector;
import org.jlab.geom.component.ScintillatorMesh;
import org.jlab.geom.component.ScintillatorPaddle;
import org.jlab.geom.detector.ftof.FTOFDetector;
import org.jlab.geom.detector.ftof.FTOFDetectorMesh;
import org.jlab.geom.detector.ftof.FTOFFactory;
import org.jlab.geom.prim.Line3D;
import org.jlab.geom.prim.Path3D;
import org.jlab.geom.prim.Point3D;
import org.jlab.geom.prim.Vector3D;
import org.jlab.groot.group.DataGroup;
import org.jlab.io.base.DataBank;
//import org.jlab.calib.temp.DataGroup;
import org.jlab.io.base.DataEvent;
import org.jlab.io.evio.EvioDataBank;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.utils.groups.IndexedList;
import org.jlab.utils.groups.IndexedTable;

/**
 *
 * @author gavalian
 */
public class DataProvider {

	private static boolean test = false;

	public static List<TOFPaddle> getPaddleList(DataEvent event) {

		if (test) {
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
			if (event.hasBank("REC::Particle")) {
				event.getBank("REC::Particle").show();
			}
			if (event.hasBank("REC::Scintillator")) {
				event.getBank("REC::Scintillator").show();
			}

		}
		
		
		List<TOFPaddle> paddleList = new ArrayList<TOFPaddle>();
		paddleList = getPaddleListHipo(event);
		return paddleList;

	}

	private static int getIdx(DataBank bank, int hitOrder, int hitComp) {

		int idx = -1;
		for (int i = 0; i < bank.rows(); i++) {
			int component = bank.getShort("component", i);
			int order = bank.getByte("order", i);
			if (component == hitComp && order == hitOrder) {
				idx = i;
				break;
			}
		}
		return idx;
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
			//// System.out.println("Louise 135");
		}
		if (event.hasBank("CTOF::tdc")) {
			//// System.out.println("Louise 138");
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

		// iterate through hits bank getting corresponding adc and tdc
		if (event.hasBank("CTOF::hits")) {
			
			// Only continue if we have adc and tdc banks
			if (!event.hasBank("CTOF::adc") || !event.hasBank("CTOF::tdc")) {
				return paddleList;
			}
			
			DataBank adcBank = event.getBank("CTOF::adc");
			DataBank tdcBank = event.getBank("CTOF::tdc");
			
			DataBank hitsBank = event.getBank("CTOF::hits");

			for (int hitIndex = 0; hitIndex < hitsBank.rows(); hitIndex++) {

				double tx = hitsBank.getFloat("tx", hitIndex);
				double ty = hitsBank.getFloat("ty", hitIndex);
				double tz = hitsBank.getFloat("tz", hitIndex);

				int component = (int) hitsBank.getShort("component", hitIndex);
				TOFPaddle paddle = new TOFPaddle(1, 1, component);

				int adcIdx1 = getIdx(adcBank, 0, component);
				int adcIdx2 = getIdx(adcBank, 1, component);
				int tdcIdx1 = getIdx(tdcBank, 2, component);
				int tdcIdx2 = getIdx(tdcBank, 3, component);

				int adcL = 0;
				int adcR = 0;
				int tdcL = 0;
				int tdcR = 0;
				if (adcIdx1 != -1)
					adcL = adcBank.getInt("ADC", adcIdx1);
				if (adcIdx2 != -1)
					adcR = adcBank.getInt("ADC", adcIdx2);
				if (tdcIdx1 != -1)
					tdcL = tdcBank.getInt("TDC", tdcIdx1);
				if (tdcIdx2 != -1)
					tdcR = tdcBank.getInt("TDC", tdcIdx2);

				paddle.setAdcTdc(adcL, adcR, tdcL, tdcR);
				paddle.setPos(tx, ty, tz);
				paddle.ADC_TIMEL = adcBank.getFloat("time", adcIdx1);
				paddle.ADC_TIMER = adcBank.getFloat("time", adcIdx2);
				paddle.RECON_TIME = hitsBank.getFloat("time", hitIndex);

				// System.out.println("Louise 207");
				if (event.hasBank("CVTRec::Tracks") && event.hasBank("RUN::rf")) {

					DataBank trkBank = event.getBank("CVTRec::Tracks");
					DataBank rfBank = event.getBank("RUN::rf");

					if (event.hasBank("RUN::config")) {
						DataBank configBank = event.getBank("RUN::config");
						paddle.TRIGGER_BIT = configBank.getLong("trigger", 0);
						paddle.RUN = configBank.getInt("run", 0);
						paddle.TIMESTAMP = configBank.getLong("timestamp", 0);
					}
					
					// Get the start time, requiring that first particle in the event is an electron
					if (event.hasBank("REC::Event") && event.hasBank("REC::Particle")) {
						DataBank eventBank = event.getBank("REC::Event");
						DataBank  recPartBank = event.getBank("REC::Particle");
						if (recPartBank.getInt("pid", 0) == 11) {
							paddle.ST_TIME = eventBank.getFloat("STTime", 0);
						}
						else {
							paddle.ST_TIME = -1000.0;
						}
					}

					// get the RF time with id=1
					double trf = 0.0;
					for (int rfIdx = 0; rfIdx < rfBank.rows(); rfIdx++) {
						if (rfBank.getShort("id", rfIdx) == 1) {
							trf = rfBank.getFloat("time", rfIdx);
						}
					}
					// System.out.println("Louise 226");
					int trkId = hitsBank.getShort("trkID", hitIndex);
					// Get track
					double energy = hitsBank.getFloat("energy", hitIndex);

					// only use hit with associated track and a minimum energy
					if (trkId != -1 && energy > 0.5) {
						
						// Find the matching CVTRec::Tracks bank
						int trkIdx = -1;
						for (int i = 0; i < trkBank.rows(); i++) {
							if (trkBank.getShort("ID",i)==trkId) {
								trkIdx = i;
								break;
							}
						}
						
						// path length from bank
						paddle.PATH_LENGTH = hitsBank.getFloat("pathLength", hitIndex);
						paddle.PATH_LENGTH_BAR = hitsBank.getFloat("pathLengthThruBar", hitIndex);
						// System.out.println("Louise 237");
						paddle.RF_TIME = trf;
						
						// Get the momentum and record the beta (assuming every hit is a pion!)
						double mom = trkBank.getFloat("p", trkIdx);
						//double mass = massList[CTOFCalibration.massAss];
						//double beta = mom/Math.sqrt(mom*mom+0.139*0.139);
						//double beta = mom / Math.sqrt(mom * mom + mass * mass);
						//paddle.BETA = beta;
						paddle.P = mom;
						paddle.TRACK_ID = trkId;
						
						// For CTOF vertex z in cm:
						paddle.VERTEX_Z = trkBank.getFloat("z0", trkIdx);
						// For CTOF vertex z in mm -> convert to cm
						//paddle.VERTEX_Z = trkBank.getFloat("z0", trkIdx) / 10.0;

						paddle.CHARGE = trkBank.getByte("q", trkIdx);

						if (CTOFCalibration.maxRcs != 0.0) {
							// paddle.TRACK_REDCHI2 = trkBank.getFloat("circlefit_chi2_per_ndf", trkIdx);
							paddle.TRACK_REDCHI2 = trkBank.getFloat("chi2", trkIdx)
									/ trkBank.getShort("ndf", trkIdx);
						} else {
							paddle.TRACK_REDCHI2 = -1.0;
						}
						
						// Get the REC::Track and then the REC::Particle
						//setOutput(false);
						if (event.hasBank("REC::Particle") && event.hasBank("REC::Scintillator")) {
							
							DataBank  recScinBank = event.getBank("REC::Scintillator");
							int pIdx = -1;
							for (int i = 0; i < recScinBank.rows(); i++) {
								if (recScinBank.getShort("index",i)==hitIndex &&
										recScinBank.getByte("detector",i)==DetectorType.CTOF.getDetectorId()) {
									pIdx = recScinBank.getShort("pindex", i);
									break;
								}
							}

							DataBank  recPartBank = event.getBank("REC::Particle");
							paddle.PARTICLE_ID = recPartBank.getInt("pid", pIdx);
						}
						//setOutput(true);

					}
				}

				// System.out.println("Adding paddle to list");
				if (paddle.includeInCalib()) {
					
					paddleList.add(paddle);
					if (test) paddle.show();
				}
			}
		} else {
			// no hits bank, so just use adc and tdc

			// based on cosmic data
			// am getting entry for every PMT in ADC bank
			// ADC R two indices after ADC L (will assume right is always after left)
			// TDC bank only has actual hits, so can just search the whole bank for matching
			// SLC
			
			if (event.hasBank("CTOF::adc")) {
				DataBank adcBank = event.getBank("CTOF::adc");
		
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
						if (event.hasBank("CTOF::tdc")) {
							DataBank tdcBank = event.getBank("CTOF::tdc");
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
							paddle.ADC_TIMEL = adcTimeL;
							paddle.ADC_TIMER = adcTimeR;
	
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
