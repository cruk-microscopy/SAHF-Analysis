package uk.ac.cam.cruk.mnlab;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.DecimalFormat;

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

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.plugin.RoiScaler;
import ij.plugin.frame.RoiManager;
import loci.common.services.ServiceException;
import loci.formats.FormatException;

public class sourceImagePanel {
	 
	
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
	protected static final Dimension buttonSize = new Dimension(90, 10);
	
	protected static ImagePlus source = null;
	protected static TrainingSamples trainingSampleSourcePanel;
	
	protected static JTextArea sourceInfo;
	
	public void createPanel() {}
		

	/* Functions to create and configure the training sample panel
	 * It needs a text area to display image information
	 * It needs two buttons:
	 * 	refresh source
	 * 	load training sample from disk
	 *	and currently not prioritized: fetch sample from active image with ROI (RoiManager)
	 * 	It also need to control the training cell image window(s).
	 */
	public static void addSourcePanelToFrame (
			Frame f,
			TrainingSamples trainingSamples
			) throws Exception {
		// parse current active image in Fiji
		//ImagePlus source = null;
		
		//trainingSamples = new TrainingSamples();
		
		//if (WindowManager.getImageCount()!=0)
		//	source = IJ.getImage();
		//checkSourceImage(imp);
		source = trainingSamples.getCurrentSource();
		trainingSampleSourcePanel = trainingSamples;
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
		JLabel title = new JLabel("Source Image and ROIs");
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
		JButton btnLoad = new JButton("load");
		JButton btnPrepare = new JButton("prepare");
		JButton btnResize = new JButton("resize");

		//btnRefresh.setPreferredSize(buttonSize);
		// Use group layout for a JTextArea to display source image info
		// and 3 buttons horizontally aligned: refresh, load, resize
		
		GroupLayout buttonLayout = new GroupLayout(buttonPanel);
		buttonPanel.setLayout(buttonLayout);
		buttonLayout.setAutoCreateGaps(true);
		buttonLayout.setAutoCreateContainerGaps(true);

		buttonLayout.setHorizontalGroup(buttonLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
			 .addGroup(buttonLayout.createSequentialGroup()
			    .addComponent(btnRefresh)
			    .addComponent(btnPrepare))
			 .addGroup(buttonLayout.createSequentialGroup()
			    .addComponent(btnLoad)
			    .addComponent(btnResize)));
			 	
		buttonLayout.linkSize(SwingConstants.HORIZONTAL, btnRefresh, btnLoad, btnPrepare, btnResize);	

		buttonLayout.setVerticalGroup(buttonLayout.createSequentialGroup()
			.addGroup(buttonLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
				.addGroup(buttonLayout.createSequentialGroup()
		                .addComponent(btnRefresh)
		                .addComponent(btnLoad))
				.addGroup(buttonLayout.createSequentialGroup()
		                .addComponent(btnPrepare)
		                .addComponent(btnResize))));
		
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
		// add real-time action listener to panel
		
		Timer timer = new Timer(550, new ActionListener() {
	        public void actionPerformed(ActionEvent evt) {
	        	source = trainingSamples.getCurrentSource();
	        	/*
	        	if (source==null) {
	        		trainingSamples.getNewSource();
	        		source = trainingSamples.getCurrentSource();
	        	}
	        	sourceInfo.setText(getImageInfo(source));
	        	*/
	        	if (source==null) {
	        		sourceInfo.setFont(errorFont);
	        		sourceInfo.setForeground(errorFontColor);
	        	} else {
	        		sourceInfo.setFont(textFont);
	        		sourceInfo.setForeground(fontColor);
	        	}
	        	sourceInfo.repaint();	
	        }
	    });
		timer.start();
		
		//f.add(parentPanel, BorderLayout.NORTH);
		f.add(parentPanel);
		f.pack();
	}

/*
 * Function groups to manage the source image	
 */
	public static void refreshSource() {
		ImagePlus activeImage = WindowManager.getCurrentImage();
    	if (WindowManager.getImageCount()==0) {
    		source=null;
    		sourceInfo.setText(getImageInfo(null));
    		sourceInfo.setFont(errorFont);
    		sourceInfo.setForeground(errorFontColor);
    		return;
    	}
    	if (activeImage==null) {
    		//activeImage.getTitle().toLowerCase().contains("gallery") || 
			//activeImage.getTitle().toLowerCase().contains("histogram")
			//) {
    		// do nothing
    		trainingSampleSourcePanel.getNewSource();
    		source = trainingSampleSourcePanel.getCurrentSource();
    	} else {
    		source = activeImage;
    		trainingSampleSourcePanel.setSource(source);
    	}
    	sourceInfo.setText(getImageInfo(source));
    	if (source==null) {
    		sourceInfo.setFont(errorFont);
    		sourceInfo.setForeground(errorFontColor);
    	} else {
    		sourceInfo.setFont(textFont);
    		sourceInfo.setForeground(fontColor);
    	}
	}
	
	public static void loadNewSource() {
		// run cell image loader in a new thread, to update info in ImageJ log window
		Thread thread = new Thread(new Runnable() {
			public void run() {
				sourceInfo.paintImmediately(0,0,sourceInfo.getWidth(), sourceInfo.getHeight());
				sourceInfo.setText("Loading cell images from file...");
				sourceInfo.setFont(errorFont);
				sourceInfo.setForeground(fontColor);
				sourceInfo.paintImmediately(0,0,sourceInfo.getWidth(), sourceInfo.getHeight());
				try {
					LoadCellsIntoStack.loadCellImages();
				} catch (Exception e) {
					e.printStackTrace();
					sourceInfo.paintImmediately(0,0,sourceInfo.getWidth(), sourceInfo.getHeight());
					sourceInfo.setText("Loading failed!");
					sourceInfo.setFont(errorFont);
		    		sourceInfo.setForeground(errorFontColor);
				}
				sourceInfo.paintImmediately(0,0,sourceInfo.getWidth(), sourceInfo.getHeight());
				sourceInfo.setText(LoadCellsIntoStack.loadFinishInfo);
				sourceInfo.setFont(errorFont);
				sourceInfo.setForeground(fontColor);
			}});
		// start the thread
		thread.start();	
	}
	
	public static void prepareSource() {
		if (source==null) return;
		// additional function to correct and unify the Cell Class Label: 2019.09.16
		// change from "ROI:" to "Cell:"
		trainingSampleSourcePanel.correctSourceClassLabel();
		/*
		 * add button groups based on current classes
		 * if not yet exist, add histogram panel
		 * if not yet exist, add gallery panel
		 */
		ImageWindow sourceWindow = source.getWindow();
		//double mag = sourceWindow.getCanvas().getMagnification();
		if (!trainingSampleSourcePanel.checkPanel(sourceWindow, "overlayPanel"))
			trainingSampleSourcePanel.addOverlayPanel(sourceWindow);
		if (!trainingSampleSourcePanel.checkPanel(sourceWindow, "histogramPanel"))
			trainingSampleSourcePanel.addHistogramPanel(sourceWindow);
		if (!trainingSampleSourcePanel.checkPanel(sourceWindow, "galleryPanel"))
			trainingSampleSourcePanel.addGalleryPanel(sourceWindow);
		if (!trainingSampleSourcePanel.checkPanel(sourceWindow, "delSamplePanel"))
			trainingSampleSourcePanel.addDelSamplePanel(sourceWindow);
		sourceWindow.pack();
		//sourceWindow.getCanvas().setMagnification(mag);
		//WindowManager.setCurrentWindow(sourceWindow);	new Zoom().run("scale");
	}
	/*
	public static void addHistoPanel (ImageWindow window) {
		JPanel buttonPanel = new JPanel();
		buttonPanel.setName("hstPanel");
		buttonPanel.setPreferredSize(new Dimension(200, 20));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        Button button1 = new Button("show histogram");
       // button1.setPreferredSize(new Dimension(130, 20));
        buttonPanel.add(button1);

        Button button2 = new Button("close all");
        //button2.setPreferredSize(new Dimension(70, 20));
        buttonPanel.add(button2);
        
        buttonPanel.setBackground(window.getBackground());
        //buttonPanel.validate();
        //buttonPanel.repaint();
        window.add(buttonPanel);
		window.pack();
	}
	public static void addGalleryPanel (ImageWindow window) {
		JPanel buttonPanel = new JPanel();
		buttonPanel.setName("mtgPanel");
		//buttonPanel.setPreferredSize(new Dimension(320, 30));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        Button button1 = new Button("show gallery");
        //button1.setPreferredSize(new Dimension(130, 20));
        buttonPanel.add(button1);

        Button button2 = new Button("close all");
        //button2.setPreferredSize(new Dimension(70, 20));
        buttonPanel.add(button2);
        
        buttonPanel.setBackground(window.getBackground());
        //buttonPanel.validate();
        //buttonPanel.repaint();
        window.add(buttonPanel);
		window.pack();
	}
	*/
	public static void resizeSource() {
		if (source==null) return;
    	int oldWidth = source.getWidth(); int oldHeight = source.getHeight();
    	int depth = source.getStackSize();
    	GenericDialog gd = new GenericDialog("Resize Source Image");
    	String resizeMessage = "adjust the size of the source image to match created classes.";
    	gd.setInsets(20,0,0);
    	gd.addMessage(resizeMessage, errorFont, Color.blue);
    	gd.addCheckbox("keep scale", false);
    	gd.addNumericField("width", 256, 0, 3, "pixel");
    	gd.addNumericField("height", 256, 0, 3, "pixel");
    	gd.showDialog();
    	if (gd.wasCanceled()) return;
    	boolean keepScale = gd.getNextBoolean();
		int width = (int) gd.getNextNumber();
		int height = (int) gd.getNextNumber();
		if (width==oldWidth && height==oldHeight)	return;
		// take care of the overlay if there's any
		Overlay newOverlay = new Overlay();
		boolean doOverlay = source.getOverlay()!=null;
		if (doOverlay) {
			Overlay overlay = source.getOverlay();
			Roi[] Rois = overlay.toArray();
			for (Roi roi : Rois) {
				Roi newRoi = (Roi) roi.clone();
				if (!keepScale)
					newRoi = RoiScaler.scale(roi, (double)width/(double)oldWidth, (double)height/(double)oldHeight, true);
				int roiWidth = newRoi.getBounds().width; int roiHeight = newRoi.getBounds().height;
				newRoi.setLocation((double)((width-roiWidth)/2), (double)((height-roiHeight)/2));
				newRoi.setPosition(roi.getPosition());
				newRoi.setStrokeColor(roi.getStrokeColor());
				newRoi.setName(roi.getName());
				newOverlay.add(newRoi);
			}
		}
		if (keepScale)
			IJ.run(source, "Canvas Size...", "width=["+width+"] height=["+height+"] position=Center zero");
		else
			IJ.run(source, "Size...", "width=["+width+"] height=["+height+"] depth=["+depth+"] average interpolation=Bicubic");
		if (doOverlay) source.setOverlay(newOverlay);
		//WindowManager.setWindow(source.getWindow());	new Zoom().run("scale");
		sourceInfo.setText(getImageInfo(source));
	}

	
/*
 * 	Functions to configure and display strings to source info panel
 */
	// function to generate string of image information
	public static String getImageInfo (
			ImagePlus imp) {
		if (imp==null)	return ("No image recognized!");
		String imgTitle = " " + wrapString(imp.getTitle(), lineWidth, 1);
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
		return ("Source Image: " + "\n" + imgTitle + "\n" + imgSzie + "\n" + imgDimension + "\n" + imgRoi + "\n" + imgOverlay);
	}
	// current not in use: report loading information to source info panel
	public static String loadSource() {
		String status = "Loading cell images from file...";
		return status;
	}
	// function to wrap string after certain length for display in text area
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
