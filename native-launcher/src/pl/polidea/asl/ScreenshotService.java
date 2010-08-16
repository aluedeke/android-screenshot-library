package pl.polidea.asl;

import java.io.*;
import java.net.*;
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

public class ScreenshotService extends Service {

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
	private static String SCREENSHOT_FOLDER = "/data/local/";


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
	 * Communicates with the native service and retrieves a screenshot from it
	 * as a 2D array of bytes.
	 */
	private byte[][] retreiveRawScreenshot() throws IOException {
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
				int width, height, bpp;
				width = Integer.parseInt(screenData[0]);
				height = Integer.parseInt(screenData[1]);
				bpp = Integer.parseInt(screenData[2]);

				// retreive the screenshot (as an array of rows -- [y][x])
				byte[][] screen = new byte[height][];
				int bytesPerRow = width * bpp / 4;
				for (int y = 0; y < height; ++y) {
					// get the row
					screen[y] = new byte[bytesPerRow];
					is.read(screen[y]);
				}

				return screen;
			}
		}
		finally {}

		return null;
	}

	/*
	 * Saves given array of bytes into image file in the PNG format.
	 */
	private void writeImageFile(byte[][] bytes, String file) {
		if (bytes == null || bytes.length == 0 || bytes[0] == null || bytes[0].length == 0)
			throw new IllegalArgumentException();
		if (file == null || file.length() == 0)	throw new IllegalArgumentException();

		// create appropriate bitmap
		int width = bytes[0].length;
		int height = bytes.length;
		Bitmap bmp = Bitmap.createBitmap(width, height, Config.ARGB_8888);

		// fill it with given data
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				// (this assumes 32-bit ARGB -- has to be checked experimentally)
				// (WARNING: bitwise operation in Java -- be very very careful about sign propagation;
				// zero bits with 0 whenever feasible)
				int color = 0;
				color |= ((bytes[y][4*x] & 0x000000FF) << 24) & 0xFF000000;		// A
				color |= ((bytes[y][4*x+1] & 0x000000FF) << 16) & 0x00FF0000;	// R
				color |= ((bytes[y][4*x+2] & 0x000000FF) << 8) & 0x0000FF00;	// G
				color |= (bytes[y][4*x+3] & 0x000000FF) & 0x000000FF;			// B
				bmp.setPixel(y, x, color);
			}
		}

		// save it in PNG format
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			throw new InvalidParameterException();
		}
		bmp.compress(CompressFormat.PNG, 90, fos);
	}

	/*
	 * Takes screenshot and saves to a file.
	 */
	private String takeScreenshot() throws IOException {
		// construct screenshot file name
		StringBuilder sb = new StringBuilder();
		sb.append(SCREENSHOT_FOLDER);
		sb.append(UUID.randomUUID().hashCode());	// hash code of UUID should be quite random yet short
		sb.append(".png");
		String file = sb.toString();

		// fetch the screen and save it
		byte[][] bytes = retreiveRawScreenshot();
		writeImageFile(bytes, file);

		return file;
	}
}
