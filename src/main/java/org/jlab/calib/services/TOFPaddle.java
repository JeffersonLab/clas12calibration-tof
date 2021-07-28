package org.jlab.calib.services;

import org.jlab.calib.services.ctof.CTOFCalibration;
import org.jlab.calib.services.ctof.CTOFCalibrationEngine;
import org.jlab.calib.services.ctof.CtofHposBinEventListener;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.geom.base.ConstantProvider;
 
/**
 *
 * @author louiseclark
 */
public class TOFPaddle {

	private static final int LEFT = 0;
	private static final int RIGHT = 1;
	public static String tof = "FTOF";
	public static CalibrationConstants jitConsts = null;   
	public static CalibrationConstants lgtConsts = null;   
        public static DatabaseConstantProvider dcp = null;
        public static int currentRun = 0;

	private DetectorDescriptor desc = new DetectorDescriptor();

	public int ADCL = 0;
	public int ADCR = 0;
	public int TDCL = 0;
	public int TDCR = 0;
	public double ENERGY = 0;
	public float ADC_TIMEL = 0;
	public float ADC_TIMER = 0;
        public double LENGTH = 0;
	public double XPOS = 0.0;
	public double YPOS = 0.0;
	public double ZPOS = 0.0;
	public double PATH_LENGTH = 0.0;
	public double PATH_LENGTH_BAR = 0.0;
	public double P = 0.0;
	public int TRACK_ID = -1;
	public double VERTEX_Z = 0.0;
	public double TRACK_REDCHI2 = 0.0;
	public int CHARGE = 0;
	public double RF_TIME = 124.25;
	public double ST_TIME = -1000.0;
	public long TRIGGER_BIT = 0;
	// public double TOF_TIME = 0.0;
	public double RECON_TIME = 0.0;
        public double JITTER = 0.0;
	public int PARTICLE_ID = 0;
	public int RUN = 0;
	public long TIMESTAMP = 0;

        private final double DEDX_MIP = 1.956; // units = MeV/g/cm^2
	private final double C = 29.9792458;
	public static final double NS_PER_CH = 0.02345;
	// public static final double NS_PER_CH = 0.024;

	public TOFPaddle(int sector, int layer, int paddle) {
		this.desc.setSectorLayerComponent(sector, layer, paddle);
        }
        
        public void setRun(int run, long triggerbit, long timestamp) {
            // Get the TDC jitter parameters when runNo changes
            // Get the paddle lengths
            this.RUN = run;
            this.TRIGGER_BIT = triggerbit;
            this.TIMESTAMP = timestamp;
            
            if (RUN != currentRun && RUN!=0) {
                dcp = new DatabaseConstantProvider(RUN, "default");
                if (tof == "FTOF") {
                    jitConsts = dcp.readConstants("/calibration/ftof/time_jitter");
                    dcp.loadTable("/geometry/ftof/panel1a/paddles");        
                    dcp.loadTable("/geometry/ftof/panel1b/paddles");
                    dcp.loadTable("/geometry/ftof/panel2/paddles");
                }
                else {
                    jitConsts = dcp.readConstants("/calibration/ctof/time_jitter");
                    lgtConsts = dcp.readConstants("/geometry/ctof/ctof");
                }
                dcp.disconnect();
                currentRun = RUN;
                System.out.println("RUN = "+RUN);
            }
            
            
            if (tof == "FTOF") {
                if(dcp != null) {
                    if(this.getDescriptor().getLayer() == 1) {
                       this.LENGTH = dcp.getDouble("/geometry/ftof/panel1a/paddles/Length", this.getDescriptor().getComponent()-1);
                    }
                    else if(this.getDescriptor().getLayer() == 2) {
                       this.LENGTH = dcp.getDouble("/geometry/ftof/panel1b/paddles/Length", this.getDescriptor().getComponent()-1);
                    }
                    else if(this.getDescriptor().getLayer() == 3) {
                       this.LENGTH = dcp.getDouble("/geometry/ftof/panel2/paddles/Length", this.getDescriptor().getComponent()-1);
                    }
                }
            }
            else {
               if(lgtConsts!=null) {
                    this.LENGTH = lgtConsts.getDoubleValue("length", this.getDescriptor().getSector(),this.getDescriptor().getLayer(),this.getDescriptor().getComponent());
                }
            }
            
            if(jitConsts!=null) {
                double period = jitConsts.getDoubleValue("period", 0,0,0);
                int    phase  = jitConsts.getIntValue("phase", 0,0,0);
                int cycles = jitConsts.getIntValue("cycles", 0,0,0);
                if(cycles > 0) this.JITTER=period*((TIMESTAMP+phase)%cycles);
            }
        }

	public void setAdcTdc(int adcL, int adcR, int tdcL, int tdcR) {
		this.ADCL = adcL;
		this.ADCR = adcR;
		this.TDCL = tdcL;
		this.TDCR = tdcR; 
	}

	public void setPos(double xPos, double yPos, double zPos) {
		this.XPOS = xPos;
		this.YPOS = yPos;
		this.ZPOS = zPos;
	}

	public int paddleNumber() {

		int p = 0;
		int[] paddleOffset = { 0, 0, 23, 85 };
		int sector = this.getDescriptor().getSector();
		int layer = this.getDescriptor().getLayer();
		int component = this.getDescriptor().getComponent();

		p = component + (sector - 1) * 90 + paddleOffset[layer];
		return p;
	}

	public double geometricMean() {
		return Math.sqrt(ADCL * ADCR);			
	}
	
	public double geometricMeanNorm() {
		return geometricMean() * (thickness()/PATH_LENGTH_BAR); 
	}
	
	public double thickness() {
		double t = 0.0;
		if (tof=="FTOF") {
			t = 5.08;
			if (this.getDescriptor().getLayer() == 2) {
				t = 6.0;
			}
		}
		else {
			t = 3.0;
		}
		return t;
	}

	public double logRatio() {
		return Math.log((double) ADCR / (double) ADCL);
	}

	public boolean isValidGeoMean() {
		// return includeInCalib();
		return (this.geometricMean() > 100.0);
	}

	public boolean includeInCalib() {
		// return (ADCR != 0 || ADCL != 0);
		// return (this.geometricMean() > 100.0 && ADCR>0 && ADCL>0 && TDCL>0 &&
		// TDCR>0);
		double[] minAdc = { 0.0, 20.0, 50.0, 20.0 };
		int layer = this.getDescriptor().getLayer();
		return (ADCL > minAdc[layer] && ADCR > minAdc[layer]);
	}

	public boolean includeInTiming() {
		return (TDCL > 0 && TDCR > 0);
	}

	public boolean includeInCTOFTiming() {
		return (TDCL > 0 && TDCR > 0 && ST_TIME != -1000.0);
	}

	public boolean isValidLogRatio() {
		// only if geometric mean is over a minimum
		double[] minGM = { 0.0, 300.0, 500.0, 300.0 };
		int layer = this.getDescriptor().getLayer();

		return this.geometricMean() > minGM[layer];
	}

	public double mips() {
		double mipa = 0.0;
		if (tof == "FTOF") {
			mipa = TOFCalibrationEngine.gainValues.getDoubleValue("mipa_left", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		} 
                return mipa;
	}

        public double veff() {
		double veff = 16.0;
		if (tof == "FTOF") {
			veff = TOFCalibrationEngine.veffValues.getDoubleValue("veff_left", desc.getSector(), desc.getLayer(),
					desc.getComponent());
			// System.out.println("veff
			// "+desc.getSector()+desc.getLayer()+desc.getComponent()+" "+veff);
		} else {
			veff = CTOFCalibrationEngine.veffValues.getDoubleValue("veff_upstream", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		}

		return veff;
	}

	public double tdcConvL() {
		double tdcConv = NS_PER_CH;
		if (tof == "FTOF") {
			tdcConv = TOFCalibrationEngine.convValues.getDoubleValue("left", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		} else {
			tdcConv = CTOFCalibrationEngine.convValues.getDoubleValue("upstream", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		}

		return tdcConv;
	}	
	
	public double tdcConvR() {
		double tdcConv = NS_PER_CH;
		if (tof == "FTOF") {
			tdcConv = TOFCalibrationEngine.convValues.getDoubleValue("right", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		} else {
			tdcConv = CTOFCalibrationEngine.convValues.getDoubleValue("downstream", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		}

		return tdcConv;
	}

	public double rfpad() {
		double rfpad = 0.0;
		if (tof == "FTOF") {
			rfpad = TOFCalibrationEngine.rfpadValues.getDoubleValue("rfpad", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		} else {
			rfpad = CTOFCalibrationEngine.rfpadValues.getDoubleValue("rfpad", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		}
		return rfpad;
	}
	
	public double tw1() {
		double tw1 = 0.0;
		if (tof == "FTOF") {
			tw1 = TOFCalibrationEngine.timeWalkValues.getDoubleValue("tw1", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		}
		return tw1;
	}

	public double tw2() {
		double tw2 = 0.0;
		if (tof == "FTOF") {
			tw2 = TOFCalibrationEngine.timeWalkValues.getDoubleValue("tw2", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		}
		return tw2;
	}
	
	public double tw3() {
		double tw3 = 0.0;
		if (tof == "FTOF") {
			tw3 = TOFCalibrationEngine.timeWalkValues.getDoubleValue("tw3", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		}
		return tw3;
	}
	
	
	public double lamL() {
		double lamL = 0.0;
		if (tof == "FTOF") {
			lamL = TOFCalibrationEngine.timeWalkValues.getDoubleValue("tw0_left", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		}
		return lamL;
	}

	public double tw1pos() {
		double tw1 = 0.0;
		if (tof == "FTOF") {
			tw1 = TOFCalibrationEngine.twposValues.getDoubleValue("tw1pos", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		}
		return tw1;
	}

	public double tw2pos() {
		double tw2 = 0.0;
		if (tof == "FTOF") {
			tw2 = TOFCalibrationEngine.twposValues.getDoubleValue("tw2pos", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		}
		return tw2;
	}

	public double lamR() {
		double lamR = 0.0;
		if (tof == "FTOF") {
			lamR = TOFCalibrationEngine.timeWalkValues.getDoubleValue("tw0_right", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		}
		return lamR;
	}

	public double p2p() {
		double p2p = 0.0;
		if (tof == "FTOF") {
			p2p = TOFCalibrationEngine.p2pValues.getDoubleValue("paddle2paddle", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		} else {
			p2p = CTOFCalibrationEngine.p2pValues.getDoubleValue("paddle2paddle", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		}
		return p2p;
	}

	
	private double mass() {
		double mass = 0.0;
		double[] massList = { 0.13957, 0.938272, 0.000511 };
		if (TOFCalibration.massAss == TOFCalibration.USE_PID) {
			if (PARTICLE_ID == 2212) {
				mass = 0.938272;
			}
			else if (PARTICLE_ID == 11 || PARTICLE_ID == -11) {
				mass = 0.000511;
			}
			else if (PARTICLE_ID == 13 || PARTICLE_ID == -13) {
				mass = 0.105658;
			}
			else if (PARTICLE_ID == 211 || PARTICLE_ID == -211) {
				mass = 0.13957;
			}
		}
		else {
			mass = massList[TOFCalibration.massAss];
		}
		return mass;
	}
	
	private double beta() {
		return P/Math.sqrt(P*P+mass()*mass());
	}

	public double startTime() {
		double startTime = 0.0;
		double beta = 1.0;
		if (beta() != 0.0) {
			beta = beta();
		}

		startTime = averageHitTime() - (PATH_LENGTH / (beta * 29.98));
		return startTime;
	}
	
	public double startTimeNoTW() {
		double startTime = 0.0;

		double beta = 1.0;
		if (beta() != 0.0) {
			beta = beta();
		}

		startTime = averageHitTimeNoTW() - (PATH_LENGTH / (beta * 29.98));
		return startTime;
	}

	public double reconStartTime() {
		double startTime = 0.0;

		double beta = 1.0;
		if (beta() != 0.0) {
			beta = beta();
		}

		startTime = RECON_TIME - (PATH_LENGTH / (beta * 29.98));
		return startTime;
	}

	public double startTimeP2PCorr() {
		return startTime() + rfpad() + p2p();
	}

	public double averageHitTimeNoTW() {

		double lr = leftRightAdjustment();
		double tL = tdcToTimeL() - (lr / 2) - ((0.5 * this.LENGTH + paddleY()) / this.veff());

		double tR = tdcToTimeR() + (lr / 2) - ((0.5 * this.LENGTH - paddleY()) / this.veff());

		return (tL + tR) / 2.0;

	}
	
	public double averageHitTime() {

		double lr = leftRightAdjustment();
		double tL = timeLeftAfterTW() - (lr / 2) - ((0.5 * this.LENGTH + paddleY()) / this.veff());

		double tR = timeRightAfterTW() + (lr / 2) - ((0.5 * this.LENGTH - paddleY()) / this.veff());

		return (tL + tR) / 2.0;

	}

	public double vertexCorr() {
		if (TOFCalibration.vertexCorr == TOFCalibration.VERTEX_CORR_YES) {
			return (VERTEX_Z - TOFCalibration.targetPos) / 29.98;
		} else {
			return 0.0;
		}
	}

	public double refTime() {
		return this.RF_TIME - (this.startTime() - vertexCorr());
	}
	
	public double refTimeNoTW() {
		return this.RF_TIME - (this.startTimeNoTW() - vertexCorr());
	}	

	// ref time for CTOF - use STTime from REC::Event
	public double refSTTime() {
		return ST_TIME - (startTime() - vertexCorr());
	}

	public double refTimeCorr() {
		return refTime() - rfpad() - TWPosCorr();
	}
	
	public double refTimeRFCorr() {
		return refTime() - rfpad();
	}

	
	public double refTimeTWPosCorr() {
		return refTime() - TWPosCorr();
	}

	public double refSTTimeCorr() {
		return refSTTime() - rfpad() - HPosCorr();
	}

	public double TimeCorr() {
		return averageHitTime() + rfpad() + HPosCorr() + TWPosCorr() + p2p();
	}

    public double refSTTimeRFCorr() {
		return refSTTime() - rfpad();
	}

	public double refSTTimeHPosCorr() {
		return refSTTime() - HPosCorr();
	}

	public double refSTTimeHPosFuncCorr() {
		return refSTTime() - rfpad() - HPosCorrFunc();
	}

	public double refSTTimeHPosBinCorr() {
		return refSTTime() - rfpad() - HPosCorrBin();
	}
	
	
	public double HPosCorr() {
		if (tof=="CTOF") {
			return HPosCorrFunc() + HPosCorrBin();
		} else {
			return 0.0;
		}
	}	
	
	public double HPosCorrFunc() {
		if (tof=="CTOF") {
			//return hposA()*Math.exp(hposB()*paddleY()); // original hpos function
			//return (hposA()*paddleY()*paddleY()+hposB()*paddleY()); // New function for correction after bin level corrections
			return (hposA()*paddleY()*paddleY()+hposB()*paddleY()+hposC()); // New function for correction after bin level corrections
		} else {
			return 0.0;
		}
	}
	
	public double HPosCorrBin() {
		double val = 0.0;
		if (tof == "CTOF") {
			Double[] vals = new Double[CtofHposBinEventListener.xBins];
			int sliceNum = CtofHposBinEventListener.sliceNumber(paddleY());
			vals = CTOFCalibrationEngine.hposBinValues.getItem(desc.getSector(), desc.getLayer(), desc.getComponent());
			val = vals[sliceNum];
		}
		return val;
	}
	
	public double hposA() {
		double val = 0.0;
		if (tof == "CTOF") {
			val = CTOFCalibrationEngine.hposFuncValues.getDoubleValue("hposa", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		}
		return val;
	}

	public double hposB() {
		double val = 0.0;
		if (tof == "CTOF") {
			val = CTOFCalibrationEngine.hposFuncValues.getDoubleValue("hposb", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		}
		return val;
	}
	
	public double hposC() {
		double val = 0.0;
		if (tof == "CTOF") {
			val = CTOFCalibrationEngine.hposFuncValues.getDoubleValue("hposc", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		}
		return val;
	}	
	
	public double TWPosCorr() {
		if (tof=="FTOF") {
			return tw1pos()*paddleY()* paddleY() + tw2pos()*paddleY();
		} else {
			return 0.0;
		}

	}
	
//	private double TWCorrL() {
//		
//		double tw0Corr = lamL() / Math.pow(ADCL, 0.5);
//
//		return tw0Corr;
//
//	}
//
//	private double TWCorrR() {
//		
//		double tw0Corr = lamR() / Math.pow(ADCR, 0.5);
//
//		return tw0Corr;
//
//	}
	
	public double timeLeftAfterTW() {
		if (tof == "FTOF") {
			// return tdcToTime(TDCL) - (lamL() / Math.pow(ADCL, 0.5));
			return tdcToTimeL() - TWCorr();
		} else {
			return tdcToTimeL();
		}
	}

	public double timeRightAfterTW() {
		if (tof == "FTOF") {
			// return tdcToTime(TDCR) - (lamR() / Math.pow(ADCR, 0.5));
			return tdcToTimeR() - TWCorr();
		} else {
			return tdcToTimeR();
		}
	}	
	
	// LC Sep 19
	private double TWCorr() {
		
		double twCorr = tw1()*Math.exp(tw2()*energy()) + tw3()/energy();

		return twCorr;

	}
	
	// LC Sep 19
	public double deltaTTW(double offset) {

		//double dtL = tdcToTimeL(TDCL) - (lr / 2) + rfpad() - ((0.5 * paddleLength() + paddleY()) / this.veff())
		//		- (PATH_LENGTH / (beta * 29.98)) - vertexCorr() - this.RF_TIME;
		
		double dtL = -refTimeNoTW();

		// subtract the correction based on previous calibration values
		dtL = dtL - TWCorr();
		dtL = dtL + offset;
		double bb = TOFCalibrationEngine.BEAM_BUCKET;
		dtL = (dtL + (1000 * bb) + (0.5 * bb)) % bb - 0.5 * bb;

		return dtL;
	}

//	public double deltaTLeft(double offset) {
//
//		double lr = leftRightAdjustment();
//
//		double beta = 1.0;
//		if (beta() != 0.0) {
//			beta = beta();
//		}
//
//		double dtL = tdcToTimeL(TDCL) - (lr / 2) + rfpad() - ((0.5 * paddleLength() + paddleY()) / this.veff())
//				- (PATH_LENGTH / (beta * 29.98)) - vertexCorr() - this.RF_TIME;
//		
//		// subtract the correction based on previous calibration values
//		dtL = dtL - TWCorrL();
//		dtL = dtL + offset;
//		double bb = TOFCalibrationEngine.BEAM_BUCKET;
//		dtL = (dtL + (1000 * bb) + (0.5 * bb)) % bb - 0.5 * bb;
//
//		return dtL;
//	}
//
//	public double deltaTRight(double offset) {
//
//		double lr = leftRightAdjustment();
//
//		double beta = 1.0;
//		if (beta() != 0.0) {
//			beta = beta();
//		}
//
//		double dtR = tdcToTimeR(TDCR) + (lr / 2) + rfpad() - ((0.5 * paddleLength() - paddleY()) / this.veff())
//				- (PATH_LENGTH / (beta * 29.98)) - vertexCorr() - this.RF_TIME;
//
//		// subtract the correction based on previous calibration values
//		dtR = dtR - TWCorrR();
//		dtR = dtR + offset;
//		double bb = TOFCalibrationEngine.BEAM_BUCKET;
//		dtR = (dtR + (1000 * bb) + (0.5 * bb)) % bb - 0.5 * bb;
//
//		return dtR;
//	}
	
	public double ctofCenter() {
		double center = 0.0;
		int paddle = this.getDescriptor().getComponent();
		if (paddle%2==0) {
			center = -8.5031 + TOFCalibration.targetPos;
		}
		else {
			center = -8.9874 + TOFCalibration.targetPos;
		}
		return center;
	}
	
//	public double paddleLength() {
//
//		double len = 0.0;
//		int paddle = this.getDescriptor().getComponent();
//
//		if (tof == "FTOF") {
//			int layer = this.getDescriptor().getLayer();
//			
//			if (layer == 1 && paddle <= 5) {
//				len = (15.85 * paddle) + 16.43;
//			} else if (layer == 1 && paddle > 5) {
//				len = (15.85 * paddle) + 11.45;
//			} else if (layer == 2) {
//				len = (6.4 * paddle) + 10.84;
//			} else if (layer == 3) {
//				len = (13.73 * paddle) + 357.55;
//			}
//		} else {
//			if (paddle%2==0) {
//				len = 88.0467;
//			}
//			else {
//				len = 88.9328;
//			}
//		}
//
//		return len;
//
//	}

        public double energy() {
            double energyCalc = ENERGY;
            if(mips()!=0) {
                double AdcToEConv = mips() / (DEDX_MIP * thickness());
 
                double edepLeft  = ADCL / AdcToEConv;
                double edepRight = ADCR / AdcToEConv;
                
                energyCalc = Math.sqrt(edepLeft * edepRight);
            }
            return energyCalc;
        }
        
	public double leftRight() {
		return (timeLeftAfterTW() - timeRightAfterTW());
	}

	public boolean isValidLeftRight() {
		return (tdcToTimeL() != tdcToTimeR());
	}

	public double tdcToTimeL() {
		return tdcConvL() * TDCL  - this.JITTER;
	}

	public double tdcToTimeR() {
		return tdcConvR() * TDCR  - this.JITTER;
	}
	
	public double veffHalfTimeDiff() {

		double timeL = timeLeftAfterTW();
		double timeR = timeRightAfterTW();
		return (timeL - timeR) / 2;
	}

	public double halfTimeDiff() {

		double timeL = timeLeftAfterTW();
		double timeR = timeRightAfterTW();
		return (timeL - timeR - leftRightAdjustment()) / 2;
	}

	public double leftRightAdjustment() {

		double lr = 0.0;

		if (tof == "FTOF") {
			lr = TOFCalibrationEngine.leftRightValues.getDoubleValue("left_right", desc.getSector(), desc.getLayer(),
					desc.getComponent());

		} else {
			lr = CTOFCalibrationEngine.leftRightValues.getDoubleValue("upstream_downstream", desc.getSector(),
					desc.getLayer(), desc.getComponent());
		}

		return lr;
	}

	public double position() {

		double pos = halfTimeDiff() * veff();
		// System.out.println("position "+pos);
		// System.out.println("veff "+veff());
                if (tof == "CTOF") {
                    pos = pos + ctofCenter();
                }
		return pos;
                }

	public double paddleY() {

		double y = 0.0;
		if (tof == "FTOF") {
			int sector = desc.getSector();
			double rotation = Math.toRadians((sector - 1) * 60);
			y = YPOS * Math.cos(rotation) - XPOS * Math.sin(rotation);
		} else {
			y = zPosCTOF();
		}
		return y;
	}

	public boolean trackFound() {
		return TRACK_ID != -1;
	};

	public boolean goodTrackFound() {

		double maxRcs = 100.0;
		double minV = -10.0;
		double maxV = 10.0;
		double minP = 0.0;
		double maxP = 9.0;
		if (tof == "FTOF") {
			if (getDescriptor().getLayer() == 3) {
				maxRcs = TOFCalibration.maxRcs2;
			}
			else {
				maxRcs = TOFCalibration.maxRcs;
			}
			minV = TOFCalibration.minV;
			maxV = TOFCalibration.maxV;
			minP = TOFCalibration.minP;
			maxP = TOFCalibration.maxP;
		} else {
			maxRcs = CTOFCalibration.maxRcs;
			minV = CTOFCalibration.minV;
			maxV = CTOFCalibration.maxV;
			minP = CTOFCalibration.minP;
			maxP = CTOFCalibration.maxP;
		}

		return (trackFound() && TRACK_REDCHI2 < maxRcs && VERTEX_Z > minV && VERTEX_Z < maxV 
				&& P > minP && P < maxP
				&& chargeMatch()
				&& pidMatch()
				&& mass() != 0.0);
	}
	
	public boolean pidMatch() {
		
		int selectedPid = 0;
		if (getDescriptor().getLayer() == 3) {
			selectedPid = TOFCalibration.trackPid2;
		}
		else {
			selectedPid = TOFCalibration.trackPid;
		}

		return (selectedPid==TOFCalibration.PID_ALL) ||
					(selectedPid==TOFCalibration.PID_L && (PARTICLE_ID==13 || PARTICLE_ID==-13)) ||
					(selectedPid==TOFCalibration.PID_L && (PARTICLE_ID==11 || PARTICLE_ID==-11)) ||
					(selectedPid==TOFCalibration.PID_PI && (PARTICLE_ID==211 || PARTICLE_ID==-211)) ||
					(selectedPid==TOFCalibration.PID_P && (PARTICLE_ID==2212)) ||
					(selectedPid==TOFCalibration.PID_LPI && (PARTICLE_ID==13 || PARTICLE_ID==-13)) ||
					(selectedPid==TOFCalibration.PID_LPI && (PARTICLE_ID==11 || PARTICLE_ID==-11)) ||
					(selectedPid==TOFCalibration.PID_LPI && (PARTICLE_ID==211 || PARTICLE_ID==-211));
					
	}

	public boolean chargeMatch() {

		int trackCharge = 0;
		if (tof == "FTOF") {
			trackCharge = TOFCalibration.trackCharge;
		} else {
			trackCharge = CTOFCalibration.trackCharge;
		}

		return (trackCharge == TOFCalibration.TRACK_BOTH || trackCharge == TOFCalibration.TRACK_NEG && CHARGE == -1
				|| trackCharge == TOFCalibration.TRACK_POS && CHARGE == 1);
	}

	public double zPosCTOF() {
		return ZPOS;
	}

	public DetectorDescriptor getDescriptor() {
		return this.desc;
	}

	public String toString() {
		return "S " + desc.getSector() + " L " + desc.getLayer() + " C " + desc.getComponent() + " ADCR " + ADCR
				+ " ADCL " + ADCL + " TDCR " + TDCR + " TDCL " + TDCL + " Geometric Mean " + geometricMean()
				+ " Log ratio " + logRatio();
	}

	public void show() {
		System.out.println("");
		System.out.println("S " + desc.getSector() + " L " + desc.getLayer() + " C " + desc.getComponent() + " ADCR "
				+ ADCR + " ADCL " + ADCL + " TDCR " + TDCR + " TDCL " + TDCL);
		System.out.println("geometricMean "+geometricMean()+ " PATH_LENGTH_BAR " + PATH_LENGTH_BAR);
		System.out.println("XPOS " + XPOS + " YPOS " + YPOS + " ZPOS " + ZPOS + " PATH_LENGTH " + PATH_LENGTH 
				+ " TRACK_ID " + TRACK_ID);
		System.out.println("PARTICLE_ID "+PARTICLE_ID+" mass "+mass()+" beta " + beta() + " P " + P + " RF_TIME " + RF_TIME + "ST_TIME " + ST_TIME + " RECON_TIME "
				+ RECON_TIME + "ENERGY " + ENERGY);
		System.out.println("VERTEX_Z " + VERTEX_Z + " TRACK_REDCHI2 " + TRACK_REDCHI2 + " CHARGE " + CHARGE
				+ " TRIGGER_BIT " + TRIGGER_BIT);
		System.out.println("goodTrackFound " + goodTrackFound() + " chargeMatch " + chargeMatch() + " pidMatch "+pidMatch());
		System.out.println("refTime " + refTime() + " startTime " + startTime() + " averageHitTime "
				+ averageHitTime() + " vertexCorr " + vertexCorr());
		System.out.println("refSTTime " + refSTTime() + " refSTTimeRFCorr "+ refSTTimeRFCorr() + " refSTTimeHPosCorr "+ refSTTimeHPosCorr() + " refSTTimeCorr "+ refSTTimeCorr());
		System.out.println("hposA " + hposA() + "hposB " + hposB());
		System.out.println("startTimeP2PCorr " + startTimeP2PCorr());
		System.out.println("rfpad " + rfpad() + " p2p " + p2p() + " lamL " + lamL() + " tw1pos " + tw1pos() + " tw2pos "
				+ tw2pos() + " lamR " + lamR() + " LR " + leftRightAdjustment()
				+ " veff " + veff() + " tdcConvL "+ tdcConvL() + " tdcConvR "+ tdcConvR());
		System.out.println("paddleLength " + LENGTH + " paddleY " + paddleY() + "ctofCenter "+ctofCenter());
		System.out.println("timeLeftAfterTW " + timeLeftAfterTW() + " timeRightAfterTW " + timeRightAfterTW());
		System.out.println("deltaTW " + this.deltaTTW(0.0));

	}

}