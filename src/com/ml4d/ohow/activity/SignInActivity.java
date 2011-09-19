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
import com.ml4d.ohow.App;
import com.ml4d.ohow.OHOWAPIResponseHandler;
import com.ml4d.ohow.CredentialStore;
import com.ml4d.ohow.R;
import com.ml4d.ohow.exceptions.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

/*
 * Interactive logic for the sign in activity.
 */
public class SignInActivity extends Activity implements OnClickListener, DialogInterface.OnClickListener {

	/*
	 * With the activity lifecycle an an asynchronous HTTP request to handle,
	 * this class is designed as a state machine at its core.
	 * http://en.wikipedia.org/wiki/State_machine
	 */
	private enum State {
		INITIAL, DATA_MOMENT, WAITING, SUCCESS, FAILED
	}

	// Request codes for starting various activities.
	private static final int PRE_REGISTER_SLIDE_SHOW_REQUEST_CODE = 818548;
	private static final int SHOW_HOME_REQUEST_CODE = 6874314;
	private static final int SHOW_REGISTER_REQUEST_CODE = 4167184;
	
	/**
	 * Tracks whether the home activity has been started.
	 */
	private boolean _homeActivityAtTopOfStack = false;
	
	private String _errorMessage;
	private SignInTask _signInTask;
	private State _state = State.INITIAL;
	private DialogInterface _dialog;

	/**
	 * Removes any credentials from the credentials store, and then
	 * navigates to the 'SignIn' activity.
	 */
	public static void signInAgain(Activity activity) {
		CredentialStore.getInstance().clear();
		
		// We set some flags to ensure that if the user presses the 'back' button they 'quit' the app.
		// (the sign in activity is always the first history item in the stack - we clear all items in front of it,
		//  the splash screen activity is the first activity to be shown when starting the app, but it has the 'noHistory'
		//  flag set).
		Intent i = new Intent(activity, SignInActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		activity.startActivity(i);
		activity.finish();
	}
	
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

		if (CredentialStore.getInstance().getHaveVerifiedCredentials()) { 
			startHomeActivity();
		} else {
			Resources resources = getResources();
	
			switch (_state) {
			case DATA_MOMENT:
				// Nothing to do.
				break;
			case WAITING:
				// Show a 'waiting' dialog.
				_dialog = ProgressDialog.show(this, resources.getString(R.string.sign_in_waiting_dialog_title),
						resources.getString(R.string.sign_in_waiting_dialog_body), true, // Indeterminate.
						false); // Not cancellable.
				break;
			case SUCCESS:
				throw new RuntimeException("if we have been successful, we should have credentials");
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
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (CredentialStore.getInstance().getHaveVerifiedCredentials()) {
			// Start the home activity.
			startHomeActivity();
		} else {
			setContentView(R.layout.signin);

			findViewById(R.id.sign_in_sign_in_button).setOnClickListener(this);
			findViewById(R.id.sign_in_register_button).setOnClickListener(this);
	
			if (savedInstanceState != null) {
				_state = Enum.valueOf(State.class, savedInstanceState.getString("_state"));
				_errorMessage = savedInstanceState.getString("_errorMessage");
				
				// If we have previously successfully logged in, go back to the data-moment state.
				// Otherwise we will redirect immediately back to the home activity.
				if (State.SUCCESS == _state) {
					_state = State.DATA_MOMENT;
					_errorMessage = "";
				}
	
			} else {
				_state = State.DATA_MOMENT;
			}
	
			showState();
		}
	}

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		ensureTaskIsStopped();
		
		if (State.WAITING == _state) {
			_state = State.FAILED;
		}

		outState.putString("_state", _state.name());
		outState.putString("_errorMessage", _errorMessage);
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
		// Start the 'SlideShow' activity. 
		Intent intent = new Intent(this, SlideShowActivity.class);
		startActivityForResult(intent, PRE_REGISTER_SLIDE_SHOW_REQUEST_CODE);
	}
	
	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		if (requestCode == PRE_REGISTER_SLIDE_SHOW_REQUEST_CODE) {
			if (RESULT_OK == resultCode) {
				Intent intent = new Intent(this, RegisterActivity.class);
				startActivityForResult(intent, SHOW_REGISTER_REQUEST_CODE);
			} else if (RESULT_CANCELED == resultCode) {
				// The slide show was cancelled (the user pressed 'back') - stay 
				// where we are, the user must watch the slide show all the way through
				// before registering.
			} else {
				throw new RuntimeException("Unexpected resultCode value.");
			}
		} else if (requestCode == SHOW_REGISTER_REQUEST_CODE) {
			if (RESULT_CANCELED == resultCode) {
				// The user has pressed 'back' from register - do nothing.
			} else if (RESULT_OK == resultCode) {
				// The registration completed successfully, we should now have credentials.
				assert CredentialStore.getInstance().getHaveVerifiedCredentials();
				startHomeActivity();
			} else {
				throw new RuntimeException("unexpected result code from activity 'Register'.");
			}
		} else if (requestCode == SHOW_HOME_REQUEST_CODE) {
			_homeActivityAtTopOfStack = false;
			if (RESULT_CANCELED == resultCode) {
				// The user has pressed 'back' from home - bubble back.
				setResult(RESULT_CANCELED);
				finish();
			} else {
				throw new RuntimeException("unexpected result code from activity 'Home'.");
			}
		}  else {
			throw new RuntimeException("unexpected requestCode.");
		}
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
			HttpPost post = new HttpPost(OHOWAPIResponseHandler.getBaseUrlIncludingTrailingSlash(true) + "check_credentials.php");
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
					_state = State.DATA_MOMENT;
					showState();
				} else {
					throw new IllegalStateException();
				}
				break;
			case SUCCESS:
			case DATA_MOMENT:
			case WAITING:
				throw new IllegalStateException();
			default:
				throw new UnexpectedEnumValueException(_state);
		}
	}
	
	/**
	 * Start the home activity, unless it has already been started.
	 */
	private void startHomeActivity() {
		// We need to ensure we don't create multiple instances of the 'Home' activity at the top of the stack.
		if (!_homeActivityAtTopOfStack) {
			_homeActivityAtTopOfStack = true;
			Intent i = new Intent(this, HomeActivity.class);
			startActivityForResult(i, SHOW_HOME_REQUEST_CODE);
		}
	}

	/**
	 * Asynchronously performs a HTTP request.
	 */
	private class SignInTask extends AsyncTask<HttpPost, Void, HttpResponse> {
		private WeakReference<SignInActivity> _parent;
		private String _username;
		private String _password;		 

		public SignInTask(SignInActivity parent, String username, String password) {
			// Use a weak-reference for the parent activity. This prevents a memory leak should the activity be destroyed.
			_parent = new WeakReference<SignInActivity>(parent);
			_username = username;
			_password = password;
		}

		@Override
		protected HttpResponse doInBackground(HttpPost... arg0) {
			// This is executed on a background thread.
			HttpClient client = new DefaultHttpClient();
			HttpProtocolParams.setUserAgent(client.getParams(), App.Instance.getUserAgent());
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
			
			SignInActivity parent = _parent.get();
			
			// 'parent' will be null if it has already been garbage collected.
			if ((null != parent ) && (parent._signInTask == this)) {

				CredentialStore auth = CredentialStore.getInstance();
				
				try {
					// ProcessJSONResponse() appropriately handles a null result.
					
					// We don't actually care about the response, we just need to ensure there are no errors.
					OHOWAPIResponseHandler.ProcessJSONResponse(response);

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
