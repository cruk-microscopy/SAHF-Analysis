package uk.ac.cam.cruk.mnlab;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.scijava.prefs.DefaultPrefService;

import fiji.util.gui.GenericDialogPlus;
import ij.blob.*;

import ij.IJ;
import ij.ImageStack;
import ij.ImagePlus;

import ij.gui.NewImage;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.measure.Calibration;
import ij.plugin.ChannelSplitter;
import ij.plugin.Duplicator;
import ij.plugin.HyperStackConverter;
import ij.plugin.PlugIn;
import ij.plugin.RGBStackMerge;
import ij.plugin.frame.RoiManager;
import ij.plugin.RoiEnlarger;
import ij.plugin.RoiScaler;
import ij.plugin.ZProjector;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.LUT;
import loci.common.services.ServiceException;
import loci.formats.ChannelSeparator;
import loci.formats.FormatException;
import loci.formats.meta.MetadataRetrieve;
import loci.formats.services.OMEXMLServiceImpl;
import loci.plugins.util.ImageProcessorReader;
import loci.plugins.util.LociPrefs;
import ome.units.UNITS;

public class LoadCellsIntoStack implements PlugIn {
	
	protected final static Color[] Colors = 
		{Color.YELLOW, Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA, Color.CYAN, 
				Color.ORANGE, Color.PINK, Color.GRAY, Color.WHITE, Color.BLACK};
	protected final static String[] colorStrings =
		{"YELLOW", "RED", "GREEN", "BLUE", "MAGENTA", "CYAN", 
				"ORANGE", "PINK", "GRAY", "WHITE", "BLACK"};
	protected final static String[] thresholdMethods = 
		{"Triangle","Li","Default","Huang","Intermodes","IsoData",
				"IJ_IsoData","MaxEntropy","Mean","MinError","Minimum","Moments",
				"Otsu","Percentile","RenyiEntropy","Shanbhag","Yen"};

	protected static String inputFilePath = IJ.getDir("home");
	protected static File inputFile = new File(inputFilePath);
	protected static boolean doSubDir = false;
	protected final static String[] extensions = {"lif", "czi", "tif"};
	protected static String extension = extensions[0];
	
	protected static String channelString = "0";	// if target channel is 0, then extract all channels
	protected final static String[] zOptions = 
		{"each Z slice", "Mean-projection", 
				"Max-projection", "Sum-projection", "Median-projection"};
	protected final static String[] zProjectorParam = 
		{"empty entry", "avg all", "max all", "sum all", "median all"};
	protected static int zOption = 0;	// z option index: 0: each z slice; 1: mean projection; 2: max projection; 3: sum projection; 4: median projection
	
	protected static int cellColor = 5;
	protected static double cellMinSize = 75.0; // minimum allowed cell area 75 µm^2
	protected static double shapeGoodnessLevel = 0.55;
	protected static boolean keepScale = false;
	protected static double scaleFactor = 1.0;	// keep calibration and scale in image 
	protected static int imageSize = 256;	//pixel
	protected static int imageMargin = 10;	//pixel
	
	// cell outline ROI generation option
	protected static boolean loadRoi = true;
	protected static int thresholdMethod = 0;
	
	protected static double edgeSize = 1.5;	// micron
	protected static boolean doSubPixel = false;
	
	protected static final String roiNameAll = WekaUtility.localizationFeatureString[0];
	protected static final String roiNameCenter = WekaUtility.localizationFeatureString[1];
	protected static final String roiNameEdge = WekaUtility.localizationFeatureString[2];
	
	protected static boolean doRoiAll = true;
	protected static boolean doRoiCenter = true;
	protected static boolean doRoiEdge = true;
	
	protected static boolean drawOverlay = true;
	protected static int colorAll = 0;
	protected static int colorCenter = 1;
	protected static int colorEdge = 2;
	
	protected static int numTotal = 0;
	protected static int numCurrent = 0;
	protected static String loadFinishInfo = "";
	
	public static void loadCellImages() throws IOException, FormatException, ServiceException, NullPointerException {
		/*
		 *  load image and roi pairs into a image stack: 
		 *  If there's a roiset with same name, load cells based on ROI,
		 *   and also label slice with ROI name;
		 *  Otherwise, load cells based on thresholding and connected-component analysis 
		 */		
		if (!userDialog()) {
			loadFinishInfo = "Loading cancelled.";
			return;
		}
		double start = System.currentTimeMillis();
		String date = GetDateAndTime.getCurrentDate();
		String time = GetDateAndTime.getCurrentTime();
		IJ.log("\n\nCell loading starts at:");
		IJ.log(date + " " + time);
		//boolean managerOpen = true; 
		//Roi[] originalRois = RoiManagerUtility.managerToRoiArray();
		RoiManager rm = RoiManager.getInstance();
		if (rm==null)	rm = new RoiManager();
		else rm.reset();
		rm.setVisible(false);		
		
		String[] extensions = {extension};
		if (extension.equals("tif")) extensions = new String[] {"tif", "tiff"};
		int[] targetChannels = parseChannelString(channelString);	// target channels are 1-based
		
		ArrayList<File> imageFileList = new ArrayList<File>();
		if (inputFile.isFile())	imageFileList.add(inputFile);
		else imageFileList = new ArrayList<File>(FileUtils.listFiles(inputFile, extensions, doSubDir));

		ImagePlus cellImageStack = new ImagePlus();
		
		if (loadRoi && extension.equals("tif")) {
			cellImageStack = getCellsFromImageRoiPair (
					imageFileList, keepScale, imageSize, imageMargin, cellMinSize,
					shapeGoodnessLevel, edgeSize, doSubPixel, Colors[cellColor],
					doRoiAll, doRoiCenter, doRoiEdge, 
					Colors[colorAll], Colors[colorCenter], Colors[colorEdge]);
		} else {
			ArrayList<ImagePlus> imageList = new ArrayList<ImagePlus>();
			switch(extension) {
			case "lif":
				for (File imageFile : imageFileList) {
					imageList.addAll(lifFileLoader(imageFile, targetChannels, zOption));
				}
				break;
			case "czi":
				for (File imageFile : imageFileList) {
					imageList.addAll(cziFileLoader(imageFile, targetChannels, zOption));
				}
				break;
			case "tif":
				for (File imageFile : imageFileList) {
					imageList.add(tifFileLoader(imageFile, targetChannels, zOption));
				}
				break;
			}
			
			//System.out.println(imageList.get(0).getTitle());
			cellImageStack = getCellsFromImageList(
				imageList, keepScale, imageSize, imageMargin,
				thresholdMethod, cellMinSize, shapeGoodnessLevel, edgeSize, doSubPixel, Colors[cellColor],
				doRoiAll, doRoiCenter, doRoiEdge, 
				Colors[colorAll], Colors[colorCenter], Colors[colorEdge]);
		}
		if (cellImageStack!=null) {
			cellImageStack.setTitle("Cell images from "+inputFile.getName());
			IJ.run(cellImageStack, "Enhance Contrast", "saturated=0.35 stack");
			cellImageStack.show();
		}

		rm.setVisible(true); rm.close();
		double duration = (System.currentTimeMillis()-start)/1000;
		IJ.log("Cell loading complete after " + String.valueOf(duration) + " seconds.");
	}
	
	public static boolean userDialog() {
		// first load from system prefs the saved persisted parameters
		DefaultPrefService prefs = new DefaultPrefService();
		inputFilePath = prefs.get(String.class, "SAHF-inputFilePath", inputFilePath);
		doSubDir = prefs.getBoolean(Boolean.class, "SAHF-doSubDir", doSubDir);
		extension = prefs.get(String.class, "SAHF-extension", extension);
		channelString = prefs.get(String.class, "SAHF-channelString", channelString);
		zOption = prefs.getInt(Integer.class, "SAHF-zOption", zOption);
		keepScale = prefs.getBoolean(Boolean.class, "SAHF-keepScale", keepScale);
		imageSize = prefs.getInt(Integer.class, "SAHF-imageSize", imageSize);
		imageMargin = prefs.getInt(Integer.class, "SAHF-imageMargin", imageMargin);
		cellColor = prefs.getInt(Integer.class, "SAHF-cellColor", cellColor);
		cellMinSize = prefs.getDouble(Double.class, "SAHF-cellMinSize", cellMinSize);
		shapeGoodnessLevel = prefs.getDouble(Double.class, "SAHF-shapeGoodnessLevel", shapeGoodnessLevel);
		loadRoi = prefs.getBoolean(Boolean.class, "SAHF-loadRoi", loadRoi);
		thresholdMethod = prefs.getInt(Integer.class, "SAHF-thresholdMethod", thresholdMethod);
		drawOverlay = prefs.getBoolean(Boolean.class, "SAHF-drawOverlay", drawOverlay);
		doSubPixel = prefs.getBoolean(Boolean.class, "SAHF-doSubPixel", doSubPixel);
		edgeSize = prefs.getDouble(Double.class, "SAHF-edgeSize", edgeSize);
		doRoiAll = prefs.getBoolean(Boolean.class, "SAHF-doRoiAll", doRoiAll);
		colorAll = prefs.getInt(Integer.class, "SAHF-colorAll", colorAll);
		doRoiCenter = prefs.getBoolean(Boolean.class, "SAHF-doRoiCenter", doRoiCenter);
		colorCenter = prefs.getInt(Integer.class, "SAHF-colorCenter", colorCenter);
		doRoiEdge = prefs.getBoolean(Boolean.class, "SAHF-doRoiEdge", doRoiEdge);
		colorEdge = prefs.getInt(Integer.class, "SAHF-colorEdge", colorEdge);
		// create user dialog
		final Font highlightFont = new Font("Helvetica", Font.BOLD, 12);
		
		GenericDialogPlus gdp = new GenericDialogPlus("Load Cell Images");
		// file open options:
		String fileOpenMessage = "File import options:";
		gdp.setInsets(10,0,0);
		gdp.addMessage(fileOpenMessage, highlightFont);
		gdp.addDirectoryOrFileField("input folder", inputFilePath);
		gdp.addCheckbox("search sub-folder", doSubDir);
		gdp.addChoice("file extension", extensions, extension);
		
		// cell image display options:
		String cellImageMessage = "Cell display options:";
		gdp.setInsets(10,0,0);
		gdp.addMessage(cellImageMessage, highlightFont);
		gdp.addStringField("extract channel:", channelString, 0);
		gdp.addChoice("Z slice option", zOptions, zOptions[zOption]);
		gdp.addCheckbox("keep original scale", keepScale);
		gdp.addNumericField("image window size", imageSize, 0, 3, "pixel");
		gdp.addNumericField("image margin size", imageMargin, 0, 2, "pixel");
		gdp.addChoice("cell color (LUT)", colorStrings, colorStrings[cellColor]);
		gdp.addNumericField("minimum cell size", cellMinSize, 1, 4, "µm^2");
		gdp.addNumericField("cell shape goodness", shapeGoodnessLevel*100, 1, 3, "%");
		gdp.setInsets(0,0,0);
		gdp.addMessage("thresholding method for auto cell delineation:");
		gdp.addChoice("method", thresholdMethods, thresholdMethods[thresholdMethod]);
		
		// ROI and overlay options:
		String cellRoiMessage = "ROI and Overlay options:";
		gdp.setInsets(10,0,0);
		gdp.addMessage(cellRoiMessage, highlightFont);
		gdp.addCheckbox("load image-coupled ROIset if exist", loadRoi);
		gdp.addCheckbox("draw ROI as overlay", drawOverlay);
		gdp.addCheckbox("sub-pixel smoothing", doSubPixel);
		gdp.addNumericField("nuclei edge size", edgeSize, 1, 2, "micron");
		gdp.addCheckbox("ROI all", doRoiAll);
		gdp.addToSameRow();
		gdp.addChoice("overlay color", colorStrings, colorStrings[colorAll]);
		gdp.addCheckbox("ROI center", doRoiCenter);
		gdp.addToSameRow();
		gdp.addChoice("overlay color", colorStrings, colorStrings[colorCenter]);
		gdp.addCheckbox("ROI edge", doRoiEdge);
		gdp.addToSameRow();
		gdp.addChoice("overlay color", colorStrings, colorStrings[colorEdge]);
		
		String html = "<html>"
				
				+"<h2>Cell Image Loader (SAHF plugin)</h2>"
				 
				+" version: 1.4.0<br>"
				 +" date: 2019.09.17<br>"
				 +" author: Ziqiang Huang (Ziqiang.Huang@cruk.cam.ac.uk)<br><br>"
				
				 +"<h3>Description:</h3>"
					+" The Cell Image Loader detect and crop around cell, nucleus, or any other oval<br>"
					+" shaped objects that bearing fluorescent signal in given image file(s). It recognize<br>"
					+" certain image files by file extension: tif, lif, czi. It can also load pre-defined<br>"
					+" ROIs that coupled with image file.<br><br>"
	
				+"<h3>Principle:</h3>"
					+" The Cell Image Loader detect and crop objects based on intensity thresholding, <br>"
					+" smoothing, connected-component analysis, and morphological filtering. The thresholding<br>"
					+" method has to be one of the Fiji inbuilt method.<br>" 
					+" A candidate object will be recognized as thresholded pixels connected together.<br>"
					+" A morphological filter is then applied to remove non-ideal objects based on<br>"
					+" \"shape goodness\", which defined as the product of solidity and thinnes ratio<br>"
					+" of the given object. Based on user defined parameter, 3 region of interest (ROI)<br>"
					+" can be created and displayed onto the cell images.<br>"
					+" ROI all is the cell outline, ROI center is the center region of the cell that shrinked<br>"
					+" propotionally from the cell boundary  for given distance, and finally the ROI edge<br>"
					+" is a donut-shaped region of ROI all after exclusion of ROI center.<br><br>" 

				+"<h3>Usage:</h3>"
					+" The user need to speficy either a file or a folder as input, and also the targeted<br>"
					+" file type by its extension. If thooce search sub-folder then the plugin will search<br>"
					+" all sub-folders rooted from the input folder, for all files matching the file type.<br>"
					+" Notice if choose to load image-coupled ROIset, then for each located image file, the<br>"
					+" plugin will look for a file with the same name but \".zip\" or \".roi\" extension and<br>"
					+" take that as the ROI input.<br>"
					+" Consequently it will skip the thresholding, smoothing, and connected-component analysis<br>"
					+" steps, and directly apply the shape goodness filtering.<br>"
					+" The user can further define the range of data desired to be extract: channel and slice<br>"
					+" number<br>"
					+" For the purpose of image classification, all of the cell images need to be unified to<br>"
					+" the same size. Therefore, the user can either choose to keep the original scale so it<br>"
					+" simply copy paste from the input image, or choose to rescale the image so all the extracted<br>"
					+" cells will be unified to similar size that can fill the image in both horizontal and<br>"
					+" vertical axis.<br>"
					+" The user can also define different color (Look-Up Table, LUT) for the cell, and all 3<br>"
					+" location ROIs.<br>"
					+" Finally it's relatively important to decide for a meaningful edge size, so that later<br>"
					+" we can separate well and fair the edge and center regions of a cell, regardless its<br>"
					+" original scale and shape.<br>"
					+" Please note many times the X and Y axis are not scaled equally especially for elongated or<br>"
					+" flat cells So the edge size in X axis and in Y axis can also appeared to be quite different.<br>";
		
		gdp.addHelp(html);
		
		
		gdp.showDialog();
		if (gdp.wasCanceled()) return false;
		inputFilePath = gdp.getNextString();
		inputFile = new File(inputFilePath);
		if (!inputFile.exists())	return false;
		doSubDir = gdp.getNextBoolean();
		extension = extensions[gdp.getNextChoiceIndex()];
		channelString = gdp.getNextString();
		zOption = gdp.getNextChoiceIndex();
		keepScale = gdp.getNextBoolean();
		imageSize = (int)gdp.getNextNumber();
		imageMargin = (int)gdp.getNextNumber();
		cellColor = gdp.getNextChoiceIndex();
		cellMinSize = gdp.getNextNumber();
		shapeGoodnessLevel = gdp.getNextNumber()/100;
		thresholdMethod = gdp.getNextChoiceIndex();
		loadRoi = gdp.getNextBoolean();
		drawOverlay = gdp.getNextBoolean();
		doSubPixel = gdp.getNextBoolean();
		edgeSize = gdp.getNextNumber();
		doRoiAll = gdp.getNextBoolean();
		colorAll = gdp.getNextChoiceIndex();
		doRoiCenter = gdp.getNextBoolean();
		colorCenter = gdp.getNextChoiceIndex();
		doRoiEdge = gdp.getNextBoolean();
		colorEdge = gdp.getNextChoiceIndex();
		// save newly entered parameters into system prefs
		prefs.put(String.class, "SAHF-inputFilePath", inputFilePath);
		prefs.put(Boolean.class, "SAHF-doSubDir", doSubDir);
		prefs.put(String.class, "SAHF-extension", extension);
		prefs.put(Integer.class, "SAHF-channelString", channelString);
		prefs.put(Integer.class, "SAHF-zOption", zOption);
		prefs.put(Boolean.class, "SAHF-keepScale", keepScale);
		prefs.put(Integer.class, "SAHF-imageSize", imageSize);
		prefs.put(Integer.class, "SAHF-imageMargin", imageMargin);
		prefs.put(Integer.class, "SAHF-cellColor", cellColor);
		prefs.put(Double.class, "SAHF-cellMinSize", cellMinSize);
		prefs.put(Double.class, "SAHF-shapeGoodnessLevel", shapeGoodnessLevel);
		prefs.put(Boolean.class, "SAHF-loadRoi", loadRoi);
		prefs.put(Integer.class, "SAHF-thresholdMethod", thresholdMethod);
		prefs.put(Boolean.class, "SAHF-drawOverlay", drawOverlay);
		prefs.put(Boolean.class, "SAHF-doSubPixel", doSubPixel);
		prefs.put(Double.class, "SAHF-edgeSize", edgeSize);
		prefs.put(Boolean.class, "SAHF-doRoiAll", doRoiAll);
		prefs.put(Integer.class, "SAHF-colorAll", colorAll);
		prefs.put(Boolean.class, "SAHF-doRoiCenter", doRoiCenter);
		prefs.put(Integer.class, "SAHF-colorCenter", colorCenter);
		prefs.put(Boolean.class, "SAHF-doRoiEdge", doRoiEdge);
		prefs.put(Integer.class, "SAHF-colorEdge", colorEdge);
		return true;
	}
	
	public double getLoadingPercent() {		
		return numCurrent/numTotal;
	}
	
	public static String getLoadingFinishInfo() {
		return loadFinishInfo;
	}
	
	/*
	 * Functions to load 2D image(s) into a ImagePlus from file.
	 */
	public static ImagePlus tifFileLoader(
			File tifFile,
			int[] targetChannels,	//
			int zOption) throws IOException {
		
		ImagePlus tifStackImage = IJ.openImage(tifFile.getCanonicalPath());
		int numC = tifStackImage.getNChannels();

		int nChannels = targetChannels.length;
		
		//if (tifStackImage.getNChannels()<targetChannel) return null;

		ImagePlus channelImage = null;
		
		if (targetChannels[0]!=0 && numC>1) {	// check if extract all channels
			ImagePlus[] allChannels = ChannelSplitter.split(tifStackImage);
			if (nChannels==1) {	// extract single channel
				if (targetChannels[0]<numC)	// control the channel number not exceed beyond source image
					channelImage = allChannels[targetChannels[0]-1];
			} else {
				ImagePlus[] channelImages = new ImagePlus[nChannels];
				for (int i=0; i<nChannels; i++) {
					if (targetChannels[i]>=numC)	continue;
					channelImages[i] = allChannels[targetChannels[i]-1];
				}
				channelImage = RGBStackMerge.mergeChannels(channelImages, false);
			}
		} else {
			channelImage = tifStackImage.duplicate();
		}
		channelImage.setTitle(tifStackImage.getTitle());

		if (tifStackImage.getNFrames()>1)
			IJ.log(" Image " + tifFile.getName() + " appeared to have more than 1 time points!");
		if (tifStackImage.getNSlices()>1) {
			IJ.log(" Image " + tifFile.getName() + " appeared to have more than 1 Z slice!");
			IJ.log("   extract " + zOptions[zOption] + " images.");
			if (zOption==0)
				IJ.run(channelImage, "Hyperstack to Stack", "");
			else
				channelImage = ZProjector.run(channelImage, zProjectorParam[zOption]);
		}
		
		
		
		//ImagePlus[] channelImages = {channels[0], channels[2], channels[3]};
		//RGBStackMerge.mergeChannels(newImage, false).show();
		
		return channelImage;
	}
	
	public static ArrayList<ImagePlus> lifFileLoader(
			File lifFile,
			int[] targetChannels,
			int zOption) throws IOException, FormatException, ServiceException {
		ArrayList<ImagePlus> finalImageList = new ArrayList<ImagePlus>();
		ImageProcessorReader  r = new ImageProcessorReader(new ChannelSeparator(LociPrefs.makeImageReader()));
		OMEXMLServiceImpl OMEXMLService = new OMEXMLServiceImpl();
		r.setMetadataStore(OMEXMLService.createOMEXMLMetadata());
		r.setId(lifFile.getCanonicalPath());
		MetadataRetrieve meta = (MetadataRetrieve) r.getMetadataStore();
		int numSeries = meta.getImageCount();
		r.setSeries(0);
		
		int nChannels = targetChannels.length;
		int principleChannel = targetChannels[0];
		//ImageStack stack = new ImageStack(r.getSizeX(), r.getSizeY());
		for (int i = 0; i < numSeries; i++) {
			r.setSeries(i);
			// temp stack to store hyperstack images
			ImageStack seriesStack = new ImageStack(r.getSizeX(), r.getSizeY());
			// get physical calibration from meta
			Calibration cali = new Calibration();
			cali.pixelWidth = meta.getPixelsPhysicalSizeX(i).value(UNITS.MICROMETER).doubleValue();
			cali.pixelHeight = meta.getPixelsPhysicalSizeY(i).value(UNITS.MICROMETER).doubleValue();
			if (r.getSizeZ() > 1)
				cali.pixelDepth = meta.getPixelsPhysicalSizeZ(i).value(UNITS.MICROMETER).doubleValue();
			cali.setUnit("micron");
			// get (hyperstack)image dimensions
			int numC = r.getSizeC(); 
			//if (numC<targetChannel)	continue;
			
			int numT = r.getSizeT();
			if (numT>1) 
				IJ.log("Image " + meta.getImageName(i) + "appeared to have more than 1 time points!");
			int numZ = r.getSizeZ(); 
			if (numZ>1) {
				IJ.log("Image " + meta.getImageName(i) + "appeared to have more than 1 Z slice!");
				IJ.log("   extract " + zOptions[zOption] + " images.");
			}
			
			if (nChannels==1 && principleChannel!=0) {	// extract single channel
				if (numC<principleChannel) continue;
				for (int n = principleChannel-1; n < r.getImageCount(); n+=numC) {
					ImageProcessor ip = r.openProcessors(n)[0];
					if (ip.getHeight()!=seriesStack.getHeight())	continue;
					seriesStack.addSlice(meta.getImageName(i), ip);
				}
			} else if (principleChannel!=0) {	// extract multiple channel
				for (int n = 0; n < r.getImageCount(); n+=numC) {
					for (int c=0; c<nChannels; c++) {
						int cNum = targetChannels[c];
						ImageProcessor ip = r.openProcessors(n+cNum-1)[0];
						if (ip.getHeight()!=seriesStack.getHeight())	continue;
						seriesStack.addSlice(meta.getImageName(i), ip);
					}	
				}
			} else {	// extract all channels
				nChannels = numC;
				for (int n = 0; n < r.getImageCount(); n++) {
					ImageProcessor ip = r.openProcessors(n)[0];
					//System.out.println("bit depth: " + ip.getBitDepth());	// HERE
					if (ip.getHeight()!=seriesStack.getHeight())	continue;
					seriesStack.addSlice(meta.getImageName(i), ip);	
				}
			}
			
			/*
			for (int n = targetChannel-1; n < r.getImageCount(); n+=numC) {
				ImageProcessor ip = r.openProcessors(n)[0];
				if (ip.getHeight()!=seriesStack.getHeight())	continue;
				seriesStack.addSlice(meta.getImageName(i), ip);
			}
			*/
			
			ImagePlus seriesImage = new ImagePlus(lifFile.getName() + ", " + meta.getImageName(i), seriesStack);
			if (seriesImage.isStack() && seriesImage.getStackSize()!=1)
				seriesImage = HyperStackConverter.toHyperStack(seriesImage, nChannels, numZ, numT);
			
			if (numZ>1 && zOption!=0) {
				seriesImage = ZProjector.run(seriesImage, zProjectorParam[zOption]);
				seriesImage.setTitle(lifFile.getName() + ", " + meta.getImageName(i)+"("+zOptions[zOption]+")");
				cali.pixelDepth = cali.pixelDepth * numZ;
			}
			seriesImage.setCalibration(cali);
			finalImageList.add(seriesImage);
		}
		r.close(); System.gc();
		//if (finalImageList.size()==0)	return null;
		return finalImageList;
	}
	
	public static ArrayList<ImagePlus> cziFileLoader(
			File cziFile,
			int[] targetChannels,
			int zOption) throws IOException, FormatException, ServiceException{
		ArrayList<ImagePlus> finalImageList = new ArrayList<ImagePlus>();
		ImageProcessorReader  r = new ImageProcessorReader(new ChannelSeparator(LociPrefs.makeImageReader()));
		OMEXMLServiceImpl OMEXMLService = new OMEXMLServiceImpl();
		r.setMetadataStore(OMEXMLService.createOMEXMLMetadata());
		r.setId(cziFile.getCanonicalPath());
		MetadataRetrieve meta = (MetadataRetrieve) r.getMetadataStore();
		int numSeries = meta.getImageCount();
		r.setSeries(0);
		
		int nChannels = targetChannels.length;
		int principleChannel = targetChannels[0];
		//ImageStack stack = new ImageStack(r.getSizeX(), r.getSizeY());
		for (int i = 0; i < numSeries; i++) {
			r.setSeries(i);
			// temp stack to store hyperstack images
			ImageStack seriesStack = new ImageStack(r.getSizeX(), r.getSizeY());
			// get physical calibration from meta
			Calibration cali = new Calibration();
			cali.pixelWidth = meta.getPixelsPhysicalSizeX(i).value(UNITS.MICROMETER).doubleValue();
			cali.pixelHeight = meta.getPixelsPhysicalSizeY(i).value(UNITS.MICROMETER).doubleValue();
			if (r.getSizeZ() > 1)
				cali.pixelDepth = meta.getPixelsPhysicalSizeZ(i).value(UNITS.MICROMETER).doubleValue();
			cali.setUnit("micron");
			// get (hyperstack)image dimensions
			int numC = r.getSizeC(); 
			//if (numC<targetChannel)	continue;
			int numT = r.getSizeT();
			if (numT>1) 
				IJ.log("Image " + meta.getImageName(i) + "appeared to have more than 1 time points!");
			int numZ = r.getSizeZ(); 
			if (numZ>1) {
				IJ.log("Image " + meta.getImageName(i) + "appeared to have more than 1 Z slice!");
				IJ.log("   extract " + zOptions[zOption] + " images.");
			}
			if (nChannels==1 && principleChannel!=0) {	// extract single channel
				if (numC<principleChannel) continue;
				for (int n = principleChannel-1; n < r.getImageCount(); n+=numC) {
					ImageProcessor ip = r.openProcessors(n)[0];
					if (ip.getHeight()!=seriesStack.getHeight())	continue;
					seriesStack.addSlice(meta.getImageName(i), ip);
				}
			} else if (principleChannel!=0) {	// extract multiple channel
				for (int n = 0; n < r.getImageCount(); n+=numC) {
					for (int c=0; c<nChannels; c++) {
						int cNum = targetChannels[c];
						ImageProcessor ip = r.openProcessors(n+cNum-1)[0];
						if (ip.getHeight()!=seriesStack.getHeight())	continue;
						seriesStack.addSlice(meta.getImageName(i), ip);
					}	
				}
			} else {	// extract all channels
				nChannels = numC;
				for (int n = 0; n < r.getImageCount(); n++) {
					ImageProcessor ip = r.openProcessors(n)[0];
					if (ip.getHeight()!=seriesStack.getHeight())	continue;
					seriesStack.addSlice(meta.getImageName(i), ip);	
				}
			}
			
			/*
			for (int n = targetChannel-1; n < r.getImageCount(); n+=numC) {
				ImageProcessor ip = r.openProcessors(n)[0];
				if (ip.getHeight()!=seriesStack.getHeight())	continue;
				seriesStack.addSlice(meta.getImageName(i), ip);
			}
			*/
			
			ImagePlus seriesImage = new ImagePlus(cziFile.getName() + ", " + meta.getImageName(i), seriesStack);
			seriesImage = HyperStackConverter.toHyperStack(seriesImage, nChannels, numZ, numT);
			
			if (numZ>1 && zOption!=0) {
				seriesImage = ZProjector.run(seriesImage, zProjectorParam[zOption]);
				seriesImage.setTitle(cziFile.getName() + ", " + meta.getImageName(i)+"("+zOptions[zOption]+")");
				cali.pixelDepth = cali.pixelDepth * numZ;
			}
			seriesImage.setCalibration(cali);
			finalImageList.add(seriesImage);
		}
		r.close(); System.gc();
		//if (finalImageList.size()==0)	return null;
		return finalImageList;
	}
	
	/*
	 * Functions to extract cell image from a list of ImagePlus
	 *  Cell ROI will be generated using ImageJ thresholding method
	 *  The center and edge will be delineated
	 *  function will return a stack contain all the recognized cell images.
	 */
	public static ImagePlus getCellsFromImageList(
			ArrayList<ImagePlus> impList,	// physical calibrated image list, with same pixel size
			boolean keepScale,
			int size,	// in pixel
			int margin,	// in pixel
			int thresholdMethod,
			double minSize,
			double shapeGoodness,	// 0 - 1
			double edge,	// in micron
			boolean doRoiSmoothing,
			Color cellColor,
			boolean roiAll,
			boolean roiCenter,
			boolean roiEdge,
			Color roiColorAll,
			Color roiColorCenter,
			Color roiColorEdge
			) {
		//ImageStack stack = new ImageStack(size, size);
		RoiManager rm = RoiManager.getInstance();
		if (rm==null) rm = new RoiManager();
		else rm.reset();
		rm.setVisible(false);
		// if we found cell larger than defined image size,
		// resize the image window
		// only in the case of using original scale
		ArrayList<ImagePlus> cellImageList = new ArrayList<ImagePlus>();
		Overlay overlay = new Overlay();
		int maxSize = size;	
		int finalStackIndex = 0;
		IJ.log(" Loading cell images using " + thresholdMethods[thresholdMethod] + "  threshold method:");
		int nImages = impList.size(); int nFiles = 0; String fileName = "";
		numTotal = nImages;
		//impList.get(0).show(); // HERE!!!
		for (ImagePlus imp : impList) {
			numCurrent = impList.indexOf(imp)+1;
			
			IJ.log("\\Update:   Processing image " + String.valueOf(numCurrent)+ 
					" of in total " + String.valueOf(numTotal));			
			if (imp==null)	{
				nImages--;
				continue;
			}
			
			//System.out.println("bit depth: " + imp.getBitDepth());
			
			int dotIdx = (int)Math.max(0, imp.getTitle().indexOf(".")); // we recognize image name by removing the extension
			String imageFileName = imp.getTitle().substring(0, dotIdx);
			if (!imageFileName.equals(fileName)) {
				nFiles++;
				fileName = imageFileName;
			}
			IJ.showStatus("Extracting cells from image: " + imp.getTitle());
			// control image physical calibration
			//int nSlices = imp.getStackSize();
			int nSlices = imp.getNSlices();		// get number of Z slices;
			
			double pixelSize = 1.0;	// default pixel size 1.0 pixel
			String calUnit = imp.getCalibration().getUnit();
			if (calUnit.equals("micron") || calUnit.equals("μm") || calUnit.equals("um")) {
				pixelSize = imp.getCalibration().pixelWidth;
			//} else if (calUnit.equals("inch")) {	// extra PAINNNNN has to be taken for the bloody "inch" countries
			//	imp.setCalibration(null);
			} else {
				calUnit = "pixel";
				pixelSize = 0.2;	// if not calibrated use 0.2 micron / pixel as pixel size
				IJ.log(" Image: " + imp.getTitle() + " is not calibrated! Use pixel for edge calculation.");
			}
			String scale = String.format("%.2f", pixelSize) + calUnit + ",";
			double edgeInPixel = edge/pixelSize;
			//IJ.log(imp.getTitle() + ", pixel size: " + pixelSize + ", edge: " + String.valueOf(edgeInPixel));
			for (int i=0; i<nSlices; i++) {
				IJ.showStatus("Extracting cells in slice " + String.valueOf(i) + 
						" of " + String.valueOf(nSlices));
				rm.reset();
				imp.deleteRoi();
				//ImagePlus imp2 = new Duplicator().run(imp, i+1, i+1);
				ImagePlus imp2 = new Duplicator().run(imp, 1, 1, i+1, i+1, 1, 1); //THIS??HERE?? which one is target channel
				//imp2.hide();
				//ImagePlus imp3 =imp2.duplicate();
				IJ.setAutoThreshold(imp2, thresholdMethods[thresholdMethod]+" dark");
				//double threshold = imp2.getProcessor().getMinThreshold();
				IJ.run(imp2, "Median...", "radius=["+String.valueOf(edgeInPixel)+"]");
				// minimum cell size should be a circle with radius equals to edge size
				//double minAreaInPixel = 4*edgeInPixel*edgeInPixel*Math.PI;	
				double minAreaInPixel = minSize/pixelSize;
				
				//imp2.show();
				IJ.run(imp2, "Analyze Particles...", 
						"size=["+String.valueOf(minAreaInPixel)+"]-Infinity pixel exclude include add");
				int nROI = rm.getCount();
				
				// make sure each cell has enough border to be cropped
				int widthWithMargin = imp.getWidth()+margin*2;
				int heightWithMargin = imp.getHeight()+margin*2;
				IJ.run(imp, "Canvas Size...", "width=["+widthWithMargin+"] height=["+heightWithMargin+"] position=Center zero");
				for (int r=0; r<nROI; r++) {
					Roi currentRoi = (Roi) rm.getRoi(r).clone();
					if (!checkShape(rm.getRoi(r), shapeGoodness)) continue;
					int roiCenterX = (int)(currentRoi.getXBase() + currentRoi.getFloatWidth()/2);
					int roiCenterY = (int)(currentRoi.getYBase() + currentRoi.getFloatHeight()/2);
					
					Roi enlargedRoi = RoiEnlarger.enlarge(currentRoi, margin);
					maxSize = Math.max(maxSize, enlargedRoi.getBounds().width);
					maxSize = Math.max(maxSize, enlargedRoi.getBounds().height);
					enlargedRoi.setLocation(currentRoi.getBounds().getX(), currentRoi.getBounds().getY());
					imp.setSliceWithoutUpdate(i+1); //imp.setC(targetChannel);
					imp.setRoi(enlargedRoi);
					
					//ImagePlus impCrop = imp.crop();
					
					ImagePlus impCrop = new Duplicator().run(imp, 1, imp.getNChannels(), 1, 1, 1, imp.getNFrames());
					//impCrop.getWindow().setVisible(false);
					//impCrop.hide();
					/* Image title contains image original information:
					 *  Image: image file and series
					 *  ROI: XY(CZT)
					 *	Scale: pixel size, scale factor	
					 */
					impCrop.setCalibration(imp.getCalibration());
					String roiName = "x"+String.valueOf(roiCenterX) + ",y" + String.valueOf(roiCenterY);
					impCrop.setTitle("Image:"+imp.getTitle()+", Cell:"+roiName +", Scale:"+scale);
					
					cellImageList.add(impCrop);
					finalStackIndex++;
					//impCrop.show();
					
					addRoiToOverlay(
							overlay, impCrop, rm.getRoi(r), 
							margin, edgeInPixel, doRoiSmoothing,
							roiAll, roiCenter, roiEdge,
							roiColorAll, roiColorCenter, roiColorEdge,
							finalStackIndex);
				}
				imp2.changes=false; imp2.close(); System.gc();
				//imp3.changes=false; imp3.close(); System.gc();
			}
		}
		loadFinishInfo = String.valueOf(cellImageList.size()) + " cells loaded from " 
						+ String.valueOf(nImages) + " images in "
						+ String.valueOf(nFiles) + " files.";
		IJ.log("\\Update:   " + loadFinishInfo);
		if (cellImageList.isEmpty()) return null;
		
		if (!keepScale) maxSize=size;

		ImagePlus result = imageListToStack("sample stack", cellImageList, maxSize, keepScale, overlay, cellColor);
	
		return result;
		//return imageListToStack("sample stack", cellImageList, maxSize, keepScale, overlay, cellColor);
	}
	
	public static ImagePlus getCellsFromImageRoiPair (
			ArrayList<File> imageFileList,
			boolean keepScale,
			int size,	// in pixel
			int margin,	// in pixel
			double minSize,
			double shapeGoodness,	// 0 - 1
			double edge,	// in micron
			boolean doRoiSmoothing,
			Color cellColor,
			boolean roiAll,
			boolean roiCenter,
			boolean roiEdge,
			Color roiColorAll,
			Color roiColorCenter,
			Color roiColorEdge
			) {
		RoiManager rm = RoiManager.getInstance();
		if (rm==null) rm = new RoiManager();
		else rm.reset();
		rm.setVisible(false);
		// if we found cell larger than defined image size,
		// resize the image window
		// only in the case of using original scale
		Overlay overlay = new Overlay();
		ArrayList<ImagePlus> cellImageList = new ArrayList<ImagePlus>();
		int maxSize = size;	
		int finalStackIndex = 0;
		IJ.log(" Loading cell images using pre-defined ROIs.");
		int nPairs = imageFileList.size();
		numTotal = imageFileList.size();
		for (File imageFile : imageFileList) {
			numCurrent = imageFileList.indexOf(imageFile)+1;
			IJ.log("\\Update:   Processing image " + String.valueOf(numCurrent)+ 
					" of in total " + String.valueOf(numTotal));
			int dot = imageFile.getPath().lastIndexOf(".tif");
			if (dot==-1) continue;
			String sampleName = imageFile.getPath().substring(0,dot);
			String roiFilePath = sampleName + ".zip";
			if (!new File(roiFilePath).exists())	{
				roiFilePath = sampleName + ".roi";
				if (!new File(roiFilePath).exists()) {
					nPairs--;
					continue;
				}
			}
			rm.reset();
			ImagePlus imp = IJ.openImage(imageFile.getPath());
			
			double pixelSize = 1.0;	// default pixel size 1.0 pixel
			String calUnit = imp.getCalibration().getUnit();
			if (calUnit.equals("micron") || calUnit.equals("μm") || calUnit.equals("um")) {
				pixelSize = imp.getCalibration().pixelWidth;
			//} else if (calUnit.equals("inch")) {	// extra PAINNNNN has to be taken for the bloody "inch" countries
			//	imp.setCalibration(null);
			} else {
				calUnit = "pixel";
				pixelSize = 0.2;	// when not calibrated, use 0.2 micron / pixel as default calibration
				IJ.log(" Image: " + imp.getTitle() + " is not calibrated! Use pixel for edge calculation.");
			}
			String scale = String.format("%.2f", pixelSize) + calUnit + ",";
			double edgeInPixel = edge/pixelSize;
			double minSizeInPixel = minSize/pixelSize;
			
			// make sure each cell has enough border to be cropped
			int widthWithMargin = imp.getWidth()+margin*2;
			int heightWithMargin = imp.getHeight()+margin*2;
			IJ.run(imp, "Canvas Size...", "width=["+widthWithMargin+"] height=["+heightWithMargin+"] position=Center zero");
			
			rm.runCommand("Open", roiFilePath);
			int nROI = rm.getCount();
			for (int r=0; r<nROI; r++) {
				Roi currentRoi = (Roi) rm.getRoi(r).clone();
				if (!currentRoi.isArea())	continue;
				imp.setRoi(currentRoi, false);
				if ((double)imp.getProcessor().getStats().pixelCount<minSizeInPixel) continue;
				if (!checkShape(currentRoi, shapeGoodness)) continue;
				int roiCenterX = (int)(currentRoi.getXBase() + currentRoi.getFloatWidth()/2);
				int roiCenterY = (int)(currentRoi.getYBase() + currentRoi.getFloatHeight()/2);
				Roi enlargedRoi = RoiEnlarger.enlarge(currentRoi, margin);
				maxSize = Math.max(maxSize, enlargedRoi.getBounds().width);
				maxSize = Math.max(maxSize, enlargedRoi.getBounds().height);
				enlargedRoi.setLocation(currentRoi.getBounds().getX(), currentRoi.getBounds().getY());
				
				//imp.setC(channelString);
				
				imp.setRoi(enlargedRoi);
				ImagePlus impCrop = imp.crop();
				impCrop.setCalibration(imp.getCalibration());
				String roiName = "x"+String.valueOf(roiCenterX) + ",y" + String.valueOf(roiCenterY);
				impCrop.setTitle("Image:"+imp.getTitle()+", Cell:"+roiName +", Scale:"+scale);
				cellImageList.add(impCrop);
				finalStackIndex++;
				addRoiToOverlay(
						overlay, impCrop, rm.getRoi(r), 
						margin, edgeInPixel, doRoiSmoothing,
						roiAll, roiCenter, roiEdge,
						roiColorAll, roiColorCenter, roiColorEdge,
						finalStackIndex);
			}
		}
		loadFinishInfo = String.valueOf(cellImageList.size()) + " cells loaded from " 
						+ String.valueOf(nPairs) + " image-ROI pairs.";
		IJ.log("\\Update:   " + loadFinishInfo);
		if (cellImageList.isEmpty()) return null;
		if (!keepScale) maxSize=size;
		return imageListToStack("sample stack", cellImageList, maxSize, keepScale, overlay, cellColor);
	}
	
	public boolean checkShape(
			ImagePlus imp,
			int imageSize,
			int filterSize,
			double threshold,
			double goodnessThreshold
			) {
		imp.deleteRoi();
		ImagePlus imp2 = imp.duplicate();
		imp2.getProcessor().setThreshold(threshold, 255, ImageProcessor.NO_LUT_UPDATE);
		imp2.setProcessor(imp2.getProcessor().createMask());
		IJ.run(imp2, "Canvas Size...", "width=["+(imageSize+4*filterSize)+"] height=["+(imageSize+4*filterSize)+"] position=Center zero");
		IJ.run(imp2, "Median...", "radius=["+filterSize+"] stack");
		IJ.run(imp2, "Fill Holes", "stack");
		ManyBlobs allBlobs = new ManyBlobs(imp2); // Extended ArrayList
		allBlobs.setBackground(0); // 0 for black, 1 for 255
		allBlobs.findConnectedComponents(); // Start the Connected Component Algorithm
		int blobIdx = 0;
		//double area = allBlobs.get(blobIdx).getEnclosedArea();
		if (allBlobs.size()>1) {
			imp2.close();
			return false;
		}
		//if (area<(imageSize*imageSize/4))	return false;
		double solidity = allBlobs.get(blobIdx).getSolidity();
		double thinnesRatio = allBlobs.get(blobIdx).getThinnesRatio();
		if ((solidity*thinnesRatio)<goodnessThreshold)	{
			imp2.close();
			return false;
		}
		imp2.close();
		return true;	
	}
	
	public static boolean checkShape(
			Roi originalRoi,
			double goodnessThreshold
			) {
		Roi roi = (Roi) originalRoi.clone(); // don't touch the original ROI
		ImagePlus roiImage = NewImage.createByteImage("ROI Image", (int)(roi.getFloatWidth()*2), (int)(roi.getFloatHeight()*2), 1, NewImage.FILL_BLACK);
		roi.setLocation(roi.getFloatWidth()/2, roi.getFloatHeight()/2);
		roiImage.getProcessor().setColor(255);
		roiImage.getProcessor().fill(roi);
		ManyBlobs allBlobs = new ManyBlobs(roiImage); // Extended ArrayList
		allBlobs.setBackground(0); // 0 for black, 1 for 255
		allBlobs.findConnectedComponents(); // Start the Connected Component Algorithm
		int blobIdx = 0;
		//double area = allBlobs.get(blobIdx).getEnclosedArea();
		if (allBlobs.size()>1) {
			roiImage.close();
			return false;
		}
		//if (area<(imageSize*imageSize/4))	return false;
		double solidity = allBlobs.get(blobIdx).getSolidity();
		double thinnesRatio = allBlobs.get(blobIdx).getThinnesRatio();
		if ((solidity*thinnesRatio)<goodnessThreshold)	{
			roiImage.close();
			return false;
		}
		roiImage.close();
		return true;	
	}
	
	public double guessBackground(
			ImagePlus imp	// guess dark background from image/stack
			) {
		if (imp.getProcessor().isBinary())	return 0.0;
		Roi roi = imp.getRoi();
		imp.deleteRoi();
		ImageStatistics stats = imp.getRawStatistics();
		double range = (stats.min+stats.max)/4;
		double min = stats.min;
		double mean = stats.mean;
		double mode = stats.mode;
		imp.setRoi(roi);
		return (range<mean)?min:mode;	
	}
	
	public static int[] parseChannelString(
			String channelString	// parse channel indices from channel string
			) {
		ArrayList<Integer> channels = new ArrayList<Integer>();
		String strNoSpace = channelString.replaceAll(" ",",");	// remove spaces
		String[] parts = strNoSpace.split(",");
		for (int i=0; i<parts.length; i++) {
			String part = parts[i];
			if (part.length()==0) continue;
			if (part.contains("-")) {
				String[] range = part.split("-");
				if (range.length!=2)	continue;
				int start = Integer.valueOf(range[0]);
				int end = Integer.valueOf(range[1]);
				for (int j=start; j<=end; j++) {
					if (!channels.contains(j))
						channels.add(j);
				}
				continue;
			}
			if (!channels.contains(Integer.valueOf(part)))
				channels.add(Integer.valueOf(part));
		}
		if (channels.contains(0))
			channels.clear();
		if (channels.size()<1)
			channels.add(0);
		
		int nC = channels.size();
		int[] result = new int[nC];
		for (int i=0; i<nC; i++) {
			result[i] = channels.get(i);
		}
		return result;
	}
	
	public static void addRoiToOverlay(
			//!!! NEED DEBUG FOR EDGE SIZE, AND image size!!!
			Overlay overlay,
			ImagePlus imp,
			Roi roi,
			int marginSizeInPixel,
			double edgeSizeInPixel,
			boolean doRoiSmoothing,
			boolean roiAll,
			boolean roiCenter,
			boolean roiEdge,
			Color colorAll,
			Color colorCenter,
			Color colorEdge,
			int stackPosition
			) {
		if (!roiAll && !roiCenter && !roiEdge)	return;
		
		Roi newRoiAll = (Roi) roi.clone();
		if (doRoiSmoothing) {
			newRoiAll = new PolygonRoi(newRoiAll.getInterpolatedPolygon(0.5, true), Roi.FREEROI );
			newRoiAll.enableSubPixelResolution();
		}
		newRoiAll.setLocation((double)(marginSizeInPixel), (double)(marginSizeInPixel));  // center current ROI
		newRoiAll.setPosition(stackPosition);
		newRoiAll.setStrokeColor(colorAll);
		newRoiAll.setName(roiNameAll);
		if (roiAll) overlay.add(newRoiAll);
		
		// control ROI shape to be area, and can be shrinked by edge size
		if (!roi.isArea() 
				|| roi.getBounds().width<=edgeSizeInPixel 
				|| roi.getBounds().height<=edgeSizeInPixel) return;
		Roi newRoiCenter = RoiEnlarger.enlarge(newRoiAll, -1*edgeSizeInPixel);
		if (newRoiCenter.getBounds().width!=newRoiAll.getBounds().width) {
			if (!newRoiCenter.getTypeAsString().equals("Composite") && doRoiSmoothing) {
				newRoiCenter = new PolygonRoi(newRoiCenter.getInterpolatedPolygon(0.5, true), Roi.FREEROI );
				newRoiCenter.enableSubPixelResolution();
			}
			newRoiCenter.setPosition(stackPosition);
			newRoiCenter.setStrokeColor(colorCenter);
			newRoiCenter.setName(roiNameCenter);
		}
		
		if (roiEdge) {
			ImagePlus roiImage = NewImage.createByteImage(
					"ROI Image", 
					imp.getWidth(), 
					imp.getHeight(), 
					1, NewImage.FILL_BLACK);
			ImageProcessor ip = roiImage.getProcessor();
			/*
			newRoiAllCopy.setLocation(0, 0);
			newRoiCenter.setLocation(
					(newRoiAllCopy.getFloatWidth()-newRoiCenterCopy.getFloatWidth())/2, 
					(newRoiAllCopy.getFloatHeight()-newRoiCenterCopy.getFloatHeight())/2);
			roiImage.setRoi(newRoiAllCopy); 
			*/
			//Toolbar.getForegroundColor();
			//Color foreground = Toolbar.setForegroundColor();
			ip.setColor(Color.WHITE); ip.fill(newRoiAll);
			ip.setColor(Color.BLACK); ip.fill(newRoiCenter);
			ip.setThreshold(1, 255, ImageProcessor.NO_LUT_UPDATE);
			Roi newRoiEdge = ThresholdToSelection.run(roiImage);
			//roiImage.show();
			roiImage.changes = false; roiImage.close(); System.gc();
			if (newRoiEdge!=null) {
				if (doRoiSmoothing)
					newRoiEdge.enableSubPixelResolution();
				newRoiEdge.setLocation((double)(marginSizeInPixel), (double)(marginSizeInPixel));
				newRoiEdge.setPosition(stackPosition);
				newRoiEdge.setStrokeColor(colorEdge);
				newRoiEdge.setName(roiNameEdge);
				overlay.add(newRoiEdge);
			}
		}
		if (roiCenter) overlay.add(newRoiCenter);	// avoid center overlay blocked by edge overlay
		return;
	}
	
	public static ImagePlus imageListToStack(
			String imageTitle,
			ArrayList<ImagePlus> impList,
			int imageSize,	// size in pixel
			boolean keepScale,
			Overlay overlay,
			Color lutColor
			) {
		ImageStack stack = new ImageStack(imageSize, imageSize);
		Overlay stackOverlay = new Overlay();
		double universalPixelSize = 0.0;
		Calibration oriCal = impList.get(0).getCalibration().copy();
		int numC = 1; //int numZ = 1; int numT = 1;
		//impList.get(0).show(); // multi-channel images still
		for (ImagePlus imp : impList) {
			if (imp==null)	continue;

			//System.out.println(imp.getTitle());
			
			numC = Math.max(numC, imp.getNChannels());
			//numZ = Math.max(numZ, imp.getNSlices());
			//numT = Math.max(numT, imp.getNFrames());
			
			oriCal = imp.getCalibration().copy();
			if (universalPixelSize != oriCal.pixelWidth) {
				if (universalPixelSize==0.0)
					universalPixelSize = oriCal.pixelWidth;
				else
					IJ.log("WARNING: inconsistent pixel size in source image list!");
			}
			int sliceIdx = impList.indexOf(imp)+1;
			Overlay sliceOverlay = overlay.duplicate();
			sliceOverlay.crop(sliceIdx, sliceIdx);
			Roi[] sliceRois = sliceOverlay.toArray();
			ArrayList<Roi> newRois = new ArrayList<Roi>();
			int oldWidth = imp.getWidth(); int oldHeight = imp.getHeight();
			double scaleFactorX = 1.0; double scaleFactorY = 1.0;
			if (keepScale) {
				IJ.run(imp, "Canvas Size...", "width=["+imageSize+"] height=["+imageSize+"] position=Center zero");
				for (Roi sliceRoi : sliceRois) {
					int roiWidth = sliceRoi.getBounds().width; int roiHeight = sliceRoi.getBounds().height;
					sliceRoi.setLocation((double)((imageSize-roiWidth)/2), (double)((imageSize-roiHeight)/2));
					newRois.add(sliceRoi);
				}
			} else {
				scaleFactorX = (double)imageSize/(double)imp.getWidth();
				scaleFactorY = (double)imageSize/(double)imp.getHeight();
				
				//System.out.println("C:"+imp.getNChannels()+"Z:"+imp.getNSlices()+"T:"+imp.getNFrames());
				IJ.run(imp, "Size...", "width=["+imageSize+"] height=["+imageSize+"] depth=["+imp.getNChannels()+"] average interpolation=Bicubic");
				//IJ.run(imp, "Size...", "width=128 height=128 depth=["+imp.getNChannels()+"] time=["+imp.getNFrames()+"] average interpolation=Bicubic");
				
				
				//imp.getCalibration().pixelWidth *= (1/scaleFactorX);
				//imp.getCalibration().pixelHeight *= (1/scaleFactorY);
				for (Roi sliceRoi : sliceRois) {
					Roi sliceRoiScaled = RoiScaler.scale(sliceRoi, (double)imageSize/(double)oldWidth, (double)imageSize/(double)oldHeight, true);
					//IJ.log("scale ROI: " + String.valueOf(imageSize/oldWidth));
					int roiWidth = sliceRoiScaled.getBounds().width; int roiHeight = sliceRoiScaled.getBounds().height;
					sliceRoiScaled.setLocation((double)((imageSize-roiWidth)/2), (double)((imageSize-roiHeight)/2));
					sliceRoiScaled.setStrokeColor(sliceRoi.getStrokeColor());
					sliceRoiScaled.setName(sliceRoi.getName());
					newRois.add(sliceRoiScaled);
				}
			}
			imp.setCalibration(oriCal); // still use the original calibration as universal image calibration
			// construct scale factor string and add into slice label
			String scaleFactor = "x" + String.format("%.2f", scaleFactorX)
							  + ",y" + String.format("%.2f", scaleFactorY);
			String newSliceLabel = imp.getTitle() + scaleFactor;
			//String scaleLabel = sliceLabel.substring(sliceLabel.indexOf("Scale:"), sliceLabel.length());
			//String newSliceLabel = sliceLabel.substring(0, sliceLabel.lastIndexOf("Scale:")) + scaleFactor;
			for (int c=0; c<numC; c++) {
				imp.setC(c+1);
				stack.addSlice(newSliceLabel, imp.getProcessor());
			}
			//stack.addSlice(newSliceLabel, imp.getProcessor());
			for (Roi newRoi : newRois) {
				if (numC==1)
					newRoi.setPosition(sliceIdx);
				else
					newRoi.setPosition(0,sliceIdx,0);
				stackOverlay.add(newRoi);
			}
		}
		
		int numZ = stack.size()/numC;
		ImagePlus result = HyperStackConverter.toHyperStack(new ImagePlus(imageTitle, stack), numC, numZ, 1);
		
		//ImagePlus result = new ImagePlus(imageTitle, stack);
		
		result.setCalibration(oriCal);
		result.setOverlay(stackOverlay);
		result.setLut(LUT.createLutFromColor(lutColor));
		return result;
	}


	@Override
	public void run(String arg) {
		// TODO Auto-generated method stub
		try {
			loadCellImages();
		} catch (IOException | FormatException | ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/*
	public void main(String[] args) {
		
		LoadCellsIntoStack lc = new LoadCellsIntoStack();
		lc.run(null);
		
	}
	*/
}
