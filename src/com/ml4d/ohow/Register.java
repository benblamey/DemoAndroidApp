package com.ml4d.ohow;

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

import com.ml4d.ohow.exceptions.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/*
 * Interactive logic for the register activity.
 */
public class Register extends Activity implements OnClickListener, DialogInterface.OnClickListener {

	/*
	 * With the activity lifecycle an an asynchronous HTTP request to handle,
	 * this class is designed as a state machine at its core.
	 * http://en.wikipedia.org/wiki/State_machine
	 */
	private enum State {
		DATA_ENTRY, WAITING, SUCCESS, FAILED
	}

	private String _errorMessage;
	private RegisterApiTask _registerTask;
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
			_dialog = ProgressDialog.show(this, resources.getString(R.string.register_waiting_dialog_title),
					resources.getString(R.string.register_waiting_dialog_body), true, // Indeterminate.
					false); // Not cancellable.
			break;
		case SUCCESS:
			// Show a 'success' dialog.
			AlertDialog successDialog = new AlertDialog.Builder(this).create();
			successDialog.setTitle(resources.getString(R.string.register_success_dialog_title));
			successDialog.setMessage(resources.getString(R.string.register_success_dialog_body));
			successDialog.setButton(DialogInterface.BUTTON_POSITIVE,
					getResources().getString(R.string.ok_button_label), this);
			successDialog.show();
			_dialog = successDialog;
			break;
		case FAILED:
			// Show a 'failed' dialog.
			AlertDialog failedDialog = new AlertDialog.Builder(this).create();
			failedDialog.setTitle(resources.getString(R.string.register_error_dialog_title));
			failedDialog.setMessage(_errorMessage);
			failedDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", this);
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
		setContentView(R.layout.register);

		View registerButton = findViewById(R.id.register_register_button);
		registerButton.setOnClickListener(this);

		View viewTermsAndConditionsButton = findViewById(R.id.register_view_terms_and_conditions_button);
		viewTermsAndConditionsButton.setOnClickListener(this);

		if (savedInstanceState != null) {
			_state = Enum.valueOf(State.class, savedInstanceState.getString("_state"));
			_errorMessage = savedInstanceState.getString("_errorMessage");

			// Because we have different layouts for portrait and landscape
			// views, we need to manually save and restore the state of the
			// TextViews.
			restoreTextViewInstanceState(savedInstanceState, R.id.register_edittext_first_name);
			restoreTextViewInstanceState(savedInstanceState, R.id.register_edittext_last_name);
			restoreTextViewInstanceState(savedInstanceState, R.id.register_edittext_email);
			restoreTextViewInstanceState(savedInstanceState, R.id.register_edittext_username);
			restoreTextViewInstanceState(savedInstanceState, R.id.register_edittext_password);
			restoreTextViewInstanceState(savedInstanceState, R.id.register_checkbox_terms);

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
		switch (_state) {
		case DATA_ENTRY:
			break;
		case WAITING:
			// Cancel the operation.
			_registerTask.cancel(false); // Don't interrupt the operation if it
											// has started. The results are
											// difficult to predict.
			_registerTask = null;
			_state = State.FAILED;
			break;
		case SUCCESS:
			break;
		case FAILED:
			break;
		default:
			throw new UnexpectedEnumValueException(_state);
		}

		// Because we have different layouts for portrait and landscape views,
		// we need to manually save and restore the state of the TextViews.
		saveTextViewInstanceState(outState, R.id.register_edittext_first_name);
		saveTextViewInstanceState(outState, R.id.register_edittext_last_name);
		saveTextViewInstanceState(outState, R.id.register_edittext_email);
		saveTextViewInstanceState(outState, R.id.register_edittext_username);
		saveTextViewInstanceState(outState, R.id.register_edittext_password);
		saveTextViewInstanceState(outState, R.id.register_checkbox_terms);

		// Save which view is focused.
		View v = getCurrentFocus();
		if (null != v) {
			outState.putInt("focused_view", v.getId());
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
		if (State.WAITING == _state) {
			if (this._registerTask != null) {
				_registerTask.cancel(false); // Don't interrupt the operation if
												// it has started. The results
												// are difficult to predict.
				_registerTask = null;
			}
			_errorMessage = getResources().getString(R.string.register_error_task_cancelled);
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

		Resources resources = this.getResources();

		String firstName = ((TextView) this.findViewById(R.id.register_edittext_first_name)).getText().toString();
		String lastName = ((TextView) this.findViewById(R.id.register_edittext_last_name)).getText().toString();
		String emailAddress = ((TextView) this.findViewById(R.id.register_edittext_email)).getText().toString();
		String username = ((TextView) this.findViewById(R.id.register_edittext_username)).getText().toString();
		String password = ((TextView) this.findViewById(R.id.register_edittext_password)).getText().toString();

		Pattern emailAddressRegex = Pattern.compile(APIConstants.emailRegex);
		Matcher emailAddressRegexMatcher = emailAddressRegex.matcher(emailAddress);

		Pattern usernameRegex = Pattern.compile(APIConstants.usernameRegex);
		Matcher usernameRegexMatcher = usernameRegex.matcher(username);

		String validationMessage = "";

		// First name.
		if (firstName.length() < APIConstants.firstNameMinLength) {
			validationMessage = resources.getString(R.string.register_first_name_too_short);
		} else if (firstName.length() > APIConstants.firstNameMaxLength) {
			validationMessage = String.format(resources.getString(R.string.register_first_name_too_long),
					APIConstants.firstNameMaxLength);
		}

		// Last name.
		else if (lastName.length() < APIConstants.lastNameMinLength) {
			validationMessage = resources.getString(R.string.register_last_name_too_short);
		} else if (lastName.length() > APIConstants.lastNameMaxLength) {
			validationMessage = String.format(resources.getString(R.string.register_last_name_too_long),
					APIConstants.lastNameMaxLength);
		}

		// Email address.
		else if (!emailAddressRegexMatcher.matches()) {
			validationMessage = resources.getString(R.string.register_invalid_email_address);
		}

		// Username.
		else if (!usernameRegexMatcher.matches()) {
			validationMessage = resources.getString(R.string.register_invalid_username);
		}

		// Password.
		else if (!Pattern.compile("[0-9]").matcher(password).find()
					|| !Pattern.compile("[a-z]").matcher(password).find()
					|| !Pattern.compile("[A-Z]").matcher(password).find()
					|| (APIConstants.passwordMinLength > password.length())
					|| (APIConstants.passwordMaxLength < password.length())) {
				validationMessage = resources.getString(R.string.register_invalid_password);
		}
		
		// Terms and Conditions.
		else if (false == ((CheckBox) this.findViewById(R.id.register_checkbox_terms)).isChecked()) {
			validationMessage = resources.getString(R.string.register_must_accept_terms);
		}

		if (validationMessage.length() > 0) {
			Toast.makeText(this, validationMessage, Toast.LENGTH_LONG).show();
		} else {

			// The HttpClient will verify the certificate is signed by a trusted
			// source.
			HttpPost post = new HttpPost("https://cpanel02.lhc.uk.networkeq.net/~soberfun/1/register.php");
			post.setHeader("Accept", "application/json");

			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("first_name", firstName));
			params.add(new BasicNameValuePair("last_name", lastName));
			params.add(new BasicNameValuePair("email", emailAddress));
			params.add(new BasicNameValuePair("username", username));
			params.add(new BasicNameValuePair("password", password));

			UrlEncodedFormEntity url = null;
			try {
				url = new UrlEncodedFormEntity(params, HTTP.UTF_8);
				post.setEntity(url);

				_registerTask = new RegisterApiTask(this);
				_registerTask.execute(post);
			} catch (UnsupportedEncodingException e) {
				throw new ImprobableCheckedExceptionException(e);
			}
			_state = State.WAITING;
			showState();
		}
	}

	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.register_register_button:
			registerButtonClicked();
			break;
		case R.id.register_view_terms_and_conditions_button:
			viewTermsAndConditionsClicked();
			break;
		default:
			throw new UnexpectedEnumValueException(_state);
		}
	}

	private void viewTermsAndConditionsClicked() {
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://cpanel02.lhc.uk.networkeq.net/~soberfun/terms.html"));
		startActivity(browserIntent);
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
			// When the user clicks on the success confirmation, go back to the
			// sign_in page.
			if (DialogInterface.BUTTON_POSITIVE == which) {
				Intent i = new Intent(this, SignIn.class);
				startActivity(i);
				break;
			} else {
				throw new IllegalStateException();
			}
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
	private class RegisterApiTask extends AsyncTask<HttpPost, Void, HttpResponse> {
		private WeakReference<Register> _parent;
		private String _userAgent;

		public RegisterApiTask(Register parent) {
			// Use a weak-reference for the parent activity. This prevents a memory leak should the activity be destroyed.
			_parent = new WeakReference<Register>(parent);

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

		protected void onPostExecute(HttpResponse result) {
			
			Register parent = _parent.get();
			
			// 'parent' will be null if it has already been garbage collected.
			if (parent._registerTask == this) {

				try {
					// ProcessJSONResponse() appropriately handles a null
					// result.
					APIResponseHandler.ProcessJSONResponse(result, getResources());
					parent._state = State.SUCCESS;
				} catch (OHOWAPIException e) {
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
