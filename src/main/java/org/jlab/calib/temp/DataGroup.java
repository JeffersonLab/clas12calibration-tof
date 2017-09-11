package org.jlab.calib.temp;

// Version of 29 Jul

//import java.util.ArrayList;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import org.jlab.groot.data.GraphErrors;
//import org.jlab.groot.data.H1F;
//import org.jlab.groot.data.H2F;
//import org.jlab.groot.data.IDataSet;
//import org.jlab.groot.math.F1D;
//
///**
// *
// * @author gavalian
// */
//public class DataGroup {
//    
//    Map<String,IDataSet> dataGroup      = new LinkedHashMap<String,IDataSet>();
//    Map<String,Integer>  dataGroupOrder = new LinkedHashMap<String,Integer>();
//    int  numberOfRows    = 1;
//    int  numberOfColumns = 1;
//    
//    private String dataGroupName = "generic";
//    
//    public DataGroup(){
//        
//    }
//    
//    public DataGroup(String name){
//        this.dataGroupName = name;
//    }
//    
//    public final void setName(String name){ this.dataGroupName = name;}
//    public String getName(){ return this.dataGroupName;}
//    
//    public DataGroup(int ncols, int nrows){
//        this.numberOfColumns = ncols;
//        this.numberOfRows    = nrows;
//    }
//    
//    public void addDataSet(IDataSet ds, int order){
//        if(dataGroup.containsKey(ds.getName())==true){
//            System.out.println("[warning] --> object with name " + ds.getName()
//            + " already exists..");
//            return;
//        }
//        
//        dataGroup.put(ds.getName(), ds);
//        dataGroupOrder.put(ds.getName(), order);        
//    }
//    
//    public int getColumns(){
//        return this.numberOfColumns;
//    }
//    
//    public int getRows(){
//        return this.numberOfRows;
//    }
//   
//    public IDataSet  getData(String name){
//        if(this.dataGroup.containsKey(name)==false){
//            System.out.println("[error] ---> requested data set does not exist");
//            return null;
//        }
//        
//        return this.dataGroup.get(name);
//    }
//    
//     public GraphErrors  getGraph(String name){
//        IDataSet ds = this.getData(name);
//        if(ds!=null){
//            if(ds instanceof GraphErrors){
//                return (GraphErrors) ds;
//            } else {
//                System.out.println("[error] ---> data set is " + 
//                        name + " not a Graph");
//            }
//        }
//        return null;
//    }
//    
//    public F1D  getF1D(String name){
//        IDataSet ds = this.getData(name);
//        if(ds!=null){
//            if(ds instanceof F1D){
//                return (F1D) ds;
//            } else {
//                System.out.println("[error] ---> data set is " + 
//                        name + " not an F1D");
//            }
//        }
//        return null;
//    }
//    
//    public H1F  getH1F(String name){
//        IDataSet ds = this.getData(name);
//        if(ds!=null){
//            if(ds instanceof H1F){
//                return (H1F) ds;
//            } else {
//                System.out.println("[error] ---> data set is " + 
//                        name + " not an H1F");
//            }
//        }
//        return null;
//    }
//    
//    public H2F  getH2F(String name){
//        IDataSet ds = this.getData(name);
//        if(ds!=null){
//            if(ds instanceof H2F){
//                return (H2F) ds;
//            } else {
//                System.out.println("[error] ---> data set is " + 
//                        name + " not an H2F");
//            }
//        }
//        return null;
//    }
//    
//    
//    
//    public List<IDataSet>  getData(int order){
//        List<IDataSet> dataList = new ArrayList<IDataSet>();
//        for(Map.Entry<String,Integer> entry : dataGroupOrder.entrySet()){
//            if(entry.getValue()==order){
//                dataList.add(dataGroup.get(entry.getKey()));
//            }
//        }
//        return dataList;
//    }
//}
//

// Version of 4 Dec with dataGroup.put(ds.getName(), ds); in addDataSet added back in

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.data.IDataSet;
import org.jlab.groot.math.F1D;

/**
 *
 * @author gavalian
 */
public class DataGroup extends org.jlab.groot.group.DataGroup {
    
    Map<String,IDataSet> dataGroup      = new LinkedHashMap<String,IDataSet>();
    Map<String,Integer>  dataGroupOrder = new LinkedHashMap<String,Integer>();
    
    Map<Integer, List<IDataSet>> dataGroupSets = new LinkedHashMap<Integer, List<IDataSet>>();
    
    int  numberOfRows    = 1;
    int  numberOfColumns = 1;
    
    private String dataGroupName = "generic";
    
    public DataGroup(){
        
    }
    
    public DataGroup(String name){
        this.dataGroupName = name;
    }
    
    //public final void setName(String name){ this.dataGroupName = name;}
    public String getName(){ return this.dataGroupName;}
    
    public DataGroup(String name, int ncols, int nrows){
        this.dataGroupName   = name;
        this.numberOfColumns = ncols;
        this.numberOfRows    = nrows;
    }
    
    public DataGroup(int ncols, int nrows){
        this.numberOfColumns = ncols;
        this.numberOfRows    = nrows;
    }
    
    public void addDataSet(IDataSet ds, int order){
        
//    	// version of 29 Jul -3 Oct
////    	if(dataGroup.containsKey(ds.getName())==true){
////            System.out.println("[warning] --> object with name " + ds.getName()
////            + " already exists..");
////            return;
////        }
////        
////        dataGroup.put(ds.getName(), ds);
////        dataGroupOrder.put(ds.getName(), order); 
    	
    	// Latest version (4 Oct)
    	
        if(dataGroupSets.containsKey(order)==false){
           dataGroupSets.put(order, new ArrayList<IDataSet>());
        }
        
        dataGroupSets.get(order).add(ds);
        dataGroup.put(ds.getName(), ds);  // *** need this line added back in
        /*
        if(dataGroup.containsKey(ds.getName())==true){
            System.out.println("[warning] --> object with name " + ds.getName()
            + " already exists..");
            return;
        }
        
        dataGroup.put(ds.getName(), ds);
        dataGroupOrder.put(ds.getName(), order); 
        */
    }
    
    public int getColumns(){
        return this.numberOfColumns;
    }
    
    public int getRows(){
        return this.numberOfRows;
    }
   
    public IDataSet  getData(String name){
        if(this.dataGroup.containsKey(name)==false){
            System.out.println("[error] ---> requested data set does not exist");
            return null;
        }        
        return this.dataGroup.get(name);
    }
    
     public GraphErrors  getGraph(String name){
        IDataSet ds = this.getData(name);
        if(ds!=null){
            if(ds instanceof GraphErrors){
                return (GraphErrors) ds;
            } else {
                System.out.println("[error] ---> data set is " + 
                        name + " not a Graph");
            }
        }
        return null;
    }
    
    public F1D  getF1D(String name){
        IDataSet ds = this.getData(name);
        if(ds!=null){
            if(ds instanceof F1D){
                return (F1D) ds;
            } else {
                System.out.println("[error] ---> data set is " + 
                        name + " not an F1D");
            }
        }
        return null;
    }
    
    public H1F  getH1F(String name){
        IDataSet ds = this.getData(name);
        if(ds!=null){
            if(ds instanceof H1F){
                return (H1F) ds;
            } else {
                System.out.println("[error] ---> data set is " + 
                        name + " not an H1F");
            }
        }
        return null;
    }
    
    public H2F  getH2F(String name){
        IDataSet ds = this.getData(name);
        if(ds!=null){
            if(ds instanceof H2F){
                return (H2F) ds;
            } else {
                System.out.println("[error] ---> data set is " + 
                        name + " not an H2F");
            }
        }
        return null;
    }
        
    public List<IDataSet>  getData(int order){
        if(dataGroupSets.containsKey(order)==false) 
            return new ArrayList<IDataSet>();
        return dataGroupSets.get(order);
        /*
        List<IDataSet> dataList = new ArrayList<IDataSet>();
        for(Map.Entry<String,Integer> entry : dataGroupOrder.entrySet()){
            if(entry.getValue()==order){
                dataList.add(dataGroup.get(entry.getKey()));
            }
        }
        return dataList;*/
    }
}