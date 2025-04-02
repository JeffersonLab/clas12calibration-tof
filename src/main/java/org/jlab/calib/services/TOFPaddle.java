package org.jlab.calib.services;

import org.jlab.calib.services.ctof.CTOFCalibration;
import org.jlab.calib.services.ctof.CTOFCalibrationEngine;
import org.jlab.calib.services.ctof.CtofHposBinEventListener;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;
 
/**
 *
 * @author louiseclark
 */
public class TOFPaddle {

	public static CalibrationConstants jitConsts = null;   
	public static CalibrationConstants lgtConsts = null;   
        public static DatabaseConstantProvider dcp = null;
        public static int currentRun = 0;

	private final DetectorDescriptor desc = new DetectorDescriptor(TOFCalibration.TYPE);

	private int ADCL = 0;
	private int ADCR = 0;
	private int TDCL = 0;
	private int TDCR = 0;
	private double ENERGY = 0;
	private float ADC_TIMEL = 0;
	private float ADC_TIMER = 0;
        private double LENGTH = 0;
	private double XPOS = 0.0;
	private double YPOS = 0.0;
	private double ZPOS = 0.0;
	private double PATH_LENGTH = 0.0;
	private double PATH_LENGTH_BAR = 0.0;
	private double P = 0.0;
	private int TRACK_ID = -1;
	private double VERTEX_Z = 0.0;
	private double TRACK_REDCHI2 = -1.0;
	private int CHARGE = 0;
	private double RF_TIME = 124.25;
	private double ST_TIME = -1000.0;
	private long TRIGGER_BIT = 0;
	// public double TOF_TIME = 0.0;
	private double RECON_TIME = 0.0;
        private double JITTER = 0.0;
	private int PARTICLE_ID = 0;
	private int RUN = 0;
	private long TIMESTAMP = 0;

        private final double DEDX_MIP = 1.956; // units = MeV/g/cm^2
	private final double C = 29.9792458;
	public static final double NS_PER_CH = 0.02345;
	// public static final double NS_PER_CH = 0.024;

	//store variables to speed up code

	private boolean isInit;
	private int paddleNumber;
	private double geometricMean;
	private double geometricMeanNorm;
	private double thickness;
	private double logRatio;
	private boolean isValidGeoMean;
	private boolean includeInCalib;
	private boolean includeInTiming;
	private boolean includeInCTOFTiming;
	private boolean isValidLogRatio;
	private double mips;
	private double veff;
	private double tdcConvL;
	private double tdcConvR;
	private double rfpad;
	private double tw1;
	private double tw2;
	private double tw3;
	private double lamL;
	private double tw1pos;
	private double tw2pos;
	private double lamR;
	private double p2p;
	private double mass;
	private double beta;
	private double startTime;
	private double startTimeNoTW;
	private double reconStartTime;
	private double startTimeP2PCorr;
	private double averageHitTimeNoTW;
	private double averageHitTime;
	private double vertexCorr;
	private double refTime;
	private double refTimeNoTW;
	private double refSTTime;
	private double refTimeCorr;
	private double refTimeRFCorr;
	private double refTimeTWPosCorr;
	private double refSTTimeCorr;
	private double TimeCorr;
	private double refSTTimeRFCorr;
	private double refSTTimeHPosCorr;
	private double refSTTimeHPosFuncCorr;
	private double refSTTimeHPosBinCorr;
	private double HPosCorr;
	private double HPosCorrFunc;
	private double HPosCorrBin;
	private double hposA;
	private double hposB;
	private double hposC;
	private double TWPosCorr;
	private double timeLeftAfterTW;
	private double timeRightAfterTW;
	private double TWCorr;
	private double deltaTTW;
	private double ctofCenter;
	private double energy;
	private double leftRight;
	private boolean isValidLeftRight;
	private double tdcToTimeL;
	private double tdcToTimeR;
	private double veffHalfTimeDiff;
	private double halfTimeDiff;
	private double leftRightAdjustment;
	private double position;
	private double paddleY;
	private boolean trackFound;
	private boolean goodTrackFound;
	private boolean pidMatch;
	private boolean chargeMatch;

	public int getADCL() {
		return ADCL;
	}

	public int getADCR() {
		return ADCR;
	}

	public void setADC_TIMEL(float aDC_TIMEL) {
		ADC_TIMEL = aDC_TIMEL;
	}

	public float getADC_TIMEL() {
		return ADC_TIMEL;
	}

	public void setADC_TIMER(float aDC_TIMER) {
		ADC_TIMER = aDC_TIMER;
	}

	public float getADC_TIMER() {
		return ADC_TIMER;
	}

	public int getTDCL() {
		return TDCL;
	}

	public int getTDCR() {
		return TDCR;
	}

	public void setENERGY(double eNERGY) {
		ENERGY = eNERGY;
	}

	public double getENERGY() {
		return ENERGY;
	}

	public double getXPOS() {
		return XPOS;
	}

	public double getYPOS() {
		return YPOS;
	}

	public double getZPOS() {
		return ZPOS;
	}

	public void setPATH_LENGTH(double pATH_LENGTH) {
		PATH_LENGTH = pATH_LENGTH;
	}

	public double getPATH_LENGTH() {
		return PATH_LENGTH;
	}

	public void setPATH_LENGTH_BAR(double pATH_LENGTH_BAR) {
		PATH_LENGTH_BAR = pATH_LENGTH_BAR;
	}

	public double getPATH_LENGTH_BAR() {
		return PATH_LENGTH_BAR;
	}

	public void setP(double p) {
		P = p;
	}

	public double getP() {
		return P;
	}

	public void setTRACK_ID(int tRACK_ID) {
		TRACK_ID = tRACK_ID;
	}

	public int getTRACK_ID() {
		return TRACK_ID;
	}

	public void setVERTEX_Z(double vERTEX_Z) {
		VERTEX_Z = vERTEX_Z;
	}
	public double getVERTEX_Z() {
		return VERTEX_Z;
	}
	public void setTRACK_REDCHI2(double tRACK_REDCHI2) {
		TRACK_REDCHI2 = tRACK_REDCHI2;
	}

	public double getTRACK_REDCHI2() {
		return TRACK_REDCHI2;
	}

	public void setCHARGE(int cHARGE) {
		CHARGE = cHARGE;
	}

	public void setRF_TIME(double rF_TIME) {
		RF_TIME = rF_TIME;
	}

	public void setST_TIME(double sT_TIME) {
		ST_TIME = sT_TIME;
	}

	public double getST_TIME() {
		return ST_TIME;
	}

	public void setRECON_TIME(double rECON_TIME) {
		RECON_TIME = rECON_TIME;
	}

	public void setJITTER(double jITTER) {
		JITTER = jITTER;
	}

	public void setPARTICLE_ID(int pARTICLE_ID) {
		PARTICLE_ID = pARTICLE_ID;
	}

	public void setRUN(int rUN) {
		RUN = rUN;
	}

	public void setTIMESTAMP(long tIMESTAMP) {
		TIMESTAMP = tIMESTAMP;
	}

	private void setPaddleNumber() { //no dependence
		final int[] paddleOffset = { 0, 0, 23, 85 };
		final int sector = this.getDescriptor().getSector();
		final int layer = this.getDescriptor().getLayer();
		final int component = this.getDescriptor().getComponent();

		final int p = component + (sector - 1) * 90 + paddleOffset[layer];
		paddleNumber=p;
}
	private void setGeometricMean() { //Adcl & r
		geometricMean = Math.sqrt(ADCL * ADCR);			
}
	private void setGeometricMeanNorm() { //geometricmean & thickness & Path_length_bar
		geometricMeanNorm = geometricMean * (thickness/PATH_LENGTH_BAR);
}
	private void setThickness() { //no dependence
		double t = 0.0;
		if (this.desc.getType()==DetectorType.FTOF) {
			t = 5.08;
			if (this.getDescriptor().getLayer() == 2) {
				t = 6.0;
			}
		}
		else {
			t = 3.0;
		}
		thickness=t;
}
	private void setLogRatio() { //Adcl & r
		logRatio = Math.log((double) ADCR / (double) ADCL);
}
	private void setIsValidGeoMean() { //geometricmean
		isValidGeoMean = (geometricMean > 100.0);

}
	private void setIncludeInCalib() { //Adcl & r
		// return (ADCR != 0 || ADCL != 0);
		// return (this.geometricMean() > 100.0 && ADCR>0 && ADCL>0 && TDCL>0 &&
		// TDCR>0);
		final double[] minAdc = { 0.0, 20.0, 50.0, 20.0 };
		final int layer = this.getDescriptor().getLayer();
		includeInCalib = (ADCL > minAdc[layer] && ADCR > minAdc[layer]);
}
	private void setIncludeInTiming() { //Tdcl & r
		includeInTiming = (TDCL > 0 && TDCR > 0);
}
	private void setIncludeInCTOFTiming() { //Tdcl & r & St_time
		includeInCTOFTiming = (TDCL > 0 && TDCR > 0 && ST_TIME != -1000.0);
}
	private void setIsValidLogRatio() { //geometricmean
		// only if geometric mean is over a minimum
		final double[] minGM = { 0.0, 300.0, 500.0, 300.0 };
		final int layer = this.getDescriptor().getLayer();

		isValidLogRatio = geometricMean > minGM[layer];
}
	private void setMips() { //no dependence
		// take from target MIP channel for CTOF

		mips = (this.desc.getType() == DetectorType.FTOF)?(TOFCalibrationEngine.gainValues.getDoubleValue("mipa_left", desc.getSector(), desc.getLayer(), desc.getComponent())):(CTOFCalibration.expectedMipChannel);
}
	private void setVeff() { //no dependence
		veff = (this.desc.getType() == DetectorType.FTOF)?(TOFCalibrationEngine.veffValues.getDoubleValue("veff_left", desc.getSector(), desc.getLayer(), desc.getComponent())):(CTOFCalibrationEngine.veffValues.getDoubleValue("veff_upstream", desc.getSector(), desc.getLayer(), desc.getComponent()));
}
	private void setTdcConvL() { //no dependence
		tdcConvL = (this.desc.getType() == DetectorType.FTOF)?(TOFCalibrationEngine.convValues.getDoubleValue("left", desc.getSector(), desc.getLayer(), desc.getComponent())):(CTOFCalibrationEngine.convValues.getDoubleValue("upstream", desc.getSector(), desc.getLayer(), desc.getComponent()));
}
	private void setTdcConvR() { //no dependence
		tdcConvR = (this.desc.getType() == DetectorType.FTOF)?(TOFCalibrationEngine.convValues.getDoubleValue("right", desc.getSector(), desc.getLayer(), desc.getComponent())):(CTOFCalibrationEngine.convValues.getDoubleValue("downstream", desc.getSector(), desc.getLayer(), desc.getComponent()));
}
	private void setRfpad() { //no dependence
		rfpad = (this.desc.getType() == DetectorType.FTOF)?(TOFCalibrationEngine.rfpadValues.getDoubleValue("rfpad", desc.getSector(), desc.getLayer(), desc.getComponent())):(CTOFCalibrationEngine.rfpadValues.getDoubleValue("rfpad", desc.getSector(), desc.getLayer(), desc.getComponent()));
}
	private void setTw1() { //no dependence
		tw1 = (this.desc.getType() == DetectorType.FTOF)?(TOFCalibrationEngine.timeWalkValues.getDoubleValue("tw1", desc.getSector(), desc.getLayer(), desc.getComponent())):0.0;
}
	private void setTw2() { //no dependence
		tw2 = (this.desc.getType() == DetectorType.FTOF)?(TOFCalibrationEngine.timeWalkValues.getDoubleValue("tw2", desc.getSector(), desc.getLayer(), desc.getComponent())):0.0;
}
	private void setTw3() { //no dependence
		tw3 = (this.desc.getType() == DetectorType.FTOF)?(TOFCalibrationEngine.timeWalkValues.getDoubleValue("tw3", desc.getSector(), desc.getLayer(), desc.getComponent())):0.0;
}
	private void setLamL() { //no dependence
		lamL = (this.desc.getType() == DetectorType.FTOF)?(TOFCalibrationEngine.timeWalkValues.getDoubleValue("tw0_left", desc.getSector(), desc.getLayer(), desc.getComponent())):0.0;
}
	private void setTw1pos() { //no dependence
		tw1pos = (this.desc.getType() == DetectorType.FTOF)?(TOFCalibrationEngine.twposValues.getDoubleValue("tw1pos", desc.getSector(), desc.getLayer(), desc.getComponent())):0.0;
}
	private void setTw2pos() { //no dependence
		tw2pos = (this.desc.getType() == DetectorType.FTOF)?(TOFCalibrationEngine.twposValues.getDoubleValue("tw2pos", desc.getSector(), desc.getLayer(), desc.getComponent())):0.0;
}
	private void setLamR() { //no dependence
		lamR = (this.desc.getType() == DetectorType.FTOF)?(TOFCalibrationEngine.timeWalkValues.getDoubleValue("tw0_right", desc.getSector(), desc.getLayer(), desc.getComponent())):0.0;
}
	private void setP2p() { //no dependence
		p2p = (this.desc.getType() == DetectorType.FTOF)?(TOFCalibrationEngine.p2pValues.getDoubleValue("paddle2paddle", desc.getSector(), desc.getLayer(), desc.getComponent())):(CTOFCalibrationEngine.p2pValues.getDoubleValue("paddle2paddle", desc.getSector(), desc.getLayer(), desc.getComponent()));
}
	private void setMass() { //Particle_id
		double mass = 0.0;
		final double[] massList = { 0.13957, 0.938272, 0.000511 };
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
		this.mass = mass;
}
	private void set_beta() { //mass & P
		beta = P/Math.sqrt(P*P+mass*mass);
}
	private void setStartTime() { //beta & averagehittime & Path_length
		final double beta = (this.beta != 0.0)?this.beta:1.0;

		final double startTime = averageHitTime - (PATH_LENGTH / (beta * 29.98));
		this.startTime = startTime;
}
	private void setStartTimeNoTW() { //beta & averagehittimenotw & Path_length
		final double beta = (this.beta != 0.0)?this.beta:1.0;

		final double startTime = averageHitTimeNoTW - (PATH_LENGTH / (beta * 29.98));
		startTimeNoTW = startTime;
}
	private void setReconStartTime() { //beta & Path_length & Recon_time
		final double beta = (this.beta != 0.0)?this.beta:1.0;

		final double startTime = RECON_TIME - (PATH_LENGTH / (beta * 29.98));
		reconStartTime = startTime;
}
	private void setStartTimeP2PCorr() { //starttime & rfpad & p2p
		startTimeP2PCorr = startTime + rfpad + p2p;
}
	private void setAverageHitTimeNoTW() { //leftrightadjustment & tdctotimel & tdctotimer & veff & paddley & Length
		final double lr = leftRightAdjustment;
		final double tL = tdcToTimeL - (lr / 2) - ((0.5 * LENGTH + paddleY) / veff);

		final double tR = tdcToTimeR + (lr / 2) - ((0.5 * LENGTH - paddleY) / veff);

		averageHitTimeNoTW = (tL + tR) / 2.0;
}
	private void setAverageHitTime() { //leftrightadjustment & timeleftaftertw & timerightaftertw & veff & paddley & Length
		final double lr = leftRightAdjustment;
		final double tL = timeLeftAfterTW - (lr / 2) - ((0.5 * LENGTH + paddleY) / veff);

		final double tR = timeRightAfterTW + (lr / 2) - ((0.5 * LENGTH - paddleY) / veff);

		averageHitTime = (tL + tR) / 2.0;
}
	private void setVertexCorr() { //Vertex_z
		vertexCorr = (this.desc.getType() == DetectorType.FTOF && TOFCalibration.vertexCorr == TOFCalibration.VERTEX_CORR_YES)?
		((VERTEX_Z - TOFCalibration.targetPos) / 29.98):0.0;
}
	private void setRefTime() { //starttime & vertexcorr & Rf_time
		refTime = RF_TIME - (startTime - vertexCorr);
}
	private void setRefTimeNoTW() { //starttimenotw & vertexcorr & Rf_time
		refTimeNoTW = RF_TIME - (startTimeNoTW - vertexCorr);
}
	private void setRefSTTime() { //starttime & vertexcorr & St_time
		refSTTime = ST_TIME - (startTime - vertexCorr);
}
	private void setRefTimeCorr() { //reftime & rfpad & twposcorr
		refTimeCorr = refTime - rfpad - TWPosCorr;
}
	private void setRefTimeRFCorr() { //reftime & rfpad
		refTimeRFCorr = refTime - rfpad;
}
	private void setRefTimeTWPosCorr() { //reftime & twposcorr
		refTimeTWPosCorr = refTime - TWPosCorr;
}
	private void setRefSTTimeCorr() { //refsttime & rfpad & hposcorr & p2p
		refSTTimeCorr = refSTTime - rfpad - HPosCorr - p2p;
}
	private void setTimeCorr() { //averagehittime & rfpad & hposcorr & twposcorr & p2p
		TimeCorr = averageHitTime + rfpad + HPosCorr + TWPosCorr + p2p;
}
	private void setRefSTTimeRFCorr() { //refsttime & rfpad
		refSTTimeRFCorr = refSTTime - rfpad;
}
	private void setRefSTTimeHPosCorr() { //refsttime & hposcorr
		refSTTimeHPosCorr = refSTTime - HPosCorr;
}
	private void setRefSTTimeHPosFuncCorr() { //refsttime & rfpad & hposcorrfunc
		refSTTimeHPosFuncCorr = refSTTime - rfpad - HPosCorrFunc;
}
	private void setRefSTTimeHPosBinCorr() { //refsttime & rfpad & hposcorrbin
		refSTTimeHPosBinCorr = refSTTime - rfpad - HPosCorrBin;
}
	private void setHPosCorr() { //hposcorrfunc & hposcorrbin
		HPosCorr = (this.desc.getType()==DetectorType.CTOF)?(HPosCorrFunc + HPosCorrBin):0.0;
}
	private void setHPosCorrFunc() { //paddley &hposa, b, c
		HPosCorrFunc = (this.desc.getType()==DetectorType.CTOF)?(hposA*paddleY*paddleY+hposB*paddleY+hposC):0.0;
}
	private void setHPosCorrBin() { //paddley
		HPosCorrBin = (this.desc.getType()==DetectorType.CTOF)?CTOFCalibrationEngine.hposBinValues.getItem(desc.getSector(), desc.getLayer(), desc.getComponent())[CtofHposBinEventListener.sliceNumber(paddleY)]:0.0;
}
	private void setHposA() { //no dependence
		hposA = (this.desc.getType()==DetectorType.CTOF)?CTOFCalibrationEngine.hposFuncValues.getDoubleValue("hposa", desc.getSector(), desc.getLayer(),
		desc.getComponent()):0.0;
}
	private void setHposB() { //no dependence
		hposB = (this.desc.getType()==DetectorType.CTOF)?CTOFCalibrationEngine.hposFuncValues.getDoubleValue("hposb", desc.getSector(), desc.getLayer(),
		desc.getComponent()):0.0;
}
	private void setHposC() { //no dependence
		hposC = (this.desc.getType()==DetectorType.CTOF)?CTOFCalibrationEngine.hposFuncValues.getDoubleValue("hposc", desc.getSector(), desc.getLayer(),
		desc.getComponent()):0.0;
}
	private void setTWPosCorr() { //paddley & tw1pos & tw2pos
		TWPosCorr = (this.desc.getType()==DetectorType.FTOF)?(tw1pos*paddleY*paddleY + tw2pos*paddleY):0.0;
}
	private void setTimeLeftAfterTW() { //tdctimetol && twcorr
		timeLeftAfterTW = (this.desc.getType() == DetectorType.FTOF)?(tdcToTimeL-TWCorr):tdcToTimeL;
}
	private void setTimeRightAfterTW() { //tdctimetor && twcorr
		timeRightAfterTW = (this.desc.getType() == DetectorType.FTOF)?(tdcToTimeR-TWCorr):tdcToTimeR;
}
	private void setTWCorr() { //tw1 & 2 & 3 & eneregy
		TWCorr = tw1*Math.exp(tw2*energy) + tw3/energy;
}
	private void setDeltaTTW() { //twcorr (calculate with 0 offset and recalculate later if necessary)
		final double bb = TOFCalibrationEngine.BEAM_BUCKET;
		deltaTTW = ((-refTimeNoTW - TWCorr/* + offset*/) + (1000 * bb) + (0.5 * bb)) % bb - 0.5 * bb;
}
	private void setCtofCenter() { //no dependence
		ctofCenter = (this.getDescriptor().getComponent()%2==0)?(-8.5031 + TOFCalibration.targetPos):(-8.9874 + TOFCalibration.targetPos);
}
	private void setEnergy() { //mips & thickness & Energy & Adcl & r
		double energyCalc = ENERGY;
		if(mips!=0) {
			final double AdcToEConv = mips / (DEDX_MIP * thickness);
			final double edepLeft  = ADCL / AdcToEConv;
			final double edepRight = ADCR / AdcToEConv;
			
			energyCalc = Math.sqrt(edepLeft * edepRight);
		}
		energy = energyCalc;
}
	private void setLeftRight() { //timeleftaftertw & timerightaftertw
		leftRight = timeLeftAfterTW - timeRightAfterTW;
}
	private void setIsValidLeftRight() { //tdctotimel & r
		isValidLeftRight = (tdcToTimeL != tdcToTimeR);
}
	private void setTdcToTimeL() { //tdcconvl & Tdcl & Jitter
		tdcToTimeL = tdcConvL * TDCL  - JITTER;
}
	private void setTdcToTimeR() { //tdcconvr & Tdcr & Jitter
		tdcToTimeR = tdcConvR * TDCR  - JITTER;

}
	private void setVeffHalfTimeDiff() { //timeleftaftertw & timerightaftertw
		veffHalfTimeDiff = (timeLeftAfterTW - timeRightAfterTW) / 2;

}
	private void setHalfTimeDiff() { //timeleftaftertw & timerightaftertw & leftrightadjustment
		halfTimeDiff = (timeLeftAfterTW - timeRightAfterTW - leftRightAdjustment) / 2;
}
	private void setLeftRightAdjustment() { //no dependence
		leftRightAdjustment = ((this.desc.getType() == DetectorType.FTOF)?(TOFCalibrationEngine.leftRightValues.getDoubleValue("left_right", desc.getSector(), desc.getLayer(), 		desc.getComponent())):(CTOFCalibrationEngine.leftRightValues.getDoubleValue("upstream_downstream", desc.getSector(), desc.getLayer(), desc.getComponent())));
}
	private void setPosition() { //halftimediff & veff & ctofcenter
		position = halfTimeDiff * veff + ((this.desc.getType() == DetectorType.CTOF)?ctofCenter:0.0);
}
	private void setPaddleY() { //no dependence
		double y = 0.0;
		if (this.desc.getType() == DetectorType.FTOF) {
			final int sector = desc.getSector();
			final double rotation = Math.toRadians((sector - 1) * 60);
			y = YPOS * Math.cos(rotation) - XPOS * Math.sin(rotation);
		} else {
			y = ZPOS;
		}
		paddleY = y;
}
	private void setTrackFound() { //Track_id
		trackFound = (TRACK_ID != -1);
}
	private void setGoodTrackFound() { //trackfound & chargematch & pidmatch & mass & Track_redchi2 & Vertex_z
		double maxRcs = 100.0;
		double minV = -10.0;
		double maxV = 10.0;
		double minP = 0.0;
		double maxP = 9.0;
		if (this.desc.getType() == DetectorType.FTOF) {
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

		goodTrackFound = (trackFound && TRACK_REDCHI2 < maxRcs && VERTEX_Z > minV && VERTEX_Z < maxV 
				&& P > minP && P < maxP
				&& chargeMatch
				&& pidMatch
				&& mass != 0.0);
}
	private void setPidMatch() { //Particle_id
		int selectedPid = 0;
		if (getDescriptor().getLayer() == 3) {
			selectedPid = TOFCalibration.trackPid2;
		}
		else {
			selectedPid = TOFCalibration.trackPid;
		}

		pidMatch = (selectedPid==TOFCalibration.PID_ALL) ||
					(selectedPid==TOFCalibration.PID_L && (PARTICLE_ID==13 || PARTICLE_ID==-13)) ||
					(selectedPid==TOFCalibration.PID_L && (PARTICLE_ID==11 || PARTICLE_ID==-11)) ||
					(selectedPid==TOFCalibration.PID_PI && (PARTICLE_ID==211 || PARTICLE_ID==-211)) ||
					(selectedPid==TOFCalibration.PID_P && (PARTICLE_ID==2212)) ||
					(selectedPid==TOFCalibration.PID_LPI && (PARTICLE_ID==13 || PARTICLE_ID==-13)) ||
					(selectedPid==TOFCalibration.PID_LPI && (PARTICLE_ID==11 || PARTICLE_ID==-11)) ||
					(selectedPid==TOFCalibration.PID_LPI && (PARTICLE_ID==211 || PARTICLE_ID==-211));
}
	private void setChargeMatch() { //Charge
		final int trackCharge = (this.desc.getType() == DetectorType.FTOF)?TOFCalibration.trackCharge:CTOFCalibration.trackCharge;

		chargeMatch = (trackCharge == TOFCalibration.TRACK_BOTH || trackCharge == TOFCalibration.TRACK_NEG && CHARGE == -1
				|| trackCharge == TOFCalibration.TRACK_POS && CHARGE == 1);
}
	

	public void init(){
		if(isInit) return;
		setPaddleNumber();
		setGeometricMean();
		setThickness();
		setGeometricMeanNorm();
		setLogRatio();
		setIsValidGeoMean();
		setIncludeInCalib();
		setIncludeInTiming();
		setIncludeInCTOFTiming();
		setIsValidLogRatio();
		setMips();
		setVeff();
		setTdcConvL();
		setTdcConvR();
		setRfpad();
		setTw1();
		setTw2();
		setTw3();
		setLamL();
		setTw1pos();
		setTw2pos();
		setLamR();
		setP2p();
		setMass();
		set_beta();
		setReconStartTime();
		setVertexCorr();
		setLeftRightAdjustment();
		setPaddleY();
		setHposA();
		setHposB();
		setHposC();
		setCtofCenter();
		setTrackFound();
		setPidMatch();
		setChargeMatch();
		setTdcToTimeL();
		setTdcToTimeR();
		setIsValidLeftRight();
		setEnergy();
		setTWCorr();
		setDeltaTTW();
		setTimeLeftAfterTW();
		setTimeRightAfterTW();
		setLeftRight();
		setAverageHitTime();
		setStartTime();
		setRefTime();
		setRefSTTime();
		setAverageHitTimeNoTW();
		setStartTimeNoTW();
		setRefTimeNoTW();
		setStartTimeP2PCorr();
		setTWPosCorr();
		setRefTimeCorr();
		setRefTimeRFCorr();
		setRefTimeTWPosCorr();
		setHPosCorrFunc();
		setHPosCorrBin();
		setHPosCorr();
		setRefSTTimeCorr();
		setTimeCorr();
		setRefSTTimeRFCorr();
		setRefSTTimeHPosCorr();
		setRefSTTimeHPosFuncCorr();
		setRefSTTimeHPosBinCorr();
		setVeffHalfTimeDiff();
		setHalfTimeDiff();
		setPosition();
		setGoodTrackFound();
		isInit = true;
	}

	public TOFPaddle(int sector, int layer, int paddle) {
		this.desc.setSectorLayerComponent(sector, layer, paddle);
		isInit = false;
        }
        
        public void setRun(int run, long triggerbit, long timestamp) {
            // Get the TDC jitter parameters when runNo changes
            // Get the paddle lengths
            this.RUN = run;
            this.TRIGGER_BIT = triggerbit;
            this.TIMESTAMP = timestamp;
            
            if (RUN != currentRun && RUN!=0) {
                dcp = new DatabaseConstantProvider(RUN, "default");
                if (this.desc.getType() == DetectorType.FTOF) {
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
            
            
            if (this.desc.getType() == DetectorType.FTOF) {
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
		return paddleNumber;		
	}

	public double geometricMean() {
		return geometricMean;
	}
	
	public double geometricMeanNorm() {
		return geometricMeanNorm; 
	}
	
	public double thickness() {
		return thickness;
	}

	public double logRatio() {
		return logRatio;
	}

	public boolean isValidGeoMean() {
		return isValidGeoMean;
	}

	public boolean includeInCalib() {
		if (!isInit) {
			setIncludeInCalib();
		}
		return includeInCalib;
	}

	public boolean includeInTiming() {
		return includeInTiming;
	}

	public boolean includeInCTOFTiming() {
		return includeInCTOFTiming;
	}

	public boolean isValidLogRatio() {
		return isValidLogRatio;
	}

	public double mips() {
		return mips;
	}

    public double veff() {
		return veff;
	}

	public double tdcConvL() {
		return tdcConvL;
	}	
	
	public double tdcConvR() {
		return tdcConvR;
	}

	public double rfpad() {
		return rfpad;
	}
	
	public double tw1() {
		return tw1;
	}

	public double tw2() {
		return tw2;
	}
	
	public double tw3() {
		return tw3;
	}
	
	
	public double lamL() {
		return lamL;
	}

	public double tw1pos() {
		return tw1pos;
	}

	public double tw2pos() {
		return tw2pos;
	}

	public double lamR() {
		return lamR;
	}

	public double p2p() {
		return p2p;
	}

	
	private double mass() {
		return mass;
	}
	
	private double beta() {
		return beta;
	}

	public double startTime() {
		return startTime;
	}
	
	public double startTimeNoTW() {
		return startTimeNoTW;
	}

	public double reconStartTime() {
		return reconStartTime;
	}

	public double startTimeP2PCorr() {
		return startTimeP2PCorr;
	}

	public double averageHitTimeNoTW() {
		return averageHitTimeNoTW;
	}
	
	public double averageHitTime() {
		return averageHitTime;
	}

	public double vertexCorr() {
		return vertexCorr;
	}

	public double refTime() {
		return refTime;
	}
	
	public double refTimeNoTW() {
		return refTimeNoTW;
	}	

	public double refSTTime() {
		return refSTTime;
	}

	public double refTimeCorr() {
		return refTimeCorr;
	}
	
	public double refTimeRFCorr() {
		return refTimeRFCorr;
	}

	public double refTimeTWPosCorr() {
		return refTimeTWPosCorr;
	}

	public double refSTTimeCorr() {
		return refSTTimeCorr;
	}

	public double TimeCorr() {
		return TimeCorr;
	}

    public double refSTTimeRFCorr() {
		return refSTTimeRFCorr;
	}

	public double refSTTimeHPosCorr() {
		return refSTTimeHPosCorr;
	}

	public double refSTTimeHPosFuncCorr() {
		return refSTTimeHPosFuncCorr;
	}

	public double refSTTimeHPosBinCorr() {
		return refSTTimeHPosBinCorr;
	}
	
	
	public double HPosCorr() {
		return HPosCorr;
	}	
	
	public double HPosCorrFunc() {
		return HPosCorrFunc;
	}
	
	public double HPosCorrBin() {
		return HPosCorrBin;
	}
	
	public double hposA() {
		return hposA;
	}

	public double hposB() {
		return hposB;
	}
	
	public double hposC() {
		return hposC;
	}	
	
	public double TWPosCorr() {
		return TWPosCorr;
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
		return timeLeftAfterTW;
	}

	public double timeRightAfterTW() {
		return timeRightAfterTW;
	}	
	
	// LC Sep 19
	private double TWCorr() {
		return TWCorr;
	}
	
	// LC Sep 19
	public double deltaTTW(double offset) {
		final double bb = TOFCalibrationEngine.BEAM_BUCKET;
		return ((offset==0)?deltaTTW:(((-refTimeNoTW - TWCorr + offset) + (1000 * bb) + (0.5 * bb)) % bb - 0.5 * bb));
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
		return ctofCenter;
	}
	
//	public double paddleLength() {
//
//		double len = 0.0;
//		int paddle = this.getDescriptor().getComponent();
//
//		if (this.desc.getType() == DetectorType.FTOF) {
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
		if (!isInit) {
			//Init();
			setMips();
			setThickness();
			setEnergy();
		}
		return energy;
    }
        
	public double leftRight() {
		return leftRight;
	}

	public boolean isValidLeftRight() {
		return isValidLeftRight;
	}

	public double tdcToTimeL() {
		return tdcToTimeL;
	}

	public double tdcToTimeR() {
		return tdcToTimeR;
	}
	
	public double veffHalfTimeDiff() {
		return veffHalfTimeDiff;
	}

	public double halfTimeDiff() {
		return halfTimeDiff;
	}

	public double leftRightAdjustment() {
		return leftRightAdjustment;
	}

	public double position() {
		return position;
                }

	public double paddleY() {
		return paddleY;
	}

	public boolean trackFound() {
		return trackFound;
	};

	public boolean goodTrackFound() {
		return goodTrackFound;
	}
	
	public boolean pidMatch() {
		return pidMatch;		
	}

	public boolean chargeMatch() {
		return chargeMatch;	
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