package org.jlab.calib.temp;

import org.jlab.groot.data.H1F;
//import org.jlab.groot.group.DataGroup;
import org.jlab.calib.temp.DataGroup;
import org.jlab.groot.math.F1D;

public class TestDatagroup {

	public static void main(String[] args) {
		H1F geoMeanHist = new H1F("geomean","geomean", 
				100, 0.0, 3000.0);
		//geoMeanHist.setName("geomean");
		System.out.println(geoMeanHist.getName());
		DataGroup dg = new DataGroup(1,1);
		dg.addDataSet(geoMeanHist, 0);
		
		H1F h = dg.getH1F("geomean");
		System.out.println(h.getName());
		//dg.getH1F("geomean").setTitleX("ADC geometric mean");
		
		//F1D gmFunc = new F1D("gmFunc", "[amp]*landau(x,[mean],[sigma]) +[exp_amp]*exp([p]*x)",
		//		0.0, 3000.0);
		
		//dg.addDataSet(gmFunc, 0);
		//dg.getF1D("gmFunc").setRange(200.0, 1500.0);
	}

}
