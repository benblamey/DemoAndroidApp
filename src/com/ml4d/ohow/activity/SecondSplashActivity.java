package com.ml4d.ohow.activity;

import com.ml4d.ohow.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/**
 * Logic for the second splash screen activity.
 */
public class SecondSplashActivity extends Activity implements View.OnClickListener {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.second_splash);
		
	}

	@Override
	public void onClick(View v) {
		// We go to the SignIn activity - it handles redirection based on whether we have
		// saved credentials.
		Intent nextActivityIntent = new Intent(this, SignInActivity.class);
		startActivity(nextActivityIntent);
	}

}