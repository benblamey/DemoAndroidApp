package com.ml4d.ohow.activity;

import java.lang.ref.WeakReference;

import com.ml4d.core.exceptions.ImprobableCheckedExceptionException;
import com.ml4d.ohow.R;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Logic for the splash screen activity.
 */
public class SplashActivity extends Activity {

	private SplashScreenWaitTask _task;
	private static final Integer DELAY_MS = 1000; 
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);
				
		PackageInfo packageInfo;
		try {
			packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		} catch (NameNotFoundException e1) {
			throw new ImprobableCheckedExceptionException(e1);
		}
		
		// Show the version number in the TextView.
		((TextView)findViewById(R.id.splash_text_view_version_number)).setText(packageInfo.versionName);
		
		_task = new SplashScreenWaitTask(this, DELAY_MS);
		_task.execute((Object[])null);
	}
	
    protected void onPause() {
    	super.onPause();
    	// We run an asynchronous task to time the splash screen. Ensure that we don't leak any resources when leaving the activity.
    	ensureTaskIsStopped();
    }

    protected void onStop() {
    	super.onStop();
    	// We run an asynchronous task to time the splash screen. Ensure that we don't leak any resources when leaving the activity.
    	ensureTaskIsStopped();
    }

    protected void onDestroy() {
    	super.onDestroy();
    	// We run an asynchronous task to time the splash screen. Ensure that we don't leak any resources when leaving the activity.
    	ensureTaskIsStopped();
    }

	private void ensureTaskIsStopped() {
		if (this._task != null) {
			// false: Don't interrupt the operation if it has started. The results are difficult to predict.
			_task.cancel(false); 
			_task = null;
		}
	}
    
	/**
	 * Does a simple wait on another thread.
	 */
	private static class SplashScreenWaitTask extends AsyncTask<Object, Object, Object> {
		private WeakReference<SplashActivity> _parent;
		private Integer _delayMs;
		private static final Integer _loopDelayMs = 100;
		
		public SplashScreenWaitTask(SplashActivity parent, Integer delayMs) {
			// Use a weak-reference for the parent activity. This prevents a memory leak should the activity be destroyed.
			_parent = new WeakReference<SplashActivity>(parent);
			_delayMs = delayMs;
		}

		@Override
		protected Object doInBackground(Object... arg0) {
            int waitedMs = 0;
            // We execute a loop incase the task is canceled while we are waiting.
            while(!super.isCancelled() && (waitedMs < _delayMs)) {
            	try {
					Thread.sleep(_loopDelayMs);
				} catch (InterruptedException e) {
					// Something has gone wrong, its only a splash screen so just finish immediately.
					return null;
				}
                waitedMs += _loopDelayMs;
            }
			return null;
		}

		protected void onPostExecute(Object result) {
			SplashActivity parent = _parent.get();
			
			// 'parent' will be null if it has already been garbage collected.
			// We want to ensure we only take action if the parent is actually 'using' this instance of the task.
			if (this == parent._task) {
				// We go to the SignIn activity - it handles redirection based on whether we have
				// saved credentials.
				Intent nextActivityIntent = new Intent(parent, SignInActivity.class);
				parent.startActivity(nextActivityIntent);
			}
		}
	}
	
}
