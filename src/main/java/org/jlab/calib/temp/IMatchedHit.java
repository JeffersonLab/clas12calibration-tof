package org.jlab.calib.temp;

import java.util.ArrayList;
import java.util.List;

public interface IMatchedHit {

	public String DetectorName();
	public List<BaseHit> MatchHits(ArrayList<BaseHit> ADCandTDCLists);
	
}
