package org.jlab.calib.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jlab.calib.services.ctof.CTOFCalibration;
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
import org.jlab.groot.fitter.DataFitter;
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

//		if (event.hasBank("TimeBasedTrkg::TBTracks")) {
//			DataBank testBank = event.getBank("TimeBasedTrkg::TBTracks");
//			for (int tbtIdx=0; tbtIdx<testBank.rows(); tbtIdx++) {
//				// fill test hist
//				TOFCalibration.trackRCS.fill(testBank.getFloat("chi2", tbtIdx)/testBank.getShort("ndf", tbtIdx));
//				TOFCalibration.trackRCS2.fill(testBank.getFloat("chi2", tbtIdx)/testBank.getShort("ndf", tbtIdx));
//				TOFCalibration.vertexHist.fill(testBank.getFloat("Vtx0_z", tbtIdx));
//				
//			}
//		}

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

                                double tx     = hitsBank.getFloat("tx", hitIndex);
				double ty     = hitsBank.getFloat("ty", hitIndex);
				double tz     = hitsBank.getFloat("tz", hitIndex);
				paddle.setPos(tx,ty,tz); 
//				paddle.ADC_TIMEL = adcBank.getFloat("time", hitsBank.getShort("adc_idx1", hitIndex));
//				paddle.ADC_TIMER = adcBank.getFloat("time", hitsBank.getShort("adc_idx2", hitIndex));
						paddle.setRECON_TIME(hitsBank.getFloat("time", hitIndex));
						paddle.setENERGY(hitsBank.getFloat("energy", hitIndex));
						
				//System.out.println("Paddle created "+paddle.getDescriptor().getSector()+paddle.getDescriptor().getLayer()+paddle.getDescriptor().getComponent());

						paddle.setPATH_LENGTH(hitsBank.getFloat("pathLength", hitIndex));
						paddle.setPATH_LENGTH_BAR(hitsBank.getFloat("pathLengthThruBar", hitIndex));
						paddle.setRF_TIME(trf);
						
						// Get the momentum and record the beta using the mass assumption
//						double px  = tbtBank.getFloat("p0_x",trkId-1);
//						double py  = tbtBank.getFloat("p0_y",trkId-1);
//						double pz  = tbtBank.getFloat("p0_z",trkId-1);
//						double mom = Math.sqrt(px*px + py*py + pz*pz);
//						double mass = massList[TOFCalibration.massAss];
//						double beta = mom/Math.sqrt(mom*mom+mass*mass);
//						paddle.BETA = beta;
						paddle.setP(hitsBank.getFloat("p", hitIndex));
						paddle.setTRACK_ID(hitsBank.getInt("trackid", hitIndex));
						paddle.setVERTEX_Z(hitsBank.getFloat("vz", hitIndex));
						paddle.setPARTICLE_ID(hitsBank.getInt("pid", hitIndex));
						paddle.setCHARGE(hitsBank.getByte("charge", hitIndex));
						
						if (TOFCalibration.maxRcs != 0.0) {
							paddle.setTRACK_REDCHI2(hitsBank.getFloat("chi2", hitIndex)/hitsBank.getShort("NDF", hitIndex));
						}
						
//						if (paddle.getDescriptor().getComponent()==13 &&
//								paddle.getDescriptor().getLayer()== 1 && trkId !=-1) {
//							//refPaddleFound = true;
//						}
//						if (paddle.getDescriptor().getComponent()==35 &&
//								paddle.getDescriptor().getLayer()== 2 && trkId !=-1) {
//							//testPaddleFound = true;
//						}

						// check if it's an electron by matching to the generated particle
//						int    q    = tbtBank.getByte("q",trkId-1);
//						double p0x  = tbtBank.getFloat("p0_x",trkId-1);
//						double p0y  = tbtBank.getFloat("p0_y",trkId-1);
//						double p0z  = tbtBank.getFloat("p0_z",trkId-1);
//						Particle recParticle = new Particle(11,p0x,p0y,p0z,0,0,0);

//						System.out.println("q "+q);
//						System.out.println("recParticle.p() "+recParticle.p());
//						System.out.println("electronGen.p() "+electronGen.p());
						// select negative tracks matching the generated electron as electron candidates
//						if(q==-1
//								&& Math.abs(recParticle.p()-electronGen.p())<0.5
//								&& Math.abs(Math.toDegrees(recParticle.theta()-electronGen.theta()))<2.0
//								&& Math.abs(Math.toDegrees(recParticle.phi()-electronGen.phi()))<8) {
//							paddle.PARTICLE_ID = TOFPaddle.PID_ELECTRON;
//						} 
//						else {
//							paddle.PARTICLE_ID = TOFPaddle.PID_PION;
//						}
						
						// Get the REC::Track and then the REC::Particle
//						setOutput(false);
//						if (event.hasBank("REC::Particle") && event.hasBank("REC::Track") && event.hasBank("REC::Scintillator")) {
//							
//							DataBank  recTrkBank = event.getBank("REC::Track");
//							DataBank  recSciBank = event.getBank("REC::Scintillator");
//							int pIdx = -1;
//							for (int i = 0; i < recTrkBank.rows(); i++) {
//								if (recTrkBank.getShort("index",i)==trkId-1) {
//									pIdx = recTrkBank.getShort("pindex", i);
//									break;
//								}
//							}
//							for (int i = 0; i < recSciBank.rows(); i++) {
//								if (recSciBank.getShort("pindex",i)==pIdx && recSciBank.getByte("layer", i)==paddle.getDescriptor().getLayer()) {
//									paddle.PATH_LENGTH = recSciBank.getFloat("path", i);
//									break;
//								}
//							}
//							
//							DataBank  recPartBank = event.getBank("REC::Particle");
//							paddle.PARTICLE_ID = recPartBank.getInt("pid", pIdx);
//						}
//						setOutput(true);
							
//					}
//				}
				if (paddle.includeInCalib()) {
					paddle.Init();
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
						paddle.Init();

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
