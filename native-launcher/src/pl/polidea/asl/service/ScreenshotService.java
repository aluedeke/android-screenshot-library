package pl.polidea.asl.service;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.UUID;

import android.app.Service;
import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.os.*;
import android.util.Log;
import android.widget.Toast;

public class ScreenshotService extends Service {
	
	/*
	 * Action name for intent used to bind to service.
	 */
	public static final String BIND = "pl.polidea.asl.service.ScreenshotService.BIND";  

	/*
	 * Name of the native process.
	 */
	private static final String NATIVE_PROCESS_NAME = "asl-native"; 

	/*
	 * Port number used to communicate with native process.
	 */
	private static final int PORT = 42380;

	/*
	 * Timeout allowed in communication with native process.
	 */
	private static final int TIMEOUT = 1000;  

	/*
	 * Directory where screenshots are being saved.
	 */
	private static String SCREENSHOT_FOLDER = "/sdcard/screens/";


	/*
	 * An implementation of interface used by clients to take screenshots.
	 */
	private final IScreenshotProvider.Stub mBinder = new IScreenshotProvider.Stub() {

		@Override
		public String takeScreenshot() throws RemoteException {
			try {
				return ScreenshotService.this.takeScreenshot();
			}
			catch(Exception e) { return null; }
		}

		@Override
		public boolean isAvailable() throws RemoteException {
			return isNativeRunning();
		}
	};
	
	@Override
	public void onCreate() {
		Log.i("service", "Service created."); 
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}


	/*
	 * Checks whether the internal native application is running,
	 */
	private boolean isNativeRunning() {
		ActivityManager am = (ActivityManager)getSystemService(Service.ACTIVITY_SERVICE);
		List<ActivityManager.RunningAppProcessInfo> ps = am.getRunningAppProcesses();

		if (am != null) {
			for (ActivityManager.RunningAppProcessInfo rapi : ps) {
				if (rapi.processName.contains(NATIVE_PROCESS_NAME))
					// native application found
					return true;
			}

		}
		return false;
	}
	
	
	/*
	 * Internal class describing a screenshot.
	 */
	class Screenshot {
		public Buffer pixels;
		public int width;
		public int height;
		public int bpp;
		
		public boolean isValid() {
			if (pixels == null || pixels.capacity() == 0 || pixels.limit() == 0) return false;
			if (width <= 0 || height <= 0)	return false;
			return true;
		}
	}
	
	
	private int convertRGBAtoARGB(int c) {
		int r = ((c & 0xFF000000) >> 24) & 0x000000FF;
		int g = ((c & 0x00FF0000) >> 16) & 0x000000FF;
		int b = ((c & 0x0000FF00) >> 8) & 0x000000FF;
		int a = c & 0x000000FF;
		
		c = 0;
		c |= ((a << 24) & 0xFF000000);
		c |= ((r << 16) & 0x00FF0000);
		c |= ((g << 8) & 0x0000FF00);
		c |= (b & 0x00000FF);
		return c;
	}

	/*
	 * Communicates with the native service and retrieves a screenshot from it
	 * as a 2D array of bytes.
	 */
	private Screenshot retreiveRawScreenshot() throws Exception {
		try {
			// connect to native application
			Socket s = new Socket();
			s.connect(new InetSocketAddress("localhost", PORT), TIMEOUT);

			// send command to take screenshot
			OutputStream os = s.getOutputStream();
			os.write("SCREEN".getBytes("ASCII"));

			// retrieve response -- first the size and BPP of the screenshot
			InputStream is = s.getInputStream();
			StringBuilder sb = new StringBuilder();
			int c;
			while ((c = is.read()) != -1) {
				if (c == 0) break;
				sb.append((char)c);
			}

			// parse it
			String[] screenData = sb.toString().split(" ");
			if (screenData.length >= 3) {
				Screenshot ss = new Screenshot();
				ss.width = Integer.parseInt(screenData[0]);
				ss.height = Integer.parseInt(screenData[1]);
				ss.bpp = Integer.parseInt(screenData[2]);

				// retreive the screenshot
				// (this method - via ByteBuffer - seems to be the fastest)
				ByteBuffer bytes = ByteBuffer.allocate (ss.width * ss.height * ss.bpp / 8);
				is = new BufferedInputStream(is);	// buffering is very important apparently
				is.read(bytes.array());				// reading all at once for speed
				bytes.position(0);					// reset position to the beginning of ByteBuffer
				ss.pixels = bytes;
				
				return ss;
			}
		}
		catch (Exception e) {
			throw new Exception(e);
		}
		finally {}

		return null;
	}

	/*
	 * Saves given array of bytes into image file in the PNG format.
	 */
	private void writeImageFile(Screenshot ss, String file) {
		if (ss == null || !ss.isValid())		throw new IllegalArgumentException();
		if (file == null || file.length() == 0)	throw new IllegalArgumentException();

		// create appropriate bitmap and fill it wit data
		Bitmap bmp = Bitmap.createBitmap(ss.width, ss.height, Config.ARGB_8888);
		bmp.copyPixelsFromBuffer(ss.pixels);

		// save it in PNG format
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			throw new InvalidParameterException();
		}
		bmp.compress(CompressFormat.PNG, 100, fos);
	}

	/*
	 * Takes screenshot and saves to a file.
	 */
	private String takeScreenshot() throws IOException {
		// construct screenshot file name
		StringBuilder sb = new StringBuilder();
		sb.append(SCREENSHOT_FOLDER);
		sb.append(Math.abs(UUID.randomUUID().hashCode()));	// hash code of UUID should be quite random yet short
		sb.append(".png");
		String file = sb.toString();

		// fetch the screen and save it
		Screenshot ss = null;
		try {
			ss = retreiveRawScreenshot();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		writeImageFile(ss, file);

		return file;
	}
}
