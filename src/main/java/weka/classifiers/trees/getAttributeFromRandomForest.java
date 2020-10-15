package weka.classifiers.trees;

import weka.classifiers.trees.RandomForest;
import weka.classifiers.meta.getAttributeFromBagging;
import weka.core.Instances;

public class getAttributeFromRandomForest {
	
	public static Instances getAttributeFromRandomForest(RandomForest rf) throws Exception { 
		return getAttributeFromBagging.getAttributeFromBagging(rf);
	  } 
	
	
	
	public static void main(String[] args) { 
    
	    try { 
	    	getAttributeFromRandomForest test = new getAttributeFromRandomForest(); 
	    } catch (Exception e) { 
	      e.printStackTrace(); 
	    } 
	} 

}
