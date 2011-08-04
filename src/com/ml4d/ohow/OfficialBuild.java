package com.ml4d.ohow;

import java.util.regex.Pattern;

import com.ml4d.core.exceptions.CalledFromWrongThreadException;
import com.ml4d.core.exceptions.ImprobableCheckedExceptionException;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

/**
 * Various utility methods relating to whether the running app is an official build.
 */
public class OfficialBuild {
	
	private static OfficialBuild _instance;
	private boolean _isOfficialBuild;

	/**
	 * Class is single instance, we do not allow direct instantiation.
	 * @param context
	 */
	private OfficialBuild(Activity activity) {
		
		PackageInfo packageInfo;
		try {
			packageInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			throw new ImprobableCheckedExceptionException(e);
		}
		
		String versionName = packageInfo.versionName;
		
		boolean matchesVersionNameForOfficialBuild = Pattern.matches(("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+ .*"), versionName);
		
		_isOfficialBuild = matchesVersionNameForOfficialBuild;
	}

	/**
	 * Gets the single instance of this class (creating one if necessary).
	 * @param activity 
	 * @return The single instance of this class.
	 */
	public static OfficialBuild getInstance(Activity activity)
	{
		// This method may only be called from the main looper thread.
		if (Thread.currentThread() != activity.getMainLooper().getThread()) {
			throw new CalledFromWrongThreadException();
		}

		// We have ensured we are running on the UI thread, so there is no need for locking here. 
		if (null == _instance) {
			_instance = new OfficialBuild(activity);
		}
		return _instance;
	}

	/**
	 * Get whether this is an official/server build.
	 * 
	 * A server build is built directly from source control, and has a traceable version number associated with it.
	 * This is contrasted with a developer build, which is a build done on the developers machine. One cannot be certain of what source code was on the developers machine
	 * at the time, and developer builds do not have a traceable version number.
	 * 
	 * The public should only have access to official/server builds.-
	 * 
	 * This is not to be confused with the choice of either release or debug build targets - which is orthogonal.
	 * @param activity
	 * @return whether this is an official/server build.
	 */
	public boolean isOfficialBuild() {
		return _isOfficialBuild;
	}
}