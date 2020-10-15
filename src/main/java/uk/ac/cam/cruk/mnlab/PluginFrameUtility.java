package uk.ac.cam.cruk.mnlab;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
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
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.plugin.RoiScaler;
import ij.plugin.frame.RoiManager;
import loci.common.services.ServiceException;
import loci.formats.FormatException;

public class PluginFrameUtility {
	 
	
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
			Frame f
			) throws Exception {
		// parse current active image in Fiji
		//ImagePlus source = null;
		if (WindowManager.getImageCount()!=0)
			source = IJ.getImage();
		//checkSourceImage(imp);
		
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
		
		JTextArea sourceInfo = new JTextArea();
		sourceInfo.setMaximumSize(textAreaMax);
		sourceInfo.setEditable(false);
		contentPanel.add(sourceInfo);
		sourceInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		JPanel buttonPanel = new JPanel();
		JButton btnRefresh = new JButton("refresh");
		JButton btnLoad = new JButton("load");
		JButton btnResize = new JButton("resize");
		btnRefresh.setPreferredSize(buttonSize);
		// Use group layout for a JTextArea to display source image info
		// and 3 buttons horizontally aligned: refresh, load, resize
		GroupLayout buttonLayout = new GroupLayout(buttonPanel);
		buttonPanel.setLayout(buttonLayout);
		buttonLayout.setAutoCreateGaps(true);
		buttonLayout.setAutoCreateContainerGaps(true);

		buttonLayout.setHorizontalGroup(buttonLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
			 .addGroup(buttonLayout.createSequentialGroup()
			    .addComponent(btnRefresh)
			    .addComponent(btnLoad)
			 	.addComponent(btnResize)));
		buttonLayout.linkSize(SwingConstants.HORIZONTAL, btnRefresh, btnLoad, btnResize);	
		
		buttonLayout.setVerticalGroup(buttonLayout.createSequentialGroup()
			.addGroup(buttonLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(btnRefresh)
			        .addComponent(btnLoad)
			     	.addComponent(btnResize)));
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
		    public void actionPerformed(ActionEvent ae) { 
		    	ImagePlus activeImage = WindowManager.getCurrentImage();
		    	if (activeImage==null && WindowManager.getImageCount()==0) {
		    		source=null;
		    		sourceInfo.setText(getImageInfo(null));
		    		sourceInfo.setFont(errorFont);
		    		sourceInfo.setForeground(errorFontColor);
		    		return;
		    	}
		    	if (activeImage==null ||
		    		activeImage.getTitle().toLowerCase().contains("gallery") || 
					activeImage.getTitle().toLowerCase().contains("histogram")
	    			) {
		    		// do nothing
		    	} else {
		    		source = activeImage;
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
		});
		// configure load button
		btnLoad.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) { 
		    	try {
					LoadCellsIntoStack.loadCellImages();
				} catch (NullPointerException | IOException | FormatException | ServiceException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
		});
		// configure resize button
		btnResize.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) { 
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
				sourceInfo.setText(getImageInfo(source));
		    }
		});
		// add title and content panel to the parent panel, and finally add to plugin frame
		parentPanel.add(contentPanel);
		contentPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		//f.add(parentPanel, BorderLayout.NORTH);
		f.add(parentPanel);
		f.pack();
	}
	
	
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
			Frame f
			) throws Exception {
		// parse current active image in Fiji
		//ImagePlus source = null;
		if (WindowManager.getImageCount()!=0)
			source = IJ.getImage();
		//checkSourceImage(imp);
		
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
		int nCol = 2; int nRow = 4;
 		String[] columnNames = {"Class Name", "Sample Size"};
 		Object[][] data = new Object[nRow][nCol];
 		for (int i=0; i<nRow; i++) {
 			data[i][0] = "Class "+String.valueOf(i+1);
 			data[i][1] = (int)(Math.random()*100);
 		}
 		DefaultTableModel model = new DefaultTableModel(data, columnNames);
 		JTable table = new JTable(model) {
		    DefaultTableCellRenderer renderCenter = new DefaultTableCellRenderer();
		    { // initializer block
		        renderCenter.setHorizontalAlignment(SwingConstants.CENTER);
		    }
		    @Override
		    public TableCellRenderer getCellRenderer (int arg0, int arg1) {
		        return renderCenter;
		    } 
		};
		table.setEnabled(false);
		table.setBackground(f.getBackground());
		table.setPreferredSize(tablePreferred);
		table.setMaximumSize(tableMax);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBackground(f.getBackground());
        scrollPane.setPreferredSize(tablePreferred);
        scrollPane.setMaximumSize(tableMax);
		contentPanel.add(scrollPane);
		scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
		//HERE!
		
		JTextArea sampleInfo = new JTextArea();
		sampleInfo.setMaximumSize(textAreaMax);
		contentPanel.add(sampleInfo);
		sampleInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		JPanel buttonPanel = new JPanel();
		JButton btnAdd = new JButton("add");
		JButton btnDel = new JButton("delete");
		JButton btnRename = new JButton("rename");
		btnAdd.setPreferredSize(buttonSize);
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
			 	.addComponent(btnRename)));
		buttonLayout.linkSize(SwingConstants.HORIZONTAL, btnAdd, btnDel, btnRename);	
		
		buttonLayout.setVerticalGroup(buttonLayout.createSequentialGroup()
			.addGroup(buttonLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(btnAdd)
			        .addComponent(btnDel)
			     	.addComponent(btnRename)));
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
		sampleInfo.setText(getImageInfo(source));
		if (source==null) {
			sampleInfo.setFont(errorFont);
			sampleInfo.setForeground(errorFontColor);
		}
		// configure add button
		btnAdd.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) { 
		    	
		    }
		});
		// configure rename button
		btnDel.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) { 
		    	
		    }
		});
		// configure resize button
		btnRename.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) { 
		    	
		    }
		});
		// add title and content panel to the parent panel, and finally add to plugin frame
		parentPanel.add(contentPanel);
		contentPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		//f.add(parentPanel, BorderLayout.CENTER);
		f.add(parentPanel);
		f.pack();
	}
	
	/* Functions to create and configure the classifier panel
	 * It needs a text area to display classifier information
	 * It needs six buttons:
	 * 	configure, check
	 * 	train, apply
	 *  load, export
	 */
	public static void addClassifierPanelToFrame (
			Frame f
			) throws Exception {
		// parse current active image in Fiji
		//ImagePlus source = null;
		if (WindowManager.getImageCount()!=0)
			source = IJ.getImage();
		//checkSourceImage(imp);
		
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
		JTextArea classifierInfo = new JTextArea();
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
		    public void actionPerformed(ActionEvent ae) { 
		    	
		    }
		});
		// configure check button
		btnCheck.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) { 
		    	
		    }
		});
		// configure train button
		btnTrain.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) { 
		    	
		    }
		});
		// configure apply button
		btnApply.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) { 
		    	
		    }
		});
		// configure load button
		btnLoad.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) { 
		    	
		    }
		});
		// configure export button
		btnExport.addActionListener(new ActionListener() { 
		    @Override 
		    public void actionPerformed(ActionEvent ae) { 
		    	
		    }
		});
		
		contentPanel.add(buttonPanel);
		buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		classifierInfo.setText(getImageInfo(source));
		if (source==null)
			classifierInfo.setFont(errorFont);//sourceInfo.setForeground(errorFontColor);

		// add title and content panel to the parent panel, and finally add to plugin frame
		parentPanel.add(contentPanel);
		contentPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		//f.add(parentPanel, BorderLayout.SOUTH);
		f.add(parentPanel);
		f.pack();
	}
	
	
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
		return ("Image: " + "\n" + imgTitle + "\n" + imgSzie + "\n" + imgDimension + "\n" + imgRoi + "\n" + imgOverlay);
	}
	
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
