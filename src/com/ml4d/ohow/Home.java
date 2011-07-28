package com.ml4d.ohow;

import com.ml4d.ohow.exceptions.UnknownClickableItemException;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

/*
 * Interactive logic for the sign in activity.
 */
public class Home extends Activity implements OnClickListener {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home);

		findViewById(R.id.home_sign_out_button).setOnClickListener(this);
		
		if (!new CredentialStore(this).getHaveVerifiedCredentials()) {
			// Start the sign in activity.
			startActivity(new Intent(this, SignIn.class));
		}
	}

	private void signOutButtonClicked() {
		// Clear the saved credentials.
		CredentialStore auth = new CredentialStore(this);
		auth.Clear();
		
		// Start the sign in activity.
		startActivity(new Intent(this, SignIn.class));
	}

	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.home_sign_out_button:
			signOutButtonClicked();
			break;
		default:
			throw new UnknownClickableItemException(view.getId());
		}
	}

}