package weka.classifiers.trees.j48;

import weka.classifiers.trees.j48.ClassifierTree;
import weka.core.Instances;

public class getAttributeFromClassifierTree {

	public static Instances getAttributeFromClassifierTree(ClassifierTree ct) throws Exception {
		return ct.m_train;
	  } 
	  
}
