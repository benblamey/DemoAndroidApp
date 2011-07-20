package com.ml4d.ohow;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;

/*
 * Interactive logic for the sign in activity.
 */
public class SignIn extends Activity implements OnClickListener {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.signin);

		View registerButton = findViewById(R.id.sign_in_register_button);
		registerButton.setOnClickListener(this);
	}

	/*
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.sign_in_register_button:
			// Start the 'register' activity.
			Intent registerIntent = new Intent(this, Register.class);
			startActivity(registerIntent);
			break;
		}
	}

}