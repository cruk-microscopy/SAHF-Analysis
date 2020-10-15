package uk.ac.cam.cruk.mnlab;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets; 
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import org.scijava.prefs.DefaultPrefService;

import fiji.util.gui.GenericDialogPlus;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.RoiScaler;
import ij.plugin.Zoom;
import ij.plugin.frame.RoiManager;

import loci.common.services.ServiceException;
import loci.formats.FormatException;

import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.functions.MultilayerPerceptron;

import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ConverterUtils.DataSink;

public class classifierPanel {
	
	protected static final int lineWidth = 44;
	protected static final Color panelColor = new Color(204, 229, 255);
	protected static final Font textFont = new Font("Helvetica", Font.PLAIN, 12);
	protected static final Color fontColor = Color.BLACK;
	protected static final Font errorFont = new Font("Helvetica", Font.BOLD, 12);
	protected static final Color errorFontColor = Color.RED;
	protected static final Color textAreaColor = new Color(204, 229 , 255);
	protected static final Font panelTitleFont = new Font("Helvetica", Font.BOLD, 13);
	protected static final Color panelTitleColor = Color.BLUE;
	protected static final EmptyBorder border = new EmptyBorder(new Insets(5, 5, 5, 5));
	
	protected static final Dimension textAreaMax = new Dimension(260, 800);
	protected static final Dimension tablePreferred = new Dimension(260, 100);
	protected static final Dimension tableMax = new Dimension(260, 150);
	protected static final Dimension panelTitleMax = new Dimension(500, 30);
	protected static final Dimension panelMax = new Dimension(300, 200);
	protected static final Dimension panelMin = new Dimension(280, 200);
	protected static final Dimension buttonSize = new Dimension(90, 15);
	
	protected static ImagePlus source = null;
	protected static TrainingSamples trainingSamplesClassifierPanel;
	protected static Classifier cls = null;
	protected static String classifierFilePath = IJ.getDir("home");
	protected static final String[] classifierTypes = WekaUtility.classifierTypes;
	protected static int classifierType = 0;
	
	protected static double mlpLearningRate = 0.1;
	protected static double mlpMomentum = 0.2;
	protected static int mlpTrainingTime = 2000;
	protected static int mlpNumHiddenLayers = 3;
	
	protected static JTextArea classifierInfo;
	protected static final String noSource = "No source image!";
	protected static final String noClassifier = "No classifier!";
	protected static final String errorConfigure = "Classifier configuration wrong!";
	protected static final String errorTrain = "Fail to train classifier";
	protected static final String errorApply = "Fail to apply classifier";
	protected static final String errorCheck = "Fail to extract information from classifier";
	protected static final String errorLoad = "Fail to load classifier";
	protected static final String errorSave = "Fail to export classifier";
	
	protected static final String[] channelFeatureString = WekaUtility.channelFeatureString;
	protected static final String[] localizationFeatureString = WekaUtility.localizationFeatureString;
	protected static final String[] statisticalFeatureString = WekaUtility.statisticalFeatureString;
	protected static ArrayList<Integer> attrChannel = new ArrayList<Integer>();
	protected static ArrayList<Integer> attrLocalization = new ArrayList<Integer>();
	protected static ArrayList<Integer> attrStatistical = new ArrayList<Integer>();
	protected static boolean[] doChannel;
	protected static boolean[] doRoi;
	protected static boolean[] doStat;
	
	protected static Instances trainingData = null;
	
	protected static boolean[] showClassImage;	// display or not a certain class prediction image
	protected static double[] probabilityTreshold; // set probability threshold for each class
	protected static boolean autoProbThreshold;
	protected static HashMap<Integer, double[]> sliceWiseResult;	 // slice wise result: slice number : class prediction, probabilities...
	protected static ArrayList<ArrayList<double[]>> classWiseResult;	// class wise result: slice number in source, class prediction, probabilities...
	
	public void createPanel() {}
		
	/* Functions to create and configure the classifier panel
	 * It needs a text area to display classifier information
	 * It needs six buttons:
	 * 	configure, check
	 * 	train, apply
	 *  load, export
	 */
	public static void addClassifierPanelToFrame (
			Frame f,
			TrainingSamples trainingSamples
			) throws Exception {
		// parse current active image in Fiji
		//ImagePlus source = null;
		//if (WindowManager.getImageCount()!=0)
		//	source = IJ.getImage();
		//checkSourceImage(imp);
		source = trainingSamples.getCurrentSource();
		trainingSamplesClassifierPanel = trainingSamples;
		// create a parent panel for both title and content panels
		JPanel parentPanel = new JPanel();
		parentPanel.setBorder(border);
		parentPanel.setBackground(f.getBackground());
		parentPanel.setLayout(new BoxLayout(parentPanel, BoxLayout.Y_AXIS));
		//parentPanel.add(titlePanel, BorderLayout.NORTH);
		
		// create and configure the content panel 
		JPanel contentPanel = new JPanel();
		contentPanel.setBorder(border);
		contentPanel.setBackground(panelColor);
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		
		// title "Sample image and ROIs"
		JLabel title = new JLabel("Classifier");
		title.setFont(panelTitleFont);
		title.setForeground(panelTitleColor);
		contentPanel.add(title);
		title.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		//titlePanel.setMaximumSize(panelTitleMax);
		//titlePanel.setBackground(f.getBackground());
		//titlePanel.add(title);
		
		// display classifier information
		classifierInfo = new JTextArea();
		classifierInfo.setMaximumSize(textAreaMax);
		classifierInfo.setEditable(false);
		contentPanel.add(classifierInfo);
		classifierInfo.setAlignmentX(Component.CENTER_ALIGNMENT);

		// add button groups
		JPanel buttonPanel = new JPanel();
		JButton btnCfg = new JButton("configure");
		JButton btnCheck = new JButton("check");
		JButton btnTrain = new JButton("train");
		JButton btnApply = new JButton("apply");
		JButton btnLoad = new JButton("load");
		JButton btnExport = new JButton("export");
		btnCfg.setPreferredSize(buttonSize);
		// Use group layout for a JTextArea to display source image info
		// and 3 buttons horizontally aligned: refresh, load, resize
		GroupLayout buttonLayout = new GroupLayout(buttonPanel);
		buttonPanel.setLayout(buttonLayout);
		buttonLayout.setAutoCreateGaps(true);
		buttonLayout.setAutoCreateContainerGaps(true);

		buttonLayout.setHorizontalGroup(buttonLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
			 .addGroup(buttonLayout.createSequentialGroup()
			    .addComponent(btnCheck)
			    .addComponent(btnApply)
			    .addComponent(btnLoad))
			 .addGroup(buttonLayout.createSequentialGroup()
		        .addComponent(btnCfg)
		        .addComponent(btnTrain)
		        .addComponent(btnExport)));
			                
		buttonLayout.linkSize(SwingConstants.HORIZONTAL, btnCheck, btnCfg, btnApply, btnTrain, btnLoad, btnExport);	
		
		buttonLayout.setVerticalGroup(buttonLayout.createSequentialGroup()
			.addGroup(buttonLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addGroup(buttonLayout.createSequentialGroup()
			                .addComponent(btnCheck)
			                .addComponent(btnCfg))
					.addGroup(buttonLayout.createSequentialGroup()
			                .addComponent(btnApply)
			                .addComponent(btnTrain))
					.addGroup(buttonLayout.createSequentialGroup()
			                .addComponent(btnLoad)
			                .addComponent(btnExport))));
						
		buttonPanel.setBorder(border);
		buttonPanel.setBackground(panelColor);
		
		
		
		//buttonPanel.setMaximumSize(panelMax);
		// configure the JTextArea to display source image info
		//sourceInfo.setBackground(textAreaColor);
		
		// configure configuration button
		btnCfg.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) {configureClassifier(false);}
		});
		// configure check button
		btnCheck.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) {checkClassifier();}
		});
		// configure train button
		btnTrain.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) {trainClassifier();}
		});
		// configure apply button
		btnApply.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) {applyClassifier();}
		});
		// configure load button
		btnLoad.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) {loadClassifier();}
		});
		// configure export button
		btnExport.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) {exportClassifier(f);}
		});
		
		contentPanel.add(buttonPanel);
		buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		String clsInfo = getClassifierInfo();
		classifierInfo.setText(clsInfo);
		if (clsInfo.equals(noClassifier)) {
			classifierInfo.setFont(errorFont);
			classifierInfo.setForeground(errorFontColor);
		} else {
			classifierInfo.setFont(textFont);
			classifierInfo.setForeground(fontColor);
		}

		// add title and content panel to the parent panel, and finally add to plugin frame
		parentPanel.add(contentPanel);
		contentPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		//f.add(parentPanel, BorderLayout.SOUTH);
		f.add(parentPanel);
		f.pack();
	}

	/*
	 * Function group to configure, train, and apply classifier
	 */
	public static void configureClassifier(Boolean configDefaultSilent) {
		// get number of features: channel from source image, localization and statistics from pre-defined string array
		source = trainingSamplesClassifierPanel.getCurrentSource();
		final int numC = source==null ? 1 : source.getNChannels();
		final int numR = localizationFeatureString.length-1;	// ignore the last element, which is "whole image"
		final int numS = statisticalFeatureString.length;
		
		// make use of scijava parameter persistence storage
		DefaultPrefService prefs = new DefaultPrefService();
		classifierType = prefs.getInt(Integer.class, "SAHF-classifierType", classifierType);
		doChannel = new boolean[numC]; // 0 based channel index
		doRoi = new boolean[numR];
		doStat = new boolean[numS];
		for (int c=0; c<numC; c++)	
			doChannel[c] = prefs.getBoolean(Boolean.class, "SAHF-doChannel"+String.valueOf(c), doChannel[c]);
		for (int r=0; r<numR; r++)
			doRoi[r] = prefs.getBoolean(Boolean.class, "SAHF-doRoi"+String.valueOf(r), doRoi[r]);
		for (int s=0; s<numS; s++)
			doStat[s] = prefs.getBoolean(Boolean.class, "SAHF-doStat"+String.valueOf(s), doStat[s]);
		
		if (!configDefaultSilent) {
			// create user interface
			final Font highlightFont = new Font("Helvetica", Font.BOLD, 12);
			GenericDialogPlus gd = new GenericDialogPlus("SAHF classifier configuration");
			
			// show type information
			String classifierTypeMessage = "Classifier:";
			gd.setInsets(20,0,0);
			gd.addMessage(classifierTypeMessage, highlightFont, Color.blue); 
			gd.setInsets(0, 20, 0 );
			gd.addChoice("type:", classifierTypes, classifierTypes[classifierType]);
			// show class information, as defined by the sample panel and image stacks
			String classMessage = "Classes: ";
			gd.setInsets(20,0,0);
	    	gd.addMessage(classMessage, highlightFont, Color.blue);
			ArrayList<String> classNames = trainingSamplesClassifierPanel.getClassNames();
			gd.setInsets(0, 20, 10 );
			if (classNames==null || classNames.size()==0) gd.addMessage("No class defined!", highlightFont, Color.red);
			else {
				String classNameString = "";
				for (String name : classNames)
					classNameString += name + "\n";
				gd.addMessage(classNameString, highlightFont, Color.black);
			}
	    	// show feature information and configuration
			String locationFeatureMessage = "Feature:";
			gd.setInsets(0,0,0);
			gd.addMessage(locationFeatureMessage, highlightFont, Color.blue); 
			gd.setInsets(0, 20, 0 );
			String[] headingsChannel = {"Channel"};
			//String[] labelsChannel = channelString[numC];
			String[] labelsChannel = (String[])(Arrays.copyOfRange(channelFeatureString, 0, numC));
			//boolean[] statesChannel = new boolean[source.getNChannels()]; statesChannel[0] = true;
			gd.addCheckboxGroup(1, numC, labelsChannel, doChannel, headingsChannel);
			gd.setInsets(10, 20, 0 );
			String[] headingsLocalization = {"Localization"};
			String[] labelsLocalization = localizationFeatureString;
			//boolean[] statesLocalization = {doRoiWhole, doRoiEdge, doRoiCenter};
			gd.addCheckboxGroup(1, numR, labelsLocalization, doRoi, headingsLocalization);
			gd.setInsets(10, 20, 0 );
			String[] headingsStats = {"Statistics"};
			gd.addCheckboxGroup((int) Math.ceil(numS/3), 3, statisticalFeatureString, doStat, headingsStats);
	    	gd.showDialog();
	    	if (gd.wasCanceled()) return;
	    	classifierType = gd.getNextChoiceIndex();
	    	for (int i=0; i<numC; i++)
	    		doChannel[i] = gd.getNextBoolean();
	    	for (int i=0; i<numR; i++)
	    		doRoi[i] = gd.getNextBoolean();
	    	for (int i=0; i<numS; i++)
	    		doStat[i] = gd.getNextBoolean();
		}
    	
    	// parse new configuration to current feature list
    	for (int i=0; i<doChannel.length; i++) {
    		if (doChannel[i] && !attrChannel.contains(i)) attrChannel.add(i);
    		if (!doChannel[i] && attrChannel.contains(i)) attrChannel.remove(Integer.valueOf(i));
    	}
    	for (int i=0; i<doRoi.length; i++) {
    		if (doRoi[i] && !attrLocalization.contains(i)) attrLocalization.add(i);
    		if (!doRoi[i] && attrLocalization.contains(i)) attrLocalization.remove(Integer.valueOf(i));
    	}
    	for (int i=0; i<doStat.length; i++) {
    		if (doStat[i] && !attrStatistical.contains(i)) attrStatistical.add(i);
    		if (!doStat[i] && attrStatistical.contains(i)) attrStatistical.remove(Integer.valueOf(i));
    	}	
    	// check if channel feature or statistical feature are empty;
    	if (attrChannel.size()==0 || attrStatistical.size()==0) {
    		classifierInfo.setText(errorConfigure);
			classifierInfo.setFont(errorFont);
			classifierInfo.setForeground(errorFontColor);
			return;
    	}
    	// check if any ROI specified, if not, then take whole image as ROI
    	attrLocalization.remove(Integer.valueOf(3));
    	if (attrLocalization.size()==0) attrLocalization.add(3);
    	
    	//String x = "<html>W<sub>next</sub> = W + ΔW.</html>";
    	//String y = "<html>ΔW = -learningRate * gradient + momentum * ΔW<sub>previous</sub></html>";
    	
    	// additional configuration for multi-layer perceptron
    	if (classifierType==2) {
    		//classifierPanel temp = new classifierPanel();
    		//temp.new configureMLP();
    		//new Thread("configureMLP") {
    		//	public void run() {
	    		mlpLearningRate = prefs.getDouble(Integer.class, "SAHF-mlpLearningRate", mlpLearningRate);
	    		mlpMomentum = prefs.getDouble(Integer.class, "SAHF-mlpMomentum", mlpMomentum);
	    		mlpTrainingTime = prefs.getInt(Integer.class, "SAHF-mlpTrainingTime", mlpTrainingTime);
	    		mlpNumHiddenLayers = prefs.getInt(Integer.class, "SAHF-mlpNumHiddenLayers", mlpNumHiddenLayers);
		    	
	    		if (!configDefaultSilent) {
		    		
		    		GenericDialog gd2 = new GenericDialog("Neural Network Configuration");
		    		gd2.addSlider("Learning Rate:", 0, 1, mlpLearningRate, 0.1);
		    		gd2.addSlider("Momentum:", 0, 1, mlpMomentum, 0.1);
		    		gd2.addSlider("Number of Epoch:", 0, 10000, mlpTrainingTime, 100);
		    		gd2.addSlider("Number of HiddenLayer:", 0, 20, mlpNumHiddenLayers, 1);
		    		String html = "<html>"
		   				 +"<h2>Configuration of Neural Network (SAHF plugin)</h2>"
		   				 +" version: 1.4.0<br>"
		   				 +" date: 2019.09.17<br>"
		   				 +" author: Ziqiang Huang (Ziqiang.Huang@cruk.cam.ac.uk)<br><br>"
		   				 +"<h3>Usage:</h3>"
		   				 +"&nbsp;&nbsp;  The current implemented Neural Network is a Multilayer Perceptron(MLP).<br>"
		   				 +"&nbsp;&nbsp;  MLP use backpropagation method to solve supervised learning problem,<br>"
		   				 +" and suitable for classify data that not linearly separable.<br>"
		   				 +"&nbsp;&nbsp;  If time allows, do a quick read of backprop network in any ML textbook,<br>"
		   				 +" otherwise read the following:<br>"
		   				 +" <h3><br>&nbsp;&nbsp;W<sub>next</sub> = W + ΔW<br></h3>"
		   				 +" <h3>&nbsp;&nbsp;ΔW = -learningRate * gradient  +  momentum * ΔW<sub>previous</sub><br></h3>"
		   				 +" <h4><br>W and ΔW: the weight that applied onto a node, and the change of weight.<br></h4>"
		   				 +" <h4>learning rate: weight changing step size.<br></h4>"
		   				 +" <h4>gradient: determined by backpropagation.<br></h4>"
		   				 +" <h4>momentum: weight changing back step (escape from local minima).<br></h4>"
		   				 +" <h4>Epoch: number of passes through the training data.<br></h4>"
		   				 +"<br>&nbsp;&nbsp;Note 1: smaller learning rate: slower learning but more precise.<br>"
		   				 +"&nbsp;&nbsp;Note 2: larger momentum: easier to get rid of local minima, <br>"
		   				 +" however need a smaller learning rate to work and can miss the global minimum.<br>"
		   				 +"  smaller momentum: more likely to converge the network to a local minima,<br>"
		   				 +"  and also takes longer for the training.<br>"
		   				 +"&nbsp;&nbsp;Note 3: Epoch and number of hidden layer are more convoluted parameters,<br>"
		   				 +"  that can only be decided through experiments, experience (or epiphany).<br>Good luck I guess...<br>"
		   				 +"&nbsp;&nbsp;Note 4: if set hidden layer to 0, then it forms standard perceptron,<br>"
		   				 +" which suitable for linear separable data.<br>";
		    		gd2.addHelp(html);
		    		gd2.showDialog();
		        	if (gd2.wasCanceled()) return;
		    		mlpLearningRate = gd2.getNextNumber();
		    		mlpMomentum = gd2.getNextNumber();
		    		mlpTrainingTime = (int) gd2.getNextNumber();
		    		mlpNumHiddenLayers = (int) gd2.getNextNumber();
		    		if (mlpLearningRate==0 || mlpTrainingTime==0)	return;
		    		prefs.put(Integer.class, "SAHF-mlpLearningRate", mlpLearningRate);
		    		prefs.put(Integer.class, "SAHF-mlpMomentum", mlpMomentum);
		    		prefs.put(Integer.class, "SAHF-mlpTrainingTime", mlpTrainingTime);
		    		prefs.put(Integer.class, "SAHF-mlpNumHiddenLayers", mlpNumHiddenLayers);
    		}
    		//}.start();
    		
    	}
    	
    	if (!configDefaultSilent) {
			// save paramters to persist service
	    	prefs.put(Integer.class, "SAHF-classifierType", classifierType);
	    	for (int c=0; c<doChannel.length; c++)
				prefs.put(Boolean.class, "SAHF-doChannel"+String.valueOf(c), doChannel[c]);
			for (int r=0; r<doRoi.length; r++)
				prefs.put(Boolean.class, "SAHF-doRoi"+String.valueOf(r), doRoi[r]);
			for (int s=0; s<doStat.length; s++)
				prefs.put(Boolean.class, "SAHF-doStat"+String.valueOf(s), doStat[s]);
    	}
		
	}
	
	// create an new thread to configure the MLP, so the help text can display while not taking the focus from main panel
	/*
	public class configureMLP implements Runnable {
		public void main(String[] args) {
	        Runnable runnable = new configureMLP();
	        Thread thread = new Thread(runnable);
	        thread.start();
	    }
		@Override
		public void run() {
			// make use of scijava parameter persistence storage
			DefaultPrefService prefs = new DefaultPrefService();
			mlpLearningRate = prefs.getDouble(Integer.class, "SAHF-mlpLearningRate", mlpLearningRate);
    		mlpMomentum = prefs.getDouble(Integer.class, "SAHF-mlpMomentum", mlpMomentum);
    		mlpTrainingTime = prefs.getInt(Integer.class, "SAHF-mlpTrainingTime", mlpTrainingTime);
    		mlpNumHiddenLayers = prefs.getInt(Integer.class, "SAHF-mlpNumHiddenLayers", mlpNumHiddenLayers);
    		// create user dialog
    		GenericDialog gd2 = new GenericDialog("Neural Network Configuration");
    		gd2.addSlider("Learning Rate:", 0, 1, mlpLearningRate, 0.1);
    		gd2.addSlider("Momentum:", 0, 1, mlpMomentum, 0.1);
    		gd2.addSlider("Number of Epoch:", 0, 10000, mlpTrainingTime, 100);
    		gd2.addSlider("Number of HiddenLayer:", 0, 20, mlpNumHiddenLayers, 1);
    		String html = "<html>"
   				 +"<h2>Configuration of Neural Network</h2>"
   				 +" date: 2019.09.17<br>"
   				 +" author: Ziqiang Huang (Ziqiang.Huang@cruk.cam.ac.uk)<br><br>"
   				 +"<h3>Usage:</h3>"
   				 +"&nbsp;&nbsp;  The current implemented Neural Network is a Multilayer Perceptron(MLP).<br>"
   				 +"&nbsp;&nbsp;  MLP use backpropagation method to solve supervised learning problem,<br>"
   				 +" and suitable for classify data that not linearly separable.<br>"
   				 +"&nbsp;&nbsp;  If time allows, do a quick read of backprop network in any ML textbook,<br>"
   				 +" otherwise read the following:<br>"
   				 +" <h3><br>&nbsp;&nbsp;W<sub>next</sub> = W + ΔW<br></h3>"
   				 +" <h3>&nbsp;&nbsp;ΔW = -learningRate * gradient  +  momentum * ΔW<sub>previous</sub><br></h3>"
   				 +" <h4><br>W and ΔW: the weight that applied onto a node, and the change of weight.<br></h4>"
   				 +" <h4>learning rate: weight changing step size.<br></h4>"
   				 +" <h4>gradient: determined by backpropagation.<br></h4>"
   				 +" <h4>momentum: weight changing back step (escape from local minima).<br></h4>"
   				 +" <h4>Epoch: number of passes through the training data.<br></h4>"
   				 +"<br>&nbsp;&nbsp;Note 1: smaller learning rate: slower learning but more precise.<br>"
   				 +"&nbsp;&nbsp;Note 2: larger momentum: easier to get rid of local minima, <br>"
   				 +" however need a smaller learning rate to work and can miss the global minimum.<br>"
   				 +"  smaller momentum: more likely to converge the network to a local minima,<br>"
   				 +"  and also takes longer for the training.<br>"
   				 +"&nbsp;&nbsp;Note 3: Epoch and number of hidden layer are more convoluted parameters,<br>"
   				 +"  that can only be decided through experiments, experience (or epiphany).<br>Good luck I guess...<br>"
   				 +"&nbsp;&nbsp;Note 4: if set hidden layer to 0, then it forms standard perceptron,<br>"
   				 +" which suitable for linear separable data.<br>";
    		gd2.addHelp(html);
    		gd2.showDialog();
        	if (gd2.wasCanceled()) return;
    		mlpLearningRate = gd2.getNextNumber();
    		mlpMomentum = gd2.getNextNumber();
    		mlpTrainingTime = (int) gd2.getNextNumber();
    		mlpNumHiddenLayers = (int) gd2.getNextNumber();
    		if (mlpLearningRate==0 || mlpTrainingTime==0)	return;
    		prefs.put(Integer.class, "SAHF-mlpLearningRate", mlpLearningRate);
    		prefs.put(Integer.class, "SAHF-mlpMomentum", mlpMomentum);
    		prefs.put(Integer.class, "SAHF-mlpTrainingTime", mlpTrainingTime);
    		prefs.put(Integer.class, "SAHF-mlpNumHiddenLayers", mlpNumHiddenLayers);
    	}
	}
	*/
	
	// function to train classifier, based on the current trainingSamples (shared among source, sample and classifier panels)
	public static void trainClassifier() {
		// trainingSamplesClassifierPanel is a reference from sample panel, that should contains the current training sample information
		// create training data
		classifierInfo.paintImmediately(0,0,classifierInfo.getWidth(), classifierInfo.getHeight());
		//classifierInfo.setText("Construct training data...");
		classifierInfo.setText("Training...");
		classifierInfo.setFont(textFont);
		classifierInfo.setForeground(fontColor);
		trainingData = buildTrainingData();
		//System.out.println(trainingData);
		if (trainingData==null) {
			classifierInfo.setText(errorTrain);
			classifierInfo.setFont(errorFont);
			classifierInfo.setForeground(errorFontColor);
			return;
		}
		// construct classifier of specific type
		switch (classifierType) {
			case 0: // random forest
				cls = new RandomForest();
				break;
			case 1:	// J4.5 decision tree
				cls = new J48();
				break;
			case 2:	// multi layer perceptron
				cls = new MultilayerPerceptron();
				((MultilayerPerceptron) cls).setLearningRate(mlpLearningRate);
				((MultilayerPerceptron) cls).setMomentum(mlpMomentum);
				((MultilayerPerceptron) cls).setTrainingTime(mlpTrainingTime);
				((MultilayerPerceptron) cls).setHiddenLayers(String.valueOf(mlpNumHiddenLayers));
				break;
		}
		// train classifier with training data
		//classifierInfo.paintImmediately(0,0,classifierInfo.getWidth(), classifierInfo.getHeight());
		//classifierInfo.setText("Training...");
    	try {
			cls.buildClassifier(trainingData);
			classifierInfo.paintImmediately(0,0,classifierInfo.getWidth(), classifierInfo.getHeight());
			classifierInfo.setText("Training complete.");
			classifierInfo.setFont(textFont);
			classifierInfo.setForeground(fontColor);
		} catch (Exception e) {
			classifierInfo.setText(errorTrain);
			classifierInfo.setFont(errorFont);
			classifierInfo.setForeground(errorFontColor);
			e.printStackTrace();
		}
    	
		//System.gc();
	}
	// function to apply classifier on the current source image
	public static void applyClassifier() {
		// need source image, classifier, not neccessarily training samples (different classes with sample images)
		// fetch trainingSamplesClassifierPanel is a reference from sample panel, that should contains the current training sample information
		// create testing data based on the classifier configuration, from source image
		if (trainingSamplesClassifierPanel.getCurrentSource()==null) {
			classifierInfo.setText(noSource);
			classifierInfo.setFont(errorFont);
			classifierInfo.setForeground(errorFontColor);
			return;
		}
		if (cls==null) {
			classifierInfo.setText(noClassifier);
			classifierInfo.setFont(errorFont);
			classifierInfo.setForeground(errorFontColor);
			return;
		}
		// prepare class name and check box
		ArrayList<String> classNames = null;
		try {
			classNames = WekaUtility.getClassAttributeFromModel(cls);
		} catch (Exception e1) {
			classifierInfo.setText(errorCheck);
			classifierInfo.setFont(errorFont);
			classifierInfo.setForeground(errorFontColor);
			e1.printStackTrace();
			return;
		}
		int numClass = classNames.size();
		String[] nameArray = classNames.toArray(new String[numClass]);
		showClassImage = new boolean[numClass];
		probabilityTreshold = new double[numClass];
		// make use of scijava parameter persistence storage
		DefaultPrefService prefs = new DefaultPrefService();
		for (int i=0; i<numClass; i++)	{
			showClassImage[i] = prefs.getBoolean(Boolean.class, "SAHF-showClassImage"+String.valueOf(i), showClassImage[i]);
			probabilityTreshold[i] = prefs.getDouble(Double.class, "SAHF-probabilityTreshold"+String.valueOf(i), probabilityTreshold[i]);
		}
		autoProbThreshold = prefs.getBoolean(Boolean.class, "SAHF-autoProbThreshold", autoProbThreshold);
		// configure user dialog
		GenericDialogPlus gd = new GenericDialogPlus("Apply Trained Classifier");
		String[] headingsShowClass = {"Show Class Prediction as Image(s)"};
		gd.addCheckboxGroup(numClass, 1, nameArray, showClassImage, headingsShowClass);
		String probsThresMessage = "Probability Treshold (%)";
		gd.setInsets(20,0,0);
		gd.addMessage(probsThresMessage, errorFont);
		gd.addCheckbox("Auto Treshold", autoProbThreshold);
		for (int i=0; i<numClass; i++)
			gd.addSlider(nameArray[i], 0, 100, probabilityTreshold[i]*100, 1);
		gd.showDialog();
    	if (gd.wasCanceled()) return;
    	for (int i=0; i<numClass; i++)
    		showClassImage[i] = gd.getNextBoolean();
    	autoProbThreshold = gd.getNextBoolean();
    	for (int i=0; i<numClass; i++) {
    		if (autoProbThreshold) probabilityTreshold[i] = Math.round(1000/numClass)/(double)1000;
    		else probabilityTreshold[i] = gd.getNextNumber()/100;
    	}
		// save paramters to persist service
		prefs.put(Boolean.class, "SAHF-autoProbThreshold", autoProbThreshold);
		for (int i=0; i<numClass; i++) {
			prefs.put(Boolean.class, "SAHF-showClassImage"+String.valueOf(i), showClassImage[i]);
			prefs.put(Double.class, "SAHF-probabilityTreshold"+String.valueOf(i), probabilityTreshold[i]);
		}
		
		// update classifier information
		classifierInfo.paintImmediately(0,0,classifierInfo.getWidth(), classifierInfo.getHeight());
		classifierInfo.setText("Classify images in source image...");
		try {	// create testing data, and classify the testing data
			classifyTestingData(buildTestingData(), showClassImage, autoProbThreshold, probabilityTreshold);
		} catch (Exception e) {
			classifierInfo.setText(errorApply);
			classifierInfo.setFont(errorFont);
			classifierInfo.setForeground(errorFontColor);
			e.printStackTrace();
		}
		classifierInfo.setText("Classification complete.");
		classifierInfo.setFont(textFont);
		classifierInfo.setForeground(fontColor);
		System.gc();
	}
	// function to build training data
	public static Instances buildTrainingData () {
		// get features from classifier configuration
		if (trainingSamplesClassifierPanel==null) return null;
		ArrayList<String> classNames = trainingSamplesClassifierPanel.getClassNames();
		if (classNames.size()==0)	return null;
		removeSpaceInClassNames(classNames);
		if (attrChannel==null || attrLocalization==null || attrStatistical==null || attrChannel.size()==0 || attrLocalization.size()==0 || attrStatistical.size()==0)
			configureClassifier(true);
		
		Instances newData = WekaUtility.createEmptyData (classNames, attrChannel, attrLocalization, attrStatistical, 0);
		recoverSpaceInClassNames(classNames);
		for (ImagePlus classSample : trainingSamplesClassifierPanel.trainingSamples) {
			ImagePlus classSampleDuplicate = classSample.duplicate();
			WekaUtility.addNewInstanceToData (classSampleDuplicate, classNames.indexOf(classSample.getTitle()), newData, attrChannel, attrLocalization, attrStatistical);
			classSampleDuplicate.changes = true; classSampleDuplicate.close(); System.gc();
		}
		newData.setClassIndex(newData.numAttributes()-1);
		return newData;
	}
	// function to build testing data
	public static Instances buildTestingData () throws Exception {
		// get features from source images, and configuration
		source = trainingSamplesClassifierPanel.getCurrentSource();
		if (source==null || cls==null) return null;
		int numSamples = source.getStackSize();
		ImagePlus sourceDuplicate = source.duplicate();
		ArrayList<String> classNames = WekaUtility.getClassAttributeFromModel(cls);
		attrChannel = WekaUtility.getChannelFeaturesFromModel(cls);
		attrLocalization = WekaUtility.getLocalizationFeatureFromModel(cls);
		attrStatistical = WekaUtility.getStatisticalFeatureFromModel(cls);
		// construct testing data
		removeSpaceInClassNames(classNames);
		Instances newData = WekaUtility.createEmptyData (classNames, attrChannel, attrLocalization, attrStatistical, numSamples);
		recoverSpaceInClassNames(classNames);
		WekaUtility.addNewInstanceToData (sourceDuplicate, -1, newData, attrChannel, attrLocalization, attrStatistical);
		sourceDuplicate.changes = true; sourceDuplicate.close(); System.gc();
		newData.setClassIndex(newData.numAttributes()-1);
		return newData;
	}
	// function to classify testing data
	public static void classifyTestingData (
			Instances testingData,
			boolean[] showClasses,
			boolean autoThreshold,
			double[] probThresholds
			) throws Exception {
		if (cls==null || testingData==null || testingData.numInstances()==0) return;
		ArrayList<String> classNames = WekaUtility.getClassAttributeFromModel(cls);
		recoverSpaceInClassNames(classNames);
		// create result table to store values and displaautoThresholdy later
		ResultsTable classificationResultTable = new ResultsTable();
		classificationResultTable.setPrecision(2);
		sliceWiseResult = new HashMap<Integer, double[]>(); // slice wise result: source slice index : class prediction, probabilities...
		int numClasses = classNames.size();
		int numSample = testingData.numInstances();
		int classIdx=testingData.numAttributes()-1;
		testingData.setClassIndex(classIdx);
		
		String classResult; double label;	double[] probs;
		for (int i=0; i<numSample; i++) {
			double[] result = new double[numClasses+1];
			try {
				label = cls.classifyInstance(testingData.instance(i));
				probs = cls.distributionForInstance(testingData.instance(i));
			} catch (Exception e1) {
				result[0] = -1;
				sliceWiseResult.put(i+1, result);
				System.out.println("Classification error on slice " + (i+1) + " in source image " + source.getTitle());
				e1.printStackTrace();
				continue;
			}
			testingData.instance(i).setClassValue(label);
			classResult = classNames.get((int)testingData.instance(i).classValue());
			result[0] = label;
			classificationResultTable.incrementCounter();	// add 1 new row to result table
			classificationResultTable.addValue("Slice", (i+1));	// add slice index to corresponding class
			classificationResultTable.addValue("Class", classResult);
			for (int p=0; p<classNames.size(); p++) {
				classificationResultTable.addValue(classNames.get(p)+" probability", probs[p]);
				result[p+1] = probs[p];
			}
			sliceWiseResult.put(i+1, result);
		}	
		classificationResultTable.show("SAHF Classification Result (" + trainingSamplesClassifierPanel.getCurrentSource().getTitle() + ")");
		showPredictionImages(trainingSamplesClassifierPanel.getCurrentSource(), sliceWiseResult, classNames, showClasses, probThresholds);
	}
	
	/*
	 * Function group for check, load, and save classifier
	 */
	// function to check classifier information
	public static void checkClassifier() {
		String info = getClassifierInfo();
		classifierInfo.setText(info);
		if (info.equals(noClassifier) || info.equals(errorCheck)) {
			classifierInfo.setFont(errorFont);
			classifierInfo.setForeground(errorFontColor);
		} else {
			classifierInfo.setFont(textFont);
			classifierInfo.setForeground(fontColor);
		}
	}
	// function to load a per-trained classifier from file
	public static void loadClassifier() {
		// make use of scijava parameter persistence storage	
		DefaultPrefService prefs = new DefaultPrefService();
		classifierFilePath = prefs.get(String.class, "SAHF-classifierFilePath", classifierFilePath);
		// create user interface
		GenericDialogPlus gd = new GenericDialogPlus("Load classifier from file");
		gd.addFileField("classifier path:", classifierFilePath, 55);
    	gd.showDialog();
    	if (gd.wasCanceled()) return;
		classifierFilePath = gd.getNextString();
		if (classifierFilePath==null) return;
		// save paramters to persist service
		prefs.put(String.class, "SAHF-classifierFilePath", classifierFilePath);
		// load classifier use weka serialization helper reader
		try {
			cls = (Classifier) SerializationHelper.read(classifierFilePath);
		} catch (Exception e) {
			classifierInfo.setText(errorLoad);
			classifierInfo.setFont(errorFont);
			classifierInfo.setForeground(errorFontColor);
			e.printStackTrace();
		}
		// check the loaded classifier info
		checkClassifier();
	}
	// function to save/export trained classifier to file
	public static void exportClassifier(Frame f) {
		if (cls==null) {
    		classifierInfo.setText("No trained classifier found!\nTrain a classifier first, and then export.");
    		classifierInfo.setFont(errorFont);
			classifierInfo.setForeground(errorFontColor);
    		return;
    	}
		// prompt for user to decide if save the training data together
		String title = "Also save training data to file?";
		String body = "Press OK to confirm" + 
					"\npress Cancel to omit training data saving.";
		boolean saveData = IJ.showMessageWithCancel(title, body);
		// make use of scijava parameter persistence storage	
		DefaultPrefService prefs = new DefaultPrefService();
		classifierFilePath = prefs.get(String.class, "SAHF-classifierFilePath", classifierFilePath);
		if (classifierFilePath==null) classifierFilePath = IJ.getDir("current");	
		// prompt for user to choose a location and file name
    	String savePath = null; String trainingDataPath = null;
    	JFileChooser fileChooser = new JFileChooser();
    	fileChooser.setCurrentDirectory(new File(classifierFilePath));
    	fileChooser.setDialogTitle("Specify file name to save (file extension not obligatory)");
    	int userSelection = fileChooser.showSaveDialog(f);
    	if (userSelection == JFileChooser.APPROVE_OPTION)
    	    savePath = fileChooser.getSelectedFile().getAbsolutePath();
    	if (savePath==null)	return;
    	int extIdx = savePath.lastIndexOf(".");
    	if (extIdx==-1)
    		trainingDataPath = savePath + ".arff";
    	else
    		trainingDataPath = savePath.substring(0, extIdx) + ".arff";
    	// save classifier and data to file
		try {
			SerializationHelper.write(savePath, cls);
			if (saveData && trainingData!=null)
				DataSink.write(trainingDataPath, trainingData);
			String displaySavePath = wrapString(savePath, lineWidth, 0);
			classifierInfo.setText("Classifier saved as:\n" + displaySavePath);
			classifierInfo.setFont(textFont);
			classifierInfo.setForeground(fontColor);
			prefs.put(String.class, "SAHF-classifierFilePath", classifierFilePath);
		} catch (Exception e) {
			classifierInfo.setText(errorSave);
			classifierInfo.setFont(errorFont);
			classifierInfo.setForeground(errorFontColor);
			System.err.println("Failed to save data to: " + trainingDataPath);
			e.printStackTrace();
		}
	}
	// function to get classifier information
	public static String getClassifierInfo () {
		if (cls==null) return noClassifier;
		// construct feature strings
		String classifierType = "Type: " + WekaUtility.getClassifierTypeFromModel(cls);
		String featureClass = "Class: ";
		String featureChs = "Channel: ";
		String featureLoci = "Localization: ";
		String featureStats = "Statistics: ";
		try {	// fill feature string with contents extracted from classifier
			ArrayList<String> className = WekaUtility.getClassAttributeFromModel(cls);
			recoverSpaceInClassNames(className);
			for (String name : className)
				featureClass += name + ", ";
			
			ArrayList<Integer> attrCh = WekaUtility.getChannelFeaturesFromModel(cls);
			for (Integer c : attrCh)
				featureChs += channelFeatureString[c] + ", ";
			
			ArrayList<Integer> attrLoca = WekaUtility.getLocalizationFeatureFromModel(cls);
			for (Integer l : attrLoca)
				featureLoci += localizationFeatureString[l] + ", ";
			
			ArrayList<Integer> attrStats = WekaUtility.getStatisticalFeatureFromModel(cls);
			for (Integer s : attrStats)
				featureStats += statisticalFeatureString[s] + ", ";
		} catch (Exception e) {
			e.printStackTrace();
			return errorCheck;
		}
		// trim the last ", " in those feature strings
		featureClass = wrapString(featureClass.substring(0, featureClass.length()-2), lineWidth, 13);
		featureChs = wrapString(featureChs.substring(0, featureChs.length()-2), lineWidth, 17);
		featureLoci = wrapString(featureLoci.substring(0, featureLoci.length()-2), lineWidth, 23);
		featureStats = wrapString(featureStats.substring(0, featureStats.length()-2), lineWidth, 19);
		// return concatenated feature string
		return (classifierType + "\n\n" + featureClass + "\n\n"  + featureChs + "\n\n" + featureLoci + "\n\n" + featureStats);
	}
	// function to wrap string at wrap length, leave space with indent length
	public static String wrapString(
			String inputLongString,
			int wrapLength,
			int indent
			) {
		String wrappedString = ""; String indentStr = "";
		for (int i=0; i<indent; i++)
			indentStr += " ";
		for (int i=0; i<inputLongString.length(); i++) {
			if (i!=0 && i%lineWidth==0)	wrappedString += ("\n"+indentStr);
			wrappedString += inputLongString.charAt(i);
		}
		return wrappedString;
	}
	// function to code/decode space in the class names
	public static void removeSpaceInClassNames (ArrayList<String> classNames) {
		for (int i=0; i<classNames.size(); i++) {
			String newName = classNames.get(i).replaceAll("_", "µn!c0de002D");
			newName = newName.replaceAll(" ", "_");
			classNames.set(i, newName);
		}	
	}
	public static void recoverSpaceInClassNames (ArrayList<String> classNames) {
		for (int i=0; i<classNames.size(); i++) {
			String newName = classNames.get(i).replaceAll("_", " ");
			newName = newName.replaceAll("µn!c0de002D", "_");
			classNames.set(i, newName);
		}	
	}
	
	
	
	/*  
	 *  Function groups to prepare and configure the prediction images and their windows
	 */
	// function to display predicted images of each classes
	public static void showPredictionImages(
			ImagePlus sourceImage, 
			HashMap<Integer, double[]> sliceResults,  // slice wise result (HashMap): source slice index : class prediction, probabilities...
			ArrayList<String> classNames,
			boolean[] showClasses,
			double[] probabilityThreshold
			) {
		if (sourceImage==null || sliceResults==null) return;
		if (sourceImage.getNSlices()!=sliceResults.size()) return;
		if (sliceResults.get(1).length!=(classNames.size()+1)) return;
		if (classNames.size()!=showClasses.length) return;
		int numC = sourceImage.getNChannels();
		int numZ = sourceImage.getNSlices();
		int numT = sourceImage.getNFrames();
		int numClasses = classNames.size();
		// get each class into sub-stacks with thresholded probability;
		ImagePlus sourceDuplicate = sourceImage.duplicate();
		ImageStack impStack = sourceDuplicate.getStack();
		classWiseResult = new ArrayList<ArrayList<double[]>>();
		// create class-wise storages
		ImageStack[] classStacks = new ImageStack[numClasses];	// image stack for each class, to store slices
		int[] sliceIndex = new int[numClasses];	// keep track of slice index of each class image stack: intialized as all 0
		Overlay[] classOverlays = new Overlay[numClasses];	// store the slice overlay for each class image stack
		for (int i=0; i<numClasses; i++) {
			classStacks[i] = new ImageStack(sourceDuplicate.getWidth(), sourceDuplicate.getHeight());
			sliceIndex[i] = 0;
			classOverlays[i] = new Overlay();
			classWiseResult.add(new ArrayList<double[]>());
		}
		// iterate through slices in the source image
		for (int i=0; i<numZ; i++) {
			int predictedClass = (int)sliceResults.get(i+1)[0];	// predicted class belonging: 0 based
			double[] sliceResult = new double[sliceResults.get(i+1).length + 1]; // slice result (double array): slice number, class prediction, probabilities...
			sliceResult[0] = (double)(i+1);
			for (int j=1; j<sliceResult.length; j++)
				sliceResult[j] = sliceResults.get(i+1)[j-1];
			// wrong prediction result, skip
			if (predictedClass==-1)	continue;
			// iterate through classes to check criterion for each class,
			for (int c=0; c<numClasses; c++) {
				// if store current slice and its result into classWiseResult, classStack, sliceIndex, classOverlays
				if (!showClasses[c] ||sliceResult[c+2]<probabilityThreshold[c]) continue;
				// increment slice index in current class
				sliceIndex[c]++;
				// get slice overlay
				Overlay sliceOverlay2 = sourceDuplicate.getOverlay().duplicate();
	    		sliceOverlay2.crop(1, numC, i+1, i+1, 1, numT);
	    		Roi[] sliceRois = sliceOverlay2.toArray();
	    		for (Roi sliceRoi : sliceRois) {
	    			if (sourceImage.isHyperStack())
	    				sliceRoi.setPosition(0, sliceIndex[c], 0);
	    			else
	    				sliceRoi.setPosition(sliceIndex[c]);
	    			classOverlays[c].add(sliceRoi);
	    		}
	    		// add current slice into class image stack
	    		//for (int t=numT; t>=1; t--) {
    			int index = sourceDuplicate.getStackIndex(1, i+1, 1);
    			for (int ch=0; ch<numC; ch++)
    				classStacks[c].addSlice(impStack.getSliceLabel(index+ch)+", Slice:"+String.valueOf(i+1), impStack.getProcessor(index+ch));
	    		//}
	    		
	    		//classStacks[c].addSlice(impStack.getSliceLabel(i+1)+", Slice:"+String.valueOf(i+1), impStack.getProcessor(i+1));
	    		// add current slice result into class result of current class
	    		classWiseResult.get(c).add(sliceResult); // class wise result: slice number in source, class prediction, probabilities...	
			}
		}
		// display each class image stack according to criterion
		for (int i=0; i<classStacks.length; i++) {
			if (classStacks[i]==null || classStacks[i].getSize()==0) continue;
	    	ImagePlus classImage = new ImagePlus(classNames.get(i) + " prediction", classStacks[i]);
	    	if (sourceImage.isHyperStack()) {
	    		classImage = IJ.createHyperStack(classNames.get(i) + " prediction", 
	    				sourceImage.getWidth(), sourceImage.getHeight(), 
	    				sourceImage.getNChannels(), 1, sourceImage.getNFrames(), 
	    				sourceImage.getBitDepth());
			}
	    	classImage.setStack(classStacks[i], numC, (int)(classStacks[i].size()/numC/numT), numT);
	    	classImage.setOverlay(classOverlays[i]);
	    	classImage.show();
	    	preparePredictionImage(classImage, classNames, i);	// prepare class image window
		}
	}
	// prepare prediction image window
	public static void preparePredictionImage(ImagePlus classPredictionImage, ArrayList<String> classNames, int classIndex) {
		// prediction images don't need to be updated, so all panels are created only once without checking
		// add sort panel: with sort by probability, sort by slice, and generate data table button
		createSortPanel(classPredictionImage.getWindow(), classNames, classIndex);
		// add histogram panel and gallery panel
		TrainingSamples.addGalleryPanel(classPredictionImage.getWindow());
		TrainingSamples.addHistogramPanel(classPredictionImage.getWindow());
		// add delete panel: with delete slice and delete range button
		createDeletePanel(classPredictionImage.getWindow(), classIndex);
		// add move panel: with move (range) to each class belonging to current classes
		createMovePanel(classPredictionImage.getWindow(), classNames, classIndex);
		// adjust zoom to fit
		WindowManager.setCurrentWindow(classPredictionImage.getWindow()); 	new Zoom().run("scale");
	}
	// create class prediction operation panel, and add to image window at the bottom
	public static void createSortPanel(ImageWindow window, ArrayList<String> classNames, int classIndex) {
		//ArrayList<String> classNames = this.getClassNames();
		JPanel buttonPanel = new JPanel();
		buttonPanel.setName("sortPanel");
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        Button btnSortByProb = addSortByProbabilityButton(window, classIndex); buttonPanel.add(btnSortByProb);
        Button btnSortBySlice = addSortBySliceButton(window, classIndex); buttonPanel.add(btnSortBySlice);
        Button btnDataTable = addGenerateTableButton(window, classIndex, classNames); buttonPanel.add(btnDataTable);
        buttonPanel.setBackground(window.getBackground());
        window.add(buttonPanel);
		window.pack();
	}
		// create sort by probability button
		public static Button addSortByProbabilityButton(ImageWindow window, int classIndex) {
			Button sortByProbability = new Button("sort by probability");
			sortByProbability.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					ImagePlus classImage = window.getImagePlus();
					int numZ = classImage.getNSlices();
					if (numZ!=classWiseResult.get(classIndex).size()) {
						System.out.println("size mismatch in class " + classImage.getTitle() + "!");
						return;
					}
					ArrayList<double[]> oldResult = (ArrayList<double[]>) classWiseResult.get(classIndex).clone();
					ArrayList<double[]> result = classWiseResult.get(classIndex);
					int numSamples = classImage.getNSlices();
					ArrayList<Integer> newIndex = new ArrayList<Integer>();
					for (int i=0; i<numSamples; i++)	newIndex.add(i+1);
		        	// swap sorting algorithm
		        	boolean swapped = false;
		        	for (int pass=0; pass<numSamples; pass++) {
		        		for (int iSlice=1; iSlice<(numSamples-pass); iSlice++) {
		        			double comp = result.get(iSlice-1)[classIndex+2] - result.get(iSlice)[classIndex+2];
		        			if (comp<0) {	// sort from large to small probability
		        				swapped = true;
		        				Collections.swap(newIndex, iSlice-1, iSlice);
		    					Collections.swap(classWiseResult.get(classIndex), iSlice-1, iSlice);
		        			}
		        		}
		        		if(!swapped)break;
		        	}
		        	HyperStackUtility.swapSlices(classImage, 4, newIndex);
		        	return;
				}
			}); 
			return sortByProbability;
		}
		// create sort by slice button
		public static Button addSortBySliceButton(ImageWindow window, int classIndex) {
			Button sortBySlice = new Button("sort by slice");
			sortBySlice.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					ImagePlus classImage = window.getImagePlus();
					int numZ = classImage.getNSlices();
					if (numZ!=classWiseResult.get(classIndex).size()) {
						System.out.println("size mismatch in class " + classImage.getTitle() + "!");
						return;
					}
					ArrayList<double[]> oldResult = (ArrayList<double[]>) classWiseResult.get(classIndex).clone();
					ArrayList<double[]> result = classWiseResult.get(classIndex);
					int numSamples = classImage.getNSlices();
					ArrayList<Integer> newIndex = new ArrayList<Integer>();
					for (int i=0; i<numSamples; i++)	newIndex.add(i+1);
		        	// swap sorting algorithm
		        	boolean swapped = false;
		        	for (int pass=0; pass<numSamples; pass++) {
		        		for (int iSlice=1; iSlice<(numSamples-pass); iSlice++) {
		        			double comp = result.get(iSlice-1)[0] - result.get(iSlice)[0];
		        			if (comp>0) {	// sort from small to large slice index
		        				swapped = true;
		        				Collections.swap(newIndex, iSlice-1, iSlice);
		    					Collections.swap(classWiseResult.get(classIndex), iSlice-1, iSlice);
		        			}
		        		}
		        		if(!swapped)break;
		        	}
		        	HyperStackUtility.swapSlices(classImage, 4, newIndex);
	        		return;
				}
			});
			return sortBySlice;
		}
		// create generate data table button
		public static Button addGenerateTableButton(ImageWindow window, int classIndex,  ArrayList<String> classNames) {
			Button generateTable = new Button("data table");
			generateTable.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					ImagePlus classImage = window.getImagePlus();
					int numZ = classImage.getNSlices();
					if (numZ!=classWiseResult.get(classIndex).size()) {
						System.out.println("size mismatch in class " + classImage.getTitle() + "!");
						return;
					}
					//ArrayList<String> classNames = WekaUtility.getClassAttributeFromModel(cls);
					//recoverSpaceInClassNames(classNames);
					String className = classNames.get(classIndex);
					ArrayList<double[]> result = classWiseResult.get(classIndex); // class wise result: slice number in source, class prediction, probabilities...
					ResultsTable classData = new ResultsTable();
					classData.setPrecision(2);
					
					for (int z=0; z<numZ; z++) {
						classData.incrementCounter();	// add 1 new row to result table
						classData.addValue("Slice", (z+1));	// add slice index to corresponding class
						classData.addValue("Slice in source image", result.get(z)[0]);
						classData.addValue("Class", classNames.get((int) result.get(z)[1]));
						classData.addValue(className + " probability", result.get(z)[classIndex+2]);
						for (int p=0; p<classNames.size(); p++) {
							if (p==classIndex)	continue;
							classData.addValue(classNames.get(p)+" probability", result.get(z)[p+2]);
						}
					}	
					classData.show(classImage.getTitle() + " data table (" + source.getTitle() + ")");
				}
			});
			return generateTable;
		}
	// create deletion operation panel, and add to image window at the bottom
	public static void createDeletePanel(ImageWindow window, int classIndex) {
		//ArrayList<String> classNames = this.getClassNames();
		JPanel buttonPanel = new JPanel();
		buttonPanel.setName("deletePanel");
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        Button btnDeleteSlice = addDeleteSliceButton(window, classIndex); buttonPanel.add(btnDeleteSlice);
        Button btnDeleteRange = addDeleteRangeButton(window, classIndex); buttonPanel.add(btnDeleteRange);
        buttonPanel.setBackground(window.getBackground());
        window.add(buttonPanel);
		window.pack();
	}
		// create delete slice button
		public static Button addDeleteSliceButton(ImageWindow window, int classIndex) {
		    Button delSlice = new Button("delete"); 
		    delSlice.addActionListener(new ActionListener() { 
		        @Override 
		        public void actionPerformed(ActionEvent ae) {
		        	ImagePlus classImage = window.getImagePlus();
		    		int nSlices = classImage.getNSlices();
		    		int currentSlice = classImage.getCurrentSlice();
		    		classWiseResult.get(classIndex).remove(currentSlice-1);	// update class wise result arraylist
		    		if (nSlices==1) {	// only 1 image left, close the image
		    			classImage.changes = true;
		    			classImage.close();
		    			System.gc();
		    			return;
		    		}
		    		// prepare image stack and overlay
		    		ImageStack stack = classImage.getStack();
		    		Overlay overlay = classImage.getOverlay();
		    		Overlay newOverlay = new Overlay();
		    		if (overlay!=null) {
		    			for (Roi sourceRoi : overlay.duplicate().toArray()) {
		    				int roiPosition = sourceRoi.getPosition();
		    				if (roiPosition < currentSlice) {
		    					newOverlay.add(sourceRoi);	// roi position before delete index stay the same
		    				} else if (roiPosition > currentSlice) {
		    					sourceRoi.setPosition(roiPosition-1);	// roi position after delete index move 1 forward
		    					newOverlay.add(sourceRoi);
		    				}	// roi position at delete index will be removed from array
		    			}
		    		}
		    		stack.deleteSlice(currentSlice);
		    		classImage.setStack(stack);
		    		classImage.setOverlay(newOverlay);
		    		classImage.setSlice(currentSlice==1?1:(currentSlice-1));
		    		classImage.changes = true;
		    		classImage.getWindow().updateImage(classImage);
		    		classImage.getWindow().pack();
		    		return;
		        } 
		    }); 
		    return delSlice;
		}
		// create delete range button
		public static Button addDeleteRangeButton(ImageWindow window, int classIndex) {
		    Button delRange = new Button("delete range"); 
		    delRange.addActionListener(new ActionListener() { 
		        @Override 
		        public void actionPerformed(ActionEvent ae) {
		        	ImagePlus classImage = window.getImagePlus();
		    		int nSlices = classImage.getNSlices();
		    		int currentSlice = classImage.getCurrentSlice();
		    		// user dialog to indicate deletion range
		        	GenericDialog gd = new GenericDialog("Indicate Range:");
		        	gd.addSlider("From: ", 1, nSlices, currentSlice, 1);
		        	gd.addSlider("To: ", 1, nSlices, currentSlice, 1);
					gd.showDialog();
					if (gd.wasCanceled()) return;
					int beginIdx = (int)gd.getNextNumber();
					int endIdx = (int)gd.getNextNumber();
					if (endIdx<beginIdx) {	// swap begin and end slice index, so (begin < end)
						int temp = endIdx;
						endIdx = beginIdx;
						beginIdx = temp;
					}
					for (int idx=endIdx; idx>=beginIdx; idx--)
						classWiseResult.get(classIndex).remove(idx-1);	// update class wise result arraylist
					if (endIdx==nSlices && beginIdx==1) {	// whole range, then close the image stack
						classImage.changes=false;
						classImage.close();
						return;
					}
					int range = endIdx - beginIdx + 1;	// closed range between begin and end idx [beginIdx, endIdx];
					// prepare image stack and overlay
					ImageStack stack = classImage.getStack();
					Overlay overlay = classImage.getOverlay();
		    		Overlay newOverlay = new Overlay();
		    		if (overlay!=null) {
		    			for (Roi roi : overlay.duplicate().toArray()) {
		    				int roiPosition = roi.getPosition();
		    				if (roiPosition < beginIdx) {
		    					newOverlay.add(roi);	// roi position before delete index stay the same
		    				} else if (roiPosition > endIdx) {
		    					roi.setPosition(roiPosition-range);	// roi position move forward by range
		    					newOverlay.add(roi);
		    				}	// roi position at delete index will be removed from array
		    			}
		    			classImage.setOverlay(newOverlay);
		    		}
					for (int idx=endIdx; idx>=beginIdx; idx--)
						stack.deleteSlice(idx);	// recursively delete slices from the end to the begin index
					classImage.setStack(classImage.getTitle(), stack);
					classImage.changes = true;
		        } 
		    }); 
		    return delRange;
		}
	// create move to class (labelled) operation panel, and add to image window at the bottom
	public static void createMovePanel(ImageWindow window, ArrayList<String> classNames, int classIndex) {
		ImagePlus classImage = window.getImagePlus();
		String classNameImage = classImage.getTitle().substring(0, classImage.getTitle().indexOf(" prediction"));
		JPanel buttonPanel = TrainingSamples.getPanel(window, "movePanel");
		if (buttonPanel==null) {	// move to class panel does not exist
			buttonPanel = new JPanel();
			buttonPanel.setName("movePanel");
	        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
		}
        if (!TrainingSamples.checkButton(window, "Move to "+classNames.get(classIndex))) {	// add move to its predicted class button first
			Button btnMoveSample = addMoveButton(window, classNames.get(classIndex), classIndex);
			buttonPanel.add(btnMoveSample);
		}
        for (int i=0; i<classNames.size(); i++) {	// add move to other classes button beneath
			if (i==classIndex) continue;
			if (!TrainingSamples.checkButton(window, "Move to "+classNames.get(i))) {
				Button btnMoveSample = addMoveButton(window, classNames.get(i), classIndex);
				buttonPanel.add(btnMoveSample);
			}
		}
        buttonPanel.setBackground(window.getBackground());
        window.add(buttonPanel);
		window.pack();
	}
		// create move sample button
		public static Button addMoveButton(
				ImageWindow window,
				String className,
				int classIndex
				) {
		    Button moveSample = new Button("Move to "+className); 
		    moveSample.addActionListener(new ActionListener() { 
		        @Override 
		        public void actionPerformed(ActionEvent ae) {
		        	ImagePlus classImage = window.getImagePlus();
		    		int nSlices = classImage.getNSlices();
		    		int currentSlice = classImage.getZ();
		        	// user dialog to indicate deletion range
		        	GenericDialog gd = new GenericDialog("Indicate Range:");
		        	gd.addSlider("From: ", 1, nSlices, currentSlice, 1);
		        	gd.addSlider("To: ", 1, nSlices, currentSlice, 1);
					gd.showDialog();
					if (gd.wasCanceled()) return;
					int beginIdx = (int)gd.getNextNumber();
					int endIdx = (int)gd.getNextNumber();
					if (endIdx<beginIdx) {	// swap begin and end slice index, so (begin < end)
						int temp = endIdx;
						endIdx = beginIdx;
						beginIdx = temp;
					}
					int range = endIdx - beginIdx + 1;	// closed range between begin and end idx [beginIdx, endIdx];
		        	// if training samples not defined, do nothing
		        	if (trainingSamplesClassifierPanel==null) return;
		        	ImagePlus moveToClass = null;
		        	ArrayList<String> classNamesInSamplePanel = trainingSamplesClassifierPanel.getClassNames();
		        	// check if the specified class exist in sample panel, if not then create new one
		        	if (classNamesInSamplePanel==null || !classNamesInSamplePanel.contains(className)) {
		        		moveToClass = trainingSamplesClassifierPanel.addNewEmptyClass(className);
		        		// add first indicated slice in range to the empty new class window
		        		if (!HyperStackUtility.replaceSlice(classImage, moveToClass, 4, beginIdx, 1, true))	// replace slice and delete in source
	        				System.out.println("Can not add slice " + beginIdx + " from source image: " + classImage.getTitle());
		        		else
		        			classWiseResult.get(classIndex).remove(beginIdx-1);	// update class wise result arraylist
		        		if (range!=1) {
			        		for (int idx=endIdx; idx>=beginIdx; idx--) {	// add located slices to new class window, reversily
		        				if (!HyperStackUtility.addSlice(classImage, moveToClass, 4, idx, 0, true))	// add slice and delete in source
		        					System.out.println("Could not add slice " + idx + " from source image: " + classImage.getTitle());
		        				else
		        					classWiseResult.get(classIndex).remove(idx-1);	// update class wise result arraylist
		        			}
		        		}
		        	} else {
		        		moveToClass = trainingSamplesClassifierPanel.getClassImagePlus(className);
		        		for (int idx=endIdx; idx>=beginIdx; idx--) {
	        				if (!HyperStackUtility.addSlice(classImage, moveToClass, 4, idx, 0, true))	// add slice and delete in source
	        					System.out.println("Could not add slice " + idx + " from source image: " + classImage.getTitle());
	        				else
	        					classWiseResult.get(classIndex).remove(idx-1);	// update class wise result arraylist
	        			}
		        	}
		        	moveToClass.getWindow().setVisible(true);
		        	trainingSamplesClassifierPanel.updateTrainingSampleWindow();
		        } 
		    }); 
		    return moveSample;
		}

}
