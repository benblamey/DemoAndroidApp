package com.ml4d.ohow;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import com.ml4d.core.exceptions.ImprobableCheckedExceptionException;
import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

/**
 * The OHOW application.
 * @author ben
 */

// The key of the Google Docs Form for submitting crash reports.
@ReportsCrashes(formKey = "dDI4M2gwVmZvLW1oOWNLOGtFQ09oR2c6MQ")
public class App extends Application {

	/**
	 * The single instance of the OHOW app.
	 */
	public static App Instance;
	
	public App() {
		super();
		Instance = this;
	}
	
    @Override
    public void onCreate() {
        // We use the 'ACRA' crash reporting tool on official builds only.
    	if (OfficialBuild.getInstance().isOfficialBuild()) {
    		ACRA.init(this);
    	}
        super.onCreate();
    }

	/**
	 * Gets a string that indentifies this app and includes its version number,
	 * intended for use a user-agent string in a HTTP request.
	 * @return
	 */
	public String getUserAgent() {
		// While we are on the UI thread, build a user-agent string from
		// the package details.
		PackageInfo packageInfo;
		try {
			packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		} catch (NameNotFoundException e1) {
			throw new ImprobableCheckedExceptionException(e1);
		}
		return packageInfo.packageName + " Android App, version: " + packageInfo.versionName;
	}

}
