package uk.ac.cam.cruk.mnlab;

import java.io.BufferedInputStream;
import java.io.File;
//import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.HttpURLConnection;
//import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.FilenameUtils;

import ij.IJ;
import ij.plugin.PluginInstaller;


public class PluginUtility {

	public static String pluginDir = IJ.getDirectory("plugins");
	public static String jarDir = IJ.getDirectory("imagej")+File.separator+"jars"+File.separator;
	
	protected final static String url_MexicanHatFilter = "https://imagej.nih.gov/ij/plugins/mexican-hat/Mexican_Hat_Filter.class";
	protected final static String url_MorpholibJ = "https://github.com/ijpb/MorphoLibJ/releases/download/v1.4.0/MorphoLibJ_-1.4.0.jar";
	
	public static boolean installMorpholibJ () {
		IJ.log("   Attempt installing IJPB plugin.");
		try {
			if (!pluginInstaller(url_MorpholibJ, pluginDir)) {
				return pluginDownloader(url_MorpholibJ);
			} else {
				IJ.log("   \"MorpholibJ\" installed successfully.");
				return true;
			}
		} catch (IOException e) {
			return false;
		}
	}
	
	public static boolean installMexicanHatFilter () {
		IJ.log("   Attempt installing 3rd party plugin.");
		try {
			if (!pluginInstaller(url_MexicanHatFilter, pluginDir)) {
				return pluginDownloader(url_MexicanHatFilter);
			} else {
				IJ.log("   \"3rd party plugin\" installed successfully.");
				return true;
			}
		} catch (IOException e) {
			return false;
		}
	}
	
	
	public static boolean pluginDownloader(
			String pluginUrl
			) {
		boolean successInstalled = new PluginInstaller().install(pluginUrl);
		if (!successInstalled) {
			IJ.log("   plugin not installed!");
			IJ.log("   URL: " + pluginUrl);
		}
		return successInstalled;
	}
	
	public static boolean pluginInstaller(
			String pluginUrl,
			String installDir
			) throws IOException {
		
		if (pluginUrl == null) return false;
		URL url = new URL(pluginUrl);
		
		String pluginName = FilenameUtils.getName(url.getPath());
		
		if (installDir==null) {
			installDir = IJ.getDirectory("plugins");
		}
		String pluginPath =  installDir + File.separator + pluginName;
		if (pluginName == null) return false;

		// prepare download buffer stream
		BufferedInputStream inputStream;
		try {
			inputStream = new BufferedInputStream(url.openStream());  
		} catch (IOException e) {	// server denied access with error 403
			HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
			httpcon.addRequestProperty("User-Agent", "Mozilla/4.0");	// trick the server with dummy user agent request
			inputStream = new BufferedInputStream(httpcon.getInputStream());  
		}
		
		FileOutputStream fileOS = new FileOutputStream(pluginPath);
		byte[] data = new byte[1024];
		int byteContent;
		try {
		    while ((byteContent = inputStream.read(data, 0, 1024)) != -1) {
		        fileOS.write(data, 0, byteContent);
		    }
		    fileOS.close();
		} catch (IOException e) {
			IJ.log("   Cannot install plugin \"" + pluginName + "\"");
			IJ.log("   You will have to install it manually.");
		    return false;
		}
		
		return true;
	}
	
	public static boolean pluginCheck(
			String pluginKeywords
			) {
		String pluginDir = pluginFolderLocator();
		if (pluginDir == null) return false;
		
		File f = new File(pluginDir);
		File[] matchingFiles = f.listFiles(new FilenameFilter() {
		    public boolean accept(File dir, String name) {
		        return name.contains(pluginKeywords);
		    }
		});
		if (matchingFiles.length == 1) {
			return true;
		}
		else {
			IJ.log("   plugin \"" + pluginKeywords + "\" not found");
			return false;
		}
	}
	
	public static String pluginFolderLocator(
			) {
		return IJ.getDirectory("plugins");
	}
	
	
}
