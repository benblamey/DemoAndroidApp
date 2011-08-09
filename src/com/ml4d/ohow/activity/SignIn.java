package com.ml4d.ohow.activity;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import com.ml4d.core.exceptions.ImprobableCheckedExceptionException;
import com.ml4d.core.exceptions.UnexpectedEnumValueException;
import com.ml4d.core.exceptions.UnknownClickableItemException;
import com.ml4d.ohow.OHOWAPIResponseHandler;
import com.ml4d.ohow.CredentialStore;
import com.ml4d.ohow.R;
import com.ml4d.ohow.exceptions.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

/*
 * Interactive logic for the sign in activity.
 */
public class SignIn extends Activity implements OnClickListener, DialogInterface.OnClickListener {

	/*
	 * With the activity lifecycle an an asynchronous HTTP request to handle,
	 * this class is designed as a state machine at its core.
	 * http://en.wikipedia.org/wiki/State_machine
	 */
	private enum State {
		DATA_ENTRY, WAITING, SUCCESS, FAILED
	}

	private String _errorMessage;
	private SignInTask _signInTask;
	private State _state;
	private DialogInterface _dialog;

	/*
	 * Updates the user-interface to represent the state of this activity
	 * object.
	 */
	private void showState() {

		// If a dialog is being displayed, remove it.
		if (_dialog != null) {
			_dialog.dismiss();
			_dialog = null;
		}

		Resources resources = getResources();

		switch (_state) {
		case DATA_ENTRY:
			// Nothing to do.
			break;
		case WAITING:
			// Show a 'waiting' dialog.
			_dialog = ProgressDialog.show(this, resources.getString(R.string.sign_in_waiting_dialog_title),
					resources.getString(R.string.sign_in_waiting_dialog_body), true, // Indeterminate.
					false); // Not cancellable.
			break;
		case SUCCESS:
			// Start the 'home' activity.
			// Credentials/session key has already been stored.
			startActivity(new Intent(this, Home.class));
			break;
		case FAILED:
			// Show a 'failed' dialog.
			AlertDialog failedDialog = new AlertDialog.Builder(this).create();
			failedDialog.setTitle(resources.getString(R.string.error_dialog_title));
			failedDialog.setMessage(_errorMessage);
			failedDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", this);
			failedDialog.setCancelable(false);
			failedDialog.show();
			_dialog = failedDialog;
			break;
		default:
			throw new UnexpectedEnumValueException(_state);
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.signin);
		
		if (CredentialStore.getInstance(this).getHaveVerifiedCredentials()) {
			// Start the home activity.
			startActivity(new Intent(this, Home.class));
		}

		findViewById(R.id.sign_in_sign_in_button).setOnClickListener(this);
		findViewById(R.id.sign_in_register_button).setOnClickListener(this);

		if (savedInstanceState != null) {
			_state = Enum.valueOf(State.class, savedInstanceState.getString("_state"));
			_errorMessage = savedInstanceState.getString("_errorMessage");
			
			// If we have previously successfully logged in, go back to the data-entry state.
			// Otherwise we will redirect immediately back to the home activity.
			if (State.SUCCESS == _state) {
				_state = State.DATA_ENTRY;
				_errorMessage = "";
			}

			// Because we have different layouts for portrait and landscape
			// views, we need to manually save and restore the state of the
			// TextViews.
			restoreTextViewInstanceState(savedInstanceState, R.id.signin_edittext_username);
			restoreTextViewInstanceState(savedInstanceState, R.id.signin_edittext_password);

			// Restore the focused view.
			View focusTarget = findViewById(savedInstanceState.getInt("focused_view"));
			if (null != focusTarget) {
				focusTarget.requestFocus();
			}

		} else {
			_state = State.DATA_ENTRY;
		}

		showState();
	}

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		ensureTaskIsStopped();
		
		if (State.WAITING == _state) {
			_state = State.FAILED;
		}

		// Because we have different layouts for portrait and landscape views,
		// we need to manually save and restore the state of the TextViews.
		saveTextViewInstanceState(outState, R.id.signin_edittext_username);
		saveTextViewInstanceState(outState, R.id.signin_edittext_password);

		// Save which view is focused.
		View focusedView = getCurrentFocus();
		if (null != focusedView) {
			outState.putInt("focused_view", focusedView.getId());
		}

		outState.putString("_state", _state.name());
		outState.putString("_errorMessage", _errorMessage);
	}

	/*
	 * Saves the state of the specified TextView.
	 */
	private void saveTextViewInstanceState(Bundle state, int textViewId) {
		Parcelable instanceState = ((TextView) this.findViewById(textViewId)).onSaveInstanceState();
		state.putParcelable("textView_id_" + Integer.toString(textViewId), instanceState);
	}

	/*
	 * Restores the state of the specified TextView.
	 */
	private void restoreTextViewInstanceState(Bundle state, int textViewId) {
		Parcelable instanceState = state.getParcelable("textView_id_" + Integer.toString(textViewId));
		if (null != instanceState) {
			((TextView) this.findViewById(textViewId)).onRestoreInstanceState(instanceState);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		// The activity is about to become visible.
		showState();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// The activity has become visible (it is now "resumed").
		showState();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Another activity is taking focus (this activity is about to be
		// "paused").
		ensureTaskIsStopped();
	}

	/**
	 * Ensures the asynchronous HTTP task is stopped.
	 */
	private void ensureTaskIsStopped() {
		
		// Don't interrupt the operation if it has started. The results are difficult to predict.
		if (_signInTask != null) {
			_signInTask.cancel(false); 
			_signInTask = null;
		}
		
		if (State.WAITING == _state) {
			_errorMessage = getResources().getString(R.string.dialog_error_task_canceled);
			_state = State.FAILED;
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		// The activity is no longer visible (it is now "stopped").
		ensureTaskIsStopped();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// The activity is about to be destroyed.
		ensureTaskIsStopped();
	}

	private void registerButtonClicked() {
		// Start the 'register' activity.
		startActivity(new Intent(this, Register.class));
	}
	
	private void signInButtonClicked() {
		Resources resources = getResources();

		String username = ((TextView) findViewById(R.id.signin_edittext_username)).getText().toString();
		String password = ((TextView) findViewById(R.id.signin_edittext_password)).getText().toString();

		String validationMessage = "";

		// Username.
		if (0 == username.length()) {
			validationMessage = resources.getString(R.string.sign_in_invalid_username);
		}

		// Password.
		else if (0 == password.length()) {
			validationMessage = resources.getString(R.string.sign_in_invalid_password);
		}
		
		if (validationMessage.length() > 0) {
			Toast.makeText(this, validationMessage, Toast.LENGTH_LONG).show();
		} else {

			// The HttpClient will verify the certificate is signed by a trusted
			// source.
			HttpPost post = new HttpPost("https://cpanel02.lhc.uk.networkeq.net/~soberfun/1/check_credentials.php");
			post.setHeader("Accept", "application/json");

			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("username", username));
			params.add(new BasicNameValuePair("password", password));

			// Update the UI to show that we are waiting.
			_state = State.WAITING;
			showState();
			
			UrlEncodedFormEntity url = null;
			try {
				url = new UrlEncodedFormEntity(params, HTTP.UTF_8);
				
				
				post.setEntity(url);
				_signInTask = new SignInTask(this, username, password);
				_signInTask.execute(post);
			} catch (UnsupportedEncodingException e) {
				throw new ImprobableCheckedExceptionException(e);
			}
		}
	}

	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.sign_in_register_button:
			registerButtonClicked();
			break;
		case R.id.sign_in_sign_in_button:
			signInButtonClicked();
			break;
		default:
			throw new UnknownClickableItemException(view.getId());
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {

		switch (_state) {

		case FAILED:
			// When the user clicks on the success confirmation, go back to the
			// sign_in page.
			if (DialogInterface.BUTTON_POSITIVE == which) {
				_state = State.DATA_ENTRY;
				showState();
			} else {
				throw new IllegalStateException();
			}
			break;
		case SUCCESS:
		case DATA_ENTRY:
		case WAITING:
			throw new IllegalStateException();
		default:
			throw new UnexpectedEnumValueException(_state);
		}
	}

	/**
	 * Asynchronously performs a HTTP request.
	 */
	private class SignInTask extends AsyncTask<HttpPost, Void, HttpResponse> {
		private WeakReference<SignIn> _parent;
		private String _userAgent;
		private String _username;
		private String _password;		 

		public SignInTask(SignIn parent, String username, String password) {
			// Use a weak-reference for the parent activity. This prevents a memory leak should the activity be destroyed.
			_parent = new WeakReference<SignIn>(parent);
			_username = username;
			_password = password;

			// Whilst we are on the UI thread, build a user-agent string from
			// the package details.
			PackageInfo packageInfo;
			try {
				packageInfo = parent.getPackageManager().getPackageInfo(parent.getPackageName(), 0);
			} catch (NameNotFoundException e1) {
				throw new ImprobableCheckedExceptionException(e1);
			}
			_userAgent = packageInfo.packageName + " Android App, version: " + packageInfo.versionName;
		}

		@Override
		protected HttpResponse doInBackground(HttpPost... arg0) {
			// This is executed on a background thread.
			HttpClient client = new DefaultHttpClient();
			HttpProtocolParams.setUserAgent(client.getParams(), _userAgent);
			try {
				return client.execute(arg0[0]);
			} catch (ClientProtocolException e) {
				return null;
			} catch (IOException e) {
				return null;
			}
		}

		protected void onPostExecute(HttpResponse response) {
			// On the main thread.
			
			SignIn parent = _parent.get();
			
			// 'parent' will be null if it has already been garbage collected.
			if ((null != parent ) && (parent._signInTask == this)) {

				CredentialStore auth = CredentialStore.getInstance(parent);
				
				try {
					// ProcessJSONResponse() appropriately handles a null result.
					
					// We don't actually care about the response, we just need to ensure there are no errors.
					OHOWAPIResponseHandler.ProcessJSONResponse(response, getResources());

					// Store the credentials now that they have been verified.
					auth.setKnownGoodDetails(_username, _password);

					// To complete without error is a success.
					parent._state = State.SUCCESS;
					
				} catch (ApiViaHttpException e) {
					
					if ((401 == e.getHttpCode()) && (3 == e.getExceptionCode())) {
						// The password was wrong. Clear any saved credentials or session keys.
						auth.clear();
						((TextView) findViewById(R.id.signin_edittext_password)).setText("");
					}
					
					parent._state = State.FAILED;
					parent._errorMessage = e.getLocalizedMessage();
				} catch (NoResponseAPIException e) {
					parent._state = State.FAILED;
					parent._errorMessage = parent.getResources().getString(R.string.comms_error);
				}


				parent.showState();
			}
		}

	}
}
