package uk.ac.cam.cruk.mnlab;

import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileSystemView;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GUI;
import ij.gui.NonBlockingGenericDialog;
import ij.plugin.PlugIn;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.frame.RoiManager;
import weka.core.SerializationHelper;
import weka.classifiers.Classifier;

public class SAHFTrainableClassifier implements PlugIn {

	protected static ImagePlus sourceImage = null;	
	//protected static ImagePlus source = null;
	protected static TrainingSamples trainingSamples;
	
	protected ArrayList<String> classLabel = null;
	protected ArrayList<String> classAlias = null;
					
	
	//protected static boolean[] activeChannels;
	protected static String activeChannels = "1110";
	//protected static double probThreshold = 90;
	protected static final String[] statisticalFeatureString = {"Mean", "StdDev", "CV", "Skewness", "Kurtosis", "Entropy"};
	
	protected static Classifier cls = null;
	
	protected static ArrayList<Integer> featureChannels = null;
	protected static ArrayList<Integer> statFeatures = null;	//statistical features: 
	protected static ArrayList<String> classAttributes = null;
	
	protected static int feature = 1;
	
	public Boolean addDialog() {
		return true;
	}
	
	// temporary modify positive and negative mitosis sample stack

	public String getImageInfo (
			ImagePlus imp) {
		if (imp==null)	return ("No image recognized!");
		String imgTitle = "Image: " + imp.getTitle();
		String imgSzie = " size: "
				+ new DecimalFormat(".#").format(imp.getSizeInBytes()/1048576)
				+ "MB (" + String.valueOf(imp.getBitDepth()) + " bit)";
		int[] dim = imp.getDimensions();

		String imgRoi = imp.getRoi()==null?"Image does not have active ROI.":"Image has active ROI.";
		String imgOverlay = imp.getOverlay()==null?"Image does not have overlay.":"Image contains overlay.";
		
		String imgDimension = " X:" + String.valueOf(dim[0])
						   + ", Y:" + String.valueOf(dim[1])
						   + ", Z:" + String.valueOf(dim[3])
						   + ", C:" + String.valueOf(dim[2])
						   + ", T:" + String.valueOf(dim[4]);
		return (imgTitle + "\n" + imgSzie + "\n" + imgDimension + "\n" + imgRoi + "\n" + imgOverlay);
	}
	
	public String getRoiManagerInfo () {
		RoiManager rm = RoiManager.getInstance2();
		if (rm==null) {
			return ("ROI Manager is not open!");
		}
		int nROI = rm.getCount();
		
		String roiManagerInfo = String.valueOf(nROI) + " ROIs in Manager.";
		
		boolean indexAllMatch = true;
		ArrayList<Integer> unmatchIdx = new ArrayList<Integer>();
		ArrayList<String> labels = new ArrayList<String>();
		ArrayList<Integer> labelCounts = new ArrayList<Integer>();
		String roiName = null;
		String[] labelParts = null;
		for (int i=0; i<nROI; i++) {
			if (Integer.valueOf(rm.getName(i).split("\\D")[0])!=(i+1)) {
				indexAllMatch = false;
				unmatchIdx.add(i);
			}
			labelParts = rm.getName(i).split("-");
			if (labelParts.length>1) {
				if (!labels.contains(labelParts[1])) {
					labels.add(labelParts[1]);
					labelCounts.add(1);
				} else {
					int idx = labels.indexOf(labelParts[1]);
					labelCounts.set(idx, labelCounts.get(idx)+1);
				}
			}
		}

		
		String indexMatch = indexAllMatch?"all ROI name match with index.":"ROI name and index not match (check log).";
		if (!indexAllMatch) {
			IJ.log("Unmatched ROI: ");
			for (Integer idx : unmatchIdx) {
				IJ.log("   ROI: " + String.valueOf(idx+1) + " with name: " + rm.getName(idx));
			}
		}
		String roiLabel = "ROI labels: ";
		for (int i=0; i<labels.size(); i++) {
			roiLabel += labels.get(i) + " (" + String.valueOf(labelCounts.get(i))+"),  ";
		}
		roiLabel = roiLabel.substring(0, roiLabel.length()-2);
		
		return (roiManagerInfo + "\n" + indexMatch + "\n" + roiLabel);

	}

	public void addSamplePanel (
			Frame f
			) throws Exception {
		RoiManager rm = RoiManager.getInstance2();
		// configure and create the "Sample image and ROIs" panel
		Label sampleTitle = new Label("Sample Image and ROIs:");
		sampleTitle.setFont(new Font("Helvetica", Font.BOLD, 12));
		sampleTitle.setMaximumSize(new Dimension(500, 10));
		f.add(sampleTitle);
		
		JPanel samplePanel = new JPanel();
		samplePanel.setLayout(new BoxLayout (samplePanel, BoxLayout.Y_AXIS));
		samplePanel.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));
		samplePanel.setBackground(f.getBackground());
		samplePanel.setMaximumSize(new Dimension(500, 200));
		
		JTextArea imageInfo = new JTextArea(50, 8); 
		imageInfo.setEditable(false);
		imageInfo.setText(getImageInfo(sourceImage));
		if (sourceImage==null) {
			imageInfo.setFont(new Font("Helvetica", Font.BOLD, 12));
		}
		
		JTextArea roiInfo = new JTextArea(50, 8); 
		roiInfo.setEditable(false);
		roiInfo.setText(getRoiManagerInfo());
		if (rm==null) {
			roiInfo.setFont(new Font("Helvetica", Font.BOLD, 12));
		}
		JButton refresh = new JButton("refresh source");
		refresh.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) { 
		    	ImagePlus activeImage = WindowManager.getCurrentImage();
		    	if ( // filter sub-components from source image candidate 
					activeImage.getTitle().toLowerCase().contains("montage") ||
					activeImage.getTitle().toLowerCase().contains("histogram")
	    			) {
		    		// do nothing
		    	} else {
		    		sourceImage = activeImage;
		    	}
		    }
		});
		refresh.setPreferredSize(new Dimension(40, 40));
		imageInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
		roiInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
		refresh.setAlignmentX(Component.LEFT_ALIGNMENT);
		samplePanel.add(imageInfo);
		samplePanel.add(roiInfo);
		samplePanel.add(refresh);
		f.add(sampleTitle);
		f.add(samplePanel);
		//f.add(refresh);
	}

	public static void addClassificationPanel (
			Frame f
			//ImagePlus imp
			) throws Exception {
		
		//JPanel classifierPanel = new JPanel();
		//classifierPanel.setLayout(new BoxLayout (classifierPanel, BoxLayout.Y_AXIS));
		//classifierPanel.setBorder(new EmptyBorder(new Insets(10, 15, 10, 100)));
		//classifierPanel.setBackground(f.getBackground());
		cls = null;
		
		Label classifierTitle = new Label("Classification:");
		classifierTitle.setFont(new Font("Helvetica", Font.BOLD, 12));
		classifierTitle.setMaximumSize(new Dimension(500, 10));
		f.add(classifierTitle);
		
		JPanel classifierPanel = new JPanel();
		classifierPanel.setLayout(new GridLayout(4, 2));
		//classifierButtonPanel.setMaximumSize(new Dimension(120, 100));
		classifierPanel.setBorder(new EmptyBorder(new Insets(10, 55, 10, 10)));
		classifierPanel.setBackground(f.getBackground());
		
		JTextArea classifierInfo = new JTextArea(30, 8); 
		classifierInfo.setEditable(false);
		
		
		
		Button checkClassifier = new Button("check classifier");
		checkClassifier.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) { 
				if (cls==null)	{
					classifierInfo.setText("No classifier.");
					return;
				}
				
		    } 
		});
		
		Button loadClassifier = new Button("load classifier");
		loadClassifier.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) { 
		    	
		    	String modelPath = null;
		    	JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());

				int returnValue = jfc.showOpenDialog(null);
				// int returnValue = jfc.showSaveDialog(null);

				if (returnValue == JFileChooser.APPROVE_OPTION) {
					File selectedFile = jfc.getSelectedFile();
					modelPath = selectedFile.getAbsolutePath();
				}
		    
				/*
		    	OpenDialog od = new OpenDialog("Load classifier");
				String modelPath = od.getPath();
				*/
				if (modelPath==null)	{
					return;
				}
				
				try {
					cls = (Classifier) SerializationHelper.read(modelPath);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					classifierInfo.setText("Failed to load classifier.");
					e.printStackTrace();
				}
		    } 
		});
		
		Button applyClassifier = new Button("apply classifier");
		applyClassifier.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) { 
		    	
		    }
		});
		
		Button trainClassifier = new Button("train classifier");
		trainClassifier.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) { 
		    	
		    }
		});
		
		Button exportClassifier = new Button("export classifier");
		exportClassifier.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) { 
		    	if (cls==null) {
		    		classifierInfo.setText("No trained classifier found!\nTrain a classifier first, and then export.");
		    		return;
		    	}
		    	String savePath = null;
		    	JFileChooser fileChooser = new JFileChooser();
		    	fileChooser.setDialogTitle("Specify a file to save");
		    	int userSelection = fileChooser.showSaveDialog(f);
		    	if (userSelection == JFileChooser.APPROVE_OPTION) {
		    	    savePath = fileChooser.getSelectedFile().getAbsolutePath();
		    	}
		    	if (savePath==null)	return;
				try {
					SerializationHelper.write(savePath,cls);
					String firstLine = savePath; int sepIdx = 0; int loopCount=0;
					while(firstLine.length()>40) {
						loopCount++;
						if (loopCount>10)	break;
						sepIdx = firstLine.lastIndexOf(File.separator);
						firstLine = firstLine.substring(0, sepIdx);
					}
					String displaySavePath = firstLine + "\n" + savePath.substring(sepIdx, savePath.length());
					classifierInfo.setText("classifier saved as:\n" + displaySavePath);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					classifierInfo.setText("Failed to save classifier.");
					e.printStackTrace();
				}
		    } 
		});
		
		Button configureClassifier = new Button("configure classifier");
		configureClassifier.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) {
				
		    	//boolean[] location = [true, false, false];
		    	
		    	

		    	final Font highlightFont = new Font("Helvetica", Font.BOLD, 12);
		    	NonBlockingGenericDialog gd = new NonBlockingGenericDialog("Classify data (ROI):");


		    	String locationFeatureMessage = "Feature:";
		    	gd.setInsets(20,0,0);
		    	gd.addMessage(locationFeatureMessage, highlightFont, Color.blue);

		    	String roiFeatureMessage = "Image(ROI) size:";
		    	gd.setInsets(10,0,0);
		    	gd.addMessage(roiFeatureMessage, highlightFont);
		    	gd.setInsets(0,130,0);
		    	gd.addCheckbox("regularize ROI size", true);
		    	gd.setInsets(-5,0,0);
		    	gd.addSlider("unify to (pixel):", 32, 256, 128, 32);
		    	
		    	
		    	gd.setInsets(10, 20, 0 );
		    	String[] headingsLocalization = {"Localization:"};
		    	String[] labelsLocalization = {"whole", "edge", "center"};
		    	boolean[] statesLocalization = {true, false, false};
		    	gd.addCheckboxGroup(1, 3, labelsLocalization, statesLocalization, headingsLocalization);
		    	gd.setInsets(-5, 20, 0 );
		    	gd.addSlider("center-edge (µm):", 0, 100, 5, 1);
		    	gd.setInsets(-5, 20, 0 );
		    	gd.addSlider("edge width (µm):", 0, 100, 5, 1);

		    	gd.setInsets(10, 20, 0 );
		    	String[] headingsStats = {"Statistics"};
		    	String[] labelsStats = {"mean", "stdDev", "CV", "skewness", "kurtosis", "entropy"};
		    	boolean[] statesStats = {true, true, true, true, true, false};
		    	gd.addCheckboxGroup(2, 3, labelsStats, statesStats, headingsStats);

		    	String classMessage = "Classes:";
		    	gd.setInsets(20,0,0);
		    	gd.addMessage(classMessage, highlightFont, Color.blue);
		    	
		    	gd.setInsets(0,0,0);
		    	gd.addStringField("Class 1:", "", 10);
		    	
		    	
		    	
		    	gd.showDialog();
		    	if (gd.wasCanceled()) return;


			}
		});
		
		classifierPanel.add(classifierInfo);
		classifierPanel.add(checkClassifier);
		classifierPanel.add(loadClassifier);
		classifierPanel.add(applyClassifier);
		classifierPanel.add(trainClassifier);
		classifierPanel.add(exportClassifier);
		classifierPanel.add(configureClassifier);
		
		classifierPanel.setMinimumSize(new Dimension(100, 300));
		classifierPanel.setMaximumSize(new Dimension(300, 500));
		//classifierPanel.setAlignmentX(Component.LEFT_ALIGNMENT );
		
		f.add(classifierPanel);
	}
	
	public static void createClassifierPanel(
			//ImagePlus imp
			) throws Exception {
		/* create a frontal panel with 3 major component: sample image and ROIs; labels, classifier
		 * 1, source image stack info;
		 * 2, classes
		 * 3, classifier information;
		 * 4, button groups;
		 * 
		 * 
		 * 
		 * 
		 * 
		 */
		trainingSamples = new TrainingSamples();
		//if (WindowManager.getImageCount()!=0)
		//	source = IJ.getImage();
		//checkSourceImage(imp);
		sourceImage = trainingSamples.getCurrentSource();
		
		PlugInFrame pf = new PlugInFrame("SAHF classifier");
		pf.setLayout(new BoxLayout(pf, BoxLayout.Y_AXIS));
		
		sourceImagePanel.addSourcePanelToFrame(pf, trainingSamples);
		samplePanel.addClassPanelToFrame(pf, trainingSamples);
		classifierPanel.addClassifierPanelToFrame(pf, trainingSamples);
		
		/*
		JPanel classifierPanel = new JPanel();
		classifierPanel.setLayout(new BoxLayout (classifierPanel, BoxLayout.Y_AXIS));
		classifierPanel.setBorder(new EmptyBorder(new Insets(10, 15, 10, 100)));
		classifierPanel.setBackground(pf.getBackground());
		
		JPanel classifierButtonPanel = new JPanel();
		classifierButtonPanel.setLayout(new GridLayout(4, 2, 3, 3));
		classifierButtonPanel.setBorder(new EmptyBorder(new Insets(10, 15, 10, 100)));
		classifierButtonPanel.setBackground(pf.getBackground());

		Button refreshSource = new Button("refresh source stack");
		Button addSampleClass = new Button("add class");
		Button applyClassifier = new Button("apply classifier");
		Button trainClassifier = new Button("train classifier");
		Button loadClassifier = new Button("load classifier");
		Button exportClassifier = new Button("export classifier");
		Button configureClassifier = new Button("configure classifier");

		classifierButtonPanel.add(refreshSource);
		classifierButtonPanel.add(addSampleClass);
		classifierButtonPanel.add(applyClassifier);
		classifierButtonPanel.add(trainClassifier);
		classifierButtonPanel.add(loadClassifier);
		classifierButtonPanel.add(exportClassifier);
		classifierButtonPanel.add(configureClassifier);
		classifierPanel.add(classifierButtonPanel);
		*/
		
		/*
		refreshSource.addActionListener(new ActionListener() {
			@Override 
		    public void actionPerformed(ActionEvent ae) { this.refreshSource();}
		});
		addSampleClass.addActionListener(new ActionListener() {
			@Override 
		    public void actionPerformed(ActionEvent ae) { this.addSampleClass();}
		});
		applyClassifier.addActionListener(new ActionListener() {
			@Override 
		    public void actionPerformed(ActionEvent ae) { this.applyClassifier();}
		});
		trainClassifier.addActionListener(new ActionListener() {
			@Override 
		    public void actionPerformed(ActionEvent ae) { this.trainClassifier();}
		});
		loadClassifier.addActionListener(new ActionListener() {
			@Override 
		    public void actionPerformed(ActionEvent ae) {this.loadClassifier();}
		});
		exportClassifier.addActionListener(new ActionListener() {
			@Override 
		    public void actionPerformed(ActionEvent ae) { this.exportClassifier();}
		});
		configureClassifier.addActionListener(new ActionListener() {
			@Override 
		    public void actionPerformed(ActionEvent ae) { this.configureClassifier();}
		});
		*/
		
		//pf.add(classifierPanel);
		
		WindowManager.addWindow(pf);
					
		pf.pack();
		//pf.setSize(positiveSample.getWindow().getWidth(), (int) (positiveSample.getWindow().getHeight()*1.3));
		pf.setSize(337, 800);
		//pf.setMaximumSize(new Dimension(300, 500));
		pf.setMaximumSize(new Dimension(337, 880));
		pf.setMinimumSize(new Dimension(337, 480));
		pf.setVisible(true);
		pf.setLocationRelativeTo(null);
		GUI.center(pf);
		//pf.setResizable(false);	//fix the plugin frame size
		
		//addSamplePanel(pf);
		
		//pf.add(labelTitle);
		//pf.add(labelPanel);
		//pf.add(classifierTitle);
		//addClassificationPanel(pf);

		/*
		Timer timer = new Timer(550, new ActionListener() {
	        public void actionPerformed(ActionEvent evt) {
	        	
	        	
	        	
	        	// handle updated status of the label panel:
	        	int positiveSampleSize = positiveSample.getStack().size();
	        	int negativeSampleSize = negativeSample.getStack().size();
	        	
		    	positiveLabel.setText("positive samples: " + String.valueOf(positiveSampleSize));
		    	negativeLabel.setText("negative samples: " + String.valueOf(negativeSampleSize));
		    	
		    	String labelBalanceText = "Training sample balanced.";
				labelBalanceText = (negativeSampleSize<positiveSampleSize)?"Unbalanced training samples!\nNot enough negative sample labels.":labelBalanceText;
				labelBalanceText = (positiveSampleSize<negativeSampleSize/10)?"Unbalanced training samples!\nNot enough positive sample labels.":labelBalanceText;
				labelBalanceLabel.setFont(new Font("Helvetica", Font.BOLD, 12));
				labelBalanceLabel.setText(labelBalanceText);
		    	
				labelPanel.repaint();
				
				
	        }
	    });
		timer.start();
		*/
	}
	
	@Override
	public void run(String arg) {
		System.gc();
		try {
			createClassifierPanel();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.gc();	
	}
	
	public static void main(String[] args) {
		
		String [] ij_args = 
			{ "-Dplugins.dir=C:/Fiji.app/plugins",
			"-Dmacros.dir=C:/Fiji.app/macros" };

		ij.ImageJ.main(ij_args);
		
		SAHFTrainableClassifier sc = new SAHFTrainableClassifier();
		sc.run(null);

	}
	
	
	
}
