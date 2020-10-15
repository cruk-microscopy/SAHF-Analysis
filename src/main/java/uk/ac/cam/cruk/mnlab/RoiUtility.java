package uk.ac.cam.cruk.mnlab;

import ij.blob.ManyBlobs;

import ij.gui.NewImage;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.IJ;
import ij.ImagePlus;
import ij.macro.Interpreter;
import ij.plugin.frame.RoiManager;

import java.awt.Point;
import java.awt.Rectangle;

public class RoiUtility {

	public static Roi[] managerToRoiArray () {
		int rmState = checkManager();
		if (rmState != 2 && rmState != 4) // RoiManager need to be open with entry
			return null;
		RoiManager rm = RoiManager.getInstance();
		Roi[] rois = rm.getRoisAsArray();
		return rois;
	}
	
	public static void roiArrayToManager (
			Roi[] rois,
			Boolean modifyExist,
			Boolean append
			) {
		if (rois == null) return;
		RoiManager rm = RoiManager.getInstance();
		int rmState = checkManager();
		if (rmState == 2 || rmState == 4) {// RoiManager open with entry
			if (!modifyExist) return;
			if (!append) rm.reset();
		} else {
			rm = new RoiManager();
		}
		for (int i = 0; i < rois.length; i++) {
			rm.addRoi(rois[i]);
		}
	}
	
	public static Boolean isOpen() {
		RoiManager rm = RoiManager.getInstance();
		if (rm == null && IJ.isMacro()) {
			rm = Interpreter.getBatchModeRoiManager();
		}
		if (rm == null) return false;
		else return true;
	}
	
	public static Boolean isEmpty() {
		RoiManager rm = RoiManager.getInstance();
		if (isOpen()) {
			if (rm.getCount() == 0) return true;
			else return false;
		} else {
			// RoiManager not open
			// open a new RoiManager and return true
			// rm = new RoiManager();
			return true;
		}
	}
	
	public static Boolean isInterpolatable() {
		RoiManager rm = RoiManager.getInstance();
		if (isOpen()) {
			if (rm.getCount() >= 2) return true;
			else return false;
		} else {
			return false;
		}
	}
	public static Boolean isHidden() {
		RoiManager rm = RoiManager.getInstance();
		if (isOpen()) {
			return !rm.isVisible();
		} else {
			// RoiManager not open, return false
			return false;
		}
	}
	
	public static Point getLocation() {
		RoiManager rm = RoiManager.getInstance();
		if (!isOpen()) return null;
		return new Point(rm.getLocation());
	}
	
	public static void setLocation(Point p) {
		RoiManager rm = RoiManager.getInstance();
		if (!isOpen()) return;
		rm.setLocation(p);
	}
	
	public static int checkManager() {
		// RoiManager state:
		// 0: not open;
		// 1: open without entry;
		// 2: open with entries;
		// 3: macro batch mode;
		// 4: hidden without entry;
		// 5: hidden with entries;
		int state = 0;
		RoiManager rm = RoiManager.getInstance();
		if (rm == null) {
			if (IJ.isMacro()) {
				rm = Interpreter.getBatchModeRoiManager();
				if (rm != null) state = 3;
			} else state = 0;
		} else {
			if (rm.getCount() == 0) {
				if (rm.isVisible()) state = 1;
				else state = 4;
			}
			else {
				if (rm.isVisible()) state = 2;
				else state = 5;
			}
		}
		return state;
	}
	
	public static void resetManager() {
		RoiManager rm = RoiManager.getInstance();
		int state = checkManager();
		if (state == 0) {
			rm = new RoiManager();
		} else if (state > 1) {
			rm.reset();
		}
		return;
	}
	
	public static void closeManager() {
		RoiManager rm = RoiManager.getInstance();
		if (isOpen()) {
			rm.reset();
			rm.close();
		}
		return;
	}
	
	public static void hideManager() {
		RoiManager rm = RoiManager.getInstance();
		if (rm == null && IJ.isMacro()) {
			rm = Interpreter.getBatchModeRoiManager();
		}
		if (rm == null) return;
		rm.setVisible(false);
	}
	
	public static void showManager() {
		RoiManager rm = RoiManager.getInstance();
		if (rm == null && IJ.isMacro()) {
			rm = Interpreter.getBatchModeRoiManager();
		}
		if (rm == null) return;
		rm.setVisible(true);
	}
	
	/*
	 * Function to check if ROI has overlap region with image
	 */
	public static boolean checkRoiOverlap(
			ImagePlus imp,
			Roi roi
			) {
		if (roi==null)	return false;
		if (!roi.isArea())	return false;
		imp.setRoi(roi, false);
		if (imp.getProcessor().getStats().pixelCount==0)	return false;
		return true;
	}
	/*
	 * Function to check if ROI fully contained inside image
	 */
	public static boolean checkRoiContain(
			ImagePlus imp,
			Roi roi
			) {
		//roi.getStatistics().pixelCount;	//pixel count that rounded to pixel: e.g.: round with radius=2 will be 12.566
		//imp.setRoi(roi); imp.getProcessor().getStats().pixelCount	//pixel count that takes every pixel that overlap with the ROI: e.g.: round with radius=2 will be 16
		if (!checkRoiOverlap(imp, roi))	return false;
		if (roi.getBounds().x<0 || roi.getBounds().y<0)	return false;
		if ((roi.getBounds().x+roi.getBounds().width) > imp.getWidth())	return false;
		if ((roi.getBounds().y+roi.getBounds().height) > imp.getHeight())	return false;
		return true;
	}
	/*
	 * Function to crop ROI according to image
	 */
	public static Roi cropRoi(
			ImagePlus imp, 
			Roi roi
			) {
		
		if (roi==null)
			return null;
		if (imp==null)
			return roi;
		Rectangle b = roi.getBounds();
		int w = imp.getWidth();
		int h = imp.getHeight();
		if (b.x<0 || b.y<0 || b.x+b.width>w || b.y+b.height>h) {
			ShapeRoi shape1 = new ShapeRoi(roi);
			ShapeRoi shape2 = new ShapeRoi(new Roi(0, 0, w, h));
			roi = shape2.and(shape1);
		}
		if (roi.getBounds().width==0 || roi.getBounds().height==0)
			throw new IllegalArgumentException("Selection is outside the image");
		return roi;
	}
	/*
	 * Function to check if ROI shape is too irregular
	 */
	public static boolean checkRoiShape(
			Roi roi,
			double solidityLowBound	// default value: 0.90
			) {
		ImagePlus roiImage = NewImage.createByteImage("ROI Image", (int)(roi.getFloatWidth()*2), (int)(roi.getFloatHeight()*2), 1, NewImage.FILL_BLACK);
		roi.setLocation(roi.getFloatWidth()/2, roi.getFloatHeight()/2);
		roiImage.getProcessor().setColor(255);
		roiImage.getProcessor().fill(roi);
		ManyBlobs allBlobs = new ManyBlobs(roiImage); // Extended ArrayList
		allBlobs.setBackground(0); // 0 for black, 1 for 255
		allBlobs.findConnectedComponents(); // Start the Connected Component Algorithm
		if (allBlobs.size()>1) {	// only one connected-component allowed in the ROI
			roiImage.close(); System.gc();
			return false;
		}
		//int blobIdx = 0;
		//double area = allBlobs.get(blobIdx).getEnclosedArea();
		//if (area<(imageSize*imageSize/4))	return false;	// check if object area is too small, currently inactive
		double value = allBlobs.get(0).getSolidity();
		if (value<solidityLowBound)	{
			roiImage.close(); System.gc();
			return false;
		}
		roiImage.close(); System.gc();
		return true;	
	}
	
}
