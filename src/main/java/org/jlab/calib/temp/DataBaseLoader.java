package org.jlab.calib.temp;


import org.jlab.detector.base.DetectorType;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.geom.base.ConstantProvider;

/**
 *
 * @author gavalian
 */
public class DataBaseLoader {
    
    public static ConstantProvider getDriftChamberConstants(){
        
        DatabaseConstantProvider provider = new DatabaseConstantProvider("mysql://clas12reader@clasdb.jlab.org/clas12");
        provider.loadTable("/geometry/dc/dc");
        provider.loadTable("/geometry/dc/region");
        provider.loadTable("/geometry/dc/superlayer");
        provider.loadTable("/geometry/dc/layer");
        return provider;
    }
    
    public static ConstantProvider getCalorimeterConstants(){
        DatabaseConstantProvider provider = new DatabaseConstantProvider("mysql://clas12reader@clasdb.jlab.org/clas12");
        provider.loadTable("/geometry/pcal/pcal");
        provider.loadTable("/geometry/pcal/Uview");
        provider.loadTable("/geometry/pcal/Vview");
        provider.loadTable("/geometry/pcal/Wview");
        provider.loadTable("/geometry/ec/ec");
        provider.loadTable("/geometry/ec/uview");
        provider.loadTable("/geometry/ec/vview");
        provider.loadTable("/geometry/ec/wview");
        return provider;
    }
    
    
    public static ConstantProvider  getGeometryConstants(DetectorType type){
        return DataBaseLoader.getGeometryConstants(type, 10, "default");
    }
    
    public static ConstantProvider  getGeometryConstants(DetectorType type, int run){
        return DataBaseLoader.getGeometryConstants(type, run, "default");
    }
    
    public static ConstantProvider getCalibrationConstants(DetectorType type, int run){
        return DataBaseLoader.getCalibrationConstants(type, run, "default");
    }
    
    public static ConstantProvider getDetectorConstants(String type){
        return DataBaseLoader.getDetectorConstants(DetectorType.getType(type));
    }
    /**
     * This section returns for all detectors the known geometry constants
     * @param type
     * @return 
     */
    public static ConstantProvider getDetectorConstants(DetectorType type){
        int run = 10;
        String variation = "default";
        
        if(type==DetectorType.FTOF){
            DatabaseConstantProvider provider = new DatabaseConstantProvider(run,variation);
            provider.loadTable("/geometry/ftof/panel1a/paddles");        
            provider.loadTable("/geometry/ftof/panel1a/panel");
            provider.loadTable("/geometry/ftof/panel1b/paddles");
            provider.loadTable("/geometry/ftof/panel1b/panel");
            provider.loadTable("/geometry/ftof/panel2/paddles");
            provider.loadTable("/geometry/ftof/panel2/panel");
            provider.disconnect();
            return provider;
        }
        
        if(type==DetectorType.EC){
            DatabaseConstantProvider provider = new DatabaseConstantProvider(run,variation);
            provider.loadTable("/geometry/pcal/pcal");
            provider.loadTable("/geometry/pcal/Uview");
            provider.loadTable("/geometry/pcal/Vview");
            provider.loadTable("/geometry/pcal/Wview");
            provider.loadTable("/geometry/ec/ec");
            provider.loadTable("/geometry/ec/uview");
            provider.loadTable("/geometry/ec/vview");
            provider.loadTable("/geometry/ec/wview");
            provider.disconnect();
            return provider;
        }
        
        if(type==DetectorType.DC){
            DatabaseConstantProvider provider = new DatabaseConstantProvider(run,variation);
            provider.loadTable("/geometry/dc/dc");
            provider.loadTable("/geometry/dc/region");
            provider.loadTable("/geometry/dc/superlayer");
            provider.loadTable("/geometry/dc/layer");
            provider.disconnect();
            return provider;
        }
        
        if(type==DetectorType.CND){
            DatabaseConstantProvider provider = new DatabaseConstantProvider(run,variation);
            provider.loadTable("/geometry/cnd/cnd");
            provider.loadTable("/geometry/cnd/layer");
            provider.disconnect();
            return provider;
        }
        
        if(type==DetectorType.FTCAL){
            DatabaseConstantProvider provider = new DatabaseConstantProvider(run,variation);
            provider.loadTable("/geometry/ft/ftcal");
            provider.disconnect();
            return provider;
        }
        return null;
    }
    
    
    public static ConstantProvider getCalibrationConstants(DetectorType type, int run, String variation){
        if(type==DetectorType.FTOF){
            DatabaseConstantProvider provider = new DatabaseConstantProvider(run,variation);
            provider.loadTable("/calibration/ftof/attenuation");
            provider.loadTable("/calibration/ftof/effective_velocity");
            provider.loadTable("/calibration/ftof/gain_balance");
            provider.loadTable("/calibration/ftof/timing_offset");
            provider.disconnect();
            return provider;
        }
        return null;
    }
    
    
    public static ConstantProvider  getGeometryConstants(DetectorType type, int run, String variation){
        
        if(type==DetectorType.FTCAL){
            DatabaseConstantProvider provider = new DatabaseConstantProvider(run,variation);
            provider.loadTable("/geometry/ft/ftcal");
            provider.disconnect();
            return provider;
        }
        
        if(type==DetectorType.CND){
            DatabaseConstantProvider provider = new DatabaseConstantProvider(run,variation);
            provider.loadTable("/geometry/cnd/cnd");
            provider.loadTable("/geometry/cnd/layer");
            provider.disconnect();
            return provider;
        }
        
        if(type==DetectorType.DC){
            DatabaseConstantProvider provider = new DatabaseConstantProvider(run,variation);
            provider.loadTable("/geometry/dc/dc");
            provider.loadTable("/geometry/dc/region");
            provider.loadTable("/geometry/dc/superlayer");
            provider.loadTable("/geometry/dc/layer");
            provider.disconnect();
            return provider;
        }
        
        if(type==DetectorType.EC){
            DatabaseConstantProvider provider = new DatabaseConstantProvider(run,variation);
            provider.loadTable("/geometry/pcal/pcal");
            provider.loadTable("/geometry/pcal/Uview");
            provider.loadTable("/geometry/pcal/Vview");
            provider.loadTable("/geometry/pcal/Wview");
            provider.loadTable("/geometry/ec/ec");
            provider.loadTable("/geometry/ec/uview");
            provider.loadTable("/geometry/ec/vview");
            provider.loadTable("/geometry/ec/wview");
            provider.disconnect();
            return provider;
        }
        
        if(type==DetectorType.FTOF){
            DatabaseConstantProvider provider = new DatabaseConstantProvider(run,variation);
            provider.loadTable("/geometry/ftof/panel1a/paddles");        
            provider.loadTable("/geometry/ftof/panel1a/panel");
            provider.loadTable("/geometry/ftof/panel1b/paddles");
            provider.loadTable("/geometry/ftof/panel1b/panel");
            provider.loadTable("/geometry/ftof/panel2/paddles");
            provider.loadTable("/geometry/ftof/panel2/panel");
            provider.disconnect();
            return provider;
        }
        
        return null;
    }
    
    public static ConstantProvider getTimeOfFlightConstants(){
        DatabaseConstantProvider provider = new DatabaseConstantProvider("mysql://clas12reader@clasdb.jlab.org/clas12");
        provider.loadTable("/geometry/ftof/panel1a/paddles");        
        provider.loadTable("/geometry/ftof/panel1a/panel");
        provider.loadTable("/geometry/ftof/panel1b/paddles");
        provider.loadTable("/geometry/ftof/panel1b/panel");
        provider.loadTable("/geometry/ftof/panel2/paddles");
        provider.loadTable("/geometry/ftof/panel2/panel");
        return provider;
    }
    
    public static ConstantProvider getConstantsDC(int run, String variation){
        DatabaseConstantProvider provider = new DatabaseConstantProvider("mysql://clas12reader@clasdb.jlab.org/clas12");
        provider.loadTable("/geometry/dc/dc");
        provider.loadTable("/geometry/dc/region");
        provider.loadTable("/geometry/dc/superlayer");
        provider.loadTable("/geometry/dc/layer");
        return provider;
    }
    
    public static ConstantProvider getConstantsEC(){
        DatabaseConstantProvider provider = new DatabaseConstantProvider("mysql://clas12reader@clasdb.jlab.org/clas12");
        provider.loadTable("/geometry/pcal/pcal");
        provider.loadTable("/geometry/pcal/UView");
        provider.loadTable("/geometry/pcal/VView");
        provider.loadTable("/geometry/pcal/WView");
        provider.loadTable("/geometry/ec/ec");
        provider.loadTable("/geometry/ec/uview");
        provider.loadTable("/geometry/ec/vview");
        provider.loadTable("/geometry/ec/wview");
        return provider;
    }
     
    public static ConstantProvider getConstantsFTOF(){
        DatabaseConstantProvider provider = // new DatabaseConstantProvider(run,variation);
                new DatabaseConstantProvider("mysql://clas12reader@clasdb.jlab.org/clas12");
        provider.loadTable("/geometry/ftof/panel1a/paddles");        
        provider.loadTable("/geometry/ftof/panel1a/panel");
        provider.loadTable("/geometry/ftof/panel1b/paddles");
        provider.loadTable("/geometry/ftof/panel1b/panel");
        provider.loadTable("/geometry/ftof/panel2/paddles");
        provider.loadTable("/geometry/ftof/panel2/panel");
        return provider;
    }
    
    public static ConstantProvider getConstantsCND(){
        DatabaseConstantProvider provider = new DatabaseConstantProvider("mysql://clas12reader@clasdb.jlab.org/clas12");
        provider.loadTable("/geometry/cnd/cnd");
        provider.loadTable("/geometry/cnd/layer");
        return provider;
    }
    
    public static ConstantProvider getConstantsFTCAL(){
        DatabaseConstantProvider provider = new DatabaseConstantProvider("mysql://clas12reader@clasdb.jlab.org/clas12");
        provider.loadTable("/geometry/ft/ftcal");
        return provider;
    }

    public static ConstantProvider getConstantsBST(){
        DatabaseConstantProvider provider = new DatabaseConstantProvider("mysql://clas12reader@clasdb.jlab.org/clas12");
        provider.loadTable("/geometry/bst/region");
        provider.loadTable("/geometry/bst/sector");
        provider.loadTable("/geometry/bst/bst");
        return provider;
    }
    
}