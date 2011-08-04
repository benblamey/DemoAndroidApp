package com.ml4d.ohow.activity;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;
import com.ml4d.core.Charset2;
import com.ml4d.core.exceptions.ImprobableCheckedExceptionException;
import com.ml4d.core.exceptions.UnexpectedEnumValueException;
import com.ml4d.core.exceptions.UnknownClickableItemException;
import com.ml4d.ohow.APIResponseHandler;
import com.ml4d.ohow.CredentialStore;
import com.ml4d.ohow.R;
import com.ml4d.ohow.exceptions.NoResponseAPIException;
import com.ml4d.ohow.exceptions.OHOWAPIException;
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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

/**
 * Interactive logic for the 'capturelocation' activity.
 */
public class CaptureLocation extends Activity implements OnClickListener, DialogInterface.OnClickListener {
	
	/**
	 * With the activity lifecycle an an asynchronous HTTP request to handle,
	 * this class is designed as a state machine at its core.
	 * http://en.wikipedia.org/wiki/State_machine
	 */
	private enum State {
		DATA_ENTRY, WAITING, SUCCESS, FAILED, FAILED_INVALID_CREDENTIALS
	}

	/**
	 * The message used only with the State.FAILED state.
	 */
	private String _errorMessage;
	private CaptureTask _captureTask;
	private State _state;
	private DialogInterface _dialog;
	private String _body;
	private Double _longitude;
	private Double _latitude;
	private File _photoFile;

	
	private static final String _jpegMime = "image/jpeg";
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.capturelocation);
		
		findViewById(R.id.capture_location_button_capture).setOnClickListener(this);
		
		// Note that the 'savedInstanceState' bundle is not the 'extras' from the intent that launched the activity.
		// They are available in 'onNewIntent'.
		
		// Get some details from the previous capture step. We assume they are valid, the API will check for us.
		Intent startingIntent = getIntent();
		_body = startingIntent.getStringExtra("body");
		if (null == _body) {
			throw new RuntimeException("This activity should only be started by the CaptureTextPhoto activity (with the details from that step filled in as intent extras).");
		}
		_longitude = startingIntent.getDoubleExtra("longitude", 9999); // The default value will eventually be rejected by the API if it is used.
		_latitude = startingIntent.getDoubleExtra("latitude", 9999); // The default value will eventually be rejected by the API if it is used.
		_photoFile = (File)startingIntent.getSerializableExtra("photoFile");

		if (!CredentialStore.getInstance(this).getHaveVerifiedCredentials()) {
			// Start the sign in activity.
			startActivity(new Intent(this, SignIn.class));
		}

		if (savedInstanceState != null) {
			_state = Enum.valueOf(State.class, savedInstanceState.getString("_state"));
			_errorMessage = savedInstanceState.getString("_errorMessage");
			
			// If we have previously successfully logged in, go back to the data-entry state.
			// Otherwise we will redirect immediately back to the home activity.
			if (State.SUCCESS == _state) {
				_state = State.DATA_ENTRY;
				_errorMessage = "";
			} else if (State.FAILED_INVALID_CREDENTIALS == _state) {
				// When the credentials are invalid, we immediately redirect to the sign in page.
				// We don't want to do this automatically if the user reaches the activity from history.
				_errorMessage = "";
				_state = State.DATA_ENTRY;
			}

			// TODO: restore view states.
			// Because we may have different layouts for portrait and landscape
			// views, we need to manually save and restore the state of the
			// TextViews.
			//restoreTextViewInstanceState(savedInstanceState, R.id.capture_text_photo_edittext_body);

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

	@Override
	protected void onNewIntent (Intent intent) {
		Log.d("OHOW", "CaptureLocation.onNewIntent called - terminating");
		throw new RuntimeException("onNewIntent called!");
	}

	/**
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
			_dialog = ProgressDialog.show(this, resources.getString(R.string.capture_location_waiting_dialog_title),
					resources.getString(R.string.capture_location_waiting_dialog_body), true, // Indeterminate.
					false); // Not cancellable.
			break;
		case SUCCESS:
			
			// TODO - clear stuff so that it isn't here if the user navigates back in history.
			
			// Start the 'home' activity.
			// Credentials/session key has already been stored.
			startActivity(new Intent(this, Home.class));
			
			// Show the user some toast to inform them of the success.
			Toast.makeText(this, resources.getString(R.string.capture_location_waiting_dialog_success), Toast.LENGTH_LONG).show();
			
			_state = State.DATA_ENTRY;
			break;
		case FAILED:
			// Show a 'failed' dialog.
			AlertDialog failedDialog = new AlertDialog.Builder(this).create();
			failedDialog.setTitle(resources.getString(R.string.error_dialog_title));
			failedDialog.setMessage(_errorMessage);
			failedDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", this);
			failedDialog.setCancelable(false); // Prevent the user from cancelling the dialog with the back key.
			failedDialog.show();
			_dialog = failedDialog;
			break;
		case FAILED_INVALID_CREDENTIALS:
			
			// Don't redirect more than once.
			_errorMessage = "";
			_state = State.DATA_ENTRY;
			
			// Clear credentials saved in the store.
			CredentialStore.getInstance(this).clear();
			
			// Go back to the sign in activity.
			startActivity(new Intent(this, SignIn.class));
			
			// Show the user some toast explaining why they have been redirected.
			Toast.makeText(this, resources.getString(R.string.sign_in_redirected_because_credentials_invalid), Toast.LENGTH_LONG).show();
			
			// We leave the location picker as-is (and let it get its state persisted), then if the user goes back, they can try again with their original info.
			break;
		default:
			throw new UnexpectedEnumValueException(_state);
		}
	}

	protected void onSaveInstanceState(Bundle outState) {
		tearEverythingDown();

		// TODO: SAVE view states here.
		
		// Because we have different layouts for portrait and landscape views,
		// we need to manually save and restore the state of the TextViews.
		//saveTextViewInstanceState(outState, R.id.capture_text_photo_edittext_body);

		// Save which view is focused.
		View focusedView = getCurrentFocus();
		if (null != focusedView) {
			outState.putInt("focused_view", focusedView.getId());
		}

		outState.putString("_state", _state.name());
		outState.putString("_errorMessage", _errorMessage);
		
		if (null != _photoFile) {
			outState.putSerializable("_photoFile", _photoFile);
		}
	}

	// TODO: will need these. They can be refactored into a utility class.
//	/**
//	 * Saves the state of the specified TextView.
//	 */
//	private void saveTextViewInstanceState(Bundle state, int textViewId) {
//		Parcelable instanceState = ((TextView) findViewById(textViewId)).onSaveInstanceState();
//		state.putParcelable("textView_id_" + Integer.toString(textViewId), instanceState);
//	}
//
//	/**
//	 * Restores the state of the specified TextView.
//	 */
//	private void restoreTextViewInstanceState(Bundle state, int textViewId) {
//		Parcelable instanceState = state.getParcelable("textView_id_" + Integer.toString(textViewId));
//		if (null != instanceState) {
//			((TextView) findViewById(textViewId)).onRestoreInstanceState(instanceState);
//		}
//	}

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
		tearEverythingDown();
	}

	@Override
	protected void onStop() {
		super.onStop();
		// The activity is no longer visible (it is now "stopped").
		tearEverythingDown();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// The activity is about to be destroyed.
		tearEverythingDown();
	}
	
	/**
	 * Ensures the asynchronous HTTP task is stopped.
	 */
	private void tearEverythingDown() {
				
		// Ensure the Async HTTP task is cleaned away.
		
		// Don't interrupt the operation if it has started. The results are difficult to predict.
		if (_captureTask != null) {
			_captureTask.cancel(false); 
			_captureTask = null;
		}
		
		if (State.WAITING == _state) {
			_errorMessage = getResources().getString(R.string.dialog_error_task_canceled);
			_state = State.FAILED;
		}
	}
	
	private void captureButtonClicked() {

		CredentialStore store = CredentialStore.getInstance(this);
		if (!store.getHaveVerifiedCredentials()) {
			_errorMessage = "";
			_state = State.FAILED_INVALID_CREDENTIALS;
		} else {
						
			// Validate the user data the same as it will be validated by the OHOW API.
			String validationMessage = "";
			
			if (validationMessage.length() > 0) {
				Toast.makeText(this, validationMessage, Toast.LENGTH_LONG).show();
			} else {

				// The HttpClient will verify the certificate is signed by a trusted
				// source.
				HttpPost post = new HttpPost("https://cpanel02.lhc.uk.networkeq.net/~soberfun/1/capture.php");
				post.setHeader("Accept", "application/json");
				post.setHeader("X_OHOW_DEBUG_KEY", "sj30fj5X9whE93Bf0tjfhSh3jkfs2w03udj92");

				// PHP doesn't seem to accept the post if we specify a character set in the 'MultipartEntity' constructor. 
				MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
				final String textMimeType = "text/plain";
				final Charset utf8 = Charset2.getUtf8();
				 
				try {
					entity.addPart(new FormBodyPart("username", new StringBody(store.getUsername(), textMimeType, utf8))); 
					entity.addPart(new FormBodyPart("password", new StringBody(store.getPassword(), textMimeType, utf8)));
					entity.addPart(new FormBodyPart("body", new StringBody(_body, textMimeType, utf8))); // The 'body' was validated by the previous activity. We don't validate it again because the API is going to validate it anyway.
					entity.addPart(new FormBodyPart("longitude", new StringBody(Double.toString(_longitude), textMimeType, utf8)));
					entity.addPart(new FormBodyPart("latitude", new StringBody(Double.toString(_latitude), textMimeType, utf8)));
				} catch (UnsupportedEncodingException e) {
					throw new ImprobableCheckedExceptionException(e);
				}
				
				if (null != _photoFile) {
					FileBody photoFilePart= new FileBody(_photoFile, _photoFile.getAbsolutePath(), _jpegMime, Charset2.getUtf8().name());
					entity.addPart("photo", photoFilePart);
				}
				post.setEntity(entity);
				
				_state = State.WAITING;
				_captureTask = new CaptureTask(this);
				_captureTask.execute(post);
			}
		}

		showState();
	}

	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.capture_location_button_capture:
			captureButtonClicked();
			break;
		default:
			throw new UnknownClickableItemException(view.getId());
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (_state) {
		case FAILED:
			// Something was wrong, go back to data-entry to let the user try again.
			_errorMessage = "";
			_state = State.DATA_ENTRY;
			showState();
			break;
		case SUCCESS:
		case DATA_ENTRY:
		case WAITING:
		case FAILED_INVALID_CREDENTIALS:
			throw new IllegalStateException();
		default:
			throw new UnexpectedEnumValueException(_state);
		}
	}

	/**
	 * Asynchronously performs a HTTP request.
	 */
	private class CaptureTask extends AsyncTask<HttpPost, Void, HttpResponse> {
		private WeakReference<CaptureLocation> _parent;
		private String _userAgent;		 

		public CaptureTask(CaptureLocation parent) {
			// Use a weak-reference for the parent activity. This prevents a memory leak should the activity be destroyed.
			_parent = new WeakReference<CaptureLocation>(parent);

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
			
			CaptureLocation parent = _parent.get();
			
			// 'parent' will be null if it has already been garbage collected.
			if (parent._captureTask == this) {

				CredentialStore auth = CredentialStore.getInstance(parent);
				
				try {
					// ProcessJSONResponse() appropriately handles a null result.
					
					// We don't actually care about the response, we just need to ensure there are no errors.
					APIResponseHandler.ProcessJSONResponse(response, getResources());

					// To complete without error is a success.
					parent._state = State.SUCCESS;
					
				} catch (OHOWAPIException e) {
					parent._state = State.FAILED;
					
					if ((401 == e.getHttpCode()) && (3 == e.getExceptionCode())) {
						// The password was wrong. Clear any saved credentials or session keys.
						auth.clear();
						parent._state = State.FAILED_INVALID_CREDENTIALS;	
					}
					
					parent._errorMessage = e.getLocalizedMessage();
				} catch (NoResponseAPIException e) {
					parent._state = State.FAILED;
					parent._errorMessage = parent.getResources().getString(R.string.comms_error);
				}

				// Allow this task to be garbage-collected as it is no longer needed.
				// I think that for large requests (e.g. images) this helps bring down our memory footprint.
				parent._captureTask = null;

				parent.showState();
			}
		}

	}
	
}
