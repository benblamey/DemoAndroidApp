package com.ml4d.ohow;

import android.app.Application;

/**
 * The OHOW application.
 * @author ben

 */
public class App extends Application {

	/**
	 * The single instance of the OHOW app.
	 */
	public static App Instance;
	
	public App() {
		super();
		Instance = this;
	}

}
