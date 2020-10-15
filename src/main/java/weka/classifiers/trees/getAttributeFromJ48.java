package weka.classifiers.trees;

import weka.classifiers.trees.J48;
import weka.classifiers.trees.j48.getAttributeFromClassifierTree;
import weka.core.Instances;

public class getAttributeFromJ48 {

	public static Instances getAttributeFromJ48(J48 j48) throws Exception { 
		return getAttributeFromClassifierTree.getAttributeFromClassifierTree(j48.m_root);
	} 

	public static void main(String[] args) { 
    
	    try { 
	    	getAttributeFromJ48 test = new getAttributeFromJ48(); 
	    } catch (Exception e) { 
	      e.printStackTrace(); 
	    } 
	} 
}
