package com.ml4d.ohow;

import java.io.IOException;
import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

public class Splash extends Activity {

	private SplashScreenWait _task;
	private static final Integer _delay = 5000; 
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// If we did some waiting the last time this activity was being shown, go straight to the next activity.
		if (null != savedInstanceState) {
			boolean hadPreviouslyBegunWaiting = savedInstanceState.getBoolean("HAVE_BEGUN_WAITING");			
			if (hadPreviouslyBegunWaiting) {
				startSignInActivity();
			}
		}
		
		_task = new SplashScreenWait(this, _delay);
	}

	protected void onSaveInstanceState(Bundle outState) {
		
		outState.putBoolean("HAVE_BEGUN_WAITING", true);
	}
	
	private void startSignInActivity() {
		Intent signInIntent = new Intent(this, SignIn.class);
		startActivity(signInIntent);
	}
	
	/**
	 * Asynchronously performs a HTTP request.
	 */
	private class SplashScreenWait extends AsyncTask<Object, Object, Object> {
		private WeakReference<Splash> _parent;
		private Integer _delay = 5000;
		
		public SplashScreenWait(Splash parent, Integer delay) {
			// Use a weak-reference for the parent activity. This prevents a memory leak should the activity be destroyed.
			_parent = new WeakReference<Splash>(parent);
			_delay = delay;
		}

		@Override
		protected Object doInBackground(Object... arg0) {
			
            int waited = 0;
            while(!super.isCancelled() && (waited < _delay)) {
            	// Sleep for 100 ms.
            	try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					return null;
				}
                waited += 100;
            }
			
			return null;
		}

		protected void onPostExecute(Object result) {
			
			Splash parent = _parent.get();
			
			// 'parent' will be null if it has already been garbage collected.
			if (parent._task == this) {
				parent.startSignInActivity();
			}
		}

	}
	
}