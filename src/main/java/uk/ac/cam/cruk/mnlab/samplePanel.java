package uk.ac.cam.cruk.mnlab;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.math3.special.Erf;

import org.scijava.prefs.DefaultPrefService;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;

public class samplePanel {
	 
	
	protected static final int lineWidth = 40;
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
	protected static TrainingSamples trainingSamplesSamplePanel;
	
	protected static String trainingSamplePath = null;
	
	protected static JTable classTable;
	protected static DefaultTableModel tableModel;
	
	protected static final String classesBalanced = "Class sample sizes are balanced.";
	protected static final String noClassDefined = "No class defined!";
	
	protected static final double maxRatio = 2.0;	// allow one class no more than twice the size of another class
	protected static final double maxRangeRatio = 3.0;	// allow no more than 3 times difference between largest and smallest classes
	protected static final double zScoreTolerance = 0.55; // allow the minimum and maximum z-score of data point between [-0.5, 0.5]
	protected static final double ChauvenetCutOff = 0.5; // constant cutoff for Chauvenet's outlier detection algorithm
	
	public void createPanel() {}
		

	/* Functions to create and configure the training sample panel
	 * It needs a text area to display image information
	 * It needs two buttons:
	 * 	refresh source
	 * 	load training sample from disk
	 *	and currently not prioritized: fetch sample from active image with ROI (RoiManager)
	 * 	It also need to control the training cell image window(s).
	 */
	
	
	/* Functions to create and configure the class panel
	 * It needs a text area to display class information:
	 * 	Class Name:  Super_Low   Low   Intermediate   High
	 * 	Sample Size: 30          40     50             60
	 *  Sample size balanced./Sample size not balance!
	 * It needs three buttons:
	 * 	add, delete, rename
	 */
	@SuppressWarnings("serial")
	public static void addClassPanelToFrame (
			Frame f,
			TrainingSamples trainingSamples
			) throws Exception {
		// parse current active image in Fiji
		//ImagePlus source = null;
		//if (WindowManager.getImageCount()!=0)
		//	source = IJ.getImage();
		//checkSourceImage(imp);
		source = trainingSamples.getCurrentSource();
		trainingSamplesSamplePanel = trainingSamples;
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

		// create and configure the title panel "Training Sample and Classes"
		JLabel title = new JLabel("Training Sample and Class");
		title.setFont(panelTitleFont);
		title.setForeground(panelTitleColor);
		contentPanel.add(title);
		title.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		
		// HERE!
		//JTable table = new JTable();
		int nCol = 2; 
		int nRow = trainingSamples.getNClasses();
 		String[] columnNames = {"Class Name", "Sample Size"};
 		Object[][] data = new Object[nRow][nCol];
 		for (int i=0; i<nRow; i++) {
 			data[i][0] = trainingSamples.trainingSamples.get(i).getTitle();
 			data[i][1] = trainingSamples.trainingSamples.get(i).getNSlices();
 		}
 		tableModel = new DefaultTableModel(data, columnNames) {
 	        public boolean isCellEditable(int rowIndex, int mColIndex) {
 	            return false;
 	        }
 	    };
 		classTable = new JTable(tableModel) {
		    DefaultTableCellRenderer renderCenter = new DefaultTableCellRenderer();
		    { // initializer block
		        renderCenter.setHorizontalAlignment(SwingConstants.CENTER);
		    }
		    @Override
		    public TableCellRenderer getCellRenderer (int arg0, int arg1) {
		        return renderCenter;
		    } 
		};
		classTable.setEnabled(true);
		classTable.setBackground(f.getBackground());
		classTable.setPreferredSize(tablePreferred);
		classTable.setMaximumSize(tableMax);
        JScrollPane scrollPane = new JScrollPane(classTable);
        scrollPane.setBackground(f.getBackground());
        scrollPane.setPreferredSize(tablePreferred);
        scrollPane.setMaximumSize(tableMax);
		contentPanel.add(scrollPane);
		scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		JTextArea sampleInfo = new JTextArea();
		sampleInfo.setMaximumSize(textAreaMax);
		contentPanel.add(sampleInfo);
		sampleInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		JPanel buttonPanel = new JPanel();
		JButton btnAdd = new JButton("add");
		JButton btnDel = new JButton("delete");
		JButton btnRename = new JButton("rename");
		JButton btnLoad = new JButton("load");
		JButton btnLabel = new JButton("apply label");
		//btnAdd.setPreferredSize(buttonSize);
		// Use group layout for a JTextArea to display source image info
		// and 3 buttons horizontally aligned: refresh, load, resize
		GroupLayout buttonLayout = new GroupLayout(buttonPanel);
		buttonPanel.setLayout(buttonLayout);
		buttonLayout.setAutoCreateGaps(true);
		buttonLayout.setAutoCreateContainerGaps(true);

		buttonLayout.setHorizontalGroup(buttonLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
			 .addGroup(buttonLayout.createSequentialGroup()
			    .addComponent(btnAdd)
			    .addComponent(btnDel)
			    .addComponent(btnRename))
			 .addGroup(buttonLayout.createSequentialGroup()
				.addComponent(btnLoad)
			 	.addComponent(btnLabel)));
		buttonLayout.linkSize(SwingConstants.HORIZONTAL, btnAdd, btnDel, btnRename, btnLoad);			
		
		buttonLayout.setVerticalGroup(buttonLayout.createSequentialGroup()
			.addGroup(buttonLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
				.addGroup(buttonLayout.createSequentialGroup()
					.addComponent(btnAdd)
			        .addComponent(btnLoad))
				.addGroup(buttonLayout.createSequentialGroup()
	                .addComponent(btnDel)
	                .addComponent(btnLabel))
				.addGroup(buttonLayout.createSequentialGroup()
		            .addComponent(btnRename))));
	                
		buttonPanel.setBorder(border);
		buttonPanel.setBackground(f.getBackground());
		buttonPanel.setMaximumSize(panelMax);
		// configure the JTextArea to display source image info
		//sourceInfo.setBackground(textAreaColor);
		buttonPanel.setBackground(panelColor);
		contentPanel.add(buttonPanel);
		buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		sampleInfo.setMaximumSize(textAreaMax);
		sampleInfo.setEditable(false);
		String classInfo = getClassInfo(trainingSamples.trainingSamples);
		sampleInfo.setText(classInfo);
		if (classInfo.equals(noClassDefined)) {
			sampleInfo.setFont(errorFont);
			sampleInfo.setForeground(errorFontColor);
		} else {
			sampleInfo.setFont(textFont);
			sampleInfo.setForeground(fontColor);
		}
		
		// configure add button
		btnAdd.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) {addSampleClass();}		    	
		});
		// configure rename button
		btnDel.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) {deleteSampleClass();}
		});
		// configure resize button
		btnRename.addActionListener(new ActionListener() {
		    @Override 
		    public void actionPerformed(ActionEvent ae) {renameSampleClass();} 
		});
		// configure load button
		btnLoad.addActionListener(new ActionListener() {
		    @Override 
		    public void actionPerformed(ActionEvent ae) {loadSampleClass();} 
		});
		// configure label button
		btnLabel.addActionListener(new ActionListener() {
		    @Override 
		    public void actionPerformed(ActionEvent ae) {applyLabelToSource();} 
		});
		// add title and content panel to the parent panel, and finally add to plugin frame
		parentPanel.add(contentPanel);
		contentPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		// add real-time action listener to panel

		// add timer to refresh the sample panel (every 1023 milli second)
		Timer timer = new Timer(1023, new ActionListener() {
	        public void actionPerformed(ActionEvent evt) {
	        	source = trainingSamples.getCurrentSource();
	        	String classInfo = getClassInfo(trainingSamples.trainingSamples);
	    		sampleInfo.setText(classInfo);
	    		if (classInfo.equals(noClassDefined)) {
	    			sampleInfo.setFont(errorFont);
	    			sampleInfo.setForeground(errorFontColor);
	    		} else {
	    			sampleInfo.setFont(textFont);
	    			sampleInfo.setForeground(fontColor);
	    		}
	    		refreshTable();
	    		sampleInfo.repaint();	
	        }
	    });
		timer.start();
		
		f.add(parentPanel);
		f.pack();
	}
	
	protected static void addSampleClass() {
		GenericDialog gd = new GenericDialog("specify new class name and labels");
		gd.addStringField("Class Name", "");	// ROI index start with 0
		gd.addStringField("Class Label:", "");
		//gd.addStringField("label exclude:", "");
		gd.showDialog();
		if (gd.wasCanceled()) return;
		String newClassName = gd.getNextString();
		String newClassLabel = gd.getNextString();
		if (newClassName==null || newClassName.length()==0)	return;
		trainingSamplesSamplePanel.addNewClass(newClassName, newClassLabel);
		Object[] newRow = new Object[2];
		newRow[0] = newClassName;
		newRow[1] = trainingSamplesSamplePanel.getNSamples(newClassName);
		DefaultTableModel newModel = (DefaultTableModel)(classTable.getModel());
		newModel.addRow(newRow);
		classTable.setModel(newModel);
	}
	
	protected static void deleteSampleClass() {
		DefaultTableModel dtm = (DefaultTableModel) classTable.getModel();
	    int nRows = dtm.getRowCount();
	    int selectedRow = classTable.getSelectedRow();
	    String[] classNames = new String[nRows];
	    for (int i = 0 ; i < nRows ; i++) {
	    	classNames[i] = (String) dtm.getValueAt(i,0);
	    }
    	GenericDialog gd = new GenericDialog("specify class name to be deleted");
		gd.addChoice("Class to delete", classNames, classNames[selectedRow==-1?nRows-1:selectedRow]);
		gd.showDialog();
		if (gd.wasCanceled()) return;		
		int rowToDel = gd.getNextChoiceIndex();
		trainingSamplesSamplePanel.removeClass(classNames[rowToDel]);
		DefaultTableModel newModel = (DefaultTableModel)(classTable.getModel());
		newModel.removeRow(rowToDel);
		classTable.setModel(newModel);
	}
	
	protected static void renameSampleClass() {
		DefaultTableModel dtm = (DefaultTableModel) classTable.getModel();
		int nRows = dtm.getRowCount();
		int selectedRow = classTable.getSelectedRow();
	    String[] classNames = new String[nRows];
	    for (int i = 0 ; i < nRows ; i++) {
	    	classNames[i] = (String) dtm.getValueAt(i,0);
	    }
    	GenericDialog gd = new GenericDialog("specify class to rename");
		gd.addChoice("Class to rename", classNames, classNames[selectedRow==-1?nRows-1:selectedRow]);
		gd.addStringField("new name:", "");
		gd.showDialog();
		if (gd.wasCanceled()) return;		
		int rowRename = gd.getNextChoiceIndex();
		String newName = gd.getNextString();
		trainingSamplesSamplePanel.renameClass(classNames[rowRename], newName);
		DefaultTableModel newModel = (DefaultTableModel)(classTable.getModel());
		newModel.setValueAt(newName, rowRename, 0);
		classTable.setModel(newModel);
	}
	
	protected static void loadSampleClass() {
		// make use of scijava parameter persistence storage	
		DefaultPrefService prefs = new DefaultPrefService();
		trainingSamplePath = prefs.get(String.class, "SAHF-trainingSamplePath", trainingSamplePath);
		// prompt for user to choose a location and file name		
		GenericDialogPlus gd = new GenericDialogPlus("Load class image from file");
		gd.addFileField("File", trainingSamplePath, 40);
		gd.addStringField("Class Name", "Use Image Title", 12);
		gd.showDialog();
		if (gd.wasCanceled()) return;	
		trainingSamplePath = gd.getNextString();
		String className = gd.getNextString();
		
		ImagePlus newClassImage = IJ.openImage(trainingSamplePath);
		if (newClassImage==null) return;
		
		if (className==null || className.length()==0 || className.equals("Use Image Title")) {
			String imageTitle = newClassImage.getTitle();
			int dotIndex = imageTitle.lastIndexOf(".");
			if (dotIndex==-1)
				className = imageTitle;
			else
				className = imageTitle.substring(0, dotIndex);
		}
		if (className.length()>12)
			className = IJ.getString("Class Name too Long! Enter new class name here:", className);
		// store path back to scijava parameter persistence storage
		prefs.put(String.class, "SAHF-trainingSamplePath", trainingSamplePath);
		ArrayList<Integer> sliceIndex = new ArrayList<Integer>();
		for (int i=0; i<newClassImage.getNSlices(); i++) {
			sliceIndex.add(i+1);
		}
		trainingSamplesSamplePanel.addImageToClass(newClassImage, sliceIndex, className, true);
		newClassImage.changes=false; newClassImage.close(); System.gc();
		ArrayList<String> classNames = trainingSamplesSamplePanel.getClassNames();
		if (!classNames.contains(className)) {	// class name not exist, update table
			Object[] newRow = new Object[2];
			newRow[0] = className;
			newRow[1] = trainingSamplesSamplePanel.getNSamples(className);
			DefaultTableModel newModel = (DefaultTableModel)(classTable.getModel());
			newModel.addRow(newRow);
			classTable.setModel(newModel);
		}
	}
	protected static void refreshTable() {
		if (trainingSamplesSamplePanel==null || trainingSamplesSamplePanel.trainingSamples.size()==0) return;
		int nClasses = trainingSamplesSamplePanel.trainingSamples.size();
		String[] classNames = new String[nClasses]; int[] classSize = new int[nClasses];
		for (int i=0; i<nClasses; i++) {
			classNames[i] = trainingSamplesSamplePanel.trainingSamples.get(i).getTitle();
			classSize[i] = trainingSamplesSamplePanel.trainingSamples.get(i).getNSlices();
		}
		//DefaultTableModel dtm = (DefaultTableModel) classTable.getModel();
	    DefaultTableModel newModel = (DefaultTableModel)(classTable.getModel());
	    newModel.setRowCount(nClasses);
	    for (int i = 0 ; i < nClasses ; i++) {
	    	newModel.setValueAt(classNames[i], i, 0);
	    	newModel.setValueAt(classSize[i], i, 1);
	    }
		classTable.setModel(newModel);
	}
	
	protected static void applyLabelToSource() {
		DefaultTableModel dtm = (DefaultTableModel) classTable.getModel();
		int nRows = dtm.getRowCount();
		int selectedRow = classTable.getSelectedRow();
	    String[] classNames = new String[nRows];
	    for (int i = 0 ; i < nRows ; i++) {
	    	classNames[i] = (String) dtm.getValueAt(i,0);
	    }
    	GenericDialog gd = new GenericDialog("Apply Class Label to Source Image");
		gd.addChoice("Class: ", classNames, classNames[selectedRow==-1?nRows-1:selectedRow]);
		gd.addStringField("Label: ", "");
		gd.showDialog();
		if (gd.wasCanceled()) return;		
		int rowRename = gd.getNextChoiceIndex();
		String classLabel = gd.getNextString();
		trainingSamplesSamplePanel.applyClassLabelToSource(classNames[rowRename], classLabel);
	}
	// function to construct the class information string to be displayed to panel
	public static String getClassInfo (ArrayList<ImagePlus> trainingSamples) {
		if (trainingSamples.size()==0) return (noClassDefined);	// need error font
		String classSizeInfo = "";
		int nClasses = trainingSamples.size();
		String[] classNames = new String[nClasses]; int[] classSize = new int[nClasses];
		for (int i=0; i<nClasses; i++) {
			classNames[i] = trainingSamples.get(i).getTitle();
			classSize[i] = trainingSamples.get(i).getNSlices();
		}
		// get outlier from sample class size
		int[] outlier = getOutlier(classSize);
		// go through class to get class size info for each
		for (int i=0; i<nClasses; i++) {
			//System.out.println(i + " : " + outlier[i]);
			if (classSize[i] == 0) {
				classSizeInfo += ("class \"" + classNames[i] + "\" is empty!" + '\n');
			} else if (outlier[i] == -1) {
				classSizeInfo += ("too little \"" + classNames[i] + "\" samples!" + '\n');
			} else if (outlier[i] == 1) {
				classSizeInfo += ("too much \"" + classNames[i] + "\" samples!" + '\n');
			}	
		}
		// if no size error found, display balance info
		if (classSizeInfo.length()==0)
			classSizeInfo = classesBalanced;
		else
			classSizeInfo += "Class sample sizes are NOT balanced!";		
		return classSizeInfo;
	}
	
	// function to calculate mean value from integer array
	public static double getMean (int[] data) {
		if (data.length==0) return 0;
        int sum = 0;
        for (int i=0; i<data.length; i++){
            sum += data[i];
        }
       return (double)sum/(double)(data.length);
    }
	// function to calculate standard deviation from integer array (corrected for sample size)
	public static double getStdDev(int[] data) {
		if (data.length<=1) return 0;
	    double mean = getMean(data);
	    double sum2 = 0;
	    for (int i=0; i<data.length; i++){
	        sum2 += Math.pow(data[i]-mean, 2);
	    }
	    return Math.sqrt(sum2/(double)(data.length-1));
	}
	// function to calculate Z-score of data
	public static double[] getZScore(int[] data) {
		double[] zScore = new double[data.length];
		double mean = getMean(data);
		double stdDev = getStdDev(data);
		for (int i=0; i<data.length; i++) {
			zScore[i] = stdDev==0 ? 0 : (data[i]-mean)/stdDev;
		}
		return zScore;
	}
	// function to detect large value increment step (acount for small sample size problem when using IQR or Z-score based outlier detection
	public static ArrayList<Integer> getDiff(int[] data, boolean getLarge) {
		if (data == null) return null;
		ArrayList<Integer> diff = new ArrayList<Integer>();
		int N = data.length;
		ArrayList<Integer> dataList = new ArrayList<Integer>();
		for (int i=0; i<N; i++)
			dataList.add(data[i]);
		int[] dataCopy = data.clone();
		Arrays.sort(dataCopy);
		for (int i=1; i<N; i++) {
			if (dataCopy[i-1]==0) continue;
			if ((double)dataCopy[i]/(double)dataCopy[i-1] > maxRatio) {
				if (getLarge) 
					diff.add(dataList.indexOf(Integer.valueOf(dataCopy[i])));
				else
					diff.add(dataList.indexOf(Integer.valueOf(dataCopy[i-1])));
			}
		}
		if (dataCopy[N-1]/dataCopy[0]>maxRangeRatio) {
			if (getLarge) {
				int maxIdx = dataList.indexOf(Integer.valueOf(dataCopy[N-1]));
				if (!diff.contains(maxIdx)) diff.add(maxIdx);
			} else {
				int minIdx = dataList.indexOf(Integer.valueOf(dataCopy[0]));
				if (!diff.contains(minIdx)) diff.add(minIdx);
			}
		}
		return diff;
	}
	// function to get outlier based on Chauvenet's criterion: -1: too small, 1: too large
	public static int[] getOutlier(int[] data) {
		if (data.length==0) return null;
		int nSample = data.length;
		int[] decision = new int[nSample];
		if (nSample==1) return decision;	// sample size always balanced for only 1 class
		
		// in the case of more than 2 classes
		//int maxSize = 10;	// sample size more than 10, start to consider over fitting effect
		//int minSize = 2;	// need at least 2 sample in each class
		int validSize = 5;	// no need to perform computation when sample sizes are less than 6
		int maxSize = 0;
		for (int i=0; i<data.length; i++)
			maxSize = Math.max(data[i], maxSize);
		if (maxSize<=5)
			return decision;
		// get outlier from data
		ArrayList<Integer> stepBig = getDiff(data, true);
		ArrayList<Integer> stepSmall = getDiff(data, false);
		// get z score
		
		double[] zScore = getZScore(data);
		for (int i=0; i<nSample; i++) {
			if (stepBig.contains(i) && zScore[i]>zScoreTolerance && data[i]>validSize)
				decision[i] = 1;
			if (stepSmall.contains(i) && zScore[i]<-zScoreTolerance)
				decision[i] = -1;
			/*// depracate the Chauvenet check
			double zScoreAbs = Math.abs(zScore[i]);
			if (zScoreAbs<zScoreTolerance) continue;
			double p = Erf.erfc(zScoreAbs/Math.sqrt(2));
			if (p*(double)nSample<ChauvenetCutOff) {
				if (zScore[i]<0) {
					decision[i] = -1;
					continue;
				}
				if (zScore[i]>0 && data[i]>maxSize) {
					decision[i] = 1;
					continue;
				}
			}
			*/
			//System.out.println(i + " : " + decision[i]);
		}

		return decision;
	}
	// function to wrap long string at panel border
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

	
	
}
