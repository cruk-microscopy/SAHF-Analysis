package uk.ac.cam.cruk.mnlab;

import java.util.ArrayList;
import java.util.Arrays;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.process.ImageStatistics;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.SerializationHelper;

import weka.classifiers.Classifier;
import weka.classifiers.trees.getAttributeFromJ48;
import weka.classifiers.trees.getAttributeFromRandomForest;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;

import weka.classifiers.functions.MultilayerPerceptron;

public class WekaUtility {
	
	// currently only support random forest and J48
	public static final String[] classifierTypes = {"Random Forest", "J4.5 decision tree", "Neural Network"};
	
	// currently don't support multi-channel images with number of channel more than 9
	public static final String[] channelFeatureString = {"C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8", "C9"};
	public static final String[] localizationFeatureString = {"All", "Center", "Edge", "Whole_Image"};
	public static final String[] statisticalFeatureString = {"Mean", "StdDev", "CV", "Skewness", "Kurtosis", "Entropy"};
	
	public static final String sepLabel = "_";


	/*
	 * Functions to extract information and/or properties from built classifier
	 */
	// Weka way to load classifier from file
	public static Classifier getClassifier (String modelPath) throws Exception {
		return (Classifier) SerializationHelper.read(modelPath);
	}
	

	// get classifier type from classifier: only support Random Forest and J48 so far (J48 hasn't been tested)
	public static String getClassifierTypeFromModel (Classifier cls) {
		if (cls instanceof RandomForest) return "Random Forest";
		if (cls instanceof J48) return "J4.5 decision tree";
		if (cls instanceof MultilayerPerceptron) return "Neural Network";
		return "Unknown type";
	}
	// get classes name from classifier: Low, High, Intermediate and so on (case sensitive)
	public static ArrayList<String> getClassAttributeFromModel (Classifier cls) throws Exception {
		Instances data = null;
		if (cls instanceof RandomForest) {
			RandomForest rf = (RandomForest) cls;
			data = getAttributeFromRandomForest.getAttributeFromRandomForest(rf);	//hacky way to get protected member m_data from weka.classifier.meta.bagging...
		} else if (cls instanceof J48) {
			J48 j48 = (J48) cls;
			data = getAttributeFromJ48.getAttributeFromJ48(j48);	//hacky way to get protected member m_train from weka.classifier.trees.j48.ClassifierTree...
		} else if (cls instanceof MultilayerPerceptron) {
			return getAttributeFromMLP(cls, "class");
		} else {
			return null;
		}
		int numClasses = data.numClasses();
		ArrayList<String> classAttributes = new ArrayList<String>();
		for (int i=0; i<numClasses; i++) {
			classAttributes.add(data.classAttribute().value(i));
		}
		return classAttributes;
	}
	// get feature attributes from classifier: e.g: C1_Center_StdDev, C2_Edge_Entropy
	public static ArrayList<String> getFeatureAttributeFromModel (Classifier cls) throws Exception {
		Instances data = null;
		if (cls instanceof RandomForest) {
			RandomForest rf = (RandomForest) cls;
			data = getAttributeFromRandomForest.getAttributeFromRandomForest(rf);	//hacky way to get protected member m_data from weka.classifier.meta.bagging...
		} else if (cls instanceof J48) {
			J48 j48 = (J48) cls;
			data = getAttributeFromJ48.getAttributeFromJ48(j48);	//hacky way to get protected member m_train from weka.classifier.trees.j48.ClassifierTree...
		} else if (cls instanceof MultilayerPerceptron) {
			return getAttributeFromMLP(cls, "feature");
		} else {
			return null;
		}
		int numAttributes = data.numAttributes() - 1;
		ArrayList<String> featureAttributes = new ArrayList<String>();
		for (int i=0; i<numAttributes; i++) {
			featureAttributes.add(data.attribute(i).name());
		}
		return featureAttributes;
	}
	// get number of training instances(data) from classifier: !!!DOESN"T WORK SO FAR!!! (probably impossible: i.e.: saved classifier doesn't contain this information)
	public static int getNumInstanceFromModel (Classifier cls) throws Exception {
		Instances data = null;
		if (cls instanceof RandomForest) {
			RandomForest rf = (RandomForest) cls;
			data = getAttributeFromRandomForest.getAttributeFromRandomForest(rf);	//hacky way to get protected member m_data from weka.classifier.meta.bagging...
		} else if (cls instanceof J48) {
			J48 j48 = (J48) cls;
			data = getAttributeFromJ48.getAttributeFromJ48(j48);	//hacky way to get protected member m_train from weka.classifier.trees.j48.ClassifierTree...
		} else if (cls instanceof MultilayerPerceptron) {
			return 0;
		} else {
			return 0;
		}
		return data.numInstances();
	}
	// get channel feature from classifier: List<0,1,2,...> from {"C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8", "C9"}
	public static ArrayList<Integer> getChannelFeaturesFromModel (Classifier cls) throws Exception {//  channel indices will be returned as list
		ArrayList<String> featureAttributes = getFeatureAttributeFromModel(cls);
		ArrayList<Integer> chlFeatures = new ArrayList<Integer>();
		for (String feature : featureAttributes) {
			for (int i=0; i<channelFeatureString.length; i++) {
				if (feature.contains(channelFeatureString[i]) && !chlFeatures.contains(i))
					chlFeatures.add(i);
			}
		}
		if (chlFeatures.size()==0)	chlFeatures.add(0);	// if channel string does not exist in feature attribute, add channel 1 as default
		return chlFeatures;
	}
	// get localization feature from classifier: List <0,1,2> from {"Whole", "Center", "Edge"}
	public static ArrayList<Integer> getLocalizationFeatureFromModel (	// if not from the list, then take the whole image as ROI
			Classifier cls
			) throws Exception {
		ArrayList<String> featureAttributes = getFeatureAttributeFromModel(cls);
		ArrayList<Integer> locaFeatures = new ArrayList<Integer>();		
		for (String feature : featureAttributes) {
			for (int i=0; i<localizationFeatureString.length; i++) {
				if (feature.contains(localizationFeatureString[i]) && !locaFeatures.contains(i))
					locaFeatures.add(i);
			}
		}
		return locaFeatures;
	}
	// get statistical feature from classifier: List<0,1,2,3,4,5> from {"Mean", "StdDev", "CV", "Skewness", "Kurtosis", "Entropy"}
	public static ArrayList<Integer> getStatisticalFeatureFromModel (
			Classifier cls
			) throws Exception {
		ArrayList<String> featureAttributes = getFeatureAttributeFromModel(cls);
		ArrayList<Integer> statsFeatures = new ArrayList<Integer>();		
		for (String feature : featureAttributes) {
			for (int i=0; i<statisticalFeatureString.length; i++) {
				if (feature.contains(statisticalFeatureString[i]) && !statsFeatures.contains(i))
					statsFeatures.add(i);
			}
		}
		return statsFeatures;
	}

	
	/*
	 * Functions to create training and testing data
	 */
	// function to create a new empty training data (Instances) with all feature attributes
	public static Instances createEmptyData (
			ArrayList<String> objectClasses,
			ArrayList<Integer> channelFeatures,	// 0 based
			ArrayList<Integer> localizationFeatures,	// if 
			ArrayList<Integer> statisticalFeatures,
			int numData
			) {
		 // set up attributes
	    ArrayList<Attribute> atts = new ArrayList<Attribute>();	    
	    for (Integer channel : channelFeatures) {
	    	for (Integer localizationFeature : localizationFeatures) {
		    	for (Integer statisticFeature : statisticalFeatures) {
		    		String featureName = 
		    				channelFeatureString[channel] + sepLabel
		    				+ localizationFeatureString[localizationFeature] + sepLabel
		    				+ statisticalFeatureString[statisticFeature];
		    		atts.add(new Attribute(featureName));
		    	}
	    	}
	    }
		atts.add(new Attribute("class", objectClasses));
	    // create empty Instances object
		Instances data = new Instances("ObjectSegmentation", atts, numData);
		return data;
	}
	// function to fill data with new (Dense)Instance, which takes value from image
	public static void addNewInstanceToData (
			ImagePlus imp, // image stack contains training samples of one class (classLabel)
			int classLabel,	// if classLabel is -1, then it's a testing data (instance)
			Instances data,
			ArrayList<Integer> channelFeatures,	// 0 based
			ArrayList<Integer> localizationFeatures,
			ArrayList<Integer> statisticalFeatures
			) {
		// parse ROIs from attributes, parse input image stack, get ROIs
		Overlay overlay = imp.getOverlay();
		if (overlay==null && localizationFeatures.size()!=0) {
			System.out.println(" sample image " + imp.getTitle() + " doesn't contain any Overlay ROI!");
			return;
		}
		Roi[] overlayRois = overlay.toArray();
		
		int numSample = imp.getNSlices();

		for (int z=0; z<numSample; z++) {
			double[] vals = new double[data.numAttributes()];
			int idx = 0;
			for (Integer c : channelFeatures) {
				imp.setPositionWithoutUpdate(c+1, z+1, 1);	// currently don't support multiple time frames
				for (Integer localizationFeature : localizationFeatures) {
					Roi currentRoi = null;
					switch (localizationFeature) {
						case 0:
							currentRoi = getRoi(overlayRois, z+1, localizationFeatureString[0].toLowerCase());
							break;
						case 1:
							currentRoi = getRoi(overlayRois, z+1, localizationFeatureString[1].toLowerCase());
							break;
						case 2:
							currentRoi = getRoi(overlayRois, z+1, localizationFeatureString[2].toLowerCase());
							break;
					}
					imp.setRoi(currentRoi, false);
					for (Integer statisticalFeature : statisticalFeatures) {
						switch (statisticalFeature) {
							case 0:
								vals[idx] = imp.getRawStatistics().mean;
								break;
							case 1:
								vals[idx] = imp.getRawStatistics().stdDev;
								break;
							case 2:
								vals[idx] = imp.getRawStatistics().stdDev/imp.getRawStatistics().mean;
								break;
							case 3:
								vals[idx] = imp.getAllStatistics().skewness;
								break;
							case 4:
								vals[idx] = imp.getAllStatistics().kurtosis;
								break;
							case 5:
								vals[idx] = getEntropy(imp, currentRoi);
								break;
							default:
								continue;
						}
						idx++;
					}
			    }
			}
			if (classLabel==-1) vals[vals.length-1] = Utils.missingValue();
			else vals[vals.length-1] = classLabel;
			
			data.add(new DenseInstance(1.0, vals)); // weight by default is 1.0
		}
	}
	/*
	// function to create training instance/data (old function)
	public static Instances createTrainingInstance (
			ImagePlus imp,
			Roi roiAll,
			Roi roiCenter,
			Roi roiEdge,
			ArrayList<String> objectClasses,
			ArrayList<Integer> channelFeatures,	// 0 based
			ArrayList<Integer> localizationFeatures,
			ArrayList<Integer> statisticalFeatures,
			int classLabel
			) {
	    // 1. set up attributes
	    ArrayList<Attribute>	atts = new ArrayList<Attribute>();	    
	    for (Integer channel : channelFeatures) {
	    	String featureName = channelFeatureString[channel] + sepLabel;
	    	for (Integer localizationFeature : localizationFeatures) {
	    		featureName += (localizationFeatureString[localizationFeature] + sepLabel);
		    	for (Integer statisticFeature : statisticalFeatures) {
		    		featureName += statisticalFeatureString[statisticFeature];
		    		atts.add(new Attribute(featureName));
		    	}
	    	}
	    }
		atts.add(new Attribute("class", objectClasses));
	    // 2. create Instances object, populate with value
		Instances data = new Instances("ObjectSegmentation", atts, 0);
		double[] vals = new double[data.numAttributes()];
		int idx = 0;
		for (Integer c : channelFeatures) {
			imp.setC(c+1);
			for (Integer localizationFeature : localizationFeatures) {
				switch (localizationFeature) {
					case 0:
						imp.setRoi(roiAll, false);
						break;
					case 1:
						imp.setRoi(roiCenter, false);
						break;
					case 2:
						imp.setRoi(roiEdge, false);
						break;
				}
				for (Integer statisticalFeature : statisticalFeatures) {
					switch (statisticalFeature) {
						case 0:
							vals[idx] = imp.getRawStatistics().mean;
							break;
						case 1:
							vals[idx] = imp.getRawStatistics().stdDev;
							break;
						case 2:
							vals[idx] = imp.getRawStatistics().stdDev/imp.getRawStatistics().mean;
							break;
						case 3:
							vals[idx] = imp.getAllStatistics().skewness;
							break;
						case 4:
							vals[idx] = imp.getAllStatistics().kurtosis;
							break;
						case 5:
							vals[idx] = getEntropy(imp, roiAll);
						default:
							continue;
					}
					idx++;
				}
		    }
		}
		if (classLabel==-1) {
			vals[vals.length-1] = Utils.missingValue();
		} else {
			vals[vals.length-1] = classLabel;
		}
		data.add(new DenseInstance(1.0, vals));
		return data;
	}
	public static Instances createTestingInstance (
			ImagePlus imp,
			Roi roiAll,
			Roi roiCenter,
			Roi roiEdge,
			ArrayList<String> objectClasses,
			ArrayList<Integer> channelFeatures,	// 0 based
			ArrayList<Integer> localizationFeatures,
			ArrayList<Integer> statisticalFeatures
			) {
	    // 1. set up attributes
	    ArrayList<Attribute>	atts = new ArrayList<Attribute>();	    
	    for (Integer channel : channelFeatures) {
	    	String featureName = channelFeatureString[channel] + sepLabel;
	    	for (Integer localizationFeature : localizationFeatures) {
	    		featureName += (localizationFeatureString[localizationFeature] + sepLabel);
		    	for (Integer statisticFeature : statisticalFeatures) {
		    		featureName += statisticalFeatureString[statisticFeature];
		    		atts.add(new Attribute(featureName));
		    	}
	    	}
	    }
		atts.add(new Attribute("class", objectClasses));
	    // 2. create Instances object, populate with value
		Instances data = new Instances("ObjectSegmentation", atts, 0);
		double[] vals = new double[data.numAttributes()];
		int idx = 0;
		for (Integer c : channelFeatures) {
			imp.setC(c+1);
			for (Integer localizationFeature : localizationFeatures) {
				switch (localizationFeature) {
					case 0:
						imp.setRoi(roiAll, false);
						break;
					case 1:
						imp.setRoi(roiCenter, false);
						break;
					case 2:
						imp.setRoi(roiEdge, false);
						break;
				}
				for (Integer statisticalFeature : statisticalFeatures) {
					switch (statisticalFeature) {
						case 0:
							vals[idx] = imp.getRawStatistics().mean;
							break;
						case 1:
							vals[idx] = imp.getRawStatistics().stdDev;
							break;
						case 2:
							vals[idx] = imp.getRawStatistics().stdDev/imp.getRawStatistics().mean;
							break;
						case 3:
							vals[idx] = imp.getAllStatistics().skewness;
							break;
						case 4:
							vals[idx] = imp.getAllStatistics().kurtosis;
							break;
						case 5:
							vals[idx] = getEntropy(imp, roiAll);
						default:
							continue;
					}
					idx++;
				}
		    }
		}
		vals[vals.length-1] = Utils.missingValue();
		data.add(new DenseInstance(1.0, vals));
		return data;
	}
	*/
	
	/*
	 *  Miscellaneous Functions
	 */
	public static Roi getRoi (
			Roi[] roiArray,
			int zPosition,
			String roiName
			) {
		if (roiName.equals(localizationFeatureString[3])) return null;	// whole image, return null ROI
		for (Roi roi : roiArray) {
			if (roi.getZPosition()==zPosition && roi.getName().toLowerCase().equals(roiName))
				return roi;
		}
		return null;
	}
	public static double getEntropy(
			ImagePlus imp,
			Roi roi
			) {
		if (roi!=null && !RoiUtility.checkRoiContain(imp, roi)) {
			System.out.println("ROI is not area or not fully contained inside image: "+imp.getTitle());
			return Double.NaN;
		}
		
		double entropy=0;
		imp.setRoi(roi, false);
		ImageStatistics stats = imp.getProcessor().getStats();
		int totalPixels = stats.pixelCount;
		long[] hist = stats.getHistogram();
		for (int bin=0; bin<hist.length; bin++) {
			double p = (double)hist[bin] / totalPixels;
			if (p!=0) {
				entropy -= p * Math.log(p) / Math.log(2.0);	// use Shannon entropy definition
			}
		}
		return entropy;
	}
	public static double guessBackground(
			ImagePlus imp	// guess dark background from image/stack
			) {
		//imp.deleteRoi();
		if (imp.getProcessor().isBinary())	return 0.0;
		ImageStatistics stats = imp.getRawStatistics();
		double range = (stats.min+stats.max)/4;
		double min = stats.min;
		double mean = stats.mean;
		double mode = stats.mode;
		return (range<mean)?min:mode;
	}
	
	// hack the multi-layer perceptron attributes
	public static ArrayList<String> getAttributeFromMLP (Classifier cls, String feature) {
		if (!(cls instanceof MultilayerPerceptron))	{
			System.out.println("Classifier is not Multi-layer Perceptron!");
			return null;
		}
		String str = null;
		switch (feature) {
		case "class":
			str = "Class";
			break;
		case "feature":
			str = "Attrib";
			break;
		}
		MultilayerPerceptron mlp = (MultilayerPerceptron) cls;
		String all = mlp.toString();
		String[]lines = all.split("\n");
		ArrayList<String> featureString = new ArrayList<String>();
		if (str==null) return null;
		for (int row=0; row<lines.length; row++) {
			String[] parts = lines[row].split(" ");
			for (int col=0; col<parts.length-1; col++) {
				if (parts[col].equals(str) && !featureString.contains(parts[col+1])) {
					featureString.add(parts[col+1]);
				}
			}
		}		
		return featureString;
	}
}
