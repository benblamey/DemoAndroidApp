package com.ml4d.ohow;

import java.io.*;
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

import com.ml4d.ohow.exceptions.*;

import android.app.Activity;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

/*
 * Interactive logic for the sign in activity.
 */
public class Home extends Activity implements OnClickListener { //, DialogInterface.OnClickListener {

//	/*
//	 * With the activity lifecycle an an asynchronous HTTP request to handle,
//	 * this class is designed as a state machine at its core.
//	 * http://en.wikipedia.org/wiki/State_machine
//	 */
//	private enum State {
//		NORMAL, WAITING, SUCCESS
//	}
//
//	private SignInTask _signInTask;
//	private State _state;
//	private DialogInterface _dialog;

//	/*
//	 * Updates the user-interface to represent the state of this activity
//	 * object.
//	 */
//	private void showState() {
//
//		// If a dialog is being displayed, remove it.
//		if (_dialog != null) {
//			_dialog.dismiss();
//			_dialog = null;
//		}
//
//		Resources resources = getResources();
//
//		switch (_state) {
//		case NORMAL:
//			// Nothing to do.
//			break;
//		case WAITING:
//			// Show a 'waiting' dialog.
//			_dialog = ProgressDialog.show(this, resources.getString(R.string.sign_in_waiting_dialog_title),
//					resources.getString(R.string.sign_in_waiting_dialog_body), true, // Indeterminate.
//					false); // Not cancellable.
//			break;
//		case SUCCESS:
//			// Start the 'home' activity.
//			// TODO: store credentials.
//			startActivity(new Intent(this, Home.class));
//			break;
//		case FAILED:
//			// Show a 'failed' dialog.
//			AlertDialog failedDialog = new AlertDialog.Builder(this).create();
//			failedDialog.setTitle(resources.getString(R.string.sign_in_waiting_dialog_title));
//			failedDialog.setMessage(_errorMessage);
//			failedDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", this);
//			failedDialog.show();
//			_dialog = failedDialog;
//			break;
//		default:
//			throw new UnexpectedEnumValueException(_state);
//		}
//	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home);

		findViewById(R.id.home_sign_out_button).setOnClickListener(this);

//		if (savedInstanceState != null) {
//			_state = Enum.valueOf(State.class, savedInstanceState.getString("_state"));
//			//_errorMessage = savedInstanceState.getString("_errorMessage");
//
//			// Because we have different layouts for portrait and landscape
//			// views, we need to manually save and restore the state of the
//			// TextViews.
//			restoreTextViewInstanceState(savedInstanceState, R.id.signin_edittext_username);
//			restoreTextViewInstanceState(savedInstanceState, R.id.signin_edittext_password);
//
//			// Restore the focused view.
//			View focusTarget = findViewById(savedInstanceState.getInt("focused_view"));
//			if (null != focusTarget) {
//				focusTarget.requestFocus();
//			}
//
//		} else {
//			_state = State.NORMAL;
//		}

		//showState();
	}

//	protected void onSaveInstanceState(Bundle outState) {
//		switch (_state) {
//		case NORMAL:
//			break;
//		case WAITING:
//			// Don't interrupt the operation if it has started. The results are difficult to predict.
//			// Cancel the operation.
//			_signInTask.cancel(false); 
//			_signInTask = null;
//			_state = State.FAILED;
//			break;
//		case SUCCESS:
//			_state = State.NORMAL;
//			break;
//		case FAILED:
//			break;
//		default:
//			throw new UnexpectedEnumValueException(_state);
//		}
//
//		// Because we have different layouts for portrait and landscape views,
//		// we need to manually save and restore the state of the TextViews.
//		saveTextViewInstanceState(outState, R.id.signin_edittext_username);
//		saveTextViewInstanceState(outState, R.id.signin_edittext_password);
//
//		// Save which view is focused.
//		View v = getCurrentFocus();
//		if (null != v) {
//			outState.putInt("focused_view", v.getId());
//		}
//
//		outState.putString("_state", _state.name());
//		outState.putString("_errorMessage", _errorMessage);
//	}

//	/*
//	 * Saves the state of the specified TextView.
//	 */
//	private void saveTextViewInstanceState(Bundle state, int textViewId) {
//		Parcelable instanceState = ((TextView) this.findViewById(textViewId)).onSaveInstanceState();
//		state.putParcelable("textView_id_" + Integer.toString(textViewId), instanceState);
//	}
//
//	/*
//	 * Restores the state of the specified TextView.
//	 */
//	private void restoreTextViewInstanceState(Bundle state, int textViewId) {
//		Parcelable instanceState = state.getParcelable("textView_id_" + Integer.toString(textViewId));
//		if (null != instanceState) {
//			((TextView) this.findViewById(textViewId)).onRestoreInstanceState(instanceState);
//		}
//	}

	@Override
	protected void onStart() {
		super.onStart();
		// The activity is about to become visible.
		//showState();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// The activity has become visible (it is now "resumed").
	//	showState();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Another activity is taking focus (this activity is about to be
		// "paused").
	//	ensureTaskIsStopped();
	}

//	/**
//	 * Ensures the asynchronous HTTP task is stopped.
//	 */
//	private void ensureTaskIsStopped() {
//		if (State.WAITING == _state) {
//			// Don't interrupt the operation if it has started. The results are difficult to predict.
//			if (this._signInTask != null) {
//				_signInTask.cancel(false); 
//				_signInTask = null;
//			}
//			_errorMessage = getResources().getString(R.string.register_error_rotate_when_busy);
//			_state = State.FAILED;
//		}
//	}

	@Override
	protected void onStop() {
		super.onStop();
		// The activity is no longer visible (it is now "stopped").
		//ensureTaskIsStopped();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// The activity is about to be destroyed.
		//ensureTaskIsStopped();
	}

//	private void registerButtonClicked() {
//		// Start the 'register' activity.
//		startActivity(new Intent(this, Register.class));
//	}
//	
	private void signInButtonClicked() {

	//	Resources resources = this.getResources();

//		String username = ((TextView) this.findViewById(R.id.signin_edittext_username)).getText().toString();
//		String password = ((TextView) this.findViewById(R.id.signin_edittext_password)).getText().toString();
//
//		String validationMessage = "";
//
//		// Username.
//		if (0 == username.length()) {
//			validationMessage = resources.getString(R.string.sign_in_invalid_username);
//		}
//
//		// Password.
//		else if (0 == password.length()) {
//			validationMessage = resources.getString(R.string.sign_in_invalid_password);
//		}
//		
//		if (validationMessage.length() > 0) {
//			Toast.makeText(this, validationMessage, Toast.LENGTH_LONG).show();
//		} else {

		// The HttpClient will verify the certificate is signed by a trusted
		// source.
	
		APIAuthentication auth = new APIAuthentication(this);
	
		HttpPost post = new HttpPost("https://cpanel02.lhc.uk.networkeq.net/~soberfun/1/sign_out.php");
		post.setHeader("Accept", "application/json");

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("session_key", auth.getSessionKey()));
		auth.Clear();
		
		startActivity(new Intent(this, SignIn.class));
		
		UrlEncodedFormEntity url = null;
		try {
			url = new UrlEncodedFormEntity(params, HTTP.UTF_8);
			post.setEntity(url);

			// Don't store the result, we don't care about the outcome.
			new SignOutTask(this).execute(post);
			//_signInTask
		} catch (UnsupportedEncodingException e) {
			throw new ImprobableCheckedExceptionException(e);
		}
		//_state = State.WAITING;
		//showState();
	}

	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.home_sign_out_button:
			signInButtonClicked();
			break;
		default:
			// TODO: throw new UnexpectedEnumValueException();
		}
	}

//	@Override
//	public void onClick(DialogInterface dialog, int which) {
//
//		switch (_state) {
//
//		case FAILED:
//			// When the user clicks on the success confirmation, go back to the
//			// sign_in page.
//			if (DialogInterface.BUTTON_POSITIVE == which) {
//				_state = State.NORMAL;
//				showState();
//			} else {
//				throw new IllegalStateException();
//			}
//			break;
//		case SUCCESS:
//		case NORMAL:
//		case WAITING:
//			throw new IllegalStateException();
//		default:
//			throw new UnexpectedEnumValueException(_state);
//		}
//	}

	/**
	 * Asynchronously performs a HTTP request.
	 */
	private class SignOutTask extends AsyncTask<HttpPost, Void, HttpResponse> {
//		private WeakReference<SignIn> _parent;
		private String _userAgent;

		public SignOutTask(ContextWrapper parent) {
			// Use a weak-reference for the parent activity. This prevents a memory leak should the activity be destroyed.
	//		_parent = new WeakReference<SignIn>(parent);

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
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		protected void onPostExecute(HttpResponse response) {
			// Don't worry about the result.
		}
			
//			SignIn parent = _parent.get();
//			
//			// 'parent' will be null if it has already been garbage collected.
//			if (parent._signInTask == this) {
//
//				APIAuthentication auth = new APIAuthentication(parent);
//				
//				try {
//					// ProcessJSONResponse() appropriately handles a null
//					// result.
//					
//					Object result = APIResponseHandler.ProcessJSONResponse(response, getResources());
//					
//					if (!(result instanceof JSONObject)) {
//						throw new UnexpectedOHOWAPIResponseException("Expecting a JSON Object - ensure you are using the latest version of the app.");
//					}
//					
//					JSONObject resultJson = (JSONObject)result;
//					
//					String sessionKey = resultJson.getString("session_key");
//					auth.setKnownGoodSessionKeyAndUsername(_username, _password, sessionKey, DateTimeUtilities.getTimeFromUnixTime(resultJson.getInt("expires")));
//					parent._state = State.SUCCESS;
//					
//				} catch (OHOWAPIException e) {
//					
//					if ((401 == e.getHttpCode()) && (3 == e.getExceptionCode())) {
//						// The password was wrong. Clear any saved credentials or session keys.
//						auth.Clear();
//					}
//					
//					parent._state = State.FAILED;
//					parent._errorMessage = e.getLocalizedMessage();
//				} catch (NoResponseAPIException e) {
//					parent._state = State.FAILED;
//					parent._errorMessage = parent.getResources().getString(R.string.comms_error);
//				} catch (UnexpectedOHOWAPIResponseException e) {
//					// This exception is unlikely. We don't localize the message. 
//					parent._state = State.FAILED;
//					parent._errorMessage = e.getLocalizedMessage();
//				} catch (JSONException e) {
//					// A JSON property was missing or something similar.
//					// This exception is unlikely. We don't localize the message. 
//					parent._state = State.FAILED;
//					parent._errorMessage = e.getLocalizedMessage();
//				}
//
//				parent.showState();
//			}
//		}

	}
}
