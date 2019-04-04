/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package uk.ac.cam.cruk.mnlab;

import java.awt.Panel;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import org.scijava.prefs.DefaultPrefService;

import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;
import uk.ac.cam.cruk.mnlab.RoiManagerUtility;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.StackConverter;

/**
 *
 * @author Ziqiang Huang
 */
public class GenerateCellMask implements PlugIn {
	
	protected ImagePlus imp;
	protected static int impC = 1;
	protected static int impZ = 1;
	protected static int impT = 1;
	protected static double diameter = 200;
	protected static double sensitivity = 50;
	protected static double enlarge = 0;
	protected static Boolean display = false;

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
	            public void actionPerformed( ActionEvent e )  
	            {  
	                imp = WindowManager.getCurrentImage();
	                gd.repaint();
	                if (imp == null) IJ.log("No image is open!");
	            }  
	        }); 

		Panel customPane = new Panel();
		customPane.add(refresh);

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
		RoiManagerUtility.resetManager();
		RoiManager rm = RoiManager.getInstance2();
		RoiManagerUtility.hideManager();

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
		RoiManagerUtility.showManager();
		
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
