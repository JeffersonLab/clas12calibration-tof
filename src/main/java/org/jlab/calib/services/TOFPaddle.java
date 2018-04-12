package org.jlab.calib.services;

import org.jlab.calib.services.ctof.CTOFCalibration;
import org.jlab.calib.services.ctof.CTOFCalibrationEngine;
import org.jlab.detector.base.DetectorDescriptor;

/**
 *
 * @author louiseclark
 */
public class TOFPaddle {

	private static final int LEFT = 0;
	private static final int RIGHT = 1;
	public static String tof = "FTOF";

	private DetectorDescriptor desc = new DetectorDescriptor();

	public int ADCL = 0;
	public int ADCR = 0;
	public int TDCL = 0;
	public int TDCR = 0;
	public float ADC_TIMEL = 0;
	public float ADC_TIMER = 0;
	public double XPOS = 0.0;
	public double YPOS = 0.0;
	public double ZPOS = 0.0;
	public double PATH_LENGTH = 0.0;
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
	public int PARTICLE_ID = 0;

	private final double C = 29.98;
	public static final double NS_PER_CH = 0.02345;
	// public static final double NS_PER_CH = 0.024;

	public TOFPaddle(int sector, int layer, int paddle) {
		this.desc.setSectorLayerComponent(sector, layer, paddle);
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
	
	public double lamL() {
		double lamL = 0.0;
		if (tof == "FTOF") {
			lamL = TOFCalibrationEngine.timeWalkValues.getDoubleValue("tw0_left", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		}
		return lamL;
	}

	public double tw1L() {
		double tw1L = 0.0;
		if (tof == "FTOF") {
			tw1L = TOFCalibrationEngine.twposValues.getDoubleValue("tw1_left", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		}
		return tw1L;
	}

	public double tw2L() {
		double tw2L = 0.0;
		if (tof == "FTOF") {
			tw2L = TOFCalibrationEngine.twposValues.getDoubleValue("tw2_left", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		}
		return tw2L;
	}

	public double lamR() {
		double lamR = 0.0;
		if (tof == "FTOF") {
			lamR = TOFCalibrationEngine.timeWalkValues.getDoubleValue("tw0_right", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		}
		return lamR;
	}

	public double tw1R() {
		double tw1R = 0.0;
		if (tof == "FTOF") {
			tw1R = TOFCalibrationEngine.twposValues.getDoubleValue("tw1_right", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		}
		return tw1R;
	}

	public double tw2R() {
		double tw2R = 0.0;
		if (tof == "FTOF") {
			tw2R = TOFCalibrationEngine.twposValues.getDoubleValue("tw2_right", desc.getSector(), desc.getLayer(),
					desc.getComponent());
		}
		return tw2R;
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

	public double reconStartTime() {
		double startTime = 0.0;

		double beta = 1.0;
		if (beta() != 0.0) {
			beta = beta();
		}

		startTime = RECON_TIME - (PATH_LENGTH / (beta * 29.98));
		return startTime;
	}

	public double startTimeRFCorr() {
		return startTime() + rfpad();
	}

	public double startTimeP2PCorr() {
		return startTime() + rfpad() + p2p();
	}

	public double averageHitTime() {

		double lr = leftRightAdjustment();
		double tL = timeLeftAfterTW() - (lr / 2) - ((0.5 * paddleLength() + paddleY()) / this.veff());

		double tR = timeRightAfterTW() + (lr / 2) - ((0.5 * paddleLength() - paddleY()) / this.veff());

		return (tL + tR) / 2.0;

	}

	public double tofTimeRFCorr() {
		return averageHitTime() + rfpad();
	}

	public double vertexCorr() {
		if (TOFCalibration.vertexCorr == TOFCalibration.VERTEX_CORR_YES) {
			return VERTEX_Z / 29.98;
		} else {
			return 0.0;
		}
	}

	public double refTime() {
		return this.RF_TIME - (this.startTime() - vertexCorr());
	}

	// ref time for CTOF - use STTime from REC::Event
	public double refSTTime() {
		return ST_TIME - startTime();
	}

	public double refTimeCorr() {
		return refTime() - rfpad();
	}
	
	public double refSTTimeCorr() {
		return refSTTime() - rfpad();
	}

	private double TWCorrL() {
		
		double tw0Corr = lamL() / Math.pow(ADCL, 0.5);
		double twposCorr = tw1L()*paddleY()* paddleY() + tw2L()*paddleY();

		return tw0Corr+twposCorr;

	}

	private double TWCorrR() {
		
		double tw0Corr = lamR() / Math.pow(ADCR, 0.5);
		double twposCorr = tw1R()*paddleY()*paddleY() + tw2R()*paddleY();

		return tw0Corr+twposCorr;

	}
	
	public double timeLeftAfterTW() {
		if (tof == "FTOF") {
			// return tdcToTime(TDCL) - (lamL() / Math.pow(ADCL, 0.5));
			return tdcToTime(TDCL) - TWCorrL();
		} else {
			return tdcToTime(TDCL);
		}
	}

	public double timeRightAfterTW() {
		if (tof == "FTOF") {
			// return tdcToTime(TDCR) - (lamR() / Math.pow(ADCR, 0.5));
			return tdcToTime(TDCR) - TWCorrR();
		} else {
			return tdcToTime(TDCR);
		}
	}	

	public double deltaTLeft(double offset) {

		double lr = leftRightAdjustment();

		double beta = 1.0;
		if (beta() != 0.0) {
			beta = beta();
		}

		double dtL = tdcToTime(TDCL) - (lr / 2) + rfpad() - ((0.5 * paddleLength() + paddleY()) / this.veff())
				- (PATH_LENGTH / (beta * 29.98)) - vertexCorr() - this.RF_TIME;

		// subtract the correction based on previous calibration values
		dtL = dtL - TWCorrL();
		dtL = dtL + offset;
		double bb = TOFCalibrationEngine.BEAM_BUCKET;
		dtL = (dtL + (1000 * bb) + (0.5 * bb)) % bb - 0.5 * bb;

		return dtL;
	}
	
	public double deltaTLeftTest(double offset) {
		
		boolean show=false;
		if (this.XPOS==280.42010498046875) show=true;

		double lr = leftRightAdjustment();

		double beta = 1.0;
		if (beta() != 0.0) {
			beta = beta();
		}

		double dtL = tdcToTime(TDCL) - (lr / 2) + rfpad() - ((0.5 * paddleLength() + paddleY()) / this.veff())
				- (PATH_LENGTH / (beta * 29.98)) - vertexCorr() - this.RF_TIME;

		if (show) {
			System.out.println("tdcToTime(TDCL) "+tdcToTime(TDCL));
			System.out.println("(lr / 2) "+(lr / 2));
			System.out.println("rfpad() "+rfpad());
			System.out.println("((0.5 * paddleLength() + paddleY()) / this.veff()) "+((0.5 * paddleLength() + paddleY()) / this.veff()));
			System.out.println("(PATH_LENGTH / (beta * 29.98)) "+(PATH_LENGTH / (beta * 29.98)));
			System.out.println("dtL "+dtL);
			
		}
		
		// subtract the correction based on previous calibration values
		dtL = dtL - TWCorrL();
		if (show) {
			System.out.println("dtL2 "+dtL);
		}
		dtL = dtL + offset;
		double bb = TOFCalibrationEngine.BEAM_BUCKET;
		dtL = (dtL + (1000 * bb) + (0.5 * bb)) % bb - 0.5 * bb;

		if (show) {
			System.out.println("dtL3 "+dtL);
		}
		return dtL;
	}	

	public double deltaTRight(double offset) {

		double lr = leftRightAdjustment();

		double beta = 1.0;
		if (beta() != 0.0) {
			beta = beta();
		}

		double dtR = tdcToTime(TDCR) + (lr / 2) + rfpad() - ((0.5 * paddleLength() - paddleY()) / this.veff())
				- (PATH_LENGTH / (beta * 29.98)) - vertexCorr() - this.RF_TIME;

		// subtract the correction based on previous calibration values
		dtR = dtR - TWCorrR();
		dtR = dtR + offset;
		double bb = TOFCalibrationEngine.BEAM_BUCKET;
		dtR = (dtR + (1000 * bb) + (0.5 * bb)) % bb - 0.5 * bb;

		return dtR;
	}

//	public double deltaTLeftRFCorr() {
//		return deltaTLeft(0.0) + rfpad();
//	}
//	
//	public double deltaTRightRFCorr() {
//		return deltaTRight(0.0) + rfpad();
//	}
	
	public double paddleLength() {

		double len = 0.0;

		if (tof == "FTOF") {
			int layer = this.getDescriptor().getLayer();
			int paddle = this.getDescriptor().getComponent();

			if (layer == 1 && paddle <= 5) {
				len = (15.85 * paddle) + 16.43;
			} else if (layer == 1 && paddle > 5) {
				len = (15.85 * paddle) + 11.45;
			} else if (layer == 2) {
				len = (6.4 * paddle) + 10.84;
			} else if (layer == 3) {
				len = (13.73 * paddle) + 357.55;
			}
		} else {
			len = 88.05;
		}

		return len;

	}

	public double leftRight() {
		return (timeLeftAfterTW() - timeRightAfterTW());
	}

	public boolean isValidLeftRight() {
		return (tdcToTime(TDCL) != tdcToTime(TDCR));
	}

	double tdcToTime(double value) {
		return NS_PER_CH * value;
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
		return halfTimeDiff() * veff();
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

		double maxRcs = 75.0;
		double minV = -10.0;
		double maxV = 10.0;
		double minP = 0.0;
		double maxP = 5.0;
		if (tof == "FTOF") {
			maxRcs = TOFCalibration.maxRcs;
			minV = TOFCalibration.minV;
			maxV = TOFCalibration.maxV;
			minP = TOFCalibration.minP;
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
		boolean match = true;
		
		if (tof=="FTOF") {
			match = (TOFCalibration.trackPid==TOFCalibration.PID_ALL) ||
					(TOFCalibration.trackPid==TOFCalibration.PID_L && (PARTICLE_ID==13 || PARTICLE_ID==-13)) ||
					(TOFCalibration.trackPid==TOFCalibration.PID_L && (PARTICLE_ID==11 || PARTICLE_ID==-11)) ||
					(TOFCalibration.trackPid==TOFCalibration.PID_PI && (PARTICLE_ID==211 || PARTICLE_ID==-211)) ||
					(TOFCalibration.trackPid==TOFCalibration.PID_P && (PARTICLE_ID==2212));
					
		}
		
		return match;
		
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
		return ZPOS - CTOFCalibration.ctofCenter;
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
		System.out.println("XPOS " + XPOS + " YPOS " + YPOS + " ZPOS " + ZPOS + " PATH_LENGTH " + PATH_LENGTH
				+ " TRACK_ID " + TRACK_ID);
		System.out.println("PARTICLE_ID "+PARTICLE_ID+" mass "+mass()+" beta " + beta() + " P " + P + " RF_TIME " + RF_TIME + "ST_TIME " + ST_TIME + " RECON_TIME "
				+ RECON_TIME);
		System.out.println("VERTEX_Z " + VERTEX_Z + " TRACK_REDCHI2 " + TRACK_REDCHI2 + " CHARGE " + CHARGE
				+ " TRIGGER_BIT " + TRIGGER_BIT);
		System.out.println("goodTrackFound " + goodTrackFound() + " chargeMatch " + chargeMatch());
		System.out.println("refTime " + refTime() + " startTime " + startTime() + " averageHitTime "
				+ averageHitTime() + " vertexCorr " + vertexCorr());
		System.out.println("tofTimeRFCorr " + tofTimeRFCorr() + " startTimeRFCorr " + startTimeRFCorr()
				+ " startTimeP2PCorr " + startTimeP2PCorr());
		System.out.println("rfpad " + rfpad() + " p2p " + p2p() + " lamL " + lamL() + " tw1L " + tw1L() + " tw2L "
				+ tw2L() + " lamR " + lamR() + " tw1R " + tw1R() + " tw2R " + tw2R() + " LR " + leftRightAdjustment()
				+ " veff " + veff());
		System.out.println("paddleLength " + paddleLength() + " paddleY " + paddleY());
		System.out.println("timeLeftAfterTW " + timeLeftAfterTW() + " timeRightAfterTW " + timeRightAfterTW());
		System.out.println("deltaTLeft " + this.deltaTLeft(0.0) + " deltaTRight " + this.deltaTRight(0.0));

	}

}