package pl.polidea.asl;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ASLService extends Service {

	private bool runExecutable(String path) {
		try {
			Class<?> execClass = Class.forName("android.os.Exec");
			Method createSubprocess = execClass.getMethod("createSubprocess",
				String.class, String.class, String.class, int[].class);
			Method waitFor = execClass.getMethod("waitFor", int.class);


		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
