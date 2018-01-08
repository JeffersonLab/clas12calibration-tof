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

	private static	boolean test = false;
//	public static CodaEventDecoder codaDecoder;
//	public static DetectorEventDecoder eventDecoder;
//	public static List<DetectorDataDgtz> detectorData;

//	public static void init() {
//
//		codaDecoder = new CodaEventDecoder();
//		eventDecoder = new DetectorEventDecoder();
//		detectorData = new ArrayList<DetectorDataDgtz>();
//	}

	public static List<TOFPaddle> getPaddleList(DataEvent event) {

		List<TOFPaddle>  paddleList = new ArrayList<TOFPaddle>();

		paddleList = getPaddleListHipo(event);
		//paddleList = getPaddleListDgtzNew(event);

		return paddleList;

	}
	
	private static int getIdx(DataBank bank, int hitOrder, int hitComp) {
		
		int idx = -1;
		for (int i=0; i<bank.rows(); i++) {
			int component = bank.getShort("component", i);
			int order = bank.getByte("order", i);
			if (component==hitComp && order==hitOrder) {
				idx = i;
				break;
			}
		}
		return idx;
	}

	public static List<TOFPaddle> getPaddleListHipo(DataEvent event){

		if (test) {

			event.show();
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
		}

		ArrayList<TOFPaddle>  paddleList = new ArrayList<TOFPaddle>();
		//System.out.println("Louise 118");
		// Set the status flags
		if (event.hasBank("CTOF::adc")) {
			//System.out.println("Louise 121");
			DataBank adcBank = event.getBank("CTOF::adc");
			
			for (int i = 0; i < adcBank.rows(); i++) {
				int component = adcBank.getShort("component", i);
				int order = adcBank.getByte("order", i);
				int adc = adcBank.getInt("ADC", i);
				if (order==0 && adc != 0) {
					CTOFCalibrationEngine.adcLeftStatus.add(0, 1,1,component);
				}
				if (order==1 && adc != 0) {
					CTOFCalibrationEngine.adcRightStatus.add(0, 1,1,component);
				}
			}
			//System.out.println("Louise 135");
		}
		if (event.hasBank("CTOF::tdc")) {
			//System.out.println("Louise 138");
			DataBank tdcBank = event.getBank("CTOF::tdc");
			
			for (int i = 0; i < tdcBank.rows(); i++) {
				int component = tdcBank.getShort("component", i);
				int order = tdcBank.getByte("order", i);
				int tdc = tdcBank.getInt("TDC", i);
				if (order==2 && tdc != 0) {
					CTOFCalibrationEngine.tdcLeftStatus.add(0, 1,1,component);
				}
				if (order==3 && tdc != 0) {
					CTOFCalibrationEngine.tdcRightStatus.add(0, 1,1,component);
				}
			}
			//System.out.println("Louise 152");
		}
				

		// Only continue if we have adc and tdc banks
		//if (!event.hasBank("CTOF::adc") || !event.hasBank("CTOF::tdc")) {
		//	return paddleList;
		//}

		DataBank  adcBank = event.getBank("CTOF::adc");
		DataBank  tdcBank = event.getBank("CTOF::tdc");
		

		// iterate through hits bank getting corresponding adc and tdc
		if (event.hasBank("CTOF::hits")) {
			//System.out.println("Louise 167");
			DataBank  hitsBank = event.getBank("CTOF::hits");

			for (int hitIndex=0; hitIndex<hitsBank.rows(); hitIndex++) {

				double tx     = hitsBank.getFloat("tx", hitIndex);
				double ty     = hitsBank.getFloat("ty", hitIndex);
				double tz     = hitsBank.getFloat("tz", hitIndex);

				int component = (int) hitsBank.getShort("component", hitIndex);
				TOFPaddle  paddle = new TOFPaddle(1,1,
						component);

				int adcIdx1 = getIdx(adcBank,0, component);
				int adcIdx2 = getIdx(adcBank,1, component);
				int tdcIdx1 = getIdx(tdcBank,2, component);
				int tdcIdx2 = getIdx(tdcBank,3, component);
				
//				System.out.println("Paddle "+component);
//				System.out.println("tx ty tz"+tx+" "+ty+" "+tz);
//				System.out.println("ADC L idx "+adcIdx1);
//				System.out.println("ADC R idx "+adcIdx2);
//				System.out.println("TDC L idx "+tdcIdx1);
//				System.out.println("TDC R idx "+tdcIdx2);
				
				paddle.setAdcTdc(
						adcBank.getInt("ADC", adcIdx1),
						adcBank.getInt("ADC", adcIdx2),
						tdcBank.getInt("TDC", tdcIdx1),
						tdcBank.getInt("TDC", tdcIdx2));
//						adcBank.getInt("ADC", hitsBank.getShort("adc_idx1", hitIndex)),
//						adcBank.getInt("ADC", hitsBank.getShort("adc_idx2", hitIndex)),
//						tdcBank.getInt("TDC", hitsBank.getShort("tdc_idx1", hitIndex)),
//						tdcBank.getInt("TDC", hitsBank.getShort("tdc_idx2", hitIndex)));
				paddle.setPos(tx,ty,tz); 
				paddle.ADC_TIMEL = adcBank.getFloat("time", adcIdx1);
				paddle.ADC_TIMER = adcBank.getFloat("time", adcIdx2);
				paddle.RECON_TIME = hitsBank.getFloat("time", hitIndex);
								
				//paddle.show();
				//System.out.println("Louise 207");
				if (event.hasBank("CVTRec::Tracks") && event.hasBank("RUN::rf")) {

					DataBank  trkBank = event.getBank("CVTRec::Tracks");
					DataBank  rfBank = event.getBank("RUN::rf");
					
					if (event.hasBank("RUN::config")) {
						DataBank  configBank = event.getBank("RUN::config");
						paddle.TRIGGER_BIT = configBank.getLong("trigger", 0);
					}
					
					// get the RF time with id=1
					double trf = 0.0; 
					for (int rfIdx=0; rfIdx<rfBank.rows(); rfIdx++) {
						if (rfBank.getShort("id",rfIdx)==1) {
							trf = rfBank.getFloat("time",rfIdx);
						}
					}

					// Get track
					int trkId = hitsBank.getShort("trkID", hitIndex);
					double energy = hitsBank.getFloat("energy", hitIndex);
					
					// only use hit with associated track and a minimum energy
					if (trkId!=-1 && energy>1.5) {
						
						double c3x  = trkBank.getFloat("c_x",trkId);
						double c3y  = trkBank.getFloat("c_y",trkId);
						double c3z  = trkBank.getFloat("c_z",trkId);
						double path = trkBank.getFloat("pathlength",trkId) + Math.sqrt((tx-c3x)*(tx-c3x)+(ty-c3y)*(ty-c3y)+(tz-c3z)*(tz-c3z));
						// calculated path length
						//paddle.PATH_LENGTH = path;
						
						// path length from bank
						paddle.PATH_LENGTH = hitsBank.getFloat("pathLength", hitIndex);
						//System.out.println("Path length calc "+path);
						//System.out.println("Path length bank "+hitsBank.getFloat("pathLength", hitIndex));
						//double diff = path - hitsBank.getFloat("pathLength", hitIndex);
						//System.out.println("Path length diff "+diff);
						paddle.RF_TIME = trf;
						
						// Get the momentum and record the beta (assuming every hit is a pion!)
//						double px  = tbtBank.getFloat("p0_x",trkId-1);
//						double py  = tbtBank.getFloat("p0_y",trkId-1);
//						double pz  = tbtBank.getFloat("p0_z",trkId-1);
//						double mom = Math.sqrt(px*px + py*py + pz*pz);
						double mom = trkBank.getFloat("p", trkId);
						double beta = mom/Math.sqrt(mom*mom+0.139*0.139);
						paddle.BETA = beta;
						paddle.P = mom;
						paddle.TRACK_ID = trkId;
						paddle.VERTEX_Z = trkBank.getFloat("z0", trkId);
						paddle.CHARGE = trkBank.getInt("q", trkId);
						
						if (CTOFCalibration.maxRcs != 0.0) {
							paddle.TRACK_REDCHI2 = trkBank.getFloat("circlefit_chi2_per_ndf", trkId);
						}
						else {
							paddle.TRACK_REDCHI2 = -1.0;
						}
						
					}
					//System.out.println("Louise 269");
				}

//				paddle.show();
//				System.out.println("Adding paddle to list");
				if (paddle.includeInCalib()) {
					//System.out.println("Louise 275");
					paddleList.add(paddle);
					//System.out.println("Louise 277");
				}
			}
		}
		else {
			//System.out.println("Louise 280");
			// no hits bank, so just use adc and tdc

			// based on cosmic data
			// am getting entry for every PMT in ADC bank
			// ADC R two indices after ADC L (will assume right is always after left)
			// TDC bank only has actual hits, so can just search the whole bank for matching SLC

			for (int i = 0; i < adcBank.rows(); i++) {
				int order = adcBank.getByte("order", i);
				int adc = adcBank.getInt("ADC", i);
				if (order==0 && adc != 0) {

					int component = adcBank.getShort("component", i);
					int adcL = adc;
					int adcR = 0;
					float adcTimeL = adcBank.getFloat("time", i);
					float adcTimeR = 0;
					int tdcL = 0;
					int tdcR = 0;

					for (int j=0; j < adcBank.rows(); j++) {
						int c = adcBank.getShort("component", j);
						int o = adcBank.getByte("order", j);
						if (c==component && o == 1) {
							// matching adc R
							adcR = adcBank.getInt("ADC", j);
							adcTimeR = adcBank.getFloat("time", j);
							break;
						}
					}

					// Now get matching TDCs
					// can search whole bank as it has fewer rows (only hits)
					// break when you find so always take the first one found
					for (int tdci=0; tdci < tdcBank.rows(); tdci++) {
						int c = tdcBank.getShort("component", tdci);
						int o = tdcBank.getByte("order", tdci);
						if (c==component && o == 2) {
							// matching tdc L
							tdcL = tdcBank.getInt("TDC", tdci);
							break;
						}
					}
					for (int tdci=0; tdci < tdcBank.rows(); tdci++) {
						int c = tdcBank.getShort("component", tdci);
						int o = tdcBank.getByte("order", tdci);
						if (c==component && o == 3) {
							// matching tdc R
							tdcR = tdcBank.getInt("TDC", tdci);
							break;
						}
					}
					
					// set status to ok if at least one reading
	                if (adcL !=0) {
	                    CTOFCalibrationEngine.adcLeftStatus.add(0, 1,1,component);
	                }
	                if (adcR !=0) {
	                    CTOFCalibrationEngine.adcRightStatus.add(0, 1,1,component);
	                }
	                if (tdcL !=0) {
	                    CTOFCalibrationEngine.tdcLeftStatus.add(0, 1,1,component);
	                }
	                if (tdcR !=0) {
	                    CTOFCalibrationEngine.tdcRightStatus.add(0, 1,1,component);
	                }					

					if (test) {
						System.out.println("Values found "+component);
						System.out.println(adcL+" "+adcR+" "+tdcL+" "+tdcR);
					}

					if (adcL>100 && adcR>100) {

						TOFPaddle  paddle = new TOFPaddle(1,1,component);
						paddle.setAdcTdc(adcL, adcR, tdcL, tdcR);
						paddle.ADC_TIMEL = adcTimeL;
						paddle.ADC_TIMER = adcTimeR;

						//if (paddle.includeInCalib()) {

							if (test) {
								System.out.println("Adding paddle "+component);
								System.out.println(adcL + " "+adcR+" "+tdcL+" "+tdcR);
							}
							paddleList.add(paddle);							
						//}
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
