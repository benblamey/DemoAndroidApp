package com.ml4d.ohow;

import java.util.regex.Pattern;
import com.ml4d.core.exceptions.ImprobableCheckedExceptionException;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

/**
 * Various utility methods relating to whether the running app is an official build.
 */
public class OfficialBuild {
	
	private static OfficialBuild _instance;
	private boolean _isOfficialBuild;
	private boolean _isLiveOfficialBuild; 

	/**
	 * Class is single instance, we do not allow direct instantiation.
	 * @param context
	 */
	private OfficialBuild() {
		
		PackageInfo packageInfo;
		try {
			packageInfo = App.Instance.getPackageManager().getPackageInfo(App.Instance.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			throw new ImprobableCheckedExceptionException(e);
		}
		
		String versionName = packageInfo.versionName;
		
		_isOfficialBuild = Pattern.matches(("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+.*"), versionName);
		_isLiveOfficialBuild = !Pattern.matches(("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+ \\(Live\\)"), versionName);
	}

	/**
	 * Gets the single instance of this class (creating one if necessary).
	 * @param activity 
	 * @return The single instance of this class.
	 */
	public synchronized static OfficialBuild getInstance()
	{
		// We have ensured we are running on the UI thread, so there is no need for locking here. 
		if (null == _instance) {
			_instance = new OfficialBuild();
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
	
	/**
	 * Get whether the LIVE OHOW server API should be used.
	 * 
	 * 'LIVE' official builds use the 'LIVE' Server API. Other official builds use the 'DEV' Server API.
	 * @param activity
	 * @return whether this is an official/server build.
	 */
	public boolean useLiveOHOWApi() {
		return _isLiveOfficialBuild;
	}
}
