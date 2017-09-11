package org.jlab.calib.temp;

public 	class DetectorLocation {
	int[] SLC = new int[3];
	
	public DetectorLocation(int[] SecLayComp) {
		SLC = SecLayComp;
	}

	@Override
    public int hashCode() {
		int hc = this.SLC[0]*10000 + this.SLC[1]*1000 + this.SLC[2];
		return hc;
    }
	@Override
    public boolean equals(Object obj){
        if (obj instanceof DetectorLocation) {
        	DetectorLocation loc = (DetectorLocation) obj;
            return (loc.SLC[0] == this.SLC[0] && loc.SLC[1] == this.SLC[1] && loc.SLC[2] == this.SLC[2]);
        } else {
            return false;
        }
    }

}