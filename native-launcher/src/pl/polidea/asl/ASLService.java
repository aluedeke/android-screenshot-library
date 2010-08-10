package pl.polidea.asl;

import java.io.*;
import java.lang.reflect.*;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

public class ASLService extends Service {

	// Asset containing the native binary file
	private static final String NATIVE_EXEC_ASSET = "asl-native";

	// Name for temporary file from which the executable is ran after unpacking
	// from assets and chmoding to 744
	private static final String NATIVE_EXEC_TEMP_FILE = "asl";

	// Data directory for where the temporary executable is unpacked
	private static final String NATIVE_EXEC_TEMP_DIR = "/data/data/" + ASLService.class.getPackage().getName();
	private static final String NATIVE_EXEC_TEMP_PATH = NATIVE_EXEC_TEMP_DIR + "/" + NATIVE_EXEC_TEMP_FILE;

	private boolean runExecutable(String path, String param) {
		try {
			// Get the Exec class and its createSubprocess method using reflection
			Class<?> execClass = Class.forName("android.os.Exec");
			Method createSubprocess = execClass.getMethod("createSubprocess",
				String.class, String.class, String.class, int[].class);
			Method waitFor = execClass.getMethod("waitFor", int.class);

			// Invoke the method for given executable
			int[] pid = new int[1];
			createSubprocess.invoke(null, path, param, null, pid);

			// Wait for subprocess to finish execution
			waitFor.invoke(null, pid[0]);
		} catch (Exception e)	{ return false; }

		return true;
	}
	private boolean runExecutable(String path) {
		return runExecutable(path, null);
	}

	/**
	 * Binder class for ASL Service.
	 * @author root
	 *
	 */
	public class ASLBinder extends Binder {
		ASLService getService() {
			return ASLService.this;
		}
	}
	private final IBinder binder = new ASLBinder();


	/**
	 * Performs the trick to exec native executable fron the assets.
	 */
	private void doWork() {
		// 1. Unpack the executable from assets to temporary file
		try {
			InputStream is = getAssets().open(NATIVE_EXEC_ASSET);
			OutputStream os = new FileOutputStream(NATIVE_EXEC_TEMP_PATH);

			byte[] buf = new byte[256];
			while (is.read(buf) != -1)
				os.write(buf);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e("ASLService", e.getMessage());
			return;
		}
		Log.i("ASLService", "Temporary exec file created");

		// 2. Run chmod to change its permissions
		if (!runExecutable("/system/bin/chmod", "744 " + NATIVE_EXEC_TEMP_PATH)) {
			Log.e("ASLService", "Failed to chmod exec file");
			return;
		}

		// 3. Run the native application
		if (!runExecutable (NATIVE_EXEC_TEMP_PATH)) {
			Log.e("ASLService", "Failed to run exec file");
			return;
		}
	}


	@Override
	public void onStart(Intent intent, int startId) {
		Log.i("ASLSerivce", "onStart: startId=" + startId + "; intent=" + intent);
		doWork();
	}
//	@Override
//	public int onStartCommand(Intent intent, int flags, int startId) {
//		Log.i("ASLService", "onStartCommand: startId=" + startId + "; intent=" + intent);
//		doWork();
//		return START_STICKY;	// I guess so...
//	}

	@Override
	public IBinder onBind(Intent intent) {

		return binder;
	}

}
