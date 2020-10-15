/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package uk.ac.cam.cruk.mnlab;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import org.scijava.prefs.DefaultPrefService;

import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;
import uk.ac.cam.cruk.mnlab.RoiUtility;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

/**
 *
 * @author Ziqiang Huang
 */
public class GenerateCellMask implements PlugIn {
	
	protected ImagePlus imp = null;
	protected static int impC = 1;
	protected static int impZ = 1;
	protected static int impT = 1;
	protected static double diameter = 200;
	protected static double sensitivity = 50;
	protected static double enlarge = 0;
	protected static Boolean display = false;
	
	
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
	protected static final Dimension panelMax = new Dimension(500, 200);
	protected static final Dimension panelMin = new Dimension(380, 200);
	protected static final Dimension buttonSize = new Dimension(90, 10);
	protected static JTextArea sourceInfo;

	@Override
	public void run(String arg) {
		
		// install 3rd party plugin
		boolean installMexicanHatFilter = false;
		if (!PluginUtility.pluginCheck("Mexican_Hat_Filter")) {
			installMexicanHatFilter = PluginUtility.installMexicanHatFilter();
			if (!installMexicanHatFilter) {
				IJ.log("   \"Laplacian of Gaussian Filter\" not installed!");
				IJ.log("   Consider installing it manually.");
				return;
			}
		}
		boolean installMorpholibJ = false;
		if (!PluginUtility.pluginCheck("MorphoLibJ")) {
			installMorpholibJ = PluginUtility.installMorpholibJ();
			if (!installMorpholibJ) {
				IJ.log("   \"IJPB plugin\" not installed!");
				IJ.log("   Install it manually by adding IJPB-plugins to update sites of Fiji.");
				return;
			}
		}
		// Ask User to restart Fiji after newly installed plugin
		if (installMexicanHatFilter || installMorpholibJ) {
			IJ.log("   3rd party plugin installed.");
			IJ.log("   Restart Fiji to run the RSOM analysis plugin.");
			return;
		}
		
		
		imp = WindowManager.getCurrentImage();
		
		
		
		
		/* THIS
		PlugInFrame f = new PlugInFrame("Laplacian of Gaussian ROI delineator");
		f.setLayout(new BoxLayout(f, BoxLayout.Y_AXIS));
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
		// create and configure the title panel "Sample image and ROIs"
		JLabel title = new JLabel("Source Info");
		title.setFont(panelTitleFont);
		title.setForeground(panelTitleColor);
		contentPanel.add(title);
		title.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		sourceInfo = new JTextArea();
		sourceInfo.setMaximumSize(textAreaMax);
		sourceInfo.setEditable(false);
		contentPanel.add(sourceInfo);
		sourceInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		JPanel buttonPanel = new JPanel();
		JButton btnRefresh = new JButton("refresh");
		JButton btnLoad = new JButton("ROI");
		JButton btnPrepare = new JButton("measure");

		//btnRefresh.setPreferredSize(buttonSize);
		// Use group layout for a JTextArea to display source image info
		// and 3 buttons horizontally aligned: refresh, load, resize
		
		
		buttonPanel.setBorder(border);
		buttonPanel.setBackground(panelColor);
		buttonPanel.setMaximumSize(panelMax);
		
		contentPanel.add(buttonPanel);
		buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		// configure the JTextArea to display source image info
		//sourceInfo.setBackground(textAreaColor);
		
		sourceInfo.setText(getImageInfo(source));
		if (source==null) {
			sourceInfo.setFont(errorFont);
			sourceInfo.setForeground(errorFontColor);
		}
		// configure refresh button
		btnRefresh.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) {refreshSource();}
		});
		// configure load button
		btnLoad.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) {loadNewSource();}
		});
		// configure prepare button
		btnPrepare.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) {prepareSource();}
		});
		// configure resize button
		btnResize.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) {resizeSource();}
		});
		// add title and content panel to the parent panel, and finally add to plugin frame
		parentPanel.add(contentPanel);
		contentPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

		//f.add(parentPanel, BorderLayout.NORTH);
		f.add(parentPanel);
		f.pack();
		
		///THIS
		*/
		
		NonBlockingGenericDialog gd = new NonBlockingGenericDialog("Auto Cell ROI delineator");

		//gd.addNumericField("C", impC, 0);
		//gd.addNumericField("Z", impZ, 0);
		//gd.addNumericField("T", impT, 0);
		if (imp == null) {
			gd.addSlider("C", 1, 1, 1, 1);
			gd.addSlider("Z", 1, 1, 1, 1);
			gd.addSlider("T", 1, 1, 1, 1);
		} else {
			gd.addSlider("C", 1, imp.getNChannels(), imp.getC(), 1);
			gd.addSlider("Z", 1, imp.getNSlices(), imp.getZ(), 1);
			gd.addSlider("T", 1, imp.getNFrames(), imp.getT(), 1);
		}
		gd.addSlider("cell diameter (pixel)", 0, 500, diameter);
		gd.addSlider("cell detection sensitivity", 0, 100, sensitivity);
		gd.addSlider("shrink-enlarge ROI (pixel)", -50, 50, enlarge);
		
		gd.addCheckbox("display mask image", display);


		JButton refresh = new JButton("Refresh source");
		refresh.addActionListener (new ActionListener()  
	        {  
	            @Override
				public void actionPerformed( ActionEvent e )  
	            {  
	                imp = WindowManager.getCurrentImage();
	                gd.repaint();
	                if (imp == null) IJ.log("No image is open!");
	            }  
	        }); 
		
		JButton measure = new JButton("Measure source");
		measure.addActionListener (new ActionListener() {  
            @Override
			public void actionPerformed( ActionEvent e ) {  
                if (imp == null) return;
                if (imp.getOverlay()==null) return;
                
                int[] dim = imp.getDimensions();
                int numC = dim[2];
                int numZ = dim[3];
                int numT = dim[4];
                if (numT>1) return;
                
                Calibration cal = imp.getCalibration();
                boolean calibrated = cal.getUnit().toLowerCase().equals("micron");
                double pixelSize = cal.pixelWidth;
                
                double[] scaleFactorArea = new double[numZ];
                for (int z=0; z<numZ; z++) {
                	scaleFactorArea[z] = 1;
                	double xScale = 1.0; double yScale = 1.0;
                	String label = imp.getStack().getSliceLabel(imp.getStackIndex(1, z+1, 1));
					int scaleIdx = label.indexOf("Scale:");
					if (scaleIdx==-1) {
						System.out.println("Scale string not detected in image " + imp.getTitle());
						continue;
					}
					String[] scaleString = label.substring(scaleIdx+6, label.length()).split(",");
					if (scaleString.length!=3) {
						System.out.println("Scale string format wrong in image " + imp.getTitle());
						continue;
					}
					String xScaleString = scaleString[1];
					String yScaleString = scaleString[2];
					xScale = Double.valueOf(xScaleString.substring(1, xScaleString.length())); // xScale = current/original
					yScale = Double.valueOf(yScaleString.substring(1, yScaleString.length()));
					scaleFactorArea[z] = (1/xScale)*(1/yScale);
				}

                Roi roi = imp.getRoi();
                imp.deleteRoi();
                ImagePlus impDup = imp.duplicate();
                imp.setRoi(roi);

                Overlay overlay = imp.getOverlay();
                Roi[] RoiArray = overlay.toArray().clone();

                ResultsTable table = new ResultsTable();

                for (int i=0; i<RoiArray.length; i++) {
                	Roi r = RoiArray[i];
                	if (!r.getName().toLowerCase().equals("all")) continue;
                	impDup.setRoi(r);
                	for (int c=0; c<numC; c++) {
                		table.incrementCounter();
                		table.addValue("Sample", r.getZPosition());
                		table.addValue("Channel", c+1);
                		impDup.setPositionWithoutUpdate(c+1, r.getZPosition(), 1);
                		ImageStatistics stat = impDup.getStatistics();
                		double area = stat.area * scaleFactorArea[r.getZPosition()-1];
                		double mean = stat.mean;
                		double stdDev = stat.stdDev;

                		table.addValue(calibrated ? "area (µm^2)" : "area (pixel^2)", area);
                		table.addValue("mean", mean);
                		table.addValue("total", area*mean);
                		table.addValue("standard deviation", stdDev);
                		table.addValue("CV", stdDev/mean);
                	}
                }
                table.show(imp.getTitle() + "Measurement");
                impDup.changes=true; impDup.close(); System.gc();
            }  
        }); 

		Panel customPane = new Panel();
		customPane.add(refresh);
		customPane.add(measure);

		gd.addPanel(customPane);

		gd.showDialog();
		if (gd.wasCanceled())	return;
		if (imp == null)	imp = WindowManager.getCurrentImage();
		if (imp == null) {
			IJ.log("No image is open!");
			return;
		}

		impC = (int)gd.getNextNumber();
		impZ = (int)gd.getNextNumber();
		impT = (int)gd.getNextNumber();
		diameter = gd.getNextNumber();
		sensitivity = gd.getNextNumber();
		enlarge = gd.getNextNumber();
		display = gd.getNextBoolean();
		//int diameter = 200;
		int filterSize = (int)(diameter/20);
		double minimumSize = Math.PI*(diameter/2)*(diameter/2)/25; // smallest cell would be 1/5 of the input diameter
		//println(minimumSize);

		// prepare RoiManager for operation
		RoiUtility.resetManager();
		RoiManager rm = RoiManager.getInstance2();
		RoiUtility.hideManager();

		Roi r = imp.getRoi();
		imp.deleteRoi();
		ImagePlus imp2 = imp.duplicate();
		if (r!=null) {
			imp.setRoi(r);	
		}
		if(imp2.getType() != ImagePlus.GRAY8) {
			IJ.run(imp2, "8-bit", "");
		}
		imp2.getCalibration().pixelWidth = 1.0;
		imp2.getCalibration().pixelHeight = 1.0;
		imp2.getCalibration().pixelDepth = 1.0;
		imp2.getCalibration().setUnit("pixel");

		imp2.setC(impC);
		imp2.setZ(impZ);
		imp2.setT(impT);

		
		// do Gaussian blur
		//ImageProcessor ip = imp2.getProcessor();
		//new GaussianBlur().blurGaussian(ip, filterSize);
		
		// do LaplacianOfGaussian filtering
		IJ.run(imp2, "Mexican Hat Filter", "radius="+filterSize);
		
		// do Otsu thresholding
		
		ImageProcessor ip = imp2.getProcessor();
		
		/* Use LoG image statistics to determine thresholding level 
		ImageStatistics stats = imp2.getRawStatistics();
		double dElements = stats.pixelCount;
		double dMean = stats.mean;
		double dMaximum = stats.max;
		double dMinimum = stats.min;
		double dADevSum = 0.0;
		for (int i=0; i<(int)dElements-1; ++i) {
			dADevSum += Math.abs(ip.getf(i) - dMean);
		}
		double dADeviation = dADevSum/dElements;
		double initLowbound = dMean + dADeviation;
		double alpha = (sensitivity-50)/5;	// normalize sensitivity to [-10, 10];
		//lowBoundModify = alpha>0 ? (dMaximum-initLowbound)*alpha : dADeviation*alpha;
		initLowbound += alpha*dADeviation;
		initLowbound = Math.min(dMaximum, initLowbound);	// make sure lowbound don't go beyond maximum
		initLowbound = Math.max(dMinimum, initLowbound);	// make sure lowbound don't go below minimum
		*/
		double dMaximum = imp2.getRawStatistics().max;
		double dMinimum = imp2.getRawStatistics().min;
		IJ.setAutoThreshold(imp2, "Otsu dark no-reset");
		double initLowbound = imp2.getProcessor().getMinThreshold();
		double alpha = (sensitivity-50)/5;	// normalize sensitivity to [-10, 10];
		double step = (dMaximum - initLowbound)/10;
		initLowbound += alpha*step;
		initLowbound = Math.max(dMinimum, initLowbound);	// make sure lowbound don't go below minimum
		IJ.setThreshold(imp2, initLowbound, dMaximum);
		//IJ.log(String.valueOf(initLowbound));

		ImagePlus mask = new ImagePlus("mask", ip.createMask());
		imp2.close();
		
		// use morphological filter to close contour inside cell
		Strel strel = Strel.Shape.DISK.fromRadius(filterSize/2);	// create structuring element (cube of radius 'radius')
		ImagePlus imp_closed = new ImagePlus("closed", Morphology.closing(mask.getProcessor(), strel));
		//mask.close();
		//ImagePlus imp_opened = new ImagePlus("opened", Morphology.opening(imp_closed.getProcessor(), strel));
		//imp_closed.close();
		//IJ.run(imp3, "Analyze Particles...", "size="+minimumSize+"-Infinity pixel exclude include add");
		//IJ.run(mask, "Median...", "radius="+(filterSize*2));
		IJ.run(imp_closed, "Analyze Particles...", "size="+minimumSize+"-Infinity pixel include add");
		imp_closed.close();
		
		imp.getWindow().setVisible(false);
		
		imp.setC(impC);
		imp.setZ(impZ);
		imp.setT(impT);
		for (int i=0; i<rm.getCount(); i++) {
			rm.select(i);
			rm.rename(i, String.valueOf(i+1));
			IJ.run(imp, "Enlarge...", "enlarge="+enlarge+" pixel");
			rm.runCommand(imp,"Update");
		}
		imp.getWindow().setVisible(true);
		imp.deleteRoi();
		RoiUtility.showManager();
		
		if (display) {
			mask.show();
		} else {
			mask.close();
		}
	}

	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu

		DefaultPrefService prefs = new DefaultPrefService();
		
		impC = prefs.getInt(Integer.class, "persistedDouble", impC);
		impZ = prefs.getInt(Integer.class, "persistedDouble", impZ);
		impT = prefs.getInt(Integer.class, "persistedDouble", impT);
		diameter = prefs.getDouble(Double.class, "persistedDouble", diameter);
		sensitivity = prefs.getDouble(Double.class, "persistedDouble", sensitivity);
		enlarge = prefs.getDouble(Double.class, "persistedDouble", enlarge);
		display = prefs.getBoolean(Boolean.class, "persistedBoolean", display);
		
		GenerateCellMask gc = new GenerateCellMask();
		gc.run(null);
	}

}
