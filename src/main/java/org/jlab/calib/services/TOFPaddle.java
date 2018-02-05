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
	public double BETA = 0.0;
	public double P = 0.0;
	public int TRACK_ID = -1;
	public double VERTEX_Z = 0.0;
	public double TRACK_REDCHI2 = 0.0;
	public int CHARGE = 0;
	public double RF_TIME = 124.25;
	public long TRIGGER_BIT = 0;
	//public double TOF_TIME = 0.0;
	public double RECON_TIME = 0.0;
	public int PARTICLE_ID = -1;

	public static final int PID_ELECTRON = 11;
	public static final int PID_PION = 211;
	private final double C = 29.98;
	public static final double NS_PER_CH = 0.02345;
//	public static final double NS_PER_CH = 0.024;

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
		int[] paddleOffset = {0, 0, 23, 85};
		int sector = this.getDescriptor().getSector();
		int layer = this.getDescriptor().getLayer();
		int component = this.getDescriptor().getComponent();

		p = component + (sector-1)*90 + paddleOffset[layer]; 
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
		//return (ADCR != 0 || ADCL != 0);
		//return (this.geometricMean() > 100.0 && ADCR>0 && ADCL>0 && TDCL>0 && TDCR>0);
		double[] minAdc = {0.0, 20.0, 50.0, 20.0};
		int layer = this.getDescriptor().getLayer();
		return (ADCL > minAdc[layer] && ADCR > minAdc[layer]);
	}
	
	public boolean includeInTiming() {
		return (TDCL>0 && TDCR>0);
	}

	//	public boolean includeInCtofVeff() {
	//		// exclude if position is zero or veff is unrealistic
	//		return (this.ZPOS != 0)
	//				&& (Math.abs(position() - this.zPosCTOF()) < 20.0);
	//	}

	public boolean isValidLogRatio() {
		// only if geometric mean is over a minimum
		double[] minGM = {0.0, 300.0, 500.0, 300.0};
		int layer = this.getDescriptor().getLayer();

		return this.geometricMean() > minGM[layer];
	}

	public double veff() {
		double veff = 16.0;
		if (tof == "FTOF") {
			veff = TOFCalibrationEngine.veffValues.getDoubleValue("veff_left",
					desc.getSector(), desc.getLayer(), desc.getComponent());
			//System.out.println("veff "+desc.getSector()+desc.getLayer()+desc.getComponent()+" "+veff);
		} else {
			veff = CTOFCalibrationEngine.veffValues.getDoubleValue("veff_upstream",
					desc.getSector(), desc.getLayer(), desc.getComponent());
		}

		return veff;
	}

	//	public double p2p() {
	//		double p2p = 0.0;
	//		if (tof == "FTOF") {
	//			p2p = TOFCalibrationEngine.p2pValues.getDoubleValue("paddle2paddle",
	//					desc.getSector(), desc.getLayer(), desc.getComponent())
	//				+ TOFCalibrationEngine.rfpadValues.getDoubleValue("rfpad", desc.getSector(), desc.getLayer(), desc.getComponent());
	//			//System.out.println("p2p "+desc.getSector()+desc.getLayer()+desc.getComponent()+" "+p2p);
	//		} else {
	//			p2p = 0.0;
	//			//p2p = CTOFCalibrationEngine.p2pValues.getItem(desc.getSector(), desc.getLayer(), desc.getComponent());
	//		}
	//
	//		return p2p;
	//	}

	public double rfpad() {
		double rfpad = 0.0;
		if (tof == "FTOF") {
			rfpad = TOFCalibrationEngine.rfpadValues.getDoubleValue("rfpad", desc.getSector(), desc.getLayer(), desc.getComponent());
		}
		else {
			rfpad = CTOFCalibrationEngine.rfpadValues.getDoubleValue("rfpad", desc.getSector(), desc.getLayer(), desc.getComponent());
		}
		return rfpad;
	}
	
	public double p2p() {
		double p2p = 0.0;
		if (tof == "FTOF") {
			p2p = TOFCalibrationEngine.p2pValues.getDoubleValue("paddle2paddle", desc.getSector(), desc.getLayer(), desc.getComponent());
		}
		else {
			p2p = CTOFCalibrationEngine.p2pValues.getDoubleValue("paddle2paddle", desc.getSector(), desc.getLayer(), desc.getComponent());
		}
		return p2p;
	}	

	// timeResidualsADC
	// rename to timeResiduals to use this version comparing TDC time to ADC time
	public double[] timeResidualsADC(double[] lambda, double[] order, int iter) {
		double[] tr = { 0.0, 0.0 };

		double timeL = tdcToTime(TDCL);
		double timeR = tdcToTime(TDCR);

		tr[LEFT] = timeL - this.ADC_TIMEL;
		tr[RIGHT] = timeR - this.ADC_TIMER;

		return tr;
	}	

	public double startTime() {
		double startTime = 0.0;

		double beta = 1.0;
		if (BETA != 0.0) {
			beta = BETA;
		}

		//startTime = TOF_TIME - (PATH_LENGTH/(beta*29.98));
		startTime = p2pAverageHitTime() - (PATH_LENGTH/(beta*29.98));
		return startTime;
	}

	public double reconStartTime() {
		double startTime = 0.0;

		double beta = 1.0;
		if (BETA != 0.0) {
			beta = BETA;
		}

		startTime = RECON_TIME - (PATH_LENGTH/(beta*29.98));
		return startTime;
	}
	
	public double startTimeRFCorr() {
		return startTime() + rfpad();
	}
	
	public double startTimeP2PCorr() {
		return startTime() + rfpad() + p2p();
	}

	public double p2pAverageHitTime() {

		double lr = leftRightAdjustment();
		double tL = timeLeftAfterTW() - (lr/2) 
		- ((0.5*paddleLength() + paddleY())/this.veff());

		double tR = timeRightAfterTW() + (lr/2)
				- ((0.5*paddleLength() - paddleY())/this.veff());

		return (tL+tR)/2.0;

	}

	public double tofTimeRFCorr() {
		return p2pAverageHitTime() + rfpad();
	}

	public double vertexCorr() {
		if (TOFCalibration.vertexCorr == TOFCalibration.VERTEX_CORR_YES) {
			return VERTEX_Z/29.98;
		}
		else {
			return 0.0;
		}
	}

	public double refTime() {
		return this.RF_TIME - (this.startTime() - vertexCorr());
	}

	public double refTimeCorr() {
		return refTime() - rfpad();
	}

	private double TWCorrL() {
		
		if (TOFCalibration.twMethod == TOFCalibration.TW_POS_DEP) {
			return this.posDepTWCorrL();
		}
		else {
			return this.posIndepTWCorrL();
		}
		
	}
	
	private double TWCorrR() {
		
		if (TOFCalibration.twMethod == TOFCalibration.TW_POS_DEP) {
			return this.posDepTWCorrR();
		}
		else {
			return this.posIndepTWCorrR();
		}
		
	}
	
	private double posIndepTWCorrL() {

//		System.out.println("SLC "+desc.getSector()+desc.getLayer()+desc.getComponent());
//		System.out.println("ADCL "+ADCL+" lamL "+lamL());
//		System.out.println("posIndepTWCorrL "+(lamL() / Math.pow(ADCL, 0.5)));
		return lamL() / Math.pow(ADCL, 0.5);
		
	}

	private double posIndepTWCorrR() {
//		System.out.println("SLC "+desc.getSector()+desc.getLayer()+desc.getComponent());
//		System.out.println("ADCR "+ADCR+" lamR "+lamR());
//		System.out.println("posIndepTWCorrR "+(lamR() / Math.pow(ADCR, 0.5)));
		return lamR() / Math.pow(ADCR, 0.5);
	}	

	// position dependent version
	private double posDepTWCorrL() {
		double padNum = getDescriptor().getComponent();
		double tw0 = lamL();
		double tw1 = tw1L();
		double tw2 = tw2L();
		double coorTerm = paddleY();
		double paddleTerm = (1 - (tw2 - tw1*padNum)) / paddleLength();
		double newTw0 = tw0 - tw0*coorTerm*paddleTerm;
		
//		System.out.println("pos Dep TWCorrL");
//		System.out.println("Panel "+this.getDescriptor().getLayer());
//		System.out.println("padNum "+padNum);
//		System.out.println("lamL "+lamL());
//		System.out.println("tw1L "+tw1L());
//		System.out.println("tw2L "+tw2L());
//		System.out.println("paddleLength "+paddleLength());
//		System.out.println("paddleY "+paddleY());
//		System.out.println("coorTerm "+coorTerm);
//		System.out.println("paddleTerm "+paddleTerm);
//		System.out.println("new tw0 "+newTw0);
		
		return newTw0 / (Math.pow(ADCL,0.5));
	}	
	
	private double posDepTWCorrR() {
		double padNum = getDescriptor().getComponent();
		double tw0 = lamR();
		double tw1 = tw1R();
		double tw2 = tw2R();
		double coorTerm = paddleY();
		double paddleTerm = (1 - (tw2 - tw1*padNum)) / paddleLength();
		double newTw0 = tw0 + tw0*coorTerm*paddleTerm;
		
//		System.out.println("TWCorrR");
//		System.out.println("Panel "+this.getDescriptor().getLayer());
//		System.out.println("padNum "+padNum);
//		System.out.println("lamR "+lamR());
//		System.out.println("tw1R "+tw1R());
//		System.out.println("tw2R "+tw2R());
//		System.out.println("paddleLength "+paddleLength());
//		System.out.println("paddleY "+paddleY());
//		System.out.println("coorTerm "+coorTerm);
//		System.out.println("paddleTerm "+paddleTerm);
//		System.out.println("new tw0 "+newTw0);
		
		return newTw0 / (Math.pow(ADCR,0.5));
	}			
	
	public double deltaTLeft(double offset) {

		double lr = leftRightAdjustment();

		double beta = 1.0;
		if (BETA != 0.0) {
			beta = BETA;
		}

		double dtL = tdcToTime(TDCL) - (lr/2) + rfpad() 
				- ((0.5*paddleLength() + paddleY())/this.veff())
				- (PATH_LENGTH/(beta*29.98)) - vertexCorr()
				- this.RF_TIME;

		// subtract the correction based on previous calibration values
		dtL = dtL - TWCorrL();
		dtL = dtL + offset;
		double bb = TOFCalibrationEngine.BEAM_BUCKET;
		dtL = (dtL+(1000*bb) + (0.5*bb))%bb - 0.5*bb;

		return dtL;
	}

	public double deltaTRight(double offset) {

		double lr = leftRightAdjustment();

		double beta = 1.0;
		if (BETA != 0.0) {
			beta = BETA;
		}

		double dtR = tdcToTime(TDCR) + (lr/2) + rfpad()
				- ((0.5*paddleLength() - paddleY())/this.veff())
				- (PATH_LENGTH/(beta*29.98)) - vertexCorr()
				- this.RF_TIME;

		// subtract the correction based on previous calibration values
		dtR = dtR - TWCorrR();
		dtR = dtR + offset;
		double bb = TOFCalibrationEngine.BEAM_BUCKET;
		dtR = (dtR+(1000*bb) + (0.5*bb))%bb - 0.5*bb;

		return dtR;
	}

	public double paddleLength() {

		double len = 0.0;

		if (tof=="FTOF") {
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
		}
		else {
			//len = 100.0;
			len = 88.05;
		}

		return len;

	}

	public double leftRight() {
		return (timeLeftAfterTW() - timeRightAfterTW());
	}

	public double timeLeftAfterTW() {
		if (tof=="FTOF") {
			//return tdcToTime(TDCL) - (lamL() / Math.pow(ADCL, 0.5));
			return tdcToTime(TDCL) - TWCorrL();
		}
		else {
			return tdcToTime(TDCL);
		}
	}

	public double timeRightAfterTW() {
		if (tof=="FTOF") {
			//return tdcToTime(TDCR) - (lamR() / Math.pow(ADCR, 0.5));
			return tdcToTime(TDCR) - TWCorrR();
		}
		else {
			return tdcToTime(TDCR);
		}
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
		//System.out.println("timeL "+timeL);
		double timeR = timeRightAfterTW();
		//System.out.println("timeR "+timeR);
		//System.out.println("leftRightAdjustment "+leftRightAdjustment());
		return (timeL - timeR - leftRightAdjustment()) / 2;
	}

	public double leftRightAdjustment() {

		double lr = 0.0;

		if (tof == "FTOF") {
			lr = TOFCalibrationEngine.leftRightValues.getDoubleValue("left_right", 
					desc.getSector(), desc.getLayer(), desc.getComponent());
			//System.out.println("lr "+desc.getSector()+desc.getLayer()+desc.getComponent()+" "+lr);

		} else {
			lr = CTOFCalibrationEngine.leftRightValues.getDoubleValue("upstream_downstream", 
					desc.getSector(), desc.getLayer(), desc.getComponent());
			//lr = -25.0;
		}

		return lr;
	}

	public double lamL() {
		double lamL = 0.0;
		if (tof == "FTOF") {
			lamL = TOFCalibrationEngine.timeWalkValues.getDoubleValue("tw0_left",
					desc.getSector(),desc.getLayer(),desc.getComponent());
		}
		return lamL;			
	}

	public double tw1L() {
		double tw1L = 0.0;
		if (tof == "FTOF") {
			tw1L = TOFCalibrationEngine.timeWalkValues.getDoubleValue("tw1_left",
					desc.getSector(),desc.getLayer(),desc.getComponent());
		}
		return tw1L;			
	}
	
	public double tw2L() {
		double tw2L = 0.0;
		if (tof == "FTOF") {
			tw2L = TOFCalibrationEngine.timeWalkValues.getDoubleValue("tw2_left",
					desc.getSector(),desc.getLayer(),desc.getComponent());
		}
		return tw2L;			
	}	

	public double lamR() {
		double lamR = 0.0;
		if (tof == "FTOF") {
			lamR = TOFCalibrationEngine.timeWalkValues.getDoubleValue("tw0_right",
					desc.getSector(),desc.getLayer(),desc.getComponent());
		}
		return lamR;			
	}

	public double tw1R() {
		double tw1R = 0.0;
		if (tof == "FTOF") {
			tw1R = TOFCalibrationEngine.timeWalkValues.getDoubleValue("tw1_right",
					desc.getSector(),desc.getLayer(),desc.getComponent());
		}
		return tw1R;			
	}
	
	public double tw2R() {
		double tw2R = 0.0;
		if (tof == "FTOF") {
			tw2R = TOFCalibrationEngine.timeWalkValues.getDoubleValue("tw2_right",
					desc.getSector(),desc.getLayer(),desc.getComponent());
		}
		return tw2R;			
	}	

	public double position() {

		double pos = halfTimeDiff() * veff();
		//System.out.println("position "+pos);
		//System.out.println("veff "+veff());
		return halfTimeDiff() * veff();
	}

	public double paddleY() {

		double y = 0.0;
		if (tof=="FTOF") {
			int sector = desc.getSector();
			double rotation = Math.toRadians((sector - 1) * 60);
			y= YPOS * Math.cos(rotation) - XPOS * Math.sin(rotation);
		}
		else {
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
		double minP = 1.0;
		if (tof=="FTOF") {
			maxRcs = TOFCalibration.maxRcs;
			minV = TOFCalibration.minV;
			maxV = TOFCalibration.maxV;
			minP = TOFCalibration.minP;
		}
		else {
			maxRcs = CTOFCalibration.maxRcs;
			minV = CTOFCalibration.minV;
			maxV = CTOFCalibration.maxV;
			minP = CTOFCalibration.minP;
		}


		return (trackFound() && 
				TRACK_REDCHI2 < maxRcs &&		
				VERTEX_Z > minV &&
				VERTEX_Z < maxV &&
				P > minP &&
				chargeMatch()); 
	}	

	public boolean chargeMatch() {

		int trackCharge = 0;
		if (tof=="FTOF") {
			trackCharge = TOFCalibration.trackCharge;
		}
		else {
			trackCharge = CTOFCalibration.trackCharge;
		}		

		return (trackCharge == TOFCalibration.TRACK_BOTH ||
				trackCharge == TOFCalibration.TRACK_NEG && CHARGE == -1 ||
				trackCharge == TOFCalibration.TRACK_POS && CHARGE == 1);
	}

	public double zPosCTOF() {
		return ZPOS - CTOFCalibration.ctofCenter;
	}

	public DetectorDescriptor getDescriptor() {
		return this.desc;
	}

	public String toString() {
		return "S " + desc.getSector() + " L " + desc.getLayer() + " C "
				+ desc.getComponent() + " ADCR " + ADCR + " ADCL " + ADCL
				+ " TDCR " + TDCR + " TDCL " + TDCL + " Geometric Mean "
				+ geometricMean() + " Log ratio " + logRatio();
	}

	public void show() {
		System.out.println("");
		System.out.println("S " + desc.getSector() + " L " + desc.getLayer() + " C "
				+ desc.getComponent() + " ADCR " + ADCR + " ADCL " + ADCL
				+ " TDCR " + TDCR + " TDCL " + TDCL);
		System.out.println("XPOS "+XPOS+" YPOS "+YPOS+" ZPOS "+ZPOS+" PATH_LENGTH "+PATH_LENGTH+" TRACK_ID "+TRACK_ID);
		System.out.println("BETA "+BETA+" P "+P+" RF_TIME "+RF_TIME+" RECON_TIME "+RECON_TIME);
		System.out.println("VERTEX_Z "+VERTEX_Z+" TRACK_REDCHI2 "+TRACK_REDCHI2+" CHARGE "+CHARGE+" TRIGGER_BIT "+TRIGGER_BIT);
		System.out.println("goodTrackFound "+goodTrackFound()+" chargeMatch "+chargeMatch());
		System.out.println("refTime "+refTime()+" startTime "+startTime()+" averageHitTime "+p2pAverageHitTime()+" vertexCorr "+vertexCorr());
		System.out.println("tofTimeRFCorr "+tofTimeRFCorr()+" startTimeRFCorr "+startTimeRFCorr()+" startTimeP2PCorr "+startTimeP2PCorr());
		System.out.println("rfpad "+rfpad()+" p2p "+p2p()+" lamL "+lamL()+" tw1L "+tw1L()+" tw2L "+tw2L()+" lamR "+lamR()+" tw1R "+tw1R()+" tw2R "+tw2R()+" LR "+leftRightAdjustment()+" veff "+veff());
		System.out.println("paddleLength "+paddleLength()+" paddleY "+paddleY());
		System.out.println("timeLeftAfterTW "+timeLeftAfterTW()+" timeRightAfterTW "+timeRightAfterTW());
		System.out.println("deltaTLeft "+this.deltaTLeft(0.0)+ " deltaTRight "+this.deltaTRight(0.0));

	}
	
	public void showTWCalc() {
		
		System.out.println("");
		System.out.println("Sector "+desc.getSector()+" Layer "+desc.getLayer()+" paddleNumber "+desc.getComponent());
		System.out.println("ADCL="+ADCL+" ADCR="+ADCR+" TDCL="+TDCL+" TDCR="+TDCR);
		System.out.println("paddleLength="+paddleLength()+" Hit posn (paddle y)="+paddleY());
		System.out.println("");
		System.out.println("TW0 (left)="+lamL()+" TW1 (left)="+tw1L()+" TW2 (left)="+tw2L());
		System.out.println("TW0 (right)="+lamR()+" TW1 (right)="+tw1R()+" TW2 (right)="+tw2R());
		System.out.println("");

		if (TOFCalibration.twMethod == TOFCalibration.TW_POS_DEP) {
			System.out.println("Position dependent method");
			System.out.println("LEFT");
			System.out.println("coordinate term = paddle y="+paddleY());
			double padTerm = (1 - (tw2L() - tw1L()*desc.getComponent())) / paddleLength();
			System.out.println("paddle term = (1 - (TW2L - TW1L*paddleNumber)) / paddleLength ="+padTerm);
			double newtw0 = lamL() - lamL()*paddleY()*padTerm;
			System.out.println("New TW0 Left = tw0L - tw0L*coorTerm*paddleTerm="+newtw0);
			System.out.println("TW left correction=new TW0/sqrt(ADCL)="+posDepTWCorrL());
			
			System.out.println("RIGHT");
			System.out.println("coordinate term = paddle y="+paddleY());
			padTerm = (1 - (tw2R() - tw1R()*desc.getComponent())) / paddleLength();
			System.out.println("paddle term = (1 - (TW2R - TW1R*paddleNumber)) / paddleLength ="+padTerm);
			newtw0 = lamR() + lamR()*paddleY()*padTerm;
			System.out.println("New TW0 Right = tw0R + tw0R*coorTerm*paddleTerm="+newtw0);
			System.out.println("TW right correction=new TW0/sqrt(ADCR)="+posDepTWCorrR());
		}
		else {
			System.out.println("Position independent method");
			System.out.println("TW left correction=TW0/sqrt(ADCL)="+posIndepTWCorrL());
			System.out.println("TW right correction=TW0/sqrt(ADCR)="+posIndepTWCorrR());
		}
		System.out.println("");
		System.out.println("timeLeftAfterTW=TDCL*0.02345 - TW left correction="+timeLeftAfterTW());
		System.out.println("timeRightAfterTW=TDCR*0.02345 - TW right correction="+timeRightAfterTW());
		
		double lr = leftRightAdjustment();
		double tL = timeLeftAfterTW() - (lr/2) 
		- ((0.5*paddleLength() + paddleY())/this.veff());
		double tR = timeRightAfterTW() + (lr/2)
				- ((0.5*paddleLength() - paddleY())/this.veff());
		System.out.println("lr="+leftRightAdjustment()+" veff="+veff());
		System.out.println("timeLeft = timeLeftAfterTW - (lr/2) - ((0.5*paddleLength + paddleY))/veff)="+tL);
		System.out.println("timeRight = timeRightAfterTW + (lr/2) - ((0.5*paddleLength - paddleY)/veff="+tR);
		System.out.println("");
		System.out.println("Path length ="+PATH_LENGTH+" beta="+BETA);
		System.out.println("AverageHitTime=(timeLeft+timeRight)/2.0="+p2pAverageHitTime());
		System.out.println("Start time=AverageHitTime - (PATH_LENGTH/(beta*29.98))="+startTime());
		System.out.println("RF time="+RF_TIME);
		System.out.println("");
		System.out.println("delta T =RF time - startTime="+refTime());
		System.out.println("RF calib value="+rfpad());
		System.out.println("delta T RF corrected ="+refTimeCorr());
		
		
	}
}