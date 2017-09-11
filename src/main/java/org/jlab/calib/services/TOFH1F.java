package org.jlab.calib.services;

import org.jlab.groot.data.H1F;


public class TOFH1F extends H1F {

	public TOFH1F(String name, String title, int bins, double xMin, double xMax) {
		super(name, title, bins, xMin, xMax);
	}

	public void rebin(int nBinsCombine) {
		
		H1F histIn = this.histClone("Rebinned");
		int nBinsOrig = histIn.getAxis().getNBins();

		this.set(nBinsOrig/nBinsCombine, histIn.getAxis().min(), histIn.getAxis().max());
		
		int newBin = 0;

		for (int origBin=0; origBin<=nBinsOrig;) {

			double newBinCounts = 0;
			for (int i=0; i<nBinsCombine; i++) {
				newBinCounts = newBinCounts + histIn.getBinContent(origBin);
				origBin++;				
			}
			this.setBinContent(newBin, newBinCounts);
			newBin++;

		}
	}
	
	public int getEntriesTOF() {
		
		int n = 0;
		
		for (int i=0; i<getxAxis().getNBins(); i++) {
			n = (int) (n+this.getBinContent(i));
		}
		return n;
		
	}

}
